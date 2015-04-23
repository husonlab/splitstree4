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

import jloda.util.Basic;
import jloda.graphview.Transform;
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
