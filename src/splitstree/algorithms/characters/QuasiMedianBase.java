/**
 * QuasiMedianBase.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.algorithms.characters;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * base class for algorithms that produce quasi-median-type networks
 * <p/>
 * huson 10.2009
 */
public abstract class QuasiMedianBase {
    private boolean optionLabelEdges = false;
    private boolean optionShowHaplotypes = false;
    private int optionSpringEmbedderIterations = 2000;
    private boolean optionSubdivideEdges = false;
    private boolean optionScaleNodesByTaxa = false;

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param chars0 the characters matrix
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Characters chars0) throws CanceledException {
        doc.notifySubtask(Basic.getShortName(getClass()));
        doc.notifySetProgress(0);
        doc.notifySetMaximumProgress(100);    //initialize maximum progress

        char[][] origCharacters = computeUnmaskedCharacters(chars0);
        String[] origCharacterLabels = computeUnmaskedCharacterLabels(chars0);
        int ntax = taxa.getNtax();
        int nchar = origCharacterLabels.length - 1;

        int[] orig2CondensedPos = new int[nchar + 1];
        int[] orig2CondensedTaxa = new int[ntax + 1];

        final Translator translator = new Translator(); // translates between original character states and condensed ones

        // NOTE: in condensedCharacters we count taxa and positions starting from 0 (not 1, as otherwise in SplitsTree)
        String[] condensedCharacters = condenseCharacters(ntax, nchar, origCharacters, orig2CondensedPos, orig2CondensedTaxa, translator);

        BitSet[] condensed2OrigPos = invert(orig2CondensedPos);

        double[] weights = computeWeights(condensedCharacters[1].length(), orig2CondensedPos);

        if (false) {
            System.err.println("Translator:\n" + translator);

            System.err.println("Condensed characters:");
            for (String condensedCharacter1 : condensedCharacters) {
                System.err.println(condensedCharacter1);
            }

            System.err.println("uncondensed characters:");
            for (String condensedCharacter : condensedCharacters) {
                System.err.println(expandCondensed(condensedCharacter, orig2CondensedPos, translator));
            }

            System.err.println("Weights:");
            for (double weight : weights) {
                System.err.print(" " + weight);
            }
            System.err.println();
            System.err.println("Condensed to orig:");
            for (int t = 0; t < condensed2OrigPos.length; t++) {
                System.err.println(t + ": " + Basic.toString(condensed2OrigPos[t]));
            }
        }
        Set condensedInputSet = new TreeSet();
        Collections.addAll(condensedInputSet, condensedCharacters);

        PhyloGraph graph = computeGraph(doc.getProgressListener(), condensedInputSet, weights);

        PhyloGraphView view = new PhyloGraphView(graph, 400, 400);
        view.setMaintainEdgeLengths(false);
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            String condensed = (String) v.getInfo();

            if (condensedInputSet.contains(condensed)) {
                view.setShape(v, NodeView.OVAL_NODE);
                for (int t = 1; t <= taxa.getNtax(); t++) {
                    int o = orig2CondensedTaxa[t];
                    if (condensedCharacters[o].equals(condensed)) {
                        graph.setTaxon2Node(t, v);
                        graph.setNode2Taxa(v, t);
                    }
                }

                List vTaxa = graph.getNode2Taxa(v);
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (Object aVTaxa : vTaxa) {
                    if (first)
                        first = false;
                    else
                        buf.append(",");
                    buf.append(taxa.getLabel((Integer) aVTaxa));
                }
                view.setLabel(v, buf.toString());
            } else {
                view.setShape(v, NodeView.RECT_NODE);
                view.setHeight(v, 2);
                view.setWidth(v, 2);
                graph.setLabel(v, null);
                view.setLabel(v, null);
            }
            if (getOptionShowHaplotypes()) {
                String full = expandCondensed(condensed, orig2CondensedPos, translator);
                view.setLabel(v, full);
            }
        }

        if (getOptionScaleNodesByTaxa()) {
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                {
                    List vTaxa = graph.getNode2Taxa(v);
                    if (vTaxa != null && vTaxa.size() > 0) {
                        int size = (int) (4 + 10 * (Math.log(vTaxa.size())));
                        view.setHeight(v, size);
                        view.setBackgroundColor(v, Color.WHITE);
                        view.setWidth(v, size);
                    }
                }
            }
        }
        if (getOptionLabelEdges()) {
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
                String label = computeEdgeLabel(origCharacterLabels, (String) e.getSource().getInfo(), (String) e.getTarget().getInfo(), orig2CondensedPos, translator);
                view.setLabel(e, label);
                view.setLabelVisible(e, true);
            }
        }

        if (optionSubdivideEdges) {
            List originalEdges = new LinkedList();
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext())
                originalEdges.add(e);

            for (Object originalEdge : originalEdges) {
                Edge e = (Edge) (originalEdge);
                int[] differences = getDifferences((String) e.getSource().getInfo(), (String) e.getTarget().getInfo(), orig2CondensedPos, translator);
                if (differences.length > 1) {
                    Node prev = e.getSource();
                    for (int i = 1; i < differences.length; i++) {
                        Node u = graph.newNode();
                        Edge f = graph.newEdge(prev, u);
                        prev = u;
                        view.setShape(u, NodeView.RECT_NODE);
                        view.setHeight(u, 1);
                        view.setWidth(u, 1);
                        if (getOptionLabelEdges()) {
                            String label = origCharacterLabels[differences[i - 1]];
                            if (label == null)
                                label = "" + differences[i - 1];
                            view.setLabel(f, label);
                        }
                    }
                    Edge f = graph.newEdge(prev, e.getTarget());
                    if (getOptionLabelEdges()) {
                        String label = origCharacterLabels[differences[differences.length - 1]];
                        if (label == null)
                            label = "" + differences[differences.length - 1];
                        view.setLabel(f, label);
                    }
                    graph.deleteEdge(e);
                }

            }
        }
        view.computeSpringEmbedding(optionSpringEmbedderIterations, false);
        return new Network(taxa, view);
    }

    /**
     * compute the matrix of unmasked characters
     *
     * @param chars
     * @return unmasked characters
     */
    private char[][] computeUnmaskedCharacters(Characters chars) {
        List[] list = new LinkedList[chars.getNtax() + 1];
        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                char majorityState = 0;
                for (int t = 1; t <= chars.getNtax(); t++) {
                    char ch = chars.get(t, c);
                    if (list[t] == null)
                        list[t] = new LinkedList();
                    if (ch == chars.getFormat().getGap() || ch == chars.getFormat().getMissing()) {
                        if (majorityState == 0)
                            majorityState = determineMajorityState(chars, c);
                        ch = majorityState;
                    }
                    list[t].add(ch);
                }
            }
        }
        char[][] unmaskedCharacters = new char[chars.getNtax() + 1][list[1].size() + 1];
        for (int t = 1; t <= chars.getNtax(); t++) {
            int count = 0;
            for (Object o : list[t]) {
                unmaskedCharacters[t][++count] = (Character) o;
            }
        }
        return unmaskedCharacters;
    }

    /**
     * determines the majority state for the given position
     *
     * @param chars
     * @param c
     * @return majority state
     */
    private char determineMajorityState(Characters chars, int c) {
        BitSet states = new BitSet();
        int[] count = new int[256];

        for (int t = 1; t <= chars.getNtax(); t++) {
            char ch = chars.get(t, c);
            states.set(ch);
            count[ch]++;
        }

        int best = 0;
        for (int ch = states.nextSetBit(0); ch != -1; ch = states.nextSetBit(ch + 1)) {
            if (count[ch] > count[best])
                best = ch;
        }
        return (char) best;
    }

    /**
     * all labels of unmasked characters
     *
     * @param chars
     * @return
     */
    private String[] computeUnmaskedCharacterLabels(Characters chars) {
        List list = new LinkedList();
        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                String label = chars.getCharLabel(c);
                if (label == null)
                    label = "" + c;
                list.add(label);
            }
        }
        String[] labels = new String[list.size() + 1];
        int count = 0;
        for (Object aList : list) {
            labels[++count] = aList.toString();
        }
        return labels;
    }


    /**
     * computes the actual graph
     *
     * @param inputSequences
     * @param weights
     * @return median joining network
     */
    public abstract PhyloGraph computeGraph(ProgressListener progressListener, Set inputSequences, double[] weights) throws CanceledException;

    /**
     * computes all original positions at which the two sequences differ in display coordinates 1--length
     *
     * @param conA
     * @param conB
     * @param orig2CondensedPos
     * @return positions at which orig sequences differ
     */
    private String computeEdgeLabel(String[] labels, String conA, String conB, int[] orig2CondensedPos, Translator translator) {
        StringBuilder buf = new StringBuilder();

        String seqA = expandCondensed(conA, orig2CondensedPos, translator);
        String seqB = expandCondensed(conB, orig2CondensedPos, translator);

        boolean first = true;
        for (int i = 0; i < seqA.length(); i++) {
            if (seqA.charAt(i) != seqB.charAt(i)) {
                if (first)
                    first = false;
                else
                    buf.append(",");
                String label = labels[i + 1];
                if (label == null)
                    label = "" + (i + 1);
                buf.append(label);
            }
        }
        return buf.toString();
    }

    /**
     * gets the differences in display coordinates 1--length
     *
     * @param conA
     * @param conB
     * @param orig2CondensedPos
     * @return length
     */
    private int[] getDifferences(String conA, String conB, int[] orig2CondensedPos, Translator translator) {
        String seqA = expandCondensed(conA, orig2CondensedPos, translator);
        String seqB = expandCondensed(conB, orig2CondensedPos, translator);
        List list = new LinkedList();
        for (int i = 0; i < seqA.length(); i++) {
            if (seqA.charAt(i) != seqB.charAt(i)) {
                list.add(i + 1);
            }
        }
        int[] result = new int[list.size()];
        int count = 0;
        for (Object aList : list) {
            result[count++] = (Integer) aList;
        }
        return result;
    }

    /**
     * invert the orig 2 new mapping
     *
     * @param orig2new
     * @return new 2 orig mapping
     */
    private BitSet[] invert(int[] orig2new) {
        int maxValue = 0;
        for (int i = 1; i < orig2new.length; i++)
            maxValue = Math.max(orig2new[i], maxValue);
        BitSet[] new2orig = new BitSet[maxValue + 1];

        for (int i = 1; i < orig2new.length; i++) {
            int value = orig2new[i];
            if (new2orig[value] == null)
                new2orig[value] = new BitSet();
            new2orig[value].set(i);
        }
        return new2orig;
    }

    /**
     * computes the weights associated with the condensed characters
     *
     * @param numChars
     * @param origPos2CondensedPos
     * @return weights
     */
    private double[] computeWeights(int numChars, int[] origPos2CondensedPos) {
        int[] counts = new int[origPos2CondensedPos.length];

        for (int origPos2CondensedPo : origPos2CondensedPos) counts[origPos2CondensedPo]++;

        double[] weights = new double[numChars];
        int pos = 0;
        for (int count : counts) {
            if (count > 0)
                weights[pos++] = count;
        }
        return weights;
    }

    /**
     * expand a condensed sequence
     *
     * @param condensed
     * @param orig2CondensedPos
     * @return expanded sequence
     */
    private String expandCondensed(String condensed, int[] orig2CondensedPos, Translator translator) {
        StringBuilder buf = new StringBuilder();

        for (int origPos = 1; origPos < orig2CondensedPos.length; origPos++) {
            int conPos = orig2CondensedPos[origPos];
            char conChar = condensed.charAt(conPos);
            char origChar = translator.get(origPos, conPos, conChar);
            buf.append(origChar);


        }
        return buf.toString();
    }


    /**
     * computes the condensed sequences
     *
     * @param ntax
     * @param chars
     * @param origPos2CondensedPos
     * @param origTaxa2CondensedTaxa
     * @return array condensed sequences
     */
    private String[] condenseCharacters(int ntax, int nchar, char[][] chars, int[] origPos2CondensedPos, int[] origTaxa2CondensedTaxa,
                                        Translator translator) {
        // check that all columns differ:
        int[] samePosAs = new int[nchar + 1];
        for (int i = 1; i <= nchar; i++) {
            samePosAs[i] = i;
        }

        for (int i = 1; i <= nchar; i++) {
            for (int j = i + 1; j <= nchar; j++) {
                boolean same = true;
                char[] i2j = new char[256];
                char[] j2i = new char[256];

                for (int t = 1; same && t <= ntax; t++) {
                    char chari = chars[t][i];
                    char charj = chars[t][j];

                    if (i2j[chari] == (char) 0) {
                        i2j[chari] = charj;
                        if (j2i[charj] == (char) 0)
                            j2i[charj] = chari;
                        else if (j2i[charj] != chari)
                            same = false; // differ
                    } else if (i2j[chari] != charj)
                        same = false; // differ
                }
                if (same) {
                    samePosAs[j] = samePosAs[i];
                    break;
                }
            }
        }

        StringBuffer[] buffers = new StringBuffer[ntax + 1];
        for (int t = 1; t <= ntax; t++)
            buffers[t] = new StringBuffer();

        int newPos = 0;
        for (int i = 1; i < samePosAs.length; i++) {
            if (samePosAs[i] != 0) {
                if (samePosAs[i] < i) {
                    origPos2CondensedPos[i] = origPos2CondensedPos[samePosAs[i]];
                } else // sameAs[i]==i
                {
                    origPos2CondensedPos[i] = newPos++;
                    for (int t = 1; t <= ntax; t++)
                        buffers[t].append(chars[t][i]);
                }
                for (int t = 1; t <= ntax; t++) {
                    int conPos = origPos2CondensedPos[samePosAs[i]];
                    char chari = chars[t][i];
                    char charj = chars[t][samePosAs[i]];
                    translator.put(i, chari, conPos, charj);
                }
            }
        }
        // condensed positions start at 0
        //  for (int i = 1; i < origPos2CondensedPos.length; i++)
        //      origPos2CondensedPos[i]--;

        int[] sameTaxonAs = new int[ntax + 1];
        for (int s = 1; s <= ntax; s++) {
            sameTaxonAs[s] = s;
        }

        for (int s = 1; s <= ntax; s++) {
            String seqS = buffers[s].toString();
            for (int t = s + 1; t <= ntax; t++) {
                if (seqS.equals(buffers[t].toString()))
                    sameTaxonAs[t] = sameTaxonAs[s];
            }
        }

        int count = 0;
        List list = new LinkedList();
        for (int t = 1; t <= ntax; t++) {
            if (sameTaxonAs[t] < t)
                origTaxa2CondensedTaxa[t] = origTaxa2CondensedTaxa[sameTaxonAs[t]];
            else // sameTaxonAs[t]==t
            {
                origTaxa2CondensedTaxa[t] = (++count);
                list.add(buffers[t].toString());
            }
        }

        // condensed taxa start at 0
        for (int t = 1; t <= ntax; t++) {
            origTaxa2CondensedTaxa[t]--;
        }

        String[] result = new String[list.size()];
        int which = 0;
        for (Object aList : list) {
            result[which++] = (String) aList;
        }
        return result;
    }

    public boolean getOptionLabelEdges() {
        return optionLabelEdges;
    }

    public void setOptionLabelEdges(boolean optionLabelEdges) {
        this.optionLabelEdges = optionLabelEdges;
    }

    public int getOptionSpringEmbedderIterations() {
        return optionSpringEmbedderIterations;
    }

    public void setOptionSpringEmbedderIterations(int optionSpringEmbedderIterations) {
        this.optionSpringEmbedderIterations = optionSpringEmbedderIterations;
    }

    public boolean getOptionShowHaplotypes() {
        return optionShowHaplotypes;
    }

    public void setOptionShowHaplotypes(boolean optionShowHaplotypes) {
        this.optionShowHaplotypes = optionShowHaplotypes;
    }

    public boolean getOptionSubdivideEdges() {
        return optionSubdivideEdges;
    }

    public void setOptionSubdivideEdges(boolean optionSubdivideEdges) {
        this.optionSubdivideEdges = optionSubdivideEdges;
    }

    public boolean getOptionScaleNodesByTaxa() {
        return optionScaleNodesByTaxa;
    }

    public void setOptionScaleNodesByTaxa(boolean optionScaleNodesByTaxa) {
        this.optionScaleNodesByTaxa = optionScaleNodesByTaxa;
    }

    class Translator {
        Map mapOrigPosCondensedPosCondensedCharToOrigChar = new HashMap();
        int maxOrigPos = 0;
        int maxOrigChar = 0;
        int maxCondensedPos = 0;

        public void put(int origPos, char origChar, int condensedPos, char condensedChar) {
            maxOrigPos = Math.max(maxOrigPos, origPos);
            maxOrigChar = Math.max(maxOrigChar, origChar);
            maxCondensedPos = Math.max(maxCondensedPos, condensedPos);
            Triple triple = new Triple(origPos, condensedPos, condensedChar);
            Character ch = origChar;
            mapOrigPosCondensedPosCondensedCharToOrigChar.put(triple, ch);
        }

        public char get(int origPos, int condensedPos, char condensedChar) {
            Triple triple = new Triple(origPos, condensedPos, condensedChar);
            Character ch = (Character) mapOrigPosCondensedPosCondensedCharToOrigChar.get(triple);

            if (ch != null)
                return ch;
            else
                return (char) 0;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();

            for (int i = 0; i <= maxCondensedPos; i++) {
                for (int j = 1; j <= maxOrigPos; j++) {
                    for (int k = 0; k <= maxOrigChar; k++) {
                        Character z = (Character) mapOrigPosCondensedPosCondensedCharToOrigChar.get(new Triple(j, i, (char) k));
                        if (z != null) {
                            buf.append("condensed[").append(i).append("]=").append((char) k).append(" -> original[").append(j).append("]=").append(z).append("\n");
                        }
                    }
                }
            }
            return buf.toString();
        }

        class Triple {
            int first;
            int second;
            char third;

            Triple(int first, int second, char third) {
                this.first = first;
                this.second = second;
                this.third = third;
            }

            public int hashCode() {
                return first + 17 * second + 37 * third;
            }

            public boolean equals(Object other) {
                if (other instanceof Triple) {
                    Triple t = (Triple) other;
                    return first == t.first && second == t.second && third == t.third;
                } else
                    return false;
            }
        }
    }
}
