/**
 * TaxaSet.java 
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
/**
 * @version $Id: TaxaSet.java,v 1.23 2006-05-23 05:57:34 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.core;

import java.util.BitSet;

/**
 * A set of taxa
 */
public class TaxaSet implements Cloneable, Comparable {
    private BitSet bits;

    /**
     * Constructor
     */
    public TaxaSet() {
        bits = new BitSet();
    }

    public TaxaSet(BitSet in) {
        bits = in;
    }

    /**
     * Returns the complement of the set
     *
     * @return the set 1..ntax minus this set
     */
    public TaxaSet getComplement(int ntax) {
        TaxaSet result = new TaxaSet();
        for (int i = 1; i <= ntax; i++)
            if (!get(i))
                result.set(i);
        return result;
    }

    /**
     * returns the BitSet
     *
     * @return the BitSet
     */
    public BitSet getBits() {
        return this.bits;
    }

    /**
     * Sets the membership of a taxon
     *
     * @param t the taxon
     */
    public void set(int t) {
        bits.set(t);
    }

    /**
     * Gets the membership of a taxon
     *
     * @param t the taxon
     * @return true, if t is a member of this set
     */
    public boolean get(int t) {
        return bits.get(t);
    }

    /**
     * Unset the membership of a taxon
     *
     * @param t the taxon
     */
    public void unset(int t) {
        bits.set(t, false);
    }

    /**
     * Set a whole interval of elements
     *
     * @param low first element to set
     * @param hi  last element to set
     */
    public void set(int low, int hi) {
        for (int i = low; i <= hi; i++)
            this.set(i);
    }

    /**
     * Adds all elements in a given set
     *
     * @param aset the set of elements to add to the set
     */
    public void set(TaxaSet aset) {
        for (int t = 1; t <= aset.max(); t++) {
            if (aset.get(t))
                this.set(t);
        }
    }

    /**
     * Produces a string representation
     *
     * @return string representation
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int t = 1; t <= max(); t++) {
            if (get(t))
                sb.append(" ").append(t);
        }
        return sb.toString();
    }

    /**
     * Returns the number of elements in the set
     *
     * @return the number of elements
     */
    public int cardinality() {
        return bits.cardinality();
    }


    /**
     * Clears the set
     */
    public void clear() {
        bits = new BitSet();
    }

    /**
     * Returns the maximal element or -1, if empty
     *
     * @return maximalelement or -1, if empty
     */
    public int max() {
        return bits.length() - 1;
    }

    /**
     * And
     *
     * @param ts set to and with
     */
    public void and(TaxaSet ts) {
        bits.and(ts.bits);
    }

    /**
     * Or
     *
     * @param ts set to or with
     */
    public void or(TaxaSet ts) {
        bits.or(ts.bits);
    }

    /**
     * does this set contain the given set ts?
     *
     * @param ts
     * @return true, if this set contains set ts
     */
    public boolean contains(TaxaSet ts) {
        TaxaSet tmp = (TaxaSet) ts.clone();
        tmp.and(this);
        return tmp.cardinality() == ts.cardinality();
    }

    /**
     * Xor
     *
     * @param ts set to or with
     */
    public void xor(TaxaSet ts) {
        bits.xor(ts.bits);
    }


    /**
     * And not
     *
     * @param ts set to add-not with
     */
    public void andNot(TaxaSet ts) {
        bits.andNot(ts.bits);
    }


    /**
     * Clone a taxaset
     *
     * @return a clone
     */
    public Object clone() {
        TaxaSet nt = new TaxaSet();

//		for(int i=1;i<=max();i++)
//			if(get(i))
//				nt.set(i);
        nt.bits = (BitSet) this.bits.clone();
        return nt;
    }

    /**
     * equal?
     *
     * @param taxa
     * @return true, if taxa equals this set
     */
    public boolean equals(Object taxa) {
        return taxa instanceof TaxaSet && getBits().equals(((TaxaSet) taxa).getBits());
    }


    /**
     * find out if two TaxaSets define the same split.
     * (needed this for Circular - ms)
     *
     * @param ts   set to compare with
     * @param ntax number of taxa
     * @return true if both sets define the same split
     */
    public boolean equalsAsSplit(TaxaSet ts, int ntax) {
        for (int i = 1; i <= ntax; i++)
            if (get(i) != ts.get(i)) return false;
        return true;
    }

    /**
     * gets a hash code
     *
     * @return hash code
     */
    public int hashCode() {
        return getBits().hashCode();
    }


    /**
     * returns the smaller of totalSize-cardinality and cardinality
     *
     * @param totalSize
     * @return split size
     */
    public int getSplitSize(int totalSize) {

        return Math.min(totalSize - cardinality(), cardinality());
    }

    /**
     * does this set intersect the given one?
     *
     * @param a2
     * @return true if sets intersect
     */
    public boolean intersects(TaxaSet a2) {
        return getBits().intersects(a2.getBits());
    }

    /**
     * compares two taxa sets
     *
     * @param o1
     * @param o2
     * @return returns -1, 1 or 0
     */
    public static int compare(Object o1, Object o2) {
        BitSet b1 = ((TaxaSet) o1).getBits();
        BitSet b2 = ((TaxaSet) o2).getBits();

        int t1 = b1.nextSetBit(0);
        int t2 = b2.nextSetBit(0);

        while (t1 != -1 || t2 != -1) {
            if (t1 < t2)
                return -1;
            else if (t1 > t2)
                return 1;
            t1 = b1.nextSetBit(t1 + 1);
            t2 = b2.nextSetBit(t2 + 1);
        }
        return 0;
    }

    public int compareTo(Object o) {
        return TaxaSet.compare(this, o);
    }

    /**
     * returns the union of two sets
     *
     * @param b1
     * @param b2
     * @return union
     */
    public static TaxaSet union(TaxaSet b1, TaxaSet b2) {
        TaxaSet result = new TaxaSet();
        result.or(b1);
        result.or(b2);
        return result;
    }

    /**
     * returns the intersection of two sets
     *
     * @param b1
     * @param b2
     * @return intersection
     */
    public static TaxaSet intersection(TaxaSet b1, TaxaSet b2) {
        TaxaSet result = new TaxaSet();
        result.or(b1);
        result.and(b2);
        return result;
    }
}

// EOF
