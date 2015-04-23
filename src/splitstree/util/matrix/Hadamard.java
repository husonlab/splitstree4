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

package splitstree.util.matrix;

/**
 * implements the hadamard conjugation
 *
 * @author huson
 *         Date: 21-Mar-2005
 *         based on Pascal code written by Mike Hendy
 */
public class Hadamard {

    /**
     * Computes y=Hx
     *
     * @param x
     * @return Hx
     */
    static public float[] compute(float[] x) {
        return compute(x, new float[x.length]);
    }

    /**
     * Computes y=Hx
     *
     * @param x input array
     * @param y output array
     * @return Hx
     */
    static public float[] compute(float[] x, float[] y) {
        float[] tmp = x.clone();

        int step, i, j, k;


        step = 1;
        while (step < x.length) {
            if (step > 1) {
                System.arraycopy(y, 0, tmp, 0, tmp.length);
            }
            i = 0;
            while (i < x.length) {
                k = i;

                i += step;

                j = i;

                while (k < i) {
                    y[k] = tmp[k] + tmp[j];
                    y[j] = tmp[k] - tmp[j];
                    k++;
                    j++;
                }
                i += step;
            }
            step += step;
        }
        return y;
    }

    /**
     * Computes the inverse hadamard: y=H_inv x using the formula y= 1/#rows H x
     *
     * @param x
     * @return 1/#rows H x
     */
    static public float[] computeInverse(float[] x) {
        return computeInverse(x, new float[x.length]);
    }

    /**
     * Computes the inverse hadamard: y=H_inv x using the formula y= 1/#rows H x
     *
     * @param x input array
     * @param y output array
     * @return 1/#rows H x
     */

    static public float[] computeInverse(float[] x, float[] y) {
        if (x.length == 0)
            return null;
        compute(x, y);
        for (int i = 0; i < x.length; i++)
            y[i] /= x.length;
        return y;
    }

}
