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

package splitstree.algorithms.distances;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Pair;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * minimal spanning network
 * Daniel Huson and David Bryant, 2.2008
 */
public class MinSpanningNetworkForDistances implements Distances2Network {
    public final static String DESCRIPTION = "Computes the Minimum Spanning Network (Excoffier & Smouse, 1994)";

    private int optionSpringEmbedderIterations = 1000;
    private int optionNodeSize = 15;
    private boolean optionCollapseIdentical = true;
    private boolean optionSubdivideEdges = true;
    private int optionEpsilon = 0;
    Random rand;

    /**
     * Applies the method to the given data
     *
     * @param taxa      the input taxa
     * @param distances the input distances
     * @return the computed network as a Network objec t
     */
    public Network apply(Document doc, Taxa taxa, Distances distances) {
        PhyloGraphView graphView = new PhyloGraphView();
        PhyloGraph graph = graphView.getPhyloGraph();

        // setup nodes
        Node[] taxon2node = new Node[taxa.getNtax() + 1];
        for (int t = 1; t <= taxa.getNtax(); t++) {
            Node v = taxon2node[t] = graph.newNode();
            graph.setNode2Taxa(v, t);
            graph.setTaxon2Node(t, v);
        }

        // compute minimum spanning network using algorithm described in Bandelt1999
        int[] component = new int[taxa.getNtax() + 1];
        for (int t = 1; t <= taxa.getNtax(); t++)
            component[t] = t;

        int numComponents = taxa.getNtax();

        SortedMap<Double, List<Pair<Integer, Integer>>> value2pairs = new TreeMap<>();
        for (int s = 1; s <= taxa.getNtax(); s++) {
            for (int t = s + 1; t <= taxa.getNtax(); t++) {
                Double value = distances.get(s, t);
                List<Pair<Integer, Integer>> pairs = value2pairs.get(value);
                if (pairs == null) {
                    {
                        pairs = new LinkedList<>();
                        value2pairs.put(value, pairs);
                    }
                }
                pairs.add(new Pair<>(s, t));
            }
        }

        double maxValue = Double.MAX_VALUE;

        for (Double threshold : value2pairs.keySet()) {
            //System.err.println("Threshold: "+threshold+" pairs: "+ value2pairs.get(threshold).size());
            List<Pair<Integer, Integer>> toAdd = new LinkedList<>();
            for (Pair pair : value2pairs.get(threshold)) {
                int s = pair.getFirstInt();
                int t = pair.getSecondInt();
                if (distances.get(s, t) == threshold && component[s] != component[t])
                    toAdd.add(new Pair<>(s, t));
            }

            for (Pair pair : toAdd) {
                int s = pair.getFirstInt();
                int t = pair.getSecondInt();
                // System.err.println("s "+s+" t "+t+" dist: "+distances.get(s, t));
                //System.err.println("components: "+component[s]+" "+component[t]);

                Edge e = graph.newEdge(taxon2node[s], taxon2node[t]);
                graph.setWeight(e, distances.get(s, t));

                if (component[s] != component[t]) {
                    numComponents--;
                    int newComponent = component[s];
                    int oldComponent = component[t];
                    for (int x = 1; x < component.length; x++) {
                        if (component[x] == oldComponent)
                            component[x] = newComponent;
                    }
                }
                if (numComponents == 1 && maxValue == Double.MAX_VALUE)
                    maxValue = threshold + getOptionEpsilon();
            }
            if (threshold > maxValue)
                break;
        }

        /*
        // process nodes:
        for (int s = 2; s <= taxa.getNtax(); s++) {
            double min = Double.MAX_VALUE;
            for (int t = 1; t <= s - 1; t++) {
                min = Math.min(distances.get(s, t), min);
            }
            for (int t = 1; t <= s - 1; t++) {
                if (distances.get(s, t) <= min) {
                    Edge e = graph.newEdge(taxon2node[s], taxon2node[t]);
                    graph.setWeight(e, min);
                }
            }
        }
        */

        // collapse identical nodes, if desired:
        if (getOptionCollapseIdentical()) {
            for (int s = 1; s <= taxa.getNtax(); s++) {
                Node v = taxon2node[s];
                if (v != null) {
                    for (int t = s + 1; t <= taxa.getNtax(); t++) {
                        Node w = taxon2node[t];
                        if (w != null && w != v) {
                            if (distances.get(s, t) <= 0) {
                                java.util.List<Integer> list = graph.getNode2Taxa(w);
                                if (list != null)
                                    for (Integer z : list) {
                                        graph.setNode2Taxa(v, z);
                                        graph.setTaxon2Node(z, v);
                                    }
                                taxon2node[t] = null;
                                // find all subdiving nodes for deletion
                                for (Edge e = w.getFirstAdjacentEdge(); e != null; e = w.getNextAdjacentEdge(e)) {
                                    Node u = e.getOpposite(w);
                                    if (u != v && graph.getCommonEdge(u, v) == null) {
                                        Edge f;
                                        if (u == e.getSource())
                                            f = graph.newEdge(u, v);
                                        else
                                            f = graph.newEdge(v, u);
                                        graph.setWeight(f, graph.getWeight(e));
                                        graph.setConfidence(f, graph.getConfidence(e));
                                    }
                                }
                                graph.deleteNode(w);
                            }
                        }
                    }
                }
            }

            java.util.List<Node> toDelete = new LinkedList<>();
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getDegree() == 1 && (graph.getNode2Taxa(v) == null || graph.getNode2Taxa(v).size() == 0)) {
                    toDelete.add(v);
                }
            }

            for (Node v : toDelete) {
                graph.deleteNode(v);
            }
        }

        // add subdivision nodes, if desired

        if (getOptionSubdivideEdges()) {
            java.util.List<Edge> originalEdges = new LinkedList<>();
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
                originalEdges.add(e);
            }
            for (Edge e : originalEdges) {
                Node v = e.getSource();
                Node w = e.getTarget();
                int s = graph.getNode2Taxa(v).get(0);
                int t = graph.getNode2Taxa(w).get(0);
                int dist = (int) (Math.round(distances.get(s, t)));
                if (dist > 1) {
                    Node prev = v;
                    for (int i = 1; i < dist; i++) {
                        Node u = graph.newNode();
                        Edge f = graph.newEdge(prev, u);
                        graph.setWeight(f, 1);
                        prev = u;
                    }
                    Edge f = graph.newEdge(prev, w);
                    graph.setWeight(f, 1);
                    graph.deleteEdge(e);
                }
            }
        }

        // add labels to nodes and set other stuff:
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (graph.getNode2Taxa(v) != null && graph.getNode2Taxa(v).size() > 0) {
                StringBuilder buf = new StringBuilder();
                java.util.List<Integer> list = graph.getNode2Taxa(v);
                if (list != null) {
                    boolean first = true;
                    for (Integer z : list) {
                        if (first)
                            first = false;
                        else
                            buf.append(",");
                        buf.append(taxa.getLabel(z));
                    }
                }
                graphView.setLabel(v, buf.toString());
                int h = getOptionNodeSize();
                if (graph.getNode2Taxa(v) != null)
                    h += getOptionNodeSize() * graph.getNode2Taxa(v).size();
                graphView.setHeight(v, h);
                graphView.setWidth(v, h);
                graphView.getNV(v).setBackgroundColor(Color.WHITE);
                graphView.setShape(v, NodeView.OVAL_NODE);
            } else {
                graphView.setHeight(v, 1);
                graphView.setWidth(v, 1);
                graphView.setShape(v, NodeView.RECT_NODE);
                graphView.setBackgroundColor(v, Color.BLACK);
            }
            graphView.getNV(v).setLabelLayout(NodeView.CENTRAL);
        }

        graphView.computeSpringEmbedding(getOptionSpringEmbedderIterations(), false);

        doc.getAssumptions().setAutoLayoutNodeLabels(false);
        doc.getAssumptions().setRadiallyLayoutNodeLabels(false);

        Network network = new Network(taxa, graphView);
        network.setLayout(Network.CIRCULAR);
        return network;

    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return doc.isValid(taxa) && doc.isValid(distances);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public int getOptionSpringEmbedderIterations() {
        return optionSpringEmbedderIterations;
    }

    public void setOptionSpringEmbedderIterations(int optionSpringEmbedderIterations) {
        this.optionSpringEmbedderIterations = optionSpringEmbedderIterations;
    }

    public boolean getOptionCollapseIdentical() {
        return optionCollapseIdentical;
    }

    public void setOptionCollapseIdentical(boolean optionCollapseIdentical) {
        this.optionCollapseIdentical = optionCollapseIdentical;
    }

    public boolean getOptionSubdivideEdges() {
        return optionSubdivideEdges;
    }

    public void setOptionSubdivideEdges(boolean optionSubdivideEdges) {
        this.optionSubdivideEdges = optionSubdivideEdges;
    }

    public int getOptionNodeSize() {
        return optionNodeSize;
    }

    public void setOptionNodeSize(int optionNodeSize) {
        this.optionNodeSize = optionNodeSize;
    }

    public int getOptionEpsilon() {
        return optionEpsilon;
    }

    public void setOptionEpsilon(int optionEpsilon) {
        this.optionEpsilon = optionEpsilon;
    }
}
