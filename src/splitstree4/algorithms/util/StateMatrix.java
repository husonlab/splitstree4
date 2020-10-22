/**
 * StateMatrix.java
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
/*
 * Created on Aug 25, 2004
 *
 * State by State matrix - used for setting matrices in options
 */
package splitstree4.algorithms.util;

import splitstree4.nexus.Characters;

/**
 * @author bryant
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class StateMatrix {


    Object[][] cells; //Values in the matrix
    int nstates; //Number of rows and columns


    StateMatrix(Characters chars) {
        String sym = chars.getFormat().getSymbols();
        nstates = sym.length();
        cells = new Object[nstates][nstates];
    }

    StateMatrix(Characters chars, Object[][] entries) {
        this(chars);
        if (nstates > 0) {
            for (int i = 0; i < nstates; i++) {
                for (int j = 0; j < nstates; j++) {
                    set(i, j, entries[i][j]);
                }
            }
        }
    }

    void set(int i, int j, Object x) {
        cells[i][j] = x;
    }

    Object get(int i, int j) {
        return cells[i][j];
    }
}
