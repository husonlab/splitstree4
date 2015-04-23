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



