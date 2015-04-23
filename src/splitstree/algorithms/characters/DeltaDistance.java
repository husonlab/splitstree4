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

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

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
