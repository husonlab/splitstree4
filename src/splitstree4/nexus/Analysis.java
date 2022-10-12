/*
 * Analysis.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.nexus;

import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import splitstree4.algorithms.util.Configurator;
import splitstree4.analysis.bootstrap.BootstrapAnalysisMethod;
import splitstree4.analysis.characters.CharactersAnalysisMethod;
import splitstree4.analysis.distances.DistancesAnalysisMethod;
import splitstree4.analysis.network.NetworkAnalysisMethod;
import splitstree4.analysis.quartets.QuartetsAnalysisMethod;
import splitstree4.analysis.splits.SplitsAnalysisMethod;
import splitstree4.analysis.trees.TreesAnalysisMethod;
import splitstree4.analysis.unaligned.UnalignedAnalysisMethod;
import splitstree4.core.Document;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The nexus analysis block. This is where we launch different data analysis methods
 * from
 */
public class Analysis extends NexusBlock {
    /**
     * Identification string
     */
    public final static String NAME = "st_Analysis";
    private List analyzers = new LinkedList();

    /**
     * Constructor
     */
    public Analysis() {
        this(true);
    }

    /**
     * Constructor
     */
    public Analysis(boolean addStandardStats) {
        if (addStandardStats) {
            analyzers.add(new Analyzer(Characters.NAME + " on Stats;"));
            analyzers.add(new Analyzer(Splits.NAME + " on Stats;"));
            analyzers.add(new Analyzer(Trees.NAME + " on Stats;"));

            //analyzers.add(new Analyzer(Distances.NAME + " on Stats;"));
        }
    }

    /**
     * returns the number of registered analyzers
     *
     * @return number of analyzers
     */
    public int getNanalyzers() {
        if (analyzers == null)
            return 0;
        else
            return analyzers.size();
    }

    /**
     * Reads the block
     *
     * @param np nexus stream parser
     */
    public void read(NexusStreamParser np) throws IOException {

        np.matchBeginBlock(NAME);

        while (!np.peekMatchIgnoreCase("end;")) {
            if (np.peekMatchIgnoreCase("clear;")) {
                analyzers = new LinkedList();
                np.matchIgnoreCase("clear;");
            } else {
                Analyzer analyzer = new Analyzer();
                analyzer.read(np);
                if (!analyzer.getName().equals("Stats"))  // don't read Stats ones
                    analyzers.add(analyzer);
            }
        }
        np.matchEndBlock();
    }

    /**
     * Write the analysis block
     *
     * @param w a writer
     */
    public void write(Writer w) throws IOException {
        w.write("\nBEGIN " + Analysis.NAME + ";\n");
        for (Object analyzer1 : analyzers) {
            Analyzer analyzer = (Analyzer) (analyzer1);
            if (!analyzer.getName().equals("Stats"))  // don't write Stats ones
                analyzer.write(w);
        }
        w.write("END; [" + Analysis.NAME + "]\n");
    }

    /**
     * write a block, blocks should override this
     *
	 */
    public void write(Writer w, Taxa taxa) throws IOException {
        write(w);
    }

    /**
     * Show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN ST_ANALYSIS;");
        ps.println("\t[clear;]");
        ps.println("\t[" + Unaligned.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Characters.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Distances.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Quartets.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Trees.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Splits.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Network.NAME + " [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("\t[" + Bootstrap.NAME + " [{ON|OFF|ONCE}] name [parameters];]");

        ps.println("\t[ALL [{ON|OFF|ONCE}] name [parameters];]");
        ps.println("END;");
    }

    /**
     * Applies all current analyzers
     *
     * @param doc the document
     * @return output produced by analyzers
     */
    public String apply(Document doc) throws CanceledException {
        return apply(doc, doc.getTaxa(), null);
    }

    /**
     * Applies all current analyzers to the given block
     *
     * @param blockName the name of the block
     * @return results of all applied analyzers
     */
    public String apply(Document doc, Taxa taxa, String blockName) throws CanceledException {
        List toDelete = new LinkedList(); // we delete any analyzers that are only to be run once

        String result = "";
        int count = 0;
        doc.notifySetMaximumProgress(analyzers.size());
        Iterator it = analyzers.iterator();
        while (it.hasNext()) {
            Analyzer analyzer = (Analyzer) (it.next());
            if (!analyzer.getState().equals("off"))
                try {
                    if (blockName == null || blockName.equalsIgnoreCase(analyzer.getKind())) {
                        result += analyzer.apply(doc, taxa) + "\n";
                        if (analyzer.getState().equals("once")) {
                            analyzer.setState("off");
                            toDelete.add(analyzer);
                        }
                    }
                } catch (Exception ex) {
                    jloda.util.Basic.caught(ex);
                }
            doc.notifySetProgress(++count);
        }
        // delete all once only analyers
        it = toDelete.iterator();
        while (it.hasNext())
            analyzers.remove(it.next());
        return result;
    }


}

/**
 * A single analysis object
 */
class Analyzer {
    private String kind = null;  // the kind of data to apply the analyzer to
    private String name = null;  // the name of the analyzer class
    private String params = null; // the parameter string
    private String state = "once"; // off, on or once

    /**
     * Constructor
     */
    Analyzer() {
    }

    /**
     * Constructor an analyzer from a string
     *
     * @param str the string
     */
    Analyzer(String str) {
        NexusStreamParser np = new NexusStreamParser(new StringReader(str));
        try {
            read(np);
        } catch (IOException ex) {
            jloda.util.Basic.caught(ex);
        }
    }

    /**
     * Get the kind
     *
     * @return kind
     */
    String getKind() {
        return kind;
    }

    /**
     * Get the name
     *
     * @return name
     */
    String getName() {
        return name;
    }

    /**
     * Get the parameters
     *
     * @return the parameters
     */
    String getParameters() {
        return params;
    }

    /**
     * Gets the state on, off or once
     *
     * @return the state
     */
    String getState() {
        return state;
    }

    /**
     * Sets the state, must be off, on or once
     *
     * @param state the new state
     */
    void setState(String state) {
        this.state = state;
    }

    /**
     * reads an analyzer
     *
     * @param np a nexus parser
     */
    void read(NexusStreamParser np) throws IOException {
        np.peekMatchAnyTokenIgnoreCase
                (Unaligned.NAME + " " + Characters.NAME + " " + Distances.NAME +
                 " " + Quartets.NAME + " " + Splits.NAME + " " + Trees.NAME + " " + Network.NAME + " " + Bootstrap.NAME);
        kind = np.getWordRespectCase();
        if (np.peekMatchAnyTokenIgnoreCase("off on once"))
            state = np.getWordRespectCase();
        name = np.getWordRespectCase();
        if (!np.peekMatchIgnoreCase(";"))
            params = np.getTokensStringRespectCase(";");
        else
            np.matchIgnoreCase(";");
    }

    /**
     * Writes the analyzer
     *
     * @param w writer
     */
    void write(Writer w) throws IOException {
        w.write(kind + " " + state + " " + name);
        if (params != null)
            w.write(" " + params);
        w.write(";\n");
    }

    /**
     * gets string representation
     *
     * @return string representation
     */
    public String toString() {
        return kind + " " + state + " " + name;
    }

    /**
     * Applies the analyzer
     *
     * @param doc  the document block
     * @param taxa the taxa
     */
    String apply(Document doc, Taxa taxa) throws Exception {
        String prefix = "splitstree4.analysis." + kind.toLowerCase() + ".";
        Class theClass;
        if (!getName().contains("."))
            theClass = Class.forName(prefix + getName());
        else
            theClass = Class.forName(getName());

        splitstree4.analysis.AnalysisMethod plugin = (splitstree4.analysis.AnalysisMethod) theClass.newInstance();
        Configurator.setOptions(plugin, getParameters());

        String result = "";
        if (getKind().equalsIgnoreCase(Unaligned.NAME))
            result = ((UnalignedAnalysisMethod) plugin).apply(doc, taxa, doc.getUnaligned());
        else if (getKind().equalsIgnoreCase(Characters.NAME))
            result = ((CharactersAnalysisMethod) plugin).apply(doc);
        else if (getKind().equalsIgnoreCase(Distances.NAME))
            result = ((DistancesAnalysisMethod) plugin).apply(doc, taxa, doc.getDistances());
        else if (getKind().equalsIgnoreCase(Quartets.NAME))
            result = ((QuartetsAnalysisMethod) plugin).apply(doc, taxa, doc.getQuartets());

        else if (getKind().equalsIgnoreCase(Trees.NAME))
            result = ((TreesAnalysisMethod) plugin).apply(doc, taxa, doc.getTrees());

        else if (getKind().equalsIgnoreCase(Splits.NAME))
            result = ((SplitsAnalysisMethod) plugin).apply(doc, taxa, doc.getSplits());

        else if (getKind().equalsIgnoreCase(Network.NAME))
            result = ((NetworkAnalysisMethod) plugin).apply(doc, taxa, doc.getNetwork());

        else if (getKind().equalsIgnoreCase(Bootstrap.NAME))
            result = ((BootstrapAnalysisMethod) plugin).apply(doc);

        else if (getKind().equalsIgnoreCase(Characters.NAME))
            result = ((CharactersAnalysisMethod) plugin).apply(doc);
        if (result != null) {
            System.err.println(result);
        }
        return result;
    }
}

//EOF
