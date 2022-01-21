/*
 * SimulationForLarsPaper.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util.simulate;

import jloda.swing.util.Alert;
import splitstree4.algorithms.distances.LARSnetwork;
import splitstree4.algorithms.util.PaupNode;
import splitstree4.algorithms.util.PaupTreeUtils;
import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 28, 2008
 * Time: 7:08:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimulationForLarsPaper {

    static String safeDouble(double x) {
        String s = "" + x;
        char[] c = new char[s.length()];

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.')
                c[i] = '_';
            else
                c[i] = s.charAt(i);
        }

        return new String(c);
    }

    static public void apply(int ntax, double height, double balance, double noise, int nblocks, String var, int reps, String filebase, boolean longHeader, boolean shortHeader, long seed) {
        String filename = filebase + "_" + ntax + "_" + safeDouble(noise) + "__" + var;

        String header = "LARS simulation\n";
        header += "Coalescent tree; ntax = " + ntax + " height = " + height + " balance = " + balance + "\n";
        header += "Gaussian distances: noise(stdev) = " + noise + "\n";
        header += "Inference with least squares: variance = " + var + "\n";
        header += "Number of reps = " + reps + "\n";
        header += "output base filename = " + filebase + "\n";
        header += "-----------------------------------";
        System.out.println(header);

        //Open file
        try {
            FileWriter out = new FileWriter(filename + ".txt");
            if (longHeader)
                out.write(header + "\n");

            if (shortHeader) {
                out.write("rep\tntax\theight\tbalance\tnoise\tvar\t");
                out.write("nnet:sigma1\tnsplits1\tntrivial1\tres1\taic1\tnnet:sigma2\tnsplits2\tntrivial2\tres2\taic2\tnnet:sigma3\tnsplits3\tntrivial3\tres3\taic3\tnnetfull:nsplitsf\tntrivialf\tresf\taicf\t");
                out.write("nj:sigma1\tnsplits1\tntrivial1\tres1\taic1\tnj:sigma2\tnsplits2\tntrivial2\tres2\taic2\tnj:sigma3\tnsplits3\tntrivial3\tres3\taic3\tnjfull:nsplitsf\tntrivialf\tresf\taicf\n");

            }
            GenerateRandom random = new GenerateRandom(seed);

            long startTime = System.currentTimeMillis();

            for (int r = 0; r < reps; r++) {
                System.out.println("Replicate " + (r + 1));
                //Simulate tree.
                Document newDoc = new Document();
                Taxa taxa = RandomTaxa.generateTaxa(ntax);
                newDoc.setTaxa(taxa);
                newDoc.setCharacters(null);


                PaupNode T = RandomTrees.randomCoalescentTree(taxa, 0.1, random);
                RandomTrees.relaxExponential(T, balance, random);

                //Generate distance matrix
                Distances dist_orig = RandomDistances.getAdditiveDistances(taxa, T);

                for (int i = 0; i < nblocks - 1; i++) {
                    // T = RandomTrees.randomCoalescentTree(taxa, 0.1, random);
                    System.out.println("" + PaupTreeUtils.getNewick(taxa, T, false));

                    RandomTrees.randomSubtreeSwap(T, random);


                    System.out.println("" + PaupTreeUtils.getNewick(taxa, T, false));

                    //RandomTrees.relaxExponential(T, 0.0,random);
                    Distances dist_block = RandomDistances.getAdditiveDistances(taxa, T);
                    //RandomDistances.alterDistances(dist_block, noise, random);

                    for (int k = 1; k <= taxa.getNtax(); k++) {
                        for (int l = 1; l <= taxa.getNtax(); l++) {
                            dist_orig.set(k, l, dist_orig.get(k, l) + dist_block.get(k, l));
                        }
                    }
                }
                if (nblocks > 1) {
                    for (int k = 1; k <= taxa.getNtax(); k++) {
                        for (int l = 1; l <= taxa.getNtax(); l++) {
                            dist_orig.set(k, l, dist_orig.get(k, l) / (double) nblocks);
                        }
                    }
                }

                RandomDistances.alterDistances(dist_orig, noise, random);


                out.write("" + (r + 1) + "\t" + ntax + "\t" + height + "\t" + balance + "\t" + noise + "\t" + var);


                LARSnetwork larsnet = new LARSnetwork();
                larsnet.setOptionLeastAngleRegression(true);
                larsnet.setOptionNormalisation("EuclideanSquared");
                larsnet.setOptionOrdering(null);
                larsnet.setOptionUseAllCircularSplits(false);
                larsnet.setOptionVariance(var);
                larsnet.setOptionVar(noise); //ToDo CAREFUL: this applies only to power = 1 case!!!


                larsnet.setOptionForceTrivial(true);
                larsnet.output = out;

                larsnet.setOptionNetworkMethod("NeighborNet");

                out.write("\t");
                //System.out.println("LARS on NeighborNet");
                larsnet.apply(null, taxa, dist_orig);

                larsnet.setOptionNetworkMethod("NeighborJoining");
                //System.out.println("LARS on Neighbor-Joining");

                larsnet.apply(null, taxa, dist_orig);
                out.write("\n");
            }

            out.close();
            long endTime = System.currentTimeMillis();
            System.out.println(" Simulation took " + ((float) (endTime - startTime) / 1000.0) + " seconds");


        } catch (Exception ex) {
            new Alert("Error when performing LARS simulations");
            ex.printStackTrace();
        }

        //Generate underlying tree


    }
}
