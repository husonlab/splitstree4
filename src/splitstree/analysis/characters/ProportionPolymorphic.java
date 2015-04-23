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


         for(int c=1; c<=chars.getNchar(); c++) {
            if (chars.isMasked(c))
                continue;
            char topChar = missing;
            boolean segregating = false;
            for (int n=1;n<=ntax;n++) {
                char ch = chars.get(n,c);
                if (ch!=missing && ch!=gap) {
                    if (topChar==missing)
                        topChar = ch;
                    else if (topChar!=ch) {
                        segregating = true;
                        break;
                    }
                }
            }
            if (topChar!=missing) {
                numSitesNotAllMissing++;
                if (segregating)
                    numSegregating++;
            }

         }

        double a1=0;
        double a2=0;
        for (int i=1;i<=ntax-1;i++) {
            a1 += 1.0/i;
            a2 += 1.0/(i*i);
        }


        String result = "Segregating (polymorphic) sites\n";
        result += "\tNumber of segregating (polymorphic) sites (S): "+numSegregating+"\n";
        result+="\tNumber of sites with at least one non-missing, non-gap base: "+numSitesNotAllMissing+"\n";
        result+="\tProportion of segregating sites: "+(double)numSegregating/numSitesNotAllMissing + "\n";
        result+="\t Theta_S estimate: "+(double)numSegregating/a1;
        double S = numSegregating;
        double v = (a1*a1 * S + a2*S*S)/(a1*a1 * (a1*a1 + a2));
        result+="\tVar(Theta_S): "+v+"\tsd(Theta_S): "+Math.sqrt(v)+"\t\t(Tajima 1989)\n";
        result+="\t Per-site-Theta_S estimate: "+(double)numSegregating/(a1*numSitesNotAllMissing);
        result+="\tVar(Per-site-Theta_S): "+v/(numSitesNotAllMissing*numSitesNotAllMissing)+"\tsd(pre-site-Theta_S): "+Math.sqrt(v)/numSitesNotAllMissing;
        result+="\t\t(Tajima 1989)\n";


        return result;
    }

}

// EOF
