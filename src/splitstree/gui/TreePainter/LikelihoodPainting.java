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

package splitstree.gui.TreePainter;

import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.main.MainViewer;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 16, 2005
 * Time: 12:51:47 PM
 */
public class LikelihoodPainting {

    final private String DESCRIPTION = "Paints posterior probabilities on a tree";

    public static void apply(Director dir) {

        MainViewer viewer = (MainViewer) dir.getViewerByClass(MainViewer.class);
        int size = viewer.getGraph().getNumberOfNodes();
        //LikelihoodTreeModel model = new LikelihoodTreeModel(viewer,dir.getDocument().getTrees(),dir.getDocument().getDistances(),dir.getDocument().getCharacters());
//                LikelihoodUtilities.fillDowngroup(model.getL().getDowngroup(),model);
//                LikelihoodUtilities.fillUpgroup(model.getL().getDowngroup(),model.getP().getUpgroup(),model);
//                model.getL().computeLikelihood(model);
//                System.out.println("The likelihood is : " + model.getL().Likelihood);
//
//                LikelihoodUtilities.printDowngroup(model.getL().getDowngroup(),model);
//                LikelihoodUtilities.printUpgroup(model.getP().getUpgroup(),model);

        // When we have the likelihood, we use it to paint the edges

        //viewer.me.setColoredEdges(true);
        //viewer.me.model = model;
    }

    static public boolean isApplicable(Director dir) {
        Document doc = dir.getDocument();
        return doc.getTrees() != null && doc.getCharacters() != null && dir.getViewerByClass(MainViewer.class) != null;
    }

    public String getDescription() {
        return DESCRIPTION;
    }
}
