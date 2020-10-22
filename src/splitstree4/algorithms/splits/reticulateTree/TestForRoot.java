/**
 * TestForRoot.java
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
package splitstree4.algorithms.splits.reticulateTree;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.HashSet;

/**
 * The class gives a implementation to check if a reticulation scenario is valid given a root, The metod uses a directed graph.
 * the root makes the treeGraph a directed graph, such that we have relations between the reticulation nodes in such a way, that
 * we know if the reticulation event that generated reticulation node r1 was before the reticulation event that generated r2. If we
 * generate a directed graph (testGraph) in wich the nodes are the reticulation events and the edges are the relations implied by the root
 * the solution is only valid if the graph is acyclic.
 * DESCRIPTION
 *
 * @author huson, kloepper
 * Date: 18-Sep-2004
 */

public class TestForRoot {
    /**
     * this method tests of the given root is consistent with the calculated scenario
     *
     * @return
     * @throws Exception
     */
    public static boolean checkIfRootCanBePlaced(PhyloSplitsGraph treeGraph, Taxa treeTaxa, Splits treeSplits, int treeOutgroupID, Taxa orgTaxa, Splits orgSplits, int[] treeTaxa2OrgTaxa, TaxaSet rTaxa, ReticulationTree ret) {
        PhyloSplitsGraph testGraph = new PhyloSplitsGraph();
        Node[] rTaxa2Node = new Node[orgTaxa.getNtax() + 1];
        for (int i = rTaxa.getBits().nextSetBit(1); i != -1; i = rTaxa.getBits().nextSetBit(i + 1)) {
            Node v = testGraph.newNode();
            testGraph.setInfo(v, i);
            rTaxa2Node[i] = v;
        }
        // get Node with treeOutgroupId
        Node root = treeGraph.getTaxon2Node(treeOutgroupID);
        TaxaSet seenRTaxa = new TaxaSet();
        HashSet seenNodes = new HashSet();
        seenNodes.add(root);
        RecMakeRootTestGraph(testGraph, seenRTaxa, treeGraph, treeSplits, rTaxa2Node, treeTaxa2OrgTaxa, root, ret, seenNodes, orgTaxa, rTaxa);
        return findCycle(testGraph, testGraph.getFirstNode());
    }

    /**
     * check the graph for cycles
     *
     * @param testGraph
     * @param startNode
     * @return
     * @throws Exception
     */
    private static boolean findCycle(PhyloSplitsGraph testGraph, Node startNode) {
        for (Edge e : startNode.adjacentEdges()) {
            Node toVisit = e.getTarget();
            if (toVisit != startNode) {
                if (testGraph.getInfo(toVisit) != null)
                    return true;
                else {
                    testGraph.setInfo(toVisit, true);
                    boolean tmp = findCycle(testGraph, toVisit);
                    if (tmp) return true;
                }
            }
        }
        return false;
    }


    /**
     * recursivly generate the test graph,
     *
     * @param testGraph
     * @param seenRTaxa
     * @param treeGraph
     * @param treeSplits
     * @param rTaxa2Node
     * @param treeTaxa2OrgTaxa
     * @param startNode
     * @param ret
     * @param seenNodes
     * @param orgTaxa
     * @param rTaxa
     * @throws Exception
     */
    private static void RecMakeRootTestGraph(PhyloSplitsGraph testGraph, TaxaSet seenRTaxa, PhyloSplitsGraph treeGraph, Splits treeSplits, Node[] rTaxa2Node, int[] treeTaxa2OrgTaxa, Node startNode, ReticulationTree ret, HashSet seenNodes, Taxa orgTaxa, TaxaSet rTaxa) {
    }

}
