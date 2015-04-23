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
import splitstree.nexus.Taxa;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: Jan 20, 2004
 * Time: 9:10:26 PM
 * To change this template use Options | File Templates.
 */
public class Clustal extends ExporterAdapter implements Exporter {

    private String Description = "Exports sequences in Clustal format.";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (blocks.size() != 1 || !blocks.contains(Characters.NAME))
            return false;
        return !(doc != null && !doc.isValidByName(Characters.NAME));
    }

    /**
     * convert input into ClustalW format
     *
     * @return
     */
    public Map apply(Writer w, Document dp, Collection blocks) throws Exception {
        int maxNameLength = 0;
        Taxa taxa = dp.getTaxa();
        Characters chars = dp.getCharacters();
        for (int i = 1; i <= taxa.getNtax(); i++)
            if (maxNameLength < taxa.getLabel(i).length()) maxNameLength = taxa.getLabel(i).length();


        w.write("CLUSTAL X (1.82) multiple sequence alignment\n\n\n");

        if (getOptionExportAll()
                || chars.getMask() == null || chars.getNactive() == chars.getNchar()) {
            for (int i = 0; i <= (chars.getNchar() / 60); i++) {
                for (int j = 1; j <= taxa.getNtax(); j++) {
                    StringBuilder tmp = new StringBuilder(taxa.getLabel(j));
                    while (tmp.length() < maxNameLength + 6) tmp.append(" ");
                    w.write(tmp.toString());
                    for (int k = i * 60; k < (i + 1) * 60 && k < chars.getNchar(); k++) {
                        w.write(chars.get(j, k + 1));
                    }
                    w.write("\n");
                }
                w.write("\n\n");
            }
        } else // save only unmasked sites:
        {
            int numActive = chars.getNactive();
            int total = 0;
            int iOrig = 1;
            for (int block = 0; block <= (numActive / 60); block++) {
                while (iOrig <= chars.getNchar() && chars.isMasked(iOrig))
                    iOrig++;
                if (iOrig > chars.getNchar())
                    break;
                int kOrig = chars.getNchar(); // really gets set in loop:
                for (int j = 1; j <= taxa.getNtax(); j++) {
                    StringBuilder tmp = new StringBuilder(taxa.getLabel(j));
                    while (tmp.length() < maxNameLength + 6) tmp.append(" ");
                    w.write(tmp.toString());
                    int cols = 0;
                    kOrig = iOrig;
                    while (++cols <= 60) {
                        while (kOrig <= chars.getNchar() && chars.isMasked(kOrig))
                            kOrig++;
                        if (kOrig > chars.getNchar())
                            break;
                        w.write(chars.get(j, kOrig));
                        if (j == taxa.getNtax())
                            total++;
                        kOrig++;
                    }
                    w.write("\n");
                }
                iOrig = kOrig;
                w.write("\n\n");
            }
            if (total != chars.getNactive())
                throw new SplitsException("Export failed (internal error: " + total + "!=" + chars.getNactive() + ")");
        }
        return null;
    }


    public String getDescription() {
        return Description;
    }

}
