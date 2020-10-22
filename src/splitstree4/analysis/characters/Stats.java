/**
 * Stats.java
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
 * $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 * $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 */
/** $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 */
package splitstree4.analysis.characters;


import splitstree4.core.Document;
import splitstree4.nexus.Characters;

import java.util.BitSet;

/**
 * Basic statistics for characters
 */
public class Stats implements CharactersAnalysisMethod {
    public static String DESCRIPTION = "Computes basic stats for characters";

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

        int nConstant = 0;
        int nGaps = 0;
        int nMissing = 0;
        int nMissingAny = 0;
        int nNonParsimony = 0;
        int nMasked = 0;

        Characters chars = doc.getCharacters();
        char gap = chars.getFormat().getGap();
        char missing = chars.getFormat().getMissing();

        /*TODO: It appears that masked characters are included in the stats calculations.
        Note: this appears deliberate, since masked/unmasked state of constant, uninformative, gaps is indicated in stats */

        for (int c = 1; c <= chars.getNchar(); c++) {
            int nonP = 0;
            char ch;
            boolean ok = true, first = true, con = true;

            BitSet seenOnce = new BitSet();
            BitSet seenTwice = new BitSet();

            int missingTaxa = 0;
            int ntax = chars.getNtax();
            int threshold = (int) Math.floor(doc.getAssumptions().getExcludeConstant() * ntax) + 1; //Exclude sites with this much missing

            if (chars.isMasked(c))
                nMasked++;
            // Could wrap this loop in an "else" to only calculate stats for chars that aren't masked
            for (int t = 1; ok && t <= chars.getNtax(); t++) {
                if (chars.get(t, c) == gap) {
                    nGaps++;
                    ok = false;
                } else if (chars.get(t, c) == missing) {
                    //nMissing++;
                    missingTaxa++;
                    if (missingTaxa >= threshold) {
                        ok = false;
                        nMissing++;
                    }
                } else {
                    ch = chars.get(t, c);
                    if (!seenOnce.get(ch))
                        seenOnce.set(ch);
                    else {
                        seenTwice.set(ch);
                    }
                }
            }
            if (ok && seenOnce.cardinality() < 2)
                nConstant++;
            if (ok && seenTwice.cardinality() > 1)
                nNonParsimony++;
            if (missingTaxa > 0)
                nMissingAny++;
        }

        /*int count = 0;
for (int c = 1; c <= chars.getNchar(); c++)
    if (chars.isMasked(c))
        count++;*/
        if (nMasked > 0)
            System.err.println("Masked characters: " + nMasked);


        String result =
                "Active sites: ";
        if (nMasked == 0)
            result += chars.getNchar() + "\n";
        else
            result += (chars.getNchar() - nMasked) + " of " + chars.getNchar() + "\n";

        if (doc.getAssumptions().getUseCharSets() != null && doc.getAssumptions().getUseCharSets().size() > 0)
            result += "Charsets used: " + doc.getAssumptions().getUseCharSets().size() + "\n";

        result += "Number of Constant sites:     " + nConstant;
        if (doc.getAssumptions().getExcludeConstant() == Characters.EXCLUDE_ALL_CONSTANT)
            result += " (excluded)";
        if (doc.getAssumptions().getExcludeConstant() > 0)
            result += doc.getAssumptions().getExcludeConstant() + " (excluded)";
        result +=
                "\nNumber of Non-parsimony-informative sites: " + nNonParsimony;
        if (doc.getAssumptions().getExcludeNonParsimony())
            result += " (excluded)";

        result += "\nNumber of gapped sites         " + nGaps;
        if (doc.getAssumptions().getExcludeGaps())
            result += " (excluded)";
        result += "\nNumber of sites with missing data      " + nMissingAny;


        double missingVal = doc.getAssumptions().getExcludeMissing();
        result += "\n\t(Excluded " + nMissing + " site(s) with at least " + Math.round(100.0 * missingVal) + "% missing data)";

        return result;
    }

}

// EOF
