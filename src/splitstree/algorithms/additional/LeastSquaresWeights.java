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

package splitstree.algorithms.additional;

import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree.algorithms.util.LeastSquares;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

/**
 * wrapper for the least squares computations
 *
 * @author huson
 *         Date: 17-Feb-2004
 */
public class LeastSquaresWeights implements Splits2Splits {

    public final static String DESCRIPTION = "Compute least squares weights";
    private boolean optionConstrain = true;

    /**
     * can we apply least squares weight modification?
     *
     * @param doc    the document
     * @param taxa   the taxa
     * @param splits the split
     * @return true, if split2splits transform applicable?
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValidByName(Distances.NAME) && doc.isValid(taxa) && doc.isValid(splits);
    }

    /**
     * apply least squares weight modification
     *
     * @param doc    the document
     * @param taxa   the taxa
     * @param splits the splits
     * @throws jloda.util.CanceledException
     */
    public void apply(Document doc, Taxa taxa, Splits splits) throws CanceledException {
        System.err.println("Computing least squares...");
        doc.notifySetMaximumProgress(100);
        /*
        try {
            Writer w = new StringWriter();
            splits.write(w, doc.getTaxa());
            System.err.println(w.toString());
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        */
        doc.notifySetProgress(50);
        try {
            LeastSquares.optimizeLS(splits, doc.getDistances(), getOptionConstrain());
            SplitsUtilities.computeFits(true, splits, doc.getDistances(), doc);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        doc.notifySetProgress(100);
        /*
        try {
            Writer w = new StringWriter();
            splits.write(w, doc.getTaxa());
            System.err.println(w.toString());
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        */
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * get constrained optimization?
     *
     * @return flag indicating whether to use constrained least squares.(true = constrained)
     */
    public boolean getOptionConstrain() {
        return optionConstrain;
    }

    /**
     * set constrained optimization
     *
     * @param optionConstrain, flag indicating whether to use constrained least squares (true = constrained)
     */
    public void setOptionConstrain(boolean optionConstrain) {
        this.optionConstrain = optionConstrain;
    }
}
