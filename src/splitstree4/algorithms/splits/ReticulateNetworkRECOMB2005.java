/**
 * ReticulateNetworkRECOMB2005.java
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
import jloda.util.Basic;
import jloda.util.Pair;
import splitstree4.algorithms.splits.reticulate.AlgorithmRECOMB2005;
import splitstree4.algorithms.splits.reticulate.Reticulation;
import splitstree4.algorithms.util.ReticulateEmbedder;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * framework for generating reticulate evolution graphs from splits
 *
 * @author huson
 * Date: 16-Sep-2004
 */
public class ReticulateNetworkRECOMB2005 implements Splits2Network {
    public final boolean EXPERT = true;

    // known options:
    boolean optionShowSplits = false;
    String optionOutGroup = Taxa.FIRSTTAXON;
    public String optionLayout = ReticulateNetwork.EQUALANGLE120;
    private int optionMaxReticulationsPerTangle = 4;
    private int optionWhich = 1;
    public int optionPercentOffset = 10;

    protected boolean optionShowSequences = false; // used in extension RecombinationNetwork
    protected boolean optionShowMutations = false;  // used in extension RecombinationNetwork

    protected Map split2Chars = null; // map each split to one or more character positions
    protected char[] firstChars = null; // character states for first taxon

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        System.err.println("Computing network...");

        // 0. if this hasn't be set explicitly, then compute for splits:
        // always need this to do edge resolution
        if (split2Chars == null) {
            firstChars = new char[splits.getNsplits() + 1];
            split2Chars = new HashMap();
            for (int s = 1; s <= splits.getNsplits(); s++) {
                final BitSet bits = new BitSet();
                bits.set(s);
                split2Chars.put(s, bits);
            }
        }

        // 1. compute incompatibility graph
        Graph incompGraph = buildIncompatibilityGraph(splits);
        // 2. compute components
        int[] split2incomp = new int[splits.getNsplits() + 3]; // will add root node
        NodeSet[] incompComponents = computeNonTrivialConnectedComponents(incompGraph, split2incomp);
        //System.err.print("# Non-trivial components (" + incompComponents.length + "):");
        //for (int i = 0; i < incompComponents.length; i++)
        //System.err.print(" " + incompComponents[i].size());
        //System.err.println();

        List reticulationList = new LinkedList();

        List induced2origs = new LinkedList();

        for (int i = 0; i < incompComponents.length; i++) {
            System.err.println("Processing component " + i + ":");
            // 3. for each component, compute equivalence classes of taxa and induced splits
            Taxa inducedTaxa = new Taxa();
            Splits inducedSplits = new Splits();
            TaxaSet[] induced2origTaxa = computeInducedProblem(taxa, splits, incompComponents[i], incompGraph, inducedTaxa, inducedSplits);

            for (int t = 1; t <= inducedTaxa.getNtax(); t++) {
                //System.err.println("induced2origTaxa["+t+"]="+induced2origTaxa[t])
            }

            // determine which induced taxon contains the original root:
            int outGroupId = taxa.indexOf(getOptionOutGroup());
            int inducedOutGroupId = 0;
            if (outGroupId != -1) {
                for (int t = 1; t <= inducedTaxa.getNtax(); t++) {
                    if (induced2origTaxa[t].get(outGroupId)) {
                        inducedOutGroupId = t;
                        break;
                    }
                }
                if (inducedOutGroupId == 0)
                    throw new SplitsException("Couldn't map original outgroup " + outGroupId + " to induced taxon");
            }

            // 4. analyse induced splits and find configurations:

            //System.err.println("#nexus");
            //System.err.println(inducedTaxa.toString());
            //System.err.println(inducedSplits.toString(taxa));

            // different analysis methods:
            Reticulation ret = new Reticulation();
            AlgorithmRECOMB2005 finder = new AlgorithmRECOMB2005();
            if (finder.apply(inducedTaxa, inducedSplits, inducedOutGroupId, ret, getOptionWhich())) {
                reticulationList.add(ret);
                induced2origs.add(induced2origTaxa);
            }
        }

        // 5. build graph
        PhyloGraphView graphView;
        PhyloSplitsGraph graph;

        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX) &&
                taxa.indexOf(getOptionOutGroup()) > 0) {
            RootedEqualAngle ea = new RootedEqualAngle();
            ea.setOptionMaxAngle(ReticulateNetwork.getLayoutAngle(getOptionLayout()));
            ea.setOutGroup(getOptionOutGroup(), doc);
            ea.apply(doc, taxa, splits);
            graphView = ea.getGraphView();
            graph = graphView.getPhyloGraph();
        } else {
            EqualAngle ea = new EqualAngle();
            ea.apply(doc, taxa, splits);
            graphView = ea.getPhyloGraphView();
            graph = graphView.getPhyloGraph();
        }

        // compute node to sequence map:
        NodeArray node2Sequence = graph.labelNodesBySequences(split2Chars, firstChars);

        // 6. find netted components in graph
        NodeSet gateNodes = new NodeSet(graph);

        NodeSet[] nettedComps = computeNettedComps(split2incomp, graph, gateNodes);

        int numNettedComps = 0;
        if (nettedComps != null)
            numNettedComps = nettedComps.length - 1;
        //System.err.println("# Netted components in graph: " + numNettedComps);

        // 7. map each found configuration to graph and modify graph:
        BitSet found = new BitSet();
        for (int ncomp = 1; ncomp <= numNettedComps; ncomp++) {
            //System.err.println("# Processing component: " + ncomp);
            NodeSet nettedComp = nettedComps[ncomp];
            // compute gate node 2 external taxa map:
            NodeArray gate2externalTaxa = computeGate2ExternalTaxa(gateNodes, nettedComp, graph);

            // find result that matches this netted component:
            for (int r = 0; r < reticulationList.size(); r++) {
                if (!found.get(r)) {
                    Reticulation ret = (Reticulation) reticulationList.get(r);
                    TaxaSet[] induced2origTaxa = (TaxaSet[]) induced2origs.get(r);
                    Node[] induced2gateNode = computeInduced2GateNodes(ret, induced2origTaxa, gateNodes,
                            gate2externalTaxa, graph);

                    if (induced2gateNode != null) {
                        found.set(r);

                        if (!getOptionShowSplits())
                            modifyGraph(ret, induced2gateNode, gateNodes, nettedComp, graph, graphView, node2Sequence);
                        else
                            colorComponent(nettedComp, graph, graphView);
                    }
                }
            }
        }
        // 8. remove any nodes of degree two:
        removeDivertices(graph);

        // 9. label nodes by sequences, if desired:
        if (optionShowSequences && node2Sequence != null) {
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                if (node2Sequence.get(v) != null) {
                    if (graphView.getLabel(v) == null)
                        graphView.setLabel(v, (String) node2Sequence.get(v));
                    else
                        graphView.setLabel(v, graphView.getLabel(v) + ":" + node2Sequence.get(v));
                }
            }
        }

        // 10. label edges by mutation and recombinations, if desired:
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            {
                graphView.setLabel(e, null);
                graph.setLabel(e, null);
            }
        }

        if (optionShowMutations && node2Sequence != null) {
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
                Node v = e.getSource();
                Node w = e.getTarget();
                if (node2Sequence.get(v) != null && node2Sequence.get(w) != null) {
                    String label = AlgorithmRECOMB2005.deltaBinarySequences((String) node2Sequence.get(v),
							(String) node2Sequence.get(w));
                    if (graph.getLabel(e) == null)
                        graph.setLabel(e, label);
                    else
                        graph.setLabel(e, graph.getLabel(e) + ":" + label);
                }
            }
        }

        // 11. if rectangular phylogram requested, compute now
        if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARPHYLOGRAM)) {
            int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

            ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
            reticulateEmbedder.computeRectangularPhylogram(taxa, splits, graphView,
                    outgroupId, true, false, getOptionPercentOffset(), false, false);
        } else if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARCLADOGRAM)) {
            int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

            ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
            reticulateEmbedder.computeRectangularCladogram(taxa, splits, graphView, outgroupId, false, true);
        }

        // 12. return modified graph and info

        Network network = new Network(taxa, graphView);
        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
            network.setLayout(Network.CIRCULAR);
        else
            network.setLayout(Network.RECTILINEAR);
        return network;
    }

    /**
     * color all edges that lie between any two nodes in the given set
     *
     * @param comp
     * @param graph
     * @param graphView
     * @throws NotOwnerException
     */
    private void colorComponent(NodeSet comp, PhyloSplitsGraph graph, PhyloGraphView graphView) throws NotOwnerException {
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
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValid(taxa) && doc.isValid(splits);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return "Detects reticulate evolution as described in: Huson, Kloepper, Steel RECOMB2005";
    }

    /**
     * preserve split edges in components?
     *
     * @return true, if to save split edges
     */
    public boolean getOptionShowSplits() {
        return optionShowSplits;
    }

    /**
     * preserve edges in components?
     *
     * @param optionShowSplits
     */
    public void setOptionShowSplits(boolean optionShowSplits) {
        this.optionShowSplits = optionShowSplits;
    }

    public String getOptionOutGroup() {
        return optionOutGroup;
    }

    public void setOptionOutGroup(String optionOutGroup) {
        this.optionOutGroup = optionOutGroup;
    }

    public int getOptionMaxReticulationsPerTangle() {
        return optionMaxReticulationsPerTangle;
    }

    public void setOptionMaxReticulationsPerTangle(int optionMaxReticulationsPerTangle) {
        this.optionMaxReticulationsPerTangle = optionMaxReticulationsPerTangle;
    }

    /**
     * if there is more than one solution for a component, get this one
     *
     * @return which
     */
    public int getOptionWhich() {
        return optionWhich;
    }

    public void setOptionWhich(int optionWhich) {
        this.optionWhich = optionWhich;
    }

    public boolean getOptionShowSequences() {
        return optionShowSequences;
    }

    public void setOptionShowSequences(boolean optionShowSequences) {
        this.optionShowSequences = optionShowSequences;
    }

    public boolean getOptionShowMutations() {
        return optionShowMutations;
    }

    public void setOptionShowMutations(boolean optionShowMutations) {
        this.optionShowMutations = optionShowMutations;
    }

    /**
     * build the incompatibility graph.
     * Each node is labeled by a Pair consisting of the ID of the split and its weight
     *
     * @param splits
     * @return incompatibility graph
     */
    Graph buildIncompatibilityGraph(Splits splits) {
        Node[] split2node = new Node[splits.getNsplits() + 1];
        Graph graph = new Graph();

        try {
            for (int s = 1; s <= splits.getNsplits(); s++) {
                Pair pair = new Pair(s, splits.getWeight(s));
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
     * computes the components of the incompatibility graph:
     *
     * @param incompGraph
     * @return components
     */
    private NodeSet[] computeNonTrivialConnectedComponents(Graph incompGraph, int[] split2incomp)
            throws NotOwnerException {
        List components = new LinkedList();

        NodeSet unvisited = new NodeSet(incompGraph);
        unvisited.addAll();
        while (unvisited.size() > 0) {
            Node v = unvisited.getFirstElement();
            NodeSet comp = new NodeSet(incompGraph);
            visitComponentRec(v, unvisited, comp);
            if (comp.size() > 1) {
                components.add(comp);
                int ncomp = components.size();
                for (Node u = incompGraph.getFirstNode(); u != null; u = incompGraph.getNextNode(u)) {
                    if (comp.contains(u)) {
                        int s = ((Pair) incompGraph.getInfo(u)).getFirstInt();
                        split2incomp[s] = ncomp;
                        //System.err.println("split " + s + " -> comp " + ncomp);
                    }
                }
            }
        }
        return (NodeSet[]) components.toArray(new NodeSet[components.size()]);
    }

    /**
     * visit a connected component
     *
     * @param v
     * @param unvisited
     * @param comp
     * @throws NotOwnerException
     */
    private void visitComponentRec(Node v, NodeSet unvisited, NodeSet comp) throws NotOwnerException {
        if (unvisited.contains(v)) {
            unvisited.remove(v);
            comp.add(v);
            for (Node w : v.adjacentNodes()) {
                visitComponentRec(w, unvisited, comp);
            }
        }
    }

    /**
     * computes the taxa and splits induced by the given component of the incompatibility graph
     *
     * @param taxa
     * @param splits
     * @param component
     * @param incompGraph
     * @param inducedTaxa
     * @param inducedSplits
     * @return map from induced taxa to original taxa
     */
    private TaxaSet[] computeInducedProblem(Taxa taxa, Splits splits, NodeSet component, Graph incompGraph,
                                            Taxa inducedTaxa, Splits inducedSplits) throws NotOwnerException, SplitsException {

        // compute the matrix: sepMatrix[i][j] equals the number of splits in component
        // that separate i and j
        int[][] sepMatrix = new int[taxa.getNtax() + 1][taxa.getNtax() + 1];

        Iterator it = component.iterator();
        while (it.hasNext()) {
            Node vs = (Node) it.next();
            TaxaSet s = splits.get(((Pair) incompGraph.getInfo(vs)).getFirstInt());
            for (int t1 = s.getBits().nextSetBit(1); t1 > 0; t1 = s.getBits().nextSetBit(t1 + 1)) {
                for (int t2 = s.getBits().nextClearBit(1); t2 <= taxa.getNtax(); t2 = s.getBits().nextClearBit(t2 + 1)) {
                    sepMatrix[t2][t1] = ++sepMatrix[t1][t2];
                }
            }
        }

        // detect equivalence classes of non-separated taxa
        List equivClasses = new LinkedList();

        int[] orig2inducedTaxa = new int[taxa.getNtax() + 1];
        BitSet seen = new BitSet();
        int numInducedTaxa = 0;
        for (int t1 = 1; t1 <= taxa.getNtax(); t1++) {
            if (!seen.get(t1)) {
                numInducedTaxa++;
                TaxaSet aClass = new TaxaSet();
                aClass.set(t1);
                seen.set(t1);
                orig2inducedTaxa[t1] = numInducedTaxa;

                for (int t2 = t1 + 1; t2 <= taxa.getNtax(); t2++) {
                    if (sepMatrix[t1][t2] == 0) {
                        aClass.set(t2);
                        orig2inducedTaxa[t2] = numInducedTaxa;
                        seen.set(t2);
                    }
                }
                equivClasses.add(aClass);
            }
        }
        // convert list into array:
        TaxaSet[] induced2origTaxa = new TaxaSet[numInducedTaxa + 1];
        for (int t = 1; t <= numInducedTaxa; t++)
            induced2origTaxa[t] = (TaxaSet) equivClasses.get(t - 1);

        // make induced taxon set:
        inducedTaxa.setNtax(numInducedTaxa);
        for (int t = 1; t <= inducedTaxa.getNtax(); t++)
            inducedTaxa.setLabel(t, "t" + t);

        // make induced splits set:
        Set newSplitSet = new HashSet();

        it = component.iterator();
        while (it.hasNext()) {
            Node vs = (Node) it.next();
            TaxaSet origSplit = splits.get(((Pair) incompGraph.getInfo(vs)).getFirstInt());
            TaxaSet newSplit = new TaxaSet();
            for (int t = origSplit.getBits().nextSetBit(1); t > 0; t = origSplit.getBits().nextSetBit(t + 1))
                newSplit.set(orig2inducedTaxa[t]);
            newSplitSet.add(newSplit);
        }

        inducedSplits.setNtax(inducedTaxa.getNtax());
        it = newSplitSet.iterator();
        while (it.hasNext()) {
            inducedSplits.add((TaxaSet) it.next());
        }

        for (int t = 1; t <= inducedTaxa.getNtax(); t++) {
            //System.err.println("# induced " + t + " -> " + orig2inducedTaxa[t]);
        }

        return induced2origTaxa;
    }

    /**
     * determines all netted components in the graph. They are numbered 1, 2...
     *
     * @param split2incomp map splits to non-trivial components of incompat graph
     * @param graph
     * @param gateNodes
     * @return the components
     */
    private NodeSet[] computeNettedComps(int[] split2incomp, PhyloSplitsGraph graph, NodeSet gateNodes) throws NotOwnerException {

        int ncomps = 0;
        for (int aSplit2incomp : split2incomp)
            if (aSplit2incomp > ncomps)
                ncomps = aSplit2incomp;
        NodeSet[] components = new NodeSet[ncomps + 1];
        for (int i = 1; i < components.length; i++)
            components[i] = new NodeSet(graph);

        EdgeSet seen = new EdgeSet(graph);
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {
                int ncomp = split2incomp[graph.getSplit(e)];
                if (!seen.contains(e) && ncomp > 0) {
                    computeNettedCompsRec(v, ncomp, seen, split2incomp, graph, components);
                }
            }
        }

        // determine gate nodes: any node not completely contained in one component
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            if (graph.getDegree(v) > 1) {
                int nc = split2incomp[graph.getSplit(graph.getFirstAdjacentEdge(v))];
                for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {
                    if (split2incomp[graph.getSplit(e)] != nc) {
                        gateNodes.add(v);
                        break;
                    }
                }
            }
        }
        //for (int i = 0; i < ncomps; i++)
        //System.err.println("netted comp: " + components[i]);
        return components;
    }

    /**
     * recursively determine all netted compnents
     *
     * @param v
     * @param seen
     * @param split2incomp
     * @param graph
     * @param components
     * @throws NotOwnerException
     */
    private void computeNettedCompsRec(Node v, int ncomp, EdgeSet seen, int[] split2incomp, PhyloSplitsGraph graph,
                                       NodeSet[] components) throws NotOwnerException {
        for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {

            if (!seen.contains(e) && split2incomp[graph.getSplit(e)] == ncomp) {
                seen.add(e);
                components[ncomp].add(v);
                computeNettedCompsRec(graph.getOpposite(v, e), ncomp, seen, split2incomp, graph, components);
            }
        }
    }

    /**
     * computes the gate node 2 external taxa map for a given component
     *
     * @param allGateNodes set of all gate nodes in graph
     * @param compNodes    set of nodes in current component
     * @param graph
     */
    private NodeArray computeGate2ExternalTaxa(NodeSet allGateNodes,
                                               NodeSet compNodes,
                                               PhyloSplitsGraph graph) throws NotOwnerException {

        // make set of component gate nodes:
        NodeSet compGateNodes = new NodeSet(graph);
        Iterator it = allGateNodes.iterator();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            if (compNodes.contains(v))
                compGateNodes.add(v);
        }

        //System.err.println("compGateNodes " + compGateNodes.size());

        // compute the set of external taxa reachable from each gate node
        NodeArray gate2externalTaxa = new NodeArray(graph);
        it = compGateNodes.iterator();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            gate2externalTaxa.put(v, computeGate2ExternalTaxaNode(v, compNodes, graph));
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
    private TaxaSet computeGate2ExternalTaxaNode(Node v, NodeSet componentNodes, PhyloSplitsGraph graph) throws NotOwnerException {
        NodeSet visited = new NodeSet(graph);
        for (Node w : v.adjacentNodes()) {
            computeGate2ExternalTaxaRec(w, componentNodes, graph, visited);
        }
        TaxaSet result = new TaxaSet();
        for (Node u : visited) {
            for (Integer t : graph.getTaxa(u)) {
                result.set(t);
            }
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
    private void computeGate2ExternalTaxaRec(Node v, NodeSet compNodes, PhyloSplitsGraph graph, NodeSet visited)
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
     * @param ret
     * @param induced2origTaxa
     * @param gateNodes
     * @param gate2externalTaxa
     * @param graph
     * @return true, if backbone and recticulation were successfully fitted
     */
    private Node[] computeInduced2GateNodes(Reticulation ret,
                                            TaxaSet[] induced2origTaxa, NodeSet gateNodes,
                                            NodeArray gate2externalTaxa,
                                            PhyloSplitsGraph graph) throws NotOwnerException {

        //System.err.println("# Attempting to match reticulation with graph:");
        //if (ret == null || ret.getBackbone() == null)
        //System.err.println("Warning: ret=" + ret);
        // attempt to match induced taxa to gate nodes:
        Node[] induced2gate = new Node[induced2origTaxa.length + 1];
        NodeSet used = new NodeSet(graph);
        int[] backbone = ret.getBackbone();
        for (int t : backbone) {
            //System.err.println("# processing backbone " + t);
            TaxaSet origTaxa = induced2origTaxa[t];
            //System.err.println("origTaxa " + origTaxa);

            Iterator it = gateNodes.iterator();
            boolean found = false;
            while (it.hasNext()) {
                Node v = (Node) it.next();
                if (!used.contains(v)) {
                    TaxaSet gateTaxa = (TaxaSet) gate2externalTaxa.get(v);
                    ////System.err.println("gate node " + v);
                    //System.err.println("gate taxa " + gateTaxa);
                    if (gateTaxa != null && origTaxa.equals(gateTaxa)) {
                        found = true;
                        used.add(v);
                        induced2gate[t] = v;
                        break;
                    }
                }
            }
            if (!found) {
                //System.err.println("# Failed to match backbone node: " + backbone[i]);
                return null;
            }
        }

        int[] hybrids = ret.getReticulates();
        for (int t : hybrids) {
            //System.err.println("processing hybrid " + t);

            TaxaSet origTaxa = induced2origTaxa[t];
            //System.err.println("original taxa " + origTaxa);

            Iterator it = gateNodes.iterator();
            boolean found = false;
            while (it.hasNext()) {
                Node v = (Node) it.next();
                if (!used.contains(v)) {
                    TaxaSet gateTaxa = (TaxaSet) gate2externalTaxa.get(v);
                    if (gateTaxa != null && origTaxa.equals(gateTaxa)) {
                        found = true;
                        used.add(v);
                        induced2gate[t] = v;
                        break;
                    }
                }
            }
            if (!found) {
                //System.err.println("# Failed to match hybrid node: " + backbone[i]);
                return null;
            }
        }

        return induced2gate;
    }

    /**
     * modifies the splits graph so as to display a reticulation
     *
     * @param ret
     * @param induced2gateNode
     * @param gateNodes
     * @param nettedComp       netted component
     * @param graph
     * @param graphView
     * @param node2Sequence
     */
    private void modifyGraph(Reticulation ret, Node[] induced2gateNode, NodeSet gateNodes, NodeSet nettedComp,
                             PhyloSplitsGraph graph, PhyloGraphView graphView, NodeArray node2Sequence) throws NotOwnerException {

        //System.err.println("# Modify graph:");
        // delete all non-gate nodes in the netted component:
        List toDelete = new LinkedList();
        Iterator it = nettedComp.iterator();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            if (!gateNodes.contains(v))
                toDelete.add(v);
        }
        it = toDelete.iterator();
        while (it.hasNext())
            graph.deleteNode((Node) it.next());

        // delete edges that lie between two taxa in the netted component:
        toDelete.clear();
        BitSet seen = new BitSet();
        for (Node v : nettedComp) {
            for (Edge e : v.adjacentEdges()) {
                Node w = graph.getOpposite(v, e);
                if (nettedComp.contains(w) && !seen.get(graph.getId(e))) {
                    toDelete.add(e);
                    seen.set(graph.getId(e)); // make unique
                }
            }
        }
        it = toDelete.iterator();
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            graph.deleteEdge(e);
        }

        int[] backbone = ret.getBackbone();
        System.err.println("Backbone: " + backbone.length);

        // setup map from position in backbone to gate node:
        Node[] backbonePos2node = new Node[backbone.length];
        for (int i = 0; i < backbone.length; i++)
            backbonePos2node[i] = induced2gateNode[backbone[i]];

        // successor nodes are placed between backbones so that we can later attach
        // the hybrid nodes
        Node[] backbonePos2succNode = new Node[backbone.length - 1]; // last has no successor
        for (int i = 0; i < backbone.length - 1; i++) {
            Node v = graph.newNode();
            double x = 0.5 * (graphView.getLocation(backbonePos2node[i]).getX()
                    + graphView.getLocation(backbonePos2node[i + 1]).getX());
            double y = 0.5 * (graphView.getLocation(backbonePos2node[i]).getY()
                    + graphView.getLocation(backbonePos2node[i + 1]).getY());

            graphView.setLocation(v, x, y);
            backbonePos2succNode[i] = v;
        }

        // connect hybrids:
        int[] hybrids = ret.getReticulates();
        for (int i = 0; i < hybrids.length; i++) {
            int reticulateTaxon = hybrids[i];
            int firstPosCovered = ret.getFirstPositionCovered()[i];
            int lastPosCovered = ret.getLastPositionCovered()[i];

            final Node r = induced2gateNode[reticulateTaxon];
            final String rSequence = (String) node2Sequence.get(r);

            final Node pa = backbonePos2node[firstPosCovered - 1];
            final String paSequence = (String) node2Sequence.get(pa);
            final Node pb = backbonePos2succNode[firstPosCovered - 1];
            final Node pc = backbonePos2node[firstPosCovered];
			final String pcSequence = (String) node2Sequence.get(pc);
			final String pConsensus = AlgorithmRECOMB2005.majorityBinarySequences(paSequence, pcSequence, rSequence);
			final Node pAttach;
            if (pConsensus.equals(paSequence))
                pAttach = pa;
            else if (pConsensus.equals(pcSequence))
                pAttach = pc;
            else {
                pAttach = pb;
                if (node2Sequence.get(pAttach) != null) {
                    String oldSequence = (String) node2Sequence.get(pAttach);
                    if (!oldSequence.equals(pConsensus))
                        System.err.println("Replaced: " + oldSequence + "\n"
                                + "by:       " + pConsensus);
                }
                node2Sequence.put(pAttach, pConsensus);
            }

            final Node qa = backbonePos2node[lastPosCovered + 1];
            final String qaSequence = (String) node2Sequence.get(qa);
            final Node qb = backbonePos2succNode[lastPosCovered];
            final Node qc = backbonePos2node[lastPosCovered];
			final String qcSequence = (String) node2Sequence.get(qc);
			final String qConsensus = AlgorithmRECOMB2005.majorityBinarySequences(qaSequence, qcSequence, rSequence);
			final Node qAttach;
            if (qConsensus.equals(qaSequence))
                qAttach = qa;
            else if (qConsensus.equals(qcSequence))
                qAttach = qc;
            else {
                qAttach = qb;
                if (node2Sequence.get(qAttach) != null) {
                    String oldSequence = (String) node2Sequence.get(qAttach);
                    if (!oldSequence.equals(qConsensus))
                        System.err.println("Replaced: " + oldSequence + "\n"
                                + "by:       " + qConsensus);
                }
                node2Sequence.put(qAttach, qConsensus);
            }

            Edge e = graph.newEdge(pAttach, r);
            graphView.setColor(e, Color.BLUE);
            graph.setSplit(e, -1);
            e = graph.newEdge(qAttach, r);
            graphView.setColor(e, Color.BLUE);
            graph.setSplit(e, -1);
        }

        // construct backbone edges:
        for (int i = 0; i < backbone.length; i++) {
            Node v = backbonePos2node[i];
            if (i > 0) {
                Node u = backbonePos2succNode[i - 1];
                graph.newEdge(u, v);
            }
            if (i < backbone.length - 1) {
                Node w = backbonePos2succNode[i];
                graph.newEdge(v, w);
            }
        }
    }

    /**
     * remove all unlabeled nodes of degree 2
     *
     * @param graph
     */
    private void removeDivertices(PhyloSplitsGraph graph) throws NotOwnerException {
        List<Node> toDelete = new ArrayList<>();
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            if (v.getDegree() == 2 && graph.getNumberOfTaxa(v) == 0)
                toDelete.add(v);
        }
        for (Object aToDelete : toDelete) {
            Node v = (Node) aToDelete;
            Edge e = graph.getFirstAdjacentEdge(v);
            Edge f = graph.getLastAdjacentEdge(v);
            double weight = graph.getWeight(e) + graph.getWeight(f);
            Node p = graph.getOpposite(v, e);
            Node q = graph.getOpposite(v, f);
            Edge g;

            if (p == e.getTarget())
                g = graph.newEdge(p, q);
            else
                g = graph.newEdge(q, p);

            graph.setWeight(g, weight);

            graph.deleteNode(v);
        }
    }

    /**
     * gets the split 2 characters map
     *
     * @return splits 2 characters map
     */
    public Map getSplit2Chars() {
        return split2Chars;
    }

    /**
     * sets the split 2 characters map if we want to label nodes and edges by
     *
     * @param split2Chars
     */
    public void setSplit2Chars(Map split2Chars) {
        this.split2Chars = split2Chars;
    }

    /**
     * gets the reference sequence of labeling
     *
     * @return reference sequence
     */
    public char[] getFirstChars() {
        return firstChars;
    }

    /**
     * sets the reference sequence of labeling
     *
     * @param firstChars
     */
    public void setFirstChars(char[] firstChars) {
        this.firstChars = firstChars;
    }

    public String getOptionLayout() {
        return optionLayout;
    }

    public void setOptionLayout(String optionLayout) {
        this.optionLayout = optionLayout;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List<String> selectionOptionLayout(Document doc) {
        List<String> list = new LinkedList<>();
        list.add(ReticulateNetwork.EQUALANGLE60);
        list.add(ReticulateNetwork.EQUALANGLE120);
        list.add(ReticulateNetwork.EQUALANGLE180);
        list.add(ReticulateNetwork.EQUALANGLE360);
        list.add(ReticulateNetwork.RECTANGULARPHYLOGRAM);
        list.add(ReticulateNetwork.RECTANGULARCLADOGRAM);
        return list;
    }


    public int getOptionPercentOffset() {
        return optionPercentOffset;
    }

    public void setOptionPercentOffset(int optionPercentOffset) {
        this.optionPercentOffset = Math.max(0, Math.min(100, optionPercentOffset));
    }

}
