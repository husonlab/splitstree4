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

/** $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 */
package splitstree.analysis.characters;


import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.util.CharactersUtilities;

/**
 * Basic statistics for characters
 */
public class GeneDiversity implements CharactersAnalysisMethod {
    public static String DESCRIPTION = "Computes gene diversity statistics for haplotype sequences";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc) {
        return true;
    }

    
    /**
     * Runs the analysis
     *
     * @param doc
     */
    public String apply(Document doc) {
        //First identify haplotypes in the data.
        Taxa taxa = doc.getTaxa();
        Characters chars = doc.getCharacters();
        int ntax = taxa.getNtax();

        int typecount = 0;

        int[] taxaTypes = new int[taxa.getNtax() + 1];

        //Use a breadth-first search to identify classes of identical sequences or distance matrix rows.
        for (int i = 1; i <= ntax; i++) {
            if (taxaTypes[i] != 0)  //Already been 'typed'
                continue;
            typecount++;
            taxaTypes[i] = typecount;
            for (int j = i + 1; j <= ntax; j++) {
                if (taxaTypes[j] != 0)
                    continue;
                boolean taxaIdentical = CharactersUtilities.taxaIdentical(chars,i,j);
                if (taxaIdentical) {
                    taxaTypes[j] = typecount;
                }
            }
        }

        double[] freqs = new double[typecount+1];
        for (int i=1;i<=ntax;i++) {
            freqs[taxaTypes[i]]+=1.0/ntax;
        }

        double sumSquares = 0.0;
        double sumCubes = 0.0;
        for (int i=1;i<=typecount;i++) {
            double p_i = freqs[i];
            sumSquares += p_i*p_i;
            sumCubes += p_i*p_i*p_i;
        }

        double H = (double) ntax / (ntax - 1.0) * (1.0 - sumSquares);
        double v = 2.0 / ((ntax - 1.0) * ntax) * (2.0 * (ntax - 2.0) * (sumCubes - sumSquares * sumSquares) + sumSquares - (sumSquares * sumSquares));

        String result = "Gene Diversity\n";
        result+="Gene diversity, treating each sequence as a single haplotype\n";
        result+="Gene diversity estimate of H: "+H;
        result+="\tVar(H): "+v+"\ts.d.(H): "+Math.sqrt(v)+"\t\t(Nei 1987, p.g. 180)\n";
        return result;
    }

}

// EOF
