/*
 * Exporter.java Copyright (C) 2022 Daniel H. Huson
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
    boolean isApplicable(Document dp, Collection<String> selected);

    /**
     * Writes the Data to the writer w. If the exporter only handles subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w          The writer
     * @param dp         The Document
     * @param blockNames list of blocks to exported
     * @return mapping from export names to original names
     * @throws Exception
     */
    Map<String, String> apply(Writer w, Document dp, Collection<String> blockNames) throws Exception;

    /**
     * Writes the Data to the writer w. If the exporter only handles subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w              The writer
     * @param dp             The Document
     * @param blockNames     list of blocks to exported
     * @param additionalInfo Additional info required by exporter
     * @return mapping from export names to original names
     * @throws Exception
     */
    Map<String, String> apply(Writer w, Document dp, Collection<String> blockNames, ExporterInfo additionalInfo) throws Exception;

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
