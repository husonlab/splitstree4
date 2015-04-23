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
                    colors.set(a, viewer.getColor(a));
                else if (lbg)
                    colors.set(a, viewer.getLabelBackgroundColor(a));
                else if (label)
                    colors.set(a, viewer.getLabelColor(a));
            } else if (randomColors)
                colors.set(a, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));

            else colors.set(a, color);
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
                    colors.set(a, viewer.getColor(a));
                else if (lbg)
                    colors.set(a, viewer.getLabelBackgroundColor(a));
                else if (label)
                    colors.set(a, viewer.getLabelColor(a));
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
            Color color = (Color) colors.get(a);
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
