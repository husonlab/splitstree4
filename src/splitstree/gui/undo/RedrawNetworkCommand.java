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
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Network;

/**
 * Redraw rooted network
 * daniel Huson, 11.2010
 */
public class RedrawNetworkCommand extends ICommandAdapter implements ICommand {
    private final MainViewer viewer;

    /**
     * redraw
     *
     * @param viewer
     */
    public RedrawNetworkCommand(MainViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * execute the command
     *
     * @return reverse command
     */
    public ICommand execute() {
        final Director dir = viewer.getDir();
        final Document doc = dir.getDocument();

        setReverseCommand(new RedrawNetworkCommand(viewer));

        String lastBeforeNetwork = null;
        for (String name : doc.getListOfValidInputBlocks()) {
            if (name.equals(Network.NAME))
                break;
            lastBeforeNetwork = name;
        }
        if (lastBeforeNetwork != null)
            try {
                dir.getDocument().update(lastBeforeNetwork);
            } catch (Exception e) {
                Basic.caught(e);
            }
        return getReverseCommand();
    }
}
