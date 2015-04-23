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

package splitstree.gui.main;

import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

/**
 * syncs the viewer to the doc
 *
 * @author huson
 *         Date: 11-Mar-2005
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
