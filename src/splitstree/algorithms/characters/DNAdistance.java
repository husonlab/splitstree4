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

package splitstree.algorithms.characters;

import jloda.util.Alert;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.models.NucleotideModel;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.util.CharactersUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Tools for setting up the options panels (custom designed) with DNA distance methods).
 */
public abstract class DNAdistance extends SequenceBasedDistance {


    /* These are the parameters used for distance calculation */
    private double optionPInvar;
    private double optionGamma;
    private double[] baseFreq;  //Base frequences (unnormalised)
    private double tratio;
    private boolean useML;

    /* These are used in the panel to decide how to compute the above*/

    /* These constant used to decide where to get parameter estimates from */
    public static final int FROMCHARS = -1;
    public static final int FROMUSER = -2;
    public static final int DEFAULT = 0;

    //The symbols in the character matrix can come in any order, however
    //the order of states in the Q matrix is fixed. Here is the fixed order.
    public static final String DNASTATES = "acgt";
    public static final String RNASTATES = "acgu";

    private int whichPInvar;
    private int whichGamma;
    private int whichBaseFreq;

    public DNAdistance() {
        optionPInvar = 0.0;
        whichPInvar = DEFAULT;
        optionGamma = -1;   //Negative gamma corresponds to equal rates
        whichGamma = DEFAULT;
        baseFreq = new double[]{0.25, 0.25, 0.25, 0.25};    //default is equal frequencies
        whichBaseFreq = DEFAULT;
        tratio = 2.0; //default is no difference between transitions and transversions
        useML = false; //Use the exact distance by default - transforms without exact distances should set useML = false
    }

    /* Setters and getters. These always return what is currently stored */

    public double getOptionPInvar() {
        return optionPInvar;
    }

    public void setOptionPInvar(double pinvar) {
        optionPInvar = pinvar;
    }

    public double getOptionGamma() {
        return optionGamma;
    }

    public void setOptionGamma(double gamma) {
        optionGamma = gamma;
    }

    public double getOptionTratio() {
        return tratio;
    }

    public void setOptionTratio(double tratio) {
        this.tratio = tratio;
    }

    public boolean getOptionMaximum_Likelihood() {
        return useML;
    }

    public void setOptionMaximum_Likelihood(boolean useML) {
        this.useML = useML;
    }

    public double[] getOptionBaseFreq() {
        return baseFreq;
    }

    public void setOptionBaseFreq(double[] baseFreq) {
        this.baseFreq = baseFreq;
    }

    public double[] getNormedBaseFreq() {
        double[] freqs = new double[4];
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            sum += getOptionBaseFreq()[i];
        }
        for (int i = 0; i < 4; i++) {
            freqs[i] = getOptionBaseFreq()[i] / sum;
        }
        return freqs;
    }


    public int getWhichPInvar() {
        return whichPInvar;
    }

    public void setWhichPInvar(int whichPInvar) {
        this.whichPInvar = whichPInvar;
    }

    public int getWhichGamma() {
        return whichGamma;
    }

    public void setWhichGamma(int whichGamma) {
        this.whichGamma = whichGamma;
    }

    public int getWhichBaseFreq() {
        return whichBaseFreq;
    }

    public void setWhichBaseFreq(int whichBaseFreq) {
        this.whichBaseFreq = whichBaseFreq;
    }

    public boolean freqsOK(double[] freqs) {
        if (freqs == null || freqs.length != 4)
            return false;
        for (int i = 0; i < 4; i++) {
            if (freqs[i] < 0.0)
                return false;
        }
        return true;
    }

    /**
     * Update properties from the characters block.
     * <p/>
     * Affects only those parameters which are set to be read from characters.
     * The base frequencies are computed from scratch - but the others
     * are read from what is in Characters.properties (and assumed correct)
     *
     * @param characters
     */
    public void updateSettings(Characters characters) {
        if (whichPInvar == FROMCHARS) {
            setOptionPInvar(characters.getProperties().getpInvar());
        }
        if (whichGamma == FROMCHARS) {
            setOptionGamma(characters.getProperties().getGammaParam());
        }
        if (whichBaseFreq == FROMCHARS) {
            setOptionBaseFreq(CharactersUtilities.computeFreqs(characters, false));
        }
    }


    /* Now the fields relating to distance computation */
    public boolean isApplicable(Document doc, Taxa taxa, Characters ch) {
        return taxa != null && ch != null
                && (ch.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.DNA)
                || ch.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.RNA));
    }

    /**
     * REturn the inverse of the moment generating function corresponding to the current settings
     *
     * @param x
     * @return double
     */

    protected double Minv(double x) throws SaturatedDistancesException {
        if (x <= 0.0)
            throw new SaturatedDistancesException();
        double p = getOptionPInvar();
        if (p < 0.0 || p > 1.0)
            p = 0.0;
        if (x - p <= 0.0)
            throw new SaturatedDistancesException();
        double alpha = getOptionGamma();
        if (alpha > 0.0) {
            return alpha * (1.0 - Math.pow((x - p) / (1.0 - p), -1.0 / alpha));
        } else
            return Math.log((x - p) / (1 - p));
    }

    /**
     * exact Distance - use an exact distance formula (if available) SHould never be called
     * if the transform does not have an exact dist formula.
     *
     * @param F
     * @return
     * @throws SaturatedDistancesException
     */
    abstract protected double exactDist(double[][] F) throws SaturatedDistancesException;

    /**
     * Fill in the distance matrix
     *
     * @param doc        The document used to display the progress (not used for taxa or characters)
     * @param characters
     * @param model
     * @return
     * @throws SplitsException
     * @throws CanceledException
     */
    protected Distances fillDistanceMatrix(Document doc, Characters characters, NucleotideModel model) throws SplitsException, CanceledException {

        int ntax = characters.getNtax();
        Distances distances = new Distances(ntax);
        distances.getFormat().setTriangle("both");
        String states;
        if (characters.getFormat().getDatatypeID() == Characters.Datatypes.RNAID)
            states = RNASTATES;
        else
            states = DNASTATES;


        int numMissing = 0;

        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {
                PairwiseCompare seqPair =
                        new PairwiseCompare(characters, states, s, t);
                double dist = 100.0;

                if (this.useML) {
                    //Maximum likelihood distance
                    try {
                        dist = seqPair.mlDistance(model);
                    } catch (SaturatedDistancesException e) {
                        numMissing++;
                    }
                } else {
                    //Exact distance
                    double[][] F = seqPair.getF();
                    if (F==null)
                        numMissing++;
                    else {
                        try {
                            dist = exactDist(F);
                        } catch (SaturatedDistancesException e) {
                            numMissing++;
                        }
                    }

                }

                distances.set(s, t, dist);
                distances.set(t, s, dist);

                double var = seqPair.bulmerVariance(dist, 0.75);
                distances.setVar(s, t, var);
                distances.setVar(t, s, var);
            }
            if (doc != null)
                doc.notifySetProgress(s * 100 / ntax);
        }
        if (doc != null)
            doc.notifySetProgress(100); //set progress to 100%

        if (numMissing>0) {
            new Alert("Warning: " + numMissing + " saturated or missing entries in the distance matrix - proceed with caution ");
        }
        return distances;
    }

    public Distances computeDist(Characters characters) {
        try {
            return computeDist(null, characters);
        } catch (CanceledException | SplitsException e) {
        }
        return null;
    }

    abstract protected Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException;

    public Distances apply(Document doc, Taxa taxa, Characters characters) throws SplitsException, CanceledException {
        return computeDist(doc, characters);
    }


    /* The GuiPanel */
    protected JPanel guiPanel;


    protected JPanel getMLPanel(boolean fixedOn) {
        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        //panel.setBorder(BorderFactory.createEtchedBorder());
        final JCheckBox mlBox = new JCheckBox("Use ML Distances");
        mlBox.setToolTipText("Use maximum likelihood distances");
        mlBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                useML = mlBox.isSelected();

            }
        });
        panel.add(mlBox, constraints);

        if (fixedOn) {
            mlBox.setSelected(true);
            mlBox.setEnabled(false);
        } else {
            mlBox.setSelected(getOptionMaximum_Likelihood());
        }

        panel.setMinimumSize(panel.getPreferredSize());
        return panel;
    }

    protected JPanel getTsTVPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        //panel.setBorder(BorderFactory.createEtchedBorder());

        JLabel label = new JLabel("Transition/transversion ratio  ");

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

        panel.add(label, constraints);
        panel.add(textField, constraints);

        panel.setMinimumSize(panel.getPreferredSize());
        return panel;
    }


}
