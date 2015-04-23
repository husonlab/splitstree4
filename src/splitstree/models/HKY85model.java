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
public class HKY85model extends NucleotideModel {

    /**
     * Constructor taking the expected rate of transitions versus transversions (rather
     * than the parameter kappa in Swofford et al, pg 436.)
     * We first compute the corresponding kappa, fill in Q according to the standard model/.
     *
     * @param basefreqs
     * @param TsTv
     */
    public HKY85model(double[] basefreqs, double TsTv) {
        super();

        double a = basefreqs[0] * basefreqs[2] + basefreqs[1] * basefreqs[3];
        double b = basefreqs[0] * basefreqs[1] + basefreqs[0] * basefreqs[3];
        b += basefreqs[1] * basefreqs[2] + basefreqs[2] * basefreqs[3];

        /* We have the identity
           *    a * kappa =  TsTv * b
           * which we solve to get kappa
           */
        double kappa = (TsTv * b) / a;

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

}

