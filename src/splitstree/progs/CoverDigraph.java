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

/**
 * Cover digraph construction
 * @version $Id: CoverDigraph.java,v 1.3 2006-05-23 05:57:37 huson Exp $
 * @author daniel Huson
 * 7.03
 */
package splitstree.progs;

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
