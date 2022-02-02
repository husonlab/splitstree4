/*
 * SplitContainer.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util;

import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


/**
 * User: bryant
 * Date: May 21, 2005
 * Time: 5:24:41 PM
 * Split container stores lots of collections of splits - implemented to use in bootstrap but could be used
 * for COnsensus splits etc.
 * <p/>
 * //ToDo: Replace vectors by arrays to avoid inefficiency of so many objects!
 */


public class SplitContainer {

	private final int ntax;
	private final Vector splitWeights;
	private final Map splitIndices; // Map from splits to indices
	private final Splits allSplits; //Splits block containing all splits

	private int weightMethod = this.MEAN_WEIGHTS;
	private int confidenceMethod = this.PROPORTION;
	private boolean isBootstrap = false; //Flag indicating whether the first splits are the bootstrap splits


	/* Methods for computing split weights */
	public final int MEAN_WEIGHTS = 0;
	public final int MEDIAN_WEIGHTS = 1;

    /* Methods for computing p-values */
    public final int PROPORTION = 0;

    /**
     * CONSTRUCTOR
     *
     * @param ntax the number of taxa
     */
    public SplitContainer(int ntax) {
        this.ntax = ntax;
        splitWeights = new Vector();
        splitIndices = new HashMap();
        allSplits = new Splits(ntax);
    }

    /**
     * Constructor used when bootstrapping - the first splits are the original estimate, and not
     * used when computing weights.
     *
	 */
    public SplitContainer(Splits originalSplits) {
        this.ntax = originalSplits.getNtax();
        splitWeights = new Vector();
        splitIndices = new HashMap();
        allSplits = new Splits(ntax);
        add(originalSplits);
        isBootstrap = true;
    }

    /**
     * add a new SplitsBlock
     *
     * @param newSplits Splits to add, or null if add empty block.
     */
    public void add(Splits newSplits) {
        Vector newRow = new Vector();
        if (newSplits != null) {
            newRow.setSize(allSplits.getNsplits() + 1);
            for (int i = 1; i <= newSplits.getNsplits(); i++) {
                TaxaSet sp = newSplits.get(i);
                if (splitIndices.containsKey(sp.toString())) {
                    int id = (Integer) splitIndices.get(sp.toString());
                    newRow.set(id, newSplits.getWeight(i));
                } else {
                    allSplits.add(sp);
                    int id = allSplits.getNsplits();
                    splitIndices.put(sp.toString(), id);
                    newRow.add(newSplits.getWeight(i));
                }
            }
        }
        splitWeights.add(newRow);
    }

    /**
	 */
    public float get(int blockNum, int splitNum) {
        Vector v = (Vector) splitWeights.get(blockNum);
        if (splitNum >= v.size())
            return (float) 0.0;
        else
            return (Float) v.get(splitNum);
    }

    /**
     * public float get(int blockNum, TaxaSet sp) {
     * if (splitIndices.containsKey(sp.toString())) {
     * int id = ((Integer) splitIndices.get(sp.toString())).intValue();
     * return get(blockNum, id);
     * } else
     * return (float) 0.0;
     * }
     * <p>
     * <p>
     * public void set(int blockNum, int splitNum, float val) {
     * Vector v = (Vector) splitWeights.get(blockNum);
     * if (splitNum >= v.size()) {
     * v.setSize(allSplits.getNsplits() + 1);
     * v.set(splitNum, new Float(val));
     * } else
     * v.set(splitNum, new Float(val));
     * }
     * <p>
     * public void set(int blockNum, TaxaSet sp, float val) {
     * if (splitIndices.containsKey(sp.toString())) {
     * int id = ((Integer) splitIndices.get(sp.toString())).intValue();
     * set(blockNum, id, val);
     * } else {
     * allSplits.add(sp);
     * int id = allSplits.getNsplits();
     * splitIndices.put(sp.toString(), (int)(id));
     * set(blockNum, id, val);
     * }
     * }
     * <p>
     * <p>
     * /**
     * Return number of blocks currently stored
     *
	 */
    public int getNumSplitBlocks() {
        return splitWeights.size();
    }

    /**
     * Return method used to estimate weights in summary splits
     *
	 */
    public int getWeightMethod() {
        return weightMethod;
    }

    /**
     * Set method used to evaluate weights in summary splits
     *
	 */
    public void setWeightMethod(int val) {

        if (weightMethod != val) {
            weightMethod = val;
        }
    }

    /**
     * Return method used to evaluate confidence levels in summary splits
     *
	 */
    public int getConfidenceMethod() {
        return confidenceMethod;
    }

    /**
     * Set method used to evaluation confidence levels in summary splits
     *
	 */
    public void setConfidenceMethod(int val) {
        if (val != confidenceMethod) {
            confidenceMethod = val;
        }
    }


    /**
     * Return number of splits currently stored
     */
    public int getNsplits() {
        return allSplits.getNsplits();
    }


    /**
     * Returns an indexed split block, or null if the  index is not valid.
     *
     * @param index Replicate number.
	 */
    Splits getBlock(int index) {
        if (index < 0 || index >= getNumSplitBlocks())
            return null;

        Splits s = new Splits(this.ntax);
        Vector thisRow = (Vector) splitWeights.get(index);
        for (int i = 0; i < thisRow.size(); i++) {
            float x = (Float) thisRow.get(i);
            if (x > 0.0) {
                s.add(allSplits.get(i), x);
            }
        }
        return s;
    }

    Splits getAllSplits() {
        return getAllSplits(0, 0);
    }

    Splits getAllSplits(float minWeight, float minConfidence) {
        Splits s = new Splits(allSplits.getNtax());
        for (int i = 1; i <= getNsplits(); i++) {
            float w = allSplits.getWeight(i);
            float c = allSplits.getWeight(i);
            if (w >= minWeight && c >= minConfidence)
                s.add(s.get(i), w, c);
        }
        return s;
    }


    void recalcWeights(int method) {

        int offset = (isBootstrap) ? 1 : 0;
        int n = getNumSplitBlocks() - offset;
        float[] row;
        float sum;
        float weight;

        if (method == this.MEDIAN_WEIGHTS) {
            row = new float[getNumSplitBlocks() - offset];
        } else
            row = null;

        for (int i = 1; i <= getNsplits(); i++) {
            sum = 0;
            for (int j = offset; j < getNumSplitBlocks(); j++) {
                weight = get(j, i);
                if (method == this.MEDIAN_WEIGHTS)
                    row[j - offset] = weight;
                else if (method == this.MEAN_WEIGHTS)
                    sum += weight;
            }
            if (method == this.MEAN_WEIGHTS) {
                weight = sum;
                if (sum > 0.0)
                    weight = weight / (float) n;
            } else if (method == this.MEDIAN_WEIGHTS) {
                weight = QuickSelect.median(row);
            } else
                weight = 0;
            allSplits.setWeight(i, weight);
        }
    }

    void recalcConfidence(int method) {

        int offset = (isBootstrap) ? 1 : 0;
        int n = getNumSplitBlocks() - offset;
        int count;
        float weight = 0;

        for (int i = 1; i <= getNsplits(); i++) {
            count = 0;
            for (int j = offset; j < getNumSplitBlocks(); j++) {
                weight = get(j, i);
                if (method == this.PROPORTION) {
                    if (weight > 0)
                        count++;
                }
            }
            if (method == this.PROPORTION) {
                weight = (float) count / n;
            }
            allSplits.setWeight(i, weight);
        }
    }


}
