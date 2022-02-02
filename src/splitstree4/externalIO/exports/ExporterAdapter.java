/*
 * ExporterAdapter.java Copyright (C) 2022 Daniel H. Huson
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

import splitstree4.core.Document;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * DESCRIPTION
 *
 * @author huson
 * Date: 24-Nov-2004
 */
public class ExporterAdapter implements Exporter {

    boolean optionExportAll = true;

    /**
     * Returns true if there is at least one block in goodBlocks that is
     * present and applicable, and there are no blocks in selected that are
     * not in goodBlocks
     *
	 */
    public boolean blocksOK(Document doc, Collection selected, Collection goodBlocks) {
        boolean containsValid = false;

        for (Object aSelected : selected) {
            String block = (String) aSelected;
            if (goodBlocks.contains(block)) {
                if (doc == null || doc.isValidByName(block))
                    containsValid = true;
            } else
                return false; //Block not a good block!!
        }

        return containsValid;
    }

    /**
     * Returns true if all of the blocks in goodBlocks are
     * present and applicable, and there are no blocks selected that are
     * not in goodBlocks
     *
     * @param doc        Document
     * @param selected   Blocks selected in exporter
     * @param goodBlocks Blocks required by this exporter
     * @return boolean true iff all blocks are present and in goodBlocks
     */
    public boolean blocksAllOK(Document doc, Collection<String> selected, Collection<String> goodBlocks) {


        if (doc == null)
            return goodBlocks.isEmpty();
        if (selected.size() != goodBlocks.size())
            return false;

        for (var block : selected) {
            if (!goodBlocks.contains(block) || (!doc.isValidByName(block)))
                return false; //Block not a good block!!
        }
        return true;
    }


    /**
     * can we import this data?
     *
     * @param selected set of selected blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document dp, Collection<String> selected) {
        return false;
    }

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w          The wirter
     * @param dp         The Document
     * @param blockNames list of blocks to exported
     * @return mapping from export names to original names
	 */
    public Map<String, String> apply(Writer w, Document dp, Collection<String> blockNames) throws Exception {
        return null;
    }

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     * The default behavior of the member function is to ignore the additional Info. A class
     * extending ExporterAdapter needs to override this function
     *
     * @param w              The wirter
     * @param dp             The Document
     * @param blockNames     list of blocks to exported
     * @param additionalInfo Additional info required by exporter  (ignored by default)
     * @return mapping from export names to original names
	 */
    public Map<String, String> apply(Writer w, Document dp, Collection<String> blockNames, ExporterInfo additionalInfo) throws Exception {
        return apply(w, dp, blockNames);
    }


    public String getDescription() {
        return Description;
    }

    /**
     * save all sites in sequences, not just active ones
     *
	 */
    public void setOptionExportAll(boolean exportAll) {
        this.optionExportAll = exportAll;
    }

    /**
     * save all sites in sequences, not just active ones
     *
     * @return true, if all sites are to be saved, false else
     */
    public boolean getOptionExportAll() {
        return optionExportAll;
    }

    /**
     * Override this if the exporter has some additional export information (e.g. a dialog).
     *
     * @param doc Document
     * @return ExporterInfo  results from the request: exact type varies by exporter
     */
    public ExporterInfo requestAdditionalInfo(Document doc) {
        return null;
    }

}
