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

 
