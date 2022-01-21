/*
 * RandomDistances.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util.simulate;

import jloda.phylo.PhyloTree;
import splitstree4.algorithms.util.PaupNode;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

/**
 * @author David Bryant
 * Utilities for generating random distance matrices from a tree.
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
