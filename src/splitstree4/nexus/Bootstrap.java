/**
 * Bootstrap.java
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
package splitstree4.nexus;

import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.simulate.RandomCharacters;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.gui.Director;
import splitstree4.models.SubstitutionModel;
import splitstree4.util.CharactersUtilities;
import splitstree4.util.SplitMatrix;
import splitstree4.util.SplitMatrixAnalysis;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Random;

/**
 * The nexus boostrap block
 * Contains boostrap information
 */
public class Bootstrap extends NexusBlock {

    /**
     * the subclass Format
     */
    public final class Format {
        // labels
        private boolean labels;
        // List the splits, or just the bootstrap values. This has to be true if there are splits not present in the
        // documents splits block. Perhaps it should always be true, since otherwise we'd lose information.
        private boolean showSplits;
        //??
        private boolean all;

        /*the constructor of Format */
        public Format() {
            labels = false;
            showSplits = true;
            all = true;
        }

        /**
         * Gets the value of labels
         *
         * @return labels
         */
        public boolean getLabels() {
            return labels;
        }

        /**
         * Sets the value of labels
         *
         * @param lab
         */
        public void setLabels(boolean lab) {
            this.labels = lab;
        }

        /**
         * Gets the value of show splits
         *
         * @return showSplits
         */
        public boolean getShowSplits() {
            return showSplits;
        }

        /**
         * Sets the value of show splits
         *
         * @param s
         */
        public void setShowSplits(boolean s) {
            this.showSplits = s;
        }

        /**
         * Gets the value of all
         *
         * @return all
         */
        public boolean getAll() {
            return all;
        }

        /**
         * Sets the value of all
         *
         * @param all
         */
        public void setAll(boolean all) {
            this.all = all;
        }
    }

    /**
     * Identification string
     */
    final public static String NAME = "st_Bootstrap";
    /**
     * The number of runs for the bootstrap
     */
    private int runs;
    /**
     * The length of the new sequences
     */
    private int length;
    /**
     * the random seed
     */
    private int seed;
    /**
     * The number of taxa
     */
    private int ntax;
    /**
     * The number of characters in the original Sequences
     */
    private int nchar;
    /**
     * The number of splits in bsplits
     */
    private int nsplits;

    /**
     * Internal flag indicating that the user should be permitted to save trees if they want to.
     */
    private boolean canSaveTrees;

    /**
     * filename of files to save bootstrap replicate trees.
     */
    private boolean saveTrees;

    /**
     * the format subclass
     */
    private Format format = null;
    /**
     * The Splits which occur in the bootstrap process
     */
    private Splits bsplits;

    /**
     * SplitMatrix containing al replicate information.
     */
    private SplitMatrix splitMatrix;

    /**
     * Number of original splits
     */
    private int nOriginalSplits;


    /**
     * Constructor
     */
    public Bootstrap() {

        bsplits = null;
        runs = 0;
        length = -1;
        seed = 0;
        ntax = 0;
        nchar = 0;
        format = new Format();
        nsplits = 0;
        saveTrees = false; //DEBUG
        canSaveTrees = false;
        splitMatrix = null;

    }

    /**
     * Constructor
     */
    public Bootstrap(Document doc) {

        bsplits = null;
        runs = 0;
        length = -1;
        seed = 1;
        if (doc.getTaxa() != null)
            ntax = doc.getTaxa().getNtax();
        else
            ntax = 0;
        if (doc.getCharacters() != null)
            nchar = doc.getCharacters().getNchar();
        else
            nchar = 0;
        format = new Format();
        nsplits = 0;
        saveTrees = false;
        canSaveTrees = false;
        splitMatrix = null;
    }

    // public Methods

    /**
     * the subclass to count ids
     *
     * @deprecated
     */
    private class IdCount {
        public int id;
        public int count;

        public IdCount() {
            id = 0;
            count = 0;
        }

        public IdCount(int id, int count) {
            this.id = id;
            this.count = count;
        }

        public void addOne() {
            count++;
        }
    }

    /**
     * Produce a new characters block by resampling original.
     *
     * @param taxa     the taxa
     * @param original characters block to resample from
     * @param rand     random number generator
     * @return Characters    Characters block with same formatting as original, but resampled.
     * @throws SplitsException If  characters are diploid and # characters resampled is not even
     */
    protected Characters resample(Taxa taxa, Characters original, Random rand) {
        Characters result;
        try {
            result = CharactersUtilities.resample(taxa, original, getLength(), rand);
        } catch (SplitsException ex) {
            result = null;
        }
        return result;
    }


    /**
     * Computes the boostrap analysis given the document
     *
     * @param doc
     * @throws IOException
     * @throws CanceledException
     * @throws SplitsException
     */
    public void compute(Document doc) throws IOException, CanceledException, SplitsException {

        /* Create a new document that will be the bootstrap replicate document  */
        Document bdoc = new Document();
        bdoc.setTaxa((Taxa) doc.getTaxa().clone());
        bdoc.setAssumptions(doc.getAssumptions().clone(bdoc.getTaxa()));
        bdoc.getAssumptions().setExTaxa(null);
        bdoc.setInBootstrap(true);

        doc.notifySetMaximumProgress(this.getRuns());
        doc.notifyTasks("Bootstrapping", "runs=" + this.getRuns());

        /* Initialise the SplitMatrix that stores the replicates */
        //SplitMatrix splitMatrix = new SplitMatrix(doc.getSplits());
        SplitMatrix splitMatrix = new SplitMatrix(doc.getTaxa().getNtax(), doc.getSplits());
        nOriginalSplits = doc.getSplits().getNsplits();

        int len = getLength();   //ToDo: remove the length parameter.
        if (len < 0) len = getNchar();
        setLength(len);

        // resample characters from original characters block:
        Random rand = new Random();
        if (getSeed() != 0)
            rand.setSeed(getSeed());

        // Store trees if the user has specified a treefilename */
        Trees bootstrapTrees = new Trees();

        int r = 1;
        PrintStream ps = null;
        try {
            for (r = 1; r <= this.getRuns(); r++) {
                //Sample the characters
                bdoc.setCharacters(resample(bdoc.getTaxa(), doc.getCharacters(), rand));
                //bdoc.getCharacters().setFormat(doc.getCharacters().getFormat());
                ps = jloda.util.Basic.hideSystemErr();//disable syserr.
                // Compute everything
                bdoc.update();

                //store the first tree if we are storing these.
                if (getSaveTrees() && bdoc.getTrees() != null && bdoc.getTrees().getNtrees() > 0)
                    bootstrapTrees.addTree("bootstrap_" + r, bdoc.getTrees().getTree(1), bdoc.getTaxa());

                jloda.util.Basic.restoreSystemErr(ps);   //enable syserr
                splitMatrix.add(bdoc.getSplits());  //Store the splits recovered.
                doc.notifySetProgress(r); //Move the progress bar and check for cancellation
            }
        } catch (CanceledException ex) {
            String message = "Bootstrap cancelled: only " + r + " bootstrap replicates stored";
            new Alert(message);
        } catch (OutOfMemoryError ex) {
            String message = "Out of memory error: only " + (r - 1) + " bootstraps performed";
            new Alert(message);
        } catch (Exception ex) {
            Basic.caught(ex);
            throw new SplitsException("Bootstrapping failed: " + ex);
        } finally {
            jloda.util.Basic.restoreSystemErr(ps);
        }

        //First we compute the bootstrap support for all of the present splits
        SplitMatrixAnalysis.evalConfidences(splitMatrix, doc.getSplits());
        SplitMatrixAnalysis.getConfidenceIntervals(splitMatrix, doc.getSplits(), 0.95);
        doc.getSplits().getFormat().setConfidences(true);
        //       doc.getSplits().getFormat().setIntervals(true);

        //ToDo: add filter to remove splits with zero weight.


        bsplits = splitMatrix.getSplits();
        nsplits = bsplits.getNsplits();

        SplitMatrixAnalysis.evalConfidences(splitMatrix, bsplits);
        SplitMatrixAnalysis.computePercentages(bsplits);

        setSplitMatrix(splitMatrix);

        //Save the bootstrap trees stored.
        //TODO: Make a generic utility for spawning a new document
        if (getSaveTrees() && bootstrapTrees.getNtrees() == this.getRuns()) {
            StringWriter sw = new StringWriter();
            doc.getTaxa().write(sw);
            bootstrapTrees.write(sw, doc.getTaxa());
            Director newDir = Director.newProject(sw.toString(), doc.getFile().getAbsolutePath());
            newDir.getDocument().setTitle("Bootstrap trees for " + doc.getTitle());
            newDir.showMainViewer();
        }


    }

    /**
     * Performs a parametric bootstrap, generating replicate alignments on the tree T with model M.
     *
     * @param doc
     * @param T
     * @param M
     * @throws IOException
     * @throws CanceledException
     * @throws SplitsException
     */
    //ToDo: Add interface, remove parallel code with non-parametric bootstrap.
    public void computeParametric(Document doc, PaupNode T, SubstitutionModel M) throws CanceledException, SplitsException {
        /* Create a new document that will be the bootstrap replicate document  */
        Document bdoc = new Document();
        bdoc.setTaxa((Taxa) doc.getTaxa().clone());
        bdoc.setAssumptions(doc.getAssumptions().clone(bdoc.getTaxa()));
        bdoc.getAssumptions().setExTaxa(null);
        bdoc.setInBootstrap(true);

        /* Initialise the SplitMatrix that stores the replicates */
        SplitMatrix splitMatrix = new SplitMatrix(doc.getTaxa().getNtax());

        doc.notifySetMaximumProgress(this.getRuns());
        doc.notifyTasks("Parametric Bootstrapping", "runs=" + this.getRuns());

        int len = getLength();   //ToDo: remove the length parameter.
        if (len < 0) len = getNchar();
        setLength(len);

        // resample characters from original characters block:
        Random rand = new Random(getSeed());

        Characters newChars = new Characters(doc.getTaxa().getNtax(), doc.getCharacters().getNchar(), doc.getCharacters().getFormat());
        bdoc.setCharacters(newChars);

        for (int r = 1; r <= this.getRuns(); r++) {

            RandomCharacters.simulateCharacters(bdoc.getCharacters(), T, M);

            PrintStream ps = jloda.util.Basic.hideSystemErr();//disable syserr.
            // Update the doc Parser
            try {
                bdoc.update();
            } catch (Exception ex) {
                Basic.caught(ex);
                throw new SplitsException("Bootstrapping failed: " + ex);
            } finally {
                jloda.util.Basic.restoreSystemErr(ps);
            }

            try {
                splitMatrix.add(bdoc.getSplits());
            } catch (OutOfMemoryError ex) {
                int rr = r - 1;
                String message = "Out of memory error: only " + rr + " bootstraps performed";
                new Alert(message);
            }

            try {
                doc.notifySetProgress(r);
            } catch (CanceledException ex) {
                String message = "Bootstrap cancelled: only " + r + " bootstrap replicates stored";
                new Alert(message);
                break;
            }
        }


        SplitMatrixAnalysis.evalConfidences(splitMatrix, doc.getSplits());
        doc.getSplits().getFormat().setConfidences(true);

        //ToDo: add filter to remove splits with zero weight.
        bsplits = splitMatrix.getSplits();
        nsplits = bsplits.getNsplits();

        SplitMatrixAnalysis.evalConfidences(splitMatrix, bsplits);
        SplitMatrixAnalysis.computePercentages(bsplits);

        setSplitMatrix(splitMatrix);
    }

    /**
     * private methods
     */

    /**
     * Count the splits  in a Set
     *
     * @param s             The Splits Set
     * @param occuredSplits
     */

    /*private void countsplits(Splits s, Map occuredSplits, int r, Splits osplits) {

        int n = s.getNsplits();
        if (saveReplicates) {
            bs_weights[r] = new Vector(bsplits.getNsplits()+1);
            bs_weights[r].setSize(bsplits.getNsplits()+1);
        }

        double dist = 0.0;

        for (int i = 1; i <= n; i++) {
            TaxaSet sp = s.get(i);
            if (occuredSplits.containsKey(sp.toString())) {
                IdCount idCount = (IdCount) occuredSplits.get(sp.toString());
                idCount.addOne();
                occuredSplits.put(sp.toString(), idCount);
                if (saveReplicates)  {
                    if (saveweights)
                      this.bs_weights[r].set(idCount.id,new Double(s.getWeight(i)));
                    else
                      this.bs_weights[r].set(idCount.id,new Double(1.0));
                }
                if (computeDistr) {
                    double x;
                    if (idCount.id<=osplits.getNsplits())
                        x = s.getWeight(i) - osplits.getWeight(idCount.id);
                    else
                        x = s.getWeight(i);
                    dist += x*x;
                }
            }
            else {
                this.bsplits.add(sp);
                IdCount idCount = new IdCount(bsplits.getNsplits(),1);
                occuredSplits.put(sp.toString(),idCount);
                if (saveReplicates) {
                if (saveweights)
                    bs_weights[r].add(new Double(s.getWeight(i)));
                else
                    bs_weights[r].add(new Double(1.0));
                }
                if (computeDistr)
                    dist+= (s.getWeight(i)*s.getWeight(i));
            }
        }



    }*/

    /**
     * Getter
     */

    /**
     * Return the format object
     *
     * @return the format object
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Gets the number of cycles
     *
     * @return the number of runs
     */
    public int getRuns() {
        return this.runs;
    }

    /**
     * Gets the random seed
     *
     * @return the random seed
     */
    public int getSeed() {
        return this.seed;
    }

    /**
     * Gets the split matrix.
     *
     * @return the matrix of all split weights over all replicates
     */
    public SplitMatrix getSplitMatrix() {
        return this.splitMatrix;
    }

    /**
     * Gets the number of characters
     *
     * @return the number of characters
     */
    public int getNchar() {
        return this.nchar;
    }

    /**
     * Gets the ntax
     *
     * @return ntax the number of taxa
     */
    public int getNtax() {
        return this.ntax;
    }

    /**
     * Gets the nsplits
     *
     * @return nsplits the number of splits
     */
    public int getNsplits() {
        return nsplits;
    }

    /**
     * Gets the number of resampled characters
     *
     * @return length
     */

    public int getLength() {
        return length;
    }

    /**
     * Gets the bootstrap splits
     *
     * @return bsplits
     */
    public Splits getBsplits() {
        return bsplits;
    }


    /**
     * Get the flag indicating whether the user should have the option to save trees
     *
     * @return boolean flag indicating whether the user should have the option to save trees
     */
    public boolean getCanSaveTrees() {
        return canSaveTrees;
    }

    /**
     * Get the flag indicating whether the user should have the option to save trees
     *
     * @param canSavetrees flag indicating whether the user should have the option to save trees
     */
    public void setCanSaveTrees(boolean canSavetrees) {
        this.canSaveTrees = canSavetrees;
    }


    /**
     * Get the flag indicating whether a new document should be opened with the bootstrap trees
     *
     * @return boolean flag indicating whether a new document should be opened with the bootstrap trees.
     */
    public boolean getSaveTrees() {
        return saveTrees;
    }

    /**
     * Get the flag indicating whether a new document should be opened with the bootstrap trees
     *
     * @param savetrees flag indicating whether a new document should be opened with the bootstrap trees.
     */
    public void setSaveTrees(boolean savetrees) {
        this.saveTrees = savetrees;
    }


    /**
     * Sets the number of cycles
     *
     * @param n the number of runs
     */
    public void setRuns(int n) {
        this.runs = n;
    }

    /**
     * Sets the random seed
     *
     * @param n the random seed
     */
    public void setSeed(int n) {
        this.seed = n;
    }

    /**
     * Sets the split matrix
     *
     * @param splitMatrix
     */
    public void setSplitMatrix(SplitMatrix splitMatrix) {
        this.splitMatrix = splitMatrix;
    }

    /**
     * Sets the number of characters
     *
     * @param n the number of characters
     */
    public void setNchar(int n) {
        this.nchar = n;
    }

    /**
     * Sets the ntax
     *
     * @param n
     */
    public void setNtax(int n) {
        this.ntax = n;
    }

    /**
     * Sets the nsplits
     *
     * @param n
     */
    public void setNsplits(int n) {
        this.nsplits = n;
    }

    /*Sets the length
   *@param len
   */
    public void setLength(int len) {
        this.length = len;
    }

    /**
     * Sets the bootstrap splits
     *
     * @param bsplits the splits computed by bootstrapping
     */
    public void setBsplits(Splits bsplits) {
        this.bsplits = bsplits;
    }

    /**
     * Usage
     */

    /**
     * Show the usage of bootstrap.
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN ST_BOOTSTRAP;");
        ps.println("\t[DIMENSIONS [NTAX=number-of-taxa]" +
                " [NCHAR=number-of-characters]\n\t\t[NSPLITS=number-of-splits];]");
        ps.println("\t[FORMAT");
        ps.println("\t    [LABELS={LEFT|NO}]");
        ps.println("\t    [SPLITS={NO|YES}]");
        ps.println("\t    [ALL={YES|NO}]");
        ps.println("\t;]");
        ps.println("\t[RUNS=the-number-of-runs;]");
        ps.println("\t[LENGTH={sample-length | SAME}];");
        ps.println("\t[SEED=random-number-seed;]");
        ps.println("\t[MATRIX");
        ps.println("\t    [label_1]  value_1  [split_1,]");
        ps.println("\t    [label_2]  value_2  [split_2,]");
        ps.println("\t    ....");
        ps.println("\t    [label_nsplits]    value_nsplits  [split_nsplits,]");
        ps.println("\t    [label_nsplits+1]  value_(nsplits+1)  [splits_(nsplits+1),] ");
        ps.println("\t    ....");
        ps.println("\t    [label_n]  value_n  [splits_n,]");
        ps.println("\t;]");
        ps.println("END;");

    }

    /**
     * IO Handling
     */


    /**
     * Read the st_bootstrap
     *
     * @param np   NexusStreamParser
     * @param taxa Taxa
     */
    public void read(NexusStreamParser np, Taxa taxa, Characters characters,
                     Splits splits)
            throws IOException {
        if (taxa == null)
            throw new IOException("line " + np.lineno() +
                    ": ST_BOOTSTRAP block must be preceded by valid TAXA block");
        if (characters == null)
            throw new IOException("line " + np.lineno() +
                    ": ST_BOOTSTRAP block must be preceded by valid CHARACTERS block");
        if (splits == null)
            throw new IOException("line " + np.lineno() +
                    ": ST_BOOTSTRAP block must be preceded by valid ST_SPLITS block");

        np.matchBeginBlock(NAME);

        if (np.peekMatchIgnoreCase("DIMENSIONS")) {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax=")) {
                np.matchIgnoreCase("ntax=");
                setNtax(np.getInt());
                if (ntax != taxa.getNtax()) {
                    throw new IOException("line " + np.lineno() +
                            ": wrong number of taxa: " + ntax +
                            "; Expected: " + taxa.getNtax());
                    //setNtax(taxa.getNtax());
                }

            }
            if (np.peekMatchIgnoreCase("nchar=")) {
                np.matchIgnoreCase("nchar=");
                setNchar(np.getInt());
                if (nchar != characters.getNchar()) {
                    throw new IOException("line " + np.lineno() +
                            ": wrong number of characters: " + nchar +
                            "; Expected: " + characters.getNchar());
                    //setNchar(characters.getNchar());

                }
            }
            if (np.peekMatchIgnoreCase("nsplits=")) {
                np.matchIgnoreCase("nsplits=");
                setNsplits(np.getInt());
                /*
                if (nsplits != splits.getNsplits()) {
                    throw new IOException("line " + np.lineno() +
                            ": wrong number of splits: " + nsplits +
                            "; Expected: " + splits.getNsplits());
                }
                */
            }
            np.matchIgnoreCase(";");
        }

        //We need to store the number of splits in the original block, which we hope is the same
        //as is being read in in the bootstrap block.
        //ToDO: Potential problem here! Maybe we should store this, or check that the splits in the
        //matrix correspond to splits in the document, or allow a flag indicating that we do not
        //store more splits than in the original block.
        nOriginalSplits = splits.getNsplits();


        if (np.peekMatchIgnoreCase("FORMAT")) {
            List tokens = np.getTokensLowerCase("format", ";");

            getFormat().labels = np.findIgnoreCase(tokens, "labels=left", true, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels=no", false, getFormat().labels);

            getFormat().showSplits = np.findIgnoreCase(tokens, "splits=yes", true, getFormat().showSplits);
            getFormat().showSplits = np.findIgnoreCase(tokens, "splits=no", false, getFormat().showSplits);

            getFormat().all = np.findIgnoreCase(tokens, "all=yes", true, getFormat().all);
            getFormat().all = np.findIgnoreCase(tokens, "all=no", false, getFormat().all);

            // the following so that we can read old files:
            getFormat().labels = np.findIgnoreCase(tokens, "no labels", false, getFormat().labels);
            getFormat().labels = np.findIgnoreCase(tokens, "labels", true, getFormat().labels);

            getFormat().showSplits = np.findIgnoreCase(tokens, "no splits", false, getFormat().showSplits);
            getFormat().showSplits = np.findIgnoreCase(tokens, "splitstree4", true, getFormat().showSplits);

            getFormat().all = np.findIgnoreCase(tokens, "no all", false, getFormat().all);
            getFormat().all = np.findIgnoreCase(tokens, "all", true, getFormat().all);
        }

        if (np.peekMatchIgnoreCase("end;") && bsplits != null) {
            np.matchIgnoreCase("end;");
            return;
        }

        if (np.peekMatchIgnoreCase("RUNS=")) {
            np.matchIgnoreCase("runs=");
            setRuns(np.getInt());
            np.matchIgnoreCase(";");
        }
        if (np.peekMatchIgnoreCase("LENGTH=")) {
            np.matchIgnoreCase("length=");
            if (np.peekMatchIgnoreCase("SAME")) {
                np.matchIgnoreCase("SAME");
                setLength(-1);
            } else
                this.setLength(np.getInt());

            np.matchIgnoreCase(";");
        }
        if (np.peekMatchIgnoreCase("SEED=")) {
            np.matchIgnoreCase("seed=");
            setSeed(np.getInt());
            np.matchIgnoreCase(";");
        }


        if (bsplits != null)
            bsplits.clear();
        else
            bsplits = new Splits();

        if (np.peekMatchIgnoreCase("MATRIX")) {
            np.matchIgnoreCase("MATRIX");
            readMatrix(np, splits);
            np.matchIgnoreCase(";");
        }
        np.matchEndBlock();
    }


    /**
     * Read a matrix from a NexusStreamParser
     *
     * @param np     the NexusStreamParser to read from.
     * @param splits the set of splits
     * @throws IOException
     */
    private void readMatrix(NexusStreamParser np, Splits splits) throws IOException {
        int i = 0; // number of split
        while (!np.peekMatchIgnoreCase(";")) {
            String label = null;

            i++;

            if (getFormat().getLabels()) {
                label = np.getWordRespectCase();
                if (i <= splits.getNsplits() && !label.equals(splits.getLabel(i)))
                    throw new IOException("line " + np.lineno() + ": illegal label: "
                            + label);
            }

            float value = new Float(np.getWordRespectCase());

            if (getFormat().getShowSplits()) {
                TaxaSet ts = new TaxaSet();
                while (!np.peekMatchIgnoreCase(",")) {
                    // @todo check if we really want this to be a int
                    ts.set((int) (new Float(np.getWordRespectCase())).floatValue());
                }
                np.matchIgnoreCase(",");
                this.bsplits.add(ts, value, label);
            } else if (i <= splits.getNsplits()) {
                this.bsplits.add(splits.get(i), value, label);
            } else
                throw new IOException("line " + np.lineno() + ": too many splits");
        }
        if (i < splits.getNsplits())
            throw new IOException("line " + np.lineno() + ": too few splits");
    }


    /**
     * write a matrix to the given writer
     *
     * @param w    The writer to which the matrix should be written to.
     * @param taxa
     * @throws java.io.IOException
     */
    public void write(Writer w, Taxa taxa)
            throws java.io.IOException {
        w.write("\nBEGIN st_bootstrap;\n");
        // w.write("DIMENSIONS ntax=" + taxa.getNtax() + " nchar=" +
        //        characters.getNchar() + " nsplits=" + bsplits.getNsplits() + ";\n");


        w.write("DIMENSIONS ntax=" + taxa.getNtax() + " nchar=" +
                getNchar() + " nsplits=" + bsplits.getNsplits() + ";\n");


        w.write("\tFORMAT");
        if (getFormat().getLabels())
            w.write(" labels=left");
        else
            w.write(" labels=no");
        if (getFormat().getShowSplits())
            w.write(" splits=yes");
        else
            w.write(" no splits=no");
        if (getFormat().getAll())
            w.write(" all=yes");
        else
            w.write(" all=no");
        w.write(";\n");
        w.write("\tRUNS=" + this.getRuns() + ";\n");
        w.write("\tLENGTH=");
        if (this.getLength() == -1)
            w.write("SAME");
        else
            w.write(Integer.toString(this.getLength()));
        w.write(";\n");
        if (getSeed() != 0)
            w.write("\tSEED=" + this.getSeed() + ";\n");
        w.write("MATRIX \n");
        for (int i = 1; i <= bsplits.getNsplits(); i++) {
            w.write("[" + i + "]\t");
            if (getFormat().getLabels())
                w.write((bsplits.getLabel(i)) + "\t");
            w.write(bsplits.getWeight(i) + "\t");
            if (getFormat().getShowSplits())
                w.write(" " + bsplits.get(i) + ",");
            w.write("\n");
            // draw separating line between original splits and new ones
            if (i > 1 && i == nOriginalSplits && i < bsplits.getNsplits()
                    && getFormat().getAll()) {
                w.write
                        ("[--------------------------------------------------]\n");
            }
            if (!getFormat().getAll() && i == nOriginalSplits)
                break;
        }
        w.write(";\n");
        w.write("END; [" + Bootstrap.NAME + "]\n");
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
        else if (name.equalsIgnoreCase("splitstree4"))
            return getFormat().getShowSplits();
        else
            return !name.equalsIgnoreCase("all") || getFormat().getAll();
    }
}

// EOF
