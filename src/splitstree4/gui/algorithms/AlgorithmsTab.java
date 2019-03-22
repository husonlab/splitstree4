/**
 * AlgorithmsTab.java
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
package splitstree4.gui.algorithms;

import jloda.swing.util.ProgramProperties;
import jloda.util.Basic;
import splitstree4.algorithms.Transformation;
import splitstree4.algorithms.util.Configurator;
import splitstree4.core.Document;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.spreadsheet.SheetCell;
import splitstree4.gui.spreadsheet.SpreadSheet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * manages the tabs of the algorithms viewer
 *
 * @author huson
 *         Date: 05-Dec-2003
 */
public class AlgorithmsTab extends JPanel {
    static String OPT_SETTER = "OSET";
    static String OPT_GETTER = "OGET";
    static String OPT_SELECTION = "OSEL";
    static String OPT_COMBOBOX = "OCBOX";
    static String TEXT_FIELD = "TFIELD";
    static String CHECK_BOX = "CBOX";
    static String TABLE = "TABLE";
    static String FILE_CHOOSER = "CHOOSE";

    JLabel cBoxLabel = new JLabel();
    JComboBox cBox;
    JButton applyButton;
    JLabel dataSummaryLabel = new JLabel();
    final JTextArea descriptionLabel = new JTextArea();
    final JPanel optionsBox = new JPanel();

    JPanel optionsPanel;
    JCheckBox hideDialog;
    GridBagLayout gridBagLayout = new GridBagLayout();

    /**
     * sets up the tab
     */
    public void setup() {
        removeAll();

        cBox.setRenderer(new ComboBoxRenderer()); // render actions correctly!

        descriptionLabel.setEditable(false);
        descriptionLabel.setBackground(optionsBox.getBackground());
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

        top.add(cBoxLabel);
        top.add(Box.createHorizontalGlue());

        top.add(cBox);
        top.add(Box.createHorizontalGlue());

        top.add(applyButton);
        add(top, BorderLayout.NORTH);

        optionsBox.setLayout(new BorderLayout());
        add(optionsBox, BorderLayout.CENTER);

        AbstractAction action = new AbstractAction() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                String name = (String) ((AbstractAction) cBox.getSelectedItem()).getValue(AbstractAction.NAME);
                JCheckBox cbox = (JCheckBox) getValue(DirectorActions.JCHECKBOX);
                ProgramProperties.put("HideDialog." + name, cbox.isSelected());
            }
        };

        JPanel bottom2 = new JPanel();
        bottom2.setLayout(new GridLayout(2, 0));

        action.putValue(AbstractAction.NAME, "Don't show this dialog to configure this method again");
        hideDialog = new JCheckBox(action);
        action.putValue(DirectorActions.JCHECKBOX, hideDialog);

        bottom2.add(hideDialog);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        bottom.add(descriptionLabel);
        bottom.add(Box.createHorizontalGlue());
        bottom.setBorder(BorderFactory.createEtchedBorder());

        bottom2.add(bottom);
        add(bottom2, BorderLayout.SOUTH);
    }

    // all the usual getters and setters

    public String getCBoxLabel() {
        return cBoxLabel.getText();
    }

    public void setCBoxLabel(String label) {
        this.cBoxLabel.setText(label);
    }

    public JComboBox getCBox() {
        return cBox;
    }

    public void setCBox(JComboBox cBox) {
        this.cBox = cBox;
    }

    public JButton getApplyButton() {
        return applyButton;
    }

    public void setApplyButton(JButton applyButton) {
        this.applyButton = applyButton;
    }

    public String getDataSummaryLabel() {
        return dataSummaryLabel.getText();
    }

    public void setDataSummaryLabel(String label) {
        this.dataSummaryLabel.setText(label);
    }

    public String getDescriptionLabel() {
        return descriptionLabel.getText();
    }

    public void setDescriptionLabel(String label) {
        this.descriptionLabel.setText(label);
    }

    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    public void setOptionsPanel(JPanel optionsPanel) {
        if (this.optionsPanel != null)
            optionsBox.removeAll();
        this.optionsPanel = optionsPanel;

        optionsBox.add(new JScrollPane(optionsPanel));
    }

    /**
     * get the hide dialog check box
     *
     * @return
     */
    public JCheckBox getHideDialog() {
        return hideDialog;
    }

    /**
     * decodeOptionName
     * <p/>
     * miguel
     * <p/>
     * To improve readability of dialog boxes, we use an encoding for special
     * characters (such as spaces) that we want in the dialog box but can't put
     * in the method name.
     * <p/>
     * '_' is replaced by ' '
     *
     * @param x string to be decoded
     * @return decoded string
     */
    private static String decodeOptionName(String x) {

        return Basic.insertSpacesBetweenLowerCaseAndUpperCaseLetters(x);
    }

    /**
     * creates the options panel for the given transform
     *
     * @param transform
     * @return options panel
     */
    public static JPanel createOptionsPanel(Document doc, Transformation transform, java.util.List actions) {

        try // if this transform can supply its own panel, well use it!
        {
            if (transform.getClass().getMethod("getGUIPanel", Document.class) != null) {
                Method method = transform.getClass().getMethod("getGUIPanel", Document.class);
                return (JPanel) method.invoke(transform, doc);
            }
        } catch (java.lang.NoSuchMethodException ex) {
        } catch (Exception ex) {
            Basic.caught(ex);
        }

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // collect and parse options:
        String usage = Configurator.getShortUsage(transform);
        StringTokenizer st = new StringTokenizer(usage, " ");

        // need to skip the method name:
        st.nextToken();

        if (!st.hasMoreTokens()) {
            panel.add(new JLabel("No options available"));
        }

        int count = 0;
        while (st.hasMoreTokens()) {
            String opt = st.nextToken();

            String optName = opt.substring(0, opt.lastIndexOf('='));
            String optType = opt.substring(opt.indexOf('<') + 1, opt.length() - 1);
            Object defaultVal = Configurator.getOption(transform, optName);

            if (defaultVal == null) {
                System.err.println("Unexpected null " + optName + " <" + optType + "> = null in [" + opt + "]");
                break;
            }

            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.gridx = 0;
            constraints.gridy = count;
            constraints.gridwidth = 3;

            /*
             * Old version - prints variable type after label - this is now redundant since we
             * specify the type with a ToolTip
             panel.add(new JLabel(decodeOptionName(optName) + " [" + optType + "]", JLabel.LEFT), constraints);
			*/
            panel.add(new JLabel(decodeOptionName(optName), JLabel.LEFT), constraints);


            constraints.gridx = 3;

            // add all option items to the panel
            switch (optType) {
                case "java.lang.String":
                    Method selectionMethod = Configurator.getSelectionMethod(transform, optName);
                    // System.err.println("has selection: " + optName);

                    if (selectionMethod == null) {
                        final JComponent input = new JTextField(defaultVal.toString(), 20);

                        AbstractAction action = new AbstractAction() {
                            public void actionPerformed(ActionEvent event) {
                                Transformation transform =
                                        (Transformation) getValue(DirectorActions.TRANSFORM);
                                Method method = (Method) getValue(OPT_SETTER);
                                Object[] params = new Object[1];
                                try {
                                    params[0] = ((JTextField) getValue(TEXT_FIELD)).getText();
                                    method.invoke(transform, params);
                                } catch (Exception ex) {

                                    System.err.println("Method invoke failed 1: " + ex);
                                }
                            }
                        };
                        action.putValue(DirectorActions.TRANSFORM, transform);
                        action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                        action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                        action.putValue(TEXT_FIELD, input);

                        ((JTextField) input).addActionListener(action);
                        input.setMinimumSize(new Dimension(200, 25));
                        input.setToolTipText("String");

                        panel.add(input, constraints);
                        actions.add(action);
                    } else // to be chosen from list
                    {
                        final JComboBox input = new JComboBox();

                        AbstractAction action = new AbstractAction() {
                            public void actionPerformed(ActionEvent event) {
                                Transformation transform =
                                        (Transformation) getValue(DirectorActions.TRANSFORM);
                                Method method = (Method) getValue(OPT_SETTER);
                                Object[] params = new Object[1];
                                try {
                                    params[0] = (((JComboBox) getValue(OPT_COMBOBOX)).getSelectedItem());
                                    method.invoke(transform, params);
                                } catch (Exception ex) {
                                    Basic.caught(ex);
                                    System.err.println("Method invoke failed 2: " + ex);
                                }
                            }
                        };
                        action.putValue(DirectorActions.TRANSFORM, transform);
                        action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                        action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                        action.putValue(OPT_SELECTION, Configurator.getSelectionMethod(transform, optName));
                        action.putValue(OPT_COMBOBOX, input);

                        input.addActionListener(action);
                        input.setMinimumSize(new Dimension(200, 25));
                        panel.add(input, constraints);
                        actions.add(action);
                    }
                    break;
                case "char": {
                    final JComponent input = new JTextField(defaultVal.toString(), 20);

                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);

                            try {
                                String str = ((JTextField) getValue(TEXT_FIELD)).getText();
                                Object[] params = new Object[1];
                                params[0] = new Character(str.length() == 0 ? ' ' : str.charAt(0));
                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                System.err.println("Method invoke failed 3: " + ex);
                            }
                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(TEXT_FIELD, input);

                    ((JTextField) input).addActionListener(action);
                    input.setToolTipText("Character");
                    input.setMinimumSize(new Dimension(200, 25));
                    panel.add(input, constraints);
                    actions.add(action);

                    break;
                }
                case "boolean": {
                    JComponent input = new JCheckBox();

                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);
                            try {
                                Object[] params = new Object[1];
                                params[0] = new Boolean(((JCheckBox) getValue(CHECK_BOX)).isSelected());
                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                System.err.println("Method invoke failed 4: " + ex);
                            }
                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(CHECK_BOX, input);

                    ((JCheckBox) input).addActionListener(action);
                    ((JCheckBox) input).setSelected(((Boolean) defaultVal).booleanValue());
                    input.setMinimumSize(new Dimension(200, 25));
                    panel.add(input, constraints);
                    actions.add(action);

                    break;
                }
                case "int": {
                    JComponent input = new JTextField(defaultVal.toString(), 8);

                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);
                            try {
                                String str = ((JTextField) getValue(TEXT_FIELD)).getText();
                                Object[] params = new Object[1];
                                params[0] = new Integer(str);
                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                System.err.println("Method invoke failed 5: " + ex);
                            }
                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(TEXT_FIELD, input);

                    ((JTextField) input).addActionListener(action);
                    input.setMinimumSize(new Dimension(200, 25));
                    input.setToolTipText("Integer");
                    panel.add(input, constraints);
                    actions.add(action);

                    break;
                }
                case "double": {
                    JComponent input = new JTextField(defaultVal.toString(), 12);

                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);
                            try {
                                String str = ((JTextField) getValue(TEXT_FIELD)).getText();
                                Object[] params = new Object[1];
                                try {
                                    params[0] = new Double(str);
                                } catch (NumberFormatException ex1) {
                                    params[0] = new Double(0.0);
                                    throw ex1;
                                }
                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                System.err.println("Method invoke failed 6: " + ex);

                            }
                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(TEXT_FIELD, input);

                    ((JTextField) input).addActionListener(action);
                    input.setMinimumSize(new Dimension(200, 25));
                    input.setToolTipText("Double");
                    panel.add(input, constraints);
                    actions.add(action);

                    break;
                }
                case "[D": {
                    //Set up a JTable type
                    double[] vals = (double[]) defaultVal;
                    int n = vals.length;
                    SheetCell[][] cells = new SheetCell[1][n];
                    for (int i = 0; i < n; i++) {
                        cells[0][i] = new SheetCell(0, i, new Double(vals[i]));
                    }
                    SpreadSheet sp = new SpreadSheet(cells, cells.length, cells[0].length, null, null);

//TODO: Add editor which checks for Double... see http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#validtext
                    JComponent input = sp.getScrollPane();


                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            System.err.println("FIRE");
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);
                            try {
                                //Get the table, table model, and size
                                SpreadSheet theTable = (SpreadSheet) getValue(TABLE);
                                TableModel theModel = theTable.getModel();
                                int n = theModel.getColumnCount();

                                //Get the actual data
                                double[] theData = new double[n];
                                for (int i = 0; i < n; i++) {
                                    theData[i] = Double.parseDouble(theModel.getValueAt(0, i).toString());
                                }
                                Object[] params = new Object[1];
                                params[0] = theData;

                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                Basic.caught(ex);
                            }
                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(TABLE, sp);

                    //((JTable) input).addActionListener(action);
                    input.setMinimumSize(new Dimension(200, 25));
                    panel.add(input, constraints);
                    actions.add(action);
                    break;
                }
                case "splitstree4.algorithms.util.StateMatrix":

                    // 	java.io.File > show current File in a TextField,
                    //	show browse button
                    break;
                case "java.io.File": {

                    final JComponent input = new JTextField(defaultVal.toString(), 12);
                    JFileChooser fileChooser = new JFileChooser();

                    AbstractAction action = new AbstractAction() {
                        public void actionPerformed(ActionEvent event) {
                            Transformation transform =
                                    (Transformation) getValue(DirectorActions.TRANSFORM);
                            Method method = (Method) getValue(OPT_SETTER);

                            try {
                                String str = ((JTextField) getValue(TEXT_FIELD)).getText();
                                Object[] params = new Object[1];
                                try {
                                    params[0] = new File(str);
                                } catch (NullPointerException ex1) {
                                    params[0] = new File("");
                                    throw ex1;
                                }
                                method.invoke(transform, params);
                            } catch (Exception ex) {
                                System.err.println("Method invoke failed 8: " + ex);
                            }

                        }
                    };
                    action.putValue(DirectorActions.TRANSFORM, transform);
                    action.putValue(OPT_SETTER, Configurator.getSetterMethod(transform, optName));
                    action.putValue(OPT_GETTER, Configurator.getGetterMethod(transform, optName));
                    action.putValue(TEXT_FIELD, input);

                    AbstractAction browseAction = new AbstractAction("browse") {
                        public void actionPerformed(ActionEvent e) {
                        /*
                        Transformation transform =
                                (Transformation) getValue(DirectorActions.TRANSFORM);
                        Method method = (Method) getValue(OPT_SETTER);
                         */

                            JFileChooser chooser = (JFileChooser) getValue(FILE_CHOOSER);
                            int returnVal = chooser.showDialog((JPanel) getValue("OPT_PANEL"), "Choose File");
                            switch (returnVal) {
                                case JFileChooser.APPROVE_OPTION:
                                    JTextField input = (JTextField) getValue(TEXT_FIELD);
                                    input.setText(chooser.getSelectedFile().toString());
                                    break;
                                case JFileChooser.CANCEL_OPTION:
                                    break;
                                case JFileChooser.ERROR_OPTION:
                                    break;
                            }
                        }
                    };

                    browseAction.putValue(DirectorActions.TRANSFORM, transform);
                    browseAction.putValue(FILE_CHOOSER, fileChooser);
                    browseAction.putValue("OPT_PANEL", panel);
                    browseAction.putValue(TEXT_FIELD, input);

                    JButton browseButton = new JButton(browseAction);
                    browseButton.setToolTipText("File");

                    input.setMinimumSize(new Dimension(250, 22));
                    input.setToolTipText("File");

                    panel.add(input, constraints);
                    constraints.gridx = 6;
                    panel.add(browseButton, constraints);

                    actions.add(action);
                    break;
                }
            }
            count++;
        }
        return panel;
    }

    /**
     * syncronizes the transform to the GUI
     *
     * @param doc  the document
     * @param list
     */
    public static void syncronizeTransform2Tab(Document doc, List list) {
        if (list != null) {
            for (Object aList : list) {
                AbstractAction action = (AbstractAction) aList;

                //System.err.println( (action.getValue(DirectorActions.TRANSFORM)).getClass());

                Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);

                Method method = (Method) action.getValue(OPT_GETTER);
                if (transform != null && method != null) {
                    Method selection = (Method) action.getValue(OPT_SELECTION);
                    Object result = null;
                    try {
                        result = method.invoke(transform);
                    } catch (Exception ex) {
                        System.err.println("Method invoke failed 7: " + ex);
                    }
                    if (result != null) {
                        if ((result instanceof String) && selection != null) {
                            Method method2 = (Method) action.getValue(OPT_SELECTION);
                            try {
                                Object params[] = new Object[1];
                                params[0] = doc;
                                List list2 = (List) method2.invoke(transform, params);
                                Iterator it2 = list2.iterator();
                                JComboBox input = (JComboBox) action.getValue(OPT_COMBOBOX);
                                input.removeAllItems();
                                // System.err.println("Reloading: ");
                                while (it2.hasNext()) {
                                    String item = (String) it2.next();
                                    //System.err.println("\t" + item);
                                    input.addItem(item);
                                }
                                input.setSelectedItem(result);
                            } catch (Exception ex) {
                                Basic.caught(ex);
                            }
                        } else if ((result instanceof String) || (result instanceof Double)
                                || (result instanceof Integer)) {
                            JTextField input = (JTextField) action.getValue(TEXT_FIELD);
                            input.setText(result.toString());
                        } else if (result instanceof Boolean) {
                            JCheckBox input = (JCheckBox) action.getValue(CHECK_BOX);
                            boolean flag = (Boolean) result;
                            input.setSelected(flag);
                        } else if (result instanceof double[]) {
                            SpreadSheet input = (SpreadSheet) action.getValue(TABLE);
                            double[] vals = ((double[]) result);
                            int nstates = vals.length;
                            // TODO: DAVE, please fix so that change of length of array is possible
                            //SheetCell[][] cells = new SheetCell[1][nstates];
                            for (int i = 0; i < nstates; i++) {
                                input.getModel().setValueAt(vals[i], 0, i);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * @author miguel
     *         <p/>
     *         An inner class to render a combobox that contains objects.
     *         (One that may contain Separators as well)
     */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer {
        final String SEPARATOR = "SEPARATOR";
        JSeparator separator;

        public ComboBoxRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(1, 1, 1, 1));
            separator = new JSeparator(JSeparator.HORIZONTAL);
        }

        public Component getListCellRendererComponent(JList list, // uses this object's colors to set up foreground and background colors and set up the font.
                                                      Object value, // the object to render.
                                                      int index, // the index of the object to render.
                                                      boolean isSelected, // determine which colors to use.
                                                      boolean cellHasFocus)// indicates whether the  object to render has the focus.
        {

            String str = (value == null) ? "" : value.toString();

            if (SEPARATOR.equals(str)) {
                return separator;
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value == null) return this;

            setFont(list.getFont());
            String name = (String) (((AbstractAction) value).getValue(AbstractAction.NAME));
            //setText(str);
            setText(name);
            return this;
        }
    }

    /**
     * @author miguel
     *         <p/>
     *         An inner class to listen to actions performed in a ComboBox with Separators
     */
    class BlockComboListener implements ActionListener {
        final String SEPARATOR = "SEPARATOR";
        JComboBox combo;
        Object currentItem;
        int currentIndex;

        BlockComboListener(JComboBox combo) {
            this.combo = combo;
            combo.setSelectedIndex(-1); //No selected item
            currentItem = combo.getSelectedItem();
            currentIndex = combo.getSelectedIndex();
        }

        public void actionPerformed(ActionEvent e) {
            String tempItemName = combo.getSelectedItem().toString();
            int i = combo.getSelectedIndex();

            if (SEPARATOR.equals(tempItemName)) {
                if (currentIndex > i) {
                    System.out.println("I'm here!");
                    combo.setSelectedIndex(i - 1);
                    currentItem = combo.getItemAt(i - 1);
                    currentIndex = i - 1;
                } else {
                    //System.out.println("I'm not, because i = " + i + "and currentIndex = " + currentIndex);
                    combo.setSelectedIndex(i + 1);
                    currentItem = combo.getItemAt(i + 1);
                    currentIndex = i + 1;
                }
                //System.out.println(tempItemName);
                //System.out.println((String) (((AbstractAction) currentItem).getValue(AbstractAction.NAME)));
            } else {
                currentItem = combo.getItemAt(i);
            }
        }
    }
}
