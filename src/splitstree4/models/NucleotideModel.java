/**
 * NucleotideModel.java
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
 * Created on May 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package splitstree4.models;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.util.Random;

/**
 * @author bryant
 *         <p/>
 *         Generic 4x4 nucleotide model, for a general Q matrix.
 *         <p/>
 *         We are given the Q matrix, which is assumed to be a valid GTR rate matrix.
 */
public abstract class NucleotideModel implements SubstitutionModel {

    final static double EPSILON = 1e-6; //Threshold for round-off error when checking matrices

    double[] freqs; /* base frequencies */
    double[] sqrtf; /* Square roots of frequencies */
    double[] evals; /* evalues of Pi^(1/2) Q Pi^(-1/2) */
    double[][] evecs; /* evectors of Pi^(1/2) Q Pi^(-1/2) */

    double[][] Pmatrix; /* Current P matrix */
    double[][] Qmatrix; /* Current Q matrix */
    double tval;
    double pinv; /* Proportion of invariant sites */
    double gamma = 0.0;

    /*------------Constructors-----------------------*/
    NucleotideModel() {
        //NULL constructor...
    }

    /**
     * Get the base frequency for state i (ranging from 0 to 3)
     *
     * @param i state (0..3)
     * @return base frequency for state i.
     */
    public double getPi(int i) {
        return freqs[i];
    }

    /**
     * Set the rate matrix and base frequencies and compute diagonalisation
     *
     * @param Q rate matrix (0..3 x 0..3). Diagonal values are ignored
     * @param f frequencies (0..3)
     */
    public void setRateMatrix(double[][] Q, double[] f) {

        //Test GTR property.
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                if (Math.abs(f[i] * Q[i][j] - f[j] * Q[j][i]) > EPSILON)
                    throw new IllegalArgumentException("Rate matrix and frequencies do not satisfy detailed balance condition");
            }
        }

        freqs = new double[4];
        sqrtf = new double[4];
        for (int i = 0; i < 4; i++) {
            freqs[i] = f[i];
            sqrtf[i] = Math.sqrt(freqs[i]);
        }

        Qmatrix = new double[4][4];
        for (int i = 0; i < 4; i++) {
            double qsum = 0.0;
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    qsum += Q[i][j];
                    Qmatrix[i][j] = Q[i][j];
                }

            }
            Qmatrix[i][i] = -qsum;
        }

        Matrix M = new Matrix(4, 4);

        /* The matrix \Pi Q is symmetric, so the matrix M = \Pi^{1/2} Q \Pi^{-1/2} will also
  be symmetric and hence easier to diagonalise*/

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j <= i; j++) {
                double x = sqrtf[i] * Q[i][j] / sqrtf[j];
                double y = sqrtf[j] * Q[j][i] / sqrtf[i];
                M.set(i, j, (x + y) / 2.0);
                if (i != j)
                    M.set(j, i, (x + y) / 2.0);
            }
        }

        EigenvalueDecomposition EX = new EigenvalueDecomposition(M);
        evals = EX.getRealEigenvalues();
        evecs = (EX.getV().getArrayCopy());

        /* Default Pvalue is for tval = 0 */
        Pmatrix = new double[4][4];
        tval = 0.0;
        for (int i = 0; i < 4; i++)
            Pmatrix[i][i] = 1.0;

    }

    /**
     * get the Q matrix
     *
     * @return double[][] Q
     */
    public double[][] getQ() {
        double[][] Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(Qmatrix[i], 0, Q[i], 0, 4);
        }
        return Q;
    }

    /**
     * Get an entry in the Q matrix (can involve computation)
     *
     * @param i first state
     * @param j second state
     * @return Q[i][j]
     */
    public double getQ(int i, int j) {
        return Qmatrix[i][j];
    }

    /**
     * Compute the transition probabilities. These can be extracted using getP
     *
     * @param t length of branch
     */
    protected void computeP(double t) {

        double[] expD = new double[4];
        for (int i = 0; i < 4; i++) {
            if (gamma <= 0.0)
                expD[i] = Math.exp(evals[i] * t);
            else
                expD[i] = Math.pow(1.0 - gamma * evals[i] * t, -1.0 - gamma);
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double Xij = 0.0;
                for (int k = 0; k < 4; k++) {
                    Xij += evecs[i][k] * expD[k] * evecs[j][k];

                }
                //System.err.println(Xij);
                Pmatrix[i][j] = (1.0 / sqrtf[i]) * Xij * sqrtf[j];
            }
        }

        //Handle invariant sites

        if (pinv != 0.0) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    Pmatrix[i][j] *= (1.0 - pinv);
                }
                Pmatrix[i][i] += pinv;
            }
        }
        tval = t;
    }

    /**
     * COmpute the X_ij value for this distance. This is the probability of observing state i at the beginning
     * and state j at the end, or pi_i P_{ij}(t).
     *
     * @param i first state (0..3)
     * @param j second state (0..3)
     * @param t time (t>=0)
     * @return double X_ij(t) value
     */
    public double getX(int i, int j, double t) {
        if (t != tval) {
            computeP(t);
        }
        return freqs[i] * Pmatrix[i][j];
    }

    /**
     * Compute the P_ij value for this distance. This is the probability of observing state state j at the end
     * conditional on state i at the beginning or P_{ij}(t).
     *
     * @param i first state (0..3)
     * @param j second state (0..3)
     * @param t time (t>=0)
     * @return double P_ij(t) value
     */
    public double getP(int i, int j, double t) {
        if (t != tval) {
            computeP(t);
        }

        return Pmatrix[i][j];
    }

    /**
     * Get proportion of invariance sites
     *
     * @return double proportion
     */
    public double getPinv() {
        return pinv;
    }

    /**
     * Set proportion of invariance sites
     *
     * @param p proportion  (double)
     */
    public void setPinv(double p) {
        if (p != pinv) {
            pinv = p;
            if (tval != 0.0)
                computeP(tval);
        }
    }

    /**
     * Get gamma parameter for site rate distribution
     *
     * @return gamma parameter
     */
    public double getGamma() {
        return gamma;
    }

    /**
     * Sets gamma parameter for site rate distribution
     *
     * @param val gamma parameter
     */
    public void setGamma(double val) {
//Note: negative gamma -> equal rates.
        if (val != gamma) {
            gamma = val;
            if (tval != 0.0) {
                computeP(tval);
            }
        }
    }

    /**
     * Gets number of states
     *
     * @return int
     */
    public int getNstates() {
        return 4;
    }

    /**
     * getRate
     * <p/>
     * Returns rate
     */
    public double getRate() {
        return (1.0 - pinv);
    }

    /**
     * Given Q and base frequencies, normalises Q so that the rate of mutation equals one.
     */
    protected void normaliseQ() {

        double r = 0.0;
        for (int i = 0; i < 4; i++)
            r += freqs[i] * Qmatrix[i][i];

        //Normalise so rate is one.
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Qmatrix[i][j] /= r;
            }
        }
        //Normalise the diagonalisation by scaling eigenvalues
        for (int i = 0; i < 4; i++)
            evals[i] /= r;

        //Recompute transition probabilities
        computeP(this.tval);
    }

    /**
     * Computes a random value according to probabilities in the base frequency vector
     *
     * @param random random generator
     * @return int random state (0..3)
     */
    public int randomPi(Random random) {
        double x = random.nextDouble();
        int i = 0;
        x -= getPi(i);
        while (x >= 0.0) {
            i++;
            x -= getPi(i);
        }
        return i;
    }

    /**
     * Given a start state, computes a random end state
     *
     * @param start  state (0..3)
     * @param t      double (length of branch)
     * @param random random number generator
     * @return int (0..3) state
     */
    public int randomEndState(int start, double t, Random random) {
        double x = random.nextDouble();
        int i = 0;
        x -= getP(start, i, t);
        while (x >= 0.0) {
            i++;
            x -= getP(start, i, t);
        }
        return i;
    }

    /**
     * is this a group valued model
     *
     * @return true, if group valued model
     */
    public boolean isGroupBased() {
        return false;
    }

}
