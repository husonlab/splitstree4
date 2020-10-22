/**
 * Trees2Network.java
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
 */
/*
 * $Id: Trees2Network.java,v 1.4 2007-09-11 12:31:08 kloepper Exp $
 */
package splitstree4.algorithms.trees;

import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

/**
 * Interface for methods that compute a network from trees
 */
public interface Trees2Network extends TreesTransform {
    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees the trees
     * @return the computed network
     */
    Network apply(Document doc, Taxa taxa, Trees trees) throws Exception;
}

// EOF
