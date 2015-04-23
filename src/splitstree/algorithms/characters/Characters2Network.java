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

package splitstree.algorithms.characters;

import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

/**
 * characters to network transform
 *
 * @author huson
 *         Date: 10-Feb-2005
 */
public interface Characters2Network extends CharactersTransform {


    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    Network apply(Document doc, Taxa taxa, Characters chars) throws Exception;
}

