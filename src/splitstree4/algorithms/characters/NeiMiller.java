/*
 * NeiMiller.java Copyright (C) 2022 Daniel H. Huson
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
/* $Id: NeiMiller.java,v 1.26 2008-03-17 14:22:44 bryant Exp $
 */
package splitstree4.algorithms.characters;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;


/**
 * Computes the Nei and Miller (1990) distance from a set of characters
 */
public class NeiMiller implements Characters2Distances {

    public final static String DESCRIPTION = "Calculate distances from restriction-sites using Nei and Miller (1990).";

    /**
     * Determine whether Nei-Miller distances can be computed with given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return taxa != null && chars != null
                && chars.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.STANDARD)
                && chars.hasCharweights();
    }

    /**
     * Computes Nei-Miller distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws CanceledException {

        Distances neiMiller = new Distances(taxa.getNtax());

        int nchar = characters.getNchar();
        int ntax = characters.getNtax();
        int c, i, j, k;

        boolean warned_sij = false, warned_dhij = false, warned_dist = false;

        // Determine enzyme classes etc:

        double[] class_value = new double[nchar];     // Value for given enzyme class
        int[] class_size = new int[nchar];                  // number of positions for class
        int[] char2class = new int[nchar + 1];        // Maps characters to enzyme classes
        int num_classes = 0;                    // Number of different classes

        // ProgressDialog pd = new ProgressDialog("Nei-Miller Distance...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        int maxProgress = 5 * taxa.getNtax() + characters.getNchar();
        doc.notifySubtask("NeiMiller Distance");
        doc.notifySetProgress(0);


        for (c = 1; c <= nchar; c++) {
            if (!characters.isMasked(c)) {
                boolean found = false;
                for (k = 1; k <= num_classes; k++) {
                    if (class_value[k] == characters.getCharWeight(c)) {                         // belongs to class already encountered
                        char2class[c] = k;
                        class_size[k]++;
                        found = true;
                        break;
                    }
                }
                if (!found) // new class
                {
                    ++num_classes;
                    char2class[c] = num_classes;
                    class_value[num_classes] = characters.getCharWeight(c);
                    class_size[num_classes] = 1;
                }
            }

            doc.notifySetProgress(100 * c / maxProgress);
        }

// Compute mij_k:

        int[][][] mij_k = new int[ntax + 1][ntax + 1][num_classes + 1];

        for (i = 1; i <= ntax; i++) {
            for (j = i; j <= ntax; j++) {
                for (c = 1; c <= nchar; c++) {
                    if (!characters.isMasked(c)) {
                        if (characters.get(i, c) == '1' && characters.get(j, c) == '1') {
                            mij_k[i][j][char2class[c]]++;
                        }
                    }
                }
            }

            doc.notifySetProgress((characters.getNchar() + i) * 100 / maxProgress);
        }

        // Compute sij_k  (equation 2):

        double[][][] sij_k = new double[ntax + 1][ntax + 1][num_classes + 1];
        for (i = 1; i <= ntax; i++) {
            for (j = i + 1; j <= ntax; j++) {
                for (k = 1; k <= num_classes; k++) {
                    double bot = mij_k[i][i][k] + mij_k[j][j][k];

                    if (bot != 0)
                        sij_k[i][j][k] = (2 * mij_k[i][j][k]) / bot;
                    else {
                        if (!warned_sij) {
                            System.err.println("nei_miller: denominator zero in equation (2)");
                            warned_sij = true;
                        }
                        sij_k[i][j][k] = 100000;
                    }
                }
            }

            doc.notifySetProgress((characters.getNchar() + ntax + i) * 100 / maxProgress);
        }

        // Compute dhij_k (i.e. dij_k_hat in equation (3)):

        double[][][] dhij_k = new double[ntax + 1][ntax + 1][num_classes + 1];

        for (i = 1; i <= ntax; i++) {
            for (j = i + 1; j <= ntax; j++) {
                for (k = 1; k <= num_classes; k++) {
                    if (class_value[k] == 0) {
                        dhij_k[i][j][k] = 100000;
                        if (!warned_dhij) {
                            System.err.println("nei_miller: denominator zero in equation (3)");
                            warned_dhij = true;
                        }
                    } else
                        dhij_k[i][j][k]
                                = (-Math.log(sij_k[i][j][k])) / class_value[k]; // equation (3)
                }
            }

            doc.notifySetProgress(100 * (characters.getNchar() + 2 * ntax + i) / maxProgress);
        }

        // Compute mk_k (mk_bar=(mii_k+mjj_k)/2):

        double[][][] mk_k = new double[ntax + 1][ntax + 1][num_classes + 1];

        for (i = 1; i <= ntax; i++) {
            for (j = i; j <= ntax; j++) {
                for (k = 1; k <= num_classes; k++) {
                    mk_k[i][j][k] = (mij_k[i][i][k] + mij_k[j][j][k]) / 2.0;
                }
            }

            doc.notifySetProgress((100 * characters.getNchar() + 3 * ntax + i) / maxProgress);
        }

        // Computes the distances as described in equation (4):

        for (i = 1; i <= ntax; i++) {
            for (j = i + 1; j <= ntax; j++) {
                // Computes the bottom of equation 4:
                double bottom = 0;
                for (k = 1; k <= num_classes; k++)
                    bottom += mk_k[i][j][k] * class_value[k];

                // Computes the top of equation 4:
                double top = 0;
                for (k = 1; k <= num_classes; k++)
                    top += mk_k[i][j][k] * class_value[k] * dhij_k[i][j][k];

                if (bottom != 0)
                    neiMiller.set(i, j, top / bottom);
                else {
                    if (!warned_dist) {
                        System.err.println("nei_miller: denominator zero in equation (4)");
                        warned_dist = true;
                    }
                    neiMiller.set(i, j, 1);
                }
            }

            doc.notifySetProgress(100 * (characters.getNchar() + 4 * ntax + i) / maxProgress);
        }

        doc.notifySetProgress(100);   //set progress to 100%
        return neiMiller;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}//EOF
