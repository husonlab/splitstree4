/**
 * NeighborNetReboot.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant, with Vince Moulton and Kathi Huber
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
 * @author David Bryant and Daniel Huson
 * Adapted to Java by Daniel Huson and David Bryant 1.03
 * <p>
 * Implements neighbor net reboot
 * @version $Id:
 * @author David Bryant
 */
/**
 * Implements neighbor net reboot
 * @version $Id:
 *
 * @author David Bryant
 *
 */
package splitstree4.algorithms.distances;

import Jama.Matrix;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.util.Arrays;
import java.util.Stack;

/**
 * Implements Neighbor Net method of Bryant and Moulton (2004).
 */
public class NeighborNetReboot implements Distances2Splits {
    private double optionThreshold = 0.000001; // min weight of split that we consider
    private boolean makeSplits = true;
    //private boolean optionConstrain = true;
    private int[] cycle = null; // the computed cycle
    public final static String DESCRIPTION = "Computes the Neighbor-Net network (Bryant and Moulton 2004) using the revised algorithm of (Bryant, Huber and Moulton, 2019)";


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
            doc.notifyTasks("Neighbor-Net (reboot)", null);
            doc.notifySetMaximumProgress(-1);    //initialize maximum progress
        }

        long timeStart = System.nanoTime();

        Splits splits = runNeighborNet(doc, dist, optionThreshold);

        long timeElapsed = (System.nanoTime() - timeStart) / 1000000000;
        System.out.println("Time elapsed = " + timeElapsed);

        if (doc != null)
            doc.notifySubtask("edge weights");

        if (SplitsUtilities.isCompatible(splits))
            splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
        else
            splits.getProperties().setCompatibility(Splits.Properties.CYCLIC);

        SplitsUtilities.computeFits(true, splits, dist, doc);
        splits.getProperties().setLeastSquares(true);

        return splits;

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

    /**
     * Sets up the working matrix. The original distance matrix is enlarged to
     * handle the maximum number of nodes.
     *
     * TODO: Implement this as upper triangular, to save memory?
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
    private static Splits runNeighborNet(Document doc, Distances dist, double threshold) throws CanceledException {

        int ntax = dist.getNtax();
        int max_num_nodes = 3 * ntax - 5;

        //Special case. When ntax<=3, the default circular cycle will work.
        if (ntax <= 3) {
            int[] ordering = new int[ntax + 1];
            for (int i = 0; i <= ntax; i++)
                ordering[i] = i;
            //DOTHIS
            double[][] W = new double[ntax + 1][ntax + 1];
            W[1][2] = W[2][1] = 0.5 * (dist.get(1, 2) + dist.get(1, 3) - dist.get(2, 3));
            W[2][3] = W[3][2] = 0.5 * (dist.get(1, 2) + dist.get(2, 3) - dist.get(1, 3));
            W[1][3] = W[3][1] = 0.5 * (dist.get(1, 3) + dist.get(2, 3) - dist.get(1, 2));


            return getSplits(ordering, W, threshold);
        }

        double[][] D = setupMatrix(dist);


        /* Nodes are stored in a doubly linked list that we set up here */
        NetNode netNodes = new NetNode();
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
        if (doc != null)
            doc.notifySubtask("agglomeration");
        int num_nodes = agglomNodes(doc, amalgs, D, netNodes, ntax);


        /* Expansion step */
        if (doc != null)
            doc.notifySubtask("expansion");

        double[][] W = new double[max_num_nodes][max_num_nodes];  //Weights matrix
        int[] ordering = expandNodesGetWeights(doc, ntax, amalgs, netNodes, D, W);
        Splits splits = getSplits(ordering, W, threshold);

        return splits;
    }

    /**
     * Agglomerates the nodes
     */
    static private int agglomNodes(Document doc, Stack amalgs, double D[][], NetNode netNodes, int num_nodes) throws CanceledException {
        //System.err.println("agglomNodes");

        NetNode p, q, Cx, Cy, x, y;
        double Qpq = 0.0, best;
        int num_active = num_nodes;
        int num_clusters = num_nodes;
        int m;
        double Dpq;

        while (num_active > 3) {

            /* Special case If we let this one go then we get a divide by zero when computing Qpq */
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

            /* Compute the "averaged" sums s_i from each cluster to every other cluster.*/
            //TODO To Do: 2x speedup by using symmetry

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
        D[u.id][v.id] = D[v.id][u.id] = (1.0 / 3.0) * (D[x.id][y.id] + D[x.id][z.id] + D[y.id][z.id]);
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
     * @param ntax      number of taxa
     * @param amalgs    stack of amalagations
     * @param netNodes  the net nodes
     */
    static private int[] expandNodesGetWeights(Document doc, int ntax, Stack amalgs, NetNode netNodes, double[][] D, double[][] W) throws CanceledException {

        int[] ordering = new int[ntax + 1];
        int max_num_nodes = 3 * ntax - 5;

        //System.err.println("expandNodes");
        NetNode x, y, z, u, v, a;

        /* Set up the circular order for the first three nodes */
        x = netNodes.next;
        y = x.next;
        z = y.next;
        z.next = x;
        x.prev = z;

        W[x.id][y.id] = Math.max(0, 0.5 * (D[x.id][y.id] + D[x.id][z.id] - D[y.id][z.id]));
        W[y.id][x.id] = W[x.id][y.id];
        W[x.id][z.id] = Math.max(0, 0.5 * (D[x.id][z.id] + D[y.id][z.id] - D[x.id][y.id]));
        W[z.id][x.id] = W[x.id][z.id];
        W[y.id][z.id] = Math.max(0, 0.5 * (D[x.id][y.id] + D[y.id][z.id] - D[x.id][z.id]));
        W[z.id][y.id] = W[y.id][z.id];
/*
			System.err.println("Ordering and W matrix\n");
        	System.err.print(x.id);
        	for(NetNode j=x.next;j!=x;j=j.next)
        		System.err.print("\t"+j.id);
        	System.err.println();
        	for(NetNode i=x;i!=x.prev;i=i.next) {
        		for(NetNode j=i.next;j!=x;j=j.next) {
        			System.err.print("\tX["+i.id+"]["+j.id+"] = "+W[i.id][j.id]);
        		}
        		System.err.println();
        	}
        	for(NetNode i=x;i!=x.prev;i=i.next) {
        		for(NetNode j=i.next;j!=x;j=j.next) {
        			System.err.print("\tD["+i.id+"]["+j.id+"] = "+D[i.id][j.id]);
        		}
        		System.err.println();
        	}
        	*/

        /* Initialisation for the weight estimation. */
        //After each expansion, yhat[k.id] is the optimal value
        //for split w_{y.id,k.id} such that give the same induced weights
        //as the iteration before, but without the constraint that the 
        //weights are non-negative.
        //Note that yhat[y.id] = 0.0 by definition.
        double[] yhat = new double[max_num_nodes];




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

            //Compute \tp_{uv}
            double p_uv = W[u.id][v.id];
            for (NetNode k = v.next; k != u; k = k.next) {
                p_uv += W[v.id][k.id];
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

            //System.err.println("[u,v] = " + u.id + ", "+v.id+"\tp_uv = " + p_uv+ "\t [x,y,z] = "+x.id+", "+y.id+", "+z.id);

            /* Update the weights
             */

            //Compute yhat
            //yhat[x.id] is the same as \yhat_1 in the notes.
            yhat[x.id] = 1.0 / 3.0 * D[x.id][y.id] - 1.0 / 6.0 * D[y.id][z.id] - 1.0 / 6.0 * D[x.id][z.id]
                    + 1.0 / 4.0 * D[x.id][x.prev.id] - 1.0 / 2.0 * D[y.id][x.prev.id] + 1.0 / 4.0 * D[z.id][x.prev.id]
                    + 3.0 / 4.0 * W[u.id][v.id] + 1.0 / 8.0 * p_uv;

            //System.err.println("yhat[xid] = " +(1.0/3.0*D[x.id][y.id] - 1.0/6*D[y.id][z.id]-1.0/6.0*D[x.id][z.id] 
            //		+ 1.0/4*D[x.id][x.prev.id] - 1.0/2.0*D[y.id][x.prev.id]+1.0/4.0*D[z.id][x.prev.id]
            //		+3.0/4.0*W[u.id][v.id] + 1.0/8.0*p_uv));

            yhat[z.id] = 1.0 / 3.0 * D[x.id][y.id] + 1.0 / 3.0 * D[y.id][z.id] - 2.0 / 3 * D[x.id][z.id] + 1.0 / 2.0 * p_uv;
            yhat[z.next.id] = 1.0 / 2.0 * D[x.id][z.id] - 1.0 / 4.0 * D[x.id][z.next.id] - 1.0 / 2.0 * D[y.id][z.id]
                    + 1.0 / 2.0 * D[y.id][z.next.id] - 1.0 / 4.0 * D[z.id][z.next.id] + 3.0 / 4.0 * W[v.id][v.next.id] - 3.0 / 8.0 * p_uv;

            //Note: if we get here then there are at least 4 nodes. If there are 4 nodes then 
            //z.next.next = x, and the following loop is skipped.
            for (NetNode k = z.next.next; k != x; k = k.next) {
                yhat[k.id] = 0.25 * D[x.id][k.prev.id] - 0.25 * D[x.id][k.id] - 0.5 * D[y.id][k.prev.id]
                        + 0.5 * D[y.id][k.id] + 0.25 * D[z.id][k.prev.id] - 0.25 * D[z.id][k.id] + 0.75 * W[v.id][k.id];
            }
        	
            /*System.err.println("yhat");
            NetNode kk=x;
            do {
                System.err.println("yhat["+kk.id+"] = "+yhat[kk.id]);
                kk = kk.next;
            } while(kk!=x);
            */


            double[] yhat3 = new double[3];
            yhat3[0] = yhat[x.id];
            yhat3[1] = yhat[z.id];
            yhat3[2] = yhat[z.next.id];
            double[] y3 = optimize3y(yhat3, W[u.id][v.id], W[u.id][v.next.id], W[v.id][v.next.id]);

            double[] yvec = new double[max_num_nodes];
            yvec[x.id] = yhat3[0];
            yvec[z.id] = yhat3[1];
            yvec[z.next.id] = yhat3[2];

            for (NetNode k = z.next.next; k != x; k = k.next) {
                double yk = yhat[k.id];
                double upperBound = Math.min(3.0 * W[u.id][k.id], 1.5 * W[v.next.id][k.id]);
                double lowerBound = Math.max(0.0, 1.5 * W[v.id][k.id] - 3.0 * W[v.next.id][k.id]);
                yk = Math.min(yk, upperBound);
                yk = Math.max(yk, lowerBound);
                yvec[k.id] = yk;
            }
            
            /* System.err.println("y");
            kk = x;
            do {
                System.err.println("y["+kk.id+"] = "+yvec[kk.id]);
                kk = kk.next;
            } while(kk!=x); */

            W[x.id][y.id] = W[y.id][x.id] = yvec[x.id];
            W[y.id][z.id] = W[z.id][y.id] = yvec[z.id];
            W[y.id][z.next.id] = W[z.next.id][y.id] = yvec[z.next.id];
            W[x.id][z.id] = W[z.id][x.id] = Math.max(0.0, 1.5 * W[u.id][v.id] - yvec[x.id] - 0.5 * yvec[z.id]);
            W[x.id][z.next.id] = Math.max(-0.5 * W[u.id][v.id] + W[u.id][v.next.id]
                    + (1.0 / 3.0) * yvec[x.id] + 1.0 / 6.0 * yvec[z.id] - 1.0 / 3.0 * yvec[z.next.id], 0.0);
            W[z.next.id][x.id] = W[x.id][z.next.id];
            W[z.id][z.next.id] = W[z.next.id][z.id] = Math.max(1.5 * W[v.id][v.next.id] - 0.5 * yvec[z.id] - yvec[z.next.id], 0.0);


            for (NetNode j = z.next.next; j != x; j = j.next) {
                W[x.id][j.id] = W[j.id][x.id] = Math.max(0.0, W[u.id][j.id] - (1.0 / 3.0) * yvec[j.id]);
                W[y.id][j.id] = W[j.id][y.id] = yvec[j.id];
                W[z.id][j.id] = W[j.id][z.id] = Math.max(0.0, 1.5 * W[v.id][j.id] - yvec[j.id]);
                W[z.next.id][j.id] = W[j.id][z.next.id] = Math.max(-0.5 * W[v.id][j.id] + W[v.next.id][j.id] + (1.0 / 3.0) * yvec[j.id], 0.0);
            }

            //TODO: Figure out if we need the max(0,w_ij) here.
            

        	/* System.err.println("Ordering and W matrix\n");
        	System.err.print(x.id);
        	for(NetNode j=x.next;j!=x;j=j.next)
        		System.err.print("\t"+j.id);
        	System.err.println();
        	for(NetNode i=x;i!=x.prev;i=i.next) {
        		for(NetNode j=i.next;j!=x;j=j.next) {
        			System.err.print("\tX["+i.id+"]["+j.id+"] = "+W[j.id][i.id]);
        		}
        		System.err.println();
        	}
            for(NetNode i=x;i!=x.prev;i=i.next) {
        		for(NetNode j=i.next;j!=x;j=j.next) {
        			System.err.print("\tD["+i.id+"]["+j.id+"] = "+D[j.id][i.id]);
        		}
        		System.err.println();
        	}
            */


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
        int t = 1;
        do {
            // System.err.println("a="+a);
            ordering[t] = a.id;
            a = a.next;
            t++;
        } while (a != x);
        return ordering;
    }


    /** Solve the 3x3 constrained optimization problem:
     min || y - yhat ||
     s.t. X y \leq b
     where X = [{2,1,0},{-2,-1,2},{0,1,2},{-1,0,0},{0,-1,0},{0,0,-1}]
     b = [3w12,6w13-3w12,3w23,0,0,0]'
     **/
    static private double[] optimize3y(double[] yhatArray, double w12, double w13, double w23) {

        System.out.println("w12 = " + w12 + " w13 = " + w13 + " w23 = " + w23);
        System.out.println("yh[0] = " + yhatArray[0] + "yh[1] = " + yhatArray[1] + "yh[2] = " + yhatArray[2]);

        /* Special cases. We handle these separately for efficiency (e.g. when there are lots of 
        zero weights) and also to avoid degenerate active constraints in the active set algorithm. */
        if (w12 == 0) {
            double[] y = new double[3];
            double lower = 0.0;
            double upper = Math.min(1.5 * w23, 3.0 * w13);
            y[2] = Math.max(lower, Math.min(yhatArray[2], upper));
            return y;
        } else if (w23 == 0) {
            double[] y = new double[3];
            double lower = Math.max(0.0, 1.5 * w12 - 3.0 * w13);
            double upper = 1.5 * w12;
            y[0] = Math.max(lower, Math.min(yhatArray[0], upper));
            return y;
        } else if (w13 == 0) {
            double[] y = new double[3];
            double upper = Math.min(3.0 * w12, 3.0 * w23);
            double lower = 0.0;
            y[1] = Math.max(lower, Math.min(0.6 * w12 - 0.4 * yhatArray[0] + 0.8 * yhatArray[1], upper));
            y[0] = 1.5 * w12 - 0.5 * y[1];
            return y;
        }


        double EPSILON = 1e-15;

        //Set up constraints: these are Xy - b \leq 0.
        Matrix X = new Matrix(new double[][]
                {{2, 1, 0}, {-2, -1, 2}, {0, 1, 2}, {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}});
        Matrix b = new Matrix(new double[][]
                {{3 * w12}, {6.0 * w13 - 3.0 * w12}, {3.0 * w23}, {0}, {0}, {0}});
        //Initial solution (always feasible for this problem)
        Matrix y = new Matrix(new double[][]{{1.5 * w12}, {0}, {0}});

        Matrix yhat = new Matrix(new double[][]{{yhatArray[0]}, {yhatArray[1]}, {yhatArray[2]}});

        boolean[] active = new boolean[6]; //active set. Initially all false
        int nactive = 0;

        Matrix lambda = new Matrix(6, 1);  //lambda = 0.

        boolean outer = true;
        while (outer) {
            boolean inner = true;
            //System.out.println("Outer loop");
            while (inner) {
                //Set ynext so that it solves the equality constrained optimization 
                Matrix ynext = new Matrix(3, 1);
                if (nactive == 0) {
                    ynext = yhat.copy();
                    lambda = new Matrix(6, 1); //lambda=0.
                } else {
                    //These arrays used to specify submatrices
                    int[] A = new int[nactive]; //Array of active indices
                    int newi = 0;
                    for (int i = 0; i < 6; i++) {
                        if (active[i]) {
                            A[newi] = i;
                            newi++;
                        }
                    }
                    int[] B0 = {0};
                    int[] B1 = {0, 1, 2};
                    int[] B2 = new int[nactive]; //B2 = 3:(3+nactive)
                    for (int i = 0; i < nactive; i++)
                        B2[i] = 3 + i;

                    Matrix Y = X.getMatrix(A, B1);
                    Matrix btilde = b.getMatrix(A, B0);

                    //M = [2I  Y'; Y 0] 
                    Matrix M = new Matrix(3 + nactive, 3 + nactive);
                    for (int i = 0; i < 3; i++)
                        M.set(i, i, 2);
                    M.setMatrix(B1, B2, Y.transpose());
                    M.setMatrix(B2, B1, Y);

                    //c = [yhat; btilde]
                    Matrix c = new Matrix(3 + nactive, 1);
                    c.setMatrix(B1, B0, yhat);
                    c.setMatrix(B2, B0, btilde);

                    if (Math.abs(M.det()) < 1e-10) {
                        M.print(5, 5);
                        c.print(5, 5);
                        for (int i = 0; i < 6; i++) {
                            if (active[i]) System.out.print("\tTrue");
                            else System.out.print("\tFalse");
                        }
                    }


                    Matrix ylambda = M.solve(c);

                    ynext = ylambda.getMatrix(B1, B0);
                    lambda = new Matrix(6, 1);  //lambda = 0
                    lambda.setMatrix(A, B0, ylambda.getMatrix(B2, B0));
                }

                //Check feasibility
                Matrix gy = (X.times(y)).minus(b);
                Matrix gny = (X.times(ynext)).minus(b);

                double tmin = Double.MAX_VALUE;
                int imin = -1;

                for (int i = 0; i < 6; i++) {
                    if (!active[i] && gny.get(i, 0) > EPSILON) {
                        double ti = -gy.get(i, 0) / (gny.get(i, 0) - gy.get(i, 0));
                        if (ti < tmin) {
                            imin = i;
                            tmin = ti;
                        }
                    }
                }

                if (imin == -1 || nactive == 3) {
                    inner = false;
                    y = ynext;
                } else {
                    for (int i = 0; i < 3; i++)
                        y.set(i, 0, (1.0 - tmin) * y.get(i, 0) + tmin * ynext.get(i, 0));
                    active[imin] = true;
                    System.out.println("Adding constraint " + imin + "with tmin = " + tmin);
                    System.out.println("y = ");
                    System.out.println("gy min = " + gy.get(imin, 0) + "gyn min = " + gny.get(imin, 0));
                    y.print(5, 5);
                    nactive++;
                }


            }

            //Check optimality of feasible optimum.
            double lmin = -EPSILON;
            int imin = 0;
            for (int i = 0; i < 6; i++) {
                if (active[i] && lambda.get(i, 0) < lmin) {
                    lmin = lambda.get(i, 0);
                    imin = i;
                }
            }
            if (imin == 0) {
                outer = false;
                return y.getColumnPackedCopy();
            } else {
                active[imin] = false;
                nactive--;
            }
        }
        return null; //Should never get here.
    }


    /**
     * Create the set of all circular splits for a given ordering
     *
     * @param ntax     Number of taxa
     * @param ordering circular ordering
     * @param weight   weight to give each split
     * @return set of ntax*(ntax-1)/2 circular splits
     */
    static public Splits getSplits(int[] ordering, double[][] W, double threshold) {
        /* Construct the splits with the appropriate weights */
        Splits splits = new Splits(ordering.length - 1);
        int ntax = ordering.length - 1;
        for (int i = 1; i <= ntax; i++) {
            int id1 = ordering[i];
            TaxaSet t = new TaxaSet();
            for (int j = i + 1; j <= ntax; j++) {
                t.set(ordering[j - 1]);
                int id2 = ordering[j];

                if (W[id1][id2] > threshold)
                    splits.add(t, (float) W[id1][id2]);

            }
        }
        return splits;
    }




    /* A node in the net */

//  class NetNode {
// 	int id = 0;
// 	NetNode nbr = null; // adjacent node
// 	NetNode ch1 = null; // first child
// 	NetNode ch2 = null; // second child
// 	NetNode next = null; // next in list of active nodes
// 	NetNode prev = null; // prev in list of active nodes
// 	double Rx = 0;
// 	double Sx = 0;
// 
// 	public String toString() {
// 		String str = "[id=" + id;
// 		str += " nbr=" + (nbr == null ? "null" : ("" + nbr.id));
// 		str += " ch1=" + (ch1 == null ? "null" : ("" + ch1.id));
// 		str += " ch2=" + (ch2 == null ? "null" : ("" + ch2.id));
// 		str += " prev=" + (prev == null ? "null" : ("" + prev.id));
// 		str += " next=" + (next == null ? "null" : ("" + next.id));
// 		str += " Rx=" + Rx;
// 		str += " Sx=" + Sx;
// 		str += "]";
// 		return str;
// 	}
//  }

}
// EOF
