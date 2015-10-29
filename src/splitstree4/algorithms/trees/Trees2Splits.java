/**
 * Trees2Splits.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id: Trees2Splits.java,v 1.6 2006-05-23 05:57:33 huson Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: Trees2Splits.java,v 1.6 2006-05-23 05:57:33 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.algorithms.trees;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

/**
 * Interface for methods that compute splits from a tree
 */
public interface Trees2Splits extends TreesTransform {
    /**
     * Applies the method to the given data
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return the computed set of splits
     */
    Splits apply(Document doc, Taxa taxa, Trees trees) throws SplitsException, CanceledException;
}

// EOF
