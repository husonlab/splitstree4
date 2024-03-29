/*
 * Actions.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.input;

import jloda.swing.director.IDirector;
import jloda.swing.find.SearchManager;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextPrinter;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.core.BlockChooser;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;
import splitstree4.util.EnableDisable;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.io.StringReader;
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
	private final List<Action> all = new LinkedList<>();

    /**
     * setup the  actions
     *
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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Print the graph");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Print16.gif"));


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
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cut");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Cut16.gif"));
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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy graph to clipboard");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Copy16.gif"));
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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Paste");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Paste16.gif"));

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

                if (splitstree4.util.NexusFileFilter.isNexusText(text)) {
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
							} catch (CanceledException ignored) {
							}
                        return;
                    }
                } else {
                    System.err.println("Attempting data conversion to Nexus format");
                    try {
                        theDoc.importDataFromString(theDir.getMainViewerFrame(), text);
                        theDir.showMainViewer();
                    } catch (Exception ex) {
                        theDoc.clear();
                        new Alert(theDir.getMainViewerFrame(), "Enter data failed: " + ex.getMessage());
                        if (makeNewDocument)
							try {
								theDoc.setDirty(false);
								theDir.close();
							} catch (CanceledException ignored) {
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
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Play16.gif"));

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
				} catch (Exception ignored) {
				}
            }
        };
        action.putValue(AbstractAction.NAME, "Go To Line...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
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
				} catch (CannotRedoException ignored) {
				}
                updateUndoRedo();
            }
        };

        action.putValue(AbstractAction.NAME, "Undo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Undo");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Undo16.gif"));

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
				} catch (CannotRedoException ignored) {
				}
                updateUndoRedo();
            }
        };

        action.putValue(AbstractAction.NAME, "Redo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Redo");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Redo16.gif"));
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
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Find16.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Find16.gif"));
        all.add(action);

        return findReplaceAgain = action;
    }

    private AbstractAction guessFormat;

    public AbstractAction getGuessFormat() {
        AbstractAction action = guessFormat;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
				inputDialog.setFormat(InputDialog.GUESS);

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
				inputDialog.setFormat(InputDialog.NEXUS);

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
				inputDialog.setFormat(InputDialog.OLDNEXUS);

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
				inputDialog.setFormat(InputDialog.PHYLIPSEQUENCES);

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
				inputDialog.setFormat(InputDialog.PHYLIPDISTANCES);

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
				inputDialog.setFormat(InputDialog.NEWICK);

            }
        };

        action.putValue(AbstractAction.NAME, "Newick");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Try to read data in Newick format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return newickFormat = action;

    }
}
