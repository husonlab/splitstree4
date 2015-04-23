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

/*
* $Id: Trees2Network.java,v 1.4 2007-09-11 12:31:08 kloepper Exp $
*/
package splitstree.algorithms.trees;

import splitstree.core.Document;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

/**
 * Interface for methods that compute a network from trees
 */
public interface Trees2Network extends TreesTransform {
    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees the trees
     * @return the computed network
     */
    Network apply(Document doc, Taxa taxa, Trees trees) throws Exception;
}

// EOF
