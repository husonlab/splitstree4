/**
 * TranslateCommand.java 
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
import splitstree.gui.main.MainViewer;

/**
 * translate the graph
 * daniel Huson, 5.2005
 */
public class TranslateCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Transform trans;
    int horizontal;
    int vertical;

    public TranslateCommand(MainViewer view, Transform trans, int horizontal, int vertical) {
        this.viewer = view;
        this.trans = trans;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public ICommand execute() {
        setReverseCommand(new TransformCommand(viewer, trans, trans));
        // does nothing!
        viewer.repaint();
        return getReverseCommand();
    }
}
