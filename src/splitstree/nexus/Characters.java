/**
 * Characters.java 
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
/*
 * $Id: Characters.java,v 1.113 2009-11-27 17:01:12 huson Exp $
*/

package splitstree.nexus;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.util.CharactersUtilities;
import splitstree.util.SparseArray;
import splitstree.util.TaxaUtilities;

import java.io.*;
import java.util.*;


/**
 * The nexus characters block
 * Contains aligned sequence data
 */
public class Characters extends NexusBlock {
    public static final int EXCLUDE_ALL_CONSTANT = -1; // 0: don't exclude, positive: exclude some, -1: exclude all

    /**
     * Flag indicating whether unknown states are converted to missing, or if they return an error.
     */

    //ToDo: Implement read and write for Character Properties

    /**
     * The known Datatypes subclass
     */
    public interface Datatypes {

        String STANDARD = "standard";
        String STANDARDSYMBOLS = "01";
        int STANDARDID = 1;
        String DNA = "dna";
        String DNASYMBOLS = "atgc";
        int DNAID = 2;
        String RNA = "rna";
        String RNASYMBOLS = "augc";
        int RNAID = 3;
        String PROTEIN = "protein";
        String PROTEINSYMBOLS = "arndcqeghilkmfpstwyvz";
        int PROTEINID = 4;
        String MICROSAT = "microsat";
        int MICROSATID = 5;
        String MICROSATSYMBOLS = "";
        int MAXREPEAT = Character.MAX_VALUE - 256; //Maximum number of repeats for a microsat loci.


        String UNKNOWN = "unknown";
        String UNKNOWNSYMBOLS = "";
        int UNKNOWNID = 0;


        //IUPAC Ambiguous DNA characters
        String AMBIGDNA = "wrkysmbhdvn";
        String[] AMBIGDNACODES = {"at", "ag", "gt", "ct", "cg", "ac", "cgt", "act", "agt", "acg", "acgt"};
    }

    /**
     * the format subclass
     */
    public final class Format implements Cloneable {

        //ToDo: methods that change the format should be protected

        /**
         * String used to identify the datatype. These are: "standard"="01", "dna"=atgc", "rna"= "augc", "protein"="arndcqeghilkmfpstwyvz" or "unknown" != one the before
         */
        private String datatype;
        private int datatypeID;


        /**
         * boolean used to determine if the Case should be respected in calculations.
         */
        private boolean respectCase;
        /**
         * boolean used to determine if the matrix format is transpose.
         */
        private boolean transpose;
        /**
         * boolean used to determine if the matrix format is interleave.
         */
        private boolean interleave;
        /**
         * boolean used to determine if the characters have labels.
         */
        private boolean labels;
        /**
         * boolean used to determine if taxon labels should be surrounded by quotes
         */
        private boolean labelQuotes;

        /**
         * boolean used to determine if there is a token.
         */
        private boolean tokens;

        /**
         * boolean used to determine if data is diploid (otherwise assumed haploid)
         * //TODO: Could change to general ploidy number???
         */
        private boolean diploid;

        /**
         * char which holds the symbol for a missing value (standard value = '?').
         */
        private char missing;
        /**
         * char which holds the symbol for the gap (standard value = '-').
         */
        private char gap;
        /**
         * char which holds the symbol for an match (standard value = '0').
         */
        private char matchChar;

        /**
         * String containing the character symbols.
         */
        // this is not clear is it??
        private String symbols;


        /**
         * the Constructor
         */
        public Format() {
            datatype = Datatypes.UNKNOWN;
            datatypeID = 0;
            respectCase = false;
            gap = '-';
            missing = '?';
            labels = true;
            labelQuotes = true;
            transpose = false;
            interleave = false;
            diploid = false;
            tokens = false;
            symbols = Datatypes.UNKNOWN;
            matchChar = 0;
        }

        /**
         * Clone the format object
         *
         * @return Format clone of object.
         */
        public Object clone() {
            Format result = new Format();

            result.datatype = datatype;
            result.datatypeID = datatypeID;
            result.respectCase = respectCase;
            result.gap = gap;
            result.missing = missing;
            result.labels = labels;
            result.labelQuotes = labelQuotes;
            result.transpose = transpose;
            result.interleave = interleave;
            result.diploid = diploid;
            result.tokens = tokens;
            result.symbols = symbols;
            result.matchChar = matchChar;

            return result;
        }


        /**
         * Get the datatype.
         *
         * @return datatype
         */
        public String getDatatype() {
            return this.datatype;
        }

        public int getDatatypeID() {
            return this.datatypeID;
        }


        /**
         * Set the datatype and reset all symbols.
         *
         * @param str the datatype
         */
        public void setDatatype(String str) {
            this.datatype = str.trim().toLowerCase();
            if (this.datatype.equals("nucleotide")) {
                this.datatype = "dna";
                new Alert("Unsupported DATATYPE=NUCLEOTIDE specified, assuming DATATYPE=DNA");
            }

            switch (this.datatype) {
                case Datatypes.STANDARD:
                    this.datatypeID = Datatypes.STANDARDID;
                    break;
                case Datatypes.DNA:
                    this.datatypeID = Datatypes.DNAID;
                    break;
                case Datatypes.RNA:
                    this.datatypeID = Datatypes.RNAID;
                    break;
                case Datatypes.PROTEIN:
                    this.datatypeID = Datatypes.PROTEINID;
                    break;
                case Datatypes.MICROSAT:
                    this.datatypeID = Datatypes.MICROSATID;
                    break;
                default:
                    this.datatypeID = Datatypes.UNKNOWNID;
                    break;
            }
            this.resetSymbols();
        }

        /**
         * Resets the set of all symbols, depending on datatype.
         */
        private void resetSymbols() {
            switch (getDatatypeID()) {
                case Datatypes.STANDARDID:
                    this.symbols = Datatypes.STANDARDSYMBOLS;
                    break;
                case Datatypes.DNAID:
                    this.symbols = Datatypes.DNASYMBOLS;
                    break;
                case Datatypes.RNAID:
                    this.symbols = Datatypes.RNASYMBOLS;
                    break;
                case Datatypes.PROTEINID:
                    this.symbols = Datatypes.PROTEINSYMBOLS;
                    break;
                default:
                    this.symbols = Datatypes.UNKNOWNSYMBOLS;
                    break;
            }
            computeColors();
        }


        /**
         * Set the symbols.
         * If the datatype=standard or UNKNOWN or microsat, then replace symbols by the given ones.
         * For other datatypes, the command is ignored.
         *
         * @param sym the symbols
         */
        //ToDo: This needs to be made safe, since the symbol string may not be valid for the alignment.
        public void setSymbols(String sym) {
            if (getDatatypeID() == Datatypes.STANDARDID || isUnknownType()) {
                String str = "";
                for (int i = 0; i < sym.length(); i++) {
                    char ch = Character.toLowerCase(sym.charAt(i));
                    if (!Character.isSpaceChar(ch) && str.indexOf(ch) == -1)
                        str += ch;
                }
                this.symbols = str;
            } else if (getDatatypeID() == Datatypes.MICROSATID)
                this.symbols = sym;
        }

        /**
         * Get the symbols set using the symbols statement.
         *
         * @return the symbols
         */
        public String getSymbols() {
            return this.symbols;
        }

        /**
         * Get the value of labels
         *
         * @return the value of labels
         */
        public boolean getLabels() {
            return this.labels;
        }


        /**
         * Set the value of labels.
         *
         * @param labels the value of labels
         * @throws SplitsException Don't know why this is here. //TODO: remove this?
         */
        public void setLabels(boolean labels) {
            this.labels = labels;
        }

        /**
         * Get flag of whether taxon labels printed with quotes
         *
         * @return boolean
         */
        public boolean isLabelQuotes() {
            return labelQuotes;
        }

        /**
         * Set flag of whether labels are printed with quotes
         *
         * @param labelQuotes true if labels always printed in single quotes
         */
        public void setLabelQuotes(boolean labelQuotes) {
            this.labelQuotes = labelQuotes;
        }


        /**
         * Get the value of respectcase
         *
         * @return the value of respectcase
         */

        public boolean getRespectCase() {
            return this.respectCase;
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
         * Get the value of matchchar
         *
         * @return the value of matchar
         */
        public char getMatchchar() {
            return this.matchChar;
        }

        /**
         * Set the value of the matchchar
         *
         * @param matchChar char used to indicate a match (usually '.')
         * @throws SplitsException if the character is punctuation or a space.
         */
        public void setMatchchar(char matchChar) throws SplitsException {
            if (NexusStreamTokenizer.isLabelPunctuation(matchChar)
                    || NexusStreamTokenizer.isSpace(matchChar))
                throw new SplitsException("illegal match-character: " + matchChar);
            this.matchChar = matchChar;
        }

        /**
         * Get the value of gap
         *
         * @return the value of gap
         */
        public char getGap() {
            return this.gap;
        }

        /**
         * Set the value of gap
         *
         * @param gap char character used to indicate a gap.
         * @throws SplitsException if the character is punctuation or a space.
         */
        public void setGap(char gap) throws SplitsException {
            if (NexusStreamTokenizer.isLabelPunctuation(gap)
                    || NexusStreamTokenizer.isSpace(gap))
                throw new SplitsException("illegal gap-character:" + gap);
            this.gap = gap;

        }

        /**
         * Get the value of the missing character
         *
         * @return the value of the missing character
         */
        public char getMissing() {
            return this.missing;
        }

        /**
         * Set the value of missing
         *
         * @param missing sets character used to indicate a missing state
         * @throws SplitsException if character is illegal
         */
        public void setMissing(char missing) throws SplitsException {
            if (NexusStreamTokenizer.isLabelPunctuation(missing)
                    || NexusStreamTokenizer.isSpace(missing))
                throw new SplitsException("illegal missing-character:" + missing);
            this.missing = missing;

        }

        /**
         * Get the value of transpose
         *
         * @return the value of transpose
         */
        public boolean getTranspose() {
            return this.transpose;
        }

        /**
         * Set the value of transpose
         *
         * @param transpose set flag, with true indicating that matrix is transposed with columns = taxa.
         */
        public void setTranspose(boolean transpose) {
            this.transpose = transpose;
        }

        /**
         * Get the value of interleave
         *
         * @return the value of interleave
         */
        public boolean getInterleave() {
            return interleave;
        }

        /**
         * Set the value of interleave
         *
         * @param interleave boolean flag. Set to true if matrix is interleaved
         */
        public void setInterleave(boolean interleave) {
            this.interleave = interleave;
        }

        /**
         * Get value of tokens
         *
         * @return value of tokens
         */
        public boolean getTokens() {
            return this.tokens;
        }

        /**
         * Set the value of tokens
         *
         * @param tokens boolean. Set to true if states in matrix identified by tokens (not properly supported yet)
         */
        public void setTokens(boolean tokens) {
            this.tokens = tokens;
        }

        /**
         * Set the flag indicating whether this is diploid or not
         *
         * @return vale of the flag diploid
         */
        public boolean isDiploid() {
            return diploid;
        }

        /**
         * Set the value of the flag diploid, indicating whether data is diploid or not
         *
         * @param diploid true if alternate sites from different strands
         */
        public void setDiploid(boolean diploid) {
            this.diploid = diploid;
        }

        /**
         * isUnknownType
         *
         * @return true if the datatype is unknown *
         */
        public boolean isUnknownType() {
            return (datatypeID == Datatypes.UNKNOWNID);
        }

        /**
         * isNucleotideType
         *
         * @return true if the datatype is DNA or RNA *
         */
        public boolean isNucleotideType() {
            return (datatypeID == Datatypes.DNAID || datatypeID == Datatypes.RNAID);
        }


    }

    /**
     * the properties subclass
     */
    public class Properties implements Cloneable {
        private boolean hasGamma;
        private boolean hasPinvar;
        private double gammaParam = -1;
        private double pInvar = -1;


        /* Options for the program only */

        public boolean hasGamma() {
            return hasGamma;
        }

        public boolean hasPinvar() {
            return hasPinvar;
        }

        /* Options that can be read in and are written */

        public double getGammaParam() {
            return gammaParam;
        }

        public void setGammaParam(double val) {
            if (val > 0.0) {
                hasGamma = true;
                gammaParam = val;
            }
        }

        public double getpInvar() {
            return pInvar;
        }

        public void setpInvar(double pInvar) {
            if (pInvar >= 0.0 && pInvar <= 1.0) {
                hasPinvar = true;
                this.pInvar = pInvar;
            }
        }


        /**
         * Constructor
         */
        public Properties() {
            hasGamma = hasPinvar = false;
        }
    }

    // Main data:

    /**
     * Format Object holding the Format of the alignment.
     */
    private Format fmt = null;

    /**
     * Properties Object holding basic properties of the characters.
     */
    private Properties properties = null;

    /**
     * int value with the number of taxa.
     */
    private int ntax;
    /**
     * int value with the length of the alignment.
     */
    private int nchar;
    /**
     * int value with the number of active sides
     */
    private int nactive;
    /**
     * char matrix wich holds the alignment.
     */
    private char[][] matrix;
    /**
     * boolean array used to determine if the position in the alignment is masked ( if set true the position will be ignored for the claculations.
     */
    private boolean[] mask;

    /**
     * array of doubles giving the weights for each site. Set to null means all weights are 1.0
     */
    private double[] charWeights;


    /**
     * Set containing all the states that were encountered but unknown.
     */
    private BitSet unknownStates;


    /**
     * Flag indicating whether or not there are ambiguous character states here.
     */
    private boolean hasAmbigStates;

    /**
     * Stores a string of symbols for each position in the matrix with ambiguities, giving
     * the possible symbols
     */
    private SparseArray ambigStates;

    /**
     * Currently, we replace ambiguous states with a missing character - this stores the original
     * state that was replaced.
     */
    private SparseArray replacedStates;


    /**
     * Identification string
     */
    public final static String NAME = "Characters";

    /**
     * HashMap containing the Character Labels.
     */
    private Hashtable<Integer, String> charLabeler;

    /**
     * Storage of the state labels (tokens) used at each site
     */
    private StateLabeler stateLabeler;

    /**
     * Flag indicating whether CharStateLabels were read in
     */
    private boolean haveReadCharStateLabels;

    /**
     * Flag indicating that the data matrix will be in token form
     */
    private boolean matrixIsTokens;

    /**
     * boolean used to determine if there are gaps or missing positions.
     */
    public boolean gapMissingMode;


    /**
     * boolean used to determine if states should be checked as read in
     */
    private boolean checkStates;

    private static final boolean treatUnknownAsError = false;


    /**
     * Number of colors used.
     */
    public int ncolors = 0;
    /**
     * Map mapping every symbol in the matrix to an integer value (ignoring the case). This map is fixed for known datatypes.
     */
    public Map<String, String> symbols2colors;
    /**
     * Map mapping every color to an Array of symbols. This map is fixed for known datatypes.
     */
    public Map<String, ArrayList<String>> colors2symbols;


    /**
     * Construct a new Characters object.
     */
    public Characters() {
        super();
        ntax = 0;
        nchar = 0;
        nactive = -1;
        gapMissingMode = false;
        charLabeler = null;
        charWeights = null;
        stateLabeler = null;
        matrixIsTokens = false;
        haveReadCharStateLabels = false;
        matrix = null;
        mask = null;
        checkStates = true;
        fmt = new Format();
        properties = new Properties();
    }


    /**
     * Constructs a new Characters Object with a given number of taxa and characters and a given format.
     * Initialises the matrix.
     *
     * @param ntax  int NUmber of taxa
     * @param nchar int Number of characters (sites)
     * @throws SplitsException if the ntax or nchars parameters are not valid.
     */
    public Characters(int ntax, int nchar) {
        this();
        this.ntax = ntax;
        this.nchar = nchar;
        matrix = new char[ntax + 1][nchar + 1];
    }


    /**
     * Constructs a new Characters Object with a given number of taxa and characters and a given format.
     * Initialises the matrix.
     *
     * @param ntax   int NUmber of taxa
     * @param nchar  int Number of characters (sites)
     * @param format Format block
     * @throws SplitsException if the ntax or nchars parameters are not valid.
     */
    public Characters(int ntax, int nchar, Format format) {
        this(ntax, nchar);
        fmt = (Format) format.clone();
    }


    /**
     * Sets the format object
     *
     * @param format the new format object
     */
    protected void setFormat(Format format) {
        fmt = format;
    }

    /**
     * Return the format object
     *
     * @return the format object
     */
    public Format getFormat() {
        return this.fmt;
    }

    /**
     * returns the properties class
     *
     * @return properties subclass
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * sets the properties subclass
     *
     * @param properties the new properties subclass to be used
     */
    protected void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Get the number of taxa.
     *
     * @return the number taxa
     */
    public int getNtax() {
        return this.ntax;
    }

    /**
     * sets the number of taxa in this object to the given value
     *
     * @param ntax the new number of taxa in the object
     */
    protected void setNtax(int ntax) {
        this.ntax = ntax;
    }

    /**
     * Get the number of characters.
     *
     * @return the number of characters
     */
    public int getNchar() {
        return this.nchar;
    }

    /**
     * Set the number of characters.  This resets the character weights array.
     *
     * @param i the number of characters
     */
    protected void setNchar(int i) {
        this.nchar = i;
        if (charWeights != null)
            charWeights = null;
    }

    /************************Character weights and state labels******************************/


    /**
     * hasCharweights
     * <p/>
     * Determines whether character weights are stored for this block
     *
     * @return boolean true if character weights are being stored for this block (they could still be
     *         all constant)
     */
    public boolean hasCharweights() {
        return this.charWeights != null;
    }


    /**
     * Get the characterweight of a specific character.
     *
     * @param c the index of that character
     * @return weight for that character
     */
    public double getCharWeight(int c) {
        if (charWeights == null)
            return 1.0;
        else
            return charWeights[c];
    }


    /**
     * Set the characterweight of a specific character.
     *
     * @param c the index of that character
     * @param x the weight
     */
    public void setCharWeight(int c, double x) {
        if (charWeights == null) {
            this.charWeights = new double[nchar];
            Arrays.fill(charWeights, 1.0);
        }
        this.charWeights[c] = x;
    }

    /************************ Colouring  and states ******************************/


    /**
     * Gets the name of a character, as specified in the CharStateLabels
     *
     * @param c number of character
     * @return String name of character, of null if there is non specified.
     */
    public String getCharLabel(int c) {
        if (charLabeler == null)
            return null;
        return charLabeler.get(c);
    }

    /************************ Colouring  and states ******************************/

    /**
     * Checks if the character is a valid state symbol. Will always return
     * true if the datatype is UNKNOWN.
     *
     * @param ch character to check
     * @return boolean  true if character consistent with the symbol list of the block's datatype
     */
    private boolean isValidState(char ch) {
        if (getFormat().isUnknownType())
            return true;
        if (ch == getFormat().missing || ch == getFormat().gap || ch == getFormat().matchChar)
            return true;
        if (getFormat().symbols.indexOf(ch) >= 0)
            return true;
        return getFormat().isNucleotideType() && Datatypes.AMBIGDNA.indexOf(ch) >= 0;
    }

    /**
     * Returns the color of a character. Colors start at 1
     *
     * @param ch a character
     * @return the color of the character or -1 if the character is not found.
     */
    public int getColor(char ch) {
        ch = Character.toLowerCase(ch);
        if (symbols2colors.get(String.valueOf(ch)) != null)
            return Integer.parseInt(this.symbols2colors.get(String.valueOf(ch)));
        else
            return -1;
    }


    /**
     * Returns a Array with the symbols of the color
     *
     * @param color a color
     * @return an Array with the List of Symbols matching the color
     */
    public Object[] getSymbols(int color) {
        return this.colors2symbols.get(Integer.toString(color)).toArray();
    }


    /**
     * Gets the number of colors.
     *
     * @return the number of colors
     */
    public int getNcolors() {
        return this.ncolors;
    }

    /**
     * Is this nucleotide data.
     *
     * @return boolean. True if datatype is RNA or DNA
     */
    public boolean isNucleotides() {
        String datatype = getFormat().getDatatype();
        return datatype.equalsIgnoreCase(Datatypes.DNA) || datatype.equalsIgnoreCase(Datatypes.RNA);
    }


    /**
     * Computes the colors2symbols and symbols2colors maps
     * changed(17.08.2003) the following:  Since symbols are case sensetive we want to change this and assign colors to
     * to symbols wich assigns one color to the same symbols ignoring the case.
     * This maps are fixed for known datatypes.
     */
    public void computeColors() {
        this.symbols2colors = new HashMap<>();
        this.colors2symbols = new HashMap<>();
        int count = 1;
        String symbol;
        if (getFormat().symbols != null) {

            for (byte p = 0; p < getFormat().symbols.length(); p++) {
                symbol = String.valueOf(getFormat().getSymbols().charAt(p)).toLowerCase();
                if (!this.symbols2colors.containsKey(symbol)) {
                    this.symbols2colors.put(symbol, Integer.toString(count));
                    this.symbols2colors.put(symbol.toUpperCase(), Integer.toString(count));
                    ArrayList<String> temp = new ArrayList<>();
                    temp.add(symbol);
                    temp.add(symbol.toUpperCase());
                    this.colors2symbols.put(Integer.toString(count++), temp);
                }
            }

        }
        this.ncolors = this.colors2symbols.size();
    }


    public boolean hasAmbigStates() {
        return this.hasAmbigStates;
    }

    /**************************Access to elements in the alignment ****************/


    /**
     * Get the matrix value.
     *
     * @param seq  the taxon
     * @param site the position
     * @return the matrix value  matrix[t][p]
     */
    public char get(int seq, int site) {
        return this.matrix[seq][site];
    }


    /**
     * Set the matrix value.
     *
     * @param seq  the row
     * @param site the colum
     * @param val  the matix value at row seq and colum site
     */
    public void set(int seq, int site, char val) {
        this.matrix[seq][site] = val;
    }


    /**
     * Ambiguous states are replaced by the missing character, but stored in replacedStates.
     * This routine returns the state that was in this sequence at this position in the original
     * file.
     *
     * @param seq  sequence number
     * @param site the site (character)
     * @return char.  missing character returned.
     */
    public char getOriginal(int seq, int site) {
        char ch = get(seq, site);
        if (!hasAmbigStates() || ch != getFormat().getMissing() || !replacedStates.hasEntry(seq, site))
            return ch;
        else
            return (replacedStates.getString(seq, site).charAt(0));
    }


    /**
     * Get a copy of row seq of matrix (seq.e. the sequence for tax seq). NOTE: This is different than in previous versions.
     *
     * @param seq the row
     * @return the matix row seq
     */
    public char[] getRow(int seq) {
        char[] row = new char[matrix[seq].length];
        System.arraycopy(matrix[seq], 0, row, 0, matrix[seq].length);
        return row;
    }

    /**
     * gets a copy of the named column of the alignment
     *
     * @param pos
     * @return column of characters block
     */
    public String getColumn(int pos) {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= ntax; i++)
            buf.append(matrix[i][pos]);
        return buf.toString();
    }


    /**
     * returns a string consisting of all states for this row that are listed in toShow
     *
     * @param seq    The number of the sequence
     * @param toShow List (of Integer) specifying sites to show
     * @return boolean
     */
    public String getRowSubset(int seq, List toShow) {
        StringBuilder buf = new StringBuilder();
        for (Object aToShow : toShow) {
            int c = (Integer) aToShow;
            if (c >= 1 && c <= getNchar())
                buf.append(matrix[seq][c]);
        }
        return buf.toString();
    }

    /**
     * NOT USED AND REDUNDANT... COMMENTED OUT
     * returns the character at position c for taxon t
     *
     * @param t
     * @param p
     * @return character at position p for taxon t
     *
     *   public char getRowSubset(int t, int p) {
     *   return getRow(t)[p];
     *   }
     */

    /*****************************Ambiguity strings*******************************************************/


    /**
     * getAmbigString
     * <p/>
     * In the matrix, any ambiguity characters are coded as a '?' and the set of states they represent
     * is stored as a string. This method gets the string of characters associated to a position in a given
     * sequence, or null if there are none.
     * //TODo: Maybe should return original character?
     *
     * @param seq sequence
     * @param c   character (site)
     * @return string containing original states, or null if there are no ambiguity characters stored.
     */
    public String getAmbigString(int seq, int c) {
        return ambigStates.getString(seq, c);
    }

    /**
     * hasAmbigString
     * <p/>
     * In the matrix, any ambiguity characters are coded as a '?' and the set of states they represent
     * is stored as a string. To check wether there is ambiguity information stored for a particular
     * sequence and site, use 'hasAmbigString'.
     *
     * @param seq sequence
     * @param c   character (site)
     * @return boolean  true if there is an ambiguity string associated with this character
     */
    public boolean hasAmbigString(int seq, int c) {
        return ambigStates.hasEntry(seq, c);
    }
    /*****************************Masks*******************************************************/


    /**
     * Get the number of active, ie unmasked, characters.
     *
     * @return the number of active characters
     */
    public int getNactive() {
        return this.nactive;
    }

    /**
     * Returns true, if specified site is masked
     *
     * @param p the index of that site
     * @return true, if site is masked
     */
    public boolean isMasked(int p) {
        return this.mask != null && mask[p];
    }


    /**
     * Sets the masked status of a site
     *
     * @param p      the index of the site
     * @param masked the status
     */
    public void setMasked(int p, boolean masked) {
        if (this.mask == null && masked) // lazy allocation
        {
            this.mask = new boolean[this.nchar + 1];
            for (int i = 1; i <= this.nchar; i++)
                this.mask[i] = false;
            this.nactive = this.nchar;
        }
        if (masked && !this.mask[p]) {
            this.nactive--;
            this.mask[p] = true;
        } else if (masked && this.mask != null && this.mask[p]) {
            this.nactive++;
            if (this.mask != null)
                this.mask[p] = false;
        }
    }

    /**
     * Gets the mask array
     *
     * @return the mask
     */
    public boolean[] getMask() {
        return this.mask;
    }

    /**
     * Clears the mask
     */
    public void clearMask() {
        mask = null;
        this.nactive = -1;
    }

    /**
     * **********************************INPUT OUTPUT******************************************************
     */


    /**
     * Show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN CHARACTERS;");
        ps.println("\tDIMENSIONS [NTAX=number-of-taxa] NCHAR=number-of-characters;");
        ps.println("\t[PROPERTIES [GAMMASHAPE=shape-parameter] [PINVAR=proportion-invar];]");
        ps.println("\t[FORMAT");
        ps.println("\t    [DATATYPE={STANDARD|DNA|RNA|PROTEIN|MICROSAT}]");
        ps.println("\t    [RESPECTCASE]");
        ps.println("\t    [MISSING=symbol]");
        ps.println("\t    [GAP=symbol]");
        ps.println("\t    [SYMBOLS=\"symbol symbol ...\"]");
        ps.println("\t    [LABELS={NO|LEFT}]");
        ps.println("\t    [TRANSPOSE={NO|YES}]");
        ps.println("\t    [INTERLEAVE={NO|YES}]");
        ps.println("\t    [TOKENS=NO]");
        ps.println("\t;]");
        ps.println("\t[CHARWEIGHTS wgt_1 wgt_2 ... wgt_nchar;]");
        ps.println("\t[CHARSTATELABELS character-number [ character-name ][ /state-name [ state-name... ] ], ...;]");
        ps.println("\tMATRIX");
        ps.println("\t    sequence data in specified format");
        ps.println("\t;");
        ps.println("END;");
    }

    /**
     * Formats a label. Adds single quotes if addQuotes set to true.
     * Appends spaces until the length of the resulting string is at least length.
     *
     * @param label     String
     * @param addQuotes flag: oif true then label is returned surrounded by single quotes
     * @param length    add spaces to acheive this length
     * @return String of given length, or longer if the label + quotes exceed the length.
     */
    protected String padLabel(String label, boolean addQuotes, int length) {
        if (addQuotes)
            label = "'" + label + "'";
        if (label.length() >= length)
            return label;
        char[] padding = new char[length - label.length()];
        Arrays.fill(padding, ' ');
        String paddingString = new String(padding);
        return label + paddingString;
    }

    protected char flip(char ch) {
        return (char) (Character.MIN_VALUE - ch);
    }


    /**
     * read the characters block
     *
     * @param np   NexusStreamParser
     * @param taxa taxa block
     * @throws SplitsException when there are syntax errors
     * @throws IOException     when there are file IO errors
     */
    public void read(NexusStreamParser np, Taxa taxa) throws SplitsException, IOException {
        try {
            read(np, taxa, null);
        } catch (CanceledException e) {
            // can't happen
        }
    }


    /**
     * Read the characters block
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa block
     * @param doc  needed for progress bar   //ToDO: Replace with progress bar reference
     * @throws SplitsException   if there are syntax errors
     * @throws IOException       if there are file errors
     * @throws CanceledException if user presses cancel during the read
     */
    public void read(NexusStreamParser np, Taxa taxa, Document doc)
            throws SplitsException, IOException, CanceledException {
        unknownStates = new BitSet();
        hasAmbigStates = false;


        np.matchBeginBlock(NAME);

        if (matrix == null) // haven's got a matrix yet, need dimensions
        {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax="))
                np.matchIgnoreCase("ntax=" + taxa.getNtax());
            ntax = taxa.getNtax();
            np.matchIgnoreCase("nchar=");
            nchar = np.getInt();
            setNchar(nchar);
            np.matchIgnoreCase(";");
        }

        if (doc != null)
            doc.notifyProgress(np);


        if (np.peekMatchIgnoreCase("PROPERTIES")) {
            List<String> tokens = np.getTokensLowerCase("properties", ";");


            if (getProperties().hasGamma()) {
                getProperties().setGammaParam((double) np.findIgnoreCase(tokens,
                        "gammaShape=", (float) getProperties().getGammaParam()));
            } else {
                getProperties().setGammaParam((double) np.findIgnoreCase(tokens,
                        "gammaShape=", -1));     //Default is not to change Gamma setting.
            }

            if (getProperties().hasPinvar()) {
                getProperties().setpInvar((double) np.findIgnoreCase(tokens,
                        "pInvar=", (float) getProperties().getpInvar()));
            } else {
                getProperties().setpInvar((double) np.findIgnoreCase(tokens,
                        "pInvar=", -1));     //Default is not to change  setting.
            }
        }

        if (np.peekMatchIgnoreCase("FORMAT")) {
            List<String> tokens = np.getTokensLowerCase("format", ";");

            //ToDo: Use DATATYPE static strings
            String datatype = np.findIgnoreCase(tokens,
                    "datatype=", "STANDARD DNA RNA PROTEIN NUCLEOTIDE MICROSAT DNAEXTENDED UNKNOWN", getFormat().datatype);

            getFormat().setDatatype(datatype);


            getFormat().respectCase = np.findIgnoreCase(tokens,
                    "respectcase=yes", true, getFormat().respectCase);
            getFormat().respectCase = np.findIgnoreCase(tokens,
                    "respectcase=no", false, getFormat().respectCase);
            getFormat().respectCase = np.findIgnoreCase(tokens,
                    "respectcase", true, getFormat().respectCase);
            getFormat().respectCase = np.findIgnoreCase(tokens,
                    "no respectcase", false, getFormat().respectCase);

            if (getFormat().respectCase)
                System.err.println("WARNING: Format 'RespectCase' not implemented."
                        + " All character-states will be converted to lower case");

            // getFormat().missing = np.findIgnoreCase(tokens, "missing", null, getFormat().missing);
            // @todo: until we know that respectcase works, fold all characters to lower-case
            char missing = Character.toLowerCase(np.findIgnoreCase(tokens, "missing=", null, getFormat().missing));
            getFormat().setMissing(missing);

            {
                boolean nomatchchar = np.findIgnoreCase(tokens, "no matchChar", true, false);
                if (nomatchchar)
                    getFormat().matchChar = 0;
            }
            getFormat().matchChar = np.findIgnoreCase(tokens, "matchChar=", null, getFormat().matchChar);
            getFormat().gap = np.findIgnoreCase(tokens, "gap=", null, getFormat().gap);

            {
                String symbols = np.findIgnoreCase(tokens, "symbols=", "\"", "\"", this.getFormat().symbols);
                getFormat().setSymbols(symbols);
            }

            getFormat().labels = np.findIgnoreCase(tokens, "labels=no", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels=left", true, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "no labels", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels", true, getFormat().labels);

            if (taxa.getMustDetectLabels() && !getFormat().getLabels())
                throw new IOException("line " + np.lineno() +
                        ": 'no labels' invalid because no taxlabels given in TAXA block");

            getFormat().transpose = np.findIgnoreCase(tokens, "transpose=no", false, getFormat().transpose);
            getFormat().transpose = np.findIgnoreCase(tokens, "transpose=yes", true, getFormat().transpose);
            getFormat().transpose = np.findIgnoreCase(tokens, "no transpose", false, getFormat().transpose);
            getFormat().transpose = np.findIgnoreCase(tokens, "transpose", true, getFormat().transpose);

            getFormat().interleave = np.findIgnoreCase(tokens, "interleave=no", false, getFormat().interleave);
            getFormat().interleave = np.findIgnoreCase(tokens, "interleave=yes", true, getFormat().interleave);
            getFormat().interleave = np.findIgnoreCase(tokens, "no interleave", false, getFormat().interleave);
            getFormat().interleave = np.findIgnoreCase(tokens, "interleave", true, getFormat().interleave);

            getFormat().tokens = np.findIgnoreCase(tokens, "tokens=no", false, getFormat().tokens);
            getFormat().tokens = np.findIgnoreCase(tokens, "tokens=yes", true, getFormat().tokens);
            getFormat().tokens = np.findIgnoreCase(tokens, "no tokens", false, getFormat().tokens);
            getFormat().tokens = np.findIgnoreCase(tokens, "tokens", true, getFormat().tokens);

            getFormat().diploid = np.findIgnoreCase(tokens, "diploid=no", false, getFormat().diploid);
            getFormat().diploid = np.findIgnoreCase(tokens, "diploid=yes", true, getFormat().diploid);
            getFormat().diploid = np.findIgnoreCase(tokens, "diploid", true, getFormat().diploid);


            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in FORMAT");


        }
        if (doc != null)
            doc.notifyProgress(np);

        //If we are using one of the standard types, check the states and post a warning if they're no valid.
        int datatype = getFormat().getDatatypeID();
        checkStates = datatype == Datatypes.DNAID || datatype == Datatypes.PROTEINID || datatype == Datatypes.RNAID;


        if (matrix != null) // already have a matrix, can't change the data!
        {
            np.matchIgnoreCase("end;");
            return;
        }
        if (np.peekMatchIgnoreCase("CHARWEIGHTS")) {
            np.matchIgnoreCase("CHARWEIGHTS");
            charWeights = new double[getNchar() + 1];
            for (int i = 1; i <= getNchar(); i++)
                charWeights[i] = np.getDouble();
            np.matchIgnoreCase(";");
        }
        // adding CharStateLabels

        if (np.peekMatchIgnoreCase("CHARSTATELABELS")) {
            np.matchIgnoreCase("CHARSTATELABELS");
            //setHasCharStateLabels(true);     redundant.
            haveReadCharStateLabels = true;
            charLabeler = new Hashtable<>();
            stateLabeler = new StateLabeler(this);
            readCharStateLabels(np, charLabeler, stateLabeler);
            np.matchIgnoreCase(";");
        }

        //Check if matrix will be tokens
        if (getFormat().getTokens() || getFormat().datatypeID == Datatypes.MICROSATID) {
            matrixIsTokens = true;
            if (stateLabeler == null)
                stateLabeler = new StateLabeler(this);
        }

        if (np.peekMatchIgnoreCase("MATRIX")) {
            np.matchIgnoreCase("MATRIX");
            if (!getFormat().transpose && !getFormat().interleave) {
                readMatrix(np, taxa, doc);
            } else if (getFormat().transpose
                    && !getFormat().interleave) {
                readMatrixTransposed(np, taxa, doc);
            } else if (!getFormat().transpose
                    && getFormat().interleave) {
                readMatrixInterleaved(np, taxa, doc);
            } else
                throw new IOException("line " + np.lineno() + ": can't read matrix!");
            np.matchIgnoreCase(";");
        }

        np.matchEndBlock();

        //If there are tokens, we set the symbols list to the set of chars used.
        if ((getFormat().getTokens() || haveReadCharStateLabels) && stateLabeler != null && stateLabeler.getSymbolsUsed() != null) {
            getFormat().setSymbols(stateLabeler.getSymbolsUsed());
        }

        // if datatype is unknown try to predict the datatype
        if (getFormat().isUnknownType()) {
            int t = 1;
            while (getFormat().getDatatype().equals(Datatypes.UNKNOWN) && t <= getNtax())
                getFormat().setDatatype(CharactersUtilities.guessType("" + getFormat().getMatchchar() + getFormat().getMissing()
                        + getFormat().getGap(), this.getRow(t++)));

            // if the Datatype is not standard we use the fixed symbols
            if (getFormat().getDatatypeID() != Datatypes.STANDARDID)
                getFormat().resetSymbols();

            computeColors();
        }

        // if the data type is DNA or RNA, remove all ambiguity characters
        if (getFormat().isNucleotideType())
            replaceAmbiguityStates(taxa);

        if (taxa.getMustDetectLabels())
            taxa.setMustDetectLabels(false);

        if (unknownStates.cardinality() > 0)  // warn that stuff has been replaced!
        {
            StringBuilder buf = new StringBuilder();
            for (int ch = unknownStates.nextSetBit(0); ch > 0; ch = unknownStates.nextSetBit(ch + 1))
                buf.append(" ").append((char) ch);
            new Alert("Unknown states encountered in matrix:\n" + buf.toString() + "\n"
                    + "All replaced by the gap-char '" + getFormat().getGap() + "'");
        }


    }//End of read

    /**
     * replace all abiguity states by the missing char in the matrix. The corresponding state is stored
     * in replacedStates, while the corresponding string of symbols is stored in ambigStates.
     * <p/>
     * At present, this applies only to DNA or RNA data
     *
     * @param taxa Taxablock, used to report errors
     */
    private void replaceAmbiguityStates(Taxa taxa) {

        hasAmbigStates = false;
        if (!getFormat().isNucleotideType())
            return;


        replacedStates = new SparseArray();
        ambigStates = new SparseArray();

        for (int t = 1; t <= getNtax(); t++) {
            int ambigCount = 0;
            for (int c = 1; c <= getNchar(); c++) {
                char ch = get(t, c);
                int i = Datatypes.AMBIGDNA.indexOf(ch);
                if (i >= 0) {
                    hasAmbigStates = true;
                    ambigCount++;
                    replacedStates.setString(t, c, "" + ch);
                    ambigStates.setString(t, c, Datatypes.AMBIGDNACODES[i]);
                    set(t, c, getFormat().getMissing());
                }
            }
            if (ambigCount != 0)
                System.err.println("Taxa " + taxa.getLabel(t) + " has\t" + ambigCount + " ambiguous sites");

        }

    }

    /**
     * Undoes the replacement of the character states by ambiguous states.
     */
    protected void restoreAmbiguityStates() {
        if (!getFormat().isNucleotideType() || replacedStates == null)
            return;

        SparseArray.ArrayIterator iter = replacedStates.arrayIterator();
        while (iter.hasNext()) {
            iter.getNext();
            set(iter.i, iter.j, ((String) iter.o).charAt(0));
        }
    }

    /**
     * @param np the nexus parser
     * @throws IOException     when there are input format errors
     * @throws SplitsException if there are too many states at a site.
     */
    private void readCharStateLabels(NexusStreamParser np, Hashtable<Integer, String> charLabeler, StateLabeler stateLabeler) throws IOException, SplitsException {
        System.out.println("In readCharStateLabels");

        while (np.peekNextToken() != (int) ';') {
            int charNumber = np.getInt(); //get the number in front of the label

            // Deal with the fact that it is possible to not have a label for some nubmer.
            if (np.peekNextToken() == ',' || np.peekNextToken() == '/') {
            } else {
                String charLabel = np.getWordRespectCase();   //get the label otherwise
                charLabeler.put(charNumber, charLabel);
            }

            if (np.peekMatchIgnoreCase(",")) {
                np.nextToken(); //Skipping the ',' between labels
            } else if (np.peekMatchIgnoreCase("/")) {
                np.nextToken(); //Skipping the '/' between label and states
                while (np.peekNextToken() != (int) ',' && np.peekNextToken() != (int) ';') {
                    stateLabeler.token2char(charNumber, np.getWordRespectCase());
                }
                if (np.peekNextToken() == (int) ',')
                    np.nextToken(); //Skipping the ',' between labels
            }
        }
    }

    /**
     * Read a matrix in standard format
     *
     * @param np   the nexus parser
     * @param taxa the taxa
     * @param doc  document used for monitoring progress
     * @throws SplitsException   if there are syntax errors
     * @throws IOException       if there are file errors
     * @throws CanceledException if user presses cancel during the read
     */
    private void readMatrix(NexusStreamParser np, Taxa taxa, Document doc)
            throws IOException, SplitsException, CanceledException {
        matrix = new char[getNtax() + 1][getNchar() + 1];

        for (int t = 1; t <= getNtax(); t++) {

            if (taxa.getMustDetectLabels()) {
                taxa.setLabel(t, np.getLabelRespectCase());
            } else if (getFormat().labels)
                np.matchLabelRespectCase(taxa.getLabel(t));

            String str = "";
            int length = 0;

            List tokenList = null;
            if (matrixIsTokens)
                tokenList = new LinkedList();

            while (length < getNchar()) {
                String tmp = np.getWordRespectCase();
                if (matrixIsTokens) {
                    tokenList.add(tmp);
                    length++;
                } else {
                    length += tmp.length();
                    str += tmp;
                }
            }
            if (matrixIsTokens) {
                str = stateLabeler.parseSequence(tokenList, 1, false);
            }


            if (str.length() != getNchar())
                throw new IOException("line " + np.lineno() +
                        ": wrong number of chars: " + str.length());

            for (int i = 1; i <= str.length(); i++) {
                // @todo: until we know that respectcase works, fold all characters to lower-case
                //TODo clean this up.
                char ch;
                if (!matrixIsTokens)
                    ch = Character.toLowerCase(str.charAt(i - 1));
                else
                    ch = str.charAt(i - 1);

                if (ch == getFormat().getMatchchar()) {
                    if (t == 1)
                        throw new IOException("line " + np.lineno() +
                                " matchchar illegal in first sequence");
                    else
                        matrix[t][i] = matrix[1][i];
                } else {
                    if (!this.checkStates || isValidState(ch))
                        matrix[t][i] = ch;
                    else if (treatUnknownAsError)
                        throw new IOException("line " + np.lineno() +
                                " invalid character: " + ch);
                    else  // don't know this, replace by gap
                    {
                        matrix[t][i] = getFormat().getGap();
                        unknownStates.set(ch);
                    }
                }
            }

        }

        if (doc != null) doc.notifyProgress(np);
    }


    /**
     * Read a matrics in transpose format.
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa
     * @param doc  document used for monitoring progress
     * @throws SplitsException   if there are syntax errors
     * @throws IOException       if there are file errors
     * @throws CanceledException if user presses cancel during the read
     */
    private void readMatrixTransposed(NexusStreamParser np, Taxa taxa, Document doc)
            throws java.io.IOException, SplitsException, CanceledException {
        if (getFormat().getLabels()) {
            for (int t = 1; t <= getNtax(); t++) {
                if (taxa.getMustDetectLabels()) {
                    taxa.setLabel(t, np.getLabelRespectCase());
                } else {
                    np.matchLabelRespectCase(taxa.getLabel(t));
                }
            }
        }
        // read the matrix:
        matrix = new char[getNtax() + 1][getNchar() + 1];
        for (int i = 1; i <= getNchar(); i++) {
            String str = "";
            int length = 0;
            List tokenList = null;
            if (matrixIsTokens)
                tokenList = new LinkedList();

            while (length < getNtax()) {
                String tmp = np.getWordRespectCase();
                if (matrixIsTokens) {
                    tokenList.add(tmp);
                    length++;
                } else {
                    length += tmp.length();
                    str += tmp;
                }
            }

            if (matrixIsTokens) {
                str = stateLabeler.parseSequence(tokenList, i, true);
            }

            if (str.length() != getNtax())
                throw new IOException("line " + np.lineno() +
                        ": wrong number of chars: " + str.length());
            for (int t = 1; t <= getNtax(); t++) {
                //char ch = str.getRowSubset(t - 1);
                // @todo: until we now that respectcase works, fold all characters to lower-case
                char ch;
                if (!matrixIsTokens)
                    ch = Character.toLowerCase(str.charAt(t - 1));
                else
                    ch = str.charAt(t - 1);

                if (ch == getFormat().getMatchchar()) {
                    if (i == 1)
                        throw new IOException("line " + np.lineno() +
                                ": matchchar illegal in first line");
                    else
                        matrix[t][i] = matrix[t][1];
                } else {
                    if (!this.checkStates || isValidState(ch))
                        matrix[t][i] = ch;
                    else if (treatUnknownAsError)
                        throw new IOException("line " + np.lineno() +
                                " invalid character: " + ch);
                    else  // don't know this, replace by gap
                    {
                        matrix[t][i] = getFormat().getGap();
                        unknownStates.set(ch);
                    }
                }
            }
            if (i % 100 == 0) {
                if (doc != null) doc.notifyProgress(np);
            }
        }
    }

    /**
     * Read a matrics in interleave format.
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa
     * @param doc  document used for monitoring progress
     * @throws SplitsException   if there are syntax errors
     * @throws IOException       if there are file errors
     * @throws CanceledException if user presses cancel during the read
     */
    private void readMatrixInterleaved(NexusStreamParser np, Taxa taxa, Document doc)
            throws java.io.IOException, SplitsException, CanceledException {
        matrix = new char[getNtax() + 1][getNchar() + 1];
        try {
            int c = 0;
            while (c < getNchar()) {
                int linelength = 0;
                for (int t = 1; t <= getNtax(); t++) {
                    if (taxa.getMustDetectLabels()) {
                        taxa.setLabel(t, np.getLabelRespectCase());
                        np.setEolIsSignificant(true);
                    } else if (getFormat().getLabels()) {
                        np.matchLabelRespectCase(taxa.getLabel(t));
                        np.setEolIsSignificant(true);
                    } else {
                        np.setEolIsSignificant(true);       //WHAT IS THIS FOR???
                        if (t == 1 && np.nextToken() != StreamTokenizer.TT_EOL) //cosume eol
                            throw new IOException("line " + np.lineno()
                                    + ": EOL expected");
                    }

                    String str = "";
                    LinkedList tokenList;
                    if (matrixIsTokens) {
                        tokenList = new LinkedList();
                        while (np.peekNextToken() != StreamTokenizer.TT_EOL && np.peekNextToken() != StreamTokenizer.TT_EOF) {
                            tokenList.add(np.getWordRespectCase());
                        }
                        str = stateLabeler.parseSequence(tokenList, c + 1, false);
                    } else {
                        while (np.peekNextToken() != StreamTokenizer.TT_EOL && np.peekNextToken() != StreamTokenizer.TT_EOF) {
                            str += np.getWordRespectCase();
                        }
                    }
                    np.nextToken(); // consume the eol
                    np.setEolIsSignificant(false);
                    if (t == 1) { // first line in this block
                        linelength = str.length();
                    } else if (linelength != str.length())
                        throw new IOException("line " + np.lineno() +
                                ": wrong number of chars: " + str.length() + " should be: " + linelength);

                    for (int d = 1; d <= linelength; d++) {
                        int i = c + d;
                        if (i > getNchar())
                            throw new IOException("line " + np.lineno() + ": too many chars");

//char ch = str.getRowSubset(d - 1);
// @todo: until we now that respectcase works, fold all characters to lower-case
                        char ch;
                        if (!matrixIsTokens)
                            ch = Character.toLowerCase(str.charAt(d - 1));
                        else
                            ch = str.charAt(d - 1);


                        if (ch == getFormat().getMatchchar()) {
                            if (t == 1) {
                                throw new IOException("line " + np.lineno() +
                                        ": matchchar illegal in first sequence");
                            } else
                                matrix[t][i] = matrix[1][i];
                        } else {
                            if (!this.checkStates || isValidState(ch))
                                matrix[t][i] = ch;
                            else if (treatUnknownAsError)
                                throw new IOException("line " + np.lineno() +
                                        " invalid character: " + ch);
                            else  // don't know this, replace by gap
                            {
                                matrix[t][i] = getFormat().getGap();
                                unknownStates.set(ch);
                            }
                        }
                    }
                }
                if (doc != null) doc.notifyProgress(np);
                c += linelength;
            }
        } finally {
            np.setEolIsSignificant(false);
        }
    }


    /**
     * Write the characters block
     *
     * @param w    the writer
     * @param taxa the taxa
     */
    public void write(Writer w, Taxa taxa) throws java.io.IOException {
        w.write("\nBEGIN " + Characters.NAME + ";\n");
        w.write("DIMENSIONS nchar=" + getNchar() + ";\n");
        w.write("FORMAT\n");
        if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.STANDARD))
            w.write("\tdatatype=STANDARD");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.DNA))
            w.write(" datatype=DNA");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.RNA))
            w.write(" datatype=RNA");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.PROTEIN))
            w.write(" datatype=PROTEIN");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.MICROSAT))
            w.write(" datatype=MICROSAT");
        else
            w.write(" datatype='" + getFormat().getDatatype() + "'");

        if (getFormat().getRespectCase())
            w.write(" respectcase");

        if (getFormat().getMissing() != 0)
            w.write(" missing=" + getFormat().getMissing());
        if (getFormat().getMatchchar() != 0)
            w.write(" matchChar=" + getFormat().getMatchchar());
        if (getFormat().getGap() != 0)
            w.write(" gap=" + getFormat().getGap());
        if (getFormat().isDiploid())
            w.write(" diploid = yes");
        if (!getFormat().getSymbols().equals("") && !getFormat().getTokens()) {
            w.write(" symbols=\"");
            for (int i = 0; i < getFormat().getSymbols().length(); i++) {
                //if (i > 0)
                //w.write(" ");
                w.write(getFormat().getSymbols().charAt(i));
            }
            w.write("\"");
        }

        if (getFormat().getLabels())
            w.write(" labels=left");
        else
            w.write(" labels=no");

        if (getFormat().getTranspose())
            w.write(" transpose=yes");
        else
            w.write(" transpose=no");

        if (getFormat().getTokens())
            w.write(" tokens=yes");

        if (getFormat().getInterleave())
            w.write(" interleave=yes");
        else
            w.write(" interleave=no");

        w.write(";\n");
        if (charWeights != null) {
            w.write("CHARWEIGHTS");
            for (int i = 1; i <= getNchar(); i++)
                w.write(" " + charWeights[i]);
            w.write(";\n");
        }

        // Writes the CharStateLabels only if I read them in the input
        if (haveReadCharStateLabels) {

            w.write("CHARSTATELABELS\n");

            for (int i = 1; i <= getNchar(); i++) {
                if (charLabeler.containsKey(new Integer(i)) || stateLabeler.hasStates(i)) {
                    w.write("\t" + i + " ");
                    String label = charLabeler.get(i);
                    if (label != null)
                        w.write(label);
                    if (stateLabeler.hasStates(i)) {
                        w.write('/');
                        String[] stateArray = stateLabeler.getStates(i);
                        for (String aStateArray : stateArray) w.write(" ''" + aStateArray + "''");
                    }
                    w.write("\n");
                }
            }
            w.write(";\n");
        }

        w.write("MATRIX\n");
        if (matrix != null)
                if (getFormat().transpose && !getFormat().interleave)
                    writeMatrixTranposed(w, taxa);
                else if (!getFormat().transpose && getFormat().interleave)
                    writeMatrixInterleaved(w, taxa);
                else
                    writeMatrix(w, taxa);
        w.write(";\nEND; [" + Characters.NAME + "]\n");
    }

    /**
     * Write a matrics in standard format.
     *
     * @param w    the writer
     * @param taxa the taxa
     * @throws SplitsException if there are syntax errors
     * @throws IOException     if there are file errors
     */
    private void writeMatrix(Writer w, Taxa taxa) throws
            IOException {

        //Determine width of matrix columns (if appropriate) and taxa column (if appropriate)
        int columnWidth = 0;
        if (getFormat().getTokens())
            columnWidth = stateLabeler.getMaximumLabelLength() + 1;
        int taxaWidth = 0;
        if (getFormat().getLabels()) {
            taxaWidth = TaxaUtilities.getMaxLabelLength(taxa) + 1;
            if (getFormat().isLabelQuotes())
                taxaWidth += 2;
        }

        for (int t = 1; t <= ntax; t++) {
            //Print taxon label
            if (getFormat().getLabels()) {
                w.write(padLabel(taxa.getLabel(t), getFormat().isLabelQuotes(), taxaWidth));
            }

            if (!getFormat().getTokens()) { //Write sequence without tokens
                for (int c = 1; c <= getNchar(); c++) {
                    if (getFormat().getMatchchar() == 0 || t == 1 || matrix[1][c] != matrix[t][c])
                        w.write(getOriginal(t, c));
                    else
                        w.write(getFormat().getMatchchar());
                }
            } else {  //Write with tokens
                for (int c = 1; c <= getNchar(); c++) {
                    if (getFormat().getMatchchar() == 0 || c == 1 || matrix[t][1] != matrix[t][c])
                        w.write(padLabel(stateLabeler.char2token(c, getOriginal(t, c)), false, columnWidth));
                    else
                        w.write(padLabel("" + getFormat().getMatchchar(), false, columnWidth));
                }
            }
            w.write("\n");
        }
    }

    /**
     * Write a matrics in transpose format.
     *
     * @param w    the writer
     * @param taxa the taxa
     * @throws SplitsException if there are syntax errors
     * @throws IOException     if there are file errors
     */
    private void writeMatrixTranposed(Writer w, Taxa taxa)
            throws java.io.IOException {

        //Get the max width of a column, given taxa and token labels

        //Determine width of matrix columns (if appropriate) and taxa column (if appropriate)
        int columnWidth = 0;
        if (getFormat().getTokens())
            columnWidth = stateLabeler.getMaximumLabelLength() + 1;
        int taxaWidth = 0;
        if (getFormat().getLabels()) {
            taxaWidth = TaxaUtilities.getMaxLabelLength(taxa) + 1;
            if (getFormat().isLabelQuotes())
                taxaWidth += 2;
        }
        columnWidth = Math.max(taxaWidth, columnWidth); //Taxa printed above columns

        //Print taxa first
        if (getFormat().getLabels()) {
            for (int j = 1; j <= ntax; j++) {
                w.write(padLabel(taxa.getLabel(j), true, columnWidth));
            }
            w.write("\n");
        }

        if (!getFormat().getTokens()) {  //No tokens
            String padString = padLabel("", false, columnWidth - 1); //String of (columnWidth-1) spaces.
            for (int c = 1; c <= getNchar(); c++) {
                for (int t = 1; t <= getNtax(); t++) {
                    if (getFormat().getMatchchar() == 0 || c == 1 || matrix[t][1] != matrix[t][c])
                        w.write(getOriginal(t, c));
                    else
                        w.write(getFormat().getMatchchar());
                    w.write(padString);
                }
                w.write("\n");
            }
        } else {
            for (int c = 1; c <= getNchar(); c++) {
                for (int t = 1; t <= getNtax(); t++) {
                    if (getFormat().getMatchchar() == 0 || c == 1 || matrix[t][1] != matrix[t][c])
                        w.write(padLabel(stateLabeler.char2token(c, getOriginal(t, c)), false, columnWidth));
                    else
                        w.write(padLabel("" + getFormat().getMatchchar(), false, columnWidth));
                }
                w.write("\n");
            }
        }
    }

    /**
     * Write a matrics in interleave format.
     *
     * @param w    the writer
     * @param taxa the taxa
     * @throws SplitsException if there are syntax errors
     * @throws IOException     if there are file errors
     */
    private void writeMatrixInterleaved(Writer w, Taxa taxa)
            throws IOException {
        int c = 0;

        //Determine width of matrix columns (if appropriate) and taxa column (if appropriate)
        int columnWidth = 1;
        if (getFormat().getTokens())
            columnWidth = stateLabeler.getMaximumLabelLength() + 1;
        int taxaWidth = 0;
        if (getFormat().getLabels()) {
            taxaWidth = TaxaUtilities.getMaxLabelLength(taxa) + 1;
            if (getFormat().isLabelQuotes())
                taxaWidth += 2;
        }

        int maxColumns = 60 / columnWidth; //Maximum number of sites to print on one line.


        while (c < getNchar()) {
            for (int t = 1; t <= taxa.getNtax(); t++) {
                if (getFormat().getLabels()) {
                    w.write(padLabel(taxa.getLabel(t), getFormat().isLabelQuotes(), taxaWidth));
                }
                if (!getFormat().getTokens()) {
                    for (int d = 1; d <= maxColumns; d++) {
                        int i = c + d;
                        if (i > getNchar())
                            break;
                        if (getFormat().getMatchchar() == 0 || t == 1
                                || matrix[1][i] != matrix[t][i])
                            w.write(getOriginal(t, i));
                        else
                            w.write(getFormat().getMatchchar());
                    }

                } else {
                    for (int d = 1; d <= maxColumns; d++) {
                        int i = c + d;
                        if (i > getNchar())
                            break;
                        if (getFormat().getMatchchar() == 0 || i == 1 || matrix[t][1] != matrix[t][i])
                            w.write(padLabel(stateLabeler.char2token(i, getOriginal(t, i)), false, columnWidth));
                        else
                            w.write(padLabel("" + getFormat().getMatchchar(), false, columnWidth));
                    }
                }
                w.write("\n");
            }
            c += maxColumns;
            if (c < getNchar())
                w.write("\n");
        }
    }

    /**
     * gets the value of a format switch
     *
     * @param name Name of the format
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        if (name.equalsIgnoreCase("interleave"))
            return getFormat().getInterleave();
        else if (name.equalsIgnoreCase("transpose"))
            return getFormat().getTranspose();
        else if (name.equalsIgnoreCase("labels"))
            return getFormat().getLabels();
        else
            return !name.equalsIgnoreCase("tokens") || getFormat().getTokens();
    }

    /**
     * Write the characters and taxa in a data block. Added it for backward compatibility and
     * for MrBayes.
     *
     * @param w    the writer
     * @param taxa the taxa
     * @throws IOException if there are file errors
     */
    public void writeDataBlock(Writer w, Taxa taxa) throws java.io.IOException {
        w.write("\nBEGIN DATA;\n");
        w.write("DIMENSIONS ntax=" + taxa.getNtax() + " nchar=" + getNchar() + ";\n");
        w.write("FORMAT\n");
        if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.STANDARD))
            w.write("\tdatatype=STANDARD\n");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.DNA))
            w.write("\tdatatype=DNA\n");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.RNA))
            w.write("\tdatatype=RNA\n");
        else if (getFormat().getDatatype().equalsIgnoreCase(Datatypes.PROTEIN))
            w.write("\tdatatype=PROTEIN\n");
        else
            w.write("\tdatatype='" + getFormat().getDatatype() + "'\n");

        if (getFormat().getRespectCase())
            w.write("\trespectcase\n");

        if (getFormat().getMissing() != 0)
            w.write("\tmissing=" + getFormat().getMissing() + "\n");
        if (getFormat().getMatchchar() != 0)
            w.write("\tmatchChar=" + getFormat().getMatchchar() + "\n");
        if (getFormat().getGap() != 0)
            w.write("\tgap=" + getFormat().getGap() + "\n");


        if (getFormat().getInterleave())
            w.write("\tinterleave=yes\n");
        else
            w.write("\tinterleave=no\n");

        w.write(";\n");
        if (charWeights != null) {
            w.write("charweights");
            for (int i = 1; i <= getNchar(); i++)
                w.write(" " + charWeights[i]);
            w.write(";\n");
        }

        boolean oldLabelFormat = getFormat().getLabels();
            getFormat().setLabels(true);


        w.write("MATRIX\n");
        if (matrix != null)
                if (getFormat().interleave)
                    writeMatrixInterleaved(w, taxa);
                else
                    writeMatrix(w, taxa);
        w.write(";\nEND; [" + Characters.NAME + "]\n");
            getFormat().setLabels(oldLabelFormat);
    }

    /**
     * clones a characters object
     *
     * @param taxa Taxa block that this characters block is associated to
     * @return a clone
     */
    public Characters clone(Taxa taxa) {
        Characters characters = new Characters();
        try {
            StringWriter sw = new StringWriter();
            this.write(sw, taxa);
            StringReader sr = new StringReader(sw.toString());
            characters.read(new NexusStreamParser(sr), taxa, null);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return characters;
    }

    /**
     * remove all masked sites from the dataset
     *
     * @return number removed
     */
    public int removeMaskedSites() {
        int removed = removeMaskedSites(mask);
        if (originalCharacters != null)
            originalCharacters.removeMaskedSites(mask);
        mask = null;
        return removed;
    }

    /**
     * remove all sites mentioned in the mask
     *
     * @param theMask boolean array, with true indicating that the site is masked
     * @return number of sites removed
     */
    public int removeMaskedSites(boolean[] theMask) {
        if (theMask == null)
            return 0;
        int numMasked = 0;
        for (int c = 1; c <= getNchar(); c++) {
            if (isMasked(c))
                numMasked++;
        }
        if (numMasked == 0)
            return 0;
        int newNchar = getNchar() - numMasked;

        double[] oldCharWeights = null;
        if (charWeights != null) {
            oldCharWeights = charWeights;
            charWeights = new double[newNchar + 1];
        }

        Hashtable<Integer, String> oldLabelTable = null;
        if (charLabeler != null) {
            oldLabelTable = charLabeler;
            charLabeler = new Hashtable<>();
        }

        char[][] oldMatrix = new char[getNtax() + 1][];

        for (int t = 1; t <= getNtax(); t++) {
            oldMatrix[t] = matrix[t];
            matrix[t] = new char[newNchar + 1];
        }

        for (int oldPos = 1, newPos = 1; oldPos <= getNchar(); oldPos++, newPos++) {
            while (oldPos <= getNchar() && theMask[oldPos])
                oldPos++;
            if (oldPos > getNchar())
                break;
            if (oldCharWeights != null)
                charWeights[newPos] = oldCharWeights[oldPos];
            if (oldLabelTable != null)
                charLabeler.put(newPos, oldLabelTable.get(new Integer(oldPos)));
            for (int t = 1; t <= getNtax(); t++) {
                matrix[t][newPos] = oldMatrix[t][oldPos];
            }
        }
        nchar = newNchar;
        return numMasked;
    }

    private Taxa previousTaxa;   // keep this to help determine whether to reconstruct
    private Characters originalCharacters;

    /**
     * return the induced object obtained by hiding taxa
     *
     * @param origTaxa   original (full?) taxa block
     * @param hiddenTaxa set of taxa to be hidden
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if (hiddenTaxa.cardinality() == 0 && originalCharacters == null)
            return;   // nothing to do

        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = (Taxa) inducedTaxa.clone();

        if (originalCharacters == null)
            originalCharacters = this.clone(origTaxa); // make a copy


        this.ntax = inducedTaxa.getNtax();
        this.nchar = originalCharacters.getNchar();
        this.nactive = originalCharacters.getNchar();


        int count = 0;
        for (int t = 1; t <= origTaxa.getNtax(); t++) {
            if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                count++;
                matrix[count] = originalCharacters.matrix[t];
            }
        }

    }


    /**
     * produces a full string representation of this nexus block
     *
     * @param taxa Taxa block
     * @return object in necus
     */
    public String toString(Taxa taxa) {
        Writer w = new StringWriter();
        try {
            write(w, taxa);
        } catch (IOException ex) {
            Basic.caught(ex);
        }
        return w.toString();
    }

    /**
     * returns a row of data as a string
     *
     * @param t sequence number
     * @return a sequence
     */
    public String getRowAsString(int t) {
        return (String.valueOf(getRow(t))).substring(1);
    }

    /**
     * This class handles conversions from tokens(word) descriptions of states to chars.
     * Words are read in an converted to type char (0...Character.MAX_VALUE). The behavior
     * of the class differs for different datatypes
     * With PROTEIN data, the class converts from the standard
     * three letter amino acid codes to chars in the Datatypes.PROTEINSYMBOLS string.
     * With MICROSAT data, the class converts integers into chars (direct conversion + constant) and back
     * With STANDARD or UNKNOWN data, with tokens turned on, the class reads and stores tokens for
     * each site. These tokens are numbered as they are read in, and the char is a direct conversion
     * of this number.
     * <p/>
     * TODO: Separate into three classes
     */
    class StateLabeler {
        HashMap<String, Character>[] token2charMaps; //Map from strings to char, one for each site
        HashMap<Character, String>[] char2tokenMaps;  //Reverse of the above map
        boolean proteins; //Is the datatype proteins. If yes, tokens = yes means we use 3 letter aa codes.
        boolean microsat; //Is the data microsat


        int maxState; //The states used will be characters 0....maxStates (incl) in availableChars
        String availableChars; //List of ascii characters for use in standard mode

        TreeSet charsUsed; //Set of characters used in microsatelite data.
        static final int OFFSET = 256; //Offset for chars used to store microsattelite alleles (to avoid conflicts)

        /**
         * Encode ch for use in a reg exp.
         *
         * @param ch character
         * @return String the character, possibly with a backslash before.
         */
        private String regString(char ch) {
            if (ch == '^' || ch == '-' || ch == ']' || ch == '\\')
                return "\\" + ch;
            else
                return "" + ch;
        }

        /**
         * For proteins, sets up maps to go from 3 letter code to 1 letter AA code.
         * For microsats, initialises
         * Constructs the token handler. Records the chars used for missing, gap and match characters,
         * so these don't get used in the encoding and cause havoc.
         *
         * @param characters Characters block, from which we get number sites, gap, match and missing characters.
         */
        protected StateLabeler(Characters characters) {
            maxState = -1;
            if (Objects.equals(characters.getFormat().datatype, Datatypes.PROTEIN)) {
                proteins = true;
                availableChars = Datatypes.PROTEINSYMBOLS;
                String[] codes = {"ala", "arg", "asn", "asp", "cys", "gln", "glu", "gly", "his", "ile", "leu", "lys", "met", "phe", "pro", "ser", "thr", "trp", "tyr", "val"};
                token2charMaps = new HashMap[1];
                char2tokenMaps = new HashMap[1];
                token2charMaps[0] = new HashMap();
                char2tokenMaps[0] = new HashMap();
                for (int i = 0; i < 20; i++) {
                    token2charMaps[0].put(codes[i], availableChars.charAt(i));
                    char2tokenMaps[0].put(availableChars.charAt(i), codes[i]);
                }
            } else if (characters.getFormat().datatype.equalsIgnoreCase(Datatypes.MICROSAT)) {
                microsat = true;
                charsUsed = new TreeSet();
            } else {
                //Build up a string containing all characters we can use.

                availableChars = "1234567890";  //These are the standard ones for paup, mesquite etc.
                availableChars += "abcdefghijklmnopqrstuvwxyz";    //augment them with lower case letters
                for (char ch = 192; ch <= 255; ch++)           //and misc. ascii characters.
                    availableChars += "" + ch;

                //Now remove characters that are forbidden
                String forbidden = ";\\[\\],()/"; //punctuation characters  // todo: there is a problem with this expression
                forbidden += regString(characters.getFormat().getMissing());
                forbidden += regString(characters.getFormat().getMatchchar());
                forbidden += regString(characters.getFormat().getGap());
                availableChars = availableChars.replaceAll("[" + forbidden + "]", "");

                //Initialise the maps at each site.
                int nChars = characters.getNchar();
                token2charMaps = new HashMap[nChars + 1];
                char2tokenMaps = new HashMap[nChars + 1];
                for (int i = 1; i <= nChars; i++) {
                    token2charMaps[i] = new HashMap();
                    char2tokenMaps[i] = new HashMap();
                }
            }
        }

        /**
         * Takes a token and site. If the token has appeared at that site, returns corresponding char.
         * Otherwise, adds token to the map and returns a newly assigned char.
         *
         * @param site  site in the characters block
         * @param token name os token
         * @return char used to encode that token
         * @throws SplitsException if there are too many allels at a site.
         */
        protected char token2char(int site, String token) throws SplitsException {
            if (proteins) {
                if (token2charMaps[0].containsKey(token))
                    return token2charMaps[site].get(token);
                else
                    throw new SplitsException("Unidentified amino acid: " + token);
            } else if (microsat) {
                int val = Integer.parseInt(token);
                char ch = (char) (val + OFFSET);
                charsUsed.add(ch);
                return ch;
            } else if (token2charMaps[site].containsKey(token)) {
                return token2charMaps[site].get(token);
            } else {
                int id = token2charMaps[site].size() + 1;
                if (id >= availableChars.length())
                    throw new SplitsException("Too many alleles per site: please contact authors");
                Character ch = availableChars.charAt(id - 1);
                maxState = Math.max(maxState, id - 1);
                token2charMaps[site].put(token, ch);
                char2tokenMaps[site].put(ch, token);
                return ch;
            }
        }

        /**
         * Returns token associated to a given char value at a particular site, or null if
         * there is none.
         *
         * @param site number of the site
         * @param ch   char
         * @return token name, or null if ch not stored for this site.
         */
        protected String char2token(int site, char ch) {
            if (proteins)
                return char2tokenMaps[0].get(new Character(ch));
            if (microsat)
                return (new Integer((int) ch - OFFSET)).toString();
            return char2tokenMaps[site].get(new Character(ch));
        }

        /**
         * Takes a list of tokens and converts them into a string of associated char values.
         * It uses the token maps stored at each site; hence the need to know which site we
         * are start at, and if we are reading a transposed matrix or not.
         *
         * @param tokens     list of tokens
         * @param firstSite  site that first token is read for.
         * @param transposed true if the tokens all come from the same character/site
         * @return String of encoded chars.
         * @throws SplitsException if there are too many allels at a site.
         */
        protected String parseSequence(List<String> tokens, int firstSite, boolean transposed) throws SplitsException {
            char[] chars = new char[tokens.size()]; //FOr efficiency, allocate this as an array, then convert to string.
            int site = firstSite;
            int index = 0;
            for (String token : tokens) {
                chars[index] = token2char(site, token);
                if (!transposed)
                    site++;
                index++;
            }
            return new String(chars);
        }

        //TODO: The following don't apply to microsat or protein+token

        /**
         * Check if a site has states stored for it
         *
         * @param site NUmber of site (character)
         * @return true if states/tokens have been stored for that site.
         */
        protected boolean hasStates(int site) {
            return !(token2charMaps.length <= site || token2charMaps[site] == null) && (!token2charMaps[site].isEmpty());
        }


        String[] getStates(int site) {
            int size = char2tokenMaps[site].size();
            String[] stateArray = new String[size];
            int i = 0;
            char ch = availableChars.charAt(i);
            while (char2tokenMaps[site].containsKey(new Character(ch))) {
                stateArray[i] = char2tokenMaps[site].get(new Character(ch));
                i++;
                ch = availableChars.charAt(i);
            }
            return stateArray;
        }


        /**
         * Update the token records after a mask is applied to the set of characters.
         *
         * @param theMask array of boolean: true means the site is masked
         */
        protected void removeMaskedSites(boolean[] theMask) {
            if (theMask == null)
                return;
            int numMasked = 0;
            for (int c = 1; c <= getNchar(); c++) {
                if (isMasked(c))
                    numMasked++;
            }
            if (numMasked == 0)
                return;
            int newNchar = getNchar() - numMasked;

            HashMap<String, Character>[] newToken2charMaps = new HashMap[newNchar + 1];
            HashMap<Character, String>[] newChar2tokenMaps = new HashMap[newNchar + 1];

            for (int oldPos = 1, newPos = 1; oldPos <= getNchar(); oldPos++, newPos++) {
                while (oldPos <= getNchar() && theMask[oldPos])
                    oldPos++;
                if (oldPos > getNchar())
                    break;
                newToken2charMaps[newPos] = token2charMaps[oldPos];
                //newChar2tokenMaps[newPos] = token2charMaps[oldPos];
            }
            token2charMaps = newToken2charMaps;
            char2tokenMaps = newChar2tokenMaps;
        }

        /**
         * Return the length of the longest token.
         *
         * @return int the longest token
         */
        protected int getMaximumLabelLength() {
            if (proteins)
                return 3;
            if (microsat) {
                Character ch = (Character) charsUsed.last();
                Integer maxVal = (int) ch;
                return (maxVal.toString()).length();
            }
            int max = 0;
            for (int i = 1; i <= getNchar(); i++)
                for (String token : token2charMaps[i].keySet())
                    max = Math.max(max, (token).length());
            return max;
        }

        protected String getSymbolsUsed() {
            if (proteins)
                return Datatypes.PROTEINSYMBOLS;
            if (microsat) {
                StringBuilder symbols = new StringBuilder();
                for (Object aCharsUsed : charsUsed) {
                    symbols.append(((Character) aCharsUsed).charValue());
                }
                return symbols.toString();
            }
            if (maxState < 0)
                return null;
            else
                return availableChars.substring(0, maxState + 1);
        }

    }


}
