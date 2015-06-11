/**
 * LeastSquaresWeights.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
