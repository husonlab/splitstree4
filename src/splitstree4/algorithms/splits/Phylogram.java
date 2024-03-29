/*
 * Phylogram.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree4.algorithms.splits;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.NodeView;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.SplitsUtilities;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * Computes a graph from splits representing a tree..
 */
public class Phylogram implements Splits2Network {

    public final static String DESCRIPTION = "Computes a phylogram or cladogram for a tree";
    private boolean cladogram = false;
    private boolean optionSlanted = false;
    private int[] cyclicOrdering = new int[1];
    private double angle = 60.0;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return (taxa.isValid() && splits.isValid() && splits.getProperties().getCompatibility() == Splits.Properties.COMPATIBLE);
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        PhyloGraphView graphView = new PhyloGraphView();
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        graphView.setAllowEdit(false);
        double mean;

        doc.notifySetMaximumProgress(100);    //initialize maximum progress
        doc.notifySetProgress(0);                        //set progress to 0

        graph = SplitsUtilities.treeFromSplits(graph, splits, taxa, !getOptionCladogram());

        doc.notifySetProgress(15);

        setCyclicOrdering(splits.getCycle());


        Sets sets = doc.getSets();
        if (sets == null) {
            sets = new Sets();
            doc.setSets(sets);
        }
        TaxaSet outgroup = sets.getTaxSet("Outgroup", taxa);

        if (outgroup == null) {
            outgroup = new TaxaSet();
            outgroup.set(1); // will assume taxon is outgroup
            sets.addTaxSet("Outgroup", taxa.getLabels(outgroup));
        }

        Node root;
        {
            int outgroupIndex = outgroup.getBits().nextSetBit(0);
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
			root = r;
			shiftCycle(outgroupIndex);
		}

		/*if there is a node of degree 2 present, it is used as root*/
		for (Node element : graph.nodes()) {
			if (graph.getDegree(element) == 2) {
				root = element;
			}
		}

		doc.notifySetProgress(18);

		/*mark nodes with intervals of cycle*/
		NodeArray<int[]> reach = graph.newNodeArray();
		markNodes(reach, root, null, graph);

		doc.notifySetProgress(25);

		graphView.setAllowInternalEdgePoints(true);

        if (!getOptionCladogram()) {
            mean = SplitsUtilities.getMean(splits);
            computeCoordinates(mean, reach, graphView, root, new Point2D.Double(), null, taxa.getNtax(), true);
        } else
            computeCoordinatesCladogram(reach, graphView, root, null);

        graphView.setShape(root, NodeView.RECT_NODE);

        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            graph.setLabel(e, splits.getLabel(graph.getSplit(e)));
        }

        doc.notifySetProgress(100);

        for (int t = 1; t <= taxa.getNtax(); t++) {
            Node v = graph.getTaxon2Node(t);
            if (graph.getLabel(v) == null)
                graph.setLabel(v, taxa.getLabel(t));
            else
                graph.setLabel(v, graph.getLabel(v) + ", " + taxa.getLabel(t));
        }


        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLabel(v, graph.getLabel(v));
            graphView.setShape(v, NodeView.NONE_NODE);
            /*
        if (G.getLabel(v) != null && G.getLabel(v).equals("") == false)
            setShape(v, NodeView.OVAL_NODE);
        else
            setShape(v, NodeView.NONE_NODE);
            */
            if (v.getDegree() == 1)
                graphView.getNV(v).setLabelLayout(NodeView.EAST);
            else
                graphView.getNV(v).setLabelLayout(NodeView.CENTRAL);
        }
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            graphView.setDirection(e, EdgeView.UNDIRECTED);
        }

        Network network = new Network(taxa, graphView);
        network.setLayout(Network.RECTILINEAR);
        return network;
    }

    private void computeCoordinates(double correction, NodeArray<int[]> reach, PhyloGraphView gv, Node actual, Point2D last, Edge e, int nTax, boolean mt) throws NotOwnerException {
        PhyloSplitsGraph graph = gv.getPhyloGraph();
        double indexLeft = reach.get(actual)[0];
        double indexRight = reach.get(actual)[1];
        double yCoord = (indexLeft + indexRight) / 2.0;
        yCoord *= correction;
        if (/*!getOptionSlanted()*/ true) {

            if (e != null) {

                gv.setLocation(actual, last.getX() + graph.getWeight(e), yCoord);
                ArrayList<Point2D> internalPoints = new ArrayList<>();
                Point2D internal = new Point2D.Double(last.getX(), gv.getLocation(actual).getY());
                internalPoints.add(internal);
                gv.setInternalPoints(e, internalPoints);
            } else
                gv.setLocation(actual, 0.0, yCoord);

        } else {
            if (e != null) {

				Node lastNode = gv.getGraph().getOpposite(actual, e);
				double lastIndexLeft = reach.get(lastNode)[0];
				double lastIndexRight = reach.get(lastNode)[1];

				boolean m = (indexRight - indexLeft) > ((lastIndexRight - lastIndexLeft) / 2.0);
				m = (m == mt);
				if ((indexRight - indexLeft) == ((lastIndexRight - lastIndexLeft) / 2.0)) {
					System.err.println("EQUAL");
					mt = (!mt);
				}

				Point2D.Double actualCoords = new Point2D.Double();
				double ang = Math.toRadians((120.0 / (nTax * 2)) * ((1 + lastIndexRight - lastIndexLeft)));
				AffineTransform trans;
				if (!m)
					trans = AffineTransform.getTranslateInstance(graph.getWeight(e) * Math.cos(ang), -graph.getWeight(e) * Math.sin(ang));
                else
                    trans = AffineTransform.getTranslateInstance(graph.getWeight(e) * Math.cos(ang), graph.getWeight(e) * Math.sin(ang));
                trans.transform(last, actualCoords);
                gv.setLocation(actual, actualCoords);
            } else
                gv.setLocation(actual, 0.0, yCoord);
        }

        for (Edge element : actual.adjacentEdges()) {
            if (element == e) continue;
            computeCoordinates(correction, reach, gv, graph.getOpposite(actual, element), gv.getLocation(actual), element, nTax, !mt);
        }

    }

    private void computeCoordinatesCladogram(NodeArray<int[]> reach, PhyloGraphView gv, Node actual, Edge e) throws NotOwnerException {
        PhyloSplitsGraph graph = gv.getPhyloGraph();

        double indexLeft = reach.get(actual)[0];
        double indexRight = reach.get(actual)[1];
        double yCoord = (indexLeft + indexRight) / 2.0;
        double xCoord = 0.0;
        if (gv.getGraph().getDegree(actual) == 1)
            gv.setLocation(actual, xCoord, yCoord);
        else {
            for (Edge element : actual.adjacentEdges()) {
                if (element == e) continue;
                Node child = graph.getOpposite(actual, element);
                if (gv.getLocation(child).getY() == 0.0) computeCoordinatesCladogram(reach, gv, child, element);
                if (xCoord == 0.0 && getOptionSlanted())
                    xCoord = gv.getLocation(child).getX() - (Math.abs(gv.getLocation(child).getY() - yCoord)) / (Math.tan(Math.toRadians(getOptionAngle() / 2.0)));
                if (!getOptionSlanted()) {
                    if (xCoord > gv.getLocation(child).getX() - 1.0) xCoord = gv.getLocation(child).getX() - 1.0;
                }
            }
        }
        gv.setLocation(actual, xCoord, yCoord);
        if (!getOptionSlanted()) {
            for (Edge element : actual.adjacentEdges()) {
                if (element == e) continue;
                Node child = graph.getOpposite(actual, element);
                ArrayList<Point2D> internalPoints = new ArrayList<>();
                Point2D internal = new Point2D.Double(xCoord, gv.getLocation(child).getY());
                internalPoints.add(internal);
                gv.setInternalPoints(element, internalPoints);
            }

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

    private int[] markNodes(NodeArray<int[]> reach, Node v, Edge e, PhyloSplitsGraph graph) throws NotOwnerException {
        int[] result = {Integer.MAX_VALUE, 0};
        ArrayList<int[]> results = new ArrayList<>();
        for (Edge f : v.adjacentEdges()) {
            if (f != e) {
                Node w = f.getOpposite(v);
                if (w.getDegree() == 1 && graph.getNumberOfTaxa(w) > 0) {
                    int x = getCyclePos(graph.getTaxa(w).iterator().next());
                    results.add(new int[]{x, x});
                    reach.put(w, new int[]{x, x});
                } else
                    results.add(markNodes(reach, w, f, graph));
            }
        }
        for (int[] y : results) {
            if (y[0] < result[0]) result[0] = y[0];
            if (y[1] > result[1]) result[1] = y[1];
        }
        reach.put(v, result);
        return result;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * use weights in embedding?
     *
     * @return use weights
     */
    public boolean getOptionCladogram() {
        return cladogram;
    }

    /**
     * set use weights in embedding?
     *
     */
    public void setOptionCladogram(boolean cladogram) {
        this.cladogram = cladogram;
    }

    /**
     * use cladogram representation?
     *
     * @return use cladogram
     */
    public boolean getOptionSlanted() {
        return optionSlanted;
    }

    /**
     * set use cladogram representation?
     *
     */
    public void setOptionSlanted(boolean cladogram) {
        this.optionSlanted = cladogram;
        //this.optionPhylogram = false;
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

    public double getOptionAngle() {
        return angle;
    }

    public void setOptionAngle(double angle) {
        this.angle = angle;
    }
}
