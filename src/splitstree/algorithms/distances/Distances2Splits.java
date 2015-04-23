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
 * @version $Id: Distances2Splits.java,v 1.14 2007-09-11 12:31:07 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.algorithms.distances;

import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute splits from distances
 */
public interface Distances2Splits extends DistancesTransform {
   
    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param d    the input distances
     * @return the computed set of splits
     */
    Splits apply(Document doc, Taxa taxa, Distances d) throws Exception;
}

// EOF
