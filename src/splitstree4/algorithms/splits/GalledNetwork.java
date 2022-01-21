/*
 * GalledNetwork.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.StringUtils;
import splitstree4.algorithms.splits.reticulateTree.HybridFinderWithTree;
import splitstree4.algorithms.splits.reticulateTree.LabelGraph;
import splitstree4.algorithms.splits.reticulateTree.ModifyGraph;
import splitstree4.algorithms.splits.reticulateTree.ReticulationTree;
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
 * deprecated
 * Implements the Galled network method
 */
public class GalledNetwork implements Splits2Network {
    public final boolean EXPERT = true;
    private boolean verbose = false;
    private boolean checkRoot = false;

    // known options:
    boolean optionShowSplits = false;
    String optionOutGroup = Taxa.FIRSTTAXON;
    public String optionLayout = ReticulateNetwork.EQUALANGLE120;
    private int optionMaxReticulationsPerTangle = 4;
    private int optionWhich = 0;
    private int optionMaxReticulationToSearch = 1;

    protected boolean optionShowMutations = false;
    protected boolean optionShowSequences = false;
    public int optionPercentOffset = 10;


    protected Map split2Chars = null; // map each split to one or more character positions


    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {


        if (verbose) System.err.println("Computing network...");

        // 0. if this hasn't be set explicitly, then compute for splits:
        // always need this to do edge resolution
        if (split2Chars == null) {
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

        ReticulationTree[] reticulationList = new ReticulationTree[incompComponents.length];
        TaxaSet[][] inducedTaxa2origsTaxa = new TaxaSet[incompComponents.length][];

        for (int i = 1; i < incompComponents.length; i++) {
            if (verbose) System.err.println("Processing component " + i + ":");
            // 3. for each component, compute equivalence classes of taxa and induced splits
            Taxa inducedTaxa = new Taxa();
            Splits inducedSplits = new Splits();
            TaxaSet[] induced2origTaxa = computeInducedProblem(taxa, splits, incompComponents[i], incompGraph, inducedTaxa, inducedSplits);

            for (int t = 1; t <= inducedTaxa.getNtax(); t++) {
                if (verbose) System.err.println("induced2origTaxa[" + t + "]=" + induced2origTaxa[t]);
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

            // different analysis methods:
            HybridFinderWithTree hfwt = new HybridFinderWithTree();
            LinkedList rets = hfwt.apply(inducedTaxa, inducedSplits, inducedOutGroupId, checkRoot, getOptionMaxReticulationsPerTangle(), getOptionMaxReticulationToSearch());
            if (rets.size() > 0) {
                // choose first element for now
                reticulationList[i] = (ReticulationTree) rets.get(0);
                inducedTaxa2origsTaxa[i] = induced2origTaxa;
            }
        }

        // 5. build graph
        if (splits.getCycle() == null) {
            SplitsUtilities.computeCycle(doc, taxa, splits, 0);
        }

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

        // label each edge in the graph with its split
        for (var e : graph.edges()) {
            BitSet label = new BitSet(splits.getNsplits() + 1);
            label.set(graph.getSplit(e));
            e.setInfo(label);
        }
        // label each node with its sequence if wanted
        if (optionShowSequences) LabelGraph.setSequences2NodeInfo(graph, taxa, doc.getCharacters(), split2Chars);

        // 6. find netted components in graph
        // computeNettedComps makes sure that the nettedComps at position i has ReticulationTree i in reticulationList and inducedTaxa in
        // intducedTaxa2origsTaxa at position i.
        NodeSet gateNodes = new NodeSet(graph);
        NodeSet[] nettedComps = computeNettedComps(split2incomp, graph, gateNodes, incompComponents.length);
        for (TaxaSet[] anInducedTaxa2origsTaxa : inducedTaxa2origsTaxa) {
			if (verbose) System.out.println(StringUtils.toString(anInducedTaxa2origsTaxa, ","));
        }

        int numNettedComps = 0;
        if (nettedComps != null)
            numNettedComps = nettedComps.length - 1;
        if (verbose) System.err.println("# Netted components in graph: " + numNettedComps);
        // 7. map each found configuration to graph and modify graph:
        if (nettedComps != null) {
            ModifyGraph.apply(graph, graphView, reticulationList, inducedTaxa2origsTaxa, nettedComps, gateNodes, getOptionShowSplits(),
                    optionShowSequences, taxa, splits, taxa.indexOf(getOptionOutGroup()));
        }
        // 9. label all edges if
        if (verbose)
            System.err.println("# labelSplits: " + optionShowMutations + "\tlabelSequences: " + optionShowSequences);
        LabelGraph.cleanEdges(graphView, graph, splits);

        // always reove unlabeled edges
        // todo: DHH moved this out of the if condition below, because of incorrect results
        ModifyGraph.removeUnlabeldEdges(graphView, graph, split2Chars);

        if (optionShowMutations) {
            LabelGraph.writeSplits2Edges(graph, split2Chars);
        }

        LabelGraph.cleanNodes(graphView, graph);
        if (optionShowSequences) LabelGraph.writeLabels2Nodes(graphView, graph);
        /*
        Iterator tmp = graph.nodes().iterator();
        while(tmp.hasNext()){
            Node n = (Node)tmp.next();
            graph.setLabel(n,n.toString());
        }  */
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graphView.getColor(e).equals(Color.BLUE))
                graph.setSplit(e, -1); // need this for user interaction
        }

        // if rectangular phylogram requested, compute now
        if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARPHYLOGRAM)) {
            int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

            ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
            reticulateEmbedder.computeRectangularPhylogram(taxa, splits, graphView,
                    outgroupId, true, false, getOptionPercentOffset(), false, false);
        } else if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARCLADOGRAM)) {
            int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

            ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
            reticulateEmbedder.computeRectangularCladogram(taxa, splits, graphView, outgroupId, true, false);
        }

        Network network = new Network(taxa, graphView);
        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
            network.setLayout(Network.CIRCULAR);
        else
            network.setLayout(Network.RECTILINEAR);

        return network;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable
    (Document
             doc, Taxa
             taxa, Splits
             splits) {
        return doc.isValid(taxa) && doc.isValid(splits);
    }

    /**
     * build the incompatibility graph.
     * Each node is labeled by a Pair consisting of the ID of the split and its weight
     *
     * @param splits
     * @return incompatibility graph
     */
    Graph buildIncompatibilityGraph
    (Splits
             splits) {
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
                        try {
                            graph.newEdge(split2node[s], split2node[t]);
                        } catch (IllegalSelfEdgeException e) {
                            Basic.caught(e);
                        }
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
    private NodeSet[] computeNonTrivialConnectedComponents(Graph incompGraph, int[] split2incomp) throws NotOwnerException {
        List components = new LinkedList();
        // take care of index starting at 1 (!!!)
        components.add(null);
        NodeSet unvisited = new NodeSet(incompGraph);
        unvisited.addAll();
        while (unvisited.size() > 0) {
            Node v = unvisited.getFirstElement();
            NodeSet comp = new NodeSet(incompGraph);
            visitComponentRec(v, unvisited, incompGraph, comp);
            if (comp.size() > 1) {
                components.add(comp);
                // take minus one here for index starting at one (!!!)
                int ncomp = components.size() - 1;
                for (Node u = incompGraph.getFirstNode(); u != null; u = incompGraph.getNextNode(u)) {
                    if (comp.contains(u)) {
                        int s = ((Pair) incompGraph.getInfo(u)).getFirstInt();
                        split2incomp[s] = ncomp;
                        //System.err.println("split " + s + " -> comp " + ncomp);
                    }
                }
            }
        }
        if (verbose) {
            System.out.println("components: ");
            for (Object component : components) System.out.println(component);
        }

        return (NodeSet[]) components.toArray(new NodeSet[components.size()]);
    }

    /**
     * visit a connected component
     *
     * @param v
     * @param unvisited
     * @param graph
     * @param comp
     * @throws NotOwnerException
     */
    private void visitComponentRec(Node v, NodeSet unvisited, Graph graph, NodeSet comp) {
        if (unvisited.contains(v)) {
            unvisited.remove(v);
            comp.add(v);
            for (Node w : v.adjacentNodes()) {
                visitComponentRec(w, unvisited, graph, comp);
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
    private TaxaSet[] computeInducedProblem(Taxa taxa, Splits splits, NodeSet component, Graph incompGraph, Taxa inducedTaxa, Splits inducedSplits) throws SplitsException {

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
    private NodeSet[] computeNettedComps(int[] split2incomp, PhyloSplitsGraph graph, NodeSet gateNodes, int nComps) throws NotOwnerException {
        if (verbose) System.out.println("ncomps: " + (nComps - 1));
        // components start at one (!!!!)
        NodeSet[] components = new NodeSet[nComps];
        for (int i = 1; i < nComps; i++)
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

        if (verbose) {
            for (NodeSet component : components) {
                System.err.println("netted comp: " + component);
                System.err.println("gate nodes: " + gateNodes);
            }
        }

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
    private void computeNettedCompsRec(Node v, int ncomp, EdgeSet seen, int[] split2incomp, PhyloSplitsGraph graph, NodeSet[] components) throws NotOwnerException {
        for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {
            if (!seen.contains(e) && split2incomp[graph.getSplit(e)] == ncomp) {
                seen.add(e);
                components[ncomp].add(v);
                computeNettedCompsRec(graph.getOpposite(v, e), ncomp, seen, split2incomp, graph, components);
            }
        }
    }

    //  Getter and Setter


    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription
    () {
        return "Detect and Display Instances of Reticulated Evolution (under development)";
    }

    /**
     * preserve edges in components?
     *
     * @return show splits?
     */
    public boolean getOptionShowSplits
    () {
        return optionShowSplits;
    }

    /**
     * preserve edges in components?
     *
     * @param optionShowSplits
     */
    public void setOptionShowSplits
    (boolean optionShowSplits) {
        this.optionShowSplits = optionShowSplits;
    }

    public String getOptionOutGroup
            () {
        return optionOutGroup;
    }

    public void setOptionOutGroup
            (String
                     optionOutGroup) {
        this.optionOutGroup = optionOutGroup;
    }


    public int getOptionMaxReticulationsPerTangle
            () {
        return optionMaxReticulationsPerTangle;
    }

    public void setOptionMaxReticulationsPerTangle
            (int optionMaxReticulationsPerTangle) {
        this.optionMaxReticulationsPerTangle = optionMaxReticulationsPerTangle;
    }

    public int getOptionWhich
            () {
        return optionWhich;
    }

    public void setOptionWhich
            (int optionWhich) {
        this.optionWhich = optionWhich;
    }

    public int getOptionMaxReticulationToSearch
            () {
        return optionMaxReticulationToSearch;

    }

    public void setOptionMaxReticulationToSearch
            (int optionMaxReticulationToSearch) {
        this.optionMaxReticulationToSearch = optionMaxReticulationToSearch;
    }

    public Map getSplit2Chars() {
        return split2Chars;
    }

    public void setSplit2Chars(Map split2Chars) {
        this.split2Chars = split2Chars;
    }

    public boolean getOptionShowMutations() {
        return optionShowMutations;
    }

    public void setOptionShowMutations(boolean optionShowMutations) {
        this.optionShowMutations = optionShowMutations;
    }

    public boolean getOptionShowSequences() {
        return optionShowSequences;
    }

    public void setOptionShowSequences(boolean optionShowSequences) {
        this.optionShowSequences = optionShowSequences;
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
    public List selectionOptionLayout(Document doc) {
        List list = new LinkedList();
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
