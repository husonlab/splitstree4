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

import jloda.graphview.Transform;
import splitstree.gui.main.MainViewer;

import java.awt.*;

/**
 * change the graph zoom factor
 * daniel Huson, 5.2005
 */
public class ZoomCommand extends ICommandAdapter implements ICommand {
    MainViewer viewer;
    Transform trans;
    double factorx;
    double factory;
    Point aPt;

    /**
     * zoom to center
     *
     * @param view
     * @param trans
     * @param factorx
     * @param factory
     */
    public ZoomCommand(MainViewer view, Transform trans, double factorx, double factory) {
        this(view, trans, factorx, factory, null);
    }

    /**
     * zoom to a point
     *
     * @param view
     * @param trans
     * @param factorx
     * @param factory
     * @param aPt
     */
    public ZoomCommand(MainViewer view, Transform trans, double factorx, double factory, Point aPt) {
        this.viewer = view;
        this.trans = trans;
        this.factorx = factorx;
        this.factory = factory;
        this.aPt = aPt;
    }

    public ICommand execute() {
        setReverseCommand(new TransformCommand(viewer, trans, trans));
        trans.composeScale(factorx, factory);
        /**
         if (aPt != null)

         //@todo  scroll so that point aPt stays constant
         */
        viewer.repaint();
        return getReverseCommand();
    }
}
