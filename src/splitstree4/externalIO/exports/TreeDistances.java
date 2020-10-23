/*
 *  Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.externalIO.exports;

import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Exports a Trees block or compatible Splits block in Newick format
 */
public class TreeDistances extends ExporterAdapter implements Exporter {

    public String Description = "Exports trees as average pairwise distances.";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (doc == null || blocks.size() != 1)
            return false;
        if (!blocks.contains(Splits.NAME) && blocks.contains(Trees.NAME) && !blocks.contains(Network.NAME)) {
            if (doc.isValidByName(Trees.NAME))
                return true;
        }
        return false;
    }


    /**
     * convert input into tree format
     *
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection blockNames) throws Exception {
        if (blockNames.contains(Trees.NAME)) {
            final Distances distances = TreesUtilities.getAveragePairwiseDistances(doc.getTaxa(), doc.getTrees());
            w.write(distances.getNtax() + "\n");
            for (int i = 1; i <= distances.getNtax(); i++) {
                w.write(doc.getTaxa().getLabel(i));
                for (int j = 1; j <= distances.getNtax(); j++) {
                    w.write(String.format(" %.6g", distances.get(i, j)));
                }
                w.write("\n");
            }
        }
        return null;
    }
}
