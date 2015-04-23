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
* $Id: Codominant.java,v 1.4 2009-11-03 03:43:22 bryant Exp $
*/
package splitstree.algorithms.characters;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

// EOF

/**
 * Implementation of the Co-dominant genetic distance outlined in
 * <p/>
 * Smouse PE, Peakall R (1999) Spatial autocorrelation analysis of individual multiallele and multilocus
 * genetic structure. Heredity, 82, 561-573.
 */


public class Codominant implements Characters2Distances {


    protected String TASK = "Codominant Genetic Distance";
    protected String DESCRIPTION = "Codominant Genetic Distance for diploid characters (Smouse & Peakall 1999)";

    /**
     * In Smouse and Peakall, the final distance is the square root of the contribution of the
     * individual loci. This flag sets whether to use this square root, or just the averages
     * over the loci.
     */
    protected boolean useSquareRoot;

    /**
     * Determine whether  distances can be computed with given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data... taxa and characters are non-null and their is
     *         an even number of characters.
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null && (c.getFormat().isDiploid());
    }

    /**
     * Computes the hamming distance fow a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws Exception {

        return codominant(doc, taxa, characters);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    protected String getTask() {
        return TASK;
    }

    /**
     * Get the flag indicating if the distance computed is the square root of the contributions
     * of the loci (as in (Smouse and Peakall 99).
     *
     * @return boolean flag that is true if we use the square root in the final calculation.
     */
    public boolean getOptionUseSquareRoot() {
        return useSquareRoot;
    }

    /**
     * Set the flag indicating if the distance computed is the square root of the contributions
     * of the loci (as in (Smouse and Peakall 99).
     *
     * @param useSquareRoot flag that is true if we use the square root in the final calculation.
     */
    public void setOptionUseSquareRoot(boolean useSquareRoot) {
        this.useSquareRoot = useSquareRoot;
    }

    /**
     * Computes Hamming distances with a given characters block.
     *
     * @param doc        Document. Used to identify progress.
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     * @throws CanceledException if the user presses cancel in the progress dialog box.
     */
    private Distances codominant(Document doc, Taxa taxa, Characters characters) throws CanceledException, SplitsException {
        Distances distances = new Distances(taxa.getNtax());

        doc.notifySubtask(getTask());
        doc.notifySetMaximumProgress(100);
        doc.notifySetProgress(0);
        char missingchar = characters.getFormat().getMissing();
        char gapchar = characters.getFormat().getGap();

        if (characters.hasCharweights()) {
            throw new SplitsException("The Codominant distance is not available when there are character weights");
        }

        int ntax = taxa.getNtax();
        for (int i = 1; i <= ntax; i++) {
            char[] seqi = characters.getRow(i);


            for (int j = i + 1; j <= ntax; j++) {

                char[] seqj = characters.getRow(j);
                double distSquared = 0.0;


                int nchar = characters.getNchar();
                int nLoci = nchar / 2;
                int nValidLoci = 0;


                for (int k = 1; k <= nLoci; k++) {
                    char ci1 = seqi[2 * k - 1];
                    char ci2 = seqi[2 * k];
                    char cj1 = seqj[2 * k - 1];
                    char cj2 = seqj[2 * k];

                    if (ci1 == missingchar || ci2 == missingchar || cj1 == missingchar || cj2 == missingchar)
                        continue;
                    if (ci1 == gapchar || ci2 == gapchar || cj1 == gapchar || cj2 == gapchar)
                        continue;

                    nValidLoci++;

                    int diff;

                    if (ci1 == ci2) { //AA vs ...
                        if (cj1 == cj2) {
                            if (ci1 != cj1)
                                diff = 4;   //AA vs BB
                            else
                                diff = 0;  //AA vs AA
                        } else {  //AA vs XY
                            if (ci1 == cj1 || ci1 == cj2)
                                diff = 1; //AA vs AY
                            else
                                diff = 3; //AA vs BC
                        }
                    } else {     //AB vs ...
                        if (cj1 == cj2) {  //AB vs XX
                            if (ci1 == cj1 && ci2 == cj1)
                                diff = 1;   //AB vs AA
                            else
                                diff = 3;   //AB vs CC
                        } else {  //AB vs XY
                            if ((ci1 == cj1 && ci2 == cj2) || (ci1 == cj2 && ci2 == cj1))
                                diff = 0; //AB vs BA or AB vs AB
                            else if (ci1 == cj1 || ci2 == cj2 || ci1 == cj2 || ci2 == cj1)
                                diff = 1;   //AB vs AC
                            else
                                diff = 2;   //AB vs CD
                        }
                    }

                    distSquared += (double) diff;
                }

                double dij = nchar / 2.0 * distSquared / (double) nValidLoci;
                if (getOptionUseSquareRoot())
                    dij = Math.sqrt(dij);

                distances.set(i, j, Math.sqrt(dij));
                distances.set(j, i, Math.sqrt(dij));
            }
            doc.notifySetProgress(i * 100 / ntax);
        }
        doc.notifySetProgress(taxa.getNtax());
        return distances;
    }


}
