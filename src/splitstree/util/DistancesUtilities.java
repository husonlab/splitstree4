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

/**
 *@author Daniel Huson and David Bryant and David Bryant
 */

package splitstree.util;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree.algorithms.characters.Characters2Distances;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

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
