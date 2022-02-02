/*
 * Splits2Splits.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.additional;

import jloda.util.CanceledException;
import splitstree4.algorithms.Transformation;
import splitstree4.core.Document;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * Transformation from splits to splits
 *
 * @author huson
 * Date: 17-Feb-2004
 */
public interface Splits2Splits extends Transformation {

    /**
     * is split post modification applicable?
     *
     * @param doc    the document
     * @param splits the split
     * @return true, if split2splits transform applicable?
     */
    boolean isApplicable(Document doc, Taxa taxa, Splits splits);

    /**
     * applies the splits to splits transfomration
     *
	 */
    void apply(Document doc, Taxa taxa,
               Splits splits) throws CanceledException;
}
