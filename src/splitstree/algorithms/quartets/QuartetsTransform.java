/**
 * QuartetsTransform.java 
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
 * @version $Id: QuartetsTransform.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.algorithms.quartets;


import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Quartets;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute data (e.g. splits or trees) from
 * quartets
 */
public interface QuartetsTransform extends Transformation {

    String COMMAND = "quartTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa     the taxa
     * @param quartets the quartets
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Quartets quartets);
}

// EOF
