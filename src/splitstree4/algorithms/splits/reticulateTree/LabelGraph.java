/*
 * LabelGraph.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.splits.reticulateTree;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 11.01.2006
 * Time: 11:25:24
 * To change this template use File | Settings | File Templates.
 */
public class LabelGraph {
    private static boolean verbose = false;


    /**
     * label nodes with sequence information
     *
     * @param graph
     * @param taxa
     * @param chars
     * @param split2Chars
     * @throws Exception
     */
    static public void setSequences2NodeInfo(PhyloSplitsGraph graph, Taxa taxa, Characters chars, Map split2Chars) {
        if (chars != null) {
            Node startNode = graph.getTaxon2Node(1);
            StringBuilder startNodeInfo = new StringBuilder("");
            for (int i = 1; i <= chars.getNchar(); i++) {
                if (chars.get(1, i) == '1')
                    startNodeInfo.append('1');
                else
                    startNodeInfo.append('0');
            }
            startNode.setInfo(startNodeInfo.toString());
            NodeSet seenNodes = new NodeSet(graph);
            seenNodes.add(startNode);
            recSetSequences2NodeInfo(graph, startNode, seenNodes, split2Chars);
        }
    }

    /**
     * recursively do the work
     *
     * @param graph
     * @param startNode
     * @param seenNodes
     * @param split2Chars
     * @throws Exception
     */
    private static void recSetSequences2NodeInfo(PhyloSplitsGraph graph, Node startNode, NodeSet seenNodes, Map split2Chars) {
        String startNodeInfo = (String) startNode.getInfo();
        for (Node nextNode : startNode.adjacentNodes()) {
            if (!seenNodes.contains(nextNode)) {
                seenNodes.add(nextNode);
                StringBuilder nextNodeInfo = new StringBuilder("");
                BitSet splitsOnEdge = (BitSet) graph.getCommonEdge(startNode, nextNode).getInfo();
                BitSet toChange = new BitSet();
                for (int i = splitsOnEdge.nextSetBit(1); i != -1; i = splitsOnEdge.nextSetBit(i + 1))
                    toChange.or((BitSet) split2Chars.get(i));
                for (int i = 0; i < startNodeInfo.length(); i++) {
                    if (!toChange.get(i + 1)) nextNodeInfo.append(startNodeInfo.charAt(i));
                    else {
                        if (startNodeInfo.charAt(i) == '1') nextNodeInfo.append('0');
                        else nextNodeInfo.append('1');
                    }
                }
                nextNode.setInfo(nextNodeInfo.toString());
                recSetSequences2NodeInfo(graph, nextNode, seenNodes, split2Chars);
            }

        }
    }

    /**
     * Everything for making the sequences visible on the nodes of the  graph...
     */

    static public void setSequence2NewNodeInfo(PhyloSplitsGraph graph, Node newNode, String startNodeInfo, String stopNodeInfo, LinkedList cTaxaInfos) {
        if (verbose) System.out.println("startInfo: " + startNodeInfo + "\tstopInfo: " + stopNodeInfo);
        if (startNodeInfo != null && stopNodeInfo != null) {
            StringBuilder newNodeInfo = new StringBuilder("");
            for (int i = 0; i < startNodeInfo.length(); i++) {
                if (startNodeInfo.charAt(i) == stopNodeInfo.charAt(i)) newNodeInfo.append(startNodeInfo.charAt(i));
                else {
                    // check if all  connecting taxa have the same label
                    boolean same = true;
                    Iterator it = cTaxaInfos.iterator();
                    String startTmp = (String) it.next();
                    while (same && it.hasNext()) {
                        String tmp = (String) it.next();
                        if (startTmp.charAt(i) != tmp.charAt(i)) same = false;
                    }
                    if (same) newNodeInfo.append(startTmp.charAt(i));
                    else newNodeInfo.append('2');
                }
            }
            newNode.setInfo(newNodeInfo.toString());
        }
    }


    public static void cleanEdges(PhyloGraphView graphView, PhyloSplitsGraph graph, Splits splits) {
        for (var e : graph.edges()) {
            //e.setInfo(null);
            if (verbose) System.out.println("edge: " + e + "\t" + graphView.getLabel(e));
            graph.setLabel(e, "");
            graphView.setLabel(e, "");
            //graphView.getEV(e).setLabel("");
        }
        for (int i = 1; i <= splits.getNsplits(); i++) splits.setLabel(i, "");
    }

    public static void cleanNodes(PhyloGraphView graphView, PhyloSplitsGraph graph) {
        Iterator it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            //n.setInfo(null);
            if (n.getDegree() != 1) graphView.setLabel(n, "");
        }
    }

    /**
     * @param graph
     * @param graphView
     * @throws Exception
     */

    static public void writeLabels2Nodes(PhyloGraphView graphView, PhyloSplitsGraph graph) {
        Iterator it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            String label = (String) n.getInfo();
            if (label != null) {
                label = label.replaceAll("2", "[01]");
                if (n.getDegree() == 1)
                    if (verbose)
                        System.out.println("leaf: " + n + "\tlabel: " + graph.getLabel(n) + "\t" + n.getInfo());
                if (graph.getLabel(n) != null) {
                    graphView.setLabel(n, "");
                    graph.setLabel(n, graph.getLabel(n) + ":" + label);
                } else graph.setLabel(n, label);
            }
        }
    }


    /**
     * @param graph
     * @param splits2Chars
     * @throws Exception
     */
    static public void writeSplits2Edges(PhyloSplitsGraph graph, Map splits2Chars) {
        for (var e : graph.edges()) {
            ;
            BitSet edgeSplits = (BitSet) e.getInfo();
            BitSet charPositions = new BitSet();
            StringBuilder label = new StringBuilder("");
            if (e.getInfo() != null) {
                for (int id = edgeSplits.nextSetBit(1); id != -1; id = edgeSplits.nextSetBit(id + 1)) {
                    if (verbose)
                        System.out.println("LabelSplits: split id: " + id + "\t has chars: " + splits2Chars.get(id));
                    if (splits2Chars.get(id) != null) {// no empty label
                        charPositions.or((BitSet) splits2Chars.get(id));
                    }
                }
            }
            for (int i = charPositions.nextSetBit(1); i != -1; i = charPositions.nextSetBit(i + 1)) {
                int start = i;
                while (charPositions.get(i + 1)) i++;
                int stop = i;
                if (start == stop) label.append(start);
                else label.append(start).append("-").append(stop);
                if (charPositions.nextSetBit(i + 1) != -1) label.append(", ");
            }
            if (verbose) System.out.println("Label: " + label.toString());
            graph.setLabel(e, label.toString());
        }
    }
}
