/**
 * SplitsUtilities.java
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
 *
 * @version $Id: SplitsUtilities.java,v 1.50 2008-07-01 19:06:00 bryant Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: SplitsUtilities.java,v 1.50 2008-07-01 19:06:00 bryant Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree4.algorithms.distances.NeighborNet;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.*;

import java.io.PrintStream;
import java.io.StringReader;
import java.util.BitSet;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tools for analyzing a Splits object
 */
public class SplitsUtilities {


    /**
     * Determines whether a given splits system is (strongly) compatible
     *
     * @param splits the splits object
     * @return true, if the given splits are (strongly) compatible
     */
    static public boolean isCompatible(Splits splits) {
        for (int i = 1; i <= splits.getNsplits(); i++)
            for (int j = i + 1; j <= splits.getNsplits(); j++)
                if (!SplitsUtilities.areCompatible(splits.getNtax(),
                        splits.get(i), splits.get(j)))
                    return false;
        return true;
    }

    /**
     * Determines whether a given splits system is weakly compatible
     *
     * @param splits the splits object
     * @return true, if the given splits are weakly compatible
     */
    static public boolean isWeaklyCompatible(Splits splits) {
        for (int i = 1; i <= splits.getNsplits(); i++) {
            for (int j = i + 1; j <= splits.getNsplits(); j++) {
                for (int k = j + 1; k <= splits.getNsplits(); k++) {
                    if (!areWeaklyCompatible(splits.getNtax(), splits.get(i), splits.get(j), splits.get(k)))
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether a given splits system is cyclic
     *
     * @param taxa      the taxa object
     * @param splits    the splits object
     * @param stabilize attempt to stabilize layoyt, if =1, or snowball, if==2
     * @return true, if the given splits are cyclic
     */
    public static boolean isCyclic(Document doc, Taxa taxa, Splits splits, int stabilize) {
        if (splits.getCycle() == null)
            SplitsUtilities.computeCycle(doc, taxa, splits, stabilize);

        return isCyclic(taxa, splits);
    }

    /**
     * Determines whether a given splits system is cyclic
     *
     * @param taxa   the taxa object
     * @param splits the splits object
     * @return true, if the given splits are cyclic
     */
    public static boolean isCyclic(Taxa taxa, Splits splits) {
        int[] inverse = new int[taxa.getNtax() + 1];
        for (int t = 1; t <= taxa.getNtax(); t++)
            inverse[splits.getCycle()[t]] = t;
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.get(s);

            if (split.get(splits.getCycle()[1]))     // avoid wraparound
                split = split.getComplement(taxa.getNtax());

            int minA = taxa.getNtax();
            int maxA = 1;
            for (int t = 1; t <= taxa.getNtax(); t++) {
                if (split.get(t)) {
                    if (inverse[t] < minA)
                        minA = inverse[t];
                    if (inverse[t] > maxA)
                        maxA = inverse[t];
                }
            }
            if ((maxA - minA + 1) != split.cardinality()) {
                return false;
            }
        }
        return true;
    }

    static Taxa prevTaxa = null;
    static Splits prevSplits = null;
    static int[] prevCycle = null;

    /**
     * sets the previous taxa and splits. This is used by computeCycle when
     * keep cycle is desired
     *
     * @param taxa
     * @param splits
     */
    static public void setPreviousTaxaSplits(Taxa taxa, Splits splits) {
        prevTaxa = (Taxa) taxa.clone();
        prevSplits = splits.clone(taxa);
        if (splits != null)
            prevCycle = splits.getCycle().clone();
        else
            prevCycle = null;
    }

    /**
     * compute a cycle for a set of splits
     *
     * @param splits
     * @return cycle
     */
    static public int[] computeCycle(Splits splits) {
        // PrintStream pso = jloda.util.Basic.hideSystemOut();
        // PrintStream pse = jloda.util.Basic.hideSystemErr();

        Distances dist = SplitsUtilities.splitsToDistances(splits);
        return NeighborNet.computeNeighborNetOrdering(dist);
    }


    /**
     * Computes a cycle for the given splits system
     *
     * @param taxa      the taxa
     * @param splits    the splits
     * @param stabilize attempt to stabilize tree layout, if =1, or snowball, if=2
     */
    static public void computeCycle(Document doc, Taxa taxa, Splits splits, int stabilize) {
        PrintStream pso = jloda.util.Basic.hideSystemOut();
        PrintStream pse = jloda.util.Basic.hideSystemErr();
        try {
            Distances dist;

            Splits splitsSnowball = null;
            boolean prevDataAvailable = (prevTaxa != null && taxa.equals(prevTaxa) && prevSplits != null);

            if (stabilize == Assumptions.KEEP && prevDataAvailable && prevCycle != null) {
                splits.setCycle(prevCycle);
                return;
            } else if ((stabilize == Assumptions.STABILIZE || stabilize == Assumptions.SNOWBALL)
                    && prevDataAvailable) {
                splitsSnowball = mergeInto(taxa, splits, prevSplits);
                dist = SplitsUtilities.splitsToDistances(taxa, splitsSnowball);
            } else
                dist = SplitsUtilities.splitsToDistances(taxa, splits);

            if (dist != null) {
                splits.setCycle(NeighborNet.computeNeighborNetOrdering(dist));
            } else if (taxa != null && taxa.getNtax() > 0) {
                int[] cycle = new int[taxa.getNtax() + 1];
                for (int t = 1; t <= taxa.getNtax(); t++)
                    cycle[t] = t;
                splits.setCycle(cycle);
            }

            if (stabilize == Assumptions.STABILIZE || stabilize == Assumptions.RECOMPUTE) {
                prevTaxa = (Taxa) taxa.clone();
                prevSplits = splits.clone(taxa);
            } else if (stabilize == Assumptions.SNOWBALL) {
                prevTaxa = (Taxa) taxa.clone();
                if (splitsSnowball == null)
                    prevSplits = splits.clone(taxa); // start the snowball rolling
                else
                    prevSplits = splitsSnowball.clone(taxa);  // add more snow
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        } finally {
            jloda.util.Basic.restoreSystemErr(pse);
            jloda.util.Basic.restoreSystemOut(pso);
        }
    }

    /**
     * merge the second set of splits into the first
     *
     * @param taxa   the taxa
     * @param splits original splits
     * @param add    additional splits
     * @return merged splits
     */
    private static Splits mergeInto(Taxa taxa, Splits splits, Splits add) {
        Splits result = splits.clone(taxa);
        for (int s = 1; s <= add.getNsplits(); s++) {
            int so = result.getSplitsSet().find(add.get(s));
            if (so == -1)
                result.add(add.get(s), add.getWeight(s));
            else {
                result.setWeight(so, result.getWeight(so) + add.getWeight(s));
            }
        }
        return result;
    }

    /**
     * Given splits, returns the split distances.
     * This is the number of splits separating each pair of taxa
     */
    public static Distances splitsToDistances(Taxa taxa, Splits splits) {
        return splitsToDistances(taxa.getNtax(), splits);
    }

    /**
     * Given splits, returns the split distances.
     * This is the number of splits separating each pair of taxa
     */

    public static Distances splitsToDistances(Splits splits) {
        return splitsToDistances(splits.getNtax(), splits);
    }

    /**
     * Given splits, returns the split distances.
     * This is the number of splits separating each pair of taxa
     */
    public static Distances splitsToDistances(int ntax, Splits splits) {
        Distances dist = new Distances(ntax);

        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                for (int s = 1; s <= splits.getNsplits(); s++) {
                    TaxaSet split = splits.get(s);

                    if (split.get(i) != split.get(j)) {
                        dist.set(i, j, dist.get(i, j) + 1);
                        dist.set(j, i, dist.get(i, j));
                    }
                }
            }
        }
        return dist;
    }

    /**
     * Determines the fit of a splits system, ie how well it
     * represents a given distance matrix, in percent. Computes two different values.
     * //ToDo: Fix variances.
     *
     * @param forceRecalculation always recompute the fit, even if there is a valid value stored.
     * @param splits             the splits
     * @param dist               the distances
     */
    static public void computeFits(boolean forceRecalculation, Splits splits, Distances dist, Document doc) {
        double dsum = 0;
        double ssum = 0;
        double dsumSquare = 0;
        double ssumSquare = 0;
        double netsumSquare = 0;

        if (splits == null || dist == null)
            return;

        if (!forceRecalculation && splits.getProperties().getFit() >= 0 && splits.getProperties().getLSFit() >= 0)
            return; //No need to recalculate.


        splits.getProperties().setFit(-1);
        splits.getProperties().setLSFit(-1); //A fit of -1 means that we don't have a valid value.

        if (doc != null)
            doc.notifySubtask("Recomputing fit");

        double[][] sdist = new double[splits.getNtax() + 1][splits.getNtax() + 1];
        int ntax = splits.getNtax();

        for (int i = 1; i <= ntax; i++) {
            sdist[i][i] = 0.0;
            for (int j = i + 1; j <= ntax; j++) {
                double dij = 0.0;
                for (int s = 1; s <= splits.getNsplits(); s++) {
                    TaxaSet split = splits.getSplitsSet().getSplit(s);
                    if (split.get(i) != split.get(j))
                        dij += splits.getWeight(s);
                }
                sdist[i][j] = sdist[j][i] = dij;
            }
        }
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double sij = sdist[i][j];
                double dij = dist.get(i, j);
                double x = Math.abs(sij - dij);
                ssum += x;
                ssumSquare += x * x;
                dsum += dij;
                dsumSquare += dij * dij;
                netsumSquare += sij * sij;
            }
        }
        double fit = 100 * (1.0 - ssum / dsum);
        fit = Math.max(fit, 0.0);
        splits.getProperties().setFit(fit);

        double lsfit = 100.0 * (1.0 - ssumSquare / dsumSquare);


        lsfit = Math.max(lsfit, 0.0);
        splits.getProperties().setLSFit(lsfit);

        double stress = Math.sqrt(ssumSquare / netsumSquare);

        System.err.println("\nRecomputed fit:\n\tfit = " + fit + "\n\tLS fit =" + lsfit + "\n\tstress =" + stress + "\n");

    }


    /**
     * Returns the maximal set of splits associated with the given
     * circular ordering
     *
     * @param ntax     the number of taxa
     * @param ordering the circular ordering
     */
    static public Splits makeMaximalCircularSplits(int ntax, int[] ordering) {
        Splits splits = new Splits(ntax);
        splits.getProperties().setCompatibility(Splits.Properties.CYCLIC);
        try {
            splits.setCycle(ordering);
        } catch (SplitsException e) {
            Basic.caught(e);
        }

        for (int a = 2; a <= ntax; a++) {
            for (int b = a; b <= ntax; b++) {
                TaxaSet t = new TaxaSet();

                for (int i = a; i <= b; i++)
                    t.set(ordering[i]);

                splits.add(t);
            }
        }
        return splits;
    }

    /**
     * returns true, if the cycle given and the one in the text are the same
     *
     * @param cycle
     * @param text
     * @return true if cycles are equal
     */
    public static boolean equalCycles(int[] cycle, String text) {

        NexusStreamParser np = new NexusStreamParser(new StringReader(text));
        try {
            for (int i = 1; i < cycle.length; i++) {
                if (np.getInt() != cycle[i])
                    return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * remove all splits whose weight falls below the given threshold
     *
     * @param splits
     * @param thresholdValue
     */
    public static void applyWeightThreshold(Splits splits, float thresholdValue) {
        boolean changed = false;
        for (int s = 1; s <= splits.getNsplits(); ) {
            if (splits.getWeight(s) < thresholdValue) {
                splits.getSplitsSet().remove(s); // don't increment, splits are renumbered!
                changed = true;
            } else
                s++;
        }
        if (changed)
            splits.getProperties().setCompatibility(Splits.Properties.UNKNOWN);
    }

    /**
     * remove all splits whose confidence falls below the given threshold
     *
     * @param splits
     * @param thresholdValue
     */
    public static void applyConfidenceThreshold(Splits splits, float thresholdValue) {
        boolean changed = false;
        for (int s = 1; s <= splits.getNsplits(); ) {
            if (splits.getConfidence(s) < thresholdValue) {
                splits.getSplitsSet().remove(s); // don't increment, splits are renumbered!
                changed = true;
            } else
                s++;
        }
        if (changed)
            splits.getProperties().setCompatibility(Splits.Properties.UNKNOWN);
    }

    /**
     * determines whether two splits on the same taxa set are compatible
     *
     * @param ntax
     * @param split1
     * @param split2
     * @return true, if split1 and split2 are compatible
     */
    public static boolean areCompatible(int ntax, TaxaSet split1, TaxaSet split2) {
        BitSet A1 = split1.getBits();
        BitSet B1 = split1.getComplement(ntax).getBits();
        BitSet A2 = split2.getBits();
        BitSet B2 = split2.getComplement(ntax).getBits();

        return !A1.intersects(A2) || !A1.intersects(B2)
                || !B1.intersects(A2) || !B1.intersects(B2);
    }

    /**
     * gets the compatiblity matrix
     *
     * @param splits
     * @return compatibility matrix
     */
    public boolean[][] getCompatibilityMatrix(final Splits splits) {
        boolean[][] matrix = new boolean[splits.getNsplits() + 1][splits.getNsplits() + 1];

        for (int i = 1; i <= splits.getNsplits(); i++) {
            TaxaSet s1 = splits.get(i);
            for (int j = i + 1; j <= splits.getNsplits(); j++) {
                TaxaSet s2 = splits.get(j);
                matrix[i][j] = matrix[j][i] = areCompatible(splits.getNtax(), s1, s2);
            }
        }
        return matrix;
    }


    /**
     * determines whether three splits on the same taxa set are weakly compatible
     *
     * @param ntax
     * @param split1
     * @param split2
     * @return true, if split1 and split2 are compatible
     */
    public static boolean areWeaklyCompatible(int ntax, TaxaSet split1, TaxaSet split2,
                                              TaxaSet split3) {
        BitSet A1 = split1.getBits();
        BitSet B1 = split1.getComplement(ntax).getBits();
        BitSet A2 = split2.getBits();
        BitSet B2 = split2.getComplement(ntax).getBits();
        BitSet A3 = split3.getBits();
        BitSet B3 = split3.getComplement(ntax).getBits();

        return !((intersects(A1, A2, A3)
                && intersects(A1, B2, B3)
                && intersects(B1, A2, B3)
                && intersects(B1, B2, A3))
                ||
                (intersects(B1, B2, B3)
                        && intersects(B1, A2, A3)
                        && intersects(A1, B2, A3)
                        && intersects(A1, A2, B3)));
    }

    /**
     * sort splits by decreasing weight
     *
     * @param splits
     */
    public static void sortByDecreasingWeight(Splits splits) {
        SortedSet weightId = new TreeSet(new Split());
        int ntax = splits.getNtax();

        for (int s = 1; s <= splits.getNsplits(); s++)
            weightId.add(new Split(splits.get(s), splits.getWeight(s),
                    splits.getConfidence(s)));

        splits.clear();
        splits.setNtax(ntax);
        for (Object aWeightId : weightId) {
            Split aSplit = (Split) aWeightId;
            splits.add(aSplit.t, aSplit.weight, aSplit.confidence);
        }
    }


    /**
     * do the three  bitsets intersect?
     *
     * @param a
     * @param b
     * @param c
     * @return true, if non-empty   intersection
     */
    private static boolean intersects(BitSet a, BitSet b, BitSet c) {
        for (int i = a.nextSetBit(1); i >= 0; i = a.nextSetBit(i + 1))
            if (b.get(i) && c.get(i))
                return true;
        return false;
    }

    /**
     * verify that all splits are proper and are contained in the taxon set
     *
     * @param splits
     * @param taxa
     * @throws SplitsException
     */
    public static void verifySplits(Splits splits, Taxa taxa) throws SplitsException {
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.get(s);
            if (split.cardinality() == 0 || split.cardinality() == taxa.getNtax())
                throw new SplitsException
                        ("Split <" + split + "<: not proper, size is 0 or ntax");
            if (!taxa.getTaxaSet().contains(split))
                throw new SplitsException("Split <" + split + "> not contained in taxa set <"
                        + taxa.getTaxaSet() + ">");
        }
    }

    /**
     * returns the number of positions for which the char is constant on at least one side of the split.
     * All characters including gaps etc are distiqnuished
     *
     * @param splits
     * @param s
     * @param chars
     * @return number of positions that give relaxed support to split
     */
    public static int getRelaxedSupport(Splits splits, int s, Characters chars) {
        int count = 0;
        TaxaSet A = splits.get(s);
        TaxaSet B = A.getComplement(splits.getNtax());
        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                boolean ok = true;
                char ch = chars.get(A.getBits().nextSetBit(0), c);
                for (int t = A.getBits().nextSetBit(0); ok && t >= 0; t = A.getBits().nextSetBit(t + 1)) {
                    if (chars.get(t, c) != ch)
                        ok = false;
                }
                if (!ok) {
                    ok = true;
                    ch = chars.get(B.getBits().nextSetBit(0), c);
                    for (int t = B.getBits().nextSetBit(0); ok && t >= 0; t = B.getBits().nextSetBit(t + 1)) {
                        if (chars.get(t, c) != ch)
                            ok = false;
                    }
                }
                if (ok)
                    count++;
            }
        }
        return count;
    }

    /**
     * add all missing trivial splits
     *
     * @param splits
     * @param ntax
     * @return number of trivial splits added
     */
    public static int addAllTrivial(Splits splits, int ntax, float weight) {
        TaxaSet hasTrivial = new TaxaSet();

        for (int s = 1; s <= splits.getNsplits(); s++) {
            if (splits.get(s).cardinality() == 1) {
                hasTrivial.set(splits.get(s).getBits().nextSetBit(1));
            } else if (splits.get(s).cardinality() == ntax - 1) {
                hasTrivial.set(splits.get(s).getBits().nextClearBit(1));
            }
        }
        for (int t = 1; t <= ntax; t++) {
            if (!hasTrivial.get(t)) {
                TaxaSet split = new TaxaSet();
                split.set(t);
                splits.add(split, weight);
            }
        }
        return ntax - hasTrivial.cardinality();
    }

    /**
     * get set of all all original splits that map to current splits whose ids are in the set indices
     *
     * @param taxa           set of taxa
     * @param partialSplits
     * @param partialIndices
     * @param fullSplits
     * @return bit set
     */
    public static BitSet matchPartialSplits(Taxa taxa, Splits partialSplits, BitSet partialIndices, Splits fullSplits) {
        int fullntax = taxa.getOriginalTaxa().getNtax();

        TaxaSet hiddenTaxa = taxa.getHiddenTaxa();
        if (hiddenTaxa == null)
            hiddenTaxa = new TaxaSet();
        TaxaSet partialTaxa = hiddenTaxa.getComplement(fullntax);

        BitSet result = new BitSet();

        int[] new2old = new int[partialTaxa.cardinality() + 1];
        int count = 0;

        for (int i = partialTaxa.getBits().nextSetBit(0); i != -1; i = partialTaxa.getBits().nextSetBit(i + 1)) {
            count++;
            new2old[count] = i;
        }


        for (int id = 1; id <= partialSplits.getNsplits(); id++) {
            if (partialIndices.get(id)) {
                TaxaSet partialAsOld = new TaxaSet();
                TaxaSet s = partialSplits.get(id);
                //ToDO: (low priority) if we have the full set of taxa we can just compare that. 
                for (int i = s.getBits().nextSetBit(0); i != -1; i = s.getBits().nextSetBit(i + 1))
                    partialAsOld.set(new2old[i]);
                for (int full = 1; full <= fullSplits.getNsplits(); full++) {
                    TaxaSet s2 = (TaxaSet) fullSplits.get(full).clone();
                    TaxaSet s3 = ((TaxaSet) s2.clone()).getComplement(fullntax);
                    s2.and(partialTaxa);
                    s3.and(partialTaxa);               //s2|s3 is now a split of partialTaxa

                    if (s2.equals(partialAsOld) || s3.equals(partialAsOld)) {
                        result.set(full);
                    }
                }
            }
        }

        return result;

    }

    /**
     * A split. Currently only used by sortByDecreasingWeight
     */
    static class Split implements Comparator {
        TaxaSet t;
        float weight;
        float confidence;

        Split() {
        }

        Split(TaxaSet t, float weight, float confidence) {
            this.t = t;
            this.weight = weight;
            this.confidence = confidence;
        }

        public int compare(Object o1, Object o2) {
            Split s1 = (Split) o1;
            Split s2 = (Split) o2;

            if (s1.weight < s2.weight) {
                return 1;
            } else if (s1.weight > s2.weight) {
                return -1;
            } else if (s1.confidence < s2.confidence)
                return 1;
            else if (s1.confidence > s2.confidence)
                return -1;
            else
                return TaxaSet.compare(s1.t, s2.t);
        }
    }

    /**
     * cpmputes a tree from a set of splits
     *
     * @param graph
     * @param splits
     * @param taxa
     * @param weights
     * @return tree
     */
    public static PhyloSplitsGraph treeFromSplits(PhyloSplitsGraph graph, Splits splits, Taxa taxa, boolean weights) {
        /*initialize star tree*/
        BitSet seen = new BitSet();
        Node center = graph.newNode();
        for (int i = 1; i <= splits.getNsplits(); i++) {
            if (splits.get(i).cardinality() == 1 || splits.get(i).cardinality() == taxa.getNtax() - 1) {
                Node v = graph.newNode();
                for (int j = 1; j <= taxa.getNtax(); j++) {
                    TaxaSet A = splits.get(i);
                    if ((A.cardinality() == 1 && A.get(j)) || (A.cardinality() == taxa.getNtax() - 1 && !A.get(j))) {
                        graph.addTaxon(v, j);
                        seen.set(j);
                    }
                }
                Edge e = graph.newEdge(v, center);
                graph.setSplit(e, i);
                if (weights) graph.setWeight(e, splits.getWeight(i));
            }
        }

        // add temporary trivial splits:

        // place all the taxa without trivial splits on the center node
        for (int j = 1; j <= taxa.getNtax(); j++)
            if (!seen.get(j)) {
                graph.addTaxon(center, j);
            }

        /*process all non-trivial splits*/
        for (int i = 1; i <= splits.getNsplits(); i++) {
            if (splits.get(i).cardinality() > 1 && splits.get(i).cardinality() < taxa.getNtax() - 1) {
                Node u = graph.getTaxon2Node(1);
                Edge e = null;
                boolean intersect = false;
                TaxaSet split = splits.get(i);
                if (split.get(1))
                    split = split.getComplement(splits.getNtax());
                /*find node for new edge*/
                while (!intersect) {
                    Edge moveAlong = null;
                    boolean found = false;

                    for (Edge f : u.adjacentEdges()) {
                        if (f == e) continue;
                        TaxaSet splitf = splits.get(graph.getSplit(f));
                        if (splitf.get(1)) splitf = splitf.getComplement(splits.getNtax());

                        if (split.intersects(splitf)) {
                            if (!found) {
                                found = true;
                                moveAlong = f;
                            } else {
                                intersect = true;
                                break;
                            }
                        }
                    }
                    if (!intersect) {
                        u = graph.getOpposite(u, moveAlong);
                        e = moveAlong;
                    }
                }
                /*insert new edge*/
                Node newNode = graph.newNode();
                for (Edge f : u.adjacentEdges()) {
                    if (f == e) continue;
                    TaxaSet splitf = splits.get(graph.getSplit(f));
                    if (splitf.get(1)) splitf = splitf.getComplement(splits.getNtax());
                    if (split.intersects(splitf)) {
                        Edge splitEdge = graph.newEdge(newNode, graph.getOpposite(u, f));
                        graph.setSplit(splitEdge, graph.getSplit(f));
                        if (weights) graph.setWeight(splitEdge, graph.getWeight(f));
                        graph.deleteEdge(f);
                    }
                }
                Edge newEdge = graph.newEdge(u, newNode);
                graph.setSplit(newEdge, i);
                if (weights) graph.setWeight(newEdge, splits.getWeight(i));
            }
        }
        return graph;
    }

    /**
     * determines the mean split weight
     *
     * @param splits
     * @return the mean weight
     */
    public static double getMean(Splits splits) {
        double mean = 0.0;
        for (int i = 1; i <= splits.getNsplits(); i++) mean += splits.getWeight(i);
        return mean / splits.getNsplits();
    }

    /**
     * Returns the compatability matrix as a boolean array, [1...nsplits][1...nsplits]
     *
     * @param splits
     * @return compatiblity matrix
     */
    public static boolean[][] compatibilityMatrix(Splits splits) {
        int n = splits.getNsplits();
        boolean[][] AdjMatrix = new boolean[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {

            AdjMatrix[i][i] = true;
            for (int j = 1; j < i; j++) {
                boolean adj = SplitsUtilities.areCompatible(splits.getNtax(), splits.get(i), splits.get(j));
                AdjMatrix[i][j] = AdjMatrix[j][i] = adj;
            }
        }
        return AdjMatrix;
    }

    /**
     * does given split part form an interval in the given circular ordering?
     *
     * @param taxa
     * @param cycle
     * @param sp
     * @return is split circular with respect to the gjven cycle?
     */
    public static boolean isCircular(Taxa taxa, int[] cycle, TaxaSet sp) {
        TaxaSet seenPositions = new TaxaSet();
        for (int t = 1; t <= taxa.getNtax(); t++)
            if (sp.get(cycle[t]))
                seenPositions.set(t);
        if (seenPositions.get(1))
            seenPositions = seenPositions.getComplement(taxa.getNtax());
        // check whether seen positions form an interval:
        int min = seenPositions.getBits().nextSetBit(0);
        int max = seenPositions.max();
        return max - min + 1 == seenPositions.cardinality();
    }
}

// EOF
