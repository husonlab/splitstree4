/**
 * NetworkUtilities.java
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
 *
 * @version $Id: NetworkUtilities.java,v 1.8 2010-02-01 16:16:39 huson Exp $
 * @author Daniel Huson and David Bryant
 * @version $Id: NetworkUtilities.java,v 1.8 2010-02-01 16:16:39 huson Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: NetworkUtilities.java,v 1.8 2010-02-01 16:16:39 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.util;

import jloda.graph.*;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Alert;
import jloda.swing.util.Geometry;
import jloda.util.Basic;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;

import java.awt.geom.Point2D;
import java.util.Random;

/**
 * utilities for networks
 * Daniel Huson and David Bryant
 */
public class NetworkUtilities {

    /**
     * computes an embedding for a network. Does so by first choosing a spanning tree,
     * then computing a tree embedding and then finally, doing some spring embedding
     *
     * @param taxa
     * @param network
     */
    public static void computeEmbedding(Taxa taxa, Network network, int iterations) {
        System.err.println("Embedding graph");
        PhyloGraphView graphView = new PhyloGraphView();
        network.syncNetwork2PhyloGraphView(taxa, null, graphView);

        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        if (ConnectedComponents.count(graph) > 1) {
            new Alert("Given network is not connected, can't embed");
            return;
        }

        // select spanning tree:
        NodeIntArray components = new NodeIntArray(graph);
        int count = 0;
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            components.set(v, ++count);
        }

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (components.get(e.getSource()) != components.get(e.getTarget())) {
                renumberComponent(graph, graphView, e.getSource(), null, components.get(e.getTarget()), components);
                graphView.setSelected(e, true);
            }
        }

        // compute layout of selected tree
        embed(graph, graphView);

        graphView.computeSpringEmbedding(iterations, true);

        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            Point2D location = graphView.getLocation(v);
            network.getVertices()[graph.getId(v)].setX((float) location.getX());
            network.getVertices()[graph.getId(v)].setY((float) location.getY());
        }
    }


    /**
     * renumber component
     *
     * @param graph
     * @param view
     * @param v
     * @param e
     * @param number
     * @param components
     */
    private static void renumberComponent(PhyloSplitsGraph graph, PhyloGraphView view, Node v,
                                          Edge e, int number, NodeIntArray components) {
        components.set(v, number);
        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = f.getNextIncidentTo(v)) {
            if (f != e && view.getSelected(f) && components.get(v.getOpposite(f)) != number)
                renumberComponent(graph, view, v.getOpposite(f), f, number, components);
        }
    }

    /**
     * Embeds the tree in linear time.
     */
    private static void embed(PhyloSplitsGraph graph, PhyloGraphView view) {
        if (graph.getNumberOfNodes() == 0)
            return;

        Node root = graph.getFirstNode();
        NodeSet leaves = new NodeSet(graph);
        int rootDegree = 0;

        try {
            for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
                int selectedDegree = 0;
                for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                    if (view.getSelected(e))
                        selectedDegree++;
                }
                if (selectedDegree == 1)
                    leaves.add(v);
                if (selectedDegree > rootDegree) {
                    root = v;
                    rootDegree = selectedDegree;
                }
            }
            System.err.println("leaves: " + leaves.size());

            // recursively visit all nodes in the tree and determine the
            // angle 0-2PI of each edge. nodes are placed around the unit
            // circle at position
            // n=1,2,3,... and then an edge along which we visited nodes
            // k,k+1,...j-1,j is directed towards positions k,k+1,...,j

            EdgeDoubleArray angle = new EdgeDoubleArray(graph); // angle of edge
            Random rand = new Random();
            rand.setSeed(1);
            int seen = setAnglesRec(graph, view, 0, root, null, leaves, angle, rand);

            if (seen != leaves.size())
                System.err.println("Warning: Number of nodes seen: " + seen +
                        " != Number of leaves: " + leaves.size());

            // recursively compute node coordinates from edge angles:
            view.setLocation(root, new Point2D.Double(0, 0));
            setCoordsRec(graph, view, root, null, angle);
        } catch (NotOwnerException ex) {
            Basic.caught(ex);
        }
    }

    /**
     * Recursively determines the angle of every tree edge.
     *
     * @param num    int
     * @param root   Node
     * @param entry  Edge
     * @param leaves NodeSet
     * @param angle  EdgeDoubleArray
     * @param rand   Random
     * @return b int
     */

    private static int setAnglesRec(PhyloSplitsGraph graph, PhyloGraphView view, int num, Node root, Edge entry, NodeSet leaves, EdgeDoubleArray angle, Random rand) {
        if (leaves.contains(root))
            return num + 1;
        else {
            int a = num; // is number of nodes seen so far
            int b = 0;     // number of nodes after visiting subtree

            for (Edge e : root.adjacentEdges()) {
                if (e != entry && view.getSelected(e)) {
                    b = setAnglesRec(graph, view, a, graph.getOpposite(root, e), e, leaves, angle, rand);

                    // point towards the segment of the unit circle a...b:
                    angle.put(e, Math.PI * (a + b) / leaves.size());

                    a = b;
                }
            }
            return b;
        }
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param root  Node
     * @param entry Edge
     * @param angle EdgeDouble
     */

    static private void setCoordsRec(PhyloSplitsGraph graph, PhyloGraphView view, Node root, Edge entry, EdgeDoubleArray angle) {
        for (Edge e : root.adjacentEdges()) {
            if (e != entry && view.getSelected(e)) {
                Node v = graph.getOpposite(root, e);

                // translate in the computed direction by the given amount
                view.setLocation(v,
                        Geometry.translateByAngle(view.getLocation(root), angle.getDouble(e), 1));

                setCoordsRec(graph, view, v, e, angle);
            }
        }
    }
}

// EOF
