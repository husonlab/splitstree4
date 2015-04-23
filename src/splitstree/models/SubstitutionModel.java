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

/*
 * Created on May 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package splitstree.models;

import java.util.Random;

/**
 * SubstitutionModel
 * <p/>
 * Abstract substitution model
 *
 * @author bryant
 */
public interface SubstitutionModel {

    /**
     * Returns P_{ij}(t), probability of change to j at time t given i at time 0
     *
     * @param i
     * @param j
     * @param t
     * @return P_{ij}(t), probability of change to j at time t given i at time 0
     */
    double getP(int i, int j, double t);

    /**
     * Returns  X_{ij}(t) = \pi_i P_{ij}(t) , probability of i at time 0 AND j at time t
     *
     * @param i
     * @param j
     * @param t
     * @return X_{ij}(t) = \pi_i P_{ij}(t) , probability of i at time 0 AND j at time t
     */
    double getX(int i, int j, double t);

    /**
     * Get an entry in the Q matrix (can involve computation)
     *
     * @param i
     * @param j
     * @return Q[i][j]
     */
    double getQ(int i, int j);

    /**
     * Returns base frequency
     *
     * @param i
     * @return base frequency of ith state
     */
    double getPi(int i);

    /**
     * Returns a state random selected from the base frequencies.
     *
     * @param random
     * @return state (int 0...nstates-1)
     */
    int randomPi(Random random);

    /**
     * Returns a state j from the distribution P_ij(t) with i = start.
     *
     * @param start
     * @param t
     * @param random
     * @return Returns a state j from the distribution P_ij(t) with i = start.
     */
    int randomEndState(int start, double t, Random random);

    /**
     * Returns, the rate \sum \pi_i Q_ii
     *
     * @return Returns \sum \pi_i Q_ii, the rate
     */
    double getRate();

    /**
     * Returns number of states in model
     *
     * @return number of states in model
     */
    int getNstates();

    /**
     * Is this model group valued (as in Szekely and Steel)
     *
     * @return true if model is group based.
     */
    boolean isGroupBased();
}

