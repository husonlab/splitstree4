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

package splitstree.util;

import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;

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

    private int ntax;
    private Vector splitWeights;
    private Map splitIndices; // Map from splits to indices
    private Splits allSplits; //Splits block containing all splits

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
     * @param originalSplits
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
     *
     * @param blockNum
     * @param splitNum
     * @return
     */
    public float get(int blockNum, int splitNum) {
        Vector v = (Vector) splitWeights.get(blockNum);
        if (splitNum >= v.size())
            return (float) 0.0;
        else
            return (Float) v.get(splitNum);
    }

    /**
    public float get(int blockNum, TaxaSet sp) {
        if (splitIndices.containsKey(sp.toString())) {
            int id = ((Integer) splitIndices.get(sp.toString())).intValue();
            return get(blockNum, id);
        } else
            return (float) 0.0;
    }


    public void set(int blockNum, int splitNum, float val) {
        Vector v = (Vector) splitWeights.get(blockNum);
        if (splitNum >= v.size()) {
            v.setSize(allSplits.getNsplits() + 1);
            v.set(splitNum, new Float(val));
        } else
            v.set(splitNum, new Float(val));
    }

    public void set(int blockNum, TaxaSet sp, float val) {
        if (splitIndices.containsKey(sp.toString())) {
            int id = ((Integer) splitIndices.get(sp.toString())).intValue();
            set(blockNum, id, val);
        } else {
            allSplits.add(sp);
            int id = allSplits.getNsplits();
            splitIndices.put(sp.toString(), new Integer(id));
            set(blockNum, id, val);
        }
    }


    /**
     * Return number of blocks currently stored
     *
     * @return
     */
    public int getNumSplitBlocks() {
        return splitWeights.size();
    }

    /**
     * Return method used to estimate weights in summary splits
     *
     * @return
     */
    public int getWeightMethod() {
        return weightMethod;
    }

    /**
     * Set method used to evaluate weights in summary splits
     *
     * @param val
     */
    public void setWeightMethod(int val) {

        if (weightMethod != val) {
            weightMethod = val;
        }
    }

    /**
     * Return method used to evaluate confidence levels in summary splits
     *
     * @return
     */
    public int getConfidenceMethod() {
        return confidenceMethod;
    }

    /**
     * Set method used to evaluation confidence levels in summary splits
     *
     * @param val
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
     * @return
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
