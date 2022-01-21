/*
 * CircularDFilter.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util;

import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;


/**
 * Given a set of splits, removes splits that are highly incompatible with a given circular order; the idea being that
 * this should reduce the number of boxes.
 *
 * @author huson
 * Date: 14-May-2004
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
