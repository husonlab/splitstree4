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

package splitstree.algorithms.characters;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import splitstree.algorithms.distances.MinSpanningNetworkForDistances;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;

import java.util.*;


/**
 * minimal spanning network
 * Daniel Huson and David Bryant, 2.2008
 */
public class MinSpanningNetwork extends QuasiMedianBase implements Characters2Network {
    public final static String DESCRIPTION = "Computes the Minimum Spanning Network (Excoffier & Smouse, 1994)";

    final MinSpanningNetworkForDistances msn = new MinSpanningNetworkForDistances();

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa       the taxa
     * @param characters the distances matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters characters) {
        return doc.isValid(taxa) && doc.isValid(characters);
    }

    public PhyloGraph computeGraph(ProgressListener progressListener, Set inputSequences, double[] weights) throws CanceledException {
        PhyloGraph graph = new PhyloGraph();
        computeMinimumSpanningNetwork(inputSequences, weights, getOptionEpsilon(), graph);
        return graph;
    }

    /**
     * computes the minimum spanning network upto a tolerance of epsilon
     *
     * @param sequences
     * @param weights
     * @param epsilon
     * @param graph
     */
    private void computeMinimumSpanningNetwork(Set sequences, double[] weights, int epsilon, PhyloGraph graph) {
        String[] array = (String[]) sequences.toArray(new String[sequences.size()]);
        // compute a distance matrix between all sequences:
        double[][] matrix = new double[array.length][array.length];

        SortedMap value2pairs = new TreeMap();

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                matrix[i][j] = computeDistance(array[i], array[j], weights);
                Double value = matrix[i][j];
                List pairs = (List) value2pairs.get(value);
                if (pairs == null) {
                    pairs = new LinkedList();
                    value2pairs.put(value, pairs);
                }
                pairs.add(new Pair(i, j));
            }
        }

        if (false) {
            System.err.println("Distance matrix:");
            for (int i = 0; i < array.length; i++) {
                for (int j = 0; j < array.length; j++) {
                    System.err.print(" " + matrix[i][j]);
                }
                System.err.println();
            }
        }

        Node[] nodes = new Node[array.length];
        int[] componentsOfMSN = new int[array.length];

        for (int i = 0; i < array.length; i++) {
            nodes[i] = graph.newNode(array[i]);
            graph.setLabel(nodes[i], array[i]);
            componentsOfMSN[i] = i;
        }

        int numComponentsMSN = array.length;

        double maxValue = Double.MAX_VALUE;
        // all sets of edges in ascending order of lengths
        for (Object o : value2pairs.keySet()) {
            Double value = (Double) o;
            double threshold = value;
            if (threshold > maxValue)
                break;
            List ijPairs = (List) value2pairs.get(value);

            if (false) {
                System.err.println("Graph: " + graph.toString());
            }

            // determine new edges for minimum spanning network and determine feasible links
            List newPairs = new LinkedList();
            for (Object ijPair1 : ijPairs) {
                Pair ijPair = (Pair) ijPair1;
                int i = ijPair.getFirstInt();
                int j = ijPair.getSecondInt();

                Edge e = graph.newEdge(nodes[i], nodes[j]);
                graph.setWeight(e, matrix[i][j]);
                newPairs.add(new Pair(i, j));
            }

            // update MSN components
            for (Object newPair : newPairs) {
                Pair pair = (Pair) newPair;
                int i = pair.getFirstInt();
                int j = pair.getSecondInt();
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
     * compute weighted distance between two sequences
     *
     * @param seqA
     * @param seqB
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
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public int getOptionEpsilon() {
        return msn.getOptionEpsilon();
    }

    public void setOptionEpsilon(int optionEpsilon) {
        this.msn.setOptionEpsilon(optionEpsilon);
    }
}
