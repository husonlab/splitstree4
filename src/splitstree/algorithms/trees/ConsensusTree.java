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

import jloda.util.Alert;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.util.LinkedList;
import java.util.List;

/**
 * implements common tree consensus methods
 *
 * @author huson
 *         Date: 21-Mar-2005
 */
public class ConsensusTree implements Trees2Splits {
    public final static String DESCRIPTION = "Tree consensus methods";

    final String MAJORITY = "Majority";
    final String STRICT = "Strict";
    final String GREEDY = "Greedy";
    final String LOOSE = "Loose";

    private String optionMethod = MAJORITY;


    ConsensusNetwork consensusNetwork = new ConsensusNetwork();

    /**
     * Applies the method to the given data
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) throws SplitsException, CanceledException {
        switch (getOptionMethod()) {
            case STRICT:
                consensusNetwork.setOptionThreshold(0.99999999);
                return consensusNetwork.apply(doc, taxa, trees);
            case MAJORITY:
                consensusNetwork.setOptionThreshold(0.5);
                return consensusNetwork.apply(doc, taxa, trees);
            case GREEDY:
                new Alert("Not implemented: " + GREEDY);
                return null;
            case LOOSE:
                new Alert("Not implemented: " + LOOSE);
                return null;
        }
        return null;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return consensusNetwork.isApplicable(doc, taxa, trees);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public String getOptionMethod() {
        return optionMethod;
    }

    public void setOptionMethod(String optionMethod) {
        this.optionMethod = optionMethod;
    }

    public List selectionOptionMethod(Document doc) {
        List list = new LinkedList();
        list.add(STRICT);
        list.add(MAJORITY);
        //list.add(GREEDY);
        //list.add(LOOSE);
        return list;
    }

    /**
     * decide what to scale the edge weights by
     *
     * @return
     */
    public String getOptionEdgeWeights() {
        return consensusNetwork.getOptionEdgeWeights();
    }

    /**
     * decide what to scale the edge weights by
     *
     * @param optionEdgeWeights
     */
    public void setOptionEdgeWeights(String optionEdgeWeights) {
        consensusNetwork.setOptionEdgeWeights(optionEdgeWeights);
    }

    /**
     * return the possible chocies for optionEdgeWeights
     *
     * @param doc
     * @return list of choices
     */
    public List selectionOptionEdgeWeights(Document doc) {
        return consensusNetwork.selectionOptionEdgeWeights(doc);
    }
}
