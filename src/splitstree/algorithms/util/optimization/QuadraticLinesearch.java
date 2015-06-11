/**
 * QuadraticLinesearch.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.algorithms.util.optimization;

/**
 * Line search with quadratic backtracking.
 */
public class QuadraticLinesearch extends LineSearch {

    private double ls_param1, ls_param2;
    private double mu;

    public QuadraticLinesearch(double ls_param1, double ls_param2, double mu) {
        this.ls_param1=ls_param1;
        this.ls_param2=ls_param2;
        this.mu = mu;
    }

    /**
     * Find the median of three numbers a,b,c.
     * @param a  double
     * @param b  double
     * @param c  double
     * @return    median of a,b,c.
     */
    private double median(double a, double b, double c) {
        double larger = Math.max(a,b);
        double smaller = Math.min(a,b);
        if (c<=smaller)
            return smaller;
        else if (c<=larger)
            return c;
        else
            return larger;
    }

    private double dotprod(double[] a, double[] b) {
        double atb = 0.0;
        for(int i=1;i<a.length;i++)
            atb += a[i] * b[i];
        return atb;
    }


    double linesearch(double[] x,
                      double[] p,
                      double[] grad0,
                      double f0,
                      double alpha_init,
                      AbstractFunction f,
                      double[] xnew,
                      double[] phi_p_alpha
    ) throws NumericalException {
        //  Use quadratic backtracking line search to find approximate solution to
        //    min_{alpha>0} phi(alpha),
        //  where phi(alpha) = f(x + alpha*p).

        int n = x.length-1;
        if (p.length!=n || grad0.length != n|| xnew.length!=n || phi_p_alpha.length!=n)
            throw new IllegalArgumentException("Input vectors of differing lengths");

        double phi_p_0 = dotprod(p,grad0);
        double alpha = alpha_init;

        //xnew = max(x+alpha p,0)
        for(int i=1;i<=n;i++)
            xnew[i] = Math.max(x[i]+alpha*p[i],0.0);

        double phi_alpha = f.get_val_and_grad(xnew, phi_p_alpha);     //[phi_alpha,cost_params,phi_p_alpha] = feval(q_fn,x_new,cost_params);

        boolean ls_flag = false;
        int ls_iter = 0;
        int max_iter = 40;

        //Line search iteration
        while (!ls_flag) {
            ls_iter++;

            //Check sufficient decrease condition
            double val = f0;
            if (mu!=0.0) {
                double normDiffsquared = 0.0;
                for(int i=1;i<=n;i++)  {
                    double diff_i = xnew[i] - x[i];
                    normDiffsquared += diff_i * diff_i;
                }
                val -= (mu/alpha)*normDiffsquared;
            }
            if (phi_alpha<=val)
                return phi_alpha;


            //Minimize the quadratic  which interpolates phi_0, phi_p_0 and phi_alpha.
            //Compute new alpha
            double denom = phi_alpha - alpha*phi_p_0 - f0;
            if (denom>0) {
                double alpha_new = -0.5*phi_p_0*(alpha*alpha/denom);
                alpha = median(ls_param1*alpha,alpha_new,ls_param2*alpha);
            } else {
                throw new NumericalException("Nonpositive denominator in linesearch");
            }

            //xnew = max(x+alpha p,0)
            for(int i=1;i<=n;i++)
                xnew[i] = Math.max(x[i]+alpha*p[i],0.0);


            phi_alpha = f.get_val_and_grad(xnew,phi_p_alpha);


            if (ls_iter>max_iter) {
                throw new NumericalException("Linesearch error: max lin search iterations exceeded");
            }

        }
        return phi_alpha;
    }
}
