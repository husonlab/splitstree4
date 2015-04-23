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

package splitstree.algorithms.util.simulate;

import jloda.phylo.PhyloTree;
import splitstree.algorithms.util.PaupNode;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.TreesUtilities;

/**
 * @author David Bryant
 *         Utilities for generating random distance matrices from a tree.
 */
public class RandomDistances {

    /**
     * getAdditive Distances
     * <p/>
     * Returns the additive (pairwise) distance for a PhyloTree
     *
     * @param taxa Set of taxa labelling the leaves
     * @param T    PhyloTree
     * @return Distances block
     */

    static public Distances getAdditiveDistances(Taxa taxa, PhyloTree T) {
        Trees trees = new Trees("", T, taxa);
        return TreesUtilities.getAveragePairwiseDistances(taxa, trees);
    }

    /**
     * getAdditive Distances
     * <p/>
     * Returns the additive (pairwise) distance for a PaupNode tree
     *
     * @param taxa Set of taxa labelling the leaves
     * @param T    Root of the PaupNode tree
     * @return Distances block
     */

    static public Distances getAdditiveDistances(Taxa taxa, PaupNode T) {
        Distances dist = new Distances(taxa.getNtax());
        for (PaupNode x = T.leftmostLeaf(); x != null; x = x.nextPost()) {
            if (x.isLeaf()) {
                for (PaupNode p = x; p.getPar() != null; p = p.getPar()) {
                    if (p.getNextSib() != null) {
                        for (PaupNode y = p.getNextSib().leftmostLeaf(); y != p.getPar(); y = y.nextPost()) {
                            if (y.isLeaf()) {
                                double dxy = 0.0;
                                for (PaupNode v = x; v != p.getPar(); v = v.getPar())
                                    dxy += v.length;
                                for (PaupNode v = y; v != p.getPar(); v = v.getPar())
                                    dxy += v.length;
                                dist.set(x.id, y.id, dxy);
                                dist.set(y.id, x.id, dxy);
                            }
                        }
                    }


                }
            }
        }


        return dist;
    }

    /**
     * Add random noise to the distances in a block. We add gaussian noise, truncated at 0.
     *
     * @param distances  (Distances) distance block
     * @param percentVar (double) variance  as a percentage of distance
     * @param random     (GenerateRandom) random number generator
     */

    static public void alterDistances(Distances distances, double percentVar, GenerateRandom random) {
        int n = distances.getNtax();
        for (int i = 1; i <= n; i++)
            for (int j = i + 1; j <= n; j++) {
                double dij = distances.get(i, j);
                double vij = dij * percentVar;
                dij += random.nextGaussian(0.0, vij);
                dij = java.lang.Math.abs(dij);
                distances.set(i, j, dij);
                distances.set(j, i, dij);
            }
    }


}
