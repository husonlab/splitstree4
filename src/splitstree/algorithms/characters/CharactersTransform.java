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

/** $Id: CharactersTransform.java,v 1.18 2007-09-11 12:31:02 kloepper Exp $
 */
package splitstree.algorithms.characters;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;

/**
 * Interface for methods that compute data (e.g. distances, splits, tree)
 * from characters
 */
public interface CharactersTransform extends Transformation {

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    boolean isApplicable(Document doc, Taxa taxa, Characters chars);
}

// EOF
