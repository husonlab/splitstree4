/**
 * RadiallyLabelCommand.java 
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
package splitstree.gui.undo;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.EdgeView;
import jloda.graphview.NodeView;
import jloda.util.Pair;
import splitstree.core.Document;
import splitstree.gui.main.MainViewer;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * command for turning circular-node labeling on or off
 * Daniel Huson and David Bryant, 2.2008
 */
public class RadiallyLabelCommand extends ICommandAdapter implements ICommand {
    final MainViewer viewer;
    final Document doc;
    final boolean state;
    final List oldNodeLocationPairs = new LinkedList();
    final List oldNodeLayoutPairs = new LinkedList();

    /**
     * constructor
     *
     * @param view
     * @param doc
     * @param state
     */
    public RadiallyLabelCommand(MainViewer view, Document doc, boolean state) {
        this.viewer = view;
        this.doc = doc;
        this.state = state;
        if (!state) {
            // will need old location pairs for undo
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                oldNodeLayoutPairs.add(new Pair(v, viewer.getNV(v).getLabelLayout()));
                oldNodeLocationPairs.add(new Pair(v, viewer.getNV(v).getLabelOffset()));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new RadiallyLabelCommand(viewer, doc, !state));

        viewer.setRadiallyLayoutNodeLabels(state);
        doc.getAssumptions().setRadiallyLayoutNodeLabels(state);
        viewer.setAutoLayoutLabels(false);
        doc.getAssumptions().setAutoLayoutNodeLabels(false);

        if (!state) // turn off
        {
            for (Object oldNodeLocationPair : oldNodeLocationPairs) {
                Pair pair = (Pair) oldNodeLocationPair;
                Node v = (Node) pair.getFirst();
                Point location = (Point) pair.getSecond();
                viewer.getNV(v).setLabelOffset(location);
            }
            for (Object oldNodeLayoutPair : oldNodeLayoutPairs) {
                Pair pair = (Pair) oldNodeLayoutPair;
                Node v = (Node) pair.getFirst();
                byte layout = (byte) pair.getSecondInt();
                viewer.getNV(v).setLabelLayout(layout);
                viewer.getNV(v).setLabelAngle(0);
            }
        } else // turn on
        {
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                viewer.setLabelLayout(v, NodeView.LAYOUT);
            }
            for (Edge v = viewer.getGraph().getFirstEdge(); v != null; v = v.getNext()) {
                viewer.setLabelLayout(v, EdgeView.CENTRAL);
            }
        }
        viewer.getActions().updateEnableState();
        // need update of menu
        viewer.repaint();
        return getReverseCommand();
    }
}
