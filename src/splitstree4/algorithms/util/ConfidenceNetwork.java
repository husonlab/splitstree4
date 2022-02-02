/*
 * ConfidenceNetwork.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.util.Interval;
import splitstree4.util.SplitMatrix;

import java.util.Arrays;
import java.util.Comparator;

/**
 * User: bryant
 * Date: Mar 13, 2006
 * This class creates a confidence network, at a given level, from a split matrix.
 * <p/>
 * <p/>
 * Applies the B Method detailed in
 * Beran, R. 1990 "Refining Bootstrap Simultaneous Confidence Sets". J. Amer. Stat. Assoc.
 * 85(410) 417--426
 * and in
 * Beran, R. 1988 "Balanced Simultaneous Confidence Sets". J. Amer. Stat. Assoc.
 * 83(403) 679--686
 * Using the root function  |x_i* - \hat{x_i}} for split i. Here \hat{x_i} is the estimated value for the
 * ith split weight, while x_i* is the weight for the ith split in the bootstrap replicate.
 */
public class ConfidenceNetwork {

    static private class DoubleInt {
        public DoubleInt(double rij, int j) {
            this.Rij = rij;
            this.j = j;
        }

        public double Rij;
		public final int j;
    }

    /**
     * Sort a row (1..nblocks) of DoubleInts according to the Rij value.
     *
	 */
    private static void sortAscending(DoubleInt[] row) {
		Arrays.sort(row, 1, row.length, (Comparator) (x, y) -> {
			if (((DoubleInt) x).Rij < ((DoubleInt) y).Rij)
				return -1;
			else if (((DoubleInt) y).Rij < ((DoubleInt) x).Rij)
				return 1;
			else
				return 0;
		});
	}

    /**
     * Returns a set of splits corresponding to a confidence network, from the specified split matrix.
     *
	 */
    static public Splits getConfidenceNetwork(SplitMatrix M, double level, Document doc) throws CanceledException {
        int nsplits = M.getNsplits();   //Number of splits.... |U| in Beran 88
        int nblocks = M.getNblocks();  //the value jn in Beran 88

        DoubleInt[] row = new DoubleInt[nblocks + 1];  //Row vector, indexed 1...nblocks
        int[] maxH = new int[nblocks + 1];
        double[] medians = new double[nsplits + 1];


        for (int i = 1; i <= nsplits; i++) {
            for (int j = 1; j <= nblocks; j++) {
                double x = M.get(i, j);
                row[j] = new DoubleInt(x, j);
            }

            //Find the median value.
            sortAscending(row);
            int mid = (int) Math.floor(((double) nblocks - 1.0) / 2.0) + 1;
            double median;
            if (nblocks % 2 == 0)
                median = (row[mid].Rij + row[mid + 1].Rij) / 2.0;
            else
                median = row[mid].Rij;
            //Save the median for later.
            medians[i] = median;

            //The "root" value is the abs diff between value and median.
            for (int j = 1; j <= nblocks; j++)
                row[j].Rij = Math.abs(row[j].Rij - median);

            //Now for each entry j, we count the number of entries k such that
            //Rik <= Rij. For this, we first sort R.
            sortAscending(row);
            int count = nblocks;
            double val = row[nblocks].Rij;
            for (int k = nblocks; k >= 1; k--) {
                DoubleInt x = row[k];
                if (k < nblocks && x.Rij < val)
                    count = k + 1;
                val = x.Rij;
                //int Hij = count;
                maxH[x.j] = Math.max(maxH[x.j], count);
            }
            doc.notifySetProgress((60 * i) / nsplits);
        }

        //We now have, for each j, that choosing a cut off of maxH[j] or more
        //means that all the splits in that column will get included.
        //We'd like to find as small a value K as possible so that maxH[j] <= K
        //for at least level * nblocks of the j's.
        Arrays.sort(maxH);
        int n = (int) Math.ceil(level * nblocks);
        int cutoffH = maxH[n];

        //Now go through the splits again, this time computing the values
        //NOTE: in this version we do extra calculations (sorting) here in order
        //to reduce memory usage.
        Splits newSplits = new Splits(M.getNtax());
        newSplits.getFormat().setIntervals(true);
        newSplits.getFormat().setConfidences(false);
        newSplits.getFormat().setWeights(true);

        for (int i = 1; i <= nsplits; i++) {
            double median = medians[i];
            for (int j = 1; j <= nblocks; j++) {
                double x = M.get(i, j);
                row[j] = new DoubleInt(Math.abs(x - median), j);
            }

            //Find the cutoff value.
            sortAscending(row);
            double cutoffRij = row[cutoffH].Rij;
            double low = median - cutoffRij;
            double high = median + cutoffRij;
            TaxaSet sp = M.getSplit(i);
            if (high > 0.0)
                newSplits.add(sp, (float) median, 0, new Interval(low, high), "");
            doc.notifySetProgress(40 + (60 * i) / nsplits);
        }
        return newSplits;
    }


}
