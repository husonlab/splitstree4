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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.ProgressSilent;
import splitstree.algorithms.splits.EqualAngle;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

import java.util.*;

/**
 * METHOD IS NOT USED IN THE MOMENT
 * the method will implement a faster method to search through the possible sets wich can cause a compatible subtree
 * UNDER CONSTRUCTION
 * DESCRIPTION
 *
 * @author huson, kloepper
 *         Date: 18-Sep-2004
 */

public class HybridFinderWithTree {
    // genreal Objects
    private Document doc = null;
    private int maxReticulations = 1;
    private boolean verbose = false;
    // public stuff


    /**
     * @param inducedTaxa   the original taxa set we have to work on
     * @param inducedSplits the original splits set we have to work on
     * @param checkRoot     check if the outroup can be placed
     * @param outgroupId    the id of the outroup taxa, if 0 the outgroup will be ignored
     * @param maxRet        the maximal number of reticulation nodes in a reticulation network
     * @param maxRetToFind  the maximal number of reticulations to search for
     * @return a list of possible solutions
     * @throws Exception
     */

    public LinkedList apply(Taxa inducedTaxa, Splits inducedSplits, int outgroupId, boolean checkRoot, int maxRet, int maxRetToFind) throws Exception {

        this.maxReticulations = maxRet;
        LinkedList re = new LinkedList();
        doc = new Document();
        doc.setProgressListener(new ProgressSilent());
        doc.setTaxa(inducedTaxa);
        doc.setSplits(inducedSplits);

        for (int i = 1; i <= this.maxReticulations; i++) {
            TaxaSet tmpTaxa = (TaxaSet) inducedTaxa.getTaxaSet().clone();
            LinkedList possibleRetNodes = new LinkedList();
            // 1. Generate all i subsets of the Taxa
            RecGenerateRNodes(tmpTaxa, tmpTaxa.getBits().nextSetBit(1), outgroupId, i, new TaxaSet(), possibleRetNodes);
            // 2.check for each element in the list if the reduced splits are compatible
            //            System.out.println("i:"+i+"\t"+possibleRetNodes);
            for (Object possibleRetNode : possibleRetNodes) {
                TaxaSet rTaxa = (TaxaSet) possibleRetNode;
                inducedSplits.hideTaxa(inducedTaxa, rTaxa);
                if (SplitsUtilities.isCompatible(inducedSplits)) {
                    inducedSplits.restoreOriginal(inducedTaxa);
                    if (verbose) System.out.println("\n\ncompatible with rTaxa : " + rTaxa);
                    ReticulationTree tmpRet = new ReticulationTree((Taxa) inducedTaxa.clone(), rTaxa);
                    tmpRet.setInducedSplits(inducedSplits.clone(inducedTaxa));

                    // 3. if the set is compatible check if we have a reticulation scenario
                    //    save reticulation scenario in the list of all possible minimal scenarios
                    if (IsReticulation(inducedTaxa, inducedSplits, rTaxa, tmpRet, outgroupId, checkRoot)) {
                        re.add(tmpRet);
                        if (verbose) System.out.println("found solution");
                    }
                } else {
                    inducedSplits.restoreOriginal(inducedTaxa);
                }
            }
            // If we have found enough solutions return them
            if (re.size() >= maxRetToFind) {
                return re;
            }
        }
        return re;
    }


    private boolean IsReticulation(Taxa inducedTaxa, Splits inducedSplits, TaxaSet rTaxa, ReticulationTree ret, int outgroupId, boolean checkRoot) throws Exception {
        // tree Taxa are those taxa wich are the compatible subset
        Taxa treeTaxa = new Taxa(inducedTaxa.getNtax() - rTaxa.cardinality());
        // and these are the splits wich are the compatible subset
        Splits treeSplits = new Splits(treeTaxa.getNtax());
        // Map for the compatible subset to the complete set
        HashMap treeSplit2inducedSplit = new HashMap();
        int[] treeTaxa2inducedTaxa = GenerateTreeTaxaAndSplits(inducedTaxa, inducedSplits, rTaxa, treeTaxa, treeSplits, treeSplit2inducedSplit);
        if (treeTaxa2inducedTaxa == null) {
            if (verbose) System.out.println("I is not correct");
            return false;
        }
        // set treeTaxa2inducedTaxa in the ret Object
        ret.setTreeTaxa2inducedTaxa(treeTaxa2inducedTaxa.clone());

        // Map from inducedTaxa to treeTaxa
        int[] inducedTaxa2treeTaxa = new int[inducedTaxa.getNtax() + 1];
        for (int i = 1; i < treeTaxa2inducedTaxa.length; i++) inducedTaxa2treeTaxa[treeTaxa2inducedTaxa[i]] = i;

        // building PhyloGraph of tree splits
        SplitsUtilities.computeCycle(doc, treeTaxa, treeSplits, 0);
        EqualAngle ea = new EqualAngle();
        ea.apply(doc, treeTaxa, treeSplits);
        PhyloGraphView graphView = ea.getPhyloGraphView();
        PhyloGraph treeGraph = graphView.getPhyloGraph();
        // for each rTaxa  r check if the graph  consisting of the leafs taxa- rTaxa +r is a simple Gall
        for (int i = 0; i < ret.getReticulates().length; i++) {
            if (!isSimpleGall(treeGraph, treeTaxa, treeSplits, inducedSplits, inducedTaxa2treeTaxa, rTaxa, i, ret)) {
                return false;
            }
        }

        //just connect reticulations to one node
        setReticulations(inducedSplits, treeTaxa, inducedTaxa2treeTaxa, treeSplit2inducedSplit, rTaxa, ret);

        // if checkRoot is true then test else return true
        return !checkRoot || TestForRoot.checkIfRootCanBePlaced(treeGraph, treeTaxa, treeSplits, inducedTaxa2treeTaxa[outgroupId], inducedTaxa, inducedSplits, treeTaxa2inducedTaxa, rTaxa, ret);
    }


    /**
     * sets the getInducedSplit2orderedReticulations() Map of the reticulation tree to a map in wich for each tree-split that has has a set of reticulation
     * one TaxaSet is set (==> so the ModifyGraph class will connect all reticulation nodes of the edge to one point
     *
     * @param inducedSplits
     * @param inducedTaxa2treeTaxa
     * @param treeSplits2inducedSplits
     * @param rTaxa
     * @param ret
     * @throws Exception
     */

    private void setReticulations(Splits inducedSplits, Taxa treeTaxa, int[] inducedTaxa2treeTaxa, HashMap treeSplits2inducedSplits, TaxaSet rTaxa, ReticulationTree ret) {
        HashMap treeSplit2retTaxa = new HashMap();
        for (int i = 0; i < ret.getReticulates().length; i++) {
            TaxaSet startSplit = (TaxaSet) inducedSplits.get(ret.getFirstPositionCovered()[i]).clone();
            // change to tree Taxa
            TaxaSet toAdd = new TaxaSet();
            for (int j = startSplit.getBits().nextSetBit(1); j != -1; j = startSplit.getBits().nextSetBit(j + 1))
                toAdd.set(inducedTaxa2treeTaxa[j]);
            toAdd.unset(0);

            if (!toAdd.get(1)) toAdd = toAdd.getComplement(treeTaxa.getNtax());
            if (!treeSplit2retTaxa.containsKey(toAdd)) treeSplit2retTaxa.put(toAdd, new TaxaSet());
            ((TaxaSet) treeSplit2retTaxa.get(toAdd)).set(ret.getReticulates()[i]);

            TaxaSet stopSplit = (TaxaSet) inducedSplits.get(ret.getLastPositionCovered()[i]).clone();
            // change to tree Taxa
            toAdd = new TaxaSet();
            for (int j = stopSplit.getBits().nextSetBit(1); j != -1; j = stopSplit.getBits().nextSetBit(j + 1))
                toAdd.set(inducedTaxa2treeTaxa[j]);
            toAdd.unset(0);
            if (!toAdd.get(1)) toAdd = toAdd.getComplement(treeTaxa.getNtax());
            if (!treeSplit2retTaxa.containsKey(toAdd)) treeSplit2retTaxa.put(toAdd, new TaxaSet());
            ((TaxaSet) treeSplit2retTaxa.get(toAdd)).set(ret.getReticulates()[i]);
        }


        //System.out.println("treeSplit2retNode: "+treeSplit2retNode);
        //System.out.println("treeSplit2inducedSplit: "+treeSplits2inducedSplits);
        for (Object o : treeSplit2retTaxa.keySet()) {
            TaxaSet treeSplit = (TaxaSet) o;
            // this list is unsorted
            LinkedList toAdd = new LinkedList();
            toAdd.add(treeSplit2retTaxa.get(treeSplit));
            ret.getTreeSplit2Reticulations().put(treeSplit, toAdd);
        }
    }

    /**
     * the method checks if the given reticulation is a a simple gall
     *
     * @param treeGraph            the PhyloTree of the treeSplits
     * @param treeTaxa             the Taxa element of the PhyloTree treeGraph
     * @param treeSplits           the Splits of the PhyloTree treeGraph
     * @param inducedSplits        the complete set of Splits
     * @param inducedTaxa2treeTaxa the map of all Taxa to the tree Taxa
     * @param rTaxa                the set of reticulation Taxa
     * @param reticulation         the acutal reticulation Taxa we are looking at
     * @param ret                  the reticulation Object
     * @return
     * @throws Exception
     */
    private boolean isSimpleGall(PhyloGraph treeGraph, Taxa treeTaxa, Splits treeSplits, Splits inducedSplits, int[] inducedTaxa2treeTaxa, TaxaSet rTaxa, int reticulation, ReticulationTree ret) throws Exception {

        // the map I of the inducedSplits wich are mapped onto the tree (normally there are more than one inducedSplit mapped onto one treeSplit
        HashMap treeSplits2rSplits = new HashMap();
        // the splits contained in the simple gall (in the notation of the inducedSplits)
        HashSet gallSplits = new HashSet();
        // the gallTaxa
        int gallTaxa = ret.getReticulates()[reticulation];
        //System.out.println("working on rTaxa: " + gallTaxa);
        rTaxa.unset(gallTaxa);
        //System.out.println("treeSplits: " + treeSplits + "\ninducedTaxa2treeTaxa:");
        //for (int i = 0; i < inducedTaxa2treeTaxa.length; i++) System.out.println("ind: " + i + "\ttree:" + inducedTaxa2treeTaxa[i]);

        for (int i = 1; i <= inducedSplits.getNsplits(); i++) {
            TaxaSet tmp = (TaxaSet) inducedSplits.get(i).clone();
            tmp.andNot(rTaxa);
            TaxaSet tmp2 = tmp.getComplement(inducedSplits.getNtax());
            tmp2.andNot(rTaxa);
            //System.out.println("inducedSplit (without rTaxa): " + tmp + " | " + tmp2);
            if (!gallSplits.contains(tmp) && !gallSplits.contains(tmp2)) gallSplits.add(tmp);
        }
        //System.out.println("gallSplits (all): " + gallSplits);
        // we are only interested in the incompatible subset of the gallSplits
        RemoveCompatibleSplits(gallSplits, rTaxa, inducedSplits.getNtax());
        //System.out.println("gallSplits (only incompatible): " + gallSplits);

        HashSet treeSplitsList = new HashSet();
        for (int i = 1; i <= treeSplits.getNsplits(); i++) {

            treeSplitsList.add(treeSplits.get(i).clone());
        }

        // define MAP I for the single gall
        for (Object gallSplit1 : gallSplits) {
            TaxaSet gallSplit = (TaxaSet) gallSplit1;
            TaxaSet treeSplit = new TaxaSet();
            // map taxaSet to treeTaxa
            for (int j = gallSplit.getBits().nextSetBit(1); j != -1; j = gallSplit.getBits().nextSetBit(j + 1)) {
                if (j != gallTaxa)
                    treeSplit.set(inducedTaxa2treeTaxa[j]);
            }
            //System.out.println("gallSplit: " + gallSplit + "\ttreeSplit:" + treeSplit);
            // split tree does not uses 0
            treeSplit.unset(0);
            if (!treeSplit.get(1)) treeSplit = treeSplit.getComplement(treeTaxa.getNtax());
            // add treeSplit to map if not contained
            if (treeSplits2rSplits.get(treeSplit) == null) treeSplits2rSplits.put(treeSplit, new HashSet());
            //System.out.println("added: " + gallSplit);
            ((HashSet) treeSplits2rSplits.get(treeSplit)).add(gallSplit);

            // just in case should not happen
            if (!treeSplitsList.contains(treeSplit)) {
                rTaxa.set(gallTaxa);
                System.out.println("unable to map treeSplit to  map: " + treeSplit + "\tmap: " + treeSplits2rSplits);
                return false;
            }
        }

        //System.out.println("treeSplits2rSplits:" + treeSplits2rSplits);
        // check if the map I is a path in the tree
        boolean isPath = isPath(treeGraph, treeSplits, treeTaxa, rTaxa, gallTaxa, reticulation, treeSplits2rSplits, ret);
        rTaxa.set(gallTaxa);
        return isPath;
    }


    /**
     * this method checks if the map I induces a path on the tree treeGraph
     *
     * @param treeGraph          the treeGraph
     * @param treeSplits         the splits in the treeGraph
     * @param rTaxa              the list of all reticulation taxa
     * @param gallTaxa           the gall Taxa
     * @param treeSplits2rSplits the map I
     * @return true if we the map I gives a path in the tree
     * @throws Exception
     */
    private boolean isPath(PhyloGraph treeGraph, Splits treeSplits, Taxa treeTaxa, TaxaSet rTaxa, int gallTaxa, int reticulation, HashMap treeSplits2rSplits, ReticulationTree ret) throws Exception {
        // find a start Point for the search
        Iterator it = treeSplits2rSplits.keySet().iterator();
        TaxaSet start = null;
        int minSplit = Integer.MAX_VALUE;
        while (it.hasNext()) {
            TaxaSet keySplit = (TaxaSet) it.next();
            HashSet map = (HashSet) treeSplits2rSplits.get(keySplit);
            if (map.size() > 2) {
                rTaxa.set(gallTaxa);
                if (verbose) System.out.println("simple gall with more than 2 splits mapped: " + map);
                return false;
            } else if (map.size() > 0 && minSplit > keySplit.cardinality()) {
                start = keySplit;
                minSplit = keySplit.cardinality();
            }
        }

        // no start point found
        if (start == null) {
            //System.out.println("no startpoint found" + treeSplits2rSplits);
            return false;
        }
        /*
        System.out.println("TreeSplits: ");
        for (int i=1;i<=treeSplits.getNsplits();i++)System.out.println(treeSplits.get(i));
        */
        // map start to edge
        Edge startEdge = null;
        for (Edge e = treeGraph.getFirstEdge(); e != null && startEdge == null; e = treeGraph.getNextEdge(e)) {
            if (treeSplits.get(treeGraph.getSplit(e)).equals(start))
                startEdge = e;
        }
        //System.out.println("startSplit: "+start);

        // tmp is a map that is distroyed in the search, if,after the search the map contains a list with to elements than the path is not correct
        // if the map contains lists with 0 elements (no ussage) or 1 Element (trivial Split) the path is ok.
        HashMap tmp = (HashMap) treeSplits2rSplits.clone();
        // System.out.println("pathSplits: " + tmp);
        // starting the recusion for the one side of the startEdge
        HashSet seen = new HashSet();
        seen.add(treeGraph.getTarget(startEdge));
        Edge startPoint = RecCheckPath(treeGraph.getSource(startEdge), seen, treeGraph, treeSplits, tmp);

        // starting the recusion for the other side of the startEdge
        seen = new HashSet();
        seen.add(treeGraph.getSource(startEdge));
        Edge stopPoint = RecCheckPath(treeGraph.getTarget(startEdge), seen, treeGraph, treeSplits, tmp);

        // checking if all split hit in tree are in the path
        if (verbose) System.out.println("path rest: " + tmp);
        // remove startsplit wich is somewhere in the path
        tmp.remove(start);
        // remove endpoints of the path
        HashSet startSet = (HashSet) treeSplits2rSplits.get(treeSplits.get(treeGraph.getSplit(startPoint)));
        HashSet stopSet = (HashSet) treeSplits2rSplits.get(treeSplits.get(treeGraph.getSplit(stopPoint)));
        TaxaSet startSplit = (TaxaSet) startSet.iterator().next();
        TaxaSet stopSplit = (TaxaSet) stopSet.iterator().next();
        if (verbose) System.out.println("startSplit: " + startSplit + "\tstopSplit" + stopSplit);
        Object[] keys = tmp.keySet().toArray();
        for (Object key : keys) {
            HashSet splits = (HashSet) tmp.get(key);
            if (splits.size() == 1) {
                TaxaSet ts = (TaxaSet) splits.iterator().next();
                if (ts.equals(startSplit) || ts.equals(stopSplit)) tmp.values().remove(splits);
            }
        }
        if (verbose) System.out.println(tmp);
        // if I tmp has size 0 the path is correct
        if (tmp.size() == 0) {
            // fill in ret Object;

            for (int i = 1; i <= ret.getInducedSplits().getNsplits(); i++) {
                TaxaSet split = (TaxaSet) ret.getInducedSplits().get(i).clone();
                split.andNot(rTaxa);
                if (startSplit.equalsAsSplit(split, ret.getInducedTaxa().getNtax())) {
                    ret.getFirstPositionCovered()[reticulation] = i;
                } else if (stopSplit.equalsAsSplit(split, ret.getInducedTaxa().getNtax())) {
                    ret.getLastPositionCovered()[reticulation] = i;
                }
            }
            // just to be sure
            if (ret.getFirstPositionCovered()[reticulation] == 0 || ret.getLastPositionCovered()[reticulation] == 0) {
                throw new Exception("Unable to map tree split to reticulation split, giving up");
            }
            return true;
            // the map is not correct
        } else {
            if (verbose) System.out.println("reticulation node: " + reticulation + " is not a path because: " + tmp);
            return false;
        }

    }


    /**
     * check if a set of splits is compatible, given a set of hidden taxa rTaxa
     *
     * @param in    A set of TaxaSets eg. Splits
     * @param rTaxa the taxa wich shall be ignored for the compatibility test
     * @param Ntax  the umber of taxa
     */
    private void RemoveCompatibleSplits(HashSet in, TaxaSet rTaxa, int Ntax) {
        // no concurrent modification exception
        LinkedList toRemove = new LinkedList();
        for (Object anIn : in) {
            TaxaSet split1 = (TaxaSet) anIn;
            boolean compatible = true;
            Iterator it2 = in.iterator();
            while (it2.hasNext() && compatible) {
                TaxaSet split2 = (TaxaSet) it2.next();
                // splits are compatible??
                BitSet A1 = split1.getBits();
                BitSet B1 = split1.getComplement(Ntax).getBits();
                B1.andNot(rTaxa.getBits());
                BitSet A2 = split2.getBits();
                BitSet B2 = split2.getComplement(Ntax).getBits();
                B2.andNot(rTaxa.getBits());
                if (!(!A1.intersects(A2) || !A1.intersects(B2)
                        || !B1.intersects(A2) || !B1.intersects(B2))) {
                    compatible = false;
                }
            }
            if (compatible) {
                toRemove.add(split1);
            }
        }
        for (Object aToRemove : toRemove) {
            in.remove(aToRemove);
        }
    }


    /**
     * the method checks if the map I ( treeSplits2rSplits) ´induces a the path in the treeGraph
     *
     * @param startNode
     * @param seen
     * @param treeGraph
     * @param treeSplits
     * @param treeSplits2rSplits
     * @return edge
     */
    private Edge RecCheckPath(Node startNode, HashSet seen, PhyloGraph treeGraph, Splits treeSplits, HashMap treeSplits2rSplits) {
        seen.add(startNode);
        // iterate for all edges connected to the  source of the startEdge
        Iterator edgeIt = treeGraph.getAdjacentEdges(startNode);
        Node knownNode = null;
        while (edgeIt.hasNext()) {
            Edge nextEdge = (Edge) edgeIt.next();
            TaxaSet nextSplit = treeSplits.get(treeGraph.getSplit(nextEdge));
            if (!seen.contains(treeGraph.getOpposite(startNode, nextEdge))) {
                HashSet map = (HashSet) treeSplits2rSplits.get(nextSplit);
                if ((map != null) && (map.size() > 2))
                    return null;
                else if (map != null) {
                    if (verbose) System.out.println(nextSplit);
                    treeSplits2rSplits.remove(nextSplit);
                    Node nextNode = treeGraph.getOpposite(startNode, nextEdge);
                    return RecCheckPath(nextNode, seen, treeGraph, treeSplits, treeSplits2rSplits);
                }

            } else {
                knownNode = treeGraph.getOpposite(startNode, nextEdge);
            }
        }
        // there is no succesor so we have to return the edge with between the startNode and the node that is allready known
        return treeGraph.getCommonEdge(startNode, knownNode);
    }


    /**
     * The method generates or fills in a couple of Objects. First it fills in the labels for the treeTaxa, second it maps the inducedSplits to the treeSplits
     * (given the map of treeTaxa to inducedTaxa) and third it generates a map from the treeTaxa to the inducedTaxa and returns it. ( runtime is Ntax*NSplits)
     * Here we can also check if the map I is correct, the map is not correct if we have a split, wich is not mapped onto the tree e.g. a split with one empty
     * siude
     *
     * @param inducedTaxa   the complete taxa set (also the rTaxa in it9
     * @param inducedSplits the complete splits set
     * @param rTaxa         the reticulation taxa
     * @param treeTaxa      the taxa of the underlying tree
     * @param treeSplits    the splits of the underlying tree
     * @return a map of the treeTaxa to their inducedTaxa or null iff the map I is not correct
     * @throws Exception
     */
    private int[] GenerateTreeTaxaAndSplits(Taxa inducedTaxa, Splits inducedSplits, TaxaSet rTaxa, Taxa treeTaxa, Splits treeSplits, HashMap treeSplit2inducedSplit) throws Exception {
        int[] re = new int[inducedTaxa.getNtax() - rTaxa.cardinality() + 1];
        TaxaSet[] tmpSplits = new TaxaSet[inducedSplits.getNsplits() + 1];
        for (int i = 1; i < tmpSplits.length; i++) tmpSplits[i] = new TaxaSet();
        int position = 1;
        // for all the taxa in the original set
        for (int i = 1; i <= inducedTaxa.getNtax(); i++) {
            // if not a reticulation taxa
            if (!rTaxa.get(i)) {
                // set the label
                treeTaxa.setLabel(position, inducedTaxa.getLabel(i));
                re[position] = i;
                // set the split
                for (int j = 1; j <= inducedSplits.getNsplits(); j++) {
                    if (inducedSplits.get(j).get(i)) {
                        tmpSplits[j].set(position);
                    }
                }
                position++;
            }
        }
        // add the splits to the treeSplits
        for (int i = 1; i < tmpSplits.length; i++) {
            // check if the Map I is correct
            if (tmpSplits[i].cardinality() == 0 || tmpSplits[i].cardinality() == treeTaxa.getNtax()) {
                if (verbose)
                    System.out.println("inducedSplit is: " + inducedSplits.get(i) + "|" + inducedSplits.get(i).getComplement(inducedTaxa.getNtax()) + "\ttmpSplit: " + tmpSplits[i] + "|" + tmpSplits[i].getComplement(treeTaxa.getNtax()) + "\trTaxa: " + rTaxa);
                return null;
            } else if (!tmpSplits[i].get(1)) tmpSplits[i] = tmpSplits[i].getComplement(treeTaxa.getNtax());
            if (!treeSplit2inducedSplit.containsKey(tmpSplits[i])) {
                treeSplits.add(tmpSplits[i]);
                LinkedList toAdd = new LinkedList();
                toAdd.add(inducedSplits.get(i));
                treeSplit2inducedSplit.put(tmpSplits[i], toAdd);
            } else {
                ((LinkedList) treeSplit2inducedSplit.get(tmpSplits[i])).add(inducedSplits.get(i));
            }
        }
        return re;
    }


    /**
     * OLD STUFF NO LONGER USED
     */

    private Splits GenerateReticulationScenario(HashMap ret) {
        Splits re = new Splits(doc.getTaxa().getNtax());
        Iterator keyPointer = ret.keySet().iterator();
        HashSet tmpSplit = new HashSet();
        while (keyPointer.hasNext()) {
            TaxaSet keySplit = (TaxaSet) keyPointer.next();
            // the split without the reticulation nodes
            TaxaSet clean = (TaxaSet) keySplit.clone();
            LinkedList activeRecNodes = (LinkedList) ret.get(keySplit);
            // generate all possible combinations of the activeRecNodes;
            LinkedList recSets = new LinkedList();
            // first unset all rec Nodes from the  clean set;
            Iterator acPointer = activeRecNodes.iterator();
            while (acPointer.hasNext()) clean.unset((Integer) acPointer.next());
            acPointer = activeRecNodes.iterator();
            while (acPointer.hasNext()) {
                Iterator sPointer = recSets.iterator();
                int recNode = (Integer) acPointer.next();
                LinkedList tmp = new LinkedList();
                TaxaSet toAdd = new TaxaSet();
                toAdd.set(recNode);
                tmp.add(toAdd);
                while (sPointer.hasNext()) {
                    toAdd = ((TaxaSet) sPointer.next());
                    tmp.add(toAdd);
                    toAdd = (TaxaSet) toAdd.clone();
                    toAdd.set(recNode);
                    tmp.add(toAdd);
                }
                recSets = tmp;
            }
            if (verbose) System.out.println("the recursion sets: " + recSets + "\tand the split:" + clean);
            Iterator sPointer = recSets.iterator();
            if (!sPointer.hasNext()) tmpSplit.add(clean.clone());
            while (sPointer.hasNext()) {
                TaxaSet toAdd = (TaxaSet) clean.clone();
                toAdd.or((TaxaSet) sPointer.next());
                tmpSplit.add(toAdd);
                tmpSplit.add(clean.clone());
            }
        }
        for (Object aTmpSplit : tmpSplit) re.add((TaxaSet) aTmpSplit);
        return re;
    }

    private void RecGenerateRNodes(TaxaSet taxa, int position, int outgroupId, int maxRec, TaxaSet usedTaxa, LinkedList allRs) {
        // done with search
        if (maxRec == 0) {
            allRs.add(usedTaxa);
            //System.out.println("usedTaxa: "+usedTaxa);
            // next recursion step
        } else if (position != -1) {
            int newPosition = taxa.getBits().nextSetBit(position + 1);
            //System.out.println("position: "+position+"\tnew position: "+newPosition+"\tusedTaxa: "+usedTaxa);
            // without adding the actual taxon
            RecGenerateRNodes(taxa, newPosition, outgroupId, maxRec, (TaxaSet) usedTaxa.clone(), allRs);
            if (position != outgroupId) {
                // adding the actual taxon
                TaxaSet newUsedTaxa = (TaxaSet) usedTaxa.clone();
                newUsedTaxa.set(position);
                RecGenerateRNodes(taxa, newPosition, outgroupId, maxRec - 1, newUsedTaxa, allRs);
            }
        }
    }

    // Getter and Setter

    public int getOptionMaxReticulations() {
        return maxReticulations;
    }

    public void setOptionMaxReticulations(int maxReticulations) {
        this.maxReticulations = maxReticulations;
    }

}
