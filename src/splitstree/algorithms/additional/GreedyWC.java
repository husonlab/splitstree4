/**
 * GreedyWC.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.algorithms.additional;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

/**
 * make splits greedily weakly compatible
 *
 * @author huson
 *         Date: 01-Mar-2004
 */
public class GreedyWC implements Splits2Splits {

    public final static String DESCRIPTION = "Greedily makes splits weakly compatible";

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
     * applies the splits to splits transformation
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

        doc.notifyTasks("Greedy Weakly Compatible", null);
        doc.notifySetMaximumProgress(splits.getNsplits() * splits.getNsplits());    //initialize maximum progress
        doc.notifySetProgress(0);                        //set progress to 0

        int count = 0;
        for (int s = 1; s <= origSplits.getNsplits(); s++) {
            boolean ok = true;
            for (int t = 1; ok && t <= splits.getNsplits(); t++) {
                for (int q = t + 1; ok && q <= splits.getNsplits(); q++) {
                    if (!SplitsUtilities.areWeaklyCompatible(splits.getNtax(),
                            origSplits.get(s), splits.get(t), splits.get(q)))
                        ok = false;
                    doc.notifySetProgress(++count);

                }
            }
            if (ok) {
                splits.add(origSplits.get(s), origSplits.getWeight(s),
                        splits.getConfidence(s));
            }
        }
    }

    /**
     * gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return null;
    }
}
