/**
 * PrunedQuasiMedian.java
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
package splitstree4.algorithms.characters;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;

import java.util.*;

/**
 * implementation of the     PrunedQuasiMedian method
 *
 * @author huson
 * Date: 2009
 */
public class PrunedQuasiMedian extends QuasiMedianBase implements Characters2Network {
    public final static String DESCRIPTION = "Geodesically-pruned quasi-median network (Ayling and Brown, BMC Bioinformatics 2008)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return taxa != null && chars != null && chars.getNcolors() < 6; // not too  many different states
    }

    /**
     * computes the actual graph
     *
     * @param progressListener
     * @param inputSequences
     * @param weights
     * @return
     * @throws CanceledException
     */
    public PhyloSplitsGraph computeGraph(ProgressListener progressListener, Set inputSequences, double[] weights) throws CanceledException {

        Set outputSequences = computeGeodesicPrunedQuasiMedianClosure(progressListener, inputSequences, weights.length);
        return computeOneStepGraph(outputSequences);

    }

    /**
     * compute the quasi median closure for the given set of sequences
     *
     * @param sequences
     * @param refA      if !=null, use this reference sequence in computation of quasi median
     * @param refB      if !=null, use this reference sequence in computation of quasi median
     * @return quasi median closure
     */
    public Set computeQuasiMedianClosure(Set sequences, String refA, String refB) {
        Set oldSequences = new TreeSet();
        Set curSequences = new HashSet();
        Set newSequences = new HashSet();

        //System.err.println("Computing quasi-median closure:");
        oldSequences.addAll(sequences);
        curSequences.addAll(sequences);

        while (curSequences.size() > 0) {
            String[] oldArray = (String[]) oldSequences.toArray(new String[oldSequences.size()]);
            newSequences.clear();
            for (String seqA : oldArray) {
                for (String seqB : oldArray) {
                    for (Object curSequence : curSequences) {
                        String seqC = (String) curSequence;
                        if (!seqC.equals(seqA) && !seqC.equals(seqB)) {
                            String[] medianSequences = refA != null ? computeQuasiMedian(seqA, seqB, seqC, refA, refB) :
                                    computeQuasiMedian(seqA, seqB, seqC);
                            for (String medianSequence : medianSequences) {
                                if (!oldSequences.contains(medianSequence) && !curSequences.contains(medianSequence)) {
                                    newSequences.add(medianSequence);
                                }
                            }
                        }
                    }
                }
            }
            oldSequences.addAll(curSequences);
            curSequences.clear();
            Set tmp = curSequences;
            curSequences = newSequences;
            newSequences = tmp;
            // System.err.println("Size: " + oldSequences.size());
        }
        return oldSequences;

    }

    /**
     * compute the quasi median closure for the given set of sequences
     *
     * @param sequences
     * @param sequenceLength
     * @return quasi median closure
     */
    private Set computeGeodesicPrunedQuasiMedianClosure(ProgressListener progressListener, Set sequences, int sequenceLength) throws CanceledException {
        Set result = new TreeSet();

        System.err.println("Computing geodesically-pruned quasi-median closure:");

        String[] input = (String[]) sequences.toArray(new String[sequences.size()]);

        double[][] scores = computeScores(input, sequenceLength);

        progressListener.setMaximum(input.length * input.length);    //initialize maximum progress


        for (int i = 0; i < input.length; i++) {
            for (int j = i + 1; j < input.length; j++) {
                //  System.err.println("Processing " + i + "," + j);
                BitSet use = new BitSet();
                for (int pos = 0; pos < sequenceLength; pos++) {
                    if (input[i].charAt(pos) != input[j].charAt(pos))
                        use.set(pos);
                }
                Set compressed = new HashSet();
                for (String anInput : input) {
                    StringBuilder buf = new StringBuilder();
                    for (int p = 0; p < sequenceLength; p++) {
                        if (use.get(p))
                            buf.append(anInput.charAt(p));
                    }
                    compressed.add(buf.toString());
                }
                StringBuilder bufA = new StringBuilder();
                StringBuilder bufB = new StringBuilder();

                for (int p = 0; p < sequenceLength; p++) {
                    if (use.get(p)) {
                        bufA.append(input[i].charAt(p));
                        bufB.append(input[j].charAt(p));
                    }
                }
                String refA = bufA.toString();
                String refB = bufB.toString();


                Set closure = computeQuasiMedianClosure(compressed, refA, refB);

                Set expanded = new HashSet();
                for (Object aClosure1 : closure) {
                    String current = (String) aClosure1;
                    StringBuilder buf = new StringBuilder();
                    int p = 0;
                    for (int k = 0; k < sequenceLength; k++) {
                        if (!use.get(k))
                            buf.append(input[i].charAt(k));
                        else // used in
                        {
                            buf.append(current.charAt(p++));
                        }
                    }
                    expanded.add(buf.toString());
                }
                Set geodesic = computeGeodesic(input[i], input[j], expanded, scores);
                result.add(input[i]);
                result.add(input[j]);
                result.addAll(geodesic);


                if (false) {
                    System.err.println("------Sequences :");
                    System.err.println(input[i]);
                    System.err.println(input[j]);
                    for (int x = 0; x < sequenceLength; x++)
                        System.err.print(use.get(x) ? "x" : " ");
                    System.err.println();
                    System.err.println("Refs:");

                    System.err.println(refA);
                    System.err.println(refB);
                    System.err.println("Compressed (" + compressed.size() + "):");
                    for (Object aCompressed : compressed) System.err.println(aCompressed);
                    System.err.println("Closure (" + closure.size() + "):");
                    for (Object aClosure : closure) System.err.println(aClosure);
                    System.err.println("Expanded (" + expanded.size() + "):");
                    for (Object anExpanded : expanded) System.err.println(anExpanded);
                    System.err.println("Geodesic (" + geodesic.size() + "):");
                    for (Object aGeodesic : geodesic) System.err.println(aGeodesic);
                }
                progressListener.setProgress(i * j);

            }
        }
        return result;
    }

    /**
     * compute the best geodesic between two nodes
     *
     * @param seqA
     * @param seqB
     * @param expanded
     * @param scores
     * @return geodesic
     */
    private Set computeGeodesic(String seqA, String seqB, Set expanded, double[][] scores) {
        Graph graph = new Graph();

        Node start = null;
        Node end = null;
        for (Object anExpanded : expanded) {
            String seq = (String) anExpanded;

            Node v = graph.newNode(seq);
            if (start == null && seq.equals(seqA))
                start = v;
            else if (end == null && seq.equals(seqB))
                end = v;
        }
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            String aSeq = (String) graph.getInfo(v);
            for (Node w = v.getNext(); w != null; w = w.getNext()) {
                String bSeq = (String) graph.getInfo(w);
                if (computeOneStep(aSeq, bSeq) != -1)
                    graph.newEdge(v, w);
            }
        }

        Set bestPath = new HashSet();
        NodeSet inPath = new NodeSet(graph);
        NodeDoubleArray bestScore = new NodeDoubleArray(graph, Double.NEGATIVE_INFINITY);
        inPath.add(start);
        // System.err.println("Finding best geodesic:");
        computeBestPathRec(graph, end, start, null, bestScore, inPath, 0, new HashSet(), bestPath, scores);
        return bestPath;
    }

    /**
     * get the best path from start to end
     *
     * @param end
     * @param v
     * @param e
     * @param currentScore
     * @param currentPath
     * @param bestPath
     */
    private void computeBestPathRec(Graph graph, Node end, Node v, Edge e, NodeDoubleArray bestScore, NodeSet inPath, double currentScore,
                                    HashSet currentPath,
                                    Set bestPath, double[][] scores) {
        if (v == end) {
            if (currentScore > bestScore.getValue(end)) {
                //  System.err.println("Updating best score: " + bestScore.getValue(end) + " -> " + currentScore);
                bestPath.clear();
                bestPath.addAll(currentPath);
                bestScore.set(v, currentScore);
            } else if (currentScore == bestScore.getValue(end)) {
                bestPath.addAll(currentPath); // don't break ties
            }
        } else {
            if (currentScore >= bestScore.getValue(v)) {
                bestScore.set(v, currentScore);
                for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                    if (f != e) {
                        Node w = f.getOpposite(v);
                        if (!inPath.contains(w)) {
                            inPath.add(w);
                            String seq = (String) graph.getInfo(w);
                            double add = getScore(seq, scores);
                            currentPath.add(seq);
                            computeBestPathRec(graph, end, w, f, bestScore, inPath, currentScore + add, currentPath, bestPath, scores);
                            currentPath.remove(seq);
                            inPath.remove(w);
                        }
                    }
                }
            }
        }
    }

    /**
     * computes the quasi median for three sequences
     *
     * @param seqA
     * @param seqB
     * @param seqC
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
        return (String[]) median.toArray(new String[median.size()]);
    }

    /**
     * computes the quasi median for three sequences. When resolving a three-way median, use only states in reference sequences
     *
     * @param seqA
     * @param seqB
     * @param seqC
     * @param refA
     * @param refB
     * @return quasi median
     */
    private String[] computeQuasiMedian(String seqA, String seqB, String seqC, String refA, String refB) {
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
            if (refA.charAt(pos) == seqA.charAt(pos) || refA.charAt(pos) == seqB.charAt(pos) || refA.charAt(pos) == seqC.charAt(pos)) {
                String first = seq.substring(0, pos) + refA.charAt(pos) + seq.substring(pos + 1);
                if (pos2 == -1) {
                    median.add(first);
                } else {
                    stack.add(first);
                }
            }
            if (refB.charAt(pos) != refA.charAt(pos)
                    && (refB.charAt(pos) == seqA.charAt(pos) || refB.charAt(pos) == seqB.charAt(pos) || refB.charAt(pos) == seqC.charAt(pos))) {
                String second = seq.substring(0, pos) + refB.charAt(pos) + seq.substring(pos + 1);
                if (pos2 == -1) {
                    median.add(second);
                } else {
                    stack.add(second);
                }
            }
        }
        return (String[]) median.toArray(new String[median.size()]);
    }

    /**
     * computes a log score for each state at each   position of the alignment
     *
     * @param input
     * @param sequenceLength
     * @return scores
     */
    private double[][] computeScores(String[] input, int sequenceLength) {
        double[][] scores = new double[sequenceLength][256];

        for (int pos = 0; pos < sequenceLength; pos++) {
            for (String anInput : input) {
                scores[pos][anInput.charAt(pos)]++;
            }
        }

        for (int pos = 0; pos < sequenceLength; pos++) {
            for (int i = 0; i < 256; i++) {
                if (scores[pos][i] != 0)
                    scores[pos][i] = Math.log(scores[pos][i] / input.length);
            }
        }
        return scores;
    }

    /**
     * get the log score of a sequence
     *
     * @param seq
     * @param scores
     * @return log score
     */
    private double getScore(String seq, double[][] scores) {
        double score = 0;
        for (int i = 0; i < seq.length(); i++)
            score += scores[i][seq.charAt(i)];
        return score;
    }

    /**
     * computes the one-step graph
     *
     * @param sequences
     * @return one-step graph
     */
    public PhyloSplitsGraph computeOneStepGraph(Set sequences) {
        PhyloSplitsGraph graph = new PhyloSplitsGraph();
        for (Object sequence : sequences) {
            String seq = (String) sequence;
            Node v = graph.newNode();
            graph.setLabel(v, seq);
            graph.setInfo(v, seq);
        }

        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            for (Node w = v.getNext(); w != null; w = w.getNext()) {
                int i = computeOneStep(graph.getLabel(v), graph.getLabel(w));
                if (i != -1)
                    graph.newEdge(v, w, "" + i);
            }
        }
        return graph;
    }

    /**
     * if two sequences differ at exactly one position, gets position
     *
     * @param seqa
     * @param seqb
     * @return single difference position or -1
     */
    private int computeOneStep(String seqa, String seqb) {
        int pos = -1;
        for (int i = 0; i < seqa.length(); i++) {
            if (seqa.charAt(i) != seqb.charAt(i)) {
                if (pos == -1)
                    pos = i;
                else
                    return -1;
            }
        }
        return pos;
    }


    /**
     * expand a condensed sequence
     *
     * @param condensed
     * @param orig2CondensedPos
     * @return expanded sequence
     */
    private String expandCondensed(String condensed, int[] orig2CondensedPos) {
        StringBuilder buf = new StringBuilder();

        for (int i = 1; i < orig2CondensedPos.length; i++)
            buf.append(condensed.charAt(orig2CondensedPos[i]));
        return buf.toString();
    }


    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }
}
