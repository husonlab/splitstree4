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

import jloda.util.Alert;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

/**
 * Implements the Upholt (1979) distance for restriction site data.
 */
public class Upholt implements Characters2Distances {

    public final boolean EXPERT = false;
    public final static String DESCRIPTION = "Calculates the Upholt (1979) distance for restriction site data";

    /**
     * Determine whether Upholt distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, character block exists and has standard datatype.
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        if (taxa == null || c == null || !(c.getFormat().getDatatype()).equalsIgnoreCase(Characters.Datatypes.STANDARD))
            return false;
        return c.getFormat().getSymbols().equalsIgnoreCase(Characters.Datatypes.STANDARDSYMBOLS);
    }

    /**
     * Computes Upholt distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {
        Distances distances = new Distances(taxa.getNtax());
        distances.getFormat().setTriangle("both");

        // ProgressDialog pd = new ProgressDialog("Hamming Distance...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        int ntax = taxa.getNtax();
        if (doc != null) {
            doc.notifySubtask("Upholt Distance");
            doc.notifySetProgress(0);
        }

        double maxDist = 0.0;
        int numUndefined = 0;

        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {
                //System.err.println(s+","+t);
                PairwiseCompare seqPair = new PairwiseCompare(characters, "01", s, t);
                double[][] F = seqPair.getF();
                double dist = -1.0;

                if (F==null)
                    numUndefined++;
                else {

                    double ns = F[1][0] + F[1][1];
                    double nt = F[0][1] + F[1][1];
                    double nst = F[1][1];

                    if (nst == 0) {
                        numUndefined++;
                        dist = -1;
                    } else {
                        double s_hat = 2.0 * nst / (ns + nt);
                        dist = -Math.log(s_hat) / 6.0;
                    }

                }

                distances.set(s, t, dist);
                distances.set(t, s, dist);
                if (dist > maxDist)
                    maxDist = dist;
            }
            if (doc != null)
                doc.notifySetProgress(s * 100 / ntax);
        }
        if (doc != null)
            doc.notifySetProgress(100);   //set progress to 100%
        if (numUndefined > 0) {
            for (int s = 1; s <= ntax; s++)
                for (int t = s + 1; t <= ntax; t++) {
                    if (distances.get(s, t) < 0) {
                        distances.set(s, t, 2.0 * maxDist);
                        distances.set(t, s, 2.0 * maxDist);
                    }
                }
            String message = "Distance matrix contains " + numUndefined + " undefined ";
            message += "distances. These have been arbitrarily set to 2 times the maximum";
            message += " defined distance (= " + (2.0 * maxDist) + ").";
            new Alert(message);
        }

// pd.close();								//get rid of the progress listener
// doc.setProgressListener(null);
        return distances;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    final public String getDescription() {
        return DESCRIPTION;
    }
}
