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

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.nexus.Characters;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.CharactersUtilities;
import splitstree.util.SplitsUtilities;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * computes splits from binary data  and draws them using the convex hull algorithm
 *
 * @author huson
 *         Date: 16-Feb-2004
 */
public class MedianNetwork implements Characters2Splits {
    public final static String DESCRIPTION = "Computes a median network (Bandelt et al, 1995)";
    boolean optionUseOnlyConvexHull = false;
    private boolean optionAddAllTrivial = true;
    private int optionMinimumSupport = 1;
    private boolean optionUseRelaxedSupport = false;

    private Map split2Chars;
    private boolean optionUseRYAlphabet = false;
    private boolean optionLabelEdges = false;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws CanceledException, SplitsException {
        doc.notifySetMaximumProgress(chars.getNchar());    //initialize maximum progress
        doc.notifySetProgress(0);

        // TODO: this code should be replaced by calls to Binary2Splits and DNA2Splits

        if (getOptionUseRYAlphabet()
                && !chars.getFormat().getDatatype().equals(Characters.Datatypes.DNA)
                && !chars.getFormat().getDatatype().equals(Characters.Datatypes.RNA)) {
            new Alert("Can't use RY alphabet for datatype: " + chars.getFormat().getDatatype());
            setOptionUseRYAlphabet(false);
        }

        split2Chars = new HashMap();

        Splits splits = new Splits(chars.getNtax());
        splits.getFormat().setLabels(true);

        BitSet haveTrivial = new BitSet();

        int numberSitesUsed = 0;
        int numberGaps = 0;

        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                // determine majority color:
                int ncolors = chars.getNcolors();
                int[] counts = new int[ncolors + 1];
                BitSet colorsUsed = new BitSet();

                boolean isGap = false;
                for (int t = 1; t <= chars.getNtax(); t++) {
                    char ch = chars.get(t, c);
                    int color = getColor(chars, ch, getOptionUseRYAlphabet());
                    if (ch == chars.getFormat().getGap() || ch == chars.getFormat().getMissing())
                        isGap = true;
                    if (color == -1 && !isGap) {
                        throw new SplitsException(Basic.getShortName(this.getClass()) + ": unknown character-state: " + ch);
                    }
                    if (color != -1)
                        colorsUsed.set(color);
                    if (color >= 1)
                        counts[color]++;
                }

                if (isGap) {
                    numberGaps++;
                    continue;
                }

                int majorityColor = 0;
                for (int color = 1; color <= ncolors; color++)
                    if (counts[color] > counts[majorityColor])
                        majorityColor = color;

                if (colorsUsed.cardinality() != 2)
                    continue;

                // make one side of the split:
                TaxaSet current = new TaxaSet();
                if (counts[majorityColor] > 0) {
                    for (int t = 1; t <= chars.getNtax(); t++) {
                        char ch = chars.get(t, c);
                        if (getColor(chars, ch, getOptionUseRYAlphabet()) == majorityColor)
                            current.set(t);
                    }
                }

                if (current.cardinality() == 0 || current.cardinality() == chars.getNtax())
                    continue; // not a proper split

                boolean exists = false;
                for (int s = 1; s <= splits.getNsplits(); s++) {

                    if (splits.get(s).equalsAsSplit(current, chars.getNtax())
                            || splits.get(s).equalsAsSplit(current.getComplement(chars.getNtax()), chars.getNtax())) {
                        splits.setWeight(s, splits.getWeight(s) + (float) chars.getCharWeight(c));

                        exists = true;
                        // this split already exists, add char to its support:
                        ((BitSet) split2Chars.get(s)).set(c);
                        break;
                    }
                }
                numberSitesUsed++;

                if (!exists) {
                    splits.add(current, (float) chars.getCharWeight(c));


                    int s = splits.getNsplits();

                    // update splits 2 chars
                    BitSet bits = new BitSet();
                    bits.set(c);
                    split2Chars.put(s, bits);

                    if (getOptionAddAllTrivial()) {
                        if (splits.get(s).cardinality() == 1) {
                            int t = splits.get(s).getBits().nextSetBit(1);
                            haveTrivial.set(t);
                        } else if (splits.get(s).cardinality() == taxa.getNtax() - 1) {
                            TaxaSet comp = splits.get(s).getComplement(taxa.getNtax());
                            int t = comp.getBits().nextSetBit(1);
                            haveTrivial.set(t);

                        }
                    }
                }
                doc.notifySetProgress(c);
            }
        }
        int numberSplitsGenerated = splits.getNsplits();


        int numberTrivialAdded = 0;
        if (getOptionAddAllTrivial()) {
            for (int t = 1; t <= taxa.getNtax(); t++)
                if (!haveTrivial.get(t)) {
                    TaxaSet trivialSplit = new TaxaSet();
                    trivialSplit.set(t);
                    splits.add(trivialSplit, 1);

                    int s = splits.getNsplits();

                    // update splits 2 chars
                    BitSet bits = new BitSet();
                    bits.set(0);
                    split2Chars.put(s, bits);
                    numberTrivialAdded++;
                }
        }
        System.err.println("Number of splits generated=   " + numberSplitsGenerated);
        System.err.println("Number of sites used=         " + numberSitesUsed);
        System.err.println("Number gap sites=             " + numberGaps + " (skipped)");
        System.err.println("Number trivial added=         " + numberTrivialAdded);

        // label splits by mutations:
        if (getOptionLabelEdges()) {
            for (int s = 1; s <= splits.getNsplits(); s++)
                splits.setLabel(s, Basic.toString((BitSet) split2Chars.get(s)));
        }


        if (getOptionMinimumSupport() <= 1)
            return splits;
        else // need to filter
        {
            Splits filtered = new Splits(chars.getNtax());
            for (int s = 1; s <= splits.getNsplits(); s++) {
                TaxaSet split = splits.get(s);
                float support;
                int splitSize = splits.get(s).getSplitSize(taxa.getNtax());
                if (!getOptionUseRelaxedSupport())
                    support = splits.getWeight(s);
                else
                    support = SplitsUtilities.getRelaxedSupport(splits, s, chars);

                if (splitSize == 1 || support >= getOptionMinimumSupport()) {
                    filtered.add(split, support, splits.getConfidence(s), splits.getLabel(s));
                }
            }
            System.err.println("Number of filtered splits=    " + filtered.getNsplits());
            return filtered;
        }
    }

    /**
     * add all trivial splits?
     *
     * @return option
     */
    public boolean getOptionAddAllTrivial() {
        return optionAddAllTrivial;
    }

    public void setOptionAddAllTrivial(boolean optionAddAllTrivial) {
        this.optionAddAllTrivial = optionAddAllTrivial;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * gets the color of a character, using RY alphabet, if desired
     *
     * @param chars
     * @param ch
     * @param useRY
     * @return color or -1
     */
    int getColor(Characters chars, char ch, boolean useRY) {
        if (!useRY)
            return chars.getColor(ch);
        else {
            ch = CharactersUtilities.getRYlowerCase(ch);
            if (ch == 'r')
                return 1;
            else if (ch == 'y')
                return 2;
            else
                return -1;
        }
    }

    /**
     * gets the mapping of splits to characters
     *
     * @return a map that has as key an Integer object representing the splits id and
     *         as value a BitSet that contains the positions in the characters block
     */
    public Map getSplit2Chars() {
        return split2Chars;
    }

    /**
     * threshold for minimum split weight
     *
     * @return
     */
    public int getOptionMinimumSupport() {
        return optionMinimumSupport;
    }

    /**
     * set the minimum integer value for the support of an edge
     *
     * @param optionMinimumSupport
     */
    public void setOptionMinimumSupport(int optionMinimumSupport) {
        this.optionMinimumSupport = optionMinimumSupport;
    }

    /**
     * is relaxed support used?
     *
     * @return
     */
    public boolean getOptionUseRelaxedSupport() {
        return optionUseRelaxedSupport;
    }

    /**
     * set if relaxed support should be used
     *
     * @param optionUseRelaxedSupport
     */
    public void setOptionUseRelaxedSupport(boolean optionUseRelaxedSupport) {
        this.optionUseRelaxedSupport = optionUseRelaxedSupport;
    }

    /**
     * is RY alphabet used?
     *
     * @return
     */
    public boolean getOptionUseRYAlphabet() {
        return optionUseRYAlphabet;
    }

    /**
     * set if RY alphabet  should be used
     *
     * @param optionUseRYAlphabet
     */
    public void setOptionUseRYAlphabet(boolean optionUseRYAlphabet) {
        this.optionUseRYAlphabet = optionUseRYAlphabet;
    }

    /**
     * are edges beeing labeled?
     *
     * @return
     */
    public boolean getOptionLabelEdges() {
        return optionLabelEdges;
    }

    /**
     * set if edes should be labeled
     *
     * @param optionLabelEdges
     */
    public void setOptionLabelEdges(boolean optionLabelEdges) {
        this.optionLabelEdges = optionLabelEdges;
    }
}
