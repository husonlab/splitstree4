/*
 * ChangeEdgeLabelCommand.java Copyright (C) 2022 Daniel H. Huson
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

/**
 * edge label command
 * Daniel Huson and David Bryant
 */
public class ChangeEdgeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Edge e;
    String label;

    public ChangeEdgeLabelCommand(MainViewer viewer, Edge e, String label) {
        this.viewer = viewer;
        this.e = e;
        this.label = label;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new ChangeEdgeLabelCommand(viewer, e, viewer.getLabel(e)));
        if (label != null && label.trim().length() == 0)
            label = null;
        viewer.setLabel(e, label);
        viewer.setLabelVisible(e, label != null && label.length() > 0);
        viewer.repaint();
        return getReverseCommand();
    }
}
