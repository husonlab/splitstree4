/**
 * ReticulationNetwork2.java
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
package splitstree4.algorithms.characters;

import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.algorithms.splits.GalledNetwork;
import splitstree4.analysis.splits.Stats;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

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
        PhyloSplitsGraph graph = (PhyloSplitsGraph) graphView.getGraph();
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
