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

package splitstree.algorithms.characters;

import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import splitstree.algorithms.splits.GalledNetwork;
import splitstree.analysis.splits.Stats;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.io.StringWriter;

/**
 *@deprecated under development
 */
public class ReticulationNetwork2 extends GalledNetwork implements Characters2Network {

    public final boolean EXPERT = true;
    int optionMinSplitWeight = 1;

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        // first make splits from binary sequences
        Binary2Splits binary2splits = new Binary2Splits();
        binary2splits.setOptionMinSplitWeight(getOptionMinSplitWeight());
        Splits splits = binary2splits.apply(doc, taxa, chars);
        split2Chars = binary2splits.getSplit2Chars();

        StringWriter sw = new StringWriter();
        splits.write(sw, doc.getTaxa());
        System.out.println("binary2splits: " + sw);
        // compute cycle and stuff:
        doc.setSplits(splits);
        (new Stats()).apply(doc, taxa, splits);
        doc.setSplits(null);
        //super.labelSplits=true;
        //super.labelSequences= true;
        //super.applyOrdering=true;
        Network net = apply(doc, taxa, splits);
        PhyloGraphView graphView = new PhyloGraphView();
        net.syncNetwork2PhyloGraphView(doc.getTaxa(), doc.getSplits(), graphView);
        PhyloGraph graph = (PhyloGraph) graphView.getGraph();
        //writeLabels2Edges(graphView, graph,doc.getSplits());
        return net;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc.isValid(taxa) && doc.isValid(chars) && chars.getFormat().getDatatype().equalsIgnoreCase("standard");
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return "Compute a recombination graph from binary sequences";
    }

    /**
     * what is the minimal split weight?
     *
     * @return min split weight threshold
     */
    public int getOptionMinSplitWeight() {
        return optionMinSplitWeight;
    }

    public void setOptionMinSplitWeight(int optionMinSplitWeight) {
        RecombinationNetwork.optionMinSplitWeight = optionMinSplitWeight;
    }

    public boolean getOptionShowMutations() {
        return super.optionShowMutations;
    }

    public void setOptionShowMutations(boolean labelSplits) {
        super.optionShowMutations = labelSplits;
    }

/*    public boolean getOptionApplyOrdering() {
        return super.applyOrdering;
    }

    public void setOptionApplyOrdering(boolean applyOrdering) {
        super.applyOrdering = applyOrdering;
    }
  */

    public boolean getOptionShowSequences() {
        return super.optionShowSequences;
    }

    public void setOptionShowSequences(boolean labelSequences) {
        super.optionShowSequences = labelSequences;
    }
}
