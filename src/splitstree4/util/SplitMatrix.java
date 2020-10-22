/**
 * SplitMatrix.java
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
package splitstree4.util;

import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.util.HashMap;
import java.util.Map;

/**
 * Specially designed container to store many collections of splits.
 * <p/>
 * The rows correspond to splits and the columns to blocks (different sets of splits).
 * <p/>
 * The rows are indexed from 1 to number of splits
 * The blocks are indexed from 1 to nblocks.
 */


public class SplitMatrix {


    private int nblocks;   //Number of Split sets
    SparseTable<Double> matrix;     //Split weights, indexed by split and then split set.

    private Map splitIndices; // Map from splits to indices
    private Splits allSplits; //Splits block containing all splits

    /**
     * Create a new Split matrix.
     *
     * @param ntax
     */
    public SplitMatrix(int ntax) {
        matrix = new SparseTable<>();
        allSplits = new Splits(ntax);
        splitIndices = new HashMap();
    }


    /**
     * Create a new Split matrix with rows (empty) identified with the given set of splits.
     *
     * @param ntax
     */
    public SplitMatrix(int ntax, Splits splits) {
        matrix = new SparseTable();
        allSplits = new Splits(ntax);
        splitIndices = new HashMap();
        addSplitsWithoutBlock(splits);
    }


    /**
     * Constructs a SplitMatrix from a set of trees
     *
     * @param trees
     * @param taxa
     */
    public SplitMatrix(Trees trees, Taxa taxa) {
        matrix = new SparseTable();
        splitIndices = new HashMap();
        allSplits = new Splits(taxa.getNtax());

        for (int i = 1; i <= trees.getNtrees(); i++) {
            add(TreesUtilities.convertTreeToSplits(trees, i, taxa));
        }

    }


    /**
     * Searches for a split in the matrix. Returns -1 if the split is not found
     *
     * @param sp
     * @return index (1..nsplits in matrix) or -1 if split is not found.
     */
    public int findSplit(TaxaSet sp) {
        String s;

        if (sp.get(1))   //Index splits by their half not containing 1.
            s = sp.getComplement(getNtax()).toString();
        else
            s = sp.toString();

        if (splitIndices.containsKey(s)) {
            return (Integer) splitIndices.get(s);
        } else
            return -1;
    }

    /**
     * Returns the index of a given split.
     * If the split is not currently in the matrix then memory is allocated as necc.,
     * the new split is inserted in allSplits, and
     * the index of the new split position is returned.
     *
     * @param sp
     * @return index
     */
    private int findOrAddSplit(TaxaSet sp) {
        int newid = findSplit(sp);
        if (newid < 0) {
            newid = allSplits.getNsplits() + 1;
            String s;
            if (sp.get(1))
                s = sp.getComplement(getNtax()).toString();
            else
                s = sp.toString();
            splitIndices.put(s, newid);
            allSplits.add(sp);
        }
        return newid;
    }


    /**
     * Adds a new block wiith a new set of splits and stores weights in a new block.
     *
     * @param newSplits
     */
    public void add(Splits newSplits) {

        int newBlockId = getNblocks() + 1;
        for (int i = 1; i <= newSplits.getNsplits(); i++) {
            TaxaSet sp = newSplits.get(i);
            int id = findOrAddSplit(sp);
            set(id, newBlockId, newSplits.getWeight(i));
        }
        nblocks++;
    }

    /**
     * Adds a block of splits, but does not create a new block. Essentially adds empty rows
     * to the split matrix. Splits that are already present in the matrix will not be added,
     * the other splits will be added in the order that they appear in newSplits.
     *
     * @param newSplits
     */
    public void addSplitsWithoutBlock(Splits newSplits) {
        for (int i = 1; i <= newSplits.getNsplits(); i++) {
            TaxaSet sp = newSplits.get(i);
            findOrAddSplit(sp);
        }
    }

    /**
     * Returns a split weight, or 0.0 if that block doesn't have that split.
     *
     * @param split
     * @param blockNum
     * @return weight
     */
    public double get(int split, int blockNum) {
        Double value = matrix.get(split, blockNum);
        return value != null ? value : 0;
    }


    //ToDo: Delete this

    /**
     * @param split
     * @return
     * @deprecated
     */
    public double getOriginal(int split) {
        Double value = matrix.get(split, 0);
        return value != null ? value : 0;
    }

    //ToDo: Delete this

    /**
     * @param split
     * @return
     * @deprecated
     */
    public String getLabel(int split) {
        return allSplits.getLabel(split);
    }

    /**
     * Sets the weight for a particular split (here indexed 1... nsplits in matrix)
     *
     * @param splitNum
     * @param blockNum
     * @param val
     */
    public void set(int splitNum, int blockNum, double val) {
        matrix.set(splitNum, blockNum, val);
    }

    /**
     * Return the split as indexed in matrix.
     *
     * @param id
     * @return TaxaSet
     */
    public TaxaSet getSplit(int id) {
        return allSplits.get(id);
    }

    /**
     * Returns a Splits block with all splits contained in the matrix.
     *
     * @return Splits
     */
    public Splits getSplits() {
        return allSplits;
    }

    /**
     * Return number of blocks currently stored. Blocks are indexed 1..nblocks
     *
     * @return int number of blocks
     */
    public int getNblocks() {
        return nblocks;
    }

    /**
     * Returns the number of taxa that these are splits for.
     *
     * @return int Number of taxa.
     */
    public int getNtax() {
        return allSplits.getNtax();
    }

    /**
     * Return number of splits currently stored
     */
    public int getNsplits() {
        return allSplits.getNsplits();
    }

    //ToDo: Delete this?

    /**
     * returns vector of weights for the given split id. indexed 0..nblocks-1
     *
     * @param splitId
     * @return row
     */
    public double[] getMatrixRow(int splitId) {
        int n = getNblocks();
        double[] row = new double[n];
        for (int i = 1; i <= n; i++)
            row[i - 1] = get(splitId, i);
        return row;
    }

    //ToDo: Delete this?

    /**
     * Returns column blockId in matrix, indexed 0..nsplits-1
     *
     * @param blockId
     * @return column
     */
    public double[] getMatrixColumn(int blockId) {
        double[] v = new double[getNsplits()];
        for (int i = 1; i <= getNsplits(); i++)
            v[i - 1] = get(i, blockId);
        return v;
    }


}
