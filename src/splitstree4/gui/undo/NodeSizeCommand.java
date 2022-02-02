/*
 * NodeSizeCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.graph.Node;
import jloda.graph.NodeIntArray;
import jloda.phylo.PhyloSplitsGraph;
import splitstree4.gui.main.MainViewer;

/**
 * change node size  for all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeSizeCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final NodeIntArray widths;
	final NodeIntArray heights;

	/**
	 * constructor
	 *
	 * @param width  with, or -1, if everyone keeps their original width
	 * @param height or -1
	 */
	public NodeSizeCommand(MainViewer viewer, int width, int height) {
		this.viewer = viewer;

        PhyloSplitsGraph graph = viewer.getPhyloGraph();
        widths = graph.newNodeIntArray();
        heights = graph.newNodeIntArray();

        boolean noneSelected = viewer.getSelectedNodes().isEmpty(); //No nodes currently selected... apply to all.
        for (var v : graph.nodes()) {
            widths.set(v, -1);
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
            if (widths.get(v) != -1) {
                viewer.setWidth(v, widths.get(v));
                viewer.setHeight(v, heights.get(v));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
