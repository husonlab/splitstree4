/*
 * EqualAngleTree.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.trees;

import jloda.graph.Edge;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import java.io.IOException;

/**
 * constructs a tree directly from the trees block without first producing splits
 * Daniel Huson and David Bryant
 */
public class EqualAngleTree implements Trees2Network {
    final public static String DESCRIPTION = "Equal Angle algorithm for trees";
    private int optionWhich = 1;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees) && trees.getNtrees() >= 1;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees the trees
     * @return the computed network
     */
    public Network apply(Document doc, Taxa taxa, Trees trees) throws IOException {
        PhyloTree tree = trees.getTree(getOptionWhich());

        TreesUtilities.setNode2taxa(tree, taxa);

        int count = 0;
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            tree.setSplit(e, ++count);
        }

        PhyloGraphView graphView = new PhyloGraphView(tree);
        graphView.resetViews();
        return new Network(taxa, graphView);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public int getOptionWhich() {
        return optionWhich;
    }

    public void setOptionWhich(int optionWhich) {
        this.optionWhich = optionWhich;
    }
}
