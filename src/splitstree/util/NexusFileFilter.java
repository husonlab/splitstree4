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

/**
 * @author ?
 * nexus file filter
 * 12.03
 */
package splitstree.util;

import jloda.util.parse.NexusStreamParser;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

/**
 * a FileFilter for nexus files
 */
public class NexusFileFilter extends FileFilter {

    /**
     * is the file a nexus file
     * @param f File to be accepted
     * @return true if file is a nexus file
     */
    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            String extension = getExtension(f);
            if (extension != null)
                if (extension.equalsIgnoreCase("nex")
                        || extension.equalsIgnoreCase("nxs"))
                    return true;
        }
        return false;
    }

    /**
     * the description of the file filter
     * @return description of file matching the filter
     */
    public String getDescription() {
        return "NEXUS Files (*.nex,*.nxs)";
    }

    /**
     * returns the extension of the given file
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     *         last ".")
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * determines whether given file is a valid new Nexus file
     *
     * @param file
     * @return true, if file begins with "#nexus begin taxa;"
     */
    public static boolean isNexusFile(File file) {
        try {
            NexusStreamParser np = new NexusStreamParser(new FileReader(file));
            np.matchIgnoreCase("#nexus begin taxa;");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * does this text look like new-style nexus?
     *
     * @param text
     * @return true, if looks like new style nexus
     */
    public static boolean isNexusText(String text) {
        try {
            NexusStreamParser np = new NexusStreamParser(new StringReader(text));
            np.matchIgnoreCase("#nexus begin taxa;");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
