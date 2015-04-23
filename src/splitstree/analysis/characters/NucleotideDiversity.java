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
import splitstree.util.CharactersUtilities;

/**
 * Basic statistics for characters
 */
public class NucleotideDiversity implements CharactersAnalysisMethod {
    public static String DESCRIPTION = "Computes genetic diversity statistics for the characters";

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
        
        int dataType = doc.getCharacters().getFormat().getDatatypeID();
        return (dataType == Characters.Datatypes.DNAID || dataType == Characters.Datatypes.RNAID);
    }
    
    /**
     * Runs the analysis
     *
     * @param doc
     */
    public String apply(Document doc) {
    
        //TODO: There could be issues identifying the haplotypes when the Hamming distance
        //deos not obey the triangle inequality.... e.g. when there are lots of missing
        //or gapped sites.

        //First identify haplotypes in the data.
        Characters chars = doc.getCharacters();
        int ntax = doc.getTaxa().getNtax();

        int typecount = 0;
        int numNonSingleClasses = 0;

        int[] taxaTypes = new int[ntax + 1];
        int[] representatives = new int[ntax + 1]; //Representative taxon of each type.

        //Use a breadth-first search to identify classes of identical sequences or distance matrix rows.
        for (int i = 1; i <= ntax; i++) {
            if (taxaTypes[i] != 0)  //Already been 'typed'
                continue;
            typecount++;
            taxaTypes[i] = typecount;
            representatives[typecount] = i;
            for (int j = i + 1; j <= ntax; j++) {
                if (taxaTypes[j] != 0)
                    continue;
                boolean taxaIdentical = CharactersUtilities.taxaIdentical(chars, i, j);
                if (taxaIdentical) {
                    taxaTypes[j] = typecount;
                }
            }
        }

        double[] freqs = new double[typecount + 1];
        for (int i = 1; i <= ntax; i++) {
            freqs[taxaTypes[i]] += 1.0 / ntax;
        }

        boolean hasMissingDistances = false;

        double[][] dist = new double[typecount + 1][typecount + 1];
        for (int i = 1; i <= typecount; i++) {
            for (int j = i + 1; j <= typecount; j++) {
                int seqi = representatives[i];
                int seqj = representatives[j];
                double pij = CharactersUtilities.proportionOfDifferences(chars,seqi,seqj);
                double dij = -1.0;
                if (pij>=0.0 && pij<0.75)
                    dij = -0.75 * Math.log(1.0 - 4.0 * pij / 3.0);
                dist[i][j] = pij;
                dist[j][i] = dij;
                if (pij<0.0)
                    hasMissingDistances = true;
            }
        }

        double pi1 = 0.0;
        double pi2 = 0.0;

        for (int i = 1; i <= typecount; i++) {
            for (int j = i + 1; j <= typecount; j++) {
                pi1 += freqs[i] * freqs[j] * dist[i][j];
                pi2 += freqs[i] * freqs[j] * dist[j][i];
            }
        }
        pi1 = 2.0 * pi1; //Correction for fact we only looped over upper triangle.
        pi2 = 2.0 * pi2;

        //Implementation of *total variance* formula , eqn (10.9) in (Nei 1987)
        double mT = CharactersUtilities.meanNotMissing(chars);
        double n = (double) ntax;
        double c1 = (n+1.0)/(3.0*(n-1.0))/mT;
        double c2 = 2.0*(n*n + n + 3)/(9.0*n*(n-1.0));

        String result = "Nucleotide diversity. Eqns (10.5) and (10.9) in (Nei 1987).\n";
        result += "\tuncorrected distances:\n";
        if (!hasMissingDistances)
            result += "\t\tpi: "+pi1+"\tVar(pi): "+(c1*pi1 + c2*pi1*pi1)+"\ts.d.(pi): "+Math.sqrt(c1*pi1 + c2*pi1*pi1)+"\n";
        else
            result += "\t\tCannot compute nucleotide diversity due to excessive missing data\n";
        result += "\tcorrected distances:\n";
        if (!hasMissingDistances)    {
            result += "\t\tpi: "+pi2+"\tVar(pi): "+(c1*pi2 + c2*pi2*pi2)+"\ts.d.(pi): "+Math.sqrt(c1*pi2 + c2*pi2*pi2)+"\n";
            result += "This is `total variance', including sampling and stochastic variability.\n";
        }
        else
            result+="\t\tCannot compute (corrected) nucleotide diversity due to missing data and/or saturated distances\n";

        System.err.println(result);
        return result;

    }

}

// EOF
