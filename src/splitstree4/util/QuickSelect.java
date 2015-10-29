/**
 * QuickSelect.java
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
package splitstree4.util;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 24, 2005
 * Time: 2:13:40 PM
 * <p/>
 * Routines for efficiently computing the median, upper and lower quantiles of an array.
 * <p/>
 * Based on algorithms in
 * Data Structures and Algorithm AnalysisMethod in Java, Mark Weiss
 * See also Cormen etal, 2nd ed., pg 186.
 * <p/>
 * //ToDo: make this general. Only works for floats now !?!
 */


public final class QuickSelect {
    /**
     * Simple insertion sort.
     *
     * @param a an array of float items.
     */

    public static void swap(float[] a, int index1, int index2) {
        float tmp = a[index1];
        a[index1] = a[index2];
        a[index2] = tmp;
    }


    /**
     * Quick selection algorithm.
     * Places the kth smallest item in a[k-1].
     *
     * @param a an array of float items.
     * @param k the desired rank (1 is minimum) in the entire array.
     */
    public static void quickSelect(float[] a, int k) {
        quickSelect(a, 0, a.length - 1, k);
    }


    /**
     * Returns median of the array - note that this will change the ordering in the array!
     *
     * @param a
     * @return
     */
    public static float median(float[] a) {
        int k = a.length / 2 + 1;
        quickSelect(a, k);
        return a[k - 1];
    }


    /**
     * Select the kth smallest element in the array a, with linear expected time.
     *
     * @param a
     * @param low  leftmost index (of subarray)
     * @param high rightmost index (of subarray)
     * @param k
     */
    private static void quickSelect(float[] a, int low, int high, int k) {

        if (low + 10 > high)
            Arrays.sort(a, low, high);
        else {
            // Sort low, middle, high
            int middle = (low + high) / 2;
            if (a[middle] < a[low])
                swap(a, low, middle);
            if (a[high] < a[low])
                swap(a, low, high);
            if (a[high] < a[middle])
                swap(a, middle, high);

            // Place pivot at position high - 1
            swap(a, middle, high - 1);
            float pivot = a[high - 1];

            // Begin partitioning
            int i, j;
            for (i = low, j = high - 1; ; ) {
                while (a[++i] < pivot)
                    ;
                while (pivot < a[--j])
                    ;
                if (i >= j)
                    break;
                swap(a, i, j);
            }

            // Restore pivot
            swap(a, i, high - 1);

            // Recurse; only this part changes
            if (k <= i)
                quickSelect(a, low, i - 1, k);
            else if (k > i + 1)
                quickSelect(a, i + 1, high, k);
        }
    }

}
