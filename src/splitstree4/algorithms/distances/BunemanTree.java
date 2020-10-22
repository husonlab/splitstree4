/**
 * BunemanTree.java
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
 * @version $Id: BunemanTree.java,v 1.18 2007-09-11 12:31:07 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * @version $Id: BunemanTree.java,v 1.18 2007-09-11 12:31:07 kloepper Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: BunemanTree.java,v 1.18 2007-09-11 12:31:07 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */
package splitstree4.algorithms.distances;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.SplitsSet;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * Implements the buneman tree
 */
public class BunemanTree implements Distances2Splits {
    public final static String DESCRIPTION = "Computes the Buneman tree (Buneman 1971)";

    /**
     * Determine whether given Buneman trees can be applied to given data.
     *
     * @param taxa the taxa
     * @param d    the input distance object
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances d) {
        return taxa != null && d != null && taxa.getNtax() == d.getNtax();
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

        // ProgressDialog pd = new ProgressDialog("Buneman Tree...",""); //Set new progress bar.
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
        splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
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
                (0.5 * (Math.min(d.get(i, k) + d.get(j, m), d.get(i, m) + d.get(j, k))
                        - d.get(i, j) - d.get(k, m)));
    }
}

// EOF
