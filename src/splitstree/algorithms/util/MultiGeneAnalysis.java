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

package splitstree.algorithms.util;

import jloda.util.CanceledException;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.nexus.Taxa;
import splitstree.util.CharactersUtilities;
import splitstree.util.Partition;
import splitstree.util.SplitMatrix;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jun 13, 2006
 * Time: 4:18:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiGeneAnalysis {
    /**
     * Conducts separate analysis on different blocks in the given character site position. The splits
     * from these analysis are stored in a SplitMatrix object, which is returned.
     *
     * @param doc
     * @param partition
     * @return SplitMatrix splits returned by the different analyses.
     * @throws jloda.util.CanceledException The user pressed cancel in the progress bar.
     * @throws splitstree.core.SplitsException  A problem with the analysis, or the partition was not valid.
     */
    static public SplitMatrix multiGene(Document doc, Partition partition) throws CanceledException, SplitsException {
        int nblocks = partition.getNumBlocks();
        int ntax = doc.getTaxa().getNtax();
        SplitMatrix splitMatrix = new SplitMatrix(ntax);

        Document subDoc = new Document();
        subDoc.setTaxa((Taxa) doc.getTaxa().clone());
        subDoc.setAssumptions(doc.getAssumptions().clone(subDoc.getTaxa()));
        subDoc.getAssumptions().setExTaxa(null);
        subDoc.setInBootstrap(true);
        doc.notifySetMaximumProgress(nblocks);
        doc.notifyTasks("Multigene Analysis", "");
        //PrintStream ps = null;

        try {
            for (int block = 1; block <= nblocks; block++) {
                Set subset = partition.getBlock(block);
                subDoc.setCharacters(CharactersUtilities.characterSubset(doc.getCharacters(), subset));
                //ps = jloda.util.Basic.hideSystemErr();//disable syserr.
                subDoc.update();
                //jloda.util.Basic.restoreSystemErr(ps);   //enable syserr

                splitMatrix.add(subDoc.getSplits());  //Store the splits recovered.
                doc.notifySetProgress(block);
            }
        }
        catch (CanceledException ex) {
            throw ex;
        }
        catch (Exception ex) {
            ex.printStackTrace(System.err);
            throw new SplitsException("Multigene analysis failed:" + ex);
        }
        return splitMatrix;
    }

}
