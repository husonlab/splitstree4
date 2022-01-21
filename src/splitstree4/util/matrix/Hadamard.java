/*
 * Hadamard.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util.matrix;

/**
 * implements the hadamard conjugation
 *
 * @author huson
 * Date: 21-Mar-2005
 * based on Pascal code written by Mike Hendy
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
