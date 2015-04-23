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

package splitstree.algorithms.splits.reticulate;

import splitstree.core.TaxaSet;

import java.util.Comparator;

/**
 * a simple reticulation object
 *
 * @author huson
 *         Date: 18-Sep-2004
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
     * @param backbone
     */
    public void setBackbone(int[] backbone) {
        setBackbone(backbone, backbone.length);
    }

    /**
     * set the back bone from the prefix of another backbone
     *
     * @param backbone
     * @param length
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
     * @param reticulates
     */
    public void setReticulates(int[] reticulates) {
        this.reticulates = reticulates;
    }

    /**
     * determins the reticulates as the complement of the backbone
     *
     * @param ntax
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
     * @return
     */
    public int[] getFirstPositionCovered() {
        return firstPositionCovered;
    }

    /**
     * set first position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
     * @param firstPositionCovered
     */
    public void setFirstPositionCovered(int[] firstPositionCovered) {
        this.firstPositionCovered = firstPositionCovered;
    }

    /**
     * get last position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
     * @return
     */
    public int[] getLastPositionCovered() {
        return lastPositionCovered;
    }

    /**
     * set last position (NOT taxon id, but rank 0..backbone.length-1) of backbone taxon
     * covered by hybrid
     *
     * @param lastPositionCovered
     */
    public void setLastPositionCovered(int[] lastPositionCovered) {
        this.lastPositionCovered = lastPositionCovered;
    }

    /**
     * get list of ids of splits in backbone path
     *
     * @return
     */
    public int[] getSplitsPath() {
        return splitsPath;
    }

    /**
     * get  ids of splits in backbone path
     *
     * @param splitsPath
     */
    public void setSplitsPath(int[] splitsPath) {
        setSplitsPath(splitsPath, splitsPath.length);
    }

    /**
     * sets the ids of splits in path as prefix of given array
     *
     * @param pathSplits
     * @param length
     */
    public void setSplitsPath(int[] pathSplits, int length) {
        this.splitsPath = new int[length];
        System.arraycopy(pathSplits, 0, this.splitsPath, 0, length);
    }

    /**
     * compares two objects by decreasing length of backbone
     *
     * @param o1
     * @param o2
     * @return
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
     * @param ret
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
     * @return
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
     * @return
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
