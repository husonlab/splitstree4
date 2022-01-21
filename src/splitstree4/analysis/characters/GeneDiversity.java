/*
 * GeneDiversity.java Copyright (C) 2022 Daniel H. Huson
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
/** $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 */
package splitstree4.analysis.characters;


import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.util.CharactersUtilities;

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

        double sumSquares = 0.0;
        double sumCubes = 0.0;
        for (int i = 1; i <= typecount; i++) {
            double p_i = freqs[i];
            sumSquares += p_i * p_i;
            sumCubes += p_i * p_i * p_i;
        }

        double H = (double) ntax / (ntax - 1.0) * (1.0 - sumSquares);
        double v = 2.0 / ((ntax - 1.0) * ntax) * (2.0 * (ntax - 2.0) * (sumCubes - sumSquares * sumSquares) + sumSquares - (sumSquares * sumSquares));

        String result = "Gene Diversity\n";
        result += "Gene diversity, treating each sequence as a single haplotype\n";
        result += "Gene diversity estimate of H: " + H;
        result += "\tVar(H): " + v + "\ts.d.(H): " + Math.sqrt(v) + "\t\t(Nei 1987, p.g. 180)\n";
        return result;
    }

}

// EOF
