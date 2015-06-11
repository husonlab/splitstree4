/**
 * SimpleLabelCommand.java 
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

import splitstree.core.Document;
import splitstree.gui.main.MainViewer;

/**
 * command for turning autolabeling on or off
 * Daniel Huson and David Bryant, 5.2005
 */
public class SimpleLabelCommand extends ICommandAdapter implements ICommand {
    final MainViewer viewer;
    final Document doc;
    final boolean state;

    /**
     * constructor
     *
     * @param view
     * @param doc
     * @param state
     */
    public SimpleLabelCommand(MainViewer view, Document doc, boolean state) {
        this.viewer = view;
        this.doc = doc;
        this.state = state;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new SimpleLabelCommand(viewer, doc, !state));

        viewer.setAutoLayoutLabels(false);
        doc.getAssumptions().setAutoLayoutNodeLabels(false);
        viewer.setRadiallyLayoutNodeLabels(false);
        doc.getAssumptions().setRadiallyLayoutNodeLabels(false);
        viewer.getActions().updateEnableState();
        viewer.repaint();
        return getReverseCommand();
    }
}
