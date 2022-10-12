/*
 * MainViewer.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IMainViewer;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.ISearcher;
import jloda.swing.find.SearchManager;
import jloda.swing.find.TextAreaSearcher;
import jloda.swing.graphview.EdgeActionAdapter;
import jloda.swing.graphview.NodeActionAdapter;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.ProgramProperties;
import jloda.swing.window.WindowListenerAdapter;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.gui.Director;
import splitstree4.gui.nodeEdge.Configurator;
import splitstree4.gui.undo.UndoableEdgeLabelSearcher;
import splitstree4.gui.undo.UndoableNodeLabelSearcher;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Network;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;


/**
 * The main viewer
 * Daniel Huson, 2003
 */
public class MainViewer extends PhyloGraphView implements IDirectableViewer, IMainViewer {
	// we get most actions from the director, but not these;
	final MainViewerActions viewerActions;
	final MainViewerMenuBar menuBar;

	final MainViewerToolBar mainToolBar;

	// data tree stuff:
	private final DataTree dataTree; //JTree containing the data

	// source tab
	final TextEditor editor = new TextEditor();

	// undo stuff:
	private final UndoManager undoManagerNetwork = new UndoManager();
	private final UndoableEditSupport undoSupportNetwork = new UndoableEditSupport();

	private final UndoManager undoManagerText = new UndoManager();

	private final JFrame frame;
	private final StatusBar statusBar = new StatusBar();

	private boolean uptoDate; // have we redrawn after updateViewer request
	private Director dir = null; // the director

	private String layoutType = Network.CIRCULAR; // circular or rectilinear drawing? Affects zooming and rotation

	private boolean radiallyLayoutNodeLabels = false;

	static private final Set<String> previouslySelectedNodeLabels = new HashSet<>(); // keep track of latest selection

    private ISearcher[] searchers; // searchers

    /**
     * setup the viewer
     *
     * @param dir0 need to know the director
     */
    public MainViewer(final Director dir0) {
        super();
        getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        setGraphViewListener(new splitstree4.gui.main.GraphViewListener(this));

        undoSupportNetwork.addUndoableEditListener(new MyUndoableEditListener());

        setPOWEREDBY(null);

        int windowWidth = ProgramProperties.get(SplitsTreeProperties.WINDOW_WIDTH, 600);
        int windowHeight = ProgramProperties.get(SplitsTreeProperties.WINDOW_HEIGHT, 500);
        setSize(windowWidth, windowHeight);
        setDoubleBuffered(true);

        // doesn't seem to work:
        setFont(Font.decode("Dialog-PLAIN-14"));

        this.dir = dir0;

        viewerActions = new MainViewerActions(this, dir);

        //      setup menu bars
        menuBar = new MainViewerMenuBar(this, dir);
        mainToolBar = new MainViewerToolBar(this, dir);

        mainToolBar.setFocusable(false);

        SplitsTreeProperties.notifyListChange(SplitsTreeProperties.RECENTFILES); // to get menus setup
        SplitsTreeProperties.notifyListChange(SplitsTreeProperties.RECENTMETHODS); // to get menus setup
        SplitsTreeProperties.notifyListChange(SplitsTreeProperties.TOOLBARITEMS);//to get toolbar setup

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImages(ProgramProperties.getProgramIconImages());
        setTitle(dir);

        frame.setSize(getSize());
        dir.setViewerLocation(this);

        frame.setJMenuBar(menuBar);

        // setup graph and graphview:
        setCanvasColor(Color.white);
        setDrawScaleBar(true);
        setAutoLayoutLabels(true);
        setAllowEdit(false);
        setAllowEditNodeLabelsOnDoubleClick(false);
        setAllowEditEdgeLabelsOnDoubleClick(false);
        setUseMagnify(false);
        setUseMagnifyAll(false);

        SplitsTreeProperties.applyProperties(this);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(150);
        dataTree = new DataTree(dir);
        final JScrollPane dataScrollPane = new JScrollPane();
        dataScrollPane.getViewport().add(dataTree);
        splitPane.setLeftComponent(dataScrollPane);
        splitPane.setRightComponent(getScrollPane());
        getScrollPane().addKeyListener(getGraphViewListener());
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        // frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);

        frame.getContentPane().add(mainToolBar, BorderLayout.NORTH);

        frame.getContentPane().validate();
        frame.setVisible(true);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
				try {
					dir.close();
					frame.setVisible(false);
					frame.dispose();
				} catch (CanceledException ignored) {
				}
            }

            public void windowOpened(WindowEvent event) {
            }

            public void windowActivated(WindowEvent event) {
                if (MainViewerActions.nodeEdgeFormatter != null)
                    MainViewerActions.nodeEdgeFormatter.changeDirector(dir, MainViewer.this);

                SearchManager searchManager = SearchManager.getInstance();
                if (searchManager != null) {
                    searchManager.replaceSearchers(dir, getSearchers(), true);
                    if (MessageWindow.getInstance() != null)
                        searchManager.setEnabled(MessageWindow.SEARCHER_NAME, MessageWindow.getInstance().isVisible());
                    if (dir.getViewerByClass(SearchManager.class) == null)
                        dir.addViewer(searchManager);
                    searchManager.chooseTargetForFrame(MainViewer.this.getFrame());
                }
            }

            public void windowDeactivated(WindowEvent event) {
                Set<String> selectedLabels = getSelectedNodeLabels();
                if (selectedLabels.size() != 0) {
                    previouslySelectedNodeLabels.clear();
                    previouslySelectedNodeLabels.addAll(selectedLabels);
                }
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (event.getID() == ComponentEvent.COMPONENT_RESIZED &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    Dimension size = frame.getSize();
                    ProgramProperties.put(SplitsTreeProperties.WINDOW_WIDTH, (int) size.getWidth());
                    ProgramProperties.put(SplitsTreeProperties.WINDOW_HEIGHT, (int) size.getHeight());
                }
            }
        });

        // when selection changes, need to update menus:
        addNodeActionListener(new NodeActionAdapter() {
            public void doSelect(NodeSet v) {
                if (!MainViewer.this.isLocked()) {
                    getActions().updateEnableState();

                    getActions().updateEnableState();
                    if (dir.hasViewer(Configurator.class))
                        dir.getViewerByClass(Configurator.class).updateView("sel_nodes");
                }
            }

            public void doDeselect(NodeSet v) {
                if (!MainViewer.this.isLocked()) {
                    getActions().updateEnableState();
                    if (dir.hasViewer(Configurator.class))
                        dir.getViewerByClass(Configurator.class).updateView("sel_nodes");
                }
            }
        });
        addEdgeActionListener(new EdgeActionAdapter() {
            public void doSelect(EdgeSet e) {
                if (!MainViewer.this.isLocked()) {
                    getActions().updateEnableState();
                    if (dir.hasViewer(Configurator.class))
                        dir.getViewerByClass(Configurator.class).updateView("sel_edges");
                }
            }

            public void doDeselect(EdgeSet e) {
                if (!MainViewer.this.isLocked()) {
                    getActions().updateEnableState();
                    if (dir.hasViewer(Configurator.class))
                        dir.getViewerByClass(Configurator.class).updateView("sel_edges");
                }
            }
        });

        //tooltips won't work without this call:
        setToolTipText((String) null);
        //
        setPopupListener(new PopupListener(this, dir.getDocument(), getActions(), dir.getActions()));

        resetCursor();

        if (getAllowEdit())
            checkEditableGraph();
        else
            updateView(Director.ALL);
    }

    /**
     * gets the set of selected node labels
     *
     * @return selected node labels
     */
    public Set<String> getSelectedNodeLabels() {
        Set<String> selectedLabels = new HashSet<>();
        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v))
            if (getLabel(v) != null)
                selectedLabels.add(getLabel(v));
        return selectedLabels;

    }

    /**
     * Gets the Text Editor associated with the current Viewer.
     *
     * @return editor
     */
    public TextEditor getTextEditor() {
        return editor;
    }


    /**
     * gets all searchers required by this viewer
     *
     * @return all searchers
     */
    public ISearcher[] getSearchers() {
        if (searchers == null) {
            UndoableNodeLabelSearcher nodeSearcher = new UndoableNodeLabelSearcher(this.getFrame(), this);
            UndoableEdgeLabelSearcher edgeSearcher = new UndoableEdgeLabelSearcher(this.getFrame(), this);
            ISearcher sourceSeacher = new TextAreaSearcher("Source", editor.getInputTextArea());
            searchers = new ISearcher[]{nodeSearcher, edgeSearcher, sourceSeacher};
        }
        if (MessageWindow.getInstance() != null)  // if message window exists, add it to list of targets
        {
            ISearcher[] searchers2 = new ISearcher[searchers.length + 1];
            System.arraycopy(searchers, 0, searchers2, 0, searchers.length);
            searchers2[searchers2.length - 1] = new TextAreaSearcher("Messages", MessageWindow.getInstance().getTextArea());
            return searchers2;
        } else
            return searchers;
    }

    /**
     * Update the view.
     */
    public void updateView(String what) {
        setUptoDate(false);

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            if (MainViewerActions.nodeEdgeFormatter != null)
                MainViewerActions.nodeEdgeFormatter.changeDirector(dir, MainViewer.this);
            setUptoDate(true);
            return;
        }

        // only keep graph edits
        getUndoManagerNetwork().discardAllEdits();
        getActions().updateUndo();
        getActions().updateRedo();

        setTitle(dir);

        SyncDocToViewer.syncNetworkToViewer(dir.getDocument(), this);

        setAutoLayoutLabels(dir.getDocument() != null && dir.getDocument().isValidByName(Assumptions.NAME)
                && dir.getDocument().getAssumptions().getAutoLayoutNodeLabels());

        trans.setCoordinateRect(getBBox());

        // update the taxon sets:
        menuBar.updateTaxonSets();
        repaint();

        dataTree.updateDataTreeView();
        // dataTree.expandAllPreviouslyExpandedNodes();

        getActions().updateEnableState();

        statusBar.setStatusLine(dir.getDocument());

        setUptoDate(true);
    }

    /**
     * paint the graph
     */
    public void paint(Graphics g) {
        trans.getMagnifier().setInRectilinearMode(!getLayoutType().equals(Network.CIRCULAR));

        if (getRadiallyLayoutNodeLabels())
            RadiallyLayoutNodeLabels.doCircularLayoutNodeLabels(this);

        super.paint(g);
    }

    /**
     * if we are in edit mode, make sure there is something to edit
     */
    public void checkEditableGraph() {
        if (getGraph().getNumberOfNodes() == 0 && getAllowEdit()) {
            // this is an empty graph, but we want to be able to edit it, so give it a node
            /*
            Node v = newNode();
            setHeight(v, 2);
            setWidth(v, 2);
            setShape(v, OVAL_NODE);
            setLocation(v, 0, 0);
            setSelected(v, true);
            */
            fitGraphToWindow();
        }
    }

    /**
     * lock all critical user input
     */
    public void lockUserInput() {
        if (!locked) {
            locked = true;
            statusBar.setText2("Updating...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getActions().setEnableCritical(false);
        }
    }

    /**
     * unlock all critical user input
     */
    public void unlockUserInput() {
        if (locked) {
            Thread thread = Thread.currentThread();
            if (thread.getName().startsWith("AWT-EventQueue")) {
                getActions().setEnableCritical(true);
                getActions().updateEnableState();
                statusBar.setStatusLine(dir.getDocument());
                locked = false;
                resetCursor();
            } else {
                try {
					SwingUtilities.invokeAndWait(() -> {
						getActions().setEnableCritical(true);
						getActions().updateEnableState();
						statusBar.setStatusLine(dir.getDocument());
						locked = false;
						resetCursor();
					});
				} catch (InterruptedException | InvocationTargetException e) {
                    Basic.caught(e);
                }
            }
        }
    }

    /**
     * destroy stuff associated with this view
     */
    public void destroyView() throws CanceledException {
        boolean aboutToQuit = (ProjectManager.getProjects().size() == 1);

        if (dir.getDocument().isDirty()) {
            frame.toFront();
            int result = JOptionPane.showConfirmDialog(getFrame(), "Document has been modified, save before "
                            + (aboutToQuit ? "quitting?" : "closing?"),
                    "Question - " + SplitsTreeProperties.getVersion(), JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION)
                getActions().doSaveAsDialog();
            else if (result == JOptionPane.CANCEL_OPTION)
                throw new CanceledException();
        } else if (aboutToQuit && ProgramProperties.isUseGUI()) {
            int result = JOptionPane.showConfirmDialog(getFrame(), "Exit SplitsTree?", "SplitsTree4 - Exit?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result != JOptionPane.YES_OPTION)
                throw new CanceledException();
        }
        menuBar.dispose();
        this.getFrame().dispose();
    }

    /**
     * set the up-to-date flag
     */
    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * returns true, if viewer is uptodate after an update request
     */
    public boolean isUptoDate() {
        return this.uptoDate;
    }

    /**
     * returns the frame
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * returns the main viewers actions object
     *
     * @return actions object
     */
    public MainViewerActions getActions() {
        return viewerActions;
    }


    /**
     * sets the title of the window
     */
    public void setTitle(Director dir) {
        String newTitle = dir.getDocument().getTitle();
        if (dir.getDocument().isDirty())
            newTitle += "*";

        if (dir.getID() == 1)
            newTitle += " - " + SplitsTreeProperties.getVersion();
        else
            newTitle += " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();

        if (!frame.getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * gets the actual statusBar for changing its text with the Pane later
     *
     * @return statusBar
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * gets the treeData in order for the MainViewerActions.java to
     * do the collapse all and expand all
     *
     * @return treeData
     */
    public DataTree getDataTree() {
        return dataTree;
    }

    /**
     * gets the title of this viewer
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }


    /**
     * gets the undoManagerNetwork
     *
     * @return undoManagerNetwork  for the network
     */
    public UndoManager getUndoManagerNetwork() {
        return undoManagerNetwork;
    }

    /**
     * gets the undoManagerNetwork for text
     *
     * @return undoManagerNetwork
     */
    public UndoManager getUndoManagerText() {
        return undoManagerText;
    }


    /**
     * get undo support for the network tab
     *
     * @return undo support for network tab
     */
    public UndoableEditSupport getUndoSupportNetwork() {
        return undoSupportNetwork;
    }


    private final class MyUndoableEditListener implements UndoableEditListener {

        public void undoableEditHappened(UndoableEditEvent e) {
            undoManagerNetwork.addEdit(e.getEdit());
            getActions().updateUndo();
            getActions().updateRedo();
            dir.getDocument().setDirty(true); // if we edit, then this makes the document dirty!
        }
    }


    public Director getDir() {
        return dir;
    }

    public MainViewerToolBar getMainToolBar() {
        return mainToolBar;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(String layoutType) {
        this.layoutType = layoutType;
    }

    /**
     * get the quit action
     *
     * @return quit action
     */
    public AbstractAction getQuit() {
        return dir.getActions().getQuit();
    }

    /**
     * gets the main window menu
     *
     * @return window menu
     */
    public JMenu getWindowMenu() {
        return menuBar.getWindowMenu();
    }

    /**
     * gets the menu bar
     *
     * @return menu bar
     */
    public MainViewerMenuBar getMenuBar() {
        return menuBar;
    }

    public boolean getRadiallyLayoutNodeLabels() {
        return radiallyLayoutNodeLabels;
    }

    public void setRadiallyLayoutNodeLabels(boolean radiallyLayoutNodeLabels) {
        this.radiallyLayoutNodeLabels = radiallyLayoutNodeLabels;
    }

    public boolean isUseMagnify() {
        return trans.getMagnifier().isActive();
    }

    public void setUseMagnify(boolean useMagnify) {
        trans.getMagnifier().setActive(useMagnify);


    }

    public boolean isUseMagnifyAll() {
        return trans.getMagnifier().isHyperbolicMode();
    }

    public void setUseMagnifyAll(boolean useMagnifyAll) {
        trans.getMagnifier().setHyperbolicMode(useMagnifyAll);
    }

    /**
     * select nodes by labels
     *
     * @return true, if any changes made
     */
    public boolean selectNodesByLabels(Set labels) {
        boolean changed = false;
        if (labels.size() > 0) {
            for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (getLabel(v) != null && getLabel(v).length() > 0) {
                    String label = getLabel(v);
                    if (labels.contains(label) && !getSelected(v)) {
                        setSelected(v, true);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public static Set getPreviouslySelectedNodeLabels() {
        return previouslySelectedNodeLabels;
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return null;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "MainViewer";
    }
}
