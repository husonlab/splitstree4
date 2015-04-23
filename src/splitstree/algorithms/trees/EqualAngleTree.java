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

package splitstree.algorithms.trees;

import jloda.graph.Edge;
import jloda.phylo.PhyloGraphView;
import jloda.phylo.PhyloTree;
import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.TreesUtilities;

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
    public Network apply(Document doc, Taxa taxa, Trees trees) throws Exception {
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
