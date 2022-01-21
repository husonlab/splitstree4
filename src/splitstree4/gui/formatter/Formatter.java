/*
 * Formatter.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.formatter;

/**
 * format nodes and edges
 * Daniel Huson and David Bryant, 2.2007
 */

import jloda.graph.Edge;
import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.format.IFormatterListener;
import jloda.swing.graphview.*;
import jloda.swing.window.WindowListenerAdapter;
import jloda.util.ProgramProperties;
import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.main.SyncViewerToDoc;
import splitstree4.gui.undo.CompoundCommand;
import splitstree4.gui.undo.EdgeColorCommand;
import splitstree4.gui.undo.Edit;
import splitstree4.gui.undo.NodeColorCommand;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

/**
 * format nodes and edges
 */
public class Formatter implements IDirectableViewer {
    public static final String CONFIGURATOR_GEOMETRY = "ConfiguratorGeometry";
    public static final String DEFAULT_FONT = "DefaultFont";

    private final java.util.List<IFormatterListener> formatterListeners = new LinkedList<>();

    private boolean uptodate = false;
    private IDirector dir;
    private MainViewer viewer;
    private FormatterActions actions;
    private FormatterMenuBar menuBar;
    private JFrame frame;

    private JComboBox nodeSize, fontName, fontSize, nodeShape, edgeShape, edgeWidth;
    private JCheckBox boldFont, italicFont, labels, foregroundColor, backgroundColor, labelForegroundColor,
            labelBackgroundColor;
    private JColorChooser colorChooser;


    /**
     * constructor
     *
     * @param dir               the director
     * @param viewer            the graph view
     * @param showRotateButtons show label rotate buttons?
     */
    public Formatter(final IDirector dir, final MainViewer viewer, boolean showRotateButtons) {
        this.viewer = viewer;
        this.dir = dir;
        actions = new FormatterActions(this, dir);
        menuBar = new FormatterMenuBar(this, dir, this.viewer);
        setUptoDate(true);

        frame = new JFrame();
        frame.setIconImages(ProgramProperties.getProgramIconImages());
        frame.setJMenuBar(menuBar);
        int[] geometry = ProgramProperties.get(CONFIGURATOR_GEOMETRY, new int[]{100, 100, 600, 340});
        frame.setLocation(geometry[0], geometry[1]);
        frame.setSize(geometry[2], geometry[3]);

        //dir.setViewerLocation(this);
        frame.setResizable(true);
        setTitle(dir);

        frame.getContentPane().add(getPanel(showRotateButtons));
        frame.setVisible(true);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowActivated(WindowEvent windowEvent) {
                updateView("selection");
                updateView(IDirector.TITLE);
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (event.getID() == ComponentEvent.COMPONENT_RESIZED &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put(CONFIGURATOR_GEOMETRY, new int[]
                            {frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                                    frame.getSize().height});
                }
            }
        });

        final NodeActionListener nal = new NodeActionAdapter() {
            public void doSelect(NodeSet nodes) {
                updateView("selection");
            }

            public void doDeselect(NodeSet nodes) {
                updateView("selection");
            }

        };
        viewer.addNodeActionListener(nal);
        final EdgeActionListener eal = new EdgeActionAdapter() {
            public void doSelect(EdgeSet edges) {
                updateView("selection");
            }

            public void doDeselect(EdgeSet edges) {
                updateView("selection");
            }
        };
        viewer.addEdgeActionListener(eal);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                viewer.removeNodeActionListener(nal);
                viewer.removeEdgeActionListener(eal);
                dir.removeViewer(Formatter.this);
            }
        });
        updateView(Director.ALL);
        updateView(IDirector.ALL);
    }

    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(IDirector dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Format - " + viewer.getTitle() + " - " + ProgramProperties.getProgramName();
        else
            newTitle = "Format - " + viewer.getTitle() + " [" + dir.getID() + "] - " + ProgramProperties.getProgramName();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * set the viewer to a new viewer.  Usually called in windowActivated listener in main window
     * If this is used, frame is set not to destroy itself
     *
     * @param dir
     * @param viewer
     */
    public void changeDirector(final Director dir, MainViewer viewer) {
        this.frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.viewer = viewer;
        this.dir = dir;
        actions.setViewer(dir, this.viewer);
        menuBar.setViewer(dir, this.viewer);

        removeAllFormatterListeners();

        addFormatterListener(new IFormatterListener() {
            public void nodeFormatChanged(NodeSet nodes) {
                SyncViewerToDoc.sync(Formatter.this.viewer, dir.getDocument());
            }

            public void edgeFormatChanged(EdgeSet edges) {
                SyncViewerToDoc.sync(Formatter.this.viewer, dir.getDocument());
            }
        });
        setUptoDate(true);
        setTitle(dir);
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */

    public FormatterActions getActions() {
        return actions;
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {
        if (what.equals(IDirector.TITLE)) {
            setTitle(dir);
            return;
        }

        {
            getActions().setIgnore(true); // only want to update stuff, ignore requests to perform events

            uptodate = false;
            getActions().setEnableCritical(true);
            getActions().updateEnableState();
            if (what.equals("selection") || what.equals(IDirector.ALL)) {
                int fSize = 0;
                int fStyle = 0;
                int nShape = 0;
                String fName = "";
                boolean someLabelVisible = false;

                if (viewer.getSelectedNodes().size() != 0) {
                    int nSize = 0;
                    for (Node v : viewer.getSelectedNodes()) {
                        boolean labelVisible = viewer.getLabelVisible(v) && viewer.getLabel(v) != null && viewer.getLabel(v).length() > 0;
                        int s = viewer.getWidth(v);
                        int ns = viewer.getShape(v);
                        int fs = viewer.getFont(v).getSize();
                        int fy = viewer.getFont(v).getStyle();
                        String fn = viewer.getFont(v).getFamily();
                        if (nSize == 0) {
                            nSize = s;
                            nShape = ns;
                        }

                        if (nSize != s) nSize = -1;
                        if (labelVisible) {
                            if (fSize == 0) {
                                fSize = fs;
                                fStyle = fy;
                                fName = fn;
                            }
                            if (fSize != fs) fSize = -1;
                            if (fStyle != fy) fStyle = -1;
                            if (!fName.equals(fn)) fName = "";
                        }
                        if (nShape != ns) nShape = -1;
                        if (nSize == -1 && fSize == -1 && fStyle == -1 && fName.equals("") && nShape == -1) break;
                    }
                    if (nSize == -1)
                        nodeSize.setSelectedIndex(-1);
                    else
                        nodeSize.setSelectedItem(Integer.toString(nSize));
                    if (nShape == -1)
                        nodeShape.setSelectedIndex(-1);
                    else
                        nodeShape.setSelectedIndex(nShape);
                } else {
                    nodeSize.setSelectedIndex(-1);
                    fontSize.setSelectedIndex(-1);
                    boldFont.setSelected(false);
                    italicFont.setSelected(false);
                    fontName.setSelectedIndex(-1);
                }

                if (viewer.getSelectedEdges().size() != 0) {
                    int eShape = 0;
                    int eWidth = 0;
                    for (Edge e : viewer.getSelectedEdges()) {
                        boolean labelVisible = viewer.getLabelVisible(e) && viewer.getLabel(e) != null && viewer.getLabel(e).length() > 0;
                        int w = viewer.getLineWidth(e);
                        int es = viewer.getShape(e);
                        int fs = viewer.getFont(e).getSize();
                        int fy = viewer.getFont(e).getStyle();
                        String fn = viewer.getFont(e).getFamily();
                        if (eWidth == 0) {
                            eWidth = w;
                            eShape = es;
                        }
                        if (eWidth != w) eWidth = -1;
                        if (eShape != es)
                            eShape = -1;
                        if (labelVisible) {
                            if (fSize == 0) {
                                fSize = fs;
                                fStyle = fy;
                                fName = fn;
                            }
                            if (fSize != fs) fSize = -1;
                            if (fStyle != fy) fStyle = -1;
                            if (!fName.equals(fn)) fName = "";
                        }
                        if (eWidth == -1 && fSize == -1 && fStyle == -1 && fName.equals("") && eShape == -1) break;
                    }
                    if (eWidth == -1)
                        edgeWidth.setSelectedIndex(-1);
                    else
                        edgeWidth.setSelectedItem(Integer.toString(eWidth));
                    if (eShape == -1)
                        edgeShape.setSelectedIndex(-1);
                    else {
                        int i = 0;
                        if (eShape == EdgeView.STRAIGHT_EDGE)
                            i = 1;
                        else if (eShape == EdgeView.QUAD_EDGE)
                            i = 2;
                        edgeShape.setSelectedIndex(i);
                    }
                    //labels.setSelected(someLabelVisible);
                }
                if (fSize == -1)
                    fontSize.setSelectedIndex(-1);
                else
                    fontSize.setSelectedItem(Integer.toString(fSize));
                if (fStyle == -1) {
                    boldFont.setSelected(false);
                    italicFont.setSelected(false);
                } else {
                    if (fStyle == Font.BOLD) {
                        boldFont.setSelected(true);
                        italicFont.setSelected(false);
                    }
                    if (fStyle == Font.ITALIC) {
                        boldFont.setSelected(false);
                        italicFont.setSelected(true);
                    }
                    if (fStyle == Font.ITALIC + Font.BOLD) {
                        boldFont.setSelected(true);
                        italicFont.setSelected(true);
                    }
                    if (fStyle == Font.PLAIN) {
                        boldFont.setSelected(false);
                        italicFont.setSelected(false);
                    }
                }
                if (fName.equals(""))
                    fontName.setSelectedIndex(-1);
                else
                    fontName.setSelectedItem(fName);

                getActions().getSaveDefaultFont().setEnabled(fontName.getSelectedIndex() != -1 && fSize > 0);
                frame.repaint();
                getActions().setIgnore(false); // ignore firing of events
            }

            colorChooser.setEnabled(viewer.getSelectedNodes().size() > 0 || viewer.getSelectedEdges().size() > 0);
            uptodate = true;
        }
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        getActions().setEnableCritical(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        colorChooser.setEnabled(false);
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        getActions().setEnableCritical(true);
        getActions().updateEnableState();
        colorChooser.setEnabled(true);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() {
        this.getFrame().dispose();
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    /**
     * returns the frame of the window
     */
    public JFrame getFrame() {
        return frame;
    }

    private JPanel getPanel(boolean showRotateButtons) {
        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel fontPanel = new JPanel();
        fontPanel.setLayout(new BoxLayout(fontPanel, BoxLayout.X_AXIS));
        fontPanel.add(new JLabel("Font:"));
        fontPanel.add(fontName = makeFont());
        fontPanel.add(new JLabel("Size:"));
        fontPanel.add(fontSize = makeFontSize());
        fontPanel.add(boldFont = makeBold());
        fontPanel.add(italicFont = makeItalic());
        topPanel.add(fontPanel);

        JPanel colorPanel0 = new JPanel();
        colorPanel0.setLayout(new BoxLayout(colorPanel0, BoxLayout.Y_AXIS));
        colorPanel0.setBorder(BorderFactory.createEtchedBorder());

        JPanel colorPanel1 = new JPanel();
        colorPanel1.setLayout(new BoxLayout(colorPanel1, BoxLayout.X_AXIS));
        colorPanel1.add(colorChooser = makeColor());
        JPanel colorSubPanel = new JPanel();
        colorSubPanel.setBorder(BorderFactory.createEtchedBorder());
        colorSubPanel.setLayout(new GridLayout(4, 2));
        colorSubPanel.add(foregroundColor = makeForegroundColor());
        foregroundColor.setText("Line Color");
        foregroundColor.setSelected(true);
        colorSubPanel.add(backgroundColor = makeBackgroundColor());
        backgroundColor.setText("Fill Color");
        colorSubPanel.add(labelForegroundColor = makeLabelForegroundColor());
        labelForegroundColor.setText("Label Color");
        colorSubPanel.add(labelBackgroundColor = makeLabelBackgroundColor());
        labelBackgroundColor.setText("Label Fill Color");
        colorPanel1.add(colorSubPanel);
        colorPanel0.add(colorPanel1);

        JPanel colorPanel2 = new JPanel();
        colorPanel2.setLayout(new BoxLayout(colorPanel2, BoxLayout.X_AXIS));
        colorPanel2.add(new JButton(actions.getRandomColorActionAction()));
        colorPanel2.add(new JButton(actions.getNoColorActionAction()));
        colorPanel0.add(colorPanel2);

        topPanel.add(colorPanel0);

        JPanel nodePanel = new JPanel();
        nodePanel.setLayout(new BoxLayout(nodePanel, BoxLayout.X_AXIS));
        nodePanel.add(new JLabel("Node size: "));
        nodePanel.add(nodeSize = makeNodeSize());
        nodePanel.add(new JLabel("Node shape:"));
        nodePanel.add(nodeShape = makeNodeShape());
        topPanel.add(nodePanel);

        JPanel edgePanel = new JPanel();
        edgePanel.setLayout(new BoxLayout(edgePanel, BoxLayout.X_AXIS));
        edgePanel.add(new JLabel("Edge width:"));
        edgePanel.add(edgeWidth = makeEdgeWidth());
        edgePanel.add(new JLabel("Edge Style:"));
        edgePanel.add(edgeShape = makeEdgeShape());
        topPanel.add(edgePanel);

        JPanel nodeLabelPanel = new JPanel();
        nodeLabelPanel.setLayout(new BoxLayout(nodeLabelPanel, BoxLayout.X_AXIS));
        nodeLabelPanel.add(new JLabel("Node labels:"));

        JCheckBox cbox;
        cbox = new JCheckBox();
        cbox.setAction(actions.getNodeNames(cbox));
        cbox.setSelected(true);
        nodeLabelPanel.add(cbox);
        cbox = new JCheckBox();
        cbox.setAction(actions.getNodeIDs(cbox));
        nodeLabelPanel.add(cbox);

        // labels.setText("Show Labels");
        if (showRotateButtons) {
            nodeLabelPanel.add(Box.createHorizontalGlue());
            nodeLabelPanel.add(new JLabel("   Rotate Node Labels: "));
            nodeLabelPanel.add(new JButton(actions.getRotateLabelsLeft()));
            nodeLabelPanel.add(new JButton(actions.getRotateLabelsRight()));
        }

        topPanel.add(nodeLabelPanel);

        JPanel edgeLabelPanel = new JPanel();
        edgeLabelPanel.setLayout(new BoxLayout(edgeLabelPanel, BoxLayout.X_AXIS));

        edgeLabelPanel.add(new JLabel("Edge labels:"));
        cbox = new JCheckBox();
        cbox.setAction(actions.getEdgeWeights(cbox));
        edgeLabelPanel.add(cbox);
        cbox = new JCheckBox();
        cbox.setAction(actions.getEdgeIDs(cbox));
        edgeLabelPanel.add(cbox);
        cbox = new JCheckBox();
        cbox.setAction(actions.getEdgeConfidence(cbox));
        edgeLabelPanel.add(cbox);
        cbox = new JCheckBox();
        cbox.setAction(actions.getEdgeInterval(cbox));
        edgeLabelPanel.add(cbox);

        edgeLabelPanel.add(Box.createHorizontalGlue());
        edgeLabelPanel.add(new JButton(actions.getClose()));

        topPanel.add(edgeLabelPanel);
        return topPanel;
    }

    /**
     * @return Returns the viewer.
     */
    public MainViewer getViewer() {
        return viewer;
    }

    private JComboBox makeNodeSize() {
        Object[] possibleValues = {"1", "2", "3", "4", "5", "6", "7", "8", "10"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setMinimumSize(box.getPreferredSize());
        box.setAction(actions.getNodeSize());
        return box;
    }

    private JComboBox makeNodeShape() {
        Object[] possibleValues = {"none", "square", "circle"};
        JComboBox box = new JComboBox(possibleValues);
        box.setMinimumSize(box.getPreferredSize());

        box.setAction(actions.getNodeShape());
        return box;
    }


    private JComboBox makeEdgeShape() {
        Object[] possibleValues = {"angular", "straight", "curved"};
        JComboBox box = new JComboBox(possibleValues);
        box.setMinimumSize(box.getPreferredSize());

        box.setAction(actions.getEdgeShape());
        return box;
    }

    private JComboBox makeFont() {
        JComboBox box = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        box.setAction(actions.getFont());
        box.setMinimumSize(box.getPreferredSize());

        return box;
    }

    private JComboBox makeFontSize() {
        Object[] possibleValues = {"8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "32", "36", "40", "44"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setAction(actions.getFontSize());
        box.setMinimumSize(box.getPreferredSize());

        return box;
    }

    private JCheckBox makeBold() {
        JCheckBox box = new JCheckBox("Bold");
        box.setAction(actions.getNodeFontBold());
        return box;
    }

    private JCheckBox makeItalic() {
        JCheckBox box = new JCheckBox("Italic");
        box.setAction(actions.getNodeFontItalic());
        return box;
    }

    private JCheckBox makeForegroundColor() {
        JCheckBox cbox = new JCheckBox();
        cbox.setAction(actions.getForegroundColorAction(cbox));
        return cbox;
    }

    private JCheckBox makeBackgroundColor() {
        JCheckBox cbox = new JCheckBox();
        cbox.setAction(actions.getBackgroundColorAction(cbox));
        return cbox;
    }

    private JCheckBox makeLabelForegroundColor() {
        JCheckBox cbox = new JCheckBox();
        cbox.setAction(actions.getLabelForegroundColorAction(cbox));
        return cbox;
    }

    private JCheckBox makeLabelBackgroundColor() {
        JCheckBox cbox = new JCheckBox();
        cbox.setAction(actions.getLabelBackgroundColorAction(cbox));
        return cbox;
    }

    private JColorChooser makeColor() {
        final JColorChooser chooser = new JColorChooser();

        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (!panel.getClass().getName().equals("javax.swing.colorchooser.DefaultSwatchChooserPanel"))
                chooser.removeChooserPanel(panel);
        }
        chooser.setPreviewPanel(new JPanel());
        chooser.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                Color color = chooser.getColor();
                CompoundCommand cmd = new CompoundCommand();
                boolean fg = ((JCheckBox) getActions().getForegroundColorAction(null).getValue(FormatterActions.CHECKBOXITEM)).isSelected();
                boolean bg = ((JCheckBox) getActions().getBackgroundColorAction(null).getValue(FormatterActions.CHECKBOXITEM)).isSelected();
                boolean label = ((JCheckBox) getActions().getLabelForegroundColorAction(null).getValue(FormatterActions.CHECKBOXITEM)).isSelected();
                boolean lbg = ((JCheckBox) getActions().getLabelBackgroundColorAction(null).getValue(FormatterActions.CHECKBOXITEM)).isSelected();

                cmd.add(new EdgeColorCommand(viewer, color, false, fg, label, lbg));
                cmd.add(new NodeColorCommand(viewer, color, false, fg, bg, label, lbg));
                new Edit(cmd, "color").execute(viewer.getUndoSupportNetwork());
            }
        });
        return chooser;
    }

    private JComboBox makeEdgeWidth() {
        Object[] possibleValues = {"1", "2", "3", "4", "5", "6", "7", "8", "10"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setAction(actions.getEdgeWidth());
        return box;
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
     * fire node format changed
     *
     * @param nodes
     */
    void fireNodeFormatChanged(NodeSet nodes) {
        if (nodes != null && nodes.size() > 0) {
            for (Object formatterListener : formatterListeners) {
                IFormatterListener listener = (IFormatterListener) formatterListener;
                listener.nodeFormatChanged(nodes);
            }
        }
    }

    /**
     * fire edge format changed
     *
     * @param edges
     */
    void fireEdgeFormatChanged(EdgeSet edges) {
        if (edges != null && edges.size() > 0) {
            for (Object formatterListener : formatterListeners) {
                IFormatterListener listener = (IFormatterListener) formatterListener;
                listener.edgeFormatChanged(edges);
            }
        }
    }

    /**
     * add a formatter listener
     *
     * @param listener
     */
    public void addFormatterListener(IFormatterListener listener) {
        formatterListeners.add(listener);
    }

    /**
     * remove a formatter listener
     *
     * @param listener
     */
    public void removeFormatterListener(IFormatterListener listener) {
        formatterListeners.remove(listener);
    }

    public void removeAllFormatterListeners() {
        formatterListeners.clear();
    }

    public void saveFontAsDefault() {
        try {
            String family = fontName.getSelectedItem().toString();
            int size = Integer.parseInt(fontSize.getSelectedItem().toString());
            if (size > 0) {
                boolean bold = boldFont.isSelected();
                boolean italics = italicFont.isSelected();
                int style = 0;
                if (bold)
                    style += Font.BOLD;
                if (italics)
                    style += Font.ITALIC;
                ProgramProperties.put(DEFAULT_FONT, family, style, size);
            }
        } catch (Exception ex) {
        }
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
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return false;
    }


    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "Formatter";
    }

}
