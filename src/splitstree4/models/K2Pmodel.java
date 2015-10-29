/**
 * K2Pmodel.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 11-Jun-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree4.models;

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

