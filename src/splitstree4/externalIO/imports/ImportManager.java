/*
 * ImportManager.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.externalIO.imports;

import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.ResourceUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;


/**
 * manages the importers
 * Daniel Huson, 2003
 */
public class ImportManager {

    private static final String packageName = "splitstree4.externalIO.imports";

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
            importers = ResourceUtils.fetchResources(ImportManager.class, packageName);
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
                        Importer im = (Importer) c.getConstructor().newInstance();
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
     * @return list of file filters
     */

    static public ArrayList getFileFilter(String blockName) {
        //MZ: 2006-01-28
        ArrayList filters = new ArrayList();
        String[] importers;
        try {
            importers = ResourceUtils.fetchResources(ImportManager.class, packageName);
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
                        Importer im = (Importer) c.getConstructor().newInstance();
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
     * @return nexus version of data
     */
    static public String importData(File file) throws IOException {
        return importData(file, null);
    }

    /**
     * import data from the named file
     *
     * @return nexus version of data
     */
    static public String importData(File file, String dataType) throws IOException {
        //MZ: 2006-01-28
        String[] importers;
        try {
            importers = ResourceUtils.fetchResources(ImportManager.class, packageName);
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
                        Importer importer = (Importer) c.getConstructor().newInstance();
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

        throw new IOException("No suitable Importer found");
    }

    /**
     * import data from a source string
     *
     * @return nexus
     */
    static public String importDataFromString(String source) {
        return importDataFromString(source, null);
    }

    /**
     * import data from a source string
     *
     * @return String
     */
    static public String importDataFromString(String source, String dataType) {
        //MZ: 2006-01-28
        String[] importers;
        try {
            importers = ResourceUtils.fetchResources(ImportManager.class, packageName);
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
                        Importer importer = (Importer) c.getConstructor().newInstance();
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
