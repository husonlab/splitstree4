/**
 * Matlab.java
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
package splitstree4.externalIO.exports;

import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * Output data from within the program to a tab delimited text file. Generally, the user will want to
 * output one block at a time.
 * <p/>
 * Distances are output as a column vector, indexed 12,13,14,....,(n-1)n
 * Splits are output as a topological (design) matrix.
 * <p/>
 * The output is intended mainly for those wanting to use other programs to estimate network settings
 */

public class Matlab extends ExporterAdapter implements Exporter {

    private String Description = "Exports data for matlab format";
    private String Name = "Matlab";


    private static final boolean sortTaxa = false;  //Sort taxa according to the cycle.


    /**
     * can we export this data?
     *
     * @param dp the document being exported
     * @return true, if can handle this export
     */
    public boolean isApplicable(Document dp, Collection selected) {

        LinkedList goodBlocks = new LinkedList();
        //goodBlocks.add(Characters.NAME);
        goodBlocks.add(Taxa.NAME);
        goodBlocks.add(Distances.NAME);
        goodBlocks.add(Splits.NAME);
        //goodBlocks.add(Bootstrap.NAME);

        return blocksOK(dp, selected, goodBlocks);
    }


    /**
     * Writes selected blocks to a tab-delimited text file.
     *
     * @param w          Where to write the data
     * @param dp         The document being exported
     * @param blockNames Collection of blocks to be exported
     * @return null
     * @throws Exception
     */
    public Map apply(Writer w, Document dp, Collection blockNames) throws Exception {

        w.write("%%MATLAB%%\n");

        DecimalFormat dec = new DecimalFormat("#.0#####");

        if (dp.getTaxa() != null && blockNames.contains(Taxa.NAME)) {
            w.write("%%Number Taxa then taxon names\n");
            w.write("" + dp.getTaxa().getNtax() + "\n");
            for (int i = 1; i <= dp.getTaxa().getNtax(); i++) {
                w.write("\t" + dp.getTaxa().getLabel(i) + "\n");
            }
            w.write("\n");
        }


        if (dp.getSplits() != null && blockNames.contains(Splits.NAME)) {
            w.write("%%Number of splits, then row of split weights, then design matrix, same row ordering as distances\n");

            Splits splits = dp.getSplits();
            int nsplits = splits.getNsplits();
            w.write("%%Number of splits\n");
            w.write("" + nsplits + "\n");
            w.write("%% Split weights\n");
            for (int j = 1; j <= nsplits; j++)
                w.write(" " + splits.getWeight(j));
            w.write("\n");

            int ntax = splits.getNtax();
            for (int i = 1; i < ntax; i++) {
                for (int j = i + 1; j <= ntax; j++) {
                    for (int k = 1; k <= nsplits; k++) {
                        TaxaSet S = splits.get(k);
                        if (S.get(i) != S.get(j))
                            w.write("\t" + 1);
                        else
                            w.write("\t" + 0);
                    }
                    w.write("\n");
                }
            }
            w.write("\n");
        }


        if (blockNames.contains(Distances.NAME) && dp.getDistances() != null) {
            //Export the distances as a matrix then as a column vector.
            int ntax = dp.getTaxa().getNtax();
            Distances dist = dp.getDistances();
            w.write("%%Distance matrix as column vector. (1,2),(1,3),..,(1,n),(2,3),...\n");
            for (int i = 1; i <= ntax; i++) {
                for (int j = i + 1; j <= ntax; j++)
                    w.write("" + dec.format(dist.get(i, j)) + "\n");
            }
            w.write("\n");
        }


        return null;
    }

    public String getDescription() {
        return Description;
    }

    public String getName() {
        return Name;
    }
}
