/*
 * UPGMA.java Copyright (C) 2022 Daniel H. Huson
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
 * classic n³ version implemented by Dave Bryant.
 */
public class UPGMA implements Distances2Trees {
    public final static String DESCRIPTION = "Computes the UPGMA (Unweighted Pair Group Method using Arithmetic averages) tree";

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
        PhyloTree tree = makeUPGMATree(taxa, dist, doc);

        return new Trees("UPGMA", tree, taxa);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    private PhyloTree makeUPGMATree(Taxa taxa, Distances dist, Document doc) throws CanceledException {


        PhyloTree tree = new PhyloTree();

        int ntax = dist.getNtax();

        Node[] subtrees = new Node[ntax + 1];
        int[] sizes = new int[ntax + 1];
        double[] heights = new double[ntax + 1];

        for (int i = 1; i <= ntax; i++) {
            subtrees[i] = tree.newNode();
            tree.setLabel(subtrees[i], taxa.getLabel(i));
            sizes[i] = 1;
        }

        double[][] d = new double[ntax + 1][ntax + 1];// distance matix

        //Initialise d
        //Compute the closest values for each taxa.
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double dij = (dist.get(i, j) + dist.get(j, i)) / 2.0;// distance matix h
                d[i][j] = d[j][i] = dij;

            }
        }

        int steps = 0;
        for (int actual = ntax; actual > 2; actual--) {

            int i_min = 0, j_min = 0;
            //Find closest pair.
            double d_min = Double.MAX_VALUE;
            for (int i = 1; i <= actual; i++) {
                for (int j = i + 1; j <= actual; j++) {
                    double dij = d[i][j];
                    if (i_min == 0 || dij < d_min) {
                        i_min = i;
                        j_min = j;
                        d_min = dij;
                    }
                }

            }


            double height = d_min / 2.0;

            Node v = tree.newNode();
            Edge e = tree.newEdge(subtrees[i_min], v);
            tree.setWeight(e, Math.max(height - heights[i_min], 0.0));
            Edge f = tree.newEdge(subtrees[j_min], v);
            tree.setWeight(f, Math.max(height - heights[j_min], 0.0));

            subtrees[i_min] = v;
            subtrees[j_min] = null;
            heights[i_min] = height;


            int size_i = sizes[i_min];
            int size_j = sizes[j_min];
            sizes[i_min] = size_i + size_j;

            for (int k = 1; k <= ntax; k++) {
                if ((k == i_min) || k == j_min) continue;
                double dki = (d[k][i_min] * size_i + d[k][j_min] * size_j) / ((double) (size_i + size_j));
                d[k][i_min] = d[i_min][k] = dki;
            }

            //Copy the top row of the matrix and arrays into the empty j_min row/column.
            if (j_min < actual) {
                for (int k = 1; k <= actual; k++) {
                    d[j_min][k] = d[k][j_min] = d[actual][k];
                }
                d[j_min][j_min] = 0.0;
                subtrees[j_min] = subtrees[actual];
                sizes[j_min] = sizes[actual];
                heights[j_min] = heights[actual];
            }

            steps += actual;
            if (doc != null)
                doc.notifySetProgress(steps);
        }

        int sister = 2;
        while (subtrees[sister] == null)
            sister++;

        //ToDo: fix phyloTree and get this to return a rooted tree.
        Edge e = tree.newEdge(subtrees[1], subtrees[sister]);
        tree.setWeight(e, d[1][sister]);
        tree.setRoot(e.getSource());
        tree.redirectEdgesAwayFromRoot();

        return tree;
    }

}

// EOF

