/**
 * BaseFreqDistance.java 
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
/*
* $Id: BaseFreqDistance.java,v 1.5 2008-09-10 23:50:09 bryant Exp $
*/
package splitstree.algorithms.characters;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

/**
 * Simple implementation of hamming distances
 */

public class BaseFreqDistance implements Characters2Distances {
    private boolean optionIgnoreGaps = true;

    public final static String DESCRIPTION = "Calculates distances from differences in the base composition";
    protected String TASK = "Base Frequency Distance";

    protected String getTask() {
        return TASK;
    }

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

        doc.notifySubtask(getTask());
        doc.notifySetProgress(0);
        String symbols = characters.getFormat().getSymbols();
        int nstates = symbols.length();

        int ntax = taxa.getNtax();
        double[][] baseFreqs = new double[ntax + 1][nstates];
        System.err.println("Base Frequencies");

        for (int s = 1; s <= ntax; s++) {
            System.err.print(taxa.getLabel(s) + "\t");
            double count = 0;
            for (int i = 0; i < characters.getNchar(); i++) {
                int x = symbols.indexOf(characters.get(s, i));
                if (x >= 0) {
                    double weight = characters.getCharWeight(i);
                    baseFreqs[s][x] += weight;
                    count += weight;
                }
            }

            for (int x = 0; x < nstates; x++) {
                baseFreqs[s][x] /= count;
                System.err.print("" + baseFreqs[s][x] + "\t");
            }
            System.err.println("");
        }


        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {
                double p = 0.0;
                for (int i = 0; i < nstates; i++) {
                    double pi_i = baseFreqs[s][i];
                    double pihat_i = baseFreqs[t][i];
                    //  if (pi_i>0.0 && pihat_i>0.0)
                    //    p+=pi_i * Math.log(pi_i/pihat_i) + pihat_i * Math.log(pihat_i/pi_i);
                    p += Math.abs(pi_i - pihat_i);
                }

                distances.set(s, t, p);
                distances.set(t, s, p);
            }
            doc.notifySetProgress(s * 100 / ntax);
        }
        doc.notifySetProgress(taxa.getNtax());   //set progress to 100%
// pd.close();								//get rid of the progress listener
// doc.setProgressListener(null);
        return distances;
    }


    /**
     * ignore gaps?
     *
     * @return true if gaps are ignored
     */
    public boolean getOptionignoregaps() {
        return optionIgnoreGaps;
    }

    /**
     * set option ignore gaps
     *
     * @param ignore
     */
    public void setOptionignoregaps(boolean ignore) {
        optionIgnoreGaps = ignore;
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


}

// EOF
