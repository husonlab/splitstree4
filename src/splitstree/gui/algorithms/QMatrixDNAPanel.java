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

import splitstree.algorithms.characters.GTR;

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
public class QMatrixDNAPanel extends JPanel implements ActionListener, FocusListener {


    protected JTextField[][] matrixCells;
    protected static String[] bases = {"A", "C", "G", "T/U"};
    public static String CHANGEQ = "CHANGEQ";

    GTR distTransform;

    public QMatrixDNAPanel(GTR distTransform) {
        super();

        this.distTransform = distTransform;

        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        this.setBorder(BorderFactory.createTitledBorder("Rate matrix"));

        //Set up Q matrix - first initialise the fields
        JPanel freqGrid = new JPanel();
        freqGrid.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        freqGrid.setLayout(new GridLayout(5, 5));

        matrixCells = new JTextField[4][4];

        freqGrid.add(new JLabel(""));
        for (int i = 0; i < 4; i++)
            freqGrid.add(new JLabel(bases[i], JLabel.CENTER));

        for (int i = 0; i < 4; i++) {
            freqGrid.add(new JLabel(bases[i], JLabel.RIGHT));
            for (int j = 0; j < 4; j++) {
                matrixCells[i][j] = new JTextField();
                matrixCells[i][j].setColumns(5);
                matrixCells[i][j].setMinimumSize(matrixCells[i][j].getPreferredSize());

                if (i >= j) {
                    matrixCells[i][j].setEditable(false);
                    matrixCells[i][j].setEnabled(false);
                    matrixCells[i][j].setText("-");
                } else {
                    matrixCells[i][j].setEditable(true);
                    matrixCells[i][j].setActionCommand(CHANGEQ);
                    matrixCells[i][j].addActionListener(this);
                    matrixCells[i][j].addFocusListener(this);

                }
                freqGrid.add(matrixCells[i][j]);
            }
        }
        updateCells();

        this.add(freqGrid, constraints);

        this.setMinimumSize(this.getPreferredSize());
    }

    protected void checkQcells() {
        double[][] Qmatrix = distTransform.getOptionQMatrix();
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                double val;
                try {
                    val = new Double(matrixCells[i][j].getText());
                } catch (NumberFormatException ex) {
                    val = -1;
                }
                if (val >= 0.0) {
                    Qmatrix[i][j] = val;
                }

            }
        }
        distTransform.setHalfMatrix(Qmatrix);
    }

    protected void updateCells() {

        double[][] Qmatrix = distTransform.getOptionQMatrix();
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                double val = Qmatrix[i][j];
                matrixCells[i][j].setText("" + val);
            }
        }
    }


    public void actionPerformed(ActionEvent e) {
        String theAction = e.getActionCommand();
        if (theAction.equalsIgnoreCase(CHANGEQ)) {
            checkQcells();
            updateCells();
        }
    }

    public void focusGained(FocusEvent event) {
        checkQcells();
        updateCells();
    }

    public void focusLost(FocusEvent event) {
        checkQcells();
        updateCells();
    }
}
