/**
 * SpreadSheetModel.java
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
package splitstree4.gui.spreadsheet;

import javax.swing.table.AbstractTableModel;

/**
 * This class specifies the data format
 * for the SpreadSheet JTable
 *
 * @author Thierry Manf�
 * @version 1.0 July-2002
 */
public class SpreadSheetModel extends AbstractTableModel {

    final private SpreadSheet _dpyTable;

    //static Interpreter interpreter;

    static final boolean DEBUG = false;

    private int _nbRow;
    private int _nbColumn;

    protected SheetCell[][] cells;

    /**
     * Create a nbRow by nbColumn SpreadSheetModel.
     *
     * @param cells     The cell array ('[numRow][numColumn]')
     * @param table     The associated SpreadSheet
     */
    SpreadSheetModel(SheetCell[][] cells, SpreadSheet table) {
        _dpyTable = table;
        _nbRow = cells.length;
        _nbColumn = cells[0].length;
        this.cells = cells;
        //interpreter = new Interpreter(this);
    }

    private void clean() {
        _nbRow = _nbColumn = 0;
        cells = null;
    }

    public int getRowCount() {
        return _nbRow;
    }

    public int getColumnCount() {
        return _nbColumn;
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public Object getValueAt(int row, int column) {
        return cells[row][column].value;
    }

    /**
     * Mark the corresponding cell
     * as being edited. Its formula
     * must be displayed instead of
     * its result
     */
    /* void setEditMode(int row, int column)    { cells[row][column].state=SheetCell.EDITED; }

     void setDisplayMode(int row, int column) { cells[row][column].state=SheetCell.UPDATED; } */
    public void setValueAt(Object value, int row, int column) {
        cells[row][column].value = value;
        _dpyTable.repaint();
    }

}

 
