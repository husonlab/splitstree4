/**
 * EqualAngle.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Geometry;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * The equal angle algorithm for embedding a circular splits graph
 *
 * @author huson
 * Date: 03-Jan-2004
 */
public class EqualAngle implements Splits2Network {
    public final static String DESCRIPTION = "Equal angle algorithm (Dress & Huson 2004) with equal-daylight &" +
            "  box-opening optimization (Gambette & Huson 2005)";
    private int optionDaylightIterations = 0;
    private int optionOptimizeBoxesIterations = 0;
    private boolean optionUseWeights = true;
    private boolean optionRunConvexHull = true;
    private int optionSpringEmbedderIterations = 0;
    private PhyloGraphView phyloGraphView = null;
    Document doc;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return taxa != null && splits != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        phyloGraphView = new PhyloGraphView();
        return createNetwork(doc, taxa, splits, new HashSet<Integer>(), new HashMap());
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa       the taxa
     * @param splits     the splits
     * @param graphView0 the graphview
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits, PhyloGraphView graphView0) throws Exception {
        phyloGraphView = graphView0;
        return createNetwork(doc, taxa, splits, new HashSet<Integer>(), new HashMap());
    }

    /**
     * apply the layout algorithm only to the selected nodes
     *
     * @param doc
     * @param taxa
     * @param splits
     * @param currentGraphView
     * @return network
     */
    public Network applyToSelected(Document doc, Taxa taxa, Splits splits, PhyloGraphView currentGraphView) throws Exception {
        phyloGraphView = currentGraphView;
        PhyloSplitsGraph graph = currentGraphView.getPhyloGraph();
        //The graph will be rebuild from scratch, so we store the values of edges

        HashMap<Integer, Double> splitAngles = new HashMap<>();
        HashSet<Integer> forbiddenSplits = new HashSet<>();
        //All selected edges and their "split-associated" edges won't move.
        EdgeSet selected = currentGraphView.getSelectedEdges();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            //System.out.println(graph.getSplit(e) + " " + (int)(graph.getSplit(e)));
            if (!forbiddenSplits.contains(graph.getSplit(e)) && (selected.contains(e))) {
                double oldAngle = Geometry.computeAngle(new Point2D.Double(phyloGraphView.getLocation(e.getTarget()).getX() - phyloGraphView.getLocation(e.getSource()).getX(), phyloGraphView.getLocation(e.getTarget()).getY() - phyloGraphView.getLocation(e.getSource()).getY()));
                forbiddenSplits.add(graph.getSplit(e));
                System.out.println(graph.getSplit(e) + " forbidden split : " + oldAngle);
                splitAngles.put(graph.getSplit(e), oldAngle);
            }
        }
        phyloGraphView = new PhyloGraphView();
        phyloGraphView.resetViews();
        return createNetwork(doc, taxa, splits, forbiddenSplits, splitAngles);
    }

    /**
     * initializes the graph
     *
     * @param forbiddenSplits list of splits which won't be modified by the algorithm.
     * @param forbiddenSplits for each forbidden split, its angle.
     */
    private Network createNetwork(Document doc, Taxa taxa, Splits splits, HashSet<Integer> forbiddenSplits, HashMap splitAngles) throws Exception {
        this.doc = doc;
        doc.notifyTasks("Equal Angle", null);
        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(-1);                        //set progress to 0

        PhyloSplitsGraph graph = phyloGraphView.getPhyloGraph();
        int[] cycle = normalizeCycle(splits.getCycle());
        doc.notifySetProgress(3);

        for (int i = 1; i <= taxa.getNtax(); i++)
            graph.setTaxon2Cycle(cycle[i], i);

        doc.notifySetProgress(5);

        initGraph(taxa, splits, cycle, graph);

        List<Integer> interiorSplits = getInteriorSplitsOrdered(taxa, splits);

        doc.notifySubtask("process internal splits");
        doc.notifySetMaximumProgress(interiorSplits.size());    //initialize maximum progress

        BitSet usedSplits = new BitSet();

        {
            int count = 0;
            for (Integer s : interiorSplits) {
                if (SplitsUtilities.isCircular(taxa, cycle, splits.get(s))) {
                    wrapSplit(taxa, splits, s, cycle, graph);
                    usedSplits.set(s, true);
                    doc.notifySetProgress(++count);
                }
            }
        }

        /*
        //Not sure this is necessary : save the angles of the forbidden Splits
        HashMap edgeAngles= new HashMap();
        for(Edge e=graph.getFirstEdge();e!=null;e=e.getNext())
        {
            if (forbiddenSplits.contains((int)(graph.getSplit(e)))){
                edgeAngles.put(e,new Double(graph.getAngle(e)));
            }
        }
        */

        doc.notifySetProgress(-1);
        removeTemporaryTrivialEdges(graph);
        assignAnglesToEdges(splits, cycle, graph, forbiddenSplits);


        if (getOptimizeBoxes()) {
            assignCoordinatesToNodes(optionUseWeights, phyloGraphView); // we need this to detect collisions

            runOptimizeBoxes(phyloGraphView, forbiddenSplits);

            //We build the list of all existing splits so far
            for (int splitNb = 0; splitNb < graph.countSplits(); splitNb++) {
                if (!forbiddenSplits.contains(graph.getSplitIds()[splitNb])) {
                    forbiddenSplits.add(graph.getSplitIds()[splitNb]);
                }
            }
        }

        if (getOptionRunConvexHull() && usedSplits.cardinality() < interiorSplits.size()) {
            doc.notifySetProgress(60);
            ConvexHull convexHull = new ConvexHull();
            convexHull.setOptionWeights(getOptionUseWeights());
            phyloGraphView = convexHull.apply(doc, taxa, splits, usedSplits, phyloGraphView);
        }

        //We only assign angles to the new edges created (forbiddenSplits contains the list of splits BEFORE convex hull)
        assignAnglesToEdges(splits, cycle, graph, forbiddenSplits);

        if (getRunSpringEmbedder()) {
            //set progress to 0
            assignCoordinatesToNodes(optionUseWeights, phyloGraphView); // we need this to detect collisions

            computeSpringEmbedding(doc, phyloGraphView, getOptionSpringEmbedderIterations());
            assignAverageAngleToEdges(phyloGraphView);

            assignCoordinatesToNodes(optionUseWeights, phyloGraphView);
        }


        doc.notifyTasks("Equal Angle", null); // in case this was reset by convex hull

        if (getOptimizeDaylight()) {
            doc.notifySetProgress(80);
            runOptimizeDayLight(taxa, phyloGraphView);
        }
        doc.notifySetProgress(90);

        /*
        //Load the values saved for the splitangle
        for(Edge e=graph.getFirstEdge();e!=null;e=e.getNext())
        {
            if (splitAngles.containsKey((int)(graph.getSplit(e)))){
                graph.setAngle(e,((Double) splitAngles.get((int)(graph.getSplit(e)))).doubleValue());
            }
        }
        */

        // rotateAbout so that edge leaving first taxon ist pointing at 9 o'clock
        if (graph.getNumberOfNodes() > 0 && graph.getNumberOfEdges() > 0) {
            Node v = graph.getTaxon2Node(1);
            Edge e = graph.getFirstAdjacentEdge(v);
            double angle = Math.PI + graph.getAngle(e); // add pi to be consist with Embed
            for (e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                graph.setAngle(e, graph.getAngle(e) - angle);
            }
            assignCoordinatesToNodes(optionUseWeights, phyloGraphView); // need coordinates
        }
        assignCoordinatesToNodes(optionUseWeights, phyloGraphView);

        doc.notifySetProgress(100);   //set progress to 100%

/*
        //Label the nodes
        for(Node v=graph.getFirstNode();v!=null;v=v.getNext())
        {
        int id=v.getId();
        phyloGraphView.setLabel(v,graph.getLabel(v)+" "+id);
        }
*/
        /*
        Rectangle2D rect=phyloGraphView.getBBox();
        WorldShape bbox=new WorldShape(rect,"bbox",new Point(50,50));
        phyloGraphView.getWorldShapes().add(bbox);
        */
        phyloGraphView.resetViews();
        return new Network(taxa, phyloGraphView);
    }

    /**
     * assigns the average angle of each set of edges corresponding to a given split
     * to all edges representing that split.
     * This is used when different edges for the same split have different angles
     *
     * @param graphView
     */
    private void assignAverageAngleToEdges(PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        int maxSplit = 0;
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext())
            if (graph.getSplit(e) > maxSplit)
                maxSplit = graph.getSplit(e);

        double[] split2angle = new double[maxSplit + 1];
        int[] split2count = new int[maxSplit + 1];

        EdgeSet edgesUsed = new EdgeSet(graph);
        BitSet splitsUsed = new BitSet();

        Node v = graph.getTaxon2Node(1);
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            visitAllEdges(v, e, graph, graphView, edgesUsed, splitsUsed, split2angle, split2count);
        }
        for (int i = 0; i < split2count.length; i++)
            if (split2count[i] > 0)
                split2angle[i] /= split2count[i];

        // set edge angles:
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            graph.setAngle(e, split2angle[graph.getSplit(e)]);
        }

    }

    /**
     * visit all edges and determine average angle of each split
     *
     * @param v
     * @param e
     * @param graph
     * @param graphView
     * @param edgesUsed
     * @param splitsUsed
     * @param split2angle
     * @param split2count
     */
    private void visitAllEdges(Node v, Edge e, PhyloSplitsGraph graph, PhyloGraphView graphView, EdgeSet edgesUsed, BitSet splitsUsed, double[] split2angle, int[] split2count) {
        if (!edgesUsed.contains(e)) {
            Node w = e.getOpposite(v);
            edgesUsed.add(e);
            Point2D a = graphView.getLocation(v);
            Point2D b = graphView.getLocation(w);
            Point2D c = new Point2D.Double(b.getX() - a.getX(), b.getY() - a.getY());
            double angle = Geometry.computeAngle(c);
            int s = graph.getSplit(e);
            split2angle[s] += angle;
            split2count[s]++;
            splitsUsed.set(s);
            for (Edge f = w.getFirstAdjacentEdge(); f != null; f = w.getNextAdjacentEdge(f)) {
                if (f != e && !splitsUsed.get(graph.getSplit(f))) {

                    visitAllEdges(w, f, graph, graphView, edgesUsed, splitsUsed, split2angle, split2count);
                }
            }
            splitsUsed.set(s, false);
        }
    }

    /**
     * Computes a spring embedding of the graph
     *
     * @param iterations the number of iterations used
     */
    public void computeSpringEmbedding(Document doc, PhyloGraphView phyloGraphView, int iterations) {
        Graph G = phyloGraphView.getGraph();
        NodeDoubleArray xPos = new NodeDoubleArray(G);
        NodeDoubleArray yPos = new NodeDoubleArray(G);

        Rectangle2D rect = phyloGraphView.getBBox();
        double width = rect.getWidth();
        double height = rect.getHeight();

        if (G.getNumberOfNodes() < 2)
            return;

        try {
            doc.notifyTasks("Equal Angle", "Spring embedder");
            doc.notifySetMaximumProgress(iterations);    //initialize maximum progress
            doc.notifySetProgress(0);


            for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                Point2D p = phyloGraphView.getLocation(v);
                xPos.put(v, p.getX());
                yPos.put(v, p.getY());
            }

            // run iterations of spring embedding:
            double log2 = Math.log(2);
            for (int count = 1; count <= iterations; count++) {
                double k = Math.sqrt(width * height / G.getNumberOfNodes()) / 2;

                double l2 = 25 * log2 * Math.log(1 + count);

                double tx = width / l2;
                double ty = height / l2;

                NodeDoubleArray xDispl = new NodeDoubleArray(G);
                NodeDoubleArray yDispl = new NodeDoubleArray(G);

                // repulsive forces

                for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                    double xv = xPos.get(v);
                    double yv = yPos.get(v);

                    for (Node u = G.getFirstNode(); u != null; u = G.getNextNode(u)) {
                        if (u == v)
                            continue;
                        double xdist = xv - xPos.get(u);
                        double ydist = yv - yPos.get(u);
                        double dist = xdist * xdist + ydist * ydist;
                        if (dist < 1e-3)
                            dist = 1e-3;
                        double frepulse = k * k / dist;
                        xDispl.put(v, xDispl.get(v) + frepulse * xdist);
                        yDispl.put(v, yDispl.get(v) + frepulse * ydist);
                    }

                    for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
                        Node a = G.getSource(e);
                        Node b = G.getTarget(e);
                        if (a == v || b == v)
                            continue;
                        double xdist = xv -
                                (xPos.get(a) + xPos.get(b)) / 2;
                        double ydist = yv -
                                (yPos.get(a) + yPos.get(b)) / 2;
                        double dist = xdist * xdist + ydist * ydist;
                        if (dist < 1e-3) dist = 1e-3;
                        double frepulse = k * k / dist;
                        xDispl.put(v, xDispl.get(v) + frepulse * xdist);
                        yDispl.put(v, yDispl.get(v) + frepulse * ydist);
                    }
                }

                // attractive forces

                for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
                    Node u = G.getSource(e);
                    Node v = G.getTarget(e);

                    double xdist = xPos.get(v) - xPos.get(u);
                    double ydist = yPos.get(v) - yPos.get(u);

                    double dist = Math.sqrt(xdist * xdist + ydist * ydist);

                    double f = ((G.getDegree(u) + G.getDegree(v)) / 16.0);

                    dist /= f;

                    xDispl.put(v, xDispl.get(v) - xdist * dist / k);
                    yDispl.put(v, yDispl.get(v) - ydist * dist / k);
                    xDispl.put(u, xDispl.get(u) + xdist * dist / k);
                    yDispl.put(u, yDispl.get(u) + ydist * dist / k);
                }

                // preventions

                for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                    double xd = xDispl.get(v);
                    double yd = yDispl.get(v);

                    double dist = Math.sqrt(xd * xd + yd * yd);

                    xd = tx * xd / dist;
                    yd = ty * yd / dist;

                    double xp = xPos.get(v) + xd;
                    double yp = yPos.get(v) + yd;

                    xPos.put(v, xp);
                    yPos.put(v, yp);
                }
                doc.notifySetProgress(count);
            }
        } catch (CanceledException ex) {
            doc.getProgressListener().setUserCancelled(false);
        } finally {
            // set node positions
            for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                phyloGraphView.setLocation(v, xPos.get(v), yPos.get(v));
            }
        }
    }

    /**
     * initializes the graph
     *
     * @param taxa
     * @param splits
     * @param cycle
     * @param graph
     */
    private void initGraph
    (Taxa
             taxa, Splits
             splits, int[] cycle, PhyloSplitsGraph
             graph) throws
            NotOwnerException {
        // map from each taxon to it's trivial split in splits
        int[] taxon2split = new int[taxa.getNtax() + 1];

        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet part = splits.get(s);
            if (part.cardinality() == taxa.getNtax() - 1) {
                part = part.getComplement(taxa.getNtax());
            }
            if (part.cardinality() == 1) // is trivial split
            {
                int t = part.max();
                taxon2split[t] = s;
            }
        }

        Node center = graph.newNode();
        for (int i = 1; i <= taxa.getNtax(); i++) {
            int t = cycle[i];

            Node v = graph.newNode();

            graph.setLabel(v, taxa.getLabel(t));
            graph.addTaxon(v, t);

            Edge e = graph.newEdge(center, v);
            if (taxon2split[t] != 0) {
                int s = taxon2split[t];
                graph.setWeight(e, splits.getWeight(s));
                graph.setSplit(e, s);
            } else
                graph.setSplit(e, -1); // mark as temporary split
        }
    }

    /**
     * returns the list of all non-trivial splits, ordered by by increasing size
     * of the split part containing taxon 1
     *
     * @param taxa
     * @param splits
     * @return non-trivial splits
     */
    private List<Integer> getInteriorSplitsOrdered(Taxa taxa, Splits splits) {
        SortedSet<Pair<Integer, Integer>> interiorSplits = new TreeSet<>(new Pair<Integer, Integer>()); // first component is cardinality, second is id

        for (int id = 1; id <= splits.getNsplits(); id++) {
            TaxaSet part = splits.get(id);
            if (part.cardinality() > 1 && part.cardinality() < taxa.getNtax() - 1) {
                if (!part.get(1))
                    part = part.getComplement(taxa.getNtax());

                interiorSplits.add(new Pair<>(part.cardinality(), id));
            }
        }
        List<Integer> interiorSplitIDs = new LinkedList<>();
        for (Pair<Integer, Integer> interiorSplit : interiorSplits) {
            interiorSplitIDs.add(interiorSplit.getSecond());
        }
        return interiorSplitIDs;
    }


    /**
     * normalizes cycle so that cycle[1]=1
     *
     * @param cycle
     * @return normalized cycle
     */
    private int[] normalizeCycle
    (int[] cycle) {
        int[] result = new int[cycle.length];

        int i = 1;
        while (cycle[i] != 1 && i < cycle.length)
            i++;
        int j = 1;
        while (i < cycle.length) {
            result[j] = cycle[i];
            i++;
            j++;
        }
        i = 1;
        while (j < result.length) {
            result[j] = cycle[i];
            i++;
            j++;
        }
        return result;
    }

    /**
     * adds an interior split using the wrapping algorithm
     *
     * @param taxa
     * @param cycle
     * @param splits
     * @param s
     * @param graph
     */
    private void wrapSplit(Taxa taxa, Splits splits, int s, int[] cycle, PhyloSplitsGraph graph) throws Exception {
        TaxaSet part = (TaxaSet) (splits.get(s).clone());
        if (part.get(1))
            part = part.getComplement(taxa.getNtax());

        int xp = 0; // first member of split part not containing taxon 1
        int xq = 0; // last member of split part not containing taxon 1
        for (int i = 1; i <= taxa.getNtax(); i++) {
            int t = cycle[i];
            if (part.get(t)) {
                if (xp == 0)
                    xp = t;
                xq = t;
            }
        }
        Node v = graph.getTaxon2Node(xp);
        Node z = graph.getTaxon2Node(xq);
        Edge targetLeafEdge = graph.getFirstAdjacentEdge(z);

        Edge e = graph.getFirstAdjacentEdge(v);
        v = graph.getOpposite(v, e);
        Node u = null;
        List<Edge> leafEdges = new LinkedList<>();
        leafEdges.add(e);
        Edge nextE;

        NodeSet nodesVisited = new NodeSet(graph);

        do {
            Edge f = e;
            if (nodesVisited.contains(v)) {
                System.err.println(graph);

                throw new Exception("Node already visited: " + v);
            }
            nodesVisited.add(v);

            Edge f0 = f; // f0 is edge by which we enter the node
            f = graph.getNextAdjacentEdgeCyclic(f0, v);
            while (isLeafEdge(f, graph)) {
                leafEdges.add(f);
                if (f == targetLeafEdge) {
                    break;
                }
                if (f == f0)
                    throw new RuntimeException("Node wraparound: f=" + f + " f0=" + f0);

                f = graph.getNextAdjacentEdgeCyclic(f, v);
            }
            if (isLeafEdge(f, graph))
                nextE = null; // at end of chain
            else
                nextE = f; // continue along boundary
            Node w = graph.newNode();
            Edge h = graph.newEdge(w, null, v, f0, Edge.AFTER, Edge.AFTER, null);
            // here we make sure that new edge is inserted after f0

            graph.setSplit(h, s);
            graph.setWeight(h, splits.getWeight(s));
            if (u != null) {
                h = graph.newEdge(w, u, null);
                graph.setSplit(h, graph.getSplit(e));
                graph.setWeight(h, graph.getWeight(e));
            }
            for (Object leafEdge : leafEdges) {
                f = (Edge) leafEdge;
                h = graph.newEdge(w, graph.getOpposite(v, f));

                graph.setSplit(h, graph.getSplit(f));
                graph.setWeight(h, graph.getWeight(f));
                graph.deleteEdge(f);
            }
            leafEdges.clear();

            if (nextE != null) {
                v = graph.getOpposite(v, nextE);
                e = nextE;
                u = w;
            }
        } while (nextE != null);
    }

    /**
     * does this edge lead to a leaf?
     *
     * @param f
     * @param graph
     * @return is leaf edge
     */
    private boolean isLeafEdge(Edge f, PhyloSplitsGraph graph) throws NotOwnerException {
        return graph.getDegree(graph.getSource(f)) == 1 || graph.getDegree(graph.getTarget(f)) == 1;

    }

    /**
     * this removes all temporary trivial edges added to the graph
     *
     * @param graph
     * @throws NotOwnerException
     */
    private void removeTemporaryTrivialEdges(PhyloSplitsGraph graph) throws NotOwnerException {
        EdgeSet tempEdges = new EdgeSet(graph);
        for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
            if (graph.getSplit(e) == -1) // temporary leaf edge
                tempEdges.add(e);
        }

        for (Edge e : tempEdges) {
            Node v, w;
            if (graph.getDegree(graph.getSource(e)) == 1) {
                v = graph.getSource(e);
                w = graph.getTarget(e);
            } else {
                w = graph.getSource(e);
                v = graph.getTarget(e);
            }
            for (Integer t : graph.getTaxa(v)) {
                graph.addTaxon(w, t);
            }

            if (graph.getLabel(w) != null && graph.getLabel(w).length() > 0)
                graph.setLabel(w, graph.getLabel(w) + ", " + graph.getLabel(v));
            else
                graph.setLabel(w, graph.getLabel(v));
            graph.clearTaxa(v);
            graph.setLabel(v, null);
            graph.deleteNode(v);
        }
    }

    /**
     * assigns angles to all edges in the graph
     *
     * @param splits
     * @param cycle
     * @param graph
     * @param forbiddenSplits : set of all the splits such as their edges won't have their angles changed
     */
    private void assignAnglesToEdges(Splits splits, int[] cycle, PhyloSplitsGraph graph, Set forbiddenSplits) throws NotOwnerException {

        //To avoid to be at this point when the user cancels we don't give him the possibility to cancel here.
        int ntaxa = splits.getNtax();

        //We create the list of angles representing the taxas on a circle.
        double[] TaxaAngles = new double[ntaxa + 1];
        for (int t = 1; t < ntaxa + 1; t++) {
            TaxaAngles[t] = (Math.PI * 2 * t / (double) ntaxa);
        }

        double[] split2angle = new double[splits.getNsplits() + 1];

        assignAnglesToSplits(TaxaAngles, split2angle, splits, cycle);

        for (var e : graph.edges()) {
            if (!forbiddenSplits.contains((graph.getSplit(e)))) {
                graph.setAngle(e, split2angle[graph.getSplit(e)]);
            }
        }
    }


    /**
     * assigns angles to the splits in the graph, considering that they are located exactly "in the middle" of two taxa
     * so we fill split2angle using TaxaAngles.
     *
     * @param splits
     * @param cycle
     * @param TaxaAngles  for each taxa, its angle
     * @param split2angle for each split, its angle
     */
    private void assignAnglesToSplits
    (double[] TaxaAngles,
     double[] split2angle, Splits
             splits, int[] cycle)
            throws NotOwnerException {

        int ntaxa = splits.getNtax();
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet part = splits.get(s);
            if (part.get(1))
                part = part.getComplement(splits.getNtax());
            int xp = 0; // first position of split part not containing taxon 1
            int xq = 0; // last position of split part not containing taxon 1
            for (int i = 1; i <= ntaxa; i++) {
                int t = cycle[i];
                if (part.get(t)) {
                    if (xp == 0)
                        xp = i;
                    xq = i;
                }
            }

            int xpneighbour = (xp - 2) % ntaxa + 1;
            int xqneighbour = (xq) % ntaxa + 1;
            //the split, when represented on the circle of the taxas, is a line which interescts the circle in two
            //places : SplitsByAngle is a sorted list (sorted by the angle of these intersections), where every
            // split thus appears 2 times (once per instersection)
            double TaxaAngleP;
            double TaxaAngleQ;
            TaxaAngleP = Geometry.midAngle(TaxaAngles[xp], TaxaAngles[xpneighbour]);
            TaxaAngleQ = Geometry.midAngle(TaxaAngles[xq], TaxaAngles[xqneighbour]);

            split2angle[s] = Geometry.moduloTwoPI((TaxaAngleQ + TaxaAngleP) / 2);
            if (xqneighbour == 1) {
                split2angle[s] = Geometry.moduloTwoPI(split2angle[s] + Math.PI);
            }
            //System.out.println("split from "+xp+","+xpneighbour+" ("+TaxaAngleP+") to "+xq+","+xqneighbour+" ("+TaxaAngleQ+") -> "+split2angle[s]+" $ "+(Math.PI * (xp + xq)) / (double) ntaxa);s
        }
    }

    /**
     * runs the optimize daylight algorithm
     *
     * @param taxa
     * @param graphView
     * @throws NotOwnerException
     */
    private void runOptimizeDayLight(Taxa taxa, PhyloGraphView graphView) throws NotOwnerException {

        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        NodeSet ignore = new NodeSet(graph);
        try {
            doc.notifySubtask("optimize daylight");
            doc.notifySetMaximumProgress(graph.getNumberOfNodes());
            doc.notifySetProgress(0);
            for (int i = 1; i <= getOptionDaylightIterations(); i++) {

                doc.notifySubtask("optimize daylight (" + i + ")");
                doc.notifySetMaximumProgress(graph.getNumberOfNodes());
                doc.notifySetProgress(0);

                int count = 0;

                Iterator it = Basic.randomize(graph.nodes().iterator(), 77 * i);
                while (it.hasNext()) {
                    Node v = (Node) it.next();
                    doc.notifySetProgress(++count);
                    if (graph.getDegree(v) > 1 && !ignore.contains(v)) {
                        assignCoordinatesToNodes(optionUseWeights, graphView); // need coordinates
                        if (!optimizeDaylightNode(taxa, v, graphView))
                            ignore.add(v);
                    }
                }
            }
        } catch (CanceledException e) {
            doc.getProgressListener().setUserCancelled(false);
        }
    }

    /**
     * optimize the daylight angles of the graph
     *
     * @param v
     * @param graphView
     */
    private boolean optimizeDaylightNode(Taxa taxa, Node v, PhyloGraphView graphView) throws NotOwnerException, CanceledException {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        int numComp = 0;
        EdgeIntArray edge2comp = new EdgeIntArray(graph);
        double[] comp2MinAngle = new double[taxa.getNtax() + 1];
        double[] comp2MaxAngle = new double[taxa.getNtax() + 1];

        for (Edge e : v.adjacentEdges()) {
            doc.getProgressListener().checkForCancel();

            if (edge2comp.getInt(e) == 0) {
                edge2comp.set(e, ++numComp);
                Node w = graph.getOpposite(v, e);

                // as observed from v
                double angle;
                {
                    Point2D vp = graphView.getLocation(v);
                    Point2D wp = graphView.getLocation(w);
                    angle = Geometry.computeAngle(Geometry.diff(wp, vp));
                }
                Pair<Double, Double> minMaxAngle = new Pair<>(angle, angle); // will contain min and max angles of component

                NodeSet visited = new NodeSet(graph);
                visitComponentRec(v, w, null, edge2comp, numComp, graph, graphView, visited, angle, minMaxAngle);
                if (visited.size() == graph.getNumberOfNodes())
                    return false; // visited all nodes, forget it.

                comp2MinAngle[numComp] = minMaxAngle.getFirstDouble();
                comp2MaxAngle[numComp] = minMaxAngle.getSecondDouble();
            }
        }
        if (numComp > 1) {
            double total = 0;
            for (int c = 1; c <= numComp; c++) {
                total += comp2MaxAngle[c] - comp2MinAngle[c];
            }
            if (total < 2 * Math.PI) {
                double daylightGap = (2 * Math.PI - total) / numComp;
                double[] comp2epsilon = new double[numComp + 1];
                for (int c = 1; c <= numComp; c++) {
                    double alpha = 0;
                    for (int i = 1; i < c; i++)
                        alpha += comp2MaxAngle[i] - comp2MinAngle[i];
                    alpha += (c - 1) * daylightGap;
                    comp2epsilon[c] = alpha - comp2MinAngle[c];
                }
                for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                    int c = edge2comp.getInt(e);
                    graph.setAngle(e, graph.getAngle(e) + comp2epsilon[c]);
                }
            }
        }
        return true;
    }


    /**
     * recursively visit the whole subgraph, obtaining the min and max observed angle
     *
     * @param root
     * @param v
     * @param e
     * @param edge2comp
     * @param numComp
     * @param graph
     * @param visited
     * @param minMaxAngle
     */
    private void visitComponentRec(Node root, Node v, Edge e, EdgeIntArray edge2comp, int numComp, PhyloSplitsGraph graph, PhyloGraphView graphView, NodeSet visited,
                                   double angle, Pair<Double, Double> minMaxAngle) throws NotOwnerException, CanceledException {

        if (v != root && !visited.contains(v)) {
            doc.getProgressListener().checkForCancel();

            visited.add(v);
            for (Edge f = graph.getFirstAdjacentEdge(v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                if (f != e && edge2comp.getInt(f) == 0) {
                    edge2comp.set(f, numComp);
                    Node w = graph.getOpposite(v, f);
                    double newAngle = angle + Geometry.computeObservedAngle(graphView.getLocation(root),
                            graphView.getLocation(v), graphView.getLocation(w));
                    if (newAngle < minMaxAngle.getFirstDouble())
                        minMaxAngle.setFirst(newAngle);
                    if (newAngle > minMaxAngle.getSecondDouble())
                        minMaxAngle.setSecond(newAngle);
                    visitComponentRec(root, w, f, edge2comp, numComp, graph, graphView, visited,
                            newAngle, minMaxAngle);
                }
            }
        }
    }


    /**
     * optimize the boxes of the graph
     *
     * @param graphView
     */
    private void runOptimizeBoxes(PhyloGraphView graphView, HashSet forbiddenSplits) throws NotOwnerException {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        //We first build EdgeSplits, where each split is linked with the set containing all its edges
        final HashMap<Integer, List<Edge>> EdgeSplits = getEdgeSplits(graph);
        Edge currentEdge;
        List currentEdges;
        try {
            int nbIterations = optionOptimizeBoxesIterations;
            doc.notifySubtask("optimize boxes");
            doc.notifySetMaximumProgress(nbIterations * graph.countSplits());
            doc.notifySetProgress(1);
            int counter = 0;

            Set<Integer> SplitsSet = EdgeSplits.keySet();
            double totalSize = 0;
            for (Object aSplitsSet : SplitsSet) {
                int CurrentSplit = (Integer) aSplitsSet;
                currentEdges = (List) EdgeSplits.get((int) (CurrentSplit));
                totalSize += maximizeArea(currentEdges, graph, graph.getAngle((Edge) currentEdges.get(0)), graph.getAngle((Edge) currentEdges.get(0))).getFirstDouble();
            }

            double originalSize = totalSize;
            double previousSize = totalSize;

            for (int compteur = 0; (compteur < nbIterations); compteur++) {
                int score = (int) (Math.floor(100 * (totalSize - originalSize) / originalSize));
                double miniScore = (Math.floor(10000 * (totalSize - previousSize) / previousSize)) / 100.00;
                if (score > 0) {
                    if (miniScore > 0) {
                        if (miniScore < 10) {
                            doc.notifySubtask("box optim.: +" + score + "%  (" + (compteur) + ":+" + miniScore + "%)");
                        } else {
                            doc.notifySubtask("box optim.: +" + score + "%  (" + (compteur) + ":+" + (int) Math.floor(miniScore) + "%)");
                        }
                    } else {
                        doc.notifySubtask("box optimiz.: +" + score + "%  (" + (compteur) + ":" + miniScore + "%)");
                    }
                } else {
                    if (miniScore > 0) {
                        if (miniScore < 10) {
                            doc.notifySubtask("box optim.: " + score + "%  (" + (compteur) + ":+" + miniScore + "%)");
                        } else {
                            doc.notifySubtask("box optim.: " + score + "%  (" + (compteur) + ":+" + (int) Math.floor(miniScore) + "%)");
                        }
                    } else {
                        doc.notifySubtask("box optim.: " + score + "%  (" + (compteur) + ":" + miniScore + "%)");
                    }
                }
                previousSize = totalSize;

                SplitsSet = EdgeSplits.keySet();
                totalSize = 0;

                //Iterator allSplits=SplitsSet.iterator();
                for (Object aSplitsSet : SplitsSet) {
                    int currentSplit = (Integer) aSplitsSet;
                    if (!forbiddenSplits.contains(currentSplit)) {
                        //We can move this split as it's is not in the forbidden list.

                        //If the split is improvable, it will have more chances to be improved:
                        counter++;
                        doc.notifySetProgress(counter);
                        currentEdges = (List) EdgeSplits.get(currentSplit);
                        currentEdge = (Edge) currentEdges.get(0);
                        Iterator CurrentEdgesIt;

                        double oldAngle = graph.getAngle(currentEdge);

                        //Compute the min and max variations of the split considering only the split itself (which must remain planar).
                        Pair CurrentExtremeAngles = TrigClockAngles(currentEdges, graph);

                        //Choose a new angle for the split between those two critical angles :
                        double trigAngle = oldAngle + CurrentExtremeAngles.getFirstDouble();
                        double clockAngle = oldAngle + CurrentExtremeAngles.getSecondDouble();


                        Pair newTrigClock = CreatesCollisions(trigAngle, clockAngle, oldAngle, currentEdges, oldAngle, phyloGraphView);

                        System.err.println("createsCollisions returned: " + newTrigClock);

                        trigAngle = newTrigClock.getFirstDouble();
                        clockAngle = newTrigClock.getSecondDouble();

                        if (currentEdges.size() > 1) {
                            System.err.println("\n Split " + currentSplit + " Angle : " + oldAngle + " - Edge: " + currentEdge);

                            Pair optimized = maximizeArea(currentEdges, graph, clockAngle, trigAngle);
                            CurrentEdgesIt = currentEdges.iterator();
                            while (CurrentEdgesIt.hasNext()) {
                                currentEdge = (Edge) CurrentEdgesIt.next();

                                System.err.println("Changing: " + graph.getAngle(currentEdge) + " -> " + optimized.getSecond());

                                graph.setAngle(currentEdge, optimized.getSecondDouble());
                            }
                            totalSize += optimized.getFirstDouble();
                        } else {
                            //The split only has one edge, we do not move it
                        }
                    }
                }
            }
        } catch (CanceledException e) {
            doc.getProgressListener().setUserCancelled(false);
        }//System.out.println("An Exception : "+e);/*doc.getProgressListener()userCancelled=;*/};
    }


    /**
     * returns (Max(splitArea),argMax(splitArea)) where the split is defined by its SplitEdges and the
     * split angle has to be between minAngle and maxAngle
     * <p/>
     * Can be used to get the current area of the split by setting minAngle=maxAngle= the split angle
     *
     * @param SplitEdges list of the Edges of the Split, not necessary all directed in the same sense.
     * @param graph
     * @param minAngle
     * @param maxAngle
     */
    public Pair<Double, Double> maximizeArea(List SplitEdges, PhyloSplitsGraph graph, double minAngle, double maxAngle) {
        if (minAngle < maxAngle)
            System.err.println(">> " + minAngle + " ... " + maxAngle);
        //We will first express the area of the split as A cos x + B sin x, x being the split angle
        double A = 0;
        double B = 0;
        double area = 0;

        Iterator EdgesIt = SplitEdges.iterator();
        Edge CurrentEdge = (Edge) EdgesIt.next();
        double resultAngle = 0;
        double splitAngle = Geometry.moduloTwoPI(graph.getAngle(CurrentEdge));

        if (Math.abs(graph.getWeight(CurrentEdge)) > 0.0000000000001) {
            while (EdgesIt.hasNext()) {
                Edge ThePreviousEdge = CurrentEdge;
                CurrentEdge = (Edge) EdgesIt.next();
                Node NodeA = ThePreviousEdge.getSource();
                Node NodeB = CurrentEdge.getSource();
                if (!(NodeA.isAdjacent(NodeB))) {
                    NodeB = CurrentEdge.getTarget();
                }

                Edge uncompEdge = NodeA.getCommonEdge(NodeB);
                if (Geometry.moduloTwoPI(graph.getAngle(uncompEdge) - splitAngle) < Math.PI) {
                    A = A + Math.sin(graph.getAngle(uncompEdge)) * graph.getWeight(uncompEdge);
                    B = B - Math.cos(graph.getAngle(uncompEdge)) * graph.getWeight(uncompEdge);
                } else {
                    A = A + Math.sin(graph.getAngle(uncompEdge) + Math.PI) * graph.getWeight(uncompEdge);
                    B = B - Math.cos(graph.getAngle(uncompEdge) + Math.PI) * graph.getWeight(uncompEdge);
                }
            }

            A = A * graph.getWeight(CurrentEdge);
            B = B * graph.getWeight(CurrentEdge);
            //A cos x + B sin x = C cos (x-D)
            double C = Math.sqrt(A * A + B * B);
            double D;
            D = Math.atan(B / A);

            if (A * Math.cos(Math.atan(B / A)) + B * Math.sin(Math.atan(B / A)) < 0) {
                D = D + Math.PI;
            }
            double shiftedD = minAngle + Geometry.moduloTwoPI(D - minAngle);
            if (shiftedD > maxAngle) {
                //We affect "almost" maxAngle to the split
                if (C * Math.cos(minAngle - D) > C * Math.cos(maxAngle - D)) {
                    shiftedD = Math.min(minAngle + 0.00001, (minAngle + maxAngle) / 2);
                } else {
                    //We affect "almost" maxAngle to the split
                    shiftedD = Math.max(maxAngle - 0.00001, (minAngle + maxAngle) / 2);
                }
            }
            resultAngle = shiftedD;
            area = C * Math.cos(resultAngle - D);
        }

        if (!(resultAngle == resultAngle)) {
            //The angle found is not a Number : we don't move the split. This is the case where all edges of the split
            // are in the same place so we could choose a more clever angle? Let's leave the old angle right now.
            resultAngle = splitAngle;
            area = 0;
        }

        if (area != 0)
            System.err.println("area " + area + " result " + resultAngle);

        return new Pair<>(area, resultAngle);
    }


    /**
     * computes the min and the max angles the parallel edges of the split can have,
     * considering only the configuration of the split itself.
     * <p/>
     * needs to be corrected to deal with boxes with DiffAngle=0
     *
     * @param SplitEdges sorted list of the split edges
     * @param graph
     */
    public Pair TrigClockAngles(List SplitEdges, PhyloSplitsGraph graph) {
        Iterator EdgesIt = SplitEdges.iterator();
        Edge CurrentEdge = (Edge) EdgesIt.next();
        double SplitAngle = Geometry.moduloTwoPI(graph.getAngle(CurrentEdge));
        if (SplitAngle >= Math.PI) {
            SplitAngle -= Math.PI;
        }

        double CurrentTrig = SplitAngle + Math.PI;
        double CurrentClock = CurrentTrig;
        Node firstNodeA = CurrentEdge.getSource();
        boolean onlyOneEdge = false;
        while (EdgesIt.hasNext()) {
            Edge ThePreviousEdge = CurrentEdge;
            CurrentEdge = (Edge) EdgesIt.next();
            Node NodeB = ThePreviousEdge.getSource();
            Node NodeA = CurrentEdge.getSource();
            if (!(NodeA.isAdjacent(NodeB))) {
                NodeA = CurrentEdge.getTarget();
            }

            double AngleAB = graph.getAngle(NodeA.getCommonEdge(NodeB));
            double DiffAngle = Geometry.moduloTwoPI(AngleAB - SplitAngle);
            if (DiffAngle < Math.PI) {
                //AngleAB is candidate for Trig, AngleAB-180 for Clock
                if (!(Geometry.moduloTwoPI(AngleAB - CurrentTrig) < Math.PI)) {
                    CurrentTrig = AngleAB;
                }
                if (!(Geometry.moduloTwoPI(CurrentClock - AngleAB + Math.PI) < Math.PI)) {
                    CurrentClock = AngleAB - Math.PI;
                }
            } else {
                //AngleAB-180 is candidate for Trig, AngleAB for Clock
                if (!(Geometry.moduloTwoPI(AngleAB - Math.PI - CurrentTrig) < Math.PI)) {
                    CurrentTrig = AngleAB - Math.PI;
                }
                if (!(Geometry.moduloTwoPI(CurrentClock - AngleAB) < Math.PI)) {
                    CurrentClock = AngleAB;
                }
            }
            onlyOneEdge = Geometry.squaredDistance(phyloGraphView.getLocation(NodeA), phyloGraphView.getLocation(firstNodeA)) < 0.0000000000001;
        }
        if (onlyOneEdge) {
            return new Pair<>(2 * Math.PI, -2 * Math.PI);
        } else {
            return new Pair<>(Geometry.moduloTwoPI(CurrentTrig - SplitAngle), -Geometry.moduloTwoPI(SplitAngle - CurrentClock));
        }
    }

    /**
     * returns a HashMap which gives for each split the list of its edges.
     *
     * @param graph
     */
    public HashMap<Integer, List<Edge>> getEdgeSplits(PhyloSplitsGraph graph) {
        final HashMap<Integer, List<Edge>> EdgeSplits = new HashMap<>();
        Edge currentEdge = graph.getFirstEdge();
        List<Edge> currentEdges;

        int currentSplit;
        int i = 0;
        while (i < graph.getNumberOfEdges()) {

            if (i > 0) {
                currentEdge = currentEdge.getNext();
            }
            currentSplit = graph.getSplit(currentEdge);

            if (EdgeSplits.containsKey(currentSplit)) {
                currentEdges = EdgeSplits.get(currentSplit);
            } else {
                currentEdges = new ArrayList<>();
            }
            currentEdges.add(currentEdge);
            EdgeSplits.put(graph.getSplit(currentEdge), currentEdges);
            i++;
        }

        //Now we have to sort the Edges
        Set<Integer> SplitsSet = EdgeSplits.keySet();

        for (Integer aSplit : SplitsSet) {
            EdgeArray<Edge> A1Edge = new EdgeArray<>(graph);
            EdgeArray<Edge> A2Edge = new EdgeArray<>(graph);
            EdgeArray<Integer> adjNb = new EdgeArray<>(graph);

            currentSplit = aSplit;
            currentEdges = EdgeSplits.get(currentSplit);
            Iterator currentEdgesIt = currentEdges.iterator();

            //If there is more than one edge in the split, we sort them
            if (currentEdges.size() > 1) {
                //We find, for each edge of the split, its one or two parallel neighbour edge(s) in the split.
                //The 2 edges with 1 parallel neighbour edge is an "extreme" edge of the split.
                while (currentEdgesIt.hasNext()) {
                    currentEdge = (Edge) currentEdgesIt.next();

                    for (Node adjNode : currentEdge.getSource().adjacentNodes()) {
                        if (A2Edge.get(currentEdge) != null)
                            break;
                        if (adjNode != currentEdge.getTarget()) {

                            for (Edge parallEdge : adjNode.adjacentEdges()) {
                                if (graph.getSplit(parallEdge) == graph.getSplit(currentEdge)) {
                                    if (A1Edge.get(currentEdge) == null) {
                                        A1Edge.put(currentEdge, parallEdge);
                                        adjNb.put(currentEdge, 1);
                                        //System.out.println("First parallel neighbour of "+CurrentEdge+" : "+ParallEdge);
                                    } else {
                                        if (A2Edge.get(currentEdge) == null) {
                                            A2Edge.put(currentEdge, parallEdge);
                                            adjNb.put(currentEdge, 2);
                                            //System.out.println("Second parallel neighbour of "+CurrentEdge+" : "+ParallEdge);
                                        } else {
                                            //The split is not planar and this algorithm will crash !!!
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //We first detect one of the extreme edges
                //Not necessary CurrentEdges=(List) EdgeSplits.get(CurrentSplit);
                currentEdgesIt = currentEdges.iterator();
                currentEdge = (Edge) currentEdgesIt.next();
                while ((currentEdgesIt.hasNext()) && (adjNb.get(currentEdge) != 1)) {
                    currentEdge = (Edge) currentEdgesIt.next();
                    //System.out.println("Current edge "+CurrentEdge+" : "+((Integer) AdjNb.get(CurrentEdge)).intValue());
                }

                //We check if everything is all right:
                if ((adjNb.get(currentEdge)) != 1) {
                    System.out.println("(the graph is not planar! Big Problem here!!! " + adjNb.get(currentEdge));
                }

                //Then we go through all the edges of the split to identify the 4 extreme nodes,
                //so our stop condition is to reach the other extreme edge
                Edge TheNextEdge = A1Edge.get(currentEdge);
                currentEdges = new ArrayList<>();
                while ((adjNb.get(TheNextEdge)) > 1) {
                    currentEdges.add(currentEdge);
                    if (A1Edge.get(TheNextEdge) == currentEdge) {
                        currentEdge = TheNextEdge;
                        TheNextEdge = A2Edge.get(TheNextEdge);
                    } else {
                        currentEdge = TheNextEdge;
                        TheNextEdge = A1Edge.get(TheNextEdge);
                    }
                }

                //TheNextEdge is the other extreme edge
                currentEdges.add(currentEdge);
                currentEdges.add(TheNextEdge);
                EdgeSplits.put(currentSplit, currentEdges);
            } else {
                //There is only one edge in the split: we don't move it
            }
        }
        return EdgeSplits;
    }

    /**
     * Knowing the two critical angles after the local optimization, detect the collisions and
     * return the two critical angles after the global optimization.
     * <p/>
     * Works only with splits which have more than 1 edge
     * <p/>
     * The exclusion zone has not been implemented yet (as in practice there is no node inside it)
     *
     * @param clockAngle clockAngle found so far
     * @param trigAngle  trigAngle found so far
     * @param oldAngle
     * @param SplitEdges sorted list of the split edges
     * @param Angle      we want to affect to the split
     * @param graphView
     */
    public Pair<Double, Double> CreatesCollisions(double trigAngle, double clockAngle, double oldAngle, List SplitEdges, double Angle, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        double newTrigAngle = trigAngle;
        double newClockAngle = clockAngle;
        boolean clockBlocked = false;
        boolean trigBlocked = false; //to save a little time down there
        //The critical angles we find will be stored in rresults1 and results2
        //We first determine the 4 extreme nodes of the split
        NodeSet visited1 = new NodeSet(graph);
        NodeSet visited2 = new NodeSet(graph);
        double[] rresults1 = new double[2];
        double[] results2 = new double[2];
        rresults1[0] = 2 * Math.PI;
        rresults1[1] = 2 * Math.PI;
        results2[0] = 0;
        results2[1] = 0;
        Iterator ItSplitEdges = SplitEdges.iterator();
        Edge currentEdge = (Edge) ItSplitEdges.next();
        Node ZeNode1 = currentEdge.getTarget();
        Node ZeNode2 = currentEdge.getSource();
        visited1.add(ZeNode2);
        visited2.add(ZeNode1);
        Node previousNode1 = ZeNode1;
        Node previousNode2 = ZeNode2;
        while (ItSplitEdges.hasNext()) {
            Edge TcurrentEdge = (Edge) ItSplitEdges.next();
            Node currentNode1 = TcurrentEdge.getSource();
            Node currentNode2 = TcurrentEdge.getTarget();

            if (currentNode2.isAdjacent(previousNode1)) {
                currentNode1 = currentNode2;
                currentNode2 = TcurrentEdge.getSource();
            }

            visited1.add(currentNode2);
            visited2.add(currentNode1);
            previousNode1 = currentNode1;
            previousNode2 = currentNode2;
        }
        assignCoordinatesToNodes(optionUseWeights, phyloGraphView); // we need this to detect collisions
        double firstBoxAngle = Geometry.moduloTwoPI(Geometry.basicComputeAngle(graphView.getLocation(ZeNode1), graphView.getLocation(ZeNode2), graphView.getLocation(previousNode1)));

        //We go through the 2 parts of the graph to find "defender" and "striker" nodes
        if (firstBoxAngle < Math.PI) {
            visitComponentRec2(ZeNode1, currentEdge, 0, 2 * Math.PI, 2 * Math.PI, ZeNode1, ZeNode2, previousNode1, previousNode2, graph, graphView, visited1, rresults1, false);
            visitComponentRec1(ZeNode2, currentEdge, 0, 0, ZeNode1, ZeNode2, previousNode1, previousNode2, graph, graphView, visited2, results2);
        } else {
            visitComponentRec2(previousNode1, currentEdge, 0, 2 * Math.PI, 2 * Math.PI, previousNode1, previousNode2, ZeNode1, ZeNode2, graph, graphView, visited1, rresults1, false);
            visitComponentRec1(ZeNode2, currentEdge, 0, 0, previousNode1, previousNode2, ZeNode1, ZeNode2, graph, graphView, visited2, results2);
        }

        //We use the information of defender and striker nodes to eventually change the critical angles.
        if (results2[0] > Math.PI + rresults1[0]) {
            newClockAngle = graph.getAngle(currentEdge);
            clockBlocked = true;
        } else {
            if ((oldAngle - clockAngle) > (Math.PI + rresults1[0] - results2[0])) {
                newClockAngle = oldAngle - (rresults1[0] + Math.PI - results2[0]);
            }
        }

        if (results2[1] > Math.PI + rresults1[1]) {
            newTrigAngle = graph.getAngle(currentEdge);
            trigBlocked = true;
        } else {
            if ((trigAngle - oldAngle) > (rresults1[1] + Math.PI - results2[1])) {
                newTrigAngle = oldAngle + Math.PI + rresults1[1] - results2[1];
            }
        }

        //We do the same, looking from the other part of the graph
        visited1 = new NodeSet(graph);
        visited2 = new NodeSet(graph);
        double[] rresults2 = new double[2];
        rresults2[0] = 2 * Math.PI;
        rresults2[1] = 2 * Math.PI;
        results2[0] = 0;
        results2[1] = 0;
        ItSplitEdges = SplitEdges.iterator();
        currentEdge = (Edge) ItSplitEdges.next();
        ZeNode1 = currentEdge.getTarget();
        ZeNode2 = currentEdge.getSource();
        visited1.add(ZeNode2);
        visited2.add(ZeNode1);
        previousNode1 = ZeNode1;
        previousNode2 = ZeNode2;
        while (ItSplitEdges.hasNext()) {
            Edge TcurrentEdge = (Edge) ItSplitEdges.next();
            Node currentNode1 = TcurrentEdge.getSource();
            Node currentNode2 = TcurrentEdge.getTarget();

            if (currentNode2.isAdjacent(previousNode1)) {
                currentNode1 = currentNode2;
                currentNode2 = TcurrentEdge.getSource();
            }
            visited1.add(currentNode2);
            visited2.add(currentNode1);
            previousNode1 = currentNode1;
            previousNode2 = currentNode2;
        }

        if (!(clockBlocked && trigBlocked)) {
            if (firstBoxAngle < Math.PI) {
                visitComponentRec2(previousNode2, currentEdge, 0, (2 * Math.PI), (2 * Math.PI), previousNode2, previousNode1, ZeNode2, ZeNode1, graph, graphView, visited2, rresults2, false);
                visitComponentRec1(previousNode1, currentEdge, 0, 0, previousNode2, previousNode1, ZeNode2, ZeNode1, graph, graphView, visited1, results2);
            } else {
                visitComponentRec2(ZeNode2, currentEdge, 0, 2 * Math.PI, 2 * Math.PI, ZeNode2, ZeNode1, previousNode2, previousNode1, graph, graphView, visited2, rresults2, false);
                visitComponentRec1(previousNode1, currentEdge, 0, 0, ZeNode2, ZeNode1, previousNode2, previousNode1, graph, graphView, visited1, results2);
            }

            if (results2[0] > Math.PI + rresults2[0]) {
                newClockAngle = graph.getAngle(currentEdge);

            } else {
                if ((oldAngle - newClockAngle) > (rresults2[0] + Math.PI - results2[0])) {
                    newClockAngle = oldAngle - (Math.PI + rresults2[0] - results2[0]);
                }
            }

            if (results2[1] > Math.PI + rresults2[1]) {
                newTrigAngle = graph.getAngle(currentEdge);
            } else {
                if ((newTrigAngle - oldAngle) > (Math.PI + rresults2[1] - results2[1])) {
                    newTrigAngle = oldAngle + Math.PI + rresults2[1] - results2[1];
                }
            }
        }

        return new Pair<>(newTrigAngle, newClockAngle);
    }

    /**
     * recursively visit the whole subgraph, obtaining the min and max observed angle
     *
     * @param v
     * @param e
     * @param specialNode     1 if v is a neighbour of angle1in, 2 if v neighbour of angle2in else 0
     * @param previousAngle1  previous angle, except when specialNode=1
     * @param previousAngle2  previous angle, except when specialNode=2
     * @param angle1in
     * @param angle1out
     * @param angle2in
     * @param angle2out
     * @param graph
     * @param visited
     * @param foundParameters angle1 angle2
     */
    private void visitComponentRec2(Node v, Edge e, int specialNode, double previousAngle1, double previousAngle2, Node angle1in, Node angle1out, Node angle2in, Node angle2out, PhyloSplitsGraph graph, PhyloGraphView graphView, NodeSet visited, double[] foundParameters, boolean dontCompute) {// throws CanceledException {
        double newAngle1 = 2 * Math.PI;
        double newAngle2 = 2 * Math.PI;
        boolean localDontCompute;
        if (!visited.contains(v)) {
            visited.add(v);
            if (!dontCompute) {
                if (v != angle1in) {
                    if ((specialNode == 1) || (specialNode == 3)) {
                        newAngle1 = Geometry.basicComputeAngle(graphView.getLocation(angle1in), graphView.getLocation(v), graphView.getLocation(angle1out));
                    } else {
                        newAngle1 = previousAngle1 + Geometry.signedDiffAngle(Geometry.basicComputeAngle(graphView.getLocation(angle1in), graphView.getLocation(v), graphView.getLocation(angle1out)), previousAngle1);
                    }
                    if (newAngle1 < foundParameters[0]) {
                        foundParameters[0] = newAngle1;
                    }
                }

                if (v != angle2in) {
                    if ((specialNode == 2) || (specialNode == 3)) {
                        newAngle2 = Geometry.basicComputeAngle(graphView.getLocation(angle2in), graphView.getLocation(angle2out), graphView.getLocation(v));
                    } else {
                        newAngle2 = previousAngle2 + Geometry.signedDiffAngle(Geometry.basicComputeAngle(graphView.getLocation(angle2in), graphView.getLocation(angle2out), graphView.getLocation(v)), previousAngle2);
                    }
                    if (newAngle2 < foundParameters[1]) {
                        foundParameters[1] = newAngle2;
                    }
                }
            }

            for (Edge f = graph.getFirstAdjacentEdge(v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                if (f != e) {
                    Node w = graph.getOpposite(v, f);
                    localDontCompute = Geometry.squaredDistance(graphView.getLocation(w), graphView.getLocation(v)) <= 0.0000000000001;
                    if (v == angle1in) {
                        if (dontCompute) {
                            visitComponentRec2(w, f, specialNode, previousAngle1, previousAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                        } else {
                            //If the two extreme nodes are together, notice it:
                            if (Geometry.squaredDistance(graphView.getLocation(angle1in), graphView.getLocation(angle2in)) > 0.0000000000001) {
                                visitComponentRec2(w, f, 1, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                            } else {
                                visitComponentRec2(w, f, 3, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                            }
                        }
                    } else {
                        if (v == angle2in) {
                            if (dontCompute) {
                                visitComponentRec2(w, f, specialNode, previousAngle1, previousAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                            } else {
                                //If the two extreme nodes are together, notice it
                                if (Geometry.squaredDistance(graphView.getLocation(angle1in), graphView.getLocation(angle2in)) > 0.0000000000001) {
                                    visitComponentRec2(w, f, 2, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                                } else {
                                    visitComponentRec2(w, f, 3, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                                }
                            }
                        } else {
                            if (dontCompute) {
                                visitComponentRec2(w, f, specialNode, previousAngle1, previousAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                            } else {
                                visitComponentRec2(w, f, 0, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters, localDontCompute);
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * recursively visit the whole subgraph, obtaining the min and max observed angle
     *
     * @param v
     * @param e
     * @param angle1in
     * @param angle1out
     * @param angle2in
     * @param angle2out
     * @param graph
     * @param visited
     * @param foundParameters xmin ymin xmax ymax angle1 angle2
     */
    private void visitComponentRec1(Node v, Edge e, double previousAngle1, double previousAngle2, Node angle1in, Node angle1out, Node angle2in, Node angle2out, PhyloSplitsGraph graph, PhyloGraphView graphView, NodeSet visited, double[] foundParameters) {
        if (!visited.contains(v)) {
            visited.add(v);

            double newAngle1 = previousAngle1 + Geometry.signedDiffAngle(Geometry.basicComputeAngle(graphView.getLocation(angle1in), graphView.getLocation(v), graphView.getLocation(angle1out)), previousAngle1 + Math.PI);
            if (newAngle1 > foundParameters[0]) {
                foundParameters[0] = newAngle1;
            }

            double newAngle2 = previousAngle2 + Geometry.signedDiffAngle(Geometry.basicComputeAngle(graphView.getLocation(angle2in), graphView.getLocation(angle2out), graphView.getLocation(v)), previousAngle2 + Math.PI);
            if (newAngle2 > foundParameters[1]) {
                foundParameters[1] = newAngle2;
            }

            for (Edge f = graph.getFirstAdjacentEdge(v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                if (f != e) {
                    Node w = graph.getOpposite(v, f);
                    visitComponentRec1(w, f, newAngle1, newAngle2, angle1in, angle1out, angle2in, angle2out, graph, graphView, visited, foundParameters);
                }
            }
        }
    }

    /**
     * assigns coordinates to nodes
     *
     * @param useWeights
     * @param graphView
     */
    private void assignCoordinatesToNodes(boolean useWeights, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        if (graph.getNumberOfNodes() == 0)
            return;
        Node v = graph.getTaxon2Node(1);
        graphView.setLocation(v, new Point2D.Float(0, 0));

        BitSet splitsInPath = new BitSet();
        NodeSet nodesVisited = new NodeSet(graph);

        assignCoordinatesToNodesRec(v, splitsInPath, nodesVisited, useWeights, graphView);
    }


    /**
     * recursively assigns coordinates to all nodes
     *
     * @param v
     * @param splitsInPath
     * @param nodesVisited
     * @param useWeights
     * @param graphView
     */
    private void assignCoordinatesToNodesRec(Node v, BitSet splitsInPath, NodeSet nodesVisited, boolean useWeights, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        if (!nodesVisited.contains(v)) {
            //Deleted so that the user can cancel and it doesn't destroy everything: doc.getProgressListener().checkForCancel();
            nodesVisited.add(v);
            for (Edge e : v.adjacentEdges()) {
                int s = graph.getSplit(e);
                if (!splitsInPath.get(s)) {
                    Node w = graph.getOpposite(v, e);
                    Point2D p = Geometry.translateByAngle(graphView.getLocation(v),
                            graph.getAngle(e), useWeights ? graph.getWeight(e) : 1);
                    graphView.setLocation(w, p);
                    splitsInPath.set(s, true);
                    assignCoordinatesToNodesRec(w, splitsInPath, nodesVisited, useWeights, graphView);
                    splitsInPath.set(s, false);
                }
            }
        }
    }


    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * should daylight optimization be applied?
     *
     * @return apply option?
     */
    public boolean getOptimizeDaylight() {
        return optionDaylightIterations > 0;
    }


    /**
     * scale edge lengths by weights?
     *
     * @return use weights?
     */
    public boolean getOptionUseWeights() {
        return optionUseWeights;
    }

    /**
     * scale edge lengths by weights?
     *
     * @param useWeights
     */
    public void setOptionUseWeights(boolean useWeights) {
        this.optionUseWeights = useWeights;
    }

    /**
     * run convex hull algorithm on non circular splits?
     *
     * @param runConvexHull
     */
    public void setOptionRunConvexHull(boolean runConvexHull) {
        this.optionRunConvexHull = runConvexHull;
    }

    /**
     * runs convex hull algorith,m on non circular splits?
     *
     * @return option
     */
    public boolean getOptionRunConvexHull() {
        return optionRunConvexHull;
    }

    /**
     * get the number of times to run daylight optimization
     *
     * @return option
     */
    public int getOptionDaylightIterations() {
        return optionDaylightIterations;
    }

    /**
     * set the number of times to run daylight optimization
     *
     * @param optionDaylightIterations
     */
    public void setOptionDaylightIterations(int optionDaylightIterations) {
        this.optionDaylightIterations = optionDaylightIterations;
    }

    /**
     * gets the computed PhyloGraphView object
     *
     * @return graphview
     */
    public PhyloGraphView getPhyloGraphView() {

        return phyloGraphView;
    }

    public boolean getOptimizeBoxes() {
        return optionOptimizeBoxesIterations > 0;
    }

    /**
     * get the number of times to run boxes optimization
     *
     * @return option
     */
    public int getOptionOptimizeBoxesIterations() {
        return optionOptimizeBoxesIterations;
    }

    /**
     * set the number of times to run daylight optimization
     *
     * @param optionOptimizeBoxesIterations
     */
    public void setOptionOptimizeBoxesIterations(int optionOptimizeBoxesIterations) {
        this.optionOptimizeBoxesIterations = optionOptimizeBoxesIterations;
    }

    public int getOptionSpringEmbedderIterations() {
        return optionSpringEmbedderIterations;
    }

    public void setOptionSpringEmbedderIterations(int optionSpringEmbedderIterations) {
        this.optionSpringEmbedderIterations = optionSpringEmbedderIterations;
    }

    public boolean getRunSpringEmbedder() {
        return optionSpringEmbedderIterations > 0;
    }
}
