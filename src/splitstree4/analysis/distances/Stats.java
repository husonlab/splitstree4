/*
 * Stats.java Copyright (C) 2022 Daniel H. Huson
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
/*
 * Created on Aug 17, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree4.analysis.distances;


import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;

/**
 * @author bryant
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
        return "qls score: " + qlsScore(dist);
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
