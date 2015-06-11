/**
 * Transformation.java 
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
package splitstree.algorithms;

/**
 * @author Daniel Huson and David Bryant, Michael Schr�der
 * @version $Id: Transformation.java,v 1.5 2007-09-11 12:30:59 kloepper Exp $
 */
public interface Transformation {

    /**
     * implementations of algorithms should overwrite this
     * String with a short description of what they do.
     */
    String DESCRIPTION = "No description given for this algorithm.";

    /**
     * implementations of algorithms should overwrite this
     * String with the author's name.
     */
    String CONTACT_NAME = "Unknown author";

    /**
     * implementations of algorithms should overwrite this
     * String with webpage for the project, if available.
     */
    String CONTACT_ADRESS = "No web-address available";

    /**
     * implementations of algorithms should overwrite this
     * String with (email) adress of the author.
     */
    String CONTACT_MAIL = "No contact available";

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    String getDescription();
}
