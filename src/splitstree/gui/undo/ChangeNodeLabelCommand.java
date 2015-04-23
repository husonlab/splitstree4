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
import splitstree.gui.main.MainViewer;

/**
 * node label command
 * Daniel Huson and David Bryant
 */
public class ChangeNodeLabelCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Node v;
    String label;

    public ChangeNodeLabelCommand(MainViewer viewer, Node v, String label) {
        this.viewer = viewer;
        this.v = v;
        this.label = label;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new ChangeNodeLabelCommand(viewer, v, viewer.getLabel(v)));
        if (label != null && label.trim().length() == 0)
            label = null;
        viewer.setLabel(v, label);
        viewer.setLabelVisible(v, label != null && label.length() > 0);
        viewer.repaint();
        return getReverseCommand();
    }
}
