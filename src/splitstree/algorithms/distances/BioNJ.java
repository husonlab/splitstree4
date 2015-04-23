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

// NOTE: apply uses the upper and lower triangle of the dist matix

/**
 * @version $Id: BioNJ.java,v 1.18 2008-02-26 21:20:23 huson Exp $
 *
 * @author David Bryant *
 */
package splitstree.algorithms.distances;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.util.HashMap;


/**
 * Implementation of the Bio-Neighbor-Joining algorithm (Gascuel 1997)
 */
public class BioNJ implements Distances2Trees {
    public final static String DESCRIPTION = "Computes the Bio-NJ tree (Gascuel 1997)";

    /**
     * Determine whether the NJ algorithm can be applied to given data.
     *
     * @param taxa the input taxa
     * @param dist the distances matrix
     * @return return true if taxa and dist are valid
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances dist) {
        return taxa != null && dist != null;
    }

    /**
     * Comutes the NJ tree given the taxa and distances
     *
     * @param doc
     * @param taxa
     * @param dist
     * @return
     * @throws CanceledException
     */
    public Trees apply(Document doc, Taxa taxa, Distances dist) throws CanceledException {
        return computeTrees(doc, taxa, dist);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public Trees computeTrees(Taxa taxa, Distances dist) {
        try {
            return computeTrees(null, taxa, dist);
        } catch (CanceledException e) {
        }
        return null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa the input taxa
     * @param dist the input distances
     * @return the computed phylogenetic tree (PhyloTree) as a nexus Trees object
     */
    private Trees computeTrees(Document doc, Taxa taxa, Distances dist) throws CanceledException {
        PhyloTree tree = new PhyloTree();
        // ProgressDialog pd = new ProgressDialog("Bio NJ...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        if (doc != null) {
            doc.notifySetMaximumProgress(dist.getNtax());    //initialize maximum progress
            doc.notifySetProgress(0);
        }

        try {

            HashMap TaxaHashMap = new HashMap();
            int nbNtax = dist.getNtax();
            StringBuffer tax[] = new StringBuffer[nbNtax + 1];
            //Taxalabes are saved as a StringBuffer array

            for (int i = 1; i <= nbNtax; i++) {
                tax[i] = new StringBuffer();
                tax[i].append(taxa.getLabel(i));
                Node v = tree.newNode(); // create newNode for each Taxon
                tree.setLabel(v, tax[i].toString());
                TaxaHashMap.put(tax[i].toString(), v);
            }

            double h[][] = new double[nbNtax + 1][nbNtax + 1];// distance matix

            boolean active[] = new boolean[nbNtax + 1];

            double var[][] = new double[nbNtax + 1][nbNtax + 1]; // variances matrix. This really should be upper diag of h.
            double b[] = new double[nbNtax + 1];// the b variable in Neighbor Joining
            int i_min = 0, j_min = 0; // needed for manipulation of h and b
            double temp, dist_e, dist_f;//new edge weights
            StringBuffer tax_old_i; //labels of taxa that are being merged
            StringBuffer tax_old_j;
            StringBuffer tax_old_k;
            Node v;
            Edge e, f; //from tax_old to new=merged edge
            double lambda; //lambda value in BioNJ

            for (int i = 1; i <= nbNtax; i++) {
                active[i] = true;
            }
            for (int i = 1; i <= nbNtax; i++) {
                h[i][i] = 0.0;
                for (int j = 1; j <= nbNtax; j++) { //fill up the distance matix h
                    if (i < j)
                        h[i][j] = dist.get(i, j);//
                    else
                        h[i][j] = dist.get(j, i);
                    var[i][j] = h[i][j];
                }
            }

            // calculate b:
            for (int i = 1; i <= nbNtax; i++) {
                for (int j = 1; j <= nbNtax; j++) {
                    b[i] += h[i][j];
                }
            }
            // recall: int i_min=0, j_min=0;

            // actual for (finding all nearest Neighbors)
            for (int actual = nbNtax; actual > 3; actual--) {
                // find: min D (h, b, b)
                double d_min = Double.MAX_VALUE, d_ij;
                for (int i = 1; i < nbNtax; i++) {
                    if (!active[i]) continue;
                    for (int j = i + 1; j <= nbNtax; j++) {
                        if (!active[j])
                            continue;
                        d_ij = ((double) actual - 2.0) * h[i][j] - b[i] - b[j];
                        if (d_ij < d_min) {
                            d_min = d_ij;
                            i_min = i;
                            j_min = j;
                        }
                    }
                }
                dist_e = 0.5 * (h[i_min][j_min] + b[i_min] / ((double) actual - 2.0)
                        - b[j_min] / ((double) actual - 2.0));
                dist_f = h[i_min][j_min] - dist_e;
                //dist_f=0.5*(h[i_min][j_min] + b[j_min]/((double)actual-2.0)
                //	- b[i_min]/((double)actual-2.0) );

                active[j_min] = false;

                // tax taxa update:
                tax_old_i = new StringBuffer(tax[i_min].toString());
                tax_old_j = new StringBuffer(tax[j_min].toString());
                tax[i_min].insert(0, "(");
                tax[i_min].append(",");
                tax[i_min].append(tax[j_min]);
                tax[i_min].append(")");
                tax[j_min].delete(0, tax[j_min].length());

                // b update:

                b[i_min] = 0.0;
                b[j_min] = 0.0;

                // fusion of h
                // double h_min = h[i_min][j_min];
                double var_min = var[i_min][j_min]; //Variance of the distance between i_min and j_min

//compute lambda to minimize the variances of the new distances
                lambda = 0.0;
                if ((var_min == 0.0) || (actual == 3))
                    lambda = 0.5;
                else {
                    for (int i = 1; i <= nbNtax; i++) {
                        if ((i_min != i) && (j_min != i) && (h[0][i] != 0.0))
                            lambda += var[i_min][i] - var[j_min][i];
                    }
                    lambda = 0.5 + lambda / (2.0 * (actual - 2) * var_min);
                    if (lambda < 0.0)
                        lambda = 0.0;
                    if (lambda > 1.0)
                        lambda = 1.0;
                }

//System.out.println("Joining  "+i_min + " and " + j_min);

//System.out.println("lambda = "+lambda + "\n");

//printf("lambda = %f\n",lambda);


                for (int i = 1; i <= nbNtax; i++) {
                    if ((i == i_min) || (!active[i]))
                        continue;
                    //temp=(h[i][i_min] + h[i][j_min] - h_min)/2; NJ                                        //temp=(h[i][i_min] + h[i][j_min] - dist_e - dist_f)/2; NJ
                    temp = (1.0 - lambda) * (h[i][i_min] - dist_e) + (lambda) * (h[i][j_min] - dist_f); //BioNJ

                    if (i != i_min) {
                        b[i] = b[i] - h[i][i_min] - h[i][j_min] + temp;
                    }
                    b[i_min] += temp;
                    h[i_min][i] = h[i][i_min] = temp; //WARNING... this can affect updating of b[i]
//Update variances
                    var[i_min][i] = (1.0 - lambda) * var[i_min][i] + (lambda) * var[j_min][i] - lambda * (1.0 - lambda) * var_min;
                    var[i][i_min] = var[i_min][i];
                }

                for (int i = 1; i <= nbNtax; i++) {
                    h[i_min][i] = h[i][i_min];
                    h[i][j_min] = 0.0;
                    h[j_min][i] = 0.0;
                }

                // generate new Node for merged Taxa:
                v = tree.newNode();
                TaxaHashMap.put(tax[i_min].toString(), v);

                // generate Edges from two Taxa that are merged to one:
                e = tree.newEdge((Node) TaxaHashMap.get(tax_old_i.toString()), v);
                tree.setWeight(e, dist_e);
                f = tree.newEdge((Node) TaxaHashMap.get(tax_old_j.toString()), v);
                tree.setWeight(f, dist_f);
                if (doc != null)
                    doc.notifySetProgress(dist.getNtax() - actual);
            }

            // evaluating last three nodes:
            int k_min, i;
            i = 1;
            while (!active[i])
                i++;
            i_min = i;
            i++;
            while (!active[i])
                i++;
            j_min = i;
            i++;
            while (!active[i])
                i++;
            k_min = i;

            tax_old_i = new StringBuffer(tax[i_min].toString());
            tax_old_j = new StringBuffer(tax[j_min].toString());
            tax_old_k = new StringBuffer(tax[k_min].toString());

            tax[i_min].insert(0, "(");
            tax[i_min].append(",");
            tax[i_min].append(tax[j_min]);
            tax[i_min].append(",");
            tax[i_min].append(tax[k_min]);
            tax[i_min].append(")");
            tax[j_min].delete(0, tax[j_min].length()); //not neces. but sets content to NULL
            tax[k_min].delete(0, tax[k_min].length()); //not neces. but sets content to NULL

            // System.err.println(tax[i_min].toString());

// generate new Node for the root of the tree.
            v = tree.newNode();
            TaxaHashMap.put(tax[i_min].toString(), v);
            e = tree.newEdge((Node) TaxaHashMap.get(tax_old_i.toString()), v);
            tree.setWeight(e, 0.5 * (h[i_min][j_min] + h[i_min][k_min] - h[j_min][k_min]));
            e = tree.newEdge((Node) TaxaHashMap.get(tax_old_j.toString()), v);
            tree.setWeight(e, 0.5 * (h[i_min][j_min] + h[j_min][k_min] - h[i_min][k_min]));
            e = tree.newEdge((Node) TaxaHashMap.get(tax_old_k.toString()), v);
            tree.setWeight(e, 0.5 * (h[i_min][k_min] + h[j_min][k_min] - h[i_min][j_min]));

        } catch (Exception ex) {
            Basic.caught(ex);
        }
//******************************************
        //long ti = System.currentTimeMillis();
        //while(true)if(ti+500<=System.currentTimeMillis())break;
//******************************************
        if (doc != null)
            doc.notifySetProgress(dist.getNtax());   //set progress to 100%
        // pd.close();								//get rid of the progress listener
        // // doc.setProgressListener(null);

        return new Trees("BioNJ", tree, taxa);
    }
}

// EOF

