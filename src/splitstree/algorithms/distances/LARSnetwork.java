/**
 * LARSnetwork.java 
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
/**
 * Implements neighbor net
 * @version $Id:
 *
 * @author David Bryant
 * Adapted to Java by Daniel Huson and David Bryant 1.03
 *
 */
package splitstree.algorithms.distances;

import Jama.Matrix;
import jloda.util.CanceledException;
import splitstree.algorithms.util.LeastAngleRegression;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.SplitsUtilities;
import splitstree.util.TreesUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// EOF

public class LARSnetwork /*implements Distances2Splits*/ {
    private double optionThreshold = 0.0; // min weight of split that we consider

    private String optionNetworkMethod = "NeighborNet";

    public FileWriter output = null;


    private String optionVarianceName = "OrdinaryLeastSquares";
    private boolean optionForceTrivial = false;
    private boolean optionUseAllCircularSplits = false;
    private boolean optionLeastAngleRegression = true;
    private String optionNormalisation = "EuclideanSquared";
    private String optionOrdering = "";
    private double optionVar = 1.0;
    private String optionSplitSelection = "AIC1";
    private int optionPercent;

    // private int optionIterate = 10;

    private int[] ordering = null; // the computed ordering
    public final static String DESCRIPTION = "Experimental versions of NeighborNet";


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param dist the input distance object
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances dist) {
        return doc.isValid(taxa) && doc.isValid(dist);
    }

    public Splits apply(Document doc, Taxa taxa, Distances dist_orig) throws CanceledException {
        return apply(doc, taxa, dist_orig, dist_orig, dist_orig);
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances dist_orig, Distances dist_lambda, Distances dist_mse) throws CanceledException {
        if (doc != null) {
            doc.notifyTasks("LARS network", null);
            doc.notifySetMaximumProgress(-1);    //initialize maximum progress
        }


        ordering = new int[taxa.getNtax() + 1];
        Splits splits;
        String var = selectVariance(this.optionVarianceName);


        if (getOptionNetworkMethod().equalsIgnoreCase("NeighborNet")) {
            NeighborNet nnet = new NeighborNet();
            nnet.setOptionVariance(this.optionVarianceName);
            splits = nnet.apply(doc, taxa, dist_orig);
            if (splits.getCycle() == null)
                ordering = SplitsUtilities.computeCycle(splits);
            if (optionUseAllCircularSplits)
                splits = getAllSplits(taxa.getNtax(), ordering, 1.0);
        } else if (getOptionNetworkMethod().equalsIgnoreCase("NeighborJoining")) {
            NJ nj = new NJ();
            Trees njTree = nj.apply(doc, taxa, dist_orig);
            splits = TreesUtilities.convertTreeToSplits(njTree, 1, taxa);
            if (splits.getCycle() == null)
                ordering = SplitsUtilities.computeCycle(splits);
            if (optionUseAllCircularSplits)
                splits = getAllSplits(taxa.getNtax(), ordering, 1.0);
        } else {
            StringTokenizer st = new StringTokenizer(optionOrdering);
            for (int i = 1; i <= taxa.getNtax(); i++) {
                ordering[i] = Integer.parseInt(st.nextToken());
            }
            splits = getAllSplits(taxa.getNtax(), ordering, 1.0);
        }

        //}


        if (optionLeastAngleRegression) {

            Matrix sqrtW = getVarianceFactor(dist_orig, var, ordering);
            Matrix X = getDesignMatrix(splits, sqrtW);
            Matrix y = getDataVector(dist_orig, sqrtW);
            Matrix yLambda = getDataVector(dist_orig, sqrtW);
            Matrix yMSE = getDataVector(dist_orig, sqrtW);

            BitSet trivialSplits;
            // if (getOptionForceTrivial()) {
            trivialSplits = new BitSet();
            for (int s = 1; s <= splits.getNsplits(); s++) {
                int n = splits.get(s).cardinality();
                if (n == 1 || n == taxa.getNtax() - 1)
                    trivialSplits.set(s - 1);
            }


            String normalisation = getOptionNormalisation();
            int sval;
            if (getOptionSplitSelection().equalsIgnoreCase("AIC1"))
                sval = 1;
            else if (getOptionSplitSelection().equalsIgnoreCase("AIC2"))
                sval = 2;
            else if (getOptionSplitSelection().equalsIgnoreCase("AIC3"))
                sval = 3;
            else if (getOptionSplitSelection().equalsIgnoreCase("Full"))
                sval = 4;
            else
                sval = 5;


            LeastAngleRegression lars = new LeastAngleRegression(X, y, yLambda, normalisation, trivialSplits, true, getOptionVar());

            lars.splitSelection = sval;
            lars.percent = getOptionPercent();

            lars.apply();

            if (output != null) {
                try {
                    output.write("" + lars.aicVar1 + "\t" + lars.nsplits1 + "\t" + lars.ntriv1 + "\t" + lars.res1 + "\t" + lars.aic1 + "\t");
                    output.write("" + lars.aicVar2 + "\t" + lars.nsplits2 + "\t" + lars.ntriv2 + "\t" + lars.res2 + "\t" + lars.aic2 + "\t");
                    output.write("" + lars.aicVar3 + "\t" + lars.nsplits3 + "\t" + lars.ntriv3 + "\t" + lars.res3 + "\t" + lars.aic3 + "\t");
                    output.write("" + lars.nsplitsf + "\t" + lars.ntrivf + "\t" + lars.resf + "\t" + lars.aicf + "\t");
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }


            }


            double[] beta = lars.getBetaAIC();

            //double MSE = lars.getMSE(yMSE);
            //System.out.println("MSE of model = "+MSE);
            Splits newSplits = new Splits(taxa.getNtax());

            for (int i = 1; i <= splits.getNsplits(); i++) {
                if (beta[i - 1] > 0.0)
                    newSplits.add(splits.get(i), (float) beta[i - 1]);
                splits.setWeight(i, (float) beta[i - 1]);
            }
            splits = newSplits;

        }


        if (SplitsUtilities.isCompatible(splits))
            splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
        else
            splits.getProperties().setCompatibility(Splits.Properties.CYCLIC);
        return splits;

    }

    private Matrix getVarianceFactor(Distances dist, String var, int[] ordering) {
        int ntax = dist.getNtax();
        int npairs = ((ntax - 1) * ntax) / 2;
        Matrix sqrtW = new Matrix(npairs, 1);

        int index = 0;
        for (int i = 0; i < ntax; i++)
            for (int j = i + 1; j < ntax; j++) {
                double dij = dist.get(ordering[i + 1], ordering[j + 1]);
                double vij;
                if (var.equalsIgnoreCase("ols"))
                    vij = 1.0;
                else if (var.equalsIgnoreCase("fm1"))
                    vij = dij;
                else if (var.equalsIgnoreCase("fm2"))
                    vij = dij * dij;
                else
                    vij = dist.getVar(ordering[i + 1], ordering[j + 1]);
                if (vij <= 10E-20)
                    sqrtW.set(index, 0, 10E10);
                else
                    sqrtW.set(index, 0, 1.0 / Math.sqrt(vij));
                index++;
            }
        return sqrtW;
    }

      /**
     * Create the set of all circular splits for a given ordering
     *
     * @param ntax     Number of taxa
     * @param ordering circular ordering
     * @param weight   weight to give each split
     * @return set of ntax*(ntax-1)/2 circular splits
     */
    static public Splits getAllSplits(int ntax, int[] ordering, double weight) {

        /* Construct the splits with the appropriate weights */
        Splits splits = new Splits(ntax);
        int index = 0;
        for (int i = 0; i < ntax; i++) {
            TaxaSet t = new TaxaSet();
            for (int j = i + 1; j < ntax; j++) {
                t.set(ordering[j + 1]);
                splits.add(t, (float) weight);
                index++;
            }
        }
        return splits;
    }


    private Matrix getDesignMatrix(Splits splits, Matrix sqrtW) {
        int ntax = splits.getNtax();
        int npairs = (ntax * (ntax - 1)) / 2;
        int nsplits = splits.getNsplits();
        Matrix X = new Matrix(npairs, nsplits);


        for (int k = 1; k <= nsplits; k++) {
            int pair = 1;
            TaxaSet A = splits.get(k);
            for (int i = 1; i <= ntax; i++) {
                for (int j = i + 1; j <= ntax; j++) {
                    if (A.get(i) != A.get(j))
                        X.set(pair - 1, k - 1, sqrtW.get(pair - 1, 0));
                    else
                        X.set(pair - 1, k - 1, 0.0);
                    pair++;
                }
            }
        }
        return X;
    }

    private Matrix getDataVector(Distances dist, Matrix sqrtW) {
        int ntax = dist.getNtax();
        int npairs = (ntax * (ntax - 1)) / 2;
        Matrix y = new Matrix(npairs, 1);

        int pair = 0;
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                y.set(pair, 0, sqrtW.get(pair, 0) * dist.get(i, j));
                pair++;
            }
        }
        return y;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * gets a cyclic ordering computed by the algorithm
     *
     * @return a cyclic ordering
     */
    public int[] getOrdering() {
        return ordering;
    }


    /**
     * Sets the power for least squares
     *
     * @param varName 0, 1 or 2
     */
    public void setOptionVariance(String varName) {
        this.optionVarianceName = varName;
    }

    /**
     * Gets the power for least squares
     *
     * @return the power
     */
    public String getOptionVariance() {
        return optionVarianceName;
    }

    public List selectionOptionVariance(Document doc) {
        List models = new LinkedList();
        models.add("OrdinaryLeastSquares");
        models.add("FitchMargoliash1");
        models.add("FitchMargoliash2");
        //models.add("Estimated");
        return models;
    }

    public String getOptionSplitSelection() {
        return optionSplitSelection;
    }

    public void setOptionSplitSelection(String optionSplitSelection) {
        this.optionSplitSelection = optionSplitSelection;
    }

    public List selectionOptionSplitSelection(Document doc) {
        List models = new LinkedList();
        models.add("AIC1");
        models.add("AIC2");
        models.add("AIC3");
        models.add("Full");
        models.add("Percentage");
        return models;
    }

    public double getOptionVar() {
        return optionVar;
    }

    public void setOptionVar(double optionVar) {
        this.optionVar = optionVar;
    }

    public boolean getOptionForceTrivial() {
        return optionForceTrivial;
    }

    public void setOptionForceTrivial(boolean optionForceTrivial) {
        this.optionForceTrivial = optionForceTrivial;
    }

    public String getOptionNetworkMethod() {
        return optionNetworkMethod;
    }

    public void setOptionNetworkMethod(String method) {
        this.optionNetworkMethod = method;
    }

    public List selectionOptionNetworkMethod(Document doc) {
        List methods = new LinkedList();
        methods.add("NeighborNet");
        methods.add("NeighborJoining");
        methods.add("Ordering");
        return methods;
    }

    public int getOptionPercent() {
        return optionPercent;
    }

    public void setOptionPercent(int optionPercent) {
        this.optionPercent = optionPercent;
    }

    public String getOptionOrdering() {
        return optionOrdering;
    }

    public void setOptionOrdering(String optionOrdering) {
        this.optionOrdering = optionOrdering;
    }

    public String getOptionNormalisation() {
        return optionNormalisation;
    }

    public void setOptionNormalisation(String optionNormalisation) {
        this.optionNormalisation = optionNormalisation;
    }

    public List selectionOptionNormalisation(Document doc) {
        List models = new LinkedList();
        models.add("Euclidean");
        models.add("EuclideanSquared");
        //models.add("FitchMargoliash1");
        //models.add("FitchMargoliash2");
        //models.add("Estimated");
        return models;
    }


    public String selectVariance(String varianceName) {
        if (varianceName.equalsIgnoreCase("OrdinaryLeastSquares"))
            return "ols";
        else if (varianceName.equalsIgnoreCase("FitchMargoliash1"))
            return "fm1";
        else if (varianceName.equalsIgnoreCase("FitchMargoliash2"))
            return "fm2";
        else if (varianceName.equalsIgnoreCase("Estimated"))
            return "user";
        else
            return "ols"; //In case of uncertainty, do OLS
    }

    public boolean getOptionUseAllCircularSplits() {
        return optionUseAllCircularSplits;
    }

    public void setOptionUseAllCircularSplits(boolean optionUseAllCircularSplits) {
        this.optionUseAllCircularSplits = optionUseAllCircularSplits;
    }

    /* public int getOptionIterate() {
       return optionIterate;
   }

   public void setOptionIterate(int optionIterate) {
       this.optionIterate = optionIterate;
   }

   /**
    * Sets the constrained option for least squares
    *
    * @param flag set the constrained option?
    */
    // public void setOptionConstrain(boolean flag) {
    //    this.optionConstrain = flag;
    //}

    /**
     * Gets the constrained option for least squares
     *
     * @return true, if will use the constrained least squares
     */
    //public boolean getOptionConstrain() {
    //    return optionConstrain;
    //}
    public double getOptionThreshold() {
        return optionThreshold;
    }

    public void setOptionThreshold(double optionThreshold) {
        this.optionThreshold = optionThreshold;
    }

    public boolean getOptionLeastAngleRegression() {
        return optionLeastAngleRegression;
    }

    public void setOptionLeastAngleRegression(boolean optionLeastAngleRegression) {
        this.optionLeastAngleRegression = optionLeastAngleRegression;
    }

    /**
     * Sets up the working matrix. The original distance matrix is enlarged to
     * handle the maximum number of nodes
     *
     * @param dist Distance block
     * @return a working matrix of appropriate cardinality
     */
    private static double[][] setupMatrix(Distances dist) {
        int ntax = dist.getNtax();
        int max_num_nodes = 3 * ntax - 5;
        double[][] D = new double[max_num_nodes][max_num_nodes];
        /* Copy the distance matrix into a larger, scratch distance matrix */
        for (int i = 1; i <= ntax; i++) {
            for (int j = 1; j <= ntax; j++)
                D[i][j] = dist.get(i, j);
            Arrays.fill(D[i], ntax + 1, max_num_nodes, 0.0);
        }
        for (int i = ntax + 1; i < max_num_nodes; i++)
            Arrays.fill(D[i], 0, max_num_nodes, 0.0);
        return D;
    }

    /**
     * Run the neighbor net algorithm
     */
    private static int[] runNeighborNet(Document doc, Distances dist) throws CanceledException {

        int ntax = dist.getNtax();

        //Special cases. When ntax<=3, the default circular ordering will work.
        if (ntax <= 3) // nnet can't do small data sets, so let's use split decomp
        {
            int[] ordering = new int[ntax + 1];
            for (int i = 0; i <= ntax; i++)
                ordering[i] = i;
            return ordering;

        }

        double[][] D = setupMatrix(dist);
        NetNode netNodes = new NetNode();

        /* Nodes are stored in a doubly linked list that we set up here */
        for (int i = ntax; i >= 1; i--) /* Initially, all singleton nodes are active */ {
            NetNode taxNode = new NetNode();
            taxNode.id = i;
            taxNode.next = netNodes.next;
            netNodes.next = taxNode;
        }

        /* Set up links in other direction */
        for (NetNode taxNode = netNodes; taxNode.next != null; taxNode = taxNode.next)
            taxNode.next.prev = taxNode;

        /* Perform the agglomeration step */
        Stack amalgs = new Stack();
        int num_nodes = ntax;
        if (doc != null)
            doc.notifySubtask("agglomeration");
        num_nodes = agglomNodes(doc, amalgs, D, netNodes, num_nodes);
        if (doc != null)
            doc.notifySubtask("expansion");
        return expandNodes(doc, num_nodes, ntax, amalgs, netNodes);
    }

    /**
     * Agglomerates the nodes
     */
    static private int agglomNodes(Document doc, Stack amalgs, double D[][], NetNode netNodes, int num_nodes) throws CanceledException {
        //System.err.println("agglomNodes");

        NetNode p, q, Cx, Cy, x, y;
        double Qpq, best;
        int num_active = num_nodes;
        int num_clusters = num_nodes;
        int m;
        double Dpq;

        while (num_active > 3) {

            /* Special case
            If we let this one go then we get a divide by zero when computing Qpq */
            if (num_active == 4 && num_clusters == 2) {
                p = netNodes.next;
                if (p.next != p.nbr)
                    q = p.next;
                else
                    q = p.next.next;
                if (D[p.id][q.id] + D[p.nbr.id][q.nbr.id] < D[p.id][q.nbr.id] + D[p.nbr.id][q.id]) {
                    agg3way(p, q, q.nbr, amalgs, D, netNodes, num_nodes);
                    num_nodes += 2;
                } else {
                    agg3way(p, q.nbr, q, amalgs, D, netNodes, num_nodes);
                    num_nodes += 2;
                }
                break;
            }

            /* Compute the "averaged" sums s_i from each cluster to every other cluster.

      To Do: 2x speedup by using symmetry*/

            for (p = netNodes.next; p != null; p = p.next)
                p.Sx = 0.0;
            for (p = netNodes.next; p != null; p = p.next) {
                if (p.nbr == null || p.nbr.id > p.id) {
                    for (q = p.next; q != null; q = q.next) {
                        if (q.nbr == null || (q.nbr.id > q.id) && (q.nbr != p)) {
                            if ((p.nbr == null) && (q.nbr == null))
                                Dpq = D[p.id][q.id];
                            else if ((p.nbr != null) && (q.nbr == null))
                                Dpq = (D[p.id][q.id] + D[p.nbr.id][q.id]) / 2.0;
                            else if ((p.nbr == null) && (q.nbr != null))
                                Dpq = (D[p.id][q.id] + D[p.id][q.nbr.id]) / 2.0;
                            else
                                Dpq = (D[p.id][q.id] + D[p.id][q.nbr.id] + D[p.nbr.id][q.id] + D[p.nbr.id][q.nbr.id]) / 4.0;

                            p.Sx += Dpq;
                            if (p.nbr != null)
                                p.nbr.Sx += Dpq;
                            q.Sx += Dpq;
                            if (q.nbr != null)
                                q.nbr.Sx += Dpq;
                        }
                    }
                    if (doc != null)
                        doc.getProgressListener().checkForCancel();
                }
            }

            Cx = Cy = null;
            /* Now minimize (m-2) D[C_i,C_k] - Sx - Sy */
            best = 0;
            for (p = netNodes.next; p != null; p = p.next) {
                if ((p.nbr != null) && (p.nbr.id < p.id)) /* We only evaluate one node per cluster */
                    continue;
                for (q = netNodes.next; q != p; q = q.next) {
                    if ((q.nbr != null) && (q.nbr.id < q.id)) /* We only evaluate one node per cluster */
                        continue;
                    if (q.nbr == p) /* We only evaluate nodes in different clusters */
                        continue;
                    if ((p.nbr == null) && (q.nbr == null))
                        Dpq = D[p.id][q.id];
                    else if ((p.nbr != null) && (q.nbr == null))
                        Dpq = (D[p.id][q.id] + D[p.nbr.id][q.id]) / 2.0;
                    else if ((p.nbr == null) && (q.nbr != null))
                        Dpq = (D[p.id][q.id] + D[p.id][q.nbr.id]) / 2.0;
                    else
                        Dpq = (D[p.id][q.id] + D[p.id][q.nbr.id] + D[p.nbr.id][q.id] + D[p.nbr.id][q.nbr.id]) / 4.0;
                    Qpq = ((double) num_clusters - 2.0) * Dpq - p.Sx - q.Sx;
                    /* Check if this is the best so far */
                    if ((Cx == null || (Qpq < best)) && (p.nbr != q)) {
                        Cx = p;
                        Cy = q;
                        best = Qpq;
                    }
                }
            }

            /* Find the node in each cluster */
            x = Cx;
            y = Cy;

            if (Cx.nbr != null || Cy.nbr != null) {
                Cx.Rx = ComputeRx(Cx, Cx, Cy, D, netNodes);
                if (Cx.nbr != null)
                    Cx.nbr.Rx = ComputeRx(Cx.nbr, Cx, Cy, D, netNodes);
                Cy.Rx = ComputeRx(Cy, Cx, Cy, D, netNodes);
                if (Cy.nbr != null)
                    Cy.nbr.Rx = ComputeRx(Cy.nbr, Cx, Cy, D, netNodes);
            }

            m = num_clusters;
            if (Cx.nbr != null)
                m++;
            if (Cy.nbr != null)
                m++;

            best = ((double) m - 2.0) * D[Cx.id][Cy.id] - Cx.Rx - Cy.Rx;
            if (Cx.nbr != null) {
                Qpq = ((double) m - 2.0) * D[Cx.nbr.id][Cy.id] - Cx.nbr.Rx - Cy.Rx;
                if (Qpq < best) {
                    x = Cx.nbr;
                    y = Cy;
                    best = Qpq;
                }
            }
            if (Cy.nbr != null) {
                Qpq = ((double) m - 2.0) * D[Cx.id][Cy.nbr.id] - Cx.Rx - Cy.nbr.Rx;
                if (Qpq < best) {
                    x = Cx;
                    y = Cy.nbr;
                    best = Qpq;
                }
            }
            if ((Cx.nbr != null) && (Cy.nbr != null)) {
                Qpq = ((double) m - 2.0) * D[Cx.nbr.id][Cy.nbr.id] - Cx.nbr.Rx - Cy.nbr.Rx;
                if (Qpq < best) {
                    x = Cx.nbr;
                    y = Cy.nbr;
                }
            }

            /* We perform an agglomeration... one of three types */
            if ((null == x.nbr) && (null == y.nbr)) {   /* Both vertices are isolated...add edge {x,y} */
                agg2way(x, y);
                num_clusters--;
            } else if (null == x.nbr) {     /* X is isolated,  Y  is not isolated*/
                agg3way(x, y, y.nbr, amalgs, D, netNodes, num_nodes);
                num_nodes += 2;
                num_active--;
                num_clusters--;
            } else if ((null == y.nbr) || (num_active == 4)) { /* Y is isolated,  X is not isolated
                                                        OR theres only four active nodes and none are isolated */
                agg3way(y, x, x.nbr, amalgs, D, netNodes, num_nodes);
                num_nodes += 2;
                num_active--;
                num_clusters--;
            } else {  /* Both nodes are connected to others and there are more than 4 active nodes */
                num_nodes = agg4way(x.nbr, x, y, y.nbr, amalgs, D, netNodes, num_nodes);
                num_active -= 2;
                num_clusters--;
            }
        }
        return num_nodes;
    }

    /**
     * agglomerate 2 nodes
     *
     * @param x one node
     * @param y other node
     */
    static private void agg2way(NetNode x, NetNode y) {
        x.nbr = y;
        y.nbr = x;
    }

    /**
     * agglomerate 3 nodes.
     * Note that this version doesn't update num_nodes, you need to
     * num_nodes+=2 after calling this!
     *
     * @param x one node
     * @param y other node
     * @param z other node
     * @return one of the new nodes
     */
    static private NetNode agg3way(NetNode x, NetNode y, NetNode z,
                                   Stack amalgs, double[][] D, NetNode netNodes, int num_nodes) {
/* Agglomerate x,y, and z to give TWO new nodes, u and v */
/* In terms of the linked list: we replace x and z
  	 by u and v and remove y from the linked list.
  	 and replace y with the new node z
    Returns a pointer to the node u */
//printf("Three way: %d, %d, and %d\n",x.id,y.id,z.id);

        NetNode u = new NetNode();
        u.id = num_nodes + 1;
        u.ch1 = x;
        u.ch2 = y;

        NetNode v = new NetNode();
        v.id = num_nodes + 2;
        v.ch1 = y;
        v.ch2 = z;

/* Replace x by u in the linked list */
        u.next = x.next;
        u.prev = x.prev;
        if (u.next != null)
            u.next.prev = u;
        if (u.prev != null)
            u.prev.next = u;

/* Replace z by v in the linked list */
        v.next = z.next;
        v.prev = z.prev;
        if (v.next != null)
            v.next.prev = v;
        if (v.prev != null)
            v.prev.next = v;

/* Remove y from the linked list */
        if (y.next != null)
            y.next.prev = y.prev;
        if (y.prev != null)
            y.prev.next = y.next;

/* Add an edge between u and v, and add u into the list of amalgamations */
        u.nbr = v;
        v.nbr = u;

/* Update distance matrix */

        for (NetNode p = netNodes.next; p != null; p = p.next) {
            D[u.id][p.id] = D[p.id][u.id] = (2.0 / 3.0) * D[x.id][p.id] + D[y.id][p.id] / 3.0;
            D[v.id][p.id] = D[p.id][v.id] = (2.0 / 3.0) * D[z.id][p.id] + D[y.id][p.id] / 3.0;
        }
        D[u.id][u.id] = D[v.id][v.id] = 0.0;

        amalgs.push(u);

        return u;
    }

    /**
     * Agglomerate four nodes
     *
     * @param x2 a node
     * @param x  a node
     * @param y  a node
     * @param y2 a node
     * @return the new number of nodes
     */
    static private int agg4way(NetNode x2, NetNode x, NetNode y, NetNode y2,
                               Stack amalgs, double[][] D, NetNode netNodes, int num_nodes) {
/* Replace x2,x,y,y2 by with two vertices... performed using two
  	 3 way amalgamations */

        NetNode u;

        u = agg3way(x2, x, y, amalgs, D, netNodes, num_nodes); /* Replace x2,x,y by two nodes, equal to x2_prev.next and y_prev.next. */
        num_nodes += 2;
        agg3way(u, u.nbr, y2, amalgs, D, netNodes, num_nodes); /* z = y_prev . next */
        num_nodes += 2;
        return num_nodes;
    }

    /**
     * Computes the Rx
     *
     * @param z        a node
     * @param Cx       a node
     * @param Cy       a node
     * @param D        the distances
     * @param netNodes the net nodes
     * @return the Rx value
     */
    static private double ComputeRx(NetNode z, NetNode Cx, NetNode Cy, double[][] D,
                                    NetNode netNodes) {
        double Rx = 0.0;

        for (NetNode p = netNodes.next; p != null; p = p.next) {
            if (p == Cx || p == Cx.nbr || p == Cy || p == Cy.nbr || p.nbr == null)
                Rx += D[z.id][p.id];
            else /* p.nbr != null */
                Rx += D[z.id][p.id] / 2.0; /* We take the average of the distances */
        }
        return Rx;
    }

    /**
     * Expands the net nodes to obtain the ordering, quickly
     *
     * @param num_nodes number of nodes
     * @param ntax      number of taxa
     * @param amalgs    stack of amalagations
     * @param netNodes  the net nodes
     */
    static private int[] expandNodes(Document doc, int num_nodes, int ntax, Stack amalgs, NetNode netNodes) throws CanceledException {

        int[] ordering = new int[ntax + 1];
        //System.err.println("expandNodes");
        NetNode x, y, z, u, v, a;

/* Set up the circular order for the first three nodes */
        x = netNodes.next;
        y = x.next;
        z = y.next;
        z.next = x;
        x.prev = z;

/* Now do the rest of the expansions */
        while (!amalgs.empty()) {
/* Find the three elements replacing u and v. Swap u and v around if v comes before u in the
          circular ordering being built up */
            u = (NetNode) (amalgs.pop());
            // System.err.println("POP: u="+u);
            v = u.nbr;
            x = u.ch1;
            y = u.ch2;
            z = v.ch2;
            if (v != u.next) {
                NetNode tmp = u;
                u = v;
                v = tmp;
                tmp = x;
                x = z;
                z = tmp;
            }

/* Insert x,y,z into the circular order */
            x.prev = u.prev;
            x.prev.next = x;
            x.next = y;
            y.prev = x;
            y.next = z;
            z.prev = y;
            z.next = v.next;
            z.next.prev = z;
            if (doc != null)
                doc.getProgressListener().checkForCancel();
        }

/* When we exit, we know that the point x points to a node in the circular order */
/* We loop through until we find the node after taxa zero */
        while (x.id != 1) {
            x = x.next;
        }

/* extract the ordering */
        a = x;
        int t = 0;
        do {
            // System.err.println("a="+a);
            ordering[++t] = a.id;
            a = a.next;
        } while (a != x);
        return ordering;
    }


}
