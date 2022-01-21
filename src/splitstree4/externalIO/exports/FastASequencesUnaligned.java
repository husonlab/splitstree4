/*
 * FastASequencesUnaligned.java Copyright (C) 2022 Daniel H. Huson
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


import jloda.seq.FastA;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Unaligned;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * exports data in fasta format
 *
 * @author huson
 * Date: 11-Aug-2004
 */
public class FastASequencesUnaligned extends ExporterAdapter implements Exporter {

    private String Description = "Exports unaligned sequences in FastA format.";

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

        Unaligned unalign = doc.getUnaligned();

		FastA fasta = new FastA();
        //System.out.println("Length: " + unalign.getMaxLength());
        for (int t = 1; t <= doc.getTaxa().getNtax(); t++) {

            //char[] seq = new char[unalign.getMaxLength()];
            //for (int c = 1; c <=unalign.getMaxLength(); c++){
            char[] seq = new char[unalign.getRow(t).length - 1];
            for (int c = 1; c < unalign.getRow(t).length; c++) {
                seq[c - 1] = unalign.get(t, c);
            }
            fasta.add(doc.getTaxa().getLabel(t), String.valueOf(seq));
        }
        fasta.write(w);

        return null;
    }

    public String getDescription() {
        return Description;
    }
}

