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

/*
 * Created on Feb 8, 2004
 *
 * @author David Bryant
 * Same as NexusFileFilter except for awt (preferred on OS X)
 */
package splitstree.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * a FileFilter for the mac
 */
public class NexusMacFileFilter implements FilenameFilter {

    /**
     * CONSTRUCTOR
     */
    public NexusMacFileFilter() {
    }

    /**
     * accept the given file
     * @param dir   the directory
     * @param file  the file to accept
     * @return  returns true if the file is a nexus file
     */
    public boolean accept(File dir, String file) {
        return (file.endsWith(".nxs") || file.endsWith(".nex"));
    }
}
	
	
