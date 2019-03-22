/**
 * PreferencesWindow.java
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
/*
 * Created on 10.08.2004
 * preferences window
 *                   Markus Franz and Daniel Huson and David Bryant
 *
 */
package splitstree4.gui.preferences;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.ActionJList;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.WindowListenerAdapter;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.main.StatusBar;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Splits;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

public class PreferencesWindow implements IDirectableViewer {

    private PreferencesActions actions;
    private boolean uptodate = false;
    private Director dir;
    private MainViewer mainViewer;
    private JFrame frame;
    private JCheckBox graphEditable, maintainEdgeLengths, showScaleBar,
            useSplitSelectionMode;
    StatusBar statusBar;
    private JCheckBox fitCB, lsFitCB, taxaCB, charsCB, treesCB, splitsCB, assumptionsCB,
            verticesCB, edgesCB, nodeBold, nodeItalic, edgeBold, edgeItalic, edgeWeights, edgeIDs, edgeConfidence, showToolbar;
    private JRadioButton recomputeButton, stabilizeButton, snowballButton, keepButton;
    private JTextArea cycleText = null;
    private JComboBox nodeSize, nodeFont, nodeFontSize, nodeShape, edgeWidth, edgeFont, edgeFontSize, nodeLabels;
    private JColorChooser nodeColor, edgeColor;

    java.util.List all = new LinkedList();
    private DefaultListModel listl = null;
    private DefaultListModel listr = null;
    private ActionJList jlistl = null;
    private ActionJList jlistr = null;
    JLabel descriptionLabel = null;

    static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);


    public PreferencesWindow(Director dir, MainViewer viewer0) {
        this.mainViewer = viewer0;
        this.statusBar = this.mainViewer.getStatusBar();
        this.dir = dir;
        actions = new PreferencesActions(this, dir);
        setUptoDate(true);

        frame = new JFrame();
        if (ProgramProperties.getProgramIcon() != null)
            frame.setIconImage(ProgramProperties.getProgramIcon().getImage());
        frame.setSize(515, 310);
        dir.setViewerLocation(this);
        frame.setResizable(true);
        setTitle(dir);

        frame.getContentPane().add(getPane());
        frame.setVisible(true);

        final PreferencesWindow me = this;
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
//viewer.removeNodeActionListener(nal);
//viewer.removeEdgeActionListener(eal);
                me.dir.removeViewer(me);
            }
        });

        if (dir.isInUpdate())
            lockUserInput();
        else
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
            newTitle = "Preferences - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Preferences - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * gets the content pane
     *
     * @return the content pane
     */
    private JTabbedPane getPane() {
        JTabbedPane pane = new JTabbedPane();

        pane.add("General", getGraphPanel());
        pane.add("Defaults", getGeneralPanel());
        pane.add("Layout", getLayoutPanel());
        pane.add("Toolbar", getToolBarPanel());
        pane.add("Status Line", getStatusBarPanel());
        return pane;
    }

    private JTabbedPane getGeneralPanel() {
        JTabbedPane tPane = new JTabbedPane();

        JPanel nodePanel = getNodePanel();
        JPanel edgePanel = getEdgePanel();

        tPane.add("Nodes", nodePanel);
        tPane.add("Edges", edgePanel);

        return tPane;
    }

    private JPanel getNodePanel() {
        Font nFont = ProgramProperties.get("nFont", new Font("Default", Font.PLAIN, 10));
        int nSize = ProgramProperties.get("nSize", 1);
        //Color nColor=Properties.get("nColor", Color.BLACK);
        String nShape = ProgramProperties.get("nShape", "none");
        String nLabel = ProgramProperties.get("nLabel", "Names");

        GridBagLayout gridBag = new GridBagLayout();
        JPanel pan = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 5, 1, 5);

        nodeFont = nodeFont();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(nodeFont, c);
        nodeFont.setSelectedItem(nFont.getName());

        nodeFontSize = nodeFontSize();
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(nodeFontSize, c);
        nodeFontSize.setSelectedItem(Integer.toString(nFont.getSize()));

        nodeBold = nodeBold();
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        pan.add(nodeBold, c);
        if (nFont.getStyle() == Font.BOLD || nFont.getStyle() == Font.BOLD + Font.ITALIC)
            nodeBold.setSelected(true);
        else
            nodeBold.setSelected(false);

        nodeItalic = nodeItalic();
        c.gridx = 5;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(nodeItalic, c);
        if (nFont.getStyle() == Font.ITALIC || nFont.getStyle() == Font.BOLD + Font.ITALIC)
            nodeItalic.setSelected(true);
        else
            nodeItalic.setSelected(false);

        nodeColor = nodeColor();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(nodeColor, c);
        //nodeColor.setColor(nColor);

        nodeSize = nodeSize();
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = c.gridheight = GridBagConstraints.RELATIVE;
        pan.add(nodeSize, c);
        nodeSize.setSelectedItem(Integer.toString(nSize));

        JLabel lab = new JLabel("Node Size");
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        //c.gridheight = 1;
        pan.add(lab, c);

        nodeShape = nodeShape();
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        //c.gridheight = 1;
        pan.add(nodeShape, c);
        nodeShape.setSelectedItem(nShape);

        lab = new JLabel("Node Shape");
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        //c.gridheight = 1;
        pan.add(lab, c);

        nodeLabels = nodeLabels();
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 4;
        c.gridy = 2;
        c.gridwidth = 1;
        //c.gridheight = 1;
        pan.add(nodeLabels);
        nodeLabels.setSelectedItem(nLabel);

        lab = new JLabel("Labels");
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 5;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        //c.gridheight = 1;
        pan.add(lab, c);

        /*    JButton applyNodes = new JButton(getActions().getApplyNodes(nodeBold, nodeItalic, nodeSize, nodeFont, nodeFontSize, nodeShape, nodeLabels, nodeColor));
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            c.weighty = 0;
            c.gridx = 3;
            c.gridy = 3;
            c.gridheight = GridBagConstraints.REMAINDER;
            c.gridwidth = 1;
            pan.add(applyNodes, c);

            JButton oKNodes = new JButton(getActions().getOKNodes(nodeBold, nodeItalic, nodeSize, nodeFont, nodeFontSize, nodeShape, nodeLabels, nodeColor));
            c.weightx = 1;
            c.weighty = 0;
            c.gridx = 4;
            c.gridy = 3;
            c.gridwidth = GridBagConstraints.RELATIVE;
            pan.add(oKNodes, c);*/

        JButton closeNodes = new JButton(getActions().getClose());
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(closeNodes, c);

        return pan;
    }

    private JPanel getEdgePanel() {
        Font eFont = ProgramProperties.get("eFont", new Font("Default", Font.PLAIN, 10));
        int eWidth = ProgramProperties.get("eWidth", 1);
        Color eColor = ProgramProperties.get("eColor", Color.BLACK);
        String eLabel = ProgramProperties.get("eLabel", "Names");
        boolean eWeights = ProgramProperties.get("eWeights", false);
        boolean eConfidence = ProgramProperties.get("eConfidence", false);
        boolean eIDs = ProgramProperties.get("eIDs", false);

        GridBagLayout gridBag = new GridBagLayout();
        JPanel pan = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 5, 1, 5);

        edgeFont = edgeFont();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(edgeFont, c);
        edgeFont.setSelectedItem(eFont.getName());

        edgeFontSize = edgeFontSize();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        pan.add(edgeFontSize, c);
        edgeFontSize.setSelectedItem(Integer.toString(eFont.getSize()));

        edgeBold = edgeBold();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        pan.add(edgeBold, c);
        if (eFont.getStyle() == Font.BOLD || eFont.getStyle() == Font.BOLD + Font.ITALIC)
            edgeBold.setSelected(true);
        else
            edgeBold.setSelected(false);

        edgeItalic = edgeItalic();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 5;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(edgeItalic, c);
        if (eFont.getStyle() == Font.ITALIC || eFont.getStyle() == Font.BOLD + Font.ITALIC)
            edgeItalic.setSelected(true);
        else
            edgeItalic.setSelected(false);

        edgeColor = edgeColor();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(edgeColor, c);
        edgeColor.setColor(eColor);

        edgeWeights = edgeWeights();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeWeights, c);
        edgeWeights.setSelected(eWeights);

        edgeIDs = edgeIDs();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeIDs, c);
        edgeIDs.setSelected(eIDs);

        edgeConfidence = edgeConfidence();
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeConfidence, c);
        edgeConfidence.setSelected(eConfidence);

        edgeWidth = edgeWidth();
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(edgeWidth, c);
        edgeWidth.setSelectedItem(Integer.toString(eWidth));

        JLabel ewL = new JLabel("Edge Width");
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 4;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        pan.add(ewL, c);
/*
        JButton defaultEdges = new JButton(getActions().getDefaultEdges(edgeBold,edgeItalic,edgeWeights,edgeIDs,edgeConfidence,edgeWidth,edgeFont,edgeFontSize,edgeColor));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 3;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = 1;
        pan.add(defaultEdges, c);

        JButton applyEdges = new JButton(getActions().getApplyEdges(edgeBold,edgeItalic,edgeWeights,edgeIDs,edgeConfidence,edgeWidth,edgeFont,edgeFontSize,edgeColor));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 3;
        c.gridy = 3;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = 1;
        pan.add(applyEdges, c);

        JButton oKEdges = new JButton(getActions().getOKEdges(edgeBold,edgeItalic,edgeWeights,edgeIDs,edgeConfidence,edgeWidth,edgeFont,edgeFontSize,edgeColor));
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 4;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.RELATIVE;
        pan.add(oKEdges, c);
*/
        JButton closeEdges = new JButton(getActions().getClose());
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(closeEdges, c);


        return pan;
    }

    private JComboBox nodeFont() {
        JComboBox box = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        box.setAction(actions.getNodeFont());
        return box;
    }

    private JComboBox nodeSize() {
        Object[] possibleValues = {"1", "2", "3", "4", "5", "6", "7", "8", "10"};
        JComboBox box = new JComboBox(possibleValues);
        box.setAction(actions.getNodeSize());
        return box;
    }

    private JComboBox nodeShape() {
        Object[] possibleValues = {"none", "square", "circle"};
        JComboBox box = new JComboBox(possibleValues);
        box.setAction(actions.getNodeShape());
        return box;
    }

    private JComboBox nodeFontSize() {
        Object[] possibleValues = {"6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "20", "22", "24", "26", "28", "32", "36", "40", "44"};
        JComboBox box = new JComboBox(possibleValues);
        box.setEditable(true);
        box.setAction(actions.getNodeFontSize());
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
                ProgramProperties.put("nColor", color);
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
                ProgramProperties.put("eColor", color);
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
        Object[] possibleValues = {"6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "20", "22", "24", "26", "28", "32", "36", "40", "44"};
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


    private JPanel getGraphPanel() {
        GridBagLayout gridBag = new GridBagLayout();
        JPanel pan = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 5, 1, 5);

        graphEditable = new JCheckBox(actions.getAllowEdit());
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;//2;
        c.gridheight = 1;
        pan.add(graphEditable, c);

        maintainEdgeLengths = new JCheckBox(actions.getMaintainEdgeLengths());
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(maintainEdgeLengths, c);

        showScaleBar = new JCheckBox(actions.getDrawScaleBar());
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        pan.add(showScaleBar, c);

        useSplitSelectionMode = new JCheckBox(actions.getSelectSplits());
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        pan.add(useSplitSelectionMode, c);

        JButton defaultNodes = new JButton(getActions().getDefaultGraph(graphEditable, maintainEdgeLengths, showScaleBar, useSplitSelectionMode));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 4;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = 1;
        pan.add(defaultNodes, c);

        JButton applyGraph = new JButton(getActions().getApplyGraph(graphEditable, maintainEdgeLengths, showScaleBar, useSplitSelectionMode));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 4;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = 1;
        pan.add(applyGraph, c);

        JButton oKGraph = new JButton(getActions().getOKGraph(graphEditable, maintainEdgeLengths, showScaleBar, useSplitSelectionMode));
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.RELATIVE;
        pan.add(oKGraph, c);

        JButton closeGraph = new JButton(getActions().getClose());
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 3;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        pan.add(closeGraph, c);

        return pan;
    }

    private JPanel getLayoutPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 5, 1, 5);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        panel.add(new JLabel("Select the graph layout strategy:"), constraints);

        ButtonGroup group = new ButtonGroup();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        recomputeButton = new JRadioButton(getActions().getRecompute());
        group.add(recomputeButton);
        panel.add(recomputeButton, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        stabilizeButton = new JRadioButton(getActions().getStabilize());
        group.add(stabilizeButton);
        panel.add(stabilizeButton, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        snowballButton = new JRadioButton(getActions().getSnowball());
        group.add(snowballButton);
        panel.add(snowballButton, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        keepButton = new JRadioButton(getActions().getKeep());
        group.add(keepButton);
        panel.add(keepButton, constraints);

        cycleText = new JTextArea();
        AbstractAction action = getActions().getCycle();
        cycleText.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

        JScrollPane scrollP = new JScrollPane(cycleText);

// ((JTextArea) comp).addPropertyChangeListener(action);
// textArea.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 4;
        constraints.gridheight = GridBagConstraints.RELATIVE;
        panel.add(scrollP, constraints);

        JButton apply = new JButton(getActions().getApplyLayout(recomputeButton,
                stabilizeButton, snowballButton, keepButton, cycleText));
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        panel.add(apply, constraints);

        JButton ok = new JButton(getActions().getOKLayout(recomputeButton,
                stabilizeButton, snowballButton, keepButton, cycleText));
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        panel.add(ok, constraints);

        JButton close = new JButton(getActions().getClose());
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(close, constraints);


        return panel;
    }

    /**
     * Adapted from the following class:
     *
     */
    /**
     * Status Line Panel:
     * -------------------------
     * The next class is built in order to give more choices to the user
     * with respect to the Status Bar display. This is located in
     * the Edit menu of the Main Viewer Window and is called from MainViewerActions which
     * is called from MainViewerMenuBar() when building the main viewer menu bars.
     * <p/>
     * author Miguel Jette    Daniel Huson and David Bryant 11.2004
     * 7.8.2004
     */
    private JPanel getStatusBarPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 5, 1, 5);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        panel.add(new JLabel("Choose Status Line preferences:"), constraints);

        JLabel label = new JLabel("Show Fit:");
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        fitCB = new JCheckBox(getActions().getStatusBarFit());
        panel.add(fitCB, constraints);
        fitCB.setSelected(statusBar.getFit());

        label = new JLabel("Show LSFit:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        lsFitCB = new JCheckBox(getActions().getStatusBarLSFit());
        panel.add(lsFitCB, constraints);
        lsFitCB.setSelected(statusBar.getLsFit());


        label = new JLabel("Show Taxa:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        taxaCB = new JCheckBox(getActions().getStatusBarTaxa());
        panel.add(taxaCB, constraints);
        taxaCB.setSelected(statusBar.getTaxa());


        label = new JLabel("Show Chars:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        charsCB = new JCheckBox(getActions().getStatusBarChars());
        panel.add(charsCB, constraints);
        charsCB.setSelected(statusBar.getChars());


        label = new JLabel("Show Trees:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        treesCB = new JCheckBox(getActions().getStatusBarTrees());
        panel.add(treesCB, constraints);
        treesCB.setSelected(statusBar.getTrees());

        label = new JLabel("Show Splits:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        splitsCB = new JCheckBox(getActions().getStatusBarSplits());
        panel.add(splitsCB, constraints);
        splitsCB.setSelected(statusBar.getSplits());

        label = new JLabel("Show Assumptions:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        assumptionsCB = new JCheckBox(getActions().getStatusBarAssumptions());
        panel.add(assumptionsCB, constraints);
        assumptionsCB.setSelected(statusBar.getAssumptions());

        label = new JLabel("Show Vertices:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        verticesCB = new JCheckBox(getActions().getStatusBarVertices());
        panel.add(verticesCB, constraints);
        verticesCB.setSelected(statusBar.getVertices());

        label = new JLabel("Show Edges:");
        constraints.gridx = 0;
        constraints.gridy++;

        constraints.ipadx = GridBagConstraints.EAST;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        constraints.gridheight = GridBagConstraints.RELATIVE;
        edgesCB = new JCheckBox(getActions().getStatusBarEdges());
        panel.add(edgesCB, constraints);
        edgesCB.setSelected(statusBar.getEdges());

        JButton defaultStatus = new JButton(getActions().getDefaultStatus(fitCB, lsFitCB, taxaCB,
                charsCB, treesCB, splitsCB, assumptionsCB, verticesCB, edgesCB));
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        panel.add(defaultStatus, constraints);

        JButton applyStatus = new JButton(getActions().getApplyStatus(fitCB, lsFitCB, taxaCB,
                charsCB, treesCB, splitsCB, assumptionsCB, verticesCB, edgesCB));
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 2;
        //constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        panel.add(applyStatus, constraints);

        JButton oKStatus = new JButton(getActions().getOKStatus(fitCB, lsFitCB, taxaCB,
                charsCB, treesCB, splitsCB, assumptionsCB, verticesCB, edgesCB));
        constraints.weightx = 1;
        constraints.gridx = 3;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        panel.add(oKStatus, constraints);

        JButton closeStatus = new JButton(getActions().getClose());
        constraints.weightx = 1;
        constraints.gridx = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(closeStatus, constraints);

        return panel;
    }

    private JPanel getToolBarPanel() {
        JPanel panel = new JPanel();

        this.listl = new DefaultListModel();
        this.listr = new DefaultListModel();

        descriptionLabel = new JLabel();

        GridBagLayout gridBag = new GridBagLayout();
        panel.setLayout(gridBag);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("All Toolbar Buttons"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.gridheight = 4;
        gbc.weighty = 2;
        gbc.weightx = 2;
        gbc.gridx = 0;
        gbc.gridy = 1;

        this.jlistl = new ActionJList(listl);
        jlistl.setCellRenderer(new MyCellRenderer());
        JComponent input = new JScrollPane(jlistl);
        input.setToolTipText("All buttons that can be placed in the toolbar");
        gridBag.setConstraints(input, gbc);
        panel.add(input);

        /*
        // /Using custom actionListener for JList (listens for double-click and return key)
        jlistl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] indices = jlistl.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    listr.addElement(listl.getElementAt(indices[i]));
                }
            }
        });
        */

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 1;
        gbc.gridy = 1;

        input = new JButton(actions.getShowAction());
        gridBag.setConstraints(input, gbc);
        panel.add(input);

        gbc.gridx = 1;
        gbc.gridy = 2;

        input = new JButton(actions.getHideAction());
        gridBag.setConstraints(input, gbc);
        panel.add(input, gbc);


        gbc.gridx = 1;
        gbc.gridy = 3;

        input = new JButton(actions.getMoveUpAction());
        gridBag.setConstraints(input, gbc);
        panel.add(input, gbc);

        /*
        input = new JButton(getHideAllAction());
        gbc.gridx = 1;
        gbc.gridy = 4;
        gridBag.setConstraints(input, gbc);
        panel.add(input, gbc);
*/
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(new JLabel("Selected Toolbar Buttons"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.gridheight = 4;
        gbc.weighty = 2;
        gbc.weightx = 2;
        gbc.gridx = 2;
        gbc.gridy = 1;

        //jlistr = new JList(listr);
        jlistr = new ActionJList(listr); //Using the new kind of JList I created
        jlistr.setCellRenderer(new MyCellRenderer());

        input = new JScrollPane(jlistr);
        input.setToolTipText("Buttons visible in the toolbar");
        gridBag.setConstraints(input, gbc);
        panel.add(input);

        /*
        //Using custom actionListener for JList (listens for double-click and return key)
        jlistr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] indices = jlistr.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    listr.remove(indices[i]);
                }
            }
        });
         */

        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.gridx = 3;
        gbc.gridy = 1;
        showToolbar = showToolbar();
        gridBag.setConstraints(showToolbar, gbc);
        panel.add(showToolbar, gbc);

        gbc.gridx = 3;
        gbc.gridy = 2;
        input = new JButton(actions.getApplyToolbar());
        gridBag.setConstraints(input, gbc);
        panel.add(input, gbc);

        panel.setSize(500, 210); // 2 do

        initToolbarTab();

        return panel;
    }

    private JCheckBox showToolbar() {
        JCheckBox box = new JCheckBox("Show Toolbar");
        box.setAction(actions.getShowToolbar());
        return box;
    }

    public void updateView(String what) {

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        getActions().setEnableCritical(true);
        Document doc = dir.getDocument();
        uptodate = false;
        getActions().setEnableCritical(true);
        getActions().updateEnableState();

        graphEditable.setSelected(getMainViewer().getAllowEdit());
        maintainEdgeLengths.setSelected(getMainViewer().getMaintainEdgeLengths());
        showScaleBar.setSelected(getMainViewer().isDrawScaleBar());
        useSplitSelectionMode.setSelected(getMainViewer().getUseSplitSelectionModel());
        Assumptions assumptions = doc.getAssumptions();
        if (assumptions != null) {
            switch (assumptions.getLayoutStrategy()) {
                case Assumptions.STABILIZE:
                    stabilizeButton.setSelected(true);
                    break;
                case Assumptions.SNOWBALL:
                    snowballButton.setSelected(true);
                    break;
                case Assumptions.KEEP:
                    keepButton.setSelected(true);
                    break;
                default:
                    recomputeButton.setSelected(true);
            }
        } else
            recomputeButton.setSelected(true);

        Splits splits = doc.getSplits();
        if (splits != null && splits.getCycle() != null) {
            String cycleStr = "";
            int[] cycle = dir.getDocument().getSplits().getCycle();
            for (int t = 1; t <= dir.getDocument().getTaxa().getNtax(); t++)
                cycleStr += " " + cycle[t];
            cycleText.setText(cycleStr);
        } else
            cycleText.setText("");


        getActions().updateEnableState();
        uptodate = true;

    }

    /**
     * init the two lists in the tool bar tab
     */
    void initToolbarTab() {
        java.util.List elements = new LinkedList();

        elements.addAll(this.getMainViewer().getMenuBar().getActions());

        Map showActions = new HashMap();

        int index = 0;
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(SplitsTreeProperties.TOOLBARITEMS, ""), ";");
        while (st.hasMoreTokens()) {
            showActions.put(st.nextToken(), index++);
        }
        this.listr.setSize(index);

        for (Object element1 : elements) {
            AbstractAction element = (AbstractAction) element1;
            if (showActions.containsKey(element.getValue(AbstractAction.NAME))) {
                int index2 = (Integer) showActions.get(element.getValue(AbstractAction.NAME));
                this.listr.setElementAt(element, index2);
            } else
                this.listl.addElement(element);
        }

        this.showToolbar.setSelected(getMainViewer().getMainToolBar().isVisible());
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
     * @return actions
     */
    public PreferencesActions getActions() {

        return actions;
    }

    /**
     * @return Returns the viewer.
     */
    public MainViewer getMainViewer() {
        return mainViewer;
    }

    public boolean isUptoDate() {
        return uptodate;
    }


    public String getTitle() {
        return frame.getTitle();
    }

    public DefaultListModel getListl() {
        return listl;
    }

    public DefaultListModel getListr() {
        return listr;
    }

    public ActionJList getJlistl() {
        return jlistl;
    }

    public ActionJList getJlistr() {
        return jlistr;
    }

    public JCheckBox getShowToolbar() {
        return showToolbar;
    }


//Display an icon and a string for each object in the list.


    class MyCellRenderer extends JLabel implements ListCellRenderer {

        // This is the only method defined by ListCellRenderer.
        // We just reconfigure the JLabel each time we're called.

        public Component getListCellRendererComponent(JList list,
                                                      Object value, // value to display
                                                      int index, // cell index
                                                      boolean isSelected, // is the cell selected
                                                      boolean cellHasFocus)    // the list and the cell have the focus
        {
            AbstractAction a = (AbstractAction) value;

            String s;
            if (a == null)
                s = "Null";
            else if (a.getValue(Action.NAME) == null)
                s = "Untitled";
            else
                s = a.getValue(Action.NAME).toString();
            Icon i = null;
            if (a != null)
                i = (Icon) a.getValue(Action.SMALL_ICON);
            if (i == null) i = ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Help16.gif");
            setText(s);
            setIcon(i);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
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
        return "PreferencesWindow";
    }
}
