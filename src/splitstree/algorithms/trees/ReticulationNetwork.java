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

import jloda.phylo.PhyloGraphView;
import splitstree.algorithms.splits.ReticulatedEvolutionOnTrees;
import splitstree.analysis.splits.Stats;
import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

/**
 * @deprecated
 * */
public class ReticulationNetwork extends ReticulatedEvolutionOnTrees implements Trees2Network {
    static final boolean EXPERT = true;

    public final static String MEDIAN = "median";
    public final static String MEAN = "mean";
    public final static String COUNT = "count";
    public final static String SUM = "sum";
    public final static String NONE = "none";
    private String optionEdgeWeights = MEAN;
    private double threshold = 0.00;

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees the input trees
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Trees trees) throws Exception {
        // first make splits from a set of (partial) Trees
        Splits splits;
        if (trees.getPartial()) {
            SuperNetwork zc = new SuperNetwork();
            splits = zc.apply(doc, taxa, trees);
        } else {
            ConsensusNetwork cn = new ConsensusNetwork();
            cn.setOptionThreshold(threshold);
            splits = cn.apply(doc, taxa, trees);
        }

        // compute cycle and stuff:
        doc.setSplits(splits);
        (new Stats()).apply(doc, taxa, splits);
        doc.setSplits(null);
        //super.labelSplits=true;
        super.labelSequences = false;
        //super.applyOrdering = true;
        super.setOptionMaxReticulationsPerTangle(10);
        Network net = apply(doc, taxa, splits);
        PhyloGraphView graphView = new PhyloGraphView();
        net.syncNetwork2PhyloGraphView(doc.getTaxa(), doc.getSplits(), graphView);
        //PhyloGraph graph = (PhyloGraph) graphView.getGraph();
        //writeLabels2Edges(graphView, graph,doc.getSplits());
        return net;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param trees the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees);
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

    public boolean getOptionLabelSplits() {
        return super.labelSplits;
    }

    public void setOptionLabelSplits(boolean labelSplits) {
        super.labelSplits = labelSplits;
    }

    public boolean getOptionApplyOrdering() {
        return super.applyOrdering;
    }

    public void setOptionApplyOrdering(boolean applyOrdering) {
        super.applyOrdering = applyOrdering;
    }

    public boolean getOptionLabelSequences() {
        return super.labelSequences;
    }

    public void setOptioLabelSequences(boolean labelSequences) {
        super.labelSequences = labelSequences;
    }
}
