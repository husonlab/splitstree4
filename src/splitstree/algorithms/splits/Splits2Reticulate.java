/**
 * Splits2Reticulate.java 
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
package splitstree.algorithms.splits;

import splitstree.core.Document;
import splitstree.nexus.Reticulate;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute an reticulate object from splits
 */
public interface Splits2Reticulate extends SplitsTransform {
    /** Applies the method to the given data
     *@param taxa the taxa
     *@param splits the splits
     *@return the computed set of splits
     */
    Reticulate apply(Document doc, Taxa taxa, Splits splits);
}
