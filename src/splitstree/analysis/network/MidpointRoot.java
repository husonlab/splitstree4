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

package splitstree.analysis.network;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.BitSet;
import java.util.Iterator;
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

    public static String DESCRIPTION = "Identifies edges on the network containing midpoints on the paths between the most divergent taxa";

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
     * @param doc
     * @param taxa    the taxa
     * @param network the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Network network) {

        return (doc.getSplits() != null && network != null);  //To change body of implemented methods use File | Settings | File Templates.
    }


    private List findMidpoints(PhyloGraph g, Node v, double travelled, double midway, BitSet toVisit) {

        List midpointList = new LinkedList();
        for (Iterator edgePtr = v.getAdjacentEdges(); edgePtr.hasNext();) {
            Edge e = (Edge) edgePtr.next();
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
     * @param doc
     * @param taxa    the taxa
     * @param network the block
     */
    public String apply(Document doc, Taxa taxa, Network network) {
        /** FIrst step: identify the most distant taxa, according to the splits block.
         *
         */

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

        PhyloGraph g = graphView.getPhyloGraph();
        List midpointEdges = findMidpoints(g, g.getTaxon2Node(max_i), 0.0, maxdist / 2.0, splitsOnPath);
        for (Object midpointEdge : midpointEdges) {
            Edge e = (Edge) midpointEdge;

            graphView.setLineWidth(e, 5);
        }

        return "Most divergent taxa are " + taxa.getLabel(max_i) + " and " + taxa.getLabel(max_j);

    }
}
