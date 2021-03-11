/**
 * ClusterNetwork.java
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
package splitstree4.algorithms.splits;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.NodeView;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Geometry;
import jloda.util.Basic;
import jloda.util.Pair;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Queue;
import java.util.*;

/**
 * Draws a rooted set of splits as an extended Hasse diagram
 * Daniel Huson and David Bryant, 4.2007
 */
public class ClusterNetwork implements Splits2Network {
    final static String DESCRIPTION = "Cluster network for rooted set of splits (Huson et al, 2007, in preparation)";

    private boolean optionShowClusters = false;
    private String optionOutGroup = Taxa.FIRSTTAXON;
    private boolean optionUseWeights = true;
    public String optionLayout = ReticulateNetwork.EQUALANGLE120;
    private boolean optionSimplify = false;
    public int optionPercentOffset = 10;

    private PhyloGraphView graphView;
    private PhyloSplitsGraph graph;

    /**
     * Applies the method to the given data
     *
     * @param taxa    the taxa
     * @param splits0 the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits0) throws Exception {
        double largestDistance = 0;
        for (int s = 1; s <= splits0.getNsplits(); s++)
            largestDistance = Math.max(largestDistance, splits0.getWeight(s));

        Splits splits = splits0.clone(taxa);
        // add all missing tivial splits
        SplitsUtilities.addAllTrivial(splits, taxa.getNtax(), (float) (0.05 * largestDistance));

        final int ntax = taxa.getNtax();

        //System.err.println("Outgroup taxon: "+getOptionOutGroup()+" id: "+taxa0.indexOf(getOptionOutGroup()));
        if (taxa.indexOf(getOptionOutGroup()) == -1)
            setOptionOutGroup(Taxa.FIRSTTAXON); // will assume taxon is outgroup

        int outgroupIndex = taxa.indexOf(getOptionOutGroup());

        // the graph view and graph
        graphView = new PhyloGraphView();
        graph = graphView.getPhyloGraph();

        int[] cycle = determineCycle(splits, outgroupIndex, ntax);
        int[] cycleInverse = new int[ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            cycleInverse[cycle[i]] = i;
        }

        // sort clusters by 1. increasing order of size and 2. lexicographically by cycle
        // A pair consists of (cluster,key), where cluster is a TaxaSet of the taxa and
        // key is an array of the cycle positions of the taxa
        SortedSet clusters = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                Pair p1 = (Pair) o1;
                Pair p2 = (Pair) o2;
                int[] key1 = (int[]) p1.getSecond();
                int[] key2 = (int[]) p2.getSecond();

                if (key1.length < key2.length)
                    return -1;
                else if (key1.length > key2.length)
                    return 1;
                for (int i = 0; i < key1.length; i++) {
                    if (key1[i] < key2[i])
                        return -1;
                    else if (key1[i] > key2[i])
                        return 1;
                }
                return 0;
            }
        });

        // maps each  cardinality to the list of clusters of that size
        List[] card2clusters = new List[ntax];
        for (int i = 1; i < ntax; i++)
            card2clusters[i] = new LinkedList();

        // map clusters to split ids
        Map cluster2splitId = new HashMap();
        // map splits to angles:
        double[] splitId2angle = new double[splits.getNsplits() + 1];

        // map splits to heights, used in rectangular drawing:
        double[] splitId2height = new double[splits.getNsplits() + 1];


        boolean hasOutgroupCluster = false;
        // add all clusters:
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet cluster = splits.get(s);
            if (cluster.get(outgroupIndex))
                cluster = cluster.getComplement(ntax);

            cluster2splitId.put(cluster, s);
            if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
                splitId2angle[s] = computeAngle(cluster, cycleInverse, ntax);
            else
                splitId2height[s] = computeHeight(cluster, cycleInverse, ntax);

            if (cluster.cardinality() == ntax - 1)
                hasOutgroupCluster = true;

            int[] key = new int[cluster.cardinality()];
            int count = 0;
            for (int t = cluster.getBits().nextSetBit(1); t > 0; t = cluster.getBits().nextSetBit(t + 1)) {
                key[count++] = cycleInverse[t];
            }
            clusters.add(new Pair(cluster, key));
        }

        if (!hasOutgroupCluster) {
            TaxaSet cluster = new TaxaSet();
            cluster.set(outgroupIndex);
            cluster = cluster.getComplement(ntax);
            int[] key = new int[cluster.cardinality()];
            int count = 0;
            for (int t = cluster.getBits().nextSetBit(1); t > 0; t = cluster.getBits().nextSetBit(t + 1)) {
                key[count++] = cycleInverse[t];
            }
            clusters.add(new Pair(cluster, key));

        }

        /*
        System.err.println("outgroup: " + outgroupIndex);
        System.err.println("cycle: " + Basic.toString(cycle));
        System.err.println("Clusters:");
        */

        // maps each cluster to a node
        Map cluster2node = new HashMap();

        Node root = null;

        //    schedule clusters and setup nodes
        for (Object cluster1 : clusters) {
            Pair pair = (Pair) cluster1;
            // System.err.println(pair.getFirst());
            TaxaSet cluster = (TaxaSet) pair.getFirst();
            card2clusters[cluster.cardinality()].add(cluster);
            Node v = graph.newNode();
            cluster2node.put(cluster, v);
            NodeView nv = graphView.getNV(v);
            //nv.setLocation(new Point(card2clusters[cluster.cardinality()].size(), cluster.cardinality()));
            if (optionShowClusters)
                nv.setLabel(Basic.toString(cluster.getBits()));
            v.setInfo(cluster);
            root = v; // outgroup cluster will be last cluster in ordering
        }

        // add edges:

        for (int level = 2; level < ntax; level++) {
            for (Object o : card2clusters[level]) {
                TaxaSet topCluster = (TaxaSet) o;
                Node topNode = (Node) cluster2node.get(topCluster);
                NodeSet covered = new NodeSet(graph);

                for (int lower = level - 1; lower >= 1; lower--) {
                    for (Object o1 : card2clusters[lower]) {
                        TaxaSet lowerCluster = (TaxaSet) o1;
                        Node lowerNode = (Node) cluster2node.get(lowerCluster);

                        if (topCluster.contains(lowerCluster) && !covered.contains(lowerNode)) {
                            graph.newEdge(topNode, lowerNode);
                            markAsCoveredRec(lowerNode, covered);
                        }
                    }
                }
            }
        }

        // add outgroup:
        if (root != null) {
            TaxaSet cluster = new TaxaSet();
            cluster.set(outgroupIndex);
            Integer splitId;
            {
                Integer n = (Integer) cluster2splitId.get(graph.getInfo(root));
                if (n != null)
                    splitId = n;
                else
                    splitId = 0;
            }
            cluster2splitId.put(cluster, splitId);
            if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
                splitId2angle[splitId] = computeAngle(cluster, cycleInverse, ntax);
            else
                splitId2height[splitId] = computeHeight(cluster, cycleInverse, ntax);

            Node v = graph.newNode();
            v.setInfo(cluster);
            NodeView nv = graphView.getNV(v);
            if (optionShowClusters)
                nv.setLabel("" + outgroupIndex);
            //nv.setLocation(new Point(0, 1));
            graph.newEdge(root, v);
        }

        // split every node that has indegree>1 and outdegree!=1
        List nodes = new LinkedList();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext())
            nodes.add(v);

        for (Object node : nodes) {
            Node v = (Node) node;
            if (v.getInDegree() > 1) {
                Node w = graph.newNode();
                List toDelete = new LinkedList();
                for (Edge e : v.inEdges()) {
                    Node u = e.getSource();
                    graph.newEdge(u, w);
                    toDelete.add(e);
                }
                Edge f = graph.newEdge(w, v);

                Integer n = (Integer) cluster2splitId.get(v.getInfo());
                if (n != null && n != 0)
                    graph.setSplit(f, n);
                for (Object aToDelete : toDelete) {
                    graph.deleteEdge((Edge) aToDelete);
                }
                //graphView.setLocation(w, graphView.getLocation(v).getX(), graphView.getLocation(v).getY() + 0.1);
            } else if (v.getInDegree() == 1) {
                Edge e = v.inEdges().iterator().next();
                Integer n = (Integer) cluster2splitId.get(v.getInfo());
                if (n != null && n != 0)
                    graph.setSplit(e, n);
            }
        }

        // check that root is only node with indegree 0:
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (v != root && v.getInDegree() == 0)
                System.err.println("Warning: node '" + v + "' has indegree 0");

            if (v.getOutDegree() == 0) {
                TaxaSet cluster = (TaxaSet) v.getInfo();
                if (cluster != null) {
                    for (int t = cluster.getBits().nextSetBit(1); t > 0; t = cluster.getBits().nextSetBit(t + 1)) {
                        graph.addTaxon(v, t);
                    }
                }
            }
        }

        EdgeSet blueEdges = new EdgeSet(graph);

        // set split of all "blue" edges to -1:
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (e.getTarget().getInDegree() > 1) // is blue edge
            {
                if (graph.getSplit(e) != 0)
                    System.err.println("Warning: blue edge has splitId=" + graph.getSplit(e));
                graph.setSplit(e, -1);
                graphView.setColor(e, Color.BLUE);
                graphView.setDirection(e, EdgeView.DIRECTED);
                blueEdges.add(e);
            }
        }

        // simplify, if desired:
        if (getOptionSimplify()) {
            simplify(root, blueEdges);
        }

        // COMPUTE the embedding:

        double widthFactor = largestDistance > 0 ? 10.0 / largestDistance : 1;
        double smallDistance = widthFactor * (getOptionPercentOffset() / 100.0) * largestDistance;

        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX)) // equal angle layout
        {
            assignCoordinatesEqualAngle(root, splits, splitId2angle, smallDistance, widthFactor);

        } else if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARPHYLOGRAM)) {
            double rootHeight = computeHeight(taxa.getTaxaSet(), cycleInverse, ntax);
            assignCoordinatesRectangularPhylogram(root, rootHeight, splits, splitId2height, smallDistance, widthFactor);
        } else if (getOptionLayout().equals(ReticulateNetwork.RECTANGULARCLADOGRAM)) {
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getOutDegree() == 0)   // is leaf, place on base line
                {
                    double y;
                    if (v.getInDegree() == 0)
                        y = 0;
                    else {
                        y = splitId2height[graph.getSplit(v.getFirstAdjacentEdge())];
                    }
                    graphView.setLocation(v, 0, y);
                } else
                    graphView.setLocation(v, null);
            }

            assignCoordinatesRectangularCladogramRec(root);
        }

        // move all blue edges to front of list of edges
        List toMove = new LinkedList();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == -1)
                toMove.add(e);
        }
        //  move blue edges to front of list of edges so that they are drawn first
        for (ListIterator it = toMove.listIterator(toMove.size()); it.hasPrevious(); ) {
            graph.moveToFront((Edge) it.previous());
        }

        // remove all added trivial splits:
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            int s = graph.getSplit(e);
            if (s > splits0.getNsplits()) // is an added split
            {
                graph.setSplit(e, 0);
                /*
                // move all taxa from target of e to source of e:
                Node source=e.getSource();
                Node target=e.getTarget();
                if(getOptionLayout().equals(ReticulateNetwork.RECTANGULARCLADOGRAM))
                {
                    graphView.setLocation(source,graphView.getLocation(target).getX(),graphView.getLocation(source).getY());
                }
                List list=graph.getNode2Taxa(target);
                if(list!=null)
                {
                    for(Iterator it=list.iterator();it.hasNext();)
                    {
                        int t=((Integer)it.next()).intValue();
                        graph.setTaxon2Node(t,source);
                        graph.setNode2Taxa(source,t);
                    }
                }
                //graph.deleteNode(target);
                */
            }
        }
        for (int t = 1; t <= taxa.getNtax(); t++) {
            Node v = graph.getTaxon2Node(t);
            if (graph.getLabel(v) == null)
                graph.setLabel(v, taxa.getLabel(t));
            else
                graph.setLabel(v, graph.getLabel(v) + ", " + taxa.getLabel(t));
        }

        Network network = new Network(taxa, graphView);
        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
            network.setLayout(Network.CIRCULAR);
        else
            network.setLayout(Network.RECTILINEAR);

        return network;
    }

    /**
     * assign equal angle coordinates
     *
     * @param root
     * @param splits
     * @param splitId2angle
     */
    private void assignCoordinatesEqualAngle(Node root,
                                             Splits splits, double[] splitId2angle,
                                             double smallDistance, double widthFactor) {
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLocation(v, null);
        }
        // assign coordinates:
        final Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        graphView.setLocation(root, 0, 0);

        while (queue.size() > 0) // breath-first assignment
        {
            Node v = queue.poll();

            boolean ok = true;
            if (v.getInDegree() == 1) // is regular in edge
            {
                Edge e = v.inEdges().iterator().next();
                Node w = e.getSource();
                int splitId = graph.getSplit(e);
                if (splitId > 0) {
                    Point2D location = graphView.getLocation(w);

                    if (location == null) // can't process yet
                    {
                        ok = false;
                    } else {
                        double weight = widthFactor * (getOptionUseWeights() ? splits.getWeight(splitId) : 1);
                        double angle = splitId2angle[splitId];
                        location = Geometry.translateByAngle(location, angle, weight);
                        graphView.setLocation(e.getTarget(), location);
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
                        break;
                    } else {
                        x += location.getX();
                        y += location.getY();
                    }
                    count++;
                }
                Point2D location = new Point2D.Double(x / count, y / count);
                if (smallDistance > 0) {
                    Point2D diff = Geometry.diff(location, graphView.getLocation(root));
                    if (diff.getX() != 0 || diff.getY() != 0) {
                        double angle = Geometry.computeAngle(diff);
                        location = Geometry.translateByAngle(location, angle, smallDistance);
                    }
                }
                if (ok && count > 0) {
                    graphView.setLocation(v, location);
                }
            } else  // is root node
            {
            }

            if (ok)  // add childern to end of queue:
            {
                for (Edge e : v.outEdges()) {
                    queue.add(e.getTarget());

                }
            } else  // process this node again later
                queue.add(v);
        }
    }

    /**
     * assign rectangular phylogram coordinates
     *
     * @param root
     * @param splits
     */
    private void assignCoordinatesRectangularPhylogram(Node root, double rootHeight, Splits splits, double[] splitId2height, double smallDistance, double widthFactor) {
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLocation(v, null);
        }

        // assign coordinates:
        final Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) // breath-first assignment
        {
            Node v = queue.poll();

            boolean ok = true;
            if (v.getInDegree() == 1) // is regular in edge
            {
                Edge e = v.inEdges().iterator().next();
                Node w = e.getSource();
                int splitId = graph.getSplit(e);
                if (splitId > 0) {
                    Point2D location = graphView.getLocation(w);

                    if (location == null) // can't process yet
                    {
                        ok = false;
                    } else {
                        double weight = widthFactor * (getOptionUseWeights() ? splits.getWeight(splitId) : 1);
                        double height = splitId2height[splitId];
                        graphView.setLocation(e.getTarget(), location.getX() + weight, height);
                        final List<Point2D> internalPoints = new ArrayList<>();
                        internalPoints.add(new Point2D.Double(location.getX(),
                                graphView.getLocation(v).getY()));
                        graphView.setInternalPoints(e, internalPoints);
                    }
                } else
                    System.err.println("Warning: split-id=0");
            } else if (v.getInDegree() > 1) // all in edges are 'blue' edges
            {
                double x = 0;
                double y = 0;
                int count = 0;
                for (Edge f : v.inEdges()) {
                    Node w = f.getSource();
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
                        Edge f = v.outEdges().iterator().next();
                        y = splitId2height[graph.getSplit(f)];
                    }

                    x = graphView.getLocation(v.inEdges().iterator().next().getSource()).getX();
                    for (Edge f : v.inEdges()) {
                        Point2D apt = graphView.getLocation(f.getSource());
                        if (apt.getX() > x)
                            x = apt.getX();
                    }
                    x += smallDistance;

                    graphView.setLocation(v, x, y);
                }
            } else  // is root node
            {
                graphView.setLocation(v, 0, rootHeight);
            }

            if (ok)  // add childern to end of queue:
            {
                for (Edge f : v.outEdges()) {
                    queue.add(f.getTarget());
                }
            } else  // process this node again later
                queue.add(v);
        }
    }

    static final private int blackWeight = 2; // computation of height, give black edges higher weight

    /**
     * recursively assign coordinates to rectangular cladogram
     *
     * @param v
     * @return location of v
     */
    private Point2D assignCoordinatesRectangularCladogramRec(Node v) {
        if (graphView.getLocation(v) == null) {
            double x = 0;
            double y = 0;
            int count = 0;
            for (Edge e : v.outEdges()) {
                Point2D location = assignCoordinatesRectangularCladogramRec(e.getOpposite(v));
                boolean blackEdge = (e.getTarget().getInDegree() == 1);
                x = Math.min(x, location.getX());
                y += (blackEdge ? blackWeight : 1) * location.getY();
                count += (blackEdge ? blackWeight : 1);
            }
            y /= count; // average height, weighted black against blue
            graphView.setLocation(v, x - 10, y);
            for (Edge e : v.outEdges()) {
                List<Point2D> list = new ArrayList<>();
                list.add(new Point2D.Double(graphView.getLocation(v).getX(), graphView.getLocation(e.getOpposite(v)).getY()));
                graphView.setInternalPoints(e, list);
                if (graph.getSplit(e) == -1)
                    graphView.setShape(e, EdgeView.CUBIC_EDGE);

            }
        }
        return graphView.getLocation(v);
    }

    /**
     * computes the angle of a cluster
     *
     * @param cluster
     * @param cycleInverse
     * @param ntax
     * @return angle
     */
    private double computeAngle(TaxaSet cluster, int[] cycleInverse, int ntax) {
        int min = ntax;
        int max = 0;
        for (int t = 1; t <= ntax; t++)
            if (cluster.get(t)) {
                if (cycleInverse[t] < min)
                    min = cycleInverse[t];
                if (cycleInverse[t] > max)
                    max = cycleInverse[t];
            }
        double part = ReticulateNetwork.getLayoutAngle(getOptionLayout()) / 360.0;
        return (part * Math.PI * (min + max)) / ntax - (0.5 + part) * Math.PI;
    }

    /**
     * computes the height of a cluster for the rectangular view
     *
     * @param cluster
     * @param cycleInverse
     * @param ntax
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
        return (5.0 * (min + max)) / ntax;
    }

    /**
     * recursively mark all nodes below this as covered
     *
     * @param v
     * @param covered
     */
    private void markAsCoveredRec(Node v, NodeSet covered) {
        if (!covered.contains(v)) {
            covered.add(v);
            for (Edge e : v.outEdges()) {
                markAsCoveredRec(e.getTarget(), covered);
            }
        }
    }

    /**
     * sets the cycle so that the outgroup is at the first position
     *
     * @param splits
     * @param outgroupIndex
     * @param ntax
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
        if (cycle[1] != outgroupIndex) // need to rotate
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
     * simplifies the cluster network
     *
     * @param root
     * @param blueEdges
     */
    private void simplify(Node root, EdgeSet blueEdges) {
        // 1. determine all black connected components
        NodeIntArray node2component = new NodeIntArray(graph);
        Vector indegree2RootList = new Vector();

        int count = 0;
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (node2component.get(v) == 0) {
                int indegree = v.getInDegree();
                if (indegree > 1 || v == root) // root of a black component
                {
                    count++;
                    if (indegree >= indegree2RootList.size())
                        indegree2RootList.setSize(indegree + 1);
                    List roots = (List) indegree2RootList.get(indegree);
                    if (roots == null) {
                        roots = new LinkedList();
                        indegree2RootList.set(indegree, roots);
                    }
                    roots.add(v);
                    determineBlackComponentRec(v, null, blueEdges, node2component, count);
                }
            }
        }
        // todo: for debugging:
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            int component = node2component.get(v);
            /*
            if (graph.getLabel(v) == null)
                graph.setLabel(v, "(" + component + ")");
            else
                graph.setLabel(v, graph.getLabel(v) + "(" + component + ")");
            if (v == component2rootNode.get(component))
                graph.setLabel(v, graph.getLabel(v) + "r");
                */
        }

        // 2. replace all K-PQ graphs by K-P1 graph
        for (int indegree = 2; indegree < indegree2RootList.size(); indegree++) {
            Set used = new HashSet();
            List rootList = (List) indegree2RootList.get(indegree);
            if (rootList != null) {
                Node[] roots = (Node[]) rootList.toArray(new Node[rootList.size()]);
                for (int r = 0; r < roots.length; r++) {
                    List nodesToDelete = new LinkedList();
                    Node v = roots[r];
                    if (v != null && !used.contains(v)) {
                        Set pSet = getPrecursors(v);
                        Set qSet = new HashSet();
                        qSet.add(v);
                        for (int s = r + 1; s < roots.length; s++) {
                            Node w = roots[s];
                            if (w != null && !used.contains(w) && pSet.equals(getPrecursors(w))) {
                                qSet.add(w);
                            }
                        }
                        if (qSet.size() >= 2) {
                            used.addAll(qSet);
                            nodesToDelete.addAll(qSet);
                            simplifyKPQ(qSet);
                        }
                    }
                    for (Object aNodesToDelete : nodesToDelete) {
                        Node u = (Node) aNodesToDelete;
                        System.err.println(u);
                        graph.deleteNode(u);
                    }
                }
            }
        }
    }

    /**
     * simplify the K-PQ subgraph
     *
     * @param qSet
     */
    private void simplifyKPQ(Set qSet) {
        if (qSet.size() > 1)     // have something to simplify
        {
            Node newV = graph.newNode();
            // graphView.setLocation(newV, 0, 0);
            for (Object aQSet : qSet) {
                Node v = (Node) aQSet;
                if (graph.getLabel(v) != null) {
                    if (graph.getLabel(newV) == null)
                        graph.setLabel(newV, graph.getLabel(v));
                    else
                        graph.setLabel(newV, graph.getLabel(newV) + ", " + graph.getLabel(v));
                }
                if (graph.getNumberOfTaxa(v) > 0) {
                    for (Integer t : graph.getTaxa(v)) {
                        graph.addTaxon(newV, t);
                    }
                    final List<Edge> edgesToDelete = new ArrayList<>();

                    final List<Edge> edges = new ArrayList<>();
                    for (Edge f : v.adjacentEdges()) {
                        edges.add(f);
                    }

                    for (Object edge : edges) {
                        Edge e = (Edge) edge;
                        Edge newE;
                        if (e.getTarget() == v)
                            newE = graph.newEdge(e.getSource(), newV);
                        else
                            newE = graph.newEdge(newV, e.getTarget());
                        graph.setSplit(newE, graph.getSplit(e));
                        graph.setWeight(newE, graph.getWeight(newE));
                        if (v == e.getTarget())
                            graphView.setColor(newE, Color.BLUE);
                        edgesToDelete.add(e);
                    }
                    // remove old edges:
                    for (Object anEdgesToDelete : edgesToDelete) {
                        Edge e = (Edge) anEdgesToDelete;
                        graph.deleteEdge(e);
                    }
                }
            }
        }
    }

    /**
     * gets all precursor nodes for v
     *
     * @param v
     * @return all sources of incoming edges     for v
     */
    private Set getPrecursors(Node v) {
        Set result = new HashSet();

        for (Edge e : v.inEdges()) {
            result.add(e.getOpposite(v));
        }
        return result;
    }

    /**
     * recursively determine a black component
     *
     * @param v
     * @param e
     * @param blueEdges
     * @param node2component
     * @param component
     */
    private void determineBlackComponentRec(Node v, Edge e, EdgeSet blueEdges, NodeIntArray node2component, int component) {
        node2component.set(v, component);

        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            if (f != e && !blueEdges.contains(f))
                determineBlackComponentRec(f.getOpposite(v), f, blueEdges, node2component, component);
        }
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return taxa != null && splits != null;
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * get the outgroup taxon that defines the root
     *
     * @return out group
     */
    public String getOptionOutGroup() {
        return optionOutGroup;
    }

    /**
     * sets the outgroup taxon that defines the root
     *
     * @param optionOutGroup
     */
    public void setOptionOutGroup(String optionOutGroup) {
        this.optionOutGroup = optionOutGroup;
    }

    public boolean getOptionUseWeights() {
        return optionUseWeights;
    }

    public void setOptionUseWeights(boolean optionUseWeights) {
        this.optionUseWeights = optionUseWeights;
    }

    /*
    public boolean getOptionShowClusters() {
        return optionShowClusters;
    }

    public void setOptionShowClusters(boolean optionShowClusters) {
        this.optionShowClusters = optionShowClusters;
    }
    */

    public String getOptionLayout() {
        return optionLayout;
    }

    public void setOptionLayout(String optionLayout) {
        this.optionLayout = optionLayout;
    }

    public boolean getOptionSimplify() {
        return optionSimplify;
    }

    public void setOptionSimplify(boolean optionSimplify) {
        this.optionSimplify = optionSimplify;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List selectionOptionLayout(Document doc) {
        List list = new LinkedList();
        list.add(ReticulateNetwork.EQUALANGLE60);
        list.add(ReticulateNetwork.EQUALANGLE120);
        list.add(ReticulateNetwork.EQUALANGLE180);
        list.add(ReticulateNetwork.EQUALANGLE360);
        list.add(ReticulateNetwork.RECTANGULARPHYLOGRAM);
        list.add(ReticulateNetwork.RECTANGULARCLADOGRAM);

        return list;
    }

    public int getOptionPercentOffset() {
        return optionPercentOffset;
    }

    public void setOptionPercentOffset(int optionPercentOffset) {
        this.optionPercentOffset = Math.max(0, Math.min(100, optionPercentOffset));
    }
}
