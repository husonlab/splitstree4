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
 * Created on Aug 17, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree.analysis.distances;


import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

/**
 * @author bryant
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Stats implements DistancesAnalysisMethod {
    /**
     * implementations of analysis-algorithms should overwrite this
     * String with a short description of what they do.
     */
    public static String DESCRIPTION = "Basic statistics for distances";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /* (non-Javadoc)
     * @see splits.analysis.distances.DistancesAnalysisMethod#isApplicable(splits.nexus.Taxa, splits.nexus.Distances, splits.core.DocumentData)
     */

    public boolean isApplicable(Document doc,
                                Taxa taxa, Distances dist) {
        return doc.isValid(taxa) && doc.isValid(dist);
    }

    /* (non-Javadoc)
     * @see splits.analysis.distances.DistancesAnalysisMethod#apply(splits.nexus.Taxa, splits.nexus.Distances, splits.core.DocumentData)
     */

    public String apply(Document doc, Taxa taxa, Distances dist)
            throws Exception {
        return "qls score: " + String.valueOf(qlsScore(dist));
    }

    /**
     * Computes a quartet least squares score of tree likeness
     * - this is the average of
     * w(a,b,c,d)
     * where w(a,b,c,d) is the best OLS score for the distances restricted
     * to {a,b,c,d}. Takes O(n^4) time.
     *
     * @param dist
     * @return qls
     */
    static public double qlsScore(Distances dist) {
        int n = dist.getNtax();
        double score = 0.0;
        double scoresq = 0.0;
        double numqs = 0.0; // We count manually since in future we may randomly sample quartets
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j < i; j++) {
                for (int k = 1; k < j; k++) {
                    for (int l = 1; l < k; l++) {
                        double dij = dist.get(i, j), dik = dist.get(i, k), dil = dist.get(i, l);
                        double djk = dist.get(j, k), djl = dist.get(j, l), dkl = dist.get(k, l);

                        double s1 = (dij + dkl - dik - djl);
                        s1 = s1 * s1;
                        double s2 = (dij + dkl - dil - dkl);
                        s2 = s2 * s2;
                        double s3 = (dik + djl - dil - dkl);
                        s3 = s3 * s3;

                        double ss = dij * dij + dik * dik + dil * dil + djk * djk + djl * djl + dkl * dkl;

                        double qscore = Math.min(s1, Math.min(s2, s3)) / ss;
                        score += qscore;
                        scoresq += qscore * qscore;
                        numqs += 1.0;
                    }
                }
            }
        }
        double v = scoresq / numqs - (score / numqs) * (score / numqs);
        System.err.println("std dev on q score is " + Math.sqrt(v));
        return score / numqs;
    }
}
