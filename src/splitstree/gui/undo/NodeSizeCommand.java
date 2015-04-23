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
 * change node size  for all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeSizeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeIntegerArray widths;
    NodeIntegerArray heights;

    /**
     * constructor
     *
     * @param viewer
     * @param width  with, or -1, if everyone keeps their original width
     * @param height or -1
     */
    public NodeSizeCommand(MainViewer viewer, int width, int height) {
        this.viewer = viewer;

        PhyloGraph graph = viewer.getPhyloGraph();
        widths = new NodeIntegerArray(graph, -1);
        heights = new NodeIntegerArray(graph);

        boolean noneSelected = viewer.getSelectedNodes().isEmpty(); //No nodes currently selected... apply to all.
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (viewer.getSelected(v) || noneSelected) {
                widths.set(v, width != -1 ? width : viewer.getWidth(v));
                heights.set(v, height != -1 ? height : viewer.getHeight(v));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeSizeCommand(viewer, -1, -1));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (widths.getValue(v) != -1) {
                viewer.setWidth(v, widths.getValue(v));
                viewer.setHeight(v, heights.getValue(v));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
