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

package splitstree.externalIO.imports;

import splitstree.nexus.Distances;

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
public class PhylipDistances extends FileFilter implements Importer {
    String datatype = null;

    public static String DESCRIPTION = "Imports distances in Phylip format";

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Distances.NAME);
    }

    /**
     * can we import this data?
     *
     * @param input
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input) throws Exception {
        try {
            BufferedReader bf = new BufferedReader(input);
            String aline = bf.readLine().trim();
            Integer.parseInt(aline);
            return true; // first line consists of precisely one number
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * convert input into nexus format
     *
     * @param input
     * @return
     */
    public String apply(Reader input) throws Exception {
        StreamTokenizer st = new StreamTokenizer(input);
        st.resetSyntax();
        st.eolIsSignificant(false);
        st.whitespaceChars(0, 32);
        st.wordChars(33, 126);
        st.nextToken();
        int ntax = Integer.parseInt(st.sval);
        StringBuilder taxaBuf = new StringBuilder();
        StringBuilder distancesBuf = new StringBuilder();
        taxaBuf.append("#nexus\nbegin taxa;\n dimensions ntax=").append(ntax).append(";\nTAXLABELS\n");
        distancesBuf.append("BEGIN distances;\n DIMENSIONS ntax=").append(ntax).append(";\n FORMAT triangle=both no labels missing=?;\nMATRIX\n");
        for (int i = 0; i < ntax; i++) {
            st.nextToken();
            taxaBuf.append(st.sval).append("\n");
            for (int j = 0; j < ntax; j++) {
                st.nextToken();
                distancesBuf.append(st.sval).append("\t");
            }
            distancesBuf.append("\n");
        }
        distancesBuf.append(";\nend;\n");
        taxaBuf.append(";\nend;\n");
        return (taxaBuf.toString() + distancesBuf.toString());
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
}
