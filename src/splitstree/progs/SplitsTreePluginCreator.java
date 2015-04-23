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

package splitstree.progs;

import jloda.util.CommandLineOptions;
import jloda.util.ProgramProperties;
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
