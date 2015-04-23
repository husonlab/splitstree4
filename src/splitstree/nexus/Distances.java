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
 *@author Daniel Huson and David Bryant
 */

package splitstree.nexus;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;

import java.io.*;
import java.util.List;


/**
 * * The nexus distances block
 * Contains distances data
 */
public class Distances extends NexusBlock {
    /**
     * the format subclass
     */
    public class Format {
        private String triangle;
        private boolean labels;
        private boolean diagonal;
        private String varType = "ols";

        /**
         * the Constructor
         */
        public Format() {
            triangle = "both";
            labels = true;
            diagonal = true;
            varType = "ols";
        }

        /**
         * Get the value of triangle
         *
         * @return the value of triangle
         */
        public String getTriangle() {
            return triangle;
        }

        /**
         * Set the value of triangle.
         *
         * @param triangle the value of triangle
         */
        public void setTriangle(String triangle) throws SplitsException {
            if (!triangle.equals("both") && !triangle.equals("lower") && !triangle.equals("upper"))
                throw new SplitsException("Illegal triangle:" + triangle);
            this.triangle = triangle;
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
         * Get the value of diagonal
         *
         * @return the value of diagonal
         */
        public boolean getDiagonal() {
            return diagonal;
        }

        /**
         * Set the value of diagonal.
         *
         * @param diagonal the value diagonal
         */
        public void setDiagonal(boolean diagonal) {
            this.diagonal = diagonal;
        }

        /**
         * Get the value of varPower
         *
         * @return the value of varPower
         */
        public String getVarType() {
            return varType;
        }

        /**
         * Set the var type
         *
         * @param val
         */
        public void setVarType(String val) {
            this.varType = val;
        }
    }

    private boolean isSet = false;
    private int ntax = 0;
    /**
     * Identification string
     */
    public final static String NAME = "Distances";
    private double[][] matrix = null;
    private double[][] variance = null;
    private Format format = null;

    /**
     * Construct a new Distances object.
     */
    public Distances() {
        super();
        ntax = 0;
        format = new Format();
    }

    /**
     * Construct a new Distances object.
     *
     * @param ntax number of taxa
     */
    public Distances(int ntax) {
        this();

        setNtax(ntax);

    }

    /**
     * set the format object
     *
     * @param format the format object
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Return a format object
     *
     * @return the format object
     */
    public Format getFormat() {
        return format;
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
     * Set the number of taxa and reacclocates the distance matrix.
     *
     * @param ntax the number of taxa
     */

    public void setNtax(int ntax) {
        this.ntax = Math.max(0, ntax);
        this.matrix = new double[ntax + 1][ntax + 1];
    }

    /**
     * Get the matrix value.
     *
     * @param i the row
     * @param j the colum
     * @return the matix value  matrix[i][j]
     */
    public double get(int i, int j) {
        if ((matrix.length <= i) || (matrix.length <= j))
            System.out.println("this.ntax: " + this.ntax + " matrix.length: " + matrix.length);
        return matrix[i][j];
    }

    /**
     * Set the matrix value.
     *
     * @param i   the row
     * @param j   the colum
     * @param val the matix value at row i and colum j
     */
    public void set(int i, int j, double val) {
        isSet = true;
        matrix[i][j] = val;
    }

    /**
     * Returns the complete matrix
     *
     * @return double[][] matrix
     */
    public double[][] getMatrix() {
        return matrix;
    }

    /**
     * Check if the matrix is symmetric.
     *
     * @return boolean. True if it is symmetric.
     */
    private boolean isSymmetric() {
        int ntax = getNtax();
        for (int i = 1; i <= ntax; i++) {
            for (int j = 1; j < i; j++)
                if (get(i, j) != get(j, i))
                    return false;
        }
        return true;
    }

    /**
     * Symmetrize the matrix. Replace d_ij and d_ji with (d_ij+d_ji)/2
     */
    private void symmetrize() {
        int ntax = getNtax();
        for (int i = 1; i <= ntax; i++) {
            for (int j = 1; j < i; j++) {
                double d_ij = (get(i, j) + get(j, i)) / 2.0;
                set(i, j, d_ij);
                set(j, i, d_ij);
            }
        }
    }


    /**
     * Get the variance estimate
     *
     * @param i the row
     * @param j the colum
     * @return the variance estimate on the matrix[i][j];
     */
    public double getVar(int i, int j) {
        double vij = -1.0;
        String varType = getFormat().getVarType();
        if (varType.equalsIgnoreCase("ols"))
            return 1.0;
        else if (varType.equalsIgnoreCase("fm1"))
            return get(i, j);
        else if (varType.equalsIgnoreCase("fm2"))
            return get(i, j) * get(i, j);
        else if (variance != null && varType.equalsIgnoreCase("user")) {
            return variance[i][j];
        } else
            return 1.0;
    }

    public void setVar(int i, int j, double var) {
        /** If variances have not yet been defined, allocates the
         * array and initialises to one. Otherwise, it just
         * sets the appropriate variance value.
         */
        if (variance == null) {
            variance = new double[ntax + 1][ntax + 1];
            for (int ii = 0; ii < ntax; ii++)
                for (int jj = 0; jj < ntax; jj++)
                    variance[ii][jj] = 1.0;
            format.setVarType("user");
        }
        variance[i][j] = var;
    }

    /**
     * Get the max length of all the labels.
     *
     * @param taxa
     * @return longer the max length.
     */
    private int max_label_length(Taxa taxa) {
        int len;
        int longer = 0;

        for (int i = 1; i <= taxa.getNtax(); i++) {
            len = taxa.getLabel(i).length();
            if (longer < len) {
                longer = len;
            }
        }
        return longer;


    }

    /**
     * pad with white space
     *
     * @param w     the writer
     * @param taxa  the Taxa
     * @param index the index of label
     */
    private void pad(Writer w, Taxa taxa, int index) {
        try {
            int len = taxa.getLabel(index).length();
            int max = max_label_length(taxa);

            for (int i = 1; i <= (max - len + 2); i++) {
                w.write(" ");
            }
        } catch (Exception ex) {
        }
    }


    /**
     * Show the usage of distances block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN DISTANCES;");
        ps.println("\t[DIMENSIONS [NTAX=number-of-taxa];]");
        ps.println("\t[FORMAT");
        ps.println("\t    [TRIANGLE={LOWER|UPPER|BOTH}]");
        ps.println("\t    [[NO] DIAGONAL]");
        ps.println("\t    [LABELS={LEFT|NO}]");
        ps.println("\t;]");
        ps.println("\tMATRIX");
        ps.println("\t    distance data in specified format");
        ps.println("\t;");
        ps.println("END;");
    }

    /**
     * Produces a string representation of the distances object
     *
     * @return string representation
     */
    public String toString() {
        return "[Distances, ntax=" + getNtax() + "]";
    }

    /**
     * Produces a string representation of the distances object
     *
     * @return string representation
     */
    public String toString(Taxa taxa) {
        StringWriter w = new StringWriter();
        write(w, taxa);
        return w.toString();
    }

    /**
     * Write out matrix according to the specified format
     *
     * @param w    the writer
     * @param taxa the Taxa object
     */
    public void write(Writer w, Taxa taxa) {
        try {
            w.write("\nBEGIN " + Distances.NAME + ";\n");
            w.write("DIMENSIONS ntax=" + ntax + ";\n");
            w.write("FORMAT");
            if (getFormat().getLabels())
                w.write(" labels=left");
            else
                w.write(" labels=no");
            if (getFormat().getDiagonal())
                w.write(" diagonal");
            else
                w.write(" no diagonal");

            w.write(" triangle=" + getFormat().getTriangle());

            w.write(";\n");
            w.write("MATRIX\n");

            int diag = getFormat().getDiagonal() ? 0 : 1;

            for (int t = 1; t <= getNtax(); t++) {
                if (getFormat().getLabels()) {
                    w.write("[" + t + "]");
                    w.write(" '" + taxa.getLabel(t) + "'");
                    pad(w, taxa, t);

                }
                int left;
                int right;

                switch (getFormat().getTriangle()) {
                    case "lower":
                        left = 1;//1;

                        right = t - diag;//t-1+diag;

                        break;
                    case "upper":
                        left = t + diag;//t-1+diag;

                        right = getNtax();
                        for (int i = 1; i < t; i++)
                            w.write("      ");
                        break;
                    default:
// both

                        left = 1;
                        right = getNtax();
                        break;
                }

                for (int q = left; q <= right; q++) {
                    w.write(" " + (float) (get(t, q)));
                }
                w.write("\n");
            }
            w.write(";\n");
            w.write("END; [" + Distances.NAME + "]\n");
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * Read a matrics of distances.
     *
     * @param np   the nexus streamparser
     * @param taxa the taxa
     */
    public void read(NexusStreamParser np, Taxa taxa) throws IOException, SplitsException {
        np.matchBeginBlock(NAME);

        if (!isSet && np.peekMatchIgnoreCase("dimensions")) {
            np.matchIgnoreCase("dimensions ntax=" + taxa.getNtax() + ";");
        }
        ntax = taxa.getNtax();

        if (np.peekMatchIgnoreCase("FORMAT")) {
            List format = np.getTokensLowerCase("format", ";");

            getFormat().labels = np.findIgnoreCase(format, "labels=left", true, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(format, "labels=no", false, getFormat().labels); //DJB 14mar03

            // backward compatibility:
            getFormat().labels = np.findIgnoreCase(format, "no labels", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(format, "nolabels", false, getFormat().labels); //DJB 14mar03
            getFormat().labels = np.findIgnoreCase(format, "labels", true, getFormat().labels);

            if (taxa.getMustDetectLabels() && !getFormat().getLabels())
                throw new IOException("line " + np.lineno() +
                        ": 'no labels' invalid because no taxlabels given in TAXA block");

            getFormat().diagonal = np.findIgnoreCase(format, "no diagonal", false,
                    getFormat().diagonal);
            getFormat().diagonal = np.findIgnoreCase(format, "diagonal", true,
                    getFormat().diagonal);
            getFormat().diagonal = np.findIgnoreCase(format, "nodiagonal", false,
                    getFormat().diagonal); //DJB 14mar03
            getFormat().triangle = np.findIgnoreCase(format, "triangle=", "both upper lower",
                    getFormat().triangle);

            // for compatibilty with splitstree3, swallow missing=?
            np.findIgnoreCase(format, "missing=", null, '?');

            if (format.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + format + "' unexpected in FORMAT");
        }

        if (isSet) {
            np.matchIgnoreCase("end;");
            return;
        }

        np.matchIgnoreCase("MATRIX");
        matrix = new double[ntax + 1][ntax + 1];

        boolean both = getFormat().getTriangle().equals("both");
        boolean upper = getFormat().getTriangle().equals("upper");
        boolean lower = getFormat().getTriangle().equals("lower");

        int diag = getFormat().getDiagonal() ? 0 : 1;

        for (int t = 1; t <= getNtax(); t++) {
            if (taxa.getMustDetectLabels()) {
                taxa.setLabel(t, np.getLabelRespectCase());
            } else if (getFormat().getLabels())
                np.matchLabelRespectCase(taxa.getLabel(t));

            matrix[t][t] = 0;

            int left;
            int right;

            if (lower) {
                left = 1;
                right = t - diag;
            } else if (upper) {
                left = t + diag;
                right = getNtax();
            } else // both
            {
                left = 1;
                right = getNtax();
            }

            for (int q = left; q <= right; q++) {
                double z = np.getDouble();

                if (both)
                    matrix[t][q] = z;
                else
                    matrix[t][q] = matrix[q][t] = z;

            }
        }
        np.matchIgnoreCase(";");
        np.matchEndBlock();

        if (both) {
            if (!isSymmetric()) {
                symmetrize();
                new Alert("Distance matrix not symmetric: averaging upper and lower parts");
            }
        }

        isSet = true;

        if (taxa.getMustDetectLabels())
            taxa.setMustDetectLabels(false);
    }

    /**
     * gets the value of a format switch
     *
     * @param name
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        if (name.equalsIgnoreCase("diagonal"))
            return getFormat().getDiagonal();
        else if (name.equalsIgnoreCase("triangle=upper"))
            return getFormat().getTriangle().equalsIgnoreCase("upper");
        else if (name.equalsIgnoreCase("triangle=lower"))
            return getFormat().getTriangle().equalsIgnoreCase("lower");
        else if (name.equalsIgnoreCase("triangle=both"))
            return getFormat().getTriangle().equalsIgnoreCase("both");
        else
            return !name.equalsIgnoreCase("labels") || getFormat().getLabels();
    }

    /**
     * clones a distances object
     *
     * @param taxa
     * @return a clone
     */
    public Distances clone(Taxa taxa) {
        Distances distances = new Distances();
        try {
            StringWriter sw = new StringWriter();
            this.write(sw, taxa);
            StringReader sr = new StringReader(sw.toString());
            distances.read(new NexusStreamParser(sr), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return distances;
    }

    private Taxa previousTaxa;
    private Distances originalDistances;

    /**
     * return the induced object obtained by hiding taxa
     *
     * @param origTaxa
     * @param hiddenTaxa
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if ((hiddenTaxa == null || hiddenTaxa.cardinality() == 0) && originalDistances == null)
            return;   // nothing to do

        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = inducedTaxa;

        if (originalDistances == null)
            originalDistances = this.clone(origTaxa); // make a copy

        setNtax(inducedTaxa.getNtax());
        for (int s = 1; s <= origTaxa.getNtax(); s++) {
            int sNew = inducedTaxa.indexOf(origTaxa.getLabel(s));
            if (sNew > 0) {
                for (int t = 1; t <= origTaxa.getNtax(); t++) {
                    int tNew = inducedTaxa.indexOf(origTaxa.getLabel(t));
                    if (tNew > 0)
                        set(sNew, tNew, originalDistances.get(s, t));
                }
            }
        }
    }
}

// EOF
