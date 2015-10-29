/**
 * RandomCharacters.java
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
 */
package splitstree4.algorithms.util.simulate;

import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.simulate.RandomVariables.RandomGammaInvar;
import splitstree4.algorithms.util.simulate.RandomVariables.RandomVariable;
import splitstree4.models.SubstitutionModel;
import splitstree4.nexus.Characters;

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
        simulateCharacters(chars, T, M, siteRates, false, random);
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
    public static void simulateCharacters(Characters chars, PaupNode T, SubstitutionModel M, RandomVariable siteRates, boolean discardConstant, GenerateRandom random) {

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
            while (discardConstant && rate == 0.0)
                rate = siteRates.next();


            boolean isConst = true;

            while (isConst) {
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
                int initial = chars.get(1, site);
                for (int i = 2; i <= ntax; i++)
                    if (chars.get(i, site) != initial) {
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
