/*
 * TaxaUtilities.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree4.util;

import splitstree4.nexus.Taxa;

/**
 * Methods for analyzing the Taxa class
 */
public class TaxaUtilities {
    /**
     * Get the max length of all the labels.
     *
     * @return longer the max length.
     */
    public static int getMaxLabelLength(Taxa taxa) {
        int len;
        int longer = 0;

        for (int i = 1; i <= taxa.getNtax(); i++) {
            len = taxa.getLabel(i).length();
            if (longer < len) {
                longer = len;
            }
        }
        return longer;
    }

}

// EOF
