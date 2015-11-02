/**
 * Unaligned.java
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
/* $Id: Unaligned.java,v 1.27 2007-09-11 12:30:58 kloepper Exp $ */
package splitstree4.nexus;

import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.util.TaxaUtilities;

import java.io.*;
import java.util.List;

/**
 * The nexus characters block
 * Contains unaligned sequence data
 */
public class Unaligned extends NexusBlock {
    /**
     * the format subclass
     */
    public final class Format {
        private String datatype, symbols;
        private boolean respectCase, labels;
        private char missing;

        /**
         * the Constructor
         */
        public Format() {
            datatype = "standard";
            symbols = "";
            respectCase = false;
            labels = true;
            missing = '?';

        }

        /**
         * Gets the datatype
         *
         * @return datatype
         */
        public String getDatatype() {
            return this.datatype;
        }

        /**
         * Sets the datatype
         *
         * @param str name of the datatype
         */
        private void setDatatype(String str) {
            this.datatype = str;
            this.resetSymbols();
        }

        /**
         * Resets the set of all symbols ,depending on datatype.
         */
        private void resetSymbols() {
            if (this.datatype.equals("standard"))
                this.symbols = "01";
            if (this.datatype.equals("dna"))
                this.symbols = ("atgc");
            if (this.datatype.equals("rna"))
                this.symbols = ("augc");
            if (this.datatype.equals("nucleotide"))
                this.symbols = ("atugc");
            if (this.datatype.equals("protein"))
                this.symbols = ("arndcqeghilkmfpstwyvz");
            // computeColors();

        }

        /**
         * Set the symbols.
         * If the datatype=standard, then replace symbols by the given ones.
         * For other datatypes, append symbols to existing ones.
         *
         * @param sym the symbols
         */
        public void setSymbols(String sym) {
            if (this.datatype.equals("standard"))
                symbols = sym;
            else {
                String str = symbols; // because possibly sym=symbol
                for (int i = 0; i < sym.length(); i++) {
                    char ch = sym.charAt(i);
                    if (ch != ' ' && symbols.indexOf(ch) == -1)
                        str += ch;
                }
                symbols = str;
            }
            //computeColors();
        }

        /**
         * Get the symbols set using the symbols statement.
         *
         * @return the symbols
         */
        public String getSymbols() {
            return symbols;
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

        /**
         * Get the value of respectcase
         *
         * @return the value of respectcase
         */

        public boolean getRespectCase() {
            return respectCase;
        }

        /**
         * Set the value of respectcase
         *
         * @param respectCase the value of respectCase
         */
        public void setRespectCase(boolean respectCase) {
            this.respectCase = respectCase;
        }

        /**
         * Get the value of the missing character
         *
         * @return the value of the missing character
         */
        public char getMissing() {
            return missing;
        }

        /**
         * Set the value of missing
         *
         * @param missing
         */
        public void setMissing(char missing) throws SplitsException {
            if (NexusStreamTokenizer.isLabelPunctuation(missing)
                    || NexusStreamTokenizer.isSpace(missing))
                throw new SplitsException("illegal missing-character:" + missing);
            this.missing = missing;

        }

    }

    // Main data:
    private Format fmt = null;
    private int ntax;
    private char[][] matrix;
    private boolean gapMissingMode;
    /**
     * Identification string
     */
    public final static String NAME = "Unaligned";

    /**
     * Construct a new Unaligned object.
     */
    public Unaligned() {
        super();
        ntax = 0;
        gapMissingMode = false;
        matrix = null;
        fmt = new Format();
    }

    /**
     * Return the format object
     *
     * @return the format object
     */
    public Format getFormat() {
        return fmt;
    }

    /**
     * Get the number of taxa.
     *
     * @return the number taxa
     */

    public int getNtax() {
        return ntax;
    }

    /**
     * set the number of taxa
     *
     * @param ntax
     */
    public void setNtax(int ntax) {
        this.ntax = ntax;
        matrix = new char[ntax + 1][];
    }

    /**
     * Get the matrix value.
     *
     * @param t the taxon
     * @param p the position
     * @return the matrix value  matrix[t][p]
     */
    public char get(int t, int p) {
        return matrix[t][p];
    }

    /**
     * Get the row i of matrix (i.e. the sequence for tax i).
     *
     * @param i the row
     * @return the matix row i
     */
    public char[] getRow(int i) {
        return matrix[i];
    }

    /**
     * returns a row of data as a string
     *
     * @param t
     * @return a sequence
     */
    public String getRowAsString(int t) {
        return (String.valueOf(getRow(t))).substring(1);
    }

    /**
     * Set the matrix value.
     *
     * @param i   the row
     * @param j   the colum
     * @param val the matix value at row i and colum j
     */
    public void set(int i, int j, char val) {
        matrix[i][j] = val;
    }

    /**
     * Show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN UNALIGNED;");
        ps.println("\t[DIMENSIONS NTAX=number-of-taxa;]");
        ps.println("\t[FORMAT");
        ps.println("\t    [DATATYPE={STANDARD|DNA|RNA|NUCLEOTIDE|PROTEIN}]");
        ps.println("\t    [RESPECTCASE]");
        ps.println("\t    [MISSING=symbol]");
        ps.println("\t    [SYMBOLS=\"symbol symbol ...\"]");
        ps.println("\t    [LABELS={LEFT|NO}]");
        ps.println("\t;]");
        ps.println("\tMATRIX");
        ps.println("\t    data-matrix");
        ps.println("\t;");
        ps.println("END;");
    }

    /**
     * Write out whitespace
     *
     * @param w     the writer
     * @param taxa  the Taxa
     * @param index the index of label
     */
    private void pad(Writer w, Taxa taxa, int index) throws java.io.IOException {
        int len = taxa.getLabel(index).length();
        int max = TaxaUtilities.getMaxLabelLength(taxa);

        for (int i = 1; i <= (max - len + 2); i++) {
            w.write(" ");
        }
    }

    /**
     * Produces a string representation of the unaligned object
     *
     * @return string representation
     */
    public String toString() {
        return "[Unaligned, ntax=" + getNtax() +
                ", datatype=" + getFormat().getDatatype() + "]";
    }

    /**
     * Read a matrics of unaligned.
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa
     */

    public void read(NexusStreamParser np, Taxa taxa) throws SplitsException, IOException {
        np.matchBeginBlock(NAME);

        if (matrix == null) // haven's got a matrix yet, need dimensions
        {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax="))
                np.matchIgnoreCase("ntax=" + taxa.getNtax());
            ntax = taxa.getNtax();
            np.matchIgnoreCase(";");
        }
        if (np.peekMatchIgnoreCase("FORMAT")) {
            List<String> tokens = np.getTokensLowerCase("format", ";");

            getFormat().datatype = np.findIgnoreCase(tokens, "datatype=", "STANDARD DNA RNA PROTEIN", getFormat().datatype);
            getFormat().setDatatype(getFormat().datatype);
            getFormat().respectCase = np.findIgnoreCase(tokens, "respectcase", true, getFormat().respectCase);
            getFormat().respectCase = np.findIgnoreCase(tokens, "no respectcase", false, getFormat().respectCase);
            getFormat().missing = np.findIgnoreCase(tokens, "missing=", null, getFormat().missing);
            getFormat().setMissing(getFormat().missing);

            {
                String symbols = np.findIgnoreCase(tokens, "symbols=", "\"", "\"", getFormat().symbols);
                getFormat().setSymbols(symbols);
            }

            getFormat().labels = np.findIgnoreCase(tokens, "labels=no", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels=left", true, getFormat().labels);

            getFormat().labels = np.findIgnoreCase(tokens, "no labels", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels", true, getFormat().labels);

            if (taxa.getMustDetectLabels() && !getFormat().getLabels())
                throw new IOException("line " + np.lineno() + ": no labels invalid: no taxlabels given in TAXA block");

            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in FORMAT");
        }
        if (matrix != null) // already have a matrix, can't change the data!
        {
            np.matchIgnoreCase("end;");
            return;
        }
        if (np.peekMatchIgnoreCase("MATRIX")) {
            np.matchIgnoreCase("MATRIX");
            readMatrix(np, taxa);
        }

        np.matchEndBlock();
    }

    /**
     * Read a matrix in standard format
     *
     * @param np   the nexus parser
     * @param taxa the taxa
     */
    private void readMatrix(NexusStreamParser np, Taxa taxa)
            throws java.io.IOException, SplitsException {
        matrix = new char[getNtax() + 1][];
        for (int t = 1; t <= getNtax(); t++) {
            if (taxa.getMustDetectLabels()) {
                taxa.setLabel(t, np.getLabelRespectCase());
            } else if (getFormat().labels)
                np.matchIgnoreCase(taxa.getLabel(t));

            String str;
            str = "";
            int length = 0;

            String tmp = np.getWordRespectCase();
            str += tmp;
            length += tmp.length();

            if (t != ntax)
                np.matchIgnoreCase(",");
            else
                np.matchIgnoreCase(";");
            matrix[t] = new char[length + 1];
            for (int i = 1; i <= str.length(); i++) {
                char ch = Character.toLowerCase(str.charAt(i - 1));
                matrix[t][i] = ch;
            }

        }
        if (taxa.getMustDetectLabels()) {
            taxa.checkLabelsAreUnique();
            taxa.setMustDetectLabels(false);
        }
    }

    /**
     * Write the characters block
     *
     * @param w    the writer
     * @param taxa the taxa
     */
    public void write(Writer w, Taxa taxa) throws java.io.IOException {
        w.write("\nBEGIN " + Unaligned.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + getNtax() + ";\n");
        w.write("FORMAT\n");
        if (getFormat().getDatatype().equalsIgnoreCase("STANDARD"))
            w.write("\tdatatype=STANDARD\n");
        else if (getFormat().getDatatype().equalsIgnoreCase("DNA"))
            w.write("\tdatatype=DNA\n");
        else if (getFormat().getDatatype().equalsIgnoreCase("RNA"))
            w.write("\tdatatype=RNA\n");
        else if (getFormat().getDatatype().equalsIgnoreCase("NUCLEOTIDE"))
            w.write("\tdatatype=NUCLEOTIDE\n");
        else if (getFormat().getDatatype().equalsIgnoreCase("PROTEIN"))
            w.write("\tdatatype=PROTEIN\n");

        if (getFormat().getRespectCase())
            w.write("\trespectcase\n");

        if (getFormat().getMissing() != 0)
            w.write("\tmissing=" + getFormat().getMissing() + "\n");

        if (!getFormat().getSymbols().equals("")) {
            w.write("\tsymbols=\"");
            for (int i = 0; i < getFormat().getSymbols().length(); i++) {
                if (i > 0)
                    w.write(" ");
                w.write(getFormat().getSymbols().charAt(i));
            }
            w.write("\"\n");
        }

        if (getFormat().getLabels())
            w.write("\tlabels=left\n");
        else
            w.write("\tlabels=no\n");

        w.write(";\n");

        w.write("MATRIX\n");
        try {
            writeMatrix(w, taxa);
        } catch (SplitsException ex) {
        } // simply can't happen

        w.write("END; [" + Unaligned.NAME + "]\n");
    }

    /**
     * Write a matrics in standard format.
     *
     * @param w    the writer
     * @param taxa the taxa
     */
    private void writeMatrix(Writer w, Taxa taxa) throws
            java.io.IOException, SplitsException {
        for (int t = 1; t <= ntax; t++) {
            if (getFormat().getLabels()) {
                w.write("'" + taxa.getLabel(t) + "'");
                pad(w, taxa, t);
            }
            int len = matrix[t].length - 1;
            for (int c = 1; c <= len; c++)
                w.write(get(t, c));
            if (t != ntax)
                w.write(",");
            else
                w.write(";");
            w.write("\n");
        }
    }

    /**
     * Returns the max number of characters in a row
     *
     * @return max length
     */
    public int getMaxLength() {
        int maxLength = 0;
        for (int i = 1; i <= getNtax(); i++) {
            char[] row = getRow(i);
            if (row.length > maxLength)
                maxLength = row.length;
        }
        return maxLength - 1;
    }

    /**
     * gets the value of a format switch
     *
     * @param name
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        return !name.equalsIgnoreCase("labels") || getFormat().getLabels();
    }

    public Unaligned clone(Taxa taxa) {
        Unaligned result = new Unaligned();
        try {
            StringWriter sw = new StringWriter();
            write(sw, taxa);
            NexusStreamParser np = new NexusStreamParser(new StringReader(sw.toString()));
            result.read(np, taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return result;
    }

    private Taxa previousTaxa;   // keep this to help determine whether to reconstruct
    private Unaligned originalUnaligned;

    /**
     * return the induced object obtained by hiding taxa
     *
     * @param origTaxa
     * @param hiddenTaxa
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if (hiddenTaxa.cardinality() == 0 && originalUnaligned == null)
            return;   // nothing to do

        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = (Taxa) inducedTaxa.clone();

        if (originalUnaligned == null)
            originalUnaligned = this.clone(origTaxa); // make a copy

        setNtax(inducedTaxa.getNtax());

        int count = 0;
        for (int t = 1; t <= origTaxa.getNtax(); t++) {
            if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                count++;
                matrix[count] = originalUnaligned.matrix[t];
            }
        }
    }
}

