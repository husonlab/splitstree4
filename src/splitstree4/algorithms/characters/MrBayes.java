/*
 * MrBayes.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.StreamGobbler;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.io.*;


/**
 * Calculates maximum likelihood trees from DNA or AA sequences using MrBayes
 *
 * @author Daniel Huson and David Bryant, Markus Franz
 */
public class MrBayes implements Characters2Trees {

    public final static String DESCRIPTION = "Uses MrBayes to generate samples from a posterior distribution";

    //private JPanel GUIPanel;
    static final String MRBAYES_BINARY = "MRBAYES_BINARY";

    /* OPTIONS */
    private int chainLength;    //Length of chain to be run
    private int numHeatedChains; //Number of heated chains
    private String lsetOptions;  //Options passed to lset
    private String psetOptions;  //Options passed to pset

    /* OTHER FIELDS */

    private static final int LINUX = 0;
    private static final int REDMOND = 1;
    private static final int MAC_OS = 2;
    private int os = 0;


    public MrBayes() {
        chainLength = 100000;
        numHeatedChains = 3;
        lsetOptions = "";
        psetOptions = "";
    }

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


        doc.notifyTasks("Run MrBayes", getOptionMrBayesPath());
        doc.notifySetProgress(-1);

		final File mrBayesBin = new File(getOptionMrBayesPath());
        // Check to see if this file exists/
        if (!mrBayesBin.isFile()) {
            throw new SplitsException(getClass().getName() + ": File not found: " + getOptionMrBayesPath());
        }
        final File mrBayesDir = mrBayesBin.getParentFile();
        final File infile = File.createTempFile("tmp_MB_input", ".nex", mrBayesDir);
        final String tmpFileName = infile.getName();
        final String tmpFileBaseName = tmpFileName.substring(0, tmpFileName.length() - 4);
        final File treeFile = new File(mrBayesDir, tmpFileBaseName + ".nex.run1.t");

        // get os
        if (System.getProperty("os.name").matches(".*[Ll]inux.*")) os = MrBayes.LINUX;
        if (System.getProperty("os.name").matches(".*[Ww]indows.*")) os = MrBayes.REDMOND;
        if (System.getProperty("os.name").matches(".*[Mm]ac.*")) os = MrBayes.MAC_OS;

        /* write MrBayes-infile */
        doc.notifySubtask("exporting data");

        //MrBayes has problems with quoted or complex taxon names, so we just call them TAXON1,TAXON2,...
        Taxa newTaxa = (Taxa) taxa.clone();
        for (int i = 1; i <= taxa.getNtax(); i++) {
            newTaxa.setLabel(i, "TAXON" + i);
        }
        createMrBayesInput(infile, newTaxa, chars);

        /* execute MyBayes */
        doc.notifySubtask("running " + getOptionMrBayesPath());
        executeMrBayes(mrBayesBin, infile, doc);

        /* import sampled trees */
        doc.notifySubtask("importing the trees sampled");


        return parseTrees(treeFile, taxa);

    }

    private void createMrBayesInput(File infile, Taxa taxa, Characters chars) throws SplitsException {
        FileWriter fw;
        try {
            fw = new FileWriter(infile);
            boolean oldQuoteLabels = chars.getFormat().isLabelQuotes();
            chars.getFormat().setLabelQuotes(false);
            chars.writeDataBlock(fw, taxa);
            chars.getFormat().setLabelQuotes(oldQuoteLabels);

            fw.write("\n\nbegin MrBayes;\n\tset autoclose=yes nowarn=yes;\n");
            if (getOptionLsetOptions().length() > 0)
                fw.write("\tlset " + getOptionLsetOptions() + ";\n");
            if (getOptionPrsetOptions().length() > 0)
                fw.write("\tprset " + getOptionPrsetOptions() + ";\n");
            fw.write("\tshowmodel;\n");
            fw.write("\tmcmc ");
            fw.write(" ngen=" + getOptionNumGenerations());
            //fw.write(" nruns="+getOptionNumRuns());
            fw.write(" nChains=" + (getOptionNumHeatedChains() - 1));
            fw.write(";\n\tquit;\nend;\n");
            fw.close();

        } catch (IOException e) {
            throw new SplitsException(getClass().getName() + ": could not write to temp file: " + e.getMessage());
        }
    }

    /**
     * execute the MrBayes executable using Runtime.exec
     *
     * @param mrBayesBin the executable MrBayes
     * @param inputFile  (the input file)
     */
    private void executeMrBayes(File mrBayesBin, File inputFile, Document doc) throws Exception {


        String shell = (os == MrBayes.REDMOND) ? "cmd.exe" : "sh";
        String readCmdStringOpt = (os == MrBayes.REDMOND) ? "/c" : "-c";

        String mrBayesCmd = mrBayesBin.getAbsolutePath()
                + "  "
                + inputFile.getName();


        String[] commands = new String[]
                {
                        shell,
                        readCmdStringOpt,
                        mrBayesCmd
                };

        try {
            Process proc = Runtime.getRuntime().exec(commands, null, mrBayesBin.getParentFile());
            System.out.println("Executing: ");
            for (String command : commands) System.out.println("\t" + command);

            // any error message?
            StreamGobbler errorGobbler = new
                    StreamGobbler(proc.getErrorStream(), "ERROR");

            // any output?
            MrBayesOutputGobbler outputGobbler = new
                    MrBayesOutputGobbler(proc, getOptionNumGenerations(), doc);

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            System.err.println("### Started external command ###");
            doc.notifySetMaximumProgress(getOptionNumGenerations());
            int exitVal = proc.waitFor();
            doc.notifySetProgress(-1);
            System.err.println("### Finished external command ###");
            // any error???
            if (exitVal != 0)
                throw new SplitsException("Return value=" + exitVal);

        } catch (IOException e) {
            //ToDO: we can still recover any trees generated.
            throw new SplitsException(getClass().getName() + ": " + e.getMessage());
        }
    }


    /**
     * parse computed output
     *
     * @param outtree phylip outtree-file
     * @param taxa    original taxa
     * @return the parsed Trees-block
     * @throws Exception SplitsException, if import or parsing fails
     */
    private Trees parseTrees(File outtree, Taxa taxa) throws Exception {

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
            trees.setNumberedIdentityTranslate(taxa);
        } catch (Exception e) {
            throw new SplitsException(getClass().getName() + ": parsing trees failed: " + e.getMessage());
        }

        return trees;

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
     */
    public void setOptionMrBayesPath(String phymlPath) {
        ProgramProperties.put(MrBayes.MRBAYES_BINARY, phymlPath.trim());
    }

    /**
     * path to the "PHYML" executable
     *
     * @return path to PHYML
     */
    public String getOptionMrBayesPath() {
        return ProgramProperties.get(MrBayes.MRBAYES_BINARY, "mb");
    }

    /**
     * Get length of MCMC chain
     *
     * @return length
     */
    public int getOptionNumGenerations() {
        return chainLength;
    }

    /**
     * Set length of MCMC chain
     *
     */
    public void setOptionNumGenerations(int chainLength) {
        this.chainLength = chainLength;
    }


    /**
     * Gets the number of heated chains per independent chain
     *
     * @return number of heated chains
     */
    public int getOptionNumHeatedChains() {
        return numHeatedChains;
    }

    /**
     * Sets the number of heated chains per independent chain
     */
    public void setOptionNumHeatedChains(int numHeatedChains) {
        this.numHeatedChains = numHeatedChains;
    }

    /**
     * Gets the string passed to lset in mrBayes
     *
     * @return options
     */
    public String getOptionLsetOptions() {
        return lsetOptions;
    }

    /**
     * Sets the string passed to lset in mrBayes
     */
    public void setOptionLsetOptions(String lsetOptions) {
        this.lsetOptions = lsetOptions;
    }

    /**
     * Gets the options passed to pset (prior distribution parameters) in MrBayes
     *
     * @return string of options
     */
    public String getOptionPrsetOptions() {
        return psetOptions;
    }

    /**
     * Sets the options passed to pset (prior distribution parameters) in MrBayes
     */
    public void setOptionPrsetOptions(String psetOptions) {
        this.psetOptions = psetOptions;
    }


}

class MrBayesOutputGobbler extends Thread {
	boolean stopped = false;
	final Process process;
	final InputStream inputStream;
	final int numGens;
	final Document doc;


	/**
	 * construct a gobbler
	 *
	 * @param process the process that MrBayes is running in.
	 * @param numGens number of generations in the chain
	 */
	public MrBayesOutputGobbler(Process process, int numGens, final Document doc) {
        this.process = process;
        this.inputStream = process.getInputStream();
        this.numGens = numGens;
        this.doc = doc;
    }

    private int getStep(String line) {
        int i = 0;
        int len = line.length();
        while (i < len && !Character.isDigit(line.charAt(i)))
            i++;
        int start = i;
        do {
            i++;
        } while (i < len && Character.isDigit(line.charAt(i)));
        int end = i;
        if (start < len) {
            try {
                return (Integer.parseInt(line.substring(start, end)));
            } catch (NumberFormatException ex) {
                return -1;
            }
        }

        return -1;

    }

    /**
     * the run method
     */
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line;
            boolean inChain = false;
            boolean finishedChain = false;
            while ((line = br.readLine()) != null) {
                if (!inChain && !finishedChain && line.endsWith("Chain results:")) {
                    System.err.println(line);
                    inChain = true;
                } else {
                    if (inChain) {
                        int step = getStep(line);
                        //TODO: Add other diagnostics here.
                        if (step > 0) {
                            System.err.println("STEP = " + step);
                            try {
                                doc.notifySetProgress(step);
                            } catch (CanceledException e) {
                                process.destroy();
                                //throw new CanceledException("Cancelled MrBayes Run");
                                return;
                            }
                        }
                        if (step >= numGens) {
                            inChain = false;
                            finishedChain = true;
                        }

                    }
                    System.err.println(line);

                }


                if (stopped)
                    break;
            }
        } catch (IOException ex) {
            System.err.println("Catching basic in Gobbler");
            Basic.caught(ex);
        }
    }

    /**
     * finish gobbling
     */
    public void finish() {
        stopped = true;
    }
}
