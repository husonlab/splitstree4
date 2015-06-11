/**
 * RandomTaxa.java 
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
package splitstree.algorithms.util.simulate;

import splitstree.nexus.Taxa;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Aug 28, 2005
 * Time: 10:21:42 AM
 * <p/>
 * Utility routines for creating synthetic blocks of taxa - for simulation and tests.
 */
public class RandomTaxa {
    static public Taxa generateTaxa(int ntax) {
        return generateTaxa(ntax, "taxon");
    }

    static public Taxa generateTaxa(int ntax, String prefix) {
        Taxa taxa = new Taxa();
        for (int i = 1; i <= ntax; i++) {
            taxa.add(prefix + i);
        }
        return taxa;
    }
}
