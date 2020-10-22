/**
 * ClosestTree.java
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
package splitstree4.algorithms.additional;

import jloda.util.CanceledException;
import splitstree4.algorithms.util.CircularMaxClique;
import splitstree4.core.Document;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

/**
 * DESCRIPTION
 *
 * @author bryant
 * Date: 05-May-2005
 * <p/>
 * Uses an inefficient algorithm to compute the closest tree - the subcollection of compatible
 * splits for which the sum of (squared) weights is maximised.
 * <p/>
 * Future versions will include
 * (1) Bounding - using the vertices excluded, since if the sum of the weights is W and w is the sum
 * of those excluded so far, then the maximum possible clique weight will be W-w. If this is not larger
 * than the best found so far, we do not need to branch further.
 * (2) On circular splits the Hunting for Trees algorithm can be used to solve this problem  in O(n^3) time.
 */
public class ClosestTree implements Splits2Splits {
    public final static String DESCRIPTION = "Finds closest tree";

    /**
     * is split post modification applicable?
     *
     * @param doc    the document
     * @param splits the split
     * @return true, if split2splits transform applicable?
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValid(taxa) && doc.isValid(splits);
    }

    /**
     * Takes a collection of splits, and an optional array of split weights.
     * Finds a maximum weight clique and sets the weights of all splits
     * not in the clique to zero.
     *
     * @param splits
     * @param weights
     */
    static public void apply(Splits splits, double[] weights) {
        boolean[][] AdjMatrix;
        double[] vertexWeights;
        int n = splits.getNsplits();
        AdjMatrix = SplitsUtilities.compatibilityMatrix(splits);
        vertexWeights = new double[n + 1];
        for (int i = 1; i <= n; i++) {
            if (weights != null)
                vertexWeights[i] = weights[i];
            else
                vertexWeights[i] = splits.getWeight(i);
        }

        if (splits.getProperties().getCompatibility() == Splits.Properties.COMPATIBLE)
            return;  //Already circular!!!

        if (splits.getProperties().getCompatibility() == Splits.Properties.CYCLIC && splits.getCycle() != null) {

            CircularMaxClique.getMaxClique(splits, vertexWeights);
            return;
        }

        MaxWeightClique maxClique = new MaxWeightClique(AdjMatrix, vertexWeights);
        boolean[] clique = maxClique.getMaxClique();
        for (int i = 1; i <= splits.getNsplits(); i++)
            if (!clique[i])
                splits.setWeight(i, 0);
    }

    /**
     * applies the splits to splits transfomration
     *
     * @param doc
     * @param taxa
     * @param splits
     * @throws jloda.util.CanceledException
     */
    public void apply(Document doc, Taxa taxa, Splits splits) throws CanceledException {

        double totalSquaredWeight = 0.0;
        double[] weights = new double[splits.getNsplits() + 1];

        for (int i = 1; i <= splits.getNsplits(); i++) {
            double x = splits.getWeight(i);
            weights[i] = x * x;
            totalSquaredWeight += x * x;
        }

        doc.notifyTasks("Closest Tree", null);
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(-1);

        apply(splits, weights);

        for (int i = 1; i <= splits.getNsplits(); i++) {
            double x = splits.getWeight(i);
            totalSquaredWeight -= x * x;
        }


        doc.notifySetProgress(100);
        double diff = Math.sqrt(totalSquaredWeight);
        System.err.println("Distance to closest tree = " + diff);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return "Finds the closest tree (cf Swofford et al 1996)";
    }
}
