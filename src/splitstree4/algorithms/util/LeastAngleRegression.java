/*
 * LeastAngleRegression.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.util.matrix.ActiveSet;

import java.util.BitSet;
import java.util.Vector;

/**
 * Least Angle Regression
 * <p/>
 * Carries out positive lasso Least Angle Regression (LAR) for a given design matrix and variable vector.
 */
public class LeastAngleRegression {

	private final Matrix X;    //Design matrix
	private final Matrix y;    //Variable(?) vector
	private final Matrix yLambda;
	private Matrix beta;
	private final String normalisation;
	private final Vector modelSpecs;
	private Matrix betaAIC;
	public double valAIC;
	private BitSet forcedSplits;
	private boolean verbose = false;


	//The following are introduced just for the simulation experiments
	//We consider three optima, one for each sigma method, and one for the full.
	public double aicVar1;
	public double aicVar2;
    public double aicVar3;

    public double aic1;
    public double aic2;
    public double aic3;
    public double aicf;


    public double res1;
    public double res2;
    public double res3;
    public double resf;

    public int nsplits1;
    public int nsplits2;
    public int nsplits3;
    public int nsplitsf;

    public int ntriv1;
    public int ntriv2;
    public int ntriv3;
    public int ntrivf;

    public int splitSelection = 1;
    public int percent = 100;


    public LeastAngleRegression(Matrix Xmatrix, Matrix yvector) {
        X = Xmatrix.copy();
        y = yvector.copy();
        yLambda = y;
        modelSpecs = new Vector();
        betaAIC = null;
        normalisation = null;
        //apply();
    }

    public LeastAngleRegression(Matrix Xmatrix, Matrix yvector, String normalisation, BitSet forced) {
        X = Xmatrix.copy();
        y = yvector.copy();
        yLambda = y;
        modelSpecs = new Vector();
        betaAIC = null;
        this.normalisation = normalisation;
        forcedSplits = (BitSet) forced.clone();

        //apply();
    }

    public LeastAngleRegression(Matrix Xmatrix, Matrix yvector, Matrix yLambda, String normalisation, BitSet forced, boolean verbose, double var) {
        X = Xmatrix.copy();
        y = yvector.copy();
        this.yLambda = yLambda;
        modelSpecs = new Vector();
        betaAIC = null;
        this.normalisation = normalisation;
        forcedSplits = (BitSet) forced.clone();
        aicVar3 = var;
        this.verbose = verbose;
        //apply();
    }


    private Matrix getBeta() {
        return beta;
    }

    public BitSet getForcedSplits() {
        return forcedSplits;
    }

    public void setForcedSplits(BitSet forcedSplits) {
        this.forcedSplits = forcedSplits;
    }

    public double[] getBetaIterate(int iter) {
        int p = beta.getColumnDimension();
        double[] beta_final = new double[p];
        for (int i = 0; i < p; i++)
            beta_final[i] = beta.get(iter, i);
        return beta_final;
    }

    public double[] getBetaAIC() {
        int p = betaAIC.getRowDimension();
        double[] beta_final = new double[p];
        for (int i = 0; i < p; i++)
            beta_final[i] = betaAIC.get(i, 0);
        return beta_final;
    }


    public double getMSE(Matrix yvector) {
        Matrix diff = (X.times(betaAIC).minus(yvector));
        return (diff.transpose().times(diff)).get(0, 0);
    }


    /**
     * Returns index of maximum entry in a vector matrix (only looks in first column) for which the mask
     * is set. Returns -1 if none are unmasked
     *
     * @param v    vector (Matrix)
     * @param mask Bitset indicating masked elements
     * @return int index of maximum entry in first column or -1 if all mask is not set for any.
     */
    private int getIndexOfMax(Matrix v, BitSet mask) {
        int n = v.getRowDimension();
        int maxi = -1;
        double maxval = 0;
        for (int i = 0; i < n; i++) {
            if (!mask.get(i))
                continue;
            double v_i = v.get(i, 0);
            if (maxi < 0 || v_i > maxval) {
                maxi = i;
                maxval = v_i;
            }

        }
        return maxi;
    }

    /**
     * Takes a matrix and a mask for the rows and columns. Returns the induced matrix with
     * those rows and columns for which the bitset element has been set. A null bitset
     * means include all the rows/columns.
     *
     * @param v       Matrix.
     * @param rowMask BitSet.
     * @param colMask BitSet
     * @return Matrix reduced matrix.
     */
    private Matrix subMatrix(Matrix v, BitSet rowMask, BitSet colMask) {
        int nrows = v.getRowDimension();
        int ncols = v.getColumnDimension();

        int new_nrows = nrows;
        if (rowMask != null)
            new_nrows = rowMask.cardinality();

        int new_ncols = ncols;
        if (colMask != null)
            new_ncols = colMask.cardinality();

        if (new_nrows == 0 || new_ncols == 0)
            return null;

        Matrix new_v = new Matrix(new_nrows, new_ncols);
        int new_i = 0;
        for (int i = 0; i < nrows; i++) {
            if (rowMask == null || rowMask.get(i)) {
                int new_j = 0;
                for (int j = 0; j < ncols; j++) {
                    if (colMask == null || colMask.get(j)) {
                        new_v.set(new_i, new_j, v.get(i, j));
                        new_j++;
                    }
                }
                new_i++;
            }
        }
        return new_v;
    }


    /**
     * Returns n by m matrix of ones
     *
     * @param n number of rows
     * @param m number of columns
     * @return n x m matrix of ones.
     */
    private Matrix ones(int n, int m) {
        Matrix v = new Matrix(n, m);
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                v.set(i, j, 1.0);
        return v;
    }

    /**
     * returns the sum of the entries in v, assumed to be a column vector.
     *
     * @param v Matrix, assumed to be column vector.
     * @return sum of entries in v.
     */
    private double sum_vector(Matrix v) {
        int n = v.getRowDimension();
        assert (v.getColumnDimension() == 1);
        double total = 0.0;
        for (int i = 0; i < n; i++)
            total += v.get(i, 0);
        return total;
    }

    /**
     * Takes a matrix v and embeds it in a larger matrix, with zeros used as padding.  The first row of v
     * is copied into the row of the larger matrix given by the first set bit of rowMask. The second row
     * is copued into the row for the second set bit, and so on. Likewise for columns.
     * <p/>
     * Hence we must have
     * rowMask.cardinality() = num rows of v \leq nrows
     * and
     * colMask.cardinality() = num cols of v \leq ncols
     *
     * @param nrows   Number of rows in new matrix.
     * @param ncols   Number of columns in new matrix.
     * @param rowMask Mask indicating which rows of new matrix correspond to rows of v.
     * @param colMask Mask indicating which cols of new matrix correspond to cols of v.
     * @param v       Matrix to be embedded
     * @return Matrix   Full matrix
     */

    Matrix embedMatrix(int nrows, int ncols, BitSet rowMask, BitSet colMask, Matrix v) {
        Matrix v_new = new Matrix(nrows, ncols);
        setSubMatrix(v_new, rowMask, colMask, v);
        return v_new;
    }

    /**
     * Works opposite to subMatrix. The values in subMatrix are copied into the full matrix,
     * so that the i,j'th entry in the subMatrix is copied into the entry indexed by the ith value in the
     * rowMask and jth value in the column mask. If rowMask is null, it is assumed that the subMatrix
     * has the same row dimension as the full matrix. Likewise for the colMask.
     *
     * @param full      Large matrix.
     * @param rowMask   Bitset indicating which rows of the large matrix to set
     * @param colMask   Bitset indicating which cols of the large matrix to set
     * @param subMatrix Matrix with same number of rows and cols as elements in rowMask and colMask
     */
    private void setSubMatrix(Matrix full, BitSet rowMask, BitSet colMask, Matrix subMatrix) {
        if (rowMask == null)
            assert (subMatrix.getRowDimension() == full.getRowDimension());
        else
            assert (subMatrix.getRowDimension() == rowMask.cardinality());

        if (colMask == null)
            assert (subMatrix.getColumnDimension() == full.getColumnDimension());
        else
            assert (subMatrix.getColumnDimension() == colMask.cardinality());


        int i_red = 0;
        for (int i = 0; i < full.getRowDimension(); i++) {
            if (rowMask == null || rowMask.get(i)) {
                int j_red = 0;
                for (int j = 0; j < full.getColumnDimension(); j++) {
                    if (colMask == null || colMask.get(j)) {
                        full.set(i, j, subMatrix.get(i_red, j_red));
                        j_red++;
                    }
                }
                i_red++;
            }
        }
    }


    public void apply() {

        /* The following is generic LARS code, applied to X and y */
        int n = X.getRowDimension();
        int p = X.getColumnDimension();
        int nvars = java.lang.Math.min(n, p);
        int maxk = 8 * nvars; //Max number of iterations

        //Normalise. DIvide each column by its norm.
        Matrix weights = ones(1, n).times(X.arrayTimes(X));
        for (int c = 0; c < p; c++) {
            double weight;
            if (normalisation.equalsIgnoreCase("euclidean"))
                weight = 1.0 / Math.sqrt(weights.get(0, c));
            else
                weight = 1.0 / (weights.get(0, c));

            weights.set(0, c, weight);
            //double norm = (norms.get(0,c));
            for (int r = 0; r < n; r++) {
                X.set(r, c, X.get(r, c) * weight);
            }
        }

        beta = new Matrix(2 * nvars, p); //Assume that stop=0, i.e. no stopping condition.
        Matrix mu = new Matrix(n, 1);   //Current position as LARS travels towards lsq solution.
        BitSet I = new BitSet(p); //Inactive set
        I.set(0, p);
        BitSet A = new BitSet(p); //Active set

        Matrix Gram = X.transpose().times(X);  //Pre-compute gram matrix


        boolean haveHitBoundary = false; //LASSO condition

        int k = 0;    //Iteration count
        int vars = 0; //Current number of variables

        //System.out.println("Step\tAdded\tDropped\tResidual\tActive set size\tActive set");

        while (vars < nvars && k < maxk) {
            k = k + 1;
            Matrix c = X.transpose().times(y.minus(mu));

            // Matrix diff = y.minus(mu);
            //double residual=sum_vector(diff.arrayTimes(diff));

            int j = getIndexOfMax(c, I);
            double C = c.get(j, 0);

            assert (j >= 0); ///ASSERT I should be non-empty
            //Note j is relative to the full vector, not just those that are inactive.

            if (!haveHitBoundary) {     //add variable
                A.set(j, true);
                I.set(j, false);
                vars++;
                //System.out.println(""+k+"\t"+j+"\t\t"+residual+"\t"+vars+"\t");
            }

            //System.out.println("A = "+A);
            //System.out.println("reduced matrix dim = "+subMatrix(Gram,A,A).getColumnDimension()+ " vars = "+vars+ " A.card = "+A.cardinality());
            Matrix Gram_A_A = subMatrix(Gram, A, A);
            Matrix GA1 = (Gram_A_A.inverse()).times(ones(vars, 1));
            double AA = 1.0 / java.lang.Math.sqrt((ones(1, vars).times(GA1)).get(0, 0));
            Matrix w = GA1.times(AA);  //weights applied to each active variable to get equiangular direction

            Matrix u = subMatrix(X, null, A).times(w); //equiangular direction (unit vector)

            double gamma = C / AA; //This is the full OLS step

            if (vars != nvars) {
                Matrix a = X.transpose().times(u);
                for (int i = 0; i < p; i++) {
                    if (I.get(i)) {
                        double temp = (C - c.get(i, 0)) / (AA - a.get(i, 0));
                        if (temp > 0 && temp < gamma)
                            gamma = temp;
                    }
                }
            }

            haveHitBoundary = false;

            double gamma_tilde = C / AA;
            int index_A = 0; //index with respect to elements in A... needed to access w.
            for (int i = 0; i < p; i++) {
                if (A.get(i)) {
                    double temp = -beta.get(k - 1, i) / w.get(index_A, 0);
                    if (temp > 0 && temp < gamma_tilde) {
                        j = i;  //NOTE: unlike in matlab code, this is the index w.r.t. full matrix
                        gamma_tilde = temp;
                    }
                    index_A++;
                }
            }


            if (gamma_tilde < gamma) {
                gamma = gamma_tilde;
                haveHitBoundary = true;
            }


            if (!haveHitBoundary) {
                //Evaluate the AIC of this model, first optimising without the lasso constraint.
                Matrix X_A = subMatrix(X, null, A);
                Matrix XAty = X_A.transpose().times(y);
                ActiveSet activeSetSolver = new ActiveSet(Gram_A_A, XAty, true);
                Matrix b = activeSetSolver.getSoln();
                Matrix df = (X_A.times(b)).minus(yLambda);
                double res = (df.transpose().times(df)).get(0, 0);
                Matrix fullb = embedMatrix(X.getColumnDimension(), 1, A, null, b);
                int numvars = 0;
                BitSet actualA = new BitSet();
                for (int i = 0; i < fullb.getRowDimension(); i++) {
                    if (fullb.get(i, 0) > 0.0) {
                        actualA.set(i);
                        numvars++;
                    }
                }

                ModelSpecs mspecs = new ModelSpecs();
                mspecs.active = (BitSet) actualA.clone();
                int nvars2 = mspecs.active.cardinality();

                mspecs.residual = res;
                mspecs.beta = fullb.copy();
                modelSpecs.add(mspecs);

                //System.out.println("AIC NUMBERS: res = "+res+"\tnumvars = "+numvars+"\t " + "initial vars = "+A.cardinality());

            }

            mu.plusEquals(u.times(gamma));
            if (beta.getRowDimension() < k) {
                //Increase the size of beta by adding one row.
                Matrix newbeta = new Matrix(beta.getRowDimension() + 1, p);
                newbeta.setMatrix(0, beta.getRowDimension() - 1, 0, p - 1, beta);
                beta = newbeta;
            }

            //NOTE: as we index rows from 0,1,2,..., the values of k here are one less than in the matlab code
            Matrix oldBetaA = subMatrix(beta.getMatrix(k - 1, k - 1, 0, p - 1), null, A);
            Matrix newbetaA = oldBetaA.plus(w.transpose().times(gamma));
            Matrix newbeta = embedMatrix(1, p, null, A, newbetaA);
            beta.setMatrix(k, k, 0, p - 1, newbeta);


            if (gamma == C / AA) {
                break;
            }

            // If LASSO condition satisfied, drop variable from active set
            if (haveHitBoundary) {
                I.set(j);
                A.set(j, false);
                vars--;

                // diff = y.minus(mu);
                //residual=sum_vector(diff.arrayTimes(diff));

                //System.out.println(""+k+"\t\t"+j+"\t"+residual+"\t"+vars+"\t");
            }


        }

        //COMPUTE AIC coefficients.

        int sampleSize = y.getRowDimension();

        //Full model to estimate var1 using AIC book pg 63.
        ModelSpecs mspecs = (ModelSpecs) modelSpecs.lastElement();
        double res = mspecs.residual;
        BitSet active = mspecs.active;
        int fulln = mspecs.active.cardinality();
        double var1 = res / ((double) sampleSize - fulln);

        //Next variance estimate is the sandwich estimator.
        Matrix X_A = subMatrix(X, null, active);
        Matrix b_A = subMatrix(mspecs.beta, active, null);
        Matrix r = (X_A.times(b_A)).minus(y);
        Matrix R = new Matrix(r.getRowDimension(), r.getRowDimension());
        for (int i = 0; i < r.getRowDimension(); i++)
            R.set(i, i, r.get(i, 0));
        Matrix B = (X_A.transpose().times(X_A)).inverse().times(sampleSize);
        Matrix M = R.times(X_A);
        M = M.transpose().times(M).times(1.0 / (double) sampleSize);
        Matrix S = B.times(M).times(B).times(1.0 / (double) sampleSize);
        Matrix ones = new Matrix(fulln, 1);
        for (int i = 0; i < fulln; i++)
            ones.set(i, 0, 1.0);
        double var2top = ones.transpose().times(S).times(ones).get(0, 0);
        double var2bot = ones.transpose().times(B.times(1.0 / sampleSize)).times(ones).get(0, 0);
        double var2 = var2top / var2bot;

        //System.out.println("var1 = "+ var1 +" var2=" + var2 +  " var3=" + aicSigma3);

        double min_aic1 = 0.0;
        double min_aic2 = 0.0;
        double min_aic3 = 0.0;

        ModelSpecs minSpec1 = null;
        ModelSpecs minSpec2 = null;
        ModelSpecs minSpec3 = null;

        ModelSpecs fullSpec;

        for (Object modelSpec : modelSpecs) {
            mspecs = (ModelSpecs) modelSpec;
            int nparam = mspecs.active.cardinality();
            mspecs.aic1 = mspecs.residual / ((double) sampleSize * var1) + (2.0 * mspecs.active.cardinality()) / (double) sampleSize;
            mspecs.aic2 = mspecs.residual / ((double) sampleSize * var2) + (2.0 * mspecs.active.cardinality()) / (double) sampleSize;
            mspecs.aic3 = mspecs.residual / ((double) sampleSize * aicVar3) + (2.0 * mspecs.active.cardinality()) / (double) sampleSize;

            // double aic3 =  mspecs.residual/((double)sampleSize * 0.001)+(2.0*mspecs.active.cardinality())/(double)sampleSize;
            //double aic3 = mspecs.aic2;
            if (verbose)
                System.out.println("AIC calculation>>> nparam = " + nparam + "\t aic1 = " + mspecs.aic1 + " \taic2 = " + mspecs.aic2 + " \t aic3 = " + mspecs.aic3 + " res = " + mspecs.residual);
            if (minSpec1 == null || mspecs.aic1 < min_aic1) {
                min_aic1 = mspecs.aic1;
                minSpec1 = mspecs;
            }
            if (minSpec2 == null || mspecs.aic2 < min_aic2) {
                min_aic2 = mspecs.aic2;
                minSpec2 = mspecs;
            }
            if (minSpec3 == null || mspecs.aic3 < min_aic3) {
                min_aic3 = mspecs.aic3;
                minSpec3 = mspecs;
            }
        }

        //Store stuff for simulations


        BitSet activeTrivial;


        this.aicVar1 = var1;
        this.aicVar2 = var2;

        this.res1 = minSpec1.residual;
        this.aic1 = minSpec1.aic1;
        this.nsplits1 = minSpec1.active.cardinality();
        activeTrivial = (BitSet) mspecs.active.clone();
        if (forcedSplits == null)
            forcedSplits = new BitSet();
        activeTrivial.and(forcedSplits);
        this.ntriv1 = activeTrivial.cardinality();

        this.res2 = minSpec2.residual;
        this.aic2 = minSpec1.aic2;
        this.nsplits2 = minSpec2.active.cardinality();
        activeTrivial = (BitSet) minSpec2.active.clone();
        activeTrivial.and(forcedSplits);
        this.ntriv2 = activeTrivial.cardinality();

        this.res3 = minSpec3.residual;
        this.aic3 = minSpec1.aic3;
        this.nsplits3 = minSpec3.active.cardinality();
        activeTrivial = (BitSet) minSpec3.active.clone();
        activeTrivial.and(forcedSplits);
        this.ntriv3 = activeTrivial.cardinality();

        fullSpec = (ModelSpecs) modelSpecs.lastElement();
        this.resf = fullSpec.residual;
        this.aicf = fullSpec.aic3;
        this.nsplitsf = fullSpec.active.cardinality();
        activeTrivial = (BitSet) fullSpec.active.clone();
        activeTrivial.and(forcedSplits);
        this.ntrivf = activeTrivial.cardinality();


        ModelSpecs returnSpecs;

        if (splitSelection == 1) {
            returnSpecs = minSpec1;
            valAIC = minSpec1.aic1;
        } else if (splitSelection == 2) {
            returnSpecs = minSpec2;
            valAIC = minSpec2.aic2;

        } else if (splitSelection == 3) {
            returnSpecs = minSpec3;
            valAIC = minSpec3.aic3;

        } else if (splitSelection == 4) {
            returnSpecs = fullSpec;
            valAIC = fullSpec.aic1;
        } else {
            int nspecs = modelSpecs.size();
            int index = (nspecs * percent) / 100 - 1;
            if (index < 0)
                index = 0;
            returnSpecs = (ModelSpecs) modelSpecs.get(index);
            valAIC = returnSpecs.aic1;
        }
        betaAIC = returnSpecs.beta.copy();


        for (int j = 0; j < p; j++) {
            double beta_j = betaAIC.get(j, 0);
            betaAIC.set(j, 0, beta_j * weights.get(0, j));
        }

        System.out.println("Final AIC = " + valAIC);

        beta = beta.getMatrix(0, k, 0, p - 1); //Trim beta
        //Un-normalise beta
        for (int i = 0; i <= k; i++) {
            for (int j = 0; j < p; j++) {
                double beta_ij = beta.get(i, j);
                beta.set(i, j, beta_ij * weights.get(0, j));
            }
        }

        //beta.print(5,5);
        //System.out.println();


    }

	private static class ModelSpecs {
		public BitSet active;
		double residual;
		double aic1;
		double aic2;
		double aic3;
		Matrix beta;
	}

    //ToDo: Add accessors for X and y and results.
}
