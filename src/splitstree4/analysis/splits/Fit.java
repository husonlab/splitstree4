/*
 * Fit.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.splits;


import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

/**
 * Computes the fit of the (weighted) splits to the distance matrix. Returns result.
 */
public class Fit implements SplitsAnalysisMethod {
    /**
	 * implementations of analysis-algorithms should overwrite this
	 * String with a short description of what they do.
	 */
	public static final String DESCRIPTION = "Compute the fit and the LS fit of a distance matrix and a set of splits";

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
