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

package splitstree.gui.main;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import jloda.util.Geometry;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * compute radial layout of node labels
 * Daniel Huson and David Bryant, 2.2008
 */
public class RadiallyLayoutNodeLabels {
    public static void doCircularLayoutNodeLabels(MainViewer viewer) {
        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            NodeView nv = viewer.getNV(v);
            if (nv.getLabelLayout() == NodeView.LAYOUT) {
                if (v.getDegree() == 1) {
                    Point2D location = nv.getLocation();
                    Edge f = v.getFirstAdjacentEdge();
                    Node w = f.getOpposite(v);

                    Point2D refPoint = null;
                    if (viewer.getInternalPoints(f) != null) {
                        if (v == f.getTarget())
                            refPoint = viewer.getInternalPoints(f).get(viewer.getInternalPoints(f).size() - 1);
                        else
                            refPoint = viewer.getInternalPoints(f).get(0);
                    }

                    if (refPoint == null || refPoint.distanceSq(location) <= 0.00000001) {
                        refPoint = viewer.getNV(w).getLocation();
                        if (refPoint.distanceSq(location) <= 0.00000001) {
                            refPoint = new Point2D.Double(0, 0);
                        }
                    }
                    float angle = (float) (Geometry.computeAngle(Geometry.diff(location, refPoint)) + viewer.trans.getAngle());
                    nv.setLabelAngle(angle);
                    int d = Math.max(nv.getHeight(), nv.getWidth()) / 2 + 15;
                    nv.setLabelOffset(Geometry.rotate(new Point(d, 0), nv.getLabelAngle()));
                } else {
                    nv.setLabelAngle(0);
                    nv.setLabelOffset(new Point(0, 0));
                    nv.setLabelLayout(NodeView.CENTRAL);
                }
            }
        }
    }
}
