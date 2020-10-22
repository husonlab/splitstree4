/**
 * LineSearch.java
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
package splitstree4.algorithms.util.optimization;

/**
 * Abstract class implementing a line search
 * <p>
 * * Note: all arrays and vectors start their indexing at 1.
 */
public abstract class LineSearch {


    /**
     * Abstract signature for a line search procedure. Optimizes along the line x + \alpha p
     *
     * @param x           1d array of double giving starting point
     * @param p           array with same dimensions as x. Direction of search
     * @param grad0       array with same dimensions as x. Gradient at x
     * @param f0          double. Value of function at x
     * @param alpha_init  Initial value of alpha to try.
     * @param f           AbstractFunction function being minimised
     * @param xnew        array with same dimension as x. Overwritten by point at end of linesearch
     * @param phi_p_alpha array with same dimension as x. Overwritten by gradient at xnew
     * @return double value of f at xnew
     * @throws NumericalException
     */
    abstract double linesearch(double[] x,
                               double[] p,
                               double[] grad0,
                               double f0,
                               double alpha_init,
                               AbstractFunction f,
                               double[] xnew,
                               double[] phi_p_alpha
    ) throws NumericalException;
}
