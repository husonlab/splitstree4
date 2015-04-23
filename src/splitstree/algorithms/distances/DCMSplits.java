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
 * DCM for splits
 * @version $Id: DCMSplits.java,v 1.7 2007-09-11 12:31:06 kloepper Exp $
 * @author Daniel Huson and David Bryant
 */
package splitstree.algorithms.distances;

import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 * implements the disk-covering method for split decomposition
 */
public class DCMSplits implements Distances2Splits {
    public final boolean EXPERT = true;
    public final static String DESCRIPTION = "Run the Disk-Covering method (Huson, Nettles, Warnow, 1999) [Not implemented]";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return false;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa      the taxa
     * @param distances the input distances
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Distances distances) throws Exception {

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
