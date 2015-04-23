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
public class GTRmodel extends NucleotideModel {

    private double[][] Q;

    /**
     * General time reversible model
     *
     * @param QMatrix
     * @param basefreqs Takes a provisional Q matrix and the base frequencies. Under the GTR properties, the matriix
     *                  Pi Q is symmetric. We enforce this as follows:
     *                  FOR OFF-DIAGONAL
     *                  First we construct X = PiQ
     *                  Replace X by (X + X')/2.0,
     *                  Fill in Q = Pi^{-1} X
     *                  Equivalently, Q_ij <- (Pi_i Q_ij + Pi_j Q_ji)/2.0 * Pi_i^{-1}
     *                  FOR DIAGONAL
     *                  Q_ii <= -\sum_{j \neq i} Q_{ij}
     */
    public GTRmodel(double[][] QMatrix, double[] basefreqs) {
        super();

        Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            double rowsum = 0.0;
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    Q[i][j] = (basefreqs[i] * QMatrix[i][j] + basefreqs[j] * QMatrix[j][i]) / (2.0 * basefreqs[i]);
                    rowsum += Q[i][j];
                }
            }
            Q[i][i] = -rowsum;
        }

        setRateMatrix(Q, basefreqs);
        normaliseQ();

    }

}
