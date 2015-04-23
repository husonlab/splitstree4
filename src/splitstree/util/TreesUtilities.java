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

package splitstree.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.NotOwnerException;
import splitstree.algorithms.trees.TreeSelector;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.util.*;

/**
 * some computations on trees
 *
 * @author huson
 *         Date: 29-Feb-2004
 */
public class TreesUtilities {
    /**
     * determines whether every pair of taxa occur together in some tree
     *
     * @param taxa
     * @param trees
     * @return returns true, if every pair of taxa occur together in some  tree
     */
    static public boolean hasAllPairs(Taxa taxa, Trees trees) {
        int numPairs = (taxa.getNtax() * (taxa.getNtax() - 1)) / 2;

        BitSet seen = new BitSet();

        for (int which = 1; which <= trees.getNtrees(); which++) {
            BitSet support = trees.getSupport(taxa, which).getBits();
            for (int i = support.nextSetBit(1); i > 0; i = support.nextSetBit(i + 1)) {
                for (int j = support.nextSetBit(i + 1); j > 0; j = support.nextSetBit(j + 1)) {
                    seen.set(i + taxa.getNtax() * j, true);
                    if (seen.cardinality() == numPairs)
                        return true; // seen all possible pairs
                }
            }
        }
        return false;
    }

    /**
     * given a list of trees that has the "All Pairs" properties, returns the average
     * distance between any two taxa
     *
     * @param taxa
     * @param trees
     * @return distance between any two taxa
     */
    public static Distances getAveragePairwiseDistances(Taxa taxa, Trees trees) {
        Distances distances = new Distances(taxa.getNtax());
        int[][] count = new int[taxa.getNtax() + 1][taxa.getNtax() + 1];
        // number of trees that contain two given taxa

        TreeSelector selector = new TreeSelector();

        for (int which = 1; which <= trees.getNtrees(); which++) {
            Taxa tmpTaxa = (Taxa) taxa.clone();
            selector.setOptionWhich(which);
            Splits splits = selector.apply(null, tmpTaxa, trees); // modifies tmpTaxa, too!
            for (int a = 1; a <= tmpTaxa.getNtax(); a++)
                for (int b = 1; b <= tmpTaxa.getNtax(); b++) {
                    int i = taxa.indexOf(tmpTaxa.getLabel(a)); // translate numbering
                    int j = taxa.indexOf(tmpTaxa.getLabel(b));
                    count[i][j]++;
                    count[j][i]++;
                }

            for (int s = 1; s <= splits.getNsplits(); s++) {
                BitSet A = splits.get(s).getBits();
                BitSet B = splits.get(s).getComplement(tmpTaxa.getNtax()).getBits();
                for (int a = A.nextSetBit(1); a > 0; a = A.nextSetBit(a + 1)) {
                    for (int b = B.nextSetBit(1); b > 0; b = B.nextSetBit(b + 1)) {
                        int i = taxa.indexOf(tmpTaxa.getLabel(a)); // translate numbering
                        int j = taxa.indexOf(tmpTaxa.getLabel(b));
                        distances.set(i, j, distances.get(i, j) + splits.getWeight(s));
                        distances.set(j, i, distances.get(i, j));
                    }
                }
            }
        }
        // divide by count
        for (int i = 1; i <= taxa.getNtax(); i++) {
            for (int j = 1; j <= taxa.getNtax(); j++) {
                if (count[i][j] > 0)
                    distances.set(i, j, distances.get(i, j) / count[i][j]);
                else
                    distances.set(i, j, 100); // shouldn't ever happen!
            }
        }
        return distances;
    }

    /**
     * produces a tree from a set of compatible splits
     *
     * @param taxa
     * @param splits
     * @param node2taxon
     * @return
     * @throws SplitsException
     */
    public static PhyloTree treeFromSplits(Taxa taxa, Splits splits, Map node2taxon)
            throws SplitsException {
        if (node2taxon == null)
            node2taxon = new HashMap();

        if (node2taxon.size() == 0) {
            for (int t = 1; t <= taxa.getNtax(); t++)
                node2taxon.put(taxa.getLabel(t), taxa.getLabel(t));
        }

        PhyloTree tree = new PhyloTree();
        if (!SplitsUtilities.isCompatible(splits)) {
            throw new SplitsException("incompatible splits");
        }
        Node root = tree.newNode(taxa.getTaxaSet().clone());

        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.get(s);
            if (split.get(1))
                split = split.getComplement(taxa.getNtax());
            addSplit(root, null, split, splits.getWeight(s), tree);
        }

        // label the nodes:
        try {
            Iterator it = tree.nodeIterator();
            while (it.hasNext()) {
                Node v = (Node) it.next();
                BitSet bs = ((TaxaSet) tree.getInfo(v)).getBits();
                int taxonId = 0;
                if (bs.cardinality() == taxa.getNtax()) // is leaf for taxon 1
                    taxonId = 1;
                else if (bs.cardinality() == 1) // other leaf
                    taxonId = bs.nextSetBit(1);
                if (taxonId != 0) {
                    String taxonLabel = taxa.getLabel(taxonId);
                    String nodeLabel = null;
                    for (Object o : node2taxon.keySet()) {
                        nodeLabel = (String) o;
                        if (node2taxon.get(nodeLabel).equals(taxonLabel))
                            break;
                    }
                    if (nodeLabel != null && !nodeLabel.equals(""))
                        tree.setLabel(v, nodeLabel);
                }

            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return tree;
    }

    /**
     * adds a split to a tree
     *
     * @param v     current node
     * @param e     edge
     * @param split split to be inserted
     * @param wgt   weight
     * @param tree  tree
     */
    private static void addSplit(Node v, Edge e, TaxaSet split, float wgt, PhyloTree tree) {
        try {
            List farSide = new LinkedList();
            Iterator it = tree.getAdjacentEdges(v);
            while (it.hasNext()) {
                Edge f = (Edge) it.next();
                if (f != e) {
                    Node w = tree.getOpposite(v, f);

                    TaxaSet wSet = (TaxaSet) tree.getInfo(w);

                    if (wSet.contains(split)) {  // move further down tree
                        addSplit(w, f, split, wgt, tree);
                        return;
                    } else if (wSet.intersects(split))
                        farSide.add(f);
                }
            }

            // here we insert the new edge:
            TaxaSet uSet = new TaxaSet();
            uSet.or(split);
            Node u = tree.newNode(uSet);
            Edge vu = tree.newEdge(v, u);
            tree.setWeight(vu, wgt);
            for (Object aFarSide : farSide) {
                Edge vw = (Edge) aFarSide;
                Node w = tree.getOpposite(v, vw);
                Edge uw = tree.newEdge(u, w);
                tree.setWeight(uw, tree.getWeight(vw));
                tree.deleteEdge(vw);
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * verify that tree, translation and taxa fit together
     *
     * @param tree
     * @param translate
     * @param taxa
     * @param allowAddTaxa if taxa-block is missing taxa in tree, do we allow them to be added to taxa block?
     * @throws SplitsException
     */
    public static void verifyTree(PhyloTree tree, Map translate, Taxa taxa, boolean allowAddTaxa) throws SplitsException {
        TaxaSet seen = new TaxaSet();
        Iterator it = tree.nodeIterator();
        while (it.hasNext()) {
            try {
                String nodeLabel = tree.getLabel((Node) it.next());
                if (nodeLabel != null) {
                    String taxonLabel = (String) translate.get(nodeLabel);
                    if (taxonLabel == null)
                        throw new SplitsException("Node-label not contained in translate: "
                                + nodeLabel);
                    if (taxa.indexOf(taxonLabel) == -1) {
                        if (allowAddTaxa)
                            taxa.add(taxonLabel);
                        else {
                            Taxa.show("current taxon block", taxa);
                            throw new SplitsException("Taxon-label not contained in taxa-block: "
                                    + taxonLabel);
                        }
                    }
                    seen.set(taxa.indexOf(taxonLabel));
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        }
        if (seen.cardinality() != taxa.getNtax())
            throw new SplitsException("Taxa " + taxa + " and seen <" + seen + "> differ");
    }

    /**
     * sets the node2taxa and taxon2node maps for a tree
     *
     * @param tree
     * @param taxa
     */
    public static void setNode2taxa(PhyloTree tree, Taxa taxa) {
        tree.clearTaxon2Node();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            tree.clearNode2Taxa(v);
            String label = tree.getLabel(v);
            if (label != null) {
                int id = taxa.indexOf(label);
                if (id > 0) {
                    tree.setNode2Taxa(v, id);
                    tree.setTaxon2Node(id, v);
                }
            }
        }
    }

    /**
     * determines whether a given set of trees is partial
     *
     * @param taxa
     * @param trees
     * @return true if partial, false if taxa set is identical for all trees
     */
    public static boolean computeArePartialTrees(Taxa taxa, Trees trees) {
        if (trees.getNtrees() > 1) {
            int ntax = trees.getTaxaSet(1).cardinality();
            // usually comparing sizes will tell us whether trees are partial:
            for (int t = 2; t <= trees.getNtrees(); t++) {
                if (ntax != trees.getTaxaSet(t).cardinality()) {
                    System.err.println("Partial trees: Number of taxa in tree(1): " + ntax + ", number of taxa in tree(" + t + "): " + trees.getTaxaSet(t).cardinality());
                    return true;
                }
            }

            TaxaSet taxaSet = trees.getTaxaSet(1);
            for (int t = 2; t <= trees.getNtrees(); t++) {
                TaxaSet treeTaxa = trees.getTaxaSet(t);
                if (!taxaSet.equals(treeTaxa)) {
                    System.err.println("Partial trees:");
                    System.err.println("Taxa in tree(1): " + treeTaxa);
                    System.err.println("Taxa in tree(" + t + "): " + taxaSet);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Assumming the given  tree contains only taxa mentioned in taxa,
     * gets the set of all taxon ids contained in the tree
     *
     * @param taxa  the set of original taxa
     * @param trees the set of trees
     * @param which the index of the tree
     */
    static public TaxaSet getTaxaPresentInPartialTree(Taxa taxa, Trees trees, int which) {
        TaxaSet result = new TaxaSet();
        PhyloTree tree = trees.getTree(which);
        Set nodeLabels = tree.getNodeLabels();
        for (Object nodeLabel1 : nodeLabels) {
            String nodeLabel = (String) nodeLabel1;
            result.set(taxa.indexOf((String) trees.getTranslate().get(nodeLabel)));
        }
        return result;
    }


    /**
     * Converts a PhyloTree in a trees block to splits, given taxa.
     * <p/>
     * This code was extracted from TreeSelector.java
     *
     * @param trees
     * @param which
     * @param taxa
     * @return splits
     */
    public static Splits convertTreeToSplits(Trees trees, int which, Taxa taxa) {
        return convertTreeToSplits(trees, which, taxa, false);
    }

    /**
     * Converts a PhyloTree in a trees block to splits, given taxa.
     * <p/>
     * This code was extracted from TreeSelector.java
     *
     * @param trees
     * @param which
     * @param taxa
     * @param skipNegativeSplitIds don't convert edges with negative split ids
     * @return splits
     */
    public static Splits convertTreeToSplits(Trees trees, int which, Taxa taxa, boolean skipNegativeSplitIds) {
        Splits splits = new Splits(taxa.getNtax());
        PhyloTree tree = trees.getTree(which);

        // choose an arbitrary labeled root
        Node root = null;
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (trees.getTaxaForLabel(taxa, tree.getLabel(v)).cardinality() > 0
                    && tree.getDegree(v) == 1) {
                root = v;
                break;
            }
        }
        if (root == null) // empty tree?
            return splits;

        tree2splitsRec(root, null, trees, which, taxa, splits, skipNegativeSplitIds);


        try {
            SplitsUtilities.verifySplits(splits, taxa);
        } catch (SplitsException ex) {
            splits = null;
        }

        return splits;
    }

    /**
     * recursively extract split froms tree
     *
     * @param v
     * @param e
     * @param trees
     * @param which
     * @param taxa
     * @param splits
     * @return
     * @throws NotOwnerException
     */
    private static TaxaSet tree2splitsRec(Node v, Edge e, Trees trees, int which,
                                          Taxa taxa, Splits splits, boolean skipNegativeSplitIds) throws NotOwnerException {
        PhyloTree tree = trees.getTree(which);
        TaxaSet e_taxa = trees.getTaxaForLabel(taxa, tree.getLabel(v));

        Iterator edges = tree.getAdjacentEdges(v);
        while (edges.hasNext()) {
            Edge f = (Edge) edges.next();

            if (f != e) {
                TaxaSet f_taxa = tree2splitsRec(tree.getOpposite(v, f), f, trees, which, taxa, splits, skipNegativeSplitIds);
                if (tree.getConfidence(f) != 1)
                    splits.getFormat().setConfidences(true);

                if (!skipNegativeSplitIds || tree.getSplit(f) >= 0)
                    splits.getSplitsSet().add(f_taxa, (float) tree.getWeight(f), (float) tree.getConfidence(f));
                e_taxa.set(f_taxa);
            }
        }
        return e_taxa;
    }


    /**
     * Constructs a document containing one taxon for every tree and a distances matrix
     * with the weighted or unweighted Robinson Foulds distance between the trees.
     * Assumes that all taxa appear in all trees.
     *
     * @param taxa       Set of taxa
     * @param trees      Set of trees on the same taxon set
     * @param useWeights Use weighted RF, rather than just count the number of partitions
     * @return document containin taxa and distances block only.
     */
    Document pairwiseRFdistances(Taxa taxa, Trees trees, boolean useWeights) {

        //TODO: Use more efficient algorithm.

        int ntrees = trees.getNtrees();
        Taxa treeTaxa = new Taxa();
        for (int i = 1; i <= ntrees; i++) {
            treeTaxa.add(trees.getName(i));
        }
        Distances dist = new Distances(ntrees);
        SplitMatrix splitMatrix = new SplitMatrix(trees, taxa);

        for (int i = 1; i <= ntrees; i++) {
            for (int j = i + 1; j <= ntrees; j++) {
                double dij = 0.0;
                for (int k = 1; k <= splitMatrix.getNsplits(); k++) {
                    double w1 = splitMatrix.get(k, i);
                    double w2 = splitMatrix.get(k, j);
                    if (!useWeights) {
                    }
                    double diff = Math.abs(splitMatrix.get(k, i) - splitMatrix.get(k, j));
                    dij += diff;
                }
                dist.set(i, j, dij / 2.0);
                dist.set(j, i, dij / 2.0);
            }
        }

        Document doc = new Document();
        doc.setTaxa(treeTaxa);
        doc.setDistances(dist);
        return doc;

    }


}
