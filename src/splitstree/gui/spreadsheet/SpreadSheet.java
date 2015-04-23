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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * **********************************************************************************************
 * <p/>
 * This class implements a basic spreadsheet
 * using a JTable.
 * It also provides a main() method to be run
 * as an application.
 *
 * @author Thierry Manf� [heavily edited by D. Bryant, August 2004]
 *         <p/>
 *         **********************************************************************************************
 * @version 1.0 July-2002
 */
//TODO: Check selection of cells to see whether selected cells are correctly formatted.
// If not, we'll need to re-introduce the states into SheetCell

public class SpreadSheet extends JTable {

    /**
     * Set this field to true and recompile
     * to get debug traces
     */
    public static final boolean DEBUG = true;

    private JScrollPane _scp;
    private SpreadSheetModel _model;
    private int _numRow;
    private int _numCol;

    private int _editedModelRow;
    private int _editedModelCol;

    private boolean _hasRowHeaders;
    private boolean _hasColHeaders;
    /*
     * GUI components used to tailor
     * the SpreadSheet.
     */
    private CellRenderer _renderer;
    //private FontMetrics  _metrics;

    // Cells selected.
    private Object[] _selection;

    /*

        SpreadSheet sp = new SpreadSheet(40, 40);

        /**
        SheetCell[][] cells = new SheetCell[3][2];
        cells[0][0] = new SheetCell(0 , 0, "1", null);
        cells[1][0] = new SheetCell(0 , 1, "2", null);
        cells[2][0] = new SheetCell(0 , 2, "3", null);
        cells[0][1] = new SheetCell(1 , 0, "1", "=A1");
        cells[1][1] = new SheetCell(1 , 1, "3", "=A1+A2");
        cells[2][1] = new SheetCell(1 , 2, "6", "=A1+A2+A3");
        SpreadSheet sp = new SpreadSheet(cells);


        frame.getContentPane().add(sp.getScrollPane());
        frame.pack();
        frame.setVisible(true);

    */


    public SpreadSheet(SheetCell[][] cells, int numRow, int numCol, String[] RowHeaders, String[] ColHeaders) {

        super();

        SheetCell foo[][];

        if (cells != null)
            foo = cells;
        else {
            foo = new SheetCell[numRow][numCol];
            for (int ii = 0; ii < numRow; ii++) {
                for (int jj = 0; jj < numCol; jj++)
                    foo[ii][jj] = new SheetCell(ii, jj);
            }
        }

        _numRow = numRow;
        _numCol = numCol;

        // Create the JScrollPane that includes the Table
        _scp = new JScrollPane(this);

        // Create the rendeder for the cells
        _renderer = new CellRenderer();
        try {
            setDefaultRenderer(Class.forName("java.lang.Object"), _renderer);
        } catch (ClassNotFoundException ex) {
            if (DEBUG)
                System.out.println("SpreadSheet() Can't modify renderer");
        }

        _model = new SpreadSheetModel(foo, this);
        setModel(_model);

        /*
         * Tune the selection mode
         */

        // Allows row and collumn selections to exit at the same time
        setCellSelectionEnabled(true);

        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {

                int selRow[] = getSelectedRows();
                int selCol[] = getSelectedColumns();

                _selection = new Object[selRow.length * selCol.length];

                int indice = 0;
                for (int aSelRow : selRow) {
                    for (int c = 0; c < selCol.length; c++) {
                        _selection[indice] =
                                _model.cells[aSelRow][convertColumnIndexToModel(selCol[c])];
                        indice++;
                    }
                }

            }
        });

        // Create a row-header to display row numbers.
        // This row-header is made of labels whose Borders,
        // Foregrounds, Backgrounds, and Fonts must be
        // the one used for the table column headers.
        // Also ensure that the row-header labels and the table
        // rows have the same height.
        TableColumn aColumn = getColumnModel().getColumn(0);
        TableCellRenderer aRenderer = getTableHeader().getDefaultRenderer();
        if (aRenderer == null) {
            System.out.println(" Aouch !");
            aColumn = getColumnModel().getColumn(0);
            aRenderer = aColumn.getHeaderRenderer();
            if (aRenderer == null) {
                System.out.println(" Aouch Aouch !");
                //TODO Throw exception
            }
        }
        Component aComponent =
                aRenderer.getTableCellRendererComponent(this, aColumn.getHeaderValue(), false, false, -1, 0);
        Font aFont = aComponent.getFont();
        Color aBackground = aComponent.getBackground();
        Color aForeground = aComponent.getForeground();

        Border border = (Border) UIManager.getDefaults().get("TableHeader.cellBorder");
        Insets insets = border.getBorderInsets(tableHeader);
        FontMetrics metrics = getFontMetrics(aFont);
        rowHeight = insets.bottom + metrics.getHeight() + insets.top;

        /* Check to see if the user wants row labels */
        _hasRowHeaders = (RowHeaders != null);
        if (_hasRowHeaders) {

            /*
             * Creating a panel to be used as the row header.
             *
             * Since I'm not using any LayoutManager,
             * a call to setPreferredSize().
             */
            JPanel pnl = new JPanel(null);
            Dimension dim = new Dimension(metrics.stringWidth("999") + insets.right + insets.left, rowHeight * _numRow);
            pnl.setPreferredSize(dim);

            // Adding the row header labels
            dim.height = rowHeight;
            for (int ii = 0; ii < _numRow; ii++) {
                //TODO: Introduce user-specified row and column labels, as well as the option to have none
                JLabel lbl = new JLabel(RowHeaders[ii], SwingConstants.CENTER);
                lbl.setFont(aFont);
                lbl.setBackground(aBackground);
                lbl.setForeground(aForeground);
                lbl.setBorder(border);
                lbl.setBounds(0, ii * dim.height, dim.width, dim.height);
                pnl.add(lbl);
            }

            JViewport vp = new JViewport();
            dim.height = rowHeight * _numRow;
            vp.setViewSize(dim);
            vp.setView(pnl);
            _scp.setRowHeader(vp);
        } else {
            _scp.setRowHeader(null);
        }

        /* Check to see if the user wants column labels */
        _hasColHeaders = (ColHeaders != null);
        if (_hasColHeaders) {
            //By default, the column headers will always exist. We only need to
            //change the names
            for (int ii = 0; ii < _numCol; ii++) {
                getColumnModel().getColumn(ii).setHeaderValue(ColHeaders[ii]);
            }
            getTableHeader().resizeAndRepaint();
        } else {
            setTableHeader(null);
        }
        // Set resize policy and make sure
        // the table's size is tailored
        // as soon as it gets drawn.
        /* Resize all of the columns */
        TableColumn column;
        for (int ii = 0; ii < _numCol; ii++) {
            column = getColumnModel().getColumn(ii);
            column.setPreferredWidth(metrics.stringWidth("99999"));
        }

        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        Dimension dimScpViewport = getPreferredScrollableViewportSize();
        if (_numRow > 30)
            dimScpViewport.height = 30 * rowHeight;
        else
            dimScpViewport.height = _numRow * rowHeight;
        if (_numCol > 15)
            dimScpViewport.width =
                    15 * getColumnModel().getTotalColumnWidth() / _numCol;
        else
            dimScpViewport.width = getColumnModel().getTotalColumnWidth();
        setPreferredScrollableViewportSize(dimScpViewport);
        resizeAndRepaint();
    }


    /**
     * Build SpreadSheet of numCol columns and numRow rows.
     *
     * @param cells                    If not null, the cells to be used in the spreadsheet
     *                                 It must be a two dimensional rectangular array ('[numRow][numColumn]'). If null, the cells are
     *                                 automatically created.
     * @param numRow                   The number of rows
     * @param numCol                   The number of columns
     */
    private SpreadSheet(SheetCell[][] cells, int numRow, int numCol) {

        super();

        SheetCell foo[][];

        if (cells != null)
            foo = cells;
        else {
            foo = new SheetCell[numRow][numCol];
            for (int ii = 0; ii < numRow; ii++) {
                for (int jj = 0; jj < numCol; jj++)
                    foo[ii][jj] = new SheetCell(ii, jj);
            }
        }

        _numRow = numRow;
        _numCol = numCol;

        // Create the JScrollPane that includes the Table
        _scp = new JScrollPane(this);

        // Create the rendeder for the cells
        _renderer = new CellRenderer();
        try {
            setDefaultRenderer(Class.forName("java.lang.Object"), _renderer);
        } catch (ClassNotFoundException ex) {
            if (DEBUG)
                System.out.println("SpreadSheet() Can't modify renderer");
        }

        _model = new SpreadSheetModel(foo, this);
        setModel(_model);

        /*
         * Tune the selection mode
         */

        // Allows row and collumn selections to exit at the same time
        setCellSelectionEnabled(true);

        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {

                int selRow[] = getSelectedRows();
                int selCol[] = getSelectedColumns();

                _selection = new Object[selRow.length * selCol.length];

                int indice = 0;
                for (int aSelRow : selRow) {
                    for (int c = 0; c < selCol.length; c++) {
                        _selection[indice] =
                                _model.cells[aSelRow][convertColumnIndexToModel(selCol[c])];
                        indice++;
                    }
                }

            }
        });

        // Create a row-header to display row numbers.
        // This row-header is made of labels whose Borders,
        // Foregrounds, Backgrounds, and Fonts must be
        // the one used for the table column headers.
        // Also ensure that the row-header labels and the table
        // rows have the same height.
        TableColumn aColumn = getColumnModel().getColumn(0);
        TableCellRenderer aRenderer = getTableHeader().getDefaultRenderer();
        if (aRenderer == null) {
            System.out.println(" Aouch !");
            aColumn = getColumnModel().getColumn(0);
            aRenderer = aColumn.getHeaderRenderer();
            if (aRenderer == null) {
                System.out.println(" Aouch Aouch !");
                //TODO Throw exception
            }
        }
        Component aComponent =
                aRenderer.getTableCellRendererComponent(this, aColumn.getHeaderValue(), false, false, -1, 0);
        Font aFont = aComponent.getFont();
        Color aBackground = aComponent.getBackground();
        Color aForeground = aComponent.getForeground();

        Border border = (Border) UIManager.getDefaults().get("TableHeader.cellBorder");
        Insets insets = border.getBorderInsets(tableHeader);
        FontMetrics metrics = getFontMetrics(aFont);
        rowHeight = insets.bottom + metrics.getHeight() + insets.top;

        /*
         * Creating a panel to be used as the row header.
         *
         * Since I'm not using any LayoutManager,
         * a call to setPreferredSize().
         */
        JPanel pnl = new JPanel(null);
        Dimension dim = new Dimension(metrics.stringWidth("999") + insets.right + insets.left, rowHeight * _numRow);
        pnl.setPreferredSize(dim);

        // Adding the row header labels
        dim.height = rowHeight;
        for (int ii = 0; ii < _numRow; ii++) {
            //TODO: Introduce user-specified row and column labels, as well as the option to have none
            JLabel lbl = new JLabel(Integer.toString(ii + 1), SwingConstants.CENTER);
            lbl.setFont(aFont);
            lbl.setBackground(aBackground);
            lbl.setForeground(aForeground);
            lbl.setBorder(border);
            lbl.setBounds(0, ii * dim.height, dim.width, dim.height);
            pnl.add(lbl);
        }

        JViewport vp = new JViewport();
        dim.height = rowHeight * _numRow;
        vp.setViewSize(dim);
        vp.setView(pnl);
        _scp.setRowHeader(vp);

        // Set resize policy and make sure
        // the table's size is tailored
        // as soon as it gets drawn.
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Dimension dimScpViewport = getPreferredScrollableViewportSize();
        if (_numRow > 30)
            dimScpViewport.height = 30 * rowHeight;
        else
            dimScpViewport.height = _numRow * rowHeight;
        if (_numCol > 15)
            dimScpViewport.width =
                    15 * getColumnModel().getTotalColumnWidth() / _numCol;
        else
            dimScpViewport.width = getColumnModel().getTotalColumnWidth();
        setPreferredScrollableViewportSize(dimScpViewport);
        resizeAndRepaint();
    }

    /**
     * Build a numRow by numColumn SpreadSheet included
     * in a JScrollPane. The associated model and the cells
     * are automatically created.
     *
     * @param numRow    The number of row in the spreadsheet
     * @param numColumn The number of column in the spreadsheet
     */
    public SpreadSheet(int numRow, int numColumn) {
        this(null, numRow, numColumn);
    }

    /**
     * Build a SpreadSheet included in a JScrollPane
     * from the cells given as argument.
     *
     * @param cells                    A two dimensional rectangular
     *                                 array ('[numRow][numColumn]') of cells to be used when
     *                                 creating the spreadsheet.
     */
    public SpreadSheet(SheetCell cells[][]) {
        this(cells, cells.length, cells[0].length);
    }

    /**
     * Invoked when a cell undo starts.
     * This method overrides and calls that of its super class.
     *
     * @param row         The row to be edited
     * @param column      The column to be edited
     * @param ev          The firing event
     * @return boolean false if for any reason the cell cannot be edited.
     */
    public boolean editCellAt(int row, int column, EventObject ev) {

        //	if (_editedModelRow != -1)
        //		_model.setDisplayMode(_editedModelRow, _editedModelCol);

        _editedModelRow = row;
        _editedModelCol = convertColumnIndexToModel(column);

        //	_model.setEditMode(row, convertColumnIndexToModel(column));
        return super.editCellAt(row, column, ev);

    }

    /**
     * Invoked by the cell editor when a cell edi stops.
     * This method override and calls that of its super class.
     */
    public void editingStopped(ChangeEvent ev) {
        //	_model.setDisplayMode(_editedModelRow, _editedModelCol);
        super.editingStopped(ev);
    }

    /**
     * Invoked by the cell editor when a cell edition is cancelled.
     * This method override and calls that of its super class.
     */
    public void editingCanceled(ChangeEvent ev) {
        //	_model.setDisplayMode(_editedModelRow, _editedModelCol);
        super.editingCanceled(ev);
    }

    public JScrollPane getScrollPane() {
        return _scp;
    }

    public void processMouseEvent(MouseEvent ev) {

        /* In future, we could introduce a pop-up menu here
        int type = ev.getID();
        int modifiers = ev.getModifiers();

        if ((type == MouseEvent.MOUSE_RELEASED)
            && (modifiers == InputEvent.BUTTON3_MASK)) {

            if (_selection != null) {
                if (_popupMenu == null)
                    _popupMenu = new CellMenu(this);

                if (_popupMenu.isVisible())
                    _popupMenu.setVisible(false);
                else {
                    _popupMenu.setTargetCells(_selection);
                    Point p = getLocationOnScreen();
                    _popupMenu.setLocation(
                        p.x + ev.getX() + 1,
                        p.y + ev.getY() + 1);
                    _popupMenu.setVisible(true);
                }
            }

        }
        */
        super.processMouseEvent(ev);
    }


    protected void release() {
        _model = null;
    }

    public void setVisible(boolean flag) {
        _scp.setVisible(flag);
    }

    /*
     * This class is used to customize the cells rendering.
     */
    public class CellRenderer extends JLabel implements TableCellRenderer {

        private LineBorder _selectBorder;
        private EmptyBorder _emptyBorder;
        private Dimension _dim;

        public CellRenderer() {
            super();
            _emptyBorder = new EmptyBorder(1, 2, 1, 2);
            _selectBorder = new LineBorder(Color.red);
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            _dim = new Dimension();
            _dim.height = 22;
            _dim.width = 100;
            setSize(_dim);
        }

        /**
         * Method defining the renderer to be used
         * when drawing the cells.
         */
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {


            SheetCell sc = new SheetCell(row, column, value.toString());

            setText(sc.toString());
            setForeground(sc.foreground);
            setBackground(sc.background);

            if (isSelected) {
                setBorder(_selectBorder);
                //setToolTipText("Right-click to change the cell's colors.");

            } else {
                setBorder(_emptyBorder);
                setToolTipText("Single-Click to select a cell, "
                        + "double-click to undo.");
            }

            return this;

        }

    }

}
