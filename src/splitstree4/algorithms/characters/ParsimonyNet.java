/*
 * ParsimonyNet.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.characters;

import splitstree4.algorithms.distances.NeighborNet;
import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Nov 9, 2010
 * Time: 12:00:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParsimonyNet /*implements Characters2Splits*/ {
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws Exception {

        /** Use NeighborNet to get an initial ordering */
        Hamming hamming = new Hamming();
        Distances dist = hamming.apply(null, taxa, chars);
        int[] ordering = NeighborNet.computeNeighborNetOrdering(dist);    //NB: ordering in positions 1...n


        /**Compute length **/
        String states = chars.getFormat().getSymbols();
        int r = states.length();
        char missing = chars.getFormat().getMissing();
        char gap = chars.getFormat().getGap();

        int n = taxa.getNtax();
        int[][][] L = new int[n][n][r];

        int[] s = new int[r];
        int totalLength = 0;

        for (int site = 1; site <= chars.getNchar(); site++) {
            if (chars.isMasked(site))
                continue;

            //Construct an array of character states, ordered to correspond with given circular ordering.
            String thisSite = chars.getColumn(site);
            int[] siteSymbols = new int[n + 1];
            for (int i = 1; i <= n; i++) {
                char ci = thisSite.charAt(ordering[i] - 1);
                if (ci == missing || ci == gap)
                    siteSymbols[i] = -1;
                else
                    siteSymbols[i] = states.indexOf(ci);
            }


            for (int i = 1; i <= n - 1; i++) {
                if (siteSymbols[i] < 0)      //Gap or missing character
                    Arrays.fill(L[i][i], 0);
                else {
                    Arrays.fill(L[i][i], 1);     //All states have one change, except the right one.
                    L[i][i][siteSymbols[i]] = 0;
                }
            }

            for (int k = 1; k < n - 1; k++) {
                for (int i = 1; i < n - k; i++) {
                    int j = i + k;

                    for (int a = 0; a < r; a++) {
                        int minval = Integer.MAX_VALUE;
                        for (int l = i; l < j; l++) {
                            int thisval = L[i][l][a] + L[l + 1][j][a];
                            minval = Math.min(thisval, minval);
                        }
                        s[a] = minval;
                    }

                    for (int a = 0; a < r; a++) {
                        int minval = Integer.MAX_VALUE;
                        for (int b = 0; b < r; b++) {
                            int thisval;
                            if (a == b)
                                thisval = s[b];
                            else
                                thisval = s[b] + 1;
                            minval = Math.min(thisval, minval);
                        }
                        L[i][j][a] = minval;
                    }

                }
            }

            int minval = Integer.MAX_VALUE;
            int root = siteSymbols[n];
            for (int a = 0; a < r; a++) {
                int thisval;
                if (a == root)
                    thisval = L[1][n - 1][a];
                else
                    thisval = 1 + L[1][n - 1][a];
                minval = Math.min(thisval, minval);
            }

            totalLength += minval;
            for (int i = 1; i <= n; i++) {
                System.err.print(thisSite.charAt(ordering[i] - 1));
            }

            System.err.println("  Length of site[" + site + "]=" + thisSite + " is " + minval);
        }

        System.err.println("Total length = " + totalLength);

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return chars != null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
