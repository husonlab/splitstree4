/*
 * DistancesTransform.java Copyright (C) 2022 Daniel H. Huson
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

import splitstree4.algorithms.Transformation;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * Interface for methods that compute data (e.g. splits or trees) from
 * distances
 */
public interface DistancesTransform extends Transformation {
    String COMMAND = "distTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Distances distances);
}

// EOF
