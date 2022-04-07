/*
 * NJ.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.util.HashMap;

/**
 * Implements the  Neighbor-Joining algorithm of Saitou and Nei (1987).
 */
public class NJ implements Distances2Trees {
    public final static String DESCRIPTION = "Computes the Neighbour-Joining tree (Saitou and Nei 1987)";

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
        PhyloTree tree = new PhyloTree();
        // ProgressDialog pd = new ProgressDialog("NJ...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        if (doc != null) {
            doc.notifySetMaximumProgress(taxa.getNtax());    //initialize maximum progress
            doc.notifySetProgress(0);
        }

        try {

			HashMap<String, Node> TaxaHashMap = new HashMap<>();
			int nbNtax = dist.getNtax();
			StringBuffer[] tax = new StringBuffer[nbNtax + 1];
			//Taxa labels are saved as a StringBuffer array

			for (int t = 1; t <= nbNtax; t++) {
				tax[t] = new StringBuffer();
				tax[t].append(taxa.getLabel(t));
				Node v = tree.newNode(); // create newNode for each Taxon
				tree.setLabel(v, tax[t].toString());
				tree.addTaxon(v, t);
				TaxaHashMap.put(tax[t].toString(), v);
			}

			double[][] h = new double[nbNtax + 1][nbNtax + 1];// distance matix
			double[] b = new double[nbNtax + 1];// the b variable in Neighbor Joining
			int i_min = 0, j_min = 0; // needed for manipulation of h and b
			double temp, dist_e, dist_f;//new edge weights
			StringBuilder tax_old_i; //labels of taxa that are being merged
			StringBuilder tax_old_j;
			Node v;
			Edge e, f; //from tax_old to new=merged edge

			for (int i = 0; i <= nbNtax; i++) {
				h[0][i] = 1.0; // with 1.0 marked columns indicate columns/rows
				h[i][0] = 1.0;// that haven't been deleted after merging
			}
			for (int i = 1; i <= nbNtax; i++) {
				for (int j = 1; j <= nbNtax; j++) { //fill up the
					if (i < j)
                        h[i][j] = dist.get(i, j);// distance matix h
                    else
                        h[i][j] = dist.get(j, i);
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
            for (int actual = nbNtax; actual > 2; actual--) {
                // find: min D (h, b, b)
                double d_min = Double.MAX_VALUE;
                for (int i = 1; i < nbNtax; i++) {
                    if (h[0][i] == 0.0) continue;
                    for (int j = i + 1; j <= nbNtax; j++) {
                        if (h[0][j] == 0.0)
                            continue;
                        if (h[i][j] - ((b[i] + b[j]) / (actual - 2)) < d_min) {
                            d_min = h[i][j] - ((b[i] + b[j]) / (actual - 2));
                            i_min = i;
                            j_min = j;
                        }
                    }
				}
				dist_e = 0.5 * (h[i_min][j_min] + b[i_min] / (actual - 2)
								- b[j_min] / (actual - 2));
				dist_f = 0.5 * (h[i_min][j_min] + b[j_min] / (actual - 2)
								- b[i_min] / (actual - 2));

				h[j_min][0] = 0.0;// marking
				h[0][j_min] = 0.0;

				// tax taxa update:
				tax_old_i = new StringBuilder(tax[i_min].toString());
				tax_old_j = new StringBuilder(tax[j_min].toString());
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

                for (int i = 1; i <= nbNtax; i++) {
                    if (h[0][i] == 0.0)
                        continue;
                    //temp=(h[i][i_min] + h[i][j_min] - h_min)/2; This is incorrect
                    temp = (h[i][i_min] + h[i][j_min] - dist_e - dist_f) / 2; // correct NJ


                    if (i != i_min) {
                        b[i] = b[i] - h[i][i_min] - h[i][j_min] + temp;
                    }
                    b[i_min] += temp;
                    h[i][i_min] = temp;
                    b[j_min] = 0.0;
                }

                for (int i = 0; i <= nbNtax; i++) {
                    h[i_min][i] = h[i][i_min];
                    h[i][j_min] = 0.0;
                    h[j_min][i] = 0.0;
                }

                // generate new Node for merged Taxa:
                v = tree.newNode();
                TaxaHashMap.put(tax[i_min].toString(), v);

                // generate Edges from two Taxa that are merged to one:
                e = tree.newEdge(TaxaHashMap.get(tax_old_i.toString()), v);
                tree.setWeight(e, Math.max(dist_e, 0.0));
                f = tree.newEdge(TaxaHashMap.get(tax_old_j.toString()), v);
                tree.setWeight(f, Math.max(dist_f, 0.0));
                if (doc != null)
                    doc.notifySetProgress(0);
            }

            // evaluating last two nodes:
            for (int i = 1; i <= nbNtax; i++) {
                if (h[0][i] == 1.0) {
					i_min = i;
					i++;

					for (; i <= nbNtax; i++) {
						if (h[0][i] == 1.0) {
							j_min = i;
						}
					}
				}
			}
			tax_old_i = new StringBuilder(tax[i_min].toString());
			tax_old_j = new StringBuilder(tax[j_min].toString());

			tax[i_min].insert(0, "(");
			tax[i_min].append(",");
			tax[i_min].append(tax[j_min]);
			tax[i_min].append(")");
			tax[j_min].delete(0, tax[j_min].length()); //not neces. but sets content to NULL

			// generate new Node for merged Taxa:
			// generate Edges from two Taxa that are merged to one:
			e = tree.newEdge(TaxaHashMap.get(tax_old_i.toString()), TaxaHashMap.get(tax_old_j.toString()));
            tree.setWeight(e, Math.max(h[i_min][j_min], 0.0));
            tree.setRoot(e.getSource());
            tree.redirectEdgesAwayFromRoot();
        } catch (Exception ex) {
            Basic.caught(ex);
        }
		//System.err.println(tree.toBracketString(true));

        return new Trees("NJ", tree, taxa);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}

// EOF

