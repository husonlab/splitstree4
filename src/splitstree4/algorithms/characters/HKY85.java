/**
 * HKY85.java
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
 *
 * Created on 12-Jun-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree4.algorithms.characters;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.gui.algorithms.BaseFrequencyPanel;
import splitstree4.gui.algorithms.RatesPanel;
import splitstree4.models.HKY85model;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;

import javax.swing.*;

/**
 * @author Mig
 * Computes the Hasegawa, Kishino and Yano distance for a set of characters.
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
