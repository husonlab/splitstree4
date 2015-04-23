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

package splitstree.util;

import Jama.Matrix;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Vector;

/**
 * User: bryant
 * Date: Jul 20, 2010
 * Time: 10:29:17 AM
 *
 * This code is a refactoring/improvement/extension of the various algorithms performing
 * least squares type calculations on circular splits.
 *
 * Internally, taxa are numbered 0...n-1.
 *
 * Vectors containing pairs of taxa are ordered (0,1), (0,2),...,(0,n-1),(1,2),(1,3),...,(n-2,n-1)
 *
 * In this ordering (i,j) has index (2n - i -3)i/2 + j-1
 *
 * Hence going from (i,j) to (i,j+1) increases index by 1.
 * Going from (i,n-1) to (i+1,i+2) increases index by 1.
 * Going from (i,j) to (i+1,j+1) increases index by (n-i-1).
 *
 * Splits are coded as pairs. The split (i,j) corresponds to cutting the circular order after i and after j, so
 * that one side of the split is i+1,i+2,....,j.
 *
 * Trivial splits are then
 * (0,1), (1,2), (2,3), ...., (n-2,n-1)  as well as (0,n-1).
 *  To loop. Start index = 0. Going from (i,i+1) to (i+1,i+2) increases by (n-i-1).
 * Pair (0,n-1) has index (n-2).
 *
 */
public class CircularLeastSquares {


    private final double EPSILON = 1e-14;

    private int[] ordering;

    private int ntax;

    /**
     * Initialise structure and circular ordering.
     *
     * @param circularOrdering Ordering. taxa are labelled 1...n, and in positions 1..n of array (but shuffled)
     */
    public CircularLeastSquares(int[] circularOrdering) {
        ntax = circularOrdering.length-1;
        ordering = new int[ntax];
        for(int i=0;i<ntax;i++)
            ordering[i] = circularOrdering[i+1]-1;
    }



    /**
     * Convert Distances block to vector object.
     * Note that taxa i outside -> taxon (i-1) within.
     *
     *
     * @param dist Distances block
     * @return  vector with internal indexing.
     */
    private double[] convertDistancesToVector(Distances dist) {

        double[] d = new double[(ntax*(ntax-1))/2];
        int index = 0;
        for (int i=0;i<ntax;i++)   {
            int t1 =  ordering[i]+1;
            for(int j=i+1;j<ntax;j++) {
                int t2 = ordering[j]+1;
                d[index] = dist.get(t1,t2);
                index++;
            }
        }
        return d;
    }



    /**
     * Computes Xy where X is the (full) topological matrix for the set of circular splits
     * @param y  vector
     * @param z  vector of same size as y containing Xy
     */
    private void computeXy(double[] y, double[] z) {

        double d_ij;

        //First the pairs distance one apart.
        int index;
        int dindex = 0;

        for (int i = 0; i <= ntax - 2; i++) {
            d_ij = 0.0;
            //Sum over splits (k,i) 0<=k<i.
            index = i - 1;  //(0,i)
            for (int k = 0; k <= i - 1; k++) {
                d_ij += y[index];  //(k,i)
                index += (ntax - k - 2);
            }
            index++;
            //index = (i,i+1)
            for (int k = i + 1; k <= ntax - 1; k++)  //sum over splits (i,k)  i+1<=k<=n-1
                d_ij += y[index++];

            z[dindex] = d_ij;
            dindex += (ntax - i - 2) + 1;
        }

        //Distances two apart.
        index = 1; //(0,2)
        for (int i = 0; i <= ntax - 3; i++) {
//            z[i ][i+2] = z[i ][i+1] + z[i + 1][i + 2] - 2 * b[i][i+1];

            z[index] = z[index - 1] + z[index + (ntax - i - 2)] - 2 * y[index - 1];
            index += 1 + (ntax - i - 2);
        }


        for (int k = 3; k <= ntax - 1; k++) {
            index = k - 1;
            for (int i = 0; i <= ntax - k - 1; i++) {
                //int j = i + k;
                //z[i][j] = z[i][j - 1] + z[i+1][j] - z[i+1][j - 1] - 2.0 * b[i][j - 1];
                z[index] = z[index - 1] + z[index + (ntax - i - 2)] - z[index + (ntax - i - 2) - 1] - 2.0 * y[index - 1];
                index += 1 + (ntax - i - 2);
            }
        }
    }


    /**
     * Computes z = X'y where X is the (full) topological matrix for the set of circular splits
     * @param y  vector
     * @param z vector of same size as y
     */
    private void computeXty (double[] y, double[] z) {


        //First the splits (i,i+1), which are i+1|....   i=0..n-1.
        //These are just the sum of y_(i+1)j  over (i+1) to the other taxa.
        //Index of (0,1) is 0. To go from (i,i+1) to (i+1,i+2) increase index by 1 + (n-i-2) = (n-i-1).
        double[] rowsums = new double[ntax];
        int index = 0;
        for(int i=0;i<ntax;i++) {
            for(int j=i+1;j<ntax;j++) {
                double dij = y[index];
                rowsums[i]+=dij;
                rowsums[j]+=dij;
                index++;
            }
        }
        index = 0;
        for(int i=0;i<ntax-1;i++) {
            z[index]=rowsums[i+1];
            index = index + (ntax - i - 1);
        }


        //Now the splits (i,i+2).
        index = 1;
        for (int i = 0; i < ntax - 2; i++) {
            //index = (i,i+2)
            //p[i][i+2] = p[i][i+1] + p[i + 1][i + 2] - 2 * d[i + 1][i + 2];
            z[index] = z[index - 1] + z[index + (ntax - i - 2)] - 2 * y[index + (ntax - i - 2)];
            index += (ntax - i - 2) + 1;
        }

        //Now the remaining splits
        for (int k = 3; k <= ntax - 1; k++) {
            //The splits (i,i+k)
            index = k - 1;
            for (int i = 0; i <= ntax - k - 1; i++) {
                //index = (i,i+k)
                // p[i][j] = p[i][j - 1] + p[i+1][j] - p[i+1][j - 1] - 2.0 * d[i+1][j];
                z[index] = z[index - 1] + z[index + ntax - i - 2] - z[index + ntax - i - 3] - 2.0 * y[index + ntax - i - 2];
                index += (ntax - i - 2) + 1;
            }
        }
    }





    /**
     * Find nneg vector b minimising \sum (d_ij - (b_i+b_j))^2
     *
     *
     * @param b vector of optimal values. Assumed to have length exactly n.
     * @param d  input distances. vector d_{01}, d_{02}, ...., d_{n-2,n-1}
     */
    private void starNNLS(double[] b, double[] d) {
        int n = b.length;
        if (d.length!=n*(n-1)/2)
            System.err.println("Error with vector sizes");

        //Compute row sums
        double[] r = new double[n];
        int k=0;
        for(int i=0;i<n;i++)
            for(int j=i+1;j<n;j++) {
                r[i]+=d[k];
                r[j]+=d[k];
                k++;
            }

        //Store a copy of the residuals in b, as we want to sort r.
        System.arraycopy(r, 0, b, 0, n);

        //Sort residuals
        Arrays.sort(r);


        double S = 0; //Sum of largest m entries in r.
        double R = 0;

        //Loop through the possible choices for non-zero variables. There
        //are of the form B^c_m = {r[n-m] ... r[n-1]}, with B^c_0 the emptyset.
        //At the start of each iteration, S is the sum of {r_i:r \in B^c_m}
        for(int m=0;m<=n;m++) {
            //System.err.println("R = "+R+"\tm="+m);
            R = 1.0/(m+n-2.0)*S;

            /*Stopping conditions are that r_i > R for all i in B^c_m
  %and r_i <= R for all i not in B^c_m. As we've sorted r, we need
  %only check
  %   rsorted(n-m+1) > R || m = 0
  %and
  %   rsorted(n-m) <= R || m=n.   */
            if ((m==0||r[n-m]>R)&&(m==n||r[n-m-1]<=R))
                break;

            S+=r[n-m-1];
        }

        /*At this point, positions 1..n-m in list have b_i = 0 and r_i \leq R
        %others have branch lengths given by b_i = 1/(n-2)(r_i - R)
        %Hence we can use a shortcut b = max(0,1/(n-2)*(r(i)-R);
        %NOTE: in this implementation, r is sorted - but we saved a copy in b.     */

        for(int i=0;i<n;i++)
            b[i] = Math.max(0.0,1.0/(n-2)*(b[i]-R));
    }



    /**
     * Returns the minimum value in an array. Assumes that array is non-empty.
     * @param x array of double assumed non-empty
     * @return  minimum value in the array
     */
    private double getMin(double[] x) {
        double min_x = x[0];
        for(int i=1;i<x.length;i++)
            min_x = Math.min(min_x,x[i]);
        return min_x;
    }

    /**
     * Finds w such that (X'Xw)_i = z[i] for all i in set ab and w_i = 0 for all i not in this set.
     * @param z  vector of size N := ntax(ntax-1)/2.
     * @param ab_set    boolean vector of size N.
     * @param w  vector  of size N := ntax(ntax-1)/2.
     */
    private void solveRestricted(double[] z, boolean[] ab_set, double[] w) {
        //Version 1. Use the JAMA library.
        int N,n;
        N = ab_set.length;

        n=0;
        for(int i=0;i<N;i++)
            if (ab_set[i])
                n++;

        System.err.println("size of ab = "+n);

        Jama.Matrix XtX = new Matrix(n,n); //X_AB ' X_AB matrix.
        Jama.Matrix y = new Matrix(n,1);  //reduced z vector.


        int[][] ab_pairs = new int[n][2];
        int index=0, abIndex=0;
        for(int i=0;i<ntax;i++) {
            for(int j=i+1;j<ntax;j++) {
                if (ab_set[index]) {
                    y.set(abIndex,0,z[index]);
                    ab_pairs[abIndex][0]=i;
                    ab_pairs[abIndex][1]=j;
                    abIndex++;
                }
                index++;
            }
        }


        for(int row=0;row<n;row++) {
            for(int col=row;col<n;col++) {
                int i = ab_pairs[row][0];
                int j = ab_pairs[row][1];
                int k = ab_pairs[col][0];
                int l = ab_pairs[col][1];
                int nAC = Math.max(Math.min(j,l)-Math.max(i,k),0); //|A \cap C|
                int nA = j-i; //|A|
                int nC = l-k; //|C|
                int nAD = nA - nAC;
                int nBC = nC - nAC;
                int nBD = ntax - nAC - nAD - nBC;
                XtX.set(row,col,nAC*nBD+ nAD*nBC);
                XtX.set(col,row,nAC*nBD+ nAD*nBC);
            }
        }




        // XtX.print(4,4);
        //Solve the reduced system.
        Jama.Matrix b = XtX.solve(y); //TODO: test with cholesky decomposition.

        //Pad the vector w.
        index = abIndex = 0;
        for(int i=0;i<ntax;i++) {
            for(int j=i+1;j<ntax;j++) {
                if (ab_set[index]) {
                    w[index]=b.get(abIndex,0);
                    abIndex++;
                }  else {
                    w[index]=0.0;
                }

                index++;
            }
        }


    }

    /**
     * Convert the internal representation of splits (weight vector) to a Splits object
     * @param w vector of split weights
     * @return  splits object
     */
    private Splits convertWeightsToSplits(double[] w) {
        Splits splits = new Splits(ntax);
        int index = 0;
        for (int i = 0; i < ntax; i++) {
            TaxaSet t = new TaxaSet();
            for (int j = i + 1; j < ntax; j++) {
                t.set(ordering[j]+1);
                double weight = w[index];
                if (weight>0.0)
                    splits.add(t, (float) weight);
                index++;
            }
        }
        return splits;
    }

    private void printMatlab(String name,double[] v) {
        System.err.print(name +" = [");
        for(int i=0;i<v.length-1;i++)
            System.err.print(""+v[i]+", ");
        System.err.println(""+v[v.length-1]+"]';");
    }

    public Splits optimalAICLasso(Document doc, Taxa taxa, Distances distances, String informationCriterion, int stoppingPoint, String lasso) {

        doc.notifySubtask("Applying Lasso algorithm");

        //Convert the distances to a vector d_{01}, d_{02}, ...., d_{n-2,n-1}, where
        //the taxa are sorted according to this.ordering

        int N = ntax*(ntax-1)/2;      //Number of variables in full model.
        double[] d = convertDistancesToVector(distances);

        double lambda = 0.0;
        double[] beta = new double[N];

        double[] c = new double[N];
        computeXty(d,c);
        for(int i=0;i<N;i++)
            c[i] = -c[i];       //c = -X'y

        double kappa = c[0];
        for(int i=1;i<N;i++)
            kappa = Math.min(kappa,c[i]); //kappa = min_i(c_i)

        //OUTPUT
        double zeroResidual = 0.0;
        for(int i=0;i<N;i++)
            zeroResidual+=c[i]*c[i];

            System.out.print(""+zeroResidual+"\t");

            for(int i=0;i<N;i++)
                System.out.print(""+beta[i]+"\t");
            System.out.println();




        //Initialise two arrays
        double[] w = new double[N];
        double[] a = new double[N];
        double[] tmp = new double[N];

        //Sets of indices
        BitSet aSet = new BitSet(N);
            BitSet bSet = new BitSet(N);
            BitSet cSet = new BitSet(N);

        int iteration = 0;

        while((kappa < -1e-8)) {
              if (stoppingPoint>=0 && iteration>=stoppingPoint)
                  break;

            //Find next direction
            //TEMP version: only one element in B.

            int b=-1;
            aSet.clear();
            bSet.clear();
            cSet.clear();

            for(int i=0;i<N;i++) {
                if (beta[i]>0.0)
                    aSet.set(i);
                else if (c[i]>kappa)
                    cSet.set(i);
                 else  {
                    bSet.set(i);
                    b = i;
                }
            }
            if (bSet.cardinality()>1)
                System.out.println("Problem - current implementation for small B only");
            else
                //First evaluate including bSet among non-zero.
                circularCGw(cSet,w);

                //Check if valid.
            if (!bSet.isEmpty() && (w[b] < 0.0)) {
                cSet.set(b);
                circularCGw(cSet,w);
            }


            //Length of step.
            double wsum = 0.0;
            for(int i=0;i<N;i++)
                wsum += w[i];              //wsum = 1'w

            double gamma1 = Double.MAX_VALUE;
            for(int i=0;i<N;i++) {
                if (w[i]<0.0)
                    gamma1 = Math.min(gamma1,-beta[i]*wsum/w[i]);
            }                                                          //\gamma_1 <- min{-beta/(w_i/1'w):w_i<0}

            computeXy(w,tmp);
            computeXty(tmp,a);      //a = X'Xw

            double gamma2 = Double.MAX_VALUE;
            for(int i=0;i<N;i++) {
                if (a[i]<1.0 && c[i] > kappa)
                    gamma2 = Math.min(gamma2,wsum*(c[i] - kappa)/(1 - a[i]));
            }                                                      // gamma_2 <- min{(1'w)(c_k - kappa)/(1-a_k):a_k<1}

            double alpha = Math.min(Math.min(gamma1,gamma2),-kappa);

            //Update
            lambda = lambda + alpha;
            kappa = kappa + alpha/wsum;
            double residual = 0.0;

            for(int i=0;i<N;i++) {
                c[i] = c[i] + alpha*a[i]/wsum;
                beta[i] = beta[i] + alpha*w[i]/wsum;

                residual += c[i] * c[i];

            }

            //OUTPUT
            System.out.print(""+residual+"\t");

            for(int i=0;i<N;i++)
                System.out.print(""+beta[i]+"\t");
            System.out.println();
            iteration++;

        }







        /*

        //Now estimate sigma
        doc.notifySubtask("Estimating residual");
        NJ njMethod = new NJ();
        PhyloTree njtree = (njMethod.apply(null,taxa,distances)).getTree(1);
        Distances treeDist = RandomDistances.getAdditiveDistances(taxa,njtree);

        double sumSquared = 0.0;
        for(int i=1;i<=ntax;i++)
            for(int j=i+1;j<=ntax;j++) {
                double diff_ij = distances.get(i,j) - treeDist.get(i,j);
                sumSquared+=diff_ij;
            }

        double sigma2 = sumSquared/(ntax*(ntax-1)/2 - 2*ntax+3);

        //Now identify the optimal
        doc.notifySubtask("Determining model with optimal information");
        double minIC = 0.0;
        int minICiteration = -1;
        boolean usingAIC = informationCriterion.equalsIgnoreCase("AIC");

        for(int i=0;i<reporter.getNIterations();i++) {
            //Count the number of non-zero variables.
            double[] beta = reporter.getBeta(i);
            int numNonZero = 0;
            for(int j=0;j<n;j++)
                if (beta[j]>0.0)
                    numNonZero++;

            double ic_i = reporter.getResidual(i) / (n*sigma2);
            if (usingAIC)
                ic_i -= 2.0*(double)numNonZero/n;
            else
                ic_i -= Math.log(n)*(double)numNonZero/n;

            if (minICiteration<0 || ic_i < minIC) {
                minICiteration = i;
                minIC = ic_i;
            }

        } */
        return convertWeightsToSplits(beta);
    }

    /**
     * Solves the QP:
     *
     * min 1/2 x'Bx - x'c
     *
     * such that x_i = 0 for all i in zeroset, x_i \geq 0 for all i in boundSet.
     *
     * Here B = X'X and c = X'u
     * where u_j = 0 (1) if j in the zero set (not in zero set)
     */


//    private void CircularGPCG(double[] x, BitSet zeroset, BitSet boundSet) {
//        int N = ntax*(ntax-1)/2;      //Number of variables in full model.
//
//        double[] p = new double[N];
//        double[] tmp = new double[N];
//
//        //Step 0
//        int j=0;
//        for(int i=0;i<N;i++)   {
//            if (!zeroset.get(i))
//                x[i]=1.0;
//            else
//                x[i]=0.0;
//        }
//
//        //Step 1
//        computeXy(x,tmp);
//        for(int i=0;i<N;i++) {
//            if (!zeroset.get(i))
//                tmp[i]=1.0 - tmp[i];
//            else
//                tmp[i]=0.0;
//        }
//        computeXty(tmp,p);
//        for(int i=0;i<N;i++)
//            if (zeroset.get(i))
//               p[i]=0.0;                               //p = c - Bx
//
//
//
//
//
//    }
//




    private void zeroValues(double[] x,BitSet zeroset) {
        for(int i=0;i<x.length;i++)
            if (zeroset.get(i))
                x[i] = 0.0;
    }


    /**
     * Conjugate gradient algorithm solving X'X w = u
     *
     * where w_i = 0 and u_i = 0 if i in the zero set, and otherwise u_i = 1.
     * @param zeroset
     * @param w (initial value, also used to return values.
     */
     private void circularCGw(BitSet zeroset, double[] w) {
        int N = ntax * (ntax - 1) / 2;
/* Maximum number of iterations of the cg algorithm (probably too many) */

        int k=0;  //Number of iterations

        double[] g = new double[N];
        double[] p = new double[N];
        double[] tmp = new double[N];
        double[] b = new double[N];
         double[] h = new double[N];


        for(int i=0;i<N;i++)
            if (zeroset.get(i))  {
                w[i]=0.0;
                b[i]=0.0;
            }
            else {
                b[i]=-1.0;
            }

        computeXy(w,tmp);
        computeXty(tmp,g);
        zeroValues(g,zeroset);

        double delta = 0.0;
        for(int i=0;i<N;i++) {
            g[i] = g[i]+b[i];    //g = A x + b, the initial gradient
            p[i] = -g[i]; //p=-g, initial search direction.
            delta = delta + g[i]*g[i];
        }

         double epsilon = 1e-10 * (N-zeroset.cardinality())*(N-zeroset.cardinality());


        while((k<N) & (Math.sqrt(delta) > epsilon) ) {
            computeXy(p,tmp);
            computeXty(tmp,h); //h = Ap
            zeroValues(h,zeroset);

            double pth = 0.0;
            for(int i=0;i<N;i++)
                pth += p[i]*h[i];

            double tau = delta/pth;

            double newDelta = 0.0;
            for(int i=0;i<N;i++) {
                w[i] += tau*p[i];
                g[i] += tau*h[i];
                newDelta += g[i]*g[i];
            }
            double beta = newDelta/delta;
            for(int i=0;i<N;i++)
                p[i]=-g[i]+beta*p[i];

            delta = newDelta;
            k++;
        }
    }



}

class LassoReporter {
    private int n;
    private int count;
    private Vector allBetas;
    private Vector residuals;


    public LassoReporter() {
        n = 0;
        count = 0;
        allBetas = new Vector();
        residuals = new Vector();
    }
    public int getNIterations() { return count;}
    public double[] getBeta(int iterationNumber) {
        //TODO: have the beta vector as a parameter to save reallocation of memory
        Vector thisBeta = (Vector)allBetas.get(iterationNumber);
        double[] beta = new double[n];
        for(int i=0;i<n;i++)
            beta[i] = (Double) thisBeta.get(i);
        return beta;
    }

    public void saveBeta(double[] beta) {
        if (n==0)
            n = beta.length;
        Vector thisBeta = new Vector();
        for(int i=0;i<n;i++)
            thisBeta.add(beta[i]);
        allBetas.add(thisBeta);
        residuals.add(-1.0); //Default negative means residual not stored
        count++;
    }

    public void saveBeta(double[] beta, double residual) {
        saveBeta(beta);
        residuals.set(count-1,residual);
    }

    public double getResidual(int i) {
        return (Double)residuals.get(i);
    }



}
