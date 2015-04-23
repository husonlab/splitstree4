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
 * Abstract class implementing a line search
 *
 *  * Note: all arrays and vectors start their indexing at 1.
 */
public abstract class LineSearch {


    /**
     * Abstract signature for a line search procedure. Optimizes along the line x + \alpha p
     * @param x 1d array of double giving starting point
     * @param p  array with same dimensions as x. Direction of search
     * @param grad0  array with same dimensions as x. Gradient at x
     * @param f0   double. Value of function at x
     * @param alpha_init   Initial value of alpha to try.
     * @param f    AbstractFunction function being minimised
     * @param xnew  array with same dimension as x. Overwritten by point at end of linesearch
     * @param phi_p_alpha   array with same dimension as x. Overwritten by gradient at xnew
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
