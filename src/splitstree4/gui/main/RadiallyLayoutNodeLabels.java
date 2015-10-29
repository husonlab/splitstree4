/**
 * RadiallyLayoutNodeLabels.java
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
package splitstree4.gui.main;

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
