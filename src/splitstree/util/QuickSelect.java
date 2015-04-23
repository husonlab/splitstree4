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
