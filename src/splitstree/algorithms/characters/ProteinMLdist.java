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
 * Created on Jun 8, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.algorithms.characters;

import jloda.util.Alert;
import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.models.*;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bryant
 *         Computes the maximum likelihood protein distance estimates for a set of characters
 */
public class ProteinMLdist extends SequenceBasedDistance implements Characters2Distances {


    private String optionModel = "JTT";
    private double optionPInvar = 0.0;
    private double optionGamma = 0.0;
    private boolean usePinvar = false;
    private boolean useGamma = false;
    private boolean estimateVariance = true;
    JPanel guiPanel;
    private static final String STATES = "arndcqeghilkmfpstwyv";

    public final static String DESCRIPTION = "Calculates maximum likelihood protein distance estimates";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public ProteinMLdist() {
        //System.err.println("Creating ProteinMLdist object");
    }


    /**
     * Determine whether F84 corrected Hamming distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param ch   the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters ch) {
        return taxa != null && ch != null
                && (ch.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.PROTEIN));
    }


    public boolean checkOptions(Characters characters) {
        return true;
    }

    public String getOptionModel() {
        return optionModel;
    }

    public void setOptionModel(String optionModel) {
        this.optionModel = optionModel;
    }

    public double getOptionPInvar() {
        return optionPInvar;
    }

    public void setOptionPInvar(double pinvar) {
        optionPInvar = pinvar;
    }

    public void setOptionUsePInvar(boolean val) {
        usePinvar = val;
    }

    public boolean getOptionUsePInvar() {
        return usePinvar;
    }

    public double getOptionGamma() {
        return optionGamma;
    }

    public void setOptionGamma(double gamma) {
        optionGamma = gamma;
    }

    public boolean getOptionUseGamma() {
        return useGamma;
    }

    public void setOptionUseGamma(boolean var) {
        useGamma = var;
    }


    /**
     * returns list of all known protein models
     * <p/>
     * In future, this list will be constructed using reflection
     *
     * @return methods
     */
    public List selectionOptionModel(Document doc) {
        //TODO: Get list directly from class list.
        List models = new LinkedList();
        models.add("cpREV45");
        models.add("Dayhoff");
        models.add("JTT");
        models.add("mtMAM");
        models.add("mtREV24");
        models.add("pmb");
        models.add("Rhodopsin");
        models.add("WAG");

        return models;
    }

/*
 * Given the names of the protein model (in the list), this
 * initialises and returns the appropriate ProteinModel object.
 * In future, this can be done using reflection.
 *
 * Returns null if no model identified.
 */

    public ProteinModel selectModel(String modelName) {
        ProteinModel themodel;

        System.err.println("Model name = " + modelName);
        //TODO: Add all models
        switch (modelName) {
            case "cpREV45":
                themodel = new cpREV45Model();
                break;
            case "Dayhoff":
                themodel = new DayhoffModel();
                break;
            case "JTT":
                themodel = new JTTmodel();
                break;
            case "mtMAM":
                themodel = new mtMAMModel();
                break;
            case "mtREV24":
                themodel = new mtREV24Model();
                break;
            case "pmb":
                themodel = new pmbModel();
                break;
            case "Rhodopsin":
                themodel = new RhodopsinModel();
                break;
            case "WAG":
                themodel = new WagModel();
                break;
            default:
                themodel = null;
                break;
        }


        return themodel;
    }

    public void setOptionEstimate_variances(boolean val) {
        this.estimateVariance = val;
    }

    public boolean getOptionEstimate_variances() {
        return this.estimateVariance;
    }


    public Distances computeDist(Characters characters) {
        try {
            return computeDist(null, characters);
        } catch (CanceledException | SplitsException e) {
        }
        return null;
    }


    /**
     * Computes  ML  distances using maximum likelihood and an arbitrary
     * rate matrix.
     *
     * @param characters the input characters
     * @return the computed distances Object
     */

    public Distances computeDist(Document doc, Characters characters)
            throws CanceledException, SplitsException {

        boolean hasSaturated = false;


        Distances distances = new Distances(characters.getNtax());
        distances.getFormat().setTriangle("both");

        if (doc != null) {
            doc.notifySubtask("Protein ML distance");
            doc.notifySetProgress(0);
        }

        int ntax = characters.getNtax();
        int npairs = ntax * (ntax - 1) / 2;

        doc.notifySetMaximumProgress(npairs);
        //initialize maximum progress
        doc.notifySetProgress(0);


        ProteinModel model = selectModel(getOptionModel());
        model.setPinv(this.getOptionPInvar());
        model.setGamma(this.getOptionGamma());

        if (model == null) {
            throw new SplitsException("Incorrect model name");
        }
        int k = 0;
        for (int s = 1; s <= ntax; s++) {
            for (int t = s + 1; t <= ntax; t++) {


                PairwiseCompare seqPair =
                        new PairwiseCompare(characters, STATES, s, t);
                double dist = 100.0;

                //Maximum likelihood distance. Note we want to ignore sites
                //with the stop codon.
                try {
                    dist = seqPair.mlDistance(model);
                } catch (SaturatedDistancesException e) {
                    hasSaturated = true;
                }

                distances.set(s, t, dist);
                distances.set(t, s, dist);

                double var = seqPair.bulmerVariance(dist, 0.93);
                distances.setVar(s, t, var);
                distances.setVar(t, s, var);

                k++;
                doc.notifySetProgress(k * 100 / npairs);
            }

        }

        doc.notifySetProgress(characters.getNtax()); //set progress to 100%
        // pd.close(); //get rid of the progress listener
        // doc.setProgressListener(null);
        if (hasSaturated) {
            new Alert("Warning: saturated or missing entries in the distance matrix - proceed with caution ");
        }
        return distances;

    }


    public Distances apply(Document doc, Taxa taxa, Characters characters)
            throws CanceledException, SplitsException {
        return computeDist(doc, characters);
    }


}//EOF


