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

/**
 * implements consensus networks
 * @version $Id: AverageConsensus.java,v 1.1 2008-07-01 19:15:39 bryant Exp $
 * @author Tobias Kloepper and Daniel Huson and David Bryant
 * 7.03
 */
package splitstree.algorithms.trees;

import jloda.util.CanceledException;
import splitstree.algorithms.distances.NeighborNet;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;
import splitstree.util.TreesUtilities;

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

            } catch (IOException ex) {
            }
        }

        StringWriter sw = new StringWriter();
        dist.write(sw, taxa);

        System.out.println(sw.toString());

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
