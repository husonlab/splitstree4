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

import jloda.graph.Node;
import jloda.graphview.NodeView;
import splitstree.gui.main.MainViewer;

import java.awt.*;

/**
 * moves a node label
 * Daniel Huson and David Bryant
 */
public class MoveNodeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Node v;
    Point oldLocation;
    Point newLocation;
    byte oldLabelLayout;
    byte newLabelLayout;

    public MoveNodeLabelCommand(MainViewer viewer, Node v, Point oldLocation, Point newLocation) {
        this.viewer = viewer;
        this.v = v;
        this.oldLocation = oldLocation;
        this.newLocation = newLocation;
        this.oldLabelLayout = viewer.getLabelLayout(v);
        this.newLabelLayout = NodeView.USER; // the user has changed the position of the label
    }

    /**
     * the constructor for the reverse command.
     * Need to explicitly set the new label layout
     *
     * @param viewer
     * @param v
     * @param oldLocation
     * @param newLocation
     * @param newLabelLayout
     */
    private MoveNodeLabelCommand(MainViewer viewer, Node v, Point oldLocation, Point newLocation, byte newLabelLayout) {
        this(viewer, v, oldLocation, newLocation);
        this.newLabelLayout = newLabelLayout;
    }


    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new MoveNodeLabelCommand(viewer, v, newLocation, oldLocation, oldLabelLayout));
        viewer.setLabelPositionRelative(v, newLocation);
        viewer.setLabelLayout(v, newLabelLayout);
        viewer.repaint();
        return getReverseCommand();
    }
}
