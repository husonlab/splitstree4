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

package splitstree.externalIO.exports;

import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.util.SplitsUtilities;

import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Exports a text file containing the data for a Lento-plot of splits
 * with their incompatibilities.
 */
public class LentoPlotData extends ExporterAdapter {
    private static final String DESCRIPTION = "Exports tabbed delimited file with data for a Lento Plot";

    /**
     * can we import this data?
     *
     * @param dp       Document
     * @param selected set of selected blocks
     * @return true, if can handle this import
     */
    public boolean isApplicable(Document dp, Collection selected) {
        if (dp.getTaxa() == null || dp.getSplits() == null || dp.getSplits().getNsplits() == 0)
            return false;
        return !(selected.size() != 1 || !selected.contains(Splits.NAME));
    }


    /**
     * get a description of this exporter
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Writes the Data to the writer w. If the exporter only handels subsets of different type the set
     * can be used to check for the choosen Nexus blocks. The set contains the Nexus names.
     *
     * @param w      The writer
     * @param dp     The Document
     * @param blocks list of blocks to exported
     * @return mapping from export names to original names
     * @throws Exception
     */
    public Map apply(Writer w, Document dp, Collection blocks) throws Exception {
        LentoSplit[] lentoSplits = computeLentoPlot(dp.getTaxa(), dp.getSplits());
        String result = "Lento Plot\n";
        result += "id\tweight\tconflict\tsplit\n";
        for (int i = 1; i < lentoSplits.length; i++) {
            LentoSplit s = lentoSplits[i];
            result += s.id + "\t" + s.weight + "\t" + s.conflict + "\t" + s.split + "\n";
        }
        w.write(result);

        return null;
    }


    private static LentoSplit[] computeLentoPlot(Taxa taxa, Splits splits) {
        //First fill the array.
        int ntax = taxa.getNtax();
        int nSplits = splits.getNsplits();
        LentoSplit[] lentoSplits = new LentoSplit[nSplits + 1];
        for (int i = 1; i <= nSplits; i++) {
            LentoSplit thisSplit = new LentoSplit(i, splits.get(i), splits.getWeight(i));
            if (thisSplit.split.get(ntax))
                thisSplit.split = thisSplit.split.getComplement(ntax);
            lentoSplits[i] = thisSplit;
        }
        //Now compute conflict
        for (int i = 1; i <= nSplits; i++) {
            TaxaSet split_i = splits.get(i);
            for (int j = i + 1; j <= nSplits; j++) {
                TaxaSet split_j = splits.get(j);
                if (!SplitsUtilities.areCompatible(ntax, split_i, split_j)) {
                    lentoSplits[i].conflict += splits.getWeight(j);
                    lentoSplits[j].conflict += splits.getWeight(i);
                }
            }
        }

        //Sort entries
        Object o = lentoSplits[1];
        LentoSplit s = (LentoSplit) o;

        Arrays.sort(lentoSplits, 1, nSplits + 1);
        return lentoSplits;
    }


    static class LentoSplit implements Comparable {
        public int id;
        public double conflict;
        public double weight;
        public TaxaSet split;

        LentoSplit(int id, TaxaSet split, double weight) {
            this.id = id;
            this.split = split;

            this.weight = weight;
            this.conflict = 0.0;
        }

        //Sort first by conflict, then by weight.
        public int compareTo(Object o) {
            LentoSplit s2 = (LentoSplit) o;
            LentoSplit s1 = this;


            if (s1.conflict < s2.conflict)
                return -1;
            else if (s1.conflict > s2.conflict)
                return 1;
            else {
                if (s1.weight > s2.weight)
                    return -1;
                else if (s1.weight < s2.weight)
                    return 1;
                else return 0;
            }
        }
    }


}
