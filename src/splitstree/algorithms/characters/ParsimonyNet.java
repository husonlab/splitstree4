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

import splitstree.algorithms.distances.NeighborNet;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

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
        Distances dist = hamming.apply(null,taxa,chars);
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

        for(int site=1;site<=chars.getNchar();site++) {
            if (chars.isMasked(site))
                continue;

            //Construct an array of character states, ordered to correspond with given circular ordering.
            String thisSite = chars.getColumn(site);
            int[] siteSymbols = new int[n+1];
            for(int i=1;i<=n;i++) {
                char ci = thisSite.charAt(ordering[i]-1);
                if (ci==missing || ci==gap)
                    siteSymbols[i]=-1;
                else
                    siteSymbols[i] = states.indexOf(ci);
            }


            for(int i=1;i<=n-1;i++) {
                if (siteSymbols[i]<0)      //Gap or missing character
                    Arrays.fill(L[i][i],0);
                else  {
                    Arrays.fill(L[i][i],1);     //All states have one change, except the right one.
                    L[i][i][siteSymbols[i]]=0;
                }
            }

            for(int k=1;k<n-1;k++) {
                for(int i=1;i<n-k;i++) {
                    int j=i+k;
                    
                    for (int a=0;a<r;a++) {
                        int minval = Integer.MAX_VALUE;
                        for(int l=i;l<j;l++) {
                            int thisval = L[i][l][a] + L[l+1][j][a];
                            minval = Math.min(thisval,minval);
                        }
                        s[a]=minval;
                    }

                    for(int a=0;a<r;a++) {
                        int minval = Integer.MAX_VALUE;
                        for(int b=0;b<r;b++) {
                            int thisval;
                            if (a==b)
                                thisval = s[b];
                            else
                                thisval = s[b]+1;
                            minval = Math.min(thisval,minval);
                        }
                        L[i][j][a]=minval;
                    }

                }
            }

            int minval = Integer.MAX_VALUE;
            int root = siteSymbols[n];
            for(int a=0;a<r;a++) {
                int thisval;
                if (a==root)
                    thisval = L[1][n-1][a];
                else
                    thisval = 1+L[1][n-1][a];
                minval = Math.min(thisval,minval);
            }

            totalLength+=minval;
            for(int i=1;i<=n;i++) {
                System.err.print(thisSite.charAt(ordering[i]-1));
            }

            System.err.println("  Length of site["+site+"]="+thisSite+" is "+minval);
        }

        System.err.println("Total length = "+totalLength);

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return chars!=null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
