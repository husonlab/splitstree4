/**
 * FastASequences.java
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
import splitstree4.nexus.Characters;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * exports data in fasta format
 *
 * @author huson
 * Date: 11-Aug-2004
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
    public Map apply(Writer w, Document doc, Collection blockNames) throws Exception {

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

