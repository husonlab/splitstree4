/*
 * SingleLineSequences.java Copyright (C) 2022 Daniel H. Huson
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
/**
 * inputs data given in fasta format
 * @version $Id: SingleLineSequences.java,v 1.5 2005-11-12 20:49:13 huson Exp $
 * @author huson
 * Date: Sep 29, 2003
 */
package splitstree4.externalIO.imports;

import splitstree4.nexus.Characters;
import splitstree4.util.CharactersUtilities;

import javax.swing.filechooser.FileFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * inputs data given in fasta format
 */
public class SingleLineSequences extends FileFilter implements Importer {
    String datatype = null;

    public String Description = "Single line sequences (*.txt)";

    private char gap = '-';
    private char missing = '?';
    private String type = null; // will guess this later

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
     * can we import this data?
     *
     * @param input0
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input0) throws IOException {
        BufferedReader input = new BufferedReader(input0);
        String aline;

        int length = -1;
        while ((aline = input.readLine()) != null) {
            if (aline.length() > 0 && !aline.startsWith("#")) {
                if (length == -1) {
                    length = aline.length();
                    if (aline.charAt(0) == '>')
                        return false;
                    if (aline.charAt(0) == '(')
                        return false;
                    for (int pos = 0; pos < aline.length(); pos++)
                        if (Character.isSpaceChar(aline.charAt(pos)))
                            return false;
                } else if (aline.length() != length)
                    return false;
            }
        }
        return length > 0;
    }

    /**
     * convert input string into nexus format
     *
     * @param input0
     * @return
     */
    public String apply(Reader input0) throws IOException {
        BufferedReader input = new BufferedReader(input0);
        List names = new LinkedList();
        List seqs = new LinkedList();
        int count = 0;

        String aline;

        while ((aline = input.readLine()) != null) {
            if (aline.length() > 0 && !aline.startsWith("#")) {
                count++;
                names.add("t" + count);
                seqs.add(aline);
            }
        }
        if (datatype == null) {
            datatype = CharactersUtilities.guessType(aline);
        }

        String result = "#nexus\n";
        result += "begin taxa;\n";
        result += "dimensions ntax=" + count + ";\n";
        result += "taxlabels\n";
        Iterator it = names.iterator();
        while (it.hasNext()) {
            result += it.next() + "\n";
        }
        result += ";\nend;\n";

        if (type == null && count > 0) {
            type = CharactersUtilities.guessType((String) seqs.get(0));
        }

        int nchar = ((String) seqs.get(0)).length();
        result += "begin characters;\n";
        result += "dimensions ntax=" + count + " nchar=" + nchar + ";\n";
        result += "format datatype=" + getDatatype() + " gap=" + getOptionGap()
                + " missing= " + getOptionMissing() + " no interleave no labels;\n";
        result += "matrix\n";
        it = seqs.iterator();
        while (it.hasNext()) {
            result += it.next() + "\n";
        }
        result += ";\nend;\n";
        System.err.println("Importing sequences: ntax=" + count + " nchar=" +
                nchar + " type=" + getDatatype());
        return result;
    }

    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {
        List extensions = new LinkedList();
        extensions.add("txt");
        return extensions;
    }

    /**
     * get current gap character
     *
     * @return gap character
     */
    public char getOptionGap() {
        return gap;
    }

    /**
     * set current gap character
     *
     * @param gap
     */
    public void setOptionGap(char gap) {
        this.gap = gap;
    }

    /**
     * get the missing character
     *
     * @return missing
     */
    public char getOptionMissing() {
        return missing;
    }

    /**
     * sets the missing character
     *
     * @param missing
     */
    public void setOptionMissing(char missing) {
        this.missing = missing;
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
                    if (extension.equalsIgnoreCase("fa")
                            || extension.equalsIgnoreCase("fasta"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public String getDescription() {
        return Description;
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
