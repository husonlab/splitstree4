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

package splitstree.algorithms.unaligned;

import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

import java.util.LinkedList;
import java.util.List;

/**
 * Implements the k-mer distance for unaligned sequences
 */
public class KMerDistance implements Unaligned2Distances {
    public final boolean EXPERT = true;

    // known distance methods
    static final String Muscle = "Muscle";
    static final String Euklidian = "Euklidian";

    public final static String DESCRIPTION = "Calculates the k-mer distance";
    private int kMerLength = 3;
    private String DistanceMethod = "Muscle";

    private boolean compressedAlphabet = false;

    private int[] proteinMap = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, 1, 2, 3, 4, 5, 6, 7, -1, 8, 9, 10, 11, -1, 12, 13, 14, 15, 16, -1, 17, 18, -1, 19, 20};
    private int[] dnaMap = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, 1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1};
    private int[] rnaMap = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, 1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1};
    private int[] binaryMap = new int[]{0, 1};

    /**
      * Determine whether given method can be applied to given data.
      *
      * @param taxa     the taxa
      * @param data  the unaligned sequences
      * @return true, if method applies to given data
      */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned data) {
        return (taxa.isValid() & data.isValid());
    }

    /**
       * Applies the method to the given data
       *
       * @param taxa     the taxa
       * @param data  the unaligned sequences
       * @return the computed quartets
       */
    public Distances apply(Document doc, Taxa taxa, Unaligned data) throws Exception {
        Distances re = new Distances(taxa.getNtax());
        int[] map = null;
        int[][] kmers;
        int nBases = -1;

        if (data.getFormat().getDatatype().equalsIgnoreCase("protein")) {
            map = proteinMap;
            nBases = 21;
        } else if (data.getFormat().getDatatype().equalsIgnoreCase("dna")) {
            map = dnaMap;
            nBases = 4;
        } else if (data.getFormat().getDatatype().equalsIgnoreCase("rna")) {
            map = rnaMap;
            nBases = 4;
        } else if (data.getFormat().getDatatype().equalsIgnoreCase("standard")) {
            map = binaryMap;
            nBases = 2;
        }
        if (map == null) throw new Exception("Unable to map sequence type.");
        // @todo change to start with 0
        kmers = new int[taxa.getNtax() + 1][(int) Math.pow(nBases, kMerLength)];
        // map kmers to intervalls
        for (int i = 1; i <= taxa.getNtax(); i++) {
            char[] seq = data.getRow(i);
            for (int k = 1; k < seq.length - kMerLength + 1; k++) {
                int seqId = 0;
                for (int l = 0; l < kMerLength; l++) {
                    if (Character.getNumericValue(seq[k + l]) > map.length || Character.getNumericValue(seq[k + l]) < 1 || map[Character.getNumericValue(seq[k + l])] == -1)
                    {
                        l = kMerLength;
                        seqId = -1;
                    } else
                        seqId += Math.pow(nBases, l) * map[Character.getNumericValue(seq[k + l])];
                }
                if (seqId != -1) kmers[i][seqId]++;
            }
        }
        // calculate distance
        if (getOptionDistanceMethod().equals(Euklidian)) {
            for (int i = 1; i <= taxa.getNtax(); i++) {
                for (int j = i + 1; j <= taxa.getNtax(); j++) {
                    double dist = 0.0;
                    for (int k = 0; k < kmers[0].length; k++) {
                        dist += Math.pow(kmers[i][k] - kmers[j][k], 2);
                    }
                    re.set(i, j, Math.sqrt(dist));
                    re.set(j, i, Math.sqrt(dist));
                }
            }
        } else if (getOptionDistanceMethod().equals(Muscle)) {
            for (int i = 1; i <= taxa.getNtax(); i++) {
                for (int j = i + 1; j <= taxa.getNtax(); j++) {
                    double dist = 0.0;
                    for (int k = 0; k < kmers[0].length; k++) {
                        dist += (double) Math.min(kmers[i][k], kmers[j][k]) / (double) (Math.min(data.getRow(i).length, data.getRow(j).length) - kMerLength + 1);
                    }
                    re.set(i, j, 1.0 - dist);
                    re.set(j, i, 1.0 - dist);
                }
            }
        }
        return re;
    }

    /**
     *  get the length of the kmer used in the calculation
     * @return
     */
    public int getOptionkMerLength() {
        return this.kMerLength;
    }

    /**
     * set the length of the kmer used in the calculation
     * @param kMerLength
     */
    public void setOptionkMerLength(int kMerLength) {
        this.kMerLength = kMerLength;
    }

    /**
     * gets the method to use
     *
     * @return name of method
     */
    public String getOptionDistanceMethod() {
        return DistanceMethod;
    }

    /**
     * sets the method to use
     */
    public void setOptionDistanceMethod(String DistanceMethod) {
        this.DistanceMethod = DistanceMethod;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List selectionOptionDistanceMethod(Document doc) {
        List methods = new LinkedList();
        methods.add(Muscle);
        methods.add(Euklidian);
        return methods;
    }

    /**
       * gets a short description of the algorithm
       *
       * @return a description
       */
    public String getDescription() {
        return DESCRIPTION;
    }
}
