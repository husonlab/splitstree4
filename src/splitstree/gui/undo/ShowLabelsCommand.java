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
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloGraph;
import splitstree.gui.main.MainViewer;

/**
 * show labels of selected nodes and edges
 * Daniel Huson and David Bryant
 */
public class ShowLabelsCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    boolean selectedOnly;
    boolean show;
    NodeArray showNodeLabels;
    EdgeArray showEdgeLabels;

    /**
     * constructor
     *
     * @param viewer
     * @param show
     * @param selectedOnly
     */
    public ShowLabelsCommand(MainViewer viewer, boolean show, boolean selectedOnly) {
        this.viewer = viewer;
        this.selectedOnly = selectedOnly;
        this.show = show;

        PhyloGraph graph = viewer.getPhyloGraph();
        showNodeLabels = new NodeArray(graph);
        showEdgeLabels = new EdgeArray(graph);

        for (Node a = graph.getFirstNode(); a != null; a = a.getNext()) {
            if (!selectedOnly || viewer.getSelected(a)) {
                if (viewer.getLabelVisible(a) != show)
                    showNodeLabels.set(a, show);
            }
        }
        for (Edge a = graph.getFirstEdge(); a != null; a = a.getNext()) {
            if (!selectedOnly || viewer.getSelected(a)) {
                if (viewer.getLabelVisible(a) != show)
                    showEdgeLabels.set(a, show);
            }
        }

    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new ShowLabelsCommand(viewer, !show, selectedOnly));

        for (Node a = viewer.getGraph().getFirstNode(); a != null; a = a.getNext()) {
            if (showNodeLabels.get(a) != null) {
                viewer.setLabelVisible(a, (Boolean) showNodeLabels.get(a));
            }
        }
        for (Edge a = viewer.getGraph().getFirstEdge(); a != null; a = a.getNext()) {
            if (showEdgeLabels.get(a) != null) {
                viewer.setLabelVisible(a, (Boolean) showEdgeLabels.get(a));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
