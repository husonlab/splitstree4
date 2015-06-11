/**
 * ConsensusNetwork.java 
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
/**
 * implements consensus networks
 * @version $Id: ConsensusNetwork.java,v 1.29 2010-04-23 02:58:54 bryant Exp $
 * @author Tobias Kloepper and Daniel Huson and David Bryant
 * 7.03
 */
package splitstree.algorithms.trees;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.util.*;

/**
 * implements consensus networks from splits Holland and Moulton (2003).
 */
public class ConsensusNetwork implements Trees2Splits {
    public final static String MEDIAN = "median";
    public final static String MEAN = "mean";
    public final static String COUNT = "count";
    public final static String SUM = "sum";
    public final static String NONE = "none";
    private String optionEdgeWeights = MEAN;
    private double threshold = 0.33;
    public final static String DESCRIPTION = "Computes the consensus splits of trees (Holland and Moulton 2003)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        return doc.isValid(taxa) && doc.isValid(trees) && trees.getNtrees() > 0
                && !trees.getPartial();
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containinga list of trees
     * @return the computed set of consensus splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) throws CanceledException {
        // ProgressDialog pd = new ProgressDialog("Consensus Network...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(0);

        TreeSelector trans;
        Trees tempTree;
        Splits splits = new Splits();
        Map consensus = new HashMap();  // we will store all splits and the count in the Map

        if (trees.getNtrees() == 1) System.err.println("Consensus network: only one Tree specified");

        // long start=new Date().getTime();
        for (int which = 1; which <= trees.getNtrees(); which++) {
            doc.notifySetProgress(50 * which / trees.getNtrees());
            tempTree = new Trees(trees.getName(which), trees.getTree(which), taxa, trees.getTranslate());
            trans = new TreeSelector();
            splits = trans.apply(null, taxa, tempTree);  //DJB: pass null here so that progress bar doesn't appear
            for (int i = 1; i <= splits.getNsplits(); i++) {
                Add(consensus, splits.get(i).getBits(), splits.getWeight(i));
            }
        }

        splits = new Splits(taxa.getNtax());
        Object[] keys = consensus.keySet().toArray();
        for (int t = 0; t < keys.length; t++) {
            doc.notifySetProgress(50 + 50 * t / keys.length);
            // check if the Split is in the consensus and if the appearance is high enough
            TaxaSet nTSet = new TaxaSet((BitSet) keys[t]);
            double wgt;
            WeightStats value = (WeightStats) consensus.get(keys[t]);
            if (value.getCount() / (double) trees.getNtrees() > threshold) {
                switch (getOptionEdgeWeights()) {
                    case "count":
                        wgt = value.getCount();
                        break;
                    case "mean":
                        wgt = value.getMean();
                        break;
                    case "median":
                        wgt = value.getMedian();
                        break;
                    case "sum":
                        wgt = value.getSum();
                        break;
                    default:
                        wgt = 1;
                        break;
                }

                float confidence = (float) value.getCount() / (float) trees.getNtrees();
                splits.add(nTSet, (float) wgt, confidence);
            }
        }
        splits.getFormat().setConfidences(true);
        /*
        long stop = new Date().getTime();
        System.out.println("Time needed to compute the Consensus Network is :"+((double)(stop-start)/1000.0)+" sec..");
        */
        System.runFinalization();
        doc.notifySetProgress(100);
        // pd.close();								//get rid of the progress listener
        // doc.setProgressListener(null);

        return splits;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * @param consensus the Map, we have to add the key and the value to
     * @param key       the key of where the value has to be added to. If the key is not in the Map it will be added
     */
    private static void Add(Map consensus, BitSet key, float weight) {
        WeightStats value;
        if (consensus.containsKey(key))
            value = (WeightStats) consensus.get(key);
        else {
            value = new WeightStats();
            consensus.put(key, value);
        }
        value.add(weight);
    }

    /**
     * gets the threshold (value between 0 and 1)
     *
     * @return the threshold
     */
    public double getOptionThreshold() {
        return threshold;
    }

    /**
     * sets the threshold (between 0 and 1)
     *
     * @param threshold
     */
    public void setOptionThreshold(double threshold) {
        this.threshold = Math.min(1.0, Math.max(0.0, threshold));
    }

    /**
     * decide what to scale the edge weights by
     *
     * @return
     */
    public String getOptionEdgeWeights() {
        return optionEdgeWeights;
    }

    /**
     * decide what to scale the edge weights by
     *
     * @param optionEdgeWeights
     */
    public void setOptionEdgeWeights(String optionEdgeWeights) {
        this.optionEdgeWeights = optionEdgeWeights;
    }

    /**
     * return the possible chocies for optionEdgeWeights
     *
     * @param doc
     * @return list of choices
     */
    public List selectionOptionEdgeWeights(Document doc) {
        List list = new LinkedList();
        list.add(MEDIAN);
        list.add(MEAN);
        list.add(COUNT);
        list.add(SUM);
        list.add(NONE);
        return list;
    }

    /**
     * a value object contains a set of all weights seen so far and their counts
     */
    private static class WeightStats {
        List weights;
        int totalCount;
        double sum;

        /**
         * construct a new values map
         */
        WeightStats() {
            weights = new LinkedList();
            totalCount = 0;
            sum = 0;
        }

        /**
         * add the given weight and count
         *
         * @param weight
         */
        void add(float weight) {
            Float fWeight = weight;
            weights.add(fWeight);

            totalCount++;
            sum += weight;
        }

        /**
         * returns the number of values
         *
         * @return number
         */
        int getCount() {
            return totalCount;
        }

        /**
         * computes the mean values
         *
         * @return mean
         */
        double getMean() {
            return sum / (double) totalCount;
        }

        /**
         * computes the median value
         *
         * @return median
         */
        public double getMedian() {
            Object[] array = weights.toArray();
            Arrays.sort(array);
            return (Float) array[array.length / 2];

        }

        /**
         * returns the sum of weights
         *
         * @return sum
         */
        double getSum() {
            return sum;
        }
    }
}
