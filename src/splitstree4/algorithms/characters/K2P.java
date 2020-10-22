/**
 * K2P.java
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
import splitstree4.gui.algorithms.RatesPanel;
import splitstree4.models.K2Pmodel;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;

import javax.swing.*;

/**
 * @author DJB
 * Computes  the Kimura two parameter distance for a set of characters
 */
public class K2P extends DNAdistance {

    private double tratio = 2.0;
    public final static String DESCRIPTION = "Calculates distances using the Kimura2P model";

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
        guiPanel.add(getTsTVPanel());
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }

    /**
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        double P = F[0][2] + F[1][3] + F[2][0] + F[3][1];
        double Q = F[0][1] + F[0][3] + F[1][0] + F[1][2];
        Q += F[2][1] + F[2][3] + F[3][0] + F[3][2];
        double dist = 0.5 * Minv(1 / (1 - (2 * P) - Q));
        dist += 0.25 * Minv(1 / (1 - (2 * Q)));
        return dist;
    }

    /**
     * Computes K2P corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */
    protected Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        Distances distances = new Distances(characters.getNtax());
        distances.getFormat().setTriangle("both");

        if (doc != null) {
            doc.notifySubtask("K2P Distance");
            doc.notifySetProgress(0);
        }

        K2Pmodel model = new K2Pmodel(this.tratio);
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());

        return fillDistanceMatrix(doc, characters, model);

    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}
//EOF

