/**
 * Nei_Li_RestrictionDistance.java
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

import jloda.util.Alert;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * Implements the NeiLi (1979) distance for restriction site data.
 */
public class Nei_Li_RestrictionDistance /* implements Characters2Distances */ {

    public final boolean EXPERT = false;


    private final String DESCRIPTION = "Calculates the Nei and Li (1979) distance for restriction site data";

    /**
     * Determine whether Nei and Lirestriction distances can be computed with given data.
     *
     * @param doc  the document
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
     * Computes Nei and Li restriction distances with a given characters block.
     *
     * @param doc        the document
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     * @throws SplitsException   if there is a syntax problem with the file
     * @throws CanceledException if the user pressed cancel in the progress bar dialog box
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws SplitsException, CanceledException {
        Distances distances = new Distances(taxa.getNtax());
        distances.getFormat().setTriangle("both");

        int ntax = taxa.getNtax();
        if (doc != null) {
            doc.notifySubtask("Nei Li (1979) Restriction Site Distance");
            doc.notifySetProgress(0);
        }

        double maxDist = 0.0;
        int numUndefined = 0;

        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {

                PairwiseCompare seqPair = new PairwiseCompare(characters, "01", s, t);
                double[][] F = seqPair.getF();
                double dist = -1.0;
                if (F == null)
                    numUndefined++;
                else {

                    double ns = F[1][0] + F[1][1];
                    double nt = F[0][1] + F[1][1];
                    double nst = F[1][1];

                    if (nst == 0) {
                        dist = -1;
                        numUndefined++;
                    } else {
                        double s_hat = 2.0 * nst / (ns + nt);
                        double a = (4.0 * Math.pow(s_hat, 1.0 / (2 * 6)) - 1.0) / 3.0;
                        if (a <= 0.0) {
                            dist = -1;
                            numUndefined++;
                        } else
                            dist = -1.5 * Math.log(a);
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
        return distances;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */

    public String getDescription() {
        return DESCRIPTION;
    }
}
