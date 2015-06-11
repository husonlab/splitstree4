/**
 * PTreeSplits.java 
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
/* $Id: PTreeSplits.java,v 1.14 2009-11-03 20:03:33 bryant Exp $
*/
package splitstree.algorithms.characters;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsSet;
import splitstree.core.TaxaSet;
import splitstree.nexus.Characters;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;


public class PTreeSplits implements Characters2Splits {
    public static String DESCRIPTION = "Computes the Parsimony Splits tree (Bandelt and Dress 1992)";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

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
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws CanceledException {
        System.err.println("PTreeSplits: untested");
        boolean gapmissingmode = chars.gapMissingMode;
        SplitsSet previous = new SplitsSet(); // list of previously computed splits
        SplitsSet current; // current list of splits
        TaxaSet taxa_prev = new TaxaSet(); // taxa already processed
        int maxProgress = chars.getNtax();
        doc.notifySetProgress(0);
        doc.notifySubtask("PTreeSplits");

        for (int t = 1; t <= chars.getNtax(); t++) {
            // initally, just add 1 to set of previous taxa
            if (t == 1) {
                taxa_prev.set(t);
                continue;
            }

            current = new SplitsSet(t); // restart current list of splits

            // Does t vs previous set of taxa form a split?
            TaxaSet At = new TaxaSet();
            At.set(t);

            double wgt = pIndex(gapmissingmode, t, At, chars);
            if (wgt > 0)
                current.add(At, (float) wgt);

            // consider all previously computed splits:
            for (int s = 1; s <= previous.getNsplits(); s++) {
                TaxaSet A = previous.getSplit(s);
                TaxaSet B = A.getComplement(t - 1);
                // is Au{t} vs B a split?
                A.set(t);
                wgt = Math.min((int) previous.getWeight(s),
                        pIndex(gapmissingmode, t, A, chars));
                if (wgt > 0)
                    current.add(A, (float) wgt);
                A.unset(t);

                // is A vs Bu{t} a split?
                B.set(t);
                wgt = Math.min((int) previous.getWeight(s),
                        pIndex(gapmissingmode, t, B, chars));
                if (wgt > 0)
                    current.add(B, (float) wgt);
            }
            previous = current;
            taxa_prev.set(t);
            doc.notifySetProgress(100 * t / maxProgress);
        }

        // copy splits to s
        Splits splits = new Splits();
        splits.setNtax(chars.getNtax());
        splits.addSplitsSet(previous);
        splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
        doc.notifySetProgress(100);   //set progress to 100%
// pd.close();								//get rid of the progress listener
// doc.setProgressListener(null);
        return splits;
    }

    // Computes the p-index of a split:

    private double pIndex(boolean gapmissingmode, int t, TaxaSet A, Characters characters) {
        double value = Integer.MAX_VALUE;
        int a1, a2, b1, b2;

        a1 = t;

        if (!A.get(a1))
            System.err.println("pIndex(): a1=" + a1 + " not in A");

        for (a2 = 1; a2 <= t; a2++) {
            if (A.get(a2))
                for (b1 = 1; b1 <= t; b1++) {
                    if (!A.get(b1))
                        for (b2 = b1; b2 <= t; b2++) {
                            if (!A.get(b2)) {
                                double val_a1a2b1b2 =
                                        pScore(gapmissingmode, a1, a2, b1, b2, characters);
                                if (val_a1a2b1b2 != 0)
                                    value = Math.min(value, val_a1a2b1b2);
                                else
                                    return 0;
                            }
                        }
                }
        }
        return value;
    }

// Computes the parsimony-score for the four given taxa:

    private double pScore(boolean gapmissingmode, int a1, int a2, int b1, int b2, Characters characters) {
        double a1a2_b1b2 = 0.0, a1b1_a2b2 = 0.0, a1b2_a2b1 = 0.0;
        int c;
        char missingchar = characters.getFormat().getMissing();
        char gapchar = characters.getFormat().getGap();
        int nchar = characters.getNchar();
        char[] row_a1 = characters.getRow(a1);
        char[] row_a2 = characters.getRow(a2);
        char[] row_b1 = characters.getRow(b1);
        char[] row_b2 = characters.getRow(b2);

        for (c = 1; c <= nchar; c++) {
            if (!characters.isMasked(c)) {
                char c_a1 = row_a1[c];
                char c_a2 = row_a2[c];
                char c_b1 = row_b1[c];
                char c_b2 = row_b2[c];

                double weight = characters.getCharWeight(c);

                if (c_a1 == missingchar || c_a2 == missingchar || c_b1 == missingchar || c_b2 == missingchar)
                    continue;
                if (gapmissingmode && (c_a1 == gapchar || c_a2 == gapchar || c_b1 == gapchar || c_b2 == gapchar))
                    continue;
                if (c_a1 == c_a2 && c_b1 == c_b2)
                    a1a2_b1b2 += weight;
                if (c_a1 == c_b1 && c_a2 == c_b2)
                    a1b1_a2b2 += weight;
                if (c_a1 == c_b2 && c_a2 == c_b1)
                    a1b2_a2b1 += weight;
            }
        }
        double max_val = Math.max(a1b1_a2b2, a1b2_a2b1);
        if (a1a2_b1b2 > max_val)
            return a1a2_b1b2 - max_val;
        else
            return 0;
    }

}//EOF
