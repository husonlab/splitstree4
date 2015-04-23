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
 * State by State matrix - used for setting matrices in options
 */
package splitstree.algorithms.util;

import splitstree.nexus.Characters;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
