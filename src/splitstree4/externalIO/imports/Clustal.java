/*
 * Clustal.java Copyright (C) 2022 Daniel H. Huson
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

import splitstree4.nexus.Characters;

import javax.swing.filechooser.FileFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: Nov 26, 2003
 * Time: 9:52:12 AM
 * To change this template use Options | File Templates.
 */
public class Clustal extends FileFilter implements Importer {

    public final String Description = "Clustal Alignment Files (*.aln)";

    String datatype = Characters.Datatypes.UNKNOWN;

    /**
     * does this importer apply to the type of nexus block
     *
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Characters.NAME);
    }


    /**
     * can we import this data?
     *
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input0) throws IOException {
        BufferedReader input = new BufferedReader(input0);
        String aline = input.readLine();

        return aline != null && aline.toLowerCase().contains("clustal");
    }


    /**
     * convert input into nexus format
     *
     * @return nexus
     */
    public String apply(Reader input) throws Exception {

		BufferedReader in = new BufferedReader(input);
		StringBuilder names = new StringBuilder();
		StringBuilder sequences = new StringBuilder();
		// get rid of the first line
		String aLine = in.readLine();
		if (aLine.toLowerCase().contains("clustal")) in.readLine();
		int nchar = 0, ntax = 0;

		// read first block
		while ((aLine = in.readLine()).length() == 0 || aLine.charAt(0) == ' ')
			;

		do {
            StringTokenizer st = new StringTokenizer(aLine);
            String label = st.nextToken();
            String seq = st.nextToken();
            names.append(label).append("\n");
            int pos = aLine.lastIndexOf(" ");
            if (pos != -1 && pos < aLine.length() - 1 && Character.isDigit(aLine.charAt(pos + 1)))
				sequences.append(aLine, 0, pos).append("\n");
            else
                sequences.append(aLine).append("\n");
            ntax++;
            if (ntax == 1)
                nchar = seq.length();
        } while (!((aLine = in.readLine()).length() == 0 || aLine.charAt(0) == ' '));
        // read rest of the alignment
        while (aLine != null) {
            while ((aLine = in.readLine()) != null && (aLine.length() == 0 || aLine.charAt(0) == ' '))
                ;

            sequences.append("\n");
            if (aLine != null)
                do {
                    for (int i = 1; i <= ntax; i++) {
                        StringTokenizer st = new StringTokenizer(aLine);
                        String label = st.nextToken();
                        String seq = st.nextToken();
                        if (i == 1)
                            nchar += seq.length();
                        int pos = aLine.lastIndexOf(" ");
                        if (pos != -1 && pos < aLine.length() - 1 && Character.isDigit(aLine.charAt(pos + 1)))
							sequences.append(aLine, 0, pos).append("\n");
                        else
                            sequences.append(aLine).append("\n");
                        aLine = in.readLine();
                    }
                } while ((aLine = in.readLine()) != null && !(aLine.length() == 0 || aLine.charAt(0) == ' '));
        }
        // generate String
        return "#nexus\nbegin taxa;\n dimensions ntax=" + ntax + ";\n" + "taxlabels\n" + names + ";\nend;\n" + "begin characters;\n" + "dimensions ntax=" + ntax + " nchar=" + nchar + ";\n" + "format datatype=" + getDatatype() + " interleave labels;\n" + "matrix\n" + sequences + ";\nend;\n";
    }


    /**
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("aln"))
                        return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        return false;
    }


    /**
     * @return description of file matching the filter
     */
    public String getDescription() {
        return Description;
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
