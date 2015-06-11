/**
 * NexusMacFileFilter.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
	
	
