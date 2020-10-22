/**
 * ActiveSet.java
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
/*
 *
 * @author David Bryant
 *
 *
 * Created on Jan 20, 2004
 *
 * Implements the active set method - used for solving +vely constrained quadrative programs.
 * Can also be used to solve the unconstrained problem.
 *
 * Given a matrix A and a vector (matrix with one column) b
 * Returns vector (matrix with one column) x such that
 * 		[Ax-b]_i  >=  0 for all i
 * 		[Ax-b]_i   =   0 for all i such that x_i > 0
 *
 * References:
 * 	The java implementation is inspired by the JAMA package (http://math.nist.gov/javanumerics/jama/)
 * 	Our algorithms are taken from Golub and Van Loan "Matrix Computations", 2nd edition.
 *
 */
package splitstree4.util.matrix;

import Jama.Matrix;

import java.io.Serializable;

//ToDO: Compute and save the fit.

/**
 * @author bryant
 * <p/>
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ActiveSet implements Serializable {

    /* ------------------------
            Class variables
          * ------------------------ */

    /**
     * Array for storing solution
     *
     * @serial internal array storage.
     */
    private Matrix x;


    private static final double EPSILON = 1E-10;
    /* ------------------------
            Constructor
          * ------------------------ */

    /**
     * Active Set method.
     *
     * @param XtX       Square, symmetric matrix.     X'X
     * @param Xty       X'y Matrix with one column and same number of rows as A
     * @param constrain Boolean - true if positively constrained solution to be used
     */
    public ActiveSet(Matrix XtX, Matrix Xty, boolean constrain) {

        int n;

        //System.out.println("Matrix argument passed is\n");
        //XtX.print(4,4);

        DynamicCholesky A = new DynamicCholesky(XtX);

        n = XtX.getRowDimension();
        x = new Matrix(n, 1, 0.0);
        //System.out.println("Checking Cholesky Decomposition!");
        //A.CheckCholesky();

        //Xty.print(10,4);

        if (!constrain) {
            //Unconstrained solution. Just solve using Cholesky decomposition and exit.
            x = A.solve(Xty);
            //Set negative values to zero.
            for (int i = 0; i < n; i++) {
                if (x.get(i, 0) < 0.0)
                    x.set(i, 0, 0.0);
            }
            //x.print(4,4);
            return;
        }
        //Constrained solution required.
        //Active set stored in mask entry of A - its initially empty, as we want.

        //Initial solution is arbitrary - all 1's.
        Matrix old_x = new Matrix(n, 1, 1.0);
        //System.out.println("Initial feasible solution");
        //old_x.print(10,4);
        for (; ; ) {
            for (; ; ) {
                x = A.solve(Xty); // Solve current constrained solution.
                //System.out.println("Current unconstrained solution:");
                //x.print(10,4);
                //Find the last point on the path from old_x to x that is non-negative.
                int bad_i = -1;
                double min_delta = 100000000000.0;
                for (int i = 0; i < n; i++) {
                    if (x.get(i, 0) < -EPSILON) {
                        double oldx_i = old_x.get(i, 0);
                        double x_i = x.get(i, 0);
                        double delta = oldx_i / (oldx_i - x_i);
                        if (delta < min_delta) {
                            bad_i = i;
                            min_delta = delta;
                        }
                    }
                }
                //System.out.println(bad_i);
                //System.out.println(min_delta);

                if (bad_i == -1) {
                    break; //This is a feasible solution. Skip to outer loop.
                }

                //Move oldx to the last feasible point on line from oldx to x
                for (int i = 0; i < n; i++) {
                    if (A.getmask(i))
                        continue;
                    double oldx_i = old_x.get(i, 0);
                    double x_i = x.get(i, 0);
                    oldx_i = oldx_i + min_delta * (x_i - oldx_i);
                    old_x.set(i, 0, oldx_i);
                }

                //old_x.plusEquals(x.times(bad_val));
                //System.out.println("This feasible solution");
                //old_x.print(10,4);
                A.maskRow(bad_i);

            }

            //Check that gradient is non-negative for all indices in active set.
            // and find index with smallest gradient.
            // Note: gradient/2 = Ax - Xty

            int bad_i = -1;
            double bad_val = 100000000000.0;
            Matrix grad;

            //System.out.println("Gradient");
            grad = XtX.times(x).minus(Xty).times(2.0);
            //grad.print(10,4);

            for (int i = 0; i < n; i++) {
                if (A.getmask(i)) {
                    double grad_i = grad.get(i, 0);
                    grad_i *= 2.0;
                    if (grad_i < bad_val) {
                        bad_i = i;
                        bad_val = grad_i;
                    }
                } else {
                    if (Math.abs(grad.get(i, 0)) > EPSILON) {
                        throw new IllegalStateException("Problem in the active set method");
                    }

                }
            }
            if (bad_val > -EPSILON) {
                break;
            }
            A.unmaskRow(bad_i); //Remove worst from mask

        }


    }

    /**
     * Get Solution
     * author bryant
     *
     * @return vector containing optimal vector
     */
    public Matrix getSoln() {
        return x;
    }

    /**
     * Get indexed solution
     *
     * @param i
     * @return value of ith element in solution vector
     */
    public double getSoln(int i) {
        if ((i < 0) || (i >= x.getRowDimension())) {
            throw new IllegalArgumentException("Index of element invalid");
        }
        return x.get(i, 0);
    }


}
