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

package splitstree.algorithms.additional;

import jloda.util.CanceledException;
import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

/**
 Transformation from splits to splits
 *
 * @author huson
 *         Date: 17-Feb-2004
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
     * @param doc
     * @param taxa
     * @param splits
     * @throws jloda.util.CanceledException
     */
    void apply(Document doc, Taxa taxa,
               Splits splits) throws CanceledException;
}
