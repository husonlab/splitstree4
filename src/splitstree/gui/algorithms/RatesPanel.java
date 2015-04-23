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

package splitstree.gui.algorithms;

import splitstree.algorithms.characters.DNAdistance;
import splitstree.analysis.characters.CaptureRecapture;
import splitstree.core.Document;
import splitstree.nexus.Characters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 17, 2005
 * Time: 3:01:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class RatesPanel extends JPanel implements ActionListener, FocusListener {
    static String[] buttonNames = {"Equal rates", "Gamma"};
    static String[] buttonCommands = {"equal", "gamma", "alpha", "pinvar", "capture"};


    Characters characters;
    Document doc;
    final DNAdistance distTransform;
    JRadioButton[] rateButtons;
    JTextField alphaField;
    JTextField pinvField;
    JButton estimateButton;

    public RatesPanel(Characters characters, DNAdistance distTransform, Document doc) {

        super();

        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        this.setBorder(BorderFactory.createTitledBorder("Site rate variation"));

        this.characters = characters;
        this.distTransform = distTransform;
        this.doc = doc;

        constraints.gridx = 0;
        constraints.gridy = 0;

        //Set up the radio buttons
        rateButtons = new JRadioButton[2];
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 2; i++) {
            rateButtons[i] = new JRadioButton(buttonNames[i]);
            rateButtons[i].setActionCommand(buttonCommands[i]);

            rateButtons[i].addActionListener(this);
            group.add(rateButtons[i]);
            this.add(rateButtons[i], constraints);
            constraints.gridx++;
        }

        if (distTransform.getWhichGamma() == DNAdistance.DEFAULT)
            rateButtons[0].setSelected(true);
        else
            rateButtons[1].setSelected(true);

        constraints.gridx = 0;
        constraints.gridy++;

        JLabel label = new JLabel("Alpha parameter for gamma distribution:  ");
        alphaField = new JTextField(gammaString());
        label.setLabelFor(alphaField);
        alphaField.setColumns(5);
        alphaField.setMinimumSize(alphaField.getPreferredSize());
        alphaField.setEditable(true);
        alphaField.setActionCommand(buttonCommands[2]);
        alphaField.addActionListener(this);
        alphaField.addFocusListener(this);

        this.add(label, constraints);
        constraints.gridx++;
        this.add(alphaField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;

        label = new JLabel("Prop. invariable sites:  ");
        String initial;
        if (distTransform.getOptionGamma() > 0)
            initial = "" + distTransform.getOptionPInvar();
        else {
            rateButtons[1].setEnabled(false);
            initial = "0.0";
        }
        pinvField = new JTextField(initial);

        label.setLabelFor(pinvField);
        pinvField.setColumns(5);
        pinvField.setMinimumSize(pinvField.getPreferredSize());
        pinvField.setEditable(true);
        pinvField.setActionCommand(buttonCommands[3]);
        pinvField.addActionListener(this);
        pinvField.addFocusListener(this);

        this.add(label, constraints);
        constraints.gridx++;
        this.add(pinvField, constraints);

        constraints.gridx = 1;
        constraints.gridy++;

        estimateButton = new JButton("Estimate...");
        estimateButton.setActionCommand(buttonCommands[4]);
        estimateButton.addActionListener(this);
        estimateButton.setEnabled(false);
        estimateButton.setToolTipText("Estimate proportion of invariable sites using the Capture-Recapture heuristic");
        CaptureRecapture captureRecapture = new CaptureRecapture();
        try {
            if (captureRecapture.isApplicable(doc))
                estimateButton.setEnabled(true);
        } catch (Exception e) {
        }


        this.add(estimateButton, constraints);
        this.setMinimumSize(this.getPreferredSize());

    }

    public void actionPerformed(ActionEvent e) {
        String theAction = e.getActionCommand();
        if (theAction.equalsIgnoreCase(buttonCommands[0])) {
            distTransform.setWhichGamma(DNAdistance.DEFAULT);
        } else if (theAction.equalsIgnoreCase(buttonCommands[1])) {
            distTransform.setWhichGamma(DNAdistance.FROMUSER);

        } else if (theAction.equalsIgnoreCase(buttonCommands[4])) {

            double pinv = (new CaptureRecapture()).estimatePinv(characters);
            distTransform.setOptionPInvar(pinv);
            pinvField.setText("" + pinv);
        }

        updateFields();
    }

    private String gammaString() {
        double val = distTransform.getOptionGamma();
        if (val <= 0.0)
            return "?";
        else
            return "" + distTransform.getOptionGamma();
    }


    private void updateFields() {
        //Check the gamma distribution
        double val;
        try {
            val = (new Double(alphaField.getText()));
        } catch (NumberFormatException ex) {
            val = -1;
        }
        if (val > 0.0) {
            distTransform.setOptionGamma(val);
            rateButtons[1].setEnabled(true);
        } else {
            distTransform.setWhichGamma(DNAdistance.DEFAULT);
            rateButtons[0].setSelected(true);
            rateButtons[1].setEnabled(false);
        }

        //Now the alpha
        try {
            val = (new Double(pinvField.getText()));
        } catch (NumberFormatException ex) {
            val = -1;
        }
        if (val >= 0.0 && val <= 1.0) {
            distTransform.setOptionPInvar(val);
        }
    }


    public void focusGained(FocusEvent event) {
        updateFields();
    }

    public void focusLost(FocusEvent event) {
        updateFields();
    }
}
