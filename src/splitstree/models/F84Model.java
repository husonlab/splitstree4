/**
 * F84Model.java 
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
