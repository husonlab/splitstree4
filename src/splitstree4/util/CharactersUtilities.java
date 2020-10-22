/**
 * CharactersUtilities.java
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
/*
 * $Id: CharactersUtilities.java,v 1.44 2010-05-09 19:15:29 bryant Exp $
 */

package splitstree4.util;

import jloda.swing.util.Alert;
import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.*;

import java.util.*;

/**
 * Methods for analyzing chars
 */
public class CharactersUtilities {
    private final static BitSet proteinLetters = new BitSet();
    private final static BitSet strictDNALetters = new BitSet();
    private final static BitSet strictRNALetters = new BitSet();

    private final static BitSet dnaPlusAmbiguityLetters = new BitSet();
    private final static BitSet rnaPlusAmbiguityLetters = new BitSet();
    private final static BitSet standardLetters = new BitSet();

    static {
        {
            final String letters = "ACGT?-".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                strictDNALetters.set(letters.charAt(i));
            }
        }
        {
            final String letters = "ACGU?-".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                strictRNALetters.set(letters.charAt(i));
            }
        }

        {
            final String letters = "ACDEFGHIKL*MNPQRSTVWXY-?".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                proteinLetters.set(letters.charAt(i));
            }
        }
        {
            final String letters = "ACGTRYSWKMBDHVN?-".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                dnaPlusAmbiguityLetters.set(letters.charAt(i));
            }
        }
        {
            final String letters = "ACGURYSWKMBDHVN?-".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                rnaPlusAmbiguityLetters.set(letters.charAt(i));
            }
        }
        {
            final String letters = "01?-".toLowerCase();
            for (int i = 0; i < letters.length(); i++) {
                standardLetters.set(letters.charAt(i));
            }
        }
    }

    /**
     * Returns a string description of the currently active sites
     *
     * @param chars the chars object
     * @return a description of the currently active sites
     */
    static public String getActiveSites(Characters chars) {
        String activeSites = "";
        int i = 0;
        if (chars.getMask() != null) {
            for (i = 0; i < chars.getNchar(); i++) {
                if (!chars.isMasked(i)) break;
            }
        }
        if (chars.getMask() == null || i == chars.getNchar() - 1) {
            activeSites += "all";
        } else {
            int bot = 0, top = 0;
            for (i = 1; i <= chars.getNchar(); i++) {
                if (!chars.isMasked(i)) {
                    if (bot == 0) bot = i;
                    top = i;
                }
                if (bot != 0 && (top != i || top == chars.getNchar())) {// change from the non masked to the masked area
                    if (top != bot)
                        activeSites += " " + bot + "-" + top;
                    else
                        activeSites += " " + bot;
                    bot = 0;
                }
            }


        }
        return activeSites;
    }

    /**
     * Computes a matrix containing only active sites
     *
     * @param chars the chars
     * @return the matrix of active chars
     */
    static public char[][] getActiveCharactersMatrix(Characters chars) {
        int newPos = 1;
        int nActive = chars.getNactive();
        if (nActive < 1)
            nActive = chars.getNchar();

        char[][] sourceMatrix = new char[chars.getNtax() + 1][nActive + 1];
        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                for (int t = 1; t <= chars.getNtax(); t++) {
                    sourceMatrix[t][newPos] = chars.get(t, c);
                }
                newPos++;
            }
        }
        return sourceMatrix;
    }

    //ToDo: We should be able to remove the taxa parameter, as we can get the number of taxa from source.

    /**
     * Randomly resamples a character matrix from a given matrix. If the data is diploid, then
     * pairs of characters are sampled at a time.
     * //TODO: Provide support for sampling codons (3 at a time)
     *
     * @param taxa        the taxa block
     * @param source      the sample source Characters block
     * @param ncharSample the number of chars of the sample source matrix.
     * @return the Characters Block with a matrix consisting of random samples
     */

    static public Characters resample(Taxa taxa, Characters source, int ncharSample, Random rand) throws SplitsException {
        int nchar = source.getNchar();
        Characters result = new Characters(taxa.getNtax(), ncharSample, source.getFormat());

        if (!result.getFormat().isDiploid()) {
            int pos;
            for (int c = 1; c <= ncharSample; c++) {
                pos = Math.abs(rand.nextInt() % nchar) + 1;
                for (int i = 1; i <= taxa.getNtax(); i++) {
                    result.set(i, c, source.get(i, pos));
                }
            }
        } else {
            if (ncharSample % 2 != 0)
                throw new SplitsException("When data is diploid, number of characters sampled must be even");
            int nloci = ncharSample / 2;
            int pos;
            for (int c = 1; c <= nloci; c++) {
                pos = rand.nextInt(nloci) + 1;
                for (int i = 1; i <= taxa.getNtax(); i++) {
                    result.set(i, 2 * c - 1, source.get(i, 2 * pos - 1));
                    result.set(i, 2 * c, source.get(i, 2 * pos));
                }
            }
        }
        return result;
    }

    /**
     * Given a character block and a set of sites, returns the
     * characters block containing only those sites.
     *
     * @param orig   Original character block
     * @param subset Subset of sites
     * @return Characters New characters block containing subset of original sites
     * @throws SplitsException Exception if the set of sites is invalid and contains
     *                         values not between 1 and the number of characters.
     */
    static public Characters characterSubset(Characters orig, Set subset) throws SplitsException {
        int ntax = orig.getNtax();
        int nchar = subset.size();
        Characters result = new Characters(ntax, nchar, orig.getFormat());
        int pos = 1;
        for (Object aSubset : subset) {
            int oldpos = (Integer) aSubset;
            if (oldpos <= 0 || oldpos > nchar)
                throw new SplitsException("Character subset contains invalid character index " + oldpos);
            for (int i = 1; i <= ntax; i++)
                result.set(i, pos, orig.get(i, oldpos));
            pos++;
        }

        return result;
    }


    /**
     * Computes the frequencies matrix
     *
     * @param chars the chars
     * @param s     a taxon
     * @param t     a second taxon
     * @return the frequencies matrix
     */
    static public double[][] computeF(Characters chars, int s, int t) {
        int ncolors = chars.getNcolors();
        double[][] freqMat = new double[ncolors + 1][ncolors + 1];

        for (int i = 1; i <= chars.getNchar(); i++)
            if (!chars.isMasked(i)) {
                char c = chars.get(s, i);
                char d = chars.get(t, i);

                freqMat[chars.getColor(c)][chars.getColor(d)]++;
            }

        //divide by number of valid pairs

        double count = 0;
        for (int p = 1; p <= ncolors; p++)
            for (int q = 1; q <= ncolors; q++)
                count += freqMat[p][q];

        for (int p = 1; p <= ncolors; p++)
            for (int q = 1; q <= ncolors; q++)
                if (count > 0)
                    freqMat[p][q] /= count;
                else
                    freqMat[p][q] = 1;

        return freqMat;
    }

    /**
     * Computes the frequencies matrix from *all* taxa
     *
     * @param chars  the chars
     * @param warned Throw an alert if an unexpected symbol appears.
     * @return the frequencies matrix
     */
    //TODO: Replace System.err with code throwing exceptions
//ToDo: BaseFrequencies should be stored somewhere, perhaps characters.properties
    static public double[] computeFreqs(Characters chars, boolean warned) {
        int ncolors = chars.getNcolors();
        int numNotMissing = 0;
        String symbols = chars.getFormat().getSymbols();
        int numStates = symbols.length();
        double[] Fcount = new double[numStates];
        char missingchar = chars.getFormat().getMissing();
        char gapchar = chars.getFormat().getGap();

        for (int i = 1; i < chars.getNtax(); i++) {
            char[] seq = chars.getRow(i);
            for (int k = 1; k < chars.getNchar(); k++) {
                if (!chars.isMasked(k)) {
                    char c = seq[k];

                    //Convert to lower case if the respectCase option is not set
                    if (!chars.getFormat().getRespectCase()) {
                        if (c != missingchar && c != gapchar)
                            c = Character.toLowerCase(c);
                    }
                    if (c != missingchar && c != gapchar) {
                        numNotMissing = numNotMissing + 1;

                        int state = symbols.indexOf(c);

                        if (state >= 0) {
                            Fcount[state] += 1.0;
                        } else if (state < 0 && !warned) {

                            new Alert("Unknown symbol encountered in characters: " + c);
                            warned = true;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < numStates; i++)
            Fcount[i] = Fcount[i] / (double) numNotMissing;

        return Fcount;

    }

    /**
     * Masks the character data according to the set assumptions
     *
     * @param chars the Characters block
     */
    public static void maskCharacters(Assumptions assumptions, Characters chars) {
        maskCharacters(assumptions, null, chars);
    }


    /**
     * Masks the character data according to the set assumptions
     *
     * @param characters the Characters block
     */
    public static void maskCharacters(Assumptions assumptions, Sets sets, Characters characters) {
        if (characters == null)
            return;
        characters.clearMask();  // must clean mask!

        if (assumptions.getExChar() != null) {
            for (Integer c : assumptions.getExChar()) {
                characters.setMasked(c, true);
            }
        }

        if (assumptions.getExcludeCodon1() || assumptions.getExcludeCodon2()
                || assumptions.getExcludeCodon3()) {
            for (int c = 1; c <= characters.getNchar(); c++) {
                switch (c % 3) {
                    case 0:
                        if (assumptions.getExcludeCodon3())
                            characters.setMasked(c, true);
                        break;
                    case 1:
                        if (assumptions.getExcludeCodon1())
                            characters.setMasked(c, true);
                        break;
                    case 2:
                        if (assumptions.getExcludeCodon2())
                            characters.setMasked(c, true);
                        break;
                }
            }
        }
        if (assumptions.getExcludeConstant() == Characters.EXCLUDE_ALL_CONSTANT || assumptions.getExcludeMissing() < 1.0
                || assumptions.getExcludeGaps() || assumptions.getExcludeNonParsimony()) {
            for (int c = 1; c <= characters.getNchar(); c++) {
                if (!characters.isMasked(c)) {
                    char first = characters.get(1, c);
                    boolean isConstant = true;
                    int nMissing = 0;
                    int ntax = characters.getNtax();
                    int threshold = (int) Math.floor(assumptions.getExcludeMissing() * ntax) + 1; //Exclude sites with this much missing

                    BitSet seenOnce = new BitSet();
                    BitSet seenTwice = new BitSet();
                    for (int t = 1; t <= characters.getNtax(); t++) {
                        char ch = characters.get(t, c);
                        if (ch == characters.getFormat().getMissing()) {
                            nMissing++;
                            if (nMissing >= threshold) {
                                characters.setMasked(c, true);

                                //break;
                            }
                        }
                        if (ch == characters.getFormat().getGap()) {
                            if (assumptions.getExcludeGaps()) {
                                characters.setMasked(c, true);

                                break;
                            }
                        }
                        if (ch != first) {
                            isConstant = false;
                        }
                        if (assumptions.getExcludeNonParsimony()) {
                            if (!seenOnce.get(ch))
                                seenOnce.set(ch);
                            else if (!seenTwice.get(ch))
                                seenTwice.set(ch);
                        }
                    }

                    /* System.err.print("Character missing "+nMissing+" out of "+ntax);
                    if (nMissing>=threshold)
                        System.err.println(" *Masked");
                    else
                        System.err.println();        */

                    if (isConstant && assumptions.getExcludeConstant() == Characters.EXCLUDE_ALL_CONSTANT)
                        characters.setMasked(c, true);
                    if (assumptions.getExcludeNonParsimony()
                            && (seenTwice.cardinality() <= 1 && seenOnce.cardinality() == 2))
                        characters.setMasked(c, true);
                }
            }
        }

        int maskedByCharSet = 0;
        List<String> usecharsets = assumptions.getUseCharSets();
        if (usecharsets != null && usecharsets.size() > 0 && sets != null && sets.getNumCharSets() > 0) {
            String unknown = "";
            Set<Integer> use = new HashSet<>();
            for (String label : usecharsets) {
                Set<Integer> charSet = sets.getCharSet(label);
                if (charSet != null) {
                    use.addAll(charSet);
                } else
                    unknown += " " + label;
            }
            if (unknown.length() > 0)
                new Alert("Unknown charsets: " + unknown);

            if (use.size() > 0) {
                for (int c = 1; c <= characters.getNchar(); c++) {
                    if (!use.contains(c) && !characters.isMasked(c)) {
                        characters.setMasked(c, true);
                        maskedByCharSet++;
                    }
                }
            }
        }
        if (maskedByCharSet > 0)
            System.err.println("Masked by charset: " + maskedByCharSet);
    }


    /**
     * guess the data type
     *
     * @param excluded
     * @param seq
     * @return the data type
     */
    static public String guessType(String excluded, String seq) {
        return guessType(excluded, seq.toCharArray());
    }

    /**
     * guess the data type
     *
     * @param seq
     * @return the data type
     */
    static public String guessType(String seq) {
        return guessType(null, seq.toCharArray());
    }


    /**
     * guess the data type
     *
     * @param seq
     * @param excluded characters to be disregarded
     * @return the data type
     */
    static public String guessType(String excluded, char[] seq) {
        final BitSet alphabet = new BitSet();

        for (char aSeq : seq) {
            char ch = Character.toLowerCase(aSeq);
            if (excluded == null || excluded.indexOf(ch) == -1)
                alphabet.set(Character.toLowerCase(aSeq));
        }

        if (intersection(strictDNALetters, alphabet).cardinality() == alphabet.cardinality())
            return Characters.Datatypes.DNA;

        if (intersection(strictRNALetters, alphabet).cardinality() == alphabet.cardinality())
            return Characters.Datatypes.RNA;

        boolean possiblyProtein = (intersection(proteinLetters, alphabet).cardinality() == alphabet.cardinality());
        boolean possiblyDNA = (intersection(dnaPlusAmbiguityLetters, alphabet).cardinality() == alphabet.cardinality());
        boolean possiblyRNA = (intersection(rnaPlusAmbiguityLetters, alphabet).cardinality() == alphabet.cardinality());
        boolean possiblyStandard = (intersection(standardLetters, alphabet).cardinality() == alphabet.cardinality());

        if (possiblyProtein && !possiblyDNA && !possiblyRNA)
            return Characters.Datatypes.PROTEIN;
        if (!possiblyProtein && possiblyDNA && !possiblyRNA)
            return Characters.Datatypes.DNA;
        if (!possiblyProtein && !possiblyDNA && possiblyRNA)
            return Characters.Datatypes.RNA;
        if (possiblyStandard)
            return Characters.Datatypes.STANDARD;

        return Characters.Datatypes.UNKNOWN;
    }

    /**
     * compute intersection
     *
     * @param a
     * @param b
     * @return
     */
    private static BitSet intersection(BitSet a, BitSet b) {
        BitSet result = new BitSet();
        result.or(a);
        result.and(b);
        return result;
    }

    /**
     * adjustFreqs
     * <p/>
     * Tries to make sense of a set of base frequencies input by the user.
     * Takes an array of non-negative values. Any zero entries are replaced with the mean of the
     * remaining entries. The entries are then normalised to sum to one.
     *
     * @param freqs   array of base frequencies
     * @param nstates number of states
     * @throws IllegalArgumentException if an entry is negative.
     */
    static public void adjustFreqs(double[] freqs, int nstates) {

        //Check all frequencies are positive
        for (int i = 0; i < nstates; i++) {
            if (freqs[i] < 0.0) {
                throw new IllegalArgumentException("Negative base frequency");
            }
        }

        //Compute average of non-zero frequencies
        int numNonZero = 0;
        double mu = 0.0;
        for (int i = 0; i < nstates; i++) {
            if (freqs[i] > 0.0) {
                numNonZero++;
                mu += freqs[i];
            }
        }
        if (numNonZero > 0) {
            mu = mu / (double) numNonZero;
        } else {
            mu = 1.0;
        }

        //Replace missing frequencies by average
        double total = 0.0;
        for (int i = 0; i < nstates; i++) {
            if (freqs[i] > 0.0) {
                freqs[i] = mu;
            }
            total += freqs[i];
        }

        //Normalize
        for (int i = 0; i < nstates; i++) {
            freqs[i] /= total;
        }
    }

    /**
     * returns r or y for a nucleotide
     *
     * @param ch
     * @return 'r', 'y' or 0
     */
    static public char getRYlowerCase(char ch) {
        switch (ch) {
            case 'a':
            case 'A':
            case 'g':
            case 'G':
                return 'r';
            case 'c':
            case 'C':
            case 't':
            case 'T':
            case 'u':
            case 'U':
                return 'y';
            default:
                return 0;
        }
    }

    /**
     * Concatenate two character blocks. There are no checks on whether the blocks have compatible
     * formats etc. The returned block has the format block of the first characters block.
     * If there are allocation problems, or if the characters have different numbers of taxa, then
     * null is returned.
     *
     * @param char1
     * @param char2
     * @return A Characters block, or null if there are problems
     */
    static public Characters concatenate(Characters char1, Characters char2) {
        if (char1.getNtax() != char2.getNtax())
            return null;
        int nchar = char1.getNchar() + char2.getNchar();
        int nchar1 = char1.getNchar();
        int ntax = char1.getNtax();

        Characters newChars = new Characters(char1.getNtax(), nchar, char1.getFormat());
        for (int i = 1; i <= ntax; i++) {
            for (int j = 1; j <= char1.getNchar(); j++)
                newChars.set(i, j, char1.get(i, j));
            for (int j = 1; j <= char2.getNchar(); j++)
                newChars.set(i, j + nchar1, char2.get(i, j));
        }
        return newChars;
    }

    /**
     * Check to see if two sequences are identical on all the unmasked sites.
     *
     * @param characters
     * @param i
     * @param j
     * @return true is sequences are identical. False otherwise.
     */
    static public boolean taxaIdentical(Characters characters, int i, int j) {
        char[] seqi = characters.getRow(i);
        char[] seqj = characters.getRow(j);
        char missingchar = characters.getFormat().getMissing();
        char gapchar = characters.getFormat().getGap();

        for (int k = 1; k <= characters.getNchar(); k++) {
            if (characters.isMasked(k))
                continue;
            char ci = seqi[k];
            char cj = seqj[k];

            //Convert to lower case if the respectCase option is not set
            if (!characters.getFormat().getRespectCase()) {
                if (ci != missingchar && ci != gapchar)
                    ci = Character.toLowerCase(ci);
                if (cj != missingchar && cj != gapchar)
                    cj = Character.toLowerCase(cj);
            }
            if (ci != cj)
                return false;
        }
        return true;
    }

    /**
     * Compute the fraction of sites where the sequences have different states,
     * as a proportion of sites where neither is missing or gapped.
     *
     * @param characters characters block
     * @param i          index of first sequence
     * @param j          index of second sequence
     * @return Proportion of sites where sequences differ, considering only sites without gaps or missing data in these sequences. Returns -1 if no such sites.
     */
    static public double proportionOfDifferences(Characters characters, int i, int j) {
        char[] seqi = characters.getRow(i);
        char[] seqj = characters.getRow(j);
        char missingchar = characters.getFormat().getMissing();
        char gapchar = characters.getFormat().getGap();
        int numdiff = 0;
        int nValidChar = 0;

        for (int k = 1; k <= characters.getNchar(); k++) {
            if (characters.isMasked(k))
                continue;
            char ci = seqi[k];
            char cj = seqj[k];

            if (ci != missingchar && ci != gapchar && cj != missingchar && cj != gapchar) {
                nValidChar++;
                if (!characters.getFormat().getRespectCase()) {
                    ci = Character.toLowerCase(ci);
                    cj = Character.toLowerCase(cj);
                }
                if (ci != cj)
                    numdiff++;
            }
        }
        if (nValidChar > 0)
            return (double) numdiff / nValidChar;
        else
            return -1.0;
    }

    /**
     * Returns average number of non-missing sites for a sequence in the Characters block
     *
     * @param chars Characters block
     * @return
     */
    static public double meanNotMissing(Characters chars) {

        char missingchar = chars.getFormat().getMissing();
        char gapchar = chars.getFormat().getGap();
        double nNotMissing = 0.0;
        for (int i = 1; i <= chars.getNtax(); i++) {
            for (int j = 1; j <= chars.getNchar(); j++) {
                char ch = chars.get(i, j);
                if (ch != missingchar && ch != gapchar)
                    nNotMissing += 1.0;
            }

        }
        return nNotMissing / chars.getNtax();
    }


    /**
     * Check to see if two sequences are identical using the distance data
     *
     * @param distances
     * @param i
     * @param j
     * @return true if two rows in the distance matrix are identical and the taxa have distance 0
     */
    static private boolean taxaIdentical(Distances distances, int i, int j) {

        int ntax = distances.getNtax();

        if (distances.get(i, j) > 0)
            return false;
        for (int k = 1; k <= ntax; k++) {
            if (k == i || k == j)
                continue;
            if (distances.get(i, k) != distances.get(j, k))
                return false;
        }
        return true;
    }


    /**
     * Returns the number of characters that are not masked.
     *
     * @param characters
     * @return int. Number of characters not masked.
     */
    static public int getNumberUnmasked(Characters characters) {
        int num = 0;
        int nchar = characters.getNchar();
        for (int k = 1; k <= nchar; k++)
            if (!characters.isMasked(k))
                num++;
        return num;
    }


    static public Document collapseByType(Taxa taxa, Characters characters, Distances distances, Document doc) {

        try {
            int ntax = taxa.getNtax();

            int typecount = 0;
            int numNonSingleClasses = 0;

            int[] taxaTypes = new int[taxa.getNtax() + 1];
            int[] representatives = new int[taxa.getNtax() + 1]; //Representative taxon of each type.

            Taxa newTaxa = new Taxa();
            Document newDoc = new Document();

            //Use a breadth-first search to identify classes of identical sequences or distance matrix rows.
            //Build up new taxa block. Classes of size one give new taxa with the same name, larger classes
            //are named TYPEn for n=1,2,3...
            for (int i = 1; i <= ntax; i++) {
                if (taxaTypes[i] != 0)  //Already been 'typed'
                    continue;
                typecount++;
                taxaTypes[i] = typecount;
                representatives[typecount] = i;
                int numberOfThisType = 1;
                String info = taxa.getLabel(i); //Start building up the info string for this taxon.


                for (int j = i + 1; j <= ntax; j++) {
                    if (taxaTypes[j] != 0)
                        continue;
                    boolean taxaIdentical;
                    if (characters != null)
                        taxaIdentical = taxaIdentical(characters, i, j);
                    else
                        taxaIdentical = taxaIdentical(distances, i, j);
                    if (taxaIdentical) {
                        taxaTypes[j] = typecount;
                        numberOfThisType++;
                        info += ", " + taxa.getLabel(j);
                    }
                }

                if (numberOfThisType > 1) {
                    numNonSingleClasses++;
                    newTaxa.add("TYPE" + numNonSingleClasses, info);
                } else
                    newTaxa.add(info, info); //Info is the same as taxa label.
            }
            newDoc.setTaxa(newTaxa);

            //Set up the new characters block, if one exists.
            Characters newCharacters;
            if (characters != null) {
                newCharacters = new Characters(newTaxa.getNtax(), characters.getNchar());
                for (int i = 1; i <= newTaxa.getNtax(); i++) {
                    int old_i = representatives[i];
                    for (int k = 1; k <= characters.getNchar(); k++)
                        newCharacters.set(i, k, characters.get(old_i, k));
                }
                newDoc.setCharacters(newCharacters);
            }

            //Set up the new distances block, if necc.
            Distances newDistances;
            if (distances != null) {
                newDistances = new Distances(newTaxa.getNtax());

                for (int i = 1; i <= newTaxa.getNtax(); i++) {
                    int old_i = representatives[i];
                    for (int j = 1; j < i; j++) {
                        int old_j = representatives[j];
                        double val = distances.get(old_i, old_j);
                        newDistances.set(i, j, val);
                        newDistances.set(j, i, val);
                    }
                }
                newDoc.setDistances(newDistances);
            }
            if (doc != null)
                newDoc.setTitle("Distinct Haplotypes for " + doc.getTitle());
            else
                newDoc.setTitle("Distinct Haplotypes");
            return newDoc;
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return null;
    }


}

//EOF
