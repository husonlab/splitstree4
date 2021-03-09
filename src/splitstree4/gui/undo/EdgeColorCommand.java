/**
 * EdgeColorCommand.java
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

import java.awt.*;
import java.util.Random;

/**
 * set the color of all selected edges
 * Daniel Huson and David Bryant
 */
public class EdgeColorCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    EdgeArray colors;
    boolean fg;
    boolean label;
    boolean lbg;

    /**
     * constructor
     *
     * @param viewer
     * @param color  color or null, null is invisible
     */
    public EdgeColorCommand(MainViewer viewer, Color color, boolean randomColors, boolean fg, boolean label, boolean lbg) {
        this.viewer = viewer;
        this.fg = fg;
        this.label = label;
        this.lbg = lbg;

        Random rand = new Random();

        colors = new EdgeArray(viewer.getGraph());
        for (Edge a = viewer.getGraph().getFirstEdge(); a != null; a = a.getNext()) {
            if (!viewer.getSelected(a)) {
                if (fg)
                    colors.put(a, viewer.getColor(a));
                else if (lbg)
                    colors.put(a, viewer.getLabelBackgroundColor(a));
                else if (label)
                    colors.put(a, viewer.getLabelColor(a));
            } else if (randomColors)
                colors.put(a, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));

            else colors.put(a, color);
        }
    }

    /**
     * constructor  for reverse command
     *
     * @param viewer
     */
    private EdgeColorCommand(MainViewer viewer, boolean fg, boolean label, boolean lbg) {
        this.viewer = viewer;
        this.fg = fg;
        this.label = label;
        this.lbg = lbg;

        colors = new EdgeArray(viewer.getGraph());
        for (Edge a = viewer.getGraph().getFirstEdge(); a != null; a = a.getNext()) {
            {
                if (fg)
                    colors.put(a, viewer.getColor(a));
                else if (lbg)
                    colors.put(a, viewer.getLabelBackgroundColor(a));
                else if (label)
                    colors.put(a, viewer.getLabelColor(a));
            }
        }
    }

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        setReverseCommand(new EdgeColorCommand(viewer, fg, label, lbg));

        for (Edge a = viewer.getGraph().getFirstEdge(); a != null; a = a.getNext()) {
            Color color = (Color) colors.getValue(a);
            if (fg)
                viewer.setColor(a, color);
            if (lbg)
                viewer.setLabelBackgroundColor(a, color);
            if (label)
                viewer.setLabelColor(a, color);
        }
        viewer.repaint();
        return getReverseCommand();
    }
}
