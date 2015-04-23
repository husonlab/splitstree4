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
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * DESCRIPTION
 * Daniel Huson and David Bryant
 */
public class FastASplits extends ExporterAdapter implements Exporter {

    private String Description = "Exports splits as binary sequences in FastA format.";

    /**
     * can we export this data?
     *
     * @param doc param blocks
     * @return true, if can handle this export
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (blocks.size() != 1 || !blocks.contains(Splits.NAME))
            return false;
        return !(doc != null && !doc.isValidByName(Splits.NAME));
    }

    /**
     * exports splits in fasta format
     *
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection notUsed) throws Exception {
        Taxa taxa = doc.getTaxa();
        Splits splits = doc.getSplits();

        jloda.util.FastA fasta = new jloda.util.FastA();
        for (int t = 1; t <= taxa.getNtax(); t++) {
            char[] seq = new char[splits.getNsplits()];
            for (int s = 1; s <= splits.getNsplits(); s++) {
                if (splits.get(s).get(1) == splits.get(s).get(t))
                    seq[s - 1] = '1';
                else
                    seq[s - 1] = '0';
            }
            fasta.add(taxa.getLabel(t), String.valueOf(seq));
        }
        fasta.write(w);
        return null;
    }

    public String getDescription() {
        return Description;
    }
}
