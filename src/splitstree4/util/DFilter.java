/*
 * DFilter.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util;

import jloda.graph.*;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import splitstree4.core.Document;
import splitstree4.nexus.Splits;

import java.util.BitSet;


/**
 * Given a set of splits, removes highly incompatible splits to destroy all high-dimensional boxes
 *
 * @author huson
 * Date: 14-May-2004
 */
public class DFilter {
    Document doc = null;

    DFilter(Document doc) {
        this.doc = doc;
    }

    /**
     * destroy all d-dimensional boxes in splits graph
     *
     * @param splits
     * @param maxDimension maximal dimension d
     * @return number of splits removed
     */
    static public int applyFilter(Document doc, Splits splits, int maxDimension) throws CanceledException {
        DFilter dFilter = new DFilter(doc);

        return dFilter.apply(maxDimension, splits);
    }

    /**
     * destroy all d-dimensional boxes in splits graph
     *
     * @param maxDimension maximal dimension d
     * @param splits
     * @return number of splits deleted
     */
    public int apply(int maxDimension, Splits splits) {
        final int COMPUTE_DSUBGRAPH_MAXDIMENSION = 5;
        doc.notifyTasks("Dimension filter", "maxDimension=" + maxDimension);
        System.err.println("\nRunning Dimension-Filter for d=" + maxDimension);
        BitSet toDelete = new BitSet(); // set of splits to be removed from split set

        try {
            // build initial incompatibility graph:
            Graph graph = buildIncompatibilityGraph(splits);

            //System.err.println("Init: "+graph);
            int origNumberOfNodes = graph.getNumberOfNodes();
            doc.notifySetMaximumProgress(origNumberOfNodes);    //initialize maximum progress
            doc.notifySetProgress(0);

            if (maxDimension <= COMPUTE_DSUBGRAPH_MAXDIMENSION) {
                System.err.println("(Small D: using D-subgraph)");
                computeDSubgraph(graph, maxDimension + 1);
            } else {
                System.err.println("(Large D: using maxDegree heuristic)");
                relaxGraph(graph, maxDimension - 1);
            }
            //System.err.println("relaxed: "+graph);

            while (graph.getNumberOfNodes() > 0) {
                Node worstNode = getWorstNode(graph);
                int s = ((Pair) graph.getInfo(worstNode)).getFirstInt();
                toDelete.set(s);
                graph.deleteNode(worstNode);
                //System.err.println("deleted: "+graph);

                if (maxDimension <= COMPUTE_DSUBGRAPH_MAXDIMENSION)
                    computeDSubgraph(graph, maxDimension + 1);
                else
                    relaxGraph(graph, maxDimension - 1);
                //System.err.println("relaxed: "+graph);
                doc.notifySetProgress(origNumberOfNodes - graph.getNumberOfNodes());
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        removeSplits(splits, toDelete);
        System.err.println("Splits removed: " + toDelete.cardinality());
        return toDelete.cardinality();
    }

    /**
     * build the incompatibility graph
     *
     * @param splits
     * @return incompatibility graph
     */
    Graph buildIncompatibilityGraph(Splits splits) {
        Node[] split2node = new Node[splits.getNsplits() + 1];
        Graph graph = new Graph();

        try {
            for (int s = 1; s <= splits.getNsplits(); s++) {
                Pair pair = new Pair(s, (int) (10000 * splits.getWeight(s)));
                split2node[s] = graph.newNode(pair);
            }
            for (int s = 1; s <= splits.getNsplits(); s++) {

                for (int t = s + 1; t <= splits.getNsplits(); t++)
                    if (!SplitsUtilities.areCompatible(splits.getNtax(), splits.get(s),
                            splits.get(t))) {
                        graph.newEdge(split2node[s], split2node[t]);
                    }
            }

        } catch (NotOwnerException ex) {
            Basic.caught(ex);
        }
        return graph;
    }

    /**
     * computes the subgraph in which every node is contained in a d-clique
     *
     * @param graph
     * @param d     clique size
     */
    private void computeDSubgraph(Graph graph, int d) throws NotOwnerException, CanceledException {
        //System.err.print("Compute D-subgraph: ");
        NodeSet keep = new NodeSet(graph);
        NodeSet discard = new NodeSet(graph);
        NodeSet clique = new NodeSet(graph);
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            if (!keep.contains(v)) {
                clique.clear();
                clique.add(v);
                if (findClique(graph, v, graph.getFirstAdjacentEdge(v), 1, d, clique, discard))
                    keep.addAll(clique);
                else
                    discard.add(v);
            }
            doc.getProgressListener().checkForCancel();
        }

        // remove all nodes not contained in a d-clique
        for (Node v : discard) {
            graph.deleteNode(v);
        }
        //System.err.println(" "+graph.getNumberOfNodes());
    }

    /**
     * recursively determine whether v is contained in a d-clique.
     *
     * @param graph
     * @param v
     * @param e
     * @param i
     * @param d
     * @param clique
     * @param discard
     * @return true, if v contained in a d-clique
     */
    private boolean findClique(Graph graph, Node v, Edge e, int i, int d, NodeSet clique, NodeSet discard) {
        if (i == d)
            return true;  // found clique, retreat
        else {
            while (e != null) {
                Node w = graph.getOpposite(v, e);
                e = graph.getNextAdjacentEdge(e, v);

                if (isConnectedTo(graph, w, clique) && !discard.contains(w)) {
                    clique.add(w);
                    if (findClique(graph, v, e, i + 1, d, clique, discard))
                        return true;
                    clique.remove(w);
                }
            }
            return false; // didn't work out, try different combination
        }
    }

    /**
     * determines whether node w is connected to all nodes in U
     *
     * @param graph
     * @param w
     * @param U
     * @return true, if w is connected to all nodes in U
     * @throws NotOwnerException
     */
    private boolean isConnectedTo(Graph graph, Node w, NodeSet U) throws NotOwnerException {
        int count = 0;
        for (Edge e = graph.getFirstAdjacentEdge(w); e != null; e = graph.getNextAdjacentEdge(e, w)) {
            Node u = graph.getOpposite(w, e);
            if (U.contains(u)) {
                count++;
                if (count == U.size())
                    return true;
            }
        }
        return false;
    }

    /**
     * Modify graph to become the maximal induced graph in which all nodes have degree >maxDegree
     * If maxDegree==1, then we additionally require that all remaining nodes are contained in a triangle
     *
     * @param graph
     * @param maxDegree
     */
    private void relaxGraph(Graph graph, int maxDegree) throws NotOwnerException, CanceledException {
        System.err.print("Relax graph: ");

        int maxDegreeHeuristicThreshold = 6; // use heuristic for max degrees above this threshold
        NodeSet active = new NodeSet(graph);
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            if (graph.getDegree(v) < maxDegree
                    || (maxDegree <= maxDegreeHeuristicThreshold && hasDegreeDButNotInClique(maxDegree + 1, graph, v)))
                active.add(v);
        }

        while (!active.isEmpty()) {
            Node v = active.getFirstElement();
            if (graph.getDegree(v) < maxDegree || (maxDegree <= maxDegreeHeuristicThreshold && hasDegreeDButNotInClique(maxDegree + 1, graph, v))) {
                for (Node w : v.adjacentNodes()) {
                    active.add(w);
                }
                active.remove(v);
                graph.deleteNode(v);
            } else
                active.remove(v);
            doc.getProgressListener().checkForCancel();
        }
        System.err.println("" + graph.getNumberOfNodes());
    }


    /**
     * gets the node will the lowest compatability score
     *
     * @param graph
     * @return worst node
     * @throws NotOwnerException
     */
    private Node getWorstNode(Graph graph) throws NotOwnerException {
        float worstCompatibility = 0;
        Node worstNode = null;
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            float compatibility = getCompatibilityScore(graph, v);
            if (worstNode == null || compatibility < worstCompatibility) {
                worstNode = v;
                worstCompatibility = compatibility;
            }
        }
        return worstNode;
    }

    /**
     * gets the compatibility score of a node.
     * This is the weight oif the splits minus the weight of all contradicting splits
     *
     * @param graph
     * @param v
     * @return compatibility score
     * @throws NotOwnerException
     */
    private int getCompatibilityScore(Graph graph, Node v) throws NotOwnerException {
        int score = ((Pair) graph.getInfo(v)).getSecondInt();
        for (Node w : v.adjacentNodes()) {
            score -= ((Pair) graph.getInfo(w)).getSecondInt();
        }
        return score;
    }

    /**
     * determines whether the node v has degree==d but  is not contained in a clique of size d+1
     *
     * @param d* @param graph
     * @param v
     * @return false, if the node v has degree!=d or is contained in a d+1 clique
     * @throws NotOwnerException
     */
    private boolean hasDegreeDButNotInClique(int d, Graph graph, Node v) throws NotOwnerException {
        if (graph.getDegree(v) != d)
            return false;
        for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {
            Node a = graph.getOpposite(v, e);
            for (Edge f = graph.getNextAdjacentEdge(e, v); f != null; f = graph.getNextAdjacentEdge(f, v)) {
                Node b = graph.getOpposite(v, f);
                if (!a.isAdjacent(b))
                    return true;
            }
        }
        return false;
    }

    /**
     * return the filtered set of splits
     *
     * @param splits
     * @param toDelete
     * @return filtered splits
     */
    private Splits removeSplits(Splits splits, BitSet toDelete) {
        int count = 0; // internal numbering changes as we delete stuff
        for (int s = toDelete.nextSetBit(1); s > 0; s = toDelete.nextSetBit(s + 1)) {
            splits.getSplitsSet().remove(s - count);
            count++;
        }
        return splits;
    }
}
