/**
 * PopupListener.java 
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
package splitstree.gui.main;


import jloda.graph.EdgeSet;
import jloda.graph.NodeSet;
import jloda.graphview.IPopupListener;
import splitstree.core.Document;
import splitstree.gui.DirectorActions;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * pop-up menus
 * Daniel Huson and David Bryant
 */
public class PopupListener implements IPopupListener {
    JPopupMenu nodeMenu;
    //JPopupMenu nodeLabelMenu;
    JPopupMenu edgeMenu;
    //JPopupMenu EdgeLabelMenu;
    JPopupMenu panelMenu;
    MainViewer viewer;
    Document doc;

    public PopupListener(MainViewer viewer, Document doc, MainViewerActions mainActions,
                         DirectorActions dirActions) {
        this.viewer = viewer;
        this.doc = doc;

        nodeMenu = new JPopupMenu();
        nodeMenu.add(mainActions.getCopyNodeLabel());
        nodeMenu.add(mainActions.getEditNodeLabel());
        nodeMenu.addSeparator();
        nodeMenu.add(mainActions.getExcludeSelectedTaxa());
        nodeMenu.addSeparator();
        nodeMenu.add(mainActions.getNodeName());
        nodeMenu.add(mainActions.getNodeId());
        nodeMenu.add(mainActions.getNodeNone());

        nodeMenu.addSeparator();
        nodeMenu.add(mainActions.getConfigureNodes());

        edgeMenu = new JPopupMenu();
        edgeMenu.add(mainActions.getCopyEdgeLabel());
        edgeMenu.add(mainActions.getEditEdgeLabel());
        edgeMenu.addSeparator();
        edgeMenu.add(mainActions.getHideSelectedEdges());
        edgeMenu.addSeparator();
        edgeMenu.add(mainActions.getEdgeID());
        edgeMenu.add(mainActions.getEdgeWeight());
        edgeMenu.add(mainActions.getEdgeConfidence());
        edgeMenu.add(mainActions.getEdgeInterval());
        edgeMenu.add(mainActions.getEdgeNone());
        edgeMenu.addSeparator();
        edgeMenu.add(mainActions.getConfigureEdges());

        panelMenu = new JPopupMenu();
        panelMenu.add(mainActions.getZoomIn());
        panelMenu.add(mainActions.getZoomOut());
        panelMenu.addSeparator();
        panelMenu.add(mainActions.getResetLayout());
        panelMenu.add(mainActions.getRelayoutLabels());
        panelMenu.addSeparator();
        panelMenu.add(mainActions.getRestoreAllTaxa());
        panelMenu.add(mainActions.getRestoreAllSplits());
        panelMenu.add(mainActions.getWindowSize());
        panelMenu.addSeparator();
        JCheckBoxMenuItem cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainActions.getMaintainEdgeLengths(cbox));
        panelMenu.add(cbox);
    }

    /**
     * popup menu on node
     *
     * @param me
     * @param nodes
     */
    public void doNodePopup(MouseEvent me, NodeSet nodes) {
        if (nodes.size() != 0) {
            if (!me.isShiftDown()) {
                viewer.selectAllNodes(false);
                viewer.selectAllEdges(false);
            }
            if (!viewer.getSelected(nodes.getFirstElement()))
                viewer.setSelected(nodes.getFirstElement(), true);
            nodeMenu.show(me.getComponent(), me.getX(), me.getY());
            viewer.repaint(); // stuff gets messed up
        }
    }

    /**
     * popup menu on node label
     *
     * @param me
     * @param nodes
     */
    public void doNodeLabelPopup(MouseEvent me, NodeSet nodes) {
        doNodePopup(me, nodes);
    }

    /**
     * popup menu on edge
     *
     * @param me
     * @param edges
     */
    public void doEdgePopup(MouseEvent me, EdgeSet edges) {
        if (edges.size() != 0) {
            boolean oldSplitSelectionMode = viewer.getUseSplitSelectionModel();
            if (!me.isShiftDown()) {
                viewer.selectAllNodes(false);
                viewer.selectAllEdges(false);
            }
            if (!viewer.getSelected(edges.getFirstElement()))
                viewer.setSelected(edges.getFirstElement(), true);
            viewer.setUseSplitSelectionModel(oldSplitSelectionMode);
            edgeMenu.show(me.getComponent(), me.getX(), me.getY());
            viewer.repaint(); // stuff gets messed up
        }
    }

    /**
     * popup menu on edge
     *
     * @param me
     * @param edges
     */
    public void doEdgeLabelPopup(MouseEvent me, EdgeSet edges) {
        doEdgePopup(me, edges);
    }

    /**
     * popup menu not on graph
     *
     * @param me
     */
    public void doPanelPopup(MouseEvent me) {
        panelMenu.show(me.getComponent(), me.getX(), me.getY());
    }
}
