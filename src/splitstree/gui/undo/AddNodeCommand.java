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

import jloda.graph.Node;
import jloda.graphview.NodeView;
import splitstree.gui.main.MainViewer;

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
