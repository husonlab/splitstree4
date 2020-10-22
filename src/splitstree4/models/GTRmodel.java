/**
 * GTRmodel.java
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
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
