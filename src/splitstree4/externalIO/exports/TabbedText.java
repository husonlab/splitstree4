/*
 * TabbedText.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.externalIO.exports;

import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.*;
import splitstree4.util.SplitMatrix;

import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * tab delimited text files
 */
public class TabbedText extends ExporterAdapter implements Exporter {

    private String Description = "Exports data to tab delimited text files";
    private String Name = "Tabbed Text";


    private static final boolean sortTaxa = false;  //Sort taxa according to the cycle.


    /**
     * can we export this data?
     *
     * @param dp
     * @return true, if can handle this export
     */
    public boolean isApplicable(Document dp, Collection selected) {

        LinkedList goodBlocks = new LinkedList();
        goodBlocks.add(Characters.NAME);
        goodBlocks.add(Taxa.NAME);
        goodBlocks.add(Distances.NAME);
        goodBlocks.add(Splits.NAME);
        goodBlocks.add(Trees.NAME);
        goodBlocks.add(Bootstrap.NAME);

        //Check that the tree block does not contain partial trees
        for (Object aSelected : selected) {
            if (((String) aSelected).equalsIgnoreCase(Trees.NAME))
                if (dp.getTrees() == null || dp.getTrees().getPartial())
                    return false;
        }

        return blocksOK(dp, selected, goodBlocks);
    }


    /**
     * Writes selected blocks to a tab-delimited text file.
     *
     * @param w
     * @param dp
     * @param blockNames
     * @return
     * @throws Exception
     */
    public Map apply(Writer w, Document dp, Collection blockNames) throws Exception {


        DecimalFormat dec = new DecimalFormat("#.0#####");

        if (dp.getTaxa() != null && blockNames.contains(Taxa.NAME)) {
            w.write("Taxa\n");
            for (int i = 1; i <= dp.getTaxa().getNtax(); i++) {
                w.write(i + "\t" + dp.getTaxa().getLabel(i) + "\n");
            }
            w.write("\n");
        }

        int[] cycle = null;
        if (false)
            cycle = dp.getSplits().getCycle();

        if (cycle == null) {
            //Default ordering
            int ntax = dp.getTaxa().getNtax();
            cycle = new int[ntax + 1];
            for (int i = 1; i <= ntax; i++)
                cycle[i] = i;
        }


        if (dp.getCharacters() != null && blockNames.contains(Characters.NAME)) {
            w.write("Characters\n");

            //First row is the taxa names (or ids if there is no taxa block)
            if (dp.getTaxa() != null) {
                for (int i = 1; i <= dp.getCharacters().getNtax(); i++)
                    w.write("\t" + dp.getTaxa().getLabel(cycle[i]));
            } else {
                for (int i = 1; i <= dp.getCharacters().getNtax(); i++)
                    w.write("\t" + cycle[i]);
            }
            w.write("\n");

            //Now we loop through the characters, one site per row.
            int nchars = dp.getCharacters().getNchar();
            int ntax = dp.getCharacters().getNtax();
            for (int i = 1; i <= nchars; i++) {
                if (!getOptionExportAll() && dp.getCharacters().isMasked(i))
                    continue; //Skip masked characters.
                //Site number
                w.write(Integer.toString(i));
                for (int j = 1; j <= ntax; j++)
                    w.write("\t" + dp.getCharacters().get(cycle[j], i));
                w.write("\n");
            }
            w.write("\n");
        }


        if (blockNames.contains(Distances.NAME) && dp.getDistances() != null) {
            //Export the distances as a matrix then as a column vector.
            w.write("Distance matrix\n");
            Distances dist = dp.getDistances();
            int ntax = dist.getNtax();
            for (int i = 1; i <= ntax; i++) {
                for (int j = 1; j <= ntax; j++) {
                    w.write("" + dist.get(i, j));
                    if (j < ntax)
                        w.write("\t");
                }
                w.write("\n");
            }
            w.write("Distance matrix as column vector. (1,2),(1,3),..,(1,n),(2,3),...\n");
            for (int i = 1; i <= ntax; i++) {
                for (int j = i + 1; j <= ntax; j++)
                    w.write("" + dist.get(i, j) + "\n");
            }
            w.write("\n");
        }


        if (dp.getSplits() != null && blockNames.contains(Splits.NAME)) {
            w.write("Splits\n");

            Splits splits = dp.getSplits();
            //First row is the taxa names (or ids if there is no taxa block)
            boolean hasWeights = splits.getFormat().getWeights();

            if (hasWeights)
                w.write("\tWeights");
            if (dp.getTaxa() != null) {
                for (int i = 1; i <= splits.getNtax(); i++)
                    w.write("\t" + dp.getTaxa().getLabel(cycle[i]));
            } else {
                for (int i = 1; i <= splits.getNtax(); i++)
                    w.write("\t" + cycle[i]);
            }
            w.write("\n");

            //Now we loop through the splits, one split per row.
            int nsplits = splits.getNsplits();
            int ntax = splits.getNtax();
            for (int i = 1; i <= nsplits; i++) {

                //Split number
                w.write(Integer.toString(i));
                if (hasWeights)
                    w.write("\t" + dec.format(splits.getWeight(i)));
                TaxaSet A = splits.get(i);
                for (int j = 1; j <= ntax; j++) {
                    char ch = A.get(cycle[j]) ? '1' : '0';
                    w.write("\t" + ch);
                }

                w.write("\n");
            }
            w.write("\n");
        }

        if (blockNames.contains(Trees.NAME) && dp.getTaxa() != null && dp.getTrees() != null && !dp.getTrees().getPartial()) {
            w.write("Tree splits\n");

            int ntax = dp.getTaxa().getNtax();

            SplitMatrix treeSplits = new SplitMatrix(dp.getTrees(), dp.getTaxa());

            //First row contains taxaNames, then tree names.
            for (int i = 1; i < treeSplits.getNtax(); i++) {
                w.write("\t" + dp.getTaxa().getLabel(cycle[i]));
            }
            for (int i = 1; i < treeSplits.getNblocks(); i++) {
                w.write("\t" + dp.getTrees().getName(i));
            }
            w.write("\n");

            //Now we loop through the splits. For each split we print its membership (first
            //ntax columns), then the weight of that split in each tree
            for (int i = 1; i <= treeSplits.getNsplits(); i++) {
                w.write("" + i);       //ID

                //Now the split
                TaxaSet A = treeSplits.getSplit(i);
                for (int j = 1; j <= ntax; j++) {
                    char ch = A.get(cycle[j]) ? '1' : '0';
                    w.write("\t" + ch);
                }

                //Now the weights
                for (int j = 1; j <= treeSplits.getNblocks(); j++) {
                    double weight = treeSplits.get(i, j);
                    w.write("\t" + weight);
                }
                w.write("\n");
            }

            w.write("\n");
        }


        if (blockNames.contains(Bootstrap.NAME) && dp.getBootstrap() != null && dp.getBootstrap().getBsplits() != null) {
            w.write("Bootstrap Splits\n");

            Splits splits = dp.getBootstrap().getBsplits();

            //First row is the taxa names (or ids if there is no taxa block)
            boolean hasWeights = splits.getFormat().getWeights();

            if (hasWeights)
                w.write("\tWeights");
            if (dp.getTaxa() != null) {
                for (int i = 1; i <= splits.getNtax(); i++)
                    w.write("\t" + dp.getTaxa().getLabel(cycle[i]));
            } else {
                for (int i = 1; i <= splits.getNtax(); i++)
                    w.write("\t" + cycle[i]);
            }
            w.write("\n");

            //Now we loop through the splits, one split per row.
            int nsplits = splits.getNsplits();
            int ntax = splits.getNtax();
            for (int i = 1; i <= nsplits; i++) {

                //Split number
                w.write(Integer.toString(i));
                if (hasWeights)
                    w.write("\t" + dec.format(splits.getWeight(i)));
                TaxaSet A = splits.get(i);
                for (int j = 1; j <= ntax; j++) {
                    char ch = A.get(cycle[j]) ? '1' : '0';
                    w.write("\t" + ch);
                }

                w.write("\n");
            }
            w.write("\n");
        }

        if (blockNames.contains(Bootstrap.NAME) && dp.getBootstrap() != null && dp.getBootstrap().getSplitMatrix() != null) {
            w.write("Split Matrix\n");
            SplitMatrix splitMatrix = dp.getBootstrap().getSplitMatrix();
            /* First row is split id's   */
            for (int col = 1; col <= splitMatrix.getNsplits(); col++)
                w.write("\t" + col);
            w.write("\n");
            for (int row = 0; row < splitMatrix.getNblocks(); row++) {
                w.write("" + (row + 1));
                for (int col = 1; col <= splitMatrix.getNsplits(); col++) {
                    w.write("\t" + dec.format(splitMatrix.get(col, row)));
                }
                w.write("\n");
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
