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

package splitstree.progs;

import jloda.util.CommandLineOptions;
import jloda.util.parse.NexusStreamParser;

import java.io.*;

/**
 * small program to evaluate simulation
 */
public class Evaluator {
    static public void main(String[] args) throws Exception {
        CommandLineOptions options = new CommandLineOptions(args);
        String fname = options.getMandatoryOption("-i", "input file", "");
        int length = options.getMandatoryOption("-l", "sequence length", 0);
        options.done();

        BufferedReader r = new BufferedReader(new FileReader(new File(fname)));
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
