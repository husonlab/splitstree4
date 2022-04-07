/*
 * LassoNet.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.distances;

import splitstree4.core.Document;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.util.CircularLeastSquares;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jul 23, 2010
 * Time: 10:10:26 AM
 * <p>
 * Lasso Net uses NeighborNet to obtain a circular ordering of the taxa, but then uses a Lasso algorithm (with AIC)
 * to select the splits.
 */
public class LassoNet implements Distances2Splits {

    public static final String FULL_LASSO = "All splits";
    public static final String STAR_LASSO = "Non-trivial splits";

    private String optionInformationCriterion = "AIC";
    private String optionLasso = "STAR_LASSO";
    private int optionStoppingPoint = -1;

    public void setOptionInformationCriterion(String varName) {
        this.optionInformationCriterion = varName;
    }

    public String getOptionInformationCriterion() {
        return this.optionInformationCriterion;
    }

    public List selectionOptionInformationCriterion(Document doc) {
        List ics = new LinkedList();
        ics.add("AIC");
        ics.add("BIC");
        return ics;
    }

    public void setOptionLasso(String varName) {
        this.optionLasso = varName;
    }

    public String getOptionLasso() {
        return this.optionLasso;
    }

    public List selectionOptionLasso(Document doc) {
        List ics = new LinkedList();
        ics.add(FULL_LASSO);
        ics.add(STAR_LASSO);
        return ics;
    }


    public void setOptionStoppingPoint(int k) {
        this.optionStoppingPoint = k;
    }

    public int getOptionStoppingPoint() {
        return this.optionStoppingPoint;
    }

    public Splits apply(Document doc, Taxa taxa, Distances d) throws IOException {

        //First obtain ordering
        doc.notifyTasks("Computing LassoNet", "Computing circular ordering");
        int[] ordering = NeighborNet.computeNeighborNetOrdering(d);
        CircularLeastSquares circularLeastSquares = new CircularLeastSquares(ordering);
        return circularLeastSquares.optimalAICLasso(doc, taxa, d, this.optionInformationCriterion, this.optionStoppingPoint, this.optionLasso);
    }

    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return doc.isValid(taxa) && doc.isValid(distances);
    }

    public String getDescription() {
        return "Constructs an ordering using NeighborNet but then obtains split weights using a lasso algorithm. Rea and Bryant (2010)";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
