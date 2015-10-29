/**
 * ImportManager.java
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
 * manages the importers
 *
 * @version $Id: ImportManager.java,v 1.21 2008-03-14 14:05:22 bryant Exp $
 * @author kloepper, huson
 * Date: Sep 30, 2003
 */
/**
 * manages the importers
 * @version $Id: ImportManager.java,v 1.21 2008-03-14 14:05:22 bryant Exp $
 * @author kloepper, huson
 * Date: Sep 30, 2003
 */
package splitstree4.externalIO.imports;

import jloda.util.Alert;
import jloda.util.Basic;
import splitstree4.core.SplitsException;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;


/**
 * manages the importers
 */
public class ImportManager {

    static String packageName = "splitstree.externalIO.imports";

    public ImportManager() {

    }

    /**
     * gets the list of file filters
     *
     * @return list of file filters
     */

    static public ArrayList getFileFilter() {
        //MZ: 2006-01-28
        ArrayList filters = new ArrayList();
        String[] importers;
        try {
            importers = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i != importers.length; ++i) {
            if (importers[i].endsWith(".class"))
                try {
                    importers[i] = importers[i].substring(0, importers[i].length() - 6);
                    Class c = Class.forName(packageName.concat(".").concat(importers[i]));
                    if (!c.isInterface()
                            && !Modifier.isAbstract(c.getModifiers())
                            && Importer.class.isAssignableFrom(c)
                            && FileFilter.class.isAssignableFrom(c)) {
                        Importer im = (Importer) c.newInstance();
                        filters.add(im);
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
        }

        return filters;
    }

    /**
     * gets the list of file filters for a given type of nexus block
     *
     * @param blockName
     * @return list of file filters
     */

    static public ArrayList getFileFilter(String blockName) {
        //MZ: 2006-01-28
        ArrayList filters = new ArrayList();
        String[] importers;
        try {
            importers = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i != importers.length; ++i) {
            if (importers[i].endsWith(".class"))
                try {
                    importers[i] = importers[i].substring(0, importers[i].length() - 6);
                    Class c = Class.forName(packageName.concat(".").concat(importers[i]));
                    if (!c.isInterface()
                            && !Modifier.isAbstract(c.getModifiers())
                            && Importer.class.isAssignableFrom(c)
                            && FileFilter.class.isAssignableFrom(c)) {
                        Importer im = (Importer) c.newInstance();
                        if (im.isApplicableToBlock(blockName))
                            filters.add(im);
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                    // throw new SplitsException("Import failed: "+ex.getMessage());
                }
        }

        return filters;
    }

    /**
     * import data from the named file
     *
     * @param file
     * @return nexus version of data
     * @throws IOException
     * @throws SplitsException
     */
    static public String importData(File file) throws Exception {
        return importData(file, null);
    }

    /**
     * import data from the named file
     *
     * @param file
     * @param dataType
     * @return nexus version of data
     * @throws IOException
     * @throws SplitsException
     */
    static public String importData(File file, String dataType) throws Exception {
        //MZ: 2006-01-28
        String[] importers;
        try {
            importers = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i != importers.length; ++i) {
            if (importers[i].endsWith(".class"))
                try {
                    importers[i] = importers[i].substring(0, importers[i].length() - 6);
                    Class c = Class.forName(packageName.concat(".").concat(importers[i]));
                    if (!c.isInterface()
                            && !Modifier.isAbstract(c.getModifiers())
                            && Importer.class.isAssignableFrom(c)
                            && FileFilter.class.isAssignableFrom(c)) {
                        Importer importer = (Importer) c.newInstance();
                        if (dataType != null)
                            importer.setDatatype(dataType);
                        FileReader r = new FileReader(file);
                        try {
                            if (r != null && importer.isApplicable(r)) {
                                System.err.println("Attempting to import in format: " +
                                        Basic.getShortName(importer.getClass()));
                                r.close();
                                r = new FileReader(file);

                                String nexusForm = importer.apply(r);
                                return nexusForm + "\n[" + Basic.getShortName(importer.getClass()) + "]";
                            }
                        } catch (Exception ex) {
                            new Alert("Import in format: " + Basic.getShortName(importer.getClass()) + " FAILED");
                            Basic.caught(ex);
                        }
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
        }

        throw new SplitsException("No suitable Importer found");
    }

    /**
     * import data from a source string
     *
     * @param source
     * @return nexus
     */
    static public String importDataFromString(String source) {
        return importDataFromString(source, null);
    }

    /**
     * import data from a source string
     *
     * @param source
     * @param dataType
     * @return String
     */
    static public String importDataFromString(String source, String dataType) {
        //MZ: 2006-01-28
        String[] importers;
        try {
            importers = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i != importers.length; ++i) {
            if (importers[i].endsWith(".class")) {
                try {
                    importers[i] = importers[i].substring(0, importers[i].length() - 6);
                    Class c = Class.forName(packageName.concat(".").concat(importers[i]));
                    if (!c.isInterface()
                            && !Modifier.isAbstract(c.getModifiers())
                            && Importer.class.isAssignableFrom(c)
                            && FileFilter.class.isAssignableFrom(c)) {
                        Importer importer = (Importer) c.newInstance();
                        if (dataType != null)
                            importer.setDatatype(dataType);
                        StringReader r = new StringReader(source);
                        if (importer.isApplicable(r)) {
                            System.err.print("Importing " +
                                    importer.getClass().getName() + ": ");
                            r.close();
                            r = new StringReader(source);

                            String result = importer.apply(r);
                            if (result != null)
                                return result;
                        }
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
        return null;
    }
}
