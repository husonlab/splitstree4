/**
 * GraphViewListener.java
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

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.*;
import jloda.swing.util.Alert;
import jloda.swing.util.Cursors;
import jloda.swing.util.Geometry;
import jloda.util.Basic;
import splitstree4.gui.undo.*;
import splitstree4.nexus.Network;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;

/**
 * @author Daniel Huson and David Bryant, 2005
 *         Listener for all MainViewer events.
 */
public class GraphViewListener implements IGraphViewListener {
    MainViewer viewer;
    final private int inClick = 1;
    final private int inMoveNode = 2;
    final private int inRubberband = 3;
    final private int inNewEdge = 4;
    final private int inMoveNodeLabel = 5;
    final private int inMoveEdgeLabel = 6;
    final private int inMoveInternalEdgePoint = 7;
    final private int inScrollByMouse = 8;
    final private int inMoveMagnifier = 9;
    final private int inResizeMagnifier = 10;

    private int current;
    private int downX;
    private int downY;

    // this stuff is used to record begining and end of movement of a node label or edge label:
    private Node currentNode;
    private Edge currentEdge;
    private Point startLocation;
    private Point endLocation;

    // this is where we store coordinates to be able to undo a move:
    private NodeArray startCoordinates;
    private NodeArray endCoordinates;
    private EdgeArray startInternalPoints;
    private EdgeArray endInternalPoints;


    private Rectangle selRect;
    private Point prevPt;
    private Point offset; // used by move node label

    private NodeSet hitNodes;
    private NodeSet hitNodeLabels;
    private EdgeSet hitEdges;
    private EdgeSet hitEdgeLabels;

    private boolean inPopup = false;

    // is mouse still pressed?
    private boolean stillDownWithoutMoving = false;

    /**
     * Constructor
     *
     * @param viewer MainViewer
     */
    public GraphViewListener(MainViewer viewer) {
        this.viewer = viewer;
        hitNodes = new NodeSet(viewer.getGraph());
        hitNodeLabels = new NodeSet(viewer.getGraph());
        hitEdges = new EdgeSet(viewer.getGraph());
        hitEdgeLabels = new EdgeSet(viewer.getGraph());
    }

    /**
     * Mouse pressed.
     *
     * @param me MouseEvent
     */
    public void mousePressed(MouseEvent me) {
        downX = me.getX();
        downY = me.getY();
        selRect = null;
        prevPt = null;
        offset = new Point();

        stillDownWithoutMoving = true;

        int magnifierHit = viewer.trans.getMagnifier().hit(downX, downY);

        if (magnifierHit == Magnifier.HIT_MOVE) {
            current = inMoveMagnifier;
            viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            return;
        } else if (magnifierHit == Magnifier.HIT_RESIZE) {
            current = inResizeMagnifier;
            viewer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            return;
        } else if (magnifierHit == Magnifier.HIT_INCREASE_MAGNIFICATION) {
            if (viewer.trans.getMagnifier().increaseDisplacement())
                viewer.repaint();
            return;
        } else if (magnifierHit == Magnifier.HIT_DECREASE_MAGNIFICATION) {
            if (viewer.trans.getMagnifier().decreaseDisplacement())
                viewer.repaint();
            return;
        }

        hitNodes = viewer.getGraphDrawer().getHitNodes(downX, downY);
        int numHitNodes = hitNodes.size();
        viewer.fireDoPress(hitNodes);

        hitNodeLabels = viewer.getGraphDrawer().getHitNodeLabels(downX, downY);
        int numHitNodeLabels = hitNodeLabels.size();

        hitEdges = viewer.getGraphDrawer().getHitEdges(downX, downY);
        int numHitEdges = hitEdges.size();
        viewer.fireDoPress(hitEdges);

        hitEdgeLabels = viewer.getGraphDrawer().getHitEdgeLabels(downX, downY);
        int numHitEdgeLabels = hitEdgeLabels.size();

        // try again with more tolerance
        if (numHitNodes == 0 && numHitEdges == 0 && numHitNodeLabels == 0 && numHitEdgeLabels == 0) {
            hitNodes = viewer.getGraphDrawer().getHitNodes(downX, downY, 8);
            numHitNodes = hitNodes.size();
            viewer.fireDoPress(hitNodes);
        }

        /*
        System.err.println("hit nodes: "+numHitNodes);
        System.err.println("hit node labels: "+numHitNodeLabels);
        System.err.println("hit edges: "+numHitEdges);
        System.err.println("hit edge labels: "+numHitEdgeLabels);
          */

        if (me.isPopupTrigger()) {
            inPopup = true;
            viewer.setCursor(Cursor.getDefaultCursor());
            if (numHitNodes != 0)
                viewer.fireNodePopup(me, hitNodes);
            else if (numHitNodeLabels != 0)
                viewer.fireNodeLabelPopup(me, hitNodeLabels);
            else if (numHitEdges != 0)
                viewer.fireEdgePopup(me, hitEdges);
            else if (numHitEdgeLabels != 0)
                viewer.fireEdgeLabelPopup(me, hitEdgeLabels);
            else
                viewer.firePanelPopup(me);
            viewer.resetCursor();
            return;
        }

        if (numHitNodes == 0 && numHitNodeLabels == 0 && numHitEdges == 0 && numHitEdgeLabels == 0) {
            if (me.isAltDown() || me.isShiftDown()) {
                current = inRubberband;
                viewer.setCursor(Cursor.getDefaultCursor());
            } else {
                current = inScrollByMouse;
                viewer.setCursor(Cursors.getClosedHand());

                final Thread worker = new Thread(new Runnable() {
                    public void run() {
                        try {
                            synchronized (this) {
                                wait(500);
                            }
                        } catch (InterruptedException e) {
                        }
                        if (stillDownWithoutMoving) {
                            current = inRubberband;
                            viewer.setCursor(Cursor.getDefaultCursor());
                        }
                    }
                });
                worker.setPriority(Thread.currentThread().getPriority() - 1);
                worker.start();
            }
        } else {
            viewer.setCursor(Cursor.getDefaultCursor());

            if (viewer.getAllowEdit() && numHitNodes == 1 && me.isAltDown() && !me.isShiftDown()) {
                current = inNewEdge;
            } else if (numHitNodes == 0 && numHitEdges == 0 && numHitNodeLabels > 0) {
                Node v = hitNodeLabels.getFirstElement();
                if (!viewer.getSelected(v) || viewer.getLabel(v) == null)
                    return; // move labels only of selected node
                current = inMoveNodeLabel;
                viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                currentNode = hitNodeLabels.getFirstElement();
                startLocation = viewer.getNV(currentNode).getLabelPositionRelative(viewer.trans);
                endLocation = null;
            } else if (numHitNodes == 0 && numHitEdges == 0 && numHitNodeLabels == 0 && numHitEdgeLabels > 0) {
                Edge e = hitEdgeLabels.getFirstElement();
                if (!viewer.getSelected(e) || viewer.getLabel(e) == null)
                    return; // move labels only of selected edges
                current = inMoveEdgeLabel;
                viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                currentEdge = hitEdgeLabels.getFirstElement();
                startLocation = viewer.getEV(currentEdge).getLabelPositionRelative(viewer.trans);
                endLocation = null;
            } else if (numHitNodes > 0 && !me.isAltDown()
                    && !me.isAltDown() && !me.isShiftDown()) {
                if (!viewer.getAllowMoveNodes())
                    return;
                current = inMoveNode;
                viewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                startCoordinates = getAllCoordinates(viewer);
                endCoordinates = null;
                startInternalPoints = getAllInternalPoints(viewer);
                endInternalPoints = null;

// if no hit node selected, deselect all and then select node
                boolean found = false;
                for (Node v = hitNodes.getFirstElement(); v != null;
                     v = hitNodes.getNextElement(v)) {
                    if (viewer.getSelectedNodes().contains(v)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    viewer.selectAllNodes(false);
                    viewer.selectAllEdges(false);
                    viewer.setSelected(hitNodes.getFirstElement(), true);
                }
            } else if (viewer.isAllowInternalEdgePoints()
                    && viewer.isAllowMoveInternalEdgePoints() && numHitEdges == 1) {
                current = inMoveInternalEdgePoint;
            }
        }
    }

    /**
     * Mouse released.
     *
     * @param me MouseEvent
     */
    public void mouseReleased(MouseEvent me) {
        viewer.resetCursor();
        stillDownWithoutMoving = false;

        if (current == inScrollByMouse) {
            return;
        }
        {
            NodeSet hitNodes = viewer.getGraphDrawer().getHitNodes(me.getX(), me.getY());
            viewer.fireDoRelease(hitNodes);
            EdgeSet hitEdges = viewer.getGraphDrawer().getHitEdges(me.getX(), me.getY());
            viewer.fireDoRelease(hitEdges);
            if (hitNodes.size() == 0 && hitEdges.size() == 0) {
                // try again with more tolerance
                hitNodes = viewer.getGraphDrawer().getHitNodes(me.getX(), me.getY(), 8);
                viewer.fireDoRelease(hitNodes);
            }
        }

        if (me.isPopupTrigger()) {
            inPopup = true;
            if (hitNodes.size() != 0)
                viewer.fireNodePopup(me, hitNodes);
            else if (hitNodeLabels.size() != 0)
                viewer.fireNodeLabelPopup(me, hitNodeLabels);
            else if (hitEdges.size() != 0)
                viewer.fireEdgePopup(me, hitEdges);
            else if (hitEdgeLabels.size() != 0)
                viewer.fireEdgeLabelPopup(me, hitEdgeLabels);
            else
                viewer.firePanelPopup(me);
            return;
        }


        if (current == inRubberband) {
            Rectangle rect = new Rectangle(downX, downY, 0, 0);
            rect.add(me.getX(), me.getY());
            selectNodesEdges(viewer.getGraphDrawer().getHitNodes(rect), viewer.getGraphDrawer().getHitEdges(rect), me.isShiftDown(), false);
            viewer.repaint();
        } else if (current == inNewEdge) {
            NodeSet firstHit = viewer.getGraphDrawer().getHitNodes(downX, downY, 8);
            if (firstHit.size() == 1) {
                Node v = firstHit.getFirstElement();
                NodeSet secondHit = viewer.getGraphDrawer().getHitNodes(me.getX(), me.getY(), 8);

                if (secondHit.size() == 0) {
                    int x = me.getX();
                    int y = me.getY();
                    final ICommand cmd = new AddEdgeCommand(viewer, v, null, x, y);
                    new Edit(cmd, "new edge").execute(viewer.getUndoSupportNetwork());
                } else if (secondHit.size() == 1) {
                    Node w = secondHit.getFirstElement();
                    if (v != w) {
                        final ICommand cmd = new AddEdgeCommand(viewer, v, w, 0, 0);
                        new Edit(cmd, "new edge").execute(viewer.getUndoSupportNetwork());
                    }
                }
                viewer.repaint();
            }
        } else if (current == inMoveNodeLabel) {
            if (startLocation != null && endLocation != null) {
                final ICommand cmd = new MoveNodeLabelCommand(viewer, currentNode, startLocation, endLocation);
                new Edit(cmd, "move label").execute(viewer.getUndoSupportNetwork());
            }
        } else if (current == inMoveEdgeLabel) {
            if (startLocation != null && endLocation != null) {
                final ICommand cmd = new MoveEdgeLabelCommand(viewer, currentEdge, startLocation, endLocation);
                new Edit(cmd, "move label").execute(viewer.getUndoSupportNetwork());
            }
        } else if (current == inMoveNode) {
            if (startCoordinates != null && endCoordinates != null) {
                final ICommand cmd = new MoveNodesCommand(viewer, startCoordinates, endCoordinates, startInternalPoints, endInternalPoints);
                new Edit(cmd, "reshape graph").execute(viewer.getUndoSupportNetwork());
                viewer.repaint();
            }
        }
        current = 0;
    }

    /**
     * Mouse entered.
     *
     * @param me MouseEvent
     */
    public void mouseEntered(MouseEvent me) {
        viewer.requestFocusInWindow();
    }

    /**
     * Mouse exited.
     *
     * @param me MouseEvent
     */
    public void mouseExited(MouseEvent me) {
        stillDownWithoutMoving = false;
    }

    /**
     * Mouse clicked.
     *
     * @param me MouseEvent
     */
    public void mouseClicked(MouseEvent me) {
        int meX = me.getX();
        int meY = me.getY();

        if (inPopup) {
            inPopup = false;
            return;
        }

        NodeSet hitNodes = viewer.getGraphDrawer().getHitNodes(meX, meY);
        EdgeSet hitEdges = viewer.getGraphDrawer().getHitEdges(meX, meY);
        NodeSet hitNodeLabels = viewer.getGraphDrawer().getHitNodeLabels(meX, meY);
        EdgeSet hitEdgeLabels = viewer.getGraphDrawer().getHitEdgeLabels(meX, meY);

        if (current == inScrollByMouse) // in navigation mode, double-click to lose selection
        {
            if (hitNodes.size() == 0 && hitEdges.size() == 0 && hitNodeLabels.size() == 0
                    && hitEdgeLabels.size() == 0) {
                viewer.selectAllNodes(false);
                viewer.selectAllEdges(false);
                viewer.repaint();
                return;
            }
        }
        current = inClick;

        // try again with more tolerance
        if (hitNodes.size() == 0 && hitEdges.size() == 0 && hitNodeLabels.size() == 0 && hitEdgeLabels.size() == 0)
            hitNodes = viewer.getGraphDrawer().getHitNodes(meX, meY, 8);

        viewer.fireDoClick(hitNodes, me.getClickCount());
        viewer.fireDoClick(hitEdges, me.getClickCount());

        if (!hitNodes.isEmpty() || !hitEdges.isEmpty()) {
            selectNodesEdges(hitNodes, hitEdges, me.isShiftDown(), true);
        } else if (!hitNodeLabels.isEmpty() || !hitEdgeLabels.isEmpty()) {
            selectNodesEdges(hitNodeLabels, hitEdgeLabels, me.isShiftDown(), true);
        }

        if (viewer.getAllowEdit() && hitNodes.size() == 0 && hitEdges.size() == 0 && me.isAltDown() && me.getClickCount() == 2) {
            // New node:
            if (viewer.getAllowNewNodeDoubleClick()) {
                viewer.setDefaultNodeLocation(viewer.trans.d2w(meX, meY));
                final ICommand cmd = new AddNodeCommand(viewer, meX, meY);
                new Edit(cmd, "New node").execute(viewer.getUndoSupportNetwork());
            }
        } else if (viewer.isAllowInternalEdgePoints() && hitNodes.size() == 0 && hitEdges.size() == 1 && me.getClickCount() == 3) {
            Edge e = hitEdges.getFirstElement();
            EdgeView ev = viewer.getEV(e);
            Point vp = viewer.trans.w2d(viewer.getLocation(viewer.getGraph().getSource(e)));
            Point wp = viewer.trans.w2d(viewer.getLocation(viewer.getGraph().getTarget(e)));
            int index = ev.hitEdgeRank(vp, wp, viewer.trans, me.getX(), meY, 3);
            java.util.List<Point2D> list = viewer.getInternalPoints(e);
            Point2D aptWorld = viewer.trans.d2w(me.getX(), meY);
            if (list == null) {
                list = new LinkedList<>();
                list.add(aptWorld);
                viewer.setInternalPoints(e, list);
            } else
                list.add(index, aptWorld);
        } else if (me.getClickCount() == 2
                && ((viewer.isAllowEditNodeLabelsOnDoubleClick() && hitNodeLabels.size() > 0)
                || (viewer.isAllowEditNodeLabelsOnDoubleClick() && hitNodes.size() > 0))) {
// undo node label
            final Node v;
            if (viewer.isAllowEditNodeLabelsOnDoubleClick() && hitNodeLabels.size() > 0)
                v = hitNodeLabels.getLastElement();
            else
                v = hitNodes.getLastElement();
            String label = viewer.getLabel(v);
            label = JOptionPane.showInputDialog(viewer, "Edit Node Label:", label);
            if (label != null && !label.equals(viewer.getLabel(v))) {
                final ICommand cmd = new ChangeNodeLabelCommand(viewer, v, label);
                new Edit(cmd, "edit label").execute(viewer.getUndoSupportNetwork());
            }
        } else if (me.getClickCount() == 2 &&
                ((viewer.isAllowEditEdgeLabelsOnDoubleClick() && hitEdgeLabels.size() > 0)
                        || (viewer.isAllowEditEdgeLabelsOnDoubleClick() && hitEdges.size() > 0))) {
            Edge e;
            if (viewer.isAllowEditEdgeLabelsOnDoubleClick() && hitEdgeLabels.size() > 0)
                e = hitEdgeLabels.getLastElement();
            else
                e = hitEdges.getLastElement();
            String label = viewer.getLabel(e);
            label = JOptionPane.showInputDialog(viewer, "Edit Edge Label:", label);
            if (label != null && !label.equals(viewer.getLabel(e))) {
                final ICommand cmd = new ChangeEdgeLabelCommand(viewer, e, label);
                new Edit(cmd, "edit Label").execute(viewer.getUndoSupportNetwork());
            }
        }
    }


    /**
     * Mouse dragged.
     *
     * @param me MouseEvent
     */
    public void mouseDragged(MouseEvent me) {
        stillDownWithoutMoving = false;

        /*
        if(mouseDownTime!=0) {
            long time=System.currentTimeMillis();
            if(time-mouseDownTime>400) {
                current=inRubberband;
                viewer.setCursor(Cursor.getDefaultCursor());
            }
            mouseDownTime=0;
        }
        */

        if (current == inScrollByMouse) {
            JScrollPane scrollPane = viewer.getScrollPane();
            int dX = me.getX() - downX;
            int dY = me.getY() - downY;

            if (dY != 0) {
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                int amount = Math.round(dY * (scrollBar.getMaximum() - scrollBar.getMinimum()) / viewer.getHeight());
                if (amount != 0) {
                    scrollBar.setValue(scrollBar.getValue() - amount);
                }
            }
            if (dX != 0)

            {
                JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
                int amount = Math.round(dX * (scrollBar.getMaximum() - scrollBar.getMinimum()) / viewer.getWidth());
                if (amount != 0) {
                    scrollBar.setValue(scrollBar.getValue() - amount);
                }
            }
        } else if (current == inRubberband) {
            Graphics2D gc = (Graphics2D) viewer.getGraphics();

            if (gc != null) {
                gc.setXORMode(viewer.getCanvasColor());
                if (selRect != null)
                    gc.drawRect(selRect.x, selRect.y, selRect.width, selRect.height);
                selRect = new Rectangle(downX, downY, 0, 0);
                selRect.add(me.getX(), me.getY());
                gc.drawRect(selRect.x, selRect.y, selRect.width, selRect.height);
            }
        } else if (current == inMoveNode) {
            Point2D p2 = viewer.trans.d2w(me.getPoint());
            Point2D p1 = viewer.trans.d2w(downX, downY);

            downX = me.getX();
            downY = me.getY();
            Point2D diff = new Point2D.Double(p2.getX() - p1.getX(),
                    p2.getY() - p1.getY());

            double origLength = -1; // use in maintain edge lengths

            if (viewer.getMaintainEdgeLengths()) {
                origLength = canMaintainEdgeLengths();
                if (origLength == -1) {
                    return;
                }
            }

            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (viewer.getSelectedNodes().contains(v)) {
                    Point2D p = viewer.getLocation(v);
                    viewer.setLocation(v, p.getX() + diff.getX(),
                            p.getY() + diff.getY());
                }
            }
            if (viewer.getMaintainEdgeLengths() && origLength != -1) {
                if (viewer.trans.getLockXYScale())
                    maintainEdgeLengths(origLength);
            }
            {
                for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                    Point2D startPoint = (Point2D) startInternalPoints.get(e);
                    if (startPoint != null) {
                        Node v;
                        if (((Point2D) startCoordinates.get(e.getSource())).getY() == startPoint.getY())
                            v = e.getSource();
                        else
                            v = e.getTarget();

                        Point2D aPt = (Point2D) startCoordinates.get(v);
                        diff = Geometry.diff(viewer.getLocation(v), aPt);
                        java.util.List<Point2D> list = new LinkedList<>();
                        list.add(new Point2D.Double(startPoint.getX() + diff.getX(), startPoint.getY() + diff.getY()));
                        viewer.setInternalPoints(e, list);
                    }
                }
            }

            endCoordinates = getAllCoordinates(viewer);
            endInternalPoints = getAllInternalPoints(viewer);

            viewer.repaint();
        } else if (viewer.isAllowInternalEdgePoints() && current == inMoveInternalEdgePoint) {
            Point p1 = new Point(downX, downY); // old [pos
            Edge e = viewer.getGraphDrawer().getHitEdges(downX, downY).getFirstElement();

            downX = me.getX();
            downY = me.getY();
            Point p2 = new Point(downX, downY);     // new pos

            if (e != null) {
                viewer.getEV(e).moveInternalPoint(viewer.trans, p1, p2);
                viewer.repaint();
            }
        } else if (current == inMoveNodeLabel) {
            if (hitNodeLabels.size() > 0) {
                Node v = hitNodeLabels.getFirstElement();

                if (!viewer.getSelected(v))
                    return; // move labels only of selected node
                NodeView nv = viewer.getNV(v);

                if (nv.getLabel() == null)
                    return;

                Graphics2D gc = (Graphics2D) viewer.getGraphics();

                if (gc != null) {
                    Point apt = viewer.trans.w2d(nv.getLocation());
                    int meX = me.getX();
                    int meY = me.getY();
                    gc.setXORMode(viewer.getCanvasColor());
                    if (prevPt != null)
                        gc.drawLine(apt.x, apt.y, prevPt.x, prevPt.y);
                    else {
                        prevPt = new Point(downX, downY);
                        Point labPt = nv.getLabelPosition(viewer.trans);
                        offset.x = labPt.x - downX;
                        offset.y = labPt.y - downY;
                    }
                    gc.drawLine(apt.x, apt.y, meX, meY);
                    nv.drawLabel(gc, viewer.trans, viewer.getFont());
                    nv.hiliteLabel(gc, viewer.trans, viewer.getFont());

                    int labX = meX + offset.x;
                    int labY = meY + offset.y;

                    nv.setLabelPositionRelative(labX - apt.x, labY - apt.y);
                    nv.drawLabel(gc, viewer.trans, viewer.getFont());
                    nv.hiliteLabel(gc, viewer.trans, viewer.getFont());

                    prevPt.x = meX;
                    prevPt.y = meY;

                    endLocation = viewer.getNV(v).getLabelPositionRelative(viewer.trans);
                }
            }
        } else if (current == inMoveEdgeLabel) {
            if (hitEdgeLabels.size() > 0) {
                try {
                    Edge e = hitEdgeLabels.getFirstElement();
                    if (!viewer.getSelected(e))
                        return; // move labels only of selected edges
                    EdgeView ev = viewer.getEV(e);

                    if (ev.getLabel() == null)
                        return;

                    final Graph G = viewer.getGraph();
                    final NodeView vv = viewer.getNV(G.getSource(e));
                    final NodeView wv = viewer.getNV(G.getTarget(e));

                    Point2D nextToV = wv.getLocation();
                    Point2D nextToW = vv.getLocation();
                    if (viewer.getInternalPoints(e) != null) {
                        if (viewer.getInternalPoints(e).size() != 0) {
                            nextToV = viewer.getInternalPoints(e).get(0);
                            nextToW = viewer.getInternalPoints(e).get(viewer.getInternalPoints(e).size() - 1);
                        }
                    }
                    Point pv = vv.computeConnectPoint(nextToV, viewer.trans);
                    Point pw = wv.computeConnectPoint(nextToW, viewer.trans);

                    if (G.findDirectedEdge(G.getTarget(e), G.getSource(e)) != null)
                        viewer.adjustBiEdge(pv, pw); // want parallel bi-edges

                    Graphics2D gc = (Graphics2D) viewer.getGraphics();

                    if (gc != null) {
                        ev.setLabelReferencePosition(pv, pw, viewer.trans);
                        ev.setLabelSize(gc);
                        Point apt = ev.getLabelReferencePoint();
                        int meX = me.getX();
                        int meY = me.getY();


                        gc.setXORMode(viewer.getCanvasColor());
                        if (prevPt != null)
                            gc.drawLine(apt.x, apt.y, prevPt.x, prevPt.y);
                        else {
                            prevPt = new Point(downX, downY);
                            Point labPt = ev.getLabelPosition(viewer.trans);
                            offset.x = labPt.x - downX;
                            offset.y = labPt.y - downY;
                        }
                        gc.drawLine(apt.x, apt.y, meX, meY);
                        ev.drawLabel(gc, viewer.trans, viewer.getSelected(e));
                        int labX = meX + offset.x;
                        int labY = meY + offset.y;

                        ev.setLabelPositionRelative(labX - apt.x, labY - apt.y);
                        endLocation = ev.getLabelPositionRelative(viewer.trans);
                        ev.drawLabel(gc, viewer.trans, viewer.getSelected(e));

                        prevPt.x = meX;
                        prevPt.y = meY;
                    }
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        } else if (current == inNewEdge) {
            Graphics gc = viewer.getGraphics();

            if (gc != null) {
                gc.setXORMode(viewer.getCanvasColor());
                if (selRect != null) // we misuse the selRect here...
                    gc.drawLine(downX, downY, selRect.x, selRect.y);
                selRect = new Rectangle(me.getX(), me.getY(), 0, 0);
                gc.drawLine(downX, downY, me.getX(), me.getY());
            }
        } else if (current == inMoveMagnifier) {
            int meX = me.getX();
            int meY = me.getY();
            if (meX != downX || meY != downY) {
                viewer.trans.getMagnifier().move(downX, downY, meX, meY);
                downX = meX;
                downY = meY;
                viewer.repaint();
            }
        } else if (current == inResizeMagnifier) {
            int meY = me.getY();
            if (meY != downY) {
                viewer.trans.getMagnifier().resize(downY, meY);
                downX = me.getX();
                downY = meY;
                viewer.repaint();
            }
        }
    }

    /**
     * gets a node array containing all current coordinates
     *
     * @param viewer
     * @return snap shot of current coordinates
     */
    private NodeArray<Point2D> getAllCoordinates(MainViewer viewer) {
        NodeArray<Point2D> result = new NodeArray<>(viewer.getGraph());
        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            result.put(v, (Point2D) viewer.getLocation(v).clone());
        }
        return result;
    }

    /**
     * gets an edge array containing all current single internal  points
     *
     * @param viewer
     * @return snap shot of current coordinates
     */
    private EdgeArray getAllInternalPoints(MainViewer viewer) {
        EdgeArray<Point2D> result = new EdgeArray<>(viewer.getGraph());
        for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
            java.util.List<Point2D> internalPoints = viewer.getInternalPoints(e);
            if (internalPoints != null && internalPoints.size() == 1)
                result.put(e, internalPoints.get(0));
        }
        return result;
    }

    /**
     * Mouse moved.
     *
     * @param me MouseEvent
     */
    public void mouseMoved(MouseEvent me) {
        stillDownWithoutMoving = false;
    }

    /**
     * Updates the selection of nodes and edges.
     *
     * @param hitNodes NodeSet
     * @param hitEdges EdgeSet
     * @param shift    boolean
     * @param click    boolean
     */
    void selectNodesEdges(NodeSet hitNodes, EdgeSet hitEdges, boolean shift,
                          boolean click) {
        if (hitNodes.size() == 1) // in this case, only do node selection
            hitEdges.clear();

        Graph G = viewer.getGraph();

        boolean changed = false;

        // synchronized (G)
        {
            // no shift, deselect everything:
            if (!shift
                    && (viewer.getNumberSelectedNodes() > 0 || viewer.getNumberSelectedEdges() > 0)) {
                viewer.selectAllNodes(false);
                viewer.selectAllEdges(false);
                changed = true;
            }

            NodeSet toSelectNodes = new NodeSet(G);
            NodeSet toDeselectNodes = new NodeSet(G);

            EdgeSet toSelectEdges = new EdgeSet(G);
            EdgeSet toDeselectEdges = new EdgeSet(G);
            try {
                if ((click || viewer.isAllowRubberbandNodes()) && hitNodes.size() > 0) {
                    for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                        if (hitNodes.contains(v)) {
                            if (!shift) {
                                if (!viewer.getSelected(v)) {
                                    toSelectNodes.add(v);
                                    changed = true;
                                }
                                if (click)
                                    break;
                            } else // shift==true
                            {
                                if (!viewer.getSelected(v)) {
                                    toSelectNodes.add(v);
                                } else //
                                    toDeselectNodes.add(v);
                                changed = true;
                            }
                        }
                    }
                }

                if ((click || viewer.isAllowRubberbandEdges()) && hitEdges.size() > 0) {
                    for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
                        if (hitEdges.contains(e)) {
                            if (!shift) {
                                if (!click || viewer.getNumberSelectedNodes() == 0) {
                                    if (!viewer.getSelected(e))
                                        toSelectEdges.add(e);
                                    changed = true;
                                }
                                if (click)
                                    break;
                            } else // shift==true
                            {
                                if (!viewer.getSelected(e))
                                    toSelectEdges.add(e);
                                else
                                    toDeselectEdges.add(e);
                                changed = true;
                            }
                        }
                    }
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            } finally {
                if (changed) {
                    if (toSelectNodes.size() > 0) {
                        viewer.selectedNodes.addAll(toSelectNodes);
                        viewer.fireDoSelect(toSelectNodes);
                    }
                    if (toDeselectNodes.size() > 0) {
                        viewer.selectedNodes.removeAll(toDeselectNodes);
                        viewer.fireDoDeselect(toDeselectNodes);
                    }
                    if (toSelectEdges.size() > 0) {
                        viewer.selectedEdges.addAll(toSelectEdges);
                        viewer.fireDoSelect(toSelectEdges);
                    }
                    if (toDeselectEdges.size() > 0) {
                        viewer.selectedEdges.removeAll(toDeselectEdges);
                        viewer.fireDoDeselect(toDeselectEdges);
                    }
                    viewer.repaint();
                }
            }
        }
    }

    // KeyListener methods:

    /**
     * Key typed
     *
     * @param ke Keyevent
     */
    public void keyTyped(KeyEvent ke) {
    }

    static boolean warnedCannotUndo = false;

    /**
     * Key pressed
     *
     * @param ke KeyEvent
     */
    public void keyPressed(KeyEvent ke) {
        int r = 1; // rotate angle
        double s = 1.05; // scale factor
        if ((ke.getModifiers() & InputEvent.SHIFT_MASK) != 0
                && (ke.getModifiers() & InputEvent.CTRL_MASK) != 0) {
            s = 4;
        } else if ((ke.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            s = 1.5;
            r = 10;
        } else if ((ke.getModifiers() & InputEvent.CTRL_MASK) != 0) {
            s = 2;
            r = 100;
        }
        boolean circular = (viewer.getLayoutType().equals(Network.CIRCULAR));

        if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
            if (circular) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                double angle = viewer.trans.getAngle() - r * Math.PI / 100.0;
                viewer.trans.setAngle(angle);
                spa.adjust(true, true);
                // final ICommand cmd = new RotateCommand(viewer, viewer.trans, angle);
                //new Edit(cmd, "rotate left").execute(viewer.getUndoSupportNetwork());
            } else   // zoom rectilinear
            {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                viewer.trans.composeScale(1.0 / s, 1);
                spa.adjust(false, true);
                //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, 1.0 / s, 1);
                //new Edit(cmd, "Zoom In").execute(viewer.getUndoSupportNetwork());
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
            if (circular) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                double angle = viewer.trans.getAngle() + r * Math.PI / 100.0;
                viewer.trans.setAngle(angle);
                spa.adjust(true, true);
                //final ICommand cmd = new RotateCommand(viewer, viewer.trans, angle);
                //new Edit(cmd, "rotate right").execute(viewer.getUndoSupportNetwork());
            } else   // zoom rectilinear
            {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                viewer.trans.composeScale(s, 1);
                spa.adjust(true, false);

                //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, s, 1);
                //new Edit(cmd, "Zoom Out").execute(viewer.getUndoSupportNetwork());
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
            ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
            double f = circular ? 1.0 / s : 1.0;
            viewer.trans.composeScale(f, 1.0 / s);
            spa.adjust(f != 1.0, true);

            //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, f, 1.0 / s);
            //new Edit(cmd, "Zoom In").execute(viewer.getUndoSupportNetwork());
        } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
            ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
            double f = circular ? s : 1.0;
            viewer.trans.composeScale(f, s);
            spa.adjust(f != 1.0, true);
            //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, f, s);
            //new Edit(cmd, "zoom Out").execute(viewer.getUndoSupportNetwork());
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
            ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
            viewer.trans.composeScale(1.0 / s, 1.0 / s);
            spa.adjust(true, true);
            //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, 1.0 / s, 1.0 / s);
            //new Edit(cmd, "zoom In").execute(viewer.getUndoSupportNetwork());
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
            viewer.trans.composeScale(s, s);
            spa.adjust(true, true);
            //final ICommand cmd = new ZoomCommand(viewer, viewer.trans, s, s);
            //new Edit(cmd, "zoom Out").execute(viewer.getUndoSupportNetwork());
        } else if (viewer.getAllowEdit() && (ke.getKeyCode() == KeyEvent.VK_DELETE || ke.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            if (!warnedCannotUndo) {
                warnedCannotUndo = true;
                new Alert("Can't undo deletions");
                viewer.getUndoManagerNetwork().discardAllEdits();
                viewer.getActions().updateUndo();
                viewer.getActions().updateRedo();
            }
            viewer.delSelectedEdges();
            viewer.delSelectedNodes();
            viewer.repaint();
        } else if ((ke.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            viewer.setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Key released
     *
     * @param ke KeyEvent
     */
    public void keyReleased(KeyEvent ke) {
        if ((ke.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            viewer.resetCursor();
        }
    }

    // ComponentListener methods:

    /**
     * component hidded
     *
     * @param ev ComponentEvent
     */
    public void componentHidden(ComponentEvent ev) {
    }

    /**
     * component moved
     *
     * @param ev ComponentEvent
     */
    public void componentMoved(ComponentEvent ev) {
    }

    /**
     * component resized
     *
     * @param ev ComponentEvent
     */
    public void componentResized(ComponentEvent ev) {
        viewer.setSize(viewer.getSize());
    }

    /**
     * component shown
     *
     * @param ev ComponentEvent
     */
    public void componentShown(ComponentEvent ev) {
    }

    /**
     * If edge lengths can be maintained in user interaction, returns
     * the length of any edge connecting the selected from the non-selected
     * nodes. Otherwise, returns -1
     * We can maintain edge lengths if every edge in the set of edges that
     * separate the selected from the none-selected nodes
     * has the same angle and length
     *
     * @return firstlength double
     */

    private double canMaintainEdgeLengths() {
        PhyloSplitsGraph G = viewer.getPhyloGraph();
        boolean first = true;
        double firstAngle = 0;
        double firstLength = 0;

        for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
            if (G.getSplit(e) != -1) {
                Node v = G.getSource(e);
                Node w = G.getTarget(e);
                Point2D pv;
                Point2D pw;
                if (viewer.getSelectedNodes().contains(v) && !viewer.getSelectedNodes().contains(w)) {
                    pv = viewer.getLocation(v);
                    pw = viewer.getLocation(w);
                } else if (!viewer.getSelectedNodes().contains(v) && viewer.getSelectedNodes().contains(w)) {
                    pv = viewer.getLocation(w);
                    pw = viewer.getLocation(v);
                } else
                    continue;
                Point2D q = new Point2D.Double(pw.getX() - pv.getX(), pw.getY() - pv.getY());
                double angle = Geometry.computeAngle(q);
                double length = pv.distance(pw);
                if (first) {
                    firstAngle = angle;
                    firstLength = length;
                    first = false;
                } else // compare with first line
                {
                    if ((Math.abs(angle - firstAngle) > 0.01
                            && Math.abs(angle - firstAngle - 6.28318530717958647692) > 0.01)
                            || Math.abs(length - firstLength) > 0.01 * firstLength)
                        return -1;
                }
            }
        }
        return firstLength;
    }

    /**
     * Recompute coordinates so that edge lengths are maintained
     * Assumes canMaintainEdgeLengths returned true!
     *
     * @param origLength double
     */
    private void maintainEdgeLengths(double origLength) {
        PhyloSplitsGraph G = viewer.getPhyloGraph();
        NodeSet visited = new NodeSet(G);

        double length = -1;
        Point2D diff = null;

        // put all selected nodes into visited set:
        for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v))
            if (viewer.getSelectedNodes().contains(v))
                visited.add(v);

        for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
            if (G.getSplit(e) != -1) {
                Node v = G.getSource(e);
                Node w = G.getTarget(e);
                Node z;
                Point2D pv;
                Point2D pw;
                if (viewer.getSelectedNodes().contains(v) && !viewer.getSelectedNodes().contains(w)
                        && !visited.contains(w)) {
                    pv = viewer.getLocation(v);
                    pw = viewer.getLocation(w);
                    z = w;
                } else if (!viewer.getSelectedNodes().contains(v) && viewer.getSelectedNodes().contains(w)
                        && !visited.contains(v)) {
                    pv = viewer.getLocation(w);
                    pw = viewer.getLocation(v);
                    z = v;
                } else
                    continue;

                if (length == -1) // use first edge to define diff
                {
                    length = pv.distance(pw);

                    if (Math.abs(length - origLength) < 0.001 * length)
                        return; // no change of length, return

                    diff = new Point2D.Double((length - origLength) * (pw.getX() - pv.getX()) / length,
                            (length - origLength) * (pw.getY() - pv.getY()) / length);
                }
                shiftAllNodesRecursively(G, z, diff, visited);
            }
        }
    }

    /**
     * recursively shifts all nodes necessary to maintain edge lengths
     *
     * @param G       Graph
     * @param v       Node
     * @param diff    Point2D
     * @param visited NodeSet
     */
    private void shiftAllNodesRecursively(PhyloSplitsGraph G, Node v, Point2D diff, NodeSet visited) {
        if (!visited.contains(v)) {
            viewer.setLocation(v, viewer.getLocation(v).getX() - diff.getX(),
                    viewer.getLocation(v).getY() - diff.getY());
            visited.add(v);
            for (Edge f = G.getFirstAdjacentEdge(v); f != null; f = G.getNextAdjacentEdge(f, v)) {
                Node w = G.getOpposite(v, f);
                shiftAllNodesRecursively(G, w, diff, visited);
            }
        }
    }

    /**
     * react to a mouse wheel event
     *
     * @param e
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            boolean circular = (viewer.getLayoutType().equals(Network.CIRCULAR));

            boolean doScrollVertical = !e.isMetaDown() && e.isAltDown() && !e.isShiftDown();
            boolean doScrollHorizontal = !e.isMetaDown() && e.isAltDown() && e.isShiftDown();
            boolean doScaleVertical = !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown();
            boolean doScaleHorizontal = !e.isMetaDown() && !e.isAltDown() && e.isShiftDown();

            boolean useMag = viewer.trans.getMagnifier().isActive();
            viewer.trans.getMagnifier().setActive(false);

            if (doScrollVertical) { //scroll
                viewer.getScrollPane().getVerticalScrollBar().setValue(viewer.getScrollPane().getVerticalScrollBar().getValue() + e.getUnitsToScroll());
            } else if (doScaleVertical && !circular) { //scale
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());
                double toScroll = 1.0 + (e.getUnitsToScroll() / 100.0);
                double s = (toScroll > 0 ? 1.0 / toScroll : toScroll);
                double scale = s * viewer.trans.getScaleY();
                if (scale >= GraphView.YMIN_SCALE && scale <= GraphView.YMAX_SCALE) {
                    viewer.trans.composeScale(1, s);
                    viewer.repaint();
                    spa.adjust(false, true);
                }

            } else if (doScrollHorizontal) {
                viewer.getScrollPane().getHorizontalScrollBar().setValue(viewer.getScrollPane().getHorizontalScrollBar().getValue() + e.getUnitsToScroll());
            } else if (doScaleHorizontal && !circular) { //scale
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());
                double toScroll = 1.0 + (e.getUnitsToScroll() / 100.0);
                double s = (toScroll > 0 ? 1.0 / toScroll : toScroll);
                double scale = s * viewer.trans.getScaleX();

                if (scale >= GraphView.XMIN_SCALE && scale <= GraphView.XMAX_SCALE) {
                    viewer.trans.composeScale(s, 1);
                    viewer.repaint();
                    spa.adjust(true, false);
                }
            } else if (doScaleHorizontal && circular) { //rotate
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());
                double diff = e.getUnitsToScroll() * Math.PI / 1000.0;
                if (viewer.trans.getFlipH() != viewer.trans.getFlipV())
                    diff = -diff;
                double angle = viewer.trans.getAngle() - diff;
                viewer.trans.setAngle(angle);
                viewer.repaint();
                spa.adjust(true, true);
            } else if ((doScaleVertical || doScaleHorizontal) && circular) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());

                double toScroll = 1.0 + (e.getUnitsToScroll() / 100.0);
                double s = (toScroll > 0 ? 1.0 / toScroll : toScroll);
                double scaleX = s * viewer.trans.getScaleX();
                double scaleY = s * viewer.trans.getScaleY();
                if (scaleX >= GraphView.XMIN_SCALE && scaleX <= GraphView.XMAX_SCALE && scaleY >= GraphView.YMIN_SCALE && scaleY <= GraphView.YMAX_SCALE) {
                    viewer.trans.composeScale(s, s);
                    viewer.repaint();
                    spa.adjust(true, true);
                }
            }
            viewer.trans.getMagnifier().setActive(useMag);
        }
    }
}

// EOF
