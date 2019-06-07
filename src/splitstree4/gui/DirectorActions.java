/**
 * DirectorActions.java
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
 * <p/>
 * Actions that the viewer gets from the director to present to the user
 *
 * @author huson
 * Date: 26-Nov-2003
 */
/**
 * Actions that the viewer gets from the director to present to the user
 * @author huson
 * Date: 26-Nov-2003
 */
package splitstree4.gui;

import jloda.swing.director.IDirectorListener;
import jloda.swing.director.ProjectManager;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.Alert;
import jloda.swing.util.Message;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import splitstree4.algorithms.Transformation;
import splitstree4.core.Document;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.gui.bootstrap.BootstrapDialog;
import splitstree4.gui.bootstrap.ConfidenceNetworkDialog;
import splitstree4.gui.main.ExportWindow;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.main.SyncViewerToDoc;
import splitstree4.gui.preferences.PreferencesWindow;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Bootstrap;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.util.NexusFileFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.*;

/**
 * Common event actions that are managed by the director
 *
 * @author huson
 *         Date: 26-Nov-2003
 */
public class DirectorActions implements IDirectorListener {
    // action property keys
    public final static String TEXTAREA = "TEXTAREA"; // text area object
    public final static String JCHECKBOX = "JCHECKBOX"; // menu checkbox item
    public final static String JBUTTON = "JBUTTON";
    public final static String CRITICAL = "CRITICAL"; // is action critical? bool
    public final static String DEFAULT = "DEFAULT"; // be in menu by default?
    public final static String DEPENDS_ON = "DEPENDS_ON"; // name of block on which
    public final static String TRANSFORM = "TRANSFORM"; // the transformer
    public final static String SELECT_STATE = "SELECT_STATE"; // a state class used to select/deselect
    public final static String ENABLE_STATE = "ENABLE_STATE"; // sate class to enable/disable
    // action depends on, if any

    // list of all critical actions that must be disabled during update
    final private List<Action> all = new LinkedList<>();
    final private Director dir; // need this to update states

    /**
     * Constructor. Sets up all the actions
     *
     * @param dir
     */
    DirectorActions(final Director dir) {
        this.dir = dir;
    }

    /**
     * enable/disable all critical (non-concurrent) actions given in the list
     *
     * @param flag enable/disable
     */
    static public void setEnableCritical(List<Action> actions, boolean flag) {
        for (Action action : actions) {
            if (action.getValue(DirectorActions.CRITICAL) != null
                    && (((Boolean) action.getValue(DirectorActions.CRITICAL))).equals(Boolean.TRUE))
                action.setEnabled(flag);
        }
    }

    /**
     * Updates the 'enable' state for all action items.
     * If an action is a transform, checks the relevant 'from' block is enabled and the
     * transform is applicable.
     * If the action has an associated Menu checkbox, this is selected or deselected as appropriate.
     *
     * @param dir     Director
     * @param actions List of actions to be examined.
     */
    static public void updateEnableState(Director dir, List<Action> actions) {

        for (Action action : actions) {
            boolean enable = false;
            if (action.getValue(DirectorActions.DEPENDS_ON) == null)
                enable = true;
            else {
                String name = (String) action.getValue(DirectorActions.DEPENDS_ON);
                if (dir.getDocument().isValidByName(name)) {
                    Transformation transform = null;
                    try {
                        transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    } catch (ClassCastException e) {
                    }
                    if (transform == null || dir.getDocument().isApplicable(transform))
                        enable = true;
                    if (transform != null && action.getValue(DirectorActions.JCHECKBOX) != null) {
                        JCheckBoxMenuItem cbox = (JCheckBoxMenuItem) action.getValue(DirectorActions.JCHECKBOX);
                        boolean check = false;
                        Assumptions assumptions = dir.getDocument().getAssumptions();
                        if (dir.getDocument().isValid(assumptions)
                                && assumptions.isSetTransform(transform))
                            check = true;
                        cbox.setSelected(check);
                    }
                }
            }
            action.setEnabled(enable);
        }
    }

    /**
     * disable all critical actions
     */
    public void lockUserInput() {
        setEnableCritical(getAll(), false);
        // disable all necessary actions
    }

    /**
     * enable all appropriate actions after update
     */
    public void unlockUserInput() {
        // enable all critical states again
        setEnableCritical(getAll(), true);

        // now set document specific enablings:
        updateEnableState(dir, getAll());
    }

    // don't need these, just for interface

    public void updateView(String what) {
    }

    // don't need this, just for interface

    public void destroyView() {
    }

    public void setUptoDate(boolean flag) {
    }

    /**
     * returns a list of all actions
     *
     * @return all actions
     */
    public List<Action> getAll() {
        return all;
    }

    ////////// all action events defined and accessed here:

    private AbstractAction newProject;

    /**
     * new project action
     */
    public AbstractAction getNewProject() {
        AbstractAction action = newProject;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Director newDir = Director.newProject();
                newDir.showMainViewer();
                MainViewer newMainViewer = (MainViewer) newDir.getMainViewer();
            }
        };
        action.putValue(AbstractAction.NAME, "New...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open a new empty document");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/New16.gif"));

        all.add(action);
        return newProject = action;
    }

    private Map menuTitleActions = new HashMap();

    /**
     * returns a  menu action
     */
    public AbstractAction getMenuTitleAction(final String name, char mnemonicKey) {
        AbstractAction action = (AbstractAction) menuTitleActions.get(name);
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, name);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, name + " menu");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) (mnemonicKey));
        menuTitleActions.put(name, action);
        all.add(action);
        return action;
    }

    /* importFile and openFile are identical except for one thing - on the mac, open restricts files to .nex or .nxs.
    * Both menus will only appear on the mac.
    */
    private AbstractAction openFile;

    public AbstractAction getOpenFile() {
        AbstractAction action = openFile;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                File lastOpenFile = ProgramProperties.getFile("OpenFile");

                MainViewer mainViewer = dir.showMainViewer();

                boolean viewerIsEmpty = ((dir.getDocument() == null || !dir.getDocument().isValidByName(Taxa.NAME))
                        && (mainViewer.getTextEditor().getEditText() == null || mainViewer.getTextEditor().getEditText().length() == 0));

                if (!viewerIsEmpty) // viewer not empty, open a new one:
                {
                    Director newDirector = Director.newProject();
                    newDirector.getActions().getOpenFile().actionPerformed(null);
                    return;
                }

                File file = null;
                boolean choseFile = false;

                if (ProgramProperties.isMacOS()) {
                    //Use native file dialog on mac
                    FileDialog dialog = new FileDialog(dir.getMainViewerFrame(), "Open file", FileDialog.LOAD);
                    dialog.setFilenameFilter(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return true;
                        }
                    });

                    dialog.setVisible(true);
                    if (dialog.getFile() != null) {
                        file = new File(dialog.getDirectory(), dialog.getFile());
                        choseFile = true;
                    }
                } else {
                    JFileChooser chooser = new JFileChooser(lastOpenFile);
                    // Add the FileFilter for the Import Plugins
                    try {
                        ArrayList fileFilter = ImportManager.getFileFilter();
                        for (Object aFileFilter : fileFilter) {
                            FileFilter filter = (FileFilter) aFileFilter;
                            chooser.addChoosableFileFilter(filter);
                        }
                    } catch (Exception e) {
                        Basic.caught(e);
                    }
                    chooser.setAcceptAllFileFilterUsed(true);

                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        file = chooser.getSelectedFile();
                        choseFile = true;
                    }
                }

                if (choseFile) {
                    if (file.exists() && file.canRead()) {
                        lastOpenFile = file;
                        ProgramProperties.put("OpenFile", lastOpenFile.getAbsolutePath());
                        SplitsTreeProperties.addRecentFile(lastOpenFile);
                        if (NexusFileFilter.isNexusFile(file)) {
                            dir.openFile(file);

                        } else {
                            dir.getDocument().setFile(file, ".nex");
                            dir.notifyUpdateViewer(Director.TITLE);
                            dir.importFile(file);
                        }
                    } else {
                        new Alert(dir.getMainViewerFrame(), "File not found:\n" + file);
                    }
                } else {
                    if (ProjectManager.getNumberOfProjects() > 1) {
                        try {
                            dir.close();

                        } catch (CanceledException ex) {
                        }

                    }
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Open...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('O'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open an input file");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Open16.gif"));

        all.add(action);

        return openFile = action;
    }

    Map openRecent = new HashMap();

    public AbstractAction getOpenRecent(final String path) {
        if (openRecent.keySet().contains(path))
            return (AbstractAction) openRecent.get(path);
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                MainViewer mainViewer = dir.showMainViewer();

                boolean viewerIsEmpty = ((dir.getDocument() == null || !dir.getDocument().isValidByName(Taxa.NAME))
                        && (mainViewer.getTextEditor().getEditText() == null || mainViewer.getTextEditor().getEditText().length() == 0));

                if (!viewerIsEmpty) // viewer not empty, open a new one:
                {
                    Director newDirector = Director.newProject();
                    newDirector.getActions().getOpenRecent(path).actionPerformed(null);
                    return;
                }
                File file = new File(path);
                if (file.exists() && file.canRead()) {
                    ProgramProperties.put("OpenFile", file.getAbsolutePath());
                    SplitsTreeProperties.addRecentFile(file);
                    if (NexusFileFilter.isNexusFile(file)) {
                        dir.openFile(file);
                    } else {
                        dir.getDocument().setFile(file, ".nex");
                        dir.notifyUpdateViewer(Director.TITLE);
                        dir.importFile(file);
                    }
                } else {
                    new Alert(dir.getMainViewerFrame(), "File not found:\n" + file);
                }
            }
        };
        String fName;
        if (path.length() <= 40)
            fName = path;
        else
            fName = "..." + path.substring(path.length() - 35);
        action.putValue(AbstractAction.NAME, fName);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open recent file: " + fName);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Open16.gif"));

        all.add(action);

        openRecent.put(path, action);
        return action;
    }


    // static File lastExportFile = new File(System.getProperty("user.dir"));
    private AbstractAction exportFile;
    ExportWindow exportWindow;

    public AbstractAction getExportFile() {
        AbstractAction action = exportFile;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SyncViewerToDoc.sync((MainViewer) dir.getViewerByClass(MainViewer.class), dir.getDocument());
                if (dir.containsViewer(exportWindow)) {
                    exportWindow.getFrame().setState(JFrame.NORMAL);
                    exportWindow.getFrame().toFront();
                } else {
                    exportWindow = new ExportWindow(dir);
                    dir.addViewer(exportWindow);
                    exportWindow.updateView(Director.ALL);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Export...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('X'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Export data in other format");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Export16.gif"));

        all.add(action);

        return exportFile = action;
    }

    private AbstractAction close; // close this director

    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                try {
                    dir.notifyDestroyViewer();
                } catch (CanceledException ex) {
                    //Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this viewer");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));

        // not critical!
        all.add(action);

        return close = action;
    }

    private AbstractAction quit; // quit program

    public AbstractAction getQuit() {
        AbstractAction action = quit;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ProgramProperties.store();
                try {
                    ProjectManager.closeAll();
                    System.exit(0);
                } catch (CanceledException ex) {
                    //Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Quit");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('Q'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Quit the program");

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Stop16.gif"));

        // not critical!
        all.add(action);

        return quit = action;
    }

    private AbstractAction clear; // clear the document

    public AbstractAction getClear() {
        AbstractAction action = clear;
        if (action != null) return action;

        // Clear the document
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("clear");
            }
        };
        action.putValue(AbstractAction.NAME, "Clear");
        // clear.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear document");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);

        all.add(action);

        return clear = action;
    }

    ////// basic textComponent Actions
    private AbstractAction undo;

    /**
     * undo action
     */
    public AbstractAction getUndo(final UndoManager undoManager) {
        AbstractAction action = undo;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    undoManager.undo();
                } catch (CannotUndoException ex) {
                }
                updateUndoRedo(undoManager);
            }
        };
        action.setEnabled(false);

        action.putValue(AbstractAction.NAME, "Undo");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('U'));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        // quit.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Undo");

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Undo16.gif"));

        //all.add(action);
        return undo = action;
    }//End of getUndo

    /**
     * updates the undo action
     */
    public void updateUndoRedo(UndoManager undoManager) {
        if (undoManager.canUndo()) {
            getUndo(((MainViewer) dir.getMainViewer()).getUndoManagerText()).setEnabled(true);
        } else {
            getUndo(((MainViewer) dir.getMainViewer()).getUndoManagerText()).setEnabled(false);
        }
        if (undoManager.canRedo()) {
            getRedo(((MainViewer) dir.getMainViewer()).getUndoManagerText()).setEnabled(true);
        } else {
            getRedo(((MainViewer) dir.getMainViewer()).getUndoManagerText()).setEnabled(false);
        }
    }

    private AbstractAction redo;

    /**
     * redo action for text as in source and data tabs
     */
    public AbstractAction getRedo(final UndoManager undoManager) {
        AbstractAction action = redo;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    undoManager.redo();
                } catch (CannotRedoException ex) {
                }
                updateUndoRedo(undoManager);
            }
        };
        action.setEnabled(false);

        action.putValue(AbstractAction.NAME, "Redo");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('R'));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        // quit.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Redo");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Redo16.gif"));

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        //all.add(action);
        return redo = action;

    }//End of getRedo

    // need an instance to get default textComponent Actions
    private DefaultEditorKit kit;


    private Action cut;

    public Action getCut() {
        Action action = cut;
        if (action != null) return action;

        if (kit == null) kit = new DefaultEditorKit();

        Action[] defActions = kit.getActions();
        for (Action defAction : defActions) {
            if (defAction.getValue(Action.NAME) == DefaultEditorKit.cutAction) {
                action = defAction;
            }
        }
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) 'T');
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        action.putValue(Action.SHORT_DESCRIPTION, "Cut");

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Cut16.gif"));

        all.add(action);
        return cut = action;
    }

    private Action copy;

    public Action getCopy() {
        Action action = copy;
        if (action != null) return action;

        if (kit == null) kit = new DefaultEditorKit();

        Action[] defActions = kit.getActions();
        for (Action defAction : defActions) {

            if ((defAction.getValue(Action.NAME)).equals(DefaultEditorKit.copyAction)) {
                action = defAction;
            }
        }
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) 'C');
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Copy");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Copy16.gif"));

        all.add(action);
        return copy = action;
    }

    private Action paste;

    public Action getPaste() {
        Action action = paste;
        if (action != null) return action;

        if (kit == null) kit = new DefaultEditorKit();

        Action[] defActions = kit.getActions();
        for (Action defAction : defActions) {
            if (defAction.getValue(Action.NAME) == DefaultEditorKit.pasteAction) {
                action = defAction;
            }
        }
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) 'P');
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Paste");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Paste16.gif"));

        all.add(action);
        return paste = action;
    }

    private Action selectAll;

    public Action getSelectAll() {
        Action action = selectAll;
        if (action != null) return action;

        if (kit == null) kit = new DefaultEditorKit();

        Action[] defActions = kit.getActions();
        for (Action defAction : defActions) {
            if (defAction.getValue(Action.NAME) == DefaultEditorKit.selectAllAction) {
                action = defAction;
            }
        }
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) 'A');
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Select All");

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return selectAll = action;
    }


    private AbstractAction runCommand;

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getRunCommand() {
        AbstractAction action = runCommand;
        if (action != null)
            return action;

        // setup action document event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String command = JOptionPane.showInputDialog("Enter a command",
                        ProgramProperties.get(SplitsTreeProperties.LASTCOMMAND, ""));
                if (command != null) {
                    ProgramProperties.put(SplitsTreeProperties.LASTCOMMAND, command);
                    getMessageWindow().actionPerformed(null);
                    dir.execute(command);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Enter a command...");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('E'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Enter and execute a command");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Command16.gif"));

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        all.add(action);
        return runCommand = action;
    }

    private AbstractAction aboutWindow;

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getAboutWindow() {
        AbstractAction action = aboutWindow;
        if (action != null)
            return action;

        //setup About Window event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SplitsTreeProperties.getAbout().showAboutModal();
            }
        };
        action.putValue(AbstractAction.NAME, "About...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "About SplitsTree and the authors");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/About16.gif"));

        all.add(action);
        return aboutWindow = action;
    }

    // get help on command line stuff
    private AbstractAction commandHelp;

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getCommandHelp() {
        AbstractAction action = commandHelp;
        if (action != null)
            return action;

        //setup help event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                messageAction.actionPerformed(null);
                Document.showUsage(System.err);
            }
        };
        action.putValue(AbstractAction.NAME, "Command Syntax...");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show all command-line commands");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Help16.gif"));

        all.add(action);
        return commandHelp = action;
    }

    //static because there is only one message window for the whole program!
    static private AbstractAction messageAction;
    static public MessageWindow messageWindow;

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getMessageWindow() {
        AbstractAction action = messageAction;
        if (action != null)
            return action;

        //setup Message Window event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (messageWindow != null) {
                    messageWindow.getFrame().setState(JFrame.NORMAL);
                    messageWindow.getFrame().toFront();
                    messageWindow.getFrame().setVisible(true);
                    messageWindow.startCapturingOutput();
                } else {
                    messageWindow = new MessageWindow(ProgramProperties.getProgramIcon(),
                            "Message window - " + SplitsTreeProperties.getVersion(),
                            dir.getMainViewerFrame());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Message Window...");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('M'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open the message window");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/History16.gif"));

        all.add(action);
        return messageAction = action;
    }

    private AbstractAction updateDocument;  // update events

    /**
     * gets an instance of the action event
     *
     * @return action event
     */
    public AbstractAction getUpdateDocument() {
        AbstractAction action = updateDocument;
        if (action != null)
            return action;

        // setup update document event
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("update");
            }
        };
        action.putValue(AbstractAction.NAME, "Update");
        updateDocument.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("update"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Update the document");

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);

        all.add(action);
        return updateDocument = action;
    }


    private AbstractAction preferences;
    private PreferencesWindow pref;

    /**
     * the preferences configuration action
     */
    public AbstractAction getPreferences() {
        AbstractAction action = preferences;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (dir.containsViewer(pref)) {
                    pref.getFrame().setState(JFrame.NORMAL);
                    pref.getFrame().toFront();
                } else {
                    pref = new PreferencesWindow(dir, (MainViewer) dir.getMainViewer());
                    dir.addViewer(pref);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Preferences...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Configure various preferences");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return preferences = action;
    }


    private AbstractAction bootstrapping;

    public AbstractAction getBootstrapping() {
        AbstractAction action = bootstrapping;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Bootstrap bs = dir.getDocument().getBootstrap();
                if (bs == null)
                    bs = new Bootstrap();
                //Determine if the user is currently producing trees
                bs.setCanSaveTrees(dir.getDocument().getTrees() != null);

                BootstrapDialog bootstrapDialog = new BootstrapDialog(bs, dir.getMainViewerFrame());

                if (bootstrapDialog.executeBootstrap()) {
                    System.err.println("Executing bootstrap");
                    System.err.println("Runs = " + bs.getRuns());

                    if (bs.getRuns() > 0) {
                        String execString = "bootstrap runs=" + bs.getRuns();
                        if (bs.getSaveTrees())
                            execString += " saveTrees = yes";

                        dir.execute(execString + "; draw splitlabels=confidence");
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Bootstrap...");

        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Run bootstrapping");
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        all.add(action);
        return bootstrapping = action;
    }

    private AbstractAction bootstrappingNetwork;

    public AbstractAction getBootstrappingNetwork() {
        AbstractAction action = bootstrappingNetwork;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    StringWriter sw = new StringWriter();
                    dir.getDocument().getTaxa().write(sw);
                    dir.getDocument().writeBS(dir.getDocument().getSplits(), sw);
                    String fileName = (dir.getDocument().getFile() != null ? dir.getDocument().getFile().getAbsolutePath() : dir.getDocument().getTitle());
                    Director newDir = Director.newProject(sw.toString(), fileName);
                    newDir.getDocument().setTitle("Bootstrap Network for " + dir.getDocument().getTitle());
                    newDir.showMainViewer();
                } catch (IOException ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Show Bootstrap Network");

        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('o'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the bootstrap network in a new viewer");
        action.putValue(DirectorActions.DEPENDS_ON, Bootstrap.NAME);

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        all.add(action);
        return bootstrappingNetwork = action;
    }


    private AbstractAction confidenceNetwork;

    public AbstractAction getConfidenceNetwork() {
        AbstractAction action = confidenceNetwork;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                Document doc = dir.getDocument();
                if (doc.getBootstrap() != null && doc.getBootstrap().getSplitMatrix() != null) {
                    ConfidenceNetworkDialog dialog = new ConfidenceNetworkDialog(dir.getMainViewerFrame(), 95);
                    dialog.showDialog(dir);
                } else {
                    new Alert("Please perform bootstraps before constructing confidence network");
                    //ToDo: fix this - the action can be enabled when bootstraps are read in from the file w/out split matrix

                }
            }
        };
        action.putValue(AbstractAction.NAME, "Show Confidence Network...");

        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('o'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the confidence network in a new viewer");
        action.putValue(DirectorActions.DEPENDS_ON, Bootstrap.NAME);

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        all.add(action);
        return confidenceNetwork = action;
    }


    private AbstractAction howToCite;

    public AbstractAction getHowToCite() {
        AbstractAction action = howToCite;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                new Message(dir.showMainViewer().getFrame(),
                        "Please cite:\nD.H. Huson and D. Bryant,"
                                + " Application of Phylogenetic Networks in Evolutionary Studies, "
                                + "Molecular Biology and Evolution, 23(2):254-267, 2006.");
            }
        };
        action.putValue(AbstractAction.NAME, "How to Cite...");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Help16.gif"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "How to cite SplitsTree4");

        all.add(action);
        return howToCite = action;
    }


    Map syntaxMap = new HashMap();

    public AbstractAction getSyntaxAction(final String name, final Integer mnemonic) {
        AbstractAction action = (AbstractAction) syntaxMap.get(name);
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.getActions().getMessageWindow().actionPerformed(null);
                try {
                    dir.getDocument().execute("help data=" + name);
                } catch (Exception e) {
                }
            }
        };
        action.putValue(AbstractAction.NAME, name);
        if (mnemonic != null) action.putValue(AbstractAction.MNEMONIC_KEY, mnemonic);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show syntax of " + name);
        all.add(action);
        syntaxMap.put(name, action);
        return action;
    }


    /**
     * PluginData Manager Stuff


     //static because there is only one plugin window for the whole program!
     private AbstractAction pluginManagerAction;
     static private PluginOverview pluginManagerWindow;

     /**
     * gets an instance of the action event
     *
     * @return action event

    public AbstractAction getPluginManagerWindow() {
    AbstractAction action = pluginManagerAction;
    if (action != null)
    return action;

    //setup Message Window event
    action = new AbstractAction() {
    public void actionPerformed(ActionEvent event) {
    if (pluginManagerWindow != null) {
    pluginManagerWindow.getMainFrame().setState(JFrame.NORMAL);
    pluginManagerWindow.getMainFrame().toFront();
    pluginManagerWindow.getMainFrame().setVisible(true);
    } else {
    PluginManagerSettings pms = new PluginManagerSettings();
    pms.setMysqlTunneling(SplitsTreeProperties.mysqlTunneling);
    pms.setServerPluginFolder(SplitsTreeProperties.serverPluginFolder);
    pms.setDatabaseName(SplitsTreeProperties.pluginDatabase);
    pms.setPluginFolder(ProgramProperties.get("PluginFolder"));
    pms.setMainProgramVersion(SplitsTreeProperties.getShortVersion());
    pluginManagerWindow = new PluginOverview(ProgramProperties.getProgramIcon().getImage(),
    "PluginData Manager Overview - " + SplitsTreeProperties.getShortVersion(),
    pms, true);
    }
    }
    };
    action.putValue(AbstractAction.NAME, "Plugin Manager...");
    action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open the plugin manager");
    action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));

    all.add(action);
    return pluginManagerAction = action;
    }
     */
    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return false;
    }
}
