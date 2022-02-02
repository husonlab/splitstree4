/*
 * EdgeWidthCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.graph.EdgeIntArray;
import splitstree4.gui.main.MainViewer;

/**
 * change edge width  for all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeWidthCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final EdgeIntArray widths;

	/**
	 * constructor
	 *
	 * @param width width, or -1, if everyone keeps their original width
	 */
	public EdgeWidthCommand(MainViewer viewer, int width) {
		this.viewer = viewer;

		var graph = viewer.getPhyloGraph();
        widths = graph.newEdgeIntArray();

        boolean noneSelected = viewer.getSelectedEdges().isEmpty(); //No nodes currently selected... apply to all.
        for (var e : graph.edges()) {
            widths.set(e, -1);
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

        var graph = viewer.getPhyloGraph();
        for (var e : graph.edges()) {
            if (widths.getInt(e) != -1) {
                viewer.setLineWidth(e, widths.getInt(e));
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
