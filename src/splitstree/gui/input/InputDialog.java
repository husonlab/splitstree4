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

package splitstree.gui.input;

import jloda.util.ProgramProperties;
import splitstree.gui.main.MainViewer;
import splitstree.gui.main.TextEditor;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import java.awt.*;

/**
 * Dialog for inputting data
 * Daniel Huson and David Bryant, 11.2010
 */
public class InputDialog extends JDialog {
    public static final String GUESS = "GUESS";
    public static final String NEXUS = "NEXUS";
    public static final String OLDNEXUS = "OLDNEXUS";
    public static final String NEWICK = "NEWICK";
    public static final String PHYLIPSEQUENCES = "PHYLIPSEQUENCES";
    public static final String PHYLIPDISTANCES = "PHYLIPDISTANCES";

    final private MainViewer viewer;
    final private TextEditor editor;
    final private UndoManager undoManager;

    private static String lastInput = null;

    private String format = GUESS;

    /**
     * constructor
     *
     * @param viewer
     */
    public InputDialog(final MainViewer viewer) {
        this.viewer = viewer;
        final Actions actions = new Actions(this);
        setModal(true);
        setTitle("Enter Data Dialog - " + ProgramProperties.getProgramName());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(actions.getPrintIt());
        fileMenu.addSeparator();
        fileMenu.add(actions.getClose());
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(actions.getUndo());
        editMenu.add(actions.getRedo());
        editMenu.addSeparator();
        editMenu.add(actions.getCut());
        editMenu.add(actions.getCopy());
        editMenu.add(actions.getPaste());
        editMenu.addSeparator();
        editMenu.add(actions.getResetEditor());
        editMenu.addSeparator();
        editMenu.add(actions.getGotoLine());

        menuBar.add(editMenu);
        JMenu formatMenu = new JMenu("Format");
        ButtonGroup group = new ButtonGroup();
        JRadioButton but = new JRadioButton(actions.getGuessFormat());
        formatMenu.add(but);
        but.setSelected(true);
        group.add(but);
        but = new JRadioButton(actions.getNexusFormat());
        formatMenu.add(but);
        group.add(but);
        but = new JRadioButton(actions.getOldNexusFormat());
        formatMenu.add(but);
        group.add(but);
        but = new JRadioButton(actions.getNewickFormat());
        formatMenu.add(but);
        group.add(but);
        but = new JRadioButton(actions.getPhylipSequencesFormat());
        formatMenu.add(but);
        group.add(but);
        but = new JRadioButton(actions.getPhylipSequencesFormat());
        formatMenu.add(but);
        group.add(but);
        menuBar.add(formatMenu);

        setJMenuBar(menuBar);

        setSize(600, 600);
        setLocationRelativeTo(viewer.getFrame());

        getContentPane().setLayout(new BorderLayout());

        editor = new TextEditor();

        JScrollPane dataScrollPane = editor.initializeEditor(viewer);
        undoManager = new UndoManager() {
            public void undoableEditHappened(UndoableEditEvent e) {
                super.undoableEditHappened(e);
                actions.updateEnableState();
            }
        };
        actions.setUndoManager(undoManager);

        if (lastInput != null)
            editor.getInputTextArea().setText(lastInput);
        editor.getInputTextArea().getDocument().addUndoableEditListener(undoManager);

        getContentPane().add(dataScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(new JButton(actions.getClose()));
        buttonPanel.add(new JButton(actions.getExecuteText()));
        buttonPanel.add(Box.createHorizontalStrut(10));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        actions.updateEnableState();
    }

    /**
     * get the associated editor
     *
     * @return editor
     */
    public TextEditor getEditor() {
        return editor;
    }

    /**
     * get the main viewer
     *
     * @return main viewer
     */
    public MainViewer getViewer() {
        return viewer;
    }

    public void setLastInputText(String text) {
        lastInput = text;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
