/*
 * PreferencesActions.java Copyright (C) 2022 Daniel H. Huson
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
/*
 * Created on 10.08.2004
 *
 */
package splitstree4.gui.preferences;

import jloda.util.ProgramProperties;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.main.MainViewerToolBar;
import splitstree4.gui.main.StatusBar;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;
import splitstree4.util.SplitsUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Markus Franz
 */
public class PreferencesActions {

	private final PreferencesWindow viewer;
	private final Director dir;
	private final List<Action> all = new LinkedList<>();
	public final String JCHECKBOX = "JCHECKBOX";
	public final String JTEXTAREA = "JTEXTAREA";

	public PreferencesActions(PreferencesWindow pref, Director dir) {
		this.viewer = pref;
		this.dir = dir;
	}

	/**
	 * enable or disable critical actions
     *
     * @param flag show or hide?
     */
    public void setEnableCritical(boolean flag) {
        DirectorActions.setEnableCritical(all, flag);
// because we don't want to duplicate that code
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);
        // because we don't want to duplicate that code
    }

    /**
     * returns all actions
     *
     * @return actions
     */
    public List getAll() {
        return all;
    }

    private AbstractAction close;

    /**
     * close this viewer
     *
     * @return close action
     */
    public AbstractAction getClose
    () {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                close();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        //action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));

        // close is critical because we can't easily kill the worker thread

        all.add(action);
        return close = action;
    }

    private void close() {
        dir.removeViewer(viewer);
        viewer.getFrame().setVisible(false);
        viewer.getFrame().dispose();
    }

    //Graph tab actions

    private AbstractAction allowEdit;

    public AbstractAction getAllowEdit() {
        AbstractAction action = allowEdit;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Allow Graph Editing");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('E'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Allow full editing of graph");
        all.add(action);
        return allowEdit = action;
    }


    private AbstractAction maintainEdgeLengths;

    public AbstractAction getMaintainEdgeLengths() {
        AbstractAction action = maintainEdgeLengths;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Lock Edge Lengths");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('K'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Lock edge lengths so that they cannot be changed interactively");
        all.add(action);
        return maintainEdgeLengths = action;
    }

    private AbstractAction drawScaleBar;

    public AbstractAction getDrawScaleBar() {
        AbstractAction action = drawScaleBar;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Show Scale Bar");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('B'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Display the scale bar");
        all.add(action);
        return drawScaleBar = action;
    }

    private AbstractAction selectSplits;

    public AbstractAction getSelectSplits() {
        AbstractAction action = selectSplits;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Use Split-Selection Mode");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('M'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "In split-selection mode, clicking an edge selects all edges of the same split");
        all.add(action);
        return selectSplits = action;
    }

    private AbstractAction defaultGraph;

    public AbstractAction getDefaultGraph(final JCheckBox editCB, final JCheckBox edgeCB, final JCheckBox scaleCB,
                                          final JCheckBox splitsCB) {
        if (defaultGraph != null)
            return defaultGraph;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                boolean scaleBar = scaleCB.isSelected();
                ProgramProperties.put("scaleBar", scaleBar);
                boolean allowEdit = editCB.isSelected();
                ProgramProperties.put("allowEdit", allowEdit);
                boolean maintainEdgeLengths = edgeCB.isSelected();
                ProgramProperties.put("mainEdgeLengths", maintainEdgeLengths);
                boolean useSplitSelectionMode = splitsCB.isSelected();
                ProgramProperties.put("useSplitSelectionMode", useSplitSelectionMode);
                ProgramProperties.store();
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set as default");
        action.putValue(AbstractAction.NAME, "Set As Default");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return defaultGraph = action;
    }


    private AbstractAction applyGraph;

    public AbstractAction getApplyGraph(final JCheckBox editCB, final JCheckBox edgeCB, final JCheckBox scaleCB,
                                        final JCheckBox splitsCB) {
        if (applyGraph != null)
            return applyGraph;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyGraph(editCB, edgeCB, scaleCB, splitsCB);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply");
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyGraph = action;
    }

    private void applyGraph(final JCheckBox editCB, final JCheckBox edgeCB, final JCheckBox scaleCB,
                            final JCheckBox splitsCB) {
        viewer.getMainViewer().setAllowEdit(editCB.isSelected());
        viewer.getMainViewer().setMaintainEdgeLengths(edgeCB.isSelected());
        viewer.getMainViewer().setDrawScaleBar(scaleCB.isSelected());
        viewer.getMainViewer().setUseSplitSelectionModel(splitsCB.isSelected());
        viewer.getMainViewer().checkEditableGraph();
        viewer.getMainViewer().repaint();
    }

    private AbstractAction oKGraph;

    public AbstractAction getOKGraph(final JCheckBox editCB, final JCheckBox edgeCB, final JCheckBox scaleCB,
                                     final JCheckBox splitsCB) {
        if (oKGraph != null)
            return oKGraph;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyGraph(editCB, edgeCB, scaleCB, splitsCB);
                close();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply changes and close window");
        action.putValue(AbstractAction.NAME, "OK");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return oKGraph = action;
    }

    //Status Line Tab Actions

    private AbstractAction fit;

    /**
     * writeInfoFile fit
     *
     * @return action
     */
    AbstractAction getStatusBarFit() {
        if (fit != null)
            return fit;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Fit value in Status Line");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return fit = action;
    }

    private AbstractAction lsFit;

    /**
     * least square fit
     *
     * @return action
     */
    AbstractAction getStatusBarLSFit() {
        if (lsFit != null)
            return lsFit;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the LSFit value in Status Line");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return lsFit = action;
    }

    private AbstractAction taxa;

    /**
     * taxa
     *
     * @return action
     */
    AbstractAction getStatusBarTaxa() {
        if (taxa != null)
            return taxa;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Taxa value in Status Line");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        all.add(action);
        return taxa = action;
    }

    private AbstractAction chars;

    AbstractAction getStatusBarChars() {
        if (chars != null)
            return chars;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Chars value in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return chars = action;
    }

    private AbstractAction trees;

    AbstractAction getStatusBarTrees() {
        if (trees != null)
            return trees;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Trees value in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        all.add(action);
        return trees = action;
    }

    private AbstractAction splits;

    AbstractAction getStatusBarSplits() {
        if (splits != null)
            return splits;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Splits value in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return splits = action;
    }

    private AbstractAction assumptions;

    AbstractAction getStatusBarAssumptions() {
        if (assumptions != null)
            return assumptions;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the Assumption in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Assumptions.NAME);
        all.add(action);
        return assumptions = action;
    }

    private AbstractAction vertices;

    AbstractAction getStatusBarVertices() {
        if (vertices != null)
            return vertices;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the number of vertices in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return vertices = action;
    }

    private AbstractAction edges;

    AbstractAction getStatusBarEdges() {
        if (edges != null)
            return edges;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Write the number of edges in Status Line");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edges = action;
    }

    private AbstractAction defaultStatus;

    public AbstractAction getDefaultStatus(final JCheckBox fitCB, final JCheckBox lsFitCB, final JCheckBox taxaCB,
                                           final JCheckBox charsCB, final JCheckBox treesCB,
                                           final JCheckBox splitsCB, final JCheckBox assumptionsCB,
                                           final JCheckBox verticesCB, final JCheckBox edgesCB) {
        if (defaultStatus != null)
            return defaultStatus;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String stats = "";
                if (fitCB.isSelected()) stats += 'F';
                if (lsFitCB.isSelected()) stats += 'L';
                if (taxaCB.isSelected()) stats += 'T';
                if (charsCB.isSelected()) stats += 'C';
                if (treesCB.isSelected()) stats += 't';
                if (splitsCB.isSelected()) stats += 'S';
                if (assumptionsCB.isSelected()) stats += 'A';
                if (verticesCB.isSelected()) stats += 'V';
                if (edgesCB.isSelected()) stats += 'E';
                ProgramProperties.put("statusBar", stats);
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set as default");
        action.putValue(AbstractAction.NAME, "Set As Default");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return defaultStatus = action;
    }


    private AbstractAction applyStatus;

    public AbstractAction getApplyStatus(final JCheckBox fitCB, final JCheckBox lsFitCB, final JCheckBox taxaCB,
                                         final JCheckBox charsCB, final JCheckBox treesCB,
                                         final JCheckBox splitsCB, final JCheckBox assumptionsCB,
                                         final JCheckBox verticesCB, final JCheckBox edgesCB) {
        if (applyStatus != null)
            return applyStatus;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyStatus(fitCB, lsFitCB, taxaCB, charsCB, treesCB, splitsCB, assumptionsCB, verticesCB, edgesCB);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply");
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyStatus = action;
    }

    private void applyStatus(final JCheckBox fitCB, final JCheckBox lsFitCB, final JCheckBox taxaCB,
                             final JCheckBox charsCB, final JCheckBox treesCB,
                             final JCheckBox splitsCB, final JCheckBox assumptionsCB,
                             final JCheckBox verticesCB, final JCheckBox edgesCB) {
        StatusBar sbar = viewer.statusBar;
        sbar.setFit(fitCB.isSelected());
        sbar.setLsFit(lsFitCB.isSelected());
        sbar.setTaxa(taxaCB.isSelected());
        sbar.setChars(charsCB.isSelected());
        sbar.setTrees(treesCB.isSelected());
        sbar.setSplits(splitsCB.isSelected());
        sbar.setAssumptions(assumptionsCB.isSelected());
        sbar.setVertices(verticesCB.isSelected());
        sbar.setEdges(edgesCB.isSelected());
        sbar.setStatusLine(dir.getDocument());
    }

    private AbstractAction oKStatus;

    public AbstractAction getOKStatus(final JCheckBox fitCB, final JCheckBox lsFitCB, final JCheckBox taxaCB,
                                      final JCheckBox charsCB, final JCheckBox treesCB,
                                      final JCheckBox splitsCB, final JCheckBox assumptionsCB,
                                      final JCheckBox verticesCB, final JCheckBox edgesCB) {
        if (oKStatus != null)
            return oKStatus;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyStatus(fitCB, lsFitCB, taxaCB, charsCB, treesCB, splitsCB, assumptionsCB, verticesCB, edgesCB);
                close();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply changes and close window");
        action.putValue(AbstractAction.NAME, "OK");
        action.putValue(DirectorActions.DEPENDS_ON, Taxa.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return oKStatus = action;
    }

    //Layout Tab Actions


    private AbstractAction cycle;

    AbstractAction getCycle() {
        if (cycle != null)
            return cycle;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Circular ordering of taxa used for layout");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return cycle = action;
    }

    private AbstractAction recompute;

    /**
     * always recompute cycle
     *
     * @return action
     */
    AbstractAction getRecompute() {
        if (recompute != null)
            return recompute;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Recompute");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Always recompute layout");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return recompute = action;
    }

    private AbstractAction stablize;

    /**
     * stabilize the layout
     *
     * @return action
     */
    AbstractAction getStabilize() {
        if (stablize != null)
            return stablize;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Stabilize");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Compute layout that suits both current and new splits");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return stablize = action;
    }

    private AbstractAction snowball;

    /**
     * compute snowball layout
     *
     * @return action
     */
    AbstractAction getSnowball() {
        if (snowball != null)
            return snowball;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Snowball");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Compute best layout for all graphs seen so far");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return snowball = action;
    }

    private AbstractAction keep;

    /**
     * use the entered  layout
     *
     * @return action
     */
    AbstractAction getKeep() {
        if (keep != null)
            return keep;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Use the following layout:");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Use the current or entered layout");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return keep = action;
    }


    private AbstractAction applyLayout;

    /**
     * apply
     *
     * @return action
     */
    AbstractAction getApplyLayout(final JRadioButton recompute, final JRadioButton stabilize,
                                  final JRadioButton snowball, final JRadioButton keep, final JTextArea cycleText) {
        if (applyLayout != null)
            return applyLayout;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyLayout(recompute, stabilize, snowball, keep, cycleText);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply the selected graph layout strategy");
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyLayout = action;
    }

    private void applyLayout(final JRadioButton recompute, final JRadioButton stabilize,
                             final JRadioButton snowball, final JRadioButton keep, final JTextArea cycleText) {
        if (recompute.isSelected()) {
            dir.getDocument().getAssumptions().setLayoutStrategy(Assumptions.RECOMPUTE);
            dir.execute("update cycle");
        } else if (stabilize.isSelected()) {
            dir.getDocument().getAssumptions().setLayoutStrategy(Assumptions.STABILIZE);
            dir.execute("update cycle");
        } else if (snowball.isSelected()) {
            dir.getDocument().getAssumptions().setLayoutStrategy(Assumptions.SNOWBALL);
            dir.execute("update cycle");
        } else if (keep.isSelected()) {
            if (SplitsUtilities.equalCycles
                    (dir.getDocument().getSplits().getCycle(), cycleText.getText())) {
                dir.getDocument().getAssumptions().setLayoutStrategy(Assumptions.KEEP);
                SplitsUtilities.setPreviousTaxaSplits(dir.getDocument().getTaxa(), dir.getDocument().getSplits());
            } else
                dir.execute("cycle keep " + cycleText.getText());
        }
    }

    private AbstractAction oKLayout;

    /**
     * apply
     *
     * @return action
     */
    AbstractAction getOKLayout(final JRadioButton recompute, final JRadioButton stabilize,
                               final JRadioButton snowball, final JRadioButton keep, final JTextArea cycleText) {
        if (oKLayout != null)
            return oKLayout;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                applyLayout(recompute, stabilize, snowball, keep, cycleText);
                close();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply changes and close window");
        action.putValue(AbstractAction.NAME, "OK");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return oKLayout = action;
    }

    //General tab actions

    private AbstractAction edgeIDs;

    public AbstractAction getEdgeIDs() {
        AbstractAction action = edgeIDs;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                // TODO no clue, how to do this, yet
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show IDs");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeIDs = action;
    }

    private AbstractAction edgeWeights;

    public AbstractAction getEdgeWeights() {
        AbstractAction action = edgeWeights;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
//            	 TODO no clue, how to do this, yet
            }
        };
        action.putValue(AbstractAction.NAME, "Weights");
        //action.putValue(AbstractAction.MNEMONIC_KEY, (int)('W'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show weights");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeWeights = action;
    }

    private AbstractAction edgeConfidence;

    public AbstractAction getEdgeConfidence() {
        AbstractAction action = edgeConfidence;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
//            	 TODO no clue, how to do this, yet
            }
        };
        action.putValue(AbstractAction.NAME, "Confidence Values");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('V'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show confidence");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeConfidence = action;
    }

    private AbstractAction edgeFont;

    public AbstractAction getEdgeFont() {
        AbstractAction action = edgeFont;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                Font propFont = (ProgramProperties.get("eFont", new Font("Dialog", Font.PLAIN, 10)));
                ProgramProperties.put("eFont", "" + cbox.getSelectedItem(), propFont.getStyle(), propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");

        all.add(action);
        return edgeFont = action;
    }

    private AbstractAction edgeFontSize;

    public Action getEdgeFontSize() {
        AbstractAction action = edgeFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                Font propFont = (ProgramProperties.get("eFont", new Font("Dialog", Font.PLAIN, 10)));
                ProgramProperties.put("eFont", propFont.getName(), propFont.getStyle(), Integer.valueOf(cbox.getSelectedItem().toString()));
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");

        all.add(action);
        return edgeFontSize = action;
    }


    private AbstractAction edgeBold;

    public Action getEdgeFontBold() {
        AbstractAction action = edgeBold;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JCheckBox cbox = (JCheckBox) event.getSource();
                Font propFont = (ProgramProperties.get("eFont", new Font("Dialog", Font.PLAIN, 10)));
                int sty = propFont.getStyle();
                if (cbox.isSelected())
                    sty += Font.BOLD;
                else
                    sty -= Font.BOLD;
                ProgramProperties.put("eFont", propFont.getName(), sty, propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };

        action.putValue(AbstractAction.NAME, "Bold");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font bold");

        all.add(action);
        return edgeBold = action;
    }

    private AbstractAction edgeItalic;

    public Action getEdgeFontItalic() {
        AbstractAction action = edgeItalic;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JCheckBox cbox = (JCheckBox) event.getSource();
                Font propFont = (ProgramProperties.get("eFont", new Font("Dialog", Font.PLAIN, 10)));
                int sty = propFont.getStyle();
                if (cbox.isSelected())
                    sty += Font.ITALIC;
                else
                    sty -= Font.ITALIC;
                ProgramProperties.put("eFont", propFont.getName(), sty, propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };

        action.putValue(AbstractAction.NAME, "Italic");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font italic");

        all.add(action);
        return edgeItalic = action;
    }

    private AbstractAction edgeWidth;

    public AbstractAction getEdgeWidth() {
        AbstractAction action = edgeWidth;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                ProgramProperties.put("eWidth", Integer.parseInt(cbox.getSelectedItem().toString()));
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Width...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set edge width");

        all.add(action);
        return edgeWidth = action;
    }

    private AbstractAction nodeLabels;

    public AbstractAction getNodeLabels() {
        AbstractAction action = nodeLabels;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                // TODO no clue, how to do this, yet...
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        //action.putValue(AbstractAction.MNEMONIC_KEY, (int)('I'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label with taxon ids");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return nodeLabels = action;
    }

    private AbstractAction nodeFont;

    public AbstractAction getNodeFont() {
        AbstractAction action = nodeFont;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                Font propFont = (ProgramProperties.get("nFont", new Font("Dialog", Font.PLAIN, 10)));
                ProgramProperties.put("nFont", "" + cbox.getSelectedItem(), propFont.getStyle(), propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");

        all.add(action);
        return nodeFont = action;
    }

    private AbstractAction nodeFontSize;

    public Action getNodeFontSize() {
        AbstractAction action = nodeFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                Font propFont = (ProgramProperties.get("nFont", new Font("Dialog", Font.PLAIN, 10)));
                ProgramProperties.put("nFont", propFont.getName(), propFont.getStyle(), Integer.valueOf(cbox.getSelectedItem().toString()));
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Font size");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font size");

        all.add(action);
        return nodeFontSize = action;
    }

    private AbstractAction nodeBold;

    public Action getNodeFontBold() {
        AbstractAction action = nodeBold;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JCheckBox cbox = (JCheckBox) event.getSource();
                Font propFont = (ProgramProperties.get("nFont", new Font("Dialog", Font.PLAIN, 10)));
                int sty = propFont.getStyle();
                if (cbox.isSelected())
                    sty += Font.BOLD;
                else
                    sty -= Font.BOLD;
                ProgramProperties.put("nFont", propFont.getName(), sty, propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };

        action.putValue(AbstractAction.NAME, "Bold");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font bold");

        all.add(action);
        return nodeBold = action;
    }

    private AbstractAction nodeItalic;

    public Action getNodeFontItalic() {
        AbstractAction action = nodeItalic;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JCheckBox cbox = (JCheckBox) event.getSource();
                Font propFont = (ProgramProperties.get("nFont", new Font("Dialog", Font.PLAIN, 10)));
                int sty = propFont.getStyle();
                if (cbox.isSelected())
                    sty += Font.ITALIC;
                else
                    sty -= Font.ITALIC;
                ProgramProperties.put("nFont", propFont.getName(), sty, propFont.getSize());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };

        action.putValue(AbstractAction.NAME, "Italic");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font italic");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);

        all.add(action);
        return nodeItalic = action;
    }

    private AbstractAction nodeSize;

    public AbstractAction getNodeSize() {
        AbstractAction action = nodeSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                ProgramProperties.put("nSize", Integer.parseInt(cbox.getSelectedItem().toString()));
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Size...");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('S'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node size");

        all.add(action);
        return nodeSize = action;
    }

    private AbstractAction nodeShape;

    public Action getNodeShape() {

        AbstractAction action = nodeShape;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cbox = (JComboBox) event.getSource();
                ProgramProperties.put("nShape", cbox.getSelectedIndex());
                SplitsTreeProperties.applyProperties(viewer.getMainViewer());
            }
        };
        action.putValue(AbstractAction.NAME, "Shape...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node shape");

        all.add(action);
        return nodeShape = action;
    }

    private AbstractAction showAction;

    public Action getShowAction() {
        if (showAction != null)
            return showAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] indices = viewer.getJlistl().getSelectedIndices();
                for (int indice : indices) {
                    viewer.getListr().addElement(viewer.getListl().getElementAt(indice));
                }
                for (int i = indices.length - 1; i >= 0; i--) {
                    viewer.getListl().removeElementAt(indices[i]);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Show >");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show selected action buttons");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showAction = action;
    }

    private AbstractAction hideAction;

    public AbstractAction getHideAction() {
        if (hideAction != null)
            return hideAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] indices = viewer.getJlistr().getSelectedIndices();
                for (int indice : indices) {
                    viewer.getListl().addElement(viewer.getListr().getElementAt(indice));
                }
                for (int i = indices.length - 1; i >= 0; i--) {
                    viewer.getListr().removeElementAt(indices[i]);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "< Hide");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide selected action buttons");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return hideAction = action;
    }

    private AbstractAction moveUpAction;

    public AbstractAction getMoveUpAction() {
        if (moveUpAction != null)
            return moveUpAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                List<Integer> nextSelected = new ArrayList<>();
                BitSet skip = new BitSet();

                DefaultListModel listr = viewer.getListr();
                for (int i = 0; i < listr.size() - 1; i++) // -1 because we need to be able to swap
                    if (!viewer.getJlistr().isSelectedIndex(i)
                            && viewer.getJlistr().isSelectedIndex(i + 1)) {
                        Object a = listr.elementAt(i);
                        Object b = listr.getElementAt(i + 1);
                        nextSelected.add(i);
                        skip.set(i + 1);
                        listr.set(i, b);
                        listr.set(i + 1, a);
                    } else if (viewer.getJlistr().isSelectedIndex(i) && !skip.get(i))
                        nextSelected.add(i);

                if (nextSelected.size() > 0)  // adjust selection
                {
                    int[] indices = new int[nextSelected.size()];
                    for (int i = 0; i < indices.length; i++)
						indices[i] = nextSelected.get(i);
                    viewer.getJlistr().clearSelection();
                    viewer.getJlistr().setSelectedIndices(indices);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Move Up");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Move selected action buttons up in list");
        // action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return moveUpAction = action;
    }

    private AbstractAction applyToolbar;

    public AbstractAction getApplyToolbar() {
        if (applyToolbar != null)
            return applyToolbar;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MainViewerToolBar tb = viewer.getMainViewer().getMainToolBar();
                tb.removeAll();
                ProgramProperties.put(SplitsTreeProperties.TOOLBARITEMS, "");
                List<Action> elements = new LinkedList<>();
                for (int i = 0; i < viewer.getJlistr().getModel().getSize(); i++) {
                    AbstractAction element = (AbstractAction) viewer.getJlistr().getModel().getElementAt(i);
                    elements.add(element);
                }
                SplitsTreeProperties.setToolBar(elements, viewer.getShowToolbar().isSelected());
            }

        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Build selected toolbar");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyToolbar = action;
    }

    private AbstractAction showToolbar;

    public AbstractAction getShowToolbar() {
        if (showToolbar != null)
            return showToolbar;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                /*JCheckBox box=(JCheckBox)e.getSource();
viewer.getMainViewer().getMainToolBar().setVisible(box.isSelected());*/
            }

        };
        action.putValue(AbstractAction.NAME, "Show");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show Toolbar");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showToolbar = action;
    }
}
