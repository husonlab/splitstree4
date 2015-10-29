/**
 * DynamicCholesky.java
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
  * Solves linear equations with symmetric positive definite matrices
  * using Cholesky decomposition. Rows can be removed from the analysis and re-inserted.
  *
  *
  * This is a dynamic version - we implement the option to 'mask' some rows - effectively
  * deleting them from the analysis.
  *
  * At any point in time, the matrix L is the lower triangular factor in the cholesky
  * decomposition for the matrix A restricted to rows and columns that are not masked and,
  * possibly, with rows and columns permuted.
  *
  * We can mask or unmask a row or set of rows.
  *
  * When a set of rows is unmasked, we add these to the bottom of the cholesky decomposition.
  * This update takes O(n^2 k ) time, where k is the number of rows added.
  *
  * When a set of rows is masked we delete them, and then update the factorisation using a
  * modified givens rotation method (adapted from the QR decomposition). This also takes
  * O(n^2 k ) time.
  *
  * If we remove or add rows, the order of the rows in our decomposition will be mucked up. The
  * arrays full2partial and partial2full keep track of which row in the full matrix corresponds to
  * which row in the partial matrix.
  *
  *
  * References:
  *         The java implementation is inspired by the JAMA package (http://math.nist.gov/javanumerics/jama/)
  *         Our algorithms are taken from Golub and Van Loan "Matrix Computations", 2nd edition.
  *
  * The use of Givens rotations for downdating the Cholesky was suggested by Xiao-Wen Chang.
  */
package splitstree4.util.matrix;


import Jama.Matrix;

import java.io.Serializable;
import java.util.BitSet;

public class DynamicCholesky implements Serializable {
    /* ------------------------
    Class variables
    * ------------------------ */

    /**
     * Array for internal storage of decomposition.
     *
     * @serial internal array storage.
     */
    private double[][] Lfull; /* Lower triangle contains cholesky factor. Upper contains A  */
    private double[] Adiagfull; /* Diagonal of full matrix */

    private double[][] L;  /* Lower triangle cholesky factor for currentlu unmasked entries */
    private double[] Adiag; /* Current diagonal of the cholesky factor for unmasked rows */


    /**
     * Row and column dimension (square matrix).
     *
     * @serial matrix dimension.
     */
    private int n;

    /**
     * Number of rows currently not deleted
     */
    private int ncurr;

    /**
     * Symmetric and positive definite flag.
     *
     * @serial is symmetric and positive definite flag.
     */
    private boolean isspd;


    /**
     * Current mask
     *
     * @serial current mask
     */
    private BitSet mask;

    /**
     * Map from indices in the complete matrices to rows in the current submatrix.
     */
    private int[] full2partial;
    /**
     * Map from indices in the submatrix to indices in the complete matrix
     */
    private int[] partial2full;

    /* ------------------------
    Constructor
  * ------------------------ */

    /**
     * Set up data structures for the dynamic Cholesky algorithm and
     * compute Cholesky factorisation for the complete matrix
     *
     * @param Arg Square, symmetric matrix.
     */

    public DynamicCholesky(Matrix Arg) {
        // Initialize.
        mask = new BitSet();
        double[][] A = Arg.getArray();
        n = Arg.getRowDimension();
        Lfull = new double[n][n];
        ncurr = n;

        full2partial = new int[n];
        partial2full = new int[n];

        //Set up the maps back and forward;
        for (int i = 0; i < n; i++) {
            full2partial[i] = i;
            partial2full[i] = i;
        }

        //Compute Cholesky decomposition for the full matrix
        isspd = (Arg.getColumnDimension() == n);
        // Main loop.
        for (int j = 0; j < n; j++) {
            double[] Lrowj = Lfull[j];
            double d = 0.0;
            for (int k = 0; k < j; k++) {
                double[] Lrowk = Lfull[k];
                double s = 0.0;
                for (int i = 0; i < k; i++) {
                    s += Lrowk[i] * Lrowj[i];
                }
                Lrowj[k] = s = (A[j][k] - s) / Lfull[k][k];
                d = d + s * s;
                isspd = isspd & (A[k][j] == A[j][k]);
            }
            d = A[j][j] - d;
            isspd = isspd & (d > 0.0);
            Lfull[j][j] = Math.sqrt(Math.max(d, 0.0));
            for (int k = j + 1; k < n; k++) {
                Lfull[j][k] = 0.0;
            }
        }

        //Copy A into upper triangle of Lfull
        Adiagfull = new double[n];
        for (int i = 0; i < n; i++) {
            Adiagfull[i] = A[i][i];
            System.arraycopy(A[i], i + 1, Lfull[i], i + 1, n - (i + 1));
        }

        L = new double[n][n];
        Adiag = new double[n];
        // COPY Lfull to L
        for (int i = 0; i < n; i++) {
            Adiag[i] = Adiagfull[i];
            System.arraycopy(Lfull[i], 0, L[i], 0, n);
        }
    }

    /* ------------------------
    Constructor
  * ------------------------ */

    /**
     * Set up data structures for the dynamic Cholesky algorithm, but with
     * all but one rows masked. Allocates memory, but does not perform factorisation
     *
     * @param Arg Square, symmetric matrix.
     * @param row int. The row to leave unmasked.
     */

    public DynamicCholesky(Matrix Arg, int row) {
        // Initialize.
        mask = new BitSet();
        double[][] A = Arg.getArray();
        n = Arg.getRowDimension();
        Lfull = new double[n][n];
        ncurr = 1;

        full2partial = new int[n];
        partial2full = new int[n];

        //Set up the maps back and forward;
        for (int i = 0; i < n; i++) {
            full2partial[i] = -1;
            partial2full[i] = -1;
        }
        full2partial[row] = 0;
        partial2full[0] = row;

        //Copy A into upper triangle of Lfull
        Adiagfull = new double[n];
        for (int i = 0; i < n; i++) {
            Adiagfull[i] = A[i][i];
            System.arraycopy(A[i], i + 1, Lfull[i], i + 1, n - (i + 1));
        }

        L = new double[n][n];
        Adiag = new double[n];
        Adiag[0] = Adiagfull[row];
        isspd = (Adiag[0] > 0);
    }
    /* ------------------------
    Public Methods
  * ------------------------ */

    /**
     * Is the matrix symmetric and positive definite?
     *
     * @return true if A is symmetric and positive definite.
     */

    public boolean isSPD() {
        return isspd;
    }


    /**
     * Unmasks a row of the full matrix, including the row in the cholesky decomposition.
     *
     * @param rowToUnmask
     */

    public void unmaskRow(int rowToUnmask) {
        if (mask.get(rowToUnmask)) {
            int newrow = ncurr;
            ncurr = ncurr + 1;
            full2partial[rowToUnmask] = newrow;
            partial2full[newrow] = rowToUnmask;
            mask.set(rowToUnmask, true);

            /* Update the A matrix part of L... copy these from the full matrix */
            for (int i = 0; i < newrow; i++) {
                int full_i = partial2full[i];
                if (rowToUnmask > full_i)
                    L[i][newrow] = Lfull[full_i][rowToUnmask];
                else
                    L[i][newrow] = Lfull[rowToUnmask][full_i];
            }

            Adiag[newrow] = Adiagfull[rowToUnmask];

            /* Now we fill out the last row of the Cholesky decomposition */
            double rsum;
            for (int i = 0; i < newrow; i++) {
                rsum = L[i][newrow];
                for (int j = 0; j < i; j++)
                    rsum = rsum - L[newrow][j] * L[i][j];
                L[newrow][i] = rsum / L[i][i];
            }
            rsum = Adiag[newrow];
            for (int j = 0; j < newrow; j++) {
                double x = L[newrow][j];
                rsum = rsum - x * x;
            }
            L[newrow][newrow] = Math.sqrt(rsum);
        }
    }

    /**
     * Unmask multiple rows. Equivalent to repeated calls to unmaskRow (no gain in efficiency)
     *
     * @param rowsToUnmask int[]
     */

    public void unmaskRows(int[] rowsToUnmask) {
        for (int aRowsToUnmask : rowsToUnmask) unmaskRow(aRowsToUnmask);
    }

    /**
     * givens
     * <p/>
     * <p/>
     * Returns the parameters used for a Givens rotation
     * Vector returned is [c,s]. The corresponding Givens
     * matrix is
     * [c -s]
     * [s  c]
     */
    private double[] givens(double a, double b) {
        double tau, s, c;
        double[] Gtmp = new double[2];

        if (b != 0) {
            if (Math.abs(b) > Math.abs(a)) {

                tau = -a / b;
                s = 1.0 / (Math.sqrt(1.0 + (tau * tau)));
                c = s * tau;
            } else {

                tau = -b / a;
                c = 1.0 / (Math.sqrt(1.0 + (tau * tau)));
                s = c * tau;
            }
        } else {
            c = 1;
            s = 0;
        }
        Gtmp[0] = c;
        Gtmp[1] = s;

        return Gtmp;
    }

    /**
     * maskRow
     *
     * @param rowToMask Row to add to the mask.
     *                  <p/>
     *                  Remove a row from the system (add the row to the mask). The cholesky factorisation is then updated
     *                  <p/>
     *                  * Steps:
     *                  (1) Identify which row in the small matrix L we want to remove
     *                  (2) Use  (transposed) Givens rotations to update the remaining rows of L to be
     *                  the Cholesky factor of the reduced matrix
     *                  (3) Update the upper triangle (A) and Adiag, as well as mask variables and
     *                  full2partial translations
     */
    public void maskRow(int rowToMask) {
        double[] G;

        if (mask.get(rowToMask))
            return;

        //(1) row is the row in the small (partial) matrix
        int row = full2partial[rowToMask];

        //System.out.println("Deleting full row number "+ row + "\n");

        //(2) Givens updating on the lower triangle

        for (int i = row + 1; i < ncurr; i++) {
            double x0 = L[i][i - 1];
            double x1 = L[i][i];
            G = givens(x0, x1);
            double c = G[0];
            double s = G[1];

            //We always choose a multiplier so that the diagonal is positive.
            if ((c * x0 - s * x1) < 0) {
                c = -c;
                s = -s;
            }

            //System.out.println("x0 = "+x0+"\t x1 = "+x1+"\t c = "+c+" s = "+s+"\n This:" + (c*x0 - s*x1)+ " should not be zero\n");
            //System.out.println("This:" + (s*x0+c*x1) + " should be zero\n");
            for (int j = i; j < ncurr; j++) {
                x0 = L[j][i - 1];
                x1 = L[j][i];
                L[j][i - 1] = c * x0 - s * x1;
                L[j][i] = s * x0 + c * x1;
            }
            //System.out.println("Updated row "+i);
            //for(int a = 0;a<ncurr;a++) {
            //	for(int b=0;b<=a;b++) {
            //	}
            //	System.out.println("");
            //}
        }

        //(3) Update everything

        //Delete the row in the lower triangular part - we have to move entries up
        for (int i = row + 1; i < ncurr; i++) {
            System.arraycopy(L[i], 0, L[i - 1], 0, i);
        }
        //Delete the column in the upper triangular part
        //First move those entries above "row" row and to the right of column "row" left.
        for (int i = 0; i < row; i++) {
            System.arraycopy(L[i], row + 1, L[i], row + 1 - 1, ncurr - (row + 1));
        }
        //Now those below (and therefore right) of the deleted row up one and left one.
        for (int i = row + 1; i < ncurr; i++) {
            System.arraycopy(L[i], i + 1, L[i - 1], i + 1 - 1, ncurr - (i + 1));
        }

        //And update A diag.
        System.arraycopy(Adiag, row + 1, Adiag, row + 1 - 1, ncurr - (row + 1));
        //TO help with debugging, we zero the last row and column
        for (int i = 0; i < ncurr; i++) {
            L[i][ncurr - 1] = 0.0;
            L[ncurr - 1][i] = 0.0;
            Adiag[ncurr - 1] = 0.0;
        }

        //Update the maps to and from the full to the partial indices
        for (int i = row + 1; i < ncurr; i++) {
            int full = partial2full[i];
            partial2full[i - 1] = full;
            full2partial[full] = full2partial[full] - 1;
        }

        //Reduce the matrix size
        ncurr = ncurr - 1;
        mask.set(rowToMask, true);

    }

    /**
     * Unmask multiple rows. Equivalent to repeated calls to maskRow (no gain in efficiency)
     *
     * @param rowsToMask int[]
     */

    public void maskRows(int[] rowsToMask) {
        for (int aRowsToMask : rowsToMask) maskRow(aRowsToMask);
    }

    /**
     * Reset all mask information so that only one row is left unmasked.
     *
     * @param row int Row to leave unmasked
     */
    public void maskAllButOne(int row) {
        full2partial[row] = 0;
        partial2full[0] = row;
        mask.clear();
        mask.flip(0, n);
        mask.set(row, false);
        Adiag[0] = Adiagfull[row];
        //L is empty.
        ncurr = 1;
    }


    /**
     * Solve A*X = B
     *
     * @param B A Matrix with as many rows as A and any number of
     *          columns.
     * @return MX so that M*L*L'*M*X = M*B where M is the identity
     *         with zeros for masked entries.
     * @throws IllegalArgumentException Matrix row dimensions
     *                                  must agree.
     * @throws RuntimeException         Matrix is not symmetric positive
     *                                  definite.
     */

    public Matrix solve(Matrix B) {
        if (B.getRowDimension() != n) {
            throw new IllegalArgumentException("Matrix row dimensions must agree.");
        }
        if (!isspd) {
            throw new RuntimeException("Matrix is not symmetric positive definite.");
        }
        if (ncurr < 1)
            throw new RuntimeException("Cholesky: cannot solve equation with all rows masked");

        //        B.print(10,4);

        // Copy right hand side.
        double[][] Xfull = B.getArrayCopy();
        int nx = B.getColumnDimension();

        //                Set up the partial RHS
        double[][] X = new double[ncurr][nx];

        for (int i = 0; i < ncurr; i++) {
            int fullrow = partial2full[i];
            System.arraycopy(Xfull[fullrow], 0, X[i], 0, nx);
        }

        // Solve the partial system

        //        Solve L*Y = X, overwriting X with Y.
        // X_i <- (X_i - \sum_{j=0}^{i-1} L_ij X_j)
        for (int i = 0; i < ncurr; i++) {

            for (int j = 0; j < i; j++) {
                for (int col = 0; col < nx; col++) {
                    X[i][col] -= L[i][j] * X[j][col];
                }
            }
            for (int col = 0; col < nx; col++) {
                X[i][col] /= L[i][i];
            }
        }

        // Solve L'*X = Y for X. Overwrite X
        for (int i = ncurr - 1; i >= 0; i--) {

            for (int j = i + 1; j < ncurr; j++) {
                for (int col = 0; col < nx; col++) {
                    X[i][col] -= X[j][col] * L[j][i];
                }
            }
            for (int col = 0; col < nx; col++) {
                X[i][col] /= L[i][i];
            }
        }

        //Zero entries in the full matrix
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < nx; j++) {
                Xfull[i][j] = 0.0;
            }
        }
        //Copy back into the full matrix
        for (int i = 0; i < ncurr; i++) {
            int fullrow = partial2full[i];
            System.arraycopy(X[i], 0, Xfull[fullrow], 0, nx);
        }
        //Matrix Xmat = new Matrix(X,ncurr,nx);
        //System.out.println("Printing partial solution");
        //Xmat.print(4 , 4);

        return new Matrix(Xfull, n, nx);
    }


    /**
     * Check if a row is masked
     *
     * @param k index.
     * @return void
     * @throws IllegalArgumentException if k is not between 0
     *                                  and n-1
     */
    public boolean getmask(int k) {
        if ((k < 0) || (k >= n)) {
            throw new IllegalArgumentException("Index of mask invalid");
        }
        return mask.get(k);
    }


    /**
     * Debugging code.
     */
    public void CheckCholesky() {
        /* First check that LFull multiplied by LFull transpose
                  * gives back the complete matrix.
                  */
        if (n != 5)
            return;

        System.out.println("Lfull is\n");

        Matrix temp = new Matrix(Lfull, n, n);
        Matrix Lmat = temp.copy();

        Lmat.print(4, 4);
        /* zero the upper triangle of L */
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                Lmat.set(i, j, 0.0);
        /* evaluate the full matrix */
        System.out.println("Cholesky factor \n");
        Lmat.print(4, 4);

        Matrix Amat = temp.copy();
        for (int i = 0; i < n; i++)
            for (int j = 0; j < i; j++)
                Amat.set(i, j, Amat.get(j, i));

        for (int i = 0; i < n; i++) {
            Amat.set(i, i, Adiagfull[i]);
        }

        System.out.println("Matrix argument stored is\n");
        Amat.print(4, 4);
        Matrix Afull = Amat.copy();

        System.out.println("Cholesky factor is \n");
        Lmat.print(4, 4);
        System.out.println("Difference between A and LL' \n");

        /* Check to see if the Cholesky decomposition is working */
        (Amat.minus(Lmat.times(Lmat.transpose()))).print(5, 5);

        /* Check to see if solve is working correctly - should
               * return the identity matrix
               */

        Matrix temp2 = this.solve(Amat);


        System.out.println("This next one should be the identity matrix\n");

        temp2.print(4, 4);

        /* Testing downdating - remove row 2 */
        maskRow(2);
        System.out.println("Removed row 2\n  L is now \n");

        temp = new Matrix(L, n, n);
        Lmat = temp.copy();

        Lmat.print(4, 4);
        /* zero the upper triangle of L */
        for (int i = 0; i < ncurr; i++)
            for (int j = i + 1; j < ncurr; j++)
                Lmat.set(i, j, 0.0);
        /* evaluate the full matrix */
        System.out.println("Cholesky factor \n");
        Lmat.print(4, 4);

        Amat = temp.copy();
        for (int i = 0; i < ncurr; i++)
            for (int j = 0; j < i; j++)
                Amat.set(i, j, Amat.get(j, i));

        for (int i = 0; i < ncurr; i++) {
            Amat.set(i, i, Adiag[i]);
        }

        System.out.println("Matrix argument stored is\n");
        Amat.print(4, 4);


        System.out.println("Difference between A and LL' \n");

        /* Check to see if the Cholesky decomposition is working */
        (Amat.minus(Lmat.times(Lmat.transpose()))).print(5, 5);

        /* Check to see if solve is working correctly - should
               * return the identity matrix

               */
        Amat = Afull.copy();
        for (int i = 0; i < n; i++) {
            Amat.set(i, 2, 0.0);
            Amat.set(2, i, 0.0);
        }

        temp2 = this.solve(Amat);
        System.out.println("This next one should be the identity matrix with row/col 2 zerod\n");

        temp2.print(4, 4);

        /* Testing updating - replacing row 2 */
        unmaskRow(2);
        System.out.println("Added row 2 to the end \n  L is now \n");

        temp = new Matrix(L, n, n);
        Lmat = temp.copy();

        Lmat.print(4, 4);
        /* zero the upper triangle of L */
        for (int i = 0; i < ncurr; i++)
            for (int j = i + 1; j < ncurr; j++)
                Lmat.set(i, j, 0.0);
        /* evaluate the full matrix */
        System.out.println("Cholesky factor \n");
        Lmat.print(4, 4);

        Amat = temp.copy();
        for (int i = 0; i < ncurr; i++)
            for (int j = 0; j < i; j++)
                Amat.set(i, j, Amat.get(j, i));

        for (int i = 0; i < ncurr; i++) {
            Amat.set(i, i, Adiag[i]);
        }

        System.out.println("Matrix argument stored is\n");
        Amat.print(4, 4);


        System.out.println("Difference between A and LL' \n");

        /* Check to see if the Cholesky decomposition is working */
        (Amat.minus(Lmat.times(Lmat.transpose()))).print(5, 5);

        /* Check to see if solve is working correctly - should
               * return the identity matrix
               */
        temp2 = this.solve(Afull);
        System.out.println("This next one should be the identity matrix\n");

        temp2.print(4, 4);


    }
}


