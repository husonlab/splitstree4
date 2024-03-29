/*
 * Matlab.java Copyright (C) 2022 Daniel H. Huson
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

import javax.swing.filechooser.FileFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: Nov 25, 2003
 * Time: 2:16:54 PM
 * To change this template use Options | File Templates.
 */
public class Matlab extends FileFilter implements Importer {
    String datatype = null;

    public static String DESCRIPTION = "Imports data from a specific Matlab format (development only)";

    /**
     * does this importer apply to the type of nexus block
     *
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return false;
    }

    /**
     * can we import this data?
     *
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input) throws Exception {
        try {
            BufferedReader bf = new BufferedReader(input);
            String aline = bf.readLine().trim();
            return aline.equalsIgnoreCase("%%MATLAB%%");
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * convert input into nexus format
     *
	 */
    public String apply(Reader input) throws Exception {
        StreamTokenizer st = new StreamTokenizer(input);
        st.resetSyntax();
        st.eolIsSignificant(false);
        st.whitespaceChars(0, 32);
        st.wordChars(33, 126);
        st.commentChar('%'); /* Everything starting with % is ignored */

        //First item is the number of taxa.
        StringBuilder taxaBuf = new StringBuilder();

        st.nextToken();
        int ntax = Integer.parseInt(st.sval);
        taxaBuf.append("#nexus\nbegin taxa;\n dimensions ntax=").append(ntax).append(";\nTAXLABELS\n");
        for (int i = 0; i < ntax; i++) {
            st.nextToken();
            taxaBuf.append(st.sval).append("\n");

        }
        taxaBuf.append(";\nend;\n");

        //Now the split design matrix, which we use to extract the splits.
        //First token is the number of splits. This is followed by a row of split weights.

        StringBuilder splitsBuf = new StringBuilder();
        st.nextToken();
        int nsplits = Integer.parseInt(st.sval);

        double[] splitWeights = new double[nsplits + 1];
        for (int i = 1; i <= nsplits; i++) {
            st.nextToken();
            splitWeights[i] = Double.parseDouble(st.sval);
        }

        int[][] splitArray = new int[ntax + 1][nsplits + 1];
        //Top (n-1) rows of the design matrix contain  the entries for (1,2), (1,3),...,(1,n)
        //Use these to extract the splits
        for (int i = 2; i <= ntax; i++) {
            for (int j = 1; j <= nsplits; j++) {
                st.nextToken();
                if (st.sval.equalsIgnoreCase("1"))
                    splitArray[i][j] = 1;
                else if (st.sval.equalsIgnoreCase("0"))
                    splitArray[i][j] = 0;
                else {
                    throw new Exception("Found character ''" + st.sval + "'' in the design matrix");
                }
            }
        }

        splitsBuf.append("BEGIN splits;\n DIMENSIONS nsplits = ").append(nsplits).append(";\n FORMAT labels = no weights = yes; \nMATRIX\n");
        for (int j = 1; j <= nsplits; j++) {
			splitsBuf.append(splitWeights[j]).append("\t");
            for (int i = 2; i <= ntax; i++) {
                if (splitArray[i][j] == 1)
                    splitsBuf.append(" ").append(i);
            }
            splitsBuf.append(",\n");
        }
        splitsBuf.append(";\nend;");

        //Skip to the end of the Split array
        for (int t1 = 2; t1 <= ntax; t1++) {
            for (int t2 = t1 + 1; t2 <= ntax; t2++) {
                for (int j = 1; j <= nsplits; j++)
                    st.nextToken();
            }
        }


        StringBuilder distancesBuf = new StringBuilder();
        distancesBuf.append("BEGIN distances;\n DIMENSIONS ntax=").append(ntax).append(";\n FORMAT triangle=upper no diagonal no labels missing=?;\nMATRIX\n");
        for (int i = 0; i < ntax; i++) {
            for (int j = i + 1; j < ntax; j++) {
                st.nextToken();
                distancesBuf.append(st.sval).append("\t");
            }
            distancesBuf.append("\n");
        }
        distancesBuf.append(";\nend;\n");

        String splitstreeBlock = "begin st_assumptions;\nuptodate;\nend;\n";

		return (taxaBuf.toString() + distancesBuf + splitsBuf + splitstreeBlock);
    }


    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {

        List extensions = new LinkedList();
        extensions.add("dist");
        extensions.add("dst");
        return extensions;
    }

    /**
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            try {
                // Get the file extension

                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("dist")
                            || extension.equalsIgnoreCase("dst"))
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
        return "PhylipSequences Distance Matrix Files (*.dist,*.dst)";
    }


    /**
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     * last ".")
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
}
