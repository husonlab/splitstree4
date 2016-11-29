/**
 * PhylipDistances.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.externalIO.exports;

import jloda.util.PhylipUtils;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

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
    private Map<String, String> phylip2taxon;
    private boolean useFullNames = false;

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
        phylip2taxon = new HashMap<>();

        for (int t = 1; t <= taxa.getNtax(); t++) {
            String name = taxa.getLabel(t);
            if (!useFullNames) {
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
                    n++;
                }
            }
            phylip2taxon.put(name, taxa.getLabel(t));
            taxon2phylip[t] = name;
        }

        w.write(" " + taxa.getNtax() + "\n");
        for (int t = 1; t <= taxa.getNtax(); t++) {
            if (!useFullNames)
                w.write(PhylipUtils.padLabel(taxon2phylip[t], 10));
            else
                w.write(taxon2phylip[t]);
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

    public boolean isUseFullNames() {
        return useFullNames;
    }

    public void setUseFullNames(boolean useFullNames) {
        this.useFullNames = useFullNames;
    }
}
