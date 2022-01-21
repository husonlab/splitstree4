/*
 * BalancedConfidenceNetwork.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.util.CanceledException;
import splitstree4.algorithms.util.ConfidenceNetwork;
import splitstree4.core.Document;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.SplitMatrix;

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
