/*
 * K3STmodel.java Copyright (C) 2022 Daniel H. Huson
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

 */
package splitstree4.models;

/**
 * @author Mig
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class K3STmodel extends NucleotideModel {

    /**
     * Constructor taking ratio of transitions to transversions, as well
     * as the ratio of A<->T mutations to A<->C mutations.
     *
	 */
    public K3STmodel(double TsTv, double ATvsAC) {
        super();
        double AG_CT, AT_CG, AC_GT;
        AC_GT = 1.0;
        AT_CG = ATvsAC;
        AG_CT = TsTv * (AC_GT + AT_CG);
        InitK3ST(AG_CT, AT_CG, AC_GT);
    }

    /**
     * Constructor taking the expected rates of transitions (A-G and C-T),
     * transversion1 (A-T and C-G) and transversion2 (A-C and G-T)
     * just like the parameter kappa in Swofford et al, pg 434.
     * We fill in Q according to the standard model.
     *
	 */
    public K3STmodel(double AG_CT, double AT_CG, double AC_GT) {
        super();
        InitK3ST(AG_CT, AT_CG, AC_GT);
    }

    private void InitK3ST(double AG_CT, double AT_CG, double AC_GT) {

        double[] basefreqs = {0.25, 0.25, 0.25, 0.25};

        double[][] Q = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i != j) {
                    Q[i][j] = basefreqs[j];
                }
            }
        }
        Q[0][1] = Q[1][0] = Q[2][3] = Q[3][2] = AC_GT;
        Q[0][2] = Q[2][0] = Q[1][3] = Q[3][1] = AG_CT;
        Q[0][3] = Q[3][0] = Q[1][2] = Q[2][1] = AT_CG;

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

