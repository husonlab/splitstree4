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

import jloda.graphview.Transform;
import splitstree.gui.main.MainViewer;

/**
 * flip horizontally command
 * Daniel Huson and David Bryant   , 1.06
 */
public class FlipHCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Transform trans;

    /**
     * constructor
     *
     * @param trans
     */
    public FlipHCommand(MainViewer view, Transform trans) {
        this.viewer = view;
        this.trans = trans;
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(this);
        trans.setFlipH(!trans.getFlipH());

        viewer.repaint();
        return getReverseCommand();
    }
}
