/**
 * Copyright 2015, Daniel Huson and David Bryant
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

/*
 * Created on Aug 25, 2004
 *
 * State vector - used for setting state frequences or other characteristics in options
 */
package splitstree.algorithms.util;

import splitstree.nexus.Characters;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
