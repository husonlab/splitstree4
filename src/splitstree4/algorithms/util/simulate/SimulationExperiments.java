/**
 * SimulationExperiments.java
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
package splitstree4.algorithms.util.simulate;

import splitstree4.algorithms.additional.ClosestTree;
import splitstree4.algorithms.characters.JukesCantor;
import splitstree4.algorithms.distances.NJ;
import splitstree4.algorithms.distances.NeighborNet;
import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.PaupTreeUtils;
import splitstree4.analysis.bootstrap.TestTreeness;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.models.JCmodel;
import splitstree4.models.NucleotideModel;
import splitstree4.nexus.*;
import splitstree4.util.*;

import java.io.FileWriter;

/**
 * SimulationExperiments
 * <p/>
 * These are 'development' code  methods for generating random data and testing method. This will be formalised
 * in future to give a full simulator in splitstree (we're pretty close).
 */
public class SimulationExperiments {


    public static void checkNakleh(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            GenerateRandom random = new GenerateRandom();
            simDoc.setInBootstrap(true);
            Characters chars = new Characters();
            chars.getFormat().setDatatype(Characters.Datatypes.DNA);

            int[] ntaxaSim = {20};
            double[] heightSim = {0.1};
            double[] balanceSim = {Math.log(2)};
            int[] seqLengths = {4000, 500, 1000, 2000, 4000};
            double gammaShape = 0.8168;
            double pInvar = 0.5447;

            /*
            MAPLE code used to get Q matrix.
            > Q:=matrix([[0,3.297,12.55,1.167],[0,0,2.060,13.01],[0,0,0,1.0],[0,0,0,0]]);
            > f:=vector([0.1776,0.3336,0.2595,0.2293]);
            > for i from 2 to 4 do for j from 1 to i-1 do Q[i,j]:=Q[j,i]*f[j]/f[i];od;od;
            > evalm(Q);
            > u:=vector([1,1,1,1]); v:=evalm(Q&*u);
            > for i from 1 to 4 do Q[i,i]:=-v[i];od;
            > evalm(Q);
            */

            // NucleotideModel model = new GTRmodel(Q,f);
            NucleotideModel model = new JCmodel();

            //Set up the distance transform
            /* GTR dist = new GTR();
            dist.setOptionBaseFreq(f);
            dist.setOptionMaximum_Likelihood(true);
            dist.setOptionQMatrix(model.getQ());
            dist.setOptionGamma(gammaShape);
            dist.setOptionPInvar(pInvar);
            */
            JukesCantor dist = new JukesCantor();
            dist.setOptionGamma(gammaShape);
            dist.setOptionPInvar(pInvar);


            int numReplicates = 10;
            int numBootstraps = 1000;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t seqLength \t nnetFP \t nnetFN \t njFP \t njFN \t nnetBFP \t nnetBFN \t njBFP \t njBFN \t nndist \t njdist \t fat_falseneg\n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            int nn_falsepos, nn_falseneg, nj_falsepos, nj_falseneg, nnb_falsepos, nnb_falseneg, njb_falsepos, njb_falseneg, fat_falseneg;
                            nn_falsepos = nn_falseneg = nj_falsepos = nj_falseneg = nnb_falsepos = nnb_falseneg = njb_falsepos = njb_falseneg = fat_falseneg = 0;

                            double nn_l1dist = 0.0, nj_l1dist = 0.0;


                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                simDoc.setInBootstrap(true);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                //Simulate tree and characters
                                RandomTrees.relaxExponential(T, balance, random);
                                Characters newChars = new Characters(ntax, seqLength, chars.getFormat());
                                RandomCharacters.simulateCharacters(newChars, T, model, gammaShape, pInvar, random);
                                simDoc.setCharacters(newChars);

                                //Compute distance matrix.
                                Distances distances = dist.computeDist(newChars);

                                //Get the NJ tree
                                NJ njAlgo = new NJ();
                                Trees trees = njAlgo.apply(simDoc, taxa, distances);
                                Splits njSplits = TreesUtilities.convertTreeToSplits(trees, 1, taxa);

                                //Get the NNet
                                NeighborNet nnet = new NeighborNet();
                                nnet.selectVariance("FitchMargoliash2");
                                Splits nnSplits = nnet.apply(simDoc, taxa, distances);

                                //Get all NNET splits
                                int[] ordering = nnet.getCycle();

                                /*
                                FileWriter outdata = new FileWriter(filename + "Chars4000" + ".nex");
                                outdata.write("#NEXUS\n\n");
                                taxa.write(outdata);
                                newChars.write(outdata,taxa);
                                outdata.close();
                               */

                                Splits allSplits = new Splits(ntax);
                                for (int i = 0; i < ntax; i++) {
                                    TaxaSet t = new TaxaSet();
                                    for (int j = i + 1; j < ntax; j++) {
                                        t.set(ordering[j + 1]);
                                        allSplits.add(t, 1);
                                    }
                                }

                                //Bootstrap the two
                                SplitMatrix nnetBM = new SplitMatrix(ntax, nnSplits);
                                SplitMatrix njBM = new SplitMatrix(ntax, njSplits);

                                for (int i = 0; i < numBootstraps; i++) {
                                    Characters replicate = CharactersUtilities.resample(taxa, newChars, newChars.getNchar(), random);
                                    Distances replicateD = dist.computeDist(replicate);
                                    Trees njReplicateTrees = njAlgo.apply(null, taxa, replicateD);
                                    njBM.add(TreesUtilities.convertTreeToSplits(njReplicateTrees, 1, taxa));
                                    nnetBM.add(nnet.apply(null, taxa, replicateD));
                                }


                                SplitMatrixAnalysis.evalConfidences(nnetBM, nnSplits);
                                SplitMatrixAnalysis.evalConfidences(njBM, njSplits);

                                Splits nnbSplits = new Splits();
                                Splits njbSplits = new Splits();


                                for (int i = 1; i <= njSplits.getNsplits(); i++)
                                    if (njSplits.getConfidence(i) >= 0.95)
                                        njbSplits.add(njSplits.get(i), njSplits.getWeight(i));
                                for (int i = 1; i <= nnSplits.getNsplits(); i++)
                                    if (nnSplits.getConfidence(i) >= 0.95)
                                        nnbSplits.add(nnSplits.get(i), nnSplits.getWeight(i));

                                //Get the true splits
                                Splits trueSplits = PaupTreeUtils.getBinarySplits(T, ntax);

                                //Put all three split blocks in a split Matrix
                                SplitMatrix splitMatrix = new SplitMatrix(ntax);
                                splitMatrix.add(trueSplits);
                                splitMatrix.add(njSplits);
                                splitMatrix.add(nnSplits);
                                splitMatrix.add(allSplits);


                                double[] trueVec = SplitMatrixAnalysis.splitsToArray(splitMatrix, trueSplits);
                                double[] njVec = SplitMatrixAnalysis.splitsToArray(splitMatrix, njSplits);
                                double[] nnVec = SplitMatrixAnalysis.splitsToArray(splitMatrix, nnSplits);
                                double[] njbVec = SplitMatrixAnalysis.splitsToArray(splitMatrix, njbSplits);
                                double[] nnbVec = SplitMatrixAnalysis.splitsToArray(splitMatrix, nnbSplits);
                                double[] fatNetFN = SplitMatrixAnalysis.splitsToArray(splitMatrix, allSplits);

                                int n = trueVec.length;
                                for (int i = 1; i < n; i++) {
                                    //Splits indexed from 1,2,...,n-1

                                    //System.out.println(""+trueVec[i]+"\t"+njVec[i]+"\t"+nnVec[i]);

                                    if (trueVec[i] > 0.0) {
                                        if (njVec[i] == 0.0)
                                            nj_falseneg++;
                                        if (nnVec[i] == 0.0)
                                            nn_falseneg++;
                                        if (njbVec[i] == 0.0)
                                            njb_falseneg++;
                                        if (nnbVec[i] == 0.0)
                                            nnb_falseneg++;
                                        if (fatNetFN[i] == 0.0)
                                            fat_falseneg++;


                                    } else {
                                        if (njVec[i] > 0.0)
                                            nj_falsepos++;
                                        if (nnVec[i] > 0.0)
                                            nn_falsepos++;
                                        if (njbVec[i] > 0.0)
                                            njb_falsepos++;
                                        if (nnbVec[i] > 0.0)
                                            nnb_falsepos++;
                                    }
                                    nn_l1dist += Math.abs(trueVec[i] - nnVec[i]);
                                    nj_l1dist += Math.abs(trueVec[i] - njVec[i]);

                                }


                                doc.getProgressListener().setProgress(progress++);
                            }
                            out.write("" + ntax + "\t" + height + "\t" + "\t" + seqLength);
                            out.write("\t" + (double) nn_falsepos / numReplicates + "\t" + (double) nn_falseneg / numReplicates);
                            out.write("\t" + (double) nj_falsepos / numReplicates + "\t" + (double) nj_falseneg / numReplicates);
                            out.write("\t" + (double) nnb_falsepos / numReplicates + "\t" + (double) nnb_falseneg / numReplicates);
                            out.write("\t" + (double) njb_falsepos / numReplicates + "\t" + (double) njb_falseneg / numReplicates);
                            out.write("\t" + nn_l1dist / (numReplicates) + "\t" + nj_l1dist / numReplicates);
                            out.write("\t" + (double) fat_falseneg / (numReplicates) + "\n");
                            out.flush();

                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    /**
     * Simulates a taxa block, a tree and then characters on the tree. Sets the taxa and characters block of the
     * document specified. The tree is generated according to the coalescent,  with a relaxed clock. The characters
     * are generated according to the Jukes Cantor model
     *
     * @param doc
     * @param ntax
     * @param nchars
     * @param height
     * @param balance
     */
    public static void simulateData(Document doc, int ntax, int nchars, double height, double balance) {

        Taxa taxa = RandomTaxa.generateTaxa(ntax);
        doc.setTaxa(taxa);
        PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
        RandomTrees.relaxClockLogNormal(T, balance);
        try {
            Characters newChars = new Characters(ntax, nchars, doc.getCharacters().getFormat());
            //newChars.setFormat(doc.getCharacters().getFormat());
            //newChars.resetMatrix(ntax, nchars);
            RandomCharacters.simulateCharacters(newChars, T, new JCmodel());
            doc.setCharacters(newChars);
            doc.update(Taxa.NAME);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    /**
     * Generates a taxa block, random tree and a distance on the tree. The tree is generated according to the
     * coalescent,  with a relaxed clock. The distances are generated using an multivariate normal with
     * covariance matrix I * (stdev^2)
     *
     * @param doc
     * @param ntax
     * @param height
     * @param balance
     * @param stdev
     */
    public static void simulateData(Document doc, int ntax, double height, double balance, double stdev) {

        Taxa taxa = RandomTaxa.generateTaxa(ntax);
        doc.setTaxa(taxa);
        doc.setCharacters(null);
        GenerateRandom random = new GenerateRandom(10);
        PaupNode T = RandomTrees.randomCoalescentTree(taxa, height, random);
        RandomTrees.relaxClockLogNormal(T, balance);
        System.out.println(T.writeTreeDescription());
        Distances dist = RandomDistances.getAdditiveDistances(taxa, T);
        RandomDistances.alterDistances(dist, stdev, random);

        doc.setDistances(dist);

    }


    private static double evaluateDistVar(Splits trueSplits, SplitMatrix M) {
        double x, xx, y;
        x = xx = 0.0;

        double[] trueVec = SplitMatrixAnalysis.splitsToArrayFromZero(M, trueSplits);
        for (int i = 0; i < M.getNblocks(); i++) {
            //Compute this distance, d
            double[] col = M.getMatrixColumn(i);
            double d = 0.0;
            for (int j = 0; j < col.length; j++) {
                y = (col[j] - trueVec[j]);
                d += y * y;
            }
            d = Math.sqrt(d);

            x += d;
            xx += d * d;
        }
        int n = M.getNblocks();
        return xx / n - (x / n) * (x / n);
    }


    public static void compareResidues(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            simDoc.setTaxa((Taxa) doc.getTaxa().clone());
            simDoc.setAssumptions(doc.getAssumptions().clone(simDoc.getTaxa()));
            simDoc.getAssumptions().setExTaxa(null);
            simDoc.setInBootstrap(true);


            int[] ntaxaSim = {50};
            double[] heightSim = {0.01, 0.1};
            double[] balanceSim = {0, 0.5};
            int[] seqLengths = {300, 1000};
            int numReplicates = 50;
            int numBootstraps = 100;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t varParametric \t varNonParametric \n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars = new Characters(ntax, seqLength, doc.getCharacters().getFormat());
                                //newChars.setFormat(doc.getCharacters().getFormat());
                                //newChars.resetMatrix(ntax, seqLength);
                                simDoc.setCharacters(newChars);

                                //Parametric bootstrap
                                Bootstrap bootstrap = new Bootstrap();
                                bootstrap.setRuns(numBootstraps);
                                bootstrap.setLength(seqLength);
                                simDoc.setBootstrap(bootstrap);
                                JCmodel jcM = new JCmodel();
                                simDoc.setSplits(PaupTreeUtils.getBinarySplits(T, ntax));
                                simDoc.getBootstrap().computeParametric(simDoc, T, jcM);

                                //Evaluate variances
                                SplitMatrix M = simDoc.getBootstrap().getSplitMatrix();
                                Splits trueSplits = PaupTreeUtils.getBinarySplits(T, ntax);
                                double paramVar = SimulationExperiments.evaluateDistVar(trueSplits, M);

                                //NonParametric Bootstrap

                                RandomCharacters.simulateCharacters(newChars, T, jcM);
                                simDoc.setCharacters(newChars);
                                simDoc.update(null);

                                simDoc.parse("bootstrap runs=" + numBootstraps);
                                M = simDoc.getBootstrap().getSplitMatrix();
                                double nonParamVar = SimulationExperiments.evaluateDistVar(simDoc.getSplits(), M);


                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + paramVar + "\t" + nonParamVar + "\n");

                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private static double evaluateOrthogonalVar(Splits trueSplits, SplitMatrix M, double[] weights) {
        double x, xx, y;
        x = xx = 0.0;

        double[] trueVec = SplitMatrixAnalysis.splitsToArrayFromZero(M, trueSplits);
        for (int i = 0; i < M.getNblocks(); i++) {
            //Compute this distance, d
            double[] col = M.getMatrixColumn(i);
            double d = 0.0;
            for (int j = 0; j < col.length; j++) {
                if (!(weights[j] > 0.0)) {
                    y = (col[j] - trueVec[j]);
                    //d+=y*y;
                    d += Math.abs(y);
                }
            }
            //d = Math.sqrt(d);

            x += d;
            xx += d * d;
        }
        int n = M.getNblocks();
        return xx / n - (x / n) * (x / n);
    }

    public static void compareOrthogonal(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            simDoc.setTaxa((Taxa) doc.getTaxa().clone());
            simDoc.setAssumptions(doc.getAssumptions().clone(simDoc.getTaxa()));
            simDoc.getAssumptions().setExTaxa(null);
            simDoc.setInBootstrap(true);


            int[] ntaxaSim = {10, 20, 30};
            double[] heightSim = {0.01, 0.1};
            double[] balanceSim = {0, 0.5};
            int[] seqLengths = {300, 1000};
            int numReplicates = 50;
            int numBootstraps = 100;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t varParametric \t varNonParametric \n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars = new Characters(ntax, seqLength, doc.getCharacters().getFormat());
                                //newChars.setFormat(doc.getCharacters().getFormat());
                                //newChars.resetMatrix(ntax, seqLength);
                                simDoc.setCharacters(newChars);

                                //Parametric bootstrap
                                Bootstrap bootstrap = new Bootstrap();
                                bootstrap.setRuns(numBootstraps);
                                bootstrap.setLength(seqLength);
                                simDoc.setBootstrap(bootstrap);
                                JCmodel jcM = new JCmodel();
                                simDoc.setSplits(PaupTreeUtils.getBinarySplits(T, ntax));
                                simDoc.getBootstrap().computeParametric(simDoc, T, jcM);

                                //Evaluate variances
                                SplitMatrix M = simDoc.getBootstrap().getSplitMatrix();
                                Splits trueSplits = PaupTreeUtils.getBinarySplits(T, ntax);
                                double[] trueVec = SplitMatrixAnalysis.splitsToArrayFromZero(M, trueSplits);
                                double paramVar = SimulationExperiments.evaluateOrthogonalVar(trueSplits, M, trueVec);

                                //NonParametric Bootstrap

                                RandomCharacters.simulateCharacters(newChars, T, jcM);
                                simDoc.setCharacters(newChars);
                                simDoc.update(null);

                                simDoc.parse("bootstrap runs=" + numBootstraps);
                                M = simDoc.getBootstrap().getSplitMatrix();

                                Splits splits = simDoc.getSplits();
                                trueSplits = new Splits(splits.getNtax());
                                trueSplits.setCycle(splits.getCycle());
                                trueSplits.getProperties().setCompatibility(splits.getProperties().getCompatibility());

                                double[] weights = new double[splits.getNsplits() + 1];
                                for (int i = 1; i <= splits.getNsplits(); i++) {
                                    double x = splits.getWeight(i);
                                    TaxaSet A = splits.get(i);
                                    trueSplits.add(A, (float) x);
                                    //weights[i] = x*x;
                                    weights[i] = x;
                                }

                                ClosestTree.apply(trueSplits, weights);

                                trueVec = SplitMatrixAnalysis.splitsToArrayFromZero(M, trueSplits);
                                double nonParamVar = SimulationExperiments.evaluateOrthogonalVar(simDoc.getSplits(), M, trueVec);


                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + paramVar + "\t" + nonParamVar + "\n");

                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    public static void testCoverage(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            simDoc.setTaxa((Taxa) doc.getTaxa().clone());
            simDoc.setAssumptions(doc.getAssumptions().clone(simDoc.getTaxa()));
            simDoc.getAssumptions().setExTaxa(null);
            simDoc.setInBootstrap(true);


            int[] ntaxaSim = {10, 20, 30};
            double[] heightSim = {0.01, 0.1};
            double[] balanceSim = {0, 0.5};
            int[] seqLengths = {300, 1000};
            int numReplicates = 20;
            int numBootstraps = 1000;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t numUnder \t numOver \t numFalse \t covered  \n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars = new Characters(ntax, seqLength, doc.getCharacters().getFormat());
                                //newChars.setFormat(doc.getCharacters().getFormat(),);
                                simDoc.setCharacters(newChars);

                                //NonParametric Bootstrap
                                JCmodel jcM = new JCmodel();
                                RandomCharacters.simulateCharacters(newChars, T, jcM);
                                simDoc.setCharacters(newChars);
                                simDoc.update(null);

                                simDoc.parse("bootstrap runs=" + numBootstraps);

                                Splits splits = simDoc.getSplits();
                                Splits trueSplits = PaupTreeUtils.getBinarySplits(T, ntax);
                                boolean covers = true;
                                int numUnder, numOver, numFalse;

                                numUnder = numOver = numFalse = 0;

                                for (int i = 1; i <= splits.getNsplits(); i++) {
                                    TaxaSet A = splits.get(i);
                                    Interval interval = splits.getInterval(i);
                                    double a = interval.low;
                                    double b = interval.high;
                                    int id;
                                    for (id = trueSplits.getNsplits(); id > 0; id--) {
                                        TaxaSet B = trueSplits.get(id);
                                        if (A.equalsAsSplit(B, ntax))
                                            break;
                                    }

                                    if (id > 0) {
                                        //Found match
                                        if (trueSplits.getWeight(id) < a) {
                                            covers = false;
                                            numUnder++;
                                        }
                                        if (trueSplits.getWeight(id) > b) {
                                            covers = false;
                                            numOver++;
                                        }
                                    } else {
                                        if (a > 0) {
                                            numFalse++;
                                            covers = false;
                                        }

                                    }

                                }


                                int isCovered = (covers) ? 1 : 0;

                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + numUnder + "\t" + numOver + "\t" + numFalse + "\t" + isCovered + "\n");


                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    /**
     * Runs a simulation test where data is generated on trees, neoighbornet is run, and we compute whether
     * or not neighbornet contains the splits in the true tree.
     *
     * @param doc
     * @param filename
     */
    public static void splitsInNetworkTest(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            simDoc.setTaxa((Taxa) doc.getTaxa().clone());
            simDoc.setAssumptions(doc.getAssumptions().clone(simDoc.getTaxa()));
            simDoc.getAssumptions().setExTaxa(null);
            simDoc.setInBootstrap(true);


            int[] ntaxaSim = {10, 50, 100};
            double[] heightSim = {0.02, 0.2};
            double[] balanceSim = {0, 0.5};
            int[] seqLengths = {1000, 10000};
            int numReplicates = 50;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);
            JukesCantor jc = new JukesCantor();
            JCmodel jcM = new JCmodel();

            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t circNeg \t circOK \t nnetNeg \t nnetOK \n ");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars = new Characters(ntax, seqLength, doc.getCharacters().getFormat());
                                //newChars.setFormat(doc.getCharacters().getFormat());
                                //newChars.resetMatrix(ntax, seqLength);
                                RandomCharacters.simulateCharacters(newChars, T, jcM);
                                Distances dist = jc.computeDist(newChars);

                                NeighborNet nnet = new NeighborNet();
                                Splits nnSplits = nnet.apply(null, taxa, dist);
                                int[] ordering = nnet.getCycle();


                                Splits allSplits = new Splits(ntax);
                                for (int i = 0; i < ntax; i++) {
                                    TaxaSet t = new TaxaSet();
                                    for (int j = i + 1; j < ntax; j++) {
                                        t.set(ordering[j + 1]);
                                        allSplits.add(t, 1);
                                    }
                                }

                                Splits trueSplits = PaupTreeUtils.getBinarySplits(T, ntax);
                                int circMissing = 0;
                                int nnetMissing = 0;

                                for (int s = 1; s <= trueSplits.getNsplits(); s++) {
                                    TaxaSet A = trueSplits.get(s);
                                    boolean nnetFound = false;
                                    for (int t = 1; !nnetFound && t <= nnSplits.getNsplits(); t++) {
                                        TaxaSet B = nnSplits.get(t);
                                        nnetFound = B.equalsAsSplit(A, ntax);
                                    }

                                    if (!nnetFound) {
                                        nnetMissing++;
                                        boolean circFound = false;
                                        for (int t = 1; !circFound && t <= allSplits.getNsplits(); t++) {
                                            TaxaSet B = allSplits.get(t);
                                            circFound = B.equalsAsSplit(A, ntax);
                                        }
                                        if (!circFound)
                                            circMissing++;
                                    }

                                    boolean njFound = false;


                                }

                                int circOK = (circMissing == 0) ? 1 : 0;
                                int nnetOK = (nnetMissing == 0) ? 1 : 0;

                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + circMissing + "\t" + circOK + "\t" + nnetMissing + "\t" + nnetOK + "\n");


                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    /**
     * Test for false positives of the treeness test.
     *
     * @param doc
     * @param filename
     */
    public static void falsePositiveTest(Document doc, String filename) {
        try {

            //Set up the simulation document
            Document simDoc = new Document();
            simDoc.setTaxa((Taxa) doc.getTaxa().clone());
            simDoc.setAssumptions(doc.getAssumptions().clone(simDoc.getTaxa()));
            simDoc.getAssumptions().setExTaxa(null);
            simDoc.setInBootstrap(true);


            int[] ntaxaSim = {15};
            double[] heightSim = {0.2};
            double[] balanceSim = {0.5};
            int[] seqLengths = {1000, 10000};
            int numReplicates = 20;
            int numBootstraps = 1000;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t pValue  \n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                simDoc.setTaxa(taxa);
                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                //System.err.println(PaupTreeUtils.getNewick(taxa,T,true));

                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars = new Characters(ntax, seqLength, doc.getCharacters().getFormat());
                                //newChars.setFormat(doc.getCharacters().getFormat());
                                //newChars.resetMatrix(ntax, seqLength);
                                simDoc.setCharacters(newChars);

                                //NonParametric Bootstrap
                                JCmodel jcM = new JCmodel();
                                RandomCharacters.simulateCharacters(newChars, T, jcM);
                                simDoc.setCharacters(newChars);
                                simDoc.update(null);

                                simDoc.parse("bootstrap runs=" + numBootstraps);
                                boolean pass = TestTreeness.testTreeness(simDoc.getSplits(), simDoc.getBootstrap());
                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + pass + "\n");


                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    /**
     * Test for false negatives of the treeness test.
     * We use a very simple model for recombinant data.... 1/2 of the sites are generated from
     * one tree and 1/2 are from the other.
     *
     * @param doc
     * @param filename
     */
    public static void falseNegativeTest(Document doc, String filename) {
        try {

            //Set up the simulation document
            //Document simDoc = new Document();
            doc.setTaxa((Taxa) doc.getTaxa().clone());
            doc.setAssumptions(doc.getAssumptions().clone(doc.getTaxa()));
            doc.getAssumptions().setExTaxa(null);
            doc.setInBootstrap(true);


            int[] ntaxaSim = {10, 20, 30};
            double[] heightSim = {0.01, 0.1};
            double[] balanceSim = {0, 0.5};
            int[] seqLengths = {300, 1000};
            int numReplicates = 20;
            int numBootstraps = 1000;


            int ntax;
            double height;
            double balance;
            int seqLength;

            int maxProgress = ntaxaSim.length * heightSim.length * balanceSim.length * seqLengths.length * numReplicates;
            int progress = 0;
            doc.getProgressListener().setMaximum(maxProgress);
            doc.getProgressListener().setProgress(0);


            for (int aNtaxaSim : ntaxaSim) {
                ntax = aNtaxaSim;
                FileWriter out = new FileWriter(filename + ntax + ".txt");
                out.write("ntaxa \t height \t balance \t seqLength \t pValue  \n");

                for (double aHeightSim : heightSim) {
                    height = aHeightSim;
                    for (double aBalanceSim : balanceSim) {
                        balance = aBalanceSim;
                        for (int seqLength1 : seqLengths) {
                            seqLength = seqLength1;

                            for (int r = 0; r < numReplicates; r++) {
                                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                                doc.setTaxa(taxa);

                                PaupNode T = RandomTrees.randomCoalescentTree(taxa, height);
                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars1 = new Characters(ntax, seqLength / 2, doc.getCharacters().getFormat());
                                //newChars1.setFormat(doc.getCharacters().getFormat());
                                //newChars1.resetMatrix(ntax, seqLength / 2);
                                JCmodel jcM = new JCmodel();
                                RandomCharacters.simulateCharacters(newChars1, T, jcM);

                                T = RandomTrees.randomCoalescentTree(taxa, height);
                                RandomTrees.relaxClockLogNormal(T, balance);
                                Characters newChars2 = new Characters(ntax, seqLength - seqLength / 2, doc.getCharacters().getFormat());
                                //newChars2.setFormat(doc.getCharacters().getFormat());
                                //newChars2.resetMatrix(ntax, seqLength - seqLength / 2);
                                RandomCharacters.simulateCharacters(newChars2, T, jcM);


                                doc.setCharacters(CharactersUtilities.concatenate(newChars1, newChars2));
                                doc.update(null);


                                doc.parse("bootstrap runs=" + numBootstraps);
                                boolean reject = TestTreeness.testTreeness(doc.getSplits(), doc.getBootstrap());
                                out.write("" + ntax + "\t" + height + "\t" + balance + "\t" + seqLength + "\t" + reject + "\n");


                                doc.getProgressListener().setProgress(progress++);
                            }
                        }
                    }

                }
                out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


}
