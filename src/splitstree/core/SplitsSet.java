/**
 * SplitsSet.java 
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
package splitstree.core;

import splitstree.util.Interval;

import java.util.Vector;

/**
 * A set of splits
 */
public class SplitsSet implements Cloneable {

    private int nsplits,
            ntax;
    private Vector splits,
            weights,
            confidences,
            intervals,
            splitlabels;


    /**
     * Constructor
     */
    public SplitsSet() {
        nsplits = 0;
        ntax = 0;
        splits = new Vector();
        weights = new Vector();
        confidences = new Vector();
        splitlabels = new Vector();
        intervals = new Vector();
    }

    /**
     * constructor
     *
     * @param ntax the number of taxa
     */
    public SplitsSet(int ntax) {
        this();
        this.ntax = ntax;
    }

    /**
     * Sets the splits
     *
     * @param splits the split sets
     */
    public void setSplits(Vector splits) {
        this.splits = splits;
    }

    /**
     * Gets the splits
     * *@return the splits
     */
    public Vector getSplits() {
        return splits;
    }

    /**
     * Get the split labels
     *
     * @return the split labels
     */
    public Vector getSplitlabels() {
        return this.splitlabels;
    }

    /* gets the weights
    *@return the weights
    */
    public Vector getWeights() {
        return this.weights;
    }

    /**
     * Set the weights
     *
     * @param weights the weights
     */
    public void setWeights(Vector weights) {
        this.weights = weights;
    }

    /**
     * gets the intervals vector
     *
     * @return intervals
     */
    public Vector getIntervals() {
        return intervals;
    }

    /**
     * sets the intervals vector
     *
     * @param intervals
     */
    public void setIntervals(Vector intervals) {
        this.intervals = intervals;
    }


    /**
     * gets the confidences           vector
     *
     * @return confidences
     */
    public Vector getConfidences() {
        return confidences;
    }

    /**
     * sets the confidences vector
     *
     * @param confidences
     */
    public void setConfidences(Vector confidences) {
        this.confidences = confidences;
    }


    /**
     * Set the labels
     *
     * @param labels the labels
     */
    public void setLabels(Vector labels) {
        this.splitlabels = labels;
    }


    /**
     * Get the number of taxa.
     *
     * @return number of taxa
     */
    public int getNtax() {
        return ntax;
    }

    /**
     * Set the number of taxa
     *
     * @param ntax the number of taxa
     */
    public void setNtax(int ntax) {
        this.ntax = ntax;
    }

    /**
     * Get the number of splits.
     *
     * @return number of splits
     */
    public int getNsplits() {

        return nsplits;
    }

    /**
     * Set the number of splits.
     *
     * @param nsplits number of splits
     */
    public void setNsplits(int nsplits) {
        this.nsplits = nsplits;
    }


    /**
     * Gets the label of the i-th split
     *
     * @param i the index of the split
     * @return the split label
     */
    public String getLabel(int i) {
        return (String) splitlabels.get(i - 1);
    }

    /**
     * Sets the label of the i-th taxon
     *
     * @param i   the index of the taxon
     * @param lab the label
     */
    public void setLabel(int i, String lab) {
        splitlabels.set(i - 1, lab);
    }


    /**
     * Clears the list of splits
     */
    public void clear() {
        ntax = 0;
        nsplits = 0;
        splitlabels = new Vector();
        splits = new Vector();
        weights = new Vector();
    }

    /**
     * Adds a split
     *
     * @param A one side of the split
     */
    public void add(TaxaSet A) {
        add(A, 1, 1, null, null);
    }

    /**
     * Adds a split
     *
     * @param A      one side of the split
     * @param weight the weight
     */
    public void add(TaxaSet A, float weight) {
        add(A, weight, 1, null, null);

    }

    /**
     * Adds a split
     *
     * @param A          one side of the split
     * @param weight     the weight
     * @param confidence
     */
    public void add(TaxaSet A, float weight, float confidence) {
        add(A, weight, confidence, null, null);
    }


    /**
     * Adds a split
     *
     * @param A      one side of the split
     * @param weight the weight
     * @param lab    the label
     */
    public void add(TaxaSet A, float weight, String lab) {
        add(A, weight, 1, null, lab);
    }

    /**
     * Adds a split
     *
     * @param A      one side of the split
     * @param weight the weight
     * @param lab    the label
     */
    public void add(TaxaSet A, float weight, float confidence, String lab) {
        add(A, weight, confidence, null, lab);
    }

    public void add(TaxaSet A, float weight, float confidence, Interval interval, String lab) {
        nsplits++;
        if (A.get(1))
            splits.add(A.clone());
        else
            splits.add((A.getComplement(getNtax())).clone());

        weights.add(weight);
        confidences.add(confidence);
        splitlabels.add(lab);
        intervals.add(interval);
    }


    /**
     * Removes a split (need this for GRM - mf)
     *
     * @param i index of Splits to be removed
     */

    public void remove(int i) {
        this.splits.removeElementAt(i - 1);
        this.splitlabels.removeElementAt(i - 1);
        this.weights.removeElementAt(i - 1);
        this.confidences.removeElementAt(i - 1);
        this.intervals.removeElementAt(i - 1);
        nsplits--;
    }


    /**
     * Returns the i-th split
     *
     * @param i the index of the split between 1..nsplits
     * @return the taxa set of the split
     */
    public TaxaSet getSplit(int i) {
        return (TaxaSet) splits.get(i - 1);
    }

    /**
     * Returns the i-th weight
     *
     * @param i the index of the weight between 1..nsplits
     * @return the taxa set of the weight
     */
    public float getWeight(int i) {
        return (Float) weights.get(i - 1);
    }

    /**
     * Sets the weight of a split
     *
     * @param i   index of the split between 1..nsplits
     * @param wgt the weight
     */
    public void setWeight(int i, float wgt) {
        weights.set(i - 1, wgt);
    }

    /**
     * gets the confidence          of a split
     *
     * @param i
     * @return confidence
     */
    public float getConfidence(int i) {
        return (Float) confidences.get(i - 1);
    }

    /**
     * sets the confidence of a split
     *
     * @param i
     * @param confidence
     */
    public void setConfidence(int i, float confidence) {
        if (i - 1 >= confidences.size())
            confidences.add(i - 1, confidence);
        else
            confidences.set(i - 1, confidence);
    }

    /**
     * Get a confidence interval for a split
     *
     * @param i
     * @return Interval
     */
    public Interval getInterval(int i) {
        return (Interval) intervals.get(i - 1);
    }

    /**
     * Get a confidence interval for a split
     *
     * @param i
     * @param interval
     */
    public void setInterval(int i, Interval interval) {
        if (i - 1 >= intervals.size())
            intervals.add(i - 1, interval);
        else
            intervals.set(i - 1, interval);
    }


    /**
     * Gets the first index of a split with the given label
     *
     * @param lab the label
     * @return the index of the first split with the given index, or 0
     */
    public int indexOf(String lab) {
        return splitlabels.indexOf(lab) + 1;
    }


    /**
     * returns a TaxaSet representing all taxa, which are included in one specified side of one split and also one specified side of the other split.
     *
     * @return the TaxaSet, containing all taxa of the specified intersection of two splits
     * @ param splitP the index of split "P"
     * @ param sideP the "side" of the split P that should be considered
     * @ param splitQ the index of the other split "Q"
     * @ param sideQ the "side" of the split Q that should be considered
     */

    public TaxaSet intersect2(int splitP, boolean sideP, int splitQ, boolean sideQ) {
        TaxaSet t = new TaxaSet();
        for (int i = 1; i <= this.getNtax(); i++) {
            if (((this.getSplit(splitP)).get(i) == sideP)
                    && ((this.getSplit(splitQ)).get(i) == sideQ))
                t.set(i);
        }
        return t;
    }

    /**
     * returns a TaxaSet representing all taxa, which are included in specified sides of three splits.
     *
     * @return the TaxaSet, containing all taxa of the specified intersection of three splits
     * @ param splitP the index of split "P"
     * @ param sideP the "side" of the split P that should be considered
     * @ param splitQ the index of the other split "Q"
     * @ param sideQ the "side" of the split Q that should be considered
     * @ param splitR the index of the other split "R"
     * @ param sideR the "side" of the split R that should be considered
     */


    public TaxaSet intersect3(int splitP, boolean sideP, int splitQ, boolean sideQ, int splitR, boolean sideR) {
        TaxaSet t = new TaxaSet();
        for (int i = 1; i <= this.getNtax(); i++) {
            if ((this.getSplit(splitP)).get(i) == sideP && (this.getSplit(splitQ)).get(i) == sideQ && (this.getSplit(splitR)).get(i) == sideR)
                t.set(i);
        }
        return t;
    }

    public Object clone() {
        SplitsSet ss = new SplitsSet();
        ss.setNsplits(this.getNsplits());
        ss.setNtax(this.getNtax());
        ss.setSplits((Vector) this.getSplits().clone());
        ss.setWeights((Vector) this.getWeights().clone());
        ss.setConfidences((Vector) this.getConfidences().clone());
        ss.setLabels((Vector) this.getSplitlabels().clone());
        return ss;
    }

    /**
     * determine whether given split is contained in set
     *
     * @param split the split
     * @return id, if split found, -1 else
     */
    public int find(TaxaSet split) {
        for (int i = 1; i <= nsplits; i++)
            if (getSplit(i).equalsAsSplit(split, ntax))
                return i;
        return -1;
    }
}
