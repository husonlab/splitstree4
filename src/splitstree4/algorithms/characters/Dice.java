/**
 * Dice.java
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
/*
 * $Id: Dice.java,v 1.2 2007-11-21 05:08:22 bryant Exp $
 */
package splitstree4.algorithms.characters;

import jloda.swing.util.Alert;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * Simple implementation of hamming distances
 */
public class Dice implements Characters2Distances {

    public final static String DESCRIPTION = "Calculates distances using the Dice coefficient distance.";
    protected String TASK = "Dice Coefficient Distance";

    /**
     * Determine whether Dice distances can be computed with given data.
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
     * Computes Dice distances with a given characters block.
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
            doc.notifySubtask("Dice Distance");
            doc.notifySetProgress(0);
        }

        double maxDist = 0.0;
        int numUndefined = 0;

        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {
                PairwiseCompare seqPair = new PairwiseCompare(characters, "01", s, t);
                double dist;

                double[][] F = seqPair.getF();
                if (F == null) {
                    numUndefined++;
                    dist = -1;
                } else {

                    double b = F[1][0];
                    double c = F[0][1];
                    double a = F[1][1];

                    if (2 * a + b + c <= 0.0) {
                        numUndefined++;
                        dist = -1;
                    } else {
                        dist = 1.0 - 2.0 * a / (2.0 * a + b + c);
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

// EOF
