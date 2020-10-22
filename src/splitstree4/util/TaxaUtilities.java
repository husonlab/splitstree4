/**
 * TaxaUtilities.java
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
 * @version $Id: TaxaUtilities.java,v 1.4 2006-05-23 05:57:37 huson Exp $
 * @author Daniel Huson and David Bryant
 * @version $Id: TaxaUtilities.java,v 1.4 2006-05-23 05:57:37 huson Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: TaxaUtilities.java,v 1.4 2006-05-23 05:57:37 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
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
     * @param taxa
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
