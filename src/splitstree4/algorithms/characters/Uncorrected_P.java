/*
 * Uncorrected_P.java Copyright (C) 2022 Daniel H. Huson
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
/*
 * $Id: Uncorrected_P.java,v 1.5 2007-09-11 12:31:00 kloepper Exp $
 */
package splitstree4.algorithms.characters;


/**
 * Simple implementation of hamming distances
 */
public class Uncorrected_P extends Hamming implements Characters2Distances {

    public final static String DESCRIPTION = "Calculates uncorrected (observed, \"P\") distances.";
    protected final String TASK = "Uncorrected P Distance";

    protected String getTask() {
        return TASK;
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

// EOF
