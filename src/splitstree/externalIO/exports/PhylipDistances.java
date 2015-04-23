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

import jloda.util.PhylipUtils;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * export a distance matrix in phylip format
 *
 * @author huson
 *         Date: 21-Nov-2004
 */
public class PhylipDistances extends ExporterAdapter implements Exporter {
    private String[] taxon2phylip;
    private Map phylip2taxon;

    public PhylipDistances() {
    }

    private String Description = "Exports distances in Phylip format";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (blocks.size() != 1 || !blocks.contains(Distances.NAME))
            return false;
        return !(doc != null && !doc.isValidByName(Distances.NAME));
    }

    /**
     * convert input into phylip format
     *
     * @return
     */
    public Map apply(Writer w, Document doc, Collection notUsed) throws Exception {
        Taxa taxa = doc.getTaxa();
        Distances distances = doc.getDistances();

        taxon2phylip = new String[taxa.getNtax() + 1];
        phylip2taxon = new HashMap();

        for (int t = 1; t <= taxa.getNtax(); t++) {
            String name = taxa.getLabel(t);
            if (name.length() > 10)
                name = name.substring(0, 10);
            int n = 1;
            while (phylip2taxon.containsKey(name)) {
                if (n < 10)
                    name = name.substring(0, 7) + "00" + n;
                else if (n < 100)
                    name = name.substring(0, 7) + "0" + n;
                else if (n < 1000)
                    name = name.substring(0, 7) + n;
                else
                    throw new Exception("Can't resolve name conflicts");
            }
            phylip2taxon.put(name, taxa.getLabel(t));
            taxon2phylip[t] = name;
        }

        w.write(" " + taxa.getNtax() + "\n");
        for (int t = 1; t <= taxa.getNtax(); t++) {
            w.write(PhylipUtils.padLabel(taxon2phylip[t], 10));
            for (int t2 = 1; t2 <= taxa.getNtax(); t2++) {
                if (t2 > 1)
                    w.write(" ");
                w.write("" + distances.get(t, t2));
            }
            w.write("\n");
        }

        return phylip2taxon;
    }

    /**
     * gets the phylip label used for the named taxon
     *
     * @param t
     * @return phylip label
     */
    public String taxonId2phylipLabel(int t) {
        return taxon2phylip[t];

    }

    /**
     * gets the taxon that corresponds to the given phylip name
     *
     * @param label
     * @return original label
     */
    public String phylipLabel2taxonId(String label) {
        return (String) phylip2taxon.get(label);

    }

    public String getDescription() {
        return Description;
    }

}
