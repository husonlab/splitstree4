/*
 * NodeLabelsCommand.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.undo;

import jloda.graph.Node;
import jloda.graph.NodeArray;
import splitstree4.core.Document;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;

/**
 * DESCRIPTION
 * Daniel Huson and David Bryant
 */
public class NodeLabelsCommand extends ICommandAdapter implements ICommand {
    boolean names;
    boolean ids;
	final MainViewer viewer;
	boolean selectedOnly;
	NodeArray origLabels;
	final boolean isReverseCommand;

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
	 */
    private NodeLabelsCommand(MainViewer viewer) {
        this.viewer = viewer;

        isReverseCommand = true;

        origLabels = new NodeArray(viewer.getGraph());

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (!selectedOnly || viewer.getSelected(v)) {
                origLabels.put(v, viewer.getLabel(v));
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
