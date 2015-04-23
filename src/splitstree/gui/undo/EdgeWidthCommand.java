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
 * change edge width  for all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeWidthCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeIntegerArray widths;

    /**
     * constructor
     *
     * @param viewer
     * @param width  width, or -1, if everyone keeps their original width
     */
    public EdgeWidthCommand(MainViewer viewer, int width) {
        this.viewer = viewer;

        PhyloGraph graph = viewer.getPhyloGraph();
        widths = new EdgeIntegerArray(graph, -1);

        boolean noneSelected = viewer.getSelectedEdges().isEmpty(); //No nodes currently selected... apply to all.
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (viewer.getSelected(e) || noneSelected) {
                widths.set(e, width != -1 ? width : viewer.getLineWidth(e));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        if (getReverseCommand() == null)
            setReverseCommand(new EdgeWidthCommand(viewer, -1));

        for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
            if (widths.getValue(e) != -1) {
                viewer.setLineWidth(e, widths.getValue(e));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
