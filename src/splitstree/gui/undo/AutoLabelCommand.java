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
import jloda.graph.Node;
import jloda.graphview.EdgeView;
import jloda.graphview.NodeView;
import jloda.util.Pair;
import splitstree.core.Document;
import splitstree.gui.main.MainViewer;

import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * command for turning autolabeling on or off
 * Daniel Huson and David Bryant, 5.2005
 */
public class AutoLabelCommand extends ICommandAdapter implements ICommand {
    final MainViewer viewer;
    final Document doc;
    final boolean state;
    final List oldNodeLocationPairs = new LinkedList();
    final List oldNodeLayoutPairs = new LinkedList();

    /**
     * constructor
     *
     * @param view
     * @param doc
     * @param state
     */
    public AutoLabelCommand(MainViewer view, Document doc, boolean state) {
        this.viewer = view;
        this.doc = doc;
        this.state = state;
        if (!state) {
            // will need old location pairs for undo
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                oldNodeLayoutPairs.add(new Pair(v, viewer.getNV(v).getLabelLayout()));
                oldNodeLocationPairs.add(new Pair(v, viewer.getNV(v).getLabelOffset()));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new AutoLabelCommand(viewer, doc, !state));

        viewer.setAutoLayoutLabels(state);
        doc.getAssumptions().setAutoLayoutNodeLabels(state);
        viewer.setRadiallyLayoutNodeLabels(false);
        doc.getAssumptions().setRadiallyLayoutNodeLabels(false);

        if (!state) // turn off
        {
            for (Object oldNodeLocationPair : oldNodeLocationPairs) {
                Pair pair = (Pair) oldNodeLocationPair;
                Node v = (Node) pair.getFirst();
                Point location = (Point) pair.getSecond();
                viewer.getNV(v).setLabelOffset(location);
            }
            for (Object oldNodeLayoutPair : oldNodeLayoutPairs) {
                Pair pair = (Pair) oldNodeLayoutPair;
                Node v = (Node) pair.getFirst();
                byte layout = (byte) pair.getSecondInt();
                viewer.getNV(v).setLabelLayout(layout);
            }
        } else // turn on
        {
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                viewer.setLabelLayout(v, NodeView.LAYOUT);
            }
            for (Edge v = viewer.getGraph().getFirstEdge(); v != null; v = v.getNext()) {
                viewer.setLabelLayout(v, EdgeView.CENTRAL);
            }
        }
        viewer.getActions().updateEnableState();
        viewer.repaint();
        return getReverseCommand();
    }
}
