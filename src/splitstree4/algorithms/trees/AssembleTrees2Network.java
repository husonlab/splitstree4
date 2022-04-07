/*
 * AssembleTrees2Network.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.trees;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.Basic;
import splitstree4.algorithms.splits.ReticulateNetwork;
import splitstree4.algorithms.util.ReticulateEmbedder;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.SplitsUtilities;
import splitstree4.util.TreesUtilities;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * assembles a set of partial trees into a network.
 * <p/>
 * Each tree is taken to be part of the network.
 * <p/>
 * The first tree contains the root
 * <p/>
 * Subsequent trees are plugged-in to trees that have already been processed:
 * <p/>
 * If the i-th tree is called ti, then there must exist precisely two taxa
 * labeled ti.1 and ti.2  contained in the trees t1... ti-1 and these two
 * taxa indicate precisely where the tree ti is to be attached.
 * <p/>
 * Daniel Huson and David Bryant, 5.2007
 */
public class AssembleTrees2Network implements Trees2Network {
    public final static String DESCRIPTION = "Assemble a reticulate network from its tree parts";

    private String optionOutGroup = Taxa.FIRSTTAXON;
    private boolean optionUseWeights = true;
    private boolean optionKeepRootLabels = false;
    private boolean optionTreesEdgesCubic = false;
    private boolean optionReticulateEdgesCubic = true;
    public String optionLayout = ReticulateNetwork.EQUALANGLE120;
    public int optionPercentOffset = 10;

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees the trees
     * @return the computed network
     */
    public Network apply(Document doc, Taxa taxa, Trees trees) throws IOException {
        // We assume that the i-th tree attaches to precisely two nodes
        // in the first i-1 trees. If the name of the ith tree is ti, then
        // the two attachment sites are represented by two taxa named ti.1 and ti.2

        taxa.hideTaxa(new TaxaSet()); // unhide all taxa

        // the first tree is used as the graph:
        PhyloTree graph = (PhyloTree) trees.getTree(1).clone();

        NodeSet reticulateNodes = new NodeSet(graph);
        int maxReticulateDegree = 0;

        // add all other trees to the graph:
        for (int t = 2; t <= trees.getNtrees(); t++) {
            PhyloTree tree = trees.getTree(t);

            // the set of nodes in graph to which the current tree attaches:
            NodeSet attachNodes = new NodeSet(graph);
            String name = "t" + t;

            determineAttachmentSites(name, graph, graph.getFirstNode(), attachNodes);

            if (attachNodes.size() == 0)
                throw new IOException("Can't attach tree, root label not mentioned in preceeding trees: " + name);

            Node last = graph.getLastNode();

            // here we add the tree to the graph, making sure we use the correct node as root:
            {
                Node oldRoot = tree.getRoot();
                boolean found = false;
                for (Node v = tree.getFirstNode(); !found && v != null; v = v.getNext()) {
                    if (tree.getLabel(v) != null && tree.getLabel(v).equals(name)) {
                        tree.setRoot(v);
                        found = true;
                    }
                }
                if (!found)
                    throw new IOException("Tree " + t + ": node labeled 't" + t + "' is missing, can't attach to graph");

                //System.err.println("size: " + graph.getNumberOfNodes());
                graph.parseBracketNotation(tree.toString(), false, false);
                tree.setRoot(oldRoot);
            }

            //System.err.println(" -> size: " + graph.getNumberOfNodes());


            Node root = last.getNext(); // first new node is root of current tree

            reticulateNodes.add(root);  // store reticulate node

            if (attachNodes.size() > maxReticulateDegree)
                maxReticulateDegree = attachNodes.size();

            // add reticulate node
            for (Node attachNode : attachNodes) {
                // attach the root of Ti to the previous trees:
                if (attachNode.getDegree() == 1) {
                    Node tmp = attachNode;
                    attachNode = attachNode.getFirstAdjacentEdge().getOpposite(attachNode);
                    graph.deleteNode(tmp);
                }
                Edge e = graph.newEdge(attachNode, root);
                graph.setSplit(e, -1);
            }
            graph.setLabel(root, null);
        }

        // hide all taxa not present in the graph:
        TaxaSet present = new TaxaSet();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            String label = graph.getLabel(v);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    present.set(t);
                } else
                    System.err.println("Warning: can't find taxon: " + label);
            }
        }
        // hide all taxa not present in the network:
        taxa.hideTaxa(present.getComplement(taxa.getNtax()));

        // set taxon labels:
        graph.clearTaxa();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            String label = graph.getLabel(v);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    graph.addTaxon(v, t);
                }
            }
        }

        PhyloGraphView graphView = new PhyloGraphView(graph);

        // give all tree edges a number and color reticulate edges blue:
        int count = 0;
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (graph.getSplit(e) == -1)
                graphView.setColor(e, Color.BLUE);
            else
                graph.setSplit(e, ++count);
        }

        Splits splits = computeSplits(graph, reticulateNodes, maxReticulateDegree, taxa);

// 11. if rectangular phylogram requested, compute now
        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX)) {
            int maxAngle = ReticulateNetwork.getLayoutAngle(getOptionLayout());
            int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);
            ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
            reticulateEmbedder.computeEqualAngle(taxa, splits, graphView,
                    outgroupId, false, getOptionUseWeights(), getOptionPercentOffset(), maxAngle);
        }
        switch (getOptionLayout()) {
            case ReticulateNetwork.RECTANGULARPHYLOGRAM: {
                int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

                ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
                reticulateEmbedder.computeRectangularPhylogram(taxa, splits, graphView,
                        outgroupId, false, getOptionUseWeights(), getOptionPercentOffset(),
                        getOptionReticulateEdgesCubic(), getOptionTreesEdgesCubic());
                break;
            }
            case ReticulateNetwork.RECTANGULARCLADOGRAM: {
                int outgroupId = Math.max(taxa.indexOf(getOptionOutGroup()), 1);

                ReticulateEmbedder reticulateEmbedder = new ReticulateEmbedder();
                reticulateEmbedder.computeRectangularCladogram(taxa, splits, graphView, outgroupId,
                        getOptionReticulateEdgesCubic(), getOptionTreesEdgesCubic());
                break;
            }
            case "Spring Embedder":
                graphView.computeSpringEmbedding(1000, false);
                break;
        }

        // 12. return modified graph and info

        Network network = new Network(taxa, graphView);
        if (getOptionLayout().startsWith(ReticulateNetwork.EQUALANGLE_PREFIX))
            network.setLayout(Network.CIRCULAR);
        else
            network.setLayout(Network.RECTILINEAR);
        return network;
    }

    /**
     * extract a set of trees and then computes a cycle for them
     *
     * @return splits
     */
    private Splits computeSplits(PhyloTree graph, NodeSet reticulateNodes, int maxReticulateDegree, Taxa taxa) {
        // extract a list of trees from the network:
        Trees extractedTrees = extractTrees(graph, reticulateNodes, maxReticulateDegree, taxa);
        //Trees extractedTrees = extractTrees2(graph, reticulateNodes, maxReticulateDegree, taxa);

        //System.err.println("Extracted trees: "+extractedTrees.toString(taxa));

        Splits splits = new Splits();
        splits.setNtax(taxa.getNtax());
        Map split2id = new HashMap();

        for (int t = 1; t <= extractedTrees.getNtrees(); t++)
        //int t=1;
        {
            Splits treeSplits = TreesUtilities.convertTreeToSplits(extractedTrees, t, taxa);
            if (treeSplits != null) {
                for (int s = 1; s <= treeSplits.getNsplits(); s++) {
                    Integer id = (Integer) split2id.get(treeSplits.get(s));
                    float weight = treeSplits.getWeight(s);
                    if (id == null) {
                        splits.add(treeSplits.get(s), weight);
                        int i = splits.getNsplits();
                        split2id.put(treeSplits.get(s), i);
                    } else {
                        splits.setWeight(id, splits.getWeight(id) + weight);
                    }
                }
            }
        }

        try {
            splits.setCycle(SplitsUtilities.computeCycle(splits));
        } catch (SplitsException ex) {
            Basic.caught(ex);
        }
        System.err.println("#NEXUS\n" + taxa + "\n" + splits.toString(taxa));

        return splits;
    }

    /**
     * extracts trees from the graph, such that each reticulate edge occurs at least once
     *
     * @return extracted trees block
     */
    private Trees extractTrees(PhyloTree graph, NodeSet reticulateNodes, int maxReticulateDegree, Taxa taxa) {
        Trees trees = new Trees();

        for (int t = 1; t <= maxReticulateDegree; t++) {
            PhyloTree tree = new PhyloTree();
            NodeArray oldNode2NewNode = tree.copy(graph);

            // for each reticulate node keep only one edge:
            List toDelete = new LinkedList();

            for (Node reticulateNode : reticulateNodes) {
                Node v = (Node) oldNode2NewNode.get(reticulateNode); // node in copy of graph
                int keep = ((t - 1) % v.getInDegree()) + 1;

                int count = 0;
                for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                    if (e.getTarget() == v) // is in edge
                    {
                        count++;
                        if (count != keep) {
                            toDelete.add(e);
                        } else {
                            tree.setSplit(e, -1);
                        }
                    }
                }
            }
            for (Object aToDelete : toDelete) {
                Edge e = (Edge) aToDelete;
                tree.deleteEdge(e);
            }

            // System.err.println("tree: "+tree.toString()+";");

            try {
                trees.addTree("t" + t, tree, taxa);
            } catch (SplitsException e) {
                Basic.caught(e);
            }
        }
        return trees;
    }

    /**
     * determine attachment sites for current tree name
     *
	 */
    private void determineAttachmentSites(String treeName, PhyloTree graph, Node firstNode, NodeSet attachNodes) {
        for (Node v = firstNode; v != null; v = v.getNext()) {
            String label = graph.getLabel(v);
            if (label != null && isTreeNameVersion(treeName, label)) {
                attachNodes.add(v);
            }
        }
    }

    /**
     * does the label equal treeName.NUMBER
     *
     * @return true, if label is a version of the treeName
     */
    private boolean isTreeNameVersion(String treeName, String label) {
        if (label.startsWith(treeName + ".")) {
            try {
                // what comes after the dot must be an integer:
                Integer.parseInt(label.substring(treeName.length() + 1));
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees) && trees.getNtrees() > 1
                && trees.getPartial();

        /*
        int gotFirst=0;
        int gotSecond=0;
        PhyloTree tree=trees.getTree(1);
        for(Node v=tree.getFirstNode();v!=null;v=v.getNext())
        {
                List list=tree.getNode2Taxa(v);
            for(Iterator it=list.iterator();it.hasNext();)
            {
                int t=((Integer)it.next()).intValue();
                if(taxa.getLabel(t).equals(trees.getName(2)+".1"))
                    gotFirst++;
                 else if(taxa.getLabel(t).equals(trees.getName(2)+".2"))
                    gotSecond++;
            }
        }
        return gotFirst==1 && gotSecond==1;
        */

        // todo: should test further, but is too expensive
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


    public String getOptionLayout() {
        return optionLayout;
    }

    public void setOptionLayout(String optionLayout) {
        this.optionLayout = optionLayout;
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
        list.add("Spring Embedder");
        return list;
    }


    public int getOptionPercentOffset() {
        return optionPercentOffset;
    }

    public void setOptionPercentOffset(int optionPercentOffset) {
        this.optionPercentOffset = Math.max(0, Math.min(100, optionPercentOffset));
    }

    public String getOptionOutGroup() {
        return optionOutGroup;
    }

    public void setOptionOutGroup(String optionOutGroup) {
        this.optionOutGroup = optionOutGroup;
    }


    public boolean getOptionUseWeights() {
        return optionUseWeights;
    }

    public void setOptionUseWeights(boolean optionUseWeights) {
        this.optionUseWeights = optionUseWeights;
    }

    public boolean getOptionKeepRootLabels() {
        return optionKeepRootLabels;
    }

    public void setOptionKeepRootLabels(boolean optionKeepRootLabels) {
        this.optionKeepRootLabels = optionKeepRootLabels;
    }

    public boolean getOptionTreesEdgesCubic() {
        return optionTreesEdgesCubic;
    }

    public void setOptionTreesEdgesCubic(boolean optionTreesEdgesCubic) {
        this.optionTreesEdgesCubic = optionTreesEdgesCubic;
    }

    public boolean getOptionReticulateEdgesCubic() {
        return optionReticulateEdgesCubic;
    }

    public void setOptionReticulateEdgesCubic(boolean optionReticulateEdgesCubic) {
        this.optionReticulateEdgesCubic = optionReticulateEdgesCubic;
    }
}
