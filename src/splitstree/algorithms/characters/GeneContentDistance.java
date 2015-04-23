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

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

import java.util.BitSet;


/**
 * implements Snel Bork et al., 1999 as well as  Huson Steel, 2003
 */
public class GeneContentDistance implements Characters2Distances {

    public final static String DESCRIPTION = "Compute distances based on shared genes (Snel Bork et al 1999, Huson and Steel 2003)";
    private boolean useMLDistance = false;


    /**
     * Determine whether the gene content distance can be computed with given data.
     *
     * @param taxa the taxa
     * @param ch   the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters ch) {
        return taxa != null && ch != null && ch.getFormat().getDatatype().equalsIgnoreCase(Characters.Datatypes.STANDARD);
    }

    /**
     * Computes gap-distances with a given characters block.
     *
     * @param taxa       the taxa
     * @param characters the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters characters) throws CanceledException {
        System.err.println("Not tested under construction");
        /*@todo: Tobias: test this class
        */
        BitSet genes[] = computeGenes(characters);
        if (!useMLDistance)
            return computeSnelBorkDistance(taxa.getNtax(), genes);
        else
            return computeMLDistance(taxa.getNtax(), genes);
    }

    /**
     * returns true if the maximum likelihood distances is used
     *
     * @return
     */
    public boolean getOptionUseMLDistance() {
        return useMLDistance;
    }

    /**
     * use the maximum likelihood distance in the computation
     *
     * @param useMLDistance
     */
    public void setOptionUseMLDistance(boolean useMLDistance) {
        this.useMLDistance = useMLDistance;
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
     * comnputes the SnelBork et al distance
     *
     * @param ntax
     * @param genes
     * @return the distance Object
     */
    private static Distances computeSnelBorkDistance(int ntax, BitSet[] genes) {
        Distances dist = new Distances(ntax);
        for (int i = 1; i <= ntax; i++) {
            dist.set(i, i, 0.0);
            for (int j = i + 1; j <= ntax; j++) {
                BitSet intersection = ((BitSet) (genes[i]).clone());
                intersection.and(genes[j]);
                dist.set(j, i, (float) (1.0 - ((float) intersection.cardinality() / (float) Math.min(genes[i].cardinality(), genes[j].cardinality()))));
                dist.set(i, j, dist.get(j, i));
            }
        }
        return dist;
    }

    /**
     * comnputes the maximum likelihood estimator distance Huson and Steel 2003
     *
     * @param ntax
     * @param genes
     * @return the distance Object
     */
    private static Distances computeMLDistance(int ntax, BitSet[] genes) {
        Distances dist = new Distances(ntax);
// dtermine average genome size:
        double m = 0;
        for (int i = 1; i <= ntax; i++) {
            m += genes[i].cardinality();
        }
        m /= ntax;

        double ai[] = new double[ntax + 1];
        double aij[][] = new double[ntax + 1][ntax + 1];
        for (int i = 1; i <= ntax; i++) {
            ai[i] = ((double) genes[i].cardinality()) / m;
        }
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                BitSet intersection = ((BitSet) (genes[i]).clone());
                intersection.and(genes[j]);
                aij[i][j] = aij[j][i] = ((double) intersection.cardinality()) / m;
            }
        }

        for (int i = 1; i <= ntax; i++) {
            dist.set(i, i, 0.0);
            for (int j = i + 1; j <= ntax; j++) {
                double b = 1.0 + aij[i][j] - ai[i] - ai[j];

                dist.set(j, i, (float) -Math.log(0.5 * (b + Math.sqrt(b * b + 4.0 * aij[i][j] * aij[i][j]))));
                if (dist.get(j, i) < 0)
                    dist.set(j, i, 0.0);
                dist.set(i, j, dist.get(j, i));
            }
        }
        return dist;
    }


    /**
     * computes gene sets from strings
     *
     * @param characters object wich holds the sequences
     * @return sets of genes
     */
    static private BitSet[] computeGenes(Characters characters) {
        BitSet genes[] = new BitSet[characters.getNtax() + 1];

        for (int s = 1; s <= characters.getNtax(); s++) {
            genes[s] = new BitSet();
            for (int i = 1; i <= characters.getNchar(); i++) {
                if (characters.get(s, i) == '1')
                    genes[s].set(i);
            }
        }
        return genes;
    }
}
