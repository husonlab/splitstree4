/**
 * Hamming.java 
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
/*
* $Id: Hamming.java,v 1.40 2010-06-15 00:48:46 bryant Exp $
*/
package splitstree.algorithms.characters;

import jloda.util.Alert;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple implementation of hamming distances
 */
public class Hamming implements Characters2Distances {

    private int optionHandleAmbiguousStates = PairwiseCompare.IGNOREAMBIG;
    private boolean optionNormalize = true;

    JPanel guiPanel;
    private final static String[] AMBIG_OPTIONS = {"Ignore", "AverageStates", "MatchStates"};
    public final static String DESCRIPTION = "Calculates distances using the hamming distance.";
    protected String TASK = "Hamming Distance";

    /**
     * Determine whether Hamming distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null;
    }

    /**
     * Computes the hamming distance fow a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {
        if (this.optionHandleAmbiguousStates != PairwiseCompare.MATCHAMBIG || !characters.getFormat().isNucleotideType() || !characters.hasAmbigStates())
            return hamming(doc, taxa, characters);
        else
            return ambigHamming(doc, taxa, characters);
    }

    /**
     * gets the option for handling ambiguous character codes
     *
     * @return String
     */
    public String getOptionHandleAmbiguousStates() {
        return AMBIG_OPTIONS[optionHandleAmbiguousStates];
    }

    /**
     * Sets the option for handling ambiguous codes. Does nothing if the
     * option given is not valid.
     *
     * @param which String. Should be one of {"Ignore", "AverageStates", "MatchStates"}
     */
    public void setOptionHandleAmbiguousStates(String which) {

        for (int i = 0; i < AMBIG_OPTIONS.length; i++) {
            if (which != null && which.equalsIgnoreCase(AMBIG_OPTIONS[i])) {
                optionHandleAmbiguousStates = i;
                break;
            }
        }
    }

    /**
     * return the possible options to deal with ambiguous states.
     *
     * @param doc
     * @return a list of String representing the options.
     */
    public List selectionOptionHandleAmbiguousStates(Document doc) {
        List models = new LinkedList();
        for (String AMBIG_OPTION : AMBIG_OPTIONS) models.add("" + AMBIG_OPTION);

        return models;
    }

    /*
    public JPanel getGUIPanel(Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new JPanel();
        guiPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        guiPanel.add(new AmbiguousStatePanel(this), constraints);

        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }
    */

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    protected String getTask() {
        return TASK;
    }

    /**
     * Computes Hamming distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    private Distances hamming(Document doc, Taxa taxa, Characters characters) throws Exception {
        Distances distances = new Distances(taxa.getNtax());

        if (doc!=null) {
            doc.notifySubtask(getTask());
            doc.notifySetMaximumProgress(100);
            doc.notifySetProgress(0);
        }
        int numMissing = 0;
        int ntax = taxa.getNtax();
        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {
                PairwiseCompare seqPair = new PairwiseCompare(characters, characters.getFormat().getSymbols(), s, t, optionHandleAmbiguousStates);
                double p = 1.0;

                double[][] F = seqPair.getF();

                if (F==null) {
                    numMissing++;
                }
                else {
                    for (int x = 0; x < seqPair.getNumStates(); x++) {
                        p = p - F[x][x];
                    }

                    if (!getOptionNormalize())
                        p = Math.round(p * seqPair.getNumNotMissing());
                }
                distances.set(s, t, p);
                distances.set(t, s, p);
            }
            if (doc!=null)
                doc.notifySetProgress(s * 100 / ntax);
        }
        if (doc!=null)
            doc.notifySetProgress(taxa.getNtax());
        if (numMissing>0)
            new Alert("Warning: " + numMissing + " saturated or missing entries in the distance matrix - proceed with caution ");
        return distances;
    }

    /**
     * Code for computing distances given ambiguity codes.
     */
    private static final String ALLSTATES = "acgtwrkysmbhdvn";
    final String[] AMBIGDNACODES = {"a", "c", "g", "t", "at", "ag", "gt", "ct", "cg", "ac", "cgt", "act", "agt", "acg", "acgt"};

    private double stringDiff(String s1, String s2) {
        int matchCount = 0;
        for (int i = 0; i < s1.length(); i++) {
            char ch = s1.charAt(i);
            if (s2.indexOf(ch) >= 0) {
                matchCount++;
            }
        }
        for (int i = 0; i < s2.length(); i++) {
            char ch = s2.charAt(i);
            if (s1.indexOf(ch) >= 0) {
                matchCount++;
            }
        }

        return 1.0 - (double) matchCount / ((double) s1.length() + s2.length());
        //SAME IN INVERSE.
    }

    private double[][] getFmatrix(Characters characters, int i, int j) {
        int nstates = ALLSTATES.length();
        double[][] F = new double[nstates][nstates];
        double fsum = 0.0;
        for (int k = 1; k <= characters.getNchar(); k++) {
            char ch1 = characters.getOriginal(i, k);
            char ch2 = characters.getOriginal(j, k);
            int state1 = ALLSTATES.indexOf(ch1);
            int state2 = ALLSTATES.indexOf(ch2);
            if (state1 >= 0 && state2 >= 0) {
                F[state1][state2] += 1.0;
                fsum += 1.0;
            }
        }
        if (fsum > 0.0) {
            for (int x = 0; x < nstates; x++)
                for (int y = 0; y < nstates; y++)
                    F[x][y] = F[x][y] / fsum;

        }

        return F;
    }

    /**
     * Computes 'Best match' Hamming distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    private Distances ambigHamming(Document doc, Taxa taxa, Characters characters) throws Exception {
        Distances distances = new Distances(taxa.getNtax());

        doc.notifySubtask(getTask());
        doc.notifySetProgress(0);

        int ntax = taxa.getNtax();
        int nstates = ALLSTATES.length();

        /* Fill in the costs ascribed to comparing different allele combinations */
        double[][] weights = new double[nstates][nstates];
        for (int s1 = 0; s1 < nstates; s1++)
            for (int s2 = 0; s2 < nstates; s2++)
                weights[s1][s2] = stringDiff(AMBIGDNACODES[s1], AMBIGDNACODES[s2]);

        /*Fill in the distance matrix */
        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {

                double[][] F = getFmatrix(characters, s, t);
                double diff = 0.0;
                for (int s1 = 0; s1 < F.length; s1++)
                    for (int s2 = 0; s2 < F.length; s2++)
                        diff += F[s1][s2] * weights[s1][s2];

                distances.set(s, t, (float) diff);
                distances.set(t, s, (float) diff);
            }
            doc.notifySetProgress(s * 100 / ntax);
        }
        doc.notifySetProgress(taxa.getNtax());
        return distances;
    }


    public boolean getOptionNormalize() {
        return optionNormalize;
    }

    public void setOptionNormalize(boolean optionNormalize) {
        this.optionNormalize = optionNormalize;
    }
}

// EOF
