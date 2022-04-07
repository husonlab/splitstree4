/*
 * Distances2Trees.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree4.algorithms.distances;

import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.io.IOException;

/**
 * Interface for methods that compute a tree from distances
 */
public interface Distances2Trees extends DistancesTransform {

    /**
     * Applies the method to the given data
     *
     * @param t the input taxa
     * @param d the input distances
     * @return the computed phylogenetic tree as a nexus Trees object
     */
    Trees apply(Document doc, Taxa t, Distances d) throws IOException;
}

// EOF
