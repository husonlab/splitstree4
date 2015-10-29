/**
 * PhyML.java
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
package splitstree4.algorithms.characters;

import jloda.util.Alert;
import jloda.util.ProgramProperties;
import jloda.util.StreamGobbler;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.externalIO.exports.ExportManager;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Calculates maximum likelihood trees from DNA or AA sequences using PHYML
 *
 * @author Daniel Huson and David Bryant, Markus Franz
 */
public class PhyML implements Characters2Trees {

    static final String PHYML_BINARY = "PHYML_BINARY";
    static final String PHYML_USRTREE = "PHYML_USRTREE";
    public final static String DESCRIPTION = "Calculates maximum likelihood trees from DNA sequences using PHYML";
    private JPanel GUIPanel;

    /* OPTIONS */

    // private String searchMode = SEARCH_MODE_M_THOROUGH;

    // private static final String SEARCH_MODE_M_THOROUGH 	= "MoreThoroughSearch";
    //  private static final String SEARCH_MODE_L_THOROUGH 	= "LessThoroughSearch";
    //  private static final String SEARCH_MODE_REARRANGE 	= "RearrangeOnOneBestTree";

    /** Data type is DNA (false if amino acid) */
    //private boolean dataTypeDNA = true;
    /** Data type is in interleaved format (false if in sequential format) */
    //private boolean dataInterleaved = true;
    /** Number of data sets */
    //private int numberOfDataSets = 1; 
    /**
     * Use non-parametric bootstrap analysis?
     */
    private boolean bootstrap = false;
    /**
     * Number of bootstrap replicates
     */
    private int numberOfReplicates = 10;
    /**
     * Print bootstrap trees and statistics)
     */
    private boolean optionPrintBootstrap = false;
    /**
     * Nucleotide substitution model
     */
    private String substitutionModel = "HKY";
    /**
     * Optimise equilibrium frequencies?
     */
    private boolean optimiseEquilibriumFrequencies = false;
    /**
     * Equilibrium frequencies empirical?
     */
    private boolean equilibriumFrequenciesEmpirical = true;
    /**
     * Nucleotide frequencies
     */

    private double fA = 0.25;
    private double fC = 0.25;
    private double fG = 0.25;
    private double fT = 0.25;

    /**
     * Custom substitution model
     */
    private int substitutionParameter_AC = 0;
    private int substitutionParameter_AG = 0;
    private int substitutionParameter_AT = 0;
    private int substitutionParameter_CG = 0;
    private int substitutionParameter_CT = 0;
    private int substitutionParameter_GT = 0;

    /**
     * Empirical base frequency estimates (false if ML estimates)
     */
    private boolean empiricalBaseFrequencyEstimates = true;
    /**
     * Ts/tv ratio fixed (false if estimated)
     */
    private boolean tstvRatioFixed = true;
    /**
     * Ts/tv ratio
     */
    private double tstvRatio = 4.0;
    /**
     * Proportion of invariable sites fixed (false if estimated)
     */
    private boolean invariableSitesProportionFixed = true;
    /**
     * Proportion of invariable sites
     */
    private double invariableSitesProportion = 0.0;
    /**
     * Use one category of substitution rate?
     */
    private boolean oneSubstitutionCategory = true;
    /**
     * Number of substitution rate categories
     */
    private int numberOfSubstitutionCategories = 4;
    /**
     * Gamma distribution parameter fixed (false if estimated)?
     */
    private boolean gammaDistributionParameterFixed = true;
    /**
     * Gamma distribution parameter
     */
    private double gammaDistributionParameter = 1.0;
    /**
     * Use BIONJ starting (input) tree
     */
    private boolean useBioNJstart = true;
    /** Path to user tree */

    /**
     * Optimise starting tree?
     */
    private boolean optimiseStart = true;
    /**
     * Optimise starting tree keeping topology?
     */
    private boolean optimiseStartKeepingTopology = false;
    /**
     * Optimise relative rate parameters?
     */
    private boolean optimiseRelativeRateParameters = false;

    /* OTHER FIELDS */

    //	 exporter (phylip sequential format)
    private final String exportFormat = "PhylipSequences";
    private static final int LINUX = 0;
    private static final int REDMOND = 1;
    private static final int MAC_OS = 2;
    private int os = 0;


    /* (non-Javadoc)
     * @see splits.algorithms.characters.CharactersTransform#isApplicable(splits.core.Document, splits.nexus.Taxa, splits.nexus.Characters)
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return doc.isValid(taxa) && doc.isValid(chars);
    }

    /* (non-Javadoc)
     * @see splits.algorithms.characters.Characters2Trees#apply(splits.nexus.Taxa, splits.nexus.Characters)
     */
    public Trees apply(Document doc, Taxa taxa, Characters chars) throws Exception {


        if (chars.hasCharweights()) {
            throw new SplitsException("PhyML is not available when there are character weights");
        }

        doc.notifyTasks("Run PHYML", getOptionPHYMLPath());
        doc.notifySetProgress(-1);

        if (chars.hasCharweights()) {
            throw new SplitsException("Phylip is not available when there are character weights");
        }

        final File phylipBin = new File(getOptionPHYMLPath());

        // do nothing if phyml_path is wrong.
        if (!phylipBin.isFile()) {
            throw new SplitsException(getClass().getName() + ": File not found: " + getOptionPHYMLPath());
        }

        // get os 
        if (System.getProperty("os.name").matches(".*[Ll]inux.*")) os = LINUX;
        if (System.getProperty("os.name").matches(".*[Ww]indows.*")) os = REDMOND;
        if (System.getProperty("os.name").matches(".*[Mm]ac.*")) os = MAC_OS;

        /* prepare files: 
   * infile, input (options), outtree */
        final String dir = System.getProperty("user.dir") + File.separator;
        // phylip infile which the sequence data will be written to (as PHYML uses phylip input files)
        final File infile = new File(dir + "infile");

        // phyML outfiles: just delete it so phyML
        // always asks the same questions.
        final File outfileLk = new File(dir + "infile_phyml_lk.txt");
        final File outfileStat = new File(dir + "infile_phyml_stat.txt");
        final File outfileBootTrees = new File(dir + "infile_phyml_boot_trees.txt");
        final File outfileBootStats = new File(dir + "infile_phyml_boot_stats.txt");
        // phylip outtree: where trees are written to in newick-format.
        final File outtree = new File(dir + "infile_phyml_tree.txt");
        // standard input is redirected to this file for phylip.
        final File st_options = new File(dir + "st_options");
        // remove existing files
        if (!removeFiles(new File[]{infile, outfileBootStats, outfileBootTrees, outfileLk, outfileStat, outtree, st_options}))
            throw new SplitsException(getClass().getName() + ": could not remove files.");

        /* write phylip-infile */
        doc.notifySubtask("exporting data");

        // taxa-names may be truncated for phylip
        Map exportName2OrigName;
        try {
            exportName2OrigName = writeInfile(doc, infile, taxa, chars);
        } catch (Exception e) {
            throw new SplitsException(getClass().getName() + ": Export failed: " + e.getMessage());
        }

        /* execute phyml */
        doc.notifySubtask("run " + getOptionPHYMLPath());
        // write phyml-options
        writeOptionFile(infile, st_options, doc);
        executePhyML(st_options, phylipBin);

        /* import computed trees */
        doc.notifySubtask("import computed trees");

        Trees trees = parseTrees(outtree, exportName2OrigName, taxa);

        /* remove files */
        doc.notifySubtask("cleaning up");
        if (!removeFiles(new File[]{infile, outfileBootStats, outfileBootTrees, outfileLk, outfileStat, outtree, st_options}))
            throw new SplitsException(getClass().getName() + ": could not remove files.");

        return trees;

    }

    /**
     * parse computed output
     *
     * @param outtree             phylip outtree-file
     * @param exportName2OrigName maps truncated taxa-names to original ones
     * @param taxa                original taxa
     * @return the parsed Trees-block
     * @throws Exception SplitsException, if import or parsing fails
     */
    private Trees parseTrees(File outtree, Map exportName2OrigName, Taxa taxa) throws Exception {

        Trees trees = new Trees();
        String nexus;

        // convert newick to nexus format
        try {
            nexus = ImportManager.importData(outtree);
        } catch (Exception e) {
            throw new SplitsException(getClass().getName() + ": import failed: " + e.getMessage());
        }

        // parse trees in nexus format
        try {
            NexusStreamParser np = new NexusStreamParser(new StringReader(nexus));
            np.matchIgnoreCase("#nexus");
            Taxa tmpTaxa = new Taxa();
            tmpTaxa.read(np);
            trees.read(np, tmpTaxa);

            // restore original taxa labels
            if (exportName2OrigName != null) {
                trees.changeNodeLabels(exportName2OrigName);
                trees.setIdentityTranslate(taxa);
            }

        } catch (Exception e) {
            throw new SplitsException(getClass().getName() + ": parsing trees failed: " + e.getMessage());
        }

        return trees;

    }


    /**
     * write chosen options in an input file for PHYML
     *
     * @param st_options the input-file
     * @throws Exception
     */
    private void writeOptionFile(File infile, File st_options, Document doc) throws Exception {

        FileWriter fw;
        try {
            fw = new FileWriter(st_options);
        } catch (IOException e) {
            throw new SplitsException(getClass().getName() + ": could not write to file: " + e.getMessage());
        }

        /* SEQUENCE FILE*/

        fw.write(infile.getAbsolutePath() + "\n");

        /* DATA TYPE */

        if (!doc.getCharacters().getFormat().getDatatype().equalsIgnoreCase("dna")) fw.write("D\n");

        /* DATA FORMAT (is always in sequential format, as the PhylipSequences exporter does this) */

        fw.write("I\n");

        /* NON PARAMETRIC BOOTSTRAP ANALYSIS */

        if (this.bootstrap) {
            fw.write("B\n");
            /* NUMBER OF REPLICATES */
            fw.write(this.getOptionNumberOfBootstrapReplicates() + "\n");
            if (this.getOptionNumberOfBootstrapReplicates() > 1) {
                /* PRINT BOOTSTRAP TREES AND STATISTICS */
                if (this.getOptionPrintBootstrap())
                    fw.write("Y\n");
                else
                    fw.write("N\n");
            }
        }
        /* SUBSTITUTION MODEL*/
        int m = -1;

        if (substitutionModel.equalsIgnoreCase("HKY85") || substitutionModel.equalsIgnoreCase("JTT")) m = 0;
        if (substitutionModel.equalsIgnoreCase("F84") || substitutionModel.equalsIgnoreCase("mtREV")) m = 1;
        if (substitutionModel.equalsIgnoreCase("TN93") || substitutionModel.equalsIgnoreCase("WAG")) m = 2;
        if (substitutionModel.equalsIgnoreCase("GTR") || substitutionModel.equalsIgnoreCase("DCMut")) m = 3;
        if (substitutionModel.equalsIgnoreCase("Custom") || substitutionModel.equalsIgnoreCase("RtREV")) m = 4;
        if (substitutionModel.equalsIgnoreCase("JC69") || substitutionModel.equalsIgnoreCase("CpREV")) m = 5;
        if (substitutionModel.equalsIgnoreCase("K2P") || substitutionModel.equalsIgnoreCase("VT")) m = 6;
        if (substitutionModel.equalsIgnoreCase("F81") || substitutionModel.equalsIgnoreCase("Blosum62")) m = 7;
        if (substitutionModel.equalsIgnoreCase("MtMam")) m = 8;
        if (substitutionModel.equalsIgnoreCase("Dayhoff")) m = 9;

        while (m > 0) {
            fw.write("M\n");
            m--;
        }

        /* BASE FREQUENCY ESTIMATE*/

        if (!this.getOptionEmpiricalBaseFrequencyEstimates()) fw.write("E\n");

        /* TS/TV RATIO */

        if (!(getOptionTstvRatioFixed() && getOptionTstvRatio() == 4.0)) {
            fw.write("T\n");
            if (!getOptionTstvRatioFixed())
                fw.write("Y\n");
            else {
                fw.write("N\n");
                fw.write(getOptionTstvRatio() + "\n");
            }
        }

        /* PROPORTION OF INVARIABLE SITES */

        if (!(getOptionInvariableSitesProportionFixed() && getOptionInvariableSitesProportion() == 0.0)) {
            fw.write("V\n");
            if (!getOptionInvariableSitesProportionFixed())
                fw.write("Y\n");
            else {
                fw.write("N\n");
                fw.write(getOptionInvariableSitesProportion() + "\n");
            }
        }

        /* NUMBER OF SUBSTITUTION RATE CATEGORIES */

        if (!getOptionOneSubstitutionCategory()) {
            fw.write("R\n");
            if (getOptionNumberOfSubstitutionCategories() != 4) {
                fw.write("C\n");
                fw.write(getOptionNumberOfSubstitutionCategories() + "\n");
            }
/* GAMMA DISTRIBUTION PARAMETER */
            if (!(getOptionGammaDistributionParameterFixed() && getOptionGammaDistributionParameter() == 2.0)) {
                fw.write("A\n");
                if (!getOptionGammaDistributionParameterFixed())
                    fw.write("Y\n");
                else {
                    fw.write("N\n");
                    fw.write(getOptionGammaDistributionParameter() + "\n");
                }
            }
        }

        /* SPECIAL CHOICES FOR CUSTOM SUBSTITUTION MODEL*/
        if (substitutionModel.equalsIgnoreCase("Custom")) {
            /* EQUILIBRIUM FREQUENCIES */
            if (getOptionOptimiseEquilibriumFrequencies()) fw.write("E\n");
            if (!getOptionEquilibriumFrequenciesEmpirical()) {
                fw.write("F\n");
                fw.write(getOptionFrequencyA() + "\n");
                fw.write(getOptionFrequencyC() + "\n");
                fw.write(getOptionFrequencyG() + "\n");
                fw.write(getOptionFrequencyT() + "\n");
            }
            /* CUSTOM MODEL */
            fw.write("K\n");
            fw.write(getOptionSubstitutionParameterAC());
            fw.write(getOptionSubstitutionParameterAG());
            fw.write(getOptionSubstitutionParameterAT());
            fw.write(getOptionSubstitutionParameterCG());
            fw.write(getOptionSubstitutionParameterCT());
            fw.write(getOptionSubstitutionParameterGT());
            fw.write("\n");
            /* OPTIMISE RELATIVE RATE PARAMETERS */
            if (getOptionOptimiseRelativeRateParameters()) fw.write("W\n");
        }

        /* INPUT TREE */
        if (!getOptionUseBioNJstart()) {
            fw.write("U\n");
            fw.write(ProgramProperties.get(PHYML_USRTREE, " ") + "\n");
        }


        fw.write("Y\n");


        fw.flush();
        fw.close();

    }

    /**
     * execute the phyml executable using Runtime.exec
     *
     * @param optionfile the input file for phylip
     * @param phylMLBin
     * @throws Exception
     */
    private void executePhyML(File optionfile, File phylMLBin) throws Exception {
        String shell = (os == REDMOND) ? "cmd.exe" : "sh";
        String readCmdStringOpt = (os == REDMOND) ? "/c" : "-c";
        String phyMLCmd = phylMLBin.getAbsolutePath()
                + " < "
                + optionfile.getAbsolutePath();
        if (phylMLBin.getAbsolutePath().contains(" "))
            new Alert("Path contains space, this won't work");

        String[] commands = new String[]
                {
                        shell,
                        readCmdStringOpt,
                        phyMLCmd
                };
        try {
            System.out.print("Executing: ");
            for (String command : commands) System.out.println("\t" + command);
            Process proc = Runtime.getRuntime().exec(commands);

// any error message?
            StreamGobbler errorGobbler = new
                    StreamGobbler(proc.getErrorStream(), "ERROR");

// any output?
            StreamGobbler outputGobbler = new
                    StreamGobbler(proc.getInputStream(), null);

// kick them off
            errorGobbler.start();
            outputGobbler.start();

            System.err.println("### Started external command ###");
            int exitVal = proc.waitFor();
            System.err.println("### Finished external command ###");
// any error???
            if (exitVal != 0)
                throw new SplitsException("Return value=" + exitVal);

        } catch (IOException e) {
            throw new SplitsException(getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Export Characters block to phyml-infile.
     *
     * @param doc    the document
     * @param infile temporary phyml-infile
     * @throws Exception
     */
    private Map writeInfile(Document doc, File infile, Taxa taxa, Characters chars) throws Exception {

        List blockToExport = new LinkedList();
        blockToExport.add(Characters.NAME);

        Document tmpDoc = new Document();
        tmpDoc.setTaxa(taxa);
        tmpDoc.setCharacters(chars);

        System.out.println("writing infile: " + infile);

        Map exportName2OrigName;

        try {
            exportName2OrigName = ExportManager.exportData(infile, false, false, exportFormat, blockToExport, tmpDoc);
        } catch (Exception e) {
            throw new SplitsException(getClass().getName() + ": export failed: " + e.getMessage());
        }
        return exportName2OrigName;
    }

    /**
     * remove Files
     *
     * @param files
     * @return false if delete failed.
     */
    private boolean removeFiles(File[] files) {
        boolean success = true;
        for (File file : files)
            if (file.exists() && !file.delete())
                success = false;
        return success;
    }

    /* (non-Javadoc)
     * @see splits.algorithms.Transformation#getVersion()
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    //
    // user-options
    //


    /**
     * path to the "PHYML" executable
     *
     * @param phymlPath
     */
    public void setOptionPHYMLPath(String phymlPath) {
        ProgramProperties.put(PHYML_BINARY, phymlPath.trim());
    }

    /**
     * path to the "PHYML" executable
     *
     * @return path to PHYML
     */
    public String getOptionPHYMLPath() {
        return ProgramProperties.get(PHYML_BINARY, "phyml_");
    }

    /**
     * path to the user starting tree
     *
     * @param treePath
     */
    public void setOptionTreePath(String treePath) {
        ProgramProperties.put(PHYML_USRTREE, treePath.trim());
    }

    /**
     * path to the "PHYML" executable
     *
     * @return path to PHYML
     */
    public String getOptionTreePath() {
        return ProgramProperties.get(PHYML_USRTREE, " ");
    }

    /**
     * Use bootstrap?
     *
     * @param bootstrap
     */
    public void setOptionBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * Use bootstrap?
     *
     * @return bootstrap
     */
    public boolean getOptionBootstrap() {
        return this.bootstrap;
    }

    /**
     * Set number of bootstrapped data sets
     *
     * @param n
     */

    public void setOptionNumberOfBootstrapReplicates(int n) {
        this.numberOfReplicates = n;
    }

    /**
     * Get number of bootstrapped data sets
     *
     * @return the number of bootstrapped data sets
     */
    public int getOptionNumberOfBootstrapReplicates() {
        return this.numberOfReplicates;
    }

    /**
     * Print bootstrap?
     *
     * @param printBootstrap
     */
    public void setOptionPrintBootstrap(boolean printBootstrap) {
        this.optionPrintBootstrap = printBootstrap;
    }

    /**
     * Print bootstrap?
     *
     * @return printBootstrap
     */
    public boolean getOptionPrintBootstrap() {
        return this.optionPrintBootstrap;
    }

    /**
     * @return Returns the substitution model.
     */
    public String getOptionSubstitutionModel() {
        return substitutionModel;
    }

    /**
     * @param substitutionModel the substitution model
     */
    public void setOptionSubstitutionModel(String substitutionModel) {
        this.substitutionModel = substitutionModel;
    }

    /**
     * @param doc the Document
     * @return a List of possible substitution models.
     */
    public List selectionOptionSubstitutionModel(Document doc) {
        List models = new LinkedList();
        if (doc.getCharacters().getFormat().getDatatype().equalsIgnoreCase("dna")) {
            models.add("HKY85");
            models.add("K2P");
            models.add("JC69");
            models.add("F81");
            models.add("F84");
            models.add("TN93");
            models.add("GTR");
            models.add("Custom");
        } else {
            models.add("JTT");
            models.add("Dayhoff");
            models.add("mtREV");
            models.add("WAG");
            models.add("DCMut");
            models.add("RtREV");
            models.add("CpREV");
            models.add("VT");
            models.add("Blosum62");
            models.add("MtMam");
        }

        return models;
    }

    /**
     * Optimise equilibrium frequencies?
     *
     * @param opt
     */
    public void setOptionOptimiseEquilibriumFrequencies(boolean opt) {
        this.optimiseEquilibriumFrequencies = opt;
    }

    /**
     * @return Optimise equilibrium frequencies?
     */
    public boolean getOptionOptimiseEquilibriumFrequencies() {
        return this.optimiseEquilibriumFrequencies;
    }

    /**
     * Equilibrium frequencies empirical?
     *
     * @param opt
     */
    public void setOptionEquilibriumFrequenciesEmpirical(boolean opt) {
        this.equilibriumFrequenciesEmpirical = opt;
    }

    /**
     * @return Equilibrium frequencies empirical?
     */
    public boolean getOptionEquilibriumFrequenciesEmpirical() {
        return this.equilibriumFrequenciesEmpirical;
    }


    /**
     * Set nucleotide frequency
     *
     * @param f for A
     */
    public void setOptionFrequencyA(double f) {
        this.fA = f;
    }

    /**
     * get nucleotide frequency
     *
     * @return frequency of A
     */
    public double getOptionFrequencyA() {
        return this.fA;
    }

    /**
     * Set nucleotide frequency
     *
     * @param f for C
     */
    public void setOptionFrequencyC(double f) {
        this.fC = f;
    }

    /**
     * get nucleotide frequency
     *
     * @return frequency of A
     */
    public double getOptionFrequencyC() {
        return this.fC;
    }

    /**
     * Set nucleotide frequency
     *
     * @param f for G
     */
    public void setOptionFrequencyG(double f) {
        this.fG = f;
    }

    /**
     * get nucleotide frequency
     *
     * @return frequency of G
     */
    public double getOptionFrequencyG() {
        return this.fG;
    }

    /**
     * Set nucleotide frequency
     *
     * @param f for T
     */
    public void setOptionFrequencyT(double f) {
        this.fT = f;
    }

    /**
     * get nucleotide frequency
     *
     * @return frequency of T
     */
    public double getOptionFrequencyT() {
        return this.fT;
    }

    /**
     * Set substitution frequency for A<->C
     *
     * @param f for A<->C
     */
    public void setOptionSubstitutionParameterAC(int f) {
        this.substitutionParameter_AC = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of A<->C
     */
    public int getOptionSubstitutionParameterAC() {
        return this.substitutionParameter_AC;
    }

    /**
     * Set substitution frequency for A<->G
     *
     * @param f for A<->G
     */
    public void setOptionSubstitutionParameterAG(int f) {
        this.substitutionParameter_AG = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of A<->G
     */
    public int getOptionSubstitutionParameterAG() {
        return this.substitutionParameter_AG;
    }

    /**
     * Set substitution frequency for A<->T
     *
     * @param f for A<->T
     */
    public void setOptionSubstitutionParameterAT(int f) {
        this.substitutionParameter_AT = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of A<->T
     */
    public int getOptionSubstitutionParameterAT() {
        return this.substitutionParameter_AT;
    }

    /**
     * Set substitution frequency for C<->G
     *
     * @param f for C<->G
     */
    public void setOptionSubstitutionParameterCG(int f) {
        this.substitutionParameter_CG = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of C<->G
     */
    public int getOptionSubstitutionParameterCG() {
        return this.substitutionParameter_CG;
    }

    /**
     * Set substitution frequency for C<->T
     *
     * @param f for C<->T
     */
    public void setOptionSubstitutionParameterCT(int f) {
        this.substitutionParameter_CT = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of C<->T
     */
    public int getOptionSubstitutionParameterCT() {
        return this.substitutionParameter_CT;
    }

    /**
     * Set substitution frequency for G<->T
     *
     * @param f for G<->T
     */
    public void setOptionSubstitutionParameterGT(int f) {
        this.substitutionParameter_GT = f;
    }

    /**
     * get substitution frequency
     *
     * @return substitution frequency of G<->T
     */
    public int getOptionSubstitutionParameterGT() {
        return this.substitutionParameter_GT;
    }

    /**
     * Use empirical base frequency estimates (false if ML estimates are used)?
     *
     * @return empirical base frequency estimates?
     */
    public boolean getOptionEmpiricalBaseFrequencyEstimates() {
        return this.empiricalBaseFrequencyEstimates;
    }

    /**
     * Use empirical base frequency estimates (false if ML estimates are used)?
     *
     * @param emp
     */
    public void setOptionEmpiricalBaseFrequencyEstimates(boolean emp) {
        this.empiricalBaseFrequencyEstimates = emp;
    }

    /**
     * @return Returns the label of the taxa to use as outgroup root.
     *//*
    public String getOptionOutgroupRoot() {
        return outgroupRoot;
    }

    *//**
     * @param outgroupRoot the label of the taxa to use as outgroup root.
     *//*
    public void setOptionOutgroupRoot(String outgroupRoot) {
        this.outgroupRoot = outgroupRoot;
    }
    
    *//**
     *
     * @param doc the document.
     * @return Returns the List of current taxa.
     *//*
    public List selectionOptionOutgroupRoot(Document doc) {
        List l = new LinkedList();
        l.add(CHOOSE_ROOT);
        l.addAll(doc.getTaxa().getAllLabels());
        return l;
    }

    
    *//**
     * path to the weightsFile
     * @param weightsFile
     *//*
    public void setOptionWeightsFile(String weightsFile) {
        Properties.put(PHYLIP_WEIGHTS_FILE,Basic.trim(weightsFile));
    }

    *//**
     * path to the weightsFile
     * @return Returns the weightsFile.
     *//*
    public String getOptionWeightsFile() {
        return Properties.get(PHYLIP_WEIGHTS_FILE,"");
    }
*/

    /**
     * @return Returns the gammaDistributionParameter.
     */
    public double getOptionGammaDistributionParameter() {
        return gammaDistributionParameter;
    }

    /**
     * @param gammaDistributionParameter The gammaDistributionParameter to set.
     */
    public void setOptionGammaDistributionParameter(double gammaDistributionParameter) {
        this.gammaDistributionParameter = gammaDistributionParameter;
    }

    /**
     * @return Returns if gammaDistributionParameterFixed.
     */
    public boolean getOptionGammaDistributionParameterFixed() {
        return gammaDistributionParameterFixed;
    }

    /**
     * @param gammaDistributionParameterFixed
     *         The gammaDistributionParameterFixed to set.
     */
    public void setOptionGammaDistributionParameterFixed(boolean gammaDistributionParameterFixed) {
        this.gammaDistributionParameterFixed = gammaDistributionParameterFixed;
    }

    /**
     * @return Returns the invariableSitesProportion.
     */
    public double getOptionInvariableSitesProportion() {
        return invariableSitesProportion;
    }

    /**
     * @param invariableSitesProportion The invariableSitesProportion to set.
     */
    public void setOptionInvariableSitesProportion(double invariableSitesProportion) {
        this.invariableSitesProportion = invariableSitesProportion;
    }

    /**
     * @return Returns the invariableSitesProportionFixed.
     */
    public boolean getOptionInvariableSitesProportionFixed() {
        return invariableSitesProportionFixed;
    }

    /**
     * @param invariableSitesProportionFixed The invariableSitesProportionFixed to set.
     */
    public void setOptionInvariableSitesProportionFixed(boolean invariableSitesProportionFixed) {
        this.invariableSitesProportionFixed = invariableSitesProportionFixed;
    }

    /**
     * @return Returns the numberOfSubstitutionCategories.
     */
    public int getOptionNumberOfSubstitutionCategories() {
        return numberOfSubstitutionCategories;
    }

    /**
     * @param numberOfSubstitutionCategories The numberOfSubstitutionCategories to set.
     */
    public void setOptionNumberOfSubstitutionCategories(int numberOfSubstitutionCategories) {
        this.numberOfSubstitutionCategories = numberOfSubstitutionCategories;
    }

    /**
     * @return Returns the oneSubstitutionCategory.
     */
    public boolean getOptionOneSubstitutionCategory() {
        return oneSubstitutionCategory;
    }

    /**
     * @param oneSubstitutionCategory The oneSubstitutionCategory to set.
     */
    public void setOptionOneSubstitutionCategory(boolean oneSubstitutionCategory) {
        this.oneSubstitutionCategory = oneSubstitutionCategory;
    }

    /**
     * @return Returns the optimiseStart.
     */
    public boolean getOptionOptimiseStart() {
        return optimiseStart;
    }

    /**
     * @param optimiseStart The optimiseStart to set.
     */
    public void setOptionOptimiseStart(boolean optimiseStart) {
        this.optimiseStart = optimiseStart;
    }

    /**
     * @return Returns the optimiseStartKeepingTopology.
     */
    public boolean getOptionOptimiseStartKeepingTopology() {
        return optimiseStartKeepingTopology;
    }

    /**
     * @param optimiseStartKeepingTopology The optimiseStartKeepingTopology to set.
     */
    public void setOptionOptimiseStartKeepingTopology(boolean optimiseStartKeepingTopology) {
        this.optimiseStartKeepingTopology = optimiseStartKeepingTopology;
    }

    /**
     * @return Returns the tstvRatio.
     */
    public double getOptionTstvRatio() {
        return tstvRatio;
    }

    /**
     * @param tstvRatio The tstvRatio to set.
     */
    public void setOptionTstvRatio(double tstvRatio) {
        this.tstvRatio = tstvRatio;
    }

    /**
     * @return Returns the tstvRatioFixed.
     */
    public boolean getOptionTstvRatioFixed() {
        return tstvRatioFixed;
    }

    /**
     * @param tstvRatioFixed The tstvRatioFixed to set.
     */
    public void setOptionTstvRatioFixed(boolean tstvRatioFixed) {
        this.tstvRatioFixed = tstvRatioFixed;
    }

    /**
     * @return Returns the useBioNJstart.
     */
    public boolean getOptionUseBioNJstart() {
        return useBioNJstart;
    }

    /**
     * @param useBioNJstart The useBioNJstart to set.
     */
    public void setOptionUseBioNJstart(boolean useBioNJstart) {
        this.useBioNJstart = useBioNJstart;
    }

    /**
     * @return Returns the optimiseRelativeRateParameters.
     */
    public boolean getOptionOptimiseRelativeRateParameters() {
        return optimiseRelativeRateParameters;
    }

    /**
     * @param optimiseRelativeRateParameters The optimiseRelativeRateParameters to set.
     */
    public void setOptionOptimiseRelativeRateParameters(boolean optimiseRelativeRateParameters) {
        this.optimiseRelativeRateParameters = optimiseRelativeRateParameters;
    }
}
