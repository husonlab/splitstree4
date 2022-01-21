/*
 * NodeFontCommand.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.gui.main.MainViewer;

import java.awt.*;
import java.util.Iterator;

/**
 * set the node font for all selected nodes
 * Daniel Huson and David Bryant
 */
public class NodeFontCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    NodeArray fonts;

    /**
     * constructor
     *
     * @param viewer
     * @param family  null indicates keep own family
     * @param bold    0 off, 1 on, -1 keep
     * @param italics 0 off, 1 on, -1 keep
     * @param size    -1 indicates keep own size
     */
    public NodeFontCommand(MainViewer viewer, String family, int bold, int italics, int size) {
        this.viewer = viewer;

        // need to store new  fonts now!
        this.fonts = new NodeArray(viewer.getGraph());

        Iterator iter = viewer.getSelectedNodes().iterator();

        if (!iter.hasNext() && viewer.getNumberSelectedEdges() == 0)
            iter = viewer.getGraph().nodes().iterator();

        for (; iter.hasNext(); ) {
            Node v = (Node) iter.next();
            if (viewer.getLabel(v) != null && viewer.getLabel(v).length() > 0 && viewer.getFont(v) != null) {
                String familyE = viewer.getFont(v).getFamily();
                int styleE = viewer.getFont(v).getStyle();
                int sizeE = viewer.getFont(v).getSize();
                int style = 0;
                if (bold == 1 || (bold == -1 && (styleE == Font.BOLD || styleE == Font.BOLD + Font.ITALIC)))
                    style += Font.BOLD;
                if (italics == 1 || (italics == -1 && (styleE == Font.ITALIC || styleE == Font.BOLD + Font.ITALIC)))
                    style += Font.ITALIC;

                Font font = new Font((family != null ? family : familyE), style,
                        (size == -1 ? sizeE : size));
                fonts.put(v, font);
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new NodeFontCommand(viewer, null, -1, -1, -1));

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
            if (fonts.get(v) != null)
                viewer.setFont(v, (Font) fonts.get(v));
        viewer.repaint();
        return getReverseCommand();
    }
}
