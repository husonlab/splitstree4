/*
 * NewickTree.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.externalIO.exports;

import jloda.phylo.PhyloTree;
import splitstree4.core.Document;
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
public class NewickTree extends ExporterAdapter implements Exporter {

    public String Description = "Exports trees in Newick format.";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (doc == null || blocks.size() != 1)
            return false;

        if (blocks.contains(Splits.NAME) && !blocks.contains(Trees.NAME)
                && !blocks.contains(Network.NAME)) {
			return doc.isValidByName(Splits.NAME) &&
				   doc.getSplits().getProperties().getCompatibility() ==
				   Splits.Properties.COMPATIBLE;
		} else if (!blocks.contains(Splits.NAME) && blocks.contains(Trees.NAME)
                && !blocks.contains(Network.NAME)) {
			return doc.isValidByName(Trees.NAME);
        } else if (!blocks.contains(Splits.NAME) && !blocks.contains(Trees.NAME)
                && blocks.contains(Network.NAME)) {
            System.err.println("tree " + doc.getNetwork().getNewick());
			return doc.isValidByName(Network.NAME) && doc.getNetwork().getNewick() != null;
        }
        return false;
    }


    /**
     * convert input into tree format
     *
     * @return null
     */
    public Map<String, String> apply(Writer w, Document doc, Collection<String> blockNames) throws Exception {
        if (blockNames.contains(Trees.NAME)) {
            for (int i = 1; i <= doc.getTrees().getNtrees(); i++) {
                w.write(doc.getTrees().getTree(i).toString(doc.getTrees().getTranslate()) + ";\n");
            }
        } else if (blockNames.contains(Splits.NAME) && doc.getSplits().getProperties().getCompatibility() == Splits.Properties.COMPATIBLE) {
            PhyloTree tree = TreesUtilities.treeFromSplits(doc.getTaxa(), doc.getSplits(), null);
			w.write(tree + "\n");
        } else if (blockNames.contains(Network.NAME)) {
            w.write(doc.getNetwork().getNewick() + "\n");
        }
        return null;
    }
}
