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

package splitstree.algorithms.unaligned;

import splitstree.core.Document;
import splitstree.nexus.Quartets;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

/**
 * Interface for methods that compute quartets from unaligned characters
 */
public interface Unaligned2Quartets extends UnalignedTransform {


    /**
     * Applies the method to the given data
     *
     * @param taxa the taxa
     * @param data the unaligned matrix
     * @return the computed set of splits
     */
    Quartets apply(Document doc, Taxa taxa, Unaligned data);
}

// EOF
