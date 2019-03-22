/**
 * LogHamming.java
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
* $Id: LogHamming.java,v 1.7 2007-09-11 12:30:59 kloepper Exp $
*/
package splitstree4.algorithms.characters;

import jloda.swing.util.Alert;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * @deprecated replaced do not use
 * Simple implementation of hamming distances
 */
public class LogHamming /*implements Characters2Distances */ {

    public final static String DESCRIPTION = "Calculates distances using the hamming distance.";
    private boolean ignoreGaps = true;

    /**
     * Determine whether Hamming distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null;
    }

    /**
     * Computes Hamming distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {
        Distances distances = new Distances(taxa.getNtax());
        distances.getFormat().setTriangle("both");

        doc.notifySubtask("LogHamming Distance");
        doc.notifySetProgress(0);

        int numUndefined = 0;
        for (int s = 1; s <= taxa.getNtax(); s++) {
            for (int t = s + 1; t <= taxa.getNtax(); t++) {
                String states = characters.getFormat().getSymbols();

                PairwiseCompare seqPair = new PairwiseCompare(characters, states, s, t);
                double p = 1.0;
                double[][] F = seqPair.getF();
                if (F == null)
                    numUndefined++;
                else {
                    for (int x = 0; x < seqPair.getNumStates(); x++) {
                        p = p - F[x][x];
                    }
                    if (p == 1.0)
                        numUndefined++;
                }
                double x = -Math.log(1.0 - p);
                distances.set(s, t, x);
                distances.set(t, s, x);
            }
            doc.notifySetProgress(s * 100 / taxa.getNtax());
        }
        doc.notifySetProgress(taxa.getNtax());
        if (numUndefined > 0) {
            new Alert("Warning: there were saturated or missing distances in the matrix. Proceed with caution ");
        }


        return distances;
    }

    /**
     *  ignore gaps?
     * @return
     */
    public boolean getOptionignoregaps() {
        return ignoreGaps;
    }

    /**
     * set option for ignoring gaps
     * @param ignore
     */
    public void setOptionignoregaps(boolean ignore) {
        ignoreGaps = ignore;
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
