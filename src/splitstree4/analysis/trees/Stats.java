/*
 * Stats.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.trees;


import splitstree4.core.Document;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

/**
 * determines whether current trees are partial
 * Daniel Huson and David Bryant
 */
public class Stats implements TreesAnalysisMethod {
    final static public String DESCRIPTION = "Are current trees partial trees";
    boolean partial = false;

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * Runs the analysis
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees the block
     */
    public String apply(Document doc, Taxa taxa, Trees trees) {
        if (trees == null)
            return "Tree block is empty";
        partial = TreesUtilities.computeArePartialTrees(taxa, trees);
        trees.setPartial(partial);
        /*
        for(int t=1;t<=trees.getNtrees();t++)
            if(trees.getTree(t).isBifurcating())
                System.err.println("tree "+t+": bifurcating");
        else
                System.err.println("tree "+t+": multifurcating");
        */
        return "Trees are partial: " + partial;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees);
    }

    public boolean arePartial() {
        return partial;
    }
}
