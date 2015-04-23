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
 * runs clustalw externally
 * @version $Id: ClustalW.java,v 1.5 2007-09-11 12:31:02 kloepper Exp $
 * @author Daniel Huson and David Bryant
 * 7.03
 */
package splitstree.algorithms.characters;

import splitstree.algorithms.unaligned.Unaligned2Characters;
import splitstree.core.Document;
import splitstree.nexus.Characters;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

/**
 * runs clustalw externally
 */
public class ClustalW implements Unaligned2Characters {

    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "Externally runs ClustalW [not implemented]";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned data
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned unaligned) {
        return false;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param unaligned the unaligned
     * @return the computed characters Object
     */
    public Characters apply(Document doc, Taxa taxa, Unaligned unaligned) throws Exception {
        return null;
    }

    /**
       * gets a short description of the algorithm
       *
       * @return a description
       */
    public String getDescription() {
          return DESCRIPTION;
      }

}
