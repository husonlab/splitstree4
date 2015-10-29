/**
 * Unaligned2Splits.java
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
package splitstree4.algorithms.unaligned;

import splitstree4.core.Document;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Unaligned;

/**
 * Interface for methods that compute splits from unaligned
 */
public interface Unaligned2Splits extends UnalignedTransform {
    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param data the unaligned matrix
     * @return the computed set of splits
     */
    Splits apply(Document doc, Taxa taxa, Unaligned data);
}

// EOF
