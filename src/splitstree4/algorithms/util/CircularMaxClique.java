/*
 * CircularMaxClique.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util;

import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 20, 2005
 * Time: 5:02:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class CircularMaxClique {

    static public double getMaxClique(Splits splits, double[] weights) {

        int[] ordering = splits.getCycle();
        int ntax = splits.getNtax();

        /* First step - read the splits back into an array */
        //Save the splits onto a hashmap
        HashMap map = new HashMap();
        int outgroup = ordering[1];

        for (int i = 1; i <= splits.getNsplits(); i++) {
            TaxaSet sp = splits.get(i);
            if (sp.get(outgroup))
                sp = sp.getComplement(ntax);
            map.put(sp.toString(), i);
        }

        double[][] w = new double[ntax][ntax];
        int[][] splitIds = new int[ntax][ntax];
        int n = 0;

        for (int i = 0; i < ntax; i++) {
            TaxaSet t = new TaxaSet();
            for (int j = i + 1; j < ntax; j++) {
                t.set(ordering[j + 1]);
                if (map.containsKey(t.toString())) {
                    int id = (Integer) map.get(t.toString());
                    //System.err.println(""+id);
                    n++;
                    double weight = weights[id];
                    splitIds[i][j] = id;
                    w[j][i] = weight;
                }

            }
        }
        //System.err.println("Found "+n+" splits,  and there should be "+splits.getNsplits());

        //We can now pretend that the taxa are labelled 0... ntax-1, and that w[j][i] is the
        // weight for the cluster  i,i+1,....,j-1.
        // We now let w[i][j] be the max weight tree with given ordering an max cluster i,i+1,...,j-1

        int[][] M = new int[ntax][ntax];

        for (int i = 0; i + 1 < ntax; i++)
            w[i][i + 1] = w[i + 1][i];      //Trivial clusters - get weight automatically.


        for (int k = 2; k < ntax; k++) {
            for (int i = 0; i + k < ntax; i++) {
                double maxweight = -1.0;
                for (int j = i + 1; j < i + k; j++) {
                    double x = w[i][j] + w[j][i + k];
                    if (x > maxweight) {
                        M[i][i + k] = j;
                        maxweight = x;
                    }
                }
                w[i][i + k] = w[i + k][i] + maxweight;
            }
        }

        //we now extract a max weight clique

        boolean[][] clique = new boolean[ntax][ntax];
        for (int i = 0; i < ntax; i++)
            for (int j = 0; j < ntax; j++)
                clique[i][j] = false;

        extractClique(M, clique, 0, ntax - 1);

        //Now zero all splits not in the clique
        for (int i = 0; i < ntax; i++) {
            for (int j = i + 1; j < ntax; j++) {
                int id = splitIds[i][j];
                if (id > 0 && !clique[i][j])
                    splits.setWeight(id, 0);
            }
        }

        return w[0][ntax - 1];

    }

    static private void extractClique(int[][] M, boolean[][] clique, int i, int j) {
        clique[i][j] = true;
        if (j > i + 1) {
            int k = M[i][j];
            extractClique(M, clique, i, k);
            extractClique(M, clique, k, j);
        }
    }
}
