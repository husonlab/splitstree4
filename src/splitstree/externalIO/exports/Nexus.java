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
import splitstree.core.SplitsException;
import splitstree.nexus.Characters;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;


/**
 * save blocks in nexus format
 */
public class Nexus extends ExporterAdapter implements Exporter {

    private String Description = "Save blocks in Nexus format";

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
     * @param doc
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection blocks) throws Exception {
        w.write("#nexus\n");
        for (Object block : blocks) {
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
