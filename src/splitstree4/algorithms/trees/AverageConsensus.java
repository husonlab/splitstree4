/*
 * AverageConsensus.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.algorithms.distances.NeighborNet;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import java.io.IOException;
import java.io.StringWriter;

/**
 * constructs a network from the average pairwise distances in the trees.
 */
public class AverageConsensus implements Trees2Splits {

    private boolean analyseDistances = false;

    public final static String DESCRIPTION = "Constructs a Neighbor-Net from the average pairwise distances in the trees";

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
        // ProgressDialog pd = new ProgressDialog("Consensus Network...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(0);
        Distances dist = TreesUtilities.getAveragePairwiseDistances(taxa, trees);

        if (analyseDistances) {
            try {
				StringWriter sw = new StringWriter();
				doc.getTaxa().write(sw);
				dist.write(sw, doc.getTaxa());
				Director newDir = Director.newProject(sw.toString(), doc.getFile().getAbsolutePath());
				newDir.getDocument().setTitle("Average path-length distance for " + doc.getTitle());
				newDir.showMainViewer();

			} catch (IOException ignored) {
			}
        }

        StringWriter sw = new StringWriter();
        dist.write(sw, taxa);

		System.out.println(sw);

        NeighborNet nnet = new NeighborNet();
        return nnet.apply(doc, taxa, dist);
    }


    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public boolean getOptionAnalyseDistances() {
        return analyseDistances;
    }

    public void setOptionAnalyseDistances(boolean analyseDistances) {
        this.analyseDistances = analyseDistances;
    }
}
