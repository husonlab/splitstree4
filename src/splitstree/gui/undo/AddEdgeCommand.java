/**
 * Copyright 2015, Daniel Huson and David Bryant
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package splitstree.gui.undo;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import splitstree.gui.main.MainViewer;

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
