/**
 * SequenceBasedDistance.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
