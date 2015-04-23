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
