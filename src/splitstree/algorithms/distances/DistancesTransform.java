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
 * @version $Id: DistancesTransform.java,v 1.14 2008-03-10 21:02:17 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.algorithms.distances;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute data (e.g. splits or trees) from
 * distances
 */
public interface DistancesTransform extends Transformation {
    String COMMAND = "distTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Distances distances);
}

// EOF
