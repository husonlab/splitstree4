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

/**
 * @author Miguel Jett�
 *         June 10th 2004
 *         <p/>
 *         Jukes Cantor model of evolution.
 */

public class JCmodel extends NucleotideModel {
    public JCmodel() {
        super();

        double[] basefreqs = {0.25, 0.25, 0.25, 0.25};

        double[][] Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    Q[i][j] = basefreqs[j];
                }
            }
        }

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
