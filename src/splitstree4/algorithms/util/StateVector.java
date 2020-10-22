/**
 * StateVector.java
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
 * State vector - used for setting state frequences or other characteristics in options
 */
package splitstree4.algorithms.util;

import splitstree4.nexus.Characters;

/**
 * @author bryant
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class StateVector {


    Object[] cells; //Values in the matrix
    int nstates; //Number of rows and columns
    String[] labels; //Column labels


    public StateVector(Characters chars) {
        String sym = chars.getFormat().getSymbols();
        nstates = sym.length();
        cells = new Object[nstates];
        labels = new String[nstates];
        for (int i = 0; i < nstates; i++)
            labels[i] = sym.substring(i, i + 1);
    }

    public StateVector(Characters chars, Object[] entries) {
        this(chars);
        for (int i = 0; i < nstates; i++) {
            set(i, entries[i]);
        }
    }

    public StateVector(int n, Object[] entries, String[] statelabels) {
        nstates = n;
        cells = entries;
        labels = statelabels;
    }

    public void set(int i, Object x) {
        cells[i] = x;
    }

    public Object get(int i) {
        return cells[i];
    }

    public String getLabel(int i) {
        return labels[i];
    }

    public int getNstates() {
        return nstates;
    }

    /**
     * Returns a 2D array of objects containing the cells - useful for setting up JTables and spreadsheets
     *
     * @return Object[][]
     */
    public Object[][] getCells() {
        Object[][] theCells = new Object[1][];
        theCells[0] = cells;
        return theCells;
    }

    public String[] getLabels() {
        return labels;
    }
}
