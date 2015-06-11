/**
 * EqualAngleOld.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package splitstree.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.*;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * @author huson
 *         Date: 03-Jan-2004
 * @deprecated The equal angle algorithm for embedding a circular splits graph
 */
public class EqualAngleOld implements Splits2Network {
    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "Equal angle algorithm (Dress and Huson, 2004)";
    private boolean optionOptimizeDaylight = false;
    private int optionDaylightIterations = 1;
    private boolean optionOptimizeBoxes = false;
    private boolean optionUseWeights = true;
    private boolean optionRunConvexHull = true;
    private boolean optionAvoidCollisions = true;
    private PhyloGraphView phyloGraphView = null;
    Document doc;

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {

        this.doc = doc;
        doc.notifyTasks("Equal Angle", null);

        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(-1);                        //set progress to 0


        phyloGraphView = new PhyloGraphView();
        PhyloGraph graph = phyloGraphView.getPhyloGraph();
        int[] cycle = normalizeCycle(splits.getCycle());

        doc.notifySetProgress(3);

        for (int i = 1; i <= taxa.getNtax(); i++)
            graph.setTaxon2Cycle(cycle[i], i);


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

        doc.notifySetProgress(-1);

        removeTemporaryTrivialEdges(graph);

        if (getOptionRunConvexHull() && usedSplits.cardinality() < splits.getNsplits()) {
            ConvexHull convexHull = new ConvexHull();
            convexHull.setOptionWeights(getOptionUseWeights());
            phyloGraphView = convexHull.apply(doc, taxa, splits, usedSplits, phyloGraphView);
        }


        doc.notifyTasks("Equal Angle", null); // in case this was reset by convex hull
        doc.notifySetProgress(-1);

        assignAnglesToEdges(splits, cycle, graph);

        if (getOptionOptimizeDaylight()) {
            runOptimizeDayLight(taxa, phyloGraphView);
        }

        if (optionOptimizeBoxes) {
            for (Object interiorSplit : interiorSplits) {
                // do something here///
            }
        }

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

        if (splits.getProperties().getCompatibility() != Splits.Properties.COMPATIBLE
                && splits.getProperties().getCompatibility() != Splits.Properties.CYCLIC
                && getOptionAvoidCollisions()) {
            avoidCollisions(graph, splits);
        }

        assignCoordinatesToNodes(optionUseWeights, phyloGraphView);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            graph.setLabel(e, splits.getLabel(graph.getSplit(e)));
        }

        return new Network(taxa, phyloGraphView);
    }

    /**
     * make sure that no two edges leave the same node at the same angle
     *
     * @param graph
     * @param splits
     */
    private void avoidCollisions(PhyloGraph graph, Splits splits) {
        BitSet seen = new BitSet(); // splits we've already seen
        double[] split2newAngle = new double[splits.getNsplits() + 1];
        for (int s = 1; s <= splits.getNsplits(); s++)
            split2newAngle[s] = Double.MIN_VALUE;

        avoidCollisionsRec(splits.getNtax(), graph.getTaxon2Node(1), graph, seen, split2newAngle);

        for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e))
            if (split2newAngle[graph.getSplit(e)] != Double.MIN_VALUE)
                graph.setAngle(e, split2newAngle[graph.getSplit(e)]);
    }

    /**
     * recursively avoid collisions
     *
     * @param ntax
     * @param v
     * @param graph
     * @param seen
     * @param split2newAngle
     */
    private void avoidCollisionsRec(int ntax, Node v, PhyloGraph graph, BitSet seen, double[] split2newAngle) {
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            double eAngle = (split2newAngle[graph.getSplit(e)] != Double.MIN_VALUE) ?
                    split2newAngle[graph.getSplit(e)] : graph.getAngle(e);
            if (!seen.get(graph.getSplit(e))) {
                seen.set(graph.getSplit(e));
                // avoid the first collision found:
                for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                    if (f != e && seen.get(graph.getSplit(f))) {
                        double fAngle = (split2newAngle[graph.getSplit(f)] != Double.MIN_VALUE) ?
                                split2newAngle[graph.getSplit(f)] : graph.getAngle(f);
                        if (Math.abs(eAngle - fAngle) < 0.0000001) {
                            double newAngle = graph.getAngle(e) + 0.2 * Math.PI / ntax;
                            split2newAngle[graph.getSplit(e)] = newAngle;
                            break;
                        }
                    }
                }
                // recursively look at all other edges:
                for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                    avoidCollisionsRec(ntax, v.getOpposite(e), graph, seen, split2newAngle);
                }
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
    private void initGraph(Taxa taxa, Splits splits, int[] cycle, PhyloGraph graph) throws
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
            graph.setNode2Taxa(v, t);
            graph.setTaxon2Node(t, v);

            Edge e = null;
            try {
                e = graph.newEdge(center, v);
            } catch (IllegalSelfEdgeException e1) {
                Basic.caught(e1);
            }
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
    private int[] normalizeCycle(int[] cycle) {
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
    private void wrapSplit(Taxa taxa, Splits splits, int s, int[] cycle, PhyloGraph graph) throws
            Exception {
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
        List leafEdges = new LinkedList();
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
    private boolean isLeafEdge(Edge f, PhyloGraph graph) throws NotOwnerException {
        return graph.getDegree(graph.getSource(f)) == 1 || graph.getDegree(graph.getTarget(f)) == 1;

    }

    /**
     * this removes all temporary trivial edges added to the graph
     *
     * @param graph
     * @throws NotOwnerException
     */
    private void removeTemporaryTrivialEdges(PhyloGraph graph) throws NotOwnerException {
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
            for (Integer t : graph.getNode2Taxa(v)) {
                graph.setNode2Taxa(w, t);
                graph.setTaxon2Node(t, w);
            }

            if (graph.getLabel(w) != null && graph.getLabel(w).length() > 0)
                graph.setLabel(w, graph.getLabel(w) + ", " + graph.getLabel(v));
            else
                graph.setLabel(w, graph.getLabel(v));
            graph.getNode2Taxa(v).clear();
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
     */
    private void assignAnglesToEdges(Splits splits, int[] cycle, PhyloGraph graph)
            throws NotOwnerException, CanceledException {

        doc.notifySubtask("assign angles to edges");
        doc.notifySetMaximumProgress(splits.getNsplits());
        doc.notifySetProgress(0);

        int ntaxa = splits.getNtax();
        double[] split2angle = new double[splits.getNsplits() + 1];
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
            split2angle[s] = (Math.PI * (xp + xq)) / (double) ntaxa;
            doc.notifySetProgress(s);
        }

        doc.notifySetMaximumProgress(graph.getNumberOfEdges());

        int count = 0;
        Iterator it = graph.edgeIterator();
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            graph.setAngle(e, split2angle[graph.getSplit(e)]);
            doc.notifySetProgress(++count);
        }
    }

    /**
     * runs the optimize daylight algorithm
     *
     * @param taxa
     * @param graphView
     * @throws NotOwnerException
     */
    private void runOptimizeDayLight(Taxa taxa, PhyloGraphView graphView) throws CanceledException, NotOwnerException {

        PhyloGraph graph = graphView.getPhyloGraph();
        NodeSet ignore = new NodeSet(graph); // nodes that don't need daylight optimization
        for (int i = 1; i <= getOptionDaylightIterations(); i++) {

            doc.notifySubtask("optimize daylight (" + i + ")");
            doc.notifySetMaximumProgress(graph.getNumberOfNodes());
            doc.notifySetProgress(0);

            int count = 0;

            Iterator it = Basic.randomize(graph.nodeIterator(), 77 * i);
            while (it.hasNext()) {
                Node v = (Node) it.next();
                if (graph.getDegree(v) > 1 && !ignore.contains(v)) {
                    assignCoordinatesToNodes(optionUseWeights, graphView); // need coordinates
                    if (!optimizeDaylightNode(taxa, v, graphView))
                        ignore.add(v);
                    doc.notifySetProgress(++count);
                }
            }
        }
    }

    /**
     * optimize the daylight angles of the graph
     *
     * @param taxa
     * @param v
     * @param graphView
     * @throws NotOwnerException
     * @throws CanceledException
     */
    private boolean optimizeDaylightNode(Taxa taxa, Node v,
                                         PhyloGraphView graphView) throws NotOwnerException, CanceledException {
        PhyloGraph graph = graphView.getPhyloGraph();

        int numComp = 0;
        EdgeIntegerArray edge2comp = new EdgeIntegerArray(graph);
        double[] comp2MinAngle = new double[taxa.getNtax() + 1];
        double[] comp2MaxAngle = new double[taxa.getNtax() + 1];

        // for all edges adjacent to v
        Iterator it = graph.getAdjacentEdges(v);
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            doc.getProgressListener().checkForCancel();

            if (edge2comp.getValue(e) == 0) {
                edge2comp.set(e, ++numComp);
                Node w = graph.getOpposite(v, e);

                // as observed from v
                double angle;
                {
                    Point2D vp = graphView.getLocation(v);
                    Point2D wp = graphView.getLocation(w);
                    angle = Geometry.computeAngle(Geometry.diff(wp, vp));
                }
                Pair minMaxAngle = new Pair(angle, angle); // will contain min and max angles of component

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
                    int c = edge2comp.getValue(e);
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
    private void visitComponentRec(Node root, Node v, Edge e, EdgeIntegerArray edge2comp,
                                   int numComp, PhyloGraph graph, PhyloGraphView graphView,
                                   NodeSet visited, double angle,
                                   Pair minMaxAngle) throws NotOwnerException, CanceledException {

        if (v != root && !visited.contains(v)) {
            doc.getProgressListener().checkForCancel();

            visited.add(v);
            for (Edge f = graph.getFirstAdjacentEdge(v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                if (f != e && edge2comp.getValue(f) == 0) {
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
     * assigns coordinates to nodes
     *
     * @param useWeights
     * @param graphView
     */
    private void assignCoordinatesToNodes(boolean useWeights, PhyloGraphView graphView) throws NotOwnerException, CanceledException {
        PhyloGraph graph = graphView.getPhyloGraph();
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
    private void assignCoordinatesToNodesRec(Node v, BitSet splitsInPath,
                                             NodeSet nodesVisited, boolean useWeights, PhyloGraphView graphView)
            throws NotOwnerException, CanceledException {
        PhyloGraph graph = graphView.getPhyloGraph();

        if (!nodesVisited.contains(v)) {
            doc.getProgressListener().checkForCancel();

            nodesVisited.add(v);
            Iterator it = graph.getAdjacentEdges(v);
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
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
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        // return false; // doesn't work yet!
        return taxa != null && splits != null;
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
    public boolean getOptionOptimizeDaylight() {
        return optionOptimizeDaylight;
    }

    /**
     * set optimize daylight option
     *
     * @param optimizeDaylight
     */
    public void setOptionOptimizeDaylight(boolean optimizeDaylight) {
        this.optionOptimizeDaylight = optimizeDaylight;
    }

    /**
     * should  interior angle optimization be applied?
     * @return apply option?

    public boolean getOptionOptimizeInterior() {
    return optimizeInterior;
    }
     */

    /**
     * set optimize interior angle option
     * @param optimizeInterior

    public void setOptionOptimizeInterior(boolean optimizeInterior) {
    this.optimizeInterior = optimizeInterior;
    }
     */

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
     * @return
     */
    public boolean getOptionRunConvexHull() {
        return optionRunConvexHull;
    }

    /**
     * get the number of times to run daylight optimization
     *
     * @return
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

    public boolean getOptionAvoidCollisions() {
        return optionAvoidCollisions;
    }

    public void setOptionAvoidCollisions(boolean optionAvoidCollisions) {
        this.optionAvoidCollisions = optionAvoidCollisions;
    }

    /**
     * gets the computed PhyloGraphView object
     *
     * @return graphview
     */
    public PhyloGraphView getPhyloGraphView() {

        return phyloGraphView;
    }
}
