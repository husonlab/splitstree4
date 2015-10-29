/**
 * OrderReticulations.java
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
package splitstree4.algorithms.splits.reticulateTree;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloGraph;
import splitstree4.algorithms.trees.TreeSelector;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 15.01.2006
 * Time: 00:15:50
 * To change this template use File | Settings | File Templates.
 */
public class OrderReticulations {


    /**
     * Everything for generating an ordering from a set of (prtial) trees
     */
    public static HashMap generateOrderingChars(ReticulationTree ret, TaxaSet[] induced2origTaxa) {
        /*  System.err.println("OrderReticulations: start ordering");
          Iterator it11 = ret.getTreeSplit2Reticulations().keySet().iterator();
          while (it11.hasNext()) {
              Object key = it11.next();
              System.out.println("key: " + key + "\tvalue: " + ret.getTreeSplit2Reticulations().get(key));
          }
          Taxa inducedTaxa = ret.getInducedTaxa();
          Splits inducedSplits = ret.getInducedSplits();
          Document indDoc = new Document();
          indDoc.setTaxa(inducedTaxa);
          int[] induced2treeTaxa = new int[induced2origTaxa.length];
          for (int i = 1; i < ret.getTreeTaxa2inducedTaxa().length; i++) induced2treeTaxa[ret.getTreeTaxa2inducedTaxa()[i]] = i;


          StringWriter tsw = new StringWriter();
          inducedTaxa.write(tsw);
          inducedSplits.write(tsw, inducedTaxa);
          System.out.println("inducedTaxa and inducedSplits of ret: \n" + tsw);
          int[] treeTaxa2inducedTaxa = ret.getTreeTaxa2inducedTaxa();
          // replace old hashMap with a new HashMap that contains the sortedRtaxa
          HashMap treeSplit2Reticulations = ret.getTreeSplit2Reticulations();
          ret.setTreeSplit2Reticulations(new HashMap());
          Iterator it = treeSplit2Reticulations.keySet().iterator();
          TaxaSet rTaxa = ret.getReticulationTaxa();
          System.out.println("rTaxa: " + rTaxa);
          // for each split in the backbonetree with a reticulation connecting to it
          while (it.hasNext()) {
              TaxaSet treeSplit = (TaxaSet) it.next();
              System.out.println("treeSplit: " + treeSplit + "\tcTaxa: " + treeSplit2Reticulations.get(treeSplit));
              TaxaSet inducedSplit = new TaxaSet();
              for (int i = treeSplit.getBits().nextSetBit(1); i != -1; i = treeSplit.getBits().nextSetBit(i + 1)) inducedSplit.set(treeTaxa2inducedTaxa[i]);
              TaxaSet unsortedcTaxa = (TaxaSet) ((LinkedList) treeSplit2Reticulations.get(treeSplit)).getFirst();
              TaxaSet cTaxa = (TaxaSet) unsortedcTaxa.clone();
              TaxaSet rTaxaWithoutCTaxa = (TaxaSet) rTaxa.clone();
              rTaxaWithoutCTaxa.andNot(cTaxa);
              if (((LinkedList) treeSplit2Reticulations.get(treeSplit)).size() > 1) System.err.println("OrderReticulaitons: generateOrderingChars: conecting taxa for tree split " + treeSplit + " seem to be sorted?" + treeSplit2Reticulations.get(treeSplit));
              // all rTaxa that do not connect to the edge will be moved to one side of the split so they do not make any trouble and
              // we dont need to map induced Taxa to a set of Taxa conatining only treeTaxa and connecting Taxa
              Splits usedInducedSplits = new Splits(inducedTaxa.getNtax());
              HashSet seenUsedInducedSplits = new HashSet();
              for (int i = 1; i <= inducedSplits.getNsplits(); i++) {
                  TaxaSet tmpSource = (TaxaSet) inducedSplits.get(i).clone();
                  TaxaSet tmpTarget = tmpSource.getComplement(inducedTaxa.getNtax());
                  tmpSource.andNot(rTaxa);
                  tmpTarget.andNot(rTaxa);

                  if (tmpSource.equalsAsSplit(inducedSplit, inducedTaxa.getNtax()) || tmpTarget.equalsAsSplit(inducedSplit, inducedTaxa.getNtax())) {
                      TaxaSet toAdd = (TaxaSet) inducedSplits.get(i).clone();
                      toAdd.andNot(rTaxaWithoutCTaxa);
                      if (!seenUsedInducedSplits.contains(toAdd)) {
                          usedInducedSplits.add(toAdd);
                          seenUsedInducedSplits.add(toAdd);
                      }
                  }
              }
              StringWriter sw = new StringWriter();
              usedInducedSplits.write(sw, inducedTaxa);
              indDoc.setSplits(usedInducedSplits);
              SplitsUtilities.computeCycle(indDoc, inducedTaxa, usedInducedSplits, -1);
              System.out.println("split of edge: " + inducedSplit + "|" + inducedSplit.getComplement(inducedTaxa.getNtax()) + "\n" + sw);
              ConvexHull ch = new ConvexHull();
              Network net = ch.apply(indDoc, inducedTaxa, usedInducedSplits);
              PhyloGraph graph = net.graphView.getPhyloGraph();
              System.out.println(graph.toString());
              // define start and stop node of path
              int startTaxa = cTaxa.getBits().nextClearBit(1);
              int stopTaxa = -1;
              if (inducedSplit.get(startTaxa)) {
                  TaxaSet tmp = inducedSplit.getComplement(inducedTaxa.getNtax());
                  tmp.andNot(rTaxa);  // cTaxa is a subset of rTaxa...
                  System.out.println("tmp: " + tmp + "\tstart: " + startTaxa);
                  stopTaxa = tmp.getBits().nextSetBit(1);
              } else
                  stopTaxa = inducedSplit.getBits().nextSetBit(1);
              System.out.println("startTaxa: " + startTaxa + "\tstopTaxa: " + stopTaxa);
              Node startNode = graph.getTaxon2Node(startTaxa);
              Node stopNode = graph.getTaxon2Node(stopTaxa);
              // find all pathes between startNode and stopNode..
              LinkedList orderings = new LinkedList();
              LinkedList partialPath = new LinkedList();
              //  special treatment for a rTaxa that is element of start
              Iterator itT = graph.getNode2Taxa(startNode).iterator();
              TaxaSet startcTaxa = new TaxaSet();
              TaxaSet startSplit = new TaxaSet();
              System.out.println("startNode taxa: " + graph.getNode2Taxa(startNode));
              boolean add = false;
              while (itT.hasNext()) {
                  int taxa = ((Integer) itT.next()).intValue();
                  if (cTaxa.get(taxa)) {
                      startcTaxa.set(taxa);
                      add = true;
                  } else
                      startSplit.set(induced2treeTaxa[taxa]);
              }
              if (add) {
                  System.out.println("found start cTaxa : " + startcTaxa);
                  partialPath.add(startcTaxa);
              }
              recFindPathes(startNode, stopNode, graph, new BitSet(), partialPath, orderings, cTaxa);
              System.out.println("orderings: " + orderings + "\tunsortedcTaxa: " + unsortedcTaxa);
              // map orderings to orgTaxa
              /* LinkedList orgOrderings = new LinkedList();
               Iterator it2 = orderings.iterator();
               while (it2.hasNext()){
                   LinkedList path = (LinkedList)it2.next();
                   LinkedList orgPath = new LinkedList();
                   Iterator it3 = path.iterator();
                   while(it3.hasNext()){
                       TaxaSet ord = (TaxaSet)it3.next();
                       TaxaSet orgOrd = new TaxaSet();
                       for (int i=ord.getBits().nextSetBit(1);i!=-1;i=ord.getBits().nextSetBit(i+1))orgOrd.or(induced2origTaxa[i]);
                       orgPath.add(orgOrd);
                   }
                   System.out.println("replacing path: "+path+"\t with org Path: "+orgPath);
                   orgOrderings.add(orgPath);

               }  // *
              LinkedList sortedRTaxa = buildOrderingGraph(orderings, induced2origTaxa, unsortedcTaxa);
              ret.getTreeSplit2Reticulations().put(startSplit, sortedRTaxa);

          }
          */
        return null;
    }

    private static void recFindPathes(Node startNode, Node stopNode, PhyloGraph graph, BitSet usedSplits, LinkedList partialPath, LinkedList orderings, TaxaSet cTaxa) {
        Iterator adjIt = startNode.getAdjacentEdges();
        while (adjIt.hasNext()) {
            Edge e = (Edge) adjIt.next();
            if (!usedSplits.get(graph.getSplit(e))) {
                Node nextNode = e.getOpposite(startNode);
                if (stopNode.equals(nextNode)) {
                    //  special treatment for a rTaxa that is element of stop
                    Iterator itT = graph.getNode2Taxa(stopNode).iterator();
                    TaxaSet stopcTaxa = new TaxaSet();
                    System.out.println("stopNode taxa: " + graph.getNode2Taxa(stopNode));
                    boolean add = false;
                    while (itT.hasNext()) {
                        int taxa = (Integer) itT.next();
                        if (cTaxa.get(taxa)) {
                            stopcTaxa.set(taxa);
                            add = true;
                        }
                    }
                    if (add) {
                        System.out.println("found stop cTaxa : " + stopcTaxa);
                        partialPath.addLast(stopcTaxa);
                    }
                    orderings.add(partialPath.clone());
                    System.out.println("adding path: " + partialPath);
                } else {
                    // add split and taxonlabel to path
                    usedSplits.set(graph.getSplit(e));
                    List taxonList = graph.getNode2Taxa(nextNode);
                    System.out.println("going along: " + startNode + "\t" + nextNode + "\t" + taxonList);
                    if (taxonList.size() > 0) {
                        TaxaSet labelNextNode = new TaxaSet();
                        for (Object aTaxonList : taxonList) {
                            labelNextNode.set((Integer) aTaxonList);
                        }
                        partialPath.addLast(labelNextNode);
                        System.out.println("label nextnode: " + labelNextNode);
                    }
                    // recurse the rest of the path
                    recFindPathes(nextNode, stopNode, graph, usedSplits, partialPath, orderings, cTaxa);
                    // remove added split and taxonlabel from path
                    if (taxonList.size() > 0) partialPath.removeLast();
                    usedSplits.clear(graph.getSplit(e));

                }
            }
        }
    }


    private static LinkedList buildOrderingGraph(LinkedList orderings, TaxaSet[] induced2origTaxa, TaxaSet inducedcTaxa) throws Exception {
        // init the ordering graph
        // each node has an NodeSet as info, containing those nodes wich are less than it
        PhyloGraph ordGraph = new PhyloGraph();
        Node start = ordGraph.newNode();
        ordGraph.setLabel(start, "start");
        Node stop = ordGraph.newNode();
        ordGraph.setLabel(stop, "stop");
        // the original connecting taxa mapped onto the ordering node
        HashMap orgCTaxa2ordNode = new HashMap();
        // and the  ordering node mapped ontp the induced connecting taxa
        HashMap ordNode2cTaxa = new HashMap();
        for (int i = inducedcTaxa.getBits().nextSetBit(1); i != -1; i = inducedcTaxa.getBits().nextSetBit(i + 1)) {
            System.out.println("induceed cTaxa: " + i + "\torgTaxa: " + induced2origTaxa[i]);
            // add new node to graph
            Node n = ordGraph.newNode();
            // add trvial edges to graph
            NodeSet toAdd = new NodeSet(ordGraph);
            toAdd.add(start);
            ordGraph.newEdge(start, n, toAdd.clone());
            toAdd.add(n);
            ordGraph.newEdge(n, stop, toAdd);
            orgCTaxa2ordNode.put(induced2origTaxa[i], n);
            // this is for cycles
            BitSet tmp = new BitSet();
            tmp.set(i);
            ordNode2cTaxa.put(n, tmp);
        }
        System.out.println("init ordering Graph:");
        Iterator tIt2 = ordGraph.nodeIterator();
        while (tIt2.hasNext()) System.out.println("Node: " + tIt2.next());
        System.out.println();
        tIt2 = ordGraph.edgeIterator();
        while (tIt2.hasNext()) {
            Edge e = (Edge) tIt2.next();
            System.out.println("Edge: " + e + "\tsource: " + e.getSource() + "\ttarget: " + e.getTarget());
        }
        // adding orderings ot ordering graph
        for (Object ordering : orderings) {
            LinkedList data = (LinkedList) ordering;
            System.out.println("ordering: " + data);
            Iterator itD = data.iterator();
            if (itD.hasNext()) {
                TaxaSet nSet = new TaxaSet();
                HashSet nSetcTaxa = new HashSet();
                nSetcTaxa.add(start);
                TaxaSet mSet;

                // nodes visited allready by the ordering of the tree
                NodeSet seenNodes = new NodeSet(ordGraph);
                while (itD.hasNext()) {
                    TaxaSet tmp = (TaxaSet) ((TaxaSet) itD.next()).clone();
                    // map mset to originals
                    mSet = new TaxaSet();
                    for (int i = tmp.getBits().nextSetBit(1); i != -1; i = tmp.getBits().nextSetBit(i + 1))
                        mSet.or(induced2origTaxa[i]);
                    System.out.println("nSet: " + nSet + "\tmSet: " + mSet + "\t mSet induced: " + tmp);
                    nSet.andNot(mSet);
                    System.out.println("rTaxa: " + mSet + "\t" + orgCTaxa2ordNode);
                    // this is for unresolved trees
                    HashSet mSetcTaxa = new HashSet();
                    for (Object o : orgCTaxa2ordNode.keySet()) {

                        TaxaSet cTaxa = (TaxaSet) o;
                        System.out.println("cTaxa: " + cTaxa);
                        // first for mSet
                        if (mSet.contains(cTaxa)) {
                            Node m = (Node) orgCTaxa2ordNode.get(cTaxa);
                            if (m != null) {
                                System.out.println("Found node for conecting Taxa :" + cTaxa + "\t" + m);
                                mSetcTaxa.add(m);
                            }
                        }
                    }
                    System.out.println("nSet: " + nSetcTaxa + "\tmSet: " + mSetcTaxa);
                    // check if there exists a path from start to m wich contains n
                    for (Object aMSetcTaxa : mSetcTaxa) {
                        Node m = (Node) aMSetcTaxa;
                        // for all cTaxa of n
                        for (Object aNSetcTaxa : nSetcTaxa) {
                            Node n = (Node) aNSetcTaxa;
                            Iterator adjIt = m.getAdjacentEdges();
                            boolean add = true;
                            while (adjIt.hasNext() && add) {
                                Edge e = (Edge) adjIt.next();
                                if ((e.getTarget().equals(m)) && ((NodeSet) e.getInfo()).contains(n)) {
                                    System.out.println("there exists allready a path for " + n + "\t" + m);
                                    add = false;
                                }

                            }
                            System.out.println("checking if I have to add edge between " + n + "\tand: " + m + "\t: " + add);
                            if (add) {
                                //  remove all edges connecting to m and with source in seenNodes
                                adjIt = m.getAdjacentEdges();
                                ArrayList toDelete = new ArrayList();
                                while (adjIt.hasNext()) {
                                    Edge e = (Edge) adjIt.next();
                                    if (e.getTarget().equals(m) && seenNodes.contains(e.getSource())) toDelete.add(e);
                                }
                                System.out.println("deleting edges: " + toDelete);
                                for (Object aToDelete : toDelete) ((Edge) aToDelete).deleteEdge();
                                // add new edge between n and m
                                // set info to those nodes that are in pathes between start and n and the node n.
                                NodeSet info = new NodeSet(ordGraph);
                                adjIt = n.getAdjacentEdges();
                                while (adjIt.hasNext()) {
                                    Edge e = (Edge) adjIt.next();
                                    if (e.getTarget().equals(n))
                                        info.addAll((NodeSet) (e).getInfo());
                                }
                                info.add(n);
                                System.out.println("adding new edge between " + n + "\t" + m + " with info: " + info);
                                Edge e = ordGraph.newEdge(n, m);
                                e.setInfo(info);
                                // update decendants info
                                updateDecendantsInfo(m, info);

                                // remove possible edge to stop
                                e = ordGraph.getCommonEdge(n, stop);
                                if (e != null) e.deleteEdge();
                            }
                        }
                    }
                    // set next Set
                    //System.out.println(m);
                    seenNodes.addAll(nSetcTaxa);
                    nSetcTaxa = mSetcTaxa;
                    nSet = mSet;
                }

                //  remove all edges connecting to stop and with source in seenNodes
                /*Iterator adjIt = stop.getAdjacentEdges();
                ArrayList toDelete = new ArrayList();
                while (adjIt.hasNext()) {
                    Edge e = (Edge) adjIt.next();
                    if (e.getTarget().equals(stop) && seenNodes.contains(e.getSource())) toDelete.add(e);
                }
                Iterator TDit = toDelete.iterator();
                while (TDit.hasNext()) ((Edge) TDit.next()).deleteEdge();
                 */
                System.out.println("partial ordGraph: \n" + ordGraph);

            }
        }
        System.out.println("\n\ncomplete ord Graph: ");
        Iterator tIt = ordGraph.nodeIterator();
        while (tIt.hasNext()) System.out.println("Node: " + tIt.next());
        System.out.println();
        tIt = ordGraph.edgeIterator();
        while (tIt.hasNext()) {
            Edge e = (Edge) tIt.next();
            System.out.println("Edge: " + e + "\tsource: " + e.getSource() + "\ttarget: " + e.getTarget());
        }

        // analyze ordGraph
        return buildOrdering(start, stop, ordGraph, ordNode2cTaxa);
    }


    static private void updateDecendantsInfo(Node start, NodeSet info) {
        Iterator it = start.getAdjacentEdges();
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            // decandant
            if (e.getSource().equals(start)) {
                ((NodeSet) e.getInfo()).addAll(info);
                updateDecendantsInfo(e.getTarget(), info);
            }
        }
    }

    /**
     * Generates a Ordering for the ret Edges from a set of (partial) trees
     *
     * @param taxa
     * @param trees
     * @param ret
     * @param induced2origTaxa
     * @return
     * @throws Exception
     */

    public static HashMap generateOrderingTrees(Document doc, Taxa taxa, Trees trees, ReticulationTree ret, TaxaSet[] induced2origTaxa) {

        // have to update the induced2origTaxa in the form that a split of a prtial tree is mapped to the correct inducedTaxa !!
        TaxaSet hiddenTaxa = taxa.getHiddenTaxa();
        // only generate orgSplits once
        int[][] treeTaxa2orgTaxa = new int[trees.getNtrees() + 1][taxa.getNtax() + 1];
        int[][] orgTaxa2TreeTaxa = new int[trees.getNtrees() + 1][taxa.getNtax() + 1];
        TaxaSet[] orgTaxaInTree = new TaxaSet[trees.getNtrees() + 1];
        TaxaSet[][] induced2TreeTaxa = new TaxaSet[trees.getNtrees() + 1][induced2origTaxa.length];

        for (int i = 1; i <= trees.getNtrees(); i++) {
            System.err.println("# ModifyGraph.generateOrderingTrees:\t Tree number: " + i);
            TaxaSet tmp = trees.getTaxaInTree(taxa, i);
            System.err.println("# ModifyGraph.generateOrderingTrees:\t taxa in tree: " + tmp);
            Taxa treeTaxa = Taxa.getInduced(taxa, tmp.getComplement(taxa.getNtax()));
            orgTaxaInTree[i] = new TaxaSet();
            for (int j = 1; j <= treeTaxa.getNtax(); j++) {
                orgTaxaInTree[i].set(taxa.indexOf(treeTaxa.getLabel(j)));
                treeTaxa2orgTaxa[i][j] = taxa.indexOf(treeTaxa.getLabel(j));
                // /System.out.println("indTaxa2orgTaxa: for Tree: "+i+" taxa: "+j+"is org: "+indTaxa2orgTaxa[i][j]);
            }
            for (int k = 1; k < treeTaxa2orgTaxa[i].length; k++) orgTaxa2TreeTaxa[i][treeTaxa2orgTaxa[i][k]] = k;
            for (int k = 1; k < induced2origTaxa.length; k++) {
                TaxaSet origTaxaSet = induced2origTaxa[k];
                TaxaSet treeTaxaSet = new TaxaSet();
                for (int j = 1; j < treeTaxa2orgTaxa[i].length; j++) {
                    if (origTaxaSet.get(treeTaxa2orgTaxa[i][j])) treeTaxaSet.set(j);
                }
                induced2TreeTaxa[i][k] = treeTaxaSet;
            }
            TreeSelector ts = new TreeSelector();
            ts.setOptionWhich(i);
        }
        taxa.hideTaxa(hiddenTaxa);

        // The reticulation Taxa
        TaxaSet orgRTaxa = new TaxaSet();
        for (int i = 0; i < ret.getReticulates().length; i++) {
            orgRTaxa.or(induced2origTaxa[ret.getReticulates()[i]]);
        }
        /*
       //for every connecting edge
       Iterator it = ret.getTreeSplit2Reticulations().keySet().iterator();
       while (it.hasNext()) {
           // the induced split of the connecting edge
           TaxaSet treeSplit = (TaxaSet) it.next();
           TaxaSet indSplit = new TaxaSet();
           for (int i = treeSplit.getBits().nextSetBit(1); i != -1; i = treeSplit.getBits().nextSetBit(i + 1)) indSplit.set(ret.getTreeTaxa2inducedTaxa()[i]);
           // the original split of the connecting edge
           TaxaSet orgSourceEdge = new TaxaSet();
           for (int i = indSplit.getBits().nextSetBit(1); i != -1; i = indSplit.getBits().nextSetBit(i + 1)) orgSourceEdge.or(induced2origTaxa[i]);
           TaxaSet orgTargetEdge = orgSourceEdge.getComplement(taxa.getNtax());
           orgSourceEdge.andNot(orgRTaxa);
           orgTargetEdge.andNot(orgRTaxa);

           System.out.println("Split: " + orgSourceEdge + "|" + orgTargetEdge);
           // the connecting taxa of the connecting edge
           TaxaSet unsortedcTaxa = (TaxaSet) ((LinkedList) ret.getTreeSplit2Reticulations().get(treeSplit)).iterator().next();
           System.out.println("unsorted connecting Taxa: " + unsortedcTaxa + "\tOrgRTaxa: " + orgRTaxa);

           LinkedList orderings = new LinkedList();
           // for every tree check the edges in the tree that are connecting edges for rTaxa
           // sort those connecting taxa according to the algorithm descriped in the paper
           for (int i = 1; i <= trees.getNtrees(); i++) {
               TreeSet data = new TreeSet(new Comparator() {
                   public int compare(Object o1, Object o2) {
                       TaxaSet one = (TaxaSet) o1;
                       TaxaSet two = (TaxaSet) o2;
                       if (one.cardinality() > two.cardinality())
                           return -1;
                       else if (one.cardinality() < two.cardinality())
                           return 1;
                       else
                           return 0;
                   }
               });
               TaxaSet treeSourceEdge = new TaxaSet();
               TaxaSet treeTargetEdge = new TaxaSet();
               TaxaSet treeRTaxa = new TaxaSet();
               for (int k = 1; k < orgTaxa2TreeTaxa[i].length; k++) {
                   if (orgTaxa2TreeTaxa[i][k] != 0) { // is contained in tree
                       if (orgSourceEdge.get(k)) treeSourceEdge.set(orgTaxa2TreeTaxa[i][k]);
                       if (orgTargetEdge.get(k)) treeTargetEdge.set(orgTaxa2TreeTaxa[i][k]);
                       if (orgRTaxa.get(k)) treeRTaxa.set(orgTaxa2TreeTaxa[i][k]);
                   }
               }
               // add all splits of the path that are inudced splits of the connecting edge
               for (int j = 1; j <= TreeSplits[i].getNsplits(); j++) {
                   TaxaSet treeSplitSource = (TaxaSet) TreeSplits[i].get(j).clone();
                   TaxaSet treeSplitTarget = treeSplitSource.getComplement(taxa.getNtax());
                   TaxaSet source = (TaxaSet) treeSplitSource.clone();
                   TaxaSet target = (TaxaSet) treeSplitTarget.clone();
                   source.andNot(treeRTaxa);
                   target.andNot(treeRTaxa);
                   if (treeSourceEdge.contains(source) && treeTargetEdge.contains(target)) {
                       data.add(treeSplitSource);
                   } else if (treeSourceEdge.contains(target) && treeTargetEdge.contains(source))
                       data.add(treeSplitTarget);
               }
               System.out.println("edgeData: " + data);
               LinkedList ordering = new LinkedList();
               Iterator itD = data.iterator();
               if (itD.hasNext()) {
                   TaxaSet nSet = (TaxaSet) ((TaxaSet) itD.next()).clone();
                   TaxaSet mSet;
                   while (itD.hasNext()) {
                       mSet = (TaxaSet) ((TaxaSet) itD.next()).clone();
                       System.out.println("nSet: " + nSet + "\tmSet: " + mSet);
                       nSet.andNot(mSet);
                       System.out.println("new nSet: " + nSet);
                       TaxaSet inducednSet = new TaxaSet();
                       for (int k = 0; k < induced2TreeTaxa[i].length; k++) {
                           if (induced2TreeTaxa[i][k] != null && nSet.contains(induced2TreeTaxa[i][k])) {
                               System.out.println("found inducedTaxa for : " + induced2TreeTaxa[i][k] + "\t is " + k);
                               inducednSet.set(k);
                           }
                       }
                       ordering.addLast(inducednSet);
                       nSet = mSet;
                   }
               }
               ;
               if (ordering.size() > 0) {
                   System.out.println("ordering: " + ordering);

                   orderings.add(ordering);
               }
           }
// build ordering graph
           System.out.println("orderings: " + orderings + "\tunsortedcTaxa: " + unsortedcTaxa);
           LinkedList sortedRTaxa = buildOrderingGraph(orderings, induced2origTaxa, unsortedcTaxa);
           ret.getTreeSplit2Reticulations().put(treeSplit, sortedRTaxa);

       } */
        return new HashMap();
    }

    /**
     * builds an Ordering for a given Ordering Graph
     *
     * @param start
     * @param stop
     * @param ordGraph
     * @throws Exception
     */

    static private LinkedList buildOrdering(Node start, Node stop, PhyloGraph ordGraph, HashMap ordNode2cTaxa) {
        System.out.println("\nordNode2cTaxa: " + ordNode2cTaxa);
        LinkedList orderedNodes = new LinkedList();
        NodeSet seenNodes = new NodeSet(ordGraph);
        Node n = start;
        while (n != stop) {
            // find decendants
            NodeSet decendants = new NodeSet(ordGraph);
            Iterator it = n.getAdjacentEdges();
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
                if (e.getSource().equals(n)) {
                    decendants.add(e.getTarget());
                }
            }
            // define ancestors of start
            NodeSet ancestors = new NodeSet(ordGraph);
            Iterator adjIt = n.getAdjacentEdges();
            while (adjIt.hasNext()) {
                Edge e = (Edge) adjIt.next();
                if (e.getTarget().equals(n)) {
                    System.out.println("adding for edge from " + e.getSource() + " nodes \t" + e.getInfo());
                    ancestors.addAll((NodeSet) e.getInfo());
                }
            }
            ancestors.add(start);
            System.out.println("start Node: " + n + "\tancestors: " + ancestors + "\tdecendants: " + decendants + "\tseenNodes: " + seenNodes);
            seenNodes.add(n);
            seenNodes.addAll(decendants);
            n = recFindNextDec(decendants, ordGraph, start, ancestors, stop, ordNode2cTaxa, seenNodes, orderedNodes);
        }
        System.out.println("orderedNodes: " + orderedNodes);
        return orderedNodes;
    }

    static private Node recFindNextDec(NodeSet decendants, PhyloGraph ordGraph, Node start, NodeSet startAncestors, Node stop, HashMap ordNode2cTaxa, NodeSet seenNodes, LinkedList orderedcTaxa) {
        // add the taxa of the decendants to the decendantsTaxa
        // for the next recursion step
        NodeSet newDecendants = new NodeSet(ordGraph);
        NodeSet newSeenNodes = (NodeSet) seenNodes.clone();
        // find the decendant
        for (Node n : decendants) {
            System.out.println("decendant: " + n);
            Iterator adjIt = n.getAdjacentEdges();
            NodeSet ancestors = new NodeSet(ordGraph);
            while (adjIt.hasNext()) {
                Edge e = (Edge) adjIt.next();
                System.out.println("edge: " + e + "\tsource: " + e.getSource() + "\t target: " + e.getTarget() + "\t info: " + e.getInfo());
                if (e.getTarget().equals(n)) {
                    // add the ancestors of the path e to the ancestors to n
                    ancestors.addAll((NodeSet) e.getInfo());
                } else if (e.getSource().equals(n)) {
                    // if n is not the decendant add the decendants of n to the list of newDecendants
                    System.out.println("new Decendant: " + e.getTarget());
                    newDecendants.add(e.getTarget());
                }
            }
            //n is a decendant of all ancestors?
            seenNodes.remove(n);
            System.out.println("ancestors: " + ancestors + "\tseenNodes: " + seenNodes);
            if (ancestors.containsAll(seenNodes)) {
                // n is the decendant
                // add the taxa to the ordered List
                // take ancestors of n wich are not ancestors of start
                TaxaSet decendantsTaxa = new TaxaSet();
                //if (n != stop) decendantsTaxa.set(((Integer) ordNode2cTaxa.get(n)).intValue());
                for (Node o : ancestors) {
                    if (o != stop && !startAncestors.contains(o)) {
                        // @todo this are bitSets now so ordNode2cTaxa gives bet a set of Bitsets that contains the Integers ( or cycles)

                        System.out.println(o + "\tid:" + (ordNode2cTaxa.get(o)));
                        decendantsTaxa.getBits().or((BitSet) ordNode2cTaxa.get(o));
                    }
                }
                if (decendantsTaxa.cardinality() != 0)
                    orderedcTaxa.addLast(decendantsTaxa);
                //seenNodes.add(n);
                System.out.println("found: " + decendantsTaxa);
                return n;
            } else { // add n again
                seenNodes.add(n);
            }
        }
        System.out.println("recursion" + newDecendants + "\t seenNodes: " + seenNodes + "t newdecendants: " + newDecendants);        // we have not found a decendant in the decendant list so recurse with newDecendants
        newSeenNodes.addAll(seenNodes);
        return recFindNextDec(newDecendants, ordGraph, start, startAncestors, stop, ordNode2cTaxa, newSeenNodes, orderedcTaxa);
    }

}



