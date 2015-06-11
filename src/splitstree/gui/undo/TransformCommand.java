/**
 * TransformCommand.java 
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

import jloda.graphview.Transform;
import jloda.util.Basic;
import splitstree.gui.main.MainViewer;

/**
 * change the graph transform, eg scaling etc
 * Daniel Huson and David Bryant, 5.2005
 */
public class TransformCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Transform oldTrans;
    Transform newTrans;

    public TransformCommand(MainViewer view, Transform oldTrans, Transform newTrans) {
        this.viewer = view;
        this.oldTrans = oldTrans;
        try {
            this.newTrans = (Transform) newTrans.clone();
        } catch (CloneNotSupportedException ex) {
            Basic.caught(ex);
        }
    }

    public ICommand execute() {
        setReverseCommand(new TransformCommand(viewer, oldTrans, oldTrans));
        oldTrans.copy(newTrans);
        viewer.repaint();
        return getReverseCommand();
    }
}
