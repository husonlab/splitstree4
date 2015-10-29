/**
 * Binary2Splits.java
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
package splitstree4.algorithms.characters;

import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * computes splits from binary data
 *
 * @author huson
 *         Date: 16-Feb-2004
 */
public class Binary2Splits implements Characters2Splits {
    public final static String DESCRIPTION = "Convert binary characters to splits";
    private boolean optionAddAllTrivial = true;
    private int optionMinSplitWeight = 1;
    private Map split2Chars;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null && c.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.STANDARD);
    }


    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws CanceledException {
        doc.notifySetMaximumProgress(chars.getNchar());    //initialize maximum progress
        doc.notifySetProgress(0);

        split2Chars = new HashMap();

        Splits splits = new Splits(chars.getNtax());
        splits.getFormat().setLabels(true);

        BitSet haveTrivial = new BitSet();

        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                // make one side of the split:
                TaxaSet current = new TaxaSet();
                for (int t = 1; t <= chars.getNtax(); t++) {
                    if (chars.get(t, c) == '1') current.set(t);
                }

                //if (current.cardinality() > chars.getNtax()/2) current = current.getComplement(chars.getNtax());					//set minimal side to 1

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
        doc.notifySetProgress(splits.getNsplits());   //set progress to 100%

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
                }
        }
        // label splits by mutations:
        for (int s = 1; s <= splits.getNsplits(); s++)
            splits.setLabel(s, Basic.toString((BitSet) split2Chars.get(s)));

        if (getOptionMinSplitWeight() <= 1)
            return splits;
        else // need to filter
        {
            Splits filtered = new Splits(chars.getNtax());
            for (int s = 1; s <= splits.getNsplits(); s++) {
                TaxaSet split = splits.get(s);
                if (split.getSplitSize(taxa.getNtax()) == 1
                        || splits.getWeight(s) >= getOptionMinSplitWeight()) {
                    filtered.add(split, splits.getWeight(s), splits.getConfidence(s), splits.getLabel(s));
                }
            }
            return filtered;
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
     * @return return the threshold for the minimum splits weight
     */
    public int getOptionMinSplitWeight() {
        return optionMinSplitWeight;
    }

    /**
     * sets the minimum split weight to be respected by the algorithm
     *
     * @param optionMinSplitWeight
     */
    public void setOptionMinSplitWeight(int optionMinSplitWeight) {
        this.optionMinSplitWeight = optionMinSplitWeight;
    }

    /**
     * add all trivial splits?
     *
     * @return true if all trivial splits are all added
     */
    public boolean getOptionAddAllTrivial() {
        return optionAddAllTrivial;
    }

    /**
     * add all trivial splits?
     * <p/>
     * * @param optionAddAllTrivial
     */
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

}
