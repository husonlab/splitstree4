/*
 * Nexus.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.core.SplitsException;
import splitstree4.nexus.Characters;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;


/**
 * save blocks in nexus format
 */
public class Nexus extends ExporterAdapter implements Exporter {

    private final String Description = "Save blocks in Nexus format";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        for (Object block : blocks) {
            String name = (String) block;
            if (doc != null && !doc.isValidByName(name))
                return false;
        }
        return blocks.size() > 0;
    }


    /**
     * convert input into nexus format
     *
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection blockNames) throws Exception {
        w.write("#nexus\n");
        for (Object block : blockNames) {
            String blockName = (String) block;
            {
                if (blockName.equals(Characters.NAME) && !getOptionExportAll()
                        && doc.getCharacters().getMask() != null) {
                    Characters tmpChars = doc.getCharacters().clone(doc.getTaxa());
                    tmpChars.removeMaskedSites(doc.getCharacters().getMask());
                    tmpChars.write(w, doc.getTaxa());
                } else if (!doc.write(w, blockName))
                    throw new SplitsException("Unknown block: " + blockName);
            }
        }
        return null;
    }

    public String getDescription() {
        return Description;
    }
}
