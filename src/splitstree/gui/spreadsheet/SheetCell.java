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

package splitstree.gui.spreadsheet;

import java.awt.*;
import java.util.Vector;

/**
 * This class specifies the
 * cell format.
 *
 * @author Thierry Manf�
 * @version 1.0 July-2002
 */
public class SheetCell {

    static final int UNDEFINED = 0;
    static final int EDITED = 1;
    static final int UPDATED = 2;
    static final boolean USER_EDITION = true;
    static final boolean UPDATE_EVENT = false;

    Object value;
    int state;
    Vector listeners;
    Vector listenees;
    Color background;
    Color foreground;
    int row;
    int column;


    public SheetCell(int r, int c) {
        row = r;
        column = c;
        value = null;
        background = Color.white;
        foreground = Color.black;
    }


    public SheetCell(int r, int c, Object value) {
        this(r, c);
        this.value = value;
    }

    public String toString() {
        if (value != null)
            return value.toString();
        else
            return null;
    }

}
