/**
 * DistancesUtilities.java
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
 *
 * @author Daniel Huson and David Bryant and David Bryant
 */
/**
 *@author Daniel Huson and David Bryant and David Bryant
 */

package splitstree4.util;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.algorithms.characters.Characters2Distances;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

import java.io.PrintStream;
import java.util.Random;

/**
 * Methods for analyzing Distance objects
 */
public class DistancesUtilities {
    /**
     * Determines whether the given distances are a metric
     *
     * @param dist the distances object
     * @return true, if dist is a metric
     */
    static public boolean isMetric(Distances dist) {
        int n = dist.getNtax();
        for (int i = 1; i <= n; i++) {
            if (dist.get(i, i) != 0.0)
                return false;
            for (int j = 1; j < i; j++) {
                if (dist.get(i, j) <= 0.0)
                    return false;
            }
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j < i; j++) {
                for (int k = 1; k < j; k++) {
                    double dij = dist.get(i, j);
                    double dik = dist.get(i, k);
                    double djk = dist.get(j, k);
                    if ((dij > dik + djk) || (dik > dij + djk) || (djk > dij + dik))
                        return false;
                }
            }
        }
        return true;
    }

    public static double estimateSigma(Document doc, Characters2Distances dist, int nreps) {
        Document bdoc = new Document();
        bdoc.setTaxa((Taxa) doc.getTaxa().clone());
        bdoc.setAssumptions(doc.getAssumptions().clone(bdoc.getTaxa()));
        bdoc.getAssumptions().setExTaxa(null);
        bdoc.setInBootstrap(true);
        int ntax = doc.getTaxa().getNtax();

        double[][] sumD = new double[ntax + 1][ntax + 1];
        double[][] sumD2 = new double[ntax + 1][ntax + 1];

        // resample characters from original characters block:
        Random rand = new Random();


        int r = 1;
        PrintStream ps = null;
        try {
            for (r = 1; r <= nreps; r++) {
                //Sample the characters
                int len = doc.getCharacters().getNchar();
                bdoc.setCharacters(CharactersUtilities.resample(bdoc.getTaxa(), doc.getCharacters(), len, rand));
                //bdoc.getCharacters().setFormat(doc.getCharacters().getFormat());
                ps = jloda.util.Basic.hideSystemErr();//disable syserr.
                // Compute everything
                Distances bdist = dist.apply(bdoc, bdoc.getTaxa(), bdoc.getCharacters());
                for (int i = 1; i <= ntax; i++) {
                    for (int j = i + 1; j <= ntax; j++) {
                        double dij = bdist.get(i, j);
                        sumD[i][j] += dij;
                        sumD2[i][j] += dij * dij;
                    }
                }


            }
        } catch (CanceledException ex) {
            String message = "Bootstrap cancelled: only " + r + " bootstrap replicates stored";
            new Alert(message);
        } catch (OutOfMemoryError ex) {
            String message = "Out of memory error: only " + (r - 1) + " bootstraps performed";
            new Alert(message);
        } catch (Exception ex) {
            Basic.caught(ex);
        } finally {
            jloda.util.Basic.restoreSystemErr(ps);
        }

        double vsum = 0.0;
        int count = 0;
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double mu_ij = sumD[i][j] / (double) nreps;
                double vij = (double) nreps / (nreps - 1.0) * (sumD2[i][j] / (double) nreps - mu_ij * mu_ij);
                vsum += vij;
                count++;
            }
        }

        return Math.sqrt(vsum / (double) count);


    }

}

// EOF
