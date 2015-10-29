/**
 * Noalign.java
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
package splitstree4.algorithms.unaligned;

import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Unaligned;

/**
 * Interface for methods that compute characters from unaligned sequences
 */
public class Noalign implements Unaligned2Characters {
    public final static String DESCRIPTION = "Pads all unaligned sequences to same length";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param data the unaligned data
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned data) {
        return true; // this is the default transformer for unaligned, must always work!
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned
     * @return the computed characters Object
     */
    public Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception {

        int maxLength = unaligned.getMaxLength();

        Characters characters = new Characters(unaligned.getNtax(), maxLength);
        characters.getFormat().setDatatype(unaligned.getFormat().getDatatype());
        characters.getFormat().setRespectCase(unaligned.getFormat().getRespectCase());
        characters.getFormat().setMissing(unaligned.getFormat().getMissing());
        characters.getFormat().setSymbols(unaligned.getFormat().getSymbols());
        characters.getFormat().setLabels(unaligned.getFormat().getLabels());
        characters.getFormat().setGap('-');

        //characters.resetMatrix(unaligned.getNtax(), maxLength);    No longer necessary... changed constructor

        for (int i = 1; i <= unaligned.getNtax(); i++) {
            char[] row = unaligned.getRow(i);
            for (int c = 1; c <= maxLength; c++) {
                if (c < row.length)
                    characters.set(i, c, row[c]);
                else
                    characters.set(i, c, '-');
            }
        }
        return characters;
    }
}

// EOF
