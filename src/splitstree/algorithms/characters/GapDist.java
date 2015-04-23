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

/* $Id: GapDist.java,v 1.23 2009-11-03 03:45:22 bryant Exp $
*/
package splitstree.algorithms.characters;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

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
            }
            doc.notifySetProgress(t * 100 / ntax);
        }
        doc.notifySetProgress(nchar);   //set progress to 100%
        // pd.close();								//get rid of the progress listener
        // doc.setProgressListener(null);
        return gapDist;
    }
}//EOF
