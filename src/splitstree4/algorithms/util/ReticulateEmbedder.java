/*
 * ReticulateEmbedder.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util;

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.EdgeDoubleArray;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Geometry;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Queue;
import java.util.*;

/**
 * New algorithms for computing embeddings of reticulate networks
 * Daniel Huson and David Bryant, 4.2007
 */
public class ReticulateEmbedder {
    /**
     * computes a rectangular phylogram embedding
     *
	 */
    public void computeRectangularPhylogram(Taxa taxa, Splits splits, PhyloGraphView graphView,
                                            int outgroupIndex, boolean useSplitWeights, boolean useEdgeWeights,
                                            int percentOffset, boolean reticulateEdgesCubic,
                                            boolean treeEdgesCubic) {

        boolean shake = true;

        double smallDistance = 0;
        for (int s = 1; s <= splits.getNsplits(); s++)
            smallDistance = Math.max(smallDistance, splits.getWeight(s));
        smallDistance = (percentOffset / 100.0) * smallDistance;

        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        Node root = graph.getTaxon2Node(outgroupIndex);
        if (root.getDegree() > 0)
            root = root.getFirstAdjacentEdge().getOpposite(root);

        int ntax = taxa.getNtax();

        int[] cycle = determineCycle(splits, outgroupIndex, ntax);
        int[] cycleInverse = new int[ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            cycleInverse[cycle[i]] = i;
        }

        // ensure that all edges are oriented away from root
        // todo: assume that reticulate edges have split label -1 and are correctly oriented
        Set toFlip = new HashSet();
        determineEdgesToFlip(root, null, graph, toFlip);
        for (Object aToFlip : toFlip) {
            Edge e = (Edge) aToFlip;
            Edge f = graph.newEdge(e.getTarget(), e, e.getSource(), e, Edge.AFTER, Edge.AFTER, null);
            graph.setSplit(f, graph.getSplit(e));
            graph.setWeight(f, graph.getWeight(e));
            graph.setConfidence(f, graph.getConfidence(e));
            // graphView.setColor(f,Color.RED);

            graph.deleteEdge(e);
        }

        // recursively compute the edge 2 cluster map
        EdgeArray edge2cluster = new EdgeArray(graph);
        computeEdge2ClusterRec(root, graph, edge2cluster);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == 0) {
                TaxaSet cluster = (TaxaSet) edge2cluster.get(e);
                if (cluster != null) {
                    for (int s = 1; s <= splits.getNsplits(); s++) {
                        if (splits.get(s).equalsAsSplit(cluster, ntax)) {
                            graph.setSplit(e, s);
                            break;
                        }
                    }
                }
            }
        }

        Random rand = new Random();

        // map splits to heights, used in rectangular drawing:
        Map cluster2height = new HashMap();

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            int s = graph.getSplit(e);
            if (s >= 0) {
                TaxaSet cluster = (TaxaSet) edge2cluster.get(e);
                double height = computeHeight(cluster, cycleInverse, ntax);
                if (shake)
                    height += 0.5 * (rand.nextFloat() - 0.5) * smallDistance;

                cluster2height.put(cluster, height);
            }
        }

        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLocation(v, null);
        }

        // assign coordinates:
        List<Node> queue = new ArrayList<>();
        queue.add(root);
        while (queue.size() > 0) // breath-first assignment
        {
			Node v = queue.remove(0); // pop

            boolean ok = true;
            if (v.getInDegree() == 1) // is regular in edge
            {
                Edge e = v.getFirstInEdge();
                Node w = e.getSource();
                int splitId = graph.getSplit(e);
                if (splitId >= 0) {
                    Point2D location = graphView.getLocation(w);

                    if (location == null) // can't process yet
                    {
                        ok = false;
                    } else {
                        double weight;
                        if (splitId > 0) {
                            if (useSplitWeights)
                                weight = splits.getWeight(splitId);
                            else if (useEdgeWeights)
                                weight = graph.getWeight(e);
                            else
                                weight = 1;
                        } else
                            weight = smallDistance;
                        double y = (Double) cluster2height.get(edge2cluster.get(e));
                        double x = location.getX() + weight;
                        if (shake)
                            x += 0.5 * (rand.nextFloat() - 0.5) * smallDistance;

                        graphView.setLocation(e.getTarget(), x, y);
                        List<Point2D> internalPoints = new ArrayList<>();
                        internalPoints.add(new Point2D.Double(location.getX(), graphView.getLocation(v).getY()));
                        graphView.setInternalPoints(e, internalPoints);
                    }
                } else
                    System.err.println("Warning: split-id=0");
            } else if (v.getInDegree() > 1) // all in edges are 'blue' edges
            {
                double x = 0;
                double y = 0;
                int count = 0;
                for (Edge e : v.inEdges()) {
					Node w = e.getSource();
					Point2D location = graphView.getLocation(w);
                    if (location == null) {
                        ok = false;
                    } else {
                        x += location.getX();
                        y += location.getY();
                    }
                    count++;
                }
                if (ok && count > 0) {
                    y /= count;
                    if (v.getOutDegree() == 1) // should always have a single out edge
                    {
                        Edge f = v.getFirstOutEdge();
                        y = (Double) cluster2height.get(edge2cluster.get(f));
                    } else // weird, if there is a cluster associated with the edge, use it
                    {
                        Edge e = v.getFirstInEdge();
                        TaxaSet cluster = (TaxaSet) edge2cluster.get(e);
                        if (cluster != null) {
                            y = computeHeight(cluster, cycleInverse, ntax);
                        }
                    }

                    x = graphView.getLocation(v.getFirstInEdge().getSource()).getX();
                    for (Edge f : v.inEdges()) {
                        Point2D apt = graphView.getLocation(f.getSource());
                        if (apt.getX() > x)
                            x = apt.getX();
                    }

                    x += smallDistance;
                    if (shake)
                        x += 0.5 * (rand.nextFloat() - 0.5) * smallDistance;


                    graphView.setLocation(v, x, y);
                    if (reticulateEdgesCubic) {
                        if (ok) {
                            for (Edge e : v.inEdges()) {
                                Node w = e.getSource();
                                Point2D location = graphView.getLocation(w);
                                List<Point2D> internalPoints = new ArrayList<>();
                                internalPoints.add(new Point2D.Double(location.getX(), graphView.getLocation(v).getY()));
                                graphView.setInternalPoints(e, internalPoints);
                            }
                        }
                    }
                }
            } else  // is root node
            {
                double height = computeHeight(taxa.getTaxaSet(), cycleInverse, ntax);
                graphView.setLocation(v, 0, height);
            }

            if (ok)  // add childern to end of queue:
            {
                for (Edge e : v.outEdges()) {
                    queue.add(e.getTarget());
                }
            } else  // process this node again later
                queue.add(v);
        }

        List toMove = new LinkedList();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == -1) {
                graphView.setColor(e, Color.BLUE);
                if (reticulateEdgesCubic)
                    graphView.setShape(e, EdgeView.CUBIC_EDGE);
                toMove.add(e);
            } else {
                if (treeEdgesCubic)
                    graphView.setShape(e, EdgeView.CUBIC_EDGE);
            }
        }
        for (ListIterator it = toMove.listIterator(toMove.size()); it.hasPrevious(); ) {
            graph.moveToFront((Edge) it.previous());
        }
        // System.err.println("bbox: " + graphView.getBBox());
    }

    /**
     * determine which edges need flipping
     *
	 */
    private void determineEdgesToFlip(Node v, Edge e, PhyloSplitsGraph graph, Set toFlip) {
        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            if (f != e) {
                if (graph.getSplit(f) == -1) {
                    if (v == f.getSource())
                        determineEdgesToFlip(f.getOpposite(v), f, graph, toFlip);
                } else // is split edge
                {
                    if (v == f.getTarget()) // needs to be flipped
                        toFlip.add(f);
                    determineEdgesToFlip(f.getOpposite(v), f, graph, toFlip);
                }
            }
        }

    }

    /**
     * recursively computes the edge 2 cluster mapping
     *
     * @return taxa on this node or below
     */
    private TaxaSet computeEdge2ClusterRec(Node v, PhyloSplitsGraph graph, EdgeArray edge2cluster) {
        TaxaSet cluster = new TaxaSet();
        for (Edge f : v.outEdges()) {
            TaxaSet below = computeEdge2ClusterRec(f.getOpposite(v), graph, edge2cluster);
            edge2cluster.put(f, below.clone());
            //graph.setLabel(f,Basic.toString(below.getBits()));
            cluster.or(below);
        }
        for (Integer t : graph.getTaxa(v)) {
            cluster.set(t);
        }
        return cluster;
    }

    /**
     * sets the cycle so that the outgroup is at the first position
     *
     * @return cycle with outgroup at first position
     */
    private int[] determineCycle(Splits splits, int outgroupIndex, int ntax) {
        int[] cycle = new int[ntax + 1];
        if (splits.getCycle() != null) {
            System.arraycopy(splits.getCycle(), 1, cycle, 1, ntax);
        } else {
            for (int i = 1; i <= ntax; i++)
                cycle[i] = i;
        }
        if (cycle[1] != outgroupIndex) // need to rotateAbout
        {
            int[] newCycle = new int[ntax + 1];
            int p = 1;
            for (int i = 1; i <= ntax; i++) {
                if (cycle[i] == outgroupIndex) {
                    p = i;
                    break;
                }
            }
            for (int q = 1; q <= ntax; q++) {
                newCycle[q] = cycle[p];
                if (p == ntax)
                    p = 1; // wrap around
                else
                    p++;
            }
            cycle = newCycle;
        }
        return cycle;
    }


    /**
     * computes the height of a cluster for the rectangular view
     *
     * @return height
     */
    private double computeHeight(TaxaSet cluster, int[] cycleInverse, int ntax) {
        int min = ntax;
        int max = 0;
        for (int t = 1; t <= ntax; t++)
            if (cluster.get(t)) {
                if (cycleInverse[t] < min)
                    min = cycleInverse[t];
                if (cycleInverse[t] > max)
                    max = cycleInverse[t];
            }
        return (min + max) / 2.0;
    }


    /**
     * computes a rectangular phylogram embedding
     *
	 */
    public void computeRectangularCladogram(Taxa taxa, Splits splits, PhyloGraphView graphView,
                                            int outgroupIndex, boolean reticulateEdgesCubic,
                                            boolean treeEdgesCubic) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        Node root = graph.getTaxon2Node(outgroupIndex);
        if (root.getDegree() > 0)
            root = root.getFirstAdjacentEdge().getOpposite(root);

        int ntax = taxa.getNtax();

        int[] cycle = determineCycle(splits, outgroupIndex, ntax);
        int[] cycleInverse = new int[ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            cycleInverse[cycle[i]] = i;
        }

        // ensure that all edges are oriented away from root
        // todo: assume that reticulate edges have split label -1 and are correctly oriented
        Set toFlip = new HashSet();
        determineEdgesToFlip(root, null, graph, toFlip);
        for (Object aToFlip : toFlip) {
            Edge e = (Edge) aToFlip;
            Edge f = graph.newEdge(e.getTarget(), e, e.getSource(), e, Edge.AFTER, Edge.AFTER, null);
            graph.setSplit(f, graph.getSplit(e));
            graph.setWeight(f, graph.getWeight(e));
            graph.setConfidence(f, graph.getConfidence(e));
            graph.deleteEdge(e);
        }

        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() == 0)   // is leaf, place on base line
            {
                double y;
                if (v.getInDegree() == 0)
                    y = 0;
                else {
                    TaxaSet cluster = new TaxaSet();
                    for (Integer t : graph.getTaxa(v)) {
                        cluster.set(t);
                    }
                    y = computeHeight(cluster, cycleInverse, ntax);
                }
                graphView.setLocation(v, 0, y);
            } else
                graphView.setLocation(v, null);
        }
        assignCoordinatesToRectangularCladogramRec(root, graph, graphView);
        if (reticulateEdgesCubic || treeEdgesCubic)
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
                if (graph.getSplit(e) < 0) {
                    if (reticulateEdgesCubic)
                        graphView.setShape(e, EdgeView.CUBIC_EDGE);
                } else {
                    if (treeEdgesCubic)
                        graphView.setShape(e, EdgeView.CUBIC_EDGE);
                }
            }

    }

    static final private int blackWeight = 2; // computation of height, give black edges higher weight

    /**
     * recursively assign coordinates to rectangular cladogram
     *
     * @return location of v
     */
    private Point2D assignCoordinatesToRectangularCladogramRec(Node v, PhyloSplitsGraph graph, PhyloGraphView graphView) {
        if (graphView.getLocation(v) == null) {
            double x = 0;
            double y = 0;
            int count = 0;
            for (Edge e : v.outEdges()) {
                Point2D location = assignCoordinatesToRectangularCladogramRec(e.getOpposite(v), graph, graphView);
                boolean blackEdge = (e.getTarget().getInDegree() == 1);
                x = Math.min(x, location.getX());
                y += (blackEdge ? blackWeight : 1) * location.getY();
                count += (blackEdge ? blackWeight : 1);
            }

            y /= count; // average height
            graphView.setLocation(v, x - 10, y);
            for (Edge e : v.outEdges()) {
                List list = new LinkedList();
                list.add(new Point2D.Double(graphView.getLocation(v).getX(),
                        graphView.getLocation(e.getOpposite(v)).getY()));
                graphView.setInternalPoints(e, list);
            }
        }
        return graphView.getLocation(v);
    }

    /**
     * computes a equal angle embedding
     *
     * @param splits          (really only need cycle from this)
	 */
    public void computeEqualAngle(Taxa taxa, Splits splits, PhyloGraphView graphView,
                                  int outgroupIndex, boolean useSplitWeights,
                                  boolean useEdgeWeights,
                                  int percentOffset, int maxAngle) {
        double smallDistance = 0;
        for (int s = 1; s <= splits.getNsplits(); s++)
            smallDistance = Math.max(smallDistance, splits.getWeight(s));
        smallDistance = (percentOffset / 100.0) * smallDistance;


        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        Node root = graph.getTaxon2Node(outgroupIndex);
        if (root.getDegree() > 0)
            root = root.getFirstAdjacentEdge().getOpposite(root);

        int ntax = taxa.getNtax();

        int[] cycle = determineCycle(splits, outgroupIndex, ntax);
        int[] cycleInverse = new int[ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            cycleInverse[cycle[i]] = i;
        }

        // ensure that all edges are oriented away from root
        // todo: assume that reticulate edges have split label -1 and are correctly oriented
        Set toFlip = new HashSet();
        determineEdgesToFlip(root, null, graph, toFlip);
        for (Object aToFlip : toFlip) {
            Edge e = (Edge) aToFlip;
            Edge f = graph.newEdge(e.getTarget(), e, e.getSource(), e, Edge.AFTER, Edge.AFTER, null);
            graph.setSplit(f, graph.getSplit(e));
            graph.setWeight(f, graph.getWeight(e));
            graph.setConfidence(f, graph.getConfidence(e));
            graph.deleteEdge(e);
        }

        // recursively compute the edge 2 cluster map
        EdgeArray edge2cluster = new EdgeArray(graph);
        computeEdge2ClusterRec(root, graph, edge2cluster);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == 0) {
                TaxaSet cluster = (TaxaSet) edge2cluster.get(e);
                if (cluster != null) {
                    for (int s = 1; s <= splits.getNsplits(); s++) {
                        if (splits.get(s).equalsAsSplit(cluster, ntax)) {
                            graph.setSplit(e, s);
                            break;
                        }
                    }
                }
            }
        }

        // set edge to angle:
        EdgeDoubleArray edge2angle = new EdgeDoubleArray(graph);
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) != -1) {
                edge2angle.put(e, computeAngle((TaxaSet) edge2cluster.get(e), cycleInverse, ntax, maxAngle));
            }
        }

        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLocation(v, null);
        }
        // assign coordinates:
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) // breath-first assignment
        {
            Node v = queue.poll();

            boolean ok = true;
            if (v.getInDegree() == 1) // is regular in edge
            {
                Edge e = v.getFirstInEdge();
                Node w = e.getSource();
                int splitId = graph.getSplit(e);
                if (splitId >= 0) {
                    Point2D wLocation = graphView.getLocation(w);

                    if (wLocation == null) // can't process yet
                    {
                        ok = false;
                    } else {
                        double weight;
                        if (splitId > 0) {
                            if (useSplitWeights)
                                weight = splits.getWeight(splitId);
                            else if (useEdgeWeights)
                                weight = graph.getWeight(e);
                            else
                                weight = 1;
                        } else
                            weight = smallDistance;
                        double angle = edge2angle.getDouble(e);
                        Point2D vLocation = Geometry.translateByAngle(graphView.getLocation(w), angle, weight);
                        graphView.setLocation(v, vLocation);
                    }
                } else
                    System.err.println("Warning: split-id=0");
            } else if (v.getInDegree() > 1) // all in edges are 'blue' edges
            {
                double x = 0;
                double y = 0;
                int count = 0;
                for (Edge e : v.inEdges()) {
                    Node w = e.getSource();
                    Point2D location = graphView.getLocation(w);
                    if (location == null) {
                        ok = false;
                    } else {
                        x += location.getX();
                        y += location.getY();
                    }
                    count++;
                }
                if (ok && count > 0) {
                    x /= count;
                    y /= count;
                    Point2D vLocation = new Point2D.Double(x, y);
                    if (v.getOutDegree() == 1) // should always have a single out edge
                    {
                        Edge f = v.getFirstOutEdge();
                        double angle = edge2angle.getDouble(f);
                        vLocation = Geometry.translateByAngle(vLocation, angle, smallDistance);
                    }

                    graphView.setLocation(v, vLocation);
                }
            } else  // is root node
            {
                graphView.setLocation(v, 0, 0);
            }

            if (ok)  // add childern to end of queue:
            {
                for (Edge e : v.outEdges()) {
                    queue.add(e.getTarget());
                }
            } else  // process this node again later
                queue.add(v);
        }

        List<Edge> toMove = new ArrayList<>();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == -1) {
                graphView.setColor(e, Color.BLUE);
                toMove.add(e);
            } else {
                //  graphView.setLineWidth(e,2);
            }
        }
        for (ListIterator it = toMove.listIterator(toMove.size()); it.hasPrevious(); ) {
            graph.moveToFront((Edge) it.previous());
        }
    }


    /**
     * computes the angle of a cluster
     *
     * @return angle
     */
    private double computeAngle(TaxaSet cluster, int[] cycleInverse, int ntax, int maxAngle) {
        int min = ntax;
        int max = 0;
        for (int t = 1; t <= ntax; t++)
            if (cluster.get(t)) {
                if (cycleInverse[t] < min)
                    min = cycleInverse[t];
                if (cycleInverse[t] > max)
                    max = cycleInverse[t];
            }
        double part = maxAngle / 360.0;
        return (part * Math.PI * (min + max)) / ntax - (0.5 + part) * Math.PI;
    }


}
