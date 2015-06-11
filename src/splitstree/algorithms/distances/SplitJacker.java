/**
 * SplitJacker.java 
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
/**
 * @version $Id: SplitJacker.java,v 1.2 2008-03-08 07:52:57 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */
package splitstree.algorithms.distances;

import jloda.util.CanceledException;
import splitstree.algorithms.characters.Characters2Distances;
import splitstree.algorithms.characters.CharactersTransform;
import splitstree.core.Document;
import splitstree.core.SplitsSet;
import splitstree.core.TaxaSet;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.Arrays;
import java.util.Vector;

// EOF

/**
 * Implements the split decomposition method of Bandelt and Dress (1992).
 */
public class SplitJacker implements Distances2Splits {
    public final static String DESCRIPTION = "Computes the split decomposition (Bandelt and Dress 1992)";

    private int nreps = 30;

    public int getOptionNumberOfBlocks() {
        return nreps;
    }


    public void setOptionNumberOfBlocks(int nblocks) {
        this.nreps = nblocks;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param d    the input distance object
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances d) {
        //We need a valid characters block and a valid characters2distances transform.
        if (taxa == null || doc.getCharacters() == null)
            return false;

        CharactersTransform trans = doc.getAssumptions().getCharactersTransform();
        return trans instanceof Characters2Distances;

    }

    int[] taxaSet2Array(TaxaSet A) {
        int na = A.cardinality();
        int[] Ataxa = new int[na + 1];
        int i = 0;
        int j = 1;
        while (i < na) {
            if (A.get(j))
                Ataxa[i++] = j;
            j++;
        }
        return Ataxa;
    }

    private Distances[] getJacknifes(Document doc, Taxa taxa, int nreps) {

        try {


            Characters2Distances trans = (Characters2Distances) (doc.getAssumptions().getCharactersTransform());
            Characters chars = doc.getCharacters();
            int ntax = taxa.getNtax();
            Distances[] allDist = new Distances[nreps + 1];

            /* First rep is the original distances */
            allDist[0] = trans.apply(doc, taxa, chars);


            int numUnmasked = chars.getNactive();
            if (numUnmasked < 0)
                numUnmasked = chars.getNchar();
            int[] tmpMasked = new int[chars.getNchar()];
            Arrays.fill(tmpMasked, 0);


            int site = 0;
            for (int rep = 1; rep <= nreps; rep++) {
                int blockSize = Math.round((rep) * numUnmasked / nreps) - Math.round((rep - 1) * numUnmasked / nreps);
                for (int i = 0; i < blockSize; i++) {
                    do {
                        site++;
                    } while (chars.isMasked(site));
                    tmpMasked[i] = site;
                    chars.setMasked(site, true);
                }

                allDist[rep] = trans.apply(doc, taxa, chars);


                for (int i = 0; i < blockSize; i++) {
                    site = tmpMasked[i];
                    chars.setMasked(site, false);
                    tmpMasked[i] = 0;
                }

            }

            boolean printreps = false;
            if (printreps) {
                for (int rep = 0; rep <= nreps; rep++) {
                    System.out.println("\nReplicate " + rep + "\n");

                    for (int i = 1; i <= ntax; i++) {
                        for (int j = 1; j <= ntax; j++)
                            System.out.print("\t" + allDist[rep].get(i, j));
                        System.out.println();
                    }
                }

            }


            return allDist;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    double biasCorrect(double[] x) {
        double total = 0.0;
        int n = x.length - 1;
        for (int i = 1; i < x.length; i++)
            total = total + x[i];

        //return x[0];
        return (double) n * x[0] - ((double) n - 1.0) * (total / ((double) n));
    }


    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param d    the input distances
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances d) throws CanceledException {

        /* First perform the jack-knifing */
        Distances[] allDist = getJacknifes(doc, taxa, nreps);
        int ntax = d.getNtax();


        SplitsSet previous = new SplitsSet(); // list of previously computed splits
        Vector previousWeights = new Vector();
        previousWeights.add(new weightArray(nreps)); //The 0th term

        SplitsSet current; // current list of splits
        Vector currentWeights;

        TaxaSet taxa_prev = new TaxaSet(); // taxa already processed


        for (int t = 1; t <= d.getNtax(); t++) {
            // initally, just add 1 to set of previous taxa
            if (t == 1) {
                taxa_prev.set(t);
                continue;
            }

            current = new SplitsSet(t); // restart current list of splits
            currentWeights = new Vector();
            currentWeights.add(new weightArray(nreps));

            // Does t vs previous set of taxa form a split?
            TaxaSet At = new TaxaSet();
            At.set(t);

            //System.out.print("Check "+At);

            weightArray wAll = new weightArray(nreps);
            wAll.w = getIsolationIndices(t, At, taxa_prev, allDist);
            double wHat = biasCorrect(wAll.w);
            if (wHat > 0) {
                // System.out.println(" include");
                current.add((TaxaSet) (At.clone()), (float) wHat);
                currentWeights.add(wAll);

            }

            // consider all previously computed splits:
            for (int s = 1; s <= previous.getNsplits(); s++) {
                TaxaSet A = previous.getSplit(s);
                TaxaSet B = A.getComplement(t - 1);
                weightArray wOld = (weightArray) previousWeights.get(s);

                // is Au{t} vs B a split?
                A.set(t);

                //  System.out.print("Check "+A);

                wAll = new weightArray(nreps);
                wAll.w = getIsolationIndices(t, A, B, allDist);
                for (int i = 0; i <= nreps; i++) {
                    wAll.w[i] = Math.min(wAll.w[i], wOld.w[i]);
                }

                wHat = biasCorrect(wAll.w);

                if (wHat > 0.0) {
                    // System.out.println(" include");
                    current.add((TaxaSet) (A.clone()), (float) wHat);
                    currentWeights.add(wAll);
                }

                A.unset(t);

                // is A vs Bu{t} a split?
                B.set(t);

                //  System.out.print("Check "+A);

                wAll = new weightArray(nreps);
                wAll.w = getIsolationIndices(t, B, A, allDist);
                for (int i = 0; i <= nreps; i++) {
                    wAll.w[i] = Math.min(wAll.w[i], wOld.w[i]);
                }
                wHat = biasCorrect(wAll.w);

                if (wHat > 0) {
                    //System.out.println(" include");

                    current.add((TaxaSet) (B.clone()), (float) wHat);
                    currentWeights.add(wAll);
                }
            }

            // System.out.println();

            //for(int i=1;i<=current.getNsplits();i++) {
            //    System.out.println(""+i+"\t"+current.getSplit(i));
            //}
            previous = current;
            previousWeights = currentWeights;
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


    public static double[] getIsolationIndices(int t, TaxaSet A, TaxaSet B, Distances[] d) {
        int n = d.length;
        double[] w = new double[n];
        for (int i = 0; i < n; i++)
            w[i] = getIsolationIndex(t, A, B, d[i]);
        return w;
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
    public static double getIsolationIndex(int t, TaxaSet A, TaxaSet B, Distances d) {
        double min_val = Double.MAX_VALUE;

        for (int i = 1; i <= t; i++) {
            if (A.get(i)) {
                for (int j = 1; j <= t; j++)
                    if (B.get(j)) {
                        for (int k = j; k <= t; k++) {
                            if (B.get(k)) {
                                double val = getIsolationIndex(t, i, j, k, d);
                                if (val < min_val) {

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
    public static double getIsolationIndex(int i, int j, int k, int m, Distances d) {
        return 0.5 * (Math.max(d.get(i, k) + d.get(j, m), d.get(i, m) + d.get(j, k))
                - d.get(i, j) - d.get(k, m));
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

class weightArray {
    public double[] w;

    public weightArray(int nreps) {
        w = new double[nreps + 1];
    }
}
