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

/**
 * @version $Id: Splits.java,v 1.72 2008-07-01 19:06:00 bryant Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.nexus;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree.core.SplitsException;
import splitstree.core.SplitsSet;
import splitstree.core.TaxaSet;
import splitstree.util.Interval;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * NexusBlock splits class
 */
public class Splits extends NexusBlock implements Cloneable {
    /**
     * Identification string
     */
    final public static String NAME = "Splits";
    private Format format = null;
    private Properties properties = null;
    private float threshold;
    private int[] cycle;
    private SplitsSet splits;

    /**
     * The format subclass
     */
    public class Format implements Cloneable {
        private boolean labels = false;
        private boolean weights = true;
        private boolean confidences = false;
        private boolean intervals = false;

        /**
         * Constructor
         */
        public Format() {
        }

        /**
         * Show labels?
         *
         * @return true, if labels are to be printed
         */
        public boolean getLabels() {
            return labels;
        }

        /**
         * Show weights?
         *
         * @return true, if weights are to be printed
         */
        public boolean getWeights() {
            return weights;
        }

        /**
         * Show labels
         *
         * @param flag whether labels should be printed
         */
        public void setLabels(boolean flag) {
            labels = flag;
        }

        /**
         * Show weights
         *
         * @param flag whether weights should be printed
         */
        public void setWeights(boolean flag) {
            weights = flag;
        }

        /**
         * show confidences?
         *
         * @return confidence
         */
        public boolean getConfidences() {
            return confidences;
        }

        /**
         * show confidences?
         *
         * @param confidences
         */
        public void setConfidences(boolean confidences) {
            this.confidences = confidences;
        }

        /**
         * show confidence intervals?
         */
        public boolean getIntervals() {
            return intervals;
        }

        /**
         * show confidence intervals?
         *
         * @param intervals
         */
        public void setIntervals(boolean intervals) {
            this.intervals = intervals;
        }

    }

    /**
     * The properties of the splits
     */
    public class Properties implements Cloneable {
        public final static int COMPATIBLE = 1;
        public final static int CYCLIC = 2;
        public final static int WEAKLY_COMPATIBLE = 3;
        public final static int INCOMPATIBLE = 4;
        public final static int UNKNOWN = 5;
        int compatibility = UNKNOWN;
        private double fit = -1.0;
        private double lsfit = -1.0;
        private boolean leastSquares = false;

        /**
         * do edges represent least square estimates?
         *
         * @return least squares
         */
        public boolean isLeastSquares() {
            return leastSquares;
        }

        /**
         * do edges represent least square estimates?
         *
         * @param leastSquares
         */
        public void setLeastSquares(boolean leastSquares) {
            this.leastSquares = leastSquares;
        }

        /**
         * Constructor
         */
        public Properties() {
        }

        /**
         * Gets the fit value
         *
         * @return the fit value
         */
        public double getFit() {
            return fit;
        }

        /**
         * Sets the fit value
         *
         * @param fit the fit value
         */
        public void setFit(double fit) {
            this.fit = fit;
        }

        public double getLSFit() {
            return this.lsfit;
        }

        public void setLSFit(double fit) {
            this.lsfit = fit;
        }

        /**
         * Returns the compatibilty value
         *
         * @return is compatible
         */
        public int getCompatibility() {
            return compatibility;
        }

        /**
         * Set the compatiblity value
         *
         * @param flag the compatibility value
         */
        public void setCompatibility(int flag) {
            compatibility = flag;
        }
    }


    /**
     * Gets the splits set
     *
     * @return the splits set
     */
    public SplitsSet getSplitsSet() {
        return splits;
    }

    /**
     * Adds splits from a splits set
     *
     * @param splitsset the splits to be added
     */
    public void addSplitsSet(SplitsSet splitsset) {
        for (int s = 1; s <= splitsset.getNsplits(); s++) {
            add(splitsset.getSplit(s), splitsset.getWeight(s));
        }
    }

    /**
     * Construct a new Splits object.
     */
    public Splits() {
        super();
        format = new Format();
        properties = new Properties();
        splits = new SplitsSet();
        threshold = 0;
        cycle = null;
    }

    /**
     * Construct a new Splits object.
     *
     * @param ntax the number of taxa
     */
    public Splits(int ntax) {
        this();
        this.splits.setNtax(ntax);
    }

    /**
     * Return the format object
     *
     * @return the format object
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Returns the properties object
     *
     * @return the properties object
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Gets the weight threshold set below which splits are ignored
     *
     * @return the threshold
     */
    public float getThreshold() {
        return this.threshold;
    }

    /**
     * Sets the weight threshold set below which splits are ignored
     *
     * @param t the threshold
     */
    public void setThreshold(float t) {
        this.threshold = t;
    }

    /**
     * Get the number of taxa.
     *
     * @return number of taxa
     */
    public int getNtax() {
        return this.splits.getNtax();
    }

    /**
     * Set the number of taxa
     *
     * @param ntax the number of taxa
     */
    public void setNtax(int ntax) {
        this.splits.setNtax(ntax);
    }

    /**
     * Get the number of splits.
     *
     * @return number of splits
     */
    public int getNsplits() {

        return this.splits.getNsplits();
    }

    /**
     * Set the number of splits.
     *
     * @param nsplits number of splits
     */
    public void setNsplits(int nsplits) {
        this.splits.setNsplits(nsplits);
    }

    /**
     * Sets the cyclic ordering of the taxa. Use indices 1...ntax
     *
     * @param cycle a permutation of the numbers 1...ntax
     */
    public void setCycle(int[] cycle) throws SplitsException {
        if (cycle != null) {
            BitSet taxa = new BitSet();
            for (int i = 1; i < cycle.length; i++) {
                if (taxa.get(cycle[i]))
                    throw new SplitsException("setCycle(): Multiple occurence of taxon " + i);
                taxa.set(cycle[i]);
            }
        }
        this.cycle = cycle;
    }

    /**
     * Gets the cyclic ordering of the taxa. Use indices 1...ntax
     *
     * @return the cyclic ordering as a permutation of 1..ntax
     */
    public int[] getCycle() {
        return cycle;
    }


    /**
     * Gets the label of the i-th split
     *
     * @param i the index of the split
     * @return the split label
     */
    public String getLabel(int i) {
        return splits.getLabel(i);
    }

    /**
     * Sets the label of the i-th taxon
     *
     * @param i   the index of the taxon
     * @param lab the label
     */
    public void setLabel(int i, String lab) {
        splits.setLabel(i, lab);
    }


    /**
     * Clears the list of splits
     */
    public void clear() {
        splits.setNtax(0);
        splits.setNsplits(0);
        splits.setLabels(new Vector());
        splits.setSplits(new Vector());
        splits.setWeights(new Vector());
    }

    /**
     * Adds a split
     *
     * @param A one side of the split
     */
    public void add(TaxaSet A) {
        splits.add(A);
    }

    /**
     * Adds a split
     *
     * @param A      one side of the split
     * @param weight the weight
     */
    public void add(TaxaSet A, float weight) {
        splits.add(A, weight);
    }

    /**
     * Adds a split
     *
     * @param A          one side of the split
     * @param weight     the weight
     * @param confidence
     */
    public void add(TaxaSet A, float weight, float confidence) {
        splits.add(A, weight, confidence);
    }

    /**
     * Adds a split
     *
     * @param A      one side of the split
     * @param weight the weight
     * @param lab    the label
     */
    public void add(TaxaSet A, float weight, String lab) {
        splits.add(A, weight, lab);
    }

    /**
     * Adds a split
     *
     * @param A          one side of the split
     * @param weight     the weight
     * @param confidence in split
     * @param lab        the label
     */
    public void add(TaxaSet A, float weight, float confidence, String lab) {
        splits.add(A, weight, confidence, lab);
    }

    /**
     * Adds a split
     *
     * @param A
     * @param weight
     * @param confidence
     * @param interval
     * @param lab
     */
    public void add(TaxaSet A, float weight, float confidence, Interval interval, String lab) {
        splits.add(A, weight, confidence, interval, lab);
    }

    /**
     * Removes a split. This will change the indices of other splits in the block (after i)
     *
     * @param i index of split to remove.
     */
    public void remove(int i) {
        splits.remove(i);
    }


    /**
     * Returns the i-th split
     *
     * @param i the index of the split between 1..nsplits
     * @return the taxa set of the split
     */
    public TaxaSet get(int i) {
        return splits.getSplit(i);
    }

    /**
     * Returns the i-th weight
     *
     * @param i the index of the weight between 1..nsplits
     * @return the taxa set of the weight
     */
    public float getWeight(int i) {
        return splits.getWeight(i);
    }

    /**
     * returns the confidence in splits i
     *
     * @param i split
     * @return confidence
     */
    public float getConfidence(int i) {
        return splits.getConfidence(i);
    }

    public void setConfidence(int i, float confidence) {
        splits.setConfidence(i, confidence);
    }

    /**
     * Return the confidence interval for split i
     */
    public Interval getInterval(int i) {
        return splits.getInterval(i);
    }

    public void setInterval(int i, Interval interval) {
        splits.setInterval(i, interval);
    }


    /**
     * Returns a human-readable string describing this split
     *
     * @param i
     * @return split
     */
    public String toLogString(int i) {
        return " [" + this.getLabel(i) + "][" + this.get(i).toString() + "][" + this.getWeight(i) + "]";
    }

    /**
     * Returns a human-readable string describing this object
     *
     * @return object
     */
    public String toLogString() {
        StringWriter sw = new StringWriter();
        try {
            this.write(sw, this.getNtax());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.toString() + "\n" + sw.toString();
    }


    /**
     * Sets the weight of a split
     *
     * @param i   index of the split between 1..nsplits
     * @param wgt the weight
     */
    public void setWeight(int i, float wgt) {
        splits.setWeight(i, wgt);
    }

    /**
     * Gets the first index of a split with the given label
     *
     * @param lab the label
     * @return the index of the first split with the given index, or 0
     */
    public int indexOf(String lab) {
        return splits.indexOf(lab);
    }

    /**
     * Show the usage of splits block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN " + NAME + ";");
        ps.println("\t[DIMENSIONS [NTAX=number-of-taxa] [NSPLITS=number-of-splits];]");
        ps.println("\t[FORMAT");
        ps.println("\t    [LABELS={LEFT|NO}]");
        ps.println("\t    [WEIGHTS={YES|NO}]");
        ps.println("\t    [CONFIDENCES={YES|NO}]");
        ps.println("\t    [INTERVALS={YES|NO}]");
        ps.println("\t;]");
        ps.println("\t[THRESHOLD=non-negative-number;]");
        ps.println("\t[PROPERTIES");
        ps.println("\t    [FIT=non-negative-number]");
        ps.println("\t    [leastsquares]");
        ps.println("\t    [{COMPATIBLE|CYCLIC|WEAKLY COMPATIBLE|INCOMPATIBLE]");
        ps.println("\t;]");
        ps.println("\t[CYCLE [taxon_i_1 taxon_i_2 ... taxon_i_ntax];]");
        ps.println("\t[SPLITSLABELS label_1 label_2 ... label_nsplits;]");
        ps.println("\tMATRIX");
        ps.println("\t    [label_1] [weight_1] [confidence_1] split_1,");
        ps.println("\t    [label_2] [weight_2] [confidence_2] split_2,");
        ps.println("\t    ....");
        ps.println("\t    [label_nsplits] [weight_nsplits] [confidence_nsplits] split_nsplits,");
        ps.println("\t;");
        ps.println("END;");

    }

    /**
     * Writes a splits object in nexus format
     *
     * @param w a writer
     */
    public void write(Writer w, Taxa taxa) throws java.io.IOException {
        write(w, taxa.getNtax());

    }

    /**
     * Writes a splits object in nexus format
     *
     * @param w a writer
     */
    public void write(Writer w, int nTaxa) throws java.io.IOException {
        w.write("\nBEGIN " + Splits.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + getNtax() + " nsplits=" + getNsplits()
                + ";\n");

        w.write("FORMAT");
        if (getFormat().getLabels())
            w.write(" labels=left");
        else
            w.write(" labels=no");
        if (getFormat().getWeights())
            w.write(" weights=yes");
        else
            w.write(" weights=no");
        if (getFormat().getConfidences())
            w.write(" confidences=yes");
        else
            w.write(" confidences=no");
        if (getFormat().getIntervals())
            w.write(" intervals=yes");
        else
            w.write(" intervals=no");
        w.write(";\n");
        if (getThreshold() != 0)
            w.write("THRESHOLD=" + getThreshold() + "; \n");
        w.write("PROPERTIES fit=" + getProperties().getFit());
        if (getProperties().isLeastSquares())
            w.write(" leastsquares");
        switch (getProperties().getCompatibility()) {
            case Properties.COMPATIBLE:
                w.write(" compatible");
                break;
            case Properties.CYCLIC:
                w.write(" cyclic");
                break;
            case Properties.WEAKLY_COMPATIBLE:
                w.write(" weakly compatible");
                break;
            case Properties.INCOMPATIBLE:
                w.write(" non compatible");
            default:
                break;
        }
        w.write(";\n");

        if (getCycle() != null) {
            w.write("CYCLE");
            int[] cycle = getCycle();
            for (int i = 1; i < Array.getLength(cycle); i++)
                w.write(" " + cycle[i]);
            w.write(";\n");
        }
        w.write("MATRIX\n");

        for (int i = 1; i <= getNsplits(); i++) {
            int size = Math.min(get(i).cardinality(), nTaxa - get(i).cardinality());

            w.write("[" + i + ", size=" + size + "]" + " \t");
            if (format.getLabels()) {
                String lab = getLabel(i);
                w.write(" '" + lab + "'" + " \t");
            }
            if (format.getWeights()) {
                float wgt = getWeight(i);
                w.write(" " + wgt + " \t");
            }
            if (format.getConfidences()) {
                float confidence = getConfidence(i);
                w.write(" " + confidence + " \t");
            }
            if (format.getIntervals()) {
                Interval interval = getInterval(i);
                if (interval == null)
                    w.write(" ()\t");
                else
                    w.write(" " + interval.print() + "\t");

            }
            w.write(" " + get(i) + ",\n");

        }
        w.write(";\n");
        w.write("END; [" + Splits.NAME + "]\n");
    }

    /**
     * Reads a splits object in NexusBlock format
     *
     * @param np   nexus stream parser
     * @param taxa the taxa
     */
    public void read(NexusStreamParser np, Taxa taxa) throws IOException {
        if (taxa.getMustDetectLabels())
            throw new IOException("line " + np.lineno() +
                    ": Can't read SPLITS block because no taxlabels given in TAXA block");

        int[] cycle = new int[getNtax() + 1];
        setNtax(taxa.getNtax());

        if (np.peekMatchBeginBlock("st_splits"))  // read old format
            np.matchBeginBlock("st_splits");
        else
            np.matchBeginBlock(NAME);

        if (np.peekMatchIgnoreCase("DIMENSIONS")) {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax=")) {
                np.matchIgnoreCase("ntax=" + getNtax());
            }
            if (np.peekMatchIgnoreCase("nsplits=")) {
                np.matchIgnoreCase("nsplits=");
                setNsplits(np.getInt());
            }
            np.matchIgnoreCase(";");
        }

        if (np.peekMatchIgnoreCase("FORMAT")) {
            List f = np.getTokensLowerCase("format", ";");
            format.labels = np.findIgnoreCase(f, "labels=no", false, format.labels);
            format.labels = np.findIgnoreCase(f, "labels=left", true, format.labels);

            format.weights = np.findIgnoreCase(f, "weights=no", false, format.weights);
            format.weights = np.findIgnoreCase(f, "weights=yes", true, format.weights);

            format.confidences = np.findIgnoreCase(f, "confidences=no", false, format.confidences);
            format.confidences = np.findIgnoreCase(f, "confidences=yes", true, format.confidences);

            format.intervals = np.findIgnoreCase(f, "intervals=no", false, format.intervals);
            format.intervals = np.findIgnoreCase(f, "intervals=yes", true, format.intervals);

            // for backward compatibility:
            format.labels = np.findIgnoreCase(f, "no labels", false, format.labels);
            format.labels = np.findIgnoreCase(f, "labels", true, format.labels);

            format.weights = np.findIgnoreCase(f, "no weights", false, format.weights);
            format.weights = np.findIgnoreCase(f, "weights", true, format.weights);

            format.confidences = np.findIgnoreCase(f, "no confidences", false, format.confidences);
            format.confidences = np.findIgnoreCase(f, "confidences", true, format.confidences);

            format.intervals = np.findIgnoreCase(f, "no intervals", false, format.intervals);
            format.intervals = np.findIgnoreCase(f, "intervals", true, format.intervals);

            if (f.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + f +
                        "' unexpected in FORMAT");

        }
        if (np.peekMatchIgnoreCase("threshold=")) {
            np.matchIgnoreCase("threshold=");
            setThreshold((float) np.getDouble());
            np.matchIgnoreCase(";");
        }
        if (np.peekMatchIgnoreCase("PROPERTIES")) {
            List p = np.getTokensLowerCase("properties", ";");

            properties.fit = np.findIgnoreCase(p, "fit=", -1.0, 100.0, properties.fit);

            if (np.findIgnoreCase(p, "weakly compatible", true,
                    properties.compatibility == Properties.WEAKLY_COMPATIBLE))
                properties.compatibility = Properties.WEAKLY_COMPATIBLE;

            if (np.findIgnoreCase(p, "non compatible", true,
                    properties.compatibility == Properties.INCOMPATIBLE))
                properties.compatibility = Properties.INCOMPATIBLE;

            if (np.findIgnoreCase(p, "compatible", true,
                    properties.compatibility == Properties.COMPATIBLE))
                properties.compatibility = Properties.COMPATIBLE;

            if (np.findIgnoreCase(p, "cyclic", true,
                    properties.compatibility == Properties.CYCLIC))
                properties.compatibility = Properties.CYCLIC;

            if (np.findIgnoreCase(p, "incompatible", true,
                    properties.compatibility == Properties.INCOMPATIBLE))
                properties.compatibility = Properties.INCOMPATIBLE;

            if (np.findIgnoreCase(p, "leastsquares", true, getProperties().isLeastSquares()))
                getProperties().setLeastSquares(true);
            else
                getProperties().setLeastSquares(false);
        }

        if (np.peekMatchIgnoreCase("CYCLE")) {
            np.matchIgnoreCase("cycle");
            cycle = new int[getNtax() + 1];
            for (int i = 1; i <= getNtax(); i++)
                cycle[i] = np.getInt();
            np.matchIgnoreCase(";");
            try {
                setCycle(cycle);
            } catch (SplitsException ex) {
                Basic.caught(ex);
                new Alert("Read-cycle failed: multiple occurrences of taxon");
            }
        }
        if (np.peekMatchIgnoreCase("matrix")) {
            np.matchIgnoreCase("matrix");
            readMatrix(np);
            np.matchIgnoreCase(";");
        }
        np.matchEndBlock();
    }

    /**
     * Read a matrix.
     *
     * @param np the nexus parser
     */
    private void readMatrix(NexusStreamParser np) throws IOException {
        splits.setLabels(new Vector());
        splits.setSplits(new Vector());
        splits.setWeights(new Vector());
        splits.setIntervals(new Vector());

        int nsplits = getNsplits();
        setNsplits(0); // need to set this to 0 because add increments it
        float wgt = 1;
        float confidence = -1;
        Interval interval = null;

        for (int i = 1; i <= nsplits; i++) {
            String label = null;
            if (format.labels) {
                label = np.getWordRespectCase();
                if (label.equals("null"))
                    label = null;
            }
            if (format.weights)
                wgt = (float) Math.max(0.0, np.getDouble());

            if (format.confidences)
                confidence = (float) Math.max(0.0, np.getDouble());

            if (format.intervals) {
                if (np.peekMatchIgnoreCase("(")) {
                    interval = new Interval();
                    np.matchIgnoreCase("(");
                    interval.low = (float) Math.max(0.0, np.getDouble());
                    np.matchIgnoreCase(",");
                    interval.high = (float) Math.max(0.0, np.getDouble());
                    np.matchIgnoreCase(")");
                }
            }


            TaxaSet ts = new TaxaSet();

            while (!np.peekMatchIgnoreCase(",")) {
                Integer t = new Integer(np.getWordRespectCase());
                ts.set(t);

            }
            np.matchIgnoreCase(",");
            if (ts.cardinality() == 0 || ts.cardinality() == getNtax())
                throw new IOException("line " + np.lineno() + ": non-split of size " +
                        ts.cardinality());
            if (confidence == -1) {
                confidence = 1;
            }

            add(ts, wgt, confidence, interval, label);
            /*       edited by DJB
            if (confidence == -1)
                add(ts, wgt, label);
            else
                add(ts, wgt, confidence, label);
            */
        }
    }

    /**
     * Produces a string representation of a NexusBlock object
     *
     * @return string representation
     */
    public String toString() {
        return "[Splits, ntax=" + getNtax() + ", nsplits="
                + getNsplits() + "]";
    }


    /**
     * Produces a string representation of a NexusBlock object
     *
     * @return string representation
     */
    public String toString(Taxa taxa) {
        StringWriter w = new StringWriter();
        try {
            write(w, taxa);
        } catch (IOException e) {
            Basic.caught(e);
        }
        return w.toString();
    }

    /**
     * Returns a clone of this block
     *
     * @return a full clone
     */
    public Splits clone(Taxa taxa) {

        Splits aClone = new Splits();
        if (getNtax() > 0 && getNsplits() > 0)
            aClone.copy(taxa, this);

        return aClone;
    }

    /**
     * copy splits
     *
     * @param taxa
     * @param source
     */
    public void copy(Taxa taxa, Splits source) {
        // turn on everything:
        boolean confidences = source.getFormat().getConfidences();
        boolean weights = source.getFormat().getWeights();
        boolean labels = source.getFormat().getLabels();
        source.getFormat().setWeights(true);
        source.getFormat().setConfidences(true);
        source.getFormat().setLabels(true);

        try {
            StringWriter sw = new StringWriter();
            source.write(sw, taxa);
            this.read(new NexusStreamParser(new StringReader(sw.toString())), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        getFormat().setConfidences(confidences);
        getFormat().setWeights(weights);
        getFormat().setLabels(labels);
        source.getFormat().setConfidences(confidences);
        source.getFormat().setWeights(weights);
        source.getFormat().setLabels(labels);
    }

    /**
     * gets the value of a format switch
     *
     * @param name
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        if (name.equalsIgnoreCase("labels"))
            return getFormat().getLabels();
        else if (name.equalsIgnoreCase("weights"))
            return getFormat().getWeights();
        else
            return !name.equalsIgnoreCase("confidences") || getFormat().getConfidences();
    }

    private Taxa previousTaxa;
    private Splits originalSplits;

    /**
     * induces splits not containing the hidden taxa
     *
     * @param origTaxa
     * @param hiddenTaxa
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if ((hiddenTaxa == null || hiddenTaxa.cardinality() == 0) && originalSplits == null)
            return;   // nothing to do

        if (originalSplits == null)
            originalSplits = this.clone(origTaxa); // make a copy

        //Compute the new taxa block and save the old one.
        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = inducedTaxa;

        //Compute a map from the old taxa to the new taxa.
        int[] old2new = new int[origTaxa.getNtax() + 1];
        {
            int count = 0;
            for (int t = 1; t <= origTaxa.getNtax(); t++) {
                if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                    old2new[t] = ++count;
                } //else it is 0 by default
            }
        }

        //Compute a new cycle by finding the restriction of the old one.
        int[] newCycle = null;
        int[] oldCycle = getCycle();
        if (getCycle() != null) {
            newCycle = new int[inducedTaxa.getNtax() + 1];
            int newPos = 0;
            for (int oldPos = 1; oldPos <= getNtax(); oldPos++) {
                int oldId = oldCycle[oldPos];
                if (old2new[oldId] != 0) {
                    newCycle[++newPos] = old2new[oldId];
                }
            }
        }

        //Now clear the splits and put in the induced splits.

        clear();
        setNtax(inducedTaxa.getNtax());

        Map split2id = new HashMap();
        int[] id2count = new int[originalSplits.getNsplits() + 1];

        int newSplitId = 0;
        for (int s = 1; s <= originalSplits.getNsplits(); s++) {
            TaxaSet split = new TaxaSet();
            for (int t = 1; t <= origTaxa.getNtax(); t++) {
                if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                    if (originalSplits.get(s).get(t) && old2new[t] != 0)
                        split.set(old2new[t]);
                }
            }
            if (!split.get(1))
                split = split.getComplement(inducedTaxa.getNtax());

            if (split.cardinality() > 0 && split.cardinality() < inducedTaxa.getNtax()) {

                if (split2id.containsKey(split)) {
                    int i = (Integer) split2id.get(split);
                    setWeight(i, getWeight(i) + originalSplits.getWeight(s));
                    setConfidence(i, (getConfidence(i) + originalSplits.getConfidence(s)) / 2);  //Average bootstrap proportions.
                    if (getLabel(i) != null && originalSplits.getLabel(s) != null)
                        setLabel(i, getLabel(i) + "_" + originalSplits.getLabel(s));
                    id2count[i]++;
                } else {
                    add(split, originalSplits.getWeight(s), originalSplits.getConfidence(s),
                            originalSplits.getLabel(s));
                    split2id.put(split, ++newSplitId);
                    id2count[newSplitId] = 1;
                }
            }
        }

        try {
            setCycle(newCycle);
        } catch (SplitsException e) {
            Basic.caught(e);
        }

        getProperties().setFit(-1);
        getProperties().setLSFit(-1);

        // average confidence:
        for (int s = 1; s <= getNsplits(); s++)
            setConfidence(s, getConfidence(s) / id2count[s]);
    }

    /**
     * restores the original splits
     *
     * @param originalTaxa
     */
    public void restoreOriginal(Taxa originalTaxa) {
        this.copy(originalTaxa, originalSplits);
        previousTaxa = originalTaxa;
    }

    /**
     * save a copy of myself into original splits.
     *
     * @param originalTaxa
     */
    public void setOriginal(Taxa originalTaxa) {
        originalSplits = this.clone(originalTaxa);
        previousTaxa = null;
    }

    /**
     * get the original splits
     *
     * @return original splits
     */
    public Splits getOriginal() {
        return originalSplits;
    }

    /**
     * hide the named splits
     *
     * @param taxa
     * @param toHide
     */
    public void hideSplits(Taxa taxa, BitSet toHide) {
        if (toHide.cardinality() > 0) {
            if (originalSplits == null)
                setOriginal(taxa);
            Splits tmp = this.clone(taxa);
            clear();
            setNtax(taxa.getNtax());
            for (int s = 1; s <= tmp.getNsplits(); s++)
                if (!toHide.get(s))
                    add(tmp.get(s), tmp.getWeight(s), tmp.getConfidence(s), tmp.getLabel(s));
        }
    }
}

// EOF
