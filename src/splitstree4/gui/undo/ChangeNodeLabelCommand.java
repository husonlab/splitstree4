/**
 * ChangeNodeLabelCommand.java
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
import splitstree4.gui.main.MainViewer;

/**
 * node label command
 * Daniel Huson and David Bryant
 */
public class ChangeNodeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Node v;
    String label;

    public ChangeNodeLabelCommand(MainViewer viewer, Node v, String label) {
        this.viewer = viewer;
        this.v = v;
        this.label = label;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new ChangeNodeLabelCommand(viewer, v, viewer.getLabel(v)));
        if (label != null && label.trim().length() == 0)
            label = null;
        viewer.setLabel(v, label);
        viewer.setLabelVisible(v, label != null && label.length() > 0);
        viewer.repaint();
        return getReverseCommand();
    }
}
