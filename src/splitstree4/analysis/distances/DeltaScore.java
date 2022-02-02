/*
 * DeltaScore.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.distances;

import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.NumberUtils;
import splitstree4.algorithms.characters.Characters2Distances;
import splitstree4.algorithms.distances.NJ;
import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.PaupTreeUtils;
import splitstree4.algorithms.util.simulate.GenerateRandom;
import splitstree4.algorithms.util.simulate.RandomCharacters;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.gui.Director;
import splitstree4.models.EqualRatesmodel;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.util.CharactersUtilities;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Random;

/**
 * Computes the delta score of a set of taxa
 * David Bryant
 */
public class DeltaScore implements DistancesAnalysisMethod {
    final static public String DESCRIPTION = "Computes the delta score for the selected taxa (Holland et al 02)";
    int[] optionSelectedTaxa = null;

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Runs the analysis
     *
     * @param taxa the taxa
     * @param dist the distances
     */
    public String apply(Document doc, Taxa taxa, Distances dist) throws Exception {


        boolean DEBUG_DELTA = false;

        //First check that the selected taxa are all legitimate.
        if (getOptionSelectedTaxa() != null) {
            for (int i = 0; i < getOptionSelectedTaxa().length; i++) {
                int t = getOptionSelectedTaxa()[i];
                if (t < 1 || t > taxa.getNtax())
                    throw new SplitsException("Taxon-id out of range: " + getOptionSelectedTaxa()[i]);
            }
        }

        //Form the list of taxa to process.
        int[] selectedTaxa;
        if (optionSelectedTaxa != null && optionSelectedTaxa.length > 0)
            selectedTaxa = optionSelectedTaxa;
        else {
            selectedTaxa = new int[taxa.getNtax()];
            for (int i = 1; i <= taxa.getNtax(); i++)
                selectedTaxa[i - 1] = i;
        }

        //Sort selected taxa
        Arrays.sort(selectedTaxa);

        if (selectedTaxa.length < 4)
            return "Delta score=0 (because fewer than 4 taxa selected)";


        double[][] taxonAverages = new double[2][selectedTaxa.length];
        double[] totalAverage = {0.0, 0.0};

        boolean numericalProblems = false;
        // double avDistance = 0.0;

        //Loop over all quartets
        for (int i4 = 0; i4 < selectedTaxa.length; i4++) {
            int t4 = selectedTaxa[i4];
            for (int i3 = 0; i3 < i4; i3++) {
                int t3 = selectedTaxa[i3];
                for (int i2 = 0; i2 < i3; i2++) {
                    int t2 = selectedTaxa[i2];
                    for (int i1 = 0; i1 < i2; i1++) {
                        int t1 = selectedTaxa[i1];
                        double d_12_34 = dist.get(t1, t2) + dist.get(t3, t4);
                        double d_13_24 = dist.get(t1, t3) + dist.get(t2, t4);
                        double d_14_23 = dist.get(t1, t4) + dist.get(t2, t3);

                        double[] qs = {d_12_34, d_13_24, d_14_23};
                        //manual bubble sort
                        if (qs[0] > qs[1]) {
                            double tmp = qs[0];
                            qs[0] = qs[1];
                            qs[1] = tmp;
                        }
                        if (qs[1] > qs[2]) {
                            double tmp = qs[1];
                            qs[1] = qs[2];
                            qs[2] = tmp;
                        }
                        if (qs[0] > qs[1]) {
                            double tmp = qs[0];
                            qs[0] = qs[1];
                            qs[1] = tmp;
                        }
                        //evaluate score
                        double[] delta = {0, 0};
                        if (qs[2] > qs[0] + 1e-7) {
                            delta[0] = (qs[2] - qs[1]) / (qs[2] - qs[0]);
                            delta[1] = (qs[2] - qs[1]) * (qs[2] - qs[1]);
                        } else {
                            if (qs[2] != qs[0])   //Flag that there where quartets where delta is unstable.
                                numericalProblems = true;
                        }


                        if (DEBUG_DELTA) {
                            System.out.println("" + t1 + ", " + t2 + ", " + t3 + ", " + t4 + ", " + "[" + d_12_34 + ", " + d_13_24 + ", " + d_14_23 + "], " + delta[0]);
                        }


                        for (int k = 0; k < 2; k++) {
                            taxonAverages[k][i1] += delta[k];
                            taxonAverages[k][i2] += delta[k];
                            taxonAverages[k][i3] += delta[k];
                            taxonAverages[k][i4] += delta[k];
                            totalAverage[k] += delta[k];
                        }

                    }
                }
			}
		}
		int n = selectedTaxa.length;
		int ntriples = (n - 1) * (n - 2) * (n - 3) / 6;     //Number of triples containing a given taxon
		int nquads = ntriples * n / 4;     //Number of 4-sets

		for (int i = 0; i < n; i++) {
			taxonAverages[0][i] /= ntriples;
			taxonAverages[1][i] /= ntriples;
		}
		totalAverage[0] /= nquads;
		totalAverage[1] /= nquads;


		double avDistance = computeAverageDistance(dist, selectedTaxa);
		double scale = avDistance * avDistance;
		totalAverage[1] /= scale;


		//Print out the individual taxon scores
		if (numericalProblems) {
			System.out.println("WARNING: Some quartets were close to 'star-like' so set to zero for delta score calculation\n");
        }

        System.out.println("Delta scores for individual taxa\nId\tTaxon\tDelta Score \tQ-residual");
		for (int i = 0; i < selectedTaxa.length; i++) {
			System.out.print("" + selectedTaxa[i] + "\t" + doc.getTaxa().getLabel(selectedTaxa[i]));
			System.out.println("\t" + NumberUtils.roundSigFig(taxonAverages[0][i], 5) + "\t" + NumberUtils.roundSigFig(taxonAverages[1][i] / scale, 5));
		}
		System.out.println("===========================");
		//   System.out.println("Average delta score for selection = " + Basic.roundSigFig(totalAverage[0], 5) + "\nAverage Q-residual = " + Basic.roundSigFig(totalAverage[1], 5) + "\n");
		//  System.out.println("Average distance = " + (avDistance));


		// double[] pvals = computeParametricPval(doc, 100, selectedTaxa, totalAverage);
		String result = "\nDelta score = " + NumberUtils.roundSigFig(totalAverage[0], 4);//+ " (p-val = " + Basic.roundSigFig(pvals[0], 4)+")";
		result += "\nQ-residual score = " + NumberUtils.roundSigFig(totalAverage[1], 4);// + " (p-val = " + Basic.roundSigFig(pvals[1], 4)+")";


		// String result = "\nDelta score = " + Basic.roundSigFig(totalAverage[0], 4) + " (p-val = " + Basic.roundSigFig(pvals[0], 4)+")";
		//result += "\nQ-residual score = " + Basic.roundSigFig(totalAverage[1], 4) + " (p-val = " + Basic.roundSigFig(pvals[1], 4)+")";

		return result;
	}

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param dist the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances dist) {
        return doc.isValid(taxa) && doc.isValid(dist);
    }

    public int[] getOptionSelectedTaxa() {
        return optionSelectedTaxa;
    }

    public void setOptionSelectedTaxa(int[] selectedTaxa) {
        this.optionSelectedTaxa = selectedTaxa;
    }

    private double computeAverageDistance(Distances dist, int[] selectedTaxa) {
        double sum = 0;
        int npairs = 0;
        if (selectedTaxa != null) {
            for (int i = 0; i < selectedTaxa.length; i++)
                for (int j = i + 1; j < selectedTaxa.length; j++) {
                    sum += dist.get(selectedTaxa[i], selectedTaxa[j]);
                    npairs++;
                }
        } else {
            for (int i = 1; i <= dist.getNtax(); i++)
                for (int j = 2; j <= dist.getNtax(); j++) {
                    sum += dist.get(i, j);
                    npairs++;
                }
        }

        return (sum / (double) npairs);
    }

    public double[] computeParametricPval(Document doc, int nreps, int[] selectedTaxa, double[] observed) throws IOException, CanceledException, SplitsException {

        doc.notifySetMaximumProgress(nreps);
        doc.notifyTasks("Parametric Bootstrapping of Delta Score", "reps=" + nreps);

        /* Create NJ tree. Need to create a new taxa and Distances since we only want
        the selected taxa.
         */

        TaxaSet toHide = doc.getTaxa().getTaxaSet();
        for (int aSelectedTaxa : selectedTaxa) toHide.unset(aSelectedTaxa);
        Distances theDistances = doc.getDistances().clone(doc.getTaxa());
        theDistances.hideTaxa(doc.getTaxa(), toHide);
        Taxa theTaxa = Taxa.getInduced(doc.getTaxa(), toHide);

        NJ nj = new NJ();
        PhyloTree tree = (nj.apply(null, theTaxa, theDistances)).getTree(1);

        System.err.println(tree.toString());

        PaupNode paupTree = new PaupNode();
        try {
            paupTree = PaupTreeUtils.convert(theTaxa, tree, tree.getRoot());
        } catch (Exception ex) {
            Basic.caught(ex);
            throw new SplitsException("Parametric analysis failed: " + ex);
        }
        //Construct a model.
        double[] freqs = CharactersUtilities.computeFreqs(doc.getCharacters(), false);
        EqualRatesmodel model = new EqualRatesmodel(freqs);


        /* Create a new document that will be the bootstrap replicate document  */
        Document bdoc = new Document();
        bdoc.setTaxa(theTaxa);
        bdoc.setAssumptions(doc.getAssumptions().clone(bdoc.getTaxa()));
        bdoc.getAssumptions().setExTaxa(null);
        bdoc.setInBootstrap(true);


        int len = doc.getCharacters().getNactive();
        if (len < 0)
            len = doc.getCharacters().getNchar();//Resample only from the number of characters not excluded.
        GenerateRandom rand = new GenerateRandom();
        Characters newChars = new Characters(theTaxa.getNtax(), len, doc.getCharacters().getFormat());
        bdoc.setCharacters(newChars);

        int[] numGreater = {0, 0};
        int nrepsMade = 0;

        for (int r = 0; r < nreps; r++) {
            RandomCharacters.simulateCharacters(bdoc.getCharacters(), paupTree, model, null, true, rand);

            if (r < 0) {
                StringWriter sw = new StringWriter();
                theTaxa.write(sw);
                bdoc.getCharacters().write(sw, doc.getTaxa());
                Director newDir = Director.newProject(sw.toString(), doc.getFile().getAbsolutePath());
                newDir.getDocument().setTitle("First bootstrap replicate");
                newDir.showMainViewer();
            }


            PrintStream ps = jloda.util.Basic.hideSystemErr();//disable syserr.
            try {
                Characters2Distances trans = (Characters2Distances) bdoc.getAssumptions().getCharactersTransform();
                Distances dist = trans.apply(bdoc, bdoc.getTaxa(), bdoc.getCharacters());
                double valD = 0;
                double valQ = 0;
                //System.out.println(dist.toString());

                //Loop over all quartets
                for (int i4 = 0; i4 < selectedTaxa.length; i4++) {
                    int t4 = selectedTaxa[i4];
                    for (int i3 = 0; i3 < i4; i3++) {
                        int t3 = selectedTaxa[i3];

                        for (int i2 = 0; i2 < i3; i2++) {
                            int t2 = selectedTaxa[i2];
                            for (int i1 = 0; i1 < i2; i1++) {
                                int t1 = selectedTaxa[i1];
                                double d_12_34 = dist.get(t1, t2) + dist.get(t3, t4);
                                double d_13_24 = dist.get(t1, t3) + dist.get(t2, t4);
                                double d_14_23 = dist.get(t1, t4) + dist.get(t2, t3);

                                double[] qs = {d_12_34, d_13_24, d_14_23};
                                //manual bubble sort
                                if (qs[0] > qs[1]) {
                                    double tmp = qs[0];
                                    qs[0] = qs[1];
                                    qs[1] = tmp;
                                }
                                if (qs[1] > qs[2]) {
                                    double tmp = qs[1];
                                    qs[1] = qs[2];
                                    qs[2] = tmp;
                                }
                                if (qs[0] > qs[1]) {
                                    double tmp = qs[0];
                                    qs[0] = qs[1];
                                    qs[1] = tmp;
                                }
                                //evaluate score
                                if (qs[2] > qs[0]) {
                                    valD += (qs[2] - qs[1]) / (qs[2] - qs[0]);
                                    valQ += (qs[2] - qs[1]) * (qs[2] - qs[1]);
                                }


                            }
                        }
                    }
                }
                int n = selectedTaxa.length;
                int nQuads = n * (n - 1) * (n - 2) * (n - 3) / 24;     //Number of triples containing a given taxon

                double avDistance = computeAverageDistance(dist, null);
                double scale = avDistance * avDistance;
                valD /= nQuads;
                valQ /= nQuads;
                valQ /= scale;
                nrepsMade++;

                if (valD > observed[0])
                    numGreater[0]++;
                if (valQ > observed[1])
                    numGreater[1]++;


                //System.out.println("delta:\t" + valD + "\tq:\t" + valQ);
            } catch (Exception ex) {
                Basic.caught(ex);
                throw new SplitsException("Bootstrapping failed: " + ex);
            } finally {
                jloda.util.Basic.restoreSystemErr(ps);
            }


            try {
                doc.notifySetProgress(r);
            } catch (CanceledException ex) {
                String message = "Bootstrap cancelled: only " + r + " bootstrap replicates stored";
                new Alert(message);
                break;
            }
        }

        double[] pvals = new double[2];
        double mean, var, z;
        /*Compute p-value for delta scores.
        mean = totals[0]/nreps;
        var = ((double)nreps/(nreps-1)) * ((totalSquares[0])/nreps - mean*mean);
        z = (observed[0]-mean)/Math.sqrt(var);
        pvals[0] = 0.5 - 0.5* ErrorFunction.derf(z/Math.sqrt(2.0));

        //Now of the Q-scores
        mean = totals[1]/nreps;
        var = ((double)nreps/(nreps-1)) * ((totalSquares[1])/nreps - mean*mean);
        z = (observed[1]-mean)/Math.sqrt(var);
        pvals[1] = 0.5 - 0.5* ErrorFunction.derf(z/Math.sqrt(2.0));
        */
        pvals[0] = numGreater[0] / nrepsMade;
        pvals[1] = numGreater[1] / nrepsMade;


        return pvals;
    }

    public double[] computeNonParametricPval(Document doc, int nreps, int[] selectedTaxa, double[] observed) throws CanceledException, SplitsException {

        doc.notifySetMaximumProgress(nreps);
        doc.notifyTasks("Non-Parametric Bootstrapping of Delta Score", "reps=" + nreps);

        /* Set up a copied characters block
         */

        TaxaSet toHide = doc.getTaxa().getTaxaSet();
        for (int aSelectedTaxa : selectedTaxa) toHide.unset(aSelectedTaxa);
        Taxa theTaxa = Taxa.getInduced(doc.getTaxa(), toHide);

        Characters origChars = doc.getCharacters().clone(doc.getTaxa());
        origChars.hideTaxa(doc.getTaxa(), toHide);


        Characters2Distances trans = (Characters2Distances) doc.getAssumptions().getCharactersTransform();


        Random rand = new Random();
        int[] numGreater = {0, 0};
        int nrepsMade = 0; //NUmber of reps actually carried out.

        for (int r = 0; r < nreps; r++) {
            //RandomCharacters.simulateCharacters(bdoc.getCharacters(), paupTree, model, null, true, rand);
            Characters chars = CharactersUtilities.resample(theTaxa, origChars, origChars.getNchar(), rand);

            PrintStream ps = jloda.util.Basic.hideSystemErr();//disable syserr.
            try {
                Distances dist = trans.apply(null, theTaxa, chars);
                double valD = 0;
                double valQ = 0;
                //System.out.println(dist.toString());

                //Loop over all quartets
                for (int i4 = 0; i4 < selectedTaxa.length; i4++) {
                    int t4 = selectedTaxa[i4];
                    for (int i3 = 0; i3 < i4; i3++) {
                        int t3 = selectedTaxa[i3];

                        for (int i2 = 0; i2 < i3; i2++) {
                            int t2 = selectedTaxa[i2];
                            for (int i1 = 0; i1 < i2; i1++) {
                                int t1 = selectedTaxa[i1];
                                double d_12_34 = dist.get(t1, t2) + dist.get(t3, t4);
                                double d_13_24 = dist.get(t1, t3) + dist.get(t2, t4);
                                double d_14_23 = dist.get(t1, t4) + dist.get(t2, t3);

                                double[] qs = {d_12_34, d_13_24, d_14_23};
                                //manual bubble sort
                                if (qs[0] > qs[1]) {
                                    double tmp = qs[0];
                                    qs[0] = qs[1];
                                    qs[1] = tmp;
                                }
                                if (qs[1] > qs[2]) {
                                    double tmp = qs[1];
                                    qs[1] = qs[2];
                                    qs[2] = tmp;
                                }
                                if (qs[0] > qs[1]) {
                                    double tmp = qs[0];
                                    qs[0] = qs[1];
                                    qs[1] = tmp;
                                }
                                //evaluate score
                                if (qs[2] > qs[0]) {
                                    valD += (qs[2] - qs[1]) / (qs[2] - qs[0]);
                                    valQ += (qs[2] - qs[1]) * (qs[2] - qs[1]);
                                }
                                nrepsMade++;

                            }
                        }
                    }
                }
                int n = selectedTaxa.length;
                int nQuads = n * (n - 1) * (n - 2) * (n - 3) / 24;     //Number of triples containing a given taxon

                double avDistance = computeAverageDistance(dist, selectedTaxa);
                double scale = avDistance * avDistance;
                valD /= nQuads;
                valQ /= nQuads;
                valQ /= scale;

                /* totals[0]+=valD;
                totalSquares[0]+=valD*valD;
                totals[1]+=valQ;
                totalSquares[1]+=valQ*valQ;
                */


                if (valD > 2 * observed[0])
                    numGreater[0]++;
                if (valQ > 2 * observed[1])
                    numGreater[1]++;
                System.out.println("delta:\t" + valD + "\tq:\t" + valQ);
            } catch (Exception ex) {
                Basic.caught(ex);
                throw new SplitsException("Bootstrapping failed: " + ex);
            } finally {
                jloda.util.Basic.restoreSystemErr(ps);
            }


            try {
                doc.notifySetProgress(r);
            } catch (CanceledException ex) {
                String message = "Bootstrap cancelled: only " + r + " bootstrap replicates stored";
                new Alert(message);
                break;
            }
        }

        double[] pvals = new double[2];
        double mean, var, z;
        //Compute p-value for delta scores.
        /*
           nreps = nrepsMade;
           mean = totals[0]/nreps;
           var = ((double)nreps/(nreps-1)) * ((totalSquares[0])/nreps - mean*mean);
           z = (observed[0]-mean)/Math.sqrt(var);
           pvals[0] = 0.5 - 0.5* ErrorFunction.derf(z/Math.sqrt(2.0));

           //Now of the Q-scores
           mean = totals[1]/nreps;
           var = ((double)nreps/(nreps-1)) * ((totalSquares[1])/nreps - mean*mean);
           z = (observed[1]-mean)/Math.sqrt(var);
           pvals[1] = 0.5 - 0.5* ErrorFunction.derf(z/Math.sqrt(2.0));
           */
        pvals[0] = numGreater[0] / nrepsMade;
        pvals[1] = numGreater[1] / nrepsMade;

        return pvals;
    }


}
