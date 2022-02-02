/*
 * Evaluator.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.progs;

import jloda.swing.util.CommandLineOptions;
import jloda.util.parse.NexusStreamParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;

/**
 * small program to evaluate simulation
 */
public class Evaluator {
    static public void main(String[] args) throws Exception {
        CommandLineOptions options = new CommandLineOptions(args);
        String fname = options.getMandatoryOption("-i", "input file", "");
        int length = options.getMandatoryOption("-l", "sequence length", 0);
        options.done();

		BufferedReader r = new BufferedReader(new FileReader(fname));
		FileWriter good = new FileWriter("Correct_Topology." + length);
        FileWriter bad = new FileWriter("Wrong_Topology." + length);
        int nGood = 0;
        double sumGood = 0;
        int nBad = 0;
        double sumBad = 0;

        String aline = r.readLine();
        while (aline != null) {
            double w1 = 0, w2 = 0;
            int j1 = 0, j2 = 0;
            for (int line = 1; line <= 2; line++) {
                NexusStreamParser np = new NexusStreamParser(new StringReader(aline));

                double w = np.getDouble();
                np.matchIgnoreCase("1"); // must always be taxon 1 for SplitsTree
                int j = np.getInt();
                if (line == 1) {
                    w1 = w;
                    j1 = j;
                } else {
                    w2 = w;
                    j2 = j;
                }
                aline = r.readLine();
            }
            double score = 1 - Math.min(w1, w2) / Math.max(w1, w2);
            if ((j1 == 2 && w1 > w2) || (j2 == 2 && w2 > w1)) {
                good.write("" + length + " " + score + "\n");
                nGood++;
                sumGood += score;
            } else {
                bad.write("" + length + " " + score + "\n");
                nBad++;
                sumBad += score;
            }
        }

        good.flush();
        bad.flush();

        System.err.println("# Correct topologies: " + nGood + ", mean tree-likeness: " + sumGood / nGood);
        System.err.println("# Wrong topologies:   " + nBad + ", mean tree-likeness: " + sumBad / nBad);
    }

}
