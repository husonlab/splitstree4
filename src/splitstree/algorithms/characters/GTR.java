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
 * GTR distance.
 *
 * Computes distances according to a general time reversible model. 
 *
 * Created on 12-Jun-2004
 */
package splitstree.algorithms.characters;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.gui.algorithms.BaseFrequencyPanel;
import splitstree.gui.algorithms.QMatrixDNAPanel;
import splitstree.gui.algorithms.RatesPanel;
import splitstree.models.GTRmodel;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;

/**
 * @author DJB
 *         Computes the distance matrix from a set of characters using the General Time Revisible model.
 */
public class GTR extends DNAdistance {

    private double[][] QMatrix; //Q Matrix provided by user for ML estimation.
    public final static String DESCRIPTION = "Calculates distances using a General Time Reversible model";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public GTR() {
        super();
        //Default Q matrix is the Jukes-Cantor matrix
        QMatrix = new double[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i == j)
                    QMatrix[i][j] = -3;
                else
                    QMatrix[i][j] = 1;
            }
        }
    }

    /**
     * get the parameter matrix
     *
     * @return double[][]
     */
    public double[][] getOptionQMatrix() {
        return QMatrix;
    }

    /**
     * get the parameter matrix
     *
     * @param QMatrix Sets the rate matrix
     */
    public void setOptionQMatrix(double[][] QMatrix) {
        this.QMatrix = QMatrix;
    }

    /**
     * Sets Q using only the upper triangle of halfQ
     *
     * @param halfQ Rate matrix: only upper triangle used.
     */
    public void setHalfMatrix(double[][] halfQ) {
        QMatrix = new double[4][4];
        double[] baseFreq = getNormedBaseFreq();

        //Copy top half
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < i; j++) {
                QMatrix[j][i] = halfQ[j][i];
                QMatrix[i][j] = halfQ[j][i] * baseFreq[j] / baseFreq[i];
            }
            double rowsum = 0.0;
            for (int j = 0; j < 4; j++) {
                if (i != j)
                    rowsum += QMatrix[i][j];
            }
            QMatrix[i][i] = rowsum;
        }
    }

    /**
     * return the option panel for the method
     *
     * @param doc
     * @return
     */
    public JPanel getGUIPanel(Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new JPanel();
        guiPanel.setLayout(new BoxLayout(guiPanel, BoxLayout.Y_AXIS));
        guiPanel.add(getMLPanel(false));
        BaseFrequencyPanel bfp = new BaseFrequencyPanel(doc.getCharacters(), this);
        QMatrixDNAPanel qp = new QMatrixDNAPanel(this);

        guiPanel.add(bfp);
        guiPanel.add(qp);

        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }


    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        /* Exact distance - pg 456 in Swofford et al.
         * Let Pi denote the diagonal matrix with base frequencies down the
         * diagonal. The standard formula is
         *
         * dist = -trace(Pi log(Pi^{-1} F))
         *
         * The problem is that Pi^{-1}F will probably not be symmetric, so taking the
         * logarithm is difficult. However we can use an alternative formula:
         *
         * dist = -trace(Pi^{1/2} log(Pi^{-1/2} (F'+F)/2 Pi^{-1/2}) Pi^{1/2} )
         *
         * Then we will be taking the log (or inverse MGF) of a symmetric matrix.
         *
         *
         */

        int n = 4;
        double[] sqrtpi = new double[n];
        double[] baseFreq = getNormedBaseFreq();

        for (int i = 0; i < n; i++)
            sqrtpi[i] = Math.sqrt(baseFreq[i]);
        Matrix X = new Matrix(n, n);


        for (int i = 0; i < 4; i++) {
            for (int j = 0; j <= i; j++) {
                double Xij = (F[i][j] + F[j][i]) / (2.0 * sqrtpi[i] * sqrtpi[j]);
                X.set(i, j, Xij);
                if (i != j)
                    X.set(j, i, Xij);
            }
        }

        /* Compute M^{-1}(Q)  */
        EigenvalueDecomposition EX = new EigenvalueDecomposition(X);
        double[] D = EX.getRealEigenvalues();
        double[][] V = (EX.getV().getArrayCopy());
        for (int i = 0; i < 4; i++)
            D[i] = Minv(D[i]);

        /* Now evaluate trace(pi^{1/2} V D V^T pi^{1/2}) */

        double dist = 0.0;
        for (int i = 0; i < 4; i++) {
            double x = 0.0;
            for (int j = 0; j < 4; j++) {
                x += baseFreq[i] * V[i][j] * V[i][j] * D[j];
            }
            dist += -x;
        }

        return dist;
    }

    /**
     * Computes GTR corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */

    public Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        if (doc != null) {
            doc.notifySubtask("GTR Distance");
            doc.notifySetProgress(0);
        }

        GTRmodel model = new GTRmodel(QMatrix, getNormedBaseFreq());
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());


        return fillDistanceMatrix(doc, characters, model);
    }


}//EOF

