/*
 * GapDist.java Copyright (C) 2022 Daniel H. Huson
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
/* $Id: GapDist.java,v 1.23 2009-11-03 03:45:22 bryant Exp $
 */
package splitstree4.algorithms.characters;

import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * Computes the gap distance from a set of sequences
 */
public class GapDist implements Characters2Distances {
    public final static String DESCRIPTION = "Calculates the gap distance from a set of sequences.";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * Determine whether gap-distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return c.isValid() && taxa.isValid();
    }

    /**
     * Computes gap-distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {
        int nchar = characters.getNchar();
        int ntax = characters.getNtax();
        Distances gapDist = new Distances(ntax);
        gapDist.getFormat().setTriangle("both");
        char missingchar = characters.getFormat().getMissing();
        char gapchar = characters.getFormat().getGap();
        int c, s, t;

        doc.notifySubtask("Gaps Distance");
        doc.notifySetProgress(0);


        for (t = 1; t <= ntax; t++) {
            char[] row_t = characters.getRow(t);
            for (s = t + 1; s <= ntax; s++) {
                char[] row_s = characters.getRow(s);
                double sim = 0;
                double len = 0;
                char sc, tc;
                for (c = 1; c <= nchar; c++) {

                    if (!characters.isMasked(c)) {
                        sc = row_s[c];
                        tc = row_t[c];

                        double weight = characters.getCharWeight(c);
                        len += weight;
                        if (((sc == gapchar && tc == gapchar) ||
                                (sc != gapchar && tc != gapchar)))
                            sim += weight;
                    }
                }
                double v = 1.0;
                if (sim != 0 && len != 0) v = (1.0 - sim / len);
                gapDist.set(s, t, v);
                gapDist.set(t, s, v);
            }
            doc.notifySetProgress(t * 100 / ntax);
        }
        doc.notifySetProgress(nchar);   //set progress to 100%
        // pd.close();								//get rid of the progress listener
        // doc.setProgressListener(null);
        return gapDist;
    }
}//EOF
