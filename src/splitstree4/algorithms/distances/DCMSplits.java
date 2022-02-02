/*
 * DCMSplits.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.distances;

import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * implements the disk-covering method for split decomposition
 */
public class DCMSplits implements Distances2Splits {
    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "Run the Disk-Covering method (Huson, Nettles, Warnow, 1999) [Not implemented]";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return false;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param distances the input distances
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances distances) throws Exception {

        return null;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}
