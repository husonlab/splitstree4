/*
 * ClustalW.java Copyright (C) 2022 Daniel H. Huson
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
/**
 * runs clustalw externally
 * @version $Id: ClustalW.java,v 1.5 2007-09-11 12:31:02 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * 7.03
 */
package splitstree4.algorithms.characters;

import splitstree4.algorithms.unaligned.Unaligned2Characters;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Unaligned;

/**
 * runs clustalw externally
 */
public class ClustalW implements Unaligned2Characters {

    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "Externally runs ClustalW [not implemented]";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned data
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned unaligned) {
        return false;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned
     * @return the computed characters Object
     */
    public Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception {
        return null;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}
