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

package splitstree.algorithms.util.simulate;

import splitstree.nexus.Taxa;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Aug 28, 2005
 * Time: 10:21:42 AM
 * <p/>
 * Utility routines for creating synthetic blocks of taxa - for simulation and tests.
 */
public class RandomTaxa {
    static public Taxa generateTaxa(int ntax) {
        return generateTaxa(ntax, "taxon");
    }

    static public Taxa generateTaxa(int ntax, String prefix) {
        Taxa taxa = new Taxa();
        for (int i = 1; i <= ntax; i++) {
            taxa.add(prefix + i);
        }
        return taxa;
    }
}
