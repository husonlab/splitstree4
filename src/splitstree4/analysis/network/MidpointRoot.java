/*
 * MidpointRoot.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.network;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloGraphView;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 19, 2009
 * Time: 4:05:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class MidpointRoot implements NetworkAnalysisMethod {

    public static final String DESCRIPTION = "Identifies edges on the network containing midpoints on the paths between the most divergent taxa";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     * We require a splits block and a network block
     *
     * @param taxa    the taxa
     * @param network the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Network network) {

        return (doc.getSplits() != null && network != null);  //To change body of implemented methods use File | Settings | File Templates.
    }


    private List findMidpoints(PhyloSplitsGraph g, Node v, double travelled, double midway, BitSet toVisit) {

        List midpointList = new LinkedList();
        for (Edge e : v.adjacentEdges()) {
            int s = g.getSplit(e);
            if (toVisit.get(s)) {
                Node w = e.getOpposite(v);
                if (travelled + g.getWeight(e) > midway) {

                    midpointList.add(e);
                } else {
                    toVisit.set(s, false);
                    List sublist = findMidpoints(g, w, travelled + g.getWeight(e), midway, toVisit);
                    midpointList.addAll(sublist);
                    toVisit.set(s, true);
                }
            }
        }
        return midpointList;
    }


    /**
     * Runs the analysis
     *
     * @param taxa    the taxa
     * @param network the block
     */
    public String apply(Document doc, Taxa taxa, Network network) {

		if (doc != null)
			doc.notifySubtask("Finding most divergent taxa");

        Splits splits = doc.getSplits();

        int ntax = splits.getNtax();
        int max_i = -1, max_j = -1;
        double maxdist = 0.0;

        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double dij = 0.0;
                for (int s = 1; s <= splits.getNsplits(); s++) {
                    TaxaSet split = splits.getSplitsSet().getSplit(s);
                    if (split.get(i) != split.get(j))
                        dij += splits.getWeight(s);
                }
                if (max_i < 0 || dij > maxdist) {
                    max_i = i;
                    max_j = j;
                    maxdist = dij;
                }
            }
        }

        /*
        Next step.... identify the edges along which the midpoint can lie.
         */
        BitSet splitsOnPath = new BitSet();
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.getSplitsSet().getSplit(s);
            if (split.get(max_i) != split.get(max_j)) {
                splitsOnPath.set(s);
            }
        }

        /*
        Find node labelled by taxa max_i.
         */

        PhyloGraphView graphView = new PhyloGraphView();

        network.syncNetwork2PhyloGraphView(taxa, splits, graphView);

        PhyloSplitsGraph g = graphView.getPhyloGraph();
        List midpointEdges = findMidpoints(g, g.getTaxon2Node(max_i), 0.0, maxdist / 2.0, splitsOnPath);
        for (Object midpointEdge : midpointEdges) {
            Edge e = (Edge) midpointEdge;

            graphView.setLineWidth(e, 5);
        }

        return "Most divergent taxa are " + taxa.getLabel(max_i) + " and " + taxa.getLabel(max_j);

    }
}
