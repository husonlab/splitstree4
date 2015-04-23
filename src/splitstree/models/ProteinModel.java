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

/*
 * Created on May 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package splitstree.models;

import java.util.Random;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 */

//ToDO: Copy changes and java doc from Nucleotide Model

public abstract class ProteinModel implements SubstitutionModel {

    double[] freqs; /* base frequencies */
    double[] sqrtf; /* Square roots of frequencies */
    double[] evals; /* evalues of Pi^(1/2) Q Pi^(-1/2) */
    double[][] evecs; /* evectors of Pi^(1/2) Q Pi^(-1/2) */

    double[][] Pmatrix; /* Current P matrix */
    double tval;

    double gamma; /* Gamma parameter. This is 0 for no gamma */
    double pinv; /* Proportion of invariant sites */

    /*------------Constructors-----------------------*/
    ProteinModel() {

        //NULL CONSTRUCTOR
    }

    /**
     * init
     * <p/>
     * Sets P matrix to the identity, tval to zero, and computes sqrts of pi values.
     */
    protected void init() {

        tval = 0.0;
        Pmatrix = new double[20][20];
        for (int i = 0; i < 20; i++)
            for (int j = 0; j < 20; j++)
                if (i == j)
                    Pmatrix[i][j] = 1.0;
                else
                    Pmatrix[i][j] = 0.0;

        sqrtf = new double[20];
        for (int i = 0; i < 20; i++)
            sqrtf[i] = Math.sqrt(freqs[i]);
        pinv = 0.0;
        gamma = 0.0;
    }

    public double getPi(int i) {
        return freqs[i];
    }

    /**
     * computeP
     *
     * @param t Computes the P matrix for time t.
     *          <p/>
     *          If V = evecs and D = diag(evals) then
     *          V'DV = Pi^(1/2) Q Pi^(-1/2)
     *          <p/>
     *          Hence
     *          V' exp(D) V = Pi^(1/2) exp(Q) Pi^(-1/2)
     *          and
     *          P(t) = Pi^(-1/2) V' exp(D) V Pi^(1/2)
     */
    protected void computeP(double t) {

        double[] expD = new double[20];
        for (int i = 0; i < 20; i++) {
            if (this.gamma > 0)
                expD[i] = Math.pow(1.0 - gamma * evals[i] * t, -1.0 - gamma);
            else
                expD[i] = Math.exp(evals[i] * t);
        }

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                double Xij = 0.0;
                for (int k = 0; k < 20; k++) {
                    Xij += evecs[i][k] * expD[k] * evecs[j][k];

                }
                //System.err.println(Xij);
                Pmatrix[i][j] = (1.0 / sqrtf[i]) * Xij * sqrtf[j];
            }
        }

        //Handle invariant sites

        if (pinv != 0.0) {
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 20; j++) {
                    Pmatrix[i][j] *= (1.0 - pinv);
                }
                Pmatrix[i][i] += pinv;
            }
        }
        tval = t;
    }

    /**
     * get the Q matrix (can involve some calculation/roundoff error)
     *
     * @return double[][] Q
     */
    public double[][] getQ() {
        double[][] Q = new double[20][20];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                double Xij = 0.0;
                for (int k = 0; k < 20; k++) {
                    Xij += evecs[i][k] * evals[k] * evecs[j][k];

                }
                //System.err.println(Xij);
                Q[i][j] = (1.0 / sqrtf[i]) * Xij * sqrtf[j];
            }
        }
        return Q;
    }

    /**
     * Get an entry in the Q matrix (can involve computation)
     *
     * @param i
     * @param j
     * @return Q[i][j]
     */
    public double getQ(int i, int j) {
        double Xij = 0.0;
        for (int k = 0; k < 4; k++) {
            Xij += evecs[i][k] * evals[k] * evecs[j][k];

        }
        //System.err.println(Xij);
        return (1.0 / sqrtf[i]) * Xij * sqrtf[j];
    }


    public double getX(int i, int j, double t) {
        if (t != tval) {
            computeP(t);
        }
        return freqs[i] * Pmatrix[i][j];
    }


    public double getP(int i, int j, double t) {
        if (t != tval) {
            computeP(t);
        }

        return Pmatrix[i][j];
    }


    public double getPinv() {
        return pinv;
    }

    public void setPinv(double p) {
        if (p != pinv) {
            pinv = p;
            if (tval != 0.0)
                computeP(tval);
        }
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double val) {
        if (gamma != val) {
            gamma = val;
            if (tval != 0.0)
                computeP(tval);
        }
    }

    public int getNstates() {
        return 20;
    }

    /**
     * getRate
     * <p/>
     * Returns rate
     */
    public double getRate() {
        return (1.0 - pinv);
    }

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
