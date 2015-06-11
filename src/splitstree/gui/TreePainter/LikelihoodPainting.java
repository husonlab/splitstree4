/**
 * LikelihoodPainting.java 
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
