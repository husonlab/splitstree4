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
 * @version $Id: TreesTransform.java,v 1.4 2006-05-23 05:57:33 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */
package splitstree.algorithms.trees;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

/**
 * Interface for methods that compute data (e.g. splits) from a tree
 */
public interface TreesTransform extends Transformation {
    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc  the document
     * @param taxa the taxa
     * @param tree a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Trees tree);
}

// EOF
