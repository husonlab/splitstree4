/*
 * ReticulationNetwork.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.swing.graphview.PhyloGraphView;
import splitstree4.algorithms.splits.ReticulatedEvolutionOnTrees;
import splitstree4.analysis.splits.Stats;
import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.io.IOException;

/**
 * @deprecated
 */
public class ReticulationNetwork extends ReticulatedEvolutionOnTrees implements Trees2Network {
	static final boolean EXPERT = true;

	public final static String MEDIAN = "median";
	public final static String MEAN = "mean";
	public final static String COUNT = "count";
	public final static String SUM = "sum";
	public final static String NONE = "none";
	private final String optionEdgeWeights = MEAN;
	private final double threshold = 0.00;

	/**
	 * Applies the method to the given data
	 *
	 * @param taxa  the taxa
	 * @param trees the input trees
	 * @return the computed set of splits
	 */
	public Network apply(Document doc, Taxa taxa, Trees trees) throws IOException {
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
