/*
 * C2Text.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.util.StreamGobbler;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.externalIO.exports.ExportManager;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Runs external program
 */
public class C2Text implements Characters2Trees {
    public final boolean EXPERT = true;
    private boolean optionImportTrees = true;
    private String optionSendFile = "infile";
    private String optionReturnFile = "outtree";
    private String optionCommand = "run_dnapars";
    private String optionPreparation = "rm -f %i %o outree";
    private boolean optionSendAllSites = false;
    private String optionFormat = ExportManager.getExportName(new splitstree4.externalIO.exports.PhylipSequences());
    public final static String DESCRIPTION = "Runs external program (use %i and %o to specify in- and output file in command)";

    /**
     * Determine whether the external command can be applied to the given
     * data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return getOptionCommand() != null && chars != null;
    }

    /**
     * Attempts to run the given external command on the given data.
     *
     * @param taxa  the taxa
     * @param chars the input characters
     * @return the computed trees Object
     */
    public Trees apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        {
            doc.notifyTasks("Run external program", getOptionCommand());
            doc.notifySetProgress(-1);

            String shellCmd = getOptionCommand();
            if (shellCmd == null)
                return null;

            // remove any old files
            //doCleanup();

            // setup send file
            File sendFile = new File(getOptionSendFile());

            // put names of send and return file into shell command:
            shellCmd = shellCmd.replaceAll("%i", getOptionSendFile());
            shellCmd = shellCmd.replaceAll("%o", getOptionReturnFile());

            System.err.println("shellCmd: " + shellCmd);

            // setup export of data:
            doc.notifySubtask("send data to external program");
            List blockToExport = new LinkedList();
            blockToExport.add(Characters.NAME);

            Document tmpDoc = new Document();
            tmpDoc.setTaxa(taxa);
            tmpDoc.setCharacters(chars);

            Map exportName2OrigName;

            try {
                exportName2OrigName = ExportManager.exportData(sendFile, false, getOptionSendAllSites(), getOptionFormat(), blockToExport, tmpDoc);

            } catch (Exception ex) {
                throw new SplitsException("C2Text: External: export failed: " +
                        ex.getMessage());
            }

            doc.notifySubtask("run external program");

            // run the external script:
            try {
                System.out.println("Executing: " + shellCmd);
                System.err.println("### Started external command ###");
                Process proc = Runtime.getRuntime().exec(shellCmd);

                // any error message?
                StreamGobbler errorGobbler = new
                        StreamGobbler(proc.getErrorStream(), "ERROR");

                // any output?
                StreamGobbler outputGobbler = new
                        StreamGobbler(proc.getInputStream(), null);

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                System.err.println("### Wait for external command ###");
                int exitVal = proc.waitFor();
                System.err.println("### Finished external command ###");
                // any error???
                if (exitVal != 0)
                    throw new SplitsException("Return value=" + exitVal);

            } catch (Exception ex) {
                throw new SplitsException("C2Text: Execution failed: " +
                        ex.getMessage());
            }

            // setup receiving of data:
            Trees trees = new Trees();
            String nexus;

            if (getOptionImportTrees()) {
                doc.notifySubtask("import computed data");

                try {
                    File returnFile = new File(getOptionReturnFile());
                    if (!returnFile.isFile() || !returnFile.canRead())
                        throw new IOException("Can't open file: " + getOptionReturnFile());
                    nexus = ImportManager.importData(returnFile);
                } catch (Exception ex) {
                    throw new SplitsException("C2Text: Import of result failed: " + ex.getMessage());
                }

                // parse received data:
                doc.notifySubtask("parse imported data");
                try {
                    NexusStreamParser np = new NexusStreamParser(new StringReader(nexus));
                    np.matchIgnoreCase("#nexus");
                    Taxa tmpTaxa = new Taxa();
                    tmpTaxa.read(np);
                    trees.read(np, tmpTaxa);
                    if (exportName2OrigName != null) // external program ran on different names
                    {
                        trees.changeNodeLabels(exportName2OrigName);
                        trees.setIdentityTranslate(taxa);
                    }
                } catch (Exception ex) {
                    throw new SplitsException("C2Text: Run external: parsing of result failed: " +
                            ex.getMessage());
                }
            }

            return trees;
        }
    }

    /**
     * clean up before running external program
     */
    private void doCleanup() {

        // put names of send and return file into clenup command:

        String cleanup = getOptionPreparation().replaceAll("%i", getOptionSendFile());
        cleanup = cleanup.replaceAll("%o", getOptionReturnFile());

        // run the cleanup script:
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
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * Sets the external shell command.
     * The special variables %i and %e are replaced by the name of a
     * temporary input and output file, the former containing character data
     * for the extern program and the latter used to obtain the computed
     * distance matrix.
     *
     * @param command the command string
     */
    public void setOptionCommand(String command) {
        this.optionCommand = command;
    }

    /**
     * Gets the external shell command.
     *
     * @return the command
     */
    public String getOptionCommand() {
        return optionCommand;
    }

    /**
     * gets the list of possible export formats
     *
     * @return
     */
    public List selectionOptionFormat(Document doc) {
        try {
            // TODO: only ones applicable to this data type!
            return ExportManager.getExportNames(Characters.NAME);
        } catch (Exception ex) {
            Basic.caught(ex);
            return new LinkedList();
        }
    }

    /**
     * file in which to send data to external program
     *
     * @return file
     */
    public String getOptionSendFile() {
        return optionSendFile;
    }

    public void setOptionSendFile(String optionSendFile) {
        this.optionSendFile = optionSendFile;
    }

    /**
     * file in which to receive data from external program
     *
     * @return
     */
    public String getOptionReturnFile() {
        return optionReturnFile;
    }

    public void setOptionReturnFile(String optionReturnFile) {
        this.optionReturnFile = optionReturnFile;
    }

    public String getOptionPreparation() {
        return optionPreparation;
    }

    public void setOptionPreparation(String optionPreparation) {
        this.optionPreparation = optionPreparation;
    }

    public String getOptionFormat() {
        return optionFormat;
    }

    public void setOptionFormat(String optionFormat) {
        this.optionFormat = optionFormat;
    }

    public boolean getOptionSendAllSites() {
        return optionSendAllSites;
    }

    public void setOptionSendAllSites(boolean optionSendAllSites) {
        this.optionSendAllSites = optionSendAllSites;
    }

    public boolean getOptionImportTrees() {
        return optionImportTrees;
    }

    public void setOptionImportTrees(boolean optionImportTrees) {
        this.optionImportTrees = optionImportTrees;
    }
}

// EOF
