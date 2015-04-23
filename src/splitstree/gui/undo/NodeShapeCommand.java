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
import jloda.graph.NodeIntegerArray;
import jloda.phylo.PhyloGraph;
import splitstree.gui.main.MainViewer;

/**
 * change node shape  for all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeShapeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeIntegerArray shapes;

    /**
     * constructor
     *
     * @param viewer
     * @param shape  0, 1 or 2 , -1 for keep old
     */
    public NodeShapeCommand(MainViewer viewer, int shape) {
        this.viewer = viewer;

        PhyloGraph graph = viewer.getPhyloGraph();
        shapes = new NodeIntegerArray(graph);

        //Todo: checkSelectALL
        boolean noneSelected = viewer.getSelectedNodes().isEmpty();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (viewer.getSelected(v) || noneSelected) {
                shapes.set(v, shape != -1 ? shape : viewer.getShape(v));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeShapeCommand(viewer, -1));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (shapes.get(v) != null) {
                viewer.setShape(v, (byte) shapes.getValue(v));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
