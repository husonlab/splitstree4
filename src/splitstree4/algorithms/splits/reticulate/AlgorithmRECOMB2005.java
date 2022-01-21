/*
 * AlgorithmRECOMB2005.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.splits.reticulate;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.CollectionUtils;
import jloda.util.Pair;
import splitstree4.algorithms.splits.EqualAngle;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Recomb 2005 alogirthm
 *
 * @author huson
 * Date: 17-Sep-2004
 */
public class AlgorithmRECOMB2005 {
	final static int FAILED = 0;
	final static int CONTAINED = 1;
	final static int COMPLETE = 2;

	/**
	 * returns the delta between two binary strings
	 *
	 * @return delta
	 */
	static public String deltaBinarySequences(String a, String b) {
		var buf = new StringBuilder();
		var diffStart = -1;
		var first = true;
		for (var i = 0; i < a.length(); i++) {
			if (a.charAt(i) == b.charAt(i)) {
				if (diffStart > -1) {
					if (first)
						first = false;
					else
						buf.append(",");
					if (i - 1 == diffStart)
						buf.append(diffStart + 1);
					else
						buf.append(diffStart + 1).append("-").append(i);
					diffStart = -1;
				}
			} else // chars differ
			{
				if (diffStart == -1)
					diffStart = i;
			}
		}
		if (diffStart > -1) {
			if (!first)
				buf.append(",");
			if (diffStart == a.length() - 1)
				buf.append(diffStart + 1);
			else
				buf.append(diffStart + 1).append("-").append(a.length());
		}
		if (buf.length() > 0)
			return buf.toString();
		else
			return null;
	}

	/**
	 * compute the majority sequence of three binary sequences
	 *
	 * @return majority sequence
	 */
	static public String majorityBinarySequences(String a, String b, String c) {
		var buf = new StringBuilder();
		for (var i = 0; i < a.length(); i++) {
			if ((a.charAt(i) == '1' && (b.charAt(i) == '1' || c.charAt(i) == '1'))
				|| (b.charAt(i) == '1' && c.charAt(i) == '1'))
				buf.append('1');
			else
				buf.append('0');
		}
		return buf.toString();
	}

	public boolean apply(Taxa taxa, Splits splits, int outGroupId, Reticulation result, int which) throws Exception {

		System.err.println("# Running AlgorithmRECOMB2005:");
		List endPairs;

		endPairs = computeAllEndPairs(taxa, splits);
		System.err.println("# Possible end pairs: " + endPairs.size());

		PhyloGraphView graphView;
		{
			Document tmpDoc = new Document();
            tmpDoc.setTaxa(taxa);
            tmpDoc.setSplits(splits);
            SplitsUtilities.computeCycle(tmpDoc, taxa, splits, 0);

            EqualAngle ea = new EqualAngle();
            ea.apply(tmpDoc, taxa, splits);
            graphView = ea.getPhyloGraphView();
        }

        List arrangements = new LinkedList();
        Iterator it = endPairs.iterator();
        while (it.hasNext()) {
            List add = computeArrangements(taxa, splits, graphView, (Pair) it.next());
            arrangements.addAll(add);
        }

        System.err.println("# Possible arrangements: " + arrangements.size());

        // sort backbones by decreasing length:
		CollectionUtils.sort(arrangements, new Reticulation());

        it = arrangements.iterator();
        boolean found = false;
        int count = 0;
        while (it.hasNext()) {
            Reticulation ret = (Reticulation) it.next();
            // check that outgroup is contained in backbone or not set
            boolean outGroupPositionOk = (outGroupId <= 0);
            for (int i = 0; !outGroupPositionOk && i < ret.getBackbone().length; i++) {
                if (ret.getBackbone()[i] == outGroupId)
                    outGroupPositionOk = true;
            }
            if (outGroupPositionOk) {
                int status = confirmArrangement(taxa, splits, ret);
                if (status == CONTAINED && !found) {
                    count++;
                    if (which == 0 || count == which) {
                        found = true; // found something, we only discard if we find a complete one
                        result.copy(ret);
                    }
                } else if (status == COMPLETE)  // largest complete, return it!
                {
                    result.copy(ret);
                    count++;
                    if (which == 0 || count == which) {
                        System.err.println("# Found number: " + (which > 0 ? which : 1) + " (complete)");
                        return true;
                    }
                }
            }
        }
        if (found) // must have found a "contained" backbone
        {
            System.err.println("# Found number: " + (which > 0 ? which : 1) + " (contained)");
            return true;
        } else {
            System.err.println("found none");
            return false;
        }
    }

    /**
     * computes all possible end pairs a,c. A candidate end pair consists of two taxa with the property
     * that every split separates them
     *
     * @param taxa
     * @param splits
     * @return all possible end pairs
     */
    private List computeAllEndPairs(Taxa taxa, Splits splits) {
        List result = new LinkedList();

        for (int a = 1; a <= taxa.getNtax(); a++) {
            for (int c = a + 1; c <= taxa.getNtax(); c++) {
                boolean ok = true;
                for (int s = 1; ok && s <= splits.getNsplits(); s++) {
                    if (splits.get(s).get(a) == splits.get(s).get(c))
                        ok = false;
                }
                if (ok)
                    result.add(new Pair(a, c));
            }
        }
        return result;
    }

    /**
     * gor a pair of end points, computes all possible backbones. Each backbone is a list
     * of taxa ids, starting with the first member of the pair and ending with the last
     *
     * @param taxa
     * @param graphView
     * @param pair
     * @return list of backbones
     */
    private List computeArrangements(Taxa taxa, Splits splits, PhyloGraphView graphView, Pair pair)
            throws Exception {
        int start = pair.getFirstInt();
        int end = pair.getSecondInt();
        System.err.print("# Processing pair " + start + " " + end + ":");
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        BitSet usedSplits = new BitSet(); // set of split id's used in the current path
        int[] backbone = new int[taxa.getNtax()]; // current backbone
        int lenBackbone = 0; // number of taxa in current backbone
        int[] path = new int[splits.getNsplits()]; // splits along path from start to end
        int lenPath = 0;
        Node startNode = graph.getTaxon2Node(start);
        Node endNode = graph.getTaxon2Node(end);

        List reticulations = new LinkedList();

        computeBackbonesRec(taxa, startNode, endNode, usedSplits, path, lenPath, backbone, lenBackbone, graph, reticulations);
        System.err.println(" " + reticulations.size());
        return reticulations;
    }

    /**
     * recursively computes all backbones between startNode and endNode
     *
     * @param taxa
     * @param v
     * @param endNode
     * @param usedSplits
     * @param backbone
     * @param lenBackbone
     * @param graph
     * @param reticulations
     */
    private void computeBackbonesRec(Taxa taxa, Node v, Node endNode, BitSet usedSplits, int[] path, int lenPath, int[] backbone,
                                     int lenBackbone, PhyloSplitsGraph graph, List reticulations) throws NotOwnerException, SplitsException {

        if (graph.getNumberOfTaxa(v) > 0) {
            if (graph.getNumberOfTaxa(v) > 1)
                throw new SplitsException("computeBackbonesRec: node has multiple labels: " + v);
            backbone[lenBackbone++] = graph.getTaxa(v).iterator().next();
        }
        if (v == endNode) {
            Reticulation ret = new Reticulation();
            ret.setBackbone(backbone, lenBackbone);
            ret.determineReticulates(taxa.getNtax());
            ret.setSplitsPath(path, lenPath);

            reticulations.add(ret);
        } else {
            for (Edge e = graph.getFirstAdjacentEdge(v); e != null; e = graph.getNextAdjacentEdge(e, v)) {
                int splitId = graph.getSplit(e);
                if (!usedSplits.get(splitId)) {
                    usedSplits.set(splitId);
                    path[lenPath] = splitId;
                    Node w = graph.getOpposite(v, e);
                    computeBackbonesRec(taxa, w, endNode, usedSplits, path, lenPath + 1, backbone, lenBackbone, graph, reticulations);
                    usedSplits.set(splitId, false);
                }
            }
        }
    }

    /**
     * confirm that split set is compatible with a reticulation scenario and set
     * the first/last positions for each reticulation node
     *
     * @param taxa
     * @param splits
     * @param ret
     * @return Return 0 if invalid, 1 if contained, 2 if complete
     */
    private int confirmArrangement(Taxa taxa, Splits splits, Reticulation ret) {

        int lenBackbone = ret.getBackbone().length;
        int lenHybrids = ret.getReticulates().length;

        ret.setFirstPositionCovered(new int[lenHybrids]);
        ret.setLastPositionCovered(new int[lenHybrids]);

        System.err.println("# Confirm reticulation scenario: ");
        System.err.println("# backbone:");
        for (int ib = 0; ib < lenBackbone; ib++)
            System.err.print(" " + ret.getBackbone()[ib]);
        System.err.println();

        System.err.println("# reticulates:");
        for (int ih = 0; ih < lenHybrids; ih++)
            System.err.print(" " + ret.getReticulates()[ih]);
        System.err.println();

        BitSet[] leftOf = new BitSet[lenBackbone];
        BitSet[] rightOf = new BitSet[lenBackbone];
        for (int ib = 0; ib < lenBackbone; ib++) {
            leftOf[ib] = new BitSet();
            rightOf[ib] = new BitSet();
        }

        TaxaSet reticulateTaxa = new TaxaSet();
        for (int ih = 0; ih < lenHybrids; ih++)
            reticulateTaxa.set(ret.getReticulates()[ih]);

        TaxaSet backboneTaxa = new TaxaSet();
        for (int ib = 0; ib < lenBackbone; ib++)
            backboneTaxa.set(ret.getBackbone()[ib]);

        int leftMost = ret.getBackbone()[0];
        //int rightMost = ret.getBackbone()[lenBackbone - 1];

        // determine first and last position covered for each reticulate taxon:
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.get(s);
            if (!split.get(leftMost))
                split = split.getComplement(taxa.getNtax());
            for (int ih = 0; ih < lenHybrids; ih++) {
                int h = ret.getReticulates()[ih];
                for (int ib = 0; ib < lenBackbone; ib++) {
                    int b = ret.getBackbone()[ib];
                    if (split.get(h) && !split.get(b))
                        leftOf[ib].set(h);
                    else if (!split.get(h) && split.get(b))
                        rightOf[ib].set(h);
                }
            }
        }

        // check that covered positions form an interval:
        for (int ih = 0; ih < lenHybrids; ih++) {
            int h = ret.getReticulates()[ih];
            BitSet interval = new BitSet();
            int min = lenBackbone;
            int max = -1;
            for (int ib = 0; ib < lenBackbone; ib++) {
                if (leftOf[ib].get(h) && rightOf[ib].get(h)) {
                    if (ib < min)
                        min = ib;
                    if (ib > max)
                        max = ib;
                    interval.set(ib);
                }
            }
            System.err.println("# Interval for h=" + h + ": " + interval);
            if (max - min + 1 != interval.cardinality())
                return FAILED;      // not an interval
            for (int ib = 0; ib < min; ib++) {
                if (leftOf[ib].get(h) || !rightOf[ib].get(h)) {
                    System.err.println("ib: " + ib + " h: " + h);
                    System.err.println("leftOf: " + leftOf[ib].get(h));
                    System.err.println("rightOf: " + rightOf[ib].get(h));
                    System.err.println("# Left-of test failed");
                    return FAILED; // on wrong side before interval starts
                }
            }
            for (int ib = max + 1; ib < lenBackbone; ib++)
                if (!leftOf[ib].get(h) || rightOf[ib].get(h)) {
                    System.err.println("# Left-of test failed");
                    return FAILED; // on wonr side after interval finished
                }
            ret.getFirstPositionCovered()[ih] = min;
            ret.getLastPositionCovered()[ih] = max;
        }

        // TODO check that all splits are contained in maximal possible split set

        return COMPLETE;
    }
}
