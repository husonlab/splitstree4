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
import jloda.graph.NodeArray;
import splitstree.core.Document;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

/**
 * DESCRIPTION
 * Daniel Huson and David Bryant
 */
public class NodeLabelsCommand extends ICommandAdapter implements ICommand {
    boolean names;
    boolean ids;
    MainViewer viewer;
    boolean selectedOnly;
    NodeArray origLabels;
    boolean isReverseCommand;

    public NodeLabelsCommand(MainViewer viewer, boolean names, boolean ids,
                             boolean selectedOnly) {
        this.names = names;
        this.ids = ids;
        this.viewer = viewer;
        this.selectedOnly = selectedOnly;

        isReverseCommand = false;
    }

    /**
     * this constructor only used for reverse command
     *
     * @param viewer
     */
    private NodeLabelsCommand(MainViewer viewer) {
        this.viewer = viewer;

        isReverseCommand = true;

        origLabels = new NodeArray(viewer.getGraph());

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (!selectedOnly || viewer.getSelected(v)) {
                origLabels.set(v, viewer.getLabel(v));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeLabelsCommand(viewer));

        if (isReverseCommand) // we are doing a reverse operation
        {
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (origLabels.get(v) != null) {
                    viewer.setLabel(v, (String) origLabels.get(v));
                }
            }
        } else // really modify
        {
            Document doc = viewer.getDir().getDocument();
            Taxa taxa = doc.getTaxa();
            Network network = doc.getNetwork();
            network.modifyNodeLabels(names, ids, taxa, viewer, selectedOnly);
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
