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

import splitstree.algorithms.splits.ReticulateNetwork;
import splitstree.analysis.splits.Stats;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

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
