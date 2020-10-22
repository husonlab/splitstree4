/**
 * Reorder.java
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
 * Reorder taxa
 *
 * @version $Id: Reorder.java,v 1.6 2007-01-25 08:58:16 huson Exp $
 * @author Daniel Huson and David Bryant
 * 3.2003
 * Reorder taxa
 * @version $Id: Reorder.java,v 1.6 2007-01-25 08:58:16 huson Exp $
 * @author Daniel Huson and David Bryant
 * 3.2003
 */
/** Reorder taxa
 *@version $Id: Reorder.java,v 1.6 2007-01-25 08:58:16 huson Exp $
 *@author Daniel Huson and David Bryant
 * 3.2003
 */
package splitstree4.progs;

import jloda.swing.util.CommandLineOptions;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Given a taxon block and a splits block, reorders the taxa so that
 * they come in alphabetical order
 */
public class Reorder {
    /**
     * The main program.
     * Usage: Reorder -i infile -o outfile
     *
     * @param args the list of options
     */
    public static void main(String[] args) throws Exception {
        CommandLineOptions options = new CommandLineOptions(args);
        options.setDescription
                ("Reorder - Reorder taxa alphabetically");
        String inname = options.getOption("-i", "input file", "in.nex");
        String outname = options.getOption("-o", "output file", inname + ".ord");
        options.done();

        Taxa taxa = new Taxa();
        Splits splits = new Splits();

        NexusStreamParser np = new NexusStreamParser(new BufferedReader(new FileReader(new File(inname))));

        np.matchIgnoreCase("#nexus");
        taxa.read(np);
        splits.read(np, taxa);

        Taxa rtaxa = new Taxa();
        Splits rsplits = new Splits(taxa.getNtax());
        reorder(taxa, splits, rtaxa, rsplits);

        FileWriter w = new FileWriter(new File(outname));
        w.write("#nexus\n");
        rtaxa.write(w);
        rsplits.write(w, taxa);
        w.close();
    }

    /**
     * Reorders the taxa lexicographically
     *
     * @param taxa    the original taxa
     * @param splits  the original splits
     * @param rtaxa   the reordered taxa
     * @param rsplits the reordered splits
     */
    public static void reorder(Taxa taxa, Splits splits, Taxa rtaxa, Splits rsplits) throws Exception {
        SortedSet<String> taxons = new TreeSet<>();
        int[] oldid2newid = new int[taxa.getNtax() + 1];

        for (int i = 1; i <= taxa.getNtax(); i++)
            taxons.add(taxa.getLabel(i));

        rtaxa.setNtax(taxa.getNtax());

        Iterator it = taxons.iterator();
        int count = 0;
        while (it.hasNext()) {
            count++;
            String name = (String) (it.next());
            rtaxa.setLabel(count, name);
            oldid2newid[taxa.indexOf(name)] = count;
        }

        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet split = splits.get(s);
            TaxaSet newSplit = new TaxaSet();

            for (int i = 1; i <= taxa.getNtax(); i++) {
                if (split.get(i))
                    newSplit.set(oldid2newid[i]);
            }
            rsplits.add(newSplit, splits.getWeight(s));
        }
    }
}

// EOF
