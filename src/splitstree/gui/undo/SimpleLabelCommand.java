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
