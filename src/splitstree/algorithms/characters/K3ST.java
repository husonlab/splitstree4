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
import splitstree.gui.algorithms.RatesPanel;
import splitstree.models.K3STmodel;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author DJB
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class K3ST extends DNAdistance {

    private double[][] QMatrix; //Q Matrix provided by user for ML estimation.
    private double tratio = 2.0;
    private double ACvsAT = 2.0;
    public final static String DESCRIPTION = "Calculates distances using the Kimura3ST model";

    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * set ACvsAT (ACGT transversions vs ATGC transversions)
     *
     * @param value
     */
    public void setOptionAC_vs_ATRatio(double value) {
        this.ACvsAT = value;
    }

    /**
     * get ACvsAT parameter
     *
     * @return ACvsAT (ACGT transversions vs ATGC transversions)
     */
    public double getOptionAC_vs_ATRatio() {
        return this.ACvsAT;
    }

    protected JPanel getK3STPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        //panel.setBorder(BorderFactory.createEtchedBorder());

        JLabel label = new JLabel("Transition/transversion ratio  ");

        //TsTv field
        final JTextField textField = new JTextField("" + getOptionTratio());
        label.setLabelFor(textField);

        textField.setColumns(5);
        textField.setMinimumSize(textField.getPreferredSize());
        textField.setEditable(true);
        textField.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                double val;
                try {
                    val = (new Double(textField.getText()));
                } catch (NumberFormatException ex) {
                    val = -1;
                }
                if (val >= 0) {
                    setOptionTratio(val);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    textField.setText("" + getOptionTratio());
                }
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(label, constraints);
        constraints.gridx++;
        panel.add(textField, constraints);

        //AC versus AG field
        label = new JLabel("Ratio of AC to AG   ");
        final JTextField textField2 = new JTextField(getOptionTratio() + "");
        label.setLabelFor(textField2);

        textField2.setColumns(5);
        textField2.setMinimumSize(textField.getPreferredSize());
        textField2.setEditable(true);
        textField2.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                double val;
                try {
                    val = (new Double(textField2.getText()));
                } catch (NumberFormatException ex) {
                    val = -1;
                }
                if (val >= 0) {
                    setOptionAC_vs_ATRatio(val);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    textField2.setText("" + getOptionAC_vs_ATRatio());
                }
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(label, constraints);
        constraints.gridx++;
        panel.add(textField2, constraints);


        panel.setMinimumSize(panel.getPreferredSize());
        return panel;
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
        guiPanel.add(getK3STPanel());
        guiPanel.add(new RatesPanel(doc.getCharacters(), this, doc));
        guiPanel.setMinimumSize(guiPanel.getPreferredSize());
        return guiPanel;
    }


    protected double exactDist(double[][] F) throws SaturatedDistancesException {
        double a, b, c, d;
        a = F[0][0] + F[1][1] + F[2][2] + F[3][3];
        b = F[0][1] + F[1][0] + F[2][3] + F[3][2];
        c = F[0][2] + F[2][0] + F[1][3] + F[3][1];
        d = 1.0 - a - b - c;
        return -1 / 4.0 * (Math.log(a + c - b - d) + Math.log(a + b - c - d) + Math.log(a + d - b - c));
    }


    /**
     * Computes K3ST corrected Hamming distances with a given characters block.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */

    protected Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {


        if (doc != null) {
            doc.notifySubtask("K3ST Distance");
            doc.notifySetProgress(0);
        }

        K3STmodel model = new K3STmodel(this.tratio, this.ACvsAT);
        model.setPinv(getOptionPInvar());
        model.setGamma(getOptionGamma());

        return fillDistanceMatrix(doc, characters, model);
    }


}//EOF
