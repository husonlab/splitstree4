/*
 * Configurator.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.nodeEdge;

import jloda.graph.*;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.graphview.EdgeActionAdapter;
import jloda.swing.graphview.EdgeActionListener;
import jloda.swing.graphview.NodeActionAdapter;
import jloda.swing.graphview.NodeActionListener;
import jloda.swing.window.WindowListenerAdapter;
import jloda.util.ProgramProperties;
import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.undo.EdgeColorCommand;
import splitstree4.gui.undo.Edit;
import splitstree4.gui.undo.ICommand;
import splitstree4.gui.undo.NodeColorCommand;
import splitstree4.main.SplitsTreeProperties;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Objects;


public class Configurator implements IDirectableViewer {

    private boolean uptodate = false;
    private Director dir;
    private MainViewer viewer;
    private ConfiguratorActions actions;
    private ConfiguratorMenuBar menuBar;
    private JFrame frame;
    static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
    private JTabbedPane tabbedPane;


    private JComboBox nodeSize, nodeFont, nodeFontSize, nodeShape, edgeWidth, edgeFont, edgeFontSize, nodeLabels;
    private JCheckBox nodeBold, nodeItalic, edgeBold, edgeItalic, edgeWeights, edgeIDs, edgeConfidence, edgeInterval;
    private JColorChooser nodeColor, edgeColor;


    /**
     * sets up the window
     *
     * @param dir
     */
    public Configurator(final Director dir, MainViewer viewer0) {
        this.viewer = viewer0;
        this.dir = dir;
        actions = new ConfiguratorActions(this, dir);
        menuBar = new ConfiguratorMenuBar(this, dir);
        setUptoDate(true);

        frame = new JFrame();
        frame.setIconImages(ProgramProperties.getProgramIconImages());
        frame.setJMenuBar(menuBar);
        if (ProgramProperties.isMacOS())
            frame.setSize(585, 300);
        else
            frame.setSize(485, 300);
        dir.setViewerLocation(this);
        frame.setResizable(true);
        setTitle(dir);

        frame.getContentPane().add(getTabbedPane());
        frame.setVisible(true);

        final NodeActionListener nal = new NodeActionAdapter() {
            public void doSelect(NodeSet nodes) {
                updateView("sel_nodes");
            }

            public void doDeselect(NodeSet nodes) {
                updateView("sel_nodes");
            }

        };
        viewer0.addNodeActionListener(nal);
        final EdgeActionListener eal = new EdgeActionAdapter() {
            public void doSelect(EdgeSet edges) {
                updateView("sel_edges");
            }

            public void doDeselect(EdgeSet edges) {
                updateView("sel_edges");
            }
        };
        viewer0.addEdgeActionListener(eal);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                viewer.removeNodeActionListener(nal);
                viewer.removeEdgeActionListener(eal);
                dir.removeViewer(Configurator.this);
            }
        });

        updateView(Director.ALL);
    }

    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Configure nodes and edges - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Configure nodes and edges - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */

    public ConfiguratorActions getActions() {
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

    static int count = 1;

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }

        getActions().setIgnore(true); // only want to update stuff, ignore requests to perform events

        uptodate = false;
        getActions().setEnableCritical(true);
        getActions().updateEnableState();
        if (what.equals("sel_nodes") || what.equals(Director.ALL)) {
            if (viewer.getSelectedNodes().size() != 0) {
                int nSize = 0;
                int fSize = 0;
                int fStyle = 0;
                int nShape = 0;
                String fName = "";
                for (Node node : viewer.getSelectedNodes()) {
                    try {
                        int s = viewer.getWidth(node);
                        int ns = viewer.getShape(node);
                        int fs = viewer.getFont(node).getSize();
                        int fy = viewer.getFont(node).getStyle();
                        String fn = viewer.getFont(node).getFamily();
                        if (nSize == 0) {
                            nSize = s;
                            fSize = fs;
                            fStyle = fy;
                            fName = fn;
                            nShape = ns;
                        }
                        if (nSize != s) nSize = -1;
                        if (viewer.getLabel(node) != null &&
                                viewer.getLabel(node).length() > 0 && viewer.getNV(node).isLabelVisible()) {
                            if (fSize != fs) fSize = -1;
                            if (fStyle != fy) fStyle = -1;
                            if (!fName.equals(fn)) fName = "";
                        }
                        if (nShape != ns) nShape = -1;
                        if (nSize == -1 && fSize == -1 && fStyle == -1 && fName.equals("") && nShape == -1) break;
                    } catch (NotOwnerException e) {
                        e.printStackTrace();
                    }
                }
                if (nSize == -1)
                    nodeSize.setSelectedIndex(-1);
                else
                    nodeSize.setSelectedItem(Integer.toString(nSize));
                if (fSize == -1)
                    nodeFontSize.setSelectedIndex(-1);
                else
                    nodeFontSize.setSelectedItem(Integer.toString(fSize));
                if (fStyle == -1) {
                    nodeBold.setSelected(false);
                    nodeItalic.setSelected(false);
                } else {
                    if (fStyle == Font.BOLD) {
                        nodeBold.setSelected(true);
                        nodeItalic.setSelected(false);
                    }
                    if (fStyle == Font.ITALIC) {
                        nodeBold.setSelected(false);
                        nodeItalic.setSelected(true);
                    }
                    if (fStyle == Font.ITALIC + Font.BOLD) {
                        nodeBold.setSelected(true);
                        nodeItalic.setSelected(true);
                    }
                    if (fStyle == Font.PLAIN) {
                        nodeBold.setSelected(false);
                        nodeItalic.setSelected(false);
                    }
                }
                if (fName.equals(""))
                    nodeFont.setSelectedIndex(-1);
                else
                    nodeFont.setSelectedItem(fName);
                if (nShape == -1)
                    nodeShape.setSelectedIndex(-1);
                else
                    nodeShape.setSelectedIndex(nShape);
            } else {
                nodeSize.setSelectedIndex(-1);
                nodeFontSize.setSelectedIndex(-1);
                nodeBold.setSelected(false);
                nodeItalic.setSelected(false);
                nodeFont.setSelectedIndex(-1);
            }
        }

        if (what.equals("sel_edges") || what.equals(Director.ALL)) {
            if (viewer.getSelectedEdges().size() != 0) {
                int eWidth = 0;
                int fSize = 0;
                int fStyle = 0;
                String fName = "";
                for (Edge edge : viewer.getSelectedEdges()) {
                    try {
                        int w = viewer.getLineWidth(edge);
                        int fs = viewer.getFont(edge).getSize();
                        int fy = viewer.getFont(edge).getStyle();
                        String fn = viewer.getFont(edge).getFamily();
                        if (eWidth == 0) {
                            eWidth = w;
                            fSize = fs;
                            fStyle = fy;
                            fName = fn;
                        }
                        if (eWidth != w) eWidth = -1;
                        if (viewer.getLabel(edge) != null &&
                                viewer.getLabel(edge).length() > 0 && viewer.getEV(edge).isLabelVisible()) {
                            if (fSize != fs) fSize = -1;
                            if (fStyle != fy) fStyle = -1;
                            if (!Objects.equals(fName, fn)) fName = "";
                        }
                        if (eWidth == -1 && fSize == -1 && fStyle == -1 && fName.equals("")) break;
                    } catch (NotOwnerException e) {
                        e.printStackTrace();
                    }

                }
                if (eWidth == -1)
                    edgeWidth.setSelectedIndex(-1);
                else
                    edgeWidth.setSelectedItem(Integer.toString(eWidth));
                if (fSize == -1)
                    edgeFontSize.setSelectedIndex(-1);
                else
                    edgeFontSize.setSelectedItem(Integer.toString(fSize));
                if (fStyle == -1) {
                    edgeBold.setSelected(false);
                    edgeItalic.setSelected(false);
                } else {
                    if (fStyle == Font.BOLD) {
                        edgeBold.setSelected(true);
                        edgeItalic.setSelected(false);
                    }
                    if (fStyle == Font.ITALIC) {
                        edgeBold.setSelected(false);
                        edgeItalic.setSelected(true);
                    }
                    if (fStyle == Font.ITALIC + Font.BOLD) {
                        edgeBold.setSelected(true);
                        edgeItalic.setSelected(true);
                    }
                    if (fStyle == Font.PLAIN) {
                        edgeBold.setSelected(false);
                        edgeItalic.setSelected(false);
                    }
                }
                if (fName.equals(""))
                    edgeFont.setSelectedIndex(-1);
                else
                    edgeFont.setSelectedItem(fName);
            } else {
                edgeWidth.setSelectedIndex(-1);
                edgeFontSize.setSelectedIndex(-1);
                edgeBold.setSelected(false);
                edgeItalic.setSelected(false);
                edgeFont.setSelectedIndex(-1);
            }
        }
        frame.repaint();
        getActions().setIgnore(false); // ignore firing of events

        uptodate = true;
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        getActions().setEnableCritical(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        getActions().setEnableCritical(true);
        getActions().updateEnableState();
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


    /**
     * gets the content pane
     *
     * @return the content pane
     */
    public JTabbedPane getTabbedPane() {
        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane();
            tabbedPane.add("Nodes", getNodePanel());
            tabbedPane.add("Edges", getEdgePanel());
        }
        return tabbedPane;
    }

    private JPanel getNodePanel() {
        GridBagLayout gridBag = new GridBagLayout();
        JPanel pan = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();

        nodeFont = nodeFont();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(nodeFont, c);

        nodeFontSize = nodeFontSize();
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(nodeFontSize, c);

        nodeBold = nodeBold();
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        pan.add(nodeBold, c);

        nodeItalic = nodeItalic();
        c.gridx = 5;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(nodeItalic, c);

        nodeColor = nodeColor();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(nodeColor, c);


        JLabel lab = new JLabel("Node Size ");
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(lab, c);

        nodeSize = nodeSize();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(nodeSize, c);


        lab = new JLabel("Node Shape ");
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(lab, c);

        nodeShape = nodeShape();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(nodeShape, c);


        lab = new JLabel("Labels ");
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(lab, c);

        nodeLabels = nodeLabels();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(nodeLabels);


        return pan;
    }

    private JPanel getEdgePanel() {
        GridBagLayout gridBag = new GridBagLayout();
        JPanel pan = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();

        edgeFont = edgeFont();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(edgeFont, c);

        edgeFontSize = edgeFontSize();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeFontSize, c);

        edgeBold = edgeBold();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        pan.add(edgeBold, c);

        edgeItalic = edgeItalic();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 5;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(edgeItalic, c);


        edgeColor = edgeColor();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(edgeColor, c);


        edgeWeights = edgeWeights();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeWeights, c);

        edgeIDs = edgeIDs();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeIDs, c);

        edgeConfidence = edgeConfidence();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeConfidence, c);

        edgeInterval = edgeInterval();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeInterval, c);


        JLabel ewL = new JLabel("Edge Width");
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 3;
        //c.gridwidth = GridBagConstraints.REMAINDER;

        c.gridheight = 1;
        pan.add(ewL, c);


        edgeWidth = edgeWidth();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeWidth, c);


        return pan;
    }

    /**
     * @return Returns the viewer.
     */
    public MainViewer getViewer() {
        return viewer;
    }

    private JComboBox nodeSize() {
        Object[] possibleValues = {"1", "2", "3", "4", "5", "6", "7", "8", "10"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setMinimumSize(box.getPreferredSize());
        box.setAction(actions.getNodeSize());
        return box;
    }

    private JComboBox nodeShape() {
        Object[] possibleValues = {"none", "square", "circle"};
        JComboBox box = new JComboBox(possibleValues);
        box.setMinimumSize(box.getPreferredSize());

        box.setAction(actions.getNodeShape());
        return box;
    }

    private JComboBox nodeFont() {
        JComboBox box = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        box.setAction(actions.getNodeFont());
        box.setMinimumSize(box.getPreferredSize());

        return box;
    }

    private JComboBox nodeFontSize() {
        Object[] possibleValues = {"8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "32", "36", "40", "44"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setEditable(true);
        box.setAction(actions.getNodeFontSize());
        box.setMinimumSize(box.getPreferredSize());

        return box;
    }

    private JCheckBox nodeBold() {
        JCheckBox box = new JCheckBox("Bold");
        box.setAction(actions.getNodeFontBold());
        return box;
    }

    private JCheckBox nodeItalic() {
        JCheckBox box = new JCheckBox("Italic");
        box.setAction(actions.getNodeFontItalic());
        return box;
    }

    private JComboBox nodeLabels() {
        Object[] possibleValues = {"Names", "IDs", "Both", "None"};
        JComboBox box = new JComboBox(possibleValues);
        box.setAction(actions.getNodeLabels());
        return box;
    }

    private JColorChooser nodeColor() {
        final JColorChooser chooser = new JColorChooser();

        class ChListener implements ChangeListener {
            public void stateChanged(ChangeEvent e) {
                Color color = chooser.getColor();
                final ICommand cmd = new NodeColorCommand(getViewer(), color,
                        false, true, false, true, false);
                new Edit(cmd, "color").execute(getViewer().getUndoSupportNetwork());
            }
        }

        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (!panel.getClass().getName().equals("javax.swing.colorchooser.DefaultSwatchChooserPanel"))
                chooser.removeChooserPanel(panel);
        }
        chooser.setPreviewPanel(new JPanel());
        chooser.getSelectionModel().addChangeListener(new ChListener());

        return chooser;
    }

    private JColorChooser edgeColor() {
        final JColorChooser chooser = new JColorChooser();

        class ChListener implements ChangeListener {
            public void stateChanged(ChangeEvent e) {
                Color color = chooser.getColor();
                final ICommand cmd = new EdgeColorCommand(getViewer(), color,
                        false, true, true, false);
                new Edit(cmd, "color").execute(getViewer().getUndoSupportNetwork());
            }
        }

        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (!panel.getClass().getName().equals("javax.swing.colorchooser.DefaultSwatchChooserPanel"))
                chooser.removeChooserPanel(panel);
        }
        chooser.setPreviewPanel(new JPanel());
        chooser.getSelectionModel().addChangeListener(new ChListener());

        return chooser;
    }

    private JComboBox edgeFont() {
        JComboBox box = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        box.setAction(actions.getEdgeFont());
        return box;
    }

    private JComboBox edgeFontSize() {
        Object[] possibleValues = {"8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "32", "36", "40", "44"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setAction(actions.getEdgeFontSize());
        return box;
    }

    private JCheckBox edgeBold() {
        JCheckBox box = new JCheckBox("Bold");
        box.setAction(actions.getEdgeFontBold());
        return box;
    }

    private JCheckBox edgeItalic() {
        JCheckBox box = new JCheckBox("Italic");
        box.setAction(actions.getEdgeFontItalic());
        return box;
    }

    private JComboBox edgeWidth() {
        Object[] possibleValues = {"1", "2", "3", "4", "5", "6", "7", "8", "10"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setAction(actions.getEdgeWidth());
        return box;
    }

    private JCheckBox edgeWeights() {
        JCheckBox box = new JCheckBox("Weight");
        box.setAction(actions.getEdgeWeights());
        return box;
    }

    private JCheckBox edgeConfidence() {
        JCheckBox box = new JCheckBox("Confidence");
        box.setAction(actions.getEdgeConfidence());
        return box;
    }

    private JCheckBox edgeIDs() {
        JCheckBox box = new JCheckBox("IDs");
        box.setAction(actions.getEdgeIDs());
        return box;
    }

    private JCheckBox edgeInterval() {
        JCheckBox box = new JCheckBox("Intervals");
        box.setAction(actions.getEdgeInterval());
        return box;
    }

    /**
     * @return Returns the edgeConfidence.
     */
    public JCheckBox getEdgeConfidence() {
        return edgeConfidence;
    }

    public JCheckBox getEdgeIntervalBox() {
        return edgeInterval;
    }


    /**
     * @return Returns the edgeIDs.
     */
    public JCheckBox getEdgeIDs() {
        return edgeIDs;
    }

    /**
     * @return Returns the edgeNames.
     */
    public JCheckBox getEdgeWeights() {
        return edgeWeights;
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
        return "Configurator";
    }
}
