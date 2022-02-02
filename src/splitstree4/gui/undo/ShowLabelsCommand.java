/*
 * ShowLabelsCommand.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloSplitsGraph;
import splitstree4.gui.main.MainViewer;

/**
 * show labels of selected nodes and edges
 * Daniel Huson and David Bryant
 */
public class ShowLabelsCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final boolean selectedOnly;
	final boolean show;
	final NodeArray showNodeLabels;
	final EdgeArray showEdgeLabels;

	/**
	 * constructor
	 */
	public ShowLabelsCommand(MainViewer viewer, boolean show, boolean selectedOnly) {
		this.viewer = viewer;
		this.selectedOnly = selectedOnly;
		this.show = show;

        PhyloSplitsGraph graph = viewer.getPhyloGraph();
        showNodeLabels = new NodeArray(graph);
        showEdgeLabels = new EdgeArray(graph);

        for (Node a = graph.getFirstNode(); a != null; a = a.getNext()) {
            if (!selectedOnly || viewer.getSelected(a)) {
                if (viewer.getLabelVisible(a) != show)
                    showNodeLabels.put(a, show);
            }
        }
        for (Edge a = graph.getFirstEdge(); a != null; a = a.getNext()) {
            if (!selectedOnly || viewer.getSelected(a)) {
                if (viewer.getLabelVisible(a) != show)
                    showEdgeLabels.put(a, show);
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
