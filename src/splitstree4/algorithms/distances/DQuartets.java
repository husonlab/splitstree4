/**
 * DQuartets.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * returns all quartets that have positive isolation index
 *
 * @author Daniel Huson and David Bryant
 * @version $Id: DQuartets.java,v 1.11 2007-09-11 12:31:06 kloepper Exp $
 * 8.03
 * <p>
 * returns all quartets that have positive isolation index
 * @author Daniel Huson and David Bryant
 * @version $Id: DQuartets.java,v 1.11 2007-09-11 12:31:06 kloepper Exp $
 * 8.03
 */
/**
 * returns all quartets that have positive isolation index
 * @author Daniel Huson and David Bryant
 * @version $Id: DQuartets.java,v 1.11 2007-09-11 12:31:06 kloepper Exp $
 * 8.03
 */
package splitstree4.algorithms.distances;

import splitstree4.core.Document;
import splitstree4.core.Quartet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Quartets;
import splitstree4.nexus.Taxa;

/**
 * returns all quartets that have positive isolation index
 */
public class DQuartets implements Distances2Quartets {
    private double threshold = 0;
    public final static String DESCRIPTION = "Compute all quartets with positive isolation index";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return taxa != null && distances != null;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param distances
     * @return the computed set of splits
     */
    public Quartets apply(Document doc, Taxa taxa, Distances distances) throws Exception {
        Quartets quartets = new Quartets();
        // ProgressDialog pd = new ProgressDialog("D Quartets...",""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(taxa.getNtax());    //initialize maximum progress
        doc.notifySetProgress(0);
        for (int i = 1; i <= taxa.getNtax(); i++) {
            for (int j = i; j <= taxa.getNtax(); j++) {
                for (int k = 1; k <= taxa.getNtax(); k++)
                    if (k != i && k != j) {
                        for (int m = k; m <= taxa.getNtax(); m++) {
                            double alpha = SplitDecomposition.getIsolationIndex(i, j, k, m, distances);
                            if (alpha > threshold) {
                                quartets.add(new Quartet(i, j, k, m, alpha, null));
                            }
                        }
                    }
            }
            doc.notifySetProgress(i);
        }
        doc.notifySetProgress(taxa.getNtax());   //set progress to 100%	
        // pd.close();								//get rid of the progress listener
        // // doc.setProgressListener(null);
        return quartets;
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
     * get the threshold that isolation index has to exceed
     *
     * @return threshold
     */
    public double getOptionthreshold() {
        return threshold;
    }

    /**
     * set the threshold that the isolation index has to exceed
     *
     * @param threshold
     */
    public void setOptionthreshold(double threshold) {
        this.threshold = threshold;
    }
}
