/**
 * BaseFrequencyPanel.java
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
package splitstree4.gui.algorithms;

import jloda.swing.util.Alert;
import splitstree4.algorithms.characters.DNAdistance;
import splitstree4.nexus.Characters;
import splitstree4.util.CharactersUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 10, 2005
 * Time: 4:50:37 PM
 * To change this template use File | Settings | File Templates.
 */
/* Controls in the BaseFrequency Panel */

/**
 * Constructs a GUI JPanel allowing the user to set base frequency options (or estimate them)
 */
public class BaseFrequencyPanel extends JPanel implements ActionListener, FocusListener {

    static String[] buttonNames = {"Equal frequencies", "Empirical frequencies", "User defined"};
    static String[] buttonCommands = {"equal", "empirical", "user", "norm", "update", "freqs"};
    static String[] freqLabels = {"A", "C", "G", "T/U"};

    double[] charBaseFreqs;


    private JTextField[] freqFields;
    private JRadioButton[] baseButtons;
    Characters characters;
    DNAdistance distTransform;

    public BaseFrequencyPanel(Characters characters, DNAdistance distTransform) {
        super();

        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        this.setBorder(BorderFactory.createTitledBorder("Base Frequencies"));

        this.characters = characters;
        this.distTransform = distTransform;
        /* We keep our own copy of the character base frequencies until the appropriate
        changes are made in the Characters block*/
        charBaseFreqs = CharactersUtilities.computeFreqs(characters, false);

        constraints.gridx = 0;
        constraints.gridy = 0;

        //Set up the radio buttons
        baseButtons = new JRadioButton[3];
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 3; i++) {
            baseButtons[i] = new JRadioButton(buttonNames[i]);
            baseButtons[i].setActionCommand(buttonCommands[i]);

            baseButtons[i].addActionListener(this);
            group.add(baseButtons[i]);
            this.add(baseButtons[i], constraints);
            constraints.gridx++;
        }

        if (distTransform.getWhichBaseFreq() == DNAdistance.DEFAULT)
            baseButtons[0].setSelected(true);
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMCHARS)
            baseButtons[1].setSelected(true);
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER)
            baseButtons[2].setSelected(true);

        //Set up the 4 base frequency fields in a grid


        JPanel freqGrid = new JPanel();
        freqGrid.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        freqGrid.setLayout(new GridLayout(2, 4));
        freqFields = new JTextField[4];

        for (int i = 0; i < 4; i++)
            freqGrid.add(new JLabel(freqLabels[i]));

        double[] baseFreq = distTransform.getOptionBaseFreq();
        for (int i = 0; i < 4; i++) {
            freqFields[i] = new JTextField((new Float(baseFreq[i]).toString()));
            freqFields[i].setColumns(5);
            freqFields[i].setMinimumSize(freqFields[i].getPreferredSize());
            if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER)
                freqFields[i].setEditable(true);
            else
                freqFields[i].setEditable(false);
            freqFields[i].setActionCommand(buttonCommands[5]);
            freqFields[i].addActionListener(this);
            freqFields[i].addFocusListener(this);
            freqGrid.add(freqFields[i]);
        }

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.fill = GridBagConstraints.REMAINDER;
        constraints.gridwidth = 4;
        this.add(freqGrid, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;

        //Set up the normalise and update buttons

        JButton normButton = new JButton("Normalise");
        normButton.setActionCommand(buttonCommands[3]);
        normButton.addActionListener(this);
        constraints.gridy++;
        constraints.gridx = 1;
        this.add(normButton, constraints);

        JButton updateButton = new JButton("Update");
        updateButton.setActionCommand(buttonCommands[4]);
        updateButton.addActionListener(this);
        constraints.gridx++;
        this.add(updateButton, constraints);

        this.setMinimumSize(this.getPreferredSize());
    }

    public void actionPerformed(ActionEvent e) {
        String theAction = e.getActionCommand();
        if (theAction.equalsIgnoreCase(buttonCommands[0])) {
            distTransform.setWhichBaseFreq(DNAdistance.DEFAULT);
        } else if (theAction.equalsIgnoreCase(buttonCommands[1])) {
            distTransform.setWhichBaseFreq(DNAdistance.FROMCHARS);
        } else if (theAction.equalsIgnoreCase(buttonCommands[2])) {
            distTransform.setWhichBaseFreq(DNAdistance.FROMUSER);
            checkFreqs();  //If not valid then we switch back
        } else if (theAction.equalsIgnoreCase(buttonCommands[3])) {  //Normalise
            double[] freqs = getFieldFreqs();
            if (freqs == null)
                new Alert("There is an error in the base frequencies provided");
            else {
                double sum = 0.0;
                for (int i = 0; i < 4; i++)
                    sum += freqs[i];
                for (int i = 0; i < 4; i++)
                    freqs[i] = freqs[i] / sum;
                setFieldFreqs(freqs);
            }
        } else if (theAction.equalsIgnoreCase(buttonCommands[4])) {
            charBaseFreqs = CharactersUtilities.computeFreqs(characters, false);
            setFieldFreqs(charBaseFreqs);
        } else if (theAction.equalsIgnoreCase(buttonCommands[5])) {
            checkFreqs();
        }

        /* Update base frequencies */
        if (distTransform.getWhichBaseFreq() == DNAdistance.DEFAULT)
            distTransform.setOptionBaseFreq(new double[]{0.25, 0.25, 0.25, 0.25});
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMCHARS)
            distTransform.setOptionBaseFreq(charBaseFreqs);
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER)
            distTransform.setOptionBaseFreq(getFieldFreqs());

        //Update the displayed frequencies.
        setFieldFreqs(distTransform.getOptionBaseFreq());

    }

    /**
     * Returns an array containing the frequency values in the panel, or null if they are invalid.
     *
     * @return array of double
     */
    private double[] getFieldFreqs() {
        double[] fieldFreqs = new double[4];
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            String text = freqFields[i].getText();
            try {
                fieldFreqs[i] = new Double(text);
            } catch (NumberFormatException e) {
                return null;
            }
            if (fieldFreqs[i] < 0.0)
                return null;
            sum += fieldFreqs[i];
        }
        if (sum <= 0.0)
            return null;
        return fieldFreqs;
    }

    private void setFieldFreqs(double[] freqs) {
        boolean areEnabled = (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER);
        for (int i = 0; i < 4; i++) {
            freqFields[i].setText("" + freqs[i]);
            freqFields[i].setEditable(areEnabled);
        }
    }

    /**
     * Checks to see if the current base frequencies are OK. If they are, then
     * the user radio button is enabled. If they are not, then the radio button
     * is disabled and the default is selected.
     */
    private void checkFreqs() {
        double[] fieldFreqs = getFieldFreqs();
        if (fieldFreqs == null) {
            if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER) {
                distTransform.setWhichBaseFreq(DNAdistance.DEFAULT);
            }
            baseButtons[2].setEnabled(false);
            baseButtons[2].setSelected(false);
            if (distTransform.getWhichBaseFreq() == DNAdistance.DEFAULT)
                baseButtons[0].setSelected(true);
            else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMCHARS)
                baseButtons[1].setSelected(true);
        } else {
            baseButtons[2].setEnabled(true);
        }

    }

    public void focusGained(FocusEvent event) {
        checkFreqs();
        /* Update base frequencies */
        if (distTransform.getWhichBaseFreq() == DNAdistance.DEFAULT)
            distTransform.setOptionBaseFreq(new double[]{0.25, 0.25, 0.25, 0.25});
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMCHARS)
            distTransform.setOptionBaseFreq(charBaseFreqs);
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER)
            distTransform.setOptionBaseFreq(getFieldFreqs());

        //Update the displayed frequencies.
        setFieldFreqs(distTransform.getOptionBaseFreq());
    }

    public void focusLost(FocusEvent event) {
        checkFreqs();
        /* Update base frequencies */
        if (distTransform.getWhichBaseFreq() == DNAdistance.DEFAULT)
            distTransform.setOptionBaseFreq(new double[]{0.25, 0.25, 0.25, 0.25});
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMCHARS)
            distTransform.setOptionBaseFreq(charBaseFreqs);
        else if (distTransform.getWhichBaseFreq() == DNAdistance.FROMUSER)
            distTransform.setOptionBaseFreq(getFieldFreqs());

        //Update the displayed frequencies.
        setFieldFreqs(distTransform.getOptionBaseFreq());
    }
}
