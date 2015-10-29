/**
 * MoveNodeLabelCommand.java
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
import jloda.graphview.NodeView;
import splitstree4.gui.main.MainViewer;

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
