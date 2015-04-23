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

package splitstree.analysis.trees;


import splitstree.core.Document;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.TreesUtilities;

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
        if (trees==null)
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
