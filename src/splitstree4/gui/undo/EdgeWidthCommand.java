/**
 * EdgeWidthCommand.java
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
import jloda.graph.EdgeIntegerArray;
import jloda.phylo.PhyloSplitsGraph;
import splitstree4.gui.main.MainViewer;

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

        PhyloSplitsGraph graph = viewer.getPhyloGraph();
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
            if (widths.get(e) != -1) {
                viewer.setLineWidth(e, widths.get(e));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
