/**
 * TestForRoot.java 
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
package splitstree.algorithms.splits.reticulateTree;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.HashSet;
import java.util.Iterator;

/**
 * The class gives a implementation to check if a reticulation scenario is valid given a root, The metod uses a directed graph.
 * the root makes the treeGraph a directed graph, such that we have relations between the reticulation nodes in such a way, that
 * we know if the reticulation event that generated reticulation node r1 was before the reticulation event that generated r2. If we
 * generate a directed graph (testGraph) in wich the nodes are the reticulation events and the edges are the relations implied by the root
 * the solution is only valid if the graph is acyclic.
 * DESCRIPTION
 *
 * @author huson, kloepper
 *         Date: 18-Sep-2004
 */

public class TestForRoot {
    /**
     * this method tests of the given root is consistent with the calculated scenario
     *
     * @return
     * @throws Exception
     */
    public static boolean checkIfRootCanBePlaced(PhyloGraph treeGraph, Taxa treeTaxa, Splits treeSplits, int treeOutgroupID, Taxa orgTaxa, Splits orgSplits, int[] treeTaxa2OrgTaxa, TaxaSet rTaxa, ReticulationTree ret) {
        PhyloGraph testGraph = new PhyloGraph();
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
        return findCycle(testGraph, testGraph.nodeIterator().next());
    }

    /**
     * check the graph for cycles
     *
     * @param testGraph
     * @param startNode
     * @return
     * @throws Exception
     */
    private static boolean findCycle(PhyloGraph testGraph, Node startNode) {
        Iterator itE = testGraph.getAdjacentEdges(startNode);
        while (itE.hasNext()) {
            Edge e = (Edge) itE.next();
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
    private static void RecMakeRootTestGraph(PhyloGraph testGraph, TaxaSet seenRTaxa, PhyloGraph treeGraph, Splits treeSplits, Node[] rTaxa2Node, int[] treeTaxa2OrgTaxa, Node startNode, ReticulationTree ret, HashSet seenNodes, Taxa orgTaxa, TaxaSet rTaxa) {
        Iterator it = treeGraph.getAdjacentNodes(startNode);
        /* while (it.hasNext()) {
             Node nextNode = (Node) it.next();
             if (!seenNodes.contains(nextNode)) {
                 seenNodes.add(nextNode);
                 Edge e = treeGraph.getCommonEdge(startNode, nextNode);
                 // retrive reticulation Nodes of edge
                 TaxaSet newSeenRTaxa = (TaxaSet) seenRTaxa.clone();
                 TaxaSet indSplit = treeSplits.get(treeGraph.getSplit(e));
                 TaxaSet orgSourceSplit = new TaxaSet();
                 for (int i = indSplit.getBits().nextSetBit(1); i != -1; i = indSplit.getBits().nextSetBit(i + 1)) orgSourceSplit.set(treeTaxa2OrgTaxa[i]);
                 TaxaSet orgTargetSplit = orgSourceSplit.getComplement(orgTaxa.getNtax());
                 orgTargetSplit.andNot(rTaxa);
                 System.out.println("indSplit: " + indSplit + "\torgSplit: " + orgSourceSplit + "|" + orgTargetSplit + "\tmap: " + ret.getTreeSplit2Reticulations());

                 if (treeGraph.getSource(e) == startNode && ret.getTreeSplit2Reticulations().get(orgSourceSplit) != null) {
                     // ordering is correct
                     LinkedList retTaxaOnEdge = (LinkedList) ret.getTreeSplit2Reticulations().get(orgSourceSplit);
                     Iterator it2 = retTaxaOnEdge.iterator();
                     while (it2.hasNext()) {
                         TaxaSet rNodes = (TaxaSet) it2.next();
                         for (int i = rNodes.getBits().nextSetBit(1); i != -1; i = rNodes.getBits().nextSetBit(i + 1)) {
                             for (int j = newSeenRTaxa.getBits().nextSetBit(1); j != -1; j = newSeenRTaxa.getBits().nextSetBit(j + 1))
                                     // first one source second one target ?!?
                                 testGraph.newEdge(rTaxa2Node[j], rTaxa2Node[i]);
                             newSeenRTaxa.set(i);
                         }
                     }
                 } else if (ret.getTreeSplit2Reticulations().get(orgTargetSplit) != null) {
                     // ordering is vice Versa
                     LinkedList toAdd = new LinkedList();
                     LinkedList retTaxaOnEdge = (LinkedList) ret.getTreeSplit2Reticulations().get(orgTargetSplit);
                     Iterator it2 = retTaxaOnEdge.iterator();
                     while (it2.hasNext()) toAdd.addFirst(it2.next());
                     it2 = toAdd.iterator();
                     while (it2.hasNext()) {
                         TaxaSet rNodes = (TaxaSet) it2.next();
                         for (int i = rNodes.getBits().nextSetBit(1); i != -1; i = rNodes.getBits().nextSetBit(i + 1)) {
                             for (int j = newSeenRTaxa.getBits().nextSetBit(1); j != -1; j = newSeenRTaxa.getBits().nextSetBit(j + 1))
                                     // first one source second one target ?!?
                                 testGraph.newEdge(rTaxa2Node[j], rTaxa2Node[i]);
                             newSeenRTaxa.set(i);
                         }
                     }
                 }
                 // go into recusion
                 RecMakeRootTestGraph(testGraph, newSeenRTaxa, treeGraph, treeSplits, rTaxa2Node, treeTaxa2OrgTaxa, nextNode, ret, seenNodes, orgTaxa, rTaxa);
             }
         }*/
    }

}
