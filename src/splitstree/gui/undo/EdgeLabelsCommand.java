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
import jloda.graph.EdgeArray;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;

/**
 * DESCRIPTION
 * Daniel Huson and David Bryant
 */
public class EdgeLabelsCommand extends ICommandAdapter implements ICommand {
    boolean showWeight;
    boolean showEClass;
    boolean showConfidence;
    boolean showInterval;
    MainViewer viewer;
    boolean selectedOnly;
    EdgeArray origLabels;
    boolean isReverseCommand;

    public EdgeLabelsCommand(MainViewer viewer, boolean showWeight, boolean showEClass, boolean showConfidence,
                             boolean showInterval,
                             boolean selectedOnly) {
        this.showWeight = showWeight;
        this.showEClass = showEClass;
        this.showConfidence = showConfidence;
        this.showInterval = showInterval;
        this.selectedOnly = selectedOnly;
        this.viewer = viewer;

        isReverseCommand = false;
    }

    /**
     * this constructor only used for reverse command
     *
     * @param viewer
     */
    private EdgeLabelsCommand(MainViewer viewer) {
        this.viewer = viewer;

        isReverseCommand = true;

        origLabels = new EdgeArray(viewer.getGraph());

        for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
            origLabels.set(e, viewer.getLabel(e));
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new EdgeLabelsCommand(viewer));

        if (isReverseCommand) // we are doing a reverse operation
        {
            for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                String label = (String) origLabels.get(e);
                viewer.setLabel(e, label);
                viewer.getPhyloGraph().setLabel(e, label);
                viewer.setLabelVisible(e, label != null);
            }
        } else // really modify
        {
            Splits splits = viewer.getDir().getDocument().getSplits();
            Network network = viewer.getDir().getDocument().getNetwork();
            network.modifyEdgeLabels(showWeight, showEClass, showConfidence, showInterval, splits, viewer, selectedOnly);
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
