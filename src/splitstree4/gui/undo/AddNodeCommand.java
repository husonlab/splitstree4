/*
 * AddNodeCommand.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.undo;

import jloda.graph.Node;
import jloda.swing.graphview.NodeView;
import splitstree4.gui.main.MainViewer;

/**
 * add or delete a node
 * Daniel Huson and David Bryant
 */
public class AddNodeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    int x;
    int y;
    Node toDelete;

    /**
     * constructor
     *
     * @param viewer
     * @param x
     * @param y
     */
    public AddNodeCommand(MainViewer viewer, int x, int y) {
        this.viewer = viewer;
        this.x = x;
        this.y = y;
    }

    /**
     * constructor for reverse command
     *
     * @param viewer
     * @param x
     * @param y
     * @param toDelete if null, we are adding a new node, otherwise this is the node to delete
     */
    private AddNodeCommand(MainViewer viewer, int x, int y, Node toDelete) {
        this.viewer = viewer;
        this.x = x;
        this.y = y;
        this.toDelete = toDelete;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        if (toDelete == null) // we  are adding a new node
        {
            Node v = viewer.newNode();
            viewer.setLocation(v, viewer.trans.d2w(x, y));
            viewer.setDefaultNodeLocation(viewer.trans.d2w(x + 10, y + 10));
            viewer.setSelected(v, true);
            viewer.setHeight(v, 2);
            viewer.setWidth(v, 2);
            viewer.setShape(v, NodeView.OVAL_NODE);
            setReverseCommand(new AddNodeCommand(viewer, x, y, v));
        } else //edgeToDelete!=null, we are deleting a node
        {
            viewer.getGraph().deleteNode(toDelete);
            setReverseCommand(new AddNodeCommand(viewer, x, y, null));
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
