/**
 * PhylipParsimony.java
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

import jloda.swing.util.Alert;
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
 * Calculates most parsimonious trees from DNA sequences using PhylipSequences
 *
 * @author Daniel Huson and David Bryant, Michael Schroeder
 * @version $Id: PhylipParsimony.java,v 1.31 2007-09-11 12:31:02 kloepper Exp $
 */
public class PhylipParsimony implements Characters2Trees {
    // public final boolean EXPERT = true;
    static final String PHYLIP_DNAPARS = "PHYLIP_DNAPARS";
    static final String PHYLIP_WEIGHTS_FILE = "PHYLIP_WEIGHTS_FILE";
    public final static String DESCRIPTION = "Compute most parsimonious trees from DNA sequences using Phylip";
    private JPanel GUIPanel;

    /* OPTIONS */

    /**
     * Search mode:
     * <ul>
     * <li>"More thorough search"</li>
     * <li>"Less thorough search"</li>
     * <li>"Rearrange on one best tree"</li>
     * </ul>
     */
    private String searchMode = SEARCH_MODE_M_THOROUGH;

    private static final String SEARCH_MODE_M_THOROUGH = "MoreThoroughSearch";
    private static final String SEARCH_MODE_L_THOROUGH = "LessThoroughSearch";
    private static final String SEARCH_MODE_REARRANGE = "RearrangeOnOneBestTree";

    /**
     * Number of trees to save
     */
    private int numberOfTreesToSave = 10000;
    /**
     * Seed for randomizing input order
     */
    private int randomizeInputOrderSeed = 0;
    /**
     * Number of times to jumble input order
     */
    private int randomizeInputOrderJumbles = 0;
    /**
     * Outgroup root?
     */
    private static final String CHOOSE_ROOT = "choose root..";
    private String outgroupRoot = CHOOSE_ROOT;
    /**
     * Use Threshold parsimony?
     */
    private double thresholdParsimony = 0.0;
    /**
     * Use Transversion parsimony?
     */
    private boolean tranversionParsimony = false;

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
        return doc.isValid(taxa) && doc.isValid(chars) &&
                (chars.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.DNA)
                        || chars.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.DNA));
    }

    /* (non-Javadoc)
     * @see splits.algorithms.characters.Characters2Trees#apply(splits.nexus.Taxa, splits.nexus.Characters)
     */
    public Trees apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        doc.notifyTasks("Run Phylip", getOptionPhylipPath());
        doc.notifySetProgress(-1);

        final File phylipBin = new File(getOptionPhylipPath());

        // do nothing if phylip_path is wrong.
        if (!phylipBin.isFile()) {
            throw new SplitsException("Program not found: " + getOptionPhylipPath());
        }

        // get os 
        if (System.getProperty("os.name").matches(".*[Ll]inux.*")) os = LINUX;
        if (System.getProperty("os.name").matches(".*[Ww]indows.*")) os = REDMOND;
        if (System.getProperty("os.name").matches(".*[Mm]ac.*")) os = MAC_OS;

        /* prepare files: 
   * infile, input (options), outtree */
        final String dir = System.getProperty("user.dir") + File.separator;
        // phylip infile which the sequence data will be written to
        final File infile = new File(dir + "infile");
        // phylip outfile: just delete it so phylip
        // always asks the same questions.
        final File outfile = new File(dir + "outfile");
        // phylip outtree: where trees are written to in newick-format.
        final File outtree = new File(dir + "outtree");
        // standard input is redirected to this file for phylip.
        final File st_options = new File(dir + "st_options");
        // remove existing files
        if (!removeFiles(new File[]{infile, outfile, outtree, st_options}))
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

        /* execute phylip */
        doc.notifySubtask("run " + getOptionPhylipPath());
        // write phylip-options
        writeOptionFile(st_options, doc);
        executePhylip(st_options, phylipBin);

        /* import computed trees */
        doc.notifySubtask("import computed trees");
        Trees trees = parseTrees(outtree, exportName2OrigName, taxa);

        /* remove files */
        doc.notifySubtask("cleaning up");
        if (!removeFiles(new File[]{infile, outfile, outtree, st_options}))
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
     * write chosen options in an input file for phylip
     *
     * @param st_options the input-file
     * @throws Exception
     */
    private void writeOptionFile(File st_options, Document doc) throws Exception {

        FileWriter fw;
        try {
            fw = new FileWriter(st_options);
        } catch (IOException e) {
            throw new SplitsException(getClass().getName() + ": could not write to file: " + e.getMessage());
        }

        /* USER TREE */
//		if(!getOptionUserTree().equals("")) {
//			
//			fw.write("U\n");
//		}

        /* SEARCH MODE */
        if (!getOptionSearchMode().equals(PhylipParsimony.SEARCH_MODE_M_THOROUGH)) {
            fw.write("S\n");
            if (getOptionSearchMode().equals(PhylipParsimony.SEARCH_MODE_REARRANGE))
                fw.write("Y\n");
            else
                fw.write("N\n");
        }

        /* NUMBER OF TREES TO SAVE */
        fw.write("V\n");
        fw.write(getOptionNumberOfTreesToSave() + "\n");

        /* RANDOMIZE INPUT ORDER */
        if (getOptionInputOrderSeed() != 0 && getOptionInputOrderJumbles() != 0) {
            if (getOptionInputOrderSeed() % 4 != 1) {
                throw new SplitsException("seed modulo 4 must be 1.");
            } else {
                fw.write("J\n");
                fw.write(getOptionInputOrderSeed() + "\n");
                fw.write(getOptionInputOrderJumbles() + "\n");
            }
        }

        /* OUTGROUP ROOT */
        if (!getOptionOutgroupRoot().equals(CHOOSE_ROOT)) {
            fw.write("O\n");
            Taxa t = doc.getTaxa();
            for (int i = 1; i <= t.getNtax(); i++) {
                if (t.getLabel(i).equals(getOptionOutgroupRoot())) {
                    fw.write(i + "\n");
                    break;
                }
            }
        }

        /* THRESHOLD PARSIMONY */
        if (getOptionThresholdParsimony() != 0.0) {
            fw.write("T\n");
            fw.write(getOptionThresholdParsimony() + "\n");
        }

        /* TRANSVERSION PARSIMONY */
        if (getOptionTranversionParsimony())
            fw.write("N\n");

        /* WEIGHTS FILE */
        if (!getOptionWeightsFile().equals("")) {

            File _weightsFile = new File(getOptionWeightsFile());
            if (!_weightsFile.isFile()) {
                throw new SplitsException("weights file " + getOptionWeightsFile() + " not found.");
            } else {
                if (!getOptionWeightsFile().equals("weights")) {
                    fw.write("W\n");
                }
            }
        }
        fw.write("Y\n");
        if (!getOptionWeightsFile().equals("weights") &&
                !getOptionWeightsFile().equals("")) {
            fw.write(getOptionWeightsFile() + "\n");
        }

//	    if(!getOptionUserTree().equals("") && !getOptionUserTree().equals(System.getProperty("user.dir")+File.separator+"intree")) {
//			
//			fw.write(getOptionUserTree()+"\n");
//		}
        fw.flush();
        fw.close();

    }

    /**
     * execute the phylip executable using Runtime.exec
     *
     * @param optionfile the input file for phylip
     * @param phylipBin
     * @throws Exception
     */
    private void executePhylip(File optionfile, File phylipBin) throws Exception {
        String shell = (os == REDMOND) ? "cmd.exe" : "sh";
        String readCmdStringOpt = (os == REDMOND) ? "/c" : "-c";
        String phylipCmd = phylipBin.getAbsolutePath()
                + " < "
                + optionfile.getAbsolutePath();
        if (os == REDMOND && phylipBin.getAbsolutePath().contains(" "))
            new Alert("Path contains space, this won't work");
        String[] commands = new String[]
                {
                        shell,
                        readCmdStringOpt,
                        phylipCmd
                };
        try {
            System.out.print("Executing: ");
            for (String command : commands) System.out.println("\t" + command);
            Process proc = Runtime.getRuntime().exec(commands, null, new File(System.getProperty(("user.dir"))));

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
     * Export Characters block to phylip-infile.
     *
     * @param doc    the document
     * @param infile temporary phylip-infile
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
     * path to the "dnapars" executable
     *
     * @param dnaparsPath
     */
    public void setOptionPhylipPath(String dnaparsPath) {
        ProgramProperties.put(PHYLIP_DNAPARS, dnaparsPath.trim());
    }

    /**
     * path to the "dnapars" executable
     *
     * @return path to dnapars
     */
    public String getOptionPhylipPath() {
        return ProgramProperties.get(PHYLIP_DNAPARS, "dnapars");
    }

    /**
     * note: user-tree option somehow generates output files currently 
     * not readable by the splitstree-parser.
     */

    /**
     * path to the optional trees file
     * @param treesFile
     */
//    public void setOptionUserTree(String treesFile) {
//        Properties.put(PHYLIP_TREES_FILE,Basic.trim(treesFile));
//    }

    /**
     * path to the optional trees file
     * @return path to trees file
     */
//    public String getOptionUserTree() {
//        return Properties.get(PHYLIP_TREES_FILE,"");
//    }

    /**
     * @return Returns the search mode.
     */
    public String getOptionSearchMode() {
        return searchMode;
    }

    /**
     * @param searchMode the search mode
     */
    public void setOptionSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    /**
     * @param doc the Document
     * @return a List of possible search modes.
     */
    public List selectionOptionSearchMode(Document doc) {
        List modes = new LinkedList();
        modes.add(SEARCH_MODE_M_THOROUGH);
        modes.add(SEARCH_MODE_L_THOROUGH);
        modes.add(SEARCH_MODE_REARRANGE);
        return modes;
    }

    /**
     * @return Returns the number of trees to save.
     */
    public int getOptionNumberOfTreesToSave() {
        return numberOfTreesToSave;
    }

    /**
     * @param numberOfTreesToSave The number of trees to save.
     */
    public void setOptionNumberOfTreesToSave(int numberOfTreesToSave) {
        this.numberOfTreesToSave = numberOfTreesToSave;
    }

    /**
     * @return Returns the randomize input order seed.
     */
    public int getOptionInputOrderSeed() {
        return randomizeInputOrderSeed;
    }

    /**
     * @param randomizeInputOrderSeed the randomize input order seed.
     */
    public void setOptionInputOrderSeed(int randomizeInputOrderSeed) {
        this.randomizeInputOrderSeed = randomizeInputOrderSeed;
    }

    /**
     * @return Returns how many times to jumble the input order.
     */
    public int getOptionInputOrderJumbles() {
        return randomizeInputOrderJumbles;
    }

    /**
     * @param randomizeInputOrderJumbles How many times to jumble the input order.
     */
    public void setOptionInputOrderJumbles(int randomizeInputOrderJumbles) {
        this.randomizeInputOrderJumbles = randomizeInputOrderJumbles;
    }

    /**
     * @return Returns the label of the taxa to use as outgroup root.
     */
    public String getOptionOutgroupRoot() {
        return outgroupRoot;
    }

    /**
     * @param outgroupRoot the label of the taxa to use as outgroup root.
     */
    public void setOptionOutgroupRoot(String outgroupRoot) {
        this.outgroupRoot = outgroupRoot;
    }

    /**
     * @param doc the document.
     * @return Returns the List of current taxa.
     */
    public List selectionOptionOutgroupRoot(Document doc) {
        List l = new LinkedList();
        l.add(CHOOSE_ROOT);
        l.addAll(doc.getTaxa().getAllLabels());
        return l;
    }

    /**
     * @return Returns the thresholdParsimony value.
     */
    public double getOptionThresholdParsimony() {
        return thresholdParsimony;
    }

    /**
     * @param thresholdParsimony The thresholdParsimony value.
     */
    public void setOptionThresholdParsimony(double thresholdParsimony) {
        this.thresholdParsimony = thresholdParsimony;
    }

    /**
     * @return Returns whether to use tranversionParsimony.
     */
    public boolean getOptionTranversionParsimony() {
        return tranversionParsimony;
    }

    /**
     * @param tranversionParsimony whether to use tranversionParsimony.
     */
    public void setOptionTranversionParsimony(boolean tranversionParsimony) {
        this.tranversionParsimony = tranversionParsimony;
    }

    /**
     * path to the weightsFile
     *
     * @param weightsFile
     */
    public void setOptionWeightsFile(String weightsFile) {
        ProgramProperties.put(PHYLIP_WEIGHTS_FILE, weightsFile.trim());
    }

    /**
     * path to the weightsFile
     *
     * @return Returns the weightsFile.
     */
    public String getOptionWeightsFile() {
        return ProgramProperties.get(PHYLIP_WEIGHTS_FILE, "");
    }
}
