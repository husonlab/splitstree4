/*
 * LogDet.java Copyright (C) 2022 Daniel H. Huson
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
/* $Id: LogDet.java,v 1.33 2008-03-17 14:22:44 bryant Exp $
 */
package splitstree4.algorithms.characters;

import Jama.Matrix;
import jloda.swing.util.Alert;
import jloda.util.CanceledException;
import splitstree4.analysis.characters.CaptureRecapture;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Calculation of the LogDet transform.
 * <p/>
 * The standard formula that we use for computing log det distances is
 * \[d_{xy} = -1/r ln(\frac{ det F_{xy} }{\sqrt{det(\Pi_x \Pi_y)}}\]
 * which is formula (23) on page 460 of Swofford et al.
 * <p/>
 * Taking the log of the determinant is numerically unstable, so instead we compute
 * trace(log(F_xy)) using an eigenvalue decomposition of F_xy.
 * <p/>
 * Both ways of computing log det will run into problems when F has zero or negative
 * eigenvalues. To avoid this, the implementation of logDet in LDDist makes some rather arbitrary
 * modifications to the F_xy matrix. These might be especially useful in protein log Det, where
 * it can often happen that rows or columns of F_xy are all zero. It seems to me that there are
 * better ways of dealing with that (e.g. using logDet on a subset of states), but that
 * requires a lot of further investigation that, given general problems of distance based methods,
 * might not be worth it.
 * <p/>
 * Here is a description of the fudge factor as I've interpreted it from LdDist code:
 * <p/>
 * Let F_{xy}[i,j] be the number of sites with an i for x and a j for y.
 * <p/>
 * for each state i, let \f_x[i] be the number of sites in x with state i. Likewise for \f_y[i].
 * let m_x[i] be the number of sites where x has an i and y has a missing or gap.
 * let m_y[i] be the number of sites where y has an i and x has a missing or gap.
 * <p/>
 * For each pair of states i,j multiply F_{xy}[i,j] by (1.0 + m_x[i]/f_x[i] + m_y[j]/f_y[j])
 * <p/>
 * I'm not quite sure why we don't just add m_x[i]/f_x[i] + m_y[j]/f_y[j] to F_{xy}, as this would correspond
 * to 'allocating' the sites  i -- gap  over the different pairs (i,1),...,(i,r) according to the total frequencies.
 * <p/>
 * The next step is to replace zero elements in F_{xy}[i,j] by 0.5. This is, I guess, imputing missing values,
 * but it won't solve the problem of zero rows or columns.
 * <p/>
 * Finally, we rescale F so that its entries sum to 1.0.
 * <p/>
 * <p/>
 * ToDo: come up with a better way to do this.
 * <p/>
 * The other option involves the use of invariable sites. These can be estimated using the 'Estimate' button (we
 * use the capture-recapture method because it is fast and easy to implement), or you can plug in values from
 * Quartet puzzling or Phyml. Note that pvar is the proportion of variable sites, which is 1.0 minus the proportion
 * of invariable sites. Maybe we should change this.
 * <p/>
 * The formula we use is identical to 'Method 2' in the thesis of Peter Waddell. Namely, let pi[i] denote the
 * estimated frequency for state i and let p be the proportion of invariable sites. Using F (fudged or not) we compute
 * V = (F - p \Pi)
 * where \Pi is the diagonal matrix with \pi_i values down the diagonal. We then replace F by V in the formula above.
 */
public class LogDet implements Characters2Distances {


    public final static String DESCRIPTION = "Calculates the logdet- distance";
    private boolean fudgeFactor = false;
    private boolean fillZeros = false;
    private double pInvar;
    LogDetPanel guiPanel;

    /**
     * Determine whether Logdet can be applied to given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        if (taxa == null || c == null)
            return false;

        /* We can apply as long as there is more than one symbol */
        return (c.getFormat().getSymbols().length() > 1);
        //String dtype =  c.getFormat().getDatatype();
        //return (dtype.equalsIgnoreCase(Characters.Datatypes.RNA)||dtype.equalsIgnoreCase(Characters.Datatypes.DNA));
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     * @throws SplitsException
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws CanceledException, SplitsException {


        Distances distances = new Distances(taxa.getNtax());
        distances.getFormat().setTriangle("both");

        doc.notifySubtask("logDet distance");
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(0);


        int ntax = characters.getNtax();
        int numUndefined = 0;

        for (int t = 1; t <= ntax; t++) {
            for (int s = t + 1; s <= ntax; s++) {
                String states = characters.getFormat().getSymbols();
                PairwiseCompare seqPair = new PairwiseCompare(characters, states, s, t);
                double dist = -1.0;

                int r = seqPair.getNumStates();

                double[][] F = seqPair.getF();
                if (F == null) {
                    numUndefined++;
                } else {
                    if (this.fudgeFactor) {
                        /* LDDist 1.2 implements some questionable tricks to avoid singluar matrices. To enable
                   comparisons, I've implemented these here. */
                        double[][] extF = seqPair.getFcount();


                        double[] rowsum = new double[r];
                        double[] colsum = new double[r];
                        double[] rowgaps = new double[r]; //sum of gap and missng cols
                        double[] colgaps = new double[r]; //sum of gap and missing rows
                        for (int i = 0; i < r + 2; i++) {
                            for (int j = 0; j < r + 2; j++) {
                                if (i < r && j < r) {
                                    rowsum[i] += extF[i][j];
                                    colsum[j] += extF[i][j];
                                } else if (i < r && j >= r) {
                                    rowgaps[i] += extF[i][j];
                                } else if (i >= r && j < r) {
                                    colgaps[j] += extF[i][j];
                                }
                            }
                        }

                        /* add fudge factors from sites with gap or missing */
                        for (int i = 0; i < r; i++) {
                            for (int j = 0; j < r; j++) {
                                double fudgei = 0.0, fudgej = 0.0;
                                if (rowsum[i] != 0) fudgei = rowgaps[i] / rowsum[i];
                                if (colsum[j] != 0) fudgej = colgaps[j] / colsum[j];
                                F[i][j] = extF[i][j] * (1.0 + fudgei + fudgej);
                            }
                        }

                        /* Replace zeros with small numbers !?! but only in rows/columns with values present*/
                        double Fsum = 0.0;
                        for (int i = 0; i < r; i++) {
                            if (rowsum[i] == 0) continue;
                            for (int j = 0; j < r; j++) {
                                if (this.fillZeros && colsum[j] != 0 && F[i][j] < 0.5) F[i][j] = 0.5;
                                Fsum += F[i][j];
                            }
                        }
                        /*Normalise */
                        for (int i = 0; i < r; i++)
                            for (int j = 0; j < r; j++)
                                F[i][j] /= Fsum;

                    }

                    /* Determine base frequencies */
                    double[] Pi_x = new double[r];
                    double[] Pi_y = new double[r];
                    double[] Pi = new double[r];
                    for (int i = 0; i < r; i++)
                        Pi_x[i] = Pi_y[i] = Pi[i] = 0.0;

                    for (int i = 0; i < r; i++)
                        for (int j = 0; j < r; j++) {
                            double Fij = F[i][j];
                            Pi_x[i] += Fij;
                            Pi_y[j] += Fij;
                        }


                    for (int i = 0; i < r; i++)
                        Pi[i] = (Pi_x[i] + Pi_y[i]) / 2.0;

                    double logPi = 0.0;
                    for (int i = 0; i < r; i++)
                        if (Pi_x[i] != 0.0 && Pi_y[i] != 0.0)
                            logPi += Math.log(Pi_x[i]) + Math.log(Pi_y[i]);
                    logPi *= 0.5;

                    /* Compute Log Det */

                    /* Incorporate proportion of invariable sites */
                    double pinv = getOptionPInvar();
                    if (pinv > 0.0)
                        for (int i = 0; i < r; i++)
                            F[i][i] -= pinv * Pi[i];

                    Matrix Fmatrix = new Matrix(F);
                    double[] Feigs = Fmatrix.eig().getRealEigenvalues();
                    double x = 0.0;
                    boolean thisIsSaturated = false;
                    for (double Feig : Feigs) {
                        if (Feig <= 0.0)
                            thisIsSaturated = true;
                        else
                            x += Math.log(Feig);
                    }
                    /* now x =  trace(log(F)) = log(det(F)) */
                    if (thisIsSaturated) {
                        numUndefined++;
                        x = -10000000;
                    }

                    double PiSum = 0;
                    for (int i = 0; i < r; i++) {
                        PiSum += Pi[i] * Pi[i];
                    }

                    dist = -(1.0 - PiSum) / (r - 1.0) * (x - logPi);
                }
                distances.set(s, t, dist);
                distances.set(t, s, dist);
            }
            doc.notifySetProgress(t * 100 / taxa.getNtax());
        }

        if (numUndefined > 0) {
            new Alert("Warning: there were saturated or missing distances in the matrix. These have been replaced with arbitrary large values - proceed with caution ");
        }

        return distances;

    }

    public JPanel getGUIPanel(Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new LogDetPanel(doc.getCharacters(), doc, this);

        return guiPanel;
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
     * Gets flag of whether missing entries in the F matrix are imputed using the method
     * that LDDist uses.
     *
     * @return boolean
     */
    public boolean getOptionImpute_Gaps() {
        return fudgeFactor;
    }

    /**
     * Sets flag of whether missing entries in the F matrix are imputed, using the method that LDDist uses.
     *
     * @param val
     */
    public void setOptionImpute_Gaps(boolean val) {
        fudgeFactor = val;
    }

    /**
     * Sets proportion of invariable sites used when computing log det.
     *
     * @return double: proportion being used.
     */
    public double getOptionPInvar() {
        return pInvar;
    }

    /**
     * Set proportion of invariable sites to use for log det.
     *
     * @param pInvar
     */
    public void setOptionPInvar(double pInvar) {
        this.pInvar = pInvar;
    }


}

class LogDetPanel extends JPanel implements ActionListener, FocusListener {
    static String pinvFieldCommand = "Pinv";
    static String estimateCommand = "Estimate...";
    static String imputeCommand = "Impute";

    Characters characters;
    Document doc;
    JRadioButton[] rateButtons;
    JTextField alphaField;
    JTextField pinvField;
    JButton estimateButton;
    JCheckBox imputeButton;
    JCheckBox invarCheckBox;

    final LogDet logDet;


    public LogDetPanel(Characters characters, Document doc, LogDet logDet) {

        super();

        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.characters = characters;
        this.doc = doc;
        this.logDet = logDet;

        constraints.gridx = 0;
        constraints.gridy = 0;

        imputeButton = new JCheckBox("Impute missing matrix entries");
        imputeButton.setToolTipText("Use techniques in LDDist to avoid saturated distances");
        imputeButton.setActionCommand(imputeCommand);
        imputeButton.addActionListener(this);
        this.add(imputeButton, constraints);
        constraints.gridy++;


        JLabel label = new JLabel("Proportion of sites assumed invariable:");

        //Set up the text field for the proportion of invariable sites.
        pinvField = new JTextField(Double.toString(logDet.getOptionPInvar()));
        label.setLabelFor(pinvField);
        pinvField.setColumns(5);
        pinvField.setMinimumSize(pinvField.getPreferredSize());
        pinvField.setEditable(true);
        pinvField.setActionCommand(pinvFieldCommand);
        pinvField.addActionListener(this);
        pinvField.addFocusListener(this);
        this.add(label, constraints);
        constraints.gridx++;
        this.add(pinvField, constraints);

        constraints.gridx = 1;
        constraints.gridy++;

        //Set up the 'Estimate' button
        estimateButton = new JButton("Estimate...");
        estimateButton.setActionCommand(estimateCommand);
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

        constraints.gridx = 0;
        constraints.gridy++;
        this.add(new JLabel("Invariable sites are removed in proportion to base frequencies"), constraints);

        this.setMinimumSize(this.getPreferredSize());

    }

    public void actionPerformed(ActionEvent e) {
        String theAction = e.getActionCommand();
        if (theAction.equalsIgnoreCase(estimateCommand)) {
            double pinv = (new CaptureRecapture()).estimatePinv(characters);
            logDet.setOptionPInvar(pinv);
            pinvField.setText("" + (float) pinv);
        }
        updateFields();
    }


    private void updateFields() {
        //Check the gamma distribution
        double val;

        //Now the alpha
        try {
            val = (new Double(pinvField.getText()));
        } catch (NumberFormatException ex) {
            val = 0.0;
        }
        if (val >= 0.0 && val <= 1.0) {
            logDet.setOptionPInvar(val);
        }
    }


    public void focusGained(FocusEvent event) {
        updateFields();
    }

    public void focusLost(FocusEvent event) {
        updateFields();
    }
}
