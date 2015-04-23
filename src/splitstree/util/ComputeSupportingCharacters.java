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

import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Characters;
import splitstree.nexus.Splits;

import java.util.BitSet;

/**
 * compute all positions that support a given character
 * Daniel Huson and David Bryant, 5.2012
 */
public class ComputeSupportingCharacters {

    /**
     * compute the set of supporting characters for the given splits
     *
     * @param doc
     * @param selectedSplits
     * @return
     */
    public static BitSet apply(Document doc, BitSet selectedSplits) {
        BitSet supportedCharacters = new BitSet();
        final Splits splits = doc.getSplits();
        final Characters characters = doc.getCharacters();

        BitSet charA = new BitSet();
        BitSet charB = new BitSet();

        for (int splitId = selectedSplits.nextSetBit(1); splitId > 0; splitId = selectedSplits.nextSetBit(splitId + 1)) {
            final TaxaSet splitPart = splits.get(splitId);
            int sizeA = splitPart.cardinality();
            int sizeB = characters.getNtax() - sizeA;
            int countA = 0;
            int countB = 0;
            for (int c = 1; c <= characters.getNchar(); c++) {
                if (!characters.isMasked(c)) {
                    charA.clear();
                    charB.clear();
                    boolean ok = true;
                    for (int t = 1; ok && t <= characters.getNtax(); t++) {
                        int ch = characters.get(t, c);
                        if (ch != 0 && ch != characters.getFormat().getMissing() && ch != characters.getFormat().getGap()) {
                            if (splitPart.get(t)) {
                                if (charB.get(ch))
                                    ok = false;
                                else
                                    charA.set(ch);
                                countA++;
                            } else {
                                if (charA.get(ch))
                                    ok = false;
                                else
                                    charB.set(ch);
                                countB++;
                            }
                        }
                    }
                    if (ok && (charA.cardinality() > 0 || (charB.cardinality() == 1 && countB == sizeB))
                            && (charB.cardinality() > 0 || (charA.cardinality() == 1 && countA == sizeA)))
                        supportedCharacters.set(c);
                }
            }
        }
        return supportedCharacters;
    }
}
