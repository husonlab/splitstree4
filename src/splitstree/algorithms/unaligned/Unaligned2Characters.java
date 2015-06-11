/**
 * Unaligned2Characters.java 
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
package splitstree.algorithms.unaligned;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

/**
 * Interface for methods that compute characters from unaligned sequences
 */
public interface Unaligned2Characters extends UnalignedTransform {
  
    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned
     * @return the computed characters Object
     */
    Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception;
}

// EOF
