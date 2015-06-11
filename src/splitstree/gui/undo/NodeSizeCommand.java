/**
 * NodeSizeCommand.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
