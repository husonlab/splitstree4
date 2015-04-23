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

package splitstree.algorithms.splits.reticulateTree;

import splitstree.algorithms.splits.reticulate.Reticulation;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
/**
 * DESCRIPTION
 * @author huson, kloepper
 * Date: 18-Sep-2004
 */

public class ReticulationTree implements Comparator {
    static final boolean EXPERT=true;

    /** a array of the Taxa in the backbones*/
    private int[] backbones;

    private int[] reticulates;
    private int[] firstPositionCovered;
    private int[] lastPositionCovered;

    /** hashset with first index the reticulation second index 1 for firstPositionCoved and 2 for
     * secondPositionCoverd. The values are the splits from the rNode to the first connection to a possible node of
     * the connecting edge of the reticulation*/

    private HashSet[][] reticulation2splits;

    /** the taxa of the reticulation (inducedTaxa) */
    private Taxa inducedTaxa;
    /** the set of splits of the reticulation (induced Splits)*/
    private Splits inducedSplits;

    /** the reticulation taxa (induced notation)*/
    private TaxaSet reticulationTaxa;

    /** a map wich has as key a Split of the backbonetree and as Value a  List of (ordered) reticulations (induced notation),
     */
    private HashMap treeSplit2Reticulations=new HashMap();

    /**
     * a array of the tree taxa to the induced taxa needed for the above HashMap
     */
    private int[] treeTaxa2inducedTaxa;

    /**
     * construct a new object
     */

    public ReticulationTree(Taxa inducedTaxa,TaxaSet reticulationTaxa) {
        this.inducedTaxa = inducedTaxa;
        this.reticulationTaxa=(TaxaSet)reticulationTaxa.clone();
        this.reticulates=new int[reticulationTaxa.cardinality()];
        this.firstPositionCovered = new int[reticulationTaxa.cardinality()];
        this.lastPositionCovered = new int[reticulationTaxa.cardinality()];
        this.reticulation2splits = new HashSet[reticulationTaxa.cardinality()][2];
        this.backbones=new int[inducedTaxa.getNtax()-reticulationTaxa.cardinality()];
        int position =0;
        for (int i=reticulationTaxa.getBits().nextSetBit(1);i!=-1;i=reticulationTaxa.getBits().nextSetBit(i+1)){
            reticulates[position++]=i;
        }
        position=0;
        for (int i=1;i<=inducedTaxa.getNtax();i++){
            if(!reticulationTaxa.get(i)) this.backbones[position++]=i;
        }
    }

    public ReticulationTree(Taxa retTaxa) {
       this.inducedTaxa = retTaxa;
    }

    public ReticulationTree(){

    }

    // public stuff


    /**
     * determins the reticulates as the complement of the backbone
     * @param ntax
     */
    public void determineReticulates(int ntax) {
        TaxaSet hyb = new TaxaSet();
        for (int ib = 0; ib < backbones.length; ib++)
            hyb.set(getBackbones()[ib]);
        hyb = hyb.getComplement(ntax);
        reticulationTaxa = hyb;
        reticulates = new int[hyb.cardinality()];
        int pos = 0;
        for (int t = 1; t <= ntax; t++)
            if (hyb.get(t))
                reticulates[pos++] = t;
    }

    /**
     * compares two objects by decreasing length of backbone
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
     * @param ret
     */
    public void copy(ReticulationTree ret) {
        reticulates = copy(ret.reticulates);
        firstPositionCovered = copy(ret.firstPositionCovered);
        lastPositionCovered = copy(ret.lastPositionCovered);
        backbones = copy(ret.backbones);
        inducedTaxa = (Taxa)ret.getInducedTaxa().clone();
        this.inducedSplits = ret.getInducedSplits().clone(inducedTaxa);
        reticulationTaxa=(TaxaSet)ret.getReticulationTaxa().clone();
    }



    /**
     * returns string representation of this reticulation
     * @return
     */
    public String toString() {
        StringBuilder re = new StringBuilder();
        re.append("reticulates ");
        for (int reticulate : reticulates) re.append(reticulate).append(", ");
        re.append("\nbackbone: ");
        for (int backbone : backbones) re.append(backbone).append(", ");
        re.append("\n");
        for( int i=0;i<reticulates.length;i++)
            re.append("ret: ").append(reticulates[i]).append("\tfirstPos: ").append(firstPositionCovered[i]).append("\t lastPos: ").append(lastPositionCovered[i]).append("\n");

        return re.toString();
    }




    // private stuff

    private int[] copy(int[] src) {
        int[] tar = new int[src.length];
        System.arraycopy(src, 0, tar, 0, src.length);
        return tar;
    }

    // getter and setter
    public Taxa getInducedTaxa() {
        return inducedTaxa;
    }

    public void setInducedTaxa(Taxa retTaxa) {
        this.inducedTaxa = retTaxa;
    }

    public Splits getInducedSplits() {
        return inducedSplits;
    }

    public void setInducedSplits(Splits retSplits) {
        this.inducedSplits = retSplits;
    }


    public int[] getBackbones() {
        return backbones;
    }

    public void setBackbones(int[] backbones) {
        this.backbones = backbones;
    }

    public int[] getReticulates() {
        return reticulates;
    }

    public void setReticulates(int[] reticulates) {
        this.reticulates = reticulates;
        this.reticulationTaxa=new TaxaSet();
        for (int reticulate : reticulates) {
            this.reticulationTaxa.set(reticulate);
        }

    }
    public int[] getFirstPositionCovered() {
        return firstPositionCovered;
    }

    public void setFirstPositionCovered(int[] firstPositionCovered) {
        this.firstPositionCovered = firstPositionCovered;
    }

    public int[] getLastPositionCovered() {
        return lastPositionCovered;
    }

    public void setLastPositionCovered(int[] lastPositionCovered) {
        this.lastPositionCovered = lastPositionCovered;
    }

    public TaxaSet getReticulationTaxa() {
        return reticulationTaxa;
    }

   public HashMap getTreeSplit2Reticulations() {
        return treeSplit2Reticulations;
    }

    public void setTreeSplit2Reticulations(HashMap treeSplit2Reticulations) {
        this.treeSplit2Reticulations = treeSplit2Reticulations;
    }


    public int[] getTreeTaxa2inducedTaxa() {
        return treeTaxa2inducedTaxa;
    }

    public void setTreeTaxa2inducedTaxa(int[] treeTaxa2inducedTaxa) {
        this.treeTaxa2inducedTaxa = treeTaxa2inducedTaxa;
    }

    /**
     *
      * @param reticulation    the number of the reticulation
     * @param positionCovered  1 for first position, 2 for second position
     * @param splits
     */
    public void setReticulation2Splits(int reticulation, int positionCovered, HashSet splits){
        reticulation2splits[reticulation][positionCovered-1]=splits;
    }

    public HashSet getReticulation2Splits(int reticulation, int positionCovered){
      if(reticulation2splits[reticulation][positionCovered-1]==null)reticulation2splits[reticulation][positionCovered-1]= new HashSet();
      return reticulation2splits[reticulation][positionCovered-1];
    }
}
