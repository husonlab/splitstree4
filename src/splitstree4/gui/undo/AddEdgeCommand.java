/**
 * AddEdgeCommand.java
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
package splitstree4.gui.undo;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import splitstree4.gui.main.MainViewer;

import java.awt.geom.Point2D;

/**
 * add or delete a edge
 * Daniel Huson and David Bryant
 */
public class AddEdgeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Node a;
    Node b;
    int x;
    int y;
    Edge edgeToDelete;
    Node nodeToDelete;

    /**
     * constructor
     *
     * @param viewer
     * @param a
     * @param b
     * @param x      x-coordinate for new node, if b==null
     * @param y      y-coordinate for new node, if b==null
     */
    public AddEdgeCommand(MainViewer viewer, Node a, Node b, int x, int y) {
        this.viewer = viewer;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
    }

    /**
     * constructor used only by reverse commande
     *
     * @param viewer
     * @param a
     * @param b
     * @param x            x-coordinate for new node, if b==null
     * @param y            y-coordinate for new node, if b==null
     * @param edgeToDelete if null, we are adding a new edge, otherwise this is the edge to delete
     * @param nodeToDelete
     */
    private AddEdgeCommand(MainViewer viewer, Node a, Node b, int x, int y, Edge edgeToDelete, Node nodeToDelete) {
        this.viewer = viewer;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.nodeToDelete = nodeToDelete;
        this.edgeToDelete = edgeToDelete;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {

        if (edgeToDelete == null) // we  are adding a new edge
        {
            Edge e = viewer.newEdge(a, b);

            if (b != null)
                setReverseCommand(new AddEdgeCommand(viewer, a, b, 0, 0, e, null));
            else // b null, so target node was generated in newEdge command
            {
                Node w = e.getTarget();
                Point2D location = viewer.trans.d2w(x, y);

                viewer.setDefaultNodeLocation(location);
                viewer.setLocation(w, location);
                viewer.setHeight(w, 2);
                viewer.setWidth(w, 2);
                viewer.setShape(w, NodeView.RECT_NODE);

                viewer.setSelected(e, true);
                viewer.setSelected(w, true);

                setReverseCommand(new AddEdgeCommand(viewer, a, b, x, y, e, w));
            }
        } else //edgeToDelete!=null, we are deleting a edge
        {
            if (nodeToDelete != null)
                viewer.getGraph().deleteNode(nodeToDelete); // edge will die, to
            else
                viewer.getGraph().deleteEdge(edgeToDelete);
            setReverseCommand(new AddEdgeCommand(viewer, a, b, x, y, null, null));
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
