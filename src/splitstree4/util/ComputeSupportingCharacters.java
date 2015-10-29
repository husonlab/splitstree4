/**
 * ComputeSupportingCharacters.java
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
package splitstree4.util;

import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;

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
