/*
 * SpectralSplits.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.characters;

import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.matrix.Hadamard;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * computes splits using spectral analysis
 *
 * @author huson
 * Date: 21-Mar-2005
 */
public class SpectralSplits implements Characters2Splits {
    //public boolean EXPERT=true;

    final static String KIMURA_SPECTRA = "KimuraSpectra";
    String optionMethod = KIMURA_SPECTRA;

    double optionThreshold = 0;
    double optionWeight_ATvsGC = 1;
    double optionWeight_AGvsCT = 1;
    double optionWeight_ACvsGT = 1;

    final static int STANDARD_FUNCTION = 0;
	final boolean verbose = false;
	boolean log_neg_arg_warned = false;

    public final static String DESCRIPTION = "Computes splits using spectral analysis (Hendy and Penny 1993)";

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        try {
            System.err.println("apply spectral splits");
            doc.notifyTasks("SpectralSplits", null);
            float[][] spectra;

            if (getOptionMethod().equals(KIMURA_SPECTRA))
                spectra = this.computeKimuraSpectra(doc, taxa, chars);
            else
                throw new Exception("Unknown method: " + getOptionMethod());


            if (verbose)
            // print s-vectors
            {
                for (int i = 0; i < 3; i++) {
                    System.err.println("\nspectrum " + i);
                    for (int k = 0; k < spectra[i].length; k++) {
                        System.err.println(k + " " + spectra[i][k]);
                    }
                }
            }

            return makeSplits(doc, taxa.getNtax(), spectra);
        } catch (Exception ex) {
            Basic.caught(ex);
            throw ex;
        }
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc.isValid(taxa) && doc.isValid(chars)
                && chars.getFormat().getDatatype().equals(Characters.Datatypes.DNA)
                && taxa.getNtax() < 32;
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * this does the work
     *
     * @return three spectra
     */
    float[][] computeKimuraSpectra(Document doc, Taxa taxa, Characters chars) throws CanceledException {
        doc.notifyTasks("SpectralSplits", "Compute Kimura Spectra");
        doc.notifySetMaximumProgress(3);

        log_neg_arg_warned = false;

        int nsize = 1 << (taxa.getNtax() - 1);

        final float[][] result = new float[3][nsize];
        final float[][] s_vecs = new float[3][nsize];

        // Computes s-vectors:
        double sum = computeSVectors(taxa, chars, s_vecs);
        System.err.println(" s0-2");

        if (verbose)
        // print s-vectors
        {
            for (int i = 0; i < 3; i++) {
                System.err.println("\ns-vector " + i);
                for (int k = 0; k < nsize; k++) {
                    System.err.println(k + " " + s_vecs[i][k]);
                }
            }
        }

        // Consider each of the three s-vectors:
        for (int i = 0; i < 3; i++) {
            // Compute r vector:
            final float[] r_vec = Hadamard.compute(s_vecs[i], result[i]); //tempoarily use result[i]
            // Compute rho vector:
            final float[] rho_vec = s_vecs[i];  // temporarily use this vector
            applyFunction(r_vec, rho_vec, sum, STANDARD_FUNCTION);
            System.err.println(" rho" + i);
            // Compute gamma spectrum:
            result[i] = Hadamard.computeInverse(rho_vec);
            System.err.println(" gamma" + i);
            doc.notifySetProgress(i + 1);

        }
        return result;
    }

    /**
     * Given a spectrum, applies the named function to all the entries.
     *
     */
    void applyFunction(float[] src, float[] tar, double para, int whichFunction) {
        for (int i = 0; i < src.length; i++) {
            switch (whichFunction) {
                default:
                case STANDARD_FUNCTION:
                    tar[i] = standardFunction(i, para, src);
                    break;
            }
        }
    }

    /**
     * Standard invertible conversion from the r vector to the rho vector
     *
     * @return rho
     */
    float standardFunction(int i, double para, float[] src) {
        double arg = src[i] / para;

        if (arg > 0)
            return (float) Math.log(arg);
        else if (arg < 0 && !log_neg_arg_warned) {
            log_neg_arg_warned = true;
            new Alert("log(" + arg + "=" + src[i] + "/" + para + "): negative argument (no further warnings will be given)");
        }
        return (float) -100;
    }

    /**
     * Computes the 3 s-vectors (indexed 1..3) arising in the 4-state character Kimura spectral
     * analysis
     * Assumes that the three given s_vector pointers are zero and allocates space
     * appropriately:
     *
     * @return sum
     */
    double computeSVectors(Taxa taxa, Characters chars, float[][] s_vec) {
        int ntax = taxa.getNtax();
        // Set up set of acceptable character states (A, C, G, T or U)
        BitSet accept = new BitSet();
        String acgtuACGTU = "acgtuACGTU";
        for (int i = 0; i < acgtuACGTU.length(); i++)
			accept.set(acgtuACGTU.charAt(i));

        // Set characters for Kimura alpha and beta comparision:
        BitSet A1 = new BitSet();
        String gGcC = "gGcC";
        for (int i = 0; i < gGcC.length(); i++)
			A1.set(gGcC.charAt(i));

        BitSet A2 = new BitSet();
        String aAgG = "aAgG";
        for (int i = 0; i < aAgG.length(); i++)
			A2.set(aAgG.charAt(i));

        // Set up taxon indexing of sets in spectrum:
        int[] tax2pos = new int[ntax + 1];
        tax2pos[1] = 1;
        for (int i = 2; i <= ntax; i++)
            tax2pos[i] = 2 * tax2pos[i - 1];

        // Clears vectors:
        Arrays.fill(s_vec[0], 0);
        Arrays.fill(s_vec[1], 0);
        Arrays.fill(s_vec[2], 0);

        double sum = 0.0;
        for (int c = 1; c <= chars.getNchar(); c++) {
            if (!chars.isMasked(c)) {
                double weight = chars.getCharWeight(c);

                int pos1 = 0, pos2 = 0, pos3 = 0;

				int ch = chars.get(ntax, c);
                if (accept.get(ch)) {
                    boolean refer1 = A1.get(ch);
                    boolean refer2 = A2.get(ch);
                    int t;
                    boolean first, second;
                    boolean ok = true;
                    for (t = 1; t < ntax; t++) {
						ch = chars.get(t, c);
                        if (!accept.get(ch)) {
                            ok = false;
                            break;
                        }
                        first = (refer1 != A1.get(ch));
                        if (first)
                            pos1 += tax2pos[t];
                        second = (refer2 != A2.get(ch));
                        if (second)
                            pos2 += tax2pos[t];
                        if (first != second)
                            pos3 += tax2pos[t];
                    }
                    if (ok) {
                        (s_vec[0][pos1]) += (float) weight;
                        (s_vec[1][pos2]) += (float) weight;
                        (s_vec[2][pos3]) += (float) weight;
                        sum++;
                    }
                }
            }
        }
        return sum;
    }

    /**
     * makes the splits using the given weights and threshold
     *
     * @return splits
     */

    Splits makeSplits(Document doc, int ntax, float[][] spectra) throws Exception {
        doc.notifyTasks("SpectralSplits", "Make Splits");
        doc.notifySetMaximumProgress(spectra[0].length);

        Splits splits = new Splits(ntax);
        // In array we store pairs:w_n,n  where n is the number of the split and w_n the
        // splits weight:
        for (int p = 0; p < spectra[0].length; p++) {
            float wgt = (float) (getOptionWeight_ATvsGC() * spectra[0][p]
                    + getOptionWeight_AGvsCT() * spectra[1][p]
                    + getOptionWeight_ACvsGT() * spectra[2][p]);
            if (wgt > getOptionThreshold()) {
                TaxaSet split = binary2split(ntax, p);
                splits.add(split, wgt);
            }
            doc.notifySetProgress(p);
        }
        return splits;
    }

    /**
     * converts a number into a split
     *
     */
    TaxaSet binary2split(int ntax, int p) {
        TaxaSet set = new TaxaSet();

        if (ntax > 31)
            throw new RuntimeException("binary2set(ntax=" + ntax + ",p=" + p + "): ntax out of range 0..31");
        for (int i = 0; i < ntax; i++) {
            if (((p >> i) & 1) != 0)
                set.set(i + 1);
        }
        return set;
    }

    public double getOptionThreshold() {
        return optionThreshold;
    }

    public void setOptionThreshold(double optionThreshold) {
        this.optionThreshold = optionThreshold;
    }

    public double getOptionWeight_ATvsGC() {
        return optionWeight_ATvsGC;
    }

    public void setOptionWeight_ATvsGC(double optionWeight_ATvsGC) {
        this.optionWeight_ATvsGC = optionWeight_ATvsGC;
    }

    public double getOptionWeight_AGvsCT() {
        return optionWeight_AGvsCT;
    }

    public void setOptionWeight_AGvsCT(double optionWeight_AGvsCT) {
        this.optionWeight_AGvsCT = optionWeight_AGvsCT;
    }

    public double getOptionWeight_ACvsGT() {
        return optionWeight_ACvsGT;
    }

    public void setOptionWeight_ACvsGT(double optionWeight_ACvsGT) {
        this.optionWeight_ACvsGT = optionWeight_ACvsGT;
    }

    public String getOptionMethod() {
        return optionMethod;
    }

    public void setOptionMethod(String optionMethod) {
        this.optionMethod = optionMethod;
    }

    public List<String> selectionOptionMethod(Document doc) {
        List<String> list = new LinkedList<>();
        list.add(KIMURA_SPECTRA);
        return list;
    }
}
