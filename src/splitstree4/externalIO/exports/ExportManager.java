/**
 * ExportManager.java
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
 * manages the exporters
 */
/**
 * manages the exporters
 */
package splitstree4.externalIO.exports;

import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * manages the importers
 */
public class ExportManager {

    static String packageName = "splitstree.externalIO.exports";

    /**
     * gets the list of exporters
     *
     * @return all valid epxorers
     * @throws Exception
     */
    public static ArrayList getExporter() throws Exception {
        ArrayList exporter = new ArrayList();
        String[] exporters;

        try {
            exporters = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            return null;
        }

        for (int i = 0; i != exporters.length; ++i) {
            if (exporters[i].endsWith(".class")) {
                exporters[i] = exporters[i].substring(0, exporters[i].length() - 6);
                Class c = Class.forName(packageName.concat(".").concat(exporters[i]));
                if (!c.isInterface()
                        && !Modifier.isAbstract(c.getModifiers())
                        && Exporter.class.isAssignableFrom(c))
                    try {
                        Exporter e = (Exporter) c.newInstance();
                        exporter.add(e);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                        throw new SplitsException("Export failed: " + ex.getMessage());
                    }
            }
        }

        return exporter;

//        ArrayList exporter = new ArrayList();
//        String location = Basic.getPackageLocation(packageName);
//        File[] exporters = (new File(location)).listFiles();
//        for (int i = 0; i < exporters.length; i++) {
//            File file = exporters[i];
//            if (file.getName().endsWith(".class")) {
//                try {
//                    String className = packageName + "."
//                            + file.getName().substring(0, file.getName().length() - 6);
//                    Class c = Class.forName(className);
//                    if (c.isInterface() == false) {
//                        Object obj = c.newInstance();
//                        if (obj instanceof Exporter) {
//                            Exporter ex = (Exporter) obj;
//                            exporter.add(ex);
//                        }
//                    }
//                } catch (Exception ex) {
//                    throw new SplitsException("Export failed: " + ex.getMessage());
//                }
//            }
//        }
//        return exporter;
    }


    /**
     * get list of exporter names
     *
     * @return list of exporter names
     * @throws SplitsException
     */
    public static ArrayList getExportNames() throws Exception {
        return getExportNames(null);
    }

    /**
     * get list of exporter names   for named block or all, if block=null
     *
     * @param block
     * @return list of exporter names
     * @throws SplitsException
     */
    public static ArrayList getExportNames(String block) throws Exception {
        //MZ: 2006-01-28
        List blocks = new LinkedList();
        if (block != null)
            blocks.add(block);

        ArrayList names = new ArrayList();
        String[] exporters;
        try {
            exporters = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            Basic.caught(ex);
            return null;
        }

        for (int i = 0; i != exporters.length; ++i) {
            if (exporters[i].endsWith(".class")) {
                exporters[i] = exporters[i].substring(0, exporters[i].length() - 6);
                Class c = Class.forName(packageName.concat(".").concat(exporters[i]));
                if (!c.isInterface()
                        && !Modifier.isAbstract(c.getModifiers())
                        && Exporter.class.isAssignableFrom(c)) {
                    Exporter e = (Exporter) c.newInstance();
                    if (blocks.size() == 0 || e.isApplicable(null, blocks))
                        names.add(getExportName(e));
                }
            }
        }

        return names;

//        List blocks = new LinkedList();
//        if (block != null)
//            blocks.add(block);
//        ArrayList names = new ArrayList();
//        String location = Basic.getPackageLocation(packageName);
//        File[] exporters = (new File(location)).listFiles();
//        for (int i = 0; i < exporters.length; i++) {
//            File file = exporters[i];
//            if (file.getName().endsWith(".class")) {
//                {
//                    String className = packageName + "."
//                            + file.getName().substring(0, file.getName().length() - 6);
//                    Class c = Class.forName(className);
//                    if (c.isInterface() == false) {
//                        Object obj = c.newInstance();
//                        if (obj instanceof Exporter) {
//                            // Get the names of the Exporter
//                            Exporter ex = (Exporter) obj;
//                            if (blocks.size() == 0 || ex.isApplicable(null, blocks))
//                                names.add(getExportName(ex));
//                        }
//                    }
//                }
//            }
//        }
//        return names;
    }


    /**
     * gets the list of exporters suitable for the given document
     *
     * @param doc    the document
     * @param blocks names of selected blocks
     * @return
     * @throws SplitsException
     */
    public static String[] getSuitableExporter(Document doc, Collection blocks) throws SplitsException {
        //MZ: 2006-01-27
        ArrayList names = new ArrayList();
        String[] exporters;
        try {
            exporters = Basic.fetchResources(packageName);
        } catch (IOException ex) {
            return null;
        }

        for (int i = 0; i != exporters.length; ++i) {
            if (exporters[i].endsWith(".class")) {
                exporters[i] = exporters[i].substring(0, exporters[i].length() - 6);
                try {
                    Class c = Class.forName(packageName.concat(".").concat(exporters[i]));
                    if (!c.isInterface()
                            && !Modifier.isAbstract(c.getModifiers())
                            && Exporter.class.isAssignableFrom(c)) {
                        Exporter e = (Exporter) c.newInstance();
                        if (e.isApplicable(doc, blocks))
                            names.add(getExportName(e));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new SplitsException("Export failed: " + ex.getMessage());
                }
            }
        }

        return names.size() != 0 ?
                (String[]) names.toArray(new String[names.size()]) :
                new String[]{"No suitable Exporter found"};

//
//        String location = Basic.getPackageLocation(packageName);
//        
//        File[] exporters = (new File(location)).listFiles();
//        for (int i = 0; i < exporters.length; i++) {
//            File file = exporters[i];
//            if (file.getName().endsWith(".class")) {
//                try {
//                    String className = packageName + "."
//                            + file.getName().substring(0, file.getName().length() - 6);
//                    Class c = Class.forName(className);
//                    if (c.isInterface() == false) {
//                        Object obj = c.newInstance();
//                        if (obj instanceof Exporter) {
//                            Exporter ex = (Exporter) obj;
//                            if (ex.isApplicable(doc, blocks))
//                                names.add(getExportName(ex));
//                        }
//                    }
//                } catch (Exception ex) {
//                    throw new SplitsException("Export failed: " + ex.getMessage());
//                }
//
//            }
//        }
//        String[] tmp = {"No suitable Exporter found"};
//        if (names.size() != 0) {
//            tmp = new String[names.size()];
//            for (int i = 0; i < tmp.length; i++) {
//                tmp[i] = (String) names.get(i);
//            }
//        }
//        return tmp;
    }

    /**
     * export file using the named exporter
     *
     * @param saveFile
     * @param appendFile
     * @param exporterName
     * @param blocks       list of blocks to be exported
     * @param doc
     * @return mapping from names assigned in export to original names
     * @throws Exception
     */
    public static Map exportData(File saveFile, boolean appendFile, boolean exportAll, String exporterName, Collection blocks, Document doc) throws Exception {
        return exportData(saveFile, appendFile, exportAll, exporterName, blocks, doc, null);
    }


    /**
     * export file using the named exporter
     *
     * @param saveFile
     * @param appendFile
     * @param exporterName
     * @param blocks         list of blocks to be exported
     * @param doc
     * @param additionalInfo Additional information to be passed to exporter
     * @return mapping from names assigned in export to original names
     * @throws Exception
     */
    public static Map exportData(File saveFile, boolean appendFile, boolean exportAll, String exporterName, Collection blocks, Document doc, ExporterInfo additionalInfo) throws Exception {
        String className;
        if (exporterName.contains("."))
            className = exporterName;
        else
            className = packageName + "." + exporterName;

        Class c = Class.forName(className);
        if (!c.isInterface()) {
            Object obj = c.newInstance();
            if (obj instanceof Exporter) {
                Exporter exporter = (Exporter) obj;
                exporter.setOptionExportAll(exportAll);
                //exporter.setAdditionalInfo(additionalInfo);
                // try to open a file
                Writer w = new FileWriter(saveFile, appendFile);

                Map exportNames2originalNames;
                if (additionalInfo == null)
                    exportNames2originalNames = exporter.apply(w, doc, blocks);
                else
                    exportNames2originalNames = exporter.apply(w, doc, blocks, additionalInfo);

                System.err.println("Exported data in format: " + exporterName);
                w.close();
                return exportNames2originalNames;
            }
        }
        throw new SplitsException("Unknown or illegal Exporter: " + exporterName);
    }

    /**
     * Takes the name of and exporter and returns an instance of the Exporter object. Returns
     * null if there are problems/
     *
     * @param exporterName String
     * @return ExporterAdapter object
     */
    static public ExporterAdapter getExporterAdapterByName(String exporterName) {
        String className;
        if (exporterName.contains("."))
            className = exporterName;
        else
            className = packageName + "." + exporterName;

        try {
            Class c = Class.forName(className);
            if (!c.isInterface()) {
                Object obj = c.newInstance();
                if (obj instanceof ExporterAdapter)
                    return (ExporterAdapter) obj;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * gets the name of an exporter
     *
     * @return exporter name
     */
    static public String getExportName(Object exporter) {
        return exporter.getClass().getName().replaceAll(".*\\.", "");
    }
}
