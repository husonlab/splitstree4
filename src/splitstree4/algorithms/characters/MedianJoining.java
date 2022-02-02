/*
 * MedianJoining.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.characters;

import jloda.graph.Edge;
import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloSplitsGraph;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;

import java.util.*;

/**
 * computes splits from binary data  and draws them using the convex hull algorithm
 *
 * @author huson
 * Date: 16-Feb-2004
 */
public class MedianJoining extends QuasiMedianBase implements Characters2Network {
    public final static String DESCRIPTION = "Median Joining algorithm (Bandelt et al, 1999)";

    private int optionEpsilon = 0;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return taxa != null && chars != null && chars.getNcolors() < 8; // not too  many different states
    }

    /**
     * runs the median joining algorithm
     *
     * @return median joining network
     */
    public PhyloSplitsGraph computeGraph(ProgressListener progressListener, Set inputSequences, double[] weights) throws CanceledException {
        System.err.println("Computing the median joining network for epsilon=" + getOptionEpsilon());
        PhyloSplitsGraph graph;
        Set outputSequences = new HashSet();
        computeMedianJoiningMainLoop(progressListener, inputSequences, weights, getOptionEpsilon(), outputSequences);
        boolean changed;
        do {
            graph = new PhyloSplitsGraph();
            EdgeSet feasibleLinks = new EdgeSet(graph);
            computeMinimumSpanningNetwork(outputSequences, weights, 0, graph, feasibleLinks);
            List toDelete = new LinkedList();
            for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                if (!feasibleLinks.contains(e))
                    toDelete.add(e);
            }
            for (Object aToDelete : toDelete) graph.deleteEdge((Edge) aToDelete);
            changed = removeObsoleteNodes(graph, inputSequences, outputSequences, feasibleLinks);
            progressListener.incrementProgress();
        }
        while (changed);
        return graph;
    }

    /**
     * Main loop of the median joining algorithm
     *
     */
    private void computeMedianJoiningMainLoop(ProgressListener progressListener, Set input, double[] weights, int epsilon, Set<String> outputSequences) throws CanceledException {
        outputSequences.addAll(input);

        boolean changed = true;
        while (changed) {
            System.err.println("Median joining: " + outputSequences.size() + " sequences");
            progressListener.incrementProgress();
            changed = false;
            PhyloSplitsGraph graph = new PhyloSplitsGraph();
            EdgeSet feasibleLinks = new EdgeSet(graph);
            computeMinimumSpanningNetwork(outputSequences, weights, epsilon, graph, feasibleLinks);
            if (removeObsoleteNodes(graph, input, outputSequences, feasibleLinks)) {
                changed = true;   // sequences have been changed, recompute graph
            } else {
                // determine min connection cost:
                double minConnectionCost = Double.MAX_VALUE;

                for (Node u = graph.getFirstNode(); u != null; u = u.getNext()) {
                    String seqU = (String) u.getInfo();
                    for (Edge e = u.getFirstAdjacentEdge(); e != null; e = u.getNextAdjacentEdge(e)) {
                        Node v = e.getOpposite(u);
                        String seqV = (String) v.getInfo();
                        for (Edge f = u.getNextAdjacentEdge(e); f != null; f = u.getNextAdjacentEdge(f)) {
                            Node w = f.getOpposite(u);
                            String seqW = (String) w.getInfo();
                            String[] qm = computeQuasiMedian(seqU, seqV, seqW);
                            for (String aQm : qm) {
                                if (!outputSequences.contains(aQm)) {
                                    double cost = computeConnectionCost(seqU, seqV, seqW, aQm, weights);
                                    if (cost < minConnectionCost)
                                        minConnectionCost = cost;
                                }
                            }
                        }
                    }
                }
                for (Edge e : feasibleLinks) {
                    Node u = e.getSource();
                    Node v = e.getTarget();
                    String seqU = (String) u.getInfo();
                    String seqV = (String) v.getInfo();
                    for (Edge f : feasibleLinks.successors(e)) {
                        Node w;
                        if (f.getSource() == u || f.getSource() == v)
                            w = f.getTarget();
                        else if (f.getTarget() == u || f.getTarget() == v)
                            w = f.getSource();
                        else
                            continue;
                        String seqW = (String) w.getInfo();
                        String[] qm = computeQuasiMedian(seqU, seqV, seqW);
                        for (String aQm : qm) {
                            if (!outputSequences.contains(aQm)) {
                                double cost = computeConnectionCost(seqU, seqV, seqW, aQm, weights);
                                if (cost <= minConnectionCost + epsilon) {
                                    outputSequences.add(aQm);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * computes the minimum spanning network upto a tolerance of epsilon
     *
     */
    private void computeMinimumSpanningNetwork(Set sequences, double[] weights, int epsilon, PhyloSplitsGraph graph, EdgeSet feasibleLinks) {
        String[] array = (String[]) sequences.toArray(new String[0]);
        // compute a distance matrix between all sequences:
        double[][] matrix = new double[array.length][array.length];

        SortedMap<Double, List<Pair<Integer, Integer>>> value2pairs = new TreeMap<>();

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
				matrix[i][j] = computeDistance(array[i], array[j], weights);
				Double value = matrix[i][j];
				List<Pair<Integer, Integer>> pairs = value2pairs.computeIfAbsent(value, k -> new ArrayList<>());
				pairs.add(new Pair(i, j));
			}
        }

        Node[] nodes = new Node[array.length];
        int[] componentsOfMSN = new int[array.length];
        int[] componentsOfThresholdGraph = new int[array.length];

        for (int i = 0; i < array.length; i++) {
            nodes[i] = graph.newNode(array[i]);
            graph.setLabel(nodes[i], array[i]);
			componentsOfMSN[i] = i;
			componentsOfThresholdGraph[i] = i;
		}

		int numComponentsMSN = array.length;

		// TODO: This implementation of the minimum spanning network is wrong, add only edges between different connected components

		double maxValue = Double.MAX_VALUE;
		// all sets of edges in ascending order of lengths
		for (Double o : value2pairs.keySet()) {
			Double value = o;
			double threshold = value;
			if (threshold > maxValue)
				break;
			var ijPairs = (List<Pair<Integer, Integer>>) value2pairs.get(value);

			// update threshold graph components:
			for (int i = 0; i < array.length; i++) {
				for (int j = i + 1; j < array.length; j++) {
					if (componentsOfThresholdGraph[i] != componentsOfThresholdGraph[j] && matrix[i][j] < threshold - epsilon) {
						int oldComponent = componentsOfThresholdGraph[i];
						int newComponent = componentsOfThresholdGraph[j];
						for (int k = 0; k < array.length; k++) {
							if (componentsOfThresholdGraph[k] == oldComponent)
								componentsOfThresholdGraph[k] = newComponent;
						}
					}
				}
			}

			// determine new edges for minimum spanning network and determine feasible links
			var newPairs = new ArrayList<Pair<Integer, Integer>>();
			for (var ijPair : ijPairs) {
				int i = ijPair.getFirst();
				int j = ijPair.getSecond();

				Edge e = graph.newEdge(nodes[i], nodes[j]);
				graph.setWeight(e, matrix[i][j]);

				if (feasibleLinks != null && componentsOfThresholdGraph[i] != componentsOfThresholdGraph[j]) {
					feasibleLinks.add(e);
					if (false)
						System.err.println("ERROR nodes are connected: " + i + ", " + j);
				}
				newPairs.add(new Pair<>(i, j));
			}

			// update MSN components
			for (var pair : newPairs) {
				int i = pair.getFirst();
				int j = pair.getSecond();
				if (componentsOfMSN[i] != componentsOfMSN[j]) {
					numComponentsMSN--;
					int oldComponent = componentsOfMSN[i];
					int newComponent = componentsOfMSN[j];
					for (int k = 0; k < array.length; k++)
						if (componentsOfMSN[k] == oldComponent)
							componentsOfMSN[k] = newComponent;
				}
			}
			if (numComponentsMSN == 1 && maxValue == Double.MAX_VALUE)
                maxValue = threshold + epsilon; // once network is connected, add all edges upto threshold+epsilon
        }
    }

    /**
     * determine whether v and target are connected by a chain of edges all of weight-threshold. Use for debugging
     *
     * @return true, if connected
     */
    private boolean areConnected(PhyloSplitsGraph graph, Node v, Node target, NodeSet visited, double threshold) {
        if (v == target)
            return true;

        if (!visited.contains(v)) {
            visited.add(v);

            for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                if (graph.getWeight(e) < threshold) {
                    Node w = e.getOpposite(v);
                    if (areConnected(graph, w, target, visited, threshold))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * iteratively removes all nodes that are connected to only two other and are not part of the original input
     *
     * @return true, if anything was removed
     */
    private boolean removeObsoleteNodes(PhyloSplitsGraph graph, Set input, Set sequences, EdgeSet feasibleLinks) {
        int removed = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            List toDelete = new LinkedList();

            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                String seqV = (String) v.getInfo();
                if (!input.contains(seqV)) {
                    int count = 0;
                    for (Edge e = v.getFirstAdjacentEdge(); count <= 2 && e != null; e = v.getNextAdjacentEdge(e)) {
                        if (feasibleLinks.contains(e))
                            count++;
                    }
                    if (count <= 2)
                        toDelete.add(v);
                }
            }
            if (toDelete.size() > 0) {
                changed = true;
                removed += toDelete.size();
                for (Object aToDelete : toDelete) {
                    Node v = (Node) aToDelete;
                    sequences.remove(v.getInfo());
                    graph.deleteNode(v);
                }
            }
        }
        return removed > 0;
    }


    /**
     * compute the cost of connecting seqM to the other three sequences
     *
     * @return cost
     */
    private double computeConnectionCost(String seqU, String seqV, String seqW, String seqM, double[] weights) {
        return computeDistance(seqU, seqM, weights) + computeDistance(seqV, seqM, weights) + computeDistance(seqW, seqM, weights);
    }

    /**
     * compute weighted distance between two sequences
     *
     * @return distance
     */
    private double computeDistance(String seqA, String seqB, double[] weights) {
        double cost = 0;
        for (int i = 0; i < seqA.length(); i++) {
            if (seqA.charAt(i) != seqB.charAt(i))
                if (weights != null)
                    cost += weights[i];
                else
                    cost++;
        }
        return cost;
    }


    /**
     * computes the quasi median for three sequences
     *
     * @return quasi median
     */
    private String[] computeQuasiMedian(String seqA, String seqB, String seqC) {
        StringBuilder buf = new StringBuilder();
        boolean hasStar = false;
        for (int i = 0; i < seqA.length(); i++) {
            if (seqA.charAt(i) == seqB.charAt(i) || seqA.charAt(i) == seqC.charAt(i))
                buf.append(seqA.charAt(i));
            else if (seqB.charAt(i) == seqC.charAt(i))
                buf.append(seqB.charAt(i));
            else {
                buf.append("*");
                hasStar = true;
            }
        }
        if (!hasStar)
            return new String[]{buf.toString()};

        Set median = new HashSet();
        Stack stack = new Stack();
        stack.add(buf.toString());
        while (!stack.empty()) {
            String seq = (String) stack.pop();
            int pos = seq.indexOf('*');
            int pos2 = seq.indexOf('*', pos + 1);
            String first = seq.substring(0, pos) + seqA.charAt(pos) + seq.substring(pos + 1);
            String second = seq.substring(0, pos) + seqB.charAt(pos) + seq.substring(pos + 1);
            String third = seq.substring(0, pos) + seqC.charAt(pos) + seq.substring(pos + 1);
            if (pos2 == -1) {
                median.add(first);
                median.add(second);
                median.add(third);
            } else {
                stack.add(first);
                stack.add(second);
                stack.add(third);
            }
        }
		return (String[]) median.toArray(new String[0]);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public int getOptionEpsilon() {
        return optionEpsilon;
    }

    public void setOptionEpsilon(int optionEpsilon) {
        this.optionEpsilon = optionEpsilon;
    }
}
