/*
 * SplitMatrixAnalysis.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import splitstree4.core.TaxaSet;
import splitstree4.gui.bootstrap.ConfidenceNetworkDialog;
import splitstree4.nexus.Splits;

import java.util.Arrays;
import java.util.Comparator;


/**
 * AnalysisMethod methods based on the data in a splits matrix, as well
 * as methods for manipulating data within the splits container.
 */
public class SplitMatrixAnalysis {


    /**
     * Finds the id of the split in a matrix, first trying oldID
     * to see if that is correct.
     *
     * @return correct id, or -1 if the split doesn't appear.
     */
    static private int getSplitId(SplitMatrix M, TaxaSet A, int oldID) {
        if (oldID > 0 && oldID <= M.getNsplits() && A == M.getSplit(oldID))
            return oldID;
        return M.findSplit(A);
    }

    /**
     * Count the number of blocks that the split with given id has
     * positive weight.
     *
     * @return count
     */
    static private int getCount(SplitMatrix M, int id) {
        int count = 0;
        int n = M.getNblocks();
        for (int i = 1; i <= n; i++) {
            if (M.get(id, i) > 0.0)
                count++;
        }
        return count;
    }


    /**
     * Get mean weight of the splits.
     *
     * @return mean
     */
    static private double getMeanWeight(SplitMatrix M, int id) {
        double sum = 0.0;
        int n = M.getNblocks();
        for (int i = 1; i <= n; i++) {
            double x = M.get(id, i);
            if (x > 0.0)
                sum += x;
        }
        return sum / ((double) n);
    }


    /**
     * For each split in splits, computes the proportion of blocks
     * in M for which the split has strictly positive weight. Puts
     * this quantity in the confidence field.
     *
     */
    static public void evalConfidences(SplitMatrix M, Splits splits) {
        for (int i = 1; i <= splits.getNsplits(); i++) {
            int id = getSplitId(M, splits.get(i), i);
            if (id > 0) {
                int count = getCount(M, id);
                splits.setConfidence(i, (float) count / M.getNblocks());
            } else {
                splits.setConfidence(i, 0);
            }
        }
    }

    /**
     * Replaces the weights in the splits by a percentage confidence
     * value computed from the confidence values.
     *
     */
    static public void computePercentages(Splits splits) {
        for (int i = 1; i <= splits.getNsplits(); i++) {
            float confidence = splits.getConfidence(i);
            float percentage = ((float) ((int) (confidence * 1000)) / 10);
            splits.setWeight(i, percentage);
        }
    }

    //ToDo: Fix this so it always justs goes 1..nblocks.

    /**
     * Given a set of splits and weights, returns an array with the same indexing as
     * the split Matrix <b>shifted</b> down one so that it starts at zero.
     *
     * @return array of doubles indexed 0...nsplits-1
     */
    static public double[] splitsToArrayFromZero(SplitMatrix M, Splits s) {
        int n = M.getNsplits();
        double[] v = new double[n];
        for (int i = 1; i <= s.getNsplits(); i++) {
            int id = M.findSplit(s.get(i));
            if (id > 0) {
                v[id - 1] = s.getWeight(i);
            }
        }
        return v;
    }


    /**
     * Given a set of splits and weights, returns an array with the same indexing as
     * the split Matrix
     *
     * @return array of doubles indexed 0...nsplits-1
     */
    static public double[] splitsToArray(SplitMatrix M, Splits s) {
        int n = M.getNsplits();
        double[] v = new double[n + 1];
        for (int i = 1; i <= s.getNsplits(); i++) {
            int id = M.findSplit(s.get(i));
            if (id > 0) {
                v[id] = s.getWeight(i);
            }
        }
        return v;
    }


    static public void getCovarianceMatrix(SplitMatrix M) {
        int n = M.getNsplits();
        int nblocks = M.getNblocks();
        Matrix mean = new Matrix(n, 1);
        Matrix V = new Matrix(n, n);
        /* Compute means */
        for (int i = 1; i <= n; i++) {
            double sum = 0.0;
            for (int k = 0; k < nblocks; k++)
                sum += M.get(i, k);
            mean.set(i - 1, 0, sum / nblocks);
        }

        /*Compute covariance terms */
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                double sum = 0.0;
                for (int k = 0; k < nblocks; k++) {
                    sum += (M.get(i, k) - mean.get(i - 1, 0)) * (M.get(j, k) - mean.get(j - 1, 0));
                }
                V.set(i - 1, j - 1, sum / (nblocks - 1));
                V.set(j - 1, i - 1, sum / (nblocks - 1));
            }
        }
    }

    /**
     * Computes simultaneous confidence intervals on the splits in the original network/tree
     * This intervals are added to M.originalSplits
     */
    static public void getOldConfidenceIntervals(SplitMatrix M, Splits S, double level) {


        int nMsplits = M.getNsplits();   //Number of splits.... |U| in Beran 88
        int nblocks = M.getNblocks();  //the value jn in Beran 88
        int nsplits = S.getNsplits();

        //Allocate memory for the root values.
        double[][] R = new double[nsplits + 1][]; //R*_{n,u} values, with bundled splits in row 0.
        DoubleInt[] rowIndices = new DoubleInt[nblocks];  //One row of R --- with indices attached
        int[] sn = new int[nblocks];   //The table sn in Beran 88, multiplied by nblocks to make integers
        for (int j = 0; j < nblocks; j++)
            sn[j] = 1;

        /*STEP ONE:
          Compute sn vector, and sort each row of Rij's
        */
        for (int i = 1; i <= nsplits; i++) {

            int index = getSplitId(M, S.get(i), i);
            if (index < 0)
                continue; //split didn't appear. It will have interval [0,2*current]

			double[] row = M.getMatrixRow(index);
			double original = S.getWeight(i);

			for (int j = 0; j < nblocks; j++) {
				double Rij = Math.abs(row[j] - original);
				DoubleInt x = new DoubleInt(Rij, j);
				rowIndices[j] = x;
			}

			Arrays.sort(rowIndices, (Comparator) (x, y) -> {
				if (((DoubleInt) x).Rij < ((DoubleInt) y).Rij)
					return -1;
				else if (((DoubleInt) y).Rij < ((DoubleInt) x).Rij)
					return 1;
				else
					return 0;
			});

			int rank = 0;
			double prevRij = -1.0;
			for (int k = 0; k < nblocks; k++) {
				if ((rank == 0) || rowIndices[k].Rij != prevRij) {
					rank = k;
					prevRij = rowIndices[k].Rij;
				}
				int j = rowIndices[k].j;
                sn[j] = Math.max(rank, sn[j]);
            }

            R[i] = new double[nblocks];
            for (int k = 0; k < nblocks; k++)
                R[i][k] = rowIndices[k].Rij;
        }

        /* STEP TWO: Identify level = (1-alpha) quantile for the sn table */
        Arrays.sort(sn);
        double cn = (double) sn[(int) Math.floor((level) * nblocks)] / nblocks;

        /*STEP THREE. Compute cutoffs for each of the splits */
        for (int i = 1; i <= nsplits; i++) {

            double xi, ai, bi;

            if (R[i] == null) {
                xi = S.getWeight(i);
                ai = 0.0;
                bi = 2.0 * xi;
            } else {
                double di = R[i][(int) Math.floor(cn * nblocks)];
                xi = M.getOriginal(i);
                ai = Math.max(0.0, xi - di);
                bi = xi + di;
            }
            Interval interval = new Interval(ai, bi);

            S.setInterval(i, interval);

        }
    }


    /**
     * Computes simultaneous confidence intervals on the splits in the original network/tree
     * This intervals are added to M.originalSplits
     */
    static public void getConfidenceIntervals(SplitMatrix M, Splits S, double level) {


        int nMsplits = M.getNsplits();   //Number of splits.... |U| in Beran 88
        int nblocks = M.getNblocks();  //the value jn in Beran 88
        int nsplits = S.getNsplits();

        //Allocate memory for the root values.
        double[][] R = new double[nsplits + 1][]; //R*_{n,u} values, with bundled splits in row 0.
        DoubleInt[] rowIndices = new DoubleInt[nblocks];  //One row of R --- with indices attached
        int[] sn = new int[nblocks];   //The table sn in Beran 88, multiplied by nblocks to make integers
        for (int j = 0; j < nblocks; j++)
            sn[j] = 0;
        int[] tn = new int[nblocks];
        for (int j = 0; j < nblocks; j++)
            tn[j] = nblocks;

        /*STEP ONE:
          Compute sn vector, and sort each row of Rij's
        */
        for (int i = 1; i <= nsplits; i++) {

            int index = getSplitId(M, S.get(i), i);
            if (index < 0)
                continue; //split didn't appear. It will have interval [0,2*current]

			double[] row = M.getMatrixRow(index);
			double original = S.getWeight(i);


			for (int j = 0; j < nblocks; j++) {
				double Rij = row[j] - original;
				DoubleInt x = new DoubleInt(Rij, j);
				rowIndices[j] = x;
			}

			Arrays.sort(rowIndices, (Comparator) (x, y) -> {
				if (((DoubleInt) x).Rij < ((DoubleInt) y).Rij)
					return -1;
				else if (((DoubleInt) y).Rij < ((DoubleInt) x).Rij)
					return 1;
				else
					return 0;
			});

			int rank = 0;
			double prevRij = -1.0;
			for (int k = 0; k < nblocks; k++) {
				if ((rank == 0) || rowIndices[k].Rij != prevRij) {
					rank = k;
					prevRij = rowIndices[k].Rij;
				}
				int j = rowIndices[k].j;
                sn[j] = Math.max(rank, sn[j]);
            }

            rank = nblocks;
            for (int k = nblocks - 1; k >= 0; k--) {
                if ((rank == nblocks) || rowIndices[k].Rij != prevRij) {
                    rank = k;
                    prevRij = rowIndices[k].Rij;
                }
                int j = rowIndices[k].j;
                tn[j] = Math.min(rank, tn[j]);
            }
            R[i] = new double[nblocks];
            for (int k = 0; k < nblocks; k++)
                R[i][k] = rowIndices[k].Rij;
        }

        /* STEP TWO: Identify level = (1-alpha) quantile for the sn table */
        Arrays.sort(tn);
        Arrays.sort(sn);
        double cn = (double) sn[(int) Math.floor((level + 1.0) / 2.0 * nblocks)] / nblocks;
        double bn = (double) tn[(int) Math.ceil((1.0 - level) / 2.0 * nblocks)] / nblocks;
        /*STEP THREE. Compute cutoffs for each of the splits */
        for (int i = 1; i <= nsplits; i++) {
            double xi, ai, bi;
            if (R[i] == null) {
                xi = S.getWeight(i);
                ai = 0.0;
                bi = 2.0 * xi;
            } else {
                double li = R[i][(int) Math.ceil(bn * nblocks)];
                double di = R[i][(int) Math.floor(cn * nblocks)];
                xi = M.getOriginal(i);
                ai = Math.max(0.0, xi + li);
                bi = Math.max(0.0, xi + di);
            }
            Interval interval = new Interval(ai, bi);

            S.setInterval(i, interval);

        }
    }

    /**
     * Returns the median element in the subarray v[i]....v[j-1]
     *
     * @return median value
     */
    static double median(double[] v, int i, int j) {
        double[] x = new double[j - i];
        System.arraycopy(v, i, x, 0, j - i);
        Arrays.sort(x);
        return x[(int) Math.floor((j - i) / 2)];
    }


	static class DoubleInt {
		public DoubleInt(double rij, int j) {
			this.Rij = rij;
			this.j = j;
		}

		public final double Rij;
		public final int j;
	}

    /**
     * Returns a set of splits with intervals on the splits to give a set of simultaneous confidence intervals
     * with given total level. The estimate is taken to be the original Splits in the split Matrix.
     * <p/>
     * Applies the B Method detailed in
     * Beran, R. 1990 "Refining Bootstrap Simultaneous Confidence Sets". J. Amer. Stat. Assoc.
     * 85(410) 417--426
     * and in
     * Beran, R. 1988 "Balanced Simultaneous Confidence Sets". J. Amer. Stat. Assoc.
     * 83(403) 679--686
     * Using the root function  |x_i* - \hat{x_i}} for split i. Here \hat{x_i} is the estimated value for the
     * ith split weight, while x_i* is the weight for the ith split in the bootstrap replicate.
     *
     * @return Set of splits with intervals. Splits with confidence interval [0,0] are omitted.
     */

    static public Splits getConfidenceNetwork(SplitMatrix M, double level, int weightMethod) {
        return getConfidenceNetwork(M, level, 0.01, weightMethod);
    }

    /**
     * Same as above, except splits with less appearing in a proportion of fewer than cutoff
     * of the  bootstrap blocks are collected into one category.
     *
     * @return Set of splits with intervals. Splits with confidence interval [0,0] are omitted.
     */
    static public Splits getConfidenceNetwork(SplitMatrix M, double level, double cutoff, int weightMethod) {


        int nsplits = M.getNsplits();   //Number of splits.... |U| in Beran 88
        int nblocks = M.getNblocks();  //the value jn in Beran 88

        //Allocate memory for the root values.
        double[][] R = new double[nsplits + 1][]; //R*_{n,u} values, with bundled splits in row 0.
        DoubleInt[] rowIndices = new DoubleInt[nblocks];  //One row of R --- with indices attached
        int[] sn = new int[nblocks];   //The table sn in Beran 88, multiplied by nblocks to make integers
        for (int j = 0; j < nblocks; j++)
            sn[j] = 0;

        //Determine which, if any, of the splits are bundled.


        boolean[] isBundled;
        double[] bundledSum;

        int numBundled = 0;

        isBundled = new boolean[M.getNsplits() + 1];
        bundledSum = new double[M.getNblocks()];
        double bundledMean = 0.0;

        for (int i = 1; i <= M.getNsplits(); i++) {

            if (2 > 1) break; // todo: this must be a bug????

            double[] row = M.getMatrixRow(i);
            int count = 0;
            for (int j = 0; j < M.getNblocks(); j++)
                if (row[j] > 0.0)
                    count++;
            double prop = (double) count / M.getNblocks();

            // if (prop <= cutoff) {
            if (!(M.getOriginal(i) > 0.0)) {
                numBundled++;
                isBundled[i] = true;
                for (int j = 0; j < nblocks; j++)
                    bundledSum[j] += row[j];
            } else
                isBundled[i] = false;
        }
        if (numBundled > 0) {
            for (int j = 0; j < nblocks; j++)
                bundledMean += bundledSum[j];
			bundledMean /= nblocks;
        }
        System.err.println("COnfidence interval on " + (M.getNsplits() - numBundled) + "splits, with " + numBundled + " bundled.");

        /*STEP ONE:
          Compute sn vector, and sort each row of Rij's
        */
        for (int i = 0; i <= nsplits; i++) {
            double[] row;
            double original;

            if (i == 0 && numBundled > 0) {
                row = bundledSum;
                original = bundledMean;
            } else if (i > 0 && !isBundled[i]) {
                row = M.getMatrixRow(i);
                original = M.getOriginal(i);
            } else
                continue;

            int rank = 0;
            double prevRij = -1.0;

			for (int j = 0; j < nblocks; j++) {
				double Rij = Math.abs(row[j] - original);
				if (Rij != prevRij) {
					rank = j;
					prevRij = Rij;
				}
				DoubleInt x = new DoubleInt(Rij, rank);
				rowIndices[j] = x;
			}

			Arrays.sort(rowIndices, (Comparator) (x, y) -> {
				if (((DoubleInt) x).Rij < ((DoubleInt) y).Rij)
					return -1;
				else if (((DoubleInt) y).Rij < ((DoubleInt) x).Rij)
					return 1;
				else
					return 0;
			});

			for (int k = 0; k < nblocks; k++) {
				int j = rowIndices[k].j;
				sn[j] = Math.max(k, sn[j]);
			}

			R[i] = new double[nblocks];
			for (int k = 0; k < nblocks; k++)
				R[i][k] = rowIndices[k].Rij;
        }

        /* STEP TWO: Identify level = (1-alpha) quantile for the sn table */
        Arrays.sort(sn);
        double cn = (double) sn[Math.min(sn.length - 1, (int) Math.floor((level) * nblocks))] / nblocks;

        /* Compute individual confidence levels */

        Splits newSplits = new Splits(M.getNtax());
        newSplits.getFormat().setIntervals(true);
        newSplits.getFormat().setConfidences(false);
        newSplits.getFormat().setWeights(true);


        for (int i = 1; i <= nsplits; i++) {
            if (isBundled[i])
                continue;
            double di = R[i][(int) Math.floor(cn * nblocks)];
            double xi = M.getOriginal(i);
            double ai = Math.max(0.0, xi - di);
            double bi = xi + di;
            Interval interval = new Interval(ai, bi);

            if (bi > 0.0) {
                TaxaSet A = M.getSplit(i);
                int numNonZero = 0;
                double mean = 0.0;
                for (int j = 0; j < nblocks; j++)
                    if (M.get(i, j) > 0.0) {
                        numNonZero++;
                        mean += M.get(i, j);
                    }
                float confidence = (float) numNonZero / nblocks;
                float weight = 0;

                if (weightMethod == ConfidenceNetworkDialog.FREQ)
                    weight = (float) Math.round(10.0 * confidence) / 10;
                else if (weightMethod == ConfidenceNetworkDialog.LOWER)
                    weight = (float) ai;
                else if (weightMethod == ConfidenceNetworkDialog.ESTIMATED)
                    weight = (float) xi;
                else if (weightMethod == ConfidenceNetworkDialog.MID)
                    weight = (float) ((ai + bi) / 2.0);
                else if (weightMethod == ConfidenceNetworkDialog.UPPER)
                    weight = (float) bi;


                newSplits.add(A, weight, confidence, interval, "");
            }
        }


        return newSplits;
    }

    /**
     * Produces a set of splits such that the probability a sampled
     * network would be outside the set of splits has probability at most level.
     *
     * @return splits
     */
    static private Splits getFakeConfidenceNetwork(SplitMatrix M, double level, int weightType) {
        /* Compute the number of times each split appears in a replicate */
        int nsplits = M.getNsplits();
        int nblocks = M.getNblocks();

        int[] splitCount = new int[nsplits + 1];
        for (int i = 1; i <= nsplits; i++) {
            double[] row = M.getMatrixRow(i);
            int count = 0;
            for (int j = 0; j < nblocks; j++) {
                if (row[j] > 0.0)
                    count++;
            }
            splitCount[i] = count;
        }

        /* For each replicate,  compute the minimum count for a split in that replicate */
        int[] minSplits = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            int minCount = nblocks + 1;
            for (int j = 1; j <= nsplits; j++)
                if (M.get(j, i) > 0.0 && splitCount[j] < minCount)
                    minCount = splitCount[j];
            minSplits[i] = minCount;
        }

        /* Determine cutoff for the splits. Specifically we want cutoff as large
        as possible such that the proportion of replicates i with minCount[i] >= cutoff
        is at least 1 - level.
        */
        int cutoff;
        {
            int[] v = new int[nblocks];
            System.arraycopy(minSplits, 0, v, 0, nblocks);
            Arrays.sort(v);
            int x = (int) Math.floor((1.0 - level) * nblocks);
            cutoff = v[x];
        }

        /* Form a network from all splits such that splitCount >= cutoff */
        Splits newSplits = new Splits(M.getNtax());
        newSplits.getFormat().setIntervals(true);
        newSplits.getFormat().setConfidences(true);
        newSplits.getFormat().setWeights(true);

        double[] medianVec = new double[nblocks];

        for (int i = 1; i <= nsplits; i++) {
            if (splitCount[i] >= cutoff) {
                TaxaSet A = M.getSplit(i);
                String label = M.getLabel(i);
                Interval interval = null;
                double[] row = M.getMatrixRow(i);
                double sum = 0.0;
                int count = 0;
                int numNonZero = 0;

                for (int j = 0; j < nblocks; j++) {
                    if (minSplits[j] < cutoff)
                        continue;
                    sum += row[j];
                    medianVec[count] = row[j];
                    if (row[j] > 0.0)
                        numNonZero++;

                    count++;
                    if (interval == null) {
                        interval = new Interval((float) row[j], (float) row[j]);
                    } else if (row[j] < interval.low) {
                        interval.low = (float) row[j];
                    } else if (row[j] > interval.high) {
                        interval.high = (float) row[j];
                    }
                }
                if (interval.high >= 0.0001) {
                    float weight = 0;

                    if (weightType == ConfidenceNetworkDialog.FREQ)
                        weight = (float) Math.round(10.0 * (double) numNonZero / count) / 10;
                    else if (weightType == ConfidenceNetworkDialog.LOWER)
                        weight = interval.low;
                    else if (weightType == ConfidenceNetworkDialog.ESTIMATED)
                        weight = (float) median(medianVec, 0, count);
                    else if (weightType == ConfidenceNetworkDialog.MID)
                        weight = (float) ((interval.high + interval.low) / 2.0);
                    else if (weightType == ConfidenceNetworkDialog.UPPER)
                        weight = interval.high;

                    float confidence = (float) numNonZero / nblocks;
                    newSplits.add(A, weight, confidence, interval, "");
                }
            }
        }

        return newSplits;
    }

    static public void getSingularValues(SplitMatrix splitMatrix) {
        int nrows = splitMatrix.getNsplits();
        int ncols = splitMatrix.getNblocks();
        Matrix M = new Matrix(nrows, ncols);
        for (int i = 0; i < nrows; i++)
            for (int j = 0; j < ncols; j++)
                M.set(i, j, splitMatrix.get(i + 1, j + 1));

        SingularValueDecomposition svd = new SingularValueDecomposition(M);
        double[] singValues = svd.getSingularValues();

        //Sort in increasing order
        Arrays.sort(singValues);
        System.err.println("Singular values:");
        for (int i = singValues.length - 1; i >= 0; i--)
            System.err.println(singValues[i]);
    }


}
