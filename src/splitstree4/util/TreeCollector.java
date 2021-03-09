/**
 * TreeCollector.java
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
package splitstree4.util;

import jloda.graph.*;
import jloda.graphs.algorithms.Dijkstra;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.GraphView;
import jloda.util.Basic;
import splitstree4.algorithms.trees.ConsensusNetwork;
import splitstree4.algorithms.trees.PartialSplit;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * collect trees in sets
 *
 * @author huson
 * Date: 11-Dec-2004
 */
public class TreeCollector {
    int numTrees;
    Trees trees;
    Taxa taxa;
    BitSet firstInGroup; //  this points to the first tree in an consecutive
    // list of trees that where added together using the add command

    /**
     * constructors a tree collector
     *
     * @param taxa    the set of all taxa involved
     * @param partial will trees be partial?
     */
    public TreeCollector(Taxa taxa, boolean partial) {
        this.taxa = taxa;
        trees = new Trees();
        trees.setIdentityTranslate(taxa);
        numTrees = 0;
        firstInGroup = new BitSet();
    }

    /**
     * adds a set of trees to the collection
     *
     * @param addTrees
     * @param addTaxa
     */
    public void addGroup(Trees addTrees, Taxa addTaxa) throws SplitsException, NotOwnerException {
        if (addTrees.getNtrees() > 0) {
            this.trees.setIdentityTranslate(addTaxa);

            for (int i = 1; i <= addTrees.getNtrees(); i++) {
                int id = (++numTrees);
                if (i == 1)
                    firstInGroup.set(id);
                Taxa tmpTaxa = (Taxa) addTaxa.clone();
                this.trees.addTree("t" + id, addTrees.getTree(i), tmpTaxa);
            }
        }
    }

    /**
     * optimally selects one tree per added group of trees
     *
     * @return one tree per added group of trees
     */
    public Trees getOptimalSelection() {
        try {
            if (taxa.getOriginalTaxa() == null)
                this.trees.setIdentityTranslate(taxa);
            else
                this.trees.setIdentityTranslate(taxa.getOriginalTaxa());

            // set up selection graph:
            Graph graph = new Graph();
            final GraphView graphView = new GraphView(graph);
            NodeSet[] group2nodes = new NodeSet[trees.getNtrees() + 1];

            int groupNumber = 0;

            // source node:
            Node source = graph.newNode();
            graphView.setLocation(source, new Point(groupNumber, 0));

            int prevGroup = 0, group = 0;
            int groupSize = 0;

            // add a node for every tree
            // put a node between u and v if u occurs in the group before the one containing v
            for (int t = 1; t <= trees.getNtrees(); t++) {
                if (firstInGroup.get(t)) {
                    prevGroup = group;
                    group = t;
                    group2nodes[group] = new NodeSet(graph);
                    groupNumber++;
                    groupSize = 0;
                } else
                    groupSize++;
                Node v = graph.newNode();

                graph.setInfo(v, t);
                graphView.setLocation(v, new Point(groupNumber, groupSize));
                group2nodes[group].add(v);
                // link all nodes in previous group to this node:
                if (prevGroup > 0) {
                    for (Node u : group2nodes[prevGroup]) {
                        graph.newEdge(u, v);
                    }
                } else   // link source to first group of nodes:
                    graph.newEdge(source, v);
            }

            // attach last group of nodes to sink
            Node sink = graph.newNode();
            graphView.setLocation(sink, new Point(groupNumber + 1, 0));
            if (group > 0) {
                for (Node u : group2nodes[group]) {
                    graph.newEdge(u, sink);
                }
            }

            // give edge edge a weight:
            for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                if (graph.getSource(e) == source || graph.getTarget(e) == sink) // source or sink edge
                {
                    graph.setInfo(e, 0);
                } else // compare trees
                {
                    int t1 = (Integer) graph.getInfo(graph.getSource(e));
                    int t2 = (Integer) graph.getInfo(graph.getTarget(e));
                    int weight = computeWeight(t1, t2);

                    graph.setInfo(e, weight);
                }
            }

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame();

                    graphView.setSize(400, 400);
                    frame.setSize(400, 400);
                    frame.getContentPane().add(graphView);
                    frame.setVisible(true);

                    graphView.setVisible(true);
                    graphView.fitGraphToWindow();
                    graphView.setAutoLayoutLabels(true);
                }
            });


            System.err.println("graph: " + graph);

            // run dijkstras algorithm:
            List<Node> shortestPath = Dijkstra.compute(graph, source, sink, z -> (Integer) z.getInfo());
            Trees result = new Trees();
            result.setTranslate(trees.getTranslate());
            for (Node v : shortestPath) {
                int t = (Integer) graph.getInfo(v);
                result.addTree(trees.getName(t), trees.getTree(t), taxa);
            }
            System.err.println("# TreeCollector: " + result.getNtrees() + " of " + trees.getNtrees());
            return result;
        } catch (Exception ex) {
            Basic.caught(ex);
            return null;
        }

    }

    /**
     * returns the proportion of similarity of two trees: 0 completely different,
     * 1 identical
     *
     * @param t1
     * @param t2
     * @return number of pairs of incompatible induced splits
     */
    private int computeWeight(int t1, int t2) throws NotOwnerException {
        Set psplits1 = new HashSet();
        TaxaSet support1 = new TaxaSet();
        computePartialSplits(taxa, trees, t1, psplits1, support1);
        Set psplits2 = new HashSet();
        TaxaSet support2 = new TaxaSet();
        computePartialSplits(taxa, trees, t2, psplits2, support2);

        support1.and(support2);

        Set projected1 = projectSplits(support1, psplits1);
        Set projected2 = projectSplits(support1, psplits2);

        int count = 0;
        for (Object aProjected1 : projected1) {
            PartialSplit ps1 = (PartialSplit) aProjected1;
            for (Object aProjected2 : projected2) {
                PartialSplit ps2 = (PartialSplit) aProjected2;
                if (!ps1.isCompatible(ps2))
                    count++;
            }
        }
        return count;
    }

    /**
     * project all splits onto the given taxa set
     *
     * @param taxa
     * @param psplits
     * @return list of projected splits
     */
    private Set projectSplits(TaxaSet taxa, Set psplits) {
        Set result = new HashSet();
        for (Object psplit1 : psplits) {
            PartialSplit psplit = (PartialSplit) psplit1;
            PartialSplit qsplit = psplit.getInduced(taxa);
            if (qsplit != null)
                result.add(qsplit);
        }
        return result;
    }

    /**
     * returns the set of all partial splits in the given tree
     *
     * @param trees   list of all trees
     * @param which   index of specific tree
     * @param psplits partial splits are returned here
     * @param support supporting taxa are returned here
     */
    private void computePartialSplits(Taxa taxa, Trees trees, int which,
                                      Set psplits, TaxaSet support) throws NotOwnerException {
        List list = new LinkedList(); // list of (onesided) partial splits
        Node v = trees.getTree(which).getFirstNode();
        computePSplitsFromTreeRecursively(v, null, trees, taxa, list, which, support);

        for (Object aList : list) {
            PartialSplit ps = (PartialSplit) aList;
            ps.setComplement(support);
            psplits.add(ps);
        }
    }

    // recursively compute the splits:

    private TaxaSet computePSplitsFromTreeRecursively(Node v, Edge e, Trees trees,
                                                      Taxa taxa, List list, int which, TaxaSet seen) throws NotOwnerException {
        PhyloTree tree = trees.getTree(which);
        TaxaSet e_taxa = trees.getTaxaForLabel(taxa, tree.getLabel(v));
        seen.or(e_taxa);

        for (Edge f : v.adjacentEdges()) {
            if (f != e) {
                TaxaSet f_taxa = computePSplitsFromTreeRecursively(tree.getOpposite(v, f), f, trees,
                        taxa, list, which, seen);
                PartialSplit ps = new PartialSplit(f_taxa);
                ps.setWeight((float) tree.getWeight(f));
                list.add(ps);
                e_taxa.set(f_taxa);
            }
        }
        return e_taxa;
    }

    /**
     * gets one tree per added group
     *
     * @return one tree per added group
     */
    public Trees getStrictConsensusPerGroup() {
        System.err.print("# TreeCollector: ");

        ConsensusNetwork cs = new ConsensusNetwork();
        cs.setOptionThreshold(0.5);
        cs.setOptionEdgeWeights(ConsensusNetwork.MEDIAN);

        if (taxa.getOriginalTaxa() == null)
            this.trees.setIdentityTranslate(taxa);
        else
            this.trees.setIdentityTranslate(taxa.getOriginalTaxa());

        Trees result = new Trees();
        result.setTranslate(trees.getTranslate());

        Trees group = null;
        int firstInCurrentGroup = 0;
        for (int t = 1; t <= trees.getNtrees(); t++) {
            if (firstInGroup.get(t)) // new group
            {
                if (group != null && group.getNtrees() > 0) // close existing group
                {
                    try {
                        Document groupDoc = new Document();
                        Taxa groupTaxa = new Taxa();
                        group.setTaxaFromPartialTrees(groupTaxa);
                        groupDoc.setTaxa(groupTaxa);
                        groupDoc.setTrees(group);

                        groupDoc.setSplits(cs.apply(groupDoc, groupDoc.getTaxa(), groupDoc.getTrees()));
                        PhyloTree tree = TreesUtilities.treeFromSplits(groupDoc.getTaxa(), groupDoc.getSplits(), group.getTranslate());
                        result.addTree("t" + firstInCurrentGroup, tree, taxa);

                    } catch (Exception e) {
                        Basic.caught(e);
                    }
                }
                group = new Trees();
                group.setTranslate(trees.getTranslate());
                firstInCurrentGroup = t;
            }
            if (group != null) {
                try {
                    group.addTree("t" + t, trees.getTree(t), taxa);
                } catch (SplitsException | NotOwnerException e) {
                    Basic.caught(e);
                }
            } else
                System.err.println("Group undefined");
        }
        if (group != null) {   // close existing group
            try {
                Document groupDoc = new Document();
                Taxa groupTaxa = new Taxa();
                group.setTaxaFromPartialTrees(groupTaxa);
                groupDoc.setTaxa(groupTaxa);
                groupDoc.setTrees(group);

                groupDoc.setSplits(cs.apply(groupDoc, groupDoc.getTaxa(), groupDoc.getTrees()));
                PhyloTree tree = TreesUtilities.treeFromSplits(groupDoc.getTaxa(), groupDoc.getSplits(), group.getTranslate());
                result.addTree("t" + firstInCurrentGroup, tree, taxa);

            } catch (Exception e) {
                Basic.caught(e);
            }
        }
        System.err.println(" " + result.getNtrees() + " of " + trees.getNtrees());
        return result;
    }


    /**
     * gets one tree per added group
     *
     * @return one tree per added group
     */
    public Trees getFirstTreePerGroup() {
        System.err.print("# TreeCollector: ");

        if (taxa.getOriginalTaxa() == null)
            this.trees.setIdentityTranslate(taxa);
        else
            this.trees.setIdentityTranslate(taxa.getOriginalTaxa());

        Trees result = new Trees();
        result.setTranslate(trees.getTranslate());

        for (int t = firstInGroup.nextSetBit(1); t > 0; t = firstInGroup.nextSetBit(t + 1)) {
            try {
                result.addTree("t" + t, trees.getTree(t), taxa);
            } catch (SplitsException | NotOwnerException e) {
                Basic.caught(e);
            }
        }
        System.err.println(" " + result.getNtrees() + " of " + trees.getNtrees());
        return result;
    }

    /**
     * gets all trees added to this tree collector
     *
     * @return all trees
     */
    public Trees getAllTrees() {
        System.err.println("# TreeCollector: " + trees.getNtrees());

        if (taxa.getOriginalTaxa() == null)
            this.trees.setIdentityTranslate(taxa);
        else
            this.trees.setIdentityTranslate(taxa.getOriginalTaxa());
        return trees;
    }
}
