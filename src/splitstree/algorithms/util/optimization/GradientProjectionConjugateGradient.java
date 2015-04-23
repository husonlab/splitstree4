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

import Jama.Matrix;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: 2/05/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */


    //TODO: Simple test problem.

public class GradientProjectionConjugateGradient {

    private LineSearch linesrch_gp;       //Method for gradient projection line searches
    private LineSearch linesrch_cg;       //Method for conjugate gradient  line searches
    private double grad_tol;              //Relative projected gradient norm stopping tolerance
    private double gp_tol;
    private int max_gp;
    private double cg_tol;
    private int max_cg;
    private int max_iter;
    private double step_tol;

    public GradientProjectionConjugateGradient(LineSearch linesrch_gp,
                                               LineSearch linesrch_cg,
                                               double grad_tol,
                                               double gp_tol) {
        this.linesrch_cg=linesrch_cg;
        this.linesrch_gp=linesrch_gp;
        this.grad_tol = grad_tol;
        this.gp_tol = gp_tol;

    }

    private double dotprod(double[] a, double[] b) {
          double atb = 0.0;
          for(int i=1;i<a.length;i++)
              atb += a[i] * b[i];
          return atb;
    }

    void gpcg(double[] x, AbstractFunction f, double[] x_c) throws NumericalException {

        //Memory allocation
        int n = x.length-1;
        if (x_c == null || x_c.length!=n+1)
            throw new IllegalArgumentException("Vector x_c isn't the correct length");

        double g_c[] = new double[n+1];
        double p_c[] = new double[n+1];
        double pg_c[] = new double[n+1];
        double x_outer[] = new double[n+1];
        double d_c[] = new double[n+1];
        double x_new[] = new double[n+1];
        double hd[] = new double[n+1];
        double g_new[] = new double[n+1];

        double delx[] = new double[n+1];
        double resid[] = new double[n+1];
        double hp[] = new double[n+1];


        boolean[] active = new boolean[n+1];
        boolean active_new[] = new boolean[n+1];




        //x_c = max(x_c,0); Active = (x_c == 0)
        for(int i=1;i<=n;i++) {
            double xi = x[i];
            x_c[i] = Math.max(xi,0.0);
            active[i] = (xi <= 0.0);
        }

        double j_c = f.get_val_and_grad(x_c,g_c);


        //pg_c = g_c.*((1 - Active) + Active.*(g_c < 0));
        for(int i=1;i<=n;i++) {
            double g_c_i = g_c[i];
            if (active[i])
                pg_c[i] = Math.min(g_c_i,0.0);
            else
                pg_c[i] = g_c_i;
        }

        double pgradnorm0 = Math.sqrt(dotprod(pg_c, pg_c));
        double pgrad_tol = pgradnorm0 * grad_tol;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%  Outer iteration.                                                        %
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        int iter = 0;
        boolean outer_flag = false;



        while(!outer_flag) {
            iter++;

            System.arraycopy(x_c,1,x_outer,1,n);

            //%---------------------------------------------------------------------------
            //%  Gradient projection iteration.
            //%---------------------------------------------------------------------------
            boolean gp_flag = false;
            int gp_iter = 0;
            double j_diff_max = 0;

            while(!gp_flag) {
                gp_iter++;

                for(int i=1;i<=n;i++) {         //d_c = -g_c.*((1 - Active) + Active.*(g_c < 0));
                    if (!active[i])
                        d_c[i] = -g_c[i];
                    else
                        d_c[i] = Math.max(-g_c[i],0.0);
                }


                f.get_Hv(x_c,d_c,hd);

                // init_step_param = -g_c(:)'*d_c(:) / (d_c(:)'*Hd(:));
                double init_step_param = - dotprod(g_c, d_c) / dotprod(d_c,hd);

                double j_new = linesrch_gp.linesearch(x_c,d_c,g_c,j_c,init_step_param,f,x_new,g_new);
                System.arraycopy(g_new,1,g_c,1,n);


                boolean same_active = true;
                for(int i=1;i<=n;i++) {
                    active_new[i] = (x_new[i]<=0.0);
                    same_active = same_active && (active[i] == active_new[i]);
                }
                double j_diff = j_c - j_new;
                j_diff_max = Math.max(j_diff,j_diff_max);

                System.arraycopy(active_new,1,active,1,n);
                System.arraycopy(x_new,1,x_c,1,n);
                j_c = j_new;

                if ((j_diff < gp_tol*j_diff_max)||same_active||gp_iter>=max_gp)
                    gp_flag = true;

            }

            //%---------------------------------------------------------------------------
            //%  Subspace Minimization
            //%  Use CG to approximately compute an unconstrained minimizer of
            //%    q(delx) = 0.5*delx'*H*delx + delx'*g
            //%  where H = projected Hessian, g = projected gradient.
            //%---------------------------------------------------------------------------

            //Initialization
            //double q = j_c;



            for(int i=1;i<=n;i++) {
                if (!active[i])
                    resid[i] = -g_c[i];
                else
                    resid[i] = 0.0;
            }

            //TODO preconditioning

            //CG_iterations

            double q_diff_max = 0.0;
            int cgiter = 0;
            boolean cg_flag = false;

            double rdlast=0; //Set in first iteration, but not used until the second.

            while(!cg_flag) {
                cgiter++;

                System.arraycopy(resid,1,d_c,1,n);
                double rd = dotprod(resid,resid);        //This is only valid if not pre-conditioning

                //Compute conjugate direction p_c
                if (cgiter==1)
                    System.arraycopy(d_c,1,p_c,1,n);
                else {
                    double betak = rd/rdlast;
                    for(int i=1;i<=n;i++)
                        p_c[i] = d_c[i]+betak*p_c[i];
                }

                // %  Compute product of reduced Hessian and p_c.
                f.get_Hv(x_c,p_c,hp);
                for(int i=1;i<=n;i++)
                    if (active[i])
                        hp[i] = 0.0;

                //%  Update delx and residual.

                //delx = delx + alphak*p_c;
                //resid = resid - alphak*Hp;
                //rdlast = rd;
                double alphak = rd/dotprod(hp,p_c);   //alphak = rd / (p_c(:)'*Hp(:));
                for(int i=1;i<=n;i++){
                    delx[i] += alphak*p_c[i];
                    resid[i] -= alphak*hp[i];
                }
                rdlast = rd;

                //  q := q(x_old + delx);

                double tmp = 0.0;
                for(int i=1;i<=n;i++) {
                    tmp+=p_c[i] * (alphak/2*hp[i] - resid[i]);
                }
                double q_diff = alphak * tmp;

                //  Check for sufficient decrease in quadratic or max iter exceeded.

                q_diff_max = Math.max(q_diff,q_diff_max);
                if ((q_diff<= cg_tol * q_diff_max)||(cgiter == max_cg))
                    cg_flag = true;

            }

            double init_step_param = 1.0;
            j_c = linesrch_cg.linesearch(x_c,delx,g_c,j_c,init_step_param,f,x_new, g_new);
            System.arraycopy(x_new,1,x_c,1,n);
            System.arraycopy(g_new,1,g_c,1,n);

            double stepnorm = 0.0;
            for(int i=1;i<=n;i++) {
                double diff_i = x_c[i] - x_outer[i];
                stepnorm += diff_i * diff_i;
            }
            stepnorm = Math.sqrt(stepnorm);
            for(int i=1;i<=n;i++)
                active[i] = (x_c[i]<=0.0);

            Matrix pg = new Matrix(n,1);
            for(int i=1;i<=n;i++) {
                if (active[i])
                    pg.set(i,1,Math.min(g_c[i],0.0));
                else
                    pg.set(i,1,g_c[i]);
            }
            double pgradnorm = pg.norm2();
            double xc_norm = Math.sqrt(dotprod(x_c,x_c));
            if (iter>=max_iter || stepnorm < step_tol*xc_norm || pgradnorm < pgrad_tol)
                outer_flag = true;
        }
    }


}
