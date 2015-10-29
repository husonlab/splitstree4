/**
 * EdgeConfidenceHighlight.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.undo;

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.EdgeIntegerArray;
import jloda.phylo.PhyloGraph;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;

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

