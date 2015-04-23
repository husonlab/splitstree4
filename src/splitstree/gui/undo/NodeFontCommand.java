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
import splitstree.gui.main.MainViewer;

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
            iter = viewer.getGraph().nodeIterator();

        for (; iter.hasNext();) {
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
                fonts.set(v, font);
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
