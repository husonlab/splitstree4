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

/** $Id: UnalignedTransform.java,v 1.8 2006-05-23 05:57:33 huson Exp $
 */
package splitstree.algorithms.unaligned;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

/**
 * Interface for methods that compute data (e.g. distances, splits, tree)
 * from unaligned
 */
public interface UnalignedTransform extends Transformation {
    String COMMAND = "unalignedTransform";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param data the data
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Unaligned data);
}

// EOF
