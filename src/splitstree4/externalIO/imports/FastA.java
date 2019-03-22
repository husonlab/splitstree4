/**
 * FastA.java
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
 * inputs data given in fasta format
 *
 * @version $Id: FastA.java,v 1.17 2006-10-25 21:01:02 bryant Exp $
 * @author huson
 * Date: Sep 29, 2003
 */
/**
 * inputs data given in fasta format
 * @version $Id: FastA.java,v 1.17 2006-10-25 21:01:02 bryant Exp $
 * @author huson
 * Date: Sep 29, 2003
 */
package splitstree4.externalIO.imports;

import jloda.swing.util.Alert;
import jloda.util.Basic;
import splitstree4.nexus.Characters;
import splitstree4.util.CharactersUtilities;

import javax.swing.filechooser.FileFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;


/**
 * inputs data given in fasta format
 */
public class FastA extends FileFilter implements Importer {
    String datatype = null;

    public String Description = "FastA files (*.fa,*.fasta)";

    private char gap = '-';
    private char missing = '?';

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
        String aline = input.readLine();

        return aline != null && aline.length() > 0 && aline.charAt(0) == '>';
    }

    /**
     * convert input string into nexus format
     *
     * @param input0
     * @return
     */
    public String apply(Reader input0) throws IOException {
        BufferedReader input = new BufferedReader(input0);

        final List<String> headerLines = new LinkedList<>();
        final List<String> taxonNames = new LinkedList<>();
        final List<String> sequences = new LinkedList<>();

        boolean headerLinesEqualTaxonNames = true;

        boolean isAligned = true;

        boolean warned = false;

        StringBuilder buf = new StringBuilder();
        String aLine;
        int lineNumber = 0;
        while ((aLine = input.readLine()) != null) {
            lineNumber++;
            aLine = aLine.trim();
            if (aLine.length() > 0) {
                if (aLine.contains("[") || aLine.contains("]") || aLine.contains("'") || aLine.contains("\\\"")) {
                    if (!warned) {
                        warned = true;
                        new Alert(null, "File contains illegal characters in line " + lineNumber + ",\nplease fix");
                    }
                }

                if (aLine.startsWith(">")) { // new sequence
                    if (taxonNames.size() > 0) {
                        if (buf.toString().length() > 0) {
                            sequences.add(buf.toString());
                        }
                        else
                            throw new IOException("Zero sequence at end of file");
                    }
                    buf = new StringBuilder();
                    aLine = aLine.substring(1).trim();
                    String name = Basic.getFirstWord(aLine);
                    taxonNames.add(name);
                    headerLines.add(aLine);
                    if (headerLinesEqualTaxonNames && !name.equals(aLine))
                        headerLinesEqualTaxonNames = false;
                } else {
                    for (int i = 0; i < aLine.length(); i++) {
                        if (!Character.isWhitespace(aLine.charAt(i)))
                            buf.append(aLine.charAt(i));
                    }
                }
            }
        }
        // last sequence:
        if (buf.toString().length() > 0) {
            sequences.add(buf.toString());
        }
        else
            throw new IOException("Zero sequence at end of file");

        if (sequences.size() == 0)
            throw new IOException("No sequences found");

        int length = sequences.iterator().next().length();
        for (String seq : sequences) {
            if (seq.length() != length) {
                isAligned = false;
                break;
            }
        }


        StringBuilder result = new StringBuilder("#nexus\n");
        result.append("begin taxa;\n");
        result.append("dimensions ntax=").append(taxonNames.size()).append(";\n");
        result.append("taxlabels\n");
        for (String name : taxonNames) {
            result.append(name).append("\n");
        }
        result.append(";\n");
        if (!headerLinesEqualTaxonNames) {
            result.append("taxinfo\n");
            for (String header : headerLines)
                result.append(header).append("\n");
            result.append(";\n");
        }

        result.append("end;\n");

        if (datatype == null) {
            datatype = CharactersUtilities.guessType(sequences.get(0));
            if (datatype.equals(Characters.Datatypes.PROTEIN))
                setOptionMissing('x');
        }


        if (isAligned) {
            int nchar = sequences.get(0).length();
            result.append("begin characters;\n");
            result.append("dimensions ntax=").append(taxonNames.size()).append(" nchar=").append(nchar).append(";\n");
            result.append("format datatype=").append(getDatatype()).append(" gap=").append(getOptionGap()).append(" missing= ").append(getOptionMissing()).append(" no interleave no labels;\n");
            result.append("matrix\n");

            for (String sequence : sequences) {
                result.append(sequence).append("\n");
            }
            result.append(";\nend;\n");
            System.err.println("Importing aligned sequences: ntax=" + taxonNames.size() + " nchar=" +
                    nchar + " type=" + getDatatype());
        } else {
            System.err.println("Warning: Sequences have different lengths, please check their alignment!");
            result.append("begin unaligned;\n");
            result.append("dimensions ntax=").append(taxonNames.size()).append(";\n");
            result.append("format datatype=").append(getDatatype()).append(" missing= ").append(getOptionMissing()).append(" no labels;\n");
            result.append("matrix\n");


            boolean first = true;
            for (String sequence : sequences) {
                if (first)
                    first = false;
                else
                    result.append(",");
                result.append("\n").append(sequence);
            }
            result.append("\n");

            result.append(";\nend;\n");
            System.err.println("Importing unaligned sequences: ntax=" + taxonNames.size() + " type=" + getDatatype());
        }
        return result.toString();
    }

    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List<String> getFileExtensions() {
        List<String> extensions = new LinkedList<>();
        extensions.add("fa");
        extensions.add("fna");
        extensions.add("dna");
        extensions.add("fasta");
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
                String extension = Basic.getFileSuffix(f.getName());
                if (extension != null && getFileExtensions().contains(extension))
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


    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
