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

package splitstree.algorithms.characters;

import jloda.util.Basic;
import jloda.util.Pair;
import splitstree.algorithms.distances.NJ;
import splitstree.algorithms.splits.NoGraph;
import splitstree.algorithms.trees.NoSplits;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Assumptions;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.CGVizWriter;
import splitstree.util.TreeCollector;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * DCM-based tree reconstruction
 *
 * @author huson
 *         Date: 04-Nov-2004
 */

public class DCMPartialTrees implements Characters2Trees {
    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "DCM phylogeny on sparse alignment (Halpern, Huson, Yooseph, in preparation)";
    private int optionMinOverlap = 100;
    private int optionMinCoverage = 1;
    private int optionMinBaseProblemSize = 4;
    private String optionBaseCharactersMethod = Basic.getShortName(Hamming.class);
    private String optionBaseCharactersMethodParameters = "";
    private String optionBaseDistancesMethod = Basic.getShortName(NJ.class);
    private String optionBaseDistanceMethodParameters = "";
    private String optionCGVizFile = "dcm.cgv";
    private boolean optionSelectOptimalTrees = false;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc.isValid(taxa) && doc.isValid(chars);
    }


    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed splits
     */
    public Trees apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        doc.notifyTasks("DCMPartialTrees", "determine start-end of fragments");
        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(-1);

        // setup cgviz file, if desired
        CGVizWriter cgviz = new CGVizWriter(getOptionCGVizFile().trim());
        // will only write if file can be opened

        cgviz.begin("DATA", "sequence", 1);
        cgviz.add("ntax=" + taxa.getNtax() + " nchars=" + chars.getNchar() + ": 1 " + chars.getNchar());
        cgviz.end();


        int[] taxon2start = new int[taxa.getNtax() + 1];
        int[] taxon2end = new int[taxa.getNtax() + 1];
        determineStartEndPosition(taxa, chars, taxon2start, taxon2end);
        int[] ordering = computeEliminationScheme(taxa, taxon2end);

        // output fragments in cgviz format
        cgviz.begin("DATA", "original_fragments", 2);
        for (int t = 1; t <= taxa.getNtax(); t++) {
            cgviz.add("t=" + t + " name=" + taxa.getLabel(t) + " start=" +
                    taxon2start[t] + " end=" + taxon2end[t] + " length=" +
                    (taxon2end[t] - taxon2start[t] + 1) + ": " + taxon2start[t] + " " + t +
                    " " + taxon2end[t] + " " + t);
        }
        cgviz.end();
        cgviz.begin("DATA", "ordered_fragments", 2);
        for (int i = 0; i < taxa.getNtax(); i++) {
            int t = ordering[i];
            cgviz.add("t=" + t + " rank=" + (i + 1) + " name=" + taxa.getLabel(t) + " start=" +
                    taxon2start[t] + " end=" + taxon2end[t] + " length=" +
                    (taxon2end[t] - taxon2start[t] + 1) + ": " + taxon2start[t] + " " + (i + 1) +
                    " " + taxon2end[t] + " " + (i + 1));
        }
        cgviz.end();


        doc.notifyTasks("DCMPartialTrees", "determine subproblems");
        doc.notifySetMaximumProgress(ordering.length * ordering.length);    //initialize maximum progress
        doc.notifySetProgress(0);

        // determin all subproblems
        cgviz.begin("DATA", "subproblems", 2);

        TaxaSet previousSet = new TaxaSet();
        List sets = new LinkedList();
        for (int i = 0; i < ordering.length; i++) {
            int ti = ordering[i];
            TaxaSet currentSet = new TaxaSet();
            currentSet.set(ti);
            int endi = taxon2end[ti];
            int j, tj = 0;
            for (j = i + 1; j < ordering.length; j++) {
                tj = ordering[j];
                int startj = taxon2start[tj];
                if (endi < startj + getOptionMinOverlap())
                    break; // not enough overlap
                currentSet.set(tj);
            }
            if (j > ordering.length - 1)
                j = ordering.length - 1;
            if (!previousSet.contains(currentSet)) {
                if (currentSet.cardinality() >= getOptionMinBaseProblemSize()) {
                    System.err.println("# Adding subproblem: " + currentSet);
                    sets.add(currentSet);
                    previousSet = currentSet;

                    if (cgviz != null) {
                        StringBuilder buf = new StringBuilder();
                        for (int p = i; p <= j; p++) {
                            if (p > i)
                                buf.append(",");
                            buf.append(ordering[p]);
                        }
                        cgviz.add("num=" + sets.size() + " first=" + (i + 1) + " last=" + (j + 1) + " size=" +
                                (j - i + 1) + " start=" + taxon2start[ti] + " end=" + taxon2end[tj] +
                                " set=" + buf.toString() +
                                ": " + taxon2start[ti] + " " + (i + 1) + " " + taxon2end[tj] + " " + (j + 1));
                    }
                } else {
                    System.err.println("# Skipping subproblem: " + currentSet + ", too small");
                }
            }
            doc.notifySetProgress(i * j);
        }
        cgviz.end();
        System.err.println("# Number of subproblems: " + sets.size());

        doc.notifyTasks("DCMPartialTrees", "preparing tmp document");
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(-1);

        // make tmp copy of assumptions so that some assumptions are rescued:
        Assumptions tmpAssumptions = doc.getAssumptions().clone(taxa);

        // need a working copy of the document:
        Document tmpDoc = new Document();
        tmpDoc.setTrees(new Trees());// set these early

        try {
            StringWriter w = new StringWriter();
            taxa.write(w);
            chars.write(w, taxa);
            tmpAssumptions.setExTaxa(new TaxaSet());
            tmpAssumptions.setExTrees(new LinkedList());
            tmpAssumptions.setCharactersTransformName(getOptionBaseCharactersMethod());
            tmpAssumptions.setCharactersTransformParam(getOptionBaseCharactersMethodParameters());
            tmpAssumptions.setDistancesTransformName(getOptionBaseDistancesMethod());
            tmpAssumptions.setDistancesTransformParam(getOptionBaseDistanceMethodParameters());
            tmpAssumptions.setTreesTransformName(Basic.getShortName(NoSplits.class));
            tmpAssumptions.setTreesTransformParam("");
            tmpAssumptions.setSplitsTransformName(Basic.getShortName(NoGraph.class));
            tmpAssumptions.setSplitsTransformParam("");
            tmpAssumptions.getSplitsPostProcess().setFilter("none");
            tmpAssumptions.write(w, taxa);
            tmpDoc.readNexus(new StringReader(w.toString()));

            System.err.println("# Base-case assumptions:\n" + tmpDoc.getAssumptions().toString(taxa));

        } catch (Exception ex) {
            System.err.println("DCMPartialTrees: Failed to duplicate data: " + ex);
            throw ex;
        }

        // compute the trees:
        Trees trees = new Trees();
        trees.setPartial(true);
        TreeCollector treeCollector = new TreeCollector(taxa, true);

        Iterator it = sets.iterator();
        int count = 0;
        int numTrees = 0;

        // loop over all partial taxon sets:
        while (it.hasNext()) {
            doc.notifyTasks("DCMPartialTrees", "running sub-problems");
            doc.notifySetMaximumProgress(100);
            doc.notifySetProgress(-1);


            TaxaSet currentTaxa = (TaxaSet) it.next();
            System.err.println("#### PROCESSING subproblem " + (++count) + ", taxa: " + currentTaxa);

            // if one of the parameter strings has a %c in it, replace by count
            // TODO: this works, but can't manage to give the command the send and receive files
            if (getOptionBaseCharactersMethodParameters().contains("%c")
                    || getOptionBaseDistanceMethodParameters().contains("%c")) {
                tmpAssumptions.setCharactersTransformParam(getOptionBaseCharactersMethodParameters().replaceAll("%c", "" + count));
                tmpAssumptions.setDistancesTransformName(getOptionBaseDistancesMethod());
                tmpAssumptions.setDistancesTransformParam(getOptionBaseDistanceMethodParameters().replaceAll("%c", "" + count));
                tmpDoc.readNexus(new StringReader(tmpAssumptions.toString(tmpDoc.getTaxa())));
                doc.notifySetProgress(-1);
            }

            // hide appropriate taxa and characters
            TaxaSet toHide = determineTaxaToHide(tmpDoc.getTaxa().getOriginalTaxa(), currentTaxa);
            tmpDoc.getAssumptions().setExTaxa(toHide);

            List charToHide = determineCharToHide(tmpDoc.getCharacters().getNchar(), currentTaxa, taxon2start, taxon2end, getOptionMinCoverage());
            if (tmpAssumptions.getExChar() != null)
                charToHide.addAll(tmpAssumptions.getExChar());
            tmpDoc.getAssumptions().setExChar(charToHide);

            //System.err.println(tmpDoc.getTaxa().toString());
            //System.err.println(tmpDoc.getCharacters().toString(taxa));

            // run computation and collect all trees
            try {
                tmpDoc.update();

                if (tmpDoc.getTrees().getNtrees() > 0) {
                    treeCollector.addGroup(tmpDoc.getTrees(), tmpDoc.getTaxa());
                }

            } catch (Exception ex) {
                System.err.println("DCMPartialTrees: Update failed on taxon set" + currentTaxa + ": " + ex);
                throw ex;
            }
            doc.notifySetProgress(-1);
        }

        // close and flush cgviz, if open
        cgviz.close();

        //doc.getAssumptions().setTreesTransformName(Basic.getShortName(SuperNetwork.class));
        if (getOptionSelectOptimalTrees())
            return treeCollector.getOptimalSelection();
        else
            return treeCollector.getStrictConsensusPerGroup();
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
     * minimum size of overlap
     *
     * @return
     */
    public int getOptionMinOverlap() {
        return optionMinOverlap;
    }

    /**
     * minimum size of overlap
     *
     * @param optionMinOverlap
     */
    public void setOptionMinOverlap(int optionMinOverlap) {
        this.optionMinOverlap = optionMinOverlap;
    }

    /**
     * name of characters transform
     *
     * @return
     */
    public String getOptionBaseCharactersMethod() {
        return optionBaseCharactersMethod;
    }

    /**
     * name of characters transform
     *
     * @param optionBaseCharactersMethod
     */
    public void setOptionBaseCharactersMethod(String optionBaseCharactersMethod) {
        this.optionBaseCharactersMethod = optionBaseCharactersMethod;
    }

    /**
     * name of distance transform
     *
     * @return
     */
    public String getOptionBaseDistancesMethod() {
        return optionBaseDistancesMethod;
    }

    /**
     * name of distance transform
     *
     * @param optionBaseDistancesMethod
     */
    public void setOptionBaseDistancesMethod(String optionBaseDistancesMethod) {
        this.optionBaseDistancesMethod = optionBaseDistancesMethod;
    }

    /**
     * minimum number of sequences to cover any position
     *
     * @return
     */
    public int getOptionMinCoverage() {
        return optionMinCoverage;
    }

    /**
     * minimum number of sequences to cover any position
     *
     * @param optionMinCoverage
     */
    public void setOptionMinCoverage(int optionMinCoverage) {
        this.optionMinCoverage = optionMinCoverage;
    }

    public String getOptionBaseCharactersMethodParameters() {
        return optionBaseCharactersMethodParameters;
    }

    public void setOptionBaseCharactersMethodParameters(String optionBaseCharactersMethodParameters) {
        this.optionBaseCharactersMethodParameters = optionBaseCharactersMethodParameters;
    }

    public String getOptionBaseDistanceMethodParameters() {
        return optionBaseDistanceMethodParameters;
    }

    public void setOptionBaseDistanceMethodParameters(String optionBaseDistanceMethodParameters) {
        this.optionBaseDistanceMethodParameters = optionBaseDistanceMethodParameters;
    }

    public int getOptionMinBaseProblemSize() {
        return optionMinBaseProblemSize;
    }

    public void setOptionMinBaseProblemSize(int optionMinBaseProblemSize) {
        this.optionMinBaseProblemSize = optionMinBaseProblemSize;
    }

    public String getOptionCGVizFile() {
        return optionCGVizFile;
    }

    public void setOptionCGVizFile(String optionCGVizFile) {
        this.optionCGVizFile = optionCGVizFile;
    }

    public boolean getOptionSelectOptimalTrees() {
        return optionSelectOptimalTrees;
    }

    public void setOptionSelectOptimalTrees(boolean optionSelectOptimalTrees) {
        this.optionSelectOptimalTrees = optionSelectOptimalTrees;
    }

    /**
     * determine which characters to hide
     *
     * @param nchar
     * @param currentTaxa
     * @param taxon2start
     * @param taxon2end
     * @param minCover
     * @return
     */
    private List determineCharToHide(int nchar, TaxaSet currentTaxa, int[] taxon2start, int[] taxon2end, int minCover) {

        SortedSet sorted = new TreeSet();
        for (int t = currentTaxa.getBits().nextSetBit(1); t >= 0; t = currentTaxa.getBits().nextSetBit(t + 1)) {
            Pair pair = new Pair(taxon2start[t], t);// +t: is start
            sorted.add(pair);
            pair = new Pair(taxon2end[t], -t);     // -t: is end
            sorted.add(pair);
        }

        int cover = 0;
        int start = 0;
        int end = 0;

        for (Object aSorted : sorted) {
            Pair pair = (Pair) aSorted;
            if (pair.getSecondInt() > 0)//is start
            {
                cover++;
                if (cover >= minCover && start == 0)
                    start = pair.getFirstInt();

            } else        // is end
            {
                if (cover >= minCover)
                    end = pair.getFirstInt();
                cover--;
                if (start > 0 && cover < minCover)
                    break; // coverage has dropped back below min, break!
            }
        }
        System.err.println("# Character range: " + start + " - " + end);
        List list = new LinkedList();
        for (int i = 1; i < start; i++)
            list.add(i);
        for (int i = end + 1; i <= nchar; i++)
            list.add(i);
        return list;
    }

    /**
     * determines which taxa need to be hidden in origTaxa so that we get currentTaxa
     *
     * @param origTaxa
     * @param currentTaxa
     * @return to hide
     */
    private TaxaSet determineTaxaToHide(Taxa origTaxa, TaxaSet currentTaxa) {
        TaxaSet toHide = new TaxaSet();
        for (int t = 1; t <= origTaxa.getNtax(); t++)
            if (!currentTaxa.get(t))
                toHide.set(t);
        return toHide;
    }

    /**
     * determine the start and end positions of all fragments of sequence
     *
     * @param taxa
     * @param chars
     * @param taxon2start
     * @param taxon2end
     */
    private void determineStartEndPosition(Taxa taxa, Characters chars, int[] taxon2start, int[] taxon2end) {
        for (int t = 1; t <= taxa.getNtax(); t++) {
            int start = 0;
            int end = 0;
            for (int pos = 1; pos <= chars.getNchar(); pos++) {
                if (!chars.isMasked(pos) && chars.get(t, pos) != chars.getFormat().getGap()
                        && chars.get(t, pos) != chars.getFormat().getMissing()) {
                    if (start == 0)
                        start = pos;
                    end = pos;
                }
            }
            taxon2start[t] = start;
            taxon2end[t] = end;
            // System.err.println("# Taxon "+t+": start="+start+" end="+end);
        }
    }


    /**
     * compute a perfect elimination scheme. All we do is order by end positions
     * of fragments
     *
     * @param taxa
     * @param taxon2end
     * @return ordering with components 0...ntax-1
     */
    private int[] computeEliminationScheme(Taxa taxa, int[] taxon2end) {
        // sort by taxon2end:
        List list = new LinkedList();
        for (int t = 1; t <= taxa.getNtax(); t++) {
            list.add(new Pair(taxon2end[t], t));
        }
        Pair[] array = (Pair[]) list.toArray(new Pair[list.size()]);
        Arrays.sort(array);
        int[] ordering = new int[taxa.getNtax()];
        for (int i = 0; i < ordering.length; i++)
            ordering[i] = array[i].getSecondInt();
        return ordering;
    }

}
