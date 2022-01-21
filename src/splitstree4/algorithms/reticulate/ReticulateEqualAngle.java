/*
 * ReticulateEqualAngle.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.reticulate;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Geometry;
import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Reticulate;
import splitstree4.nexus.Taxa;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Implements an extended version of the equal angle algorithm that can draw reticulate networks.
 */
public class ReticulateEqualAngle implements Reticulate2Network {
    private boolean verbose = false;

    private double deltaAngle = 15.0;
    private boolean useWeights = true;
    public final static String DESCRIPTION = "Extended Equal Angle Algorithm for Reticulate Networks (Kloepper and Huson, 2007)";
    private double maxAngle = 180;

    static final String greedy = "Greedy";
    static final String perfect = "Perfect";
    private String optionSorting = greedy;


    private HashMap rNode2ReticulationNodeData;


    public boolean isApplicable(Document doc, Taxa taxa, Reticulate ret) {
        return ret.isValid();
    }

    public Network apply(Document doc, Taxa taxa, Reticulate ret) throws Exception {
        if (verbose) System.out.println("\n\nStarting draw");

        rNode2ReticulationNodeData = new HashMap();
        PhyloSplitsGraph graph = ret.getReticulateNetwork();
        Node root = ret.getRoot();
        PhyloGraphView graphView = new PhyloGraphView(graph);
        // reticulation nodes mapped onto the depending tree Nodes
        // build a dependency graph for the reticulations
        // mpas the nodes of the dependency Graph back to the reticulation nodes

        HashMap depRetNode2rNode = new HashMap();
        buildReticulationDependencyGraph(graph, depRetNode2rNode);
     /*
        System.out.println("dependency graph: ");
        Iterator itO = depRet.nodes().iterator();
        while (itO.hasNext()) System.out.println("Node: " + itO.next());
        System.out.println("-----------------------------------------");
        itO = depRet.edgeIterator();
        while (itO.hasNext()) System.out.println("Edge: " + itO.next());
      */
        // each map has a node as key and a BitSet of the size of taxa as a value
        if (verbose) System.out.println("\nstart buttom up:");
        if (verbose) System.out.print("\nstart find imaginary edges:");
        // for the imaginaryEdges we need to know which is the shortrest reticulation cycle for a reticulation and
        // which node in the cycle is closest to the root. The key is the reticulation edge and the value are two edges.
        // These edges are the startpoints for the path from source of imaginary edge to p bzw. q.
        makeImaginaryEdges(graph, root);

        HashMap parent2rNodes = new HashMap();
        Iterator it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            //System.out.println("\n" + rNodeData + "\n");
            Node parent = rNodeData.getParent();
            if (parent2rNodes.get(parent) == null) parent2rNodes.put(parent, new HashSet());
            ((HashSet) parent2rNodes.get(parent)).add(rNode);
        }

        HashMap node2AngleSize = new HashMap();
        // contains all nodes that are decendants of the key
        HashMap node2SubNodes = new HashMap();
        // init stuff
        it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getDegree() == 1) {
                node2AngleSize.put(n, 1);
            }
        }
        if (verbose) System.out.println("\ncalculating sub nodes");
        recButtomUpSubNodes(root, node2SubNodes, parent2rNodes);
      /*  it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            System.out.println("Node: " + n + "\tsubNodes: " + node2SubNodes.get(n));
        } */
        if (verbose) System.out.println("\ncalculating sub leafs");
        recButtomUpSubLeafs(root, node2AngleSize, parent2rNodes);
       /* it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            System.out.println("Node: " + n + "\tsubLeafs: " + node2AngleSize.get(n));
        }*/
        // maps those rNodes to a nodes, that have the node in their path to the GMRCA ( either through p or q)
        HashMap nodes2rNodes = getActiveRNodesForEdges(graph, rNode2ReticulationNodeData, parent2rNodes);

        if (verbose) System.out.println("\nstart top down:");
        recTopDownLabelNodes(graphView, graph, root, 0.0, taxa.getNtax(), node2AngleSize, node2SubNodes, new HashSet(), parent2rNodes, nodes2rNodes);
        if (verbose) System.out.println("DONE!");

        // place rNodes
        //optimizeRNodePositions(graphView, depRet,depRetNode2rNode);

        // and paint retiuclation edges blue
        for (var e : graph.edges()) {
            if (e.getTarget().getInDegree() == 2) graphView.setColor(e, Color.blue);
        }
        // finally add imaginary Edges so we can see them
        it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            //graphView.setColor(graph.newEdge(rNode, rNodeData.getParent()), Color.red);
        }

        if (false) {
            for (Node n : graph.nodes()) {
                graph.setLabel(n, n + "\tnLeafs:" + node2AngleSize.get(n));
            }
        }
        graphView.resetViews();
        return new Network(taxa, graphView);
    }


    private HashMap getActiveRNodesForEdges(PhyloSplitsGraph graph, HashMap<Node, Set<Node>> rNode2ReticulationNodeData, HashMap parent2rNodes) {
        HashMap<Node, Set<Node>> nodes = new HashMap<>();
        Iterator<Node> it = graph.nodes().iterator();

        while (it.hasNext())
            nodes.put(it.next(), new HashSet<>());

        it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            // find pathes from p and q to parent
            if (!rNodeData.getP().getSource().equals(rNodeData.getParent())) {
                LinkedList path2p = findMinPath2Ancestor(rNodeData.getP().getSource(), rNodeData.getParent(), parent2rNodes);
                rNodeData.setNode2p((Node) path2p.getFirst());
                rNodeData.setPathParent2P(path2p);
                Iterator itPath = path2p.iterator();
                Node ancestor = rNodeData.getParent();
                //System.out.println("found Path between: " + ancestor + "\tand " + rNodeData.getP().getSource() + "\t: " + path2p);
                while (itPath.hasNext()) {
                    Node decendant = (Node) itPath.next();
                    nodes.get(decendant).add(rNode);
                }
            } else {
                rNodeData.setNode2p(rNodeData.getParent());
            }
            if (!rNodeData.getQ().getSource().equals(rNodeData.getParent())) {
                LinkedList path2q = findMinPath2Ancestor(rNodeData.getQ().getSource(), rNodeData.getParent(), parent2rNodes);
                rNodeData.setNode2q((Node) path2q.getFirst());
                rNodeData.setPathParent2Q(path2q);
                Iterator itPath = path2q.iterator();
                Node ancestor = rNodeData.getParent();
                //System.out.println("found Path between: " + ancestor + "\tand " + rNodeData.getP().getSource() + "\t: " + path2q);
                while (itPath.hasNext()) {
                    Node decendant = (Node) itPath.next();
                    nodes.get(decendant).add(rNode);
                }
            } else {
                rNodeData.setNode2q(rNodeData.getParent());
            }
        }
        return nodes;
    }


    private void recButtomUpSubNodes(Node start, HashMap<Node, Set<Node>> node2SubNodes, HashMap<Node, Set<Node>> parent2rNodes) {
        Iterator<Node> it = start.adjacentNodes().iterator();
        HashSet<Node> start2SubNodes = new HashSet<>();
        // add tree stuff
        while (it.hasNext()) {
            Node next = (Node) it.next();
            // is decendant?
            Edge e = start.getCommonEdge(next);
            // is tree edge
            if (e.getSource().equals(start) && next.getInDegree() == 1) {
                // recursive if next is not allready known
                if (node2SubNodes.get(next) == null) {
                    //System.out.println("Recusion");
                    recButtomUpSubNodes(next, node2SubNodes, parent2rNodes);
                }
                // add node to SubNodes
                start2SubNodes.addAll(node2SubNodes.get(next));
            }
        }
        // add connected rNodes
        if (parent2rNodes.get(start) != null) {
            it = parent2rNodes.get(start).iterator();
            while (it.hasNext()) {
                Node next = (Node) it.next();
                if (node2SubNodes.get(next) == null) {
                    //System.out.println("Recusion");
                    recButtomUpSubNodes(next, node2SubNodes, parent2rNodes);
                }
                // add node to SubNodes
                start2SubNodes.addAll(node2SubNodes.get(next));
            }
        }
        start2SubNodes.add(start);
        //System.out.println("Node: " + start + "\t has subleafs: " + start2SubNodes);
        node2SubNodes.put(start, start2SubNodes);
    }

    private int recButtomUpSubLeafs(Node start, HashMap<Node, Integer> node2AngleSize, HashMap<Node, Set<Node>> parent2rNodes) {
        //System.out.println("\nnode: " + start + "\tsub: " + node2SubLeafs.get(start));
        int nLeafs = 0;

        if (node2AngleSize.containsKey(start)) nLeafs += (Integer) node2AngleSize.get(start);
        // add treenodes
        for (Node next : start.adjacentNodes()) {
            if (next.getInDegree() == 1 && next.getCommonEdge(start).getSource().equals(start)) {
                nLeafs += recButtomUpSubLeafs(next, node2AngleSize, parent2rNodes);
            }
        }
        // add reticulations
        if (parent2rNodes.get(start) != null) {
            for (Node next : parent2rNodes.get(start)) {
                nLeafs += recButtomUpSubLeafs(next, node2AngleSize, parent2rNodes);
            }
        }
        if (nLeafs == 0) nLeafs++;
        node2AngleSize.put(start, nLeafs);
        return nLeafs;
    }


    private void makeImaginaryEdges(PhyloSplitsGraph graph, Node root) {
        HashMap node2parent = new HashMap();
        Iterator it = graph.nodes().iterator();
        // find first common anceestor
        //System.out.println("making imaginary edges");
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getInDegree() == 2) {
                HashSet pathes = new HashSet();
                recFindPathesInOrgGraph2Ancestor(n, root, new LinkedList(), pathes);
                //System.out.println("Node:" + n + "\tparent" + root + "\tPathes: ");
                //Iterator tmp = pathes.iterator();
                //while (tmp.hasNext()) System.out.println(tmp.next());

                // take the first path and iterate through it
                LinkedList startPath = (LinkedList) pathes.iterator().next();
                pathes.remove(startPath);
                Iterator itPath = startPath.iterator();
                // first node is reticulation Node
                Node parent = (Node) itPath.next();
                if (itPath.hasNext())
                    parent = (Node) itPath.next();
                else if (verbose) System.out.println("path to short!"); // should contain at least one more node
                boolean foundParent = false;
                // find the first node in the start path that is contained in all other pathes
                while (!foundParent && parent != null) {
                    //System.out.println("parent: " + parent);
                    Iterator itPathes = pathes.iterator();
                    boolean cont = true;
                    while (cont && itPathes.hasNext()) {
                        if (!((LinkedList) itPathes.next()).contains(parent)) {
                            cont = false;
                        }
                    }
                    if (cont) {
                        //System.out.println("Found parent: " + parent);
                        foundParent = true;
                        node2parent.put(n, parent);
                    } else
                        parent = (Node) itPath.next();
                }
                if (!foundParent) System.err.println("unable to find Parent for node: " + n);
            }
        }
        it = node2parent.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            Node parent = (Node) node2parent.get(rNode);
            Edge p = null;
            Edge q = null;
            for (Edge e : rNode.inEdges()) {
                if (p == null)
                    p = e;
                else
                    q = e;
            }
            //System.out.println("Reticulation : " + rNode + "\tparent: " + parent + "\tp: " + p + "\tq: " + q);
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            rNodeData.setParent(parent);
            rNodeData.setP(p);
            rNodeData.setQ(q);
        }
    }


    /**
     * @param graphView
     * @param graph
     * @param start
     * @param startAngle
     * @param nTax
     * @return return a HashMap with key a node and vlaue a double[] with value at 0 is the start Angle of the tree or imaginary edge to the node  and value at 1 is the angle of the
     * tree or imaginary edge to the node
     * @throws Exception
     */
    private void recTopDownLabelNodes(PhyloGraphView graphView, PhyloSplitsGraph graph, Node start, double startAngle, int nTax, HashMap node2AngleSize, HashMap node2SubNodes, HashSet before, HashMap parent2rNodes, HashMap node2ActiverNodes) throws Exception {
        if (verbose) System.out.println("\nstart: " + start + "\t before: " + before);
        // define intervalls
        //make ordering graph ( the order in which the rNodes are dependent on this node)
        // I need the ordGraph for the perfect matching

        PhyloSplitsGraph ordGraph = new PhyloSplitsGraph();
        ArrayList orderedRNodes = new ArrayList();
        // are there reticulation nodes
        if (parent2rNodes.get(start) != null) {
            if (verbose)
                System.out.println("rNodes of parent: " + start + "\tare:" + parent2rNodes.get(start) + "\t  active rNodes: " + node2ActiverNodes.get(start));
            orderedRNodes = buildLocalWorkFlow((HashSet) parent2rNodes.get(start), ordGraph);
        }
        // the list of tree decendants of start
        ArrayList decendants = new ArrayList();
        decendants.addAll(orderedRNodes);
        for (Node n : start.adjacentNodes()) {
            if (n.getInDegree() == 1 && n.getCommonEdge(start).getSource().equals(start))
                decendants.add(n);
        }
        if (verbose) System.out.println("decendant tree nodes and ordered reticulations: " + decendants);

        // gives the distances for the scoring function of the ordering we make this once to save time
        // first are the decendants in order of the ArrayList decendants and last two entries are before and after.
        int[][] nodes2nConections = makeDistances4Edges(start, before, orderedRNodes, decendants, node2ActiverNodes);
        // some system out
        if (verbose) {
            for (int i = 0; i < nodes2nConections.length; i++) {
                if (i == nodes2nConections.length - 2) {
                    System.out.print("before:\t");
                } else if (i == nodes2nConections.length - 1) {
                    System.out.print("after:\t");
                } else
                    System.out.print(decendants.get(i) + ":\t");
                for (int j = 0; j < nodes2nConections.length; j++) System.out.print(nodes2nConections[i][j] + "\t");
                System.out.println();
            }
        }

        // place all reticulation nodes in the order given
        ArrayList ordList = new ArrayList();
        if (optionSorting.equalsIgnoreCase(greedy)) {
            buildOrdListGreedy(orderedRNodes, before, nodes2nConections, decendants, ordList);
        } else if (optionSorting.equalsIgnoreCase(perfect)) {
            ordList = buildOrdListWithBranchandBound(orderedRNodes, before, nodes2nConections, decendants, ordGraph);
        }
        if (verbose) System.out.println("orderd nodes: " + ordList);

        // recursive downward
        drawOrderedList(start, startAngle, graph, graphView, orderedRNodes, ordList, parent2rNodes, node2ActiverNodes, node2SubNodes, node2AngleSize, nTax, before);

    }


    /**
     * This method draws a list orf ordered decendants without respecting the weight on the reticulation edges
     *
     * @param start
     * @param startAngle
     * @param graph
     * @param graphView
     * @param orderedRNodes
     * @param ordList
     * @param parent2rNodes
     * @param node2ActiverNodes
     * @param node2SubNodes
     * @param node2AngleSize
     * @param nTax
     * @param before
     * @throws Exception
     */
    private void drawOrderedList(Node start, double startAngle, PhyloSplitsGraph graph, PhyloGraphView graphView, ArrayList orderedRNodes, ArrayList ordList, HashMap parent2rNodes, HashMap node2ActiverNodes, HashMap node2SubNodes, HashMap node2AngleSize, int nTax, HashSet before) throws Exception {
        // draw all subtrees we do this in order of the rNodes, such that we know that p and q of a reticulation have been drawn, before we draw the reticulation
        // thus allowing us to apply the placement of the node at once (either with or without weights. So first we have to add the rNodes with p and q and the the rest
        HashSet workedNodes = new HashSet();
        Iterator it = orderedRNodes.iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            Node node2Pathp = rNodeData.getNode2p();
            Node node2Pathq = rNodeData.getNode2q();
            // draw node2Pathp
            if (!workedNodes.contains(node2Pathp) && !node2Pathp.equals(rNodeData.getParent())) {
                // first define the angle to be added to startAngle
                //System.out.println("drawing: node2Pathp: " + node2Pathp + "\t subleafs: " + node2SubNodes.get(node2Pathp));
                double localStartAngle = 0.0;
                HashSet localBefore = new HashSet();
                Iterator it2 = ordList.iterator();
                boolean cont = true;
                while (cont && it2.hasNext()) {
                    Node n = (Node) it2.next();
                    if (!n.equals(node2Pathp)) {
                        localBefore.addAll((HashSet) node2SubNodes.get(n));
                        localStartAngle += maxAngle * (double) (Integer) node2AngleSize.get(n) / (double) nTax;
                    } else
                        cont = false;
                }
                localBefore.addAll(before);
                // draw node2Pathp
                drawNode(start, graphView.getLocation(start), node2Pathp, startAngle + localStartAngle, (Integer) node2AngleSize.get(node2Pathp), nTax, graph, graphView);
                // recursive into the subtree
                recTopDownLabelNodes(graphView, graph, node2Pathp, startAngle + localStartAngle, nTax, node2AngleSize, node2SubNodes, localBefore, parent2rNodes, node2ActiverNodes);
                workedNodes.add(node2Pathp);
            }
            // draw node2Pathq
            if (!workedNodes.contains(node2Pathq) && !node2Pathq.equals(rNodeData.getParent())) {
                // first define the angle to be added to startAngle
                //System.out.println("drawing: node2Pathq: " + node2Pathq);
                double localStartAngle = 0.0;
                HashSet localBefore = new HashSet();
                Iterator it2 = ordList.iterator();
                boolean cont = true;
                while (cont && it2.hasNext()) {
                    Node n = (Node) it2.next();
                    if (!n.equals(node2Pathq)) {
                        localBefore.addAll((HashSet) node2SubNodes.get(n));
                        localStartAngle += maxAngle * (double) (Integer) node2AngleSize.get(n) / (double) nTax;
                    } else
                        cont = false;
                }
                localBefore.addAll(before);
                // draw node2Pathq
                drawNode(start, graphView.getLocation(start), node2Pathq, startAngle + localStartAngle, (Integer) node2AngleSize.get(node2Pathq), nTax, graph, graphView);
                // recursive into the subtree
                recTopDownLabelNodes(graphView, graph, node2Pathq, startAngle + localStartAngle, nTax, node2AngleSize, node2SubNodes, localBefore, parent2rNodes, node2ActiverNodes);
                workedNodes.add(node2Pathq);
            }
            // draw reticulation
            // first define the angle to be added to startAngle
            //System.out.println("drawing: rNode: " + rNode);
            double localStartAngle = 0.0;
            HashSet localBefore = new HashSet();
            Iterator it2 = ordList.iterator();
            boolean cont = true;
            while (cont && it2.hasNext()) {
                Node n = (Node) it2.next();
                if (!n.equals(rNode)) {
                    localBefore.addAll((HashSet) node2SubNodes.get(n));
                    localStartAngle += maxAngle * (double) (Integer) node2AngleSize.get(n) / (double) nTax;
                } else
                    cont = false;
            }
            localBefore.addAll(before);
            // draw rNode
            drawNode(start, graphView.getLocation(start), rNode, startAngle + localStartAngle, (Integer) node2AngleSize.get(rNode), nTax, graph, graphView);
            placeReticulationWithNoWeight(graphView, rNodeData.getP().getSource(), rNodeData.getQ().getSource(), start, rNode);
            // recursive into the subtree
            recTopDownLabelNodes(graphView, graph, rNode, startAngle + localStartAngle, nTax, node2AngleSize, node2SubNodes, localBefore, parent2rNodes, node2ActiverNodes);
            workedNodes.add(rNode);
        }
        // add non reticulation dependend subnodes
        it = ordList.iterator();
        while (it.hasNext()) {
            Node work = (Node) it.next();
            if (!workedNodes.contains(work)) {
                //System.out.println("drawing: Node without weight: " + work + "\tstart: " + start + "\tloc: " + graphView.getLocation(start));
                double localStartAngle = 0.0;
                HashSet localBefore = new HashSet();
                Iterator it2 = ordList.iterator();
                boolean cont = true;
                while (cont && it2.hasNext()) {
                    Node n = (Node) it2.next();
                    if (!n.equals(work)) {
                        localBefore.addAll((HashSet) node2SubNodes.get(n));
                        localStartAngle += maxAngle * (double) (Integer) node2AngleSize.get(n) / (double) nTax;
                    } else
                        cont = false;
                }
                localBefore.addAll(before);
                // draw node2Pathp
                drawNode(start, graphView.getLocation(start), work, startAngle + localStartAngle, (Integer) node2AngleSize.get(work), nTax, graph, graphView);
                // recursive into the subtree
                recTopDownLabelNodes(graphView, graph, work, startAngle + localStartAngle, nTax, node2AngleSize, node2SubNodes, localBefore, parent2rNodes, node2ActiverNodes);
                workedNodes.add(work);
            }
        }
        //System.out.println("UP");
    }

    private void placeVerticalReticulationWithNoWeight(PhyloGraphView graphView, Point2D P, Point2D iP, Point2D rP, Node rNode) {
        // use sinussatz
        double alpha = Math.abs(Geometry.computeObservedAngle(iP, rP, P)) * 180 / Math.PI;
        //System.out.println("alpha: " + alpha);
        double sinGamma = Math.sin((180.0 - this.deltaAngle - alpha) / 180 * Math.PI);
        double sinBeta = Math.sin(this.deltaAngle / 180 * Math.PI);
        // calculate length of b
        double b = Point2D.distance(P.getX(), P.getY(), iP.getX(), iP.getY()) / sinGamma * sinBeta;
        //ystem.out.println("c: "+Point2D.distance(P.getX(), P.getY(), iP.getX(), iP.getY())+"\tb: "+b+"\talpßha:"+alpha+"\tsinGamma: "+sinGamma+"\tsinBeta: "+sinBeta);
        double newX = iP.getX() + (rP.getX() - iP.getX()) * b;
        double newY = iP.getY() + (rP.getY() - iP.getY()) * b;
        graphView.setLocation(rNode, newX, newY);
    }

    private void placeReticulationWithNoWeight(PhyloGraphView graphView, Node parent1, Node parent2, Node imaginaryParent, Node rNode) {
        Point2D P1 = graphView.getNV(parent1).getLocation();
        Point2D P2 = graphView.getNV(parent2).getLocation();
        Point2D P3 = graphView.getNV(imaginaryParent).getLocation();
        Point2D P4 = graphView.getNV(rNode).getLocation();

      /*System.out.println("parent1: [" + P1.getX() + "," + P1.getY() + "]");
        System.out.println("parent2: [" + P2.getX() + "," + P2.getY() + "]");
        System.out.println("GMRCA  : [" + P3.getX() + "," + P3.getY() + "]");
        System.out.println("rNode  : [" + P4.getX() + "," + P4.getY() + "]");
      */  // if this is a vertical reticulation
        if (parent1.equals(imaginaryParent)) {
            placeVerticalReticulationWithNoWeight(graphView, P2, P3, P4, rNode);
        } else if (parent2.equals(imaginaryParent)) {
            placeVerticalReticulationWithNoWeight(graphView, P1, P3, P4, rNode);
        } else {
            /**
             *  The placement is bvetween  the line frome parent1 to parent2 and the line from rNode to imaginaryParent  where we add some
             */
            // equations for a and b in (x1,y1) + (x2-x1, y2-y1) a  AND (x3,y3) + (x4-x3,y4-y3) b (Schnittpunkt)
            double konstanteA = (P3.getX() - P1.getX()) / (P2.getX() - P1.getX());
            double koeffizientA = (P4.getX() - P3.getX()) / (P2.getX() - P1.getX());
            double konstanteB = (P1.getY() - P3.getY()) / (P4.getY() - P3.getY());
            double koeffizientB = (P2.getY() - P1.getY()) / (P4.getY() - P3.getY());
            double a = (konstanteA + koeffizientA * konstanteB) / (1.0 - koeffizientA * koeffizientB);
            double b = (konstanteB + koeffizientB * a);
            //absolutes delta zurückgeben

            Point2D X = new Point2D.Double((P3.getX() + (P4.getX() - P3.getX()) * b), (P3.getY() + (P4.getY() - P3.getY()) * b));
            //System.out.println("Schnittpunkt: "+X);

            // choose P1 such that distP1New < dist P2New
            double distP1X = Point2D.distance(P1.getX(), P1.getY(), X.getX(), X.getY());
            double distP2X = Point2D.distance(P2.getX(), P2.getY(), X.getX(), X.getY());
            double distP3X = Point2D.distance(P3.getX(), P3.getY(), X.getX(), X.getY());
            double distP1P3 = Point2D.distance(P1.getX(), P1.getY(), P3.getX(), P3.getY());
            double distP2P3 = Point2D.distance(P2.getX(), P2.getY(), P3.getX(), P3.getY());

            double s1 = (distP1X + distP3X + distP1P3) / 2.0;
            double gamma1 = 2.0 * Math.asin(Math.sqrt((s1 - distP1X) * (s1 - distP3X) / (distP1X * distP3X))) * 180.0 / Math.PI;
            double beta1 = 180.0 - gamma1;
            double zeta1 = 180.0 - beta1 - deltaAngle;
            double f1 = distP1X * Math.sin(deltaAngle / 180.0 * Math.PI) / Math.sin(zeta1 / 180.0 * Math.PI);

            double s2 = (distP2X + distP3X + distP2P3) / 2.0;
            double gamma2 = 2.0 * Math.asin(Math.sqrt((s2 - distP2X) * (s2 - distP3X) / (distP2X * distP3X))) * 180.0 / Math.PI;
            double beta2 = 180.0 - gamma2;
            double zeta2 = 180.0 - beta2 - deltaAngle;
            double f2 = distP2X * Math.sin(deltaAngle / 180.0 * Math.PI) / Math.sin(zeta2 / 180.0 * Math.PI);

          /*  System.out.println("P1X: "+distP1X+"\tP2X: "+distP2X+"\tP3X: "+distP3X+"\tP1P3: "+distP1P3+"\tP2P3: "+distP2P3);
            System.out.println("s1: "+s1+"\tgamma1: "+gamma1+"\tbeta1: "+beta1+"\tzeta1: "+zeta1+"\tsin(daltaAngle): "
                    +Math.sin(deltaAngle/180.0*Math.PI)+"\tsin(zeta1): "+Math.sin(zeta1/180.0*Math.PI)+"\tf1: "+f1);
            System.out.println("s2: "+s2+"\tgamma2: "+gamma2+"\tbeta2: "+beta2+"\tzeta2: "+zeta2+"\tsin(daltaAngle): "
                    +Math.sin(deltaAngle/180.0*Math.PI)+"\tsin(zeta2): "+Math.sin(zeta2/180.0*Math.PI)+"\tf2: "+f2);
            */// deltaAngle is the max
            if (f1 < f2) {
                double stretch = (f1 + distP3X) / distP3X;
                graphView.setLocation(rNode, P3.getX() + (X.getX() - P3.getX()) * stretch, P3.getY() + (X.getY() - P3.getY()) * stretch);
                //System.out.println("f1: stretch: " + stretch + "\tnew Position: "+graphView.getLocation(rNode));
            } else {
                double stretch = (f2 + distP3X) / distP3X;
                graphView.setLocation(rNode, P3.getX() + (X.getX() - P3.getX()) * stretch, P3.getY() + (X.getY() - P3.getY()) * stretch);
                //System.out.println("f2: stretch: " + stretch + "\tnew Position: "+graphView.getLocation(rNode));
            }

        }
    }


    private double drawNode(Node start, Point2D startPosition, Node n, double startAngle, int nSubleafs, int nTax, PhyloSplitsGraph graph, PhyloGraphView graphView) {
        double addAngle = maxAngle * (double) nSubleafs / (double) nTax;
        double radian = (startAngle + 0.5 * addAngle) * Math.PI / 180.0;
        //System.out.println("working on node: " + n + "\tsubleafs: " + nSubleafs + "\tangle: " + startAngle + "\taddAngle: " + addAngle + "\tstartPosition: " + startPosition);
        if (n.getInDegree() == 1) {
            Edge e = n.getCommonEdge(start);
            Point2D p = translateByAngle(startPosition, radian, useWeights ? graph.getWeight(e) : 1.0);
            //System.out.println("TreeNode: \tedge: " + e + "\tweight: " + graph.getWeight(e) + "\tx: " + p.getX() + "\ty: " + p.getY());
            graphView.setLocation(n, p);
        } else {
            Point2D p = translateByAngle(startPosition, radian, 1.0);
            //System.out.println("ReticulationNode: \ttarget:  " + n + "\tweight: " + 1 + "\tx: " + p.getX() + "\ty: " + p.getY());
            graphView.setLocation(n, p);
        }
        // set re Map
        return (startAngle + 0.5 * addAngle);
    }


    private ArrayList bestOrdering;
    private int bound;

    private ArrayList buildOrdListWithBranchandBound(ArrayList orderedRNodes, HashSet before, int[][] nodes2nConections, ArrayList decendants, PhyloSplitsGraph ordGraph) {
        // first set bound
        bestOrdering = new ArrayList();
        this.bound = buildOrdListGreedy(orderedRNodes, before, nodes2nConections, decendants, bestOrdering);
        if (bound == 0)
            if (verbose) System.out.println("found perfect solution greedy so no BnB needed");
            else {
            /*
             // find the root
            Iterator it = ordGraph.nodes().iterator();
            Node root = null;
            while(it.hasNext() && root == null){
                Node n = (Node)it.next();
                if(n.getInDegree()==0) root = n;
            }
            if (root== null) throw new Exception("ReticulateNetworkDraw: Unable to find root in Ordering Graph:" );
            // for each decendants generate a Bit set of length two which values indicade the placement of pq for the ret Ndoes
            // that is the value is 10 if the place before is used for the node of p or q and 01 to indicate that p or q is behind
            HashMap node2UsedSide = new HashMap();
            it = orderedRNodes.iterator();
            while (it.hasNext()){
                Node n = (Node)it.next();
                node2UsedSide.put(n,new BitSet(2));
            }                              */
                ArrayList partialOrderings = new ArrayList();
                // generate the ordering with the rNodes
                ArrayList ordering = recGenerateOrderings(0, orderedRNodes, decendants, nodes2nConections, partialOrderings);

                // add all dependent nodes that are not element of the ordering

            }
        return bestOrdering;
    }

    /**
     * @param orderedRNodesIndex
     * @param orderedRNodes
     * @param decendants
     * @param nodes2nConections
     * @param partialOrdering
     * @return
     */

    private ArrayList recGenerateOrderings(int orderedRNodesIndex, ArrayList orderedRNodes, ArrayList decendants, int[][] nodes2nConections, ArrayList partialOrdering) {
        // first place p and q for rNode
        Node startRNode = (Node) orderedRNodes.get(orderedRNodesIndex);
        ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(startRNode);
        Node node2Pathp = rNodeData.getNode2p();
        Node node2Pathq = rNodeData.getNode2q();
        int ordIdp = partialOrdering.indexOf(node2Pathp);
        int ordIdq = partialOrdering.indexOf(node2Pathq);
        int ordIdrNode = partialOrdering.indexOf(startRNode);
        if (!node2Pathp.equals(rNodeData.getParent()) && ordIdp == -1) {
            for (int i = 0; i <= partialOrdering.size(); i++) {
                partialOrdering.add(i, node2Pathp);
                int score = getCrossingScore(nodes2nConections, node2Pathp, decendants, partialOrdering, i);
                if (score < bound) {
                    recGenerateOrderings(orderedRNodesIndex, orderedRNodes, decendants, nodes2nConections, partialOrdering);
                }
                partialOrdering.remove(i);
            }
        } else if (!node2Pathq.equals(rNodeData.getParent()) && ordIdq == -1) {
            for (int i = 0; i <= partialOrdering.size(); i++) {
                partialOrdering.add(i, node2Pathq);
                int score = getCrossingScore(nodes2nConections, node2Pathq, decendants, partialOrdering, i);
                if (score < bound) {
                    recGenerateOrderings(orderedRNodesIndex, orderedRNodes, decendants, nodes2nConections, partialOrdering);
                }
                partialOrdering.remove(i);
            }
        } else if (ordIdrNode == -1) {
            // place rNode between node2Pathp and node2Pathq
            int startPosition = (ordIdp > ordIdq ? ordIdq : ordIdp);
            int stopPosition = (ordIdp > ordIdq ? ordIdp : ordIdq);
            if (verbose)
                System.out.println("ordIdp: " + ordIdp + "\tordIdq: " + ordIdq + "\tstartPosition: " + startPosition + "\tstopPosition: " + stopPosition);
            for (int i = startPosition; i <= stopPosition; i++) {
                partialOrdering.add(i, startRNode);
                int score = getCrossingScore(nodes2nConections, startRNode, decendants, partialOrdering, i);
                if (score < bound) {
                    // if ordering is complete than return else place next rNode
                    if (partialOrdering.containsAll(orderedRNodes)) {
                        // new minimum found
                        bound = score;
                        bestOrdering = (ArrayList) partialOrdering.clone();
                    } else {
                        recGenerateOrderings((orderedRNodesIndex + 1), orderedRNodes, decendants, nodes2nConections, partialOrdering);
                    }
                }
                partialOrdering.remove(i);
            }

        }
        return null;
    }


    private int buildOrdListGreedy(ArrayList orderedRNodes, HashSet before, int[][] nodes2nConections, ArrayList decendants, ArrayList ordList) {
        Iterator it = orderedRNodes.iterator();
        // crossings is the last number we got as crossing score so it is the overall score we return it for the BnB approach
        int crossings = 0;
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            Node node2Pathp = rNodeData.getNode2p();
            Node node2Pathq = rNodeData.getNode2q();
            if (verbose)
                System.out.println("\nnode: " + rNode + "\tnode2Pathp: " + node2Pathp + "\tnode2Pathq: " + node2Pathq);
            // add p and q to the ordering
            if (!node2Pathp.equals(rNodeData.getParent()) && ordList.indexOf(node2Pathp) == -1) {
                if (verbose)
                    System.out.println("adding node: " + node2Pathp + "\t before: " + before.contains(node2Pathp));
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, node2Pathp);
                    int score = getCrossingScore(nodes2nConections, node2Pathp, decendants, ordList, i);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, node2Pathp);
                if (verbose)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);
            }
            if (!node2Pathq.equals(rNodeData.getParent()) && ordList.indexOf(node2Pathq) == -1) {
                if (verbose)
                    System.out.println("adding edge: node " + node2Pathq + "\t before: " + before.contains(node2Pathq));
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, node2Pathq);
                    int score = getCrossingScore(nodes2nConections, node2Pathq, decendants, ordList, i);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, node2Pathq);
                if (verbose)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);


            }
            if (!ordList.contains(rNode)) {
                // add imaginary edge  between p and q !
                int startIndex = ordList.indexOf(node2Pathp);
                int stopIndex = ordList.indexOf(node2Pathq);
                if (startIndex > stopIndex) {
                    int tmp = startIndex;
                    startIndex = stopIndex;
                    stopIndex = tmp;
                }
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                if (verbose)
                    System.out.println("adding edge: rNode " + rNode + "\t between: " + (startIndex + 1) + "\t" + stopIndex);
                for (int i = startIndex + 1; i <= stopIndex && minScore > 0; i++) {
                    ordList.add(i, rNode);
                    int score = getCrossingScore(nodes2nConections, rNode, decendants, ordList, i);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, rNode);
                if (verbose)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);
            }

        }
        // finally add al those edges that are "real" tree edges with no reticulation depending on it
        it = decendants.iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (verbose) System.out.println("n: " + n + "\tordList: " + ordList);
            if (ordList.indexOf(n) == -1 && n.getInDegree() == 1) {
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                if (verbose) System.out.println("adding edge: Node " + n + "\t before: " + before);
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, n);
                    int score = getCrossingScore(nodes2nConections, n, decendants, ordList, i);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, n);
                if (verbose)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);

            }
        }
        return crossings;
    }

    private int getCrossingScore(int[][] nodes2nConections, Node node, ArrayList decendants, ArrayList ordList, int position) {
        int score = 0;
        int indexP = -1;
        int indexQ = -1;
        if (node.getInDegree() == 2) {
            ReticulationNodeData rnd = (ReticulationNodeData) rNode2ReticulationNodeData.get(node);
            indexP = decendants.indexOf(rnd.getNode2p());
            indexQ = decendants.indexOf(rnd.getNode2q());
        }
        // calculate score of the position: 
        if (verbose) System.out.println("ordList: " + ordList);
        for (int i = 0; i < ordList.size(); i++) {
            Node n1 = (Node) ordList.get(i);
            int index1 = decendants.indexOf(n1);
            for (int j = i + 1; j < ordList.size(); j++) {
                Node n2 = (Node) ordList.get(j);
                int index2 = decendants.indexOf(n2);
                if (verbose)
                    System.out.println("indexP: " + indexP + "\t indexQ: " + indexQ + "\t index1: " + index1 + "\tindex2: " + index2 + "\tmatrixValue: " + nodes2nConections[index1][index2]);
                //if (j != position && ((indexP != index1 && indexQ != index2) && (indexP != index2 && indexQ != index1))) {
                score += (j - i - 1) * nodes2nConections[index1][index2];
                if (verbose)
                    System.out.println("i: " + i + "\tj: " + j + "\tscore: " + score + "\tadded: " + ((j - i - 1) * nodes2nConections[index1][index2]) + "orderedList: " + ordList);
                //}

            }
            // adding score for before
            score += (i) * nodes2nConections[index1][nodes2nConections.length - 2];
            if (verbose)
                System.out.println("i: " + i + "\tbefore" + "\tscore: " + score + "\tentry in scoring matrix: " + nodes2nConections[index1][nodes2nConections.length - 2] + "orderedList: " + ordList);

            // adding score for after
            score += (ordList.size() - i) * nodes2nConections[index1][nodes2nConections.length - 1];
            if (verbose)
                System.out.println("i: " + i + "\tafter" + "\tscore: " + score + "\tentry in scoring matrix: " + nodes2nConections[index1][nodes2nConections.length - 1] + "orderedList: " + ordList);

        }
        // adding score for before
        return score;
    }


    private int[][] makeDistances4Edges(Node start, HashSet before, ArrayList orderedRNodes, ArrayList decendants, HashMap nodes2ActiverNodes) {
        // last two are the scores for before and after
        int[][] distances = new int[decendants.size() + 2][decendants.size() + 2];
        // first work in the orderedRNodes
        Iterator it = orderedRNodes.iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            // first add distance for p and q
            ReticulationNodeData rnd = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);

            /*  System.out.print("p: " + rnd.getNode2p() + "\tindex: " + decendants.indexOf(rnd.getNode2p()) +
                     "\tq: " + rnd.getNode2q() + "\tindex: " + decendants.indexOf(rnd.getNode2q()) +
                     "\trNode: " + rNode + "\tindex: " + decendants.indexOf(rNode));
            */
            if (!rnd.getNode2p().equals(rnd.getParent())) {
                distances[decendants.indexOf(rnd.getNode2p())][decendants.indexOf(rNode)]++;
                distances[decendants.indexOf(rNode)][decendants.indexOf(rnd.getNode2p())]++;
            }
            if (!rnd.getNode2q().equals(rnd.getParent())) {
                distances[decendants.indexOf(rnd.getNode2q())][decendants.indexOf(rNode)]++;
                distances[decendants.indexOf(rNode)][decendants.indexOf(rnd.getNode2q())]++;
            }
        }
        // set the before and after values, Thisi s done by checking for those rNdoes that are active at start and at a decendant
        // if such a node exists it has a connection either to the before or after part of ordering of the parent of start..
        //
        HashSet startActiveRNodes = (HashSet) nodes2ActiverNodes.get(start);
        it = startActiveRNodes.iterator();
        while (it.hasNext()) {
            Node r = (Node) it.next();
            for (Object decendant : decendants) {
                Node dec = (Node) decendant;
                HashSet decActiveRNodes = (HashSet) nodes2ActiverNodes.get(dec);
                // is the node in the active set of the decendant and already placed?
                if (decActiveRNodes.contains(r) && before.contains(r)) {
                    distances[decendants.indexOf(dec)][distances.length - 2]++;
                    distances[distances.length - 2][decendants.indexOf(dec)]++;
                } else if (decActiveRNodes.contains(r)) {
                    distances[decendants.indexOf(dec)][distances.length - 1]++;
                    distances[distances.length - 1][decendants.indexOf(dec)]++;
                }
            }
        }
        return distances;
    }


    private Point2D translateByAngle(Point2D apt, double alpha, double dist) {
        double dx = dist * Math.cos(alpha);
        double dy = dist * Math.sin(alpha);
        if (Math.abs(dx) < 0.000000001)
            dx = 0;
        if (Math.abs(dy) < 0.000000001)
            dy = 0;
        return new Point2D.Double(apt.getX() + dx, apt.getY() + dy);
    }


    private LinkedList findMinPath2Ancestor(Node decendant, Node ancestor, HashMap parent2rNodes) {
        if (verbose) System.out.println("ancestor: " + ancestor + "\tdecendant: " + decendant);
        LinkedList path = new LinkedList();
        recFindPath2Ancestor(decendant, ancestor, path);
        return path;
    }


    private void recFindPath2Ancestor(Node decendant, Node ancestor, LinkedList path) {
        // check if this is a recombination node
        if (decendant.equals(ancestor)) {
        } else if (decendant.getInDegree() == 2) {
            path.addFirst(decendant);
            ReticulationNodeData decRData = (ReticulationNodeData) rNode2ReticulationNodeData.get(decendant);
            if (decRData != null && decRData.getParent() != null) {
                recFindPath2Ancestor(decRData.getParent(), ancestor, path);
            }
        } else {
            path.addFirst(decendant);
            Node next = decendant.inEdges().iterator().next().getSource();
            recFindPath2Ancestor(next, ancestor, path);
        }
    }

    private void recFindPathesInOrgGraph2Ancestor(Node decendant, Node ancestor, LinkedList visitedNodes, HashSet pathes) {
        visitedNodes.add(decendant);
        //System.out.println("ancestor: " + ancestor + "\tdecendant: " + decendant);
        for (Node next : decendant.adjacentNodes()) {
            //System.out.println("\tnext: " + next + "\tisAncestor: " + decendant.getCommonEdge(next).getSource().equals(next) + "\tequal: " + next.equals(ancestor) + "\t" + next.compareTo(ancestor));
            if (decendant.getCommonEdge(next).getSource().equals(next)) {
                if (next.equals(ancestor)) {
                    //System.out.println("found path: " + visitedNodes);
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    newVisitedNodes.add(next);
                    pathes.add(newVisitedNodes);
                } else {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    recFindPathesInOrgGraph2Ancestor(next, ancestor, newVisitedNodes, pathes);
                }
            }
            //System.out.println();
        }
        // check if this is a recombination node
        if (decendant.getInDegree() == 2) {
            ReticulationNodeData decRData = (ReticulationNodeData) rNode2ReticulationNodeData.get(decendant);
            if (decRData != null && decRData.getParent() != null) {
                if (decRData.getParent().equals(ancestor)) {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    newVisitedNodes.add(decRData.getParent());
                    pathes.add(newVisitedNodes);
                } else {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    recFindPathesInOrgGraph2Ancestor(decRData.getParent(), ancestor, newVisitedNodes, pathes);
                }
            }
        }
    }


    /**
     * Gives back a linked List of the reticulation beeing childs of start. the list is ordered in the sequence the nodes must be added to start
     *
     * @return
     */

    private ArrayList buildLocalWorkFlow(HashSet rNodes, PhyloSplitsGraph ordGraph) {
        Iterator it = rNodes.iterator();
        HashMap rNode2ordNode = new HashMap();
        HashMap ordNode2rNode = new HashMap();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            Node ordNode = ordGraph.newNode();
            rNode2ordNode.put(rNode, ordNode);
            ordNode2rNode.put(ordNode, rNode);
        }
        it = rNodes.iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            HashSet dependentRNodes = rNodeData.getDependentRNodes();
            for (Object dependentRNode : dependentRNodes) {
                Node depRNode = (Node) dependentRNode;
                if (rNodes.contains(depRNode)) {
                    ordGraph.newEdge((Node) rNode2ordNode.get(rNode), (Node) rNode2ordNode.get(depRNode));
                }
            }
        }
        /*   System.out.println("local ordering Graph: ");
         Iterator itO = ordGraph.nodes().iterator();
         while (itO.hasNext()) System.out.println("Node: " + itO.next());
         System.out.println("-----------------------------------------");
         itO = ordGraph.edgeIterator();
         while (itO.hasNext()) System.out.println("Edge: " + itO.next());
        */
        LinkedList sortedOrdNodes = DFS(ordGraph, true, true);
        ArrayList sortedNodes = new ArrayList();
        it = sortedOrdNodes.iterator();
        while (it.hasNext()) sortedNodes.add(ordNode2rNode.get(it.next()));
        //System.out.println("sortedrNodes " + " of parent: " + sortedNodes);
        return sortedNodes;
    }


    /**
     * Gives back a graph with nodes are indications of reticulation nodes and an edge between two nodes if the movement of one node has an influence on the edgelength of a
     * reticulation edge of the other
     *
     * @param graph
     * @return
     */
    private PhyloSplitsGraph buildReticulationDependencyGraph(PhyloSplitsGraph graph, HashMap depRetNode2rNode) {
        HashMap rNode2DepRetNode = new HashMap();
        PhyloSplitsGraph depRet = new PhyloSplitsGraph();
        // init depRet
        Iterator it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node retN = (Node) it.next();
            if (retN.getInDegree() == 2) {
                if (rNode2ReticulationNodeData.get(retN) == null)
                    rNode2ReticulationNodeData.put(retN, new ReticulationNodeData(retN));
                Node depN = depRet.newNode();
                depRetNode2rNode.put(depN, retN);
                rNode2DepRetNode.put(retN, depN);
                depN.setInfo(retN);
            }
        }

        it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node retN = (Node) it.next();
            HashSet dependentTreeNodes = ((ReticulationNodeData) rNode2ReticulationNodeData.get(retN)).getDependentTreeNodes();
            HashSet dependentRetNodes = ((ReticulationNodeData) rNode2ReticulationNodeData.get(retN)).getDependentRNodes();
            HashSet seenNodes = new HashSet();
            seenNodes.add(retN);
            // init toWork
            Vector nodes2Work = new Vector();
            for (Node n : retN.adjacentNodes()) {
                if (n.getCommonEdge(retN).getSource().equals(retN))
                    nodes2Work.add(n);
            }
            while (nodes2Work.size() > 0) {
                Node next = (Node) nodes2Work.remove(0);
                //System.out.println("rNode: " + retN + "\tNode: " + next + "\tlist: " + nodes2Work);
                if (!seenNodes.contains(next) && next.getInDegree() == 1) {
                    dependentTreeNodes.add(next);
                    for (Node toAdd : retN.adjacentNodes()) {
                        if (next.getCommonEdge(toAdd).getSource().equals(next)) {
                            nodes2Work.add(toAdd);
                        }
                    }
                } else if (!seenNodes.contains(next) && next.getInDegree() == 2) {
                    dependentRetNodes.add(next);
                    //System.out.println(rNode2DepRetNode.get(retN) + "\t" + rNode2DepRetNode.get(next));
                    depRet.newEdge((Node) rNode2DepRetNode.get(retN), (Node) rNode2DepRetNode.get(next));
                }
                seenNodes.add(next);
            }
            //System.out.println(rNode2ReticulationNodeData.get(retN));
        }
        return depRet;
    }


    /**
     * Graph stuff *
     */

    static private Integer white = 0, gray = 1, black = 2;
    static private int time = -1;

    public LinkedList DFS(PhyloSplitsGraph graph, boolean breakCycles, boolean removeForwardEdges) {
        HashMap node2Color = new HashMap();
        HashMap node2Predecessor = new HashMap();
        HashMap node2time = new HashMap();
        LinkedList sortedNodes = new LinkedList();
        Iterator it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            node2Color.put(n, white);
            node2Predecessor.put(n, null);
            // first is discovery, second is finishing time
            node2time.put(n, new int[]{-1, -1});
        }
        time = 0;
        LinkedList sorted = new LinkedList();
        it = graph.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (node2Color.get(n).equals(white))
                sorted.addAll(DFSVisit(n, node2time, node2Color, node2Predecessor, breakCycles, removeForwardEdges));
        }
        while (sorted.size() > 0) {
            Node n = (Node) sorted.removeLast();
            int[] nTimes = (int[]) node2time.get(n);
            //System.out.println("Node: " + n + "\ttimestamps: " + nTimes[0] + "/" + nTimes[1]);
            sortedNodes.add(n);
        }
        return sortedNodes;
    }

    public LinkedList DFSVisit(Node u, HashMap node2time, HashMap node2Color, HashMap node2Predecessor, boolean breakCycles, boolean removeForwardEdges) {
        LinkedList sorted = new LinkedList();
        node2Color.put(u, gray);
        int[] uTimes = (int[]) node2time.get(u);
        uTimes[0] = ++time;
        for (Node v : u.adjacentNodes()) {
            if (v.getCommonEdge(u).getSource().equals(u)) {
                if (node2Color.get(v).equals(white)) {
                    node2Predecessor.put(v, u);
                    sorted.addAll(DFSVisit(v, node2time, node2Color, node2Predecessor, breakCycles, removeForwardEdges));
                }
            }
        }
        node2Color.put(u, black);
        sorted.addLast(u);
        uTimes[1] = ++time;
        return sorted;
    }


    public Edge getCommonEdgeResepctImaginary(Node n1, Node n2, boolean imaginary, Set imaginaryEdges) {
        for (Edge next : n1.adjacentEdges()) {
            if (next.getSource().equals(n2) || next.getTarget().equals(n2)) {
                if (!imaginary && !imaginaryEdges.contains(next))
                    return next;
                else if (imaginary && imaginaryEdges.contains(next)) return next;
            }
        }
        return null;
    }


    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public double getOptionMaxAngle() {
        return maxAngle;
    }

    public void setOptionMaxAngle(double maxAngle) {
        this.maxAngle = maxAngle;
    }

    public boolean getOptionUseWeights() {
        return useWeights;
    }

    public void setOptionUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }

    public double getOptionDeltaAngle() {
        return deltaAngle;
    }

    public void setOptionDeltaAngle(double delta) {
        this.deltaAngle = delta;
    }

    /**
     * gets the method to use
     *
     * @return name of method
     */
 /*   public String getOptionSort() {
        return optionSorting;
    }
 */
    /**
     * sets the method to use
     *
     * @param optionMethod
     */
  /*  public void setOptionSort(String optionMethod) {
        this.optionSorting = optionMethod;
    }
  */
    /**
     * returns list of all known methods
     *
     * @return methods
     */
   /* public List selectionOptionSort(Document doc) {
        List methods = new LinkedList();
        methods.add(greedy);
        methods.add(perfect);
        return methods;
    }
   */
}


class ReticulationNodeData {

    Node rNode;
    Node parent;
    Edge p;
    Edge q;
    Node node2p;
    Node node2q;
    LinkedList pathParent2P;
    LinkedList pathParent2Q;

    HashSet dependentTreeNodes;
    HashSet dependentRNodes;


    public ReticulationNodeData(Node rNode, Node parent) {
        this.rNode = rNode;
        this.parent = parent;
        dependentRNodes = new HashSet();
        dependentTreeNodes = new HashSet();
    }

    public ReticulationNodeData(Node rNode) {
        this.rNode = rNode;
        dependentRNodes = new HashSet();
        dependentTreeNodes = new HashSet();
    }


    public String toString() {
        return "rNode: " + rNode + "\tparent: " + parent + "\np: " + p + "\tnode2p: " + node2p + "\nq: " + q + "\tnode2q: " + node2q +
                "\ndependent tree Nodes: " + dependentTreeNodes + "\ndpendent Reticulations: " + dependentRNodes;
    }

    public Node getrNode() {
        return rNode;
    }

    public void setrNode(Node rNode) {
        this.rNode = rNode;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Edge getP() {
        return p;
    }

    public void setP(Edge p) {
        this.p = p;
    }

    public Edge getQ() {
        return q;
    }

    public void setQ(Edge q) {
        this.q = q;
    }

    public HashSet getDependentTreeNodes() {
        return dependentTreeNodes;
    }

    public void setDependentTreeNodes(HashSet dependentTreeNodes) {
        this.dependentTreeNodes = dependentTreeNodes;
    }

    public HashSet getDependentRNodes() {
        return dependentRNodes;
    }

    public void setDependentRNodes(HashSet dependentRNodes) {
        this.dependentRNodes = dependentRNodes;
    }

    public boolean addDependentRNode(Node rNode) {
        if (this.dependentRNodes == null) this.dependentRNodes = new HashSet();
        return this.dependentRNodes.add(rNode);
    }

    public boolean addDependentTreeNode(Node treeNode) {
        if (this.dependentTreeNodes == null) this.dependentTreeNodes = new HashSet();
        return this.dependentTreeNodes.add(treeNode);
    }

    public Node getNode2p() {
        return node2p;
    }

    public void setNode2p(Node node2p) {
        this.node2p = node2p;
    }

    public Node getNode2q() {
        return node2q;
    }

    public void setNode2q(Node node2q) {
        this.node2q = node2q;
    }

    public LinkedList getPathParent2P() {
        return pathParent2P;
    }

    public void setPathParent2P(LinkedList pathParent2P) {
        this.pathParent2P = pathParent2P;
    }

    public LinkedList getPathParent2Q() {
        return pathParent2Q;
    }

    public void setPathParent2Q(LinkedList pathParent2Q) {
        this.pathParent2Q = pathParent2Q;
    }
}
