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
import splitstree.models.HKY85model;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;

/**
 * @author Mig
 *         Computes the Hasegawa, Kishino and Yano distance for a set of characters.
 */
public class HKY85 extends DNAdistance {

    private double tratio = 2.0;
    public final static String DESCRIPTION = "Calculates distances using the Hasegawa, Kishino and Yano model";

    /**
     *
     */
    public HKY85() {
        super();
        setOptionMaximum_Likelihood(true);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        return 0.0; //We will never get here!
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
        guiPanel.add(getMLPanel(true));
        guiPanel.add(getTsTVPanel());
        guiPanel.add(new BaseFrequencyPanel(doc.getCharacters(), this));
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }

    /**
     * Computes HKY85 corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        if (doc != null) {
            doc.notifySubtask("HKY85 Distance");
            doc.notifySetProgress(0);
        }

        HKY85model model = new HKY85model(getNormedBaseFreq(), this.tratio);
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());

        setOptionMaximum_Likelihood(true);
        return fillDistanceMatrix(doc, characters, model);
    }


}//EOF
