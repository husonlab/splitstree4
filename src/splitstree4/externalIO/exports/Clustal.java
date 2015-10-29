/**
 * Clustal.java
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

import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;

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
