/*
 * Unaligned2Tree.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.unaligned;


import splitstree4.core.Document;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.nexus.Unaligned;

/**
 * Interface for methods that compute a tree from unaligned sequences
 */
public interface Unaligned2Tree extends UnalignedTransform {
    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param data the data matrix
     * @return the computed tree
     */
    Trees apply(Document doc, Taxa taxa, Unaligned data);
}

// EOF
