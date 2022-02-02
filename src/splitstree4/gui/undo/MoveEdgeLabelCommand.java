/*
 * MoveEdgeLabelCommand.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.gui.main.MainViewer;

import java.awt.*;

/**
 * moves a edge label
 * Daniel Huson and David Bryant
 */
public class MoveEdgeLabelCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final Edge e;
	final Point oldLocation;
	final Point newLocation;

	public MoveEdgeLabelCommand(MainViewer viewer, Edge e, Point oldLocation, Point newLocation) {
		this.viewer = viewer;
		this.e = e;
		this.oldLocation = oldLocation;
		this.newLocation = newLocation;
	}

	/**
	 * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new MoveEdgeLabelCommand(viewer, e, newLocation, oldLocation));
        viewer.setLabelPositionRelative(e, newLocation);
        viewer.repaint();
        return getReverseCommand();
    }
}

