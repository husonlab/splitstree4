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

package splitstree.algorithms.splits;

/**
 * computes a hybridization network
 *
 * @author huson
 *         Date: 22-Mar-2005
 */
public class HybridizationNetwork extends ReticulateNetwork implements Splits2Network {

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return "Hybridization networks: Huson et al, RECOMB 2007, RECOMB 2005";
    }
}
