/*
 * MainViewerActions.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.main;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.director.IDirector;
import jloda.swing.export.SaveImageDialog;
import jloda.swing.export.TransferableGraphic;
import jloda.swing.find.SearchManager;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.graphview.ScrollPaneAdjuster;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.Alert;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.progress.ProgressCmdLine;
import jloda.util.progress.ProgressSilent;
import splitstree4.algorithms.splits.EqualAngle;
import splitstree4.algorithms.trees.TreeSelector;
import splitstree4.algorithms.trees.TreesTransform;
import splitstree4.analysis.bootstrap.TestTreeness;
import splitstree4.analysis.characters.CaptureRecapture;
import splitstree4.analysis.characters.GascuelGamma;
import splitstree4.analysis.characters.PhiTest;
import splitstree4.analysis.distances.DeltaScore;
import splitstree4.analysis.network.MidpointRoot;
import splitstree4.analysis.splits.PhylogeneticDiversity;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.externalIO.imports.NewickTree;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.TreesNameDialog;
import splitstree4.gui.algorithms.AlgorithmsWindow;
import splitstree4.gui.confidence.ConfidenceWindow;
import splitstree4.gui.formatter.Formatter;
import splitstree4.gui.input.InputDialog;
import splitstree4.gui.nodeEdge.Configurator;
import splitstree4.gui.undo.*;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;
import splitstree4.util.CharactersUtilities;
import splitstree4.util.EnableDisable;
import splitstree4.util.SplitsUtilities;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.List;
import java.util.*;

/**
 * viewer
 */
public class MainViewerActions {
    private final Director dir;
    private final MainViewer viewer;
    // we keep a list of critical actions: these are ones that must be disabled
    // when the director tells us to block user input
    private final List<Action> all = new LinkedList<>();
    public final static String DEPENDS_ON_NODESELECTION = "NSELECT";
    public final static String DEPENDS_ON_EDGESELECTION = "ESELECT";
    public final static String DEPENDS_ON_ROOTED = "ROOT";

    private boolean isSelectedInterleaveCBox = false; //false if the option checkbox is not selected
    private boolean isSelectedTransposeCBox = false; //false if the option checkbox is not selected

    private final boolean inChangingStates = false;

    /**
     * setup the viewer actions
     *
     * @param viewer d
     */
    public MainViewerActions(MainViewer viewer, Director dir) {
        super();

        this.viewer = viewer;
        this.dir = dir;
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
        Document doc = dir.getDocument();

        if (maintainEdgeLengths != null) {
            JCheckBoxMenuItem cbox = ((JCheckBoxMenuItem) maintainEdgeLengths.getValue(DirectorActions.JCHECKBOX));
            cbox.setSelected(viewer.getMaintainEdgeLengths());
        }
        //Checking if we should enabled or disabled transpose's action
        // depending on the state of interleave's action
        ((AbstractAction) nameState2action.get(Characters.NAME + "Transpose")).setEnabled(!isSelectedInterleaveCBox);

        //Checking if we should enabled or disabled interleave's action
        // depending on the state of transpose's action
        ((AbstractAction) nameState2action.get(Characters.NAME + "Interleave")).setEnabled(!isSelectedTransposeCBox);

        getAutoLayoutLabels(null).setEnabled(doc.isValidByName(Network.NAME));
        {
            boolean state = dir.getDocument() != null && dir.getDocument().isValidByName(Assumptions.NAME)
                    && dir.getDocument().getAssumptions().getAutoLayoutNodeLabels();
            autoLayoutLabelsCB.setSelected(state);
        }

        getRadiallyLayoutLabels(null).setEnabled(doc.isValidByName(Network.NAME));
        {
            boolean state = dir.getDocument() != null && dir.getDocument().isValidByName(Assumptions.NAME)
                    && dir.getDocument().getAssumptions().getRadiallyLayoutNodeLabels();
            radiallyLayoutLabelsCB.setSelected(state);
        }

        getSimpleLayoutLabels(null).setEnabled(doc.isValidByName(Network.NAME));
        {
            boolean state = dir.getDocument() != null && dir.getDocument().isValidByName(Assumptions.NAME)
                    && !dir.getDocument().getAssumptions().getAutoLayoutNodeLabels() && !dir.getDocument().getAssumptions().getRadiallyLayoutNodeLabels();
            simpleLayoutLabelsCB.setSelected(state);
        }


        saveFile.setEnabled(doc.isDirty());

        excludeGapsCB.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeGaps());
        excludeConstantCB.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeConstant() != 0);
        excludeNonParsimonyCB.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeNonParsimony());
        excludeGapsCB.setEnabled(doc.isValidByName(Characters.NAME));
        excludeConstantCB.setEnabled(doc.isValidByName(Characters.NAME));
        excludeNonParsimonyCB.setEnabled(doc.isValidByName(Characters.NAME));

        greedilyMakeCompatibleCB.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getSplitsPostProcess().getFilter().equalsIgnoreCase("greedycompatible"));
        greedilyMakeWeaklyCompatibleCB.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getSplitsPostProcess().getFilter().equalsIgnoreCase("greedywc"));

        for (Action action : all) {
            if (action.getValue(DEPENDS_ON_NODESELECTION) != null && viewer.getNumberSelectedNodes() == 0)
                action.setEnabled(false);
            if (action.getValue(DEPENDS_ON_EDGESELECTION) != null && viewer.getNumberSelectedEdges() == 0)
                action.setEnabled(false);
            //       if(action.get(DEPENDS_ON_ROOTED)!=null && (doc.getNetwork()==null || doc.getAssumptions().getSplitsTransformName()!=Basic.getShortName(RootedEqualAngle.class)))
            //              action.setEnabled(false);

            // if any of the OK_WITH values are set, one of the tabs must be selected:
            // new actions carry an EnableDisable class to decide whether to enable
            EnableDisable ed = (EnableDisable) action.getValue(EnableDisable.ENABLEDISABLE);
            if (ed != null)
                action.setEnabled(ed.enable());
        }


        if (magnifierAction != null) {
            JCheckBoxMenuItem cbox = ((JCheckBoxMenuItem) magnifierAction.getValue(DirectorActions.JCHECKBOX));
            cbox.setSelected(viewer.isUseMagnify());
            cbox.setEnabled(viewer.getGraph().getNumberOfNodes() > 0);
        }
        if (magnifyAll != null) {
            JCheckBoxMenuItem cbox = (JCheckBoxMenuItem) magnifyAll.getValue(DirectorActions.JCHECKBOX);
            cbox.setSelected(viewer.isUseMagnifyAll());
            cbox.setEnabled(viewer.isUseMagnify() && viewer.getGraph().getNumberOfNodes() > 0);
        }

        if (deselectAll != null)
            deselectAll.setEnabled(viewer.getSelectedNodes().size() > 0
                    || viewer.getSelectedEdges().size() > 0);

        if (selectLatest != null)
            selectLatest.setEnabled(true);

        updateUndo();
        updateRedo();

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
                job.setPrintable(viewer);

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
        action.putValue(DirectorActions.CRITICAL, true);
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
                try {
                    dir.close();
                } catch (CanceledException ignored) {
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this viewer");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        all.add(action);

        return close = action;
    }

    private AbstractAction cloneAction;

    public AbstractAction getCloneAction() {
        AbstractAction action = cloneAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                StringWriter sw = new StringWriter();
                try {
                    Document doc = dir.getDocument();
                    Taxa taxa = doc.getTaxa();
                    Network network = doc.getNetwork();
                    if (network == null)
                        network = new Network(taxa, viewer);
                    else
                        SyncViewerToDoc.sync(viewer, doc);
                    doc.setNetwork(network);

                    doc.getAssumptions().setUptodate(true);
                    doc.write(sw);
                    Director newDir = Director.newProject(sw.toString(),
                            doc.getFile() != null ? doc.getFile().getAbsolutePath() : "Untitled");
                    MainViewer newViewer = newDir.showMainViewer();
                    newViewer.getFrame().setSize(dir.getMainViewer().getFrame().getSize());
                } catch (Exception ex) {
                    System.err.println("Clone failed: " + ex);
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Clone...");

        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clone this window");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Duplicate16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        return cloneAction = action;
    }

    private AbstractAction saveFile;

    public AbstractAction getSaveFile() {
        AbstractAction action = saveFile;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                File file = dir.getDocument().getFile();
                if (file != null && file.canWrite()) {
                    Document doc = dir.getDocument();
                    Taxa taxa = doc.getTaxa();
                    Network network = doc.getNetwork();
                    if (network == null)
                        network = new Network(taxa, viewer);
                    else
                        SyncViewerToDoc.sync(viewer, doc);
                    doc.setNetwork(network);
                    dir.saveFile(file);
                } else
                    getSaveAsFile().actionPerformed(event);
            }
        };
        action.putValue(AbstractAction.NAME, "Save");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Save to a nexus file");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Save16.gif"));

        all.add(action);
        return saveFile = action;
    }

    private AbstractAction saveAsFile;

    public AbstractAction getSaveAsFile() {
        AbstractAction action = saveAsFile;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    doSaveAsDialog();
                } catch (CanceledException ignored) {
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Save As...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Save to disk");
        //action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/SaveAs16.gif"));

        all.add(action);
        return saveAsFile = action;
    }

    public void doSaveAsDialog() throws CanceledException {
        File lastSaveFile = ProgramProperties.getFile(SplitsTreeProperties.SAVEFILE);

        File preSelect = dir.getDocument().getFile();
        if (preSelect == null)
            preSelect = lastSaveFile.getParentFile();

        File file = null;
        if (!ProgramProperties.isMacOS()) {
            JFileChooser chooser = new JFileChooser(preSelect);
            chooser.setDialogTitle("SAVE " + viewer.getTitle());
            chooser.setSelectedFile(preSelect);

            boolean fileHasBeenSelected = false;

            while (!fileHasBeenSelected) {
                int result = chooser.showSaveDialog(viewer.getFrame());
                if (result == JFileChooser.CANCEL_OPTION)
                    throw new CanceledException();
                if (result == JFileChooser.APPROVE_OPTION) {
                    file = chooser.getSelectedFile();
                    if (file.exists()) {
                        lastSaveFile = file;
                        ProgramProperties.put(SplitsTreeProperties.SAVEFILE, lastSaveFile);
                    }
                    if (!file.exists())
                        fileHasBeenSelected = true;
                    else {
                        switch (
                                JOptionPane.showConfirmDialog(viewer.getFrame(),
                                        "This file already exists. " +
                                                "Would you like to overwrite the existing file?",
                                        "Save File",
                                        JOptionPane.YES_NO_CANCEL_OPTION)) {
                            case JOptionPane.YES_OPTION:
                                fileHasBeenSelected = true;
                                break;
                            case JOptionPane.NO_OPTION:
                                continue; // ask again for file
                            case JOptionPane.CANCEL_OPTION:
                                throw new CanceledException();
                        }
                    }
                }
            }
        } else {
            FileDialog dialog = new FileDialog(dir.getMainViewerFrame(), "Save Nexus file", FileDialog.SAVE);
            dialog.setFile(preSelect.getName());
            dialog.setDirectory(preSelect.getParent());
            dialog.setVisible(true);

            if (dialog.getFile() != null)
                file = new File(dialog.getDirectory(), dialog.getFile());
            else
                throw new CanceledException();
        }
        try {
            Document doc = dir.getDocument();
            Taxa taxa = doc.getTaxa();
            Network network = doc.getNetwork();

            if (taxa != null) { //Avoids crash if taxa==null, e.g. with new file.
                if (network == null)
                    network = new Network(taxa, viewer);
                else
                    SyncViewerToDoc.sync(viewer, doc);

                doc.setNetwork(network);
            }
            dir.saveFile(file);
            SplitsTreeProperties.addRecentFile(file);
        } catch (Exception ex) {
            System.err.println("Save failed: " + ex);
        }
    }


    private AbstractAction saveImage;

    public AbstractAction getSaveImage() {
        AbstractAction action = saveImage;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String fileBaseName = null;
                if (dir.getDocument().getFile() != null)
                    fileBaseName = dir.getDocument().getFile().getName();
                if (fileBaseName == null)
                    fileBaseName = "Untitled";
				fileBaseName = FileUtils.getFileBaseName(fileBaseName);
				viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                SaveImageDialog.useAWTDialog = (ProgramProperties.isMacOS() &&
                        (event != null && (event.getModifiers() & Event.SHIFT_MASK) == 0));
                new SaveImageDialog(viewer.getFrame(), viewer, viewer.getScrollPane(), fileBaseName);
                viewer.resetCursor();
            }
        };
        action.putValue(AbstractAction.NAME, "Export Image...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Export the graph to an image file");
        action.putValue(DirectorActions.CRITICAL, Boolean.FALSE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Export16.gif"));
        all.add(action);
        return saveImage = action;
    }

    private AbstractAction cut;

    public AbstractAction getCut() {
        AbstractAction action = cut;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                TransferableGraphic tg = new TransferableGraphic(viewer);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tg, tg);
            }
        };
        action.putValue(AbstractAction.NAME, "Cut");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() /*| InputEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cut");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Cut16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return cut = action;
    }

    private AbstractAction copy;

    public AbstractAction getCopy() {
        AbstractAction action = copy;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                TransferableGraphic tg = new TransferableGraphic(viewer, viewer.getScrollPane());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tg, tg);
            }
        };
        action.putValue(AbstractAction.NAME, "Copy");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() /*| InputEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy graph to clipboard");
        action.putValue(DirectorActions.CRITICAL, true);
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
                TransferableGraphic tg = new TransferableGraphic(viewer);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tg, tg);
            }
        };
        action.putValue(AbstractAction.NAME, "Paste");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Paste");
        action.putValue(DirectorActions.CRITICAL, true);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Paste16.gif"));

        all.add(action);
        return paste = action;
    }

    private AbstractAction resetLayout;

    public AbstractAction getResetLayout() {
        AbstractAction action = resetLayout;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new ResetTransformCommand(viewer);
                new Edit(cmd, "reset").execute(viewer.getUndoSupportNetwork());
                updateUndo();
                updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Reset");
        // quit.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Zoom to fit");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/AlignCenter16.gif"));
        all.add(action);
        return resetLayout = action;
    }

    private AbstractAction zoomIn;

    public AbstractAction getZoomIn() {
        AbstractAction action = zoomIn;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                final double s = 1.05;
                viewer.trans.composeScale(s, s);
                spa.adjust(true, true);
                //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, s, s);
                //new Edit(cmd, "zoom in").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Zoom In");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Zoom in to graph");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/ZoomIn16.gif"));

        all.add(action);
        return zoomIn = action;
    }

    private AbstractAction zoomOut;

    public AbstractAction getZoomOut() {
        AbstractAction action = zoomOut;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                final double s = 1.0 / 1.05;
                viewer.trans.composeScale(s, s);
                spa.adjust(true, true);
                //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, s, s);
                //new Edit(cmd, "zoom out").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Zoom Out");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Zoom out to graph");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/ZoomOut16.gif"));

        all.add(action);
        return zoomOut = action;
    }

    private AbstractAction setScale;

    public AbstractAction getSetScale() {
        AbstractAction action = setScale;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String label = "" + (float) (100.0 / viewer.trans.getScaleX());

                label = JOptionPane.showInputDialog(viewer.getFrame(), "Set units per 100 pixel: ", label);
                if (label != null && NumberUtils.isDouble(label)) {
                    double value = 100.0 / Double.parseDouble(label);
                    if (viewer.trans.getLockXYScale())
                        viewer.trans.setScale(value, value);
                    else
                        viewer.trans.setScaleX(value);
                    viewer.repaint();
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Set Scale...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set scale to draw tree or network at");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);

        all.add(action);
        return setScale = action;
    }

    private AbstractAction increaseFontSize;

    public AbstractAction getIncreaseFontSize() {
        AbstractAction action = increaseFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean changed = false;

                boolean doSelectedEdges = false;
                boolean doSelectedNodes = false;
                boolean doAllNodes = false;

                if (viewer.getSelectedNodes().size() > 0)
                    doSelectedNodes = true;
                else if (viewer.getSelectedEdges().size() > 0)
                    doSelectedEdges = true;
                else
                    doAllNodes = true;

                Set<Node> nodes = new HashSet<>();
                if (doAllNodes) {
                    for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                        nodes.add(v);
                } else if (doSelectedNodes)
                    nodes.addAll(viewer.getSelectedNodes());

                for (Node v : nodes) {
                    if (viewer.getLabel(v) != null) {
                        Font font = viewer.getFont(v);
                        int size = font.getSize() + 2;
                        font = new Font(font.getFamily(), font.getStyle(), size);
                        viewer.setFont(v, font);
						jloda.swing.util.ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
						changed = true;
                    }
                }
                if (doSelectedEdges) {
                    for (Edge e : viewer.getSelectedEdges()) {
                        if (viewer.getLabel(e) != null) {
                            Font font = viewer.getFont(e);
                            int size = font.getSize() + 2;
                            font = new Font(font.getFamily(), font.getStyle(), size);
							viewer.setFont(e, font);
							jloda.swing.util.ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
							changed = true;
                        }
                    }
                }
                if (changed) {
                    viewer.repaint();
                    dir.getDocument().setDirty(true);
                    viewer.updateView(Director.TITLE);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Increase Font Size");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set scale to draw tree or network at");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        all.add(action);
        return increaseFontSize = action;
    }


    private AbstractAction decreaseFontSize;

    public AbstractAction getDecreaseFontSize() {
        AbstractAction action = decreaseFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean changed = false;

                boolean doSelectedEdges = false;
                boolean doSelectedNodes = false;
                boolean doAllNodes = false;

                if (viewer.getSelectedNodes().size() > 0)
                    doSelectedNodes = true;
                else if (viewer.getSelectedEdges().size() > 0)
                    doSelectedEdges = true;
                else
                    doAllNodes = true;

                Set<Node> nodes = new HashSet<>();
                if (doAllNodes) {
                    for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                        nodes.add(v);
                } else if (doSelectedNodes)
                    nodes.addAll(viewer.getSelectedNodes());

                for (Node v : nodes) {
                    if (viewer.getLabel(v) != null) {
                        Font font = viewer.getFont(v);
                        int size = font.getSize() - 2;
                        if (size > 0) {
                            font = new Font(font.getFamily(), font.getStyle(), size);
							viewer.setFont(v, font);
							jloda.swing.util.ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
							changed = true;
                        }
                    }
                }
                if (doSelectedEdges) {
                    for (Edge e : viewer.getSelectedEdges()) {
                        if (viewer.getLabel(e) != null) {
                            Font font = viewer.getFont(e);
                            int size = font.getSize() - 2;
                            if (size > 0) {
                                font = new Font(font.getFamily(), font.getStyle(), size);
								viewer.setFont(e, font);
								jloda.swing.util.ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
								changed = true;
                            }
                        }
                    }
                }
                if (changed) {
                    viewer.repaint();
                    dir.getDocument().setDirty(true);
                    viewer.updateView(Director.TITLE);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Decrease Font Size");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set scale to draw tree or network at");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        all.add(action);
        return decreaseFontSize = action;
    }

    private AbstractAction rotateLeft;

    public AbstractAction getRotateLeft() {
        AbstractAction action = rotateLeft;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Network network = dir.getDocument().getNetwork();
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                double f = -(network != null && network.getLayout().equals(Network.CIRCULAR) ? 0.01 : 0.5) * Math.PI;
                if (viewer.trans.getFlipH() != viewer.trans.getFlipV())
                    f = -f;
                viewer.trans.setAngle(viewer.trans.getAngle() + f);
                spa.adjust(true, true);
                //final ICommand cmd = new RotateCommand(viewer, viewer.trans, viewer.trans.getAngle() - f);
                //new Edit(cmd, "rotate left").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Rotate Left");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Rotate graph left");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("RotateLeft16.gif"));

        all.add(action);
        return rotateLeft = action;
    }

    private AbstractAction rotateRight;

    public AbstractAction getRotateRight() {
        AbstractAction action = rotateRight;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Network network = dir.getDocument().getNetwork();
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                double f = (network != null && network.getLayout().equals(Network.CIRCULAR) ? 0.01 : 0.5) * Math.PI;
                if (viewer.trans.getFlipH() != viewer.trans.getFlipV())
                    f = -f;
                viewer.trans.setAngle(viewer.trans.getAngle() + f);
                spa.adjust(true, true);
                //final ICommand cmd = new RotateCommand(viewer, viewer.trans, viewer.trans.getAngle() + f);
                //new Edit(cmd, "rotate right").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Rotate Right");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        // quit.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Rotate graph right");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("RotateRight16.gif"));
        all.add(action);
        return rotateRight = action;
    }


    private AbstractAction flipVertical;

    public AbstractAction getFlipVertical() {
        AbstractAction action = flipVertical;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                viewer.trans.setFlipV(!viewer.trans.getFlipV());
                viewer.repaint();
                spa.adjust(true, true);
                //final ICommand cmd = new FlipVCommand(viewer, viewer.trans);
                //new Edit(cmd, "vertical flip").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Flip Up-Down");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Flip graph vertically");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        all.add(action);
        return flipVertical = action;
    }

    private AbstractAction flipHorizontal;

    public AbstractAction getFlipHorizontal() {
        AbstractAction action = flipHorizontal;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                viewer.trans.setFlipH(!viewer.trans.getFlipH());
                viewer.repaint();
                spa.adjust(true, true);
                //final ICommand cmd = new FlipHCommand(viewer, viewer.trans);
                //new Edit(cmd, "Flip").execute(viewer.getUndoSupportNetwork());
                //updateUndo();
                //updateRedo();
            }
        };
        action.putValue(AbstractAction.NAME, "Flip");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Flip the graph");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("FlipHorizontally16.gif"));
        all.add(action);
        return flipHorizontal = action;
    }

    private AbstractAction simpleLayoutLabels;
    private JCheckBoxMenuItem simpleLayoutLabelsCB;

    /**
     * simple layout of node labels
     *
     * @param cbox Check box.
     * @return action
     */
    public AbstractAction getSimpleLayoutLabels(JCheckBoxMenuItem cbox) {
        AbstractAction action = simpleLayoutLabels;
        if (action != null) return action;

        if (simpleLayoutLabelsCB == null)
            simpleLayoutLabelsCB = cbox;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean state = true;
                try {
                    state = simpleLayoutLabelsCB.getState();
                } catch (Exception ignored) {
                } // not used in a checkboxmenuitem
                final ICommand cmd = new SimpleLabelCommand(viewer, dir.getDocument(), state);
                new Edit(cmd, "simple layout labels " + (state ? "on" : "off")).execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Simple");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Do simple layout of node labels");
        action.putValue(DirectorActions.DEPENDS_ON, Assumptions.NAME);
        all.add(action);
        return simpleLayoutLabels = action;
    }

    private AbstractAction autoLayoutLabels;
    private JCheckBoxMenuItem autoLayoutLabelsCB;

    /**
     * auto layout of node labels
     *
     * @param cbox Check box.
     * @return action
     */
    public AbstractAction getAutoLayoutLabels(JCheckBoxMenuItem cbox) {
        AbstractAction action = autoLayoutLabels;
        if (action != null) return action;

        if (autoLayoutLabelsCB == null)
            autoLayoutLabelsCB = cbox;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean state = true;
                try {
                    state = autoLayoutLabelsCB.getState();
                    dir.getDocument().getAssumptions().setAutoLayoutNodeLabels(state);
                } catch (Exception ignored) {
                } // not used in a checkboxmenuitem
                final ICommand cmd = new AutoLabelCommand(viewer, dir.getDocument(), state);
                new Edit(cmd, "auto layout labels " + (state ? "on" : "off")).execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "No Overlaps");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Automatically layout node labels to avoid overlaps");
        action.putValue(DirectorActions.DEPENDS_ON, Assumptions.NAME);
        all.add(action);
        return autoLayoutLabels = action;
    }

    private AbstractAction radiallyLayoutLabels;
    private JCheckBoxMenuItem radiallyLayoutLabelsCB;

    /**
     * circular layout of node labels
     *
     * @param cbox Check box.
     * @return action
     */
    public AbstractAction getRadiallyLayoutLabels(JCheckBoxMenuItem cbox) {
        AbstractAction action = radiallyLayoutLabels;
        if (action != null) return action;

        if (radiallyLayoutLabelsCB == null)
            radiallyLayoutLabelsCB = cbox;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean state = true;
                try {
                    state = radiallyLayoutLabelsCB.getState();
                    dir.getDocument().getAssumptions().setRadiallyLayoutNodeLabels(state);
                } catch (Exception ignored) {
                } // not used in a checkboxmenuitem
                final ICommand cmd = new RadiallyLabelCommand(viewer, dir.getDocument(), state);
                new Edit(cmd, "radially layout labels " + (state ? "on" : "off")).execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Radial");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Radially layout node labels");
        action.putValue(DirectorActions.DEPENDS_ON, Assumptions.NAME);
        all.add(action);
        return radiallyLayoutLabels = action;
    }


    private AbstractAction relayoutLabels;

    /**
     * re- layout of node labels
     *
     * @return action
     */
    public AbstractAction getRelayoutLabels() {
        AbstractAction action = relayoutLabels;
        if (action != null) return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.getDocument().getAssumptions().setAutoLayoutNodeLabels(true);
                final ICommand cmd = new AutoLabelCommand(viewer, dir.getDocument(), true);
                new Edit(cmd, "reset labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Reset Label Positions");
        action.putValue(AbstractAction.SHORT_DESCRIPTION,
                "Reset the positions of all node and edge labels");
        action.putValue(DirectorActions.DEPENDS_ON, Assumptions.NAME);
        all.add(action);
        return relayoutLabels = action;
    }

    public AbstractAction getSelectTaxSet(final String taxSetName) {

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (taxSetName.equalsIgnoreCase("all"))
                    viewer.selectAllLabeledNodes();
                else {
                    Taxa taxa = dir.getDocument().getTaxa();
                    Sets sets = dir.getDocument().getSets();
                    viewer.selectNodesLabeledByTaxa(sets.getTaxSet(taxSetName, taxa).getBits());
                }
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, taxSetName);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select nodes in taxa set " + taxSetName);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return action;
    }


    private AbstractAction addSelectedTaxSetAction = null;

    /**
     * Returns action.
     * <p/>
     * Looks through selected taxa. Asks for a  name.
     * Once it has a name, checks to see if this is valid, and then adds to Sets block if it is.
     */
    public AbstractAction getAddSelectedTaxSet() {
        if (addSelectedTaxSetAction != null)
            return addSelectedTaxSetAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                //Get the set of selected taxa (maybe add this to viewer?)
                NodeSet selectedNodes = viewer.getSelectedNodes();
                TaxaSet taxaSet = new TaxaSet();
                for (Node v : selectedNodes) {
                    for (Integer t : viewer.getPhyloGraph().getTaxa(v)) {
                        taxaSet.set(t);
                    }
                }

                //Get the name
                Sets theSets = dir.getDocument().getSets();
                Taxa theTaxa = dir.getDocument().getTaxa();
                String setName = JOptionPane.showInputDialog("Enter taxa set name");
                if (setName != null) {
                    if (theSets == null)
                        theSets = new Sets();
                    setName = setName.replaceAll(" ", "_");
                    theSets.addTaxSet(setName, taxaSet, theTaxa);
                }
                dir.getDocument().setSets(theSets);
                viewer.getMenuBar().updateTaxonSets();
            }
        };
        action.putValue(AbstractAction.NAME, "New taxa set...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Create a new taxa set from selected taxa");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return addSelectedTaxSetAction = action;
    }


    private AbstractAction chooseOutgroupAction = null;

    /**
     * sets the currently selected taxa to the outgroup
     */
    public AbstractAction getChooseOutgroup() {
        if (chooseOutgroupAction != null)
            return chooseOutgroupAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                //Get the set of selected taxa (maybe add this to viewer?)
                NodeSet selectedNodes = viewer.getSelectedNodes();
                TaxaSet outgroup = new TaxaSet();
                for (Node v : selectedNodes) {
                    for (Integer t : viewer.getPhyloGraph().getTaxa(v)) {
                        outgroup.set(t);
                    }
                }

                Document doc = dir.getDocument();
                doc.setProgressListener(new ProgressSilent());
                final ICommand cmd = new SetOutgroupCommand(viewer, outgroup);
                new Edit(cmd, "set outgroup").execute(viewer.getUndoSupportNetwork());
                doc.setProgressListener(null);
            }
        };
        action.putValue(AbstractAction.NAME, "Set Outgroup");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set the outgroup to one of the currently selected taxa");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DEPENDS_ON_NODESELECTION, true);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return chooseOutgroupAction = action;
    }

    private AbstractAction clearAllTaxaSetsAction = null;

    /**
     * Returns action.
     * <p/>
     * Action clears all taxa sets form the sets block
     */
    public AbstractAction getClearAllTaxsSets() {
        if (clearAllTaxaSetsAction != null)
            return clearAllTaxaSetsAction;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                //todo: Make this undo-able.
                if (dir.getDocument().getSets() != null) {
                    dir.getDocument().getSets().clearTaxaSets();
                    viewer.getMenuBar().updateTaxonSets();
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Clear all taxa sets");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Create a new taxa set from selected taxa");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return clearAllTaxaSetsAction = action;
    }

    AbstractAction expandAll;

    public AbstractAction getExpandAll() {
        if (expandAll != null)
            return expandAll;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.getDataTree().expandAllNodes();
                // dir.notifyUpdateViewer(Director.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Expand All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Expands all nodes in the tree");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return expandAll = action;
    }

    AbstractAction collapseAll;

    public AbstractAction getCollapseAll() {
        if (collapseAll != null)
            return collapseAll;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.getDataTree().collapseAllTreeData();
                //dir.notifyUpdateViewer(Director.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Collapse All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Collapses all nodes in the tree");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return collapseAll = action;
    }

    final Map<String, Action> nameState2action = new HashMap<>();

    /**
     * this is used to change formatting for the named block
     *
     * @param cbox      check button
     * @param blockName name of block
     * @param state1    first state of switch, must coincide with setter name
     * @param state2    other state of swith
     * @return action
     */
    public AbstractAction getFormatChanger(final JCheckBoxMenuItem cbox,
                                           final String blockName, final String state1,
                                           final String state2, final char mnemonic) {
        String nameState = blockName + state1;
        AbstractAction action = (AbstractAction) nameState2action.get(nameState);
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String cmd = "begin " + blockName + "; format ";
                if (cbox.isSelected()) {
                    //Check which of the two checkboxes are selected
                    // and set their respective booleans (for updateEnableState)
                    if (state1.compareTo("Interleave") == 0)
                        isSelectedInterleaveCBox = true; //interleave is selected
                    else if (state1.compareTo("Transpose") == 0)
                        isSelectedTransposeCBox = true; //transpose is selected

                    cmd += state1;
                } else {
                    //Check which of the two checkboxes are unselected
                    // and set their respective booleans (for updateEnableState)
                    if (state1.compareTo("Interleave") == 0)
                        isSelectedInterleaveCBox = false; //interleave is not selected
                    else if (state1.compareTo("Transpose") == 0)
                        isSelectedTransposeCBox = false; //transpose is not selected

                    cmd += state2;
                }
                cmd += ";end;";
                dir.execute(cmd);
            }
        };
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.JCHECKBOX, cbox);
        action.putValue(DirectorActions.DEPENDS_ON, blockName);
        action.putValue(AbstractAction.NAME, state1);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set " + state1 + " format for " + blockName + " Nexus block");
        nameState2action.put(nameState, action);
        all.add(action);
        return action;
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
                    int lineNo = Integer.parseInt(JOptionPane.showInputDialog(viewer.getFrame(), "Go to line:")) - 1;
                    int a = viewer.editor.getInputTextArea().getLineStartOffset(lineNo);
                    int b = viewer.editor.getInputTextArea().getLineStartOffset(lineNo + 1) - 1;
                    viewer.editor.getInputTextArea().setCaretPosition(a);
                    viewer.editor.getInputTextArea().requestFocus();
                    viewer.editor.getInputTextArea().select(a, b);
                } catch (Exception ignored) {
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Go To Line...");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G,
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
                    viewer.getUndoManagerNetwork().undo();
                } catch (CannotUndoException ignored) {
                }
                updateUndo();
                updateRedo();

                // update node edge dialog:
                Configurator configurator = (Configurator) viewer.getDir().getViewerByClass(Configurator.class);
                if (configurator != null) {
                    configurator.updateView(Director.ALL);
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Undo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Undo");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Undo16.gif"));

        all.add(action);
        return undo = action;
    }//End of getUndo

    /**
     * updates the undo action
     */
    public void updateUndo() {
        if (viewer.getUndoManagerNetwork().canUndo()) {
            undo.setEnabled(true);
            undo.putValue(Action.NAME, viewer.getUndoManagerNetwork().getUndoPresentationName());
        } else {
            undo.setEnabled(false);
            undo.putValue(Action.NAME, "Undo");
        }
    }

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
                    viewer.getUndoManagerNetwork().redo();
                } catch (CannotRedoException ignored) {
                }
                updateRedo();
                updateUndo();

                // update node edge dialog:
                Configurator configurator = (Configurator) viewer.getDir().getViewerByClass(Configurator.class);
                if (configurator != null) {
                    configurator.updateView(Director.ALL);
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Redo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Redo");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Redo16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return redo = action;

    }//End of getRedo

    /**
     * updates the redo action
     */

    public void updateRedo() {
        if (viewer.getUndoManagerNetwork().canRedo()) {
            redo.setEnabled(true);
            redo.putValue(Action.NAME, viewer.getUndoManagerNetwork().getRedoPresentationName());
        } else {
            redo.setEnabled(false);
            redo.putValue(Action.NAME, "Redo");
        }
    }

    private AbstractAction replaceFile;

    public AbstractAction getReplaceFile() {
        AbstractAction action = replaceFile;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Document doc = dir.getDocument();
                if (doc != null && doc.isDirty()) {
                    int result = JOptionPane.showConfirmDialog(viewer.getFrame(),
                            "Current document has been modified\n" +
                                    "Save changes before replacing?",
                            "Save File",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result) {
                        case JOptionPane.CANCEL_OPTION:
                            return;
                        case JOptionPane.YES_OPTION:
                            getSaveFile().actionPerformed(null);
                            if (doc.isDirty())
                                return; // didn't save  so don't replace
                        default:
                        case JOptionPane.NO_OPTION:
                            break;
                    }
                }
                // set data to null so that open will reuse this window:
                if (doc != null)
                    doc.clear();
                MainViewer mainViewer = (MainViewer) dir.getMainViewer();
                mainViewer.getTextEditor().setEditText(null);
                // open file
                dir.getActions().getOpenFile().actionPerformed(null);
            }
        };
        action.putValue(AbstractAction.NAME, "Replace...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Replace the current document");


        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Replace16.gif"));
        all.add(action);
        return replaceFile = action;
    }

    private AbstractAction loadMultipleTrees;

    public AbstractAction getLoadMultipleTrees() {
        AbstractAction action = loadMultipleTrees;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean viewerIsEmpty = ((dir.getDocument() == null || !dir.getDocument().isValidByName(Taxa.NAME))
                        && (viewer.getTextEditor().getEditText() == null || viewer.getTextEditor().getEditText().length() == 0));

                if (!viewerIsEmpty) // viewer not empty, open a new one:
                {
                    Director newDirector = Director.newProject();
                    MainViewer newMainViewer = newDirector.showMainViewer();
                    newMainViewer.getActions().getLoadMultipleTrees().actionPerformed(null);
                    return;
                }

                File lastOpenFile = ProgramProperties.getFile("ImportTreesFile");
                dir.showMainViewer();

                boolean done = false;
                final List<File> files = new LinkedList<>();
                while (!done) {
                    final List<File> moreFiles = ChooseFileDialog.chooseFilesToOpen(viewer, lastOpenFile, new NewickTree(), new NewickTree(), event, "Trees to open");

                    if (moreFiles.size() > 0) {
                        for (int i = 0; i < moreFiles.size(); i++) {
                            if (i == 0)
                                ProgramProperties.put("ImportTreesFile", moreFiles.get(i).getAbsolutePath());
                            files.add(moreFiles.get(i));
                            System.err.println("added: " + moreFiles.get(i).getAbsolutePath());
                        }
                        switch (
                                JOptionPane.showConfirmDialog(viewer.getFrame(),
                                        "Added " + files.size() + " file(s) total, add another file?",
                                        "Load Trees", JOptionPane.YES_NO_CANCEL_OPTION)) {
                            case JOptionPane.YES_OPTION:
                                break;
                            case JOptionPane.NO_OPTION:
                                done = true; // ask again for file
                                break;
                            case JOptionPane.CANCEL_OPTION:
                                return;
                        }
                    } else
                        done = true;
                }
                try {
                    StringBuilder buf = new StringBuilder();
                    for (File file : files) {
						buf.append(" '").append(StringUtils.doubleBackSlashes(file.getAbsolutePath())).append("'");
                    }
                    viewer.getDir().execute("load treeFiles=" + buf + ";update;");
                } catch (Exception ex) {
                    new Alert("Load trees failed: " + ex.getMessage());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Load Multiple Trees...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Load trees from files and open in new window");


        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Import16.gif"));
        all.add(action);
        return loadMultipleTrees = action;
    }


    private AbstractAction multiLabeledTree;

    public AbstractAction getMultiLabeledTree() {
        AbstractAction action = multiLabeledTree;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean viewerIsEmpty = ((dir.getDocument() == null || !dir.getDocument().isValidByName(Taxa.NAME))
                        && (viewer.getTextEditor().getEditText() == null || viewer.getTextEditor().getEditText().length() == 0));

                if (!viewerIsEmpty) // viewer not empty, open a new one:
                {
                    Director newDirector = Director.newProject();
                    MainViewer newMainViewer = newDirector.showMainViewer();
                    newMainViewer.getActions().getMultiLabeledTree().actionPerformed(null);
                    return;
                }

                File lastOpenFile = ProgramProperties.getFile("MultiLabeledTreeFile");
                dir.showMainViewer();

                final File file = ChooseFileDialog.chooseFileToOpen(viewer, lastOpenFile, new NewickTree(), new NewickTree(), event, "Tree to open");

                if (file != null) {
                    String input = null;
                    try {
                        NewickTree newickTree = new NewickTree();
                        newickTree.setOptionConvertMultiLabeledTree(true);
                        try (FileReader r = new FileReader(file)) {
                            if (!newickTree.isApplicable(r))
                                throw new Exception("File not in Newick format");
                        }
                        try (FileReader r = new FileReader(file)) {
                            input = newickTree.apply(r);
                        }
                    } catch (Exception ex) {
                        Basic.caught(ex);
                        new Alert("Load and convert multi-labeled tree failed: " + ex.getMessage());
                    }

                    if (input != null && input.length() > 0) {
                        try {
                            viewer.getDir().getDocument().setFile(file.getAbsolutePath(), true);
                            viewer.getDir().read(new StringReader(input));
                            ProgramProperties.put("MultiLabeledTreeFile", file.getAbsolutePath());
                        } catch (Exception ex) {
                            new Alert("Load and convert multi-labeled tree failed: " + ex.getMessage());
                        }
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Load Multi-Labeled Tree...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Loads a multi-labeled tree and converts it to a single-labeled network");


        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Import16.gif"));

        all.add(action);

        return multiLabeledTree = action;
    }

    private AbstractAction concatenateSequences;

    /**
     * concatenate sequences from multiple files
     *
     * @return action
     */
    public AbstractAction getConcatenateSequences() {
        AbstractAction action = concatenateSequences;
        if (action != null) return action;

        action = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                boolean viewerIsEmpty = ((dir.getDocument() == null || !dir.getDocument().isValidByName(Taxa.NAME))
                        && (viewer.getTextEditor().getEditText() == null || viewer.getTextEditor().getEditText().length() == 0));

                if (!viewerIsEmpty) // viewer not empty, open a new one:
                {
                    Director newDirector = Director.newProject();
                    MainViewer newMainViewer = newDirector.showMainViewer();
                    newMainViewer.getActions().getLoadMultipleTrees().actionPerformed(null);
                    return;
                }

                File lastOpenFile = ProgramProperties.getFile("ConcatenateSequencesFile");
                dir.showMainViewer();

                //ToDo: implement mac file chooser.
                JFileChooser chooser = new JFileChooser(lastOpenFile);
                for (Object o : ImportManager.getFileFilter(Characters.NAME)) {
                    FileFilter filter = (FileFilter) o;
                    chooser.addChoosableFileFilter(filter);
                }
                chooser.setAcceptAllFileFilterUsed(true);
                chooser.setMultiSelectionEnabled(true);

                boolean done = false;
                List files = new LinkedList();
                while (!done) {
                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File[] moreFiles = chooser.getSelectedFiles();

                        for (int i = 0; i < moreFiles.length; i++) {
                            if (i == 0)
                                ProgramProperties.put("ConcatenateSequencesFile", moreFiles[i].getAbsolutePath());
                            files.add(moreFiles[i].getAbsolutePath());
                            System.err.println("added: " + moreFiles[i].getAbsolutePath());
                        }
                        switch (
                                JOptionPane.showConfirmDialog(viewer.getFrame(),
                                        "Added " + files.size() + " files so far. Add further files?",
                                        "Concatenate Sequences", JOptionPane.YES_NO_CANCEL_OPTION)) {
                            case JOptionPane.YES_OPTION:
                                break;
                            case JOptionPane.NO_OPTION:
                                done = true; // ask again for file
                                break;
                            case JOptionPane.CANCEL_OPTION:
                                return;
                        }
                    } else if (result == JFileChooser.CANCEL_OPTION)
                        done = true;
                }
                try {
                    StringBuilder buf = new StringBuilder();
                    for (Object file : files) {
						buf.append(" '").append(StringUtils.doubleBackSlashes((String) file)).append("'");
                    }
                    viewer.getDir().execute("load charfiles=" + buf + ";update;");
                } catch (Exception ex) {
                    new Alert("Concatenate character sequences failed: " + ex.getMessage());
                }
            }

        };

        action.putValue(AbstractAction.NAME, "Concatenate Sequences...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Concatenates sequences from files");


        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Import16.gif"));

        all.add(action);

        return concatenateSequences = action;
    }

    private AbstractAction collapseIdentical;

    /**
     * concatenate sequences from multiple files
     *
     * @return action
     */
    public AbstractAction getCollapseIdentical() {
        AbstractAction action = collapseIdentical;
        if (action != null) return action;

        action = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {

                if (dir.getDocument() == null)
                    return;
                boolean valid = (dir.getDocument().getCharacters() != null || dir.getDocument().getDistances() != null);
                if (!valid)
                    return;
                Document doc = dir.getDocument();
                Document newDoc = CharactersUtilities.collapseByType(doc.getTaxa(), doc.getCharacters(), doc.getDistances(), doc);

                if (newDoc.getTaxa().getNtax() == doc.getTaxa().getNtax()) {
                    new Alert("All taxa are distinct");
                } else {
                    StringWriter sw = new StringWriter();
                    try {
                        newDoc.getTaxa().write(sw);
                        if (newDoc.getCharacters() != null)
                            newDoc.getCharacters().write(sw, newDoc.getTaxa());
                        if (newDoc.getDistances() != null)
                            newDoc.getDistances().write(sw, newDoc.getTaxa());
                        Director newDir = Director.newProject(sw.toString(), dir.getDocument().getFile().getAbsolutePath());
                        newDir.getDocument().setTitle(newDoc.getTitle());
                        newDir.showMainViewer();
                    } catch (IOException ex) {
                        Basic.caught(ex);
                    }
                }
            }

        };

        action.putValue(AbstractAction.NAME, "Group Identical Haplotypes");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Group identical haplotypes");


        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/RowDelete16.gif"));

        all.add(action);

        return collapseIdentical = action;
    }


    public List getAll() {
        return all;
    }

    AbstractAction keepOnlySelectedTaxa;

    public Action getKeepOnlySelectedTaxa() {
        AbstractAction action = keepOnlySelectedTaxa;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                NodeSet selectedNodes = viewer.getSelectedNodes();
                PhyloSplitsGraph graph = viewer.getPhyloGraph();
                Set<String> keep = new HashSet<>();
                for (Node v : selectedNodes) {
                    for (Integer t : graph.getTaxa(v)) {
                        keep.add(dir.getDocument().getTaxa().getLabel(t));
                    }
                }
                Taxa taxa = dir.getDocument().getTaxa();
                if (taxa.getOriginalTaxa() != null)
                    taxa = taxa.getOriginalTaxa();
                StringBuilder buf = new StringBuilder();
                for (int t = 1; t <= taxa.getNtax(); t++)
                    if (!keep.contains(taxa.getLabel(t)))
                        buf.append(" ").append(taxa.getLabel(t));
                dir.execute("assume extaxa=" + buf + ";");
            }
        };
        action.putValue(AbstractAction.NAME, "Keep Only Selected Taxa");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Keep only the currently selected taxa, exclude all others");

        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);

        all.add(action);


        return keepOnlySelectedTaxa = action;
    }

    AbstractAction excludeSelectedTaxa;

    public Action getExcludeSelectedTaxa() {
        AbstractAction action = excludeSelectedTaxa;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Taxa taxa = dir.getDocument().getTaxa();
                if (!dir.getDocument().isValid(taxa))
                    return;
                NodeSet selectedNodes = viewer.getSelectedNodes();
                PhyloSplitsGraph graph = viewer.getPhyloGraph();
                TaxaSet hide = new TaxaSet();
                Iterator it = selectedNodes.iterator();

                StringBuilder buf = new StringBuilder();

                if (taxa.getOriginalTaxa() != null)  // all hidden taxa should stay hidden!
                {
                    Taxa originalTaxa = taxa.getOriginalTaxa();
                    TaxaSet hiddenTaxa = taxa.getHiddenTaxa();
                    if (hiddenTaxa != null)
                        for (int t = hiddenTaxa.getBits().nextSetBit(1); t > 0;
                             t = hiddenTaxa.getBits().nextSetBit(t + 1))
                            buf.append(" ").append(originalTaxa.getLabel(t));
                }

                while (it.hasNext()) {
                    Node v = (Node) it.next();
                    for (Integer id : graph.getTaxa(v)) {
                        if (!hide.get(id)) {
                            hide.set(id);
                            String name = taxa.getLabel(id);
                            if (name != null)
                                buf.append(" ").append(name);
                        }
                    }
                }
                dir.execute("assume extaxa=" + buf + ";");
            }

        };
        action.putValue(AbstractAction.NAME, "Exclude Selected Taxa");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude all currently selected taxa");

        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);
        all.add(action);
        return excludeSelectedTaxa = action;
    }

    AbstractAction restoreAllTaxa;

    public Action getRestoreAllTaxa() {
        AbstractAction action = restoreAllTaxa;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("assume extaxa=none;");
            }
        };
        action.putValue(AbstractAction.NAME, "Restore All Taxa");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Restore all taxa");

        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return restoreAllTaxa = action;
    }

    AbstractAction excludeGaps;
    JCheckBoxMenuItem excludeGapsCB;

    public AbstractAction getExcludeGaps(JCheckBoxMenuItem cbox) {
        AbstractAction action = excludeGaps;
        if (action != null)
            return action;
        excludeGapsCB = cbox;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String cmd = "assume exclude";
                if (!excludeGapsCB.isSelected())
                    cmd += " no";
                cmd += " gaps";
                if (!excludeNonParsimonyCB.isSelected())
                    cmd += " no";
                cmd += " nonparsimony";
                if (!excludeConstantCB.isSelected())
                    cmd += " no";
                cmd += " constant";
                dir.execute(cmd + ";");
            }
        };
        action.putValue(AbstractAction.NAME, "Exclude Gap Sites");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude all sites containing gaps");


        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return excludeGaps = action;
    }

    AbstractAction excludeNonParsimony;
    JCheckBoxMenuItem excludeNonParsimonyCB;

    public AbstractAction getExcludeNonParsimony(JCheckBoxMenuItem cbox) {
        AbstractAction action = excludeNonParsimony;
        if (action != null)
            return action;
        excludeNonParsimonyCB = cbox;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String cmd = "assume exclude";
                if (!excludeGapsCB.isSelected())
                    cmd += " no";
                cmd += " gaps";
                if (!excludeNonParsimonyCB.isSelected())
                    cmd += " no";
                cmd += " nonparsimony";
                if (!excludeConstantCB.isSelected())
                    cmd += " no";
                cmd += " constant";
                dir.execute(cmd + ";");
            }
        };
        action.putValue(AbstractAction.NAME, "Exclude Parsimony-Uninformative Sites");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude all sites that are parsimony-uniformative");


        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        return excludeNonParsimony = action;
    }

    AbstractAction excludeConstant;
    JCheckBoxMenuItem excludeConstantCB;

    public AbstractAction getExcludeConstant(JCheckBoxMenuItem cbox) {
        AbstractAction action = excludeConstant;
        if (action != null)
            return action;
        excludeConstantCB = cbox;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String cmd = "assume exclude";
                if (!excludeGapsCB.isSelected())
                    cmd += " no";
                cmd += " gaps";
                if (!excludeNonParsimonyCB.isSelected())
                    cmd += " no";
                cmd += " nonparsimony";
                if (!excludeConstantCB.isSelected())
                    cmd += " no";
                cmd += " constant";
                dir.execute(cmd + ";");
            }
        };
        action.putValue(AbstractAction.NAME, "Exclude Constant Sites");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude all constant sites");


        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return excludeConstant = action;
    }

    AbstractAction restoreCharacters;

    public AbstractAction getRestoreAllCharacters() {
        AbstractAction action = restoreCharacters;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("assume exclude no gaps no nonparsimony no constant;");
            }
        };
        action.putValue(AbstractAction.NAME, "Restore All Sites");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Restore all character sites");


        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return restoreCharacters = action;
    }


    JCheckBoxMenuItem greedilyMakeCompatibleCB;
    AbstractAction greedilyMakeCompatible;

    public AbstractAction getGreedilyMakeCompatible(JCheckBoxMenuItem cbox) {
        AbstractAction action = greedilyMakeCompatible;
        if (action != null)
            return action;
        greedilyMakeCompatibleCB = cbox;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JCheckBoxMenuItem) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("greedycompatible");
                else
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("none");
                dir.execute("update splits");
            }
        };
        action.putValue(AbstractAction.NAME, "Greedily Make Compatible");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Greedily make the set of splits compatible");

        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return greedilyMakeCompatible = action;
    }

    JCheckBoxMenuItem greedilyMakeWeaklyCompatibleCB;
    AbstractAction greedilyMakeWeaklyCompatible;

    public AbstractAction getGreedilyMakeWeaklyCompatible(JCheckBoxMenuItem cbox) {
        AbstractAction action = greedilyMakeWeaklyCompatible;
        if (action != null)
            return action;
        greedilyMakeWeaklyCompatibleCB = cbox;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JCheckBoxMenuItem) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("greedyWC");
                else
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("none");
                dir.execute("update splits");
            }
        };
        action.putValue(AbstractAction.NAME, "Greedily Make Weakly Compatible");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Greedily make the set of splits weakily compatible");

        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return greedilyMakeWeaklyCompatible = action;
    }

    AbstractAction hideSelectedEdges;

    AbstractAction getHideSelectedEdges() {
        if (hideSelectedEdges != null)
            return hideSelectedEdges;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    BitSet toHide = new BitSet();
                    PhyloSplitsGraph graph = viewer.getPhyloGraph();
                    Splits splits = dir.getDocument().getSplits();
                    for (Edge e : viewer.getSelectedEdges()) {
                        int s = graph.getSplit(e);
                        if (s >= 1 && s <= splits.getNsplits())
                            toHide.set(s);
                    }
                    for (int s = toHide.nextSetBit(1); s > 0; s = toHide.nextSetBit(s + 1))
                        viewer.removeSplit(s, true);
                    Network network = dir.getDocument().getNetwork();
                    network.syncPhyloGraphView2Network(dir.getDocument().getTaxa(), viewer);
                    viewer.repaint();
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Hide Selected Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide all splits currently selected in graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);

        all.add(action);
        return hideSelectedEdges = action;
    }


    AbstractAction hideUnselectedSplits;

    AbstractAction getHideUnselectedSplits() {
        if (hideUnselectedSplits != null)
            return hideUnselectedSplits;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    BitSet toKeep = new BitSet();
                    BitSet allSplits = new BitSet();
                    PhyloSplitsGraph graph = viewer.getPhyloGraph();
                    Splits splits = dir.getDocument().getSplits();
                    for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                        int s = graph.getSplit(e);
                        if (s >= 1 && s <= splits.getNsplits()) {
                            allSplits.set(s);
                            if (viewer.getSelected(e))
                                toKeep.set(s);
                        }
                    }
                    for (int s = allSplits.nextSetBit(1); s > 0; s = allSplits.nextSetBit(s + 1))
                        if (!toKeep.get(s))
                            viewer.removeSplit(s, true);
                    Network network = dir.getDocument().getNetwork();
                    network.syncPhyloGraphView2Network(dir.getDocument().getTaxa(), viewer);
                    viewer.repaint();
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Hide Unselected Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide all splits that are currently unselected in graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);

        all.add(action);
        return hideUnselectedSplits = action;
    }

    AbstractAction hideIncompatibleEdges;

    AbstractAction getHideIncompatibleEdges() {
        if (hideIncompatibleEdges != null)
            return hideIncompatibleEdges;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                BitSet toHide = new BitSet();
                try {
                    PhyloSplitsGraph graph = viewer.getPhyloGraph();
                    Splits splits = dir.getDocument().getSplits();
                    Iterator it = viewer.getSelectedEdges().iterator();
                    NodeArray node2selectedSplits = new NodeArray(graph);
                    while (it.hasNext()) {
                        Edge e = (Edge) it.next();
                        int s = graph.getSplit(e);
                        if (s >= 1 && s <= splits.getNsplits()) {
                            if (node2selectedSplits.get(e.getSource()) == null)
                                node2selectedSplits.put(e.getSource(), new BitSet());
                            ((BitSet) node2selectedSplits.get(e.getSource())).set(s);
                            if (node2selectedSplits.get(e.getTarget()) == null)
                                node2selectedSplits.put(e.getTarget(), new BitSet());
                            ((BitSet) node2selectedSplits.get(e.getTarget())).set(s);
                        }
                    }
                    for (Edge f = graph.getFirstEdge(); f != null; f = f.getNext()) {
                        int s = graph.getSplit(f);

                        BitSet A = (BitSet) node2selectedSplits.get(f.getSource());
                        BitSet B = (BitSet) node2selectedSplits.get(f.getTarget());
                        if (A != null && B != null && !A.get(s)
                                && !B.get(s) && A.intersects(B))
                            toHide.set(s);
                    }
                    for (int s = toHide.nextSetBit(1); s > 0; s = toHide.nextSetBit(s + 1))
                        viewer.removeSplit(s, true);
                    Network network = dir.getDocument().getNetwork();
                    network.syncPhyloGraphView2Network(dir.getDocument().getTaxa(), viewer);
                    viewer.repaint();
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Hide Incompatible Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide all splits incompatible with those currently selected in the graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);

        all.add(action);
        return hideIncompatibleEdges = action;
    }

    AbstractAction restoreHiddenEdges;

    AbstractAction getRestoreHiddenEdges() {
        if (restoreHiddenEdges != null)
            return restoreHiddenEdges;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("update splits");
            }
        };
        action.putValue(AbstractAction.NAME, "Redraw All Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Redraw complete graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);

        all.add(action);
        return restoreHiddenEdges = action;
    }

    AbstractAction hideSelected;

    public AbstractAction getHideSelected() {
        if (hideSelected != null)
            return hideSelected;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                BitSet toHide = new BitSet();
                try {
                    PhyloGraphView graphView = (PhyloGraphView) dir.getViewerByClass(MainViewer.class);
                    PhyloSplitsGraph graph = graphView.getPhyloGraph();
                    Taxa taxa = dir.getDocument().getTaxa();
                    Splits splits = dir.getDocument().getSplits();
                    for (Edge e : graphView.getSelectedEdges()) {
                        int s = graph.getSplit(e);
                        if (s >= 1 && s <= splits.getNsplits())
                            toHide.set(s);
                    }

                    if (splits.getOriginal() != null)
                        toHide = SplitsUtilities.matchPartialSplits(taxa, splits, toHide, splits.getOriginal());
                    BitSet hidden = BitSetUtils.asBitSet(dir.getDocument().getAssumptions().getExSplits());
                    toHide.or(hidden);
                    dir.getDocument().getAssumptions().setExSplits(BitSetUtils.asList(toHide));
                    dir.execute("update " + Splits.NAME);
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Exclude Selected Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude all splits currently selected in graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);

        all.add(action);
        return hideSelected = action;
    }

    AbstractAction restoreAllSplits;

    public AbstractAction getRestoreAllSplits() {
        AbstractAction action = restoreAllSplits;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                Taxa taxa = dir.getDocument().getTaxa();
                Splits splits = dir.getDocument().getSplits();
                dir.getDocument().getAssumptions().setExSplits(null);
                if (splits.getOriginal() != null) {
                    if (taxa.getOriginalTaxa() != null)
                        splits.restoreOriginal(taxa.getOriginalTaxa());
                    else
                        splits.restoreOriginal(taxa);
                }

                dir.execute("update " + Splits.NAME);

            }
        };
        action.putValue(AbstractAction.NAME, "Restore All Splits");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Restore all splits");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);


        all.add(action);

        return restoreAllSplits = action;
    }


    AbstractAction optimizeSelected;

    public AbstractAction getOptmizeSelected() {
        if (optimizeSelected != null)
            return optimizeSelected;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Document doc = dir.getDocument();
                try {
                    PhyloGraphView graphView = (PhyloGraphView) dir.getViewerByClass(MainViewer.class);
                    Taxa taxa = dir.getDocument().getTaxa();
                    Splits splits = dir.getDocument().getSplits();
                    EqualAngle equalAngle = new EqualAngle();

                    if (equalAngle.isApplicable(doc, taxa, splits)) {
                        // TODO: use configured method
                        doc.setProgressListener(new ProgressCmdLine());
                        equalAngle.setOptionDaylightIterations(5);
                        equalAngle.setOptionOptimizeBoxesIterations(5);
                        equalAngle.setOptionRunConvexHull(true);
                        equalAngle.applyToSelected(doc, taxa, splits, graphView);
                        graphView.repaint();
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                } finally {
                    doc.setProgressListener(null);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Optimize Layout");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Optimize layout of selected part of graph");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);

        all.add(action);
        return optimizeSelected = action;
    }

    private final Map launchTransform = new HashMap();

    /**
     * launch a characters transformation and then open method view
     *
     * @return apply action
     */

    public AbstractAction getLaunchTransform(final JCheckBoxMenuItem cbox, final String blockName, final Class clazz, final char shortCutKey) {
        return getLaunchTransform(cbox, blockName, clazz, shortCutKey, false);
    }


    /**
     * launch a characters transformation and then open method view
     *
     * @return apply action
     */

    public AbstractAction getLaunchTransform(final JCheckBoxMenuItem cbox, final String blockName, final Class clazz, final char shortCutKey, final boolean mustOpenWindow) {
        String name = clazz.getName();
        final String shortName = name.substring(name.lastIndexOf('.') + 1);

        AbstractAction action = (AbstractAction) launchTransform.get(blockName + "_" + shortName + mustOpenWindow);
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final boolean openWindow = mustOpenWindow || !ProgramProperties.get("HideDialog." + shortName, false);

                if (openWindow) {
                    viewer.getActions().getAddMethodWindowFilter(blockName, AlgorithmsWindow.METHOD, clazz,
                            shortCutKey).actionPerformed(null);
                } else // apply:
                {
                    switch (blockName) {
                        case Characters.NAME:
                            dir.execute("assume chartransform=" + shortName);
                            break;
                        case Unaligned.NAME:
                            dir.execute("assume unaligntransform=" + shortName);
                            break;
                        case Quartets.NAME:
                            dir.execute("assume quarttransform=" + shortName);
                            break;
                        case Distances.NAME:
                            dir.execute("assume disttransform=" + shortName);
                            break;
                        case Splits.NAME:
                            dir.execute("assume splitstransform=" + shortName);
                            break;
                        case Trees.NAME:
                            dir.execute("assume treestransform=" + shortName);
                            break;
                    }
                    SplitsTreeProperties.addRecentMethod(blockName, clazz.getName());
                }
            }
        };
        action.putValue(AbstractAction.NAME, shortName);
        try {
            action.putValue(DirectorActions.TRANSFORM, clazz.newInstance());
        } catch (Exception ignored) {
        }
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply " + shortName + " method");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, blockName);
        action.putValue(DirectorActions.JCHECKBOX, cbox);
        if (shortCutKey != 0)
            action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(shortCutKey,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        if (cbox != null)
            cbox.setEnabled(false); // start out disabled!
        all.add(action);
        launchTransform.put(blockName + "_" + shortName + mustOpenWindow, action);
        return action;
    }

    /**
     * launch one of the transforms that are available in the methods dialog
     *
     * @return action that launches the given class
     */
    public AbstractAction getLaunchTransform(final JCheckBoxMenuItem cbox, final String blockName, final Class clazz, final char shortCutKey, ImageIcon icon) {
        AbstractAction action = getLaunchTransform(cbox, blockName, clazz, shortCutKey);
        action.putValue(AbstractAction.SMALL_ICON, icon);
        action.putValue(DirectorActions.CRITICAL, true);
        return action;
    }

    AbstractAction estimateInvariableSites;

    public AbstractAction getEstimateInvariableSites() {
        AbstractAction action = estimateInvariableSites;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.analysis("characters once CaptureRecapture");
            }
        };
        action.putValue(AbstractAction.NAME, "Estimate Invariable Sites");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, CaptureRecapture.DESCRIPTION);

        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        return estimateInvariableSites = action;
    }

    AbstractAction conductPhiTest;

    public AbstractAction getConductPhiTest() {
        AbstractAction action = conductPhiTest;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.analysis("characters once PhiTest");
            }
        };
        action.putValue(AbstractAction.NAME, "Conduct Phi test for recombination");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, PhiTest.DESCRIPTION);

        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return conductPhiTest = action;
    }


    AbstractAction computePhylogeneticDiversity;

    public AbstractAction getComputePhylogeneticDiversity() {
        AbstractAction action = computePhylogeneticDiversity;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                StringBuilder buf = new StringBuilder();
                int count = 0;
                for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                    if (viewer.getSelected(v)) {
                        for (Integer id : viewer.getPhyloGraph().getTaxa(v)) {
                            buf.append(String.format(" %d", id));
                            count++;
                        }
                    }

                dir.analysis("splits once PhylogeneticDiversity SelectedTaxa=" + count + " " + buf);
            }
        };
        action.putValue(AbstractAction.NAME, "Compute Phylogenetic Diversity");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, PhylogeneticDiversity.DESCRIPTION);

        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return computePhylogeneticDiversity = action;
    }

    AbstractAction computeDeltaScore;

    public AbstractAction getComputeDeltaScore() {
        AbstractAction action = computeDeltaScore;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                StringBuilder buf = new StringBuilder();
                int count = 0;
                for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                    if (viewer.getSelected(v)) {
                        for (Integer t : viewer.getPhyloGraph().getTaxa(v)) {
                            buf.append(String.format(" %d", t));
                            count++;
                        }

                    }
                dir.analysis("distances once DeltaScore SelectedTaxa=" + count + " " + buf);
            }
        };
        action.putValue(AbstractAction.NAME, "Compute Delta Score");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, DeltaScore.DESCRIPTION);

        //action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);
        action.putValue(DirectorActions.DEPENDS_ON, Distances.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return computeDeltaScore = action;
    }


    AbstractAction testTreeness;

    public AbstractAction getTestTreeness() {
        AbstractAction action = testTreeness;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.analysis("bootstrap once TestTreeness");
            }
        };
        action.putValue(AbstractAction.NAME, "Test Treeness");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, TestTreeness.DESCRIPTION);

        action.putValue(DirectorActions.DEPENDS_ON, Bootstrap.NAME);
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        return testTreeness = action;
    }

    AbstractAction estimateAlpha;

    public AbstractAction getEstimateAlpha() {
        AbstractAction action = estimateAlpha;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.analysis("characters once GascuelGamma");
            }
        };
        action.putValue(AbstractAction.NAME, "Estimate Alpha Parameter");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, GascuelGamma.DESCRIPTION);

        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        return estimateAlpha = action;
    }

    AbstractAction clearRecentFilesMenu;

    /**
     * clear the recent files menu
     *
     * @return action
     */
    public AbstractAction getClearRecentFilesMenu() {
        AbstractAction action = clearRecentFilesMenu;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SplitsTreeProperties.clearRecentFiles();
            }
        };
        action.putValue(AbstractAction.NAME, "Clear");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear recent files");
        all.add(action);
        return clearRecentFilesMenu = action;

    }

    AbstractAction clearRecentMethodsMenu;

    /**
     * clear the recent methods menu
     *
     * @return action
     */
    public AbstractAction getClearRecentMethodsMenu() {
        AbstractAction action = clearRecentMethodsMenu;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SplitsTreeProperties.clearRecentMethods();
            }
        };
        action.putValue(AbstractAction.NAME, "Clear");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear recent methods");
        all.add(action);
        return clearRecentMethodsMenu = action;

    }

    private AbstractAction editNodeLabel;


    public AbstractAction getEditNodeLabel() {
        AbstractAction action = editNodeLabel;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                for (Node v : viewer.getSelectedNodes()) {
                    String label = viewer.getLabel(v);
                    label = JOptionPane.showInputDialog(viewer, "Edit Node Label:", label);
                    if (label != null && !label.equals(viewer.getLabel(v))) {
                        final ICommand cmd = new ChangeNodeLabelCommand(viewer, v, label);
                        new Edit(cmd, "edit label").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Edit Node Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Edit the node label");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);
        all.add(action);
        return editNodeLabel = action;
    }

    private AbstractAction copyNodeLabel;

    public AbstractAction getCopyNodeLabel() {
        AbstractAction action = copyNodeLabel;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (Node v : viewer.getSelectedNodes()) {
                    String label = viewer.getLabel(v);
                    if (label != null) {
                        if (first)
                            first = false;
                        else
                            buf.append(" ");
                        buf.append(label);
                    }
                }
                if (buf.toString().length() > 0) {
                    StringSelection selection = new StringSelection(buf.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Copy Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy the node label");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Copy16.gif"));
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, true);

        all.add(action);
        return copyNodeLabel = action;
    }

    private AbstractAction copyEdgeLabel;

    public AbstractAction getCopyEdgeLabel() {
        AbstractAction action = copyEdgeLabel;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (Edge e : viewer.getSelectedEdges()) {
                    String label = viewer.getLabel(e);
                    if (label != null) {
                        if (first)
                            first = false;
                        else
                            buf.append(" ");
                        buf.append(label);
                    }
                }
                if (buf.toString().length() > 0) {
                    StringSelection selection = new StringSelection(buf.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Copy Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy the edge label");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Copy16.gif"));
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);


        all.add(action);
        return copyEdgeLabel = action;
    }

    private AbstractAction editEdgeLabel;

    public AbstractAction getEditEdgeLabel() {
        AbstractAction action = editEdgeLabel;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                for (Edge v : viewer.getSelectedEdges()) {
                    String label = viewer.getLabel(v);
                    label = JOptionPane.showInputDialog(viewer, "Edit Label:", label);
                    if (label != null && !label.equals(viewer.getLabel(v))) {
                        final ICommand cmd = new ChangeEdgeLabelCommand(viewer, v, label);
                        new Edit(cmd, "edit label").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Edit Edge Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Edit the edge label");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, true);
        all.add(action);
        return editEdgeLabel = action;
    }

    private AbstractAction previousTree;

    public AbstractAction getPreviousTree() {
        AbstractAction action = previousTree;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                Document doc = dir.getDocument();
                if (doc.isValidByName(Trees.NAME) && doc.isValidByName(Assumptions.NAME)) {
                    Assumptions assumptions = doc.getAssumptions();
                    TreesTransform treesTransform = assumptions.getTreesTransform();
                    if (treesTransform instanceof TreeSelector) {
                        int which;
                        if (event != null && (event.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                            which = 1;
                        } else {
                            TreeSelector selector = (TreeSelector) treesTransform;
                            which = selector.getOptionWhich() - 1;
                        }
                        dir.execute("assume treestransform=TreeSelector Which=" + which + ";");
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Previous Tree");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the previous tree (or first, if Shift-key pressed)");
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Back16.gif"));
        action.putValue(EnableDisable.ENABLEDISABLE, (EnableDisable) () -> {
            Document doc = dir.getDocument();
            Assumptions assumptions = doc.getAssumptions();
            if (doc.isValidByName(Trees.NAME) && doc.isValidByName(Assumptions.NAME)) {
                TreesTransform treesTransform = assumptions.getTreesTransform();
                if (treesTransform instanceof TreeSelector) {
                    TreeSelector selector = (TreeSelector) treesTransform;
                    return selector.getOptionWhich() > 1;
                }
            }
            return false;
        });
        all.add(action);
        return previousTree = action;
    }

    private AbstractAction nextTree;

    public AbstractAction getNextTree() {
        AbstractAction action = nextTree;
        if (action != null) return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Document doc = dir.getDocument();
                Assumptions assumptions = doc.getAssumptions();
                if (doc.isValidByName(Trees.NAME) && doc.isValidByName(Assumptions.NAME)) {
                    TreesTransform treesTransform = assumptions.getTreesTransform();
                    if (treesTransform instanceof TreeSelector) {
                        int which;
                        if (event != null && (event.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                            which = doc.getTrees().getNtrees();
                        } else {
                            TreeSelector selector = (TreeSelector) treesTransform;
                            which = selector.getOptionWhich() + 1;
                        }
                        dir.execute("assume treestransform=TreeSelector Which=" + which + ";");
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Next Tree");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the next tree (or last, if Shift-key pressed)");
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Forward16.gif"));
        action.putValue(EnableDisable.ENABLEDISABLE, (EnableDisable) () -> {
            Document doc = dir.getDocument();
            Assumptions assumptions = doc.getAssumptions();
            if (doc.isValidByName(Trees.NAME) && doc.isValidByName(Assumptions.NAME)) {
                Trees trees = doc.getTrees();
                TreesTransform treesTransform = assumptions.getTreesTransform();
                if (treesTransform instanceof TreeSelector) {
                    TreeSelector selector = (TreeSelector) treesTransform;
                    return selector.getOptionWhich() < trees.getNtrees();
                }
            }
            return false;
        });
        all.add(action);
        return nextTree = action;
    }

    private AbstractAction edgeID;

    public AbstractAction getEdgeID() {
        AbstractAction action = edgeID;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new EdgeLabelsCommand(viewer, false, true, false, false, true);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Split");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edge by split id");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeID = action;
    }

    private AbstractAction edgeWeight;

    public AbstractAction getEdgeWeight() {
        AbstractAction action = edgeWeight;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new EdgeLabelsCommand(viewer, true, false, false, false, true);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Weight");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edge by weight");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeWeight = action;
    }

    private AbstractAction edgeConfidence;

    public AbstractAction getEdgeConfidence() {
        AbstractAction action = edgeConfidence;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new EdgeLabelsCommand(viewer, false, false, true, false, true);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Confidence Value");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edge by confidence value");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeConfidence = action;
    }

    private AbstractAction edgeInterval;

    public AbstractAction getEdgeInterval() {
        AbstractAction action = edgeInterval;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new EdgeLabelsCommand(viewer, false, false, false, true, true);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Interval");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edge by confidence interval");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeInterval = action;
    }

    private AbstractAction edgeNone;

    public AbstractAction getEdgeNone() {
        AbstractAction action = edgeNone;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new EdgeLabelsCommand(viewer, false, false, false, false, true);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Hide Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide edge label");
        all.add(action);
        return edgeNone = action;
    }

    private AbstractAction nodeName;

    public AbstractAction getNodeName() {
        AbstractAction action = nodeName;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ICommand cmd = new NodeLabelsCommand(viewer, true, false, true);
                new Edit(cmd, "node labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Name");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label node by taxon name");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return nodeName = action;
    }

    private AbstractAction nodeId;

    public AbstractAction getNodeId() {
        AbstractAction action = nodeId;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ICommand cmd = new NodeLabelsCommand(viewer, false, true, true);
                new Edit(cmd, "node labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Show Id");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label node by taxon id");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return nodeId = action;
    }

    private AbstractAction nodeNone;

    public AbstractAction getNodeNone() {
        AbstractAction action = nodeNone;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final ICommand cmd = new NodeLabelsCommand(viewer, false, false, true);
                new Edit(cmd, "node labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Hide Label");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide node label");
        all.add(action);
        return nodeNone = action;
    }

    private AbstractAction editTreeNames;

    public AbstractAction getEditTreeNames() {
        AbstractAction action = editTreeNames;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (TreesNameDialog.isApplicable(dir.getDocument()))
                    new TreesNameDialog(dir.getMainViewerFrame(), dir.getDocument());
                dir.notifyUpdateViewer(Director.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Edit Tree Names...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Change the names assigned to individual trees");
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        all.add(action);
        return editTreeNames = action;
    }

    private AbstractAction configureAllMethods; //adds an algorithms window

    public AbstractAction getConfigureAllMethods() {
        AbstractAction action = configureAllMethods;
        if (action != null) return action;
        // add a new viewer
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                getAddMethodWindowFilter(Unaligned.NAME, AlgorithmsWindow.METHOD, null,
                        (char) 0).actionPerformed(null);
                AlgorithmsWindow algorithmsWindow = (AlgorithmsWindow) dir.getViewerByClass(AlgorithmsWindow.class);
                if (algorithmsWindow != null)
                    algorithmsWindow.updateView(IDirector.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Configure Pipeline...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Choose and configure all methods in processing pipeline");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));

        all.add(action);
        return configureAllMethods = action;
    }


    private final Map addMethodWindowSelect = new HashMap();
    private AlgorithmsWindow methodViewer;

    /**
     * brings the algorithms window to the front and selected the named tab and
     * method
     *
     * @param blockName   name of data block
     * @param clazz       transform   class
     * @param shortCutKey short cut key or 0
     * @return an action that opens the algorithm which and selects a specific tab and method
     */
    public AbstractAction getAddMethodWindowFilter(final String blockName,
                                                   final String subPanel,
                                                   Class clazz, char shortCutKey) {
        String clazzName;
        if (clazz != null)
            clazzName = clazz.getName();
        else
            clazzName = "";
        final String transformName = clazzName;
        String shortName = "";
        if (transformName.indexOf('.') != -1)
            shortName = transformName.substring(transformName.lastIndexOf('.') + 1);
        String key = blockName + "_" + subPanel + " " + shortName;
        if (addMethodWindowSelect.containsKey(key))
            return (AbstractAction) addMethodWindowSelect.get(key);

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                // open viewer and bring to the front:
                if (dir.containsViewer(methodViewer)) {
                    methodViewer.getFrame().setState(JFrame.NORMAL);
                    methodViewer.getFrame().toFront();
                } else {
                    methodViewer = new AlgorithmsWindow(dir);
                    dir.addViewer(methodViewer);
                    methodViewer.updateView(Director.ALL);
                }
                methodViewer.select(blockName, subPanel, transformName);
                if (dir.isInUpdate())
                    methodViewer.lockUserInput();
            }
        };
        switch (subPanel) {
            case AlgorithmsWindow.METHOD:
                action.putValue(AbstractAction.NAME, shortName);
                action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply " + shortName);
                break;
            case AlgorithmsWindow.FILTER:
                action.putValue(AbstractAction.NAME, AlgorithmsWindow.FILTER + " " + blockName + "...");
                action.putValue(AbstractAction.SHORT_DESCRIPTION, "Filter the " + blockName + " data");
                break;
            case AlgorithmsWindow.SELECT:
                action.putValue(AbstractAction.NAME, AlgorithmsWindow.SELECT + " " + blockName + "...");
                action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select the " + blockName + " data");
                break;
            case AlgorithmsWindow.TRAITS:
                action.putValue(AbstractAction.NAME, "Edit traits...");
                action.putValue(AbstractAction.SHORT_DESCRIPTION, "Edit the taxa traits");
                break;
        }
        if (shortCutKey != 0)
            action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(shortCutKey,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        action.putValue(DirectorActions.DEPENDS_ON, blockName);
        action.putValue(DirectorActions.CRITICAL, true);

        all.add(action);
        addMethodWindowSelect.put(key, action);

        return action;
    }


    private AbstractAction taxaSetView;
    TaxaSetViewer taxaSetViewWindow;

    public AbstractAction getTaxaSetView() {
        AbstractAction action = taxaSetView;
        if (action != null) return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                SyncViewerToDoc.sync((MainViewer) dir.getViewerByClass(MainViewer.class), dir.getDocument());
                if (dir.containsViewer(taxaSetViewWindow)) {
                    taxaSetViewWindow.getFrame().setState(JFrame.NORMAL);
                    taxaSetViewWindow.getFrame().toFront();
                } else {
                    taxaSetViewWindow = new TaxaSetViewer(dir, (MainViewer) dir.getViewerByClass(MainViewer.class));
                    dir.addViewer(taxaSetViewWindow);
                    taxaSetViewWindow.updateView(Director.ALL);
                }
                updateTaxaSetView();
            }
        };
        action.setEnabled(false);
        //@todo edit strings and stuff
        action.putValue(AbstractAction.NAME, "Define Taxonomies...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Edit nested subsets of taxa");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return taxaSetView = action;
    }

    public void updateTaxaSetView() {
        System.out.println(dir.getDocument().getTaxa());
        taxaSetView.setEnabled(dir.getDocument().getTaxa() != null && dir.getDocument().getTaxa().getNtax() != 0);
    }

    private final Map<String, AbstractAction> menuTitleActionsSourceTab = new HashMap<>();

    /**
     * returns a  menu action
     */
    public AbstractAction getMenuTitleActionSourceTab(final String name, char mnemonicKey) {
        AbstractAction action = menuTitleActionsSourceTab.get(name);
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, name);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, name + " menu");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) (mnemonicKey));


        menuTitleActionsSourceTab.put(name, action);
        all.add(action);
        return action;
    }

    private final Map<String, AbstractAction> menuTitleActionsDataTab = new HashMap<>();

    /**
     * returns a  menu action
     */
    public AbstractAction getMenuTitleActionDataTab(final String name, char mnemonicKey) {
        AbstractAction action = menuTitleActionsDataTab.get(name);
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, name);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, name + " menu");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) (mnemonicKey));
        action.putValue(DirectorActions.CRITICAL, true);

        menuTitleActionsDataTab.put(name, action);
        all.add(action);
        return action;
    }


    private AbstractAction confidenceWindow;
    private ConfidenceWindow confidence;

    /**
     * the select node-edge configuration action
     */
    public AbstractAction getConfidenceWindow() {
        AbstractAction action = confidenceWindow;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (dir.containsViewer(confidence)) {
                    confidence.getFrame().setState(JFrame.NORMAL);
                    confidence.getFrame().toFront();
                } else {
                    confidence = new ConfidenceWindow(dir);
                    dir.addViewer(confidence);
                    confidence.updateView(Director.ALL);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Highlight Confidence...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Highlight the confidence of edges using edge-width or shading");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return confidenceWindow = action;
    }


    private AbstractAction nodeEdgeConfigAction;
    private Configurator nodeEdgeConfigurator;

    /**
     * the select node-edge configuration action
     */
    public AbstractAction getNodeEdgeConfigAction() {
        AbstractAction action = nodeEdgeConfigAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (dir.containsViewer(nodeEdgeConfigurator)) {
                    nodeEdgeConfigurator.getFrame().setState(JFrame.NORMAL);
                    nodeEdgeConfigurator.getFrame().toFront();
                } else {
                    nodeEdgeConfigurator = new Configurator(dir, (MainViewer) dir.getMainViewer());
                    dir.addViewer(nodeEdgeConfigurator);
                    nodeEdgeConfigurator.updateView(Director.ALL);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Nodes and Edges...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Configure the format of  nodes and edges");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke('J',
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return nodeEdgeConfigAction = action;
    }

    private AbstractAction configureEdges;

    /**
     * the select node-edge configuration action
     */
    public AbstractAction getConfigureEdges() {
        AbstractAction action = configureEdges;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                getNodeEdgeFormatterAction().actionPerformed(null);
            }
        };
        action.putValue(AbstractAction.NAME, "Format...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open formatter for nodes and edges");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return configureEdges = action;
    }

    private AbstractAction configureNodes;

    /**
     * the select node-node configuration action
     */
    public AbstractAction getConfigureNodes() {
        AbstractAction action = configureNodes;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                getNodeEdgeFormatterAction().actionPerformed(null);
            }
        };
        action.putValue(AbstractAction.NAME, "Format...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open formatter for nodes and edges");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return configureNodes = action;
    }

    private AbstractAction windowSize;

    public AbstractAction getWindowSize
            () {
        AbstractAction action = windowSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String original = viewer.getFrame().getWidth() + " x " + viewer.getFrame().getHeight();
                String result = JOptionPane.showInputDialog(viewer.getFrame(), "Set window size (width x height):", original);
                if (result != null && !result.equals(original)) {
                    int height = 0;
                    int width = 0;
                    StringTokenizer st = new StringTokenizer(result, "x ");
                    try {
                        if (st.hasMoreTokens())
                            width = Integer.parseInt(st.nextToken());
                        if (st.hasMoreTokens())
                            height = Integer.parseInt(st.nextToken());
                        if (st.hasMoreTokens())
                            throw new NumberFormatException("Unexcepted characters at end of string");
                        viewer.getFrame().setSize(width, height);
                        System.err.println("New size: " + viewer.getFrame().getSize());
                    } catch (NumberFormatException e) {
                        new Alert("Input error: " + e.getMessage());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Set Window Size...");
        action.putValue(AbstractAction.SMALL_ICON,
                ResourceManager.getIcon("sun/AlignJustifyHorizontal16.gif"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set size (width x height) of windows");
        all.add(action);
        return windowSize = action;
    }


    private AbstractAction highlightMidpointRoot;

    public AbstractAction getMidpointRoot() {
        AbstractAction action = highlightMidpointRoot;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.analysis(Network.NAME + " once MidpointRoot");
            }
        };
        action.putValue(AbstractAction.NAME, "Identify midpoint root for the network");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, MidpointRoot.DESCRIPTION);

        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return highlightMidpointRoot = action;
    }


    private AbstractAction magnifierAction;

    public AbstractAction getMagnifierAction(JCheckBoxMenuItem cbox) {
        AbstractAction action = magnifierAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JCheckBoxMenuItem cbox = (JCheckBoxMenuItem) getValue(DirectorActions.JCHECKBOX);
                viewer.setUseMagnify(cbox.isSelected());
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Use Magnifier");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Magnifier16.gif"));

        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Turn the magnfier on or off");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.JCHECKBOX, cbox);
        all.add(action);
        return magnifierAction = action;
    }

    private AbstractAction toggleMagnifierAction;

    public AbstractAction getToggleMagnifierAction() {
        AbstractAction action = toggleMagnifierAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.setUseMagnify(!viewer.isUseMagnify());
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Toggle Magnifier");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Magnifier16.gif"));

        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Turn the magnfier on or off");
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);
        return toggleMagnifierAction = action;
    }

    private AbstractAction magnifyAll;

    public AbstractAction getMagnifyAll(final JCheckBoxMenuItem cbox) {
        AbstractAction action = magnifyAll;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                viewer.setUseMagnifyAll(cbox.isSelected());
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Magnify All Mode");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Magnify the whole tree");
        action.putValue(DirectorActions.JCHECKBOX, cbox);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        all.add(action);
        return magnifyAll = action;
    }

    private AbstractAction nodeEdgeFormatterAction;
    public static Formatter nodeEdgeFormatter;

    /**
     * the select node-edge configuration action
     */
    public AbstractAction getNodeEdgeFormatterAction() {
        AbstractAction action = nodeEdgeFormatterAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (nodeEdgeFormatter == null) {
                    nodeEdgeFormatter = new Formatter(dir, viewer, true);
                    SyncViewerToDoc.sync(viewer, dir.getDocument());
                }
                nodeEdgeFormatter.getFrame().setState(JFrame.NORMAL);
                nodeEdgeFormatter.getFrame().toFront();
                nodeEdgeFormatter.getFrame().setVisible(true);
                nodeEdgeFormatter.updateView(Director.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Format Nodes and Edges...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Format nodes and edges");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_J,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Preferences16.gif"));
        all.add(action);
        return nodeEdgeFormatterAction = action;
    }

    // SELECTION STUFF:

    private AbstractAction selectAll = getSelectAll();

    public AbstractAction getSelectAll() {
        AbstractAction action = selectAll;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllNodes(true);
                viewer.setUseSplitSelectionModel(false);
                viewer.selectAllEdges(true);
                viewer.setUseSplitSelectionModel(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Select All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all nodes and edges");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectAll16new.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectAll = action;
    }

    private AbstractAction selectAllNonTerminal = getSelectAllNonTerminal();

    public AbstractAction getSelectAllNonTerminal() {
        AbstractAction action = selectAllNonTerminal;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllInnerNodes(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Select Non-Terminal");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all non-terminal nodes and edges");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectEdges16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectAllNonTerminal = action;
    }

    private AbstractAction selectLabeledNodes = getSelectLabeledNodes();

    public AbstractAction getSelectLabeledNodes() {
        AbstractAction action = selectLabeledNodes;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllLabeledNodes();
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Select Labeled Nodes");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all labeled Nodes");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectLeaves16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectLabeledNodes = action;
    }

    private AbstractAction selectInvert = getSelectInvert();

    public AbstractAction getSelectInvert() {
        AbstractAction action = selectInvert;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.invertSelection();
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Invert Selection");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Invert the current selection");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectInvert = action;
    }

    private AbstractAction selectNodes = getSelectNodes();

    public AbstractAction getSelectNodes() {
        AbstractAction action = selectNodes;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllNodes(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Select Nodes");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all nodes");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectNodes16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectNodes = action;
    }


    private AbstractAction selectEdges = getSelectEdges();

    public AbstractAction getSelectEdges() {
        AbstractAction action = selectEdges;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.setUseSplitSelectionModel(false);
                viewer.selectAllEdges(true);
                viewer.setUseSplitSelectionModel(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Select Edges");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all edges");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectEdges16.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return selectEdges = action;
    }

    private AbstractAction deselectAll = getDeselectAll();

    public AbstractAction getDeselectAll() {
        AbstractAction action = deselectAll;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllNodes(false);
                viewer.setUseSplitSelectionModel(false);
                viewer.selectAllEdges(false);
                viewer.setUseSplitSelectionModel(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Deselect All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Deselect all nodes and edges");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return deselectAll = action;
    }

    private AbstractAction deselectNodes = getDeselectNodes();


    public AbstractAction getDeselectNodes() {
        AbstractAction action = deselectNodes;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectAllNodes(false);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Deselect Nodes");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Deselect all nodes");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(DEPENDS_ON_NODESELECTION, true);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return deselectNodes = action;
    }

    private AbstractAction deselectEdges = getDeselectEdges();

    public AbstractAction getDeselectEdges() {
        AbstractAction action = deselectEdges;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.setUseSplitSelectionModel(false);
                viewer.selectAllEdges(false);
                viewer.setUseSplitSelectionModel(true);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "Deselect Edges");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Deselect all edges");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(DEPENDS_ON_EDGESELECTION, true);
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);

        return deselectEdges = action;
    }

    private AbstractAction selectLatest = getSelectLatest();

    public AbstractAction getSelectLatest() {
        AbstractAction action = selectLatest;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.selectNodesByLabels(MainViewer.getPreviouslySelectedNodeLabels());
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.NAME, "From Previous");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply the selection from a previous window or tree");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectAll16new.gif"));
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);

        return selectLatest = action;
    }

    private AbstractAction networkMidpoint = getNetworkMidpoint();

    public AbstractAction getNetworkMidpoint() {
        AbstractAction action = networkMidpoint;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    // NetworkUtilities.midpointNetwork(dir.getDocument().getTaxa(), dir.getDocument().getSplits(), viewer.getPhyloGraph());
                } catch (Exception ex) {
                    System.err.println("Midpoint rooting failed");
                    ex.printStackTrace();
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Midpoint Root");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Construct rooted network using midpoint rooting");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("SelectAll16new.gif"));
        // action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B,
        //         Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(DirectorActions.CRITICAL, true);
        all.add(action);

        return networkMidpoint = action;
    }


    private AbstractAction maintainEdgeLengths;

    public AbstractAction getMaintainEdgeLengths(final JCheckBoxMenuItem cbox) {
        AbstractAction action = maintainEdgeLengths;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                viewer.setMaintainEdgeLengths(cbox.isSelected());
            }
        };
        action.putValue(AbstractAction.NAME, "Lock Edge Lengths");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Lock edge lengths so that they cannot be changed interactively");
        action.putValue(DirectorActions.JCHECKBOX, cbox);
        all.add(action);
        return maintainEdgeLengths = action;
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
        action.putValue(DirectorActions.CRITICAL, true); // otherwise won't get enable state right
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
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Find16.gif"));
        all.add(action);

        return findReplaceAgain = action;
    }

    private AbstractAction inputDataDialog = getInputDataDialog();

    public AbstractAction getInputDataDialog() {
        AbstractAction action = inputDataDialog;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                InputDialog inputDialog = new InputDialog(viewer);
                inputDialog.setVisible(true);
            }
        };
        action.putValue(AbstractAction.NAME, "Enter Data...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Open dialog for entering data");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Import16.gif"));
        all.add(action);

        return inputDataDialog = action;
    }


    private AbstractAction reroot = getReroot();

    public AbstractAction getReroot() {
        AbstractAction action = reroot;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                getChooseOutgroup().actionPerformed(event);
                dir.execute("update " + Splits.NAME + ";");
            }
        };
        action.putValue(AbstractAction.NAME, "Reroot");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Reroot the current rooted tree or network");
        action.putValue(DirectorActions.CRITICAL, true);
        action.putValue(DEPENDS_ON_ROOTED, true);
        action.putValue(DEPENDS_ON_NODESELECTION, true);
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Reroot16.gif"));
        all.add(action);

        return reroot = action;
    }
}
