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

/** $Id: Fit.java,v 1.2 2010-05-31 04:27:41 huson Exp $
 */
package splitstree.analysis.splits;


import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

/**
 * Computes the fit of the (weighted) splits to the distance matrix. Returns result.
 */
public class Fit implements SplitsAnalysisMethod {
    /**
     * implementations of analysis-algorithms should overwrite this
     * String with a short description of what they do.
     */
    public static String DESCRIPTION = "Compute the fit and the LS fit of a distance matrix and a set of splits";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return (taxa != null && splits != null && doc.isValidByName(Distances.NAME));
    }

    /**
     * Runs the analysis
     *
     * @param doc    document. Assumed to have a valid distances block
     * @param taxa   the taxa
     * @param splits the block
     */
    public String apply(Document doc, Taxa taxa, Splits splits) throws Exception {

        SplitsUtilities.computeFits(false, splits, doc.getDistances(), doc);
        String result = "";

        //An invalid or non-computed fit is indicated by setting the fit to -1.
        if (splits.getProperties().getFit() >= 0.0)
            result += "Fit: " + splits.getProperties().getFit();
        if (splits.getProperties().getLSFit() >= 0.0)
            result += " LSFit: " + splits.getProperties().getLSFit();

        return result;
    }
}
