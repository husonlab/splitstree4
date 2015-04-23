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
 * @version $Id: PhylipSequencesSequential.java,v 1.1 2008-03-14 14:05:22 bryant Exp $
 * @author bryant
 * Date: , 2003
 */
package splitstree.externalIO.imports;

import splitstree.nexus.Characters;

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
