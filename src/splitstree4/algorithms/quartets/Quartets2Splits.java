/**
 * Quartets2Splits.java
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
 * @version $Id: Quartets2Splits.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * @version $Id: Quartets2Splits.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: Quartets2Splits.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.algorithms.quartets;


import splitstree4.core.Document;
import splitstree4.nexus.Quartets;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * Interface for methods that compute splits from quartets
 */
public interface Quartets2Splits extends QuartetsTransform {

    /**
     * Applies the method to the given data
     *
     * @param taxa     the taxa
     * @param quartets the given quartets
     * @return the computed quartets
     */
    Splits apply(Document doc, Taxa taxa, Quartets quartets);
}

// EOF
