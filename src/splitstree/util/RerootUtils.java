/**
 * RerootUtils.java 
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
package splitstree.util;

import jloda.graph.Edge;
import jloda.graph.EdgeIntegerArray;
import jloda.graph.Node;
import jloda.graph.NodeIntegerArray;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Alert;

import java.util.Set;

/**
 * rerooting methods
 * Daniel Huson and David Bryant, 4.2008
 */
public class RerootUtils {
    /**
     * reroot a tree by outgroup. Find the node or edge middle point so that tree is optimally rooted for
     * the given outgroup  labels
     *
     * @param viewer
     * @param outgroupLabels
     * @return root node
     */
    public static Node rerootByOutgroup(PhyloGraphView viewer, Set<String> outgroupLabels) {
        PhyloGraph tree = (PhyloGraph) viewer.getGraph();

        if (tree.getSpecialEdges().size() > 0) {
            new Alert("Reroot by outgroup: not implemented for network");
            return null;
        }

        int totalOutgroup = 0;
        int totalNodes = tree.getNumberOfNodes();

        // compute number of outgroup taxa for each node
        NodeIntegerArray node2NumberOutgroup = new NodeIntegerArray(tree);
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (tree.getLabel(v) != null && outgroupLabels.contains(tree.getLabel(v))) {
                node2NumberOutgroup.set(v, node2NumberOutgroup.getValue(v) + 1);
                totalOutgroup++;
            }
        }

        System.err.println("total outgroup " + totalOutgroup + " total nodes " + totalNodes);

        EdgeIntegerArray edge2OutgroupBelow = new EdgeIntegerArray(tree); // how many outgroup taxa below this edge?
        EdgeIntegerArray edge2NodesBelow = new EdgeIntegerArray(tree);  // how many nodes below this edge?
        NodeIntegerArray node2OutgroupBelow = new NodeIntegerArray(tree); // how many outgroup taxa below this multifurcation?
        NodeIntegerArray node2NodesBelow = new NodeIntegerArray(tree);     // how many nodes below this multifurcation (including this?)

        rerootByOutgroupRec(tree.getFirstNode(), null, node2NumberOutgroup, edge2OutgroupBelow, edge2NodesBelow, node2OutgroupBelow, node2NodesBelow, totalNodes, totalOutgroup);

        // find best edge for rooting

        Edge bestEdge = null;
        int outgroupBelowBestEdge = 0;
        int nodesBelowBestEdge = 0;

        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            int outgroupBelowE = edge2OutgroupBelow.getValue(e);
            int nodesBelowE = edge2NodesBelow.getValue(e);
            if (outgroupBelowE < 0.5 * totalOutgroup) {
                outgroupBelowE = totalOutgroup - outgroupBelowE;
                nodesBelowE = totalNodes - nodesBelowE;
            }
            if (bestEdge == null || outgroupBelowE > outgroupBelowBestEdge || (outgroupBelowE == outgroupBelowBestEdge && nodesBelowE < nodesBelowBestEdge)) {
                bestEdge = e;
                outgroupBelowBestEdge = outgroupBelowE;
                nodesBelowBestEdge = nodesBelowE;
            }
            //tree.setLabel(e,""+outgroupBelowE+" "+nodesBelowE);
        }

        // try to find better node for rooting:

        Node bestNode = null;
        int outgroupBelowBestNode = outgroupBelowBestEdge;
        int nodesBelowBestNode = nodesBelowBestEdge;

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            int outgroupBelowV = node2OutgroupBelow.getValue(v);
            int nodesBelowV = node2NodesBelow.getValue(v);
            if (outgroupBelowV > 0 && (outgroupBelowV > outgroupBelowBestNode || (outgroupBelowV == outgroupBelowBestNode && nodesBelowV < nodesBelowBestNode))) {
                bestNode = v;
                outgroupBelowBestNode = outgroupBelowV;
                nodesBelowBestNode = nodesBelowV;
                // System.err.println("node score: "+outgroupBelowV+" "+nodesBelowV);
            }
        }
        if (bestNode != null) {
            return bestNode;
        } else if (bestEdge != null) {
            Node v = bestEdge.getSource();
            Node w = bestEdge.getTarget();
            Node root = tree.newNode();
            Edge ea = tree.newEdge(root, v);
            Edge eb = tree.newEdge(root, w);
            tree.setSplit(ea, tree.getSplit(bestEdge));
            tree.setSplit(eb, tree.getSplit(bestEdge));

            double weight = tree.getWeight(bestEdge);
            double a = tree.computeAverageDistanceToALeaf(ea.getOpposite(root));
            double b = tree.computeAverageDistanceToALeaf(eb.getOpposite(root));
            double na = 0.5 * (b - a + weight);
            if (na >= weight)
                na = 0.95 * weight;
            else if (na <= 0)
                na = 0.05 * weight;
            double nb = weight - na;
            tree.setWeight(ea, na);
            tree.setWeight(eb, nb);

            tree.deleteEdge(bestEdge);
            return root;
        } else
            return null;
    }

    /**
     * recursively determine the best place to root the tree for the given outgroup
     *
     * @param v
     * @param e
     * @param node2NumberOutgroup
     * @param edge2OutgroupBelow
     * @param edge2NodesBelow
     * @param node2OutgroupBelow
     * @param node2NodesBelow
     * @param totalNodes
     * @param totalOutgroup
     */
    private static void rerootByOutgroupRec(Node v, Edge e, NodeIntegerArray node2NumberOutgroup, EdgeIntegerArray edge2OutgroupBelow,
                                            EdgeIntegerArray edge2NodesBelow, NodeIntegerArray node2OutgroupBelow, NodeIntegerArray node2NodesBelow, int totalNodes, int totalOutgroup) {
        int outgroupBelowE = node2NumberOutgroup.getValue(v);
        int nodesBelowE = 1; // including v

        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            if (f != e) {
                rerootByOutgroupRec(f.getOpposite(v), f, node2NumberOutgroup, edge2OutgroupBelow, edge2NodesBelow, node2OutgroupBelow, node2NodesBelow, totalNodes, totalOutgroup);
                outgroupBelowE += edge2OutgroupBelow.getValue(f);
                nodesBelowE += edge2NodesBelow.getValue(f);
            }
        }
        if (e != null) {
            edge2NodesBelow.set(e, nodesBelowE);
            edge2OutgroupBelow.set(e, outgroupBelowE);
        }

        // if v is a multifurcation then we may need to use it as root
        if (v.getDegree() > 3) // multifurcation
        {
            int outgroupBelowV = outgroupBelowE + node2NumberOutgroup.getValue(v);

            if (outgroupBelowV == totalOutgroup) // all outgroup taxa lie below here
            {
                // count nodes below in straight-forward way
                node2OutgroupBelow.set(v, outgroupBelowV);

                int nodesBelowV = 1;
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    if (edge2OutgroupBelow.getValue(f) > 0)
                        nodesBelowV += edge2NodesBelow.getValue(f);
                }
                node2NodesBelow.set(v, nodesBelowV);
            } else // outgroupBelowE<totalOutgroup, i.e. some outgroup nodes lie above e
            {
                // count nodes below in parts not containing outgroup taxa and then subtract appropriately

                boolean keep = false;
                int nodesBelowV = 0;
                for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                    {
                        if (f != e) {
                            if (edge2OutgroupBelow.getValue(f) > 0)
                                keep = true;   // need to have at least one node below that contains outgroup taxa
                            else
                                nodesBelowV += edge2NodesBelow.getValue(f);
                        }
                    }
                    if (keep) {
                        node2OutgroupBelow.set(v, totalOutgroup);
                        node2NodesBelow.set(v, totalNodes - nodesBelowV);
                    }
                }
            }
        }
    }
}
