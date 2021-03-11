/**
 * EdgeLabelsCommand.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.undo;

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;

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
            origLabels.put(e, viewer.getLabel(e));
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
