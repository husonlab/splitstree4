/**
 * NodeLabelRotateCommand.java
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
import jloda.graph.NodeArray;
import splitstree4.gui.main.MainViewer;

/**
 * set angle of all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeLabelRotateCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeArray angles;

    /**
     * constructor
     *
     * @param viewer
     * @param delta
     */
    public NodeLabelRotateCommand(MainViewer viewer, float delta) {
        this.viewer = viewer;
        angles = new NodeArray(viewer.getGraph());
        for (Node v : viewer.getSelectedNodes()) {
            angles.put(v, viewer.getNV(v).getLabelAngle() + delta);
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeLabelRotateCommand(viewer, 0));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (angles.get(v) != null) {
                float angle = (Float) angles.get(v);
                viewer.getNV(v).setLabelAngle(angle);
            }
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
