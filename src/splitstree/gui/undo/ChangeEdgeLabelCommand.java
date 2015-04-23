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

/**
 * edge label command
 * Daniel Huson and David Bryant
 */
public class ChangeEdgeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Edge e;
    String label;

    public ChangeEdgeLabelCommand(MainViewer viewer, Edge e, String label) {
        this.viewer = viewer;
        this.e = e;
        this.label = label;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new ChangeEdgeLabelCommand(viewer, e, viewer.getLabel(e)));
        if (label != null && label.trim().length() == 0)
            label = null;
        viewer.setLabel(e, label);
        viewer.setLabelVisible(e, label != null && label.length() > 0);
        viewer.repaint();
        return getReverseCommand();
    }
}
