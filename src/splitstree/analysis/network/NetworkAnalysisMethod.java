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

/** $Id: NetworkAnalysisMethod.java,v 1.1 2005-11-08 11:13:40 huson Exp $
 */
package splitstree.analysis.network;

import splitstree.analysis.AnalysisMethod;
import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

/**
 * Interface for classes that analyze the network
 */
public interface NetworkAnalysisMethod extends AnalysisMethod {
    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc
     * @param taxa    the taxa
     * @param network the block
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Network network)
    ;

    /**
     * Runs the analysis
     *
     * @param doc
     * @param taxa    the taxa
     * @param network the block
     */
    String apply(Document doc, Taxa taxa, Network network);

}

// EOF
