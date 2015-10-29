/**
 * SparseArray.java
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
package splitstree4.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * SparseArray
 * <p/>
 * Implementation of a sparse 2D array of doubles ints or strings. Uses a hashmap
 * instead of an array.
 *
 * @author bryant
 */
public class SparseArray {

    private final HashMap<IntPair, Object> map;

    /**
     * Create empty array.
     */
    public SparseArray() {
        map = new HashMap<>();
    }

    /**
     * Sets entry (i,j) to the given object. If there is already an entry, it is replaced.
     *
     * @param i row
     * @param j column
     * @param o object to go in the array
     */
    private void set(int i, int j, Object o) {
        map.put(new IntPair(i, j), o);
    }


    /**
     * erase
     * <p/>
     * Delete all entries
     */
    public void clear() {
        map.clear();
    }

    /**
     * Gets the object at position i,j
     *
     * @param i int index
     * @param j int index
     * @return Object
     */
    private Object get(int i, int j) {
        return map.get(new IntPair(i, j));
    }


    /**
     * Gets the double in position (i,j), or zero if there is not entry for that position
     *
     * @param i int index
     * @param j int index
     * @return double entry (i,j), or 0.0 if there is no entry at these coordinates.
     */
    public double getDouble(int i, int j) {
        Object o = get(i, j);
        if (o != null)
            return (Double) o;
        else
            return 0.0;
    }

    /**
     * Gets the string in position (i,j), or null if there is no string for that position.
     *
     * @param i int index
     * @param j int index
     * @return String entry (i,j), or null if there is no entry at these coordinates.
     */
    public String getString(int i, int j) {
        Object o = get(i, j);
        if (o != null)
            return (String) o;
        else
            return null;
    }

    /**
     * Sets the double in a particular position
     *
     * @param i int index
     * @param j int index
     * @param x double value to be placed at entry (i,j)
     */
    public void setDouble(int i, int j, double x) {
        set(i, j, x);
    }

    /**
     * Sets the string in a particular position
     *
     * @param i int index
     * @param j int index
     * @param s string to be placed at entry (i,j)
     */
    public void setString(int i, int j, String s) {
        set(i, j, s);
    }

    /**
     * Clear a single entry
     *
     * @param i int index
     * @param j int index
     */
    public void clear(int i, int j) {
        map.remove(new IntPair(i, j));
    }

    /**
     * Check whether there is an entry in this position
     *
     * @param i int index
     * @param j int index
     * @return boolean - true if there is an entry in this position.
     */
    public boolean hasEntry(int i, int j) {
        return map.containsKey(new IntPair(i, j));
    }

    /**
     * Private class, used to put entries, indexed by pairs, into the HashMap
     */
    protected class IntPair implements Comparable {
        private final int i;
        private final int j;

        /**
         * Create pair
         *
         * @param i row
         * @param j column
         */
        public IntPair(int i, int j) {
            this.i = i;
            this.j = j;
        }

        /**
         * Used in sorting of Hash Maps. Pairs are sorted lexecographically (i first, then j)
         *
         * @param object must be IntPair. Pair to be compared to,
         * @return integer. negative if this is less than object; positive if it is more; zero if equal
         */
        public int compareTo(Object object) {

            IntPair p = (IntPair) object;
            if (this.i < p.i || (this.i == p.i && this.j < p.j))
                return -1;
            else if (this.i == p.i && this.j == p.j)
                return 0;
            else
                return 1;
        }

        /**
         * Returns true if this is equal to object
         *
         * @param object IntPair
         * @return boolean
         */
        public boolean equals(Object object) {
            if (object.getClass() != IntPair.class)
                return false;
            IntPair p = (IntPair) object;
            return (p.i == i && p.j == j);
        }

        /**
         * Highly dubious hashcode.
         *
         * @return integeger hashcode
         */
        public int hashCode() {
            return i * 519037 + j;
        }

    }

    //ToDo: This should implement Iterator, right?

    /**
     * The following class and procedure add the ability to iterate through the array entries
     * without knowing the structure of the array.
     * <p/>
     * The typical loop will be
     * <p/>
     * ArrayIterator p = array.arrayIterator();
     * while(p.hasNext()) {
     * p.getNext();
     * System.out.println("i = "+p.i+" j = "+p.j+" String = "+(String)p.o);
     * }
     */

    public class ArrayIterator {
        public ArrayIterator(Iterator it) {
            this.it = it;
        }

        public int i, j;
        public Object o;
        private final Iterator it;

        public boolean hasNext() {
            return it != null && it.hasNext();
        }

        public void getNext() {
            Map.Entry e = (Map.Entry) it.next();
            IntPair p = (IntPair) e.getKey();
            i = p.i;
            j = p.j;
            o = e.getValue();
        }
    }

    public ArrayIterator arrayIterator() {
        return new ArrayIterator(map.entrySet().iterator());
    }


}
