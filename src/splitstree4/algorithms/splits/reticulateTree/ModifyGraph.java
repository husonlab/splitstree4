/**
 * ModifyGraph.java
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

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.IteratorUtils;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

/**
 * In this method the taxa and splits objects are given as the complete set of taxa and splits in the graph
 * so we have to map back the taxa of the netted Component to the complete set.
 * <p/>
 * DESCRIPTION
 *
 * @author huson, kloepper
 * Date: 18-Sep-2004
 */

public class ModifyGraph {
    /**
     * modifies the splits graph so as to display a reticulation
     * I would like to remove the labelNod es Option from this part of the code and call it directly from the ReticulatedEvolutionOnTrees class...
     */
    private static boolean verbose = false;

    static public void apply(PhyloSplitsGraph graph, PhyloGraphView graphView, ReticulationTree[] reticulationList, TaxaSet[][] inducedTaxa2origsTaxa, NodeSet[] nettedComps, NodeSet gateNodes,
                             boolean showSplits, boolean labelNodes, Taxa taxa, Splits splits, int outgroup) {
        if (!showSplits) {
            HashSet reticulationEdges = new HashSet();
            boolean retNet = true;
            for (int ncomp = 1; ncomp < nettedComps.length; ncomp++) {
                NodeSet nettedComp = nettedComps[ncomp];
                if (verbose) System.err.println("\n# ModifyGraph:  Processing component: " + ncomp);
                // compute gate node 2 external taxa map:
                NodeArray gate2externalTaxa = computeGate2ExternalTaxa(gateNodes, nettedComp, graph);
                ReticulationTree ret = reticulationList[ncomp];
                if (verbose) System.out.println("Reticulation: " + ret);
                // have we found a solution
                if (ret != null) {
                    TaxaSet[] induced2origTaxa = inducedTaxa2origsTaxa[ncomp];
                    if (verbose) {
                        System.out.println("\ninduced2Origs: ");
                        for (int i = 0; i < induced2origTaxa.length; i++)
                            System.out.println("ind: " + i + "\torg: " + induced2origTaxa[i]);

                        System.err.print("# rTaxa: ");

                        for (int i = 0; i < ret.getReticulates().length; i++) {
                            System.out.print(ret.getReticulates()[i] + "\t");
                            System.out.println("" + induced2origTaxa[ret.getReticulates()[i]]);
                        }
                        System.out.println();
                    }
                    Node[] induced2gateNode = computeInduced2GateNodes(induced2origTaxa, gateNodes, gate2externalTaxa, graph);
                    if (induced2gateNode != null) {
                        //System.out.println("Reticulation is: "+ret);
                        HashSet markedEdges = findBackboneTree(taxa, splits, ret, induced2gateNode, induced2origTaxa, gateNodes, nettedComp, graph, graphView);
                        defineReticulationEdgeSplits(taxa, splits, ret, induced2gateNode, induced2origTaxa, nettedComp, graph, graphView, markedEdges);

                        // reduce the network to a tree
                        reduce2BackboneTree(nettedComp, gateNodes, markedEdges, graph);
                        // remove any divertices
                        removeDivertices(graph, gateNodes, induced2gateNode, nettedComp, splits);

                        // add the reticulations to the tree
                        reticulationEdges.addAll(addReticulations2BackboneTree(taxa, splits, ret, induced2gateNode, induced2origTaxa, gateNodes, nettedComp, graph, graphView, labelNodes));
                        // done
                        removeDivertices(graph, gateNodes, induced2gateNode, nettedComp, splits);

                    }
                } else {
                    retNet = false;
                    colorComponent(nettedComp, graph, graphView);
                }
            }
            if (outgroup != -1 && retNet) makeGraphDirected(graph, reticulationEdges, outgroup);
        } else {
            for (int ncomp = 1; ncomp < nettedComps.length; ncomp++) {
                NodeSet nettedComp = nettedComps[ncomp];
                colorComponent(nettedComp, graph, graphView);
            }
        }
    }


    /**
     * computes the gate node 2 external taxa map for a given component
     *
     * @param allGateNodes set of all gate nodes in graph
     * @param nettedComp   set of nodes in current component
     * @param graph
     */
    static private NodeArray computeGate2ExternalTaxa(NodeSet allGateNodes, NodeSet nettedComp, PhyloSplitsGraph graph) throws NotOwnerException {

        // make set of component gate nodes:
        NodeSet compGateNodes = new NodeSet(graph);
        Iterator it = allGateNodes.iterator();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            if (nettedComp.contains(v))
                compGateNodes.add(v);
        }

        //System.err.println("compGateNodes " + compGateNodes.size());

        // compute the set of external taxa reachable from each gate node
        NodeArray gate2externalTaxa = new NodeArray(graph);
        it = compGateNodes.iterator();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            gate2externalTaxa.put(v, computeGate2ExternalTaxaNode(v, nettedComp, graph));
        }
        return gate2externalTaxa;
    }


    /**
     * for a given gate node and component, find all taxa
     *
     * @param v
     * @param componentNodes
     * @param graph
     * @return set of taxa reachable from component by gate node
     */
    static private TaxaSet computeGate2ExternalTaxaNode(Node v, NodeSet componentNodes,
                                                        PhyloSplitsGraph graph) throws NotOwnerException {
        final NodeSet visited = new NodeSet(graph);
        for (Node w : v.adjacentNodes()) {
            computeGate2ExternalTaxaRec(w, componentNodes, graph, visited);
        }
        TaxaSet result = new TaxaSet();
        for (Node u : visited) {
            if (graph.hasTaxa(u))
                result.setAll(IteratorUtils.asList(graph.getTaxa(u)));
        }
        return result;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param compNodes
     * @param graph
     * @param visited
     */
    static private void computeGate2ExternalTaxaRec(Node v, NodeSet compNodes, PhyloSplitsGraph graph, NodeSet visited)
            throws NotOwnerException {
        if (!compNodes.contains(v) && !visited.contains(v)) {
            visited.add(v);
            for (Node w : v.adjacentNodes()) {
                computeGate2ExternalTaxaRec(w, compNodes, graph, visited);
            }

        }
    }

    /**
     * attempt to compute induced taxa 2 gate nodes map. returns null, if they don't match
     *
     * @param induced2origTaxa
     * @param gateNodes
     * @param gate2externalTaxa
     * @param graph
     */
    static private Node[] computeInduced2GateNodes(TaxaSet[] induced2origTaxa, NodeSet gateNodes, NodeArray gate2externalTaxa, PhyloSplitsGraph graph) throws NotOwnerException {
        Node[] induced2gate = new Node[induced2origTaxa.length];
        NodeSet used = new NodeSet(graph);
        for (int i = 1; i < induced2origTaxa.length; i++) {
            TaxaSet origTaxa = induced2origTaxa[i];
            Iterator it = gateNodes.iterator();
            if (verbose) System.out.println("origTaxa: " + origTaxa);
            boolean found = false;
            while (it.hasNext()) {
                Node v = (Node) it.next();
                if (!used.contains(v)) {
                    TaxaSet gateTaxa = (TaxaSet) gate2externalTaxa.get(v);
                    if (gateTaxa != null) {
                        if (verbose) System.out.println("gateTaxa: " + gateTaxa);
                        if (origTaxa.equals(gateTaxa)) {
                            found = true;
                            if (verbose) System.out.println("found");
                            used.add(v);
                            induced2gate[i] = v;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                System.out.println("# ModifyGraph.computeInduced2GateNode():  Failed to match ind node: " + i + "\t taxa are:" + induced2origTaxa[i]);
                return null;
            }
        }
        return induced2gate;
    }


    /**
     * color all edges that lie between any two nodes in the given set
     *
     * @param comp
     * @param graph
     * @param graphView
     * @throws NotOwnerException
     */
    static private void colorComponent(NodeSet comp, PhyloSplitsGraph graph, PhyloGraphView graphView) throws NotOwnerException {
        for (Node v : comp) {
            for (Edge e : v.adjacentEdges()) {
                Node w = graph.getOpposite(v, e);
                if (comp.contains(w)) {
                    graphView.setColor(e, Color.BLUE);
                }
            }
        }
    }

    /**
     * this method takes the ordered list of reticulations from the reticulationTree and connects
     * the reticulations nodes to the BackboneTree according to the ordered list
     *
     * @param taxa
     * @param splits
     * @param ret
     * @param induced2gateNode
     * @param induced2origTaxa
     * @param gateNodes
     * @param nettedComp
     * @param graph
     * @param graphView
     */
    static public HashSet addReticulations2BackboneTree(Taxa taxa, Splits splits, ReticulationTree ret, Node[] induced2gateNode, TaxaSet[] induced2origTaxa, NodeSet gateNodes, NodeSet nettedComp,
                                                        PhyloSplitsGraph graph, PhyloGraphView graphView, boolean labelNodes) {
        if (verbose) System.out.println(ret.getTreeSplit2Reticulations());
        HashSet reticulationEdges = new HashSet();
        //System.out.println("gateNodes: "+gateNodes);
        HashMap gateNode2origTaxa = new HashMap();
        for (int i = 1; i < induced2origTaxa.length; i++) {
            gateNode2origTaxa.put(induced2gateNode[i], induced2origTaxa[i]);
        }
        // The reticulation Taxa
        TaxaSet rTaxa = new TaxaSet();
        TaxaSet indrTaxa = new TaxaSet();
        int[] reticulation2RetIndex = new int[taxa.getNtax() + 1];
        for (int i = 0; i < ret.getReticulates().length; i++) {
            indrTaxa.set(ret.getReticulates()[i]);
            rTaxa.or(induced2origTaxa[ret.getReticulates()[i]]);
            reticulation2RetIndex[ret.getReticulates()[i]] = i;
        }
        // Add reticulations
        // make list of edges in the nettedComp
        final HashSet<Edge> nettedEdges = new HashSet<>();
        for (Node v : nettedComp) {
            for (Edge e : v.adjacentEdges()) {
                nettedEdges.add(e);
            }
        }
        // map induced splits to org splits
        final HashMap<TaxaSet, TaxaSet> indSplit2OrgSplit = new HashMap<>();
        final HashMap<TaxaSet, Object> orgSplit2orderedReticulation = new HashMap<>();

        for (Object obj : ret.getTreeSplit2Reticulations().keySet()) {
            final TaxaSet treeSplit = (TaxaSet) obj;
            if (verbose) System.out.println(treeSplit);
            TaxaSet indSplit = new TaxaSet();
            for (int i = treeSplit.getBits().nextSetBit(1); i != -1; i = treeSplit.getBits().nextSetBit(i + 1))
                indSplit.set(ret.getTreeTaxa2inducedTaxa()[i]);
            TaxaSet orgSplit = new TaxaSet();
            for (int i = indSplit.getBits().nextSetBit(1); i != -1; i = indSplit.getBits().nextSetBit(i + 1))
                orgSplit.or(induced2origTaxa[i]);
            indSplit2OrgSplit.put(indSplit, orgSplit);
            orgSplit2orderedReticulation.put(orgSplit, ret.getTreeSplit2Reticulations().get(treeSplit));
        }
        //System.out.println("inducedSplit2Reticulations: " + ret.getInducedSplit2Reticulations());
        //System.out.println("inducedSplit2OrgSplit: " + indSplit2OrgSplit);

        for (Edge e : nettedEdges) {
            Node source = graph.getSource(e);
            Node target = graph.getTarget(e);
            if (nettedComp.contains(source) || nettedComp.contains(target)) {
                Point2D pointS = graphView.getNV(source).getLocation();
                Point2D pointT = graphView.getNV(target).getLocation();
                HashSet tmp = new HashSet();
                tmp.add(e);
                TaxaSet orgSourceTaxa = new TaxaSet();
                {
                    for (Integer integer : graph.getTaxa(source)) orgSourceTaxa.set(integer);

                }
                RecFindTaxaLabels(graphView, graph, source, tmp, orgSourceTaxa, taxa, gateNodes, nettedComp, gateNode2origTaxa);
                TaxaSet orgTargetTaxa = orgSourceTaxa.getComplement(taxa.getNtax());
                orgTargetTaxa.andNot(rTaxa);
                orgSourceTaxa.andNot(rTaxa);
                if (orgSplit2orderedReticulation.get(orgSourceTaxa) != null) {
                    // add the elements in order
                    if (verbose)
                        System.out.println("running SourceTaxa \n" + "source: " + orgSourceTaxa + "\ttarget: " + orgTargetTaxa);
                    BitSet splitsOfEdge = (BitSet) e.getInfo();
                    if (verbose) {
                        System.out.println("Splits of Edge: ");
                        for (int i = splitsOfEdge.nextSetBit(1); i != -1; i = splitsOfEdge.nextSetBit(i + 1))
                            System.out.println(splits.get(i));
                    }
                    LinkedList sortedRTaxa = (LinkedList) orgSplit2orderedReticulation.get(orgSourceTaxa);
                    if (verbose) System.out.println("sortedRTaxa: " + sortedRTaxa);
                    Iterator itS = sortedRTaxa.iterator();
                    int count = 1;

                    while (itS.hasNext()) {
                        TaxaSet splitRTaxa = (TaxaSet) itS.next();
                        TaxaSet splitOrgRTaxa = new TaxaSet();

                        for (int i = splitRTaxa.getBits().nextSetBit(1); i < splitRTaxa.getBits().size() && i != -1; i = splitRTaxa.getBits().nextSetBit(i + 1))
                            splitOrgRTaxa.or(induced2origTaxa[i]);
                        if (verbose) System.out.println("split is now: " + orgSourceTaxa + " " + splitOrgRTaxa);
                        Node newV = graphView.newNode();
                        nettedComp.add(newV);

                        graphView.setLocation(newV, (pointS.getX() + (pointT.getX() - pointS.getX()) * ((double) count / ((double) sortedRTaxa.size() + 1))),
                                (pointS.getY() + (pointT.getY() - pointS.getY()) * ((double) count / ((double) sortedRTaxa.size() + 1))));
                        count++;
                        Edge newE = graph.newEdge(source, newV);
                        // define edge label
                        BitSet splitsOfNewE = new BitSet();
                        graph.setInfo(newE, splitsOfNewE);
                        for (int i = splitsOfEdge.nextSetBit(1); i != -1; i = splitsOfEdge.nextSetBit(i + 1)) {
                            if (splits.get(i).contains(orgSourceTaxa) && !splits.get(i).contains(splitOrgRTaxa)) {
                                splitsOfNewE.set(i);
                                splitsOfEdge.clear(i);
                            }

                        }
                        //
                        LinkedList cTaxaInfos = new LinkedList();
                        if (verbose) System.out.println("Setting newEdge info to: " + splitsOfNewE);
                        // connect newNode to the reticulation Taxa
                        for (int j = splitRTaxa.getBits().nextSetBit(1); j < splitRTaxa.getBits().size() && j != -1; j = splitRTaxa.getBits().nextSetBit(j + 1)) {
                            newE = graph.newEdge(newV, induced2gateNode[j]);
                            reticulationEdges.add(newE);
                            cTaxaInfos.add(induced2gateNode[j].getInfo());
                            graphView.setColor(newE, Color.blue);

                            // add edgeInfo to the  reticulation edge edge
                            TaxaSet indSource = (TaxaSet) ret.getInducedSplits().get(ret.getFirstPositionCovered()[reticulation2RetIndex[j]]).clone();
                            TaxaSet indTarget = (TaxaSet) ret.getInducedSplits().get(ret.getLastPositionCovered()[reticulation2RetIndex[j]]).clone();
                            indSource.andNot(indrTaxa);
                            indTarget.andNot(indrTaxa);
                            //System.out.println("rTaxa: "+rTaxa+"\tsplit: " + orgSourceTaxa + "|" + orgTargetTaxa + "\t" + "indSource:" + indSource + "\tindTarget: " + indTarget+"\telement:"+indSplit2OrgSplit.get(indSource));
                            // take care of reusing orgSourceTaxa
                            TaxaSet orgSourceTaxaWtihNoCTaxa = (TaxaSet) orgSourceTaxa.clone();
                            orgSourceTaxaWtihNoCTaxa.andNot(rTaxa);
                            if (indSplit2OrgSplit.get(indSource) != null) {
                                TaxaSet orgSource = (TaxaSet) indSplit2OrgSplit.get(indSource);
                                if (orgSource.equalsAsSplit(orgSourceTaxaWtihNoCTaxa, taxa.getNtax())) {
                                    HashSet retSplits = ret.getReticulation2Splits(reticulation2RetIndex[j], 1);
                                    Iterator rsIt = retSplits.iterator();
                                    BitSet data = new BitSet();
                                    while (rsIt.hasNext()) {
                                        data.set((Integer) rsIt.next());
                                    }
                                    graph.setInfo(newE, data);
                                    //System.out.println("__ setting source for ret: " + j + "\tas: " + orgSourceTaxa + "\tinfo: " + graph.getInfo(newE));
                                }
                            }
                            if (indSplit2OrgSplit.get(indTarget) != null) {
                                TaxaSet orgTarget = (TaxaSet) indSplit2OrgSplit.get(indTarget);
                                if (orgTarget.equalsAsSplit(orgSourceTaxaWtihNoCTaxa, taxa.getNtax())) {
                                    HashSet retSplits = ret.getReticulation2Splits(reticulation2RetIndex[j], 2);
                                    Iterator rsIt = retSplits.iterator();
                                    BitSet data = new BitSet();
                                    while (rsIt.hasNext()) {
                                        data.set((Integer) rsIt.next());
                                    }
                                    graph.setInfo(newE, data);
                                    //System.out.println("__ setting target for ret: " + j + "\tas: " + orgTargetTaxa + "\tinfo: " + graph.getInfo(newE));
                                }
                            }
                        }
                        // label newV
                        if (labelNodes)
                            LabelGraph.setSequence2NewNodeInfo(graph, newV, (String) source.getInfo(), (String) target.getInfo(), cTaxaInfos);
                        source = newV;
                        // move orgSourceTaxa to the next node
                        orgSourceTaxa.or(splitOrgRTaxa);
                    }
                    Edge newE = graph.newEdge(source, target);
                    // add leftover splits to last edge
                    graph.setInfo(newE, splitsOfEdge);

                    if (verbose) System.out.println("removing edge " + e + "\t with info: " + e.getInfo());
                    graph.deleteEdge(e);

                } else if (orgSplit2orderedReticulation.get(orgTargetTaxa) != null) {
                    // add the elements the other way arround
                    if (verbose)
                        System.out.println("running TargetTaxa \n" + "source: " + orgSourceTaxa + "\ttarget: " + orgTargetTaxa);
                    BitSet splitsOfEdge = (BitSet) e.getInfo();
                    if (verbose) System.out.println("Splits of Edge: ");
                    for (int i = splitsOfEdge.nextSetBit(1); i != -1; i = splitsOfEdge.nextSetBit(i + 1))
                        System.out.println(splits.get(i));
                    LinkedList sortedRTaxa = (LinkedList) ((LinkedList) orgSplit2orderedReticulation.get(orgTargetTaxa)).clone();
                    double size = sortedRTaxa.size();
                    if (verbose) System.out.println("sortedRTaxa: " + sortedRTaxa);

                    Iterator itS = sortedRTaxa.iterator();
                    int count = 1;
                    while (itS.hasNext()) {
                        TaxaSet splitRTaxa = (TaxaSet) itS.next();
                        TaxaSet splitOrgRTaxa = new TaxaSet();
                        for (int i = splitRTaxa.getBits().nextSetBit(1); i < splitRTaxa.getBits().size() && i != -1; i = splitRTaxa.getBits().nextSetBit(i + 1))
                            splitOrgRTaxa.or(induced2origTaxa[i]);
                        if (verbose) System.out.println("split is now: " + orgTargetTaxa + " " + splitOrgRTaxa);

                        Node newV = graphView.newNode();
                        graphView.setLocation(newV, (pointT.getX() + (pointS.getX() - pointT.getX()) * ((double) count / (size + 1.0))),
                                (pointT.getY() + (pointS.getY() - pointT.getY()) * ((double) count / (size + 1.0))));
                        count++;
                        Edge newE = graph.newEdge(target, newV);
                        graph.setSplit(newE, graph.getSplit(e));
                        // define edge label
                        BitSet splitsOfNewE = new BitSet();
                        graph.setInfo(newE, splitsOfNewE);
                        for (int i = splitsOfEdge.nextSetBit(1); i != -1; i = splitsOfEdge.nextSetBit(i + 1)) {
                            if (splits.get(i).contains(orgTargetTaxa) && !splits.get(i).contains(splitOrgRTaxa)) {
                                splitsOfNewE.set(i);
                                splitsOfEdge.clear(i);
                            }
                        }
                        if (verbose) System.out.println("Setting newEdge info to: " + splitsOfNewE);

                        LinkedList cTaxaInfos = new LinkedList();
                        // connect newNode to the reticulation Taxa
                        for (int j = splitRTaxa.getBits().nextSetBit(1); j < splitRTaxa.getBits().size() && j != -1; j = splitRTaxa.getBits().nextSetBit(j + 1)) {
                            newE = graph.newEdge(newV, induced2gateNode[j]);
                            reticulationEdges.add(newE);
                            cTaxaInfos.add(induced2gateNode[j].getInfo());
                            graphView.setColor(newE, Color.blue);

                            // add edgeInfo to the reticulation edge
                            TaxaSet indSource = ret.getInducedSplits().get(ret.getFirstPositionCovered()[reticulation2RetIndex[j]]);
                            TaxaSet indTarget = ret.getInducedSplits().get(ret.getLastPositionCovered()[reticulation2RetIndex[j]]);
                            indSource.andNot(indrTaxa);
                            indTarget.andNot(indrTaxa);
                            // take care of reusing orgTargetTaxa
                            TaxaSet orgTargetTaxaWtihNoCTaxa = (TaxaSet) orgTargetTaxa.clone();
                            orgTargetTaxaWtihNoCTaxa.andNot(rTaxa);

                            // /System.out.println("rTaxa: "+rTaxa+"split: " + orgSourceTaxa + "|" + orgTargetTaxa + "\t" + "indSource:" + indSource + "\tindTarget: " + indTarget+"\telement:"+indSplit2OrgSplit.get(indSource));
                            if (indSplit2OrgSplit.get(indSource) != null) {
                                TaxaSet orgSource = (TaxaSet) ((TaxaSet) indSplit2OrgSplit.get(indSource)).clone();
                                if (orgSource.equalsAsSplit(orgTargetTaxaWtihNoCTaxa, taxa.getNtax())) {
                                    HashSet retSplits = ret.getReticulation2Splits(reticulation2RetIndex[j], 1);
                                    Iterator rsIt = retSplits.iterator();
                                    BitSet data = new BitSet();
                                    while (rsIt.hasNext()) {
                                        data.set((Integer) rsIt.next());
                                    }
                                    graph.setInfo(newE, data);
                                    //System.out.println("! setting source for ret: " + j + "\tas: " + orgSourceTaxa);
                                }
                            }
                            if (indSplit2OrgSplit.get(indTarget) != null) {
                                TaxaSet orgTarget = (TaxaSet) ((TaxaSet) indSplit2OrgSplit.get(indTarget)).clone();
                                if (orgTarget.equalsAsSplit(orgTargetTaxaWtihNoCTaxa, taxa.getNtax())) {
                                    HashSet retSplits = ret.getReticulation2Splits(reticulation2RetIndex[j], 2);
                                    Iterator rsIt = retSplits.iterator();
                                    BitSet data = new BitSet();
                                    while (rsIt.hasNext()) {
                                        data.set((Integer) rsIt.next());
                                    }
                                    graph.setInfo(newE, data);
                                    //System.out.println("! setting target for ret: " + j + "\tas: " + orgTargetTaxa);
                                }
                            }

                        }
                        // label newV
                        if (labelNodes)
                            LabelGraph.setSequence2NewNodeInfo(graph, newV, (String) source.getInfo(), (String) target.getInfo(), cTaxaInfos);
                        target = newV;
                        orgTargetTaxa.or(splitOrgRTaxa);
                    }
                    Edge newE = graph.newEdge(target, source);
                    // add leftover splits to last edge

                    graph.setInfo(newE, splitsOfEdge);
                    if (verbose) System.out.println("Setting last new Edge info to: " + splitsOfEdge);
                    if (verbose) System.out.println("removing edge " + e + "\t with info: " + e.getInfo());
                    graph.deleteEdge(e);
                }
            }
        }
        return reticulationEdges;
    }

    /**
     * this method reduces a given splitgraph to the backboneTree given in the reticulationTree object.
     *
     * @param taxa
     * @param splits
     * @param ret
     * @param induced2gateNode
     * @param induced2origTaxa
     * @param gateNodes
     * @param nettedComp
     * @param graph
     * @param graphView
     */
    static public HashSet findBackboneTree(Taxa taxa, Splits splits, ReticulationTree ret, Node[] induced2gateNode, TaxaSet[] induced2origTaxa, NodeSet gateNodes, NodeSet nettedComp,
                                           PhyloSplitsGraph graph, PhyloGraphView graphView) {
        // the splits in the nettedComp
        HashSet<TaxaSet> nettedSplits = new HashSet<>();
        for (Node n : nettedComp) {
            for (Node v : n.adjacentNodes()) {
                if (nettedComp.contains(v)) {
                    nettedSplits.add(splits.get(graph.getSplit(graph.getCommonEdge(n, v))));
                }
            }
        }
        // The reticulation Taxa
        TaxaSet rTaxa = new TaxaSet();
        BitSet notToVisit = new BitSet();
        for (int i = 0; i < ret.getReticulates().length; i++) {
            rTaxa.or(induced2origTaxa[ret.getReticulates()[i]]);
            notToVisit.set(ret.getReticulates()[i]);
        }
        // define I^-1(\Sigma)
        HashSet markedEdges = new HashSet();
        // set startNode
        int startNode = notToVisit.nextClearBit(1);
        TreeMap allPaths = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2) {
                TreeSet p1 = (TreeSet) o1;
                TreeSet p2 = (TreeSet) o2;
                if (p1.size() > p2.size())
                    return 1;
                else if (p1.size() == p2.size()) {
                    Iterator p1It = p1.iterator();
                    Iterator p2It = p2.iterator();
                    while (p1It.hasNext()) {
                        TaxaSet one = (TaxaSet) p1It.next();
                        TaxaSet two = (TaxaSet) p2It.next();
                        if (!one.equals(two)) {
                            TaxaSet tmp = (TaxaSet) one.clone();
                            tmp.xor(two);
                            int first = tmp.getBits().nextSetBit(1);
                            if (one.get(first))
                                return 1;
                            else
                                return -1;
                        }
                    }
                    return 0;
                } else
                    return -1;
            }
        });
        //System.out.println("StartNode: " + induced2origTaxa[startNode]);
        for (int i = notToVisit.nextClearBit(startNode + 1); i < induced2origTaxa.length; i = notToVisit.nextClearBit(i + 1)) {
            // first sort splits according to the path
            TreeSet path = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    TaxaSet one = (TaxaSet) o1;
                    TaxaSet two = (TaxaSet) o2;
                    if (one.cardinality() > two.cardinality())
                        //@todo dangerous not ordered complete
                        return 1;
                    else
                        return -1;
                }
            });

            for (Object nettedSplit : nettedSplits) {
                TaxaSet split = (TaxaSet) nettedSplit;
                //System.out.println("nettedSplit: " + split);
                // if both taxa are not on one side
                if ((!split.contains(induced2origTaxa[startNode]) && split.contains(induced2origTaxa[i])) || (split.contains(induced2origTaxa[startNode]) && !split.contains(induced2origTaxa[i]))) {
                    TaxaSet toAdd;
                    //System.out.println("split: " + split);
                    if (split.contains(induced2origTaxa[startNode]))
                        toAdd = (TaxaSet) split.clone();
                    else
                        toAdd = split.getComplement(splits.getNtax());

                    toAdd.andNot(rTaxa);
                    //System.out.println("add: " + toAdd);
                    path.add(toAdd);
                }
            }
            //System.out.println("Path: " + path);
            allPaths.put(path, i);
        }
        //System.out.println("allPaths: " + allPaths);
        for (Object o : allPaths.keySet()) {
            TreeSet key = (TreeSet) o;
            Object[] path = key.toArray();
            // mark all splits along the path as visited
            HashSet visited = new HashSet();
            //System.out.println("startTaxa:" + induced2origTaxa[startNode] + "\tkey: " + key + "\tin all paths: " + allPaths.get(key) + "\tinduced2gateNode: " + induced2gateNode[((Integer) allPaths.get(key)).intValue()] + "\tallPaths: " + allPaths);
            recMarkSplits(graph, induced2gateNode[startNode], induced2gateNode[((Integer) allPaths.get(key))], path, 0, markedEdges, visited, nettedComp, splits, rTaxa);
        }
        /*Iterator testIt = markedEdges.iterator();
        while (testIt.hasNext()) {
            System.out.println(splits.get(graph.getSplit((Edge) testIt.next())));
        }*/
        return markedEdges;
    }

    /**
     * @param markedEdges
     * @param graph
     */
    static public void reduce2BackboneTree(NodeSet nettedComp, NodeSet gateNodes, HashSet markedEdges, PhyloSplitsGraph graph) {
        // remove all Eddges wich are not marked
        // for (int i = 1; i < nettedComps.length; i++) {
        List<Node> nodesToDelete = new ArrayList<>();
        // System.out.println("working on component "+i);
        //   NodeSet nettedComp = nettedComps[i];
        for (Node node : nettedComp) {
            for (Edge toRemove : node.adjacentEdges()) {
                if (!markedEdges.contains(toRemove) && nettedComp.contains(graph.getOpposite(node, toRemove))) {
                    //System.out.println("removing because nodes are in nettedComp: "+node+"\t"+graph.getOpposite(node,toRemove));
                    graph.deleteEdge(toRemove);
                }
            }
            if (graph.getDegree(node) == 0) nodesToDelete.add(node);
        }
        if (verbose) System.out.println("deleting nodes: " + nodesToDelete);
        for (Node toDelete : nodesToDelete) {
            nettedComp.remove(toDelete);
            if (gateNodes.contains(toDelete)) if (verbose) System.out.println("deleting gate Node with degree 0");
            graph.deleteNode(toDelete);
        }
    }

    /**
     * Find the taxa in a given subtree
     *
     * @param graphView
     * @param graph
     * @param start
     * @param seenEdges
     * @param TaxaOfSplit
     * @param taxa
     */
    static private void RecFindTaxaLabels(PhyloGraphView graphView, PhyloSplitsGraph graph, Node start, HashSet<Edge> seenEdges, TaxaSet TaxaOfSplit, Taxa taxa, NodeSet gateNodes, NodeSet nettedComp, HashMap gateNode2origTaxa) {
        for (Edge e : start.adjacentEdges()) {
            if (!seenEdges.contains(e)) {
                seenEdges.add(e);
                Node oppositeNode = graph.getOpposite(start, e);
                // do not cross blue edges in the component
                //System.out.println(nettedComp.contains(start)+"\t"+nettedComp.contains(oppositeNode)+"\t"+(graphView.getColor(e) == Color.blue));
                if (!((nettedComp.contains(start) || nettedComp.contains(oppositeNode)) && graphView.getColor(e) == Color.blue)) {
                    for (Integer integer : graph.getTaxa(oppositeNode)) TaxaOfSplit.set(integer);
                    // Recursive into subtree
                    RecFindTaxaLabels(graphView, graph, oppositeNode, seenEdges, TaxaOfSplit, taxa, gateNodes, nettedComp, gateNode2origTaxa);

                }
            }
        }
    }

    /**
     * mark edges for reduction to the backboneTree
     *
     * @param graph
     * @param startNode
     * @param stopNode
     * @param path
     * @param pathPosition
     * @param markedEdges
     * @param visitedEdges
     * @param nettedComp
     * @param splits
     * @param rTaxa
     * @return
     */
    static private boolean recMarkSplits(PhyloSplitsGraph graph, Node startNode, Node stopNode, Object[] path, int pathPosition, HashSet<Edge> markedEdges, HashSet<Edge> visitedEdges, NodeSet nettedComp, Splits splits, TaxaSet rTaxa) {

        if (pathPosition != path.length && startNode != stopNode) {
            TaxaSet nextSplit = (TaxaSet) path[pathPosition];
            List<Edge> possibleEdges = new ArrayList<>();
            for (Edge nextEdge : startNode.adjacentEdges()) {
                if (nettedComp.contains(graph.getOpposite(startNode, nextEdge)) && !visitedEdges.contains(nextEdge)) {
                    TaxaSet sCandidate = (TaxaSet) splits.get(graph.getSplit(nextEdge)).clone();
                    sCandidate.andNot(rTaxa);
                    TaxaSet sCandidateComplement = sCandidate.getComplement(splits.getNtax());
                    sCandidateComplement.andNot(rTaxa);
                    if ((sCandidate.equals(nextSplit) || sCandidateComplement.equals(nextSplit)) && !visitedEdges.contains(nextEdge)) {
                        possibleEdges.add(nextEdge);
                    }
                }
            }
            //System.out.println("nextSplit: " + nextSplit + "possible Edges: " + possibleEdges);
            if (possibleEdges.size() == 0) {
                return false;
                //throw new Exception("no node to go to");
            } else { // we have to decide wich one to take
                Edge found = null;
                for (Edge nextEdge : possibleEdges) {
                    if (markedEdges.contains(nextEdge)) {
                        found = nextEdge;
                        break;
                    }
                }
                // if there exists a node that we have taken before
                if (found != null) {
                    markedEdges.add(found);
                    visitedEdges.add(found);
                    return recMarkSplits(graph, graph.getOpposite(startNode, found), stopNode, path, pathPosition + 1, markedEdges, visitedEdges, nettedComp, splits, rTaxa);

                } else {
                    //try all other nodes until we find a path
                    for (Edge nextEdge : possibleEdges) {
                        markedEdges.add(nextEdge);
                        visitedEdges.add(nextEdge);
                        if (recMarkSplits(graph, graph.getOpposite(startNode, nextEdge), stopNode, path, pathPosition + 1, markedEdges, visitedEdges, nettedComp, splits, rTaxa))
                        // if we found a path
                        {
                            //System.out.println("Found Path: ");
                            return true;
                        }
                        // else go to next possible path
                        else {
                            markedEdges.remove(nextEdge);
                            visitedEdges.remove(nextEdge);
                        }
                    }
                    //didn't find possible path
                    return false;
                }
            }
        } else return stopNode == startNode;
    }

    /**
     *  Everything for making the mutation sites visible on the edges of the grapah
     */


    /**
     * This method defnes the splits wich ae on the reticulatio edges. Data will be saved in the Reticulation Tree object
     *
     * @param taxa
     * @param splits
     * @param ret
     * @param induced2gateNode
     * @param induced2origTaxa
     * @param nettedComp
     * @param graph
     * @param graphView
     * @param markedEdges
     */
    static private void defineReticulationEdgeSplits(Taxa taxa, Splits splits, ReticulationTree ret, Node[] induced2gateNode, TaxaSet[] induced2origTaxa, NodeSet nettedComp, PhyloSplitsGraph graph, PhyloGraphView graphView, HashSet markedEdges) {

        Iterator it;
        // the rTaxa
        TaxaSet rTaxa = new TaxaSet();
        for (int i = 0; i < ret.getReticulates().length; i++) {
            rTaxa.or(induced2origTaxa[ret.getReticulates()[i]]);
        }
        for (int i = 0; i < ret.getReticulates().length; i++) {
            int inducedrTaxa = ret.getReticulates()[i];
            int firstPosCov = ret.getFirstPositionCovered()[i];
            int lastPosCov = ret.getLastPositionCovered()[i];
            //System.out.println("firstPosCov: " + firstPosCov + "\tsplit: " + ret.getInducedSplits().get(firstPosCov) + "\tlastPosCov: " + lastPosCov + "\tsplit: " + ret.getInducedSplits().get(lastPosCov));
            HashSet firstPosCov2Splits = ret.getReticulation2Splits(i, 1);
            HashSet lastPosCov2Splits = ret.getReticulation2Splits(i, 2);
            TaxaSet firstPosSplit = new TaxaSet();
            TaxaSet lastPosSplit = new TaxaSet();
            /*for (int k = 0; k < induced2origTaxa.length; k++) {
                System.out.println(k + "\t" + induced2origTaxa[k]);
            }*/
            for (int k = 1; k < induced2origTaxa.length; k++) {
                if (ret.getInducedSplits().get(firstPosCov).get(k)) firstPosSplit.or(induced2origTaxa[k]);
                if (ret.getInducedSplits().get(lastPosCov).get(k)) lastPosSplit.or(induced2origTaxa[k]);
            }
            // check for every split the size of the path
            //System.out.println("rTaxa: " + rTaxa + "\n" + firstPosSplit + "\t" + lastPosSplit);

            // 1.) find all nodes wich are possible start nodes  of the path
            HashSet firstNodeSplits = new HashSet();
            HashSet lastNodeSplits = new HashSet();
            firstPosSplit.andNot(rTaxa);
            lastPosSplit.andNot(rTaxa);
            Node firstNode = null;
            Node lastNode = null;

            it = markedEdges.iterator();
            //System.out.println("firstPosSplit: " + firstPosSplit + "\tlastPosSplit:" + lastPosSplit);
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
                TaxaSet A = (TaxaSet) splits.get(graph.getSplit(e)).clone();
                TaxaSet B = A.getComplement(taxa.getNtax());
                A.andNot(rTaxa);
                B.andNot(rTaxa);
                //System.out.println("A:" + A + "\tB: " + B);
                if (A.equalsAsSplit(firstPosSplit, taxa.getNtax()) || B.equalsAsSplit(firstPosSplit, taxa.getNtax())) {
                    if (firstNode == null) firstNode = e.getSource();
                    firstNodeSplits.add(graph.getSplit(e));
                }
                if (A.equalsAsSplit(lastPosSplit, taxa.getNtax()) || B.equalsAsSplit(lastPosSplit, taxa.getNtax())) {
                    if (lastNode == null) lastNode = e.getSource();
                    lastNodeSplits.add(graph.getSplit(e));
                }
            }
            //System.out.println("firstNodeSplits: " + firstNodeSplits + "\tlastNodeSplits: " + lastNodeSplits);
            // 2.) find the shortest path for firstNode
            //System.out.println("firstNode: " + firstNode + "\tlastNode: " + lastNode);
            HashSet firstPosCovEdges = findPath(induced2gateNode[inducedrTaxa], firstNode, graph);

            it = firstPosCovEdges.iterator();
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
                if (!firstNodeSplits.contains((int) (graph.getSplit(e))))
                    firstPosCov2Splits.add(graph.getSplit(e));
            }
            // some System out
            graphView.setLabel(firstNode, "" + inducedrTaxa);
            Iterator itT = firstPosCovEdges.iterator();
            while (itT.hasNext()) {
                Edge e = (Edge) itT.next();
                //if (!firstNodeSplits.contains((int)(graph.getSplit(e)))) graphView.getEV(e).setColor(Color.blue);
            }

            // 3.) find the shortest path for lastNode
            HashSet lastPosCovEdges = findPath(induced2gateNode[inducedrTaxa], lastNode, graph);

            it = lastPosCovEdges.iterator();
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
                if (!lastNodeSplits.contains((int) (graph.getSplit(e))))
                    lastPosCov2Splits.add(graph.getSplit(e));
            }
            // some System out
            graphView.setLabel(lastNode, "" + inducedrTaxa);
            itT = lastPosCovEdges.iterator();
            while (itT.hasNext()) {
                Edge e = (Edge) itT.next();
                // if (!lastNodeSplits.contains((int)(graph.getSplit(e)))) graphView.getEV(e).setColor(Color.blue);
            }

        }
    }

    /**
     * @param rNode
     * @param startNode
     * @param graph
     * @return
     */
    static private HashSet findPath(Node rNode, Node startNode, PhyloSplitsGraph graph) {
        HashSet seen = new HashSet();
        seen.add(startNode);
        Vector nodes = new Vector();
        Vector data = new Vector();
        for (Node nextNode : startNode.adjacentNodes()) {
            HashSet edges = new HashSet();
            edges.add(graph.getCommonEdge(startNode, nextNode));
            // path of length 1
            if (nextNode == rNode) return edges;
            nodes.add(nextNode);
            data.add(edges);
            seen.add(nextNode);
        }
        while (true) {
            //System.out.println("startNode: " + startNode + "\trNode:" + rNode);
            if (nodes.size() == 0) return null;
            startNode = (Node) nodes.remove(0);
            HashSet edges = (HashSet) data.remove(0);
            //System.out.println("toVisit:");
            //for (int i = 0; i < nodes.size(); i++) System.out.println(nodes.get(i) + "\tsize: " + ((HashSet) data.get(i)).size());
            //System.out.println(startNode + "\t" + edges);
            for (Node nextNode : startNode.adjacentNodes()) {
                if (!seen.contains(nextNode)) {
                    // add Data
                    HashSet newEdges = (HashSet) edges.clone();
                    newEdges.add(graph.getCommonEdge(startNode, nextNode));
                    nodes.add(nextNode);
                    data.add(newEdges);
                    seen.add(nextNode);
                    if (nextNode == rNode) {
                        //System.out.println("returning: " + nextNode);
                        return newEdges;
                    }
                }
            }
        }
    }


    /**
     * remove all unlabeled nodes of degree 2
     *
     * @param graph
     */
    static public void removeDivertices(PhyloSplitsGraph graph, NodeSet gateNodes, Node[] induced2gateNode, NodeSet nettedComp, Splits splits) throws NotOwnerException {
        final List<Node> toDelete = new ArrayList<>();
        // update the netted Component
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            if (v.getDegree() == 2 && graph.getNumberOfTaxa(v) == 0) {
                nettedComp.remove(v);
                // check if it is a gate Node
                if (gateNodes.contains(v)) {
                    // take the next one outside as gateNode
                    Iterator<Node> it = v.adjacentNodes().iterator();
                    //System.out.println("toDelete: " + v + "\tdegree: " + graph.getDegree(v));
                    Node one = (Node) it.next();
                    while (nettedComp.contains(one)) one = (Node) it.next();
                    gateNodes.add(one);
                    gateNodes.remove(v);
                    //nettedComp.add(one);
                    for (int i = 0; i < induced2gateNode.length; i++)
                        if (induced2gateNode[i] == v) induced2gateNode[i] = one;
                }
                toDelete.add(v);
            }
        }
        for (Object aToDelete : toDelete) {
            Node v = (Node) aToDelete;
            Edge e = graph.getFirstAdjacentEdge(v);
            Edge f = graph.getLastAdjacentEdge(v);
            double weight = graph.getWeight(e) + graph.getWeight(f);
            int splitId = graph.getSplit(e);
            Node p = graph.getOpposite(v, e);
            Node q = graph.getOpposite(v, f);
            Edge g = null;
            try {
                g = graph.newEdge(p, q);
            } catch (IllegalSelfEdgeException e1) {
                Basic.caught(e1);
            }
            graph.setWeight(g, weight);
            graph.setSplit(g, splitId);
            BitSet toAdd = new BitSet();
            toAdd.or((BitSet) graph.getInfo(e));
            toAdd.or((BitSet) graph.getInfo(f));
            graph.setInfo(g, toAdd);
            graph.deleteNode(v);
        }
    }


    /**
     * @param graph
     * @param split2Chars
     */
    static public void removeUnlabeldEdges(PhyloGraphView graphView, PhyloSplitsGraph graph, Map split2Chars) {
        LinkedList toDelete = new LinkedList();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            BitSet edgeSplits = (BitSet) e.getInfo();
            if (edgeSplits.nextSetBit(1) == -1 && e.getSource().getDegree() != 1 && e.getTarget().getDegree() != 1)
                toDelete.add(e);
        }
        for (Object aToDelete : toDelete) {
            Edge e = (Edge) aToDelete;
            Node source = e.getSource();
            if (source.getOwner() == null)
                continue;    // should never happen, but does! FIXED, but why?
            Node target = e.getTarget();
            if (target.getOwner() == null)
                continue;     // don't know whether this can happen, did this to be safe... FIXED, but why?

            Set<Node> adjacentNodes = new HashSet<>();
            for (Node w : target.adjacentNodes()) {
                adjacentNodes.add(w);
            }
            for (Object adjacentNode : adjacentNodes) {
                Node n = (Node) adjacentNode;
                if (!n.equals(source)) {
                    Edge oldE = target.getCommonEdge(n);
                    Edge newE;
                    if (oldE.getSource().equals(target))
                        newE = graph.newEdge(source, n);
                    else
                        newE = graph.newEdge(n, source);
                    newE.setInfo(oldE.getInfo());
                    graphView.getEV(newE).setColor(graphView.getEV(oldE).getColor());
                    graph.deleteEdge(oldE);
                }
            }
            graph.deleteEdge(e);
            graph.deleteNode(target);
        }
    }


    static private void makeGraphDirected(PhyloSplitsGraph graph, HashSet reticulationEdges, int outgroup) {
        if (verbose) System.out.println("directing graph");
        Node o = graph.getTaxon2Node(outgroup);
        // o is a leaf and connected to the root
        Node root = o.adjacentNodes().iterator().next();
        //graph.setLabel(root, "this is start: " + root.getInDegree());
        if (verbose) System.err.println("starting resorting");
        recMakeGraphDirected(graph, reticulationEdges, root, new HashSet());
    }

    static private boolean recMakeGraphDirected(PhyloSplitsGraph graph, HashSet reticulationEdges, Node start, HashSet ancestors) {
        //System.out.println("start: "+start+"\t ancestors: "+ancestors);
        for (Node n : start.adjacentNodes()) {
            //System.out.println("next Node is " + n + "\tis ancestor: " + ancestors.contains(n) + "\treticulation Node: " + reticulationEdges.contains(n.getCommonEdge(start)));
            if (!ancestors.contains(n)) {
                Edge e = start.getCommonEdge(n);
                if (!reticulationEdges.contains(e)) {
                    if (e.getSource().equals(n)) {
                        // change direction
                        //System.out.println("\nchanging direction of edge between: " + start + "\t" + n);
                        Edge newE = graph.newEdge(start, n);
                        graph.setAngle(newE, graph.getAngle(e));
                        graph.setConfidence(newE, graph.getConfidence(e));
                        graph.setInfo(newE, graph.getInfo(e));
                        graph.setLabel(newE, graph.getLabel(e));
                        graph.setSplit(newE, graph.getSplit(e));
                        graph.setWeight(newE, graph.getWeight(e));
                        e.deleteEdge();
                    }
                    HashSet newAncestors = (HashSet) ancestors.clone();
                    newAncestors.add(start);
                    if (!recMakeGraphDirected(graph, reticulationEdges, n, newAncestors)) return false;

                } else if (reticulationEdges.contains(e) && e.getSource().equals(start)) {
                    HashSet newAncestors = (HashSet) ancestors.clone();
                    newAncestors.add(start);
                    if (!recMakeGraphDirected(graph, reticulationEdges, n, newAncestors)) return false;
                }
            }
        }
        //graph.setLabel(start,start+" : " + start.getInDegree());
        return true;
    }
}
