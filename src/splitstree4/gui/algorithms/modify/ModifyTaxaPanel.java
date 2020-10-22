/**
 * ModifyTaxaPanel.java
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
 * The taxa window
 *
 * @author Markus Franz
 * The taxa window
 * @author Markus Franz
 */
/** The taxa window
 *
 * @author Markus Franz
 */
package splitstree4.gui.algorithms.modify;

import jloda.swing.director.IUpdateableView;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.UpdateableActions;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Traits;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

//TODO: If the value in a cell is 'missing' clear the cell when the user starts editing
//TODO: fill and guess should only be enabled when cells selected

/**
 * The select taxa panel
 */
public class ModifyTaxaPanel extends JPanel implements IUpdateableView, UpdateableActions {

    java.util.List all = new LinkedList();
    private Director dir;
    JTable table;
    JScrollPane scrollPane;
    int ntraits;


    class MyTableModel extends DefaultTableModel {
        public MyTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        public boolean isCellEditable(int row, int col) {
            return col >= 1;
        }

        public Vector getColumnIdentifiers() {
            return columnIdentifiers;
        }
    }


    public ModifyTaxaPanel(Director dir) {
        initialise(dir);
    }

    private void initialise(Director dir) {

        //Enable popup menu events
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);


        //First create the header panel and add it to the panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout(0, 0));
        headerPanel.add(new JLabel("Edit taxa traits:"), "West");
        JButton applyButton = new JButton(getApplyAction());
        headerPanel.add(applyButton, "East");

        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new GridBagLayout());
        JButton addTraitButton = new JButton(getAddTraitAction());
        addTraitButton.setSize(30, 30);
        footerPanel.add(addTraitButton);
        JButton deleteTraitButton = new JButton(getDeleteTraitAction());
        deleteTraitButton.setSize(30, 30);
        footerPanel.add(deleteTraitButton);
        JButton guessValueButton = new JButton(getGuessValueAction());
        guessValueButton.setSize(30, 30);
        footerPanel.add(guessValueButton);
        JButton fillDownButton = new JButton(getFillDownAction());
        fillDownButton.setSize(30, 30);
        footerPanel.add(fillDownButton);

        headerPanel.setOpaque(false);
        setBorder(new BorderUIResource.EmptyBorderUIResource(new Insets(12, 12, 12, 12)));
        setLayout(new BorderLayout(0, 0));
        add(headerPanel, "North");

        //Now construct the traits table
        this.dir = dir;
        Taxa taxa = dir.getDocument().getTaxa();
        Traits traits = dir.getDocument().getTraits();
        if (traits != null)
            ntraits = traits.getNtraits();
        else
            ntraits = 0;

        int ncols = ntraits + 1;
        String[][] data = new String[taxa.getNtax()][];
        String[] columnNames;
        columnNames = new String[ncols];
        columnNames[0] = "Taxa";
        //TODO: Protect against errors in the trait block.
        if (traits != null)
            for (int i = 1; i <= ntraits; i++)
                columnNames[i] = traits.getTraitName(i);
        for (int i = 1; i <= taxa.getNtax(); i++) {
            data[i - 1] = new String[ncols];
            data[i - 1][0] = taxa.getLabel(i);
            if (traits != null)
                for (int j = 1; j <= ntraits; j++)
                    data[i - 1][j] = traits.get(i, j);
        }


        table = new JTable(new MyTableModel(data, columnNames));
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        // todo: not available in Java 1.5
        //table.setFillsViewportHeight(true);
        //table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //force cell contents to be saved when focus is changed
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        //Set widths to be slightly larger than the text
        for (int i = 0; i <= ntraits; i++)
            adjustColumnWidth(table, i);


        //TODO: If the columns don't fill the panel, the last column should expand in width to fill the gap.

        //Create the scroll pane and add the table to it.
        scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, "Center");

        //Now some rubbish to get the scrollbars correct in the JTable
        ComponentListenerAdapter componentAdapter = new ComponentListenerAdapter() {
            //Get the scrollbar or remove the scrollbar upon resizing
            protected void resizingAction() {
                Container jTableParent = table.getParent();

                if (jTableParent instanceof JViewport) {
                    //Check if the width of the Table Parent Container
                    //is less than the Preferred Size of the Table
                    if (jTableParent.getSize().getWidth() <
                            table.getPreferredSize().getWidth()) {
                        //Yes it is
                        //Remove the Auton Resize Function and get the
                        //Scrollbar
                        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    } else {
                        //No it is not
                        //Get the Auto Resize functionality back in place
                        table.setAutoResizeMode(
                                JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
                    }
                }
            }
        };
        scrollPane.addComponentListener(componentAdapter);

        add(footerPanel, "South");
    }

    private abstract class ComponentListenerAdapter
            implements ComponentListener {
        public void componentHidden(ComponentEvent e) {
            resizingAction();
        }

        public void componentMoved(ComponentEvent e) {
            resizingAction();
        }

        public void componentResized(ComponentEvent e) {
            resizingAction();
        }

        public void componentShown(ComponentEvent e) {
            resizingAction();
        }

        protected abstract void resizingAction();
    }


    /**
     * Adjusts column width to 20 pixels + maximum entry width
     *
     * @param table JTable
     * @param col   Column to be resized
     */
    protected void adjustColumnWidth(JTable table, int col) {
        TableModel model = table.getModel();
        TableColumn column;
        Component comp;
        int headerWidth;
        int cellWidth;
        TableCellRenderer headerRenderer =
                table.getTableHeader().getDefaultRenderer();

        column = table.getColumnModel().getColumn(col);

        comp = headerRenderer.getTableCellRendererComponent(
                null, column.getHeaderValue(),
                false, false, 0, 0);
        headerWidth = comp.getPreferredSize().width;


        cellWidth = 0;
        for (int j = 0; j < table.getRowCount(); j++) {
            comp = table.getDefaultRenderer(model.getColumnClass(col)).
                    getTableCellRendererComponent(
                            table, table.getValueAt(j, col),
                            false, false, 0, col);
            cellWidth = Math.max(cellWidth, comp.getPreferredSize().width);
        }


        column.setPreferredWidth(20 + Math.max(headerWidth, cellWidth));
    }


    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     */
    public void updateView(String what) {

    }

    public void setEnableCritical(boolean flag) {
        DirectorActions.setEnableCritical(all, flag);
    }

    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);
        // because we don't want to duplicate that code

        if (dir.getDocument().isValidByName(Traits.NAME) || table.getColumnCount() > 1)
            getApplyAction().setEnabled(true);
        else
            getApplyAction().setEnabled(false);
    }

    private AbstractAction applyAction;

    private AbstractAction getApplyAction() {
        if (applyAction != null)
            return applyAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Taxa taxa = dir.getDocument().getTaxa();
                    Traits traits = dir.getDocument().getTraits();
                    Taxa fullTaxa = taxa;
                    boolean hasHidden = (taxa.getHiddenTaxa() != null && taxa.getHiddenTaxa().cardinality() > 0);
                    if (hasHidden)
                        fullTaxa = taxa.getOriginalTaxa();
                    ntraits = table.getColumnCount() - 1;

                    if (table.getColumnCount() > 1) {
                        if (traits == null)
                            traits = new Traits(taxa.getNtax());
                        //Generate a new traits block from the data in the window
                        String s = "begin traits;\n";
                        s += "dimensions ntraits = " + (table.getColumnCount() - 1) + ";\n";
                        s += "format\n";
                        s += "\tLABELS = " + (traits.getFormat().hasTaxonLabels() ? "YES" : "NO") + "\n";
                        s += "\tSEPARATOR = " + (traits.getFormat().getSeparator()) + "\n";
                        s += "\tMISSING=" + (traits.getFormat().getMissingTrait()) + "\n";
                        s += ";\n";
                        s += "traitlabels ";
                        for (int i = 1; i <= ntraits; i++)
                            s += "\t" + table.getColumnName(i);
                        s += " ;\n";


                        s += "MATRIX\n";

                        String sep;
                        if (traits.getFormat().getSeparator().equalsIgnoreCase(traits.getFormat().COMMA))
                            sep = ", ";
                        else
                            sep = "\t";


                        //Need to check if we are working with induced taxa block, as traits block needs
                        //to be constructed for full taxa block and then have taxa hidden


                        for (int i = 1; i <= fullTaxa.getNtax(); i++) {
                            //Taxon label
                            if (traits.getFormat().hasTaxonLabels())
                                s += taxa.getLabel(i) + "\t";

                            String taxaName = fullTaxa.getLabel(i);
                            int taxaNum = taxa.indexOf(taxaName);


                            //Matrix elements
                            for (int j = 1; j <= ntraits; j++) {
                                String element;
                                //If user moves columns, we change trait columns accordingly.
                                //However rows are always with respect to table model (the taxa)
                                int traitCol = table.convertColumnIndexToModel(j);
                                if (taxaNum > 0)
                                    element = (String) table.getModel().getValueAt(taxaNum - 1, traitCol);
                                else
                                    element = "";
                                element = element.trim();
                                if (element.length() > 0)
                                    s += element;
                                else
                                    s += Traits.MISSING_TRAIT;
                                if (j < ntraits)
                                    s += sep;
                            }
                            s += "\n";
                        }
                        s += ";\nEND;\n";
                        System.err.println(s);

                        traits.read(new NexusStreamParser(new StringReader(s)), fullTaxa);
                        dir.getDocument().setTraits(traits);
                        //Hide any taxa 
                        if (hasHidden)
                            dir.getDocument().getTraits().hideTaxa(fullTaxa, taxa.getHiddenTaxa());


                    } else {
                        dir.getDocument().setTraits(null);
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply changes to Traits block");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyAction = action;
    }

    private AbstractAction addTraitAction;

    private AbstractAction getAddTraitAction() {
        if (addTraitAction != null)
            return addTraitAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String newtrait;

                    newtrait = JOptionPane.showInputDialog(null, "Enter the name of the new trait", "Add trait", JOptionPane.QUESTION_MESSAGE);


                    if ((newtrait != null) && newtrait.length() > 0) {
                        //Check to see its new
                        boolean found = false;
                        for (int i = 0; !found && i < table.getColumnCount(); i++) {
                            if (newtrait.equals(table.getColumnName(i)))
                                found = true;
                        }
                        if (found) {
                            JOptionPane.showMessageDialog(null, "There is already a trait named `" + newtrait + "'");
                            return;
                        }
                        //Add a new columnd
                        table.setAutoCreateColumnsFromModel(false);
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        String[] values = new String[table.getRowCount()];
                        for (int i = 0; i < values.length; i++)
                            values[i] = Traits.MISSING_TRAIT;
                        model.addColumn(newtrait, values);

                        TableColumn column = new TableColumn(table.getColumnCount());
                        column.setHeaderValue(newtrait);
                        table.addColumn(column);

                        table.requestFocusInWindow();
                        updateEnableState();

                    }


                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Add trait");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Add a new trait");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return addTraitAction = action;
    }

    private AbstractAction deleteTraitAction;

    private AbstractAction getDeleteTraitAction() {
        if (deleteTraitAction != null)
            return deleteTraitAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (table.getColumnCount() <= 1)
                        return;//todo set enabled
                    String[] traits = new String[table.getColumnCount() - 1];
                    for (int i = 1; i < table.getColumnCount(); i++)
                        traits[i - 1] = table.getColumnName(i);

                    String delTrait = (String) JOptionPane.showInputDialog(null,
                            "Select which trait to delete:",
                            "Delete trait",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            traits,
                            traits[traits.length - 1]);
                    if ((delTrait != null) && (delTrait.length() > 0)) {
                        int traitNum = 0;
                        while (traitNum < traits.length && !delTrait.equals(traits[traitNum]))
                            traitNum++;
                        if (traitNum >= traits.length)
                            return;


                        MyTableModel model = (MyTableModel) table.getModel();
                        TableColumn col = table.getColumnModel().getColumn(traitNum + 1);
                        int columnModelIndex = col.getModelIndex();
                        Vector data = model.getDataVector();
                        Vector colIds = model.getColumnIdentifiers();

                        // Remove the column from the table
                        table.removeColumn(col);

                        // Remove the column header from the table model
                        colIds.removeElementAt(columnModelIndex);

                        // Remove the column data
                        for (Object aData : data) {
                            Vector row = (Vector) aData;
                            row.removeElementAt(columnModelIndex);
                        }
                        model.setDataVector(data, colIds);

                        // Correct the model indices in the TableColumn objects
                        // by decrementing those indices that follow the deleted column
                        Enumeration<TableColumn> enumTC = table.getColumnModel().getColumns();
                        for (; enumTC.hasMoreElements(); ) {
                            TableColumn c = enumTC.nextElement();
                            if (c.getModelIndex() >= columnModelIndex) {
                                c.setModelIndex(c.getModelIndex() - 1);
                            }
                        }
                        model.fireTableStructureChanged();
                        updateEnableState();

                    }

                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Remove trait");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Delete the selected traits");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return deleteTraitAction = action;
    }

    private AbstractAction fillDownAction;

    private AbstractAction getFillDownAction() {
        if (fillDownAction != null)
            return fillDownAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] cols = table.getSelectedColumns();
                int[] rows = table.getSelectedRows();
                for (int col : cols) {
                    if (col == 0)
                        break; //No effect on taxa column.
                    String val = (String) table.getValueAt(rows[0], col);
                    int modelCol = table.convertColumnIndexToModel(col);
                    for (int j = 1; j < rows.length; j++) {
                        table.setValueAt(val, rows[j], col);

                        // todo: not available in Java 1.5, but needed in 1.6 onwards
                        //int modelRow = table.convertRowIndexToModel(rows[j]);
                        // table.getModel().setValueAt(val,modelRow,modelCol);
                    }

                }

            }
        };
        action.putValue(AbstractAction.NAME, "Fill down");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy top value to remaining cells");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return fillDownAction = action;

    }

    private AbstractAction guessValueAction;

    private AbstractAction getGuessValueAction() {
        if (guessValueAction != null)
            return guessValueAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] cols = table.getSelectedColumns();
                int[] rows = table.getSelectedRows();
                for (int col : cols) {
                    if (col == 0)
                        break; //No effect on taxa column.

                    for (int j = 0; j < rows.length; j++) {
                        String taxaName = (String) table.getValueAt(rows[j], 0);
                        int pos = taxaName.indexOf("_");
                        String val;
                        if (pos >= 0)
                            val = taxaName.substring(0, pos);
                        else
                            val = taxaName;
                        table.setValueAt(val, rows[j], col);

                        // todo: not available in Java 1.5, but needed in 1.6 onwards
                        //int modelRow = table.convertRowIndexToModel(rows[j]);
                        // table.getModel().setValueAt(val,modelRow,modelCol);
                    }

                }
            }
        };
        action.putValue(AbstractAction.NAME, "Guess value");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Guesses trait value from prefix of taxa labels");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return guessValueAction = action;
    }
}
