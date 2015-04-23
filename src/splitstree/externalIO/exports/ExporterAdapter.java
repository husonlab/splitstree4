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

package splitstree.externalIO.exports;

import splitstree.core.Document;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * DESCRIPTION
 *
 * @author huson
 *         Date: 24-Nov-2004
 */
public class ExporterAdapter implements Exporter {

    boolean optionExportAll = true;

    /**
     * Returns true if there is at least one block in goodBlocks that is
     * present and applicable, and there are no blocks in selected that are
     * not in goodBlocks
     *
     * @param doc
     * @param selected
     * @param goodBlocks
     * @return
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
    public boolean blocksAllOK(Document doc, Collection selected, Collection goodBlocks) {


        if (doc == null)
            return goodBlocks.isEmpty();
        if (selected.size() != goodBlocks.size())
            return false;

        for (Object aSelected : selected) {
            String block = (String) aSelected;
            if (!goodBlocks.contains(block) || (!doc.isValidByName(block)))
                return false; //Block not a good block!!
        }
        return true;
    }


    /**
     * can we import this data?
     *
     * @param dp
     * @param selected set of selected blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document dp, Collection selected) {
        return false;
    }

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w      The wirter
     * @param dp     The Document
     * @param blocks list of blocks to exported
     * @return mapping from export names to original names
     * @throws java.lang.Exception
     */
    public Map apply(Writer w, Document dp, Collection blocks) throws Exception {
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
     * @param blocks         list of blocks to exported
     * @param additionalInfo Additional info required by exporter  (ignored by default)
     * @return mapping from export names to original names
     * @throws java.lang.Exception
     */
    public Map apply(Writer w, Document dp, Collection blocks, ExporterInfo additionalInfo) throws Exception {
        return apply(w, dp, blocks);
    }


    public String getDescription() {
        return Description;
    }

    /**
     * save all sites in sequences, not just active ones
     *
     * @param exportAll
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
