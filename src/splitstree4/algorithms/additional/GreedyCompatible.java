/**
 * GreedyCompatible.java
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
 */
package splitstree4.algorithms.additional;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

/**
 * Greedily computes a compatible subset of the given splits
 *
 * @author huson
 *         Date: 01-Mar-2004
 */
public class GreedyCompatible implements Splits2Splits {
    public final static String DESCRIPTION = "Greedily makes splits compatible";

    /**
     * is split post modification applicable?
     *
     * @param doc    the document
     * @param splits the split
     * @return true, if split2splits transform applicable?
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValid(taxa) && doc.isValid(splits);
    }

    /**
     * applies the splits to splits transfomration
     *
     * @param doc
     * @param taxa
     * @param splits
     * @throws jloda.util.CanceledException
     */
    public void apply(Document doc, Taxa taxa,
                      Splits splits) throws CanceledException {

        Splits origSplits = splits.clone(taxa);
        SplitsUtilities.sortByDecreasingWeight(origSplits);
        splits.clear();
        splits.setNtax(origSplits.getNtax());

        doc.notifyTasks("Greedy Compatible", null);
        doc.notifySetMaximumProgress(origSplits.getNsplits() * origSplits.getNsplits() / 2);    //initialize maximum progress
        doc.notifySetProgress(0);                        //set progress to 0

        int count = 0;
        for (int s = 1; s <= origSplits.getNsplits(); s++) {
            boolean ok = true;
            for (int t = 1; ok && t <= splits.getNsplits(); t++) {
                if (!SplitsUtilities.areCompatible(splits.getNtax(), origSplits.get(s),
                        splits.get(t)))
                    ok = false;
                doc.notifySetProgress(++count);
            }
            if (ok) {
                splits.add(origSplits.get(s), origSplits.getWeight(s),
                        splits.getConfidence(s), origSplits.getLabel(s));
            }
        }
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return null;
    }
}
