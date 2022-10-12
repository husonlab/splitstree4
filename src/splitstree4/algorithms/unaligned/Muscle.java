/*
 * Muscle.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.unaligned;

import jloda.swing.util.ProgramProperties;
import jloda.util.StreamGobbler;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.externalIO.exports.ExportManager;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Unaligned;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

/**
 * runs muscle externally
 */
public class Muscle implements Unaligned2Characters {

    public final boolean EXPERT = true;

    private String optionOptionalParameter = "";

    private final String optionPathToCommand = "musclePath";

    private int optionMaxIters = 16;

    private String optionClusterMethod_1 = "upgma";
    private String optionClusterMethod_2 = "upgma";

    private String optionDistanceMeasure_1 = "default";
    private String optionDistanceMeasure_2 = "pctid_kimura";

    private boolean optionLogFile = false;
    private String optionLogFileName = "MuscleLogFile.log";

    private String optionObjectiveScore = "sp for <100 seqs, else spf";

    public final static String DESCRIPTION = "Externally runs Muscle";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the data to be realigned
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned chars) {
        return (doc.getUnaligned() != null);
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the data to be realigned
     * @return the computed characters Object
     */
    public Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception {

        //setup send file
        File sendFile = new File("infile.fa");

        //Export unaligned sequences into sendFile
        ExportManager.exportData(sendFile, false, true, "FastASequencesUnaligned", null, doc);

        // put names of send and return file into shell command:
        if (!this.getOptionPathToCommand().endsWith("/")) setOptionPathToCommand(this.getOptionPathToCommand() + "/");
        String shellCmd = this.getOptionPathToCommand() + "muscle -in infile.fa -out outfile.fa";

        //set maximum iterations (default 16)
        if ((optionMaxIters != 16) && (optionMaxIters > 0)) shellCmd += " -maxiters " + optionMaxIters;

        //set cluster method for iteration 1,2
        if (!optionClusterMethod_1.equals("upgma")) shellCmd += " -cluster1 " + optionClusterMethod_1;
        //set cluster method for iteration 3,4,5...
        if (!optionClusterMethod_2.equals("upgma")) shellCmd += " -cluster2 " + optionClusterMethod_2;

        //set distance measure for iteration 1
        if (!optionDistanceMeasure_1.equals("default")) shellCmd += " -distance1 " + optionDistanceMeasure_1;
        //set distance measure for iteration 2,3..
        if (!optionDistanceMeasure_2.equals("pctid_kimura")) shellCmd += " -distance2 " + optionDistanceMeasure_2;

        //set Objective Score used by tree dependent refinement
        String objectiveScore = this.translateOptionObjectiveScore(optionObjectiveScore);
        if (!objectiveScore.equals("spm")) shellCmd += " -objscore " + objectiveScore;

        //set Log File Option
        if (optionLogFile) shellCmd += " -log " + optionLogFileName;

        //add optional parameter
        if (!optionOptionalParameter.equals("")) shellCmd += " " + optionOptionalParameter;

        //run the external script:
        try {
            System.out.println("Executing: " + shellCmd);
            System.err.println("### Started external command ###");
            Process p = Runtime.getRuntime().exec(shellCmd);
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			try {
				String tmp;
				while ((tmp = err.readLine()) != null) System.err.println("out: " + tmp);
				p.waitFor();
			} catch (InterruptedException ignored) {
			}


        } catch (Exception e) {
            e.printStackTrace();
        }

        //setup return file
        File returnFile = new File("outfile.fa");
        String input = ImportManager.importData(returnFile, unaligned.getFormat().getDatatype());

        //parse return file
        Taxa tax = new Taxa();
        tax.read(new NexusStreamParser(new StringReader(input.substring(input.indexOf("begin taxa"), input.indexOf("begin characters")))));
        Characters returnChar = new Characters();
        returnChar.read(new NexusStreamParser(new StringReader(input.substring(input.indexOf("begin characters")))), tax);

        //remove files
        doCleanup();

        return returnChar;
    }

    private void doCleanup() {
        String cleanup = "rm -f infile.fa outfile.fa";

        if (cleanup.length() > 0)
            try {
                Process proc = Runtime.getRuntime().exec(cleanup);
                System.out.println("Executing: " + cleanup);

                // any error message?
                StreamGobbler errorGobbler = new
                        StreamGobbler(proc.getErrorStream(), "ERROR");

                // any output?
                StreamGobbler outputGobbler = new
                        StreamGobbler(proc.getInputStream(), "OUTPUT");

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                int exitVal = proc.waitFor();
                // any error???
                if (exitVal != 0)
                    throw new Exception("Return value=" + exitVal);
            } catch (Exception ex) {
                System.err.println("Cleanup failed: " + ex.getMessage());
            }
    }


    /**
     * maximum number of iterations
     * iteration 1: k-mer distance matrix, estimate tree, progressive alignment
     * iteration 2: kimura distance matrix, estimate tree, progressive alignment
     * iteration 3,4,.. : tree refinment
     */
    public int getOptionMaxiters() {
        return optionMaxIters;
    }

    public void setOptionMaxiters(int optionMaxIters) {
        this.optionMaxIters = optionMaxIters;
    }

    /*
     * Cluster method used in iterations 1 and 2
     * @return      name of method
     */

    public String getOptionClusterMethod_1() {
        return optionClusterMethod_1;
    }

    /**
     * sets the method to use
     *
     */
    public void setOptionClusterMethod_1(String clusterMethod_1) {
        this.optionClusterMethod_1 = clusterMethod_1;
    }

    /**
     * returns list of cluster methods
     *
     * @return methods
     */
    public List selectionOptionClusterMethod_1(Document doc) {
        List methods = new LinkedList();
        methods.add("upgma");
        methods.add("neighborjoining");
        return methods;
    }

    /*
     * Cluster method used after the first and second iteration
     * @return      name of method
     */

    public String getOptionClusterMethod_2() {
        return optionClusterMethod_2;
    }

    /**
     * sets the method to use
     *
     */
    public void setOptionClusterMethod_2(String clusterMethod_2) {
        this.optionClusterMethod_2 = clusterMethod_2;
    }

    /**
     * returns list of cluster methods
     *
     * @return methods
     */
    public List selectionOptionClusterMethod_2(Document doc) {
        List methods = new LinkedList();
        methods.add("upgma");
        methods.add("neighborjoining");
        return methods;
    }

    /*
     * Distance measure for iteration 1
     * @return      name of method
     */

    public String getOptionDistanceMeasure_1() {
        return optionDistanceMeasure_1;
    }

    /**
     * sets the method to use
     *
     */
    public void setOptionDistanceMeasure_1(String distanceMeasure_1) {
        this.optionDistanceMeasure_1 = distanceMeasure_1;
    }

    /**
     * returns list of distance measure methods
     *
     * @return methods
     */
    public List selectionOptionDistanceMeasure_1(Document doc) {
        List methods = new LinkedList();
        methods.add("default");
        methods.add("kmer6_6");
        methods.add("kmer20_3");
        methods.add("kmer20_4");
        methods.add("kbit20_3");
        methods.add("kmer4_6");
        return methods;
    }

    /*
     * Distance measure for iteration 2,3...
     * @return      name of method
     */

    public String getOptionDistanceMeasure_2() {
        return optionDistanceMeasure_2;
    }

    /**
     * sets the method to use
     *
     */
    public void setOptionDistanceMeasure_2(String distanceMeasure_2) {
        this.optionDistanceMeasure_2 = distanceMeasure_2;
    }

    /**
     * returns list of distance measure methods
     *
     * @return methods
     */
    public List selectionOptionDistanceMeasure_2(Document doc) {
        List methods = new LinkedList();
        methods.add("kmer6_6");
        methods.add("kmer20_3");
        methods.add("kmer20_4");
        methods.add("pctid_kimura");
        methods.add("pctid_log");
        return methods;
    }

    /*
     * Objective score used by tree dependent refinment
     * @return      name of objective score
     */

    public String getOptionObjectiveScore() {
        return optionObjectiveScore;
    }

    /**
     * sets the score to use
     *
     */
    public void setOptionObjectiveScore(String optionObjectiveScore) {
        this.optionObjectiveScore = optionObjectiveScore;
    }

    /**
     * returns list of objective scores
     *
     * @return methods
     */
    public List selectionOptionObjectiveScore(Document doc) {
        List methods = new LinkedList();
        methods.add("sp for <100 seqs,else spf");
        methods.add("sp, dimer approximation");
        methods.add("sum of pairs");
        methods.add("dynamic programming score");
        methods.add("average profile-seq. score");
        methods.add("cross profile score");
        return methods;
    }

    public String translateOptionObjectiveScore(String objectiveScore) {
        String optionObjectiveScore;
        switch (objectiveScore) {
            case "sum of pairs":
                optionObjectiveScore = "sp";
                break;
            case "sp, dimer approximation":
                optionObjectiveScore = "spf";
                break;
            case "sp for <100 seqs, else spf":
                optionObjectiveScore = "spm";
                break;
            case "dynamic programming score":
                optionObjectiveScore = "dp";
                break;
            case "average profile-seq. score":
                optionObjectiveScore = "ps";
                break;
            case "cross profile score":
                optionObjectiveScore = "xp";
                break;
            default:
                optionObjectiveScore = "spm";
                break;
        }

        return optionObjectiveScore;
    }

    /*
     * Log File Option
     * @return      the optionLogFile
     */

    public boolean getOptionLogFile() {
        return optionLogFile;
    }

    /**
     * sets the logFile Option
     *
     */
    public void setOptionLogFile(boolean optionLogFile) {
        this.optionLogFile = optionLogFile;
    }

    /*
     * Log File Option Name
     * @return      name of the LogFile
     */

    public String getOptionLogFileName() {
        return optionLogFileName;
    }

    /**
     * sets the logFile Option
     *
     */
    public void setOptionLogFileName(String optionLogFileName) {
        this.optionLogFileName = optionLogFileName;
    }


    public String getOptionOptionalParameter() {
        return optionOptionalParameter;
    }

    public void setOptionOptionalParameter(String optionOptionalParameter) {
        this.optionOptionalParameter = optionOptionalParameter;
    }

    public String getOptionPathToCommand() {
        return ProgramProperties.get(optionPathToCommand, "musclePath");
    }

    public void setOptionPathToCommand(String pathToCommand) {
        ProgramProperties.put(optionPathToCommand, pathToCommand.trim());
    }

}
