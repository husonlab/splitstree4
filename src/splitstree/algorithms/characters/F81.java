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
 *
 * Created on 12-Jun-2004
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
import splitstree.models.F81model;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;

/**
 * Implements the Felsenstein81 DNA distance model
 */
public class F81 extends DNAdistance {


    public final static String DESCRIPTION = "Calculates distances using the Felsenstein81 model";
    private double B;

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
        guiPanel.add(new BaseFrequencyPanel(doc.getCharacters(), this));
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }


    /**
     *  return the exact distance
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        double D = 1 - (F[0][0] + F[1][1] + F[2][2] + F[3][3]);
        return -B * Minv(1 - D / B);
    }


    /**
     * Computes F81 corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */

    protected Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        if (doc != null) {
            doc.notifySubtask("F81 Distance");
            doc.notifySetProgress(0);
        }

        F81model model = new F81model(this.getNormedBaseFreq());
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());

        //System.out.println("A is: " + baseFreq[0]);
        double[] freqs = getNormedBaseFreq();

        double piA = freqs[0],
                piC = freqs[1],
                piG = freqs[2],
                piT = freqs[3];

        B = 1.0 - ((piA * piA) + (piC * piC) + (piG * piG) + (piT * piT));

        return fillDistanceMatrix(doc, characters, model);
    }
}//EOF

