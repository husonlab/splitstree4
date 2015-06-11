/**
 * NoGraph.java 
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
package splitstree.algorithms.splits;

import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * does not compute a graph
 *
 * @author huson
 *         Date: 10-Aug-2004
 */
public class NoGraph implements Splits2Network {
    final public static String DESCRIPTION = "Do nothing, don't build a graph";

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        Network network = new Network();
        network.setNtax(taxa.getNtax());
        return network;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return taxa != null && splits != null;
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }
}
