/**
 * SaturatedDistancesException.java
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
 * @author miguel
 * <p/>
 * Created on Aug 6, 2004
 */
/**
 * @author miguel
 *
 * Created on Aug 6, 2004
 *
 */
package splitstree4.algorithms.characters;

/**
 * Saturated distances exception object
 */
public class SaturatedDistancesException extends Exception {

    /**
     * Constructor for the exception
     */
    public SaturatedDistancesException() {
        super();
    }

    /**
     * Constructor for the exception
     *
     * @param str
     */
    public SaturatedDistancesException(String str) {
        super(str);
    }
}
