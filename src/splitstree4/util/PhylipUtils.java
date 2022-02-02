/*
 * PhylipUtils.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree4.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * stuff for phylip io
 */
public class PhylipUtils {
    /**
     * truncates or pads string to have exactly length len, used for PhylipSequences type io
     *
     * @param str a string
     * @param len the max length
     */
    static public String padLabel(String str, int len) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < len; i++) {
            if (i < str.length())
                result.append(str.charAt(i));
            else
                result.append(' ');
        }
        return result.toString();
    }


    /**
     * reads the dimensions sequences in PhylipSequences format. Expects first line to
     * contain ntax and nchar,
     *
     * @param dimensions ntax and nchar
     * @param r          the reader
     */
    public static void readDimensions(int[] dimensions, Reader r)
            throws IOException {
        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.eolIsSignificant(false);
        st.whitespaceChars(0, 32);
        st.wordChars(33, 126);


        st.nextToken();
        dimensions[0] = (Integer.parseInt(st.sval));
        st.nextToken();
        dimensions[1] = Integer.parseInt(st.sval);
    }

    /**
     * reads sequences in PhylipSequences format. To be precise, first expects ntax and nchar,
     * then expects a taxon name followed by nchar symbols for its sequence
     *
     * @param dimensions array ntax and nchar
     * @param data       array of names and sequences
     * @param r          the reader
     */
    public static void read(int[] dimensions, String[][] data, Reader r) throws IOException {
        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.eolIsSignificant(false);
        st.whitespaceChars(0, 32);
        st.wordChars(33, 126);

        st.nextToken();
        int ntax = Integer.parseInt(st.sval);
        String[] names = data[0] = new String[ntax + 1];
        String[] sequences = data[1] = new String[ntax + 1];
        st.nextToken();
        int nchar = Integer.parseInt(st.sval);
        dimensions[0] = ntax;
        dimensions[1] = nchar;

        for (int i = 1; i <= ntax; i++) {
            st.nextToken();
            if (st.sval.length() <= 10) {
                names[i] = st.sval;
                sequences[i] = "";
            } else {
                names[i] = st.sval.substring(0, 9);
                sequences[i] = st.sval.substring(10);
            }
            while (sequences[i].length() < nchar) {
                st.nextToken();
                sequences[i] += st.sval;
            }
        }
    }

    /**
     * reads sequences in PhylipSequences format. To be precise, first expects ntax and nchar,
     * then expects a taxon name followed by nchar symbols for its sequence
     *
     * @param data array of names and sequences
     * @param r    the reader
     */
    public static void read(String[][] data, Reader r)
            throws IOException {
        int[] dimensions = new int[2];
        read(dimensions, data, r);
    }

    /**
     * print a distance matrix in phylip format
     *
     * @param names taxon names 1..ntax
     * @param dist  distances
     * @param out   stream
     */
    public static void print(String[] names, float[][] dist, PrintStream out) {
        int ntax = names.length - 1;
        // Print phylip distance matrix
        out.println("" + ntax);
        for (int i = 1; i <= ntax; i++) {
            out.print(PhylipUtils.padLabel(names[i], 10));
            for (int j = 1; j <= ntax; j++) {
                out.print(" " + dist[i][j]);
            }
            out.print("\n");
        }

    }

    /**
     * print a sequences in phylip format
     *
     * @param os   stream
     */
    public static void print(String[][] data, PrintStream os) {
        int ntax = data[0].length - 1;
        int nchar = data[1][1].length();
        // Print phylip sequences
        os.println("" + ntax + " " + nchar);
        for (int i = 1; i <= ntax; i++) {
            os.print(PhylipUtils.padLabel(data[0][i], 10));
            os.println(data[1][i]);
        }
    }
}
