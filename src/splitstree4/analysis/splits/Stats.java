/*
 * Stats.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.io.IOException;

/**
 * Basic statistics for splits
 */
public class Stats implements SplitsAnalysisMethod {
    /**
	 * implementations of analysis-algorithms should overwrite this
	 * String with a short description of what they do.
	 */
	public static final String DESCRIPTION = "Compute basic stats for splits";

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
        return taxa != null && splits != null;
    }

    /**
     * Runs the analysis
     *
     * @param taxa   the taxa
     * @param splits the block
     */
    public String apply(Document doc, Taxa taxa, Splits splits) throws IOException {
        if (splits.getCycle() == null || splits.getCycle()[1] == 0)
            SplitsUtilities.computeCycle(doc, taxa, splits, doc.getAssumptions().getLayoutStrategy());

        String result = "Splits: ";
        if (splits.getProperties().getCompatibility() ==
            Splits.Properties.UNKNOWN
            && SplitsUtilities.isCompatible(splits)) {
            splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
        } else if (splits.getProperties().getCompatibility() ==
                   Splits.Properties.UNKNOWN
                && SplitsUtilities.isCyclic(doc, taxa, splits, doc.getAssumptions().getLayoutStrategy())) {
            splits.getProperties().setCompatibility(Splits.Properties.CYCLIC);
        } else if (splits.getProperties().getCompatibility() ==
                Splits.Properties.UNKNOWN
                && SplitsUtilities.isWeaklyCompatible(splits)) {
            splits.getProperties().setCompatibility(Splits.Properties.WEAKLY_COMPATIBLE);
        } else if (splits.getProperties().getCompatibility() ==
                Splits.Properties.UNKNOWN) {
            splits.getProperties().setCompatibility(Splits.Properties.INCOMPATIBLE);
        }

        if (splits.getProperties().getCompatibility() ==
                Splits.Properties.COMPATIBLE)

            result += "compatible\n";
        else if (splits.getProperties().getCompatibility() ==
                Splits.Properties.CYCLIC)
            result += "cyclic\n";
        else if (splits.getProperties().getCompatibility() ==
                Splits.Properties.WEAKLY_COMPATIBLE)
            result += "weakly compatible\n";
        else if (splits.getProperties().getCompatibility() ==
                Splits.Properties.INCOMPATIBLE)
            result += "incompatible\n";

        return result;
    }
}

// EOF
