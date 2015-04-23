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
import splitstree.gui.main.MainViewer;

import java.awt.*;

/**
 * moves a edge label
 * Daniel Huson and David Bryant
 */
public class MoveEdgeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Edge e;
    Point oldLocation;
    Point newLocation;

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

