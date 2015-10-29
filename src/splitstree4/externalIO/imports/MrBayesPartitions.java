/**
 * MrBayesPartitions.java
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
package splitstree4.externalIO.imports;

import jloda.util.Alert;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * imports MrBayes Partitions
 * Dave Bryant
 */
public class MrBayesPartitions extends FileFilter implements Importer {
    String datatype = Characters.Datatypes.UNKNOWN;
    public static String DESCRIPTION = "Imports MrBayes Partitions";

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Splits.NAME);
    }


    /**
     * can we import this data?
     *
     * @param input
     * @return true, if can handle this import
     *         Checks by trying to read in the first split.
     */
    public boolean isApplicable(Reader input) throws Exception {
        try {
            BufferedReader br = new BufferedReader(input);
            br.readLine();
            //Skip header
            for (int i = 0; i < 6; i++)
                br.readLine();

            //Read in a single line.
            String line = br.readLine();
            if (line == null || line.length() == 0)
                return false;
            //Read the row number. Just returns stuff between tabs.
            StringTokenizer st = new StringTokenizer(line);

            //First token is row number: this will thrown an exception
            // if there is an error.
            int rowNum = Integer.parseInt(st.nextToken());

            //Nxt token is the flags for the sets.
            String split = st.nextToken();
            int ntax = split.length();

            TaxaSet A = new TaxaSet();
            for (int i = 0; i < ntax; i++)
                if (split.charAt(i) == '*')
                    A.set(i);
                else if (split.charAt(i) != '.')
                    return false;

            //Next token is the count
            String count = st.nextToken();
            int num = Integer.parseInt(count);

            //Next token is the proportion of samples. We use
            //this on the first split to estimate the number
            //of samples.    count/N = prob => N = count/prob
            double prob = Double.parseDouble(st.nextToken());

            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            return false;
        }
    }


    /**
     * convert input into nexus format
     *
     * @param input
     * @return nexus string
     */
    public String apply(Reader input) throws Exception {
        BufferedReader br = new BufferedReader(input);
        try {
            //Skip header
            for (int i = 0; i < 6; i++)
                br.readLine();

            boolean done = false;
            int ntax = 0;
            int nsamples = 0;

            Splits splits = new Splits();

            while (!done) {
                String line = br.readLine();
                if (line == null || line.length() == 0)
                    break;
                //Read the row number. Just returns stuff between tabs.
                StringTokenizer st = new StringTokenizer(line);

                //First token is row number: skip it.
                //String rowNum = st.nextToken();
                st.nextToken();

                //Nxt token is the flags for the sets.
                String split = st.nextToken();
                if (ntax == 0) {
                    ntax = split.length();
                    splits.setNtax(ntax);
                } else {
                    if (ntax != split.length())
                        throw new IOException("line: Split encoding of different lengths");
                }
                TaxaSet A = new TaxaSet();
                for (int i = 0; i < ntax; i++)
                    if (split.charAt(i) == '*')
                        A.set(i);

                //Next token is the count
                String count = st.nextToken();
                int num = Integer.parseInt(count);

                //Next token is the proportion of samples. We use
                //this on the first split to estimate the number
                //of samples.    count/N = prob => N = count/prob
                if (nsamples == 0) {
                    String probString = st.nextToken();
                    double prob = Double.parseDouble(probString);
                    nsamples = (int) (Math.floor((double) num) / prob);
                }
                splits.add(A, num);

            }


            StringWriter result = new StringWriter();
            result.write("#NEXUS\n\n");

            //Now we need to create a synthetic taxa block.
            Taxa taxa = new Taxa();
            for (int i = 0; i < ntax; i++)
                taxa.add("taxon" + (i + 1));

            taxa.write(result);

            result.write("\n\n");

            splits.write(result, taxa);

            result.write("begin st_assumptions;\n");
            result.write("\tsplitspostprocess filter=weight value=" + ((double) nsamples / 3.0) + ";\n");
            result.write("end;\n\n\n");

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {

        List extensions = new LinkedList();
        extensions.add("parts");
        return extensions;
    }


    /**
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            try {// Get the file extension
                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("parts"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * @return description of file matching the filter
     */
    public String getDescription() {
        return "MrBayes partition files (*.parts)";
    }


    /**
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

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }


    /**
     * Attempt to extract the taxa block from the nexus file associated to this partitions file.
     * The default file is the given file, minus the '.parts' extension (as is the case with MrBayes
     * at the moment. If the user wants another file, they're probably better off cutting and pasting!
     * The file is read in, but we only use the taxa block. This then replaces the taxa block in the doc.
     * The numbers of taxa matches.... its up to MrBayes to get the order right! (which it does)
     *
     * @param mainViewerFrame Frame to place alerts and dialog
     * @param file
     * @param doc
     */
    public static void extractTaxa(JFrame mainViewerFrame, File file, Document doc) {


        String filename = file.toString();

        //The name of the nexus file should be the given filename minus the '.parts' extension.
        int dot = filename.lastIndexOf(".");
        filename = filename.substring(0, dot);
        File taxaFile = new File(filename);
        String[] buttons = {"No", "Yes"};

        //Ask the user whether we should input the taxa.
        int n = JOptionPane.showOptionDialog(mainViewerFrame,
                "Import taxon names from \n" + taxaFile.toString() + "?",
                "Import Taxa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                buttons, buttons[1]);


        if (n == 1) {     //User said yes.
            Document taxadoc = new Document();             //Document to extract taxa from
            boolean valid = false;                         //Have we successfully read stuff in yet?

            //First try and read as a NEXUS file.
            try {
                NexusStreamParser fp = new NexusStreamParser(new FileReader(taxaFile));
                taxadoc.readNexus(fp);   //Read in the file
                valid = true;

            } catch (FileNotFoundException e) {
                new Alert(mainViewerFrame, "File not found: " + e.getMessage());
                return;
            } catch (Exception e) { //Not valid NEXUS
            }

            //If that didn't work, try and import it.
            if (!valid) {
                try {
                    String input = ImportManager.importData(taxaFile);
                    taxadoc.readNexus(new NexusStreamParser(new StringReader(input)));
                    valid = true;
                } catch (Exception e) {
                }
            }
            if (valid) {
                //Check if the number of taxa matches
                if (doc.getSplits() != null && doc.getSplits().getNtax() != taxadoc.getTaxa().getNtax()) {
                    String msg = "The number of taxa in \n" + file.toString() + "\ndiffers from the number of taxa";
                    msg += " in \n" + taxaFile.toString() + "\nUsing default names instead";
                    new Alert(mainViewerFrame, msg);
                } else
                    doc.setTaxa(taxadoc.getTaxa());
            }
        }
    }
}
