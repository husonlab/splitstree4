/*
 * PhylipSequencesInterleaved.java Copyright (C) 2022 Daniel H. Huson
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

import java.io.BufferedReader;
import java.io.IOException;


/**
 * imports sequences in phylip format.
 */

public class PhylipSequencesInterleaved extends PhylipSequences {

    String datatype = Characters.Datatypes.UNKNOWN;
    public static String DESCRIPTION = "Imports sequences in Interleaved Phylip format";


    protected String readMatrix(BufferedReader br, int ntax, int nchar) throws IOException {
        StringBuilder names = new StringBuilder();
		StringBuilder sequencesHeader = new StringBuilder();
		StringBuilder sequences = new StringBuilder();
        names.append("#nexus\nbegin taxa;\n dimensions ntax=").append(ntax).append(";\n");
        names.append("taxlabels\n");

        sequences.append("matrix\n");

        int blockCount = 0;
        String[] taxaNames = new String[ntax + 1];
        boolean eachBlockHasTaxa = true;

        int nread = 0; //Number of sites read in.

        while (nread < nchar) {
            int blockSize = 0; //The number of sites in this block

            for (int i = 1; i <= ntax; i++) {
                //Skip any blank lines
                String tmp = br.readLine();

                //Skip blankspace
                while (tmp != null && tmp.trim().length() == 0)
                    tmp = br.readLine();

                if (tmp == null)
                    throw new IOException("Unexpected end-of-file encountered");


                if (blockCount == 0) {
                    String name;
                    //Read in taxon name. This is the first 10 characters, trimmed to
                    // remove whitespace at either end.
                    if (tmp.length() < 10)
                        name = tmp.trim();
                    else
                        name = tmp.substring(0, 10).trim();
                    names.append("'").append(name).append("'\n");
                    if (blockCount == 0)
                        taxaNames[i] = name;

                } else if ((blockCount == 1) && (i == 1)) {
                    String name;
                    if (tmp.length() < 10)
                        name = tmp.trim();
                    else
                        name = tmp.substring(0, 10).trim();
                    if (!name.equals(taxaNames[1]))
                        eachBlockHasTaxa = false;
                }
                //If this is the first block, or every block has taxa, skip the first 10 lines.
                if (eachBlockHasTaxa)
                    tmp = tmp.substring(10);

                if (i == 1) {
                    blockSize = numberNonWhitespace(tmp);
                    nread += blockSize;
                } else if (numberNonWhitespace(tmp) != blockSize) {
                    String message = "Different taxa have different sequence lengths";
                    throw new IOException(message);
                }
                sequences.append(tmp.replaceAll("[Xx]", "?")).append("\n");


            }
            blockCount++;

            sequences.append("\n");

        }

        //Copy anything else as a comment?
        StringBuilder endComment = new StringBuilder();
        String tmp = br.readLine();

        while (tmp != null && tmp.trim().length() == 0)
            tmp = br.readLine();

        while (tmp != null) {
            endComment.append(tmp);
            tmp = br.readLine();
        }


        names.append(";\nend;\n");
        sequences.append(";\nend;\n");

        sequencesHeader.append("begin characters;\n");
        sequencesHeader.append("dimensions nchar=").append(nchar).append(";\n");
        sequencesHeader.append("format datatype=").append(getDatatype()).append(" interleave=yes labels=no missing = ? gap = -\n");
        sequencesHeader.append(";\n");
        return names.toString() + sequencesHeader + sequences + "\n[\n" + endComment + "\n]\n";
    }


}
