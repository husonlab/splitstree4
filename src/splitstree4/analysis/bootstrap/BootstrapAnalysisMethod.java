/**
 * BootstrapAnalysisMethod.java
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
 * $Id: BootstrapAnalysisMethod.java,v 1.1 2005-11-08 11:13:40 huson Exp $
 * $Id: BootstrapAnalysisMethod.java,v 1.1 2005-11-08 11:13:40 huson Exp $
 */
/** $Id: BootstrapAnalysisMethod.java,v 1.1 2005-11-08 11:13:40 huson Exp $
 */
package splitstree4.analysis.bootstrap;

import splitstree4.analysis.AnalysisMethod;
import splitstree4.core.Document;

/**
 * Interface for classes that analyze the bootstrap block
 */
public interface BootstrapAnalysisMethod extends AnalysisMethod {
    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc)
    ;


    /**
     * Runs the analysis
     *
     * @param doc
     */
    String apply(Document doc);

}

// EOF
