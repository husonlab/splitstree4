/*
 * ConvexHull.java
 * Copyright (C) 2020 Daniel H. Huson and David J. Bryant
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
 * Construction of a Network from Splits using the Convex Hull Construction method
 *
 * @version $Id: ConvexHull.java,v 1.49 2009-09-29 12:04:52 huson Exp $
 * @author Markus Franz
 */
package splitstree4.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloSplitsGraphUtils;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.APoint2D;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.IteratorUtils;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.*;

/**
 * Construction of a Network from Splits using the Convex Hull Construction method
 */
public class ConvexHull implements Splits2Network {
    public final static String DESCRIPTION = "Computes splits graph using convex hull extension algorithm";

    private boolean optionWeights = true;
    private int optionScaleNodesMaxSize = 5;


    Document doc;

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * is convex hull algorithm applicable?
     *
     * @param taxa taxa
     * @param s    splits
     * @return true, if applicable
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits s) {
        return taxa != null && s != null; // 2 do
    }


    /**
     * applies the convex hull construction to obtain a splits graph
     *
     * @param taxa   taxa
     * @param splits
     * @return the network
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        /* no used splits: */
        BitSet usedSplits = new BitSet();

        int[] order = getOrderToProcessSplitsIn(taxa, splits, usedSplits);
        PhyloGraphView graphView = apply(doc, taxa, splits, order, null, usedSplits);
        return new Network(taxa, graphView);
    }


    /**
     * Applies the convex hull algorithm.
     * Given a non-zero phylograph, it returns that graph, otherwise a new one
     *
     * @param taxa       taxa
     * @param splits
     * @param graphView  PhyloGraphView or null
     * @param usedSplits which splits have already been used?
     * @return the modified or new graph
     */

    public PhyloGraphView apply(Document doc, Taxa taxa, Splits splits, BitSet usedSplits, PhyloGraphView graphView) throws Exception {
        int[] order = getOrderToProcessSplitsIn(taxa, splits, usedSplits);

        return apply(doc, taxa, splits, order, graphView, usedSplits);
    }

    /**
     * Applies the convex hull algorithm.
     * Given a non-zero phylograph, it returns that graph, otherwise a new one
     *
     * @param taxa       taxa
     * @param splits     splits
     * @param order      order in which splits are to be processed
     * @param graphView  PhyloGraphView or null
     * @param usedSplits which splits have already been used?
     * @return the modified or new graph
     */

    public PhyloGraphView apply(Document doc, Taxa taxa, Splits splits, int[] order, PhyloGraphView graphView, BitSet usedSplits) throws Exception {
        if (graphView != null && usedSplits.cardinality() == splits.getNsplits())
            return graphView;

        this.doc = doc;
        doc.notifyTasks("Convex Hull", null);
        doc.notifySetMaximumProgress(splits.getNsplits());    //initialize maximum progress
        doc.notifySetProgress(-1);        //set progress to 0

        final PhyloSplitsGraph graph;
        if (graphView == null) {
            graphView = new PhyloGraphView();
            graph = graphView.getPhyloGraph();
            Node startNode = graph.newNode();

            for (int i = 1; i <= taxa.getNtax(); i++) {
                graph.addTaxon(startNode, i);
                //graph.setLabel(startNode, (graph.getLabel(startNode)+", "+taxa.getLabel(i)));
            }
        } else
            graph = graphView.getPhyloGraph();

        //process one split at a time
        doc.notifySetMaximumProgress(order.length);    //initialize maximum progress
        try {
            final NodeIntArray hulls = new NodeIntArray(graph);
            //is 0, if the node is member of convex hull for the "0"-side of the current split,
            //is 1, if the node is member of convex hull for the "1"-side of the current split,
            //is 2, if the node is member of both hulls

            final ArrayList<Node> intersectionNodes = new ArrayList<>();
            //here all found "critical" nodes are stored

            final BitSet splits0 = new BitSet();
            final BitSet splits1 = new BitSet();

            // process all splits:
            for (int z = 0; z < order.length; z++) {
                doc.notifySetProgress(z);

                hulls.clear();
                intersectionNodes.clear();
                splits0.clear();
                splits1.clear();

                final TaxaSet currentSplitPartA = splits.get(order[z]);
                System.err.println("Current split: (" + currentSplitPartA.cardinality() + " of " + taxa.getNtax() + "): " + Basic.toString(currentSplitPartA.getBits()));

                //find splits, where taxa of side "0" of current split are divided
                for (int i = 1; i <= splits.getNsplits(); i++) {
                    if (!usedSplits.get(i)) continue;    //only splits already used must be regarded

                    if (splits.getSplitsSet().intersect2(order[z], false, i, true).cardinality() != 0 &&
                            splits.getSplitsSet().intersect2(order[z], false, i, false).cardinality() != 0) {
                        splits0.set(i);
                    }
                    doc.getProgressListener().checkForCancel();
                }

                //find splits, where taxa of side "1" of current split are divided
                for (int i = 1; i <= splits.getNsplits(); i++) {
                    doc.getProgressListener().checkForCancel();

                    if (!usedSplits.get(i)) continue;    //only splits already used must be regarded

                    if (splits.getSplitsSet().intersect2(order[z], true, i, true).cardinality() != 0 &&
                            splits.getSplitsSet().intersect2(order[z], true, i, false).cardinality() != 0)
                        splits1.set(i);
                }

                //find startNodes

                Node start0 = null;
                Node start1 = null;

                for (int i = 1; i <= taxa.getNtax(); i++) {
                    if (!currentSplitPartA.get(i)) {
                        start0 = graph.getTaxon2Node(i);
                    } else {
                        start1 = graph.getTaxon2Node(i);
                    }
                    if (start0 != null && start1 != null) break;
                }

                hulls.set(start0, 0);

                if (start0 == start1) {
                    hulls.set(start1, 2);
                    intersectionNodes.add(start1);
                } else
                    hulls.set(start1, 1);

                //construct the remainder of convex hull for split-side "0" by traversing all allowed (and reachable) edges (i.e. all edges in splits0)

                convexHullPath(graph, start0, hulls, splits0, intersectionNodes, 0);

                //construct the remainder of convex hull for split-side "1" by traversing all allowed (and reachable) edges (i.e. all edges in splits1)

                convexHullPath(graph, start1, hulls, splits1, intersectionNodes, 1);

                System.err.println("Intersection: " + intersectionNodes.size());

                //first duplicate the intersection nodes, set an edge between each node and its duplicate and label new edges and nodes
                for (Node v : intersectionNodes) {
                    Node v1 = graph.newNode();

                    Edge e = graph.newEdge(v1, v);
                    graph.setSplit(e, order[z]);
                    graph.setWeight(e, splits.getWeight(order[z]));
                    graph.setLabel(e, "" + order[z]);

                    final Set<Integer> set = IteratorUtils.asSet(graph.getTaxa(v)); // make a copy!
                    graph.clearTaxa(v);

                    for (Integer t : set) {
                        if (currentSplitPartA.get(t)) {
                            graph.addTaxon(v1, t);
                        } else {
                            graph.addTaxon(v, t);
                        }
                    }
                }

                //connect edges accordingly
                for (Node v : intersectionNodes) {
                    //find duplicated node of v (and their edge)
                    Node v1 = null;
                    Edge toV1 = null;

                    for (Edge en : v.adjacentEdges()) {
                        toV1 = en;
                        if (graph.getSplit(toV1) == order[z]) {
                            v1 = toV1.getOpposite(v);
                            break;
                        }
                    }

                    //visit all edges of v and move or add edges
                    for (Edge consider : v.adjacentEdges()) {
                        doc.getProgressListener().checkForCancel();

                        if (consider == toV1) continue;

                        Node w = consider.getOpposite(v);

                        if (hulls.getInt(w) == 0) {
                        } else if (hulls.getInt(w) == 1) {        //node belongs to other side
                            Edge considerDup = graph.newEdge(v1, w);
                            graph.setLabel(considerDup, "" + graph.getSplit(consider));
                            graph.setSplit(considerDup, graph.getSplit(consider));
                            graph.setWeight(considerDup, graph.getWeight(consider));
                            graph.setAngle(considerDup, graph.getAngle(consider));
                            graph.deleteEdge(consider);
                        } else if (hulls.getInt(w) == 2) {                                    //node is in intersection
                            Node w1 = null;

                            for (Edge toW1 : w.adjacentEdges()) {
                                if (graph.getSplit(toW1) == order[z]) {
                                    w1 = graph.getOpposite(w, toW1);
                                    break;
                                }
                                doc.getProgressListener().checkForCancel();
                            }

                            if (graph.getCommonEdge(v1, w1) == null) {
                                Edge considerDup = graph.newEdge(v1, w1);
                                graph.setLabel(considerDup, "" + graph.getSplit(consider));

                                graph.setWeight(considerDup, graph.getWeight(consider));
                                graph.setSplit(considerDup, graph.getSplit(consider));
                            }
                        }
                    }
                }
                //add split to usedSplits
                usedSplits.set(order[z], true);
                doc.getProgressListener().checkForCancel();
            }
        } catch (CanceledException e) {
            doc.getProgressListener().setUserCancelled(false);
        }


        doc.notifySetProgress(-1);
        Iterator it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            graph.setLabel(n, null);

            if (graph.hasTaxa(n)) {
                String label = taxa.getLabel(graph.getTaxa(n).iterator().next());

                for (Integer t : graph.getTaxa(n)) {
                    label += (", " + taxa.getLabel(t));
                }
                graph.setLabel(n, label);
            }
        }

        int[] cyclicOrdering = splits.getCycle();

        for (int i = 1; i < cyclicOrdering.length; i++) {
            graph.setTaxon2Cycle(cyclicOrdering[i], i);
        }

        NodeArray<APoint2D<?>> coords = PhyloSplitsGraphUtils.embed(graph, cyclicOrdering, getOptionWeights(), true);

        int maxNumberOfTaxaOnNode = 0;
        for (Node v : graph.nodes()) {
            graphView.setLocation(v, coords.get(v).getX(), coords.get(v).getY());
            maxNumberOfTaxaOnNode = Math.max(graph.getNumberOfTaxa(v), maxNumberOfTaxaOnNode);
        }

        if (getOptionScaleNodesMaxSize() > 1 && maxNumberOfTaxaOnNode > 1) {
            for (Node v : graph.nodes()) {
                if (graph.getNumberOfTaxa(v) > 0) {
                    int size = Math.max(graphView.getWidth(v), (getOptionScaleNodesMaxSize() * graph.getNumberOfTaxa(v)) / maxNumberOfTaxaOnNode);
                    graphView.setWidth(v, size);
                    graphView.setHeight(v, size);
                }
            }
        }

        BitSet seen = new BitSet();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            int s = graph.getSplit(e);
            if (s > 0 && !seen.get(s)) {
                seen.set(s);
                graph.setLabel(e, splits.getLabel(s));
            } else
                graph.setLabel(e, null);
        }

        return graphView;

    }//end apply


    private void convexHullPath(PhyloSplitsGraph graph, Node start, NodeIntArray hulls, BitSet allowedSplits, ArrayList<Node> intersectionNodes, int side) throws Exception {
        final EdgeSet seen = new EdgeSet(graph);
        final Stack<Node> todo = new Stack<>();
        todo.push(start);

        while (!todo.empty()) {
            final Node n = todo.pop();

            for (final Edge f : n.adjacentEdges()) {
                final Node m = f.getOpposite(n);

                if (false)
                    System.err.println("allowed: " + Basic.toString(allowedSplits));
                if (false)
                    System.err.println("got: " + graph.getSplit(f));

                if (!seen.contains(f) && allowedSplits.get(graph.getSplit(f))) {
                    //if(hulls.get(m)==side) continue;
                    seen.add(f);

                    if (false)
                        System.err.println("hulls(" + m + "): " + hulls.get(m));

                    if (hulls.get(m) == null) {
                        hulls.set(m, side);
                        todo.push(m);
                    } else if (hulls.get(m) == Math.abs(side - 1)) {
                        hulls.set(m, 2);
                        intersectionNodes.add(m);
                        todo.push(m);
                    }
                } else
                    seen.add(f);

                doc.getProgressListener().checkForCancel();
            }
        }
    }

    /**
     * use weights in embedding?
     *
     * @return use weights
     */
    public boolean getOptionWeights() {
        return optionWeights;
    }

    /**
     * set use weights in embedding?
     *
     * @param weights
     */
    public void setOptionWeights(boolean weights) {
        this.optionWeights = weights;
    }

    /**
     * scale size of nodes by number of taxa?
     */
    public int getOptionScaleNodesMaxSize() {
        return optionScaleNodesMaxSize;
    }

    public void setOptionScaleNodesMaxSize(int optionScaleNodesMaxSize) {
        this.optionScaleNodesMaxSize = optionScaleNodesMaxSize;
    }

    /**
     * computes a good order in which to process the splits.
     * Currently orders splits by increasing size
     *
     * @param taxa
     * @param splits
     * @param usedSplits
     * @return order
     */
    private int[] getOrderToProcessSplitsIn(Taxa taxa, Splits splits, BitSet usedSplits) {
        SortedSet<Integer> values = new TreeSet<>();
        for (int i = 1; i <= splits.getNsplits(); i++) {
            if (!usedSplits.get(i)) {
                values.add(10000 * splits.get(i).getSplitSize(taxa.getNtax()) + i);
            }
        }

        int[] order = new int[values.size()];
        Iterator<Integer> it = values.iterator();
        int i = 0;
        while (it.hasNext()) {
            int value = it.next();
            int size = value / 10000;
            int id = value - size * 10000;
            // System.err.println("pair "+id+" size "+size);
            order[i++] = id;
        }
        return order;
    }
}//EOF
