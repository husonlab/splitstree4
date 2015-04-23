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

package splitstree.algorithms.additional;

import jloda.util.CanceledException;
import splitstree.algorithms.util.CircularMaxClique;
import splitstree.core.Document;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

/**
 * DESCRIPTION
 *
 * @author bryant
 *         Date: 05-May-2005
 *         <p/>
 *         Uses an inefficient algorithm to compute the closest tree - the subcollection of compatible
 *         splits for which the sum of (squared) weights is maximised.
 *         <p/>
 *         Future versions will include
 *         (1) Bounding - using the vertices excluded, since if the sum of the weights is W and w is the sum
 *         of those excluded so far, then the maximum possible clique weight will be W-w. If this is not larger
 *         than the best found so far, we do not need to branch further.
 *         (2) On circular splits the Hunting for Trees algorithm can be used to solve this problem  in O(n^3) time.
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
