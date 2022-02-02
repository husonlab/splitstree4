/*
 * ZoomCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.swing.graphview.Transform;
import splitstree4.gui.main.MainViewer;

import java.awt.*;

/**
 * change the graph zoom factor
 * daniel Huson, 5.2005
 */
public class ZoomCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final Transform trans;
	final double factorx;
	final double factory;
	final Point aPt;

	/**
	 * zoom to center
	 */
	public ZoomCommand(MainViewer view, Transform trans, double factorx, double factory) {
		this(view, trans, factorx, factory, null);
	}

	/**
     * zoom to a point
     *
	 */
	public ZoomCommand(MainViewer view, Transform trans, double factorx, double factory, Point aPt) {
        this.viewer = view;
        this.trans = trans;
        this.factorx = factorx;
        this.factory = factory;
        this.aPt = aPt;
    }

    public ICommand execute() {
        setReverseCommand(new TransformCommand(viewer, trans, trans));
        trans.composeScale(factorx, factory);
		viewer.repaint();
		return getReverseCommand();
    }
}
