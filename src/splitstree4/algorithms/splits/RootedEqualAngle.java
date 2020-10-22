/**
 * RootedEqualAngle.java
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
import jloda.util.ProgressListener;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * A rooted version of the equal angle algorithm for computing a splits graph
 *
 * @author huson
 * Date: 03-Jan-2004
 */
public class RootedEqualAngle implements Splits2Network {
    public final static String DESCRIPTION = "Rooted equal angle algorithm (Gambette and Huson, submitted 2005)";
    private boolean optionOptimizeDaylight = false;
    private int optionDaylightIterations = 1;
    private boolean optionOptimizeBoxes = false;
    private boolean optionUseWeights = true;
    private boolean optionRunConvexHull = true;
    private int optionMaxAngle = 120;
    private boolean optionSpecialSwitch = false;
    private ProgressListener progress;
    PhyloGraphView graphView;

    /**
     * Applies the method to the given data
     *
     * @param taxa0   the taxa
     * @param splits0 the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa0, Splits splits0) throws Exception {
        return apply(doc, taxa0, splits0, new PhyloGraphView());
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa0      the taxa
     * @param splits0    the splits
     * @param graphView0 the graph view
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa0, Splits splits0, PhyloGraphView graphView0) throws Exception {

        progress = doc.getProgressListener(); //Set new progress bar.
        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(0);                        //set progress to 0

        Sets sets = doc.getSets();
        if (sets == null) {
            sets = new Sets();
            doc.setSets(sets);
        }
        TaxaSet outgroup = sets.getTaxSet("Outgroup", taxa0);

        if (outgroup == null) {
            outgroup = new TaxaSet();
            outgroup.set(1); // will assume taxon is outgroup
            sets.addTaxSet("Outgroup", taxa0.getLabels(outgroup));
        }

        // here we add an additional taxon to the end of the list of taxa.
        // this taxon is the root. We remove all references to it from the graph later
        // the actual root node will remain in the graph
        Taxa taxa = new Taxa();
        Splits splits = new Splits();
        addRootTaxon(outgroup, taxa0, splits0, taxa, splits);
        //System.err.println(taxa.toString() + " " + splits.toString(taxa));

        // the graph view and graph
        graphView = graphView0;
        PhyloSplitsGraph graph = graphView.getPhyloGraph();


        doc.notifySetProgress(3);

        // need to set this
        for (int i = 1; i <= taxa.getNtax(); i++) {
            graph.setTaxon2Cycle(splits.getCycle()[i], i);
        }
        doc.notifySetProgress(5);

        // set up the initial star
        initGraph(taxa, splits, splits.getCycle(), graph);

        doc.notifySetProgress(10);

        List<Integer> interiorSplits = getInteriorSplitsOrdered(taxa, splits);

        doc.notifySetProgress(15);

        BitSet usedSplits = new BitSet();

        // process all the interior splits
        {
            int[] cycle = normalizeCycle(splits.getCycle());
            for (Integer s : interiorSplits) {
                if (SplitsUtilities.isCircular(taxa, cycle, splits.get(s))) {
                    wrapSplit(taxa, splits, s, cycle, graph);
                    usedSplits.set(s, true);
                } else {
                    System.err.println("Non circular split: " + splits.get(s));
                }
            }
        }
        usedSplits.or(getTrivialSplits(taxa, splits));

        doc.notifySetProgress(50);

        removeTemporaryTrivialEdges(graph);

        doc.notifySetProgress(55);

        // process any remaining splits using the convex hull extension algorithm
        if (getOptionRunConvexHull()) {
            ConvexHull convexHull = new ConvexHull();
            convexHull.setOptionWeights(getOptionUseWeights());

            graphView = convexHull.apply(doc, taxa, splits, usedSplits, graphView);
        }

        doc.notifySetProgress(65);

        // assign initial angles
        assignAnglesToEdges(splits, splits.getCycle(), graph);

        // optimize daylight angles
        if (getBrokenOptionOptimizeDaylight()) {
            doc.notifySetProgress(70);
            runOptimizeDayLight(taxa, graphView);
        }
        doc.notifySetProgress(80);

        // optimize boxes
        if (optionOptimizeBoxes) {
            for (Integer interiorSplit : interiorSplits) {
                // do something here///
            }
        }
        doc.notifySetProgress(90);

        // rotateAbout so that edge leaving last taxon ist pointing at 12 o'clock
        {
            Node v = graph.getTaxon2Node(taxa.getNtax());
            Edge e = graph.getFirstAdjacentEdge(v);
            double angle = 0.5 * Math.PI + graph.getAngle(e); // add pi to be consist with Embed
            for (e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                graph.setAngle(e, graph.getAngle(e) - angle);
            }
            assignCoordinatesToNodes(optionUseWeights, taxa.getNtax(), graphView); // need coordinates
        }

        assignCoordinatesToNodes(optionUseWeights, taxa.getNtax(), graphView);


        doc.notifySetProgress(100);   //set progress to 100%

        // remove references to the root taxon from the graph, leaving the actual root node in
        removeRootTaxon(taxa.getNtax(), splits0.getNsplits(), graphView);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) > 0) {
                graph.setLabel(e, splits.getLabel(graph.getSplit(e)));
            }
        }

        graphView.resetViews();
        return new Network(taxa0, graphView);  // yes, the original taxon set taxa0
    }

    /**
     * creates a new taxon set and split set with an additional root taxon
     *
     * @param outgroup name of outgroup taxon
     * @param taxa0    original taxa
     * @param splits0  original splits
     * @param taxa     new taxa
     * @param splits   new splits
     */
    private void addRootTaxon(TaxaSet outgroup, Taxa taxa0, Splits splits0, Taxa taxa, Splits splits) {
        try {
            int outGroupId = outgroup.getBits().nextSetBit(0); // todo: this only uses the first element in the outgroup, need to use all!

            float averageDist = (float) computeAverageDistance(taxa0, splits0, outGroupId);
            taxa.setNtax(taxa0.getNtax() + 1);

            for (int t = 1; t <= taxa0.getNtax(); t++)
                taxa.setLabel(t, taxa0.getLabel(t));

            int rootId = taxa.getNtax();
            taxa.setLabel(rootId, "root");

            // extend all splits
            splits.setNtax(taxa.getNtax());
            splits.setNsplits(0);

            float maxWeight = 0;
            float outGroupWeight = 0;
            float outGroupConfidence = 0;
            float rootWeight = 0;

            for (int s = 1; s <= splits0.getNsplits(); s++) {
                TaxaSet split = (TaxaSet) splits0.get(s).clone();

                if (splits0.getWeight(s) > maxWeight)
                    maxWeight = splits0.getWeight(s);

                if ((split.cardinality() == 1 && split.get(outGroupId))
                        || ((split.cardinality() == taxa0.getNtax() - 1 && !split.get(outGroupId)))) {
                    if (split.get(outGroupId))
                        split.set(rootId);
                    outGroupWeight = splits0.getWeight(s);
                    if (outGroupWeight > 0.5f * averageDist)
                        rootWeight = outGroupWeight - 0.5f * averageDist;
                    else
                        rootWeight = 0.1f * outGroupWeight;
                    /*
                    System.err.println("outGroupWeight " + outGroupWeight);
                    System.err.println("averDist " + averageDist);
                    System.err.println("rootWeight " + rootWeight);
                    System.err.println("Rest " + (outGroupWeight - rootWeight));
                    */

                    outGroupConfidence = splits0.getConfidence(s);
                    splits.add(split, rootWeight, outGroupConfidence, splits0.getLabel(s));
                } else // notjust  outgrouptaxon against the rest
                {
                    if (split.get(outGroupId))
                        split.set(rootId);
                    splits.add(split, splits0.getWeight(s), splits0.getConfidence(s), splits0.getLabel(s));
                }
            }
            // add outgroup split:
            TaxaSet outgroupSplit = new TaxaSet();
            outgroupSplit.set(outGroupId);
            splits.add(outgroupSplit, outGroupWeight - rootWeight, outGroupConfidence);

            // add root split:
            TaxaSet rootSplit = new TaxaSet();
            rootSplit.set(rootId);
            splits.add(rootSplit, 0.1f * maxWeight);

            int rootPos = 0;
            int[] cycle = new int[taxa.getNtax() + 1];
            for (int t0 = 1, t = 1; t0 <= taxa0.getNtax(); t0++, t++) {
                if (splits0.getCycle()[t0] == outGroupId) // put the root next to the outgroup
                {
                    cycle[t] = rootId;
                    rootPos = t;
                    t++;
                }
                cycle[t] = splits0.getCycle()[t0];
            }
            // rotateAbout cycle so that root is first item in cycle:
            int[] cycle2 = new int[taxa.getNtax() + 1];
            for (int t = 1; t <= taxa.getNtax(); t++) {
                int t2 = t - rootPos + 1;
                if (t2 <= 0)
                    t2 += taxa.getNtax();
                cycle2[t2] = cycle[t];
            }
            splits.setCycle(cycle2);

        } catch (SplitsException ex) {
            Basic.caught(ex);
        }
    }

    /**
     * computes the average distance of the out group to all other taxa
     *
     * @param taxa
     * @param splits
     * @param outGroupTaxon
     * @return average distances
     */
    private double computeAverageDistance(Taxa taxa, Splits splits, int outGroupTaxon) {
        double dist = 0;
        for (int t = 1; t <= taxa.getNtax(); t++) {
            if (t != outGroupTaxon) {
                for (int s = 1; s <= splits.getNsplits(); s++) {
                    TaxaSet split = splits.get(s);
                    if (split.get(t) != split.get(outGroupTaxon))
                        dist += splits.getWeight(s);
                }
            }
        }
        if (taxa.getNtax() <= 1)
            return 0;
        else
            return dist / (taxa.getNtax() - 1);
    }

    /**
     * removes temporary root taxon from graph
     *
     * @param rootTaxonId
     * @param graphView
     * @return the root node
     */
    private Node removeRootTaxon(int rootTaxonId, int numOrigSplits, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        Node v = graph.getTaxon2Node(rootTaxonId);
        Edge e = v.getFirstAdjacentEdge();
        Node w = e.getOpposite(v);
        // set all additional split ids to 0:
        for (Edge f = w.getFirstAdjacentEdge(); f != null; f = w.getNextAdjacentEdge(f))
            if (graph.getSplit(f) > numOrigSplits)
                graph.setSplit(f, 0);
        graph.removeTaxon(rootTaxonId);
        return w;
    }


    /**
     * initializes the graph
     *
     * @param taxa
     * @param splits
     * @param cycle
     * @param graph
     */
    private void initGraph(Taxa taxa, Splits splits, int[] cycle, PhyloSplitsGraph graph) throws
            NotOwnerException, SplitsException {
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

        BitSet checkTaxa = new BitSet();

        Node center = graph.newNode();
        for (int i = 1; i <= taxa.getNtax(); i++) {
            int t = cycle[i];


            if (checkTaxa.get(t)) {
                throw new SplitsException("Cycle contains repeated elements");
            }
            checkTaxa.set(t);

            Node v = graph.newNode();
            graph.setLabel(v, taxa.getLabel(t));
            graph.addTaxon(v, t);

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
        SortedSet<Pair<Integer, Integer>> interiorSplits = new TreeSet<>(new Pair<>()); // first component is cardinality, second is id

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

    private BitSet getTrivialSplits(Taxa taxa, Splits splits) {
        final BitSet result = new BitSet();
        for (int id = 1; id <= splits.getNsplits(); id++) {
            TaxaSet part = splits.get(id);
            if (part.cardinality() == 1 || part.cardinality() == taxa.getNtax() - 1) {
                result.set(id);
            }
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

        int xp = 0; // first member of split part not containing  taxon  1
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
            for (Edge leafEdge : leafEdges) {
                f = leafEdge;
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
        for (Edge e : graph.edges()) {
            if (graph.getSplit(e) == -1) // temporary leaf edge
            {
                Node v, w;
                if (e.getSource().getDegree() == 1) {
                    v = e.getSource();
                    w = e.getTarget();
                } else {
                    w = e.getSource();
                    v = e.getTarget();
                }
                if (graph.getNumberOfTaxa(v) > 0) {
                    int t = graph.getTaxa(v).iterator().next();
                    graph.addTaxon(w, t);
                }
                graph.deleteNode(v);
            }
        }
    }

    /**
     * assigns angles to all edges in the graph
     *
     * @param splits
     * @param cycle
     * @param graph
     */
    private void assignAnglesToEdges(Splits splits, int[] cycle, PhyloSplitsGraph graph)
            throws NotOwnerException, CanceledException {
        int ntaxa = splits.getNtax();
        int origNtaxa = ntaxa - 1;

        double h = 0.5 * origNtaxa / Math.tan(Geometry.deg2rad(0.5 * getOptionMaxAngle()));

        double[] split2angle = new double[splits.getNsplits() + 1];
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet part = splits.get(s);
            if (part.get(cycle[1]))
                part = part.getComplement(splits.getNtax());
            int xp = 0; // first position of split part not containing root
            int xq = 0; // last position of split part not containing root
            for (int i = 1; i <= ntaxa; i++) {
                int t = cycle[i];
                if (part.get(t)) {
                    if (xp == 0)
                        xp = i;
                    xq = i;
                }
            }
            if (!getOptionSpecialSwitch()) {
                split2angle[s] = Math.PI + (0.25 * getOptionMaxAngle() / 90.0 * Math.PI * (xp + xq)) / (double) ntaxa;
            } else {
                double dp = 0.5 * origNtaxa - (xp - 1);
                double pAngle = 1.5 * Math.PI - Basic.sign(dp) * Math.atan(Math.abs(dp) / h);
                double dq = 0.5 * origNtaxa - (xq - 1);
                double qAngle = 1.5 * Math.PI - Basic.sign(dq) * Math.atan(Math.abs(dq) / h);
                split2angle[s] = 0.5 * (pAngle + qAngle);

            }
            progress.checkForCancel();
        }

        for (Edge e : graph.edges()) {
            graph.setAngle(e, split2angle[graph.getSplit(e)]);
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
        Node rootNode = graphView.getPhyloGraph().getTaxon2Node(taxa.getNtax());// root is last node
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        for (int i = 1; i <= getBrokenOptionDaylightIterations(); i++) {
            var it = Basic.randomize(graph.nodeIterator(), 77 * i);
            while (it.hasNext()) {
                final Node v = it.next();
                if (graph.getDegree(v) > 1) {
                    assignCoordinatesToNodes(optionUseWeights, taxa.getNtax(), graphView); // need coordinates
                    optimizeDaylightNode(taxa, v, rootNode, graphView);
                    progress.checkForCancel();
                }
            }
        }
    }

    /**
     * optimize the daylight angles of the graph
     *
     * @param v         the node to be optimized
     * @param rootNode  the root of the rooted graph
     * @param graphView
     */
    private void optimizeDaylightNode(Taxa taxa, Node v, Node rootNode,
                                      PhyloGraphView graphView) throws NotOwnerException {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        int numComp = 0;
        EdgeIntegerArray edge2comp = new EdgeIntegerArray(graph);
        double[] comp2MinAngle = new double[taxa.getNtax()];
        double[] comp2MaxAngle = new double[taxa.getNtax()];
        int rootComp = 0; // the component containing the root node

        for (Edge e : v.adjacentEdges()) {
            if (edge2comp.get(e) == 0) {
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
                    return; // visited all nodes, forget it.
                if (visited.contains(rootNode)) // this compoment contains root node
                    rootComp = numComp;

                comp2MinAngle[numComp] = minMaxAngle.getFirst();
                comp2MaxAngle[numComp] = minMaxAngle.getSecond();
            }
        }
        if (numComp > 1) {
            double total = 0;
            for (int c = 1; c <= numComp; c++)
                if (c != rootComp) {
                    total += comp2MaxAngle[c] - comp2MinAngle[c];
                }
            double availableAngle = getOptionMaxAngle() / 90.0 * Math.PI;
            if (total < availableAngle) {
                double daylightGap = (availableAngle - total) / (numComp - 1);
                double[] comp2epsilon = new double[numComp + 1];
                boolean seenRootComp = false;
                for (int c = 1; c <= numComp; c++) {
                    if (c != rootComp) {
                        double alpha = getOptionMaxAngle() / 90.0;
                        for (int i = 1; i < c; i++) {
                            if (i != rootComp)
                                alpha += comp2MaxAngle[i] - comp2MinAngle[i];
                        }
                        alpha += (seenRootComp ? (c - 2) : (c - 1)) * daylightGap;
                        comp2epsilon[c] = alpha - comp2MinAngle[c];
                    } else
                        seenRootComp = true;
                }
                for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                    int c = edge2comp.get(e);
                    graph.setAngle(e, graph.getAngle(e) + comp2epsilon[c]);
                }
            }
        }
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
                                   int numComp, PhyloSplitsGraph graph, PhyloGraphView graphView,
                                   NodeSet visited, double angle,
                                   Pair<Double, Double> minMaxAngle) throws NotOwnerException {

        if (v != root && !visited.contains(v)) {
            visited.add(v);
            for (Edge f = graph.getFirstAdjacentEdge(v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                if (f != e && edge2comp.get(f) == 0) {
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
     * @param rootTaxonId id of the root taxon
     * @param graphView
     */
    private void assignCoordinatesToNodes(boolean useWeights, int rootTaxonId, PhyloGraphView graphView) throws NotOwnerException {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        if (graph.getNumberOfNodes() == 0)
            return;
        Node v = graph.getTaxon2Node(rootTaxonId);
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
            throws NotOwnerException {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        if (!nodesVisited.contains(v)) {
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
    public boolean getBrokenOptionOptimizeDaylight() {
        return optionOptimizeDaylight;
    }

    /**
     * set optimize daylight option
     *
     * @param optimizeDaylight
     */
    public void setBrokenOptionOptimizeDaylight(boolean optimizeDaylight) {
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
    public int getBrokenOptionDaylightIterations() {
        return optionDaylightIterations;
    }

    /**
     * set the number of times to run daylight optimization
     *
     * @param optionDaylightIterations
     */
    public void setBrokenOptionDaylightIterations(int optionDaylightIterations) {
        this.optionDaylightIterations = optionDaylightIterations;
    }

    /**
     * get maximal angle between 0 and 90 degrees
     *
     * @return maximal angle
     */
    public int getOptionMaxAngle() {
        return optionMaxAngle;
    }

    /**
     * set the maximal angle between 0 and 90 degrees
     *
     * @param optionMaxAngle
     */
    public void setOptionMaxAngle(int optionMaxAngle) {
        this.optionMaxAngle = optionMaxAngle;
    }

    public boolean getOptionSpecialSwitch() {
        return optionSpecialSwitch;
    }

    public void setOptionSpecialSwitch(boolean optionSpecialSwitch) {
        this.optionSpecialSwitch = optionSpecialSwitch;
    }

    public PhyloGraphView getGraphView() {
        return graphView;
    }

    /**
     * for now, calling this method will set the outgroup to the named taxon
     * todo: fix this in all classes that compute a rooted network
     *
     * @param taxonName
     * @param doc
     */
    public void setOutGroup(String taxonName, Document doc) {
        Taxa taxa = doc.getTaxa();
        Sets sets = doc.getSets();
        if (sets == null) {
            sets = new Sets();
            doc.setSets(sets);
        }
        TaxaSet outgroup = sets.getTaxSet("Outgroup", taxa);

        if (outgroup == null) {
            outgroup = new TaxaSet();
            outgroup.set(taxa.indexOf(taxonName));
            sets.addTaxSet("Outgroup", taxa.getLabels(outgroup));
        }
    }
}
