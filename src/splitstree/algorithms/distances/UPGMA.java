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

// NOTE: apply uses the upper triangle of the dist matix

/**
 * @version $Id: UPGMA.java,v 1.22 2010-02-04 11:53:16 huson Exp $
 *
 * @author Christian Rausch
 *
 */
package splitstree.algorithms.distances;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

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

        double d[][] = new double[ntax + 1][ntax + 1];// distance matix

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

        return tree;
    }

}

// EOF

