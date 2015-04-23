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
 *   PhyloPainter
 *
 *   This is a version of PhyloGram that creates trees with graded coloured edges
 *        representing likelihood changes.
 *
 *   This is de-activated at the moment because we need to figure out a way
 *        of communicating the coloured edge information through the network class.
 *
 *   This class should ALWAYS be de-activated in the release versions.
 *
 *      Bryant and Jette
 */

package splitstree.algorithms.splits;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeIntegerArray;
import jloda.graphview.NodeView;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.NotOwnerException;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @deprecated
 */
public class PhyloPainter  /*implements Splits2Network*/ {

    public final static String DESCRIPTION = "Paints the edges of a Phylogenetic graph. BETA!";
    public final static String CONTACT_NAME = "Miguel Jett�";
    public final static String CONTACT_MAIL = "jette@mcb.mcgill.ca";
    public final static String CONTACT_ADRESS = "http://www-ab.informatik.uni-tuebingen.de/software/jsplits/welcome_en.html";
    private boolean useWeights = true;
    private boolean searchRoot = true;
    private boolean optionCladogram = false;
    private boolean optionPhylogram = true;
    private int[] cyclicOrdering = new int[1];
    private Node root = null;
    private String optionOutgroup = "none";
    private boolean optionUseOutgroup = false;

    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {

        return (taxa.isValid() && splits.isValid() && splits.getProperties().getCompatibility() == 1);
    }

    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        PhyloGraphView graphView = new PhyloGraphView();
        PhyloGraph graph = graphView.getPhyloGraph();
        graphView.setAllowEdit(false);
        double mean = 0.0;

        // ProgressDialog pd = new ProgressDialog("Phylogram...", ""); //Set new progress bar.
        // doc.setProgressListener(pd);
        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(0);                        //set progress to 0

        graph = SplitsUtilities.treeFromSplits(graph, splits, taxa, getOptionWeights());

        doc.notifySetProgress(15);

        setCyclicOrdering(splits.getCycle());
        //shift();

        /*if there is an outgroup present, root is between outgroup and the rest*/
        if (getOptionUseOutgroup()) {
            int outgroupIndex = taxa.indexOf(getOptionOutgroup());
            if (outgroupIndex == -1) {
                System.err.println("Error: No such taxon: " + getOptionOutgroup());
                setOptionUseOutgroup(false);
                setOptionOutgroup("none");
            } else {
                setSearchRoot(false);
                Node v = graph.getTaxon2Node(outgroupIndex);
                Edge e = graph.getFirstAdjacentEdge(v);
                Node w = graph.getOpposite(v, e);
                Node r = graph.newNode();
                Edge e1 = graph.newEdge(v, r);
                Edge e2 = graph.newEdge(w, r);
                graph.setWeight(e1, graph.getWeight(e) * 0.5);
                graph.setWeight(e2, graph.getWeight(e) * 0.5);
                graph.setSplit(e1, graph.getSplit(e));
                graph.setSplit(e2, graph.getSplit(e));
                graph.deleteEdge(e);
                setRoot(r);
                shiftCycle(outgroupIndex);
            }
        }
        /*if there is a node of degree 2 present, it is used as root*/
        if (isSearchRoot()) {
            for (Iterator iter = graph.nodeIterator(); iter.hasNext();) {
                Node element = (Node) iter.next();
                if (graph.getDegree(element) == 2) {
                    setRoot(element);
                    setSearchRoot(false);
                }
            }
        }
        /*find node in the middle between cycle[first] and cycle[last] */
        if (isSearchRoot()) {
            NodeIntegerArray distances = new NodeIntegerArray(graph, 0);
            int dist = 0;

            Node start = graph.getTaxon2Node(getCyclicOrdering()[1]);
            Node target = graph.getTaxon2Node(getCyclicOrdering()[getCyclicOrdering().length - 1]);
            Edge startEd = graph.getFirstAdjacentEdge(start);
            start = graph.getOpposite(start, startEd);

            traverse(target, start, startEd, distances, graph, 1);
        }

        doc.notifySetProgress(18);

        /*mark nodes with intervals of cycle*/
        NodeArray reach = new NodeArray(graph, new int[2]);
        markNodes(reach, root, null, graph);

        doc.notifySetProgress(25);

        graphView.setAllowInternalEdgePoints(true);

        if (getOptionWeights()) mean = SplitsUtilities.getMean(splits);

        //double min = Double.MAX_VALUE;
        /*for (int i = 1; i <= splits.getNsplits(); i++) if(splits.getWeight(i)<min)min=splits.getWeight(i);

        computeCoordinates(min,reach,graphView,root,new Point2D.Double(),null);*/
        computeCoordinates(mean, reach, graphView, root, new Point2D.Double(), null);
        graphView.setShape(root, NodeView.RECT_NODE);
        /*
LikelihoodTreeModel treeModel = new LikelihoodTreeModel(doc,graph);
LikelihoodUtilities.fillDowngroup(treeModel.getL().getDowngroup(),treeModel);
LikelihoodUtilities.fillUpgroup(treeModel.getL().getDowngroup(),treeModel.getP().getUpgroup(),treeModel);
treeModel.getL().computeLikelihood(treeModel);
System.out.println("The likelihood is : " + treeModel.getL().Likelihood);

LikelihoodUtilities.printDowngroup(treeModel.getL().getDowngroup(),treeModel);
LikelihoodUtilities.printUpgroup(treeModel.getP().getUpgroup(),treeModel);
*/
        doc.notifySetProgress(50);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            //graphView.setPEV(e,(TreeModel)treeModel);
            graph.setLabel(e, splits.getLabel(graph.getSplit(e)));
        }

        doc.notifySetProgress(100);

        return new Network(taxa, graphView);

    }

    private void computeCoordinates(double correction, NodeArray reach, PhyloGraphView gv, Node actual, Point2D last, Edge e) throws NotOwnerException {
        PhyloGraph graph = gv.getPhyloGraph();

        double indexLeft = (double) ((int[]) reach.get(actual))[0];
        double indexRight = (double) ((int[]) reach.get(actual))[1];
        double yCoord = (indexLeft + indexRight) / 2.0;
        if (getOptionWeights()) yCoord *= correction;

        if (e != null) {
            //if(getOptionCladogram())gv.setLocation(actual, last.getX()+Math.sqrt(Math.pow(graph.getWeight(e),2)-Math.pow(yCoord-last.getY(),2)), yCoord);
            //else {
            gv.setLocation(actual, last.getX() + graph.getWeight(e), yCoord);

            if (!getOptionCladogram()) {
                ArrayList internalPoints = new ArrayList();
                Point2D internal = new Point2D.Double(last.getX(), gv.getLocation(actual).getY());
                internalPoints.add(internal);
                gv.setInternalPoints(e, internalPoints);
            }

        } else
            gv.setLocation(actual, 0.0, yCoord);


        for (Iterator iter = graph.getAdjacentEdges(actual); iter.hasNext();) {
            Edge element = (Edge) iter.next();
            if (element == e) continue;
            computeCoordinates(correction, reach, gv, graph.getOpposite(actual, element), gv.getLocation(actual), element);
        }

    }

    private int getCyclePos(int tax) {
        for (int i = 0; i < this.getCyclicOrdering().length; i++) {
            if (this.getCyclicOrdering()[i] == tax) return i;
        }
        return -1;
    }

    /**
     * shift the cycle so that first is moved to the first position
     *
     * @param first
     */
    private void shiftCycle(int first) {
        int[] newCycle = getCyclicOrdering().clone();
        int n = getCyclicOrdering().length - 1;
        int pos = getCyclePos(first);
        for (int i = 1; i <= n; i++) {
            int j = i + pos - 1;
            if (j > n)
                j -= n;
            newCycle[i] = getCyclicOrdering()[j];
        }
        setCyclicOrdering(newCycle);
    }

    private int[] markNodes(NodeArray reach, Node v, Edge e, PhyloGraph graph) throws NotOwnerException {
        int[] result = {Integer.MAX_VALUE, 0};
        ArrayList results = new ArrayList();
        for (Iterator iter = graph.getAdjacentEdges(v); iter.hasNext();) {
            Edge f = (Edge) iter.next();
            if (f != e) {
                Node w = graph.getOpposite(v, f);
                if (graph.getDegree(w) == 1) {
                    int x = getCyclePos(graph.getNode2Taxa(w).get(0));
                    results.add(new int[]{x, x});
                    reach.set(w, new int[]{x, x});
                } else
                    results.add(markNodes(reach, w, f, graph));
            }
        }
        for (Object result1 : results) {
            int[] y = (int[]) result1;
            if (y[0] < result[0]) result[0] = y[0];
            if (y[1] > result[1]) result[1] = y[1];
        }
        reach.set(v, result);
        return result;
    }

    private boolean traverse(Node target, Node v, Edge e, NodeIntegerArray distances, PhyloGraph graph, int dist) throws NotOwnerException {
        boolean deadend = true;
        if (graph.getDegree(v) == 1) {
            if (v == target) {
                distances.set(v, dist);
                return false;
            } else
                return deadend;
        }
        distances.set(v, dist);
        dist++;
        for (Iterator iter = graph.getAdjacentEdges(v); iter.hasNext();) {
            Edge f = (Edge) iter.next();
            if (f == e) continue;
            Node u = graph.getOpposite(v, f);
            if (deadend) deadend = traverse(target, u, f, distances, graph, dist);
        }
        if (deadend) distances.set(v, 0);
        if (!deadend && distances.get(v) == distances.get(target) / 2)
            setRoot(v);
        return deadend;
    }


    private boolean insertionNode(Node u, Edge e, int s, Splits splits, PhyloGraph graph) throws NotOwnerException {
        TaxaSet split = splits.get(s);
        if (split.get(1)) split = split.getComplement(splits.getNtax());
        Iterator ed = graph.getAdjacentEdges(u);
        Edge moveAlong = e;
        boolean found = false;
        for (Edge f = (Edge) ed.next(); ed.hasNext();) {
            if (f == e) continue;
            TaxaSet splitf = splits.get(graph.getSplit(f));
            if (splitf.get(1)) splitf = splitf.getComplement(splits.getNtax());
            if (split.intersects(splitf)) {
                if (!found) {
                    found = true;
                    moveAlong = f;
                } else
                    return true;
            }
        }
        return false;
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * use weights in embedding?
     *
     * @return use weights
     */
    public boolean getOptionWeights() {
        return useWeights;
    }

    /**
     * set use weights in embedding?
     *
     * @param weights
     */
    public void setOptionWeights(boolean weights) {
        this.useWeights = weights;
    }

    /**
     * use cladogram representation?
     *
     * @return use cladogram
     */
    public boolean getOptionCladogram() {
        return optionCladogram;
    }

    /**
     * set use cladogram representation?
     *
     * @param cladogram
     */
    public void setOptionCladogram(boolean cladogram) {
        this.optionCladogram = cladogram;
        this.optionPhylogram = false;
    }

    /**
     * use phylogram representation?
     * @return use phylogram
     */
    /*public boolean getOptionPhylogram() {
        return phylogram;
    }*/

    /**
     * set use phylogram representation?
     * @param phylogram
     */
    /*public void setOptionPhylogram (boolean phylogram) {
        this.phylogram = phylogram;
        this.cladogram = false;
    }*/


    /**
     * @return Returns the searchRoot.
     */
    public boolean isSearchRoot() {
        return searchRoot;
    }

    /**
     * @param searchRoot The searchRoot to set.
     */
    public void setSearchRoot(boolean searchRoot) {
        this.searchRoot = searchRoot;
    }

    /**
     * @return Returns the cyclicOrdering.
     */
    public int[] getCyclicOrdering() {
        return cyclicOrdering;
    }

    /**
     * @param cyclicOrdering The cyclicOrdering to set.
     */
    public void setCyclicOrdering(int[] cyclicOrdering) {
        this.cyclicOrdering = cyclicOrdering;
    }

    /*private void shift(){
        int[] oldCycle = getCyclicOrdering();
        int[] newCycle = new int[getCyclicOrdering().length];
        int j=1;
        for (int i = shift+1; i < oldCycle.length; i++) {
            newCycle[j]=oldCycle[i];
            j++;
        }
        for (int i = 1; i <= shift; i++) {
            newCycle[j]=oldCycle[i];
            j++;
        }
        this.setCyclicOrdering(newCycle);
    }*/

    /**
     * @return Returns the root.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * @param root The root to set.
     */
    public void setRoot(Node root) {
        this.root = root;
    }

    public String getOptionOutgroup() {
        return optionOutgroup;
    }

    public void setOptionOutgroup(String optionOutgroup) {
        this.optionOutgroup = optionOutgroup;
    }

    public boolean getOptionUseOutgroup() {
        return optionUseOutgroup;
    }

    public void setOptionUseOutgroup(boolean optionUseOutgroup) {
        this.optionUseOutgroup = optionUseOutgroup;
    }

    public List selectionOptionOutGroup(Document doc) {
        List result = new LinkedList();
        if (doc.getTaxa() != null) {
            for (int t = 1; t <= doc.getTaxa().getNtax(); t++) {
                result.add(doc.getTaxa().getLabel(t));
            }
        }
        return result;
    }
}
