/**
 * NeighborNet.java
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
 * <p/>
 * Implements neighbor net
 *
 * @version $Id:
 * @author David Bryant
 * Adapted to Java by Daniel Huson and David Bryant 1.03
 */
/**
 * Implements neighbor net
 * @version $Id:
 *
 * @author David Bryant
 * Adapted to Java by Daniel Huson and David Bryant 1.03
 *
 */
package splitstree4.algorithms.distances;

import jloda.util.CanceledException;
import splitstree4.algorithms.util.NeighborNetSplitWeightOptimizer;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Implements Neighbor Net method of Bryant and Moulton (2004).
 */
public class NeighborNet implements Distances2Splits {
    private double optionThreshold = 0.000001; // min weight of split that we consider
    private double optionLambdaFrac = 1.0; 
    private boolean makeSplits = true;
    private String optionVarianceName = "Ordinary_Least_Squares";
    private String optionRegularization = "NNLS";
    //private boolean optionConstrain = true;
    private int[] cycle = null; // the computed cycle
    public final static String DESCRIPTION = "Computes the Neighbor-Net network (Bryant and Moulton 2004)";


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

    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param dist the input distances
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances dist) throws CanceledException {
        if (doc != null) {
            doc.notifyTasks("Neighbor-Net", null);
            doc.notifySetMaximumProgress(-1);    //initialize maximum progress
        }

        cycle = runNeighborNet(doc, dist);

        String var = selectVariance(this.optionVarianceName);
        if (doc != null)
            doc.notifySubtask("edge weights");
<<<<<<< HEAD
        String reg = selectRegularization(this.optionRegularization);
        
        CircularSplitWeights.Options options = new CircularSplitWeights.Options(var,optionThreshold,reg);
        Splits splits = CircularSplitWeights.getWeightedSplits(ordering, dist, options);
=======

        NeighborNetSplitWeightOptimizer.Options options = new NeighborNetSplitWeightOptimizer.Options(var, optionThreshold);
        Splits splits = NeighborNetSplitWeightOptimizer.computeWeightedSplits(cycle, dist, options);
>>>>>>> 3c03da38cb56bfa1af111c7dca6270e6e78c9bb9

        if (SplitsUtilities.isCompatible(splits))
            splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
        else
            splits.getProperties().setCompatibility(Splits.Properties.CYCLIC);

        SplitsUtilities.computeFits(true, splits, dist, doc);
        splits.getProperties().setLeastSquares(true);

        return splits;

    }

    /**
     * A scaled down version of NeighborNet that only returns the cycle, and does not
     * access the document or progress bar.
     *
     * @param dist Distance matrix
     */
    static public int[] computeNeighborNetOrdering(Distances dist) {
        int ntax = dist.getNtax();
        int[] ordering;
        if (ntax < 4) {
            ordering = new int[ntax + 1];
            for (int i = 1; i <= ntax; i++)
                ordering[i] = i;
        } else {
            try {
                ordering = runNeighborNet(null, dist);
            } catch (CanceledException e) {
                ordering = null;
            }
        }
        return ordering;
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
     * gets a cyclic cycle computed by the algorithm
     *
     * @return a cyclic cycle
     */
    public int[] getCycle() {
        return cycle;
    }

    
    /** Sets the method used for regularization
     * 
     * @param regName
     */
    public void setOptionRegularization(String regName) {
    	this.optionRegularization = regName;
    }
    
    public String getOptionRegularization() {
    	return optionRegularization;
    }
    
    public List selectionOptionRegularization(Document doc) {
    	List models = new LinkedList();
    	models.add("NNLS");
    	models.add("FullLasso");
    	models.add("WeightedLasso");
    	models.add("InternalLasso");
    	return models;
   	}
    
    public String selectRegularization(String regName) {
    	if (regName.equalsIgnoreCase("NNLS"))
    		return "nnls";
    	else if (regName.equalsIgnoreCase("FullLasso"))
    		return "lasso";
    	else if (regName.equalsIgnoreCase("WeightedLasso"))
    		return "normlasso";
    	else if (regName.equalsIgnoreCase("InternalLasso"))
    		return "internallasso";
    	else
    		return "nnls";
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

    public List<String> selectionOptionVariance(Document doc) {
        List<String> models = new ArrayList<>();
        models.add("OrdinaryLeastSquares");
        models.add("FitchMargoliash1");
        models.add("FitchMargoliash2");
        models.add("Estimated");
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

    /**
     * Sets the constrained option for least squares
     *
     * @param flag set the constrained option?
     */
//    public void setConstrain(boolean flag) {
//        this.optionConstrain = flag;
//    }

    /**
     * Gets the constrained option for least squares
     *
     * @return true, if will use the constrained least squares
     */
//    public boolean getConstrain() {
//        return optionConstrain;
//    }

    /*
    public  double getOptionThreshold() {
        return optionThreshold;
    }

    public  void setOptionThreshold(double optionThreshold) {
        this.optionThreshold = optionThreshold;
    }
    */

    public double getOptionLambdaFrac() {
    	return optionLambdaFrac;
    }
    
    public void setOptionLambdaFrac(double optionLambdaFrac) {
    	this.optionLambdaFrac = optionLambdaFrac;
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

        //Special cases. When ntax<=3, the default circular cycle will work.
        if (ntax <= 3) // nnet can't do small data sets, so let's use split decomp
        {
            int[] ordering = new int[ntax + 1];
            for (int i = 0; i <= ntax; i++)
                ordering[i] = i;
            return ordering;

        }

        double[][] D = setupMatrix(dist);

        /*
        if (D != null) {
            System.err.println("Matrix: ");
            for (int i = 0; i < D.length; i++) {
                for (int j = 0; j < D.length; j++)
                    System.err.print(" " + D[i][j]);
                System.err.println();
            }
        }
        */

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
        // System.err.println("Ordering: "+ Basic.toString(cycle));

        return expandNodes(doc, num_nodes, ntax, amalgs, netNodes);
    }

    /**
     * Agglomerates the nodes
     */
    static private int agglomNodes(Document doc, Stack amalgs, double D[][], NetNode netNodes, int num_nodes) throws CanceledException {
        //System.err.println("agglomNodes");

        NetNode p, q, Cx, Cy, x, y;
        double Qpq=0.0, best;
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
     * Expands the net nodes to obtain the cycle, quickly
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
          circular cycle being built up */
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

/* extract the cycle */
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

/* A node in the net */

class NetNode {
    int id = 0;
    NetNode nbr = null; // adjacent node
    NetNode ch1 = null; // first child
    NetNode ch2 = null; // second child
    NetNode next = null; // next in list of active nodes
    NetNode prev = null; // prev in list of active nodes
    double Rx = 0;
    double Sx = 0;

    public String toString() {
        String str = "[id=" + id;
        str += " nbr=" + (nbr == null ? "null" : ("" + nbr.id));
        str += " ch1=" + (ch1 == null ? "null" : ("" + ch1.id));
        str += " ch2=" + (ch2 == null ? "null" : ("" + ch2.id));
        str += " prev=" + (prev == null ? "null" : ("" + prev.id));
        str += " next=" + (next == null ? "null" : ("" + next.id));
        str += " Rx=" + Rx;
        str += " Sx=" + Sx;
        str += "]";
        return str;
    }
}

// EOF
