/**
 * MultiGeneAnalysis.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.algorithms.util;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Taxa;
import splitstree4.util.CharactersUtilities;
import splitstree4.util.Partition;
import splitstree4.util.SplitMatrix;

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
     * @throws jloda.util.CanceledException     The user pressed cancel in the progress bar.
     * @throws splitstree4.core.SplitsException A problem with the analysis, or the partition was not valid.
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
        } catch (CanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            throw new SplitsException("Multigene analysis failed:" + ex);
        }
        return splitMatrix;
    }

}
