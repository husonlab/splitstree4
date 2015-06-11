/**
 * PairwiseCompare.java 
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
/*
 * Created on May 11, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package splitstree.algorithms.characters;

import splitstree.core.SplitsException;
import splitstree.models.SubstitutionModel;
import splitstree.nexus.Characters;

import java.util.Random;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */

//ToDo: Add support for CharWeights


public class PairwiseCompare {

    public static final int IGNOREAMBIG = 0;
    public static final int AVERAGEAMBIG = 1;
    public static final int MATCHAMBIG = 2;


    private int numStates;
    private int numNotMissing;
    private int numActive;
    private double[][] Fcount; /* Stored as doubles, to handle ambiguities and character weights*/


    //Experimental code - pairwise bootstrapping for computation of variance.
    private double[][] originalFcount;
    private int originalNumNotMissing;


    public PairwiseCompare(Characters characters, String states, int i, int j) throws SplitsException {
        initialise(characters, states, i, j, IGNOREAMBIG);
    }

    public PairwiseCompare(Characters characters, String states, int i, int j, int handleAmbig) throws SplitsException {
        initialise(characters, states, i, j, handleAmbig);
    }


    /**
     * Constructor - analyses sequences, counts differences
     *
     * @param characters
     * @param i           ID of sequence being compared
     * @param j           ID of sequence being compared
     * @param handleAmbig handle ambiguous codes. if false, these are treated as missing.
     */

    private void initialise(Characters characters, String states, int i, int j, int handleAmbig) throws SplitsException {

        String symbols;
        char gapchar;
        char missingchar;
        int gapindex, missingindex;

        symbols = states;
        numStates = states.length();

        /* The Fcount matrix has rows and columns for missing and gap states as well */
        Fcount = new double[numStates + 2][numStates + 2];
        originalFcount = new double[numStates + 2][numStates + 2];


        gapindex = numStates;
        missingindex = numStates + 1;

        missingchar = characters.getFormat().getMissing();
        gapchar = characters.getFormat().getGap();
        numNotMissing = 0;
        numActive = 0;

        char[] seqi = characters.getRow(i);
        char[] seqj = characters.getRow(j);


        for (int k = 1; k <= characters.getNchar(); k++) {
            if (!characters.isMasked(k)) {
                numActive = numActive + 1;
                char ci = seqi[k];
                char cj = seqj[k];
                double charWeight = characters.getCharWeight(k);

                //Convert to lower case if the respectCase option is not set
                boolean respectCase = characters.getFormat().getRespectCase();
                respectCase = respectCase || characters.getFormat().getDatatypeID() != Characters.Datatypes.MICROSATID;
                respectCase = respectCase || characters.getFormat().getTokens();
                if (!respectCase) {
                    if (ci != missingchar && ci != gapchar)
                        ci = Character.toLowerCase(ci);
                    if (cj != missingchar && cj != gapchar)
                        cj = Character.toLowerCase(cj);
                }


                if (ci != missingchar
                        && ci != gapchar
                        && cj != missingchar
                        && cj != gapchar) numNotMissing = numNotMissing + 1;

                //Handle ambiguouos states.
                boolean ambigi, ambigj;
                ambigi = ambigj = false;

                if (characters.hasAmbigStates() && (handleAmbig != IGNOREAMBIG)) {
                    ambigi = (ci == missingchar && characters.hasAmbigString(i, k));
                    ambigj = (cj == missingchar && characters.hasAmbigString(j, k));
                }

                if (ambigi || ambigj) {
                    //ToDo: store a map from the ambig codes to the difference to avoid these computations.

                    String si, sj;
                    if (ambigi)
                        si = characters.getAmbigString(i, k);
                    else
                        si = "" + ci;

                    if (ambigj)
                        sj = characters.getAmbigString(j, k);
                    else
                        sj = "" + cj;

                    //Two cases... if they are the same states, then this needs to be distributed
                    //down the diagonal of F. Otherwise, average.

                    if (si.equalsIgnoreCase(sj)) {
                        double weight = 1.0 / si.length();
                        for (int x = 0; x < si.length(); x++) {
                            ci = si.charAt(x);
                            int statei = symbols.indexOf(ci);
                            Fcount[statei][statei] += weight * charWeight;
                        }
                    } else if (handleAmbig == AVERAGEAMBIG) {
                        double weight = 1.0 / (si.length() * sj.length());


                        for (int x = 0; x < si.length(); x++) {
                            for (int y = 0; y < sj.length(); y++) {
                                ci = si.charAt(x);
                                cj = sj.charAt(y);
                                int statei = symbols.indexOf(ci);
                                int statej = symbols.indexOf(cj);
                                if (ci == gapchar) statei = gapindex;
                                if (ci == missingchar) statei = missingindex;
                                if (cj == gapchar) statej = gapindex;
                                if (cj == missingchar) statej = missingindex;
                                if (statei >= 0 && statej >= 0)
                                    Fcount[statei][statej] += weight * charWeight;
                                else {
                                    if (statei < 0)
                                        throw new SplitsException("Position " + k + " for taxa " + i + " is the invalid character " + ci);
                                    else if (statej < 0)
                                        throw new SplitsException("Position " + k + " for taxa " + j + " is the invalid character " + cj);
                                }
                            }
                        }
                    }


                } else {

                    int statei = symbols.indexOf(ci);
                    int statej = symbols.indexOf(cj);
                    if (ci == gapchar) statei = gapindex;
                    if (ci == missingchar) statei = missingindex;
                    if (cj == gapchar) statej = gapindex;
                    if (cj == missingchar) statej = missingindex;
                    if (statei >= 0 && statej >= 0)
                        Fcount[statei][statej] += charWeight;
                    else {
                        if (statei < 0)
                            throw new SplitsException("Position " + k + " for taxa " + i + " is the invalid character " + ci);
                        else if (statej < 0)
                            throw new SplitsException("Position " + k + " for taxa " + j + " is the invalid character " + cj);
                    }
                }

            }
        }
        for (int ii = 0; ii < numStates; ii++)
            System.arraycopy(Fcount[ii], 0, originalFcount[ii], 0, numStates);
        originalNumNotMissing = numNotMissing;
    }


    /**
     * bootstrapF [IN DEVELOPMENT]
     * <p/>
     * Computes a bootstrap replicate, starting with the
     * original frequency matrix.
     * <p/>
     * To restore the original Frequency matrix, use
     * restoreAfterBootstrap();
     */
    public void bootstrapF() {
        int nstates = getNumStates();
        int nchars = getNumActive();
        int i, j;
        Random generator = new Random();
        for (i = 0; i < nstates; i++)
            for (j = 0; j < nstates; j++)
                Fcount[i][j] = 0;
        numNotMissing = 0;
        for (int k = 0; k < nchars; k++) {
            int x = generator.nextInt(nchars);
            boolean isfound = false;
            for (i = 0; i < nstates + 2 && !isfound; i++)
                for (j = 0; j < nstates + 2 && !isfound; j++) {
                    x -= originalFcount[i][j];
                    if (x < 0) {
                        isfound = true;
                        Fcount[i][j]++;
                        numNotMissing++;
                    }
                }
            //If we get here, then that means a missing
            //site - we do nothing.
        }
    }

    /**
     * restoreAfterBootstrap  [UNDER DEVELOPMENT]
     * <p/>
     * Restores the Fcount matrix and numNotMissing to
     * their original values.
     */
    public void restoreAfterBootstrap() {
        for (int ii = 0; ii < getNumStates() + 2; ii++)
            System.arraycopy(originalFcount[ii], 0, Fcount[ii], 0, getNumStates() + 2);
        numNotMissing = originalNumNotMissing;
    }


    /**
     * Number of sites that are not masked
     *
     * @return numActive
     */
    public int getNumActive() {
        return numActive;
    }

    /**
     * Number of active sites with gaps or missing states in one or both sequences
     *
     * @return numActive - numNotMissing
     */
    public int getNumGaps() {
        return getNumActive() - getNumNotMissing();
    }

    /**
     * Number of active sites with valid, non-gap or non-missing states for both seqs.
     * This number also includes the number of sites where one or other
     * is ambiguous.... not completely accurate really.
     *
     * @return numNotMissing
     */
    public int getNumNotMissing() {
        return numNotMissing;
    }

    /**
     * Number of states (the number of valid symbols)
     *
     * @return numStates
     */
    public int getNumStates() {
        return numStates;
    }

    /* Redundant code?
    public boolean getIgnoreLastState() {
        return ignoreLastState;
    }

    public void setIgnoreLastState(boolean val) {
        ignoreLastState = val;
    }
    */

    /**
     * Returns matrix containing the number of sites for each kind of transition
     *
     * @return Fcound
     */
    public double[][] getFcount() {
        return Fcount;
    }

    /**
     * Frequency matrix - returns matrix containing the proportion of each kind of site
     *
     * @return proportions. If no valid sites, returns proportion of 1.
     */
    public double[][] getF() {
        double[][] F = new double[getNumStates()][getNumStates()];
        double Fsum = 0.0;
        if (getNumNotMissing() > 0) {
            for (int i = 0; i < getNumStates(); i++)
                for (int j = 0; j < getNumStates(); j++)
                    Fsum += Fcount[i][j];


            for (int i = 0; i < getNumStates(); i++) {
                for (int j = 0; j < getNumStates(); j++) {
                    F[i][j] =
                            Fcount[i][j] / Fsum;
                }
            }
        } else {
            F = null;
            //TODO: This should probably throw an 'undefinedDistance' exception.
            //System.err.println("Missing distance");
        }
        return F;
    }


    public double[][] getExtendedF() {
        double[][] F = new double[getNumStates() + 2][getNumStates() + 2];
        if (getNumActive() > 0) {
            for (int i = 0; i < getNumStates() + 2; i++) {
                for (int j = 0; j < getNumStates() + 2; j++) {
                    F[i][j] =
                            Fcount[i][j] / (double) getNumActive();
                }
            }
        } else {
            F = null;
            //System.err.println("Missing distance");
        }
        return F;
    }

    /**
     * Returns negative log likelihood of a given F matrix and t value
     *
     * @param model
     * @param F
     * @param t
     * @return negative log likelihood [double]
     */
    private double evalL(SubstitutionModel model, double[][] F, double t) {

        int numstates = model.getNstates();
        double logL = 0.0;
        for (int i = 0; i < numstates; i++) {
            for (int j = 0; j < numstates; j++) {
                if (F[i][j] != 0.0)
                    logL += F[i][j] * Math.log(model.getX(i, j, t));
            }
        }
        return -logL;

    }


    private double goldenSection(
            SubstitutionModel model,
            double[][] F,
            double tmin,
            double tmax) {

        double a, b, tau, aa, bb, faa, fbb;
        tau = 2.0 / (1.0 + Math.sqrt(5.0)); //Golden ratio
        double GS_EPSILON = 0.000001;
        int nSteps;

        nSteps = 0;

        a = tmin;
        b = tmax;
        aa = a + (1.0 - tau) * (b - a);
        bb = a + tau * (b - a);
        faa = evalL(model, F, aa);
        fbb = evalL(model, F, bb);

        while ((b - a) > GS_EPSILON) {
            nSteps++;
            // cout<<"["<<a<<","<<aa<<","<<bb<<","<<b<<"] \t \t ("<<faa<<","<<fbb<<")"<<endl;

            if (faa < fbb) {
                b = bb;
                bb = aa;
                fbb = faa;
                aa = a + (1.0 - tau) * (b - a);
                faa = evalL(model, F, aa);
                //System.out.println("faa was the smallest at this iteration :" + faa);
            } else {
                a = aa;
                aa = bb;
                faa = fbb;
                bb = a + tau * (b - a);
                fbb = evalL(model, F, bb);
                //System.out.println("fbb was the smallest at this iteration :" + fbb);
            }
        }

        return b;
    }

    /**
     * Miguel Jett
     * <p/>
     * Description: A particularly simple and robust method to find a minimum of a
     * function f(x) dependent on a single variable x. The minimum must
     * initially be bracketed between two values x=a and x=b. The
     * method uses parabolic interpolation as long as the process is
     * convergent and does not leave the boundaries (a,b), and interval
     * subdividing methods otherwise. The algorithm requires keeping
     * track of six function points at all times, which are iteratively
     * updated, reducing the minimum-enclosing interval continually.
     * <p/>
     * reference: Brent R. P. 1973, Algorithms for Minimization
     * without derivatives (Englewood Cliffs, NJ: Prentice-Hall) Chapter 5.
     *
     * @param model
     * @param F
     * @param tmin
     * @param tmax
     * @return Minimum of a one dimensional function f(x)
     */
    private double brentAlgo(SubstitutionModel model,
                             double[][] F,
                             double tmin,
                             double tmax) {
        //needed definitions
        int ITMAX = 100;
        double tol = 0.000001;
        double BA_EPSILON = 0.000001;
        double CGOLD = (3 - Math.sqrt(5)) / 2; // 0.3819660

        int nSteps;

        nSteps = 0;

        // needed variables
        int iter;
        double x, d, e, etemp, m, p, q, r, tol1, tol2, u, v, w, fu, fv, fw, fx;

        // initializations
        v = w = x = tmin + (CGOLD * (tmax - tmin));
        fv = fw = fx = evalL(model, F, x); // f(x) = f(w) = f(v)
        e = 0.0;
        d = 0.0;

        // Main loops
        for (iter = 1; iter <= ITMAX; iter++) {
            nSteps++;
            m = 0.5 * (tmin + tmax);
            tol1 = tol * Math.abs(x) + BA_EPSILON;
            tol2 = 2.0 * tol1;

            // Stopping criterion
            if (Math.abs(x - m) <= (tol2 - 0.5 * (tmax - tmin))) {
                return x;
            }

            // Construct a trial parabolic fit
            if (Math.abs(e) > tol1) {
                r = (x - w) * (fx - fv);
                q = (x - v) * (fx - fw);
                p = (x - v) * q - (x - w) * r;
                q = 2 * (q - r);
                if (q > 0.0) {
                    p = -p;
                }
                q = Math.abs(q);
                etemp = e;
                e = d;
                if (Math.abs(p) >= Math.abs(0.5 * q * etemp) || p >= q * (tmin - x) || p >= q * (tmax - x)) {
                    // A "golden section" step
                    d = CGOLD * (e = (x >= m ? tmin - x : tmax - x));
                } else {
                    //A parabolic step
                    d = p / q;
                    u = x + d;
                    if (u - tmin < tol2 || tmax - u < tol2) {
                        if (x < m)
                            d = tol1;
                        else
                            d = -tol1;
                    }
                }

            } else {
                d = CGOLD * (e = (x >= m ? tmin - x : tmax - x));
            }

            if (Math.abs(d) >= tol1)
                u = x + d;
            else if (d > 0)
                u = x + tol1;
            else
                u = x - tol1;

            fu = evalL(model, F, u); // Function evaluation

            // Updating tmin,tmax,v,w and x;
            if (fu <= fx) {
                if (u >= x)
                    tmin = x;
                else
                    tmax = x;
                v = w;
                fv = fw;
                w = x;
                fw = fx;
                x = u;
                fx = fu;
            } else {
                if (u < x)
                    tmin = u;
                else
                    tmax = u;

                if (fu <= fw || w == x) {
                    v = w;
                    fv = fw;
                    w = u;
                    fw = fu;
                } else if (fu <= fv || v == x || v == w) {
                    v = u;
                    fv = fu;
                }
            }
        }

        // Should never get here...
        System.out.println("Maximum number of iteration exceeded in brentAlgo");
        return 0.0;
    }

    /**
     * Max Likelihood Distance - returns maximum likelihood distance for a given substitution
     * model.
     *
     * @param model Substitution model in use
     * @return distance
     * @throws SaturatedDistancesException distance undefined if saturated (distance more than 10 substitutions per site)
     */
    public double mlDistance(SubstitutionModel model) throws SaturatedDistancesException {
        double t;
        double dist = 0.0;

        //TODO: Replace the golden arc method with Brent's algorithm
        double[][] fullF = getF();
        double[][] F;
        int nstates = model.getNstates();

        F = new double[nstates][nstates];

        double k = 0.0;
        for (int i = 0; i < nstates; i++) {
            for (int j = 0; j < nstates; j++) {
                double Fij = fullF[i][j];
                F[i][j] = Fij;
                k += Fij;
            }
        }
        for (int i = 0; i < nstates; i++) {
            for (int j = 0; j < nstates; j++) {
                F[i][j] /= k; /* Rescale so the entries sum to 1.0 */
            }
        }

        //t = brentAlgo(model,F,0.00000001, 2.0);
        t = goldenSection(model, F, 0.00000001, 2.0);
        //System.out.println("Golden Section t : " + t);
        //System.out.println("nSteps for golden is " + nSteps);
        //dist = brentAlgo(model,F,0.00000001, 2.0);
        //System.out.println("Brent algo dist : " + dist);
        //System.out.println("nSteps for brent is " + nSteps);
        if (t == 2.0) {
            t = goldenSection(model, F, 2.0, 10.0);
            //dist = brentAlgo(model,F,2.0, 10.0);
            //t = brentAlgo(model,F,2.0, 10.0);
            if (t == 10.0) {
                throw new SaturatedDistancesException();
            }
        }
        return t * model.getRate();
    }

    public double bulmerVariance(double dist, double b) {

        return (Math.exp(2 * dist / b) * b * (1 - Math.exp(-dist / b)) * (1 - b + b * Math.exp(-dist / b))) / ((double) this.getNumNotMissing());
    }
}
