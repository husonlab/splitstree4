/**
 * JukesCantor.java 
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
 * Created on 12-Jun-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.algorithms.characters;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.gui.algorithms.RatesPanel;
import splitstree.models.JCmodel;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;
import java.awt.*;

/**
 * @author DJB
 *  Computes the Jukes Cantor distance for a set of characters
 */
public class JukesCantor extends DNAdistance {

    public final static String DESCRIPTION = "Calculates distances using the Jukes Cantor model";

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


        guiPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;

        guiPanel.add(getMLPanel(false), constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc), constraints);
        return guiPanel;
    }

    /**
     *
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        double D = 1 - (F[0][0] + F[1][1] + F[2][2] + F[3][3]);
        double B = 0.75;
        return -B * Minv(1 - D / B);
    }


    /**
     * Computes JukesCantor corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */
    protected Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        if (doc != null) {
            doc.notifySubtask("Jukes-Cantor Distance");
            doc.notifySetProgress(0);
        }

        JCmodel model = new JCmodel();
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());


        return fillDistanceMatrix(doc, characters, model);

    }

}//EOF
