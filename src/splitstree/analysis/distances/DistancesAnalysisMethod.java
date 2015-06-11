/**
 * DistancesAnalysisMethod.java 
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
/** $Id: DistancesAnalysisMethod.java,v 1.1 2005-11-08 11:13:40 huson Exp $
 */
package splitstree.analysis.distances;

import splitstree.analysis.AnalysisMethod;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

/**
 * Interface for classes that analyze distances
 */
public interface DistancesAnalysisMethod extends AnalysisMethod {
    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc
     * @param taxa the taxa
     * @param dist the block
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Distances dist)
    ;

    /**
     * Runs the analysis
     *
     * @param doc
     * @param taxa the taxa
     * @param dist the block
     */
    String apply(Document doc, Taxa taxa, Distances dist) throws Exception;

}

// EOF
