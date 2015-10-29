/**
 * ProportionPolymorphic.java
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
 */
/** $Id: Stats.java,v 1.10 2010-05-31 04:27:41 huson Exp $
 */
package splitstree4.analysis.characters;


import splitstree4.core.Document;
import splitstree4.nexus.Characters;

/**
 * Basic statistics for characters
 */
public class ProportionPolymorphic implements CharactersAnalysisMethod {
    public static String DESCRIPTION = "Returns population genetic statistics related to proportion of polymorphic (segregating) sites. (Tajima 1989)";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    //TODO is this always applicable?

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
     * @param doc  Document
     */
    public String apply(Document doc) {


        int numSegregating = 0;
        int numSitesNotAllMissing = 0;

        Characters chars = doc.getCharacters();
        char missing = chars.getFormat().getMissing();
        char gap = chars.getFormat().getGap();
        int ntax = chars.getNtax();


        for (int c = 1; c <= chars.getNchar(); c++) {
            if (chars.isMasked(c))
                continue;
            char topChar = missing;
            boolean segregating = false;
            for (int n = 1; n <= ntax; n++) {
                char ch = chars.get(n, c);
                if (ch != missing && ch != gap) {
                    if (topChar == missing)
                        topChar = ch;
                    else if (topChar != ch) {
                        segregating = true;
                        break;
                    }
                }
            }
            if (topChar != missing) {
                numSitesNotAllMissing++;
                if (segregating)
                    numSegregating++;
            }

        }

        double a1 = 0;
        double a2 = 0;
        for (int i = 1; i <= ntax - 1; i++) {
            a1 += 1.0 / i;
            a2 += 1.0 / (i * i);
        }


        String result = "Segregating (polymorphic) sites\n";
        result += "\tNumber of segregating (polymorphic) sites (S): " + numSegregating + "\n";
        result += "\tNumber of sites with at least one non-missing, non-gap base: " + numSitesNotAllMissing + "\n";
        result += "\tProportion of segregating sites: " + (double) numSegregating / numSitesNotAllMissing + "\n";
        result += "\t Theta_S estimate: " + (double) numSegregating / a1;
        double S = numSegregating;
        double v = (a1 * a1 * S + a2 * S * S) / (a1 * a1 * (a1 * a1 + a2));
        result += "\tVar(Theta_S): " + v + "\tsd(Theta_S): " + Math.sqrt(v) + "\t\t(Tajima 1989)\n";
        result += "\t Per-site-Theta_S estimate: " + (double) numSegregating / (a1 * numSitesNotAllMissing);
        result += "\tVar(Per-site-Theta_S): " + v / (numSitesNotAllMissing * numSitesNotAllMissing) + "\tsd(pre-site-Theta_S): " + Math.sqrt(v) / numSitesNotAllMissing;
        result += "\t\t(Tajima 1989)\n";


        return result;
    }

}

// EOF
