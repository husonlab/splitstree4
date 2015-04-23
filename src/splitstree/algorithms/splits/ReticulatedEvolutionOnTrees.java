/**
 * Copyright 2015, Daniel Huson and David Bryant
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package splitstree.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.NotOwnerException;
import jloda.util.Pair;
import splitstree.algorithms.splits.reticulateTree.LabelGraph;
import splitstree.algorithms.splits.reticulateTree.ModifyGraph;
import splitstree.algorithms.splits.reticulateTree.OptimizeLayout;
import splitstree.algorithms.splits.reticulateTree.ReticulationTree;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

import java.util.*;

/**
 * framework for generating reticulate evolution graphs from splits
 * The number of different taxa and splits objects used in this and the decending classes migth be a bit confusing so  we give a explenation:
 * This class uses one map of taxa:
 * The complete set of taxa is mapped onto the set of taxa needed in the netted component:
 * taxa --> inducedtaxa
 * <p/>
 * The HybridFinderWithTree* classes recive as imput the unduced taxa, but these are named taxa or more clearly orgTaxa. The generated
 * treeTaxa are again a subset of this taxa and represent a subset of taxa such that the splits set is compatible. Note that the rTaxa are alwys
 * in the notation of the orgTaxa, since they are not part of the compatible subset.
 * inducedTaxa == orgTaxa --> treeTaxa
 * <p/>
 * The reticulationTree object has to hold information and as the two maps above imply the information has to be of the inducedtaxa kind
 * inducedTaxa
 * <p/>
 * The ModifyGraph class has to handle two kind of information the taxa are the complete set of Taxa and the induced ones
 * are the induced taxa of this class
 * taxa     inducedtaxa
 *
 * @author huson, kloepper
 *         Date: 16-Sep-2004
 */
public class ReticulatedEvolutionOnTrees implements Splits2Network {
    public final boolean EXPERT = true;
    // known methods:
    static final String Naive = "NaiveFinder";
    // known options:
    String optionMethod = Naive;
    boolean optionShowSplits = false;
    String optionOutGroup = "none";
    private int optionMaxAngle = 60; // angle used to depict rooted network
    private int optionMaxReticulationsPerTangle = 4;
    private int optionShowOnly = 0;
    private int optionMaxReticulationToSearch = 1;
    //private boolean checkRoot = false;
    private boolean optimizeLayout = false;

    protected boolean applyOrdering = false;
    protected boolean labelSplits = false;
    protected boolean labelSequences = false;

    protected Map split2Chars = null; // map each split to one or more character positions


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
            System.err.println("Processing component " + i + ":");
            // 3. for each component, compute equivalence classes of taxa and induced splits
            Taxa inducedTaxa = new Taxa();
            Splits inducedSplits = new Splits();
            TaxaSet[] induced2origTaxa = computeInducedProblem(taxa, splits, incompComponents[i], incompGraph, inducedTaxa, inducedSplits);

            for (int t = 1; t <= inducedTaxa.getNtax(); t++) {
                System.err.println("induced2origTaxa[" + t + "]=" + induced2origTaxa[t]);
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

        }

        // 5. build graph
        if (splits.getCycle() == null) {
            SplitsUtilities.computeCycle(doc, taxa, splits, 0);
        }

        PhyloGraphView graphView;
        PhyloGraph graph;
        if (taxa.indexOf(getOptionOutGroup()) == -1) {
            EqualAngle ea = new EqualAngle();
            ea.apply(doc, taxa, splits);
            graphView = ea.getPhyloGraphView();
            graph = graphView.getPhyloGraph();
        } else {
            RootedEqualAngle ea = new RootedEqualAngle();
            ea.setOptionMaxAngle(getOptionMaxAngle());
            ea.setOutGroup(getOptionOutGroup(), doc);
            ea.apply(doc, taxa, splits);
            graphView = ea.getGraphView();
            graph = graphView.getPhyloGraph();

        }
        // label each edge in the graph with its split
        Iterator it = graph.edgeIterator();
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            BitSet label = new BitSet(splits.getNsplits() + 1);
            label.set(graph.getSplit(e));
            e.setInfo(label);
        }
        // label each node with its sequence if wanted
        if (labelSequences) LabelGraph.setSequences2NodeInfo(graph, taxa, doc.getCharacters(), split2Chars);

        // 6. find netted components in graph
        // computeNettedComps makes sure that the nettedComps at position i has ReticulationTree i in reticulationList and inducedTaxa in
        // intducedTaxa2origsTaxa at position i.
        NodeSet gateNodes = new NodeSet(graph);
        NodeSet[] nettedComps = computeNettedComps(split2incomp, graph, gateNodes, incompComponents.length);
        for (TaxaSet[] anInducedTaxa2origsTaxa : inducedTaxa2origsTaxa) {
            System.out.println(Basic.toString(anInducedTaxa2origsTaxa, ","));
        }

        int numNettedComps = 0;
        if (nettedComps != null)
            numNettedComps = nettedComps.length - 1;
        System.err.println("# Netted components in graph: " + numNettedComps);
        // 7. map each found configuration to graph and modify graph:
        if (nettedComps != null) {
            ModifyGraph.apply(graph, graphView, reticulationList, inducedTaxa2origsTaxa, nettedComps, gateNodes, getOptionShowSplits(),
                    labelSequences, taxa, splits, taxa.indexOf(getOptionOutGroup()));

            if (getOptionOptimizeLayout()) {
                OptimizeLayout ol = new OptimizeLayout();
                graphView = ol.apply(graph, graphView, nettedComps, inducedTaxa2origsTaxa, reticulationList, taxa, splits, doc.getCharacters(), doc.getTrees(), getOptionOutGroup(), getOptionMaxAngle());
            }
        }
        // 9. label all edges if
        System.err.println("# labelSplits: " + labelSplits + "\tlabelSequences: " + labelSequences);
        LabelGraph.cleanEdges(graphView, graph, splits);
        if (labelSplits) {
            ModifyGraph.removeUnlabeldEdges(graphView, graph, split2Chars);
            LabelGraph.writeSplits2Edges(graph, split2Chars);
        }

        LabelGraph.cleanNodes(graphView, graph);
        if (labelSequences) LabelGraph.writeLabels2Nodes(graphView, graph);
        /*
        Iterator tmp = graph.nodeIterator();
        while(tmp.hasNext()){
            Node n = (Node)tmp.next();
            graph.setLabel(n,n.toString());
        }  */
        return new Network(taxa, graphView);
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
     * build the incompatibility graph.
     * Each node is labeled by a Pair consisting of the ID of the split and its weight
     *
     * @param splits
     * @return incompatibility graph
     */
    private Graph buildIncompatibilityGraph(Splits splits) {
        Node[] split2node = new Node[splits.getNsplits() + 1];
        Graph graph = new Graph();

        try {
            for (int s = 1; s <= splits.getNsplits(); s++) {
                Pair<Integer, Float> pair = new Pair<>(s, splits.getWeight(s));
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
     * @return
     */
    private NodeSet[] computeNonTrivialConnectedComponents(Graph incompGraph, int[] split2incomp) throws NotOwnerException {
        List<NodeSet> components = new LinkedList<>();
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
        System.out.println("components: ");
        for (Object component : components) System.out.println(component);

        return components.toArray(new NodeSet[components.size()]);
    }

    /**
     * visit a connected component
     *
     * @param v
     * @param unvisited
     * @param graph
     * @param comp
     * @throws jloda.util.NotOwnerException
     */
    private void visitComponentRec(Node v, NodeSet unvisited, Graph graph, NodeSet comp) throws NotOwnerException {
        if (unvisited.contains(v)) {
            unvisited.remove(v);
            comp.add(v);
            Iterator it = graph.getAdjacentNodes(v);
            while (it.hasNext()) {
                visitComponentRec((Node) it.next(), unvisited, graph, comp);
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
    private TaxaSet[] computeInducedProblem(Taxa taxa, Splits splits, NodeSet component, Graph incompGraph, Taxa inducedTaxa, Splits inducedSplits) throws NotOwnerException, SplitsException {

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
    private NodeSet[] computeNettedComps(int[] split2incomp, PhyloGraph graph, NodeSet gateNodes, int nComps)
            throws NotOwnerException {
        System.out.println("ncomps: " + (nComps - 1));
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
        for (NodeSet component : components) {
            System.err.println("netted comp: " + component);
            System.err.println("gate nodes: " + gateNodes);
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
     * @throws jloda.util.NotOwnerException
     */
    private void computeNettedCompsRec(Node v, int ncomp, EdgeSet seen, int[] split2incomp,
                                       PhyloGraph graph, NodeSet[] components)
            throws NotOwnerException {
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
     * gets the method to use
     *
     * @return name of method
     */
    public String getOptionMethod
            () {
        return optionMethod;
    }

    /**
     * sets the method to use
     *
     * @param optionMethod
     */
    public void setOptionMethod
            (String
                    optionMethod) {
        this.optionMethod = optionMethod;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List selectionOptionMethod
            (Document
                    doc) {
        List methods = new LinkedList();
        methods.add(Naive);
        return methods;
    }

    /**
     * preserve edges in components?
     *
     * @return
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


    /**
     * get maximal angle between 0 and 90 degrees
     *
     * @return maximal angle
     */
    public int getOptionMaxAngle
            () {
        return optionMaxAngle;
    }

    /**
     * set the maximal angle between 0 and 90 degrees
     *
     * @param optionMaxAngle
     */
    public void setOptionMaxAngle
            (int optionMaxAngle) {
        this.optionMaxAngle = optionMaxAngle;
    }

    public int getOptionMaxReticulationsPerTangle
            () {
        return optionMaxReticulationsPerTangle;
    }

    public void setOptionMaxReticulationsPerTangle
            (int optionMaxReticulationsPerTangle) {
        this.optionMaxReticulationsPerTangle = optionMaxReticulationsPerTangle;
    }

    public int getOptionShowOnly
            () {
        return optionShowOnly;
    }

    public void setOptionShowOnly
            (int optionShowOnly) {
        this.optionShowOnly = optionShowOnly;
    }

    public int getOptionMaxReticulationToSearch
            () {
        return optionMaxReticulationToSearch;

    }

    public void setOptionMaxReticulationToSearch
            (int optionMaxReticulationToSearch) {
        this.optionMaxReticulationToSearch = optionMaxReticulationToSearch;
    }

    public boolean getOptionOptimizeLayout
            () {
        return optimizeLayout;
    }

    public void setOptionOptimizeLayout
            (boolean optimizeLayout) {
        this.optimizeLayout = optimizeLayout;
    }

    public Map getSplit2Chars() {
        return split2Chars;
    }

    public void setSplit2Chars(Map split2Chars) {
        this.split2Chars = split2Chars;
    }

}
