/**
 * ConvexHull.java
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
 * Construction of a Network from Splits using the Convex Hull Construction method
 *
 * @version $Id: ConvexHull.java,v 1.49 2009-09-29 12:04:52 huson Exp $
 * @author Markus Franz
 */
/** Construction of a Network from Splits using the Convex Hull Construction method
 * @version $Id: ConvexHull.java,v 1.49 2009-09-29 12:04:52 huson Exp $
 * @author Markus Franz
 */
package splitstree4.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Construction of a Network from Splits using the Convex Hull Construction method
 */
public class ConvexHull implements Splits2Network {

    public final static String DESCRIPTION = "Computes splits graph using convex hull extension algorithm";
    public final static String CONTACT_NAME = "Markus Franz";
    public final static String CONTACT_MAIL = "mfranz@informatik.uni-tuebingen.de";
    public final static String CONTACT_ADRESS = "http://www-ab.informatik.uni-tuebingen.de/software/jsplits/welcome_en.html";

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
     * @throws Exception anything can go wrong...
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
     * @throws Exception
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
     * @throws Exception
     */

    public PhyloGraphView apply(Document doc, Taxa taxa, Splits splits, int[] order, PhyloGraphView graphView, BitSet usedSplits) throws Exception {
        if (graphView != null && usedSplits != null && usedSplits.cardinality() == splits.getNsplits())
            return graphView;

        this.doc = doc;
        doc.notifyTasks("Convex Hull", null);
        doc.notifySetMaximumProgress(splits.getNsplits());    //initialize maximum progress
        doc.notifySetProgress(-1);        //set progress to 0

        final PhyloGraph graph;
        if (graphView == null) {
            graphView = new PhyloGraphView();
            graph = graphView.getPhyloGraph();
            Node startNode = graph.newNode();
            graph.setTaxon2Node(1, startNode);
            //graph.setLabel(startNode, taxa.getLabel(1));
            graph.setNode2Taxa(startNode, 1);

            for (int i = 2; i <= taxa.getNtax(); i++) {
                graph.setTaxon2Node(i, startNode);
                //graph.setLabel(startNode, (graph.getLabel(startNode)+", "+taxa.getLabel(i)));
                graph.setNode2Taxa(startNode, i);
            }
        } else
            graph = graphView.getPhyloGraph();

        //process one split at a time
        doc.notifySetMaximumProgress(order.length);    //initialize maximum progress
        try {
            for (int z = 0; z < order.length; z++) {

                doc.notifySetProgress(z);

                TaxaSet currentSplitPartA = splits.get(order[z]);

                //is 0, if the node is member of convex hull for the "0"-side of the current split,
                //is 1, if the node is member of convex hull for the "1"-side of the current split,
                //is 2, if the node is member of both hulls
                NodeIntegerArray hulls = new NodeIntegerArray(graph);

                //here all found "critical" nodes are stored
                final ArrayList intersectionNodes = new ArrayList();

                final BitSet splits1 = new BitSet();
                final BitSet splits0 = new BitSet();

                //find splits, where taxa of side "0" of current split are divided
                for (int i = 1; i <= splits.getNsplits(); i++) {
                    if (!usedSplits.get(i)) continue;    //only splits already used must be regarded

                    if (splits.getSplitsSet().intersect2(order[z], false, i, true).cardinality() != 0 &&
                            splits.getSplitsSet().intersect2(order[z], false, i, false).cardinality() != 0)
                        splits0.set(i);
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

                EdgeIntegerArray visited = new EdgeIntegerArray(graph, 0);

                convexHullPath(graph, start0, visited, hulls, splits0, intersectionNodes, 0);

                //construct the remainder of convex hull for split-side "1" by traversing all allowed (and reachable) edges (i.e. all edges in splits0)

                visited = new EdgeIntegerArray(graph, 0);

                convexHullPath(graph, start1, visited, hulls, splits1, intersectionNodes, 1);

                //first duplicate the intersection nodes, set an edge between each node and its duplicate and label new edges and nodes
                for (Object intersectionNode1 : intersectionNodes) {

                    Node v = (Node) intersectionNode1;
                    Node v1 = graph.newNode();

                    Edge e = graph.newEdge(v1, v);
                    graph.setSplit(e, order[z]);
                    graph.setWeight(e, splits.getWeight(order[z]));
                    graph.setLabel(e, "" + order[z]);

                    List aTaxa = graph.getNode2Taxa(v);

                    graph.clearNode2Taxa(v);

                    for (Object anATaxa : aTaxa) {

                        int taxon = (Integer) anATaxa;
                        if (currentSplitPartA.get(taxon)) {
                            graph.setTaxon2Node(taxon, v1);
                            graph.setNode2Taxa(v1, taxon);
                        } else {
                            graph.setNode2Taxa(v, taxon);
                        }
                    }

                    //graph.setLabel(v, vlab);
                    //graph.setLabel(v1, v1lab);
                }

                //connect edges accordingly
                for (Object intersectionNode : intersectionNodes) {

                    doc.getProgressListener().checkForCancel();

                    Node v = (Node) intersectionNode;

                    //find duplicated node of v (and their edge)
                    Node v1 = null;
                    Edge toV1 = null;

                    for (Iterator en = graph.getAdjacentEdges(v); en.hasNext(); ) {
                        toV1 = (Edge) en.next();
                        if (graph.getSplit(toV1) == order[z]) {
                            v1 = graph.getOpposite(v, toV1);
                            break;
                        }
                    }

                    //visit all edges of v and move or add edges
                    for (Iterator en = graph.getAdjacentEdges(v); en.hasNext(); ) {
                        doc.getProgressListener().checkForCancel();

                        Edge consider = (Edge) en.next();

                        if (consider == toV1) continue;

                        Node w = graph.getOpposite(v, consider);

                        if (hulls.getValue(w) == 0) {
                        } else if (hulls.getValue(w) == 1) {        //node belongs to other side
                            Edge considerDup = graph.newEdge(v1, w);
                            graph.setLabel(considerDup, "" + graph.getSplit(consider));
                            graph.setSplit(considerDup, graph.getSplit(consider));
                            graph.setWeight(considerDup, graph.getWeight(consider));
                            graph.setAngle(considerDup, graph.getAngle(consider));
                            graph.deleteEdge(consider);
                        } else if (hulls.getValue(w) == 2) {                                    //node is in intersection
                            Node w1 = null;
                            Edge toW1;

                            for (Iterator iter = graph.getAdjacentEdges(w); iter.hasNext(); ) {
                                toW1 = (Edge) iter.next();
                                doc.getProgressListener().checkForCancel();

                                if (graph.getSplit(toW1) == order[z]) {
                                    w1 = graph.getOpposite(w, toW1);
                                    break;
                                }
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
            }
        } catch (CanceledException e) {
            doc.getProgressListener().setUserCancelled(false);
        }


        doc.notifySetProgress(-1);
        Iterator it = graph.nodeIterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            graph.setLabel(n, null);
            List list = graph.getNode2Taxa(n);
            if (list.size() != 0) {
                String label = taxa.getLabel((Integer) list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    int taxon = (Integer) list.get(i);
                    label += (", " + taxa.getLabel(taxon));
                }
                graph.setLabel(n, label);
            }
        }

        int[] cyclicOrdering = splits.getCycle();

        for (int i = 1; i < cyclicOrdering.length; i++) {
            graph.setTaxon2Cycle(cyclicOrdering[i], i);
        }

        NodeArray coords = graph.embed(cyclicOrdering, getOptionWeights(), true);

        int maxNumberOfTaxaOnNode = 0;
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            graphView.setLocation(v, (Point2D) coords.get(v));
            if (graph.getNode2Taxa(v) != null && graph.getNode2Taxa(v).size() > maxNumberOfTaxaOnNode)
                maxNumberOfTaxaOnNode = graph.getNode2Taxa(v).size();
        }

        if (getOptionScaleNodesMaxSize() > 1 && maxNumberOfTaxaOnNode > 1) {
            for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
                if (graph.getNode2Taxa(v) != null && graph.getNode2Taxa(v).size() > 0) {
                    int size = Math.max(graphView.getWidth(v), (getOptionScaleNodesMaxSize() * graph.getNode2Taxa(v).size()) / maxNumberOfTaxaOnNode);
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


    private void convexHullPath(PhyloGraph g, Node start, EdgeIntegerArray visited, NodeIntegerArray hulls, BitSet allowedSplits, ArrayList intersectionNodes, int side) throws Exception {

        Stack todo = new Stack();
        todo.push(start);

        while (!todo.empty()) {

            Node n = (Node) todo.pop();

            for (Iterator en = g.getAdjacentEdges(n); en.hasNext(); ) {

                Edge f = (Edge) en.next();
                Node m = g.getOpposite(n, f);

                if (visited.getValue(f) == 0 && allowedSplits.get(g.getSplit(f))) {
                    //if(hulls.getValue(m)==side) continue;
                    visited.set(f, 1);

                    if (hulls.get(m) == null) {
                        hulls.set(m, side);
                        todo.push(m);
                    } else if (hulls.getValue(m) == Math.abs(side - 1)) {
                        hulls.set(m, 2);
                        intersectionNodes.add(m);
                        todo.push(m);
                    }
                } else
                    visited.set(f, 1);

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
     *
     * @return
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
        SortedSet pairs = new TreeSet();
        for (int i = 1; i <= splits.getNsplits(); i++) {
            if (!usedSplits.get(i)) {
                Integer pair = 10000 * splits.get(i).getSplitSize(taxa.getNtax()) + i;
                pairs.add(pair);
            }
        }

        int[] order = new int[pairs.size()];
        Iterator it = pairs.iterator();
        int i = 0;
        while (it.hasNext()) {
            int pair = (Integer) it.next();
            int size = pair / 10000;
            int id = pair - size * 10000;
            // System.err.println("pair "+id+" size "+size);
            order[i++] = id;
        }
        return order;
    }
}//EOF
