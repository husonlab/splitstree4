/**
 * SplitsTransform.java 
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
/*
* $Id: SplitsTransform.java,v 1.18 2006-05-23 05:57:33 huson Exp $
*/
package splitstree.algorithms.splits;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute data (eg network)
 * from splits
 */
public interface SplitsTransform extends Transformation {
    String COMMAND = "splitsTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Splits splits);
}

// EOF
