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
 * the exporter interface
 */
public interface Exporter {

    String Description = "No description given.";

    /**
     * can we import this data?
     *
     * @param dp
     * @param selected set of selected blocks
     * @return true, if can handle this import
     */
    boolean isApplicable(Document dp, Collection selected);

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w      The writer
     * @param dp     The Document
     * @param blocks list of blocks to exported
     * @return mapping from export names to original names
     * @throws Exception
     */
    Map apply(Writer w, Document dp, Collection blocks) throws Exception;

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w              The writer
     * @param dp             The Document
     * @param blocks         list of blocks to exported
     * @param additionalInfo Additional info required by exporter
     * @return mapping from export names to original names
     * @throws Exception
     */
    Map apply(Writer w, Document dp, Collection blocks, ExporterInfo additionalInfo) throws Exception;


    /**
     * get a description of this exporter
     *
     * @return description
     */
    String getDescription();

    /**
     * save all data rather than just active data?
     * Mainly used for character sequences. If true, save all sites, otherwise
     * only save unmasked sites
     *
     * @param saveAll
     */
    void setOptionExportAll(boolean saveAll);

    /**
     * save all data rather than active data
     *
     * @return true, if all data to be save rather than active data
     */
    boolean getOptionExportAll();

    /**
     * Ask the user for additional information needed by the exporter
     *
     * @param doc Document
     * @return Class containing additional info
     */
    ExporterInfo requestAdditionalInfo(Document doc);
}
