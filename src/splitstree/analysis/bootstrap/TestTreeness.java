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

package splitstree.analysis.bootstrap;

import jloda.util.Basic;
import splitstree.algorithms.additional.ClosestTree;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.gui.bootstrap.ConfidenceNetworkDialog;
import splitstree.nexus.Bootstrap;
import splitstree.nexus.Splits;
import splitstree.util.SplitMatrix;
import splitstree.util.SplitMatrixAnalysis;
import splitstree.util.SplitsUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 13, 2005
 * Time: 8:30:44 PM
 * Here are several tests for treeness, none of which work terribly well!
 */
public class TestTreeness implements BootstrapAnalysisMethod {
    /**
     * implementations of analysis-algorithms should overwrite this
     * String with a short description of what they do.
     */
    public static String DESCRIPTION = "No description given for this analysis.";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public boolean isApplicable(Document doc) {
      Bootstrap boots = doc.getBootstrap();
      return (doc.getSplits() != null && boots != null && boots.getSplitMatrix() != null);
  }

    static private double getStdError(double x[], double mean[], double vars[]) {
        int n = x.length;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double xi = x[i] - mean[i];
            sum += xi * xi / vars[i];
        }

        return Math.sqrt(sum);
    }


    static public double dist(double[] u, double[] v) {
        //Computes l1 distance  --- vectors indexed 1... length-1
        int len = u.length;
        double x = 0.0, y;
        for (int i = 1; i < len; i++) {
            y = u[i] - v[i];
            //x += y*y;
            x += Math.abs(y);
        }
        return x;
    }


    //ToDO: this is now l1 metric!!!

    static private double euclideanDist(double[] u, double[] v, double[] weights) {
        if (u.length != v.length) {
            System.err.println("distance miss-match");
        }
        double x = 0.0, y;
        for (int i = 0; i < u.length; i++) {
            if (!(weights[i] > 0.0)) {
                y = u[i] - v[i];
                x += Math.abs(y);
            }
        }
        return x;
        //return Math.sqrt(x);
    }

    static private double euclideanDist2(double[] u, double[] v) {
        if (u.length != v.length) {
            System.err.println("distance miss-match");
        }
        double x = 0.0, y;
        for (int i = 0; i < u.length; i++) {
            y = u[i] - v[i];
            x += y * y;
        }
        return Math.sqrt(x);
    }

    /**
     * Tests the hypothesis: the splits in 'splits' could have been
     * generated from a tree using the current network method
     * and distance transform.
     *
     * @param splits
     * @param boots
     * @return double
     */
    static public double testTreenessDist(Splits splits, Bootstrap boots) {

        //TODO: We should only need to pass this routine a splitMatrix

        //splitMatrix is a matrix with rows indexed by splits and columns indexed by replicates.
        SplitMatrix splitMatrix = boots.getSplitMatrix();

        //splits is the estimated splits. This utility stores the weights in splits
        //as a vector with the same indexing as the columns of the splitMatrix
        //Denote this vector by \hat{N}
        double[] splitvec = SplitMatrixAnalysis.splitsToArrayFromZero(splitMatrix, splits);

        //Let d( ) denote the Euclidean metric in split space.

        // First we find a tree that
        //is closest to \hat{N} under this metric.

        Splits newSplits = new Splits(splits.getNtax());
        try {
            newSplits.setCycle(splits.getCycle());
        } catch (SplitsException e) {
            Basic.caught(e);
        }
        newSplits.getProperties().setCompatibility(splits.getProperties().getCompatibility());

        double[] weights = new double[splits.getNsplits() + 1];
        for (int i = 1; i <= splits.getNsplits(); i++) {
            double x = splits.getWeight(i);
            TaxaSet A = splits.get(i);
            int id = splitMatrix.findSplit(A);
            newSplits.add(A, (float) x);
            // weights[i] = x*x;       Euclidean
            weights[i] = x; //L1 distance
        }
        ClosestTree.apply(newSplits, weights);
        double[] treevec = SplitMatrixAnalysis.splitsToArrayFromZero(splitMatrix, newSplits);

        //Now we determine the proportion of bootstrap networks N^* such that d(N^*,\hat{N}) > d(N_0,\hat{N})
        //this is the p-value. Reject the null if p<0.05


        double dhat = euclideanDist(treevec, splitvec, treevec);

        System.err.println("Distance from hatN to N_0 is " + dhat);

        int numFurther = 0;
        int numTotal = splitMatrix.getNblocks();
        double x = 0.0;

        for (int i = 0; i < numTotal; i++) {
            double di = euclideanDist(splitvec, splitMatrix.getMatrixColumn(i), treevec);
            if (di >= dhat)
                numFurther++;
            x += di;
        }

        System.err.println("Average distance from hatN to N* is " + x / numTotal);

        double pval = (double) numFurther / splitMatrix.getNblocks();
        System.err.println("P value = " + pval);

        return pval;
    }

    /**
     * Tests the hypothesis: the splits in 'splits' could have been
     * generated from a tree using the current network method
     * and distance transform.
     *
     * @param splits
     * @param boots
     * @return true if rejection at p=0.05
     */
    static public boolean testTreenessBox(Splits splits, Bootstrap boots) {


        SplitMatrix splitMatrix = boots.getSplitMatrix();

        Splits confidence = SplitMatrixAnalysis.getConfidenceNetwork(splitMatrix, 0.95, ConfidenceNetworkDialog.ESTIMATED);
        Splits treeSplits = new Splits(splits.getNtax());
        for (int i = 1; i <= confidence.getNsplits(); i++) {
            if (confidence.getInterval(i).low > 0.0)
                treeSplits.add(confidence.get(i));
        }

        return !SplitsUtilities.isCompatible(treeSplits);
    }


    static public boolean testTreeness(Splits splits, Bootstrap boots) {

        SplitMatrix splitMatrix = boots.getSplitMatrix();
        SplitMatrixAnalysis.getConfidenceIntervals(splitMatrix, splits, 0.95);
        int ntax = splits.getNtax();

        //Test if there are incompatible splits with lower bounds > 0
        boolean incompatible = false;
        for (int i = 1; !incompatible && i <= splits.getNsplits(); i++) {
            if (splits.getInterval(i).low > 0.0) {
                TaxaSet A = splits.get(i);
                TaxaSet Ac = A.getComplement(ntax);
                for (int j = 1; !incompatible && j < i; j++) {
                    if (splits.getInterval(j).low > 0.0) {
                        TaxaSet B = splits.get(j);
                        TaxaSet Bc = B.getComplement(ntax);
                        if (A.intersects(B) && A.intersects(Bc) && Ac.intersects(B) && Ac.intersects(Bc))
                            incompatible = true;
                    }
                }
            }

        }

        return incompatible;
    }


    public String apply(Document doc) {
      
      Bootstrap boots = doc.getBootstrap();
      boolean rejected = TestTreeness.testTreeness(doc.getSplits(), boots);
      String result;

      if (rejected) {
          result = "The amount of incompatibility in the network is significant (p<0.05)";
      } else {
          result = "The amount of incompatibility in the network is not significant";
      }
      return result;
    }
}
