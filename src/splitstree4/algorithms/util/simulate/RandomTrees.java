/**
 * RandomTrees.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.algorithms.util.simulate;

import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.PaupTreeUtils;
import splitstree4.nexus.Taxa;

import java.util.Random;
import java.util.Vector;


/**
 * RandomTrees
 * <p/>
 * Utilities for generating random trees. These construct PaupNode type trees, which can be converted into
 * PhyloTrees using PhyloUtils
 */
public class RandomTrees {


    static public PaupNode randomCoalescentTree(Taxa taxa, double height) {
        GenerateRandom random = new GenerateRandom();
        return randomCoalescentTree(taxa, height, random);
    }

    static public PaupNode randomCoalescentTree(Taxa taxa, double height, GenerateRandom generator) {

        int ntax = taxa.getNtax();
        PaupNode[] subtrees = new PaupNode[ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            subtrees[i] = new PaupNode();
            subtrees[i].id = i;
        }

        int i_node, j_node;
        for (int actual = ntax; actual > 1; actual--) {

            /* Add to the branch lengths of the roots of the current subtrees.
       Under the coalescent, we should add a real number proportional to
       an exponential with mean 1/actual */
            double L = generator.nextExponential(1.0 / actual);   // random uniform

            int i, j;
            for (i = 1; i <= ntax; i++) {
                if (subtrees[i] != null)
                    subtrees[i].length += L;
            }

            /* Randomly choose the next two nodes to combine*/
            int x = generator.nextInt(actual);
            for (i = 1; i < ntax; i++) {
                if (subtrees[i] != null) {
                    x--;
                    if (x < 0) break;
                }
            }
            x = generator.nextInt(actual - 1);
            for (j = 1; j < ntax; j++) {
                if ((subtrees[j] != null) && (i != j)) {
                    x--;
                    if (x < 0) break;
                }
            }

            i_node = Math.min(i, j);
            j_node = Math.max(i, j);


            PaupNode T = new PaupNode();
            subtrees[j_node].attachAsFirstChildOf(T);
            subtrees[i_node].attachAsFirstChildOf(T);

            subtrees[i_node] = T;
            subtrees[j_node] = null;


        }
        //The tree will now be in subtrees[1]


        PaupNode T = subtrees[1];

        //Fix the height

        double treeHeight = 0.0;
        for (PaupNode p = T.getFirstChild(); p != null; p = p.getFirstChild())
            treeHeight += p.length;

        double scale = height / treeHeight;

        PaupTreeUtils.scaleBranchLengths(T, scale);

        return T;
    }

    /**
     * Modifies the branch lengths in an ultrametric tree accoording to a log normal model.
     * The log of the rate at the end of the branch is assumed to be normally distributed with mean
     * equal to the log of the current rate and variance equal to sigma^2 t.
     *
     * @param T
     * @param sigma
     */
    public static void relaxClockLogNormal(PaupNode T, double sigma) {
        GenerateRandom random = new GenerateRandom();
        recurseRelaxLogNormal(T, 1.0, sigma, random);
    }

    private static void recurseRelaxLogNormal(PaupNode p, double r, double sigma, GenerateRandom random) {
        if (p.isLeaf())
            return;
        double childRate;
        double M = Math.log(r);
        for (PaupNode child = p.getFirstChild(); child != null; child = child.getNextSib()) {
            double t = child.length;
            childRate = Math.exp(random.nextGaussian(M, sigma * sigma * t));
            recurseRelaxLogNormal(child, childRate, sigma, random);
            child.length *= (r + childRate) / 2.0; //ToDo: Replace with proper sample.
        }

    }

    public static void relaxClockLogNormal(PaupNode T, double sigma, GenerateRandom random) {
        recurseRelaxLogNormal(T, 1.0, sigma, random);
    }


    /**
     * Multiplies each branch length in T by a random number e^x where x is chosen
     * uniformly in [-epsilon,epsilon]
     *
     * @param T
     * @param epsilon
     * @param random  Random number generator
     */
    public static void relaxExponential(PaupNode T, double epsilon, GenerateRandom random) {
        PaupTreeUtils.updateFastPrePost(T);
        for (PaupNode v = T; v != null; v = PaupTreeUtils.nextPre(v)) {
            if (!v.isRoot()) {
                double rate = Math.exp(random.nextUniform(-epsilon, epsilon));
                v.length *= rate;
            }
        }
    }

    /**
     * Chooses node (excluding root) uniformly according to the length of the branch.
     *
     * @param T
     * @param random
     * @return
     */
    public static PaupNode randomNode(PaupNode T, GenerateRandom random) {
        double total = 0.0;
        for (PaupNode v = T; v != null; v = PaupTreeUtils.nextPre(v)) {
            if (!v.isRoot()) {
                total += v.length;
            }
        }
        double r = random.nextUniform(0, total);
        total = 0.0;
        for (PaupNode v = T; v != null; v = PaupTreeUtils.nextPre(v)) {
            if (!v.isRoot()) {
                total += v.length;
                if (total > r)
                    return v;
            }
        }
        return null;
    }

    /**
     * Chooses two nodes in the tree (uniform on branch length). If one is not ancestral to the other,
     * swaps the two subtrees.
     * Continues until we get two nodes with different parents (and hence a different tree).
     * Assumes more than two leaves (otherwise infinite loop)
     *
     * @param T      tree
     * @param random random number generator
     */

    public static void randomSubtreeSwap(PaupNode T, GenerateRandom random) {
        PaupNode v, w;

        //ToDo: Check more than 2 leaves


        boolean ancestral;
        do {
            v = randomNode(T, random);
            w = randomNode(T, random);

            ancestral = false;
            for (PaupNode x = v; !ancestral && x != null; x = x.getPar()) {
                if (x == w)
                    ancestral = true;
            }
            for (PaupNode x = w; !ancestral && x != null; x = x.getPar()) {
                if (x == v)
                    ancestral = true;
            }
        } while (ancestral || w.getPar() == v.getPar());

        PaupNode wPar = w.getPar();
        PaupNode vPar = v.getPar();

        v.detachFromParent();
        w.detachFromParent();
        w.attachAsFirstChildOf(vPar);
        v.attachAsFirstChildOf(wPar);

    }

    public static void randomSPR(PaupNode T, GenerateRandom random) {
        PaupNode v, w;

        //ToDo: Check more than 2 leaves


        boolean ancestral;
        do {
            v = randomNode(T, random);
            w = randomNode(T, random);

            ancestral = false;
            for (PaupNode x = v; !ancestral && x != null; x = x.getPar()) {
                if (x == w)
                    ancestral = true;
            }
            for (PaupNode x = w; !ancestral && x != null; x = x.getPar()) {
                if (x == v)
                    ancestral = true;
            }
        } while (ancestral || w.getPar() == v.getPar());

        PaupNode newPar = new PaupNode();

        double l1 = random.nextUniform(0, w.length);
        double l2 = w.length - l1;
        newPar.length = l1;
        w.length = l2;
        newPar.attachAsNextSibOf(w);


        v.detachFromParent();
        w.detachFromParent();
        w.attachAsFirstChildOf(newPar);
        v.attachAsFirstChildOf(newPar);

    }


    public static PaupNode pickNodeAtHeight(PaupNode T, double height, Random random) {
        Vector paupNodes = new Vector();
        for (PaupNode v = PaupTreeUtils.leftmostLeaf(T); v != null; v = PaupTreeUtils.nextPost(v)) {
            if (v.isLeaf()) {
                double pathHeight = 0.0;
                for (PaupNode x = v; x != null; x = x.getPar()) {
                    pathHeight += x.length;
                    if (pathHeight > height) {
                        paupNodes.add(x);
                    }
                    if (x.getPar() == null || x.getPar().getFirstChild() != x)
                        break; //Only save each node once.
                }
            }
        }
        int index = random.nextInt(paupNodes.size());
        return (PaupNode) paupNodes.get(index);
    }


}
