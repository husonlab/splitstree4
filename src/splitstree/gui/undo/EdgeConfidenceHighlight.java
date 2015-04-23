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
import jloda.graph.EdgeIntegerArray;
import jloda.phylo.PhyloGraph;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;

/**
 * does confidence hightlighting
 * Daniel Huson and David Bryant
 */
public class EdgeConfidenceHighlight extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeIntegerArray widths;
    EdgeArray colors;
    Network network;
    boolean selectedOnly;

    /**
     * constructor
     *
     * @param viewer
     * @param width        uses widths to highlight
     * @param shading      use shading to highlight
     * @param selectedOnly hight selected edges only
     */
    public EdgeConfidenceHighlight(MainViewer viewer, boolean width, boolean shading, boolean selectedOnly) {
        this.viewer = viewer;
        this.selectedOnly = selectedOnly;

        PhyloGraph graph = viewer.getPhyloGraph();
        widths = new EdgeIntegerArray(graph);
        colors = new EdgeArray(graph);

        Splits splits = viewer.getDir().getDocument().getSplits();
        this.network = viewer.getDir().getDocument().getNetwork();

        network.getEdgeConfidenceHightlighting(width, shading, splits, viewer, selectedOnly, widths, colors);
    }

    /**
     * construct the reverse command
     *
     * @param viewer
     */
    private EdgeConfidenceHighlight(MainViewer viewer, boolean selectedOnly) {
        this.viewer = viewer;
        this.network = viewer.getDir().getDocument().getNetwork();
        this.selectedOnly = selectedOnly;

        PhyloGraph graph = viewer.getPhyloGraph();
        widths = new EdgeIntegerArray(graph);
        colors = new EdgeArray(graph);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (!selectedOnly || viewer.getSelected(e)) {
                widths.set(e, viewer.getLineWidth(e));
                colors.set(e, viewer.getColor(e));
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
            setReverseCommand(new EdgeConfidenceHighlight(viewer, selectedOnly));

        network.applyWidthsColors(viewer, widths, colors);

        viewer.repaint();
        return getReverseCommand();
    }
}

