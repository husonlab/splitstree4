/**
 * ConfidenceWindowActions.java
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
package splitstree4.gui.confidence;

import jloda.swing.util.ResourceManager;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.undo.EdgeConfidenceHighlight;
import splitstree4.gui.undo.Edit;
import splitstree4.gui.undo.ICommand;
import splitstree4.nexus.Network;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a confidence window
 *
 * @author huson
 * Date: 17.2.2004
 */
public class ConfidenceWindowActions {
    private ConfidenceWindow viewer;
    private Director dir;
    private List all = new LinkedList();
    public static final String JCHECKBOX = "JCHECKBOX";
    public static final String JTEXTAREA = "JTEXTAREA";

    public ConfidenceWindowActions(ConfidenceWindow viewer, Director dir) {
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


    AbstractAction edgeWidth;
    JCheckBox edgeWidthCB;

    public AbstractAction getEdgeWidth(JCheckBox edgeWidthCB0) {
        AbstractAction action = edgeWidth;
        if (action != null)
            return action;
        this.edgeWidthCB = edgeWidthCB0;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Edge Width");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('W'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Highlight confidence by varying edge widths");
        all.add(action);
        return edgeWidth = action;
    }

    AbstractAction edgeShading;
    JCheckBox edgeShadingCB;

    public AbstractAction getEdgeShading(JCheckBox edgeShadingCB0) {
        AbstractAction action = edgeShading;
        if (action != null)
            return action;
        this.edgeShadingCB = edgeShadingCB0;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.NAME, "Edge Shading");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('S'));
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Highlight confidence by varying edge shading");
        all.add(action);
        return edgeShading = action;
    }

    AbstractAction selectedOnly;
    JCheckBox selectedOnlyCB;

    public AbstractAction getSelectedOnly(JCheckBox selectedOnlyCB0) {
        AbstractAction action = selectedOnly;
        if (action != null)
            return action;
        this.selectedOnlyCB = selectedOnlyCB0;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.NAME, "Selected Edges Only");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('O'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply to Selected Edges Only");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return selectedOnly = action;
    }

    // here we define the algorithms window specific actions:

    AbstractAction close;

    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.removeViewer(viewer);
                viewer.getFrame().setVisible(false);
                viewer.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        all.add(action);
        return close = action;
    }

    // here we define the algorithms window specific actions:

    AbstractAction apply;

    public AbstractAction getApply() {
        AbstractAction action = apply;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                MainViewer mainViewer = (MainViewer) dir.getViewerByClass(MainViewer.class);
                boolean width = edgeWidthCB.isSelected();
                boolean shading = edgeShadingCB.isSelected();
                boolean selectedOnly = selectedOnlyCB.isSelected();

                final ICommand cmd = new EdgeConfidenceHighlight(mainViewer, width, shading, selectedOnly);
                new Edit(cmd, "Highlight Confidence").execute(mainViewer.getUndoSupportNetwork());
                mainViewer.getActions().updateUndo();
                mainViewer.getActions().updateRedo();

            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('A'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply the selected options");
        all.add(action);
        return apply = action;
    }
}
