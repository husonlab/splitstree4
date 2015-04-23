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

package splitstree.algorithms.util.simulate;

import splitstree.algorithms.util.PaupNode;
import splitstree.algorithms.util.simulate.RandomVariables.RandomGammaInvar;
import splitstree.algorithms.util.simulate.RandomVariables.RandomVariable;
import splitstree.models.SubstitutionModel;
import splitstree.nexus.Characters;

/**
 * simulateCharacters
 * <p/>
 * Simulates sites on a tree according to the given Substitution model.
 * <p/>
 * Site rate variation can be modelled according to a generic RandomVariable object
 */
public class RandomCharacters {

    /**
        * Replaces chars with randomly generated characters, using the tree T and substitution model
        * M
        *
        * @param chars Characters. These are overwritten
        * @param T     PaupNode Tree used to simulate characters
        * @param M     SubstitutionModel model
        */
       public static void simulateCharacters(Characters chars, PaupNode T, SubstitutionModel M, RandomVariable siteRates, GenerateRandom random) {
        simulateCharacters(chars,T,M,siteRates,false,random);
       }


    /**
     * Replaces chars with randomly generated characters, using the tree T and substitution model
     * M
     *
     * @param chars Characters. These are overwritten
     * @param T     PaupNode Tree used to simulate characters
     * @param M     SubstitutionModel model
     * @param siteRates  Random variable giving site rates (set to null for constant rates)
     * @param discardConstant If true, only polymorphic (non-constant) characters are generated.
     * @param random    Random number generator
     */
    public static void simulateCharacters(Characters chars, PaupNode T, SubstitutionModel M, RandomVariable siteRates, boolean discardConstant,GenerateRandom random) {

        int nsites = chars.getNchar();
        int ntax = chars.getNtax();
        char missing = chars.getFormat().getMissing();

        //Copy of the tree
        PaupNode simT = T.deepCopyTree();
        String symbols = chars.getFormat().getSymbols();

        for (int site = 1; site <= nsites; site++) {
            //Mark all taxa as missing
            for (int i = 1; i <= ntax; i++)
                chars.set(i, site, missing);
            double rate = 1.0;

            if (siteRates != null)
                rate = siteRates.next();
            while(discardConstant && rate==0.0)
                rate = siteRates.next();


            boolean isConst = true;

            while(isConst) {
                //Evolve the character, starting from the root.
                simT.data = M.randomPi(random);

                if (rate == 0.0) {
                    char state = symbols.charAt(((Integer) simT.data).intValue());
                    for (int i = 1; i <= ntax; i++)
                        chars.set(i, site, state);
                } else {
                    PaupNode p = simT.nextPre(simT);
                    while (p != null) {
                        int parentState = (Integer) p.getPar().data;
                        int childState = M.randomEndState(parentState, p.length * rate, random);
                        if (p.isLeaf())
                            chars.set(p.id, site, symbols.charAt(childState));
                        else
                            p.data = childState;
                        p = p.nextPre(simT);
                    }
                }
                if (!discardConstant)
                    break;
                int initial = chars.get(1,site);
                for(int i=2;i<=ntax;i++)
                    if (chars.get(i,site)!=initial) {
                        isConst = false;
                        break;
                    }
            }
        }
    }

    public static void simulateCharacters(Characters chars, PaupNode T, SubstitutionModel M) {
        simulateCharacters(chars, T, M, null, new GenerateRandom());
    }

    public static void simulateCharacters(Characters chars, PaupNode T, SubstitutionModel M, double gammaShape, double pInvar, GenerateRandom random) {
        simulateCharacters(chars, T, M, new RandomGammaInvar(gammaShape, pInvar, random), random);
    }


}
