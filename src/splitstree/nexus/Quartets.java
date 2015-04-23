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

package splitstree.nexus;

import jloda.util.parse.NexusStreamParser;
import splitstree.core.Quartet;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This is the baseclass for a colection of Quartets
 */
public class Quartets extends NexusBlock {
    /**
     * the Format subclass
     */
    public final class Format {
        /**
         * Weights present or not
         */
        protected boolean weights;
        /**
         * Are the quartets labeld*
         */
        protected boolean labels;

        /**
         * the Constructor
         */
        public Format() {
            weights = true;
            labels = true;

        }

        /**
         * Gets the weights format
         *
         * @return true, if weights are to be displayed
         */
        public boolean getWeights() {
            return this.weights;
        }

        /**
         * Sets the weights format
         *
         * @param weights
         */
        public void setWeights(boolean weights) {
            this.weights = weights;
        }


        /**
         * Get the value of labels
         *
         * @return the value of labels
         */
        public boolean getLabels() {
            return labels;
        }

        /**
         * Set the value of labels.
         *
         * @param labels the value of labels
         */
        public void setLabels(boolean labels) {
            this.labels = labels;
        }
    }

     /**
     * Holds the Format subclass
     */
    protected Quartets.Format fmt = null;
      /**
     * Identification string
     */
    public final static String NAME = "Quartets";
    /**
     * The Collection of quartet Objects
     */
    protected Collection quartets = null;

    /**
     * Default constructor with a size of 100
     */
    public Quartets() {
        this(100);
    }

    /**
     * Constructor with a specified Capacity
     *
     * @param initialCapacity expected number of quartets
     */
    public Quartets(int initialCapacity) {
        super();
        fmt = new Format();
        quartets = new ArrayList(initialCapacity);
    }

    /**
     * Return the format object
     *
     * @return the format object
     */
    public Quartets.Format getFormat() {
        return fmt;
    }

    /**
     * Get the number of quartets
     *
     * @return the number of quartets
     */
    public int size() {
        return quartets.size();

    }

    /**
     * Produces a string representation of the quartets object
     *
     * @return string representation
     */
    public String toString() {
        return "[Quartets, " +
                ", nquartets=" + size() + "]";
    }

    /**
     * Returns a human-readable string describing this object
     *
     * @return log string
     */
    public String toLogString() {
        StringWriter sw = new StringWriter();
        try {
            this.write(sw, 8888);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.toString() + "\n" + sw.toString();
    }


    /**
     * Appends the specified quartet to the end of this list.
     *
     * @param q quartet to be appended to this list.
     * @return <tt>true</tt> (as per the general contract of Collection.add).
     */
    public boolean add(Quartet q) {
        return quartets.add(q);
    }

    /**
     * sets a aquartet
     *
     * @param index the index
     * @param q     the quartet
     */
    public void set(int index, Quartet q) throws SplitsException {
        if (index < 1 || index > size())


            throw new SplitsException("quartet id " + q + ": out of range 1-" + size());

        ((List) quartets).set(index - 1, q);
    }

    /**
     * Returns the quartet at the specified position in this list.
     *
     * @param index the number of the quartet 1..size()
     * @return the quartet at the specified position in this list.
     * @throws SplitsException if index is out of range
     */
    public Quartet get(int index) throws SplitsException {
        int nquartets = size();
        if (index < 1 || index > nquartets)
            throw new SplitsException("quartet id " + index + ": out of range 1-" + nquartets);

        return ((Quartet) ((List) quartets).get(index - 1));
    }


    /**
     * Returns <tt>true</tt> if this set contains the specified element.  More
     * formally, returns <tt>true</tt> if and only if this set contains an
     * element <code>e</code> such that <code>(o==null ? e==null :
     * o.equals(e))</code>.
     *
     * @param o element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     * @throws java.lang.ClassCastException   if the type of the specified element
     *                                        is incompatible with this set (optional).
     * @throws java.lang.NullPointerException if the specified element is null and this
     *                                        set does not support null elements (optional).
     */
    public boolean contains(Object o) {
        return quartets.contains(o);
    }

    /**
     * Returns an iterator over the elements in this set.  The elements are
     * returned in no particular order (unless this set is an instance of some
     * class that provides a guarantee).
     *
     * @return an iterator over the elements in this set.
     */
    public Iterator iterator() {
        return quartets.iterator();
    }

    /**
     * Returns an array containing all of the quartets in this set.
     * Obeys the general contract of the <tt>Collection.toArray</tt> method.
     *
     * @return an array containing all of the elements in this set.
     */
    public Quartet[] toArray() {
        return (Quartet[]) quartets.toArray(new Quartet[quartets.size()]);
    }

    /**
     * clones these quartets (deep clone)
     *
     * @param taxa
     * @return the clone
     */
    public Quartets clone(Taxa taxa) {
        Writer w = new StringWriter();
        Quartets clone = null;
        try {
            clone = this.getClass().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.write(w, taxa);
            w.close();
            String buffer = w.toString();
            Reader r = new StringReader(buffer);
            NexusStreamParser nsp = new NexusStreamParser(r);
            clone.read(nsp, taxa);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clone;
    }


    /**
     * Show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN " + NAME + ";");
        ps.println("\tDIMENSIONS [NTAX=number-of-taxa] NQUARTETS=number-of-quartets;");
        ps.println("\t[FORMAT");
        ps.println("\t    [LABELS={LEFT|NO}]");
        ps.println("\t    [WEIGHTS={YES|NO}]");
        ps.println("\t;]");
        ps.println("\tMATRIX");
        ps.println("\t[label1] [weight1] a1 b1 : c1 d1,");
        ps.println("...");
        ps.println("\t[labeln] [weightn] an bn : cn dn,");
        ps.println("\t;");
        ps.println("END;");
    }

    /**************************************************************************/
    /**
     *   IO Handling
     */

    /**
     * Read a matrics of quartets
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa
     */
    public void read(NexusStreamParser np, Taxa taxa)
            throws IOException {
        int expectedNumberOfQuartets;

        if (np.peekMatchBeginBlock("st_quartets"))
            np.matchBeginBlock("st_quartets");   // for backward compatibility
        else
            np.matchBeginBlock(NAME);

        {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax="))
                np.matchIgnoreCase("ntax=" + taxa.getNtax());
            np.matchIgnoreCase("nquartets=");
            expectedNumberOfQuartets = np.getInt();
            np.matchIgnoreCase(";");
        }
        if (np.peekMatchIgnoreCase("FORMAT")) {
            List tokens = np.getTokensLowerCase("format", ";");

            getFormat().weights = np.findIgnoreCase(tokens,
                    "weights=no", false, getFormat().weights);
            getFormat().weights = np.findIgnoreCase(tokens,
                    "weights=yes", true, getFormat().weights);

            getFormat().labels = np.findIgnoreCase(tokens, "labels=no", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels=left", true, getFormat().labels);

            getFormat().weights = np.findIgnoreCase(tokens,
                    "no weights", false, getFormat().weights);
            getFormat().weights = np.findIgnoreCase(tokens,
                    "weights", true, getFormat().weights);

            getFormat().labels = np.findIgnoreCase(tokens, "no labels", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels", true, getFormat().labels);

            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in FORMAT");
        }
         if (np.peekMatchIgnoreCase("MATRIX")) {
            readMatrix(np, taxa, expectedNumberOfQuartets);
        }
        np.matchEndBlock();
    }

    /**
     * Write the characters block
     *
     * @param w    the writer
     * @param taxa the taxa
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        /** Write the characters block
         *@param w the writer
         *@param taxa the taxa
         */
        write(w, taxa.getNtax());

    }


    /**
     * Write the characters block
     *
     * @param w    the writer
     * @param nTaxa the number of taxa in the object
     */
    public void write(Writer w, int nTaxa) throws IOException {

        w.write("\nBEGIN " + Quartets.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + nTaxa + " nquartets=" + size() + ";\n");
        w.write("FORMAT\n");

        if (getFormat().getLabels())
            w.write("\tlabels=left\n");
        else
            w.write("\tlabels=no\n");
        if (getFormat().getWeights())
            w.write("\tweights=yes\n");
        else
            w.write("\tweights=no\n");

        w.write(";\n");

            writeMatrix(w);

        w.write("END; [" + Quartets.NAME + "]\n");
    }


    /**
     * Write a matrics in standard format.
     *
     * @param w the writer
     */
    protected void writeMatrix(Writer w) throws
            IOException {
        w.write("MATRIX\n");
        for (Object quartet1 : quartets) {
            Quartet quartet = (Quartet) quartet1;
            if (getFormat().getLabels()) {
                w.write("'" + quartet.getLabel() + "'");
            }
            if (getFormat().getWeights())
                w.write(" " + quartet.getWeight());
            w.write(" " + quartet.getA1() + " " + quartet.getA2() + " : " + quartet.getB1() + " " + quartet.getB2() + ",\n");
        }
        w.write(";\n");
    }

    /**
     * Read a matrix in standard format
     *
     * @param np   the nexus parser
     * @param taxa the taxa
     */
    protected void readMatrix(NexusStreamParser np, Taxa taxa, int expectedQuartets)
            throws IOException {
        np.matchIgnoreCase("MATRIX");
        for (int q = 1; q <= expectedQuartets; q++) {
            String label = "<defaultLabel>";
            if (getFormat().getLabels())
                label = np.getWordRespectCase();
            double wgt = 1;
            if (getFormat().getWeights())
                wgt = np.getDouble();
            int a1 = np.getInt();
            int a2 = np.getInt();
            np.matchIgnoreCase(":");
            int b1 = np.getInt();
            int b2 = np.getInt();
            np.matchIgnoreCase(",");
            add(new Quartet(a1, a2, b1, b2, wgt, label));
        }
        np.matchIgnoreCase(";");

    }

    /**************************************************************************/
    /**
     * Hidding an restoring Data @todo implement hide and show
     */

    /**
     * gets the value of a format switch
     *
     * @param name
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        if (name.equalsIgnoreCase("labels"))
            return getFormat().getLabels();
        else
            return !name.equalsIgnoreCase("weights") || getFormat().getWeights();
    }

    /**
     * hide some taxa
     *
     * @param origTaxa
     * @param exTaxa
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet exTaxa) {
        System.err.println("hideTaxa for Quartets: not implemented");
    }
}

// EOF
