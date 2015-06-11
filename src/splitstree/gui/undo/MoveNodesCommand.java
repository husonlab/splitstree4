/**
 * MoveNodesCommand.java 
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

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import splitstree.gui.main.MainViewer;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * move nodes in the graph
 * Daniel Huson and David Bryant
 */
public class MoveNodesCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeArray oldLocations;
    NodeArray newLocations;
    EdgeArray oldInternalPoints;
    EdgeArray newInternalPoints;

    public MoveNodesCommand(MainViewer viewer, NodeArray oldLocations,
                            NodeArray newLocations, EdgeArray oldInternalPoints, EdgeArray newInternalPoints) {
        this.viewer = viewer;
        this.oldLocations = oldLocations;
        this.newLocations = newLocations;
        this.oldInternalPoints = oldInternalPoints;
        this.newInternalPoints = newInternalPoints;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new MoveNodesCommand(viewer, newLocations, oldLocations, newInternalPoints, oldInternalPoints));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
            viewer.setLocation(v, (Point2D) newLocations.get(v));

        for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext())
            if (newInternalPoints.get(e) != null && ((List) newInternalPoints.get(e)).size() == 1)
                viewer.setInternalPoints(e, viewer.getInternalPoints(e));

        viewer.repaint();
        return getReverseCommand();
    }
}
