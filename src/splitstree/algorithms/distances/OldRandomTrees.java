/**
 * OldRandomTrees.java 
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
 * @version $Id: OldRandomTrees.java
 *
 * Generates random, unweighted trees under the coalescent distribution
 *
 * @author David Bryant
 *
 */
package splitstree.algorithms.distances;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.util.HashMap;
import java.util.Random;

/**
 * @deprecated
 */
public class OldRandomTrees implements Distances2Trees {
    public final static String DESCRIPTION = "Computes Random Trees";
    private int numTrees = 1;
    public final boolean EXPERT = true;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the input taxa
     * @param dist the distances matrix
     * @return always true, because Neighbor Joining is always applicable
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances dist) {
        return (taxa != null);
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa the input taxa
     * @param dist the input distances
     * @return the computed phylogenetic tree (PhyloTree) as a nexus Trees object
     */
    public Trees apply(Document doc, Taxa taxa, Distances dist) throws Exception {

        doc.notifySubtask("Generate random tree(s)");
        doc.notifySetMaximumProgress(this.numTrees);
        doc.notifySetProgress(0);
        PhyloTree tree = makeRandomTree(taxa);

        Trees treelist = new Trees("RandomTree", tree, taxa);
        for (int i = 2; i <= this.numTrees; i++) {
            doc.notifySetProgress(i - 1);
            tree = makeRandomTree(taxa);
            treelist.addTree("RandomTree" + i, tree, taxa);
        }
        doc.notifySetProgress(this.numTrees);
        return treelist;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public void setOptionNumber_of_trees(int value) {
        this.numTrees = value;
    }

    public int getOptionNumber_of_trees() {
        return this.numTrees;
    }

    private PhyloTree makeRandomTree(Taxa taxa) {

        PhyloTree tree = new PhyloTree();
        // ProgressDialog pd = new ProgressDialog("NJ...",""); //Set new progress bar.
        // doc.setProgressListener(pd);

        int ntax = taxa.getNtax();
        boolean[] valid = new boolean[ntax + 1];

        try {
            HashMap TaxaHashMap = new HashMap();
            StringBuffer tax[] = new StringBuffer[ntax + 1];

            //Taxalabels are saved as a StringBuffer array

            for (int i = 1; i <= ntax; i++) {
                tax[i] = new StringBuffer();
                tax[i].append(taxa.getLabel(i));
                Node v = tree.newNode(); // create newNode for each Taxon
                tree.setLabel(v, tax[i].toString());
                TaxaHashMap.put(tax[i].toString(), v);
                valid[i] = true;
            }

            int i_node, j_node;
            StringBuffer tax_old_i; //labels of taxa that are being merged
            StringBuffer tax_old_j;
            Node v;
            Edge e, f; //from tax_old to new=merged edge

            Random generator = new Random();

            for (int actual = ntax; actual > 2; actual--) {

                /* Randomly choose the next two nodes to combine*/
                int x = generator.nextInt(actual);
                for (i_node = 1; i_node < ntax; i_node++) {
                    if (valid[i_node]) {
                        x--;
                        if (x < 0) break;
                    }
                }
                x = generator.nextInt(actual - 1);
                for (j_node = 1; j_node < ntax; j_node++) {
                    if (valid[j_node] && (i_node != j_node)) {
                        x--;
                        if (x < 0) break;
                    }
                }

                valid[j_node] = false;

                // tax taxa update:
                tax_old_i = new StringBuffer(tax[i_node].toString());
                tax_old_j = new StringBuffer(tax[j_node].toString());
                tax[i_node].insert(0, "(");
                tax[i_node].append(",");
                tax[i_node].append(tax[j_node]);
                tax[i_node].append(")");
                tax[j_node].delete(0, tax[j_node].length());

                // generate new Node for merged Taxa:
                v = tree.newNode();
                if (tree.getRoot() == null) {
                    tree.setRoot(v);
                }
                TaxaHashMap.put(tax[i_node].toString(), v);

                // generate Edges from two Taxa that are merged to one:
                e = tree.newEdge((Node) TaxaHashMap.get(tax_old_i.toString()), v);
                tree.setWeight(e, 1.0);
                f = tree.newEdge((Node) TaxaHashMap.get(tax_old_j.toString()), v);
                tree.setWeight(f, 1.0);
            }

            // evaluating last two nodes:
            i_node = j_node = 0;
            for (int i = 1; i <= ntax; i++) {
                if (valid[i]) {
                    i_node = i;
                    i++;
                    for (; i <= ntax; i++) {
                        if (valid[i]) {
                            j_node = i;
                        }
                    }
                }
            }
            tax_old_i = new StringBuffer(tax[i_node].toString());
            tax_old_j = new StringBuffer(tax[j_node].toString());

            tax[i_node].insert(0, "(");
            tax[i_node].append(",");
            tax[i_node].append(tax[j_node]);
            tax[i_node].append(")");
            tax[j_node].delete(0, tax[j_node].length()); //not neces. but sets content to NULL

            // generate new Node for merged Taxa:
            // generate Edges from two Taxa that are merged to one:
            e = tree.newEdge((Node) TaxaHashMap.get(tax_old_i.toString()), (Node) TaxaHashMap.get(tax_old_j.toString()));
            tree.setWeight(e, 1.0);


        } catch (Exception ex) {
            Basic.caught(ex);
            throw ex;
        }
        return tree;
    }

}

// EOF

