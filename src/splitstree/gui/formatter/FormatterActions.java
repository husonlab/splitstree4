/**
 * FormatterActions.java 
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
package splitstree.gui.formatter;

import jloda.export.TransferableGraphic;
import jloda.graphview.EdgeView;
import jloda.gui.director.IDirector;
import jloda.util.Alert;
import jloda.util.ResourceManager;
import splitstree.gui.DirectorActions;
import splitstree.gui.MenuManager;
import splitstree.gui.main.MainViewer;
import splitstree.gui.undo.*;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a node-edge-configurator window
 */
public class FormatterActions {
    public final static String CHECKBOXITEM = "CheckBox";
    public final static String DEPENDS_ON_NODESELECTION = "NSEL";
    public final static String DEPENDS_ON_ONE_NODE_OR_EDGE = "ONORE";
    public final static String DEPENDS_ON_NODE_OR_EDGE = "NORE";
    public final static String DEPENDS_ON_XYLOCKED = "LOCK";
    public final static String DEPENDS_ON_NOT_XYLOCKED = "NLOCK";
    public final static String DEPENDS_ON_EDGESELECTION = "ESEL";
    public final static String TEXTAREA = "TA"; // text area object
    public final static String CRITICAL = "Critical"; // is action critical? bool

    final private Formatter formatter;
    private IDirector dir;
    final private List all = new LinkedList();
    private MainViewer viewer;

    private boolean ignore = false; // ignore firing when in update only of controls

    /**
     * constructor
     *
     * @param formatter
     * @param dir
     */
    FormatterActions(Formatter formatter, IDirector dir) {
        this.formatter = formatter;
        this.dir = dir;
        this.viewer = formatter.getViewer();
    }

    public void setViewer(IDirector dir, MainViewer viewer) {
        this.dir = dir;
        this.viewer = viewer;
    }

    /**
     * enable or disable critical actions
     *
     * @param on show or hide?
     */
    public void setEnableCritical(boolean on) {
        for (Object anAll : all) {
            AbstractAction action = (AbstractAction) anAll;
            if (action.getValue(CRITICAL) != null
                    && (((Boolean) action.getValue(CRITICAL))).equals(Boolean.TRUE))
                action.setEnabled(on);
        }
        if (on)
            updateEnableState();
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        for (Object anAll : all) {
            AbstractAction action = (AbstractAction) anAll;
            Boolean dependsOnNodeSelection = (Boolean) action.getValue(DEPENDS_ON_NODESELECTION);
            Boolean dependsOnEdgeSelection = (Boolean) action.getValue(DEPENDS_ON_EDGESELECTION);
            Boolean dependsOnOneNodeOrEdge = (Boolean) action.getValue(DEPENDS_ON_ONE_NODE_OR_EDGE);
            Boolean dependsOnNodeOrEdge = (Boolean) action.getValue(DEPENDS_ON_NODE_OR_EDGE);
            Boolean dependsOnXYLocked = (Boolean) action.getValue(DEPENDS_ON_XYLOCKED);

            action.setEnabled(true);
            if (dependsOnNodeSelection != null && dependsOnNodeSelection) {
                boolean enable = (viewer.getSelectedNodes() != null && viewer.getSelectedNodes().size() != 0);
                action.setEnabled(enable);
            }
            if (dependsOnEdgeSelection != null && dependsOnEdgeSelection) {
                boolean enable = (viewer.getSelectedEdges() != null && viewer.getSelectedEdges().size() != 0);
                action.setEnabled(enable);
            }
            if (dependsOnOneNodeOrEdge != null && dependsOnOneNodeOrEdge) {
                action.setEnabled(viewer.getSelectedEdges().size() == 1 || viewer.getSelectedNodes().size() == 1);
            }
            if (dependsOnXYLocked != null && dependsOnXYLocked) {
                action.setEnabled(viewer.trans.getLockXYScale());
            }
            if (dependsOnNodeOrEdge != null && dependsOnNodeOrEdge) {
                boolean enable = ((viewer.getSelectedEdges() != null && viewer.getSelectedEdges().size() != 0)
                        || (viewer.getSelectedNodes() != null && viewer.getSelectedNodes().size() != 0));
                action.setEnabled(enable);
            }
        }
    }

    /**
     * returns all actions
     *
     * @return actions
     */
    public List getAll() {
        return all;
    }

    // here we define the configurator window specific actions:

    private AbstractAction close;

    /**
     * close this viewer
     *
     * @return close action
     */
    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.removeViewer(formatter);
                formatter.getFrame().setVisible(false);
                formatter.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        // close is critical because we can't easily kill the worker thread

        all.add(action);
        return close = action;
    }

    private AbstractAction edgeWidth;

    public AbstractAction getEdgeWidth() {
        AbstractAction action = edgeWidth;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    if (selectedValue != null) {
                        int size = Integer.parseInt((String) selectedValue);
                        ICommand cmd = new EdgeWidthCommand(viewer, size);
                        new Edit(cmd, "width").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Edge Width");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set edge width");
        action.putValue(DEPENDS_ON_EDGESELECTION, Boolean.TRUE);
        all.add(action);
        return edgeWidth = action;
    }

    private AbstractAction font;

    public AbstractAction getFont() {
        AbstractAction action = font;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    boolean hasSelected = viewer.getSelectedNodes().size() > 0
                            || viewer.getSelectedEdges().size() > 0;

                    //ToDo: checkSelectALL
                    if (selectedValue != null && hasSelected) {
                        String family = selectedValue.toString();
                        CompoundCommand cmd = new CompoundCommand();
                        cmd.add(new EdgeFontCommand(viewer, family, -1, -1, -1));
                        cmd.add(new NodeFontCommand(viewer, family, -1, -1, -1));
                        new Edit(cmd, "font").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return font = action;
    }

    private AbstractAction fontSize;

    public Action getFontSize() {
        AbstractAction action = fontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore && (event.getActionCommand() == null || event.getActionCommand().equals("comboBoxChanged"))) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    boolean hasSelected = viewer.getSelectedNodes().size() > 0
                            || viewer.getSelectedEdges().size() > 0;

                    //ToDo: checkSelectALL
                    if (hasSelected && selectedValue != null) {
                        int size;
                        try {
                            size = Integer.parseInt((String) selectedValue);
                        } catch (NumberFormatException e) {
                            new Alert(viewer.getFrame(), "Font Size must be an integer! Size set to 10.");
                            size = 10;
                            ((JComboBox) event.getSource()).setSelectedItem("10");
                        }

                        CompoundCommand cmd = new CompoundCommand();
                        cmd.add(new EdgeFontCommand(viewer, null, -1, -1, size));
                        cmd.add(new NodeFontCommand(viewer, null, -1, -1, size));
                        new Edit(cmd, "font size").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font Size");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font size");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return fontSize = action;
    }

    private AbstractAction bold;

    public Action getNodeFontBold() {
        AbstractAction action = bold;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean hasSelected = viewer.getSelectedNodes().size() > 0
                            || viewer.getSelectedEdges().size() > 0;

                    if (hasSelected) {  //ToDo: checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        CompoundCommand cmd = new CompoundCommand();
                        cmd.add(new EdgeFontCommand(viewer, null, state, -1, -1));
                        cmd.add(new NodeFontCommand(viewer, null, state, -1, -1));
                        new Edit(cmd, "bold " + (state == 1 ? "on" : "off")).execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Bold");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font bold");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return bold = action;
    }

    private AbstractAction italic;

    public Action getNodeFontItalic() {
        AbstractAction action = italic;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean hasSelected = viewer.getSelectedNodes().size() > 0
                            || viewer.getSelectedEdges().size() > 0;

                    if (hasSelected) {  //ToDo: checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        CompoundCommand cmd = new CompoundCommand();
                        cmd.add(new EdgeFontCommand(viewer, null, -1, state, -1));
                        cmd.add(new NodeFontCommand(viewer, null, -1, state, -1));
                        new Edit(cmd, "italic " + (state == 1 ? "on" : "off")).execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Italic");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font italic");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return italic = action;
    }

    private AbstractAction nodeSize;

    public AbstractAction getNodeSize() {
        AbstractAction action = nodeSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    if (selectedValue != null) {
                        int size = Integer.parseInt((String) selectedValue);
                        ICommand cmd = new NodeSizeCommand(viewer, size, size);
                        new Edit(cmd, "size").execute(viewer.getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Node Size");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node size");
        action.putValue(DEPENDS_ON_NODESELECTION, Boolean.TRUE);
        all.add(action);
        return nodeSize = action;
    }

    private AbstractAction nodeShape;

    public Action getNodeShape() {

        AbstractAction action = nodeShape;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    if (selectedValue != null) {
                        int shape = -1;
                        if (selectedValue == "square") shape = 1;
                        if (selectedValue == "none") shape = 0;
                        if (selectedValue == "circle") shape = 2;
                        ICommand cmd = new NodeShapeCommand(viewer, shape);
                        new Edit(cmd, "shape").execute(viewer.getUndoSupportNetwork());
                    }
                }

            }
        };
        action.putValue(AbstractAction.NAME, "Node Shape");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node shape");
        action.putValue(DEPENDS_ON_NODESELECTION, Boolean.TRUE);
        all.add(action);
        return nodeShape = action;
    }

    private AbstractAction nodeNames;

    public AbstractAction getNodeNames(JCheckBox cbox) {
        AbstractAction action = nodeNames;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean names = ((nodeNames != null) && ((JCheckBox) nodeNames.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean ids = ((nodeIDs != null) && ((JCheckBox) nodeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    ICommand cmd = new NodeLabelsCommand(viewer, names, ids, viewer.getSelectedNodes().size() > 0);
                    new Edit(cmd, "node labels").execute(viewer.getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Names");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label nodes with taxon names");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return nodeNames = action;
    }

    private AbstractAction nodeIDs;

    public AbstractAction getNodeIDs(final JCheckBox cbox) {
        AbstractAction action = nodeIDs;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean names = ((nodeNames != null) && ((JCheckBox) nodeNames.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean ids = ((nodeIDs != null) && ((JCheckBox) nodeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    ICommand cmd = new NodeLabelsCommand(viewer, names, ids, viewer.getSelectedNodes().size() > 0);
                    new Edit(cmd, "node labels").execute(viewer.getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label nodes with IDs");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return nodeIDs = action;
    }

    private AbstractAction edgeIDs;

    public AbstractAction getEdgeIDs(final JCheckBox cbox) {
        AbstractAction action = edgeIDs;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean weights = ((edgeWeights != null) && ((JCheckBox) edgeWeights.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean ids = ((edgeIDs != null) && ((JCheckBox) edgeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean confidence = ((edgeConfidence != null) && ((JCheckBox) edgeConfidence.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean interval = ((edgeInterval != null) && ((JCheckBox) edgeInterval.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    final ICommand cmd = new EdgeLabelsCommand(viewer,
                            weights, ids, confidence, interval, viewer.getSelectedEdges().size() > 0);
                    new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edges with IDs");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeIDs = action;
    }

    private AbstractAction edgeWeights;

    public AbstractAction getEdgeWeights(final JCheckBox cbox) {
        AbstractAction action = edgeWeights;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean weights = ((edgeWeights != null) && ((JCheckBox) edgeWeights.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean ids = ((edgeIDs != null) && ((JCheckBox) edgeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean confidence = ((edgeConfidence != null) && ((JCheckBox) edgeConfidence.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    boolean interval = ((edgeInterval != null) && ((JCheckBox) edgeInterval.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                    final ICommand cmd = new EdgeLabelsCommand(viewer,
                            weights, ids, confidence, interval, viewer.getSelectedEdges().size() > 0);
                    new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Weights");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edges with weights");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeWeights = action;
    }

    private AbstractAction edgeConfidence;

    public AbstractAction getEdgeConfidence(final JCheckBox cbox) {
        AbstractAction action = edgeConfidence;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean weights = ((edgeWeights != null) && ((JCheckBox) edgeWeights.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean ids = ((edgeIDs != null) && ((JCheckBox) edgeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean confidence = ((edgeConfidence != null) && ((JCheckBox) edgeConfidence.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean interval = ((edgeInterval != null) && ((JCheckBox) edgeInterval.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                final ICommand cmd = new EdgeLabelsCommand(viewer,
                        weights, ids, confidence, interval, viewer.getSelectedEdges().size() > 0);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Confidence");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edges with confidence values");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeConfidence = action;
    }

    private AbstractAction edgeInterval;

    public AbstractAction getEdgeInterval(final JCheckBox cbox) {
        AbstractAction action = edgeInterval;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean weights = ((edgeWeights != null) && ((JCheckBox) edgeWeights.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean ids = ((edgeIDs != null) && ((JCheckBox) edgeIDs.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean confidence = ((edgeConfidence != null) && ((JCheckBox) edgeConfidence.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                boolean interval = ((edgeInterval != null) && ((JCheckBox) edgeInterval.getValue(MenuManager.CHECKBOXMENUITEM)).isSelected());
                final ICommand cmd = new EdgeLabelsCommand(viewer,
                        weights, ids, confidence, interval, viewer.getSelectedEdges().size() > 0);
                new Edit(cmd, "edge labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Intervals");
        action.putValue(MenuManager.CHECKBOXMENUITEM, cbox);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label edges with confidence intervals");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeInterval = action;
    }


    private AbstractAction edgeShape;

    public Action getEdgeShape() {

        AbstractAction action = edgeShape;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    if (selectedValue != null) {
                        byte shape = -1;
                        if (selectedValue == "angular") shape = EdgeView.ARC_LINE_EDGE;
                        if (selectedValue == "straight") shape = EdgeView.STRAIGHT_EDGE;
                        if (selectedValue == "curved") shape = EdgeView.QUAD_EDGE;
                        ICommand cmd = new EdgeShapeCommand(viewer, shape);
                        new Edit(cmd, "shape").execute(viewer.getUndoSupportNetwork());
                    }
                    viewer.repaint();
                    dir.setDirty(true);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Edge Shape");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set edge shape");
        action.putValue(DEPENDS_ON_EDGESELECTION, Boolean.TRUE);
        all.add(action);
        return edgeShape = action;
    }

    private AbstractAction rotateLabelsLeft = getRotateLabelsLeft();

    public AbstractAction getRotateLabelsLeft() {
        AbstractAction action = rotateLabelsLeft;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                float delta = -(float) (Math.PI / 50.0);
                ICommand cmd = new NodeLabelRotateCommand(viewer, delta);
                new Edit(cmd, "labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Left");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Rotate labels left");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("RotateLeft16.gif"));
        action.putValue(DEPENDS_ON_NODESELECTION, Boolean.TRUE);
        all.add(action);
        return rotateLabelsLeft = action;
    }

    private AbstractAction rotateLabelsRight = getRotateLabelsRight();

    public AbstractAction getRotateLabelsRight() {
        AbstractAction action = rotateLabelsRight;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                float delta = (float) (Math.PI / 50.0);
                ICommand cmd = new NodeLabelRotateCommand(viewer, delta);
                new Edit(cmd, "labels").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Right");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Rotate labels right");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("RotateRight16.gif"));
        action.putValue(DEPENDS_ON_NODESELECTION, Boolean.TRUE);
        all.add(action);
        return rotateLabelsRight = action;
    }

    /**
     * get ignore firing of events
     *
     * @return true, if we are currently ignoring firing of events
     */
    public boolean getIgnore() {
        return ignore;
    }

    /**
     * set ignore firing of events
     *
     * @param ignore
     */
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }


    private AbstractAction cut = getCut();

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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cut");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Cut16.gif"));
        action.putValue(CRITICAL, Boolean.TRUE);
        all.add(action);
        return cut = action;
    }

    private AbstractAction copy = getCopy();

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
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() /*| ActionEvent.SHIFT_MASK*/));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Copy graph to clipboard");
        action.putValue(CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Copy16.gif"));
        all.add(action);
        return copy = action;
    }


    private AbstractAction paste = getPaste();

    public AbstractAction getPaste() {
        AbstractAction action = paste;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                // TransferableGraphic tg = new TransferableGraphic(viewer);
                // Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tg, tg);
            }
        };
        action.putValue(AbstractAction.NAME, "Paste");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Paste");
        action.putValue(CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Paste16.gif"));
        all.add(action);
        return paste = action;
    }

    private AbstractAction saveDefaultFont = getSaveDefaultFont();

    public AbstractAction getSaveDefaultFont() {
        AbstractAction action = saveDefaultFont;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                formatter.saveFontAsDefault();
            }
        };
        action.putValue(AbstractAction.NAME, "Set Font as Default");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set current font as default");
        action.putValue(CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
        all.add(action);
        return saveDefaultFont = action;
    }

    private AbstractAction foregroundColorAction;

    public AbstractAction getForegroundColorAction(final JCheckBox cbox) {
        AbstractAction action = foregroundColorAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Color");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        action.putValue(CHECKBOXITEM, cbox);
        all.add(action);
        return foregroundColorAction = action;
    }

    private AbstractAction backgroundColorAction;

    public AbstractAction getBackgroundColorAction(final JCheckBox cbox) {
        AbstractAction action = backgroundColorAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Back Color");
        action.putValue(CHECKBOXITEM, cbox);
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        action.putValue(CHECKBOXITEM, cbox);
        all.add(action);
        return backgroundColorAction = action;
    }

    private AbstractAction labelForegroundColorAction;

    public AbstractAction getLabelForegroundColorAction(final JCheckBox cbox) {
        AbstractAction action = labelForegroundColorAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label Color");
        action.putValue(CHECKBOXITEM, cbox);
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        action.putValue(CHECKBOXITEM, cbox);
        all.add(action);
        return labelForegroundColorAction = action;
    }

    private AbstractAction labelBackgroundColorAction;

    public AbstractAction getLabelBackgroundColorAction(final JCheckBox cbox) {
        AbstractAction action = labelBackgroundColorAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label Back Color");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        action.putValue(CHECKBOXITEM, cbox);
        all.add(action);
        return labelBackgroundColorAction = action;
    }

    private AbstractAction randomColorActionAction;

    public AbstractAction getRandomColorActionAction() {
        AbstractAction action = randomColorActionAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                CompoundCommand cmd = new CompoundCommand();
                boolean fg = ((JCheckBox) foregroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean bg = ((JCheckBox) backgroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean label = ((JCheckBox) labelForegroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean lbg = ((JCheckBox) labelBackgroundColorAction.getValue(CHECKBOXITEM)).isSelected();

                cmd.add(new EdgeColorCommand(viewer, null, true, fg, label, lbg));
                cmd.add(new NodeColorCommand(viewer, null, true, fg, bg, label, lbg));
                new Edit(cmd, "random color").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Random Colors");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Randomly color nodes, edges and labels");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return randomColorActionAction = action;
    }

    private AbstractAction noColorActionAction;

    public AbstractAction getNoColorActionAction() {
        AbstractAction action = noColorActionAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                CompoundCommand cmd = new CompoundCommand();
                boolean fg = ((JCheckBox) foregroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean bg = ((JCheckBox) backgroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean label = ((JCheckBox) labelForegroundColorAction.getValue(CHECKBOXITEM)).isSelected();
                boolean lbg = ((JCheckBox) labelBackgroundColorAction.getValue(CHECKBOXITEM)).isSelected();

                cmd.add(new EdgeColorCommand(viewer, null, false, fg, label, lbg));
                cmd.add(new NodeColorCommand(viewer, null, false, fg, bg, label, lbg));
                new Edit(cmd, "no color").execute(viewer.getUndoSupportNetwork());
            }
        };
        action.putValue(AbstractAction.NAME, "Invisible");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set color to invisible");
        action.putValue(DEPENDS_ON_NODE_OR_EDGE, Boolean.TRUE);
        all.add(action);
        return noColorActionAction = action;
    }
}
