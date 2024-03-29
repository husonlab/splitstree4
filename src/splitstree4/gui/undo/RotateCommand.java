/*
 * RotateCommand.java Copyright (C) 2022 Daniel H. Huson
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

/**
 * rotateAbout graph command
 * Daniel Huson and David Bryant   , 5.05
 */
public class RotateCommand extends ICommandAdapter implements ICommand {
	final MainViewer viewer;
	final Transform trans;
	final double angle;

	/**
	 * constructor
	 */
	public RotateCommand(MainViewer view, Transform trans, double angle) {
		this.viewer = view;
		this.trans = trans;
		this.angle = angle;
	}

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new TransformCommand(viewer, trans, trans));
        trans.setAngle(angle);
        viewer.repaint();
        return getReverseCommand();
    }
}
