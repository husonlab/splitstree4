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
 * Created on 11-Jun-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.models;

/**
 * @author Mig
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class K2Pmodel extends NucleotideModel {

    /**
     * Constructor taking the expected rate of transitions versus transversions (rather
     * than the parameter kappa in Swofford et al, pg 435.)
     * We first compute the corresponding kappa, fill in Q according to the standard model.
     *
     * @param TsTv
     */
    public K2Pmodel(double TsTv) {
        super();

        double[] basefreqs = {0.25, 0.25, 0.25, 0.25};

        /* We have the identity
         *    TsTv = kappa/2
         * which we solve to get kappa
         */
        double kappa = TsTv * 2;

        double[][] Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    Q[i][j] = basefreqs[j];
                }
            }
        }

        Q[0][2] *= kappa;
        Q[1][3] *= kappa;
        Q[3][1] *= kappa;
        Q[2][0] *= kappa;

        setRateMatrix(Q, basefreqs);
        normaliseQ();

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

