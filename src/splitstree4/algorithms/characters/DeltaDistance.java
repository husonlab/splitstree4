/*
 * DeltaDistance.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.characters;

import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * @author DJB
 */

public class DeltaDistance /* implements Characters2Distances */ {

    public final static String DESCRIPTION = "Calculates delta distances: distance 0 if identical, otherwise distance 1.";

    private boolean assumeTriangleInequality = false;

    public String getDescription() {
        return DESCRIPTION;
    }

    public boolean getOptionAssumeTriangleInequality() {
        return assumeTriangleInequality;
    }

    public void setOptionAssumeTriangleInequality(boolean assumeTriangleInequality) {
        this.assumeTriangleInequality = assumeTriangleInequality;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc != null && taxa != null && chars != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {


        if (doc != null) {
            doc.notifySubtask("Delta Distance");
            doc.notifySetProgress(0);
            doc.notifySetMaximumProgress(taxa.getNtax());
        }


        int ntax = characters.getNtax();
        int nchar = characters.getNchar();
        Distances distances = new Distances(ntax);
        distances.getFormat().setTriangle("both");

        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {

                //Use triangle inequality/DFS to avoid testing some pairs.

                boolean mustCompute = true;
                boolean diff = false;
                if (assumeTriangleInequality) {
                    diff = true;
                    for (int k = 1; k < i; k++) {
                        if (distances.get(i, k) == 0) {
                            diff = (distances.get(k, j) != 0);
                            mustCompute = false;
                            break;
                        }
                        if (distances.get(j, k) == 0) {
                            diff = true; //We know i not the same as k. so if j same as k then i not same as j.
                            mustCompute = false;
                            break;
                        }
                    }
                }
                if (mustCompute) {
                    for (int k = 1; !diff && k <= nchar; k++)
                        diff = (characters.get(i, k) == characters.get(j, k));
                }

                distances.set(i, j, (diff) ? 1 : 0);
                distances.set(j, i, (diff) ? 1 : 0);
            }
            doc.notifySetProgress(i);

        }

        return distances;
    }

}//EOF
