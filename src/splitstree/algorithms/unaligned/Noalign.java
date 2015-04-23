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

package splitstree.algorithms.unaligned;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

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
