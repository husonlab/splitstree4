/**
 * Characters2Trees.java 
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
package splitstree.algorithms.characters;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

/**
 * Interface for methods that compute a tree from characters
 */
public interface Characters2Trees extends CharactersTransform {

    /**
     * Applies the method to the given data
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed tree
     */
    Trees apply(Document doc, Taxa taxa, Characters chars) throws Exception;
}

// EOF
