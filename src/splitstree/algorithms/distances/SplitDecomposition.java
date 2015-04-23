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
 * @version $Id: SplitDecomposition.java,v 1.24 2007-09-11 12:31:07 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */
package splitstree.algorithms.distances;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsSet;
import splitstree.core.TaxaSet;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * Implements the split decomposition method of Bandelt and Dress (1992).
 */
public class SplitDecomposition implements Distances2Splits {
    public final static String DESCRIPTION = "Computes the split decomposition (Bandelt and Dress 1992)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param d    the input distance object
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances d) {
        return taxa != null && d != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param d    the input distances
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances d) throws CanceledException {
        SplitsSet previous = new SplitsSet(); // list of previously computed splits
        SplitsSet current; // current list of splits
        TaxaSet taxa_prev = new TaxaSet(); // taxa already processed

        // ProgressDialog pd = new ProgressDialog("Split Decomposition...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(taxa.getNtax());    //initialize maximum progress
        doc.notifySetProgress(0);

        for (int t = 1; t <= d.getNtax(); t++) {
            // initally, just add 1 to set of previous taxa
            if (t == 1) {
                taxa_prev.set(t);
                continue;
            }

            current = new SplitsSet(t); // restart current list of splits

            // Does t vs previous set of taxa form a split?
            TaxaSet At = new TaxaSet();
            At.set(t);

            float wgt = getIsolationIndex(t, At, taxa_prev, d);
            if (wgt > 0) {
                current.add((TaxaSet) (At.clone()), wgt);
            }

            // consider all previously computed splits:
            for (int s = 1; s <= previous.getNsplits(); s++) {
                TaxaSet A = previous.getSplit(s);
                TaxaSet B = A.getComplement(t - 1);

                // is Au{t} vs B a split?
                A.set(t);
                wgt = Math.min(previous.getWeight(s), getIsolationIndex(t, A, B, d));
                if (wgt > 0) {
                    current.add((TaxaSet) (A.clone()), wgt);
                }
                A.unset(t);

                // is A vs Bu{t} a split?
                B.set(t);
                wgt = Math.min(previous.getWeight(s), getIsolationIndex(t, B, A, d));
                if (wgt > 0) {
                    current.add((TaxaSet) (B.clone()), wgt);
                }
            }
            previous = current;
            taxa_prev.set(t);
            doc.notifySetProgress(t);
        }

        // copy splits to splits
        Splits splits = new Splits(taxa.getNtax());
        splits.addSplitsSet(previous);
        //System.err.println(" "+splits.splits.getNsplits());

        doc.notifySetProgress(taxa.getNtax());   //set progress to 100%
// pd.close();								//get rid of the progress listener
// // doc.setProgressListener(null);

        return splits;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Returns the isolation index for Au{x} vs B
     *
     * @param t maximal taxon index, assumed to be contained in set A
     * @param A set A
     * @param B set B
     * @param d Distance matrix
     * @return the isolation index
     */
    public static float getIsolationIndex(int t, TaxaSet A, TaxaSet B, Distances d) {
        float min_val = Float.MAX_VALUE;

        for (int i = 1; i <= t; i++) {
            if (A.get(i)) {
                for (int j = 1; j <= t; j++)
                    if (B.get(j)) {
                        for (int k = j; k <= t; k++) {
                            if (B.get(k)) {
                                float val = getIsolationIndex(t, i, j, k, d);
                                if (val < min_val) {
                                    if (val <= 0.0000001)
                                        return 0;
                                    min_val = val;
                                }
                            }
                        }
                    }
            }
        }
        return min_val;
    }

    /**
     * Returns the isolation index of i,j vs k,l
     *
     * @param i a taxon
     * @param j a taxon
     * @param k a taxon
     * @param m a taxon
     * @param d Distance matrix
     * @return the isolation index
     */
    public static float getIsolationIndex(int i, int j, int k, int m, Distances d) {
        return (float)
                (0.5 * (Math.max(d.get(i, k) + d.get(j, m), d.get(i, m) + d.get(j, k))
                        - d.get(i, j) - d.get(k, m)));
    }
}

// EOF
