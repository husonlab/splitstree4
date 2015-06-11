/**
 * Director.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.gui;

import jloda.gui.Message;
import jloda.gui.ProgressDialog;
import jloda.gui.commands.CommandManager;
import jloda.gui.director.*;
import jloda.phylo.PhyloGraphView;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import splitstree.core.BlockChooser;
import splitstree.core.Document;
import splitstree.externalIO.exports.ExporterInfo;
import splitstree.externalIO.imports.ImportManager;
import splitstree.externalIO.imports.MrBayesPartitions;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Analysis;
import splitstree.nexus.Characters;
import splitstree.nexus.Network;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Coordinates views, actions and doc. The director runs a project.
 *
 * @author huson
 *         Date: 26-Nov-2003
 */
public class Director implements IDirector {
    private Document doc;
    private boolean docInUpdate = false;
    private List viewers;
    private List directorEventListeners;
    private DirectorActions actions;
    private int ID;

    private Thread executionWorkerThread = null;

    /**
     * construct a new director with associated doc and actions.
     */
    public Director(Document doc) {
        this.doc = doc;
        viewers = new LinkedList();
        directorEventListeners = new LinkedList();
        actions = new DirectorActions(this);
        directorEventListeners.add(actions); // centrally disable actions!
    }

    /**
     * get the actions
     *
     * @return actions
     */
    public DirectorActions getActions() {
        return actions;
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command) {
        execute(command, null);
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command, final CommandManager commandManager) {
        execute(command, commandManager, getMainViewerFrame());
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command, final CommandManager commandManager, final Component parent) {
        System.err.println("executing " + command);
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: execute(" + command + "): concurrent execution");

        if (executionWorkerThread == null || !executionWorkerThread.isAlive()) {
            notifyLockInput();

            executionWorkerThread = new Thread(new Runnable() {
                public void run() {
                    docInUpdate = true;
                    ProgressDialog progressDialog = new ProgressDialog("", "", parent);
                    doc.setProgressListener(progressDialog);
                    try {
                        // this is used to carry over taxon and split annotations from one update to the next
                        if (doc.isValidByName(Network.NAME)) {
                            PhyloGraphView graphView = (MainViewer) getMainViewer();
                            if (graphView != null) {
                                doc.getNetwork().syncPhyloGraphView2Network(doc.getTaxa(), graphView);
                                doc.getNetwork().updateTaxon2VertexDescriptionMap(doc.taxon2VertexDescription);
                            }
                        }

                        if (commandManager == null)
                            doc.execute(command);
                        else
                            commandManager.execute(command);
                    } catch (CanceledException ex) {
                        System.err.println("USER CANCELED EXECUTE");
                    } catch (OutOfMemoryError ex) {
                        System.gc();
                        new Alert("Out of memory");
                    } catch (Exception ex) {
                        Basic.caught(ex);
                        new Alert(getMainViewer().getFrame(), "Execute failed: " + ex.getMessage());
                    }

                    notifyUpdateViewer(Director.ALL);
                    WaitUntilAllViewersAreUptoDate();
                    notifyUnlockInput();

                    progressDialog.close();
                    doc.setProgressListener(null);
                    docInUpdate = false;
                    if (getMainViewer() != null)
                        getMainViewer().getFrame().toFront();
                }
            });
            executionWorkerThread.setPriority(Thread.currentThread().getPriority() - 1);
            executionWorkerThread.start();
        }
    }

    /**
     * execute a command within the swing thread
     *
     * @param command
     */
    public boolean executeImmediately(final String command) {
        return executeImmediately(command, null);
    }

    /**
     * execute a command within the swing thread
     *
     * @param command
     */
    public boolean executeImmediately(final String command, CommandManager commandManager) {
        //System.err.println("executing " + command);
        try {
            ProgressListener progressListener = new ProgressCmdLine();
            doc.setProgressListener(progressListener);
            if (commandManager == null)
                doc.execute(command);
            else
                commandManager.execute(command);
            notifyUpdateViewer(Director.ALL);
            WaitUntilAllViewersAreUptoDate();
            notifyUnlockInput();
            return true;
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED EXECUTE");
        } catch (Exception ex) {
            Basic.caught(ex);
            new Alert("Command failed: " + ex.getMessage());
        }
        return false;
    }


    /**
     * execute an analysis command. Lock all viewer input, display result in a pop-up window
     *
     * @param command
     */
    public void analysis(final String command) {
        getActions().getMessageWindow().actionPerformed(null);
        System.err.println("Executing: analysis " + command + ";");
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: analysis(" + command + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;
                ProgressDialog progressDialog = new ProgressDialog("", "", getMainViewerFrame());
                doc.setProgressListener(progressDialog);

                try {
                    Analysis analysis = new Analysis(false);
                    analysis.read(new NexusStreamParser(new StringReader("begin st_analysis;" + command + ";end;")));

                    final String result = analysis.apply(doc);
                    if (result != null) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    new Message(getMainViewer().getFrame(), result, "Analysis result");
                                    // TODO: here we want to open a nice result window!
                                }
                            });
                        } catch (InterruptedException ex) {
                        }
                    }
                } catch (CanceledException ex) {
                    System.err.println("USER CANCELED EXECUTE");
                } catch (Exception ex) {
                    Basic.caught(ex);
                    new Alert(getMainViewer().getFrame(), "AnalysisMethod failed: " + ex.getMessage());
                }

                //notifyUpdateViewer(Director.ALL);
                //WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }

    /**
     * open a new file
     *
     * @param file
     */
    public void openFile(final File file) {
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: open(" + file + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;

                ProgressDialog progressDialog = new ProgressDialog("Reading File", "", getMainViewerFrame());
                doc.setProgressListener(progressDialog);
                doc.notifyEnabled(false);

                try {
                    System.err.println("Opening");
                    getDocument().open(getMainViewerFrame(), file);
                    System.err.println("done");

                } catch (Exception ex) {
                    Basic.caught(ex);

                    final Exception fex = ex;
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                try {
                                    openEditor(new FileReader(file), NexusStreamParser.getLineNumber(fex));
                                } catch (FileNotFoundException fe) {
                                    Basic.caught(fe);
                                }
                            }
                        });
                    } catch (Exception ex2) {
                        Basic.caught(ex2);
                    } catch (OutOfMemoryError ex2) {
                        System.gc();
                        new Alert("Out of memory");
                    }
                }
                notifyUpdateViewer(Director.TITLE);
                WaitUntilAllViewersAreUptoDate();

                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }

    /**
     * read nexus from a reader     and execute
     *
     * @param reader
     */
    public void read(final Reader reader) {
        read(reader, true);
    }

    /**
     * load nexus from a reader
     *
     * @param reader
     */
    public void load(final Reader reader) {

        read(reader, false);
    }


    /**
     * read nexus from a reader and do update, if desired
     *
     * @param reader
     * @param doUpdate
     */
    public void read(final Reader reader, final boolean doUpdate) {
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: read(" + reader + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;

                ProgressDialog progressDialog = new ProgressDialog("Reading File", "", getMainViewerFrame());
                doc.setProgressListener(progressDialog);
                doc.notifyEnabled(false);

                try {
                    getDocument().readNexus(reader);
                    progressDialog.setCancelable(true);
                    if (doUpdate)
                        getDocument().update();
                    // tell undo tab that text is uptodate
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            MainViewer mainViewer = (MainViewer) (getMainViewer());
                            mainViewer.getTextEditor().setEditTextOriginal(mainViewer.getTextEditor().getEditText());
                        }
                    });
                } catch (CanceledException ex) {
                    System.err.println("USER CANCELED READ");


                } catch (Exception ex) {
                    new Alert(getMainViewerFrame(), "Read failed: " + ex.getMessage());
                    Basic.caught(ex);
                    final Exception fex = ex;
                    try {
                        reader.reset();
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                openEditor(reader, NexusStreamParser.getLineNumber(fex));
                            }
                        });
                    } catch (Exception ex2) {
                        Basic.caught(ex2);
                    }
                }
                notifyUpdateViewer(Director.TITLE);
                WaitUntilAllViewersAreUptoDate();

                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();

                notifyUnlockInput();
                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }

    /**
     * Save document to file
     *
     * @param file
     */
    public void saveFile(final File file) {
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: save(" + file + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;

                ProgressDialog progressDialog = new ProgressDialog("Saving", "", getMainViewerFrame());
                doc.setProgressListener(progressDialog);
                getDocument().getAssumptions().setUptodate(true);
                try {
                    getDocument().save(file);
                } catch (Exception ex) {
                    Basic.caught(ex);
                } finally {
                    getDocument().getAssumptions().setUptodate(false);
                }
                notifyUpdateViewer(Director.TITLE);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }


    public void exportFile(final File file, final String exporter, final Collection blocks) {
        exportFile(file, exporter, blocks, null);
    }

    /**
     * export data to a file
     *
     * @param file
     */
    public void exportFile(final File file, final String exporter, final Collection blocks, final ExporterInfo additionalInfo) {
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: export(" + file + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;

                ProgressDialog progressDialog = new ProgressDialog("Exporting", "", null);
                doc.setProgressListener(progressDialog);

                // sync view to Network if Network is to be exported
                if (blocks.contains(Network.NAME)) {
                    MainViewer viewer = (MainViewer) getMainViewer();
                    Network network = getDocument().getNetwork();
                    network.syncPhyloGraphView2Network(getDocument().getTaxa(), viewer);
                    getDocument().setNetwork(network);
                }
                // if blocks contains chars and some chars are masked, ask whether
                // to save all sites or just all active sites
                boolean complete = true;
                if (blocks.contains(Characters.NAME) && doc.getCharacters().getMask() != null
                        && doc.getCharacters().getNactive() < doc.getCharacters().getNchar()) {
                    Object[] options = {"Save All", "Save Active Only", "Cancel"};
                    int result = JOptionPane.showOptionDialog(getMainViewer().getFrame(),
                            "Some characters have been excluded", "Export option", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    System.err.println("result: " + result);
                    if (result == 1)
                        complete = false;
                }

                try {
                    getDocument().exportFile(file, exporter, doc, blocks, complete, additionalInfo);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }

    /**
     * Import a new file
     *
     * @param file the file
     */
    public void importFile(final File file) {
        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: import(" + file + "): concurrent execution");

        notifyLockInput();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                docInUpdate = true;

                ProgressDialog progressDialog = new ProgressDialog("Importing", "", getMainViewerFrame());
                doc.setProgressListener(progressDialog);
                try {
                    String input = ImportManager.importData(file);
                    doc.clear();
                    doc.setDirty(true);
                    doc.readNexus(new StringReader(input));
                    if (!BlockChooser.show(doc.getParent(), doc)) {
                        throw new CanceledException();
                    }

                    if (doc.isValidByName(Characters.NAME) && doc.getCharacters().getFormat().getDatatype().equals(Characters.Datatypes.UNKNOWN)) {
                        doc.getCharacters().getFormat().setDatatype(Document.chooseDatatype(getMainViewerFrame()));
                    }

                    if (TreesNameDialog.isApplicable(doc))
                        new TreesNameDialog(getMainViewerFrame(), doc);
                    else if (input.endsWith("[" + Basic.getShortName(MrBayesPartitions.class) + "]"))
                        MrBayesPartitions.extractTaxa(getMainViewerFrame(), file, doc);

                    doc.execute("update");
                } catch (FileNotFoundException e) {
                    new Alert(getMainViewerFrame(), "File not found: " + file.getName());
                } catch (CanceledException ex) {
                    doc.clear();

                    if (file.canRead()) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    try {
                                        openEditor(new FileReader(file), 0);

                                    } catch (Exception fex) {
                                        Basic.caught(fex);
                                    }
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException e) {
                            Basic.caught(e);
                        }
                    }
                } catch (Exception ex) {
                    new Alert(getMainViewerFrame(), "Import failed: unknown format or error in file"
                            + " (e.g. illegal characters or multiple occurrences of same taxon name).");
                    Basic.caught(ex);
                    doc.clear();
                    if (file.canRead()) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    try {
                                        openEditor(new FileReader(file), 0);

                                    } catch (Exception fex) {
                                        Basic.caught(fex);
                                    }
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException e) {
                            Basic.caught(e);
                        }
                    }
                }
                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                progressDialog.close();
                doc.setProgressListener(null);
                docInUpdate = false;
            }
        });
        worker.setPriority(Thread.currentThread().getPriority() - 1);
        worker.start();
    }


    /**
     * add a viewer to this doc
     *
     * @param viewer
     */
    public IDirectableViewer addViewer(IDirectableViewer viewer) {
        viewers.add(viewer);
        directorEventListeners.add(viewer);
        ProjectManager.projectWindowChanged(this, viewer, true);
        return viewer;
    }

    /**
     * does this director (still) contain the named viewer
     *
     * @param viewer
     * @return true, if director has this viewer
     */
    public boolean containsViewer(IDirectableViewer viewer) {
        return viewers.contains(viewer);
    }

    /**
     * remove a viewer from this doc
     *
     * @param viewer
     */
    public void removeViewer(IDirectableViewer viewer) {
        viewers.remove(viewer);
        directorEventListeners.remove(viewer);
        ProjectManager.projectWindowChanged(this, viewer, false);

        if (viewers.isEmpty())
            ProjectManager.removeProject(this);
    }

    /**
     * returns the list of viewers
     *
     * @return viewers
     */
    public List getViewers() {
        return viewers;
    }

    /**
     * waits until all viewers are uptodate
     */
    public void WaitUntilAllViewersAreUptoDate() {

        while (!isAllViewersUptodate()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }
    }

    /**
     * returns true, if all viewers are uptodate
     *
     * @return true, if all viewers uptodate
     */
    public boolean isAllViewersUptodate() {
        for (Object viewer1 : viewers) {
            IDirectableViewer viewer = (IDirectableViewer) viewer1;
            if (!viewer.isUptoDate()) {
                //System.err.println("not up-to-date: "+viewer.getTitle()+" "+viewer.getClass().getName());
                return false;
            }
        }
        return true;
    }

    /**
     * notify listeners that viewers should be updated
     *
     * @param what what should be updated?
     */
    public void notifyUpdateViewer(final String what) {
        for (Object directorEventListener : directorEventListeners) {
            final IDirectorListener d = (IDirectorListener) directorEventListener;

            try {
                // Put the update into the swing event queue
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            d.setUptoDate(false);
                            d.updateView(what);
                            d.setUptoDate(true);
                        } catch (Exception ex) {
                            Basic.caught(ex);
                            d.setUptoDate(true);
                        }
                    }
                });
            } catch (Exception ex) {
                Basic.caught(ex);
                d.setUptoDate(true);
            }
        }
    }

    /**
     * notify listeners to prevent user input
     */
    public void notifyLockInput() {
        for (Object directorEventListener : directorEventListeners) {
            IDirectorListener d = (IDirectorListener) directorEventListener;
            d.lockUserInput();
        }
    }

    /**
     * notify listeners to allow user input
     */
    public void notifyUnlockInput() {
        for (Object directorEventListener : directorEventListeners) {
            IDirectorListener d = (IDirectorListener) directorEventListener;
            d.unlockUserInput();
        }
    }

    /**
     * notify all director event listeners to destroy themselves
     */
    void notifyDestroyViewer() throws CanceledException {
        for (Object directorEventListener : directorEventListeners) {
            IDirectorListener d = (IDirectorListener) directorEventListener;
            d.destroyView();
        }

        // now remove all viewers
        while (viewers.size() > 0) {
            removeViewer((IDirectableViewer) viewers.get(0));
        }

        if (executionWorkerThread != null && executionWorkerThread.isAlive()) {
            executionWorkerThread.stop();
        }
    }

    /**
     * gets the doc
     *
     * @return the doc
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * sets the project ID
     *
     * @param ID
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    /**
     * gets the project ID
     *
     * @return ID
     */
    public int getID() {
        return ID;
    }

    /**
     * do we already have a viewer of the given class?
     *
     * @param aClass
     * @return true if a viewer of the current class is already open
     */
    public boolean hasViewer(Class aClass) {
        for (Object viewer1 : viewers) {
            IDirectableViewer viewer = (IDirectableViewer) viewer1;
            if (aClass.isAssignableFrom(viewer.getClass()))
                return true;
        }
        return false;
    }

    /**
     * opens the nexus editor
     *
     * @param lineno if >0, scroll to this line and highlight it
     */
    public void openEditor(Reader reader, int lineno) {
        MainViewer mainViewer = (MainViewer) getMainViewer();
        if (reader != null) // need to put this in the editor
        {
            try {
                BufferedReader br = new BufferedReader(reader);
                StringBuilder buf = new StringBuilder();
                String aline;
                while ((aline = br.readLine()) != null)
                    buf.append(aline).append("\n");
                mainViewer.getTextEditor().setEditText(buf.toString());
                mainViewer.getTextEditor().setEditTextOriginal("");
                mainViewer.getTextEditor().setEditSelectLine(lineno);
            } catch (IOException ex) {
                Basic.caught(ex);
            }
        }
    }

    /**
     * are we currently updating the document?
     *
     * @return true, if are in update
     */
    public boolean isInUpdate() {
        return docInUpdate;
    }

    /**
     * close everything directed by this director
     */
    public void close() throws CanceledException {
        notifyDestroyViewer();
        // todo: this could be risky!
    }

    /**
     * returns a viewer of the given class
     *
     * @param aClass
     * @return viewer of the given class, or null
     */
    public IDirectableViewer getViewerByClass(Class aClass) {
        for (Object o : getViewers()) {
            IDirectableViewer viewer = (IDirectableViewer) o;
            if (viewer.getClass().equals(aClass)) // todo: bug?
                return viewer;
        }
        return null;
    }

    /**
     * show the main viewer
     */
    public MainViewer showMainViewer() {
        MainViewer mainViewer = (MainViewer) getMainViewer();
        if (mainViewer != null) {
            mainViewer.getFrame().setState(JFrame.NORMAL);
            mainViewer.getFrame().toFront();

        }
        return mainViewer;
    }

    /**
     * get the main viewer
     *
     * @return main viewer
     */
    public IMainViewer getMainViewer() {
        return (IMainViewer) getViewerByClass(MainViewer.class);
    }

    /**
     * sets the location of the viewer on the screen
     *
     * @param viewer
     */
    public void setViewerLocation(IDirectableViewer viewer) {
        if (viewer instanceof MainViewer) {
            int x = (getID() % 10) * 30 + 50;
            int y = (getID() % 10) * 30 + 50;
            viewer.getFrame().setLocation(x, y);
        } else {
            viewer.getFrame().setLocationRelativeTo(getMainViewer().getFrame());
        }
    }

    /**
     * returns the parent viewer
     *
     * @return viewer
     */
    public JFrame getMainViewerFrame() {
        MainViewer mainViewer = (MainViewer) getMainViewer();
        if (mainViewer != null)
            return mainViewer.getFrame();
        return null;
    }

    public String getTitle() {
        return getMainViewer().getTitle();
    }

    /**
     * set the dirty flag
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        getDocument().setDirty(dirty);
    }

    /**
     * get the dirty flag
     *
     * @return dirty
     */
    public boolean getDirty() {
        return getDocument().isDirty();
    }

    /**
     * creates a new empty project
     *
     * @return the director of the new project
     */
    static public Director newProject() {
        Document doc = new Document();
        Director dir = new Director(doc);
        dir.setID(ProjectManager.getNextID());
        MainViewer mainViewer = new MainViewer(dir);
        ProjectManager.addProject(dir, mainViewer);
        return dir;

    }

    /**
     * Adds a new project and then processes the input string
     *
     * @param input a string containing nexus input
     * @param fname name of file to associate with input string, will be made unique
     * @return the director
     */
    static public Director newProject(String input, String fname) {
        Document doc = new Document();
        Director dir = new Director(doc);
        dir.setID(ProjectManager.getNextID());
        MainViewer mainViewer = new MainViewer(dir);
        ProjectManager.addProject(dir, mainViewer);
        doc.setFile(fname, true);
        dir.showMainViewer();
        dir.read(new StringReader(input));
        return dir;
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return getMainViewer().getCommandManager();
    }

}
