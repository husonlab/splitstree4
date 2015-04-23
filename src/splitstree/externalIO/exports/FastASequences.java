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
import splitstree.nexus.Characters;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * exports data in fasta format
 *
 * @author huson
 *         Date: 11-Aug-2004
 */
public class FastASequences extends ExporterAdapter implements Exporter {

    private String Description = "Exports character sequences in FastA format.";

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
     * convert input into nexus format
     *
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection notUsed) throws Exception {

        Characters chars = doc.getCharacters();

        // decide whether to save all sites or only unmasked ones:
        if (getOptionExportAll()
                || chars.getMask() == null || chars.getNactive() == chars.getNchar()) {
            System.err.println("# Exporting all sites");
            jloda.util.FastA fasta = new jloda.util.FastA();
            for (int t = 1; t <= doc.getTaxa().getNtax(); t++) {
                char[] seq = new char[chars.getNchar()];
                for (int c = 1; c <= chars.getNchar(); c++)
                    seq[c - 1] = chars.get(t, c);
                fasta.add(doc.getTaxa().getLabel(t), String.valueOf(seq));
            }
            fasta.write(w);
        } else // export only unmasked sites
        {
            System.err.println("# Exporting active sites");
            int numActive = chars.getNactive();

            jloda.util.FastA fasta = new jloda.util.FastA();
            for (int t = 1; t <= doc.getTaxa().getNtax(); t++) {
                char[] seq = new char[numActive];
                for (int cOrig = 1, c = 0; c < numActive; c++, cOrig++) {
                    while (cOrig <= chars.getNchar() && chars.isMasked(cOrig))
                        cOrig++;
                    seq[c] = doc.getCharacters().get(t, cOrig);
                }
                fasta.add(doc.getTaxa().getLabel(t), String.valueOf(seq));
            }
            fasta.write(w);
        }
        return null;
    }

    public String getDescription() {
        return Description;
    }
}

