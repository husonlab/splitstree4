/**
 * Distances2Trees.java 
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
 * @version $Id: Distances2Trees.java,v 1.2 2007-09-11 12:31:06 kloepper Exp $
 *
 * @author Christian Rausch
 *
 */

package splitstree.algorithms.distances;

import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

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
    Trees apply(Document doc, Taxa t, Distances d) throws Exception;
}

// EOF
