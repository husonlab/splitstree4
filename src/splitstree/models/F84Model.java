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
 * Created on Jun 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.models;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class F84Model extends NucleotideModel {


    /**
     * Constructor taking the expected rate of transitions versus transversions (rather
     * than the parameter K in Swofford et al, pg 436.)
     * We first compute the corresponding K, fill in Q according to the standard model/.
     *
     * @param baseFreqs
     * @param TsTv
     */
    public F84Model(double[] baseFreqs, double TsTv) {
        super();


        double a = baseFreqs[0] * baseFreqs[2] + baseFreqs[1] * baseFreqs[3];
        double b = baseFreqs[0] * baseFreqs[2] / (baseFreqs[0] + baseFreqs[2]);
        b += baseFreqs[1] * baseFreqs[3] / (baseFreqs[1] + baseFreqs[3]);
        double c = baseFreqs[0] * baseFreqs[1] + baseFreqs[0] * baseFreqs[3];
        c += baseFreqs[1] * baseFreqs[2] + baseFreqs[2] * baseFreqs[3];

        /* We have the identity
           *    a + bK =  TsTv * c
           * which we solve to get K
           */
        double K = (TsTv * c - a) / b;

        double[][] Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    Q[i][j] = baseFreqs[j];
                }
            }
        }
        double piR = baseFreqs[0] + baseFreqs[2];
        double piY = baseFreqs[1] + baseFreqs[3];
        Q[0][2] *= (1.0 + K / piR);
        Q[1][3] *= (1.0 + K / piY);
        Q[3][1] *= (1.0 + K / piR);
        Q[2][0] *= (1.0 + K / piY);

        setRateMatrix(Q, baseFreqs);
        normaliseQ();


    }

}
