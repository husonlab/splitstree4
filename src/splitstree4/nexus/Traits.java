/*
 * Traits.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.nexus;

import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;

import java.io.*;
import java.util.HashMap;
import java.util.List;

/**
 * Traits NEXUS block
 * <p/>
 * This is a custom designed block for storing trait information about the taxa.
 * Basically this is a very thin wrapper around spreadsheet of data
 */
public class Traits extends NexusBlock {

    public static final String MISSING_TRAIT = "?";

    public class Format {

        public final String COMMA = "COMMA";
        public final String SPACES = "SPACES";
        public final String TAB = "TAB";

        private boolean taxonLabels; //flag indicating whether first column contains taxon labels
        private char missingTrait;
        private String separator;

        /**
         * Constructor
         */
        public Format() {
            taxonLabels = false;
            missingTrait = '?';
            separator = TAB;
        }


        public boolean hasTaxonLabels() {
            return taxonLabels;
        }

        public void setTaxonLabels(boolean taxonLabels) {
            this.taxonLabels = taxonLabels;
        }

        public char getMissingTrait() {
            return missingTrait;
        }

        public void setMissingTrait(char missingTrait) {
            this.missingTrait = missingTrait;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }
    }

    /**
     * Identification string
     */
    public final static String NAME = "Traits";
    private String[][] matrix;
    private HashMap<String, Integer> traitNumbers; //Map from attribute names to columns

    private Format format = null;
    private int ntax;
    private int nTraits;

    /**
     * Sets up an empty traits block
     */
    public Traits() {
        super();
        ntax = 0;
        nTraits = 0;
        format = new Format();
        traitNumbers = null;
        matrix = null;
    }

    /**
     * Sets up an empty trait block with a given number of taxa
     *
     * @param ntax number of taxa
     */
    public Traits(int ntax) {
        this();
        setNtax(ntax);
    }

    /**
     * Return the format block
     *
     * @return format
     */
    public Format getFormat() {
        return format;
    }


    /**
     * Returns number of taxa
     *
     * @return number of taxa
     */
    public int getNtax() {
        return ntax;
    }

    /**
     * Sets the number of taxa and initialises the data.
     *
     * @param ntax new number of taxa
     */
    protected void setNtax(int ntax) {
        this.ntax = Math.max(0, ntax);
        if (nTraits > 0)
            this.matrix = new String[ntax + 1][nTraits];
    }

    /**
     * Get number of traits
     *
     * @return number of traits
     */
    public int getNtraits() {
        return nTraits;
    }

    /**
     * Get a trait value
     *
     * @param taxon     id of the taxon
     * @param traitName name of the trait
     * @return value of the trait for that taxon
     */
    public String get(int taxon, String traitName) {
        int col = getTraitNumber(traitName);
        if (col == 0)
            return null;

        if (taxon < 1 || taxon > ntax)
            throw new IllegalArgumentException("Illegal taxon number");

        return matrix[taxon][col];
    }

    /**
     * Get a trait value
     *
     * @param taxon       id of the taxon
     * @param traitNumber number of the trait
     * @return value of the trait for that taxon
     */
    public String get(int taxon, int traitNumber) {
        if (taxon < 1 || taxon > ntax)
            throw new IllegalArgumentException("Illegal taxon number");

        return matrix[taxon][traitNumber];
    }

    /**
     * Return array of trait values for a given trait
     *
     * @param traitName name of the trait
     * @return String[] array of values
     */
    public String[] getTraitValues(String traitName) {
        int col = getTraitNumber(traitName);
        if (col == 0)
            return null;
        String[] v = new String[ntax + 1];
        for (int i = 1; i <= ntax; i++)
            v[i] = matrix[i][col];
        return v;
    }


    /**
     * Returns the column number associated with a trait name, or 0 if there is none.
     *
     * @param traitName String: name of the trait
     * @return number for that trait (int) or 0 if there is no such trait.
     */
    public int getTraitNumber(String traitName) {
        Integer col = this.traitNumbers.get(traitName);
        if (col != null)
            return col;
        else
            return 0;
    }

    /**
     * Returns the name of a trait
     *
     * @param traitNumber id of the trait
     * @return String trait name.
     */
    public String getTraitName(int traitNumber) {
        return matrix[0][traitNumber];
    }


    public void setTraitName(int i, String lab) throws SplitsException {
        if (i <= 0 || i > ntax)
            throw new SplitsException("index out of range: " + i);
        if (lab != null) {
            String illegal = ";():\\";
            for (int pos = 0; pos < illegal.length(); pos++)
                if (lab.contains("" + illegal.charAt(pos)))
                    throw new SplitsException("Illegal character '" + illegal.charAt(pos) + "' in taxon label ("
                            + i + "): '" + lab + "'");
        }
        matrix[0][i] = lab;
    }

    /**
     * Get a vector of trait names, stored in positions 1...nTraits in the vector. The 0 entry is null.
     *
     * @return
     */
    public String[] getTraitNames() {
        String[] names = new String[nTraits + 1];
        for (int i = 1; i <= nTraits; i++)
            names[i] = getTraitName(i);
        return names;
    }

    /**
     * clones a traits object
     *
     * @param taxa Taxa block that this characters block is associated to
     * @return a clone
     */
    public Traits clone(Taxa taxa) {
        Traits traits = new Traits();
        try {
            StringWriter sw = new StringWriter();
            this.write(sw, taxa);
            StringReader sr = new StringReader(sw.toString());
            traits.read(new NexusStreamParser(sr), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return traits;
    }

    private Taxa previousTaxa;   // keep this to help determine whether to reconstruct
    private Traits originalTraits;

    /**
     * return the induced object obtained by hiding taxa
     *
     * @param origTaxa   original (full?) taxa block
     * @param hiddenTaxa set of taxa to be hidden. If null, hidden set is assumed empty.
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if (hiddenTaxa != null && hiddenTaxa.cardinality() == 0 && originalTraits == null)
            return;   // nothing to do

        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = (Taxa) inducedTaxa.clone();

        if (originalTraits == null)
            originalTraits = this.clone(origTaxa); // make a copy


        this.ntax = inducedTaxa.getNtax();
        this.nTraits = originalTraits.getNtraits();

        matrix = new String[this.ntax + 1][this.nTraits + 1];

        int count = 0;
        System.arraycopy(originalTraits.matrix[0], 1, matrix[0], 1, this.nTraits);
        for (int t = 1; t <= origTaxa.getNtax(); t++) {
            if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                count++;
                System.arraycopy(originalTraits.matrix[t], 1, matrix[count], 1, this.nTraits);
            }
        }

    }


    public void read(NexusStreamParser np, Taxa taxa)
            throws SplitsException, IOException {

        ntax = taxa.getNtax();

        np.matchBeginBlock(NAME);

        np.matchIgnoreCase("DIMENSIONS");
        np.matchIgnoreCase("nTraits=");
        nTraits = np.getInt();
        np.matchIgnoreCase(";");

        matrix = new String[ntax + 1][nTraits + 1];

        if (np.peekMatchIgnoreCase("FORMAT")) {
            List format = np.getTokensRespectCase("format", ";");

            //TODO: Check for spaces around = symbol.
            getFormat().taxonLabels = np.findIgnoreCase(format, "labels=yes", true, getFormat().taxonLabels);
            getFormat().taxonLabels = np.findIgnoreCase(format, "labels=no", false, getFormat().taxonLabels);
            getFormat().taxonLabels = np.findIgnoreCase(format, "labels=left", false, getFormat().taxonLabels);
            getFormat().missingTrait = np.findIgnoreCase(format, "missing=", null, getFormat().missingTrait);


            if (np.findIgnoreCase(format, "separator=" + getFormat().TAB, true, false))
                getFormat().setSeparator(getFormat().TAB);
            if (np.findIgnoreCase(format, "separator=" + getFormat().SPACES, true, false))
                getFormat().setSeparator(getFormat().SPACES);
            if (np.findIgnoreCase(format, "separator=" + getFormat().COMMA, true, false))
                getFormat().setSeparator(getFormat().COMMA);

        }
        ///READ TRAIT LABELS
        np.matchIgnoreCase("TRAITLABELS");
        for (int i = 1; i <= getNtraits(); i++) {
            setTraitName(i, np.getLabelRespectCase());
            //Detect repeated labels
            for (int j = 1; j < i; j++) {
                if (getTraitName(i).equals(getTraitName(j)))
                    throw new IOException("Line " + np.lineno() +
                            ": " + getTraitName(i) + " appears twice in the traits block");

            }
        }
        np.matchIgnoreCase(";");

        //READ MATRIX KEYWORD

        np.matchIgnoreCase("MATRIX");
        boolean wasSignificant = np.isEolSignificant();

        if (getFormat().separator.equalsIgnoreCase(getFormat().COMMA)) {
            np.pushPunctuationCharacters(",");
        } else if (getFormat().separator.equalsIgnoreCase(getFormat().TAB)) {
            np.pushPunctuationCharacters("\t");
        } else {
            np.pushPunctuationCharacters("\t ");
        }


        try {
            np.setEolIsSignificant(true);
            final String eolString = "" + (char) StreamTokenizer.TT_EOL;
            boolean hasTaxa = getFormat().hasTaxonLabels();


            np.nextToken(); //Swallow the EOLN following the word 'MATRIX'.
            for (int row = 1; row <= ntax; row++) {
                if (hasTaxa) {
                    np.matchLabelRespectCase(taxa.getLabel(row));
                }
                for (int col = 1; col <= nTraits; col++) {
                    String next = np.getWordRespectCase();
                    if (next.equals(eolString))
                        throw new IOException("line " + np.lineno() + "Unexpected end of line");
                    next = next.trim();
                    if (next.length() == 0)
                        next = "" + getFormat().getMissingTrait();
                    matrix[row][col] = next;
                }
                np.matchWordRespectCase(eolString);
            }

            //Set up the attribute columns
            traitNumbers = new HashMap<>();
            for (int col = 1; col <= nTraits; col++)
                traitNumbers.put(matrix[0][col], col);


        } finally {
            np.setEolIsSignificant(wasSignificant);
            np.popPunctuationCharacters();
        }
        np.matchWordIgnoreCase(";");
        np.matchEndBlock();

    }


    /**
     * write a block, blocks should override this
     *
     * @param w    Writer
     * @param taxa Taxa block for this document
     * @throws java.io.IOException
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        w.write("\nBEGIN " + Traits.NAME + ";\n");
        w.write("\tDIMENSIONS nTRAITS=" + nTraits + ";\n");
        w.write("\tFORMAT  LABELS = " + ((getFormat().hasTaxonLabels()) ? "YES" : "NO"));
        w.write("  MISSING = " + getFormat().getMissingTrait());
        w.write("  SEPARATOR = " + getFormat().getSeparator());
        w.write(" ;\n");

        w.write("\tTRAITLABELS\n");
        for (int i = 1; i <= nTraits; i++)
            w.write("\t\t" + getTraitName(i) + "\n");
        w.write("\t;\n");
        w.write("MATRIX\n");

        for (int i = 1; i <= ntax; i++) {
            if (getFormat().hasTaxonLabels())
                w.write("'" + taxa.getLabel(i) + "'\t");
            for (int col = 1; col <= nTraits; col++) {
                w.write(matrix[i][col]);
                if (col < nTraits)
                    w.write("\t");
            }
            w.write("\n");
        }
        w.write(";\nEND;\n");
    }


    public static void showUsage(PrintStream ps) {
        String Usage = "";
        Usage += "BEGIN TRAITS;\n";
        Usage += "\tDIMENSIONS NTRAITS=number-of-traits;\n";
        Usage += "\t[FORMAT\n";
        Usage += "\t\t[LABELS = {YES|NO}]\n";
        Usage += "\t\t[MISSING = symbol]\n";
        Usage += "\t\t[SEPARATOR = {COMMA|TAB|SPACES}]\n";
        Usage += "\t;]\n";
        Usage += "\tTRAITLABELS trait1 trait2 ... trait_ntraits;\n";
        Usage += "\tMATRIX\n";
        Usage += "\t\ttrait data as an array with rows corresponding to taxa\n";
        Usage += "\t\tand columns corresponding to traits. Adjacent cells\n";
        Usage += "\t\tdelimted by the SEPARATOR, and only row per line.\n";
        Usage += "\t;\n";
        Usage += "END;";
        ps.println(Usage);
    }
}
