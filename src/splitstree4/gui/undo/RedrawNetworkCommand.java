/*
 * RedrawNetworkCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Network;

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
