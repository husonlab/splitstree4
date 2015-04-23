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

/**
 * @version $Id: QuartetsTransform.java,v 1.7 2007-09-11 12:31:09 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.algorithms.quartets;


import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Quartets;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute data (e.g. splits or trees) from
 * quartets
 */
public interface QuartetsTransform extends Transformation {

    String COMMAND = "quartTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa     the taxa
     * @param quartets the quartets
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Quartets quartets);
}

// EOF
