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

import jloda.gui.director.IDirector;
import jloda.gui.find.SearchManager;
import jloda.gui.message.MessageWindow;
import jloda.util.*;
import splitstree.core.BlockChooser;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;
import splitstree.util.EnableDisable;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * actions for the input dialog
 * Daniel Huson and David Bryant, 11.2010
 */
public class Actions {
    final private InputDialog inputDialog;
    final private Director dir;
    final private MainViewer viewer;
    private UndoManager undoManager;

    // we keep a list of critical actions: these are ones that must be disabled
    // when the director tells us to block user input
    private List<Action> all = new LinkedList<>();

    /**
     * setup the  actions
     *
     * @param inputDialog
     */
    public Actions(InputDialog inputDialog) {
        super();
        this.inputDialog = inputDialog;
        viewer = inputDialog.getViewer();
        this.dir = viewer.getDir();
    }

    /**
     * sets the undo manager
     *
     * @param undoManager
     */
    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    /**
     * enable or disable critical actions
     *
     * @param flag show or hide?
     */
    public void setEnableCritical(boolean flag) {
        DirectorActions.setEnableCritical(all, flag);
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);

        for (Action anAll : all) {
            AbstractAction action = (AbstractAction) anAll;
            // new actions carry an EnableDisable class to decide whether to enable
            EnableDisable ed = (EnableDisable) action.getValue(EnableDisable.ENABLEDISABLE);
            if (ed != null)
                action.setEnabled(ed.enable());
        }
        updateUndoRedo();
    }

    private AbstractAction printIt;

    /**
     * viewer has its own print action
     *
     * @return print action
     */
    public AbstractAction getPrintIt() {
        AbstractAction action = printIt;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                PrinterJob job = PrinterJob.getPrinterJob();
                TextPrinter printer = new TextPrinter(inputDialog.getEditor().getEditText(),
                        inputDialog.getEditor().getEditTextFont());
                job.setPrintable(printer);

                // Put up the dialog box
                if (job.printDialog()) {
                    // Print the job if the user didn't cancel printing
                    try {
                        job.print();
                    } catch (Exception ex) {
                        System.err.println("Print failed: " + ex);
                    }
                }

            }
        };
        action.putValue(AbstractAction.NAME, "Print...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Print the graph");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Print16.gif"));


        all.add(action);
        return printIt = action;
    }

    private AbstractAction close; // close this viewer

    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setVisible(false);
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this dialog");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        all.add(action);
        return close = action;
    }


    private AbstractAction cut;

    public AbstractAction getCut() {
        AbstractAction action = cut;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.getActions().getCut().actionPerformed(event);
            }
        };
        action.putValue(AbstractAction.NAME, "Cut");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cut");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Cut16.gif"));
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return cut = action;
    }

    private AbstractAction copy;

    public AbstractAction getCopy() {
        AbstractAction action = copy;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.getActions().getCopy().actionPerformed(event);
            }
        };
        action.putValue(AbstractAction.NAME, "Copy");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy graph to clipboard");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Copy16.gif"));
        all.add(action);
        return copy = action;
    }


    private AbstractAction paste;

    public AbstractAction getPaste() {
        AbstractAction action = paste;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.getActions().getPaste().actionPerformed(event);
            }
        };
        action.putValue(AbstractAction.NAME, "Paste");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Paste");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Paste16.gif"));

        all.add(action);
        return paste = action;
    }


    AbstractAction resetEditor;

    public AbstractAction getResetEditor() {
        AbstractAction action = resetEditor;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!inputDialog.getEditor().getEditText().equals(inputDialog.getEditor().getEditTextOriginal())) {
                    int result = JOptionPane.showConfirmDialog(inputDialog,
                            "Clear text? All changes will be lost");
                    if (result == JOptionPane.YES_OPTION) {
                        inputDialog.getEditor().setEditText("");
                    }
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear the current text");
        action.putValue(AbstractAction.NAME, "Clear");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Reset16.gif"));
        all.add(action);
        return resetEditor = action;
    }


    AbstractAction executeText;

    public AbstractAction getExecuteText() {
        AbstractAction action = executeText;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String text = inputDialog.getEditor().getEditText().trim();
                if (text.length() == 0)
                    return;
                inputDialog.setLastInputText(text);

                Director theDir;
                Document theDoc;
                boolean makeNewDocument = ((dir.getDocument() != null && dir.getDocument().isValidByName(Taxa.NAME)));
                if (makeNewDocument) {
                    theDir = Director.newProject();
                    theDoc = theDir.getDocument();
                } else {
                    theDir = dir;
                    theDoc = dir.getDocument();
                }

                if (splitstree.util.NexusFileFilter.isNexusText(text)) {
                    try {
                        System.err.println("Attempting to parse Nexus format");
                        theDoc.readNexus(new StringReader(text));
                        theDir.showMainViewer();
                    } catch (Exception ex) {
                        theDoc.clear();
                        Basic.caught(ex);
                        new Alert(theDir.getMainViewerFrame(), "Enter data failed: " + ex.getMessage());
                        if (makeNewDocument)
                            try {
                                theDoc.setDirty(false);
                                theDir.close();
                            } catch (CanceledException e) {
                            }
                        return;
                    }
                } else {
                    System.err.println("Attempting data conversion to Nexus format");
                    try {
                        theDoc.importDataFromString(theDir.getMainViewerFrame(), text);
                        theDir.showMainViewer();
                    }
                    catch (Exception ex) {
                        theDoc.clear();
                        new Alert(theDir.getMainViewerFrame(), "Enter data failed: " + ex.getMessage());
                        if (makeNewDocument)
                            try {
                                theDoc.setDirty(false);
                                theDir.close();
                            } catch (CanceledException e) {
                            }
                        return;

                    }
                }

                if (BlockChooser.show(inputDialog, theDoc)) {
                    theDir.read(new StringReader(theDoc.toString()));
                    theDir.notifyUpdateViewer(IDirector.ALL);
                    inputDialog.setVisible(false);
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Execute the entered text");
        action.putValue(AbstractAction.NAME, "Execute");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/media/Play16.gif"));

        all.add(action);
        return executeText = action;
    }

    private AbstractAction gotoLine;

    /**
     * getGotoLine() is an action that points you cursor to
     * a specific line number and selects that line.
     */
    public AbstractAction getGotoLine() {
        if (gotoLine != null)
            return gotoLine;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int lineNo = Integer.parseInt(JOptionPane.showInputDialog(inputDialog, "Go to line:")) - 1;
                    int a = inputDialog.getEditor().getInputTextArea().getLineStartOffset(lineNo);
                    int b = inputDialog.getEditor().getInputTextArea().getLineStartOffset(lineNo + 1) - 1;
                    inputDialog.getEditor().getInputTextArea().setCaretPosition(a);
                    inputDialog.getEditor().getInputTextArea().requestFocus();
                    inputDialog.getEditor().getInputTextArea().select(a, b);
                } catch (Exception ex) {
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Go To Line...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Go to Line");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("GotoLine16.gif"));
        all.add(action);
        return gotoLine = action;
    }//End of getGotoLine()

    private AbstractAction undo;

    /**
     * undo action
     */

    public AbstractAction getUndo() {
        AbstractAction action = undo;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    undoManager.undo();
                } catch (CannotRedoException ex) {
                }
                updateUndoRedo();
            }
        };

        action.putValue(AbstractAction.NAME, "Undo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Undo");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Undo16.gif"));

        all.add(action);
        return undo = action;
    }//End of getUndo


    private AbstractAction redo;

    /**
     * redo action
     */
    public AbstractAction getRedo() {
        AbstractAction action = redo;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    undoManager.redo();
                } catch (CannotRedoException ex) {
                }
                updateUndoRedo();
            }
        };

        action.putValue(AbstractAction.NAME, "Redo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Redo");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Redo16.gif"));
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return redo = action;

    }//End of getRedo

    /**
     * /**
     * updates the undo action
     */
    public void updateUndoRedo() {
        getUndo().setEnabled(undoManager.canUndo());
        getRedo().setEnabled(undoManager.canRedo());
    }


    AbstractAction FindReplaceAction;

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getFindReplaceAction() {
        AbstractAction action = FindReplaceAction;
        if (action != null)
            return action;

        //setup FindReplace Window event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SearchManager searchManager = SearchManager.getInstance();
                if (SearchManager.getInstance() == null) {
                    searchManager = new SearchManager(dir, "Find/Replace - SplitsTree", viewer.getSearchers(), true);
                    if (MessageWindow.getInstance() != null && !MessageWindow.getInstance().isVisible())
                        searchManager.setEnabled("Messages", false);
                    dir.addViewer(searchManager);
                }
                searchManager.getFrame().setVisible(true);
                searchManager.getFrame().setState(JFrame.NORMAL);
                searchManager.getFrame().toFront();
            }
        };
        action.putValue(AbstractAction.NAME, "Find/Replace...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open the Find/Replace dialog");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Find16.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE); // otherwise won't get enable state right
        all.add(action);
        return FindReplaceAction = action;
    }

    private AbstractAction findReplaceAgain = getFindReplaceAgain();

    public AbstractAction getFindReplaceAgain() {
        AbstractAction action = findReplaceAgain;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SearchManager searchManager = (SearchManager) dir.getViewerByClass(SearchManager.class);
                if (searchManager != null)
                    searchManager.applyFindNext();
            }
        };
        action.putValue(AbstractAction.NAME, "Find Again");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Find next label matching pattern");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Find16.gif"));
        all.add(action);

        return findReplaceAgain = action;
    }

    private AbstractAction guessFormat;

    public AbstractAction getGuessFormat() {
        AbstractAction action = guessFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.GUESS);

            }
        };

        action.putValue(AbstractAction.NAME, "Guess");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to guess the data format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return guessFormat = action;

    }

    private AbstractAction nexusFormat;

    public AbstractAction getNexusFormat() {
        AbstractAction action = nexusFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.NEXUS);

            }
        };

        action.putValue(AbstractAction.NAME, "Nexus");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Nexus format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return nexusFormat = action;

    }

    private AbstractAction oldNexusFormat;

    public AbstractAction getOldNexusFormat() {
        AbstractAction action = oldNexusFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.OLDNEXUS);

            }
        };

        action.putValue(AbstractAction.NAME, "Old Nexus");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Oldnexus format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return oldNexusFormat = action;

    }

    private AbstractAction phylipSequencesFormat;

    public AbstractAction getPhylipSequencesFormat() {
        AbstractAction action = phylipSequencesFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.PHYLIPSEQUENCES);

            }
        };

        action.putValue(AbstractAction.NAME, "Phylip Sequences");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Phylip sequences format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return phylipSequencesFormat = action;

    }

    private AbstractAction phylipDistancesFormat;

    public AbstractAction getPhylipDistancesFormat() {
        AbstractAction action = phylipDistancesFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.PHYLIPDISTANCES);

            }
        };

        action.putValue(AbstractAction.NAME, "Phylip Distances");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Phylip distances format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return phylipDistancesFormat = action;

    }

    private AbstractAction newickFormat;

    public AbstractAction getNewickFormat() {
        AbstractAction action = newickFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setFormat(inputDialog.NEWICK);

            }
        };

        action.putValue(AbstractAction.NAME, "Newick");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Newick format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return newickFormat = action;

    }
}
