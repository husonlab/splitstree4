/*
 * EdgeShapeCommand.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.undo;

import jloda.graph.Edge;
import jloda.graph.EdgeIntArray;
import jloda.phylo.PhyloSplitsGraph;
import splitstree4.gui.main.MainViewer;

/**
 * change edge shape  for all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeShapeCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeIntArray shapes;

    /**
     * constructor
     *
     * @param viewer
     * @param shape  0, 1 or 2 , -1 for keep old
     */
    public EdgeShapeCommand(MainViewer viewer, int shape) {
        this.viewer = viewer;

        PhyloSplitsGraph graph = viewer.getPhyloGraph();
        shapes = new EdgeIntArray(graph);

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
                viewer.setShape(a, (byte) shapes.getInt(a));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
