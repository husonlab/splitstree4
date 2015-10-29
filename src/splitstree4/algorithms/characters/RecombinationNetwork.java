/**
 * RecombinationNetwork.java
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

import splitstree4.algorithms.splits.ReticulateNetwork;
import splitstree4.analysis.splits.Stats;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * compute recombination graph from binary sequences
 *
 * @author huson
 *         Date: 10-Feb-2005
 */
public class RecombinationNetwork extends ReticulateNetwork implements Characters2Network {
    static int optionMinSplitWeight = 1;
    public final static String DESCRIPTION = "Compute a recombination graph from binary sequences (Huson et al, 2005)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc.isValid(taxa) && doc.isValid(chars) && (
                chars.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.STANDARD) ||
                        chars.getFormat().getDatatype().equals(Characters.Datatypes.DNA) ||
                        chars.getFormat().getDatatype().equals(Characters.Datatypes.RNA));
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        Splits splits = null;

        // first make splits from binary sequences
        if (chars.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.STANDARD)) {
            Binary2Splits binary2splits = new Binary2Splits();
            binary2splits.setOptionMinSplitWeight(getOptionMinSplitWeight());
            splits = binary2splits.apply(doc, taxa, chars);
            setSplit2Chars(binary2splits.getSplit2Chars());
            setFirstChars(chars.getRow(1));

        } else if (
                chars.getFormat().getDatatype().equals(Characters.Datatypes.DNA) ||
                        chars.getFormat().getDatatype().equals(Characters.Datatypes.RNA)) {
            DNA2Splits dna2splits = new DNA2Splits();
            dna2splits.setOptionMinSplitWeight(getOptionMinSplitWeight());
            splits = dna2splits.apply(doc, taxa, chars);
            setSplit2Chars(dna2splits.getSplit2Chars());
            setFirstChars(chars.getRow(1));
        }

        // compute cycle and stuff:
        doc.setSplits(splits);
        (new Stats()).apply(doc, taxa, splits);
        doc.setSplits(null);

        return apply(doc, taxa, splits);
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
     * what is the minimal split weight?
     *
     * @return min split weight threshold
     */
    public static int getOptionMinSplitWeight() {
        return optionMinSplitWeight;
    }

    /**
     * set the minimal weight fo the splits to be consided by the algorithm
     *
     * @param optionMinSplitWeight
     */
    public static void setOptionMinSplitWeight(int optionMinSplitWeight) {
        RecombinationNetwork.optionMinSplitWeight = optionMinSplitWeight;
    }

    /**
     * are the nodes labeled with their sequences?
     *
     * @return
     */
    public boolean getOptionShowSequences() {
        return optionShowSequences;
    }

    /**
     * should the nodes beeing labeled with sequences?
     *
     * @param optionShowSequences
     */
    public void setOptionShowSequences(boolean optionShowSequences) {
        this.optionShowSequences = optionShowSequences;
    }

    /**
     * are the edges beeing labeled with the mutations
     *
     * @return
     */
    public boolean getOptionShowMutations() {
        return optionShowMutations;
    }

    /**
     * should the edges beeing labeled with the mutations
     *
     * @param optionShowMutations
     */
    public void setOptionShowMutations(boolean optionShowMutations) {
        this.optionShowMutations = optionShowMutations;
    }
}
