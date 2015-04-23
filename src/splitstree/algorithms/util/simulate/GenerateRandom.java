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

import java.util.BitSet;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Aug 2, 2005
 * Time: 3:31:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class GenerateRandom extends Random {

    public GenerateRandom() {
        super();
    }

    public GenerateRandom(long seed) {
        super(seed);
    }

    /**
     * Returns a random number uniformly distributed between low and high
     *
     * @param low
     * @param high
     * @return double Random number between low and high
     */
    public double nextUniform(double low, double high) {
        return low + (high - low) * nextDouble();
    }


    /**
     * Generates random double from exponential with given mean
     *
     * @param mean
     * @return random double
     */
    public double nextExponential(double mean) {
        return -mean * Math.log(nextDouble());
    }

    /**
     * Generates random number from Gamma distribution \Gamma[\alpha,\beta] with given parameters
     *
     * @param alpha
     * @param beta
     * @return double (random number)
     */
    public double nextGamma(double alpha, double beta) {
        return beta * nextGamma(alpha);
    }

    /**
     * Generates random double from Gaussian distribution with given mean or variance
     *
     * @param mean
     * @param variance
     * @return double
     */
    public double nextGaussian(double mean, double variance) {
        return Math.sqrt(variance) * nextGaussian() + mean;
    }

    /**
     * Generates a 1 or -1 with probability 50%
     *
     * @return integer +/- 1
     */
    public int nextSign() {
        return nextBoolean() ? 1 : -1;
    }

    /**
     * Generates variable from a gamma distribution with shape \alpha and scale 1.  This has density
     * \[ f(x|\alpha, \theta) = x^{\alpha-1} \frac{e^{-x/\theta}}{\theta^k \Gamma(k)}\]
     * where \theta = 1.
     * We use the algorithm on pg 369 of
     * Marsaglia, G. and Tsang, W.W. (2000) A simple method for generating gamma variables. ACM transactions
     * on Mathematical Software 26(3), 363--372.
     *
     * @param alpha
     * @return double
     */
    public double nextGamma(double alpha) {
        if (alpha < 1.0) {
            double u = nextDouble();
            double gamma = nextGamma(alpha + 1.0);
            return gamma * Math.pow(u, 1.0 / alpha);
        } else {
            double d, c, x, v, u;
            d = alpha - 1.0 / 3.0;
            c = 1.0 / Math.sqrt(9.0 * d);
            for (; ;) {
                do {
                    x = nextGaussian();
                    v = 1.0 + c * x;
                } while (v <= 0.0);
                v = v * v * v;
                u = nextDouble();
                if (u < 1.0 - 0.0331 * (x * x) * (x * x))
                    return (d * v);
                if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v)))
                    return (d * v);
            }

        }
    }

    /**
     * Generate a random value from the ChiSquared with given degrees of freedom
     *
     * @param degFreedom
     * @return single sampled value
     */
    public double nextChiSquared(double degFreedom) {
        return nextGamma(degFreedom / 2.0, 2.0);
    }

    /**
     * Generate a random value from the non-Central Chi-Squared with given degrees of freedom.
     *
     * @param degFreedom
     * @param nonCentrality
     * @return (double) random double.
     * @throws Exception
     */
    public double nextChiSquared(double degFreedom, double nonCentrality) throws Exception {
        if (degFreedom < 1.0) {
            throw new Exception("Chi-squared with deg freedom less than unity");
        }
        double a = nextGaussian() + Math.sqrt(nonCentrality);
        a = a * a;

        double b = nextGamma((degFreedom - 1.0) / 2.0, 2.0);
        return a + b;
    }

    /**
     * Generates a random subset of 0...n-1.
     *
     * @param n
     * @return random BitSet
     */
    public BitSet nextBitSet(int n) {
        BitSet val = new BitSet(n);
        for (int i = 0; i < n; i++)
            if (nextBoolean())
                val.set(i);
        return val;
    }

}
