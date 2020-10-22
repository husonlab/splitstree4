/**
 * LeastSquares.java
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
 * LeastSquares
 *
 * General optimization and evaluation routines for least squares computation on splits.
 *
 * Takes a collection of splits and weights.
 * 		makeTopoMatrix		Computes topological matrix, which is then available through
 * 		getTopoMatrix. NOTE: rows and columns are indexed 0...n-1
 * 		optimizeLS(constrain)  Replaces values in weights to optimal WLS weights, constrained
 * 			to be non-negative if constrain is set to true.
 *
 *
 */
package splitstree4.algorithms.util;

import Jama.Matrix;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.util.matrix.ActiveSet;

/**
 * @author David Bryant
 */
public class LeastSquares {
    /* ------------------------
       Class variables
     * ------------------------ */


    static final double EPSILON = 1e-10;


    /**
     * Return matrix A'A, where A is the topological matrix for the set of splits.
     * Algorithm takes O(m^2n) time  where m=number of splits and n is number of taxa.
     *
     * @param splits Splits blockk
     * @return Matrix. The matrix A'A
     */

    static private Matrix getTopoMatrixOLS(Splits splits) {
        int nsplits = splits.getNsplits();
        int ntaxa = splits.getNtax();

        Matrix Amat = new Matrix(nsplits, nsplits);
        TaxaSet I, J;
        int Isize, Jsize;
        for (int i = 0; i < nsplits; i++) {
            I = splits.get(i + 1); //Note - indices off by one.
            Isize = I.cardinality();
            for (int j = 0; j < i; j++) {
                J = (TaxaSet) splits.get(j + 1).clone();
                Jsize = J.cardinality();
                J.and(I);
                int x = J.cardinality(); //Size of intersection
                int Aij = x * (ntaxa - Isize - Jsize + x) + (Isize - x) * (Jsize - x);
                Amat.set(i, j, (double) Aij);
                Amat.set(j, i, (double) Aij);
            }
            Amat.set(i, i, Isize * (ntaxa - Isize));
        }
        return Amat;
    }

    /**
     * Computes matrix A'WA where W is a diagonal matrix with the reciprocal of
     * the variances on the diagonal. The variances are retrieved from the distances
     * block.
     * Algorithm takes O(m^2n^2) where m is the number of splits and n is the number of taxa.
     *
     * @param splits Splits
     * @param dist   The only use of the distances is to get the distance variances.
     * @return Matrix The matrix A'WA
     */

    static private Matrix getTopoMatrixWLS(Splits splits, Distances dist) {
        int nsplits = splits.getNsplits();
        int ntaxa = splits.getNtax();

        if (splits.getNtax() != dist.getNtax())
            throw new IllegalArgumentException("Splits and distances have different numbers of taxa");


        Matrix Amat = new Matrix(nsplits, nsplits);
        TaxaSet I, J;
        for (int i = 0; i < nsplits; i++) {
            I = splits.get(i + 1); //Note - indices off by one.
            for (int j = i; j < nsplits; j++) {
                J = splits.get(j + 1);
                double Aij = 0.0;
                for (int a = 1; a <= ntaxa; a++) {
                    for (int b = a + 1; b <= ntaxa; b++) {
                        if ((I.get(a) != I.get(b)) && (J.get(a) != J.get(b)))
                            Aij += 1.0 / dist.getVar(a, b);
                    }
                }
                Amat.set(i, j, Aij);
                Amat.set(j, i, Aij);
            }
        }
        return Amat;
    }

    /**
     * Computes A'diag(w)A
     */


    /**
     * Computes matrix A'WA where W is a diagonal matrix with diagonal elements given by the vector w
     * and A is the topological matrix for the splits
     * <p/>
     * Algorithm takes O(m^2n^2) where m is the number of splits and n is the number of taxa.
     *
     * @param splits The splits block
     * @param w      Elements used for the diagonal matrix W.
     * @return Matrix A'WA, where A is the topological matrix for the splits.
     */
    static private Matrix getAtWA(Splits splits, Matrix w) {
        int nsplits = splits.getNsplits();
        int ntaxa = splits.getNtax();

        if (w.getRowDimension() != (ntaxa * (ntaxa - 1)) / 2)
            throw new IllegalArgumentException("Weight vector of wrong dimension");

        Matrix Amat = new Matrix(nsplits, nsplits);
        TaxaSet I, J;
        for (int i = 0; i < nsplits; i++) {
            I = splits.get(i + 1); //Note - indices off by one.
            for (int j = i; j < nsplits; j++) {
                J = splits.get(j + 1);
                int pair = 0;
                double Aij = 0.0;
                for (int a = 1; a <= ntaxa; a++)
                    for (int b = a + 1; b <= ntaxa; b++) {
                        if ((I.get(a) != I.get(b)) && (J.get(a) != J.get(b)))
                            Aij += w.get(pair, 0);     //a and b are on opposite sides of I

                        pair++;
                    }
                Amat.set(i, j, Aij);
                Amat.set(j, i, Aij);
            }
        }
        return Amat;
    }


    /**
     * Returns Atd where A is the topological matrix for the splits and W is the
     * diagonal matrix with 1/var on the diagonal. Variances are taken from the
     * distances block.
     *
     * @param splits Splits
     * @param dist   Distances
     * @return Matrix (vector) AtWd, using variances from the distances block
     */

    static private Matrix getAtWd(Splits splits, Distances dist) {
        int nsplits = splits.getNsplits();
        int ntaxa = splits.getNtax();

        if (splits.getNtax() != dist.getNtax())
            throw new IllegalArgumentException("Splits and distances have different numbers of taxa");


        Matrix AtWd = new Matrix(nsplits, 1);
        double AtWdi;
        TaxaSet I;
        for (int i = 0; i < nsplits; i++) {
            AtWdi = 0.0;
            I = splits.get(i + 1); //Note - indices off by one.
            for (int a = 1; a <= ntaxa; a++) {
                if (!I.get(a))
                    continue;
                for (int b = 1; b <= ntaxa; b++) { //Note - have to loop through all 1..ntaxa
                    if (I.get(b))
                        continue;
                    //At this point, we know a and b are on opposite sides of I
                    AtWdi += dist.get(a, b) / dist.getVar(a, b);
                }
            }
            AtWd.set(i, 0, AtWdi);
        }
        return AtWd;
    }


    /**
     * Computes Atv, where A is the topological matrix for the splits and v is a matrix or vector,
     *
     * @param splits Splits block
     * @param v      Vector, indexed by pairs of taxa (12,13,14,...,23,...)
     * @return matrix  A^tv
     */

    static private Matrix getAtv(Splits splits, Matrix v) throws IllegalArgumentException {
        int nsplits = splits.getNsplits();
        int ntax = splits.getNtax();
        int npairs = ntax * (ntax - 1) / 2;
        int ncols = v.getColumnDimension();


        if (v.getRowDimension() != npairs)
            throw new IllegalArgumentException("Row dimension incorrect for getAtv");

        Matrix Atv = new Matrix(nsplits, 1);
        TaxaSet I;
        for (int i = 0; i < nsplits; i++) {
            Matrix Atvi = new Matrix(ncols, 1);
            I = splits.get(i + 1); //Note - indices off by one.
            int j = 0;
            for (int a = 1; a <= ntax; a++) {
                for (int b = a + 1; b <= ntax; b++) { //Note - have to loop through all 1..ntaxa
                    if (I.get(b) != I.get(a))
                        Atvi.plusEquals(v.getMatrix(j, j, 0, ncols - 1));
                    j++;
                }
            }
            Atv.setMatrix(i, i, 0, ncols - 1, Atvi);
        }
        return Atv;
    }


    /**
     * Computes Av where A is the topological matrix for the splits and v is a vector (one element for
     * each split). The vector returned is indexed (12,13,14,...,23,...)
     *
     * @param splits Splits block
     * @param v      Vector with n(n-1)/2 elements
     * @return vector Av
     */
    static private Matrix getAv(Splits splits, Matrix v) {
        int nsplits = splits.getNsplits();
        int ntax = splits.getNtax();
        int npairs = ntax * (ntax - 1) / 2;
        //int ncols = v.getColumnDimension();


        if (v.getRowDimension() != nsplits)
            //throw new Exception("Row dimension incorrect for getAtv");
            throw new IllegalArgumentException("Row dimension incorrect for getAv");

        Matrix Av = new Matrix(npairs, 1);
        int pair = 0;
        for (int a = 1; a <= ntax; a++) {
            for (int b = a + 1; b <= ntax; b++) { //Note - have to loop through all 1..ntaxa
                double Av_ab = 0.0;
                for (int i = 0; i < nsplits; i++) {
                    TaxaSet I = splits.get(i + 1); //Note - indices off by one.
                    if (I.get(a) != I.get(b)) {
                        Av_ab += v.get(i, 0);
                    }
                }
                Av.set(pair, 0, Av_ab);
                pair++;
            }
        }

        return Av;
    }

    /**
     *`
     *
     * @param constrain Non-negativity constraint.
     */

    /**
     * Computes the optimal least squares values for split weights, with or without a positivity constraint
     * Uses  fairly inefficient Cholesky decomposition algorithm (with updating).
     *
     * @param splits    Splits block
     * @param dist      Distances block (with same number of taxa as splits block)
     * @param constrain Flag indicating whether to constrain to non-negative weights (true) or allow negative values
     */
    //TODO: Write test unit for this.
    //ToDo: Solve using the Conjugate Gradient method
    static public void optimizeLS(Splits splits, Distances dist, boolean constrain) {

        if (splits.getNtax() != dist.getNtax())
            throw new IllegalArgumentException("Splits and distances have different numbers of taxa");

        //First compute the matrices AtWA and vector AtWd
        Matrix Amat;
        if (dist.getFormat().getVarType().equalsIgnoreCase("ols"))
            Amat = getTopoMatrixOLS(splits); //Much faster in the case of OLS
        else
            Amat = getTopoMatrixWLS(splits, dist);
        Matrix AtWd = getAtWd(splits, dist);

        //Now apply the active set method to compute the optimal weights
        ActiveSet Aset = new ActiveSet(Amat, AtWd, constrain);
        int nsplits = splits.getNsplits();
        splits.getFormat().setWeights(true);
        for (int i = 1; i <= nsplits; i++) {
            splits.setWeight(i, (float) Aset.getSoln(i - 1));
        }
        splits.getProperties().setFit(-1);
        splits.getProperties().setLSFit(-1);

    }


}
