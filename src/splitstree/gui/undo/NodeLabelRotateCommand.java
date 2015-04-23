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
import jloda.graph.NodeArray;
import splitstree.gui.main.MainViewer;

import java.util.Iterator;

/**
 * set angle of all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeLabelRotateCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeArray angles;

    /**
     * constructor
     *
     * @param viewer
     * @param delta
     */
    public NodeLabelRotateCommand(MainViewer viewer, float delta) {
        this.viewer = viewer;
        angles = new NodeArray(viewer.getGraph());
        for (Node v : viewer.getSelectedNodes()) {
            angles.set(v, viewer.getNV(v).getLabelAngle() + delta);
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeLabelRotateCommand(viewer, 0));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (angles.get(v) != null) {
                float angle = (Float) angles.get(v);
                viewer.getNV(v).setLabelAngle(angle);
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
