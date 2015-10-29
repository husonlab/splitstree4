/**
 * NodeShapeCommand.java
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

import jloda.graph.Node;
import jloda.graph.NodeIntegerArray;
import jloda.phylo.PhyloGraph;
import splitstree4.gui.main.MainViewer;

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
