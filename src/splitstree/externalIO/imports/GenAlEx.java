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

import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Nov 25, 2003
 * Time: 2:16:54 PM
 * To change this template use Options | File Templates.
 */


/*

Specifications for microsats

Frequencies can be exact counts. (so convert POP trees into exact counts).
Locus information stored as  Char Partition.


Alternative:
Individual information:

(1) poulation name
(2) locus name
(3) individual id
(4) n1, n2: fragment size or allele name

The input file look like this.

breed145 m120 11 148 148
breed145 m120 12 148 160
breed145 m120 13 148 160


Alternative 'microsat'

<taxon> <locus> <repeatlength> <frequency [default 1]>







DISTANCES:

Da: modified Cavalli-Sforza distance. Nei et al. (1983)   J. Mol. Evol. 19:153-170
   Dst: Nei's standard genetic distance. Nei (1972) Amer. Nat. 106:283-291
   Dmyu: delta myu square  Goldstein et al. (1995) PNAS 92:6723-6727
   Dsw: Shriver et al. (1995) Genetics 134:983-993

 Dmyu and Dsw are applicable only for microsatellite data.


-h option: The heterozygosities and Gst can be computed by -h option.


  The sample size is taken into account for hetetozygosities by the formula
(8,4) and for Gst by the formulae (8.31) and (8.32) in Molecular Evolutionary
Genetics (Nei 1987). The harmonic mean of sample sizes of populations is taken
for computation of (8.31) and (8.32).

 The variance of Gst is computed by the jackknife method.



 also POPULATION DISTANCES:
 	Creates a new window with taxa = populations.
 	
 */




public class GenAlEx extends FileFilter implements Importer {
    String datatype = null;
    private int nLoci;
    private int nSamples;
    private int nPops;
    private int[] popSizes;
    private String[] popNames;
    private String[] lociNames;
    private String[] taxaNames;
    private String[][] data;
    private boolean isDiploid;


    public static String DESCRIPTION = "Imports microsat data in GenAlEx format";

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName Nexus block
     * @return true, if can handle import data for this kind of block
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equals(Taxa.NAME) || blockName.equals(Characters.NAME);
    }

    /**
     * Prepare StreamTokenizer. Set quotes, and only word divisions are tabs and eolns/eofs
     *
     * @param input Reader
     * @return StreamTokenizer
     */
    protected StreamTokenizer prepareStream(Reader input) {
        StreamTokenizer st = new StreamTokenizer(input);
        st.resetSyntax();
        st.quoteChar('\'');
        st.wordChars(33, 126);
        st.whitespaceChars('\t', '\t');
        st.eolIsSignificant(true);
        return st;
    }


    /**
     * can we import this data?
     *
     * @param input Reader (reset afterwards)
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input) throws Exception {
        try {
            BufferedReader br = new BufferedReader(input);
            return readFirstLine(br);
        } catch (Exception ex) {
            return false;
        }
    }

    boolean readFirstLine(BufferedReader br) throws IOException {
        /* String s = "The end\tof\t the world\t is \t\t\tnigh";
       String[] result = s.split("\\t");
       for(int x=0;x<result.length;x++) {
           System.out.println(result[x]);
       } */

        String line = br.readLine();
        String[] tokens = line.split("\\t");
        int ntokens = tokens.length;
        if (ntokens < 3)
            return false;

        nLoci = Integer.parseInt(tokens[0]);
        nSamples = Integer.parseInt(tokens[1]);
        nPops = Integer.parseInt(tokens[2]);
        if (ntokens < 3 + nPops)
            return false;
        popSizes = new int[nPops];
        int n = 0;
        for (int i = 0; i < nPops; i++) {
            popSizes[i] = Integer.parseInt(tokens[3 + i]);
            n += popSizes[i];
        }
        return n == nSamples;

        //TODO: In Mat Godards data there are two extra fields, perhaps for the samples in *this* file???
    }


    /**
     * convert input into nexus format
     *
     * @param input
     * @return
     */
    public String apply(Reader input) throws Exception {
        BufferedReader br = new BufferedReader(input);
        if (!readFirstLine(br))
            return "";

        String lineTwoComment = br.readLine();

        String thirdLine = br.readLine();
        String[] tokens = thirdLine.split("\\t");

        //Now the locus names. If these are separated by blank columns then it means diploid.
        lociNames = new String[nLoci + 1];
        if (tokens[3].trim().length() == 0) {
            isDiploid = true;
            for (int i = 1; i <= nLoci; i++)
                lociNames[i] = tokens[2 * i];
        } else {
            isDiploid = false;
            System.arraycopy(tokens, 2, lociNames, 1, nLoci);
        }

        //Now the matrix. First two columns are taxon name and population. After that is the data.

        taxaNames = new String[nSamples + 1];
        popNames = new String[nSamples + 1];
        int nColumns = nLoci;
        if (isDiploid)
            nColumns *= 2;
        data = new String[nSamples + 1][nColumns + 1];

        for (int i = 1; i <= nSamples; i++) {
            tokens = br.readLine().split("\\t");
            taxaNames[i] = tokens[0];
            popNames[i] = tokens[1];

            System.arraycopy(tokens, 2, data[i], 1, nColumns);
        }

        //Now output

        StringBuilder out = new StringBuilder();
        out.append("#NEXUS\n\n");
        out.append("begin TAXA;\nDIMENSIONS ntax = ").append(nSamples).append(";\nTAXLABELS\n");
        for (int i = 1; i <= nSamples; i++) {
            out.append("\t'").append(taxaNames[i]).append("'\n");
        }
        out.append(";\nEND;\n\n");

        out.append("begin TRAITS;\n");
        out.append("\tDIMENSIONS nColumns = 1;\n");
        out.append("\tFORMAT Labels = yes POPULATIONHEADER = Population;\n");
        out.append("\tMATRIX\n\tPopulation\n");
        for (int i = 1; i <= nSamples; i++) {
            out.append(" ").append(taxaNames[i]).append("\t").append(popNames[i]).append("\n");
        }
        out.append(";\nEND;\n\n");

        out.append("begin CHARACTERS;\n");
        out.append("DIMENSIONS nChar = ").append(nColumns).append(";\n");
        out.append("FORMAT datatype = microsat ");
        if (isDiploid)
            out.append("diploid = yes ");
        out.append("tokens = yes labels = left missing = 0;\n");
        out.append("MATRIX\n");
        for (int i = 1; i <= nSamples; i++) {
            out.append(taxaNames[i]).append(" ");
            for (int j = 1; j <= nColumns; j++) {
                out.append(data[i][j]).append("\t");
            }
            out.append("\n");
        }
        out.append(";\nEND;\n\n");
        System.out.println(out.toString());
        return out.toString();
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
        return "GenAlEx tab delimited files (*.txt), microsattelite data";
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
