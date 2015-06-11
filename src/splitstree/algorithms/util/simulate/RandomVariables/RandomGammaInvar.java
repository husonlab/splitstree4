/**
 * RandomGammaInvar.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.algorithms.util.simulate.RandomVariables;

import splitstree.algorithms.util.simulate.GenerateRandom;

/**
 * RandomGammaInvar
 * <p/>
 * Generates random values equal to 0 with probability pInvar and otherwise drawn from
 * a gamma distribution with shape parameter gammaShape.
 * <p/>
 * User: bryant
 * Date: Nov 21, 2007
 */
public class RandomGammaInvar implements RandomVariable {
    private GenerateRandom random;
    private double gammaShape;
    private double pInvar;

    /**
     * Create new random variable
     *
     * @param gammaShape Shape parameter for gamma distribution
     * @param pInvar     proportion of values that are automatically 0
     * @param random     (GenerateRandom) random number generator
     */
    public RandomGammaInvar(double gammaShape, double pInvar, GenerateRandom random) {
        this.random = random;
        this.gammaShape = gammaShape;
        this.pInvar = pInvar;
    }

    /**
     * next
     * <p/>
     * Generate a new instance for the random variable.
     *
     * @return (double) next value
     */
    public double next() {
        if (random.nextDouble() < pInvar)
            return 0.0;
        return random.nextGamma(gammaShape, 1.0 / gammaShape);
    }
}



