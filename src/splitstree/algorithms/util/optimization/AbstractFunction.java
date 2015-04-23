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

package splitstree.algorithms.util.optimization;

/**
 * Abstract Function class
 *
 * Used for optimization algorithms.
 *
 * Note: all arrays and vectors start their indexing at 1.
 */
public abstract class AbstractFunction {
    /**
     * Return the value of f at x
     * @param x  array of doubles
     * @return double
     */
    abstract public double get_val(double[] x);

    /**
     * Compute the gradient of the function at point x
     * @param x  1d array of doubles
     * @param g  1d array with the same dimension as x
     *
     * The array g is overwritten with the gradient.
     */
    abstract public void get_grad(double[] x, double[] g);

    /**
     * Computes the product of a given vector v with the Hessian of f at x
     * @param x 1d array of double
     * @param v  1d array of double with the same dimensions as x
     * @param hv  1d array of double with the same dimensions as x
     *
     * The array hv is overwritten with the produce H(x)v, where H(x) is the Hessian
     * of f at x
     */
    abstract public void get_Hv(double[] x, double[] v, double[] hv);

    /**
     * Returns value and gradient of f at point x
     * @param x   1d array of double
     * @param g   array of double with same dimension as x. Overwritten with the gradient
     * @return double. Value of f(x)
     */
    public double get_val_and_grad(double[] x, double[] g) {
        get_grad(x,g);
        return get_val(x);
    }
}
