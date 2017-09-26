/**
 * SplitsTree.java
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
package splitstree4.main;

import jloda.util.ArgsOptions;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ProgressCmdLine;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.util.NexusFileFilter;

import javax.swing.*;
import java.io.*;

/**
 * Runs the splits tree program
 *
 * @author huson
 *         Date: 26-Nov-2003
 */
public class SplitsTree {

    public static void main(String[] args) {
        try {
            //install shutdown hook
            //its run() method is executed for sure as the VM shuts down
            Runnable finalizer = new Runnable() {
                public void run() {
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(finalizer));

            //run application
            SplitsTree application = (new SplitsTree());
            application.parseArguments(args);

        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
            if (!th.getMessage().equals("Help")) {
                Basic.caught(th);
            }
            if (!ArgsOptions.hasMessageWindow())
                System.exit(1);
        }
    }

    /**
     * the main class
     *
     * @param args command line arguments
     * @throws java.lang.Exception
     */
    public void parseArguments(String[] args) throws Exception {
        Basic.startCollectionStdErr();

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);
        ProgramProperties.setUseGUI(true);

        final ArgsOptions options = new ArgsOptions(args, this, "Interactive analysis and visualization of phylogenetic data");
        options.setAuthors("Daniel H. Huson and David J. Bryant");
        options.setLicense("Copyright (C) 2017 Daniel H. Huson and David J. Bryant. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setVersion(ProgramProperties.getProgramVersion());

        options.comment("Input:");
        final String fileName = options.getOption("-i", "input", "Input file", "");

        options.comment("Commands:");
        ProgramProperties.setUseGUI(!options.getOption("-g", "commandLineMode", "Run SplitsTree in commandline mode", false) && !options.isDoHelp());

        final String initCommand = options.getOption("-x", "initCommand", "Execute this command at startup", "");
        final String commandFileName = options.getOption("-c", "commandFile", "File of commands to execute in command-line mode", "");

        options.comment("Configuration:");
        final boolean showMessages = options.getOption("-m", "hideMessageWindow", "Hide the message window", false);
        String defaultPreferenceFile;
        if (ProgramProperties.isMacOS())
            defaultPreferenceFile = System.getProperty("user.home") + "/Library/Preferences/SplitsTreePrefs.def";
        else
            defaultPreferenceFile = System.getProperty("user.home") + File.separator + ".SplitsTree.def";
        final String propertiesFile = options.getOption("-p", "propertiesFile", "Properties file", defaultPreferenceFile);
        final boolean showVersion = options.getOption("-V", "version", "Show version string", false);
        final boolean silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));
        final boolean showSplash = !options.getOption("-s", "hideSplash", "Hide startup splash screen", false);
        SplitsTreeProperties.setExpertMode(options.getOption("-X", "expert", "!expert mode", false));
        options.done();

        if (silentMode) {
            Basic.stopCollectingStdErr();
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }
        if (showVersion) {
            System.err.println(SplitsTreeProperties.getVersion());
            System.err.println("Java version: " + System.getProperty("java.version"));
        }

        if (ProgramProperties.isUseGUI())  // run in GUI mode
        {
            System.setProperty("user.dir", System.getProperty("user.home"));
            SplitsTreeProperties.initializeProperties(propertiesFile);
            if (showSplash)
                SplitsTreeProperties.getAbout().showAbout();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Director dir = Director.newProject();
                    if (showSplash)
                        SplitsTreeProperties.getAbout().hideSplash();

                    if (showMessages) {
                        dir.getActions().getMessageWindow().actionPerformed(null);
                        dir.getMainViewerFrame().toFront(); // keep main viewer in front
                    }

                    Basic.restoreSystemOut(System.err); // send system out to system err
                    System.err.println(Basic.stopCollectingStdErr());

                    if (!fileName.equals("")) {// initial file given
                        File file = new File(fileName);
                        SplitsTreeProperties.addRecentFile(file);

                        if (NexusFileFilter.isNexusFile(file)) {
                            dir.openFile(file);
                        } else {
                            dir.importFile(file);
                        }
                    }
                    if (!initCommand.equals("")) // run initial commands, if any
                    {
                        while (dir.isInUpdate())  // wait until finished reading file
                        {
                            try {
                                Thread.sleep(100);
                            } catch (java.lang.InterruptedException ex) {
                            }
                        }
                        dir.execute(initCommand);
                    }
                }
            });
        } else // command line mode
        {
            SplitsTreeProperties.initializeProperties(propertiesFile);

            Basic.restoreSystemErr(System.out); // send system err to system out
            System.err.println(Basic.stopCollectingStdErr());

            Document doc = new Document();
            doc.setProgressListener(new ProgressCmdLine());

            StringBuilder command;
            if (!fileName.equals("")) {
                command = new StringBuilder("load file=" + fileName);
                try {
                    doc.execute(command.toString());
                } catch (Exception ex) {
                    System.err.println(command + ": failed");
                    System.err.println(ex.getMessage());
                }
            }
            if (!initCommand.equals("")) {
                try {
                    doc.execute(initCommand);
                } catch (Exception ex) {
                    System.err.println(initCommand + ": failed");
                    System.err.println(ex.getMessage());
                }
            }
            final boolean fromFile = (commandFileName.length() > 0);
            final LineNumberReader inp;
            if (fromFile)
                inp = new LineNumberReader(new BufferedReader(new FileReader(commandFileName)));
            else
                inp = new LineNumberReader(new BufferedReader(new InputStreamReader(System.in)));
            try {
                boolean inMultiLineMode = false;
                command = new StringBuilder();
                while (true) { // process commands from standard input
                    if (!fromFile) {
                        if (!inMultiLineMode)
                            System.out.print("SplitsTree: ");
                        else
                            System.out.print("+ ");
                        System.out.flush();
                    }
                    try {
                        String aline = inp.readLine();
                        if (aline == null)
                            break;
                        if (aline.equals("\\")) {
                            inMultiLineMode = !inMultiLineMode;
                        } else
                            command.append(aline);
                        if (!inMultiLineMode && command.length() > 0) {
                            doc.execute(command + ";");
                            command = new StringBuilder();
                        }
                    } catch (Exception ex) {
                        System.err.println(command + ": failed");
                        Basic.caught(ex);
                        command = new StringBuilder();
                    }
                }
            } finally {
                inp.close();
            }
        }
    }
}
