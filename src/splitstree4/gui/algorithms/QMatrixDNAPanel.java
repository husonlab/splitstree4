/*
 * QMatrixDNAPanel.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.algorithms;

import splitstree4.algorithms.characters.GTR;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/* Controls in the BaseFrequency Panel */

/**
 * Constructs a GUI JPanel allowing the user to set base frequency options (or estimate them)
 */
public class QMatrixDNAPanel extends JPanel implements ActionListener, FocusListener {


	protected final JTextField[][] matrixCells;
	protected static final String[] bases = {"A", "C", "G", "T/U"};
	public static final String CHANGEQ = "CHANGEQ";

	final GTR distTransform;

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
                    val = Double.parseDouble(matrixCells[i][j].getText());
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
