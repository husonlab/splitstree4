/**
 * ReticulateNetworkExport.java
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
package splitstree4.externalIO.exports;

import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Reticulate;
import splitstree4.nexus.Taxa;

import java.io.Writer;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 27.03.2006
 * Time: 15:05:28
 * To change this template use File | Settings | File Templates.
 */
public class ReticulateNetworkExport extends ExporterAdapter implements Exporter {

    private String Description = "Save a reticulte network";

    /**
     * can we import this data?
     *
     * @param doc param blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document doc, Collection blocks) {
        if (false) return false;
        if (blocks.size() != 1 || !blocks.contains(Reticulate.NAME))
            return false;
        return !(doc != null && !doc.isValidByName(Reticulate.NAME));
    }


    /**
     * convert input into nexus format
     *
     * @param doc
     * @return null
     */
    public Map apply(Writer w, Document doc, Collection blockNames) throws Exception {
        Taxa taxa = doc.getTaxa();
        Network net = doc.getNetwork();
        PhyloGraphView graphView = new PhyloGraphView();
        net.syncNetwork2PhyloGraphView(taxa, doc.getSplits(), graphView);
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        Node root = null;
        Iterator it = graph.nodes().iterator();
        while (root == null && it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getInDegree() == 0) root = n;
        }
        if (root == null)
            return null; //@todo give error message
        else {
            try {
                System.out.println("root: " + root);
                HashSet usedLabels = new HashSet();
                it = taxa.getAllLabels().iterator();
                while (it.hasNext()) usedLabels.add(it.next());
                HashMap rTaxaName2subtree = new HashMap();
                String rootBackbone = recMakeNewick(root, graph, taxa, new HashMap(), rTaxaName2subtree, usedLabels);
                String sortedRSubtrees = sortRSubtrees(rTaxaName2subtree);
                System.out.println("rootBackbone: " + rootBackbone);
                for (Object key : rTaxaName2subtree.keySet()) {
                    System.out.println("name: " + key + "\tsubtree: " + rTaxaName2subtree.get(key));

                }
                System.out.println("return value:\n" + sortedRSubtrees + rootBackbone);
                w.write(sortedRSubtrees + rootBackbone);
            } catch (Exception e) {
                //@todo give error message
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private String sortRSubtrees(HashMap rTaxaName2subtree) {
        HashSet rTaxaNames = new HashSet();
        rTaxaNames.addAll(rTaxaName2subtree.keySet());
        HashMap rTaxaName2Node = new HashMap();
        HashMap node2rTaxaName = new HashMap();
        // build Sort graph
        Graph g = new Graph();
        Iterator it = rTaxaNames.iterator();
        while (it.hasNext()) {
            Node n = g.newNode();
            Object name = it.next();
            rTaxaName2Node.put(name, n);
            node2rTaxaName.put(n, name);
        }
        it = rTaxaNames.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String subtree = (String) rTaxaName2subtree.get(key);
            for (Object rTaxaName : rTaxaNames) {
                String rName = (String) rTaxaName;
                if (subtree.contains(rName)) {// subtree contains rName
                    g.newEdge((Node) rTaxaName2Node.get(key), (Node) rTaxaName2Node.get(rName));
                }
            }
        }
        // some Systemout
        System.out.println("sorting Graph: \n--------------------------");
        it = rTaxaName2Node.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            System.out.println("NODE: " + rTaxaName2Node.get(key) + "\tlabel: " + key);
        }
        System.out.println("------------------------");
        for (var e : g.edges()) {
            System.out.println("EDGE: " + e);
        }

        // sort nodes by their OutDegree
        ArrayList<Node> list = new ArrayList<>();

        it = g.nodes().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            System.out.println("Node: " + n + "\toutdegree: " + n.getOutDegree());
            list.add(n);
        }
        StringBuilder re = new StringBuilder();

        list.sort(new Comparator<Node>() {
            public int compare(Node n1, Node n2) {
                if (n1.equals(n2))
                    return 0;
                else if (n1.getOutDegree() > n2.getOutDegree())
                    return 1;
                else
                    return -1;
            }
        });
        for (Node next : list) {
            String name = (String) node2rTaxaName.get(next);
            re.append(name).append("=").append(rTaxaName2subtree.get(name)).append(";\n");
        }
        return re.toString();
    }

    private String recMakeNewick(Node start, PhyloSplitsGraph graph, Taxa taxa, HashMap node2rTaxaName, HashMap rTaxaName2subtree, HashSet usedLabels) throws Exception {
        StringBuilder subtree = new StringBuilder();
        int rCount = 1;
        boolean first = true;

        for (Node next : start.adjacentNodes()) {
            Edge e = start.getCommonEdge(next);
            if (e.getSource().equals(start)) {
                // check for leaf
                if (first)
                    first = false;
                else
                    subtree.append(",");
                if (next.getOutDegree() == 0) { // leaf
                    System.out.println("is leaf: " + next);
                    subtree.append(graph.getLabel(next)).append(":").append(graph.getWeight(e));
                } else if (next.getInDegree() == 1) { // tree edge
                    System.out.println("is tree edge to node: " + next);
                    subtree.append(recMakeNewick(next, graph, taxa, node2rTaxaName, rTaxaName2subtree, usedLabels)).append(":").append(graph.getWeight(e));
                } else if (next.getInDegree() == 2) { // reticulation edge
                    System.out.println("is reticulation edge to node: " + next);
                    subtree.append(recMakeNewick(next, graph, taxa, node2rTaxaName, rTaxaName2subtree, usedLabels)).append(":").append(graph.getWeight(e));
                } else // no reticulation network
                    throw new Exception("no reticulation network");
            }
        }
        if (start.getInDegree() < 2) {
            System.out.println("returning subtree: " + "(" + subtree.toString() + ")");
            return "(" + subtree.toString() + ")";
        } else if (start.getInDegree() == 2) {
            if (node2rTaxaName.get(start) == null) {// first time we visit this node
                String rLabel;
                if (graph.getLabel(start) != null)
                    rLabel = graph.getLabel(start);
                else {
                    while (usedLabels.contains(("r_" + rCount + ""))) rCount++;
                    rLabel = "r_" + rCount;
                }
                System.out.println("new reticulation node with label: " + rLabel + " and subtree: " + subtree);
                usedLabels.add(rLabel);
                node2rTaxaName.put(start, rLabel);
                rTaxaName2subtree.put(rLabel, subtree.toString());
                System.out.println("returning subtree: " + rLabel);
                return rLabel;

            } else { // we have allready visited this node and have a name for it
                System.out.println("is known reticulation subtree: " + start + "\trLabel: " + node2rTaxaName.get(start));
                System.out.println("returning subtree: " + node2rTaxaName.get(start));
                return (String) node2rTaxaName.get(start);
            }
        } else {
            System.out.println("returning wrong subtree because node: " + start + "\t has indegree: " + graph.getInDegree(start) + ": " + subtree);
            throw new Exception("no reticulation network");

        }

    }


    public String getDescription() {
        return Description;
    }
}
