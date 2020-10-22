/**
 * SyncViewerToDoc.java
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
package splitstree4.gui.main;

import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;

/**
 * syncs the viewer to the doc
 *
 * @author huson
 * Date: 11-Mar-2005
 */
public class SyncViewerToDoc {
    static public void sync(MainViewer viewer, Document doc) {

        // here we make a taxa block and network block in the case that
        // the program was used to interactively produce a new graph
        if (viewer.getGraph().getNumberOfNodes() > 0 && doc.getTaxa() == null)
            doc.setTaxa(new Taxa());

        Network network = doc.getNetwork();

        if (viewer.getGraph().getNumberOfNodes() > 0 && network == null) {
            doc.setNetwork(new Network());
            network = doc.getNetwork();
        }

        if (doc.isValid(doc.getTaxa()) && doc.isValid(network)) {
            network.syncPhyloGraphView2Network(doc.getTaxa(), viewer);
            // save a copy of the vertex descriptions to apply again later:
            doc.getNetwork().updateTaxon2VertexDescriptionMap(doc.taxon2VertexDescription);
        }
        if (network != null)
            network.setLayout(viewer.getLayoutType());
        // sync transform:
        //System.err.println("trans: scale="+viewer.trans.getScale() +" angle="+
        //        viewer.trans.getDeviceAngle());

        if (viewer != null && doc.getAssumptions() != null) {
            doc.getAssumptions().setAutoLayoutNodeLabels(viewer.getAutoLayoutLabels());
            doc.getAssumptions().setRadiallyLayoutNodeLabels(viewer.getRadiallyLayoutNodeLabels());
        }
    }

}
