/**
 * PhylipSequencesSequential.java
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
 * imports sequences in phylip format
 *
 * @version $Id: PhylipSequencesSequential.java,v 1.1 2008-03-14 14:05:22 bryant Exp $
 * @author bryant
 * Date: , 2003
 */
/**
 * imports sequences in phylip format
 * @version $Id: PhylipSequencesSequential.java,v 1.1 2008-03-14 14:05:22 bryant Exp $
 * @author bryant
 * Date: , 2003
 */
package splitstree4.externalIO.imports;

import splitstree4.nexus.Characters;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * imports sequences in phylip format in non-interleaved(sequential) formar
 */

public class PhylipSequencesSequential extends PhylipSequences {
    String datatype = Characters.Datatypes.UNKNOWN;
    public static String DESCRIPTION = "Imports sequences in (non-interleaved) Phylip format";


    protected String readMatrix(BufferedReader br, int ntax, int nchar) throws IOException {
        StringBuilder names = new StringBuilder("");
        StringBuilder sequencesHeader = new StringBuilder();
        StringBuilder sequences = new StringBuilder("");
        names.append("#nexus\nbegin taxa;\n dimensions ntax=").append(ntax).append(";\n");
        names.append("taxlabels\n");

        sequences.append("matrix\n");


        for (int i = 1; i <= ntax; i++) {
            //Skip any blank lines
            String tmp = br.readLine();
            while (tmp != null && tmp.trim().length() == 0)
                tmp = br.readLine();

            if (tmp == null)
                throw new IOException("Unexpected end-of-file encountered");

            //Read in taxon name. This is i first 10 characters, trimmed to remove whitespace at either end.
            String name;
            if (tmp.length() < 10)
                name = tmp.trim();
            else
                name = tmp.substring(0, 10).trim();
            names.append("'").append(name).append("'\n");

            //Read in the characters. We read in exactly nchar non-Whitespace characters.
            tmp = tmp.substring(10);
            sequences.append(tmp.replaceAll("[Xx]", "?")).append("\n");
            int nread = numberNonWhitespace(tmp);   //number read in.

            while (nread < nchar) {
                tmp = br.readLine();
                sequences.append(tmp).append("\n");
                nread += numberNonWhitespace(tmp);   //number read in.
            }

            if (nread != nchar) //Can only happen if a sequence is longer than nchar
                throw new IOException("Sequence for taxon is longer than specified");

        }

        //Copy anything else as a comment?
        StringBuilder endComment = new StringBuilder();
        String tmp = br.readLine();

        while (tmp != null && tmp.trim().length() == 0)
            tmp = br.readLine();

        while (br.ready())
            endComment.append(br.readLine()).append("\n");


        names.append(";\nend;\n");
        sequences.append(";\nend;\n");

        sequencesHeader.append("begin characters;\n");
        sequencesHeader.append("dimensions ntax=").append(ntax).append(" nchar=").append(nchar).append(";\n");
        sequencesHeader.append("format datatype=").append(getDatatype()).append(" interleave=no labels=no missing = ? gap = -\n");
        sequencesHeader.append(";\n");
        return names.toString() + sequencesHeader + sequences + "\n[\n" + endComment + "\n]\n";
    }


}
