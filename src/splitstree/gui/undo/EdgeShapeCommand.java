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
import jloda.graph.EdgeIntegerArray;
import jloda.phylo.PhyloGraph;
import splitstree.gui.main.MainViewer;

/**
 * change edge shape  for all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeShapeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeIntegerArray shapes;

    /**
     * constructor
     *
     * @param viewer
     * @param shape  0, 1 or 2 , -1 for keep old
     */
    public EdgeShapeCommand(MainViewer viewer, int shape) {
        this.viewer = viewer;

        PhyloGraph graph = viewer.getPhyloGraph();
        shapes = new EdgeIntegerArray(graph);

        //Todo: checkSelectALL
        boolean noneSelected = viewer.getSelectedEdges().isEmpty();
        for (Edge a = graph.getFirstEdge(); a != null; a = a.getNext()) {
            if (viewer.getSelected(a) || noneSelected) {
                shapes.set(a, shape != -1 ? shape : viewer.getShape(a));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new EdgeShapeCommand(viewer, -1));

        for (Edge a = viewer.getGraph().getFirstEdge(); a != null; a = a.getNext()) {
            if (shapes.get(a) != null) {
                viewer.setShape(a, (byte) shapes.getValue(a));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
