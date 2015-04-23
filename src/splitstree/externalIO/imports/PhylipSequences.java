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
 * imports sequences in phylip format
 * @version $Id: PhylipSequences.java,v 1.19 2008-03-14 14:05:22 bryant Exp $
 * @author huson
 * Date: Oct 5, 2003
 */
package splitstree.externalIO.imports;

import splitstree.nexus.Characters;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * imports sequences in phylip format.
 * <p/>
 * <p/>
 * Surprisingly, this turns out to be a difficult format to read, because there are three versions
 * of the syntax:
 * <p/>
 * In all three the first line contains the ntax followed by the nchars. Also, taxa names are always
 * 10 characters long (including padded spaces). Spaces in the sequences themselves are ignored.
 * <p/>
 * In STANDARD, format is
 * <ntax> <nchars>
 * <taxaname (10)> <seq length nchars, possibly with white space and eols>
 * <taxaname (10)> <seq length nchars, possibly with white space and eols>
 * <taxaname (10)> <seq length nchars, possibly with white space and eols>
 * ...
 * <taxaname (10)> <seq length nchars, possibly with white space and eols>
 * <p/>
 * in INTERLEAVED_MULTI format is
 * <p/>
 * <ntax> <nchars>
 * <taxaname (10)> <seq length m1<=nchars, possibly with white space, but no eol> <eol>
 * <taxaname (10)> <seq length m1<=nchars, possibly with white space, but no eol> <eol>
 * ...
 * <taxaname (10)> <seq length m2<=nchars-m1, possibly with white space, but no eol> <eol>
 * [whitespace followed by eol]
 * <taxaname (10)> <seq length m2<=nchars-m1, possibly with white space, but no eol> <eol>
 * <taxaname (10)> <seq length m2<=nchars-m1, possibly with white space, but no eol> <eol>
 * ...
 * <taxaname (10)> <seq length m2<=nchars-m1, possibly with white space, but no eol> <eol>
 * [whitespace]
 * ...
 * [whitespace, folowed by eol]
 * <taxaname (10)> <seq length mk=nchars-m1-m2-..., possibly with white space, but no eol> <eol>
 * <taxaname (10)> <seq length mk=nchars-m1-m2-..., possibly with white space, but no eol> <eol>
 * ...
 * <taxaname (10)> <seq length mk=nchars-m1-m2-..., possibly with white space, but no eol> <eol>
 * <p/>
 * in INTERLEAVED format is same as INTERLEAVED_MULTI except that after the first block, taxa
 * names do not appear.
 * <p/>
 * How to distinguish them:
 * <p/>
 * Read ntax, nchar
 * <p/>
 * Read in first ntax lines. If they all have the same length (excluding whitespace after the first 10
 * characters) and this length is < 10+nchar then
 * if the beginning of the next non-empty line is same as beginning of first line then
 * INTERLEAVED_MULTI
 * else
 * INTERLEAVED
 * else STANDARD
 */
//TODO: Implement the above selection scheme.

public abstract class PhylipSequences extends FileFilter implements Importer {
    String datatype = Characters.Datatypes.UNKNOWN;
    public static String DESCRIPTION = "Imports sequences in Phylip format";

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Characters.NAME);
    }

    /**
     * reads the dimensions sequences in PhylipSequences format. Expects first line to
     * contain ntax and nchar,
     *
     * @param r the reader
     */
    private static int[] readDimensions(Reader r)
            throws IOException {
        int[] dimensions = new int[2];
        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.eolIsSignificant(true);
        st.whitespaceChars(0, 32);
        st.wordChars(33, 126);

        st.nextToken();
        dimensions[0] = (Integer.parseInt(st.sval));
        st.nextToken();
        dimensions[1] = Integer.parseInt(st.sval);
        while (st.ttype != StreamTokenizer.TT_EOL) st.nextToken();
        return dimensions;
    }


    /**
     * can we import this data?
     *
     * @param input
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input) throws Exception {
        try {
            readDimensions(input);
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
        try {
            int[] dimensions = readDimensions(input);
            int nchar = dimensions[1];
            int ntax = dimensions[0];
            BufferedReader br = new BufferedReader(input);

            return readMatrix(br, ntax, nchar);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    protected abstract String readMatrix(BufferedReader br, int ntax, int nchar) throws IOException;


    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {

        List extensions = new LinkedList();
        extensions.add("phy");
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
                    if (extension.equalsIgnoreCase("phy"))
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
        return "PhylipSequences Alignment Files (*.phy)";
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
     * numberNonWhitespace
     * <p/>
     * Takes a string and returns the number of characters in the string that are not whitespace
     *
     * @param s input string
     * @return int number of chars in string that are not whitespace
     */
    protected int numberNonWhitespace(String s) {
        int n = s.length();
        int c = 0;
        for (int i = 0; i < n; i++)
            if (!Character.isWhitespace(s.charAt(i)))
                c++;
        return c;
    }


}
