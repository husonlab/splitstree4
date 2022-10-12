/*
 * SplitsTreePluginCreator.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.progs;

import jloda.swing.util.CommandLineOptions;
import jloda.swing.util.ProgramProperties;
import jloda.util.UsageException;

import java.io.File;

/**
 * A class that calls the Plugin Creator from the plugin Manager
 */
public class SplitsTreePluginCreator {


    public static void main(String[] args) throws UsageException {
        CommandLineOptions options = new CommandLineOptions(args);
        String defaultPreferenceFile;
        if (ProgramProperties.isMacOS())
            defaultPreferenceFile = System.getProperty("user.home") + "/Library/Preferences/SplitsTreePrefs.def";
        else
            defaultPreferenceFile = System.getProperty("user.home") + File.separator + ".SplitsTree.def";

        final String propertiesFile = options.getOption("-p", "Properties file", defaultPreferenceFile);

        /*
        initializeProperties(propertiesFile);
        PluginManagerSettings pms = new PluginManagerSettings();
        pms.setMysqlTunneling(SplitsTreeProperties.mysqlTunneling);
        pms.setServerPluginFolder(SplitsTreeProperties.serverPluginFolder);
        pms.setDatabaseName(SplitsTreeProperties.pluginDatabase);
        pms.setPluginFolder(ProgramProperties.get("PluginFolder"));
        pms.setMainProgramVersion(SplitsTreeProperties.getShortVersion());
        System.out.println(pms);
        PluginCreator pc = new PluginCreator(null,"SplitsTree Plugin Creator",pms,true);
        */
    }


    private static void initializeProperties(String propertiesFile) {
        // PluginData stuff
        if (ProgramProperties.isMacOS())
            ProgramProperties.put("PluginFolder", System.getProperty("user.home") + "/Library/Preferences/.SplitsTree/plugins");
        else
            ProgramProperties.put("PluginFolder", System.getProperty("user.home") + File.separator + ".SplitsTree/plugins");
        // then read in file to override defaults:
        ProgramProperties.load(propertiesFile);
    }
}
