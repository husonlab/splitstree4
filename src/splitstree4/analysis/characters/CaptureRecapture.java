/*
 * CaptureRecapture.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.characters;


import splitstree4.core.Document;
import splitstree4.nexus.Characters;

import java.util.Arrays;
import java.util.Random;

/**
 * Estimates the proportion of invariant sites using capture-recapture
 */
public class CaptureRecapture implements CharactersAnalysisMethod {
    public static final String DESCRIPTION = "Estimation of invariant sites using capture-recapture method (Lockhart, Huson, Steel, 2000)";
	int optionTaxaCutoff = 20; //Cut off before we switch to sampling

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc) {
        return doc.getCharacters() != null;
    }

    /**
     * Chooses a random (small) subset of size elements in [1...n]
     *
     * @return array of size with numbers from [1...n]
     */
    private static int[] randomSubset(int size, int n, Random random) {
        int[] s = new int[size];
        for (int i = 0; i < size; i++) {
            int x = random.nextInt(n - i) + 1; //random integer from 1 to n-i
            for (int j = 0; j < i; j++) {      //Make sure that its unique
                if (x >= s[j])
                    x++;
            }
            s[i] = x;
        }
        Arrays.sort(s);
        return s;
    }


    /**
     * Checks to see that, for site m, the taxa in q are not missing, gaps, etc.
     *
     * @param q     array of taxa ids
     * @param m     site
     * @return true iff all not missing, not gaps, and site not masked
     */
    private static boolean goodSite(Characters block, int[] q, int m) {
        char ch;
        if (block.isMasked(m))
            return false;

        for (int aQ : q) {
            ch = block.get(aQ, m);
            if (ch == block.getFormat().getMissing())
                return false;
            if (ch == block.getFormat().getGap())
                return false;
        }
        return true;
    }

    /**
     * Computes v statistic (Steel etal) for the quartet q
     *
     * @return v score
     */

    private static double vscore(int[] q, Characters block) {

        int nsites = block.getNchar();
        int ngood = 0; //Number of sites without gaps in all four

        int f_ij_kl, f_ik_jl, f_il_jk;
        int f_ij, f_ik, f_il, f_jk, f_jl, f_kl;
        f_ij_kl = f_ik_jl = f_il_jk = 0;
        f_ij = f_ik = f_il = f_jk = f_jl = f_kl = 0;

        int nconst = 0;

        char[] s = new char[4];

        for (int m = 1; m <= nsites; m++) {
            if (!goodSite(block, q, m))
                continue;
            ngood++;

            for (int a = 0; a < 4; a++)
                s[a] = block.get(q[a], m);


            if (s[0] != s[1])
                f_ij++;
            if (s[0] != s[2])
                f_ik++;
            if (s[0] != s[3])
                f_il++;
            if (s[1] != s[2])
                f_jk++;
            if (s[1] != s[3])
                f_jl++;
            if (s[2] != s[3])
                f_kl++;
            if ((s[0] != s[1]) && (s[2] != s[3]))
                f_ij_kl++;
            if ((s[0] != s[2]) && (s[1] != s[3]))
                f_ik_jl++;
            if ((s[0] != s[3]) && (s[1] != s[2]))
                f_il_jk++;
            if (s[0] == s[1] && s[0] == s[2] && s[0] == s[3])
                nconst++;
        }

        if (ngood == 0)
            return 100.0;   //Returns an impossible amount - says choose another.

        double v = 1.0 - (double) nconst / ngood;
        if (f_ij_kl > 0)
            v = Math.max(v, (double) f_ij * f_kl / f_ij_kl / ngood);
        if (f_ik_jl > 0)
            v = Math.max(v, (double) f_ik * f_jl / f_ik_jl / ngood);
        if (f_il_jk > 0)
            v = Math.max(v, (double) f_il * f_jk / f_il_jk / ngood);

        v = Math.min(v, 1.0);
        //System.err.println(q+"\tv = "+ v);
        return v;
    }


    /**
     * Computes the proportion of Invariance sites using Steel et al.'s method
     *
     * @return proportion assumed invariant
     */
    public double estimatePinv(Characters chars) {
        int n = chars.getNtax();
        int maxsample = (n * (n - 1) * (n - 2) * (n - 3)) / 24;

        double vsum;
        int count;

        if (n > optionTaxaCutoff) {
            //Sampling          - we do a minimum of 1000, and stop once |sd| is less than 0.05 |mean|

            Random random = new Random();
            int[] q = new int[4];
            double sum2 = 0.0;
            count = 0;
            vsum = 0.0;
            boolean done = false;
            int iter = 0;

            while (!done) {
                iter++;
                q = randomSubset(4, n, random);
                double v = vscore(q, chars);
                if (v > 1.0)
                    continue; //Invalid quartet.
                vsum += v;
                sum2 += v * v;
                count++;
                if (count > 1000) {
                    //Evaluate how good the stdev is.
                    double mean = vsum / count;
                    double var = sum2 / count - mean * mean;
                    double sd = Math.sqrt(var);
                    if (Math.abs(sd / mean) < 0.05)
                        done = true;
                    // System.err.println("Mean = " + mean + " sd = " + sd);
                }
                if (iter > maxsample) {
                    done = true; //Safety check to prevent infinite loop
                }
            }
        } else {
            //Exact count
            vsum = 0.0;
            count = 0;

            for (int i = 1; i <= n; i++) {
                for (int j = i + 1; j <= n; j++) {
                    for (int k = j + 1; k <= n; k++) {
                        for (int l = k + 1; l <= n; l++) {
                            int[] q = new int[4];
                            q[0] = i;
                            q[1] = j;
                            q[2] = k;
                            q[3] = l;
                            vsum += vscore(q, chars);
                            count++;
                        }
                    }
                }
            }
        }

        return vsum / count;

    }


    /**
     * Runs the analysis
     *
	 */
    public String apply(Document doc) {//, Taxa taxa, Characters block) throws Exception {

        double pinv = estimatePinv(doc.getCharacters());

        return "Estimated proportion of invariant sites= " + (float) pinv;
    }

    public int getOptionTaxaCutoff() {
        return optionTaxaCutoff;
    }

    public void setOptionTaxaCutoff(int optionTaxaCutoff) {
        this.optionTaxaCutoff = optionTaxaCutoff;
    }

}
