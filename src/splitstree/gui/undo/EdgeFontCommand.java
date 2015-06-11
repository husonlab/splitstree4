/**
 * EdgeFontCommand.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.gui.undo;

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import splitstree.gui.main.MainViewer;

import java.awt.*;
import java.util.Iterator;

/**
 * set the edge font for all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeFontCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeArray fonts;

    /**
     * constructor
     *
     * @param viewer
     * @param family  null indicates keep own family
     * @param bold    0 off, 1 on, -1 keep
     * @param italics 0 off, 1 on, -1 keep
     * @param size    -1 indicates keep own size
     */
    public EdgeFontCommand(MainViewer viewer, String family, int bold, int italics, int size) {
        this.viewer = viewer;

        // need to store current fonts now!
        this.fonts = new EdgeArray(viewer.getGraph());

        Iterator iter = viewer.getSelectedEdges().iterator();
        if (!iter.hasNext() && viewer.getNumberSelectedNodes() == 0)
            iter = viewer.getGraph().edgeIterator();

        for (; iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (viewer.getLabel(e) != null && viewer.getLabel(e).length() > 0 && viewer.getFont(e) != null) {
                String familyE = viewer.getFont(e).getFamily();
                int styleE = viewer.getFont(e).getStyle();
                int sizeE = viewer.getFont(e).getSize();
                int style = 0;
                if (bold == 1 || (bold == -1 && (styleE == Font.BOLD || styleE == Font.BOLD + Font.ITALIC)))
                    style += Font.BOLD;
                if (italics == 1 || (italics == -1 && (styleE == Font.ITALIC || styleE == Font.BOLD + Font.ITALIC)))
                    style += Font.ITALIC;

                Font font = new Font((family != null ? family : familyE), style,
                        (size == -1 ? sizeE : size));
                fonts.set(e, font);
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new EdgeFontCommand(viewer, null, -1, -1, -1));

        for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext())
            if (fonts.get(e) != null)
                viewer.setFont(e, (Font) fonts.get(e));
        viewer.repaint();
        return getReverseCommand();
    }
}
