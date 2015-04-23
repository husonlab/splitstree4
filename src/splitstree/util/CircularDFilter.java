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

package splitstree.util;

import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;


/**
 * Given a set of splits, removes splits that are highly incompatible with a given circular order; the idea being that
 * this should reduce the number of boxes.
 *
 * @author huson
 *         Date: 14-May-2004
 */
public class CircularDFilter {
    Document doc = null;

    CircularDFilter(Document doc) {
        this.doc = doc;
    }

    /**
     * destroy all d-dimensional boxes in splits graph
     *
     * @param splits
     * @param maxCrossing if a cycle crosses the split more than this many times it is removed.
     * @return number of splits removed
     */
    static public int applyFilter(Document doc, Splits splits, int maxCrossing) {
        CircularDFilter circularDFilter = new CircularDFilter(doc);

        return circularDFilter.apply(maxCrossing, splits);
    }

    /**
     * Count the number of times the ordering changes sides in the split. Returns this number divided by 2.
     *
     * @param ordering array containing a permutation of 1....ntax.
     * @param s        split
     * @return int number of times ordering crosses taxa.
     */
    private int numCrossings(int[] ordering, TaxaSet s) {
        int ntax = ordering.length - 1;

        int count = 0;
        boolean strand = s.get(ordering[1]);    //Which side of the split we start on.

        for (int i = 2; i <= ntax; i++) {
            if (s.get(ordering[i]) != strand) {
                count++;                          //Different side than before... increment count
                strand = !strand;
            }
        }
        if (s.get(ordering[1]) != strand)         //Check if crossing between 1 and ntax.
            count++;
        return count / 2;
    }


    /**
     * destroy all d-dimensional boxes in splits graph
     *
     * @param maxCrossing maximal dimension d
     * @param splits
     * @return number of splits deleted
     */
    public int apply(int maxCrossing, Splits splits) {

        Splits newSplits = new Splits(splits.getNtax());
        int[] ordering = splits.getCycle();
        if (ordering == null) {
            ordering = SplitsUtilities.computeCycle(splits);
        }
        int removed = 0;

        for (int s = 1; s <= splits.getNsplits(); s++) {
            if (numCrossings(ordering, splits.get(s)) <= maxCrossing) {
                newSplits.add(splits.get(s), splits.getWeight(s), splits.getConfidence(s), splits.getInterval(s), splits.getLabel(s));
            } else {
                removed++;
            }

        }

        return removed;
    }
}
