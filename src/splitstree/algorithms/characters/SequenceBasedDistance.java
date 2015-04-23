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

import splitstree.nexus.Characters;
import splitstree.nexus.Distances;

/**
 * SequenceBasedDistance
 * <p/>
 * This should be implemented by any distance methods based on standard
 * Markov models for sequences. Allows setting of model parameters like
 * the gamma disribution, proportion of invariant sites, etc.
 */
public abstract class SequenceBasedDistance implements Characters2Distances {

    abstract public double getOptionPInvar();

    abstract public void setOptionPInvar(double pinvar);


    abstract public double getOptionGamma();

    abstract public void setOptionGamma(double gamma);

    abstract public Distances computeDist(Characters characters);
}
