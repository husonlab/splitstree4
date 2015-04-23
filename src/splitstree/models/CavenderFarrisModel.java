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

package splitstree.models;

import java.util.Random;

/**
 * The Cavender Farris for binary sequences
 * Dave Bryant, 3.2006
 */
public class CavenderFarrisModel implements SubstitutionModel {

    private double tCurrent; //Current value of t.
    private double pChange; // probability of change for current t.

    public CavenderFarrisModel() {
        pChange = 0;
        tCurrent = 0.0;
    }


    private void computeP(double t) {
        pChange = 0.5 - 0.5 * Math.exp(-2.0 * t);
        tCurrent = t;
    }

    /**
     * Returns P_{ij}(t), probability of change to j at time t given i at time 0
     *
     * @param i
     * @param j
     * @param t
     * @return P_{ij}(t), probability of change to j at time t given i at time 0
     */
    public double getP(int i, int j, double t) {
        if (t != tCurrent)
            computeP(t);
        if (i != j)
            return pChange;
        else
            return 1.0 - pChange;
    }

    /**
     * Get an entry in the Q matrix
     *
     * @param i
     * @param j
     * @return Q[i][j]
     */
    public double getQ(int i, int j) {
        if (i == j)
            return -1;
        else
            return 1;
    }


    public double getX(int i, int j, double t) {
        if (t != tCurrent)
            computeP(t);
        if (i != j)
            return 0.5 * pChange;
        else
            return 0.5 * (1.0 - pChange);
    }

    public double getPi(int i) {
        return 0.5;
    }

    public int randomPi(Random random) {
        return random.nextInt(2);
    }

    public int randomEndState(int start, double t, Random random) {
        if (t != tCurrent)
            computeP(t);
        if (random.nextDouble() < pChange)
            return 1 - start;
        else
            return start;
    }

    public double getRate() {
        return 1;
    }

    public int getNstates() {
        return 2;
    }

    /**
     * is this a group valued model
     *
     * @return true, if group valued model
     */
    public boolean isGroupBased() {
        return true;
    }
}
