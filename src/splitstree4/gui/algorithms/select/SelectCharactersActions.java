/**
 * SelectCharactersActions.java
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
package splitstree4.gui.algorithms.select;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.UpdateableActions;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.main.MainViewerActions;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.util.ComputeSupportingCharacters;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a characters window
 *
 * @author huson
 * Date: 19-Dec-2003
 */
public class SelectCharactersActions implements UpdateableActions {
    private Director dir;
    private List<Action> all = new LinkedList<>();
    public static final String JCHECKBOX = "JCHECKBOX";
    public static final String JTEXTAREA = "JTEXTAREA";

    public SelectCharactersActions(Director dir) {
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

        for (Action action : all) {
            if (action.isEnabled() && action.getValue(MainViewerActions.DEPENDS_ON_EDGESELECTION) != null) {
                action.setEnabled(((MainViewer) dir.getViewerByClass((MainViewer.class))).getSelectedEdges().size() > 0);
            }
            if (action.isEnabled() && action.getValue(MainViewerActions.DEPENDS_ON_NODESELECTION) != null) {
                action.setEnabled(((MainViewer) dir.getViewerByClass((MainViewer.class))).getSelectedNodes().size() > 0);
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

    // here we define the algorithms window specific actions:

    AbstractAction input;

    AbstractAction getInput() {
        if (input != null)
            return input;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Positions to show");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return input = action;
    }


    AbstractAction clear;

    /**
     * use all positions
     *
     * @return action
     */
    AbstractAction getClear() {
        if (clear != null)
            return clear;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));
                inputTA.setText("");
                getApply().actionPerformed(null);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear characters from selected taxon labels");
        action.putValue(AbstractAction.NAME, "Clear");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return clear = action;
    }

    AbstractAction apply;

    /**
     * apply
     *
     * @return action
     */
    AbstractAction getApply() {
        if (apply != null)
            return apply;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                MainViewer viewer = (MainViewer) dir.getViewerByClass(MainViewer.class);

                JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));
                Document doc = dir.getDocument();

                List toShow;
                try {
                    toShow = parseText(doc, inputTA.getText().replaceAll(",", " "));
                } catch (IOException e) {
                    new Alert(viewer, "Syntax error in input");
                    return;
                }

                Taxa taxa = doc.getTaxa();
                Characters chars = doc.getCharacters();
                PhyloSplitsGraph graph = viewer.getPhyloGraph();


                for (int t = 1; t <= taxa.getNtax(); t++) {
                    try {
                        Node v = graph.getTaxon2Node(t);
                        String oldLabel = graph.getLabel(v);
                        String label = chars.getRowSubset(t, toShow);
                        int pos = -1;
                        if (oldLabel != null)
                            pos = oldLabel.indexOf("::");
                        String newLabel;
                        if (pos != -1)
                            newLabel = oldLabel.substring(0, pos);
                        else
                            newLabel = oldLabel;
                        if (label.length() > 0)
                            newLabel += "::" + label;
                        viewer.setLabel(v, newLabel);
                    } catch (NotOwnerException ex) {
                        Basic.caught(ex);
                    }
                }
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the selected character positions on all taxa");
        action.putValue(AbstractAction.NAME, "Show on All Taxa");
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return apply = action;
    }

    AbstractAction applyToSelectedNodes;

    /**
     * apply   to selected nodes
     *
     * @return action
     */
    AbstractAction getApplyToSelectedNodes() {
        if (applyToSelectedNodes != null)
            return applyToSelectedNodes;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                MainViewer viewer = (MainViewer) dir.getViewerByClass(MainViewer.class);

                JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));
                Document doc = dir.getDocument();

                List toShow;
                try {
                    toShow = parseText(doc, inputTA.getText().replaceAll(",", " "));
                } catch (IOException e) {
                    new Alert(viewer, "Syntax error in input");
                    return;
                }

                Taxa taxa = doc.getTaxa();
                Characters chars = doc.getCharacters();
                PhyloSplitsGraph graph = viewer.getPhyloGraph();

                for (int t = 1; t <= taxa.getNtax(); t++) {
                    try {
                        Node v = graph.getTaxon2Node(t);
                        if (!viewer.getSelected(v))
                            continue; // only apply to selected nodes
                        String oldLabel = graph.getLabel(v);
                        String label = chars.getRowSubset(t, toShow);
                        int pos = -1;
                        if (oldLabel != null)
                            pos = oldLabel.indexOf("::");
                        String newLabel;
                        if (pos != -1)
                            newLabel = oldLabel.substring(0, pos);
                        else
                            newLabel = oldLabel;
                        if (label.length() > 0)
                            newLabel += "::" + label;
                        viewer.setLabel(v, newLabel);
                    } catch (NotOwnerException ex) {
                        Basic.caught(ex);
                    }
                }
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show the selected character positions on all selected taxa");
        action.putValue(AbstractAction.NAME, "Show on Selected Taxa");
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        //action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION ,Boolean.TRUE);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyToSelectedNodes = action;
    }

    AbstractAction selectAllSupportingCharacters;

    /**
     * apply   to selected nodes
     *
     * @return action
     */
    AbstractAction getSelectAllSupportingCharacters() {
        if (selectAllSupportingCharacters != null)
            return selectAllSupportingCharacters;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                final JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));

                final MainViewer viewer = (MainViewer) dir.getViewerByClass(MainViewer.class);
                final Document doc = dir.getDocument();
                BitSet selectedSplits = new BitSet();
                if (viewer.getSelectedEdges().size() == 0) {
                    new Alert(viewer.getFrame(), "Please select a split in the network or tree");
                    return;
                }
                for (Edge e : viewer.getSelectedEdges()) {
                    selectedSplits.set(viewer.getPhyloGraph().getSplit(e));
                }

                if (!doc.getListOfValidBlocks().contains(Characters.NAME)) {
                    new Alert(viewer.getFrame(), "Please provide a characters block");
                    return;
                }
                BitSet supportingCharacters = ComputeSupportingCharacters.apply(doc, selectedSplits);
				inputTA.setText(StringUtils.toString(supportingCharacters));

            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select all character positions that support the currently selected split");
        action.putValue(AbstractAction.NAME, "Select Supporting Characters");
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        // action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return selectAllSupportingCharacters = action;
    }

    /**
     * parses the list of positions to show
     *
     * @param doc
     * @param text
     * @return
     * @throws java.io.IOException
     */
    private List parseText(Document doc, String text) throws IOException {
        NexusStreamParser np = new NexusStreamParser(new StringReader("=" + text + ";"));
        return np.getIntegerList("=", ";");

    }
}
