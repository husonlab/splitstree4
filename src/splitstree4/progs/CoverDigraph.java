/*
 * CoverDigraph.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.progs;

import java.util.BitSet;
import java.util.Comparator;

class BitSetComparator implements Comparator {
    /**
     * Compares its two sets for order.   First by size, then lexicographically
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *                            being compared by this Comparator.
     */
    public int compare(Object o1, Object o2) {
        BitSet bs1 = ((GeneOccurrences) o1).taxa;
        BitSet bs2 = ((GeneOccurrences) o2).taxa;

        if (bs1.cardinality() < bs2.cardinality())
            return 1;
        else if (bs1.cardinality() > bs2.cardinality())
            return -1;


        int top = Math.max(bs1.length(), bs2.length()) + 1;

        for (int t = 1; t <= top; t++) {
            if (bs1.get(t) && !bs2.get(t))
                return 1;
            else if (!bs1.get(t) && bs2.get(t))
                return -1;
        }
        return 0;
    }

    /**
     * is bs1 subset of bs2?
     *
     * @param bs1 first bit set
     * @param bs2 second bit set
     * @return true, if bs1 subset of bs2
     */
    public static boolean isSubset(BitSet bs1, BitSet bs2) {
        int top = bs1.length() + 1;

        for (int t = 1; t <= top; t++) {
            if (bs1.get(t) && !bs2.get(t))
                return false;
        }
        return true;
    }
}

/**
 * a gene with occurrences
 */
class GeneOccurrences {
    BitSet taxa;
    String label;
}
