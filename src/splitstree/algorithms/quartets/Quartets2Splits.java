/**
 * Quartets2Splits.java 
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
/**
 * @version $Id: Quartets2Splits.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.algorithms.quartets;


import splitstree.core.Document;
import splitstree.nexus.Quartets;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute splits from quartets
 */
public interface Quartets2Splits extends QuartetsTransform {
 
    /**
     * Applies the method to the given data
     *
     * @param taxa     the taxa
     * @param quartets the given quartets
     * @return the computed quartets
     */
    Splits apply(Document doc, Taxa taxa, Quartets quartets);
}

// EOF
