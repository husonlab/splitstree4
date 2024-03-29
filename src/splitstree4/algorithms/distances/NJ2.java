/*
 * NJ2.java Copyright (C) 2022 Daniel H. Huson
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
// NOTE: apply uses the upper triangle of the dist matix

package splitstree4.algorithms.distances;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

/**
 * @deprecated
 * Neighbor-Joining
 */
public class NJ2 /* implements Distances2Trees*/ {


    public final boolean EXPERT = true;
	public static final String DESCRIPTION = "Computes the Neighbour-Joining tree (Saitou and Nei 1987)";


    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    static public PhyloTree makeNJTree(Taxa taxa, Distances dist) {
        try {
            return makeNJTree(taxa, dist, null);
        } catch (CanceledException ex) {
            return null;
        }
    }


    static private PhyloTree makeNJTree(Taxa taxa, Distances dist, Document doc) throws CanceledException {


        PhyloTree tree = new PhyloTree();


        int ntax = dist.getNtax();

        Node[] subtrees = new Node[ntax + 1];
        double[][] d = new double[ntax + 1][ntax + 1];// distance matix
        double[] r = new double[ntax + 1];// sum of distances to each taxa

        for (int i = 1; i <= ntax; i++) {
            subtrees[i] = tree.newNode();
            tree.setLabel(subtrees[i], taxa.getLabel(i));
        }

        //Initialise d
        //Compute the closest values for each taxa.
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double dij = (dist.get(i, j) + dist.get(j, i)) / 2.0;// distance matix h
                d[i][j] = d[j][i] = dij;
            }
        }

        //initialise r
        for (int i = 1; i <= ntax; i++) {
            for (int j = 1; j <= ntax; j++) {
                r[i] += d[i][j];
            }
        }


        int steps = 0;
        for (int actual = ntax; actual > 2; actual--) {

            int i_min = 0, j_min = 0;
            //Find closest pair.
            double q_min = Double.MAX_VALUE;
            for (int i = 1; i <= actual; i++) {
                for (int j = i + 1; j <= actual; j++) {
                    double qij = ((double) actual - 2.0) * d[i][j] - r[i] - r[j];
                    if (i_min == 0 || qij < q_min) {
                        i_min = i;
                        j_min = j;
                        q_min = qij;
                    }
                }

            }

            double dist_e = 0.5 * (d[i_min][j_min] + r[i_min] / (actual - 2)
                    - r[j_min] / (actual - 2));

            double dist_f = 0.5 * (d[i_min][j_min] + r[j_min] / (actual - 2)
                    - r[i_min] / (actual - 2));

            Node v = tree.newNode();
            Edge e = tree.newEdge(subtrees[i_min], v);
            tree.setWeight(e, Math.max(dist_e, 0.0));
            Edge f = tree.newEdge(subtrees[j_min], v);
            tree.setWeight(f, Math.max(dist_f, 0.0));

            subtrees[i_min] = v;
            subtrees[j_min] = null;

            //Update d matrix and r
            r[i_min] = 0;
            for (int k = 1; k <= ntax; k++) {
                if ((k == i_min) || k == j_min) continue;
                double dki = (d[k][i_min] + d[k][j_min] - dist_e - dist_f) / 2;
                r[k] = r[k] - d[k][i_min] - d[k][j_min] + dki;
                r[i_min] += dki;
                d[k][i_min] = d[i_min][k] = dki;
            }

            //Copy the top row of the matrix and arrays into the empty j_min row/column.
            if (j_min < actual) {
                for (int k = 1; k <= actual; k++) {
                    if (k != j_min)
                        d[j_min][k] = d[k][j_min] = d[actual][k];
                }
                d[j_min][j_min] = 0.0;
                r[j_min] = r[actual];
                subtrees[j_min] = subtrees[actual];
                subtrees[actual] = null;
            }

            //Update the progress bar
            steps += actual;
            if (doc != null)
                doc.notifySetProgress(steps);
        }
        //join last two nodes with a single edge.
        Edge e = tree.newEdge(subtrees[1], subtrees[2]);
        tree.setWeight(e, d[1][2]);
        return tree;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the input taxa
     * @param dist the distances matrix
     * @return always true, because Neighbor Joining is always applicable
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances dist) {
        return taxa != null && dist != null;
    }


    /**
     * Applies the method to the given data
     *
     * @param taxa the input taxa
     * @param dist the input distances
     * @return the computed phylogenetic tree (PhyloTree) as a nexus Trees object
     */
    public Trees apply(Document doc, Taxa taxa, Distances dist) throws CanceledException {
        // ProgressDialog pd = new ProgressDialog("NJ...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(taxa.getNtax() * (taxa.getNtax() - 1) / 2);    //initialize maximum progress
        doc.notifySetProgress(0);
        PhyloTree tree = makeNJTree(taxa, dist, doc);

        return new Trees("NJ", tree, taxa);
    }
}

// EOF

