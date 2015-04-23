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

package splitstree.algorithms.distances;

import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.CircularLeastSquares;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jul 23, 2010
 * Time: 10:10:26 AM
 *
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
    
    public Splits apply(Document doc, Taxa taxa, Distances d) throws Exception {

        //First obtain ordering
        doc.notifyTasks("Computing LassoNet","Computing circular ordering");
        int[] ordering = NeighborNet.computeNeighborNetOrdering(d);
        CircularLeastSquares circularLeastSquares = new CircularLeastSquares(ordering);
        return circularLeastSquares.optimalAICLasso(doc, taxa, d,this.optionInformationCriterion,this.optionStoppingPoint,this.optionLasso);
    }

    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return doc.isValid(taxa) && doc.isValid(distances);
    }

    public String getDescription() {
        return "Constructs an ordering using NeighborNet but then obtains split weights using a lasso algorithm. Rea and Bryant (2010)";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
