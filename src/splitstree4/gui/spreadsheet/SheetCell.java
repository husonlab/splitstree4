/*
 * SheetCell.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.spreadsheet;

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
	final Color background;
	final Color foreground;
	final int row;
	final int column;


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
