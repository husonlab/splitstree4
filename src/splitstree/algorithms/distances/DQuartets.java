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
 * returns all quartets that have positive isolation index
 * @author Daniel Huson and David Bryant
 * @version $Id: DQuartets.java,v 1.11 2007-09-11 12:31:06 kloepper Exp $
 * 8.03
 */
package splitstree.algorithms.distances;

import splitstree.core.Document;
import splitstree.core.Quartet;
import splitstree.nexus.Distances;
import splitstree.nexus.Quartets;
import splitstree.nexus.Taxa;

/**
 * returns all quartets that have positive isolation index
 */
public class DQuartets implements Distances2Quartets {
    private double threshold = 0;
    public final static String DESCRIPTION = "Compute all quartets with positive isolation index";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return taxa != null && distances != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param distances
     * @return the computed set of splits
     */
    public Quartets apply(Document doc, Taxa taxa, Distances distances) throws Exception {
        Quartets quartets = new Quartets();
        // ProgressDialog pd = new ProgressDialog("D Quartets...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(taxa.getNtax());    //initialize maximum progress
        doc.notifySetProgress(0);
        for (int i = 1; i <= taxa.getNtax(); i++) {
            for (int j = i; j <= taxa.getNtax(); j++) {
                for (int k = 1; k <= taxa.getNtax(); k++)
                    if (k != i && k != j) {
                        for (int m = k; m <= taxa.getNtax(); m++) {
                            double alpha = SplitDecomposition.getIsolationIndex(i, j, k, m, distances);
                            if (alpha > threshold) {
                                quartets.add(new Quartet(i, j, k, m, alpha, null));
                            }
                        }
                    }
            }
            doc.notifySetProgress(i);
        }
        doc.notifySetProgress(taxa.getNtax());   //set progress to 100%	
        // pd.close();								//get rid of the progress listener
        // // doc.setProgressListener(null);
        return quartets;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * get the threshold that isolation index has to exceed
     *
     * @return threshold
     */
    public double getOptionthreshold() {
        return threshold;
    }

    /**
     * set the threshold that the isolation index has to exceed
     *
     * @param threshold
     */
    public void setOptionthreshold(double threshold) {
        this.threshold = threshold;
    }
}
