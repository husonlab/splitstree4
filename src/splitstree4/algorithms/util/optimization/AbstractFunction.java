/*
 * AbstractFunction.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util.optimization;

/**
 * Abstract Function class
 * <p>
 * Used for optimization algorithms.
 * <p>
 * Note: all arrays and vectors start their indexing at 1.
 */
public abstract class AbstractFunction {
    /**
     * Return the value of f at x
     *
     * @param x array of doubles
     * @return double
     */
    abstract public double get_val(double[] x);

    /**
     * Compute the gradient of the function at point x
     *
     * @param x 1d array of doubles
     * @param g 1d array with the same dimension as x
     *          <p>
     *          The array g is overwritten with the gradient.
     */
    abstract public void get_grad(double[] x, double[] g);

    /**
     * Computes the product of a given vector v with the Hessian of f at x
     *
     * @param x  1d array of double
     * @param v  1d array of double with the same dimensions as x
     * @param hv 1d array of double with the same dimensions as x
     *           <p>
     *           The array hv is overwritten with the produce H(x)v, where H(x) is the Hessian
     *           of f at x
     */
    abstract public void get_Hv(double[] x, double[] v, double[] hv);

    /**
     * Returns value and gradient of f at point x
     *
     * @param x 1d array of double
     * @param g array of double with same dimension as x. Overwritten with the gradient
     * @return double. Value of f(x)
     */
    public double get_val_and_grad(double[] x, double[] g) {
        get_grad(x, g);
        return get_val(x);
    }
}
