/*
 * Reticulation.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.splits.reticulate;

import splitstree4.core.TaxaSet;

import java.util.Comparator;

/**
 * a simple reticulation object
 *
 * @author huson
 * Date: 18-Sep-2004
 */
public class Reticulation implements Comparator {

    int[] backbone;
    int[] reticulates;
    int[] firstPositionCovered;
    int[] lastPositionCovered;
    int[] splitsPath;

    /**
     * construct a new object
     */
    public Reticulation() {
    }

    /**
     * get the backbone
     *
     * @return backbone
     */
    public int[] getBackbone() {
        return backbone;
    }

    /**
     * set the backbone
     *
	 */
    public void setBackbone(int[] backbone) {
        setBackbone(backbone, backbone.length);
    }

    /**
     * set the back bone from the prefix of another backbone
     *
	 */
    public void setBackbone(int[] backbone, int length) {
        this.backbone = new int[length];
        System.arraycopy(backbone, 0, this.backbone, 0, length);
    }

    /**
     * get the hybrid taxa
     *
     * @return hybrids
     */
    public int[] getReticulates() {
        return reticulates;
    }

    /**
     * sets the hybrid taxa
     *
	 */
    public void setReticulates(int[] reticulates) {
        this.reticulates = reticulates;
    }

    /**
     * determins the reticulates as the complement of the backbone
     *
	 */
    public void determineReticulates(int ntax) {
        TaxaSet hyb = new TaxaSet();
        for (int ib = 0; ib < backbone.length; ib++)
            hyb.set(getBackbone()[ib]);
        hyb = hyb.getComplement(ntax);
        reticulates = new int[hyb.cardinality()];
        int pos = 0;
        for (int t = 1; t <= ntax; t++)
            if (hyb.get(t))
                reticulates[pos++] = t;
    }

    /**
     * get first position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
	 */
    public int[] getFirstPositionCovered() {
        return firstPositionCovered;
    }

    /**
     * set first position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
	 */
    public void setFirstPositionCovered(int[] firstPositionCovered) {
        this.firstPositionCovered = firstPositionCovered;
    }

    /**
     * get last position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
	 */
    public int[] getLastPositionCovered() {
        return lastPositionCovered;
    }

    /**
     * set last position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
	 */
    public void setLastPositionCovered(int[] lastPositionCovered) {
        this.lastPositionCovered = lastPositionCovered;
    }

    /**
     * get list of ids of splits in backbone path
     *
	 */
    public int[] getSplitsPath() {
        return splitsPath;
    }

    /**
     * get  ids of splits in backbone path
     *
	 */
    public void setSplitsPath(int[] splitsPath) {
        setSplitsPath(splitsPath, splitsPath.length);
    }

    /**
     * sets the ids of splits in path as prefix of given array
     *
	 */
    public void setSplitsPath(int[] pathSplits, int length) {
        this.splitsPath = new int[length];
        System.arraycopy(pathSplits, 0, this.splitsPath, 0, length);
    }

    /**
     * compares two objects by decreasing length of backbone
     *
	 */
    public int compare(Object o1, Object o2) {
        int[] backbone1 = ((Reticulation) o1).getBackbone();
        int[] backbone2 = ((Reticulation) o2).getBackbone();
        if (backbone1.length > backbone2.length)
            return -1;
        else if (backbone1.length < backbone2.length)
            return 1;
        for (int i = 0; i < backbone1.length; i++) {

            if (backbone1[i] < backbone2[i])
                return -1;
            else if (backbone1[i] > backbone2[i])
                return 1;
        }
        return 0;
    }

    /**
     * copy the whole case
     *
	 */
    public void copy(Reticulation ret) {
        backbone = copy(ret.backbone);
        reticulates = copy(ret.reticulates);
        firstPositionCovered = copy(ret.firstPositionCovered);
        lastPositionCovered = copy(ret.lastPositionCovered);
        splitsPath = copy(ret.splitsPath);
    }

    private int[] copy(int[] src) {
        int[] tar = new int[src.length];
        System.arraycopy(src, 0, tar, 0, src.length);
        return tar;
    }

    /**
     * returns the reticate taxa with the leftmost and rightmost covered backbone positions
     *
	 */
    public int[] getReticulates3() {
        int[] result = new int[3 * getReticulates().length];
        for (int i = 0, j = 0; i < getReticulates().length; i++, j += 3) {
            result[j] = getReticulates() != null ? getReticulates()[i] : -1;
            result[j + 1] = getFirstPositionCovered() != null ? getFirstPositionCovered()[i] : -1;
            result[j + 2] = getLastPositionCovered() != null ? getLastPositionCovered()[i] : -1;
        }
        return result;
    }

    /**
     * returns string representation of this reticulation
     *
	 */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[Backbone: ");
        if (getBackbone() != null) {
            for (int ib = 0; ib < getBackbone().length; ib++) {
                if (ib > 0)
                    buf.append(",");
                buf.append(getBackbone()[ib]);
            }
        } else
            buf.append("null");
        buf.append(" reticulates: ");
        if (getReticulates() != null) {
            int[] hybrids3 = getReticulates3();
            for (int ih = 0; ih < hybrids3.length; ih++) {
                if (ih > 0)
                    buf.append(",");
                buf.append(hybrids3[ih]);
            }
        } else
            buf.append("null");
        buf.append("]");
        return buf.toString();
    }
}
