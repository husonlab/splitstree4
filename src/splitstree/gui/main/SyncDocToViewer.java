/**
 * SyncDocToViewer.java 
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
package splitstree.gui.main;

import jloda.phylo.PhyloGraphView;
import jloda.util.ProgramProperties;
import splitstree.core.Document;
import splitstree.main.SplitsTreeProperties;
import splitstree.nexus.Assumptions;
import splitstree.nexus.Network;

/**
 * syncronizes the graph to viewer
 *
 * @author huson
 *         Date: 29-Nov-2003
 */
public class SyncDocToViewer {
    /**
     * syncronizes the graph to viewer
     */
    public static void syncNetworkToViewer(Document doc, MainViewer viewer) {
        if (viewer.getSelectedNodes().size() > 0) {
            MainViewer.getPreviouslySelectedNodeLabels().clear();
            MainViewer.getPreviouslySelectedNodeLabels().addAll(viewer.getSelectedNodeLabels());
        }
        viewer.resetViews();

        final Network network = doc.getNetwork();

        if (doc.isValid() && doc.getTaxa() != null && network != null) {

            // if we have any taxon2vertex description from last draw, apply
            if (doc.taxon2VertexDescription != null) {
                network.applyTaxon2VertexDescription(doc.taxon2VertexDescription);
                //doc.taxon2VertexDescription = null; // don't reuse
            }

            network.syncNetwork2PhyloGraphView(doc.getTaxa(), doc.getSplits(), viewer);
            network.syncNetworkToEdgeLabels(viewer);
            network.syncNetworkToNodeLabels(viewer);

            if (doc.getAssumptions() == null)
                doc.setAssumptions(new Assumptions());

            viewer.setAutoLayoutLabels(doc.getAssumptions().getAutoLayoutNodeLabels());
            viewer.setRadiallyLayoutNodeLabels(doc.getAssumptions().getRadiallyLayoutNodeLabels());

            viewer.setVisible(true);
            viewer.setLayoutType(network.getLayout());

            if (ProgramProperties.get(SplitsTreeProperties.AUTOSCALE, true) || viewer.trans.isEmpty())
                viewer.trans.setCoordinateRect(viewer.getBBox());
            viewer.fitGraphToWindow();
        } else
            viewer.setVisible(false);
    }


    /**
     * syncs the viewer's respresentation of node labels after a change
     *
     * @param doc     the document
     * @param viewer  the viewer
     * @param network the network
     */
    public static void syncNodeLabels(Document doc, PhyloGraphView viewer, Network network) {

        if (network != null)
            network.syncNetworkToNodeLabels(viewer);
        viewer.repaint();
    }

    /**
     * syncs the viewer's respresentation of edge labels after a change
     *
     * @param doc     the document
     * @param viewer  the viewer
     * @param network the network
     */
    public static void syncEdges(Document doc, PhyloGraphView viewer, Network network) {
        network.syncNetworkToEdgeLabels(viewer);
        viewer.repaint();
    }
}

