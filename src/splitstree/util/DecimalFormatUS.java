/**
 * DecimalFormatUS.java 
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
package splitstree.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Sep 21, 2005
 * Time: 6:53:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class DecimalFormatUS {
    private String formatString = "";
    private final DecimalFormat decFormat = new DecimalFormat(formatString, new DecimalFormatSymbols(Locale.US));

    public DecimalFormatUS(String formatString) {
        this.formatString = formatString;
    }

    String format(double x) {
        return decFormat.format(x);
    }

    String format(float x) {
        return decFormat.format(x);
    }
}
