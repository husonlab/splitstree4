/*
 * SuperNetwork.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.trees;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.algorithms.additional.LeastSquaresWeights;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import java.util.*;

/**
 * compute network from partial trees
 *
 * @author huson
 * Date: 19-Feb-2004
 */
public class SuperNetwork implements Trees2Splits {
    public final static String DESCRIPTION = "Z-closure super-network from partial trees (Huson, Dezulian, Kloepper and Steel 2004)";
    private boolean optionZRule = true;
    private boolean optionLeastSquare = false;
    private boolean optionSuperTree = false;
    private int optionNumberOfRuns = 1;
    private boolean optionApplyRefineHeuristic = false;
    private int optionSeed = 0;
    private String optionEdgeWeights = TREESIZEWEIGHTEDMEAN;


    // edge weight options:
    static final String AVERAGERELATIVE = "AverageRelative";
    static final String MEAN = "Mean";
    static final String TREESIZEWEIGHTEDMEAN = "TreeSizeWeightedMean";
    static final String SUM = "Sum";
    static final String MIN = "Min";
    static final String NONE = "None";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Applies the method to the given data
     *
     * @param doc   the document
     * @param taxa  the taxa (maybe set to contain all mentioned taxa)
     * @param trees a nexus trees block containing one tree
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) throws SplitsException, CanceledException {
        if (trees.getNtrees() == 0)
            return new Splits(taxa.getNtax());

        if (trees.getPartial()) // contains partial trees, most determine
            // full set of taxa
            trees.setTaxaFromPartialTrees(taxa);

        doc.notifyTasks("Z-closure", "init");

        Map[] pSplitsOfTrees = new Map[trees.getNtrees() + 1];
        // for each tree, identity map on set of splits
        TaxaSet[] supportSet = new TaxaSet[trees.getNtrees() + 1];
        Set allPSplits = new HashSet();

        doc.notifySubtask("extracting partial splits from trees");
        doc.notifySetMaximumProgress(trees.getNtrees());
        for (int which = 1; which <= trees.getNtrees(); which++) {
            try {
                doc.notifySetProgress(which);
                pSplitsOfTrees[which] = new HashMap();
                supportSet[which] = new TaxaSet();
                computePartialSplits(taxa, trees, which, pSplitsOfTrees[which], supportSet[which]);
                for (Object o : pSplitsOfTrees[which].keySet()) {
                    PartialSplit ps = (PartialSplit) o;
                    if (ps.isNonTrivial()) {
                        allPSplits.add(ps.clone());
                        doc.notifySetProgress(which);
                    }
                }
            } catch (NotOwnerException e) {
                Basic.caught(e);
            }
        }
        Splits splits = new Splits(taxa.getNtax());

        if (getOptionZRule()) {
            computeClosureOuterLoop(doc, taxa, allPSplits);
        }

        if (getOptionApplyRefineHeuristic()) {
            doc.notifySubtask("Refinement heuristic");
            applyRefineHeuristic(doc, allPSplits);
        }

        doc.notifySubtask("collecting full splits");
        doc.notifySetMaximumProgress(allPSplits.size());
        int count = 0;
        for (Object allPSplit : allPSplits) {
            doc.notifySetProgress(++count);
            PartialSplit ps = (PartialSplit) allPSplit;
            int size = ps.getXsize();

            // for now, keep all splits of correct size
            if (size == taxa.getNtax()) {
                boolean ok = true;
                if (getOptionSuperTree()) {
                    for (int t = 1; ok && t <= trees.getNtrees(); t++) {
                        Map pSplits = (pSplitsOfTrees[t]);
                        TaxaSet support = supportSet[t];
                        PartialSplit induced = ps.getInduced(support);
                        if (induced != null && !pSplits.containsKey(induced))
                            ok = false;     // found a tree that doesn't contain the induced split
                    }
                }
                if (ok)
                    splits.add(ps.getA());
            }
        }

        // add all missing trivial splits
        for (int t = 1; t <= taxa.getNtax(); t++) {
            TaxaSet ts = new TaxaSet();
            ts.set(t);
            PartialSplit ps = new PartialSplit(ts);
            ps.setComplement(taxa.getTaxaSet());
            if (!allPSplits.contains(ps))
                splits.getSplitsSet().add(ps.getA());
        }

        if (getOptionEdgeWeights().equals(AVERAGERELATIVE)) {
            setWeightAverageReleativeLength(doc, pSplitsOfTrees, supportSet, taxa, splits);
        } else if (!getOptionEdgeWeights().equals(NONE)) {
            setWeightsConfidences(doc, pSplitsOfTrees, supportSet, taxa, splits);
        }

        if (getNoOptionLeastSquare()) {
            if (!TreesUtilities.hasAllPairs(taxa, trees)) {
                new Alert("Partial trees don't have the 'All Pairs' property,\n" +
                        "can't apply Least Squares");
                setNoOptionLeastSquare(false);
            } else {
                Distances distances = TreesUtilities.getAveragePairwiseDistances(taxa, trees);
                LeastSquaresWeights leastSquares = new LeastSquaresWeights();

                Document tmpDoc = new Document();
                tmpDoc.setTaxa(taxa);
                tmpDoc.setDistances(distances);
                tmpDoc.setSplits(splits);
                tmpDoc.setProgressListener(doc.getProgressListener());
                if (!leastSquares.isApplicable(tmpDoc, taxa, splits))
                    new Alert("Least Squares not applicable");
                else
                    leastSquares.apply(tmpDoc, taxa, splits);
            }
        }
        doc.notifySetProgress(100);   //set progress to 100%
        // pd.close();								//get rid of the progress listener
        // doc.setProgressListener(null);
        return splits;
    }

    /**
     * set the weight to the mean weight of all projections of this split and confidence to
     * the count of trees containing a projection of the split
     *
     * @param pSplits
     * @param supportSet
     * @param taxa
     * @param splits
     */
    private void setWeightsConfidences(Document doc, Map[] pSplits,
                                       TaxaSet[] supportSet, Taxa taxa, Splits splits) throws CanceledException {
        for (int s = 1; s <= splits.getNsplits(); s++) {
            doc.notifySetProgress(-1);
            PartialSplit current = new PartialSplit(splits.get(s),
                    splits.get(s).getComplement(taxa.getNtax()));

            float min = 1000000;
            float sum = 0;
            float weighted = 0;
            float confidence = 0;
            int total = 0;
            for (int t = 1; t < pSplits.length; t++) {
                PartialSplit projection = current.getInduced(supportSet[t]);
                if (projection != null)  // split cuts support set of tree t
                {
                    if (pSplits[t].containsKey(projection)) {
                        float cur = ((PartialSplit) pSplits[t].get(projection)).getWeight();
                        weighted += supportSet[t].cardinality() * cur;
                        if (cur < min)
                            min = cur;
                        sum += cur;
                        confidence += supportSet[t].cardinality() *
                                ((PartialSplit) pSplits[t].get(projection)).getConfidence();
                    }
                    total += supportSet[t].cardinality();
                }
            }

            float value = 1;
            switch (getOptionEdgeWeights()) {
                case MIN:
                    value = min;
                    break;
                case MEAN:
                    value = weighted / total;
                    break;
                case TREESIZEWEIGHTEDMEAN:
                    value = sum / total;
                    break;
                case SUM:
                    value = sum;
                    break;
            }
            splits.setWeight(s, value);
            splits.setConfidence(s, total);
        }
    }

    /**
     * sets the weight of a split in the network as the average relative length of the edge
     * in the input trees
     *
     * @param doc
     * @param pSplits
     * @param supportSet
     * @param taxa
     * @param splits
     * @throws CanceledException
     */
    private void setWeightAverageReleativeLength(Document doc, Map[] pSplits,
                                                 TaxaSet[] supportSet, Taxa taxa, Splits splits) throws
            CanceledException {
        // compute average of weights and num of edges for each input tree
        float[] averageWeight = new float[pSplits.length];
        int[] numEdges = new int[pSplits.length];

        for (int t = 1; t < pSplits.length; t++) {
            numEdges[t] = pSplits[t].size();
            float sum = 0;
            for (Object o : pSplits[t].keySet()) {
                PartialSplit ps = (PartialSplit) o;
                sum += ps.getWeight();
            }
            averageWeight[t] = sum / numEdges[t];
        }

        // consider each network split in turn:
        for (int s = 1; s <= splits.getNsplits(); s++) {
            doc.notifySetProgress(-1);
            PartialSplit current = new PartialSplit(splits.get(s),
                    splits.get(s).getComplement(taxa.getNtax()));

            BitSet activeTrees = new BitSet(); // trees that contain projection of
            // current split

            for (int t = 1; t < pSplits.length; t++) {
                PartialSplit projection = current.getInduced(supportSet[t]);
                if (projection != null && pSplits[t].containsKey(projection)) {
                    activeTrees.set(t);
                }
            }

            float weight = 0;
            for (int t = activeTrees.nextSetBit(1); t >= 0; t = activeTrees.nextSetBit(t + 1)) {
                PartialSplit projection = current.getInduced(supportSet[t]);

                weight += ((PartialSplit) pSplits[t].get(projection)).getWeight()
                        / averageWeight[t];
            }
            weight /= activeTrees.cardinality();
            splits.setWeight(s, weight);
        }
    }

    /**
     * returns the set of all partial splits in the given tree
     *
     * @param trees
     * @param which
     * @param pSplitsOfTree partial splits are returned here
     * @param support       supporting taxa are returned here
     */
    private void computePartialSplits(Taxa taxa, Trees trees, int which,
                                      Map pSplitsOfTree, TaxaSet support) throws NotOwnerException {
        List list = new LinkedList(); // list of (onesided) partial splits
        Node v = trees.getTree(which).getFirstNode();
        computePSplitsFromTreeRecursively(v, null, trees, taxa, list, which, support);

        for (Object aList : list) {
            PartialSplit ps = (PartialSplit) aList;
            ps.setComplement(support);
            pSplitsOfTree.put(ps, ps);
        }
    }

    // recursively compute the splits:

    private TaxaSet computePSplitsFromTreeRecursively(Node v, Edge e, Trees trees,
                                                      Taxa taxa, List list, int which, TaxaSet seen) throws NotOwnerException {
        PhyloTree tree = trees.getTree(which);
        TaxaSet e_taxa = trees.getTaxaForLabel(taxa, tree.getLabel(v));
        seen.or(e_taxa);

        for (Edge f : v.adjacentEdges()) {
            if (f != e) {
                TaxaSet f_taxa = computePSplitsFromTreeRecursively(tree.getOpposite(v, f), f, trees,
                        taxa, list, which, seen);
                PartialSplit ps = new PartialSplit(f_taxa);
                ps.setWeight((float) tree.getWeight(f));
                list.add(ps);
                e_taxa.set(f_taxa);
            }
        }
        return e_taxa;
    }

    Random rand = null;

    /**
     * runs the closure method. Does this multiple times, if desired
     *
     * @param doc
     * @param taxa
     * @param partialSplits
     * @throws CanceledException
     */
    private void computeClosureOuterLoop(Document doc, Taxa taxa, Set partialSplits) {
        this.rand = new Random(this.optionSeed);

        try {
            Set allEverComputed = new HashSet(partialSplits);

            for (int i = 0; i < this.optionNumberOfRuns; i++) {
                doc.notifySubtask("compute closure" + (i == 0 ? "" : "(" + (i + 1) + ")"));

                Set clone = new LinkedHashSet(partialSplits);

                {
                    Vector tmp = new Vector(clone);
                    Collections.shuffle(tmp, rand);
                    clone = new LinkedHashSet(tmp);
                    computeClosure(doc, clone);
                }

                allEverComputed.addAll(clone);

            }
            partialSplits.clear();
            partialSplits.addAll(allEverComputed);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * gets the number of full splits
     *
     * @param numAllTaxa
     * @param partialSplits
     * @return number of full splits
     */
    public int getNumberOfFullSplits(int numAllTaxa, Set partialSplits) {
        int nfs = 0;
        for (Object partialSplit1 : partialSplits) {
            PartialSplit partialSplit = (PartialSplit) partialSplit1;
            if (partialSplit.getXsize() == numAllTaxa) nfs++;
        }
        return nfs;
    }


    /**
     * computes the split closure obtained using the zig-zap rule
     *
     * @param doc           the document
     * @param partialSplits
     */
    private void computeClosure(Document doc, Set partialSplits) throws CanceledException {

        PartialSplit[] splits;
        Set seniorSplits = new LinkedHashSet();
        Set activeSplits = new LinkedHashSet();
        Set newSplits = new LinkedHashSet();
        {
            splits = new PartialSplit[partialSplits.size()];
            Iterator it = partialSplits.iterator();
            int pos = 0;
            while (it.hasNext()) {
                splits[pos] = (PartialSplit) it.next();
                seniorSplits.add(pos);
                doc.notifySetProgress(-1);
                pos++;
            }
        }

        // init:
        {
            for (int pos1 = 0; pos1 < splits.length; pos1++) {

                for (int pos2 = pos1 + 1; pos2 < splits.length; pos2++) {
                    PartialSplit ps1 = splits[pos1];
                    PartialSplit ps2 = splits[pos2];
                    PartialSplit qs1 = new PartialSplit();
                    PartialSplit qs2 = new PartialSplit();
                    if (PartialSplit.applyZigZagRule(ps1, ps2, qs1, qs2)) {
                        splits[pos1] = qs1;
                        splits[pos2] = qs2;
                        newSplits.add(pos1);
                        newSplits.add(pos2);
                    }
                    doc.notifySetProgress(-1);
                }
            }
        }

        // main loop:
        {
            while (newSplits.size() != 0) {
                seniorSplits.addAll(activeSplits);
                activeSplits = newSplits;
                newSplits = new HashSet();

                Iterator it1 = seniorSplits.iterator();
                while (it1.hasNext()) {
                    Integer pos1 = ((Integer) it1.next());

                    for (Object activeSplit : activeSplits) {
                        Integer pos2 = ((Integer) activeSplit);
                        PartialSplit ps1 = splits[pos1];
                        PartialSplit ps2 = splits[pos2];
                        PartialSplit qs1 = new PartialSplit();
                        PartialSplit qs2 = new PartialSplit();
                        if (PartialSplit.applyZigZagRule(ps1, ps2, qs1, qs2)) {
                            splits[pos1] = qs1;
                            splits[pos2] = qs2;
                            newSplits.add(pos1);
                            newSplits.add(pos2);
                        }
                        doc.notifySetProgress(-1);
                    }
                }
                it1 = activeSplits.iterator();
                while (it1.hasNext()) {
                    Integer pos1 = ((Integer) it1.next());

                    for (Object activeSplit : activeSplits) {
                        Integer pos2 = ((Integer) activeSplit);
                        PartialSplit ps1 = splits[pos1];
                        PartialSplit ps2 = splits[pos2];
                        PartialSplit qs1 = new PartialSplit();
                        PartialSplit qs2 = new PartialSplit();
                        if (PartialSplit.applyZigZagRule(ps1, ps2, qs1, qs2)) {
                            splits[pos1] = qs1;
                            splits[pos2] = qs2;
                            newSplits.add(pos1);
                            newSplits.add(pos2);
                        }
                        doc.notifySetProgress(-1);
                    }
                }
            }
        }

        partialSplits.clear();
        Iterator it = seniorSplits.iterator();
        while (it.hasNext()) {
            Integer pos1 = (Integer) it.next();
            partialSplits.add(splits[pos1]);
        }
        it = activeSplits.iterator();
        while (it.hasNext()) {
            Integer pos1 = (Integer) it.next();
            partialSplits.add(splits[pos1]);
        }
    }

    /**
     * applies a simple refinement heuristic
     *
     * @param doc
     * @param partialSplits
     * @throws CanceledException
     */
    private void applyRefineHeuristic(Document doc, Set partialSplits) throws CanceledException {


        for (int i = 1; i <= 10; i++) {
            int count = 0;
            doc.notifySetMaximumProgress(partialSplits.size());

            PartialSplit[] splits = new PartialSplit[partialSplits.size()];
            splits = (PartialSplit[]) partialSplits.toArray(splits);

            for (int a = 0; a < splits.length; a++) {
                doc.notifySetMaximumProgress(a);
                final PartialSplit psa = splits[a];
                for (int p = 1; p <= 2; p++) {
                    final TaxaSet Aa, Ba;
                    if (p == 1) {
                        Aa = psa.getA();
                        Ba = psa.getB();
                    } else {
                        Aa = psa.getB();
                        Ba = psa.getA();
                    }
                    for (int b = a + 1; b < splits.length; b++) {
                        final PartialSplit psb = splits[b];
                        for (int q = 1; q <= 2; q++) {
                            final TaxaSet Ab, Bb;
                            if (q == 1) {
                                Ab = psb.getA();
                                Bb = psb.getB();
                            } else {
                                Ab = psb.getB();
                                Bb = psb.getA();
                            }
                            if (Aa.intersects(Ab)
                                    && !Ba.intersects(Ab) && !Bb.intersects(Aa)
                                    && Ba.intersects(Bb)) {
                                PartialSplit ps = new PartialSplit(TaxaSet.union(Aa, Ab), TaxaSet.union(Ba, Bb));
                                if (!partialSplits.contains(ps)) {
                                    partialSplits.add(ps);
                                    count++;
                                }
                            }
                        }
                    }
                    /*
                    if (psa.getA().cardinality() == 2) {
                        Aa = psa.getA();
                        Ba = psa.getB();
                    } else if (psa.getB().cardinality() == 2) {
                        Aa = psa.getB();
                        Ba = psa.getA();
                    } else
                        continue;
                    for (int b = a + 1; b < splits.length; b++) {
                        final PartialSplit psb = splits[b];
                        final TaxaSet Ab, Bb;
                        if (psb.getA().cardinality() == 2) {
                            Ab = psb.getA();
                            Bb = psb.getB();
                        } else if (psb.getB().cardinality() == 2) {
                            Ab = psb.getB();
                            Bb = psb.getA();
                        } else
                            continue;
                        if (TaxaSet.intersection(Aa, Ab).cardinality() == 1
                                && Ba.intersects(Ab) == false && Bb.intersects(Aa) == false
                                && Ba.intersects(Bb) == true) {
                            PartialSplit ps=new PartialSplit(
                                    TaxaSet.union(Aa, Ab), TaxaSet.union(Ba, Bb));
                            if(partialSplits.contains(ps)==false)
                            {
                                partialSplits.add(ps);
                                count++;
                            }
                        */

                }
            }
            System.err.println("# Refinement heuristic [" + i + "] added " + count + " partial splits");
            if (count == 0)
                break;
        }
    }


    public boolean getOptionZRule() {
        return optionZRule;
    }

    public void setOptionZRule(boolean optionZRule) {
        this.optionZRule = optionZRule;
    }

    public boolean getNoOptionLeastSquare() {
        return optionLeastSquare;
    }

    public void setNoOptionLeastSquare(boolean optionLeastSquare) {
        this.optionLeastSquare = optionLeastSquare;
    }

    /**
     * which seed it to be used for the random runs ?
     *
     * @return optionRandomRunSeed
     */
    public int getNoOptionSeed() {
        return this.optionSeed;
    }

    public void setNoOptionSeed(int optionSeed) {
        this.optionSeed = optionSeed;
    }


    /**
     * how many runs with random permutations of the input splits shall be done ?
     *
     * @return number of runs to be done
     */
    public int getOptionNumberOfRuns() {
        return this.optionNumberOfRuns;
    }

    public void setOptionNumberOfRuns(int optionNumberOfRuns) {
        this.optionNumberOfRuns = Math.max(1, optionNumberOfRuns);
    }


    /**
     * do we want to force the resulting split system to have the strong nduction proerty?
     * The strong induction property is that if an output split induces a proper split on some input
     * taxon set, then that induced split is contained in the input tree
     *
     * @return true, if option is set
     */
    public boolean getOptionSuperTree() {
        return optionSuperTree;
    }

    public void setOptionSuperTree(boolean optionSuperTree) {
        this.optionSuperTree = optionSuperTree;
    }

    public String getOptionEdgeWeights() {
        return optionEdgeWeights;
    }

    public void setOptionEdgeWeights(String optionEdgeWeights) {
        this.optionEdgeWeights = optionEdgeWeights;
    }


    /**
     * return the possible chocies for optionEdgeWeights
     *
     * @param doc
     * @return list of choices
     */
    public List selectionOptionEdgeWeights(Document doc) {
        List list = new LinkedList();
        list.add(AVERAGERELATIVE);
        list.add(MEAN);
        list.add(TREESIZEWEIGHTEDMEAN);
        list.add(SUM);
        list.add(MIN);
        list.add(NONE);
        return list;
    }

    public boolean getOptionApplyRefineHeuristic() {
        return optionApplyRefineHeuristic;
    }

    public void setOptionApplyRefineHeuristic(boolean optionApplyRefineHeuristic) {
        this.optionApplyRefineHeuristic = optionApplyRefineHeuristic;
    }
}

