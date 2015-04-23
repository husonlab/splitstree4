/**
 * Copyright 2015, Daniel Huson and David Bryant
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
