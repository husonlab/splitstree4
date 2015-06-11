/**
 * Reticulate2Network.java 
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
package splitstree.algorithms.reticulate;

import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Reticulate;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that computes a network object from a reticulate object
 */
public interface Reticulate2Network extends ReticulateTransform {
    /** Applies the method to the given data
     *@param taxa the taxa
     *@param ret the reticulate
     *@return the computed set of splits
     */
    Network apply(Document doc, Taxa taxa, Reticulate ret) throws Exception;
}
