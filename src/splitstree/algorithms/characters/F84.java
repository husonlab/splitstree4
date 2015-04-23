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
 * Created on Jun 8, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.algorithms.characters;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.gui.algorithms.BaseFrequencyPanel;
import splitstree.gui.algorithms.RatesPanel;
import splitstree.models.F84Model;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;

/**
 * Implements the Felsenstein84 DNA distance model
 */
public class F84 extends DNAdistance {

    private double tratio = 2.0;
    private double A, B, C;

    public final static String DESCRIPTION = "Calculates distances using the Felsenstein84 model";

    /**
       * gets a short description of the algorithm
       *
       * @return a description
       */
    public String getDescription() {
        return DESCRIPTION;
    }

       /**
     *  return the option panel for the method
     * @param doc
     * @return
     */
    public JPanel getGUIPanel(Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new JPanel();
        guiPanel.setLayout(new BoxLayout(guiPanel, BoxLayout.Y_AXIS));
        guiPanel.add(getMLPanel(false));
        guiPanel.add(getTsTVPanel());
        guiPanel.add(new BaseFrequencyPanel(doc.getCharacters(), this));
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }

    /**
     * set Kappa option (transition/transversion ratio)
     *
     * @param value
     */
    public void setOptionTRatio(double value) {
        this.tratio = value;
    }


    /**
     * get Kappa parameter
     *
     * @return Kappa (transition/transversion ratio)
     */
    public double getOptionTRatio() {
        return this.tratio;
    }


    /**
     *  return the exact distance
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    protected double exactDist(double[][] F) throws SaturatedDistancesException {

        double P = F[0][2] + F[1][3] + F[2][0] + F[3][1];
        double Q = F[0][1] + F[0][3] + F[1][0] + F[1][2];
        Q += F[2][1] + F[2][3] + F[3][0] + F[3][2];
        double dist = -2.0 * A * Minv(1.0 - P / (2.0 * A) - (A - B) * Q / (2.0 * A * C));
        dist += 2.0 * (A - B - C) * Minv(1.0 - Q / (2.0 * C));
        return dist;
    }


    /**
     * Computes F84 corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        if (doc != null) {
            doc.notifySubtask("F84 Distance");
            doc.notifySetProgress(0);
        }

        F84Model model = new F84Model(this.getNormedBaseFreq(), this.tratio);
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());

        double[] baseFreq = getNormedBaseFreq();
        double piA = baseFreq[0],
                piC = baseFreq[1],
                piG = baseFreq[2],
                piT = baseFreq[3];
        double piR = piA + piG; //frequency of purines
        double piY = piC + piT; //frequency of pyrimidines
        A = piC * piT / piY + piA * piG / piR;
        B = piC * piT + piA * piG;
        C = piR * piY;

        return fillDistanceMatrix(doc, characters, model);
    }


}//EOF


