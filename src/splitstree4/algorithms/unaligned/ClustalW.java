/**
 * ClustalW.java
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
 * <p/>
 * runs clustalw externally
 *
 * @version $Id: ClustalW.java,v 1.19 2007-09-11 12:31:03 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * 7.03
 */
/**
 * runs clustalw externally
 * @version $Id: ClustalW.java,v 1.19 2007-09-11 12:31:03 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * 7.03
 */
package splitstree4.algorithms.unaligned;

import jloda.util.ProgramProperties;
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
 * runs clustalw externally
 */
public class ClustalW implements Unaligned2Characters {

    public final boolean EXPERT = true;

    private int optionGapOpen = 10;
    private double optionGapExtension = 0.2;
    private String optionWeightMatrix = "GONNET";

    private String optionOptionalParameter = "";

    private String optionPathToCommand = "clustalWPath";

    public final static String DESCRIPTION = "Externally runs ClustalW";

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
     * @param taxa      the taxa
     * @param unaligned the unaligned data
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned unaligned) {

        return (doc.getUnaligned() != null);
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned
     * @return the computed characters Object
     */
    public Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception {

        //setup send file
        File sendFile = new File("infile.fa");

        //Export unaligned sequences into sendFile
        ExportManager.exportData(sendFile, false, true, "FastASequencesUnaligned", null, doc);

        // put names of send and return file into shell command:
        String shellCmd = this.getOptionPathToCommand() + "clustalw -infile=infile.fa -outfile=outfile.fa";

        //set gap open penalty
        if ((optionGapOpen != 10) && (optionGapOpen > 0)) shellCmd += " -gapopen=" + optionGapOpen;

        //set gap extension penalty
        if ((optionGapExtension != 0.2) && (optionGapExtension > 0)) shellCmd += " -gapext=" + optionGapExtension;

        //set protein weight matrix (default Gonnet)
        if (!optionWeightMatrix.equals("GONNET")) shellCmd += " -matrix=" + optionWeightMatrix;

        //add optional command line parameter
        if (!optionOptionalParameter.equals("")) shellCmd += " " + optionOptionalParameter;

        //run the external script:
        try {
            System.out.println("Executing: " + shellCmd);
            System.err.println("### Started external command ###");
            Process p = Runtime.getRuntime().exec(shellCmd);
            //BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                String tmp;
                while ((tmp = out.readLine()) != null) System.err.println("out: " + tmp);
                p.waitFor();
            } catch (InterruptedException e) {
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
        String cleanup = "rm -f infile.fa outfile.fa infile.dnd";

        //cleanup = cleanup+= (" " + getOptionSendFile().substring(0,getOptionSendFile().indexOf(".")+1) + "dnd");

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
     * gap opening penalty (default 10)
     *
     * @return gap opening penalty
     */
    public int getOptionGapOpen() {
        return optionGapOpen;
    }

    /**
     * sets the gap opening penalty
     *
     * @param optionGapOpen
     */
    public void setOptionGapOpen(int optionGapOpen) {
        this.optionGapOpen = optionGapOpen;
    }

    /**
     * gap extension penalty (default 0.2)
     *
     * @return gap extension penalty
     */
    public double getOptionGapExtension() {
        return optionGapExtension;
    }

    /**
     * sets the gap extension penalty
     *
     * @param optionGapExtension
     */
    public void setOptionGapExtension(double optionGapExtension) {
        this.optionGapExtension = optionGapExtension;
    }

    /*
    * Protein weight matrix (default Gonnet)
    * @return      name of matrix
    */

    public String getOptionWeightMatrix() {
        return optionWeightMatrix;
    }

    /**
     * sets the protein weight matrix to use
     *
     * @param optionWeightMatrix
     */
    public void setOptionWeightMatrix(String optionWeightMatrix) {
        this.optionWeightMatrix = optionWeightMatrix;
    }

    /**
     * returns list of Protein weight matrices
     *
     * @return methods
     */
    public List selectionOptionWeightMatrix(Document doc) {
        List matrices = new LinkedList();
        matrices.add("GONNET");
        matrices.add("BLOSUM");
        matrices.add("PAM");
        matrices.add("ID");
        return matrices;
    }

    public String getOptionOptionalParameter() {
        return optionOptionalParameter;
    }

    public void setOptionOptionalParameter(String optionOptionalParameter) {
        this.optionOptionalParameter = optionOptionalParameter;
    }


    public String getOptionPathToCommand() {
        return ProgramProperties.get(optionPathToCommand, "clustalWPath");
    }

    public void setOptionPathToCommand(String pathToCommand) {
        ProgramProperties.put(optionPathToCommand, pathToCommand.trim());
    }


}
