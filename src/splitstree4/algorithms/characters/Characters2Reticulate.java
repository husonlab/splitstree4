/**
 * Characters2Reticulate.java
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
package splitstree4.algorithms.characters;

import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Reticulate;
import splitstree4.nexus.Taxa;

/**
 * Interface for methods that computes reticulate from characters
 */
public interface Characters2Reticulate extends CharactersTransform {


    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return the computed reticulate object
     */
    Reticulate apply(Document doc, Taxa taxa, Characters c);
}

// EOF
