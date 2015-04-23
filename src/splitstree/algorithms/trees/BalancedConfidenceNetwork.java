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

import jloda.util.CanceledException;
import splitstree.algorithms.util.ConfidenceNetwork;
import splitstree.core.Document;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.SplitMatrix;

/**
 * Implements confidence networks using Beran's algorithm
 */
public class BalancedConfidenceNetwork implements Trees2Splits {
    private double level = .95;
    public final static String DESCRIPTION = "Computes a confidence network using Beran's algorithm. cf Huson and Bryant (2006)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees) && trees.getNtrees() > 0
                && !trees.getPartial();
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containinga list of trees
     * @return the computed set of consensus splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) throws CanceledException {

        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(0);

        SplitMatrix M = new SplitMatrix(trees, taxa);

        //SplitMatrixAnalysis.getSingularValues(M);


        return ConfidenceNetwork.getConfidenceNetwork(M, getOptionLevel(), doc);

    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return BalancedConfidenceNetwork.DESCRIPTION;
    }

    /**
     * Get the confidence level used in construction.
     *
     * @return confidence level (between 0 and 1).
     */
    public double getOptionLevel() {
        return level;
    }

    /**
     * Set the confidence interval used in construction.
     *
     * @param level
     */
    public void setOptionLevel(double level) {
        this.level = level;
    }

}
