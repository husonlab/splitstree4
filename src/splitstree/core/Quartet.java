/**
 * Quartet.java 
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
/* $Id: Quartet.java,v 1.7 2006-09-17 08:22:24 huson Exp $*/
package splitstree.core;

//import java.util.*;

//import jloda.util.*;

/**
 * a weighted quartet. which is immutable except for the weight and label attributes.
 * two quartets are considered equal (cf. equals(..)) when they display the same edge.
 * <p/>
 * The quartet's taxa are kept internally in two ways:
 * * in a1, a2, b1, b2 they are kept as they were originally assigned
 * * in sortedA1 etc they are kept in an ordered fashion so that the
 * following conditions are true:
 * <p/>
 * 1)   a1 == min({a1, a2, b1, b2})
 * 2)   a1 < a2
 * 3)   b1 < b2
 * <p/>
 * this is necessary for performance reasons of the hashcode and equals methods which
 * are used frequently when quartets are stored in a container (e.g. set, hashtable)
 */

public class Quartet {
    private int a1, a2, b1, b2;
    private int sortedA1;
    private int sortedA2;
    private int sortedB1;
    private int sortedB2;
    private double weight = 1;
    private String label;
    private int hashCode;
    private Object content;

    /**
     * Constructs a quartet a1 a2 vs b1 b2
     * whose taxa are immutable.
     * weight and label are mutable.
     *
     * @param a1  first left taxon
     * @param a2  second left taxon
     * @param b1  first right taxon
     * @param b2  second right taxon
     * @param wgt weight
     */
    public Quartet(int a1, int a2, int b1, int b2, double wgt, String label) {

        this.a1 = a1;
        this.a2 = a2;
        this.b1 = b1;
        this.b2 = b2;
        int minA = Math.min(a1, a2);
        int minB = Math.min(b1, b2);
        int minAB = Math.min(minA, minB);
        if (minA == minAB) {
            this.sortedA1 = minA;
            this.sortedA2 = Math.max(a1, a2);
            this.sortedB1 = minB;
            this.sortedB2 = Math.max(b1, b2);
        } else {
            this.sortedA1 = minB;
            this.sortedA2 = Math.max(b1, b2);
            this.sortedB1 = minA;
            this.sortedB2 = Math.max(a1, a2);
        }
        this.weight = wgt;
        this.label = label;
        this.hashCode = (int) (((long) this.sortedA1 * 13) + ((long) this.sortedA2 * 41) + ((long) this.sortedB1 * 1447) + ((long) this.sortedB2) % Integer.MAX_VALUE);
    }

    public int hashCode() {
        return this.hashCode;
    }

    public int getA1() {
        return a1;
    }

    public int getA2() {
        return a2;
    }

    public int getB1() {
        return b1;
    }

    public int getB2() {
        return b2;
    }

    public void setA1(int a1) {
        this.a1 = a1;
    }

    public void setA2(int a2) {
        this.a2 = a2;
    }

    public void setB1(int b1) {
        this.b1 = b1;
    }

    public void setB2(int b2) {
        this.b2 = b2;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public int getSortedA1() {
        return sortedA1;
    }

    public int getSortedA2() {
        return sortedA2;
    }

    public int getSortedB1() {
        return sortedB1;
    }

    public int getSortedB2() {
        return sortedB2;
    }

    /**
     * two quartets are equal if they display the same edge
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (!this.getClass().isAssignableFrom(obj.getClass()))
            return false;
        else {
            Quartet q = (Quartet) obj;
            return ((q.getSortedA1() == this.getSortedA1())
                    &&
                    (q.getSortedA2() == this.getSortedA2())
                    &&
                    (q.getSortedB1() == this.getSortedB1())
                    &&
                    (q.getSortedB2() == this.getSortedB2()));
        }
    }


    /**
     * Gets the weight
     *
     * @return weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Sets the weight
     *
     * @param wgt
     */
    public void setWeight(double wgt) {
        this.weight = wgt;
    }

    public String getLabel() {
        return label;
    }

    /**
     * String representation       [A1 A2 : B1 B2 (weight)]
     *
     * @return string representation
     */
    public String toString() {
        return "[" + label + ": " + a1 + " " + a2 + " : " + b1 + " " + b2 + " (" + weight + ")]";
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Are the two taxa x and y
     * 1) taxa of this quartet and
     * 2) on different sides of this quartet's edge
     * ?
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isXYonDifferentSides(int x, int y) {
        return (
                (getA1() == x || getA2() == x) &&
                        (getB1() == y || getB2() == y)
        ) ||
                (
                        (getA1() == y || getA2() == y) &&
                                (getB1() == x || getB2() == x)
                );
    }

    /**
     * Are the two taxa x and y
     * 1) taxa of this quartet and
     * 2) on the same side of this quartet's edge
     * ?
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isXYonSameSides(int x, int y) {
        return (
                (getA1() == x && getA2() == y) ||
                        (getA1() == y && getA2() == x)
        ) ||
                (
                        (getB1() == x && getB2() == y) ||
                                (getB1() == y && getB2() == x)
                );
    }

}

// EOF
