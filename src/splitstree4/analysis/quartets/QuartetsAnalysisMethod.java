/**
 * QuartetsAnalysisMethod.java
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
 * $Id: QuartetsAnalysisMethod.java,v 1.1 2005-11-08 11:13:41 huson Exp $
 */
/** $Id: QuartetsAnalysisMethod.java,v 1.1 2005-11-08 11:13:41 huson Exp $
 */
package splitstree4.analysis.quartets;

import splitstree4.analysis.AnalysisMethod;
import splitstree4.core.Document;
import splitstree4.nexus.Quartets;
import splitstree4.nexus.Taxa;

/**
 * Interface for classes that analyze quartets
 */
public interface QuartetsAnalysisMethod extends AnalysisMethod {
    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc
     * @param taxa   the taxa
     * @param quarts the block
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc,

                         Taxa taxa, Quartets quarts)
    ;

    /**
     * Runs the analysis
     *
     * @param doc
     * @param taxa   the taxa
     * @param quarts the block
     */
    String apply(Document doc, Taxa taxa, Quartets quarts);

}

// EOF
