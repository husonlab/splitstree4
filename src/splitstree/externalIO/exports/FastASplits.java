/**
 * FastASplits.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
