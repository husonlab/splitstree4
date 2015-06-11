/**
 * Trees.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
/**
 * @version $Id: Trees.java,v 1.62 2008-07-10 15:01:00 bryant Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.nexus;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.NotOwnerException;
import jloda.util.parse.NexusStreamParser;
import splitstree.algorithms.trees.TreeSelector;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.core.TaxaSet;
import splitstree.util.TreesUtilities;

import java.io.*;
import java.util.*;

/**
 * NexusBlock trees class
 */
public class Trees extends NexusBlock {
    /**
     * Identification string
     */
    final public static String NAME = "Trees";
    private int ntrees = 0; // number of trees
    private boolean partial = false; // does this block contain trees on subsets of the taxa?
    private boolean rooted = false; // are the trees rooted?
    private boolean rootedGloballySet = false; // if, true, this overrides [&R] statment
    final private Vector<String> names = new Vector<>(); // list of names
    final private Vector<PhyloTree> trees = new Vector<>(); // list of phylotrees
    final private Vector<TaxaSet> taxasets = new Vector<>(); // list of taxa sets for tree
    final private Map<String, String> translate = new HashMap<>(); // maps node labels to taxon labels

    /**
     * Construct a new Trees object.
     */
    public Trees() {
        super();
    }

    /**
     * Constructs a new Trees object and adds the given phylogenetic tree to it
     *
     * @param name the name of the tree
     * @param tree the phylogenetic tree
     * @param taxa the taxa
     */
    public Trees(String name, PhyloTree tree, Taxa taxa) {
        super();
        try {
            this.addTree(name, tree, taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * clears all the data associated with this trees block
     */
    public void clear() {
        translate.clear();
        trees.clear();
        names.clear();
        taxasets.clear();
        ntrees = 0;
        partial = false;
        rooted = false;
        rootedGloballySet = false;
    }

    /**
     * Constructs a new Trees object and adds the given phylogenetic tree to it
     *
     * @param name  the name of the tree
     * @param tree  the phylogenetic tree
     * @param taxa  the taxa
     * @param trans map of transitions
     */
    public Trees(String name, PhyloTree tree, Taxa taxa, Map<String, String> trans) {
        super();
        try {
            checkTranslation(tree, trans);
            this.setTranslate(trans);
            this.addTree(name, tree, taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * Get the number of trees
     *
     * @return number of trees
     */
    public int getNtrees() {
        return ntrees;
    }

    /**
     * Returns the i-th tree name.
     * Trees are numbered 1 to ntrees
     *
     * @param i the number of the tree
     * @return the i-th tree name
     */
    public String getName(int i) {
        return names.elementAt(i - 1);
    }

    /**
     * sets the i-th tree name
     *
     * @param i
     * @param name
     */
    public void setName(int i, String name) {
        names.setElementAt(name, i - 1);
    }

    /**
     * Returns the i-th tree taxaset.
     * Trees are numbered 1 to ntrees
     *
     * @param i the number of the tree
     * @return the i-th tree taxaset
     */
    public TaxaSet getTaxaSet(int i) {
        return taxasets.elementAt(i - 1);
    }

    /**
     * sets the i-th tree taxaset
     *
     * @param i
     * @param taxaset
     */
    public void setTaxaSet(int i, TaxaSet taxaset) {
        taxasets.setElementAt(taxaset, i - 1);
    }

    /**
     * returns the index of the named tree.
     * Trees are numbered 1 to ntrees
     *
     * @param name
     * @return index of named tree
     */
    public int indexOf(String name) {
        return names.indexOf(name) + 1;
    }

    /**
     * Returns the i-th tree
     *
     * @param i the number of the tree
     * @return the i-th tree
     */
    public PhyloTree getTree(int i) {
        return trees.elementAt(i - 1);
    }

    /**
     * Returns the nexus flag [&R] indicating whether the tree should be considered
     * as rooted
     *
     * @param i
     * @return String  Returns [&R] if rooted, and "" otherwise.
     */
    public String getFlags(int i) {
        if (getTree(i).getRoot() != null)
            return "[&R]";
        else
            return "";
    }

    /**
     * remove the tree at index i.
     * The index must be between 1 and ntrees
     *
     * @param i
     */
    public void removeTree(int i) {

        if (names.remove(i - 1) != null && trees.remove(i - 1) != null)
            ntrees--;
    }

    /**
     * Adds a tree to the list of trees. If this is called to add the first
     * tree to the trees block, then the tree nodes must be labeled with
     * taxon names or integers 1..ntax. If this is not the case, then use
     * the other addTree method described below. Subsequent trees can be
     * added by this method regardless of which labels are used for nodes,
     * as long as they are compatible with the initial translation table.
     *
     * @param name the name of the tree
     * @param tree the phylogenetic tree
     * @param taxa the taxa block
     */
    public void addTree(String name, PhyloTree tree, Taxa taxa)
            throws SplitsException, NotOwnerException {
        if (translate.size() == 0) // need to setup translation table
        {
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                String nodelabel = tree.getLabel(v);
                if (nodelabel != null) {
                    int t = taxa.indexOf(nodelabel);
                    if (t > 0)
                        // translate.put(taxa.getLabel(t), nodelabel);
                        translate.put(nodelabel, taxa.getLabel(t));
                    else if (!getPartial())
                        throw new SplitsException("Invalid node label: " + nodelabel);
                }
            }
        }
        checkTranslation(tree, this.translate);

        ntrees++;
        trees.setSize(ntrees);
        trees.add(ntrees - 1, tree);

        // make sure tree gets unique name
        names.setSize(ntrees);
        if (name.length() == 0)
            name = "tree";
        if (names.indexOf(name) != -1) {
            int count = 1;
            while (names.indexOf(name + "_" + count) != -1)
                count++;
            name += "_" + count;
        }
        names.add(ntrees - 1, name);
        taxasets.add(ntrees - 1, (this.getTaxaInTree(taxa, ntrees)));
        // these taxa sets are only set so that we can easily tell whether set of trees
        // is partial. This is done in splitstree.analysis.trees.Stats
    }

    /**
     * Determines whether a given tree and given translation map are
     * compatible with one another
     * If is partial, sets the trees block partial variable
     *
     * @param tree  the phylogenetic tree
     * @param trans the map from taxon labels to node labels
     */
    public void checkTranslation(PhyloTree tree, Map trans)
            throws SplitsException, NotOwnerException {
        // collect all node labels:
        ArrayList<String> nodelabels = new ArrayList<>();

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            String label = tree.getLabel(v);
            if (label != null)
                nodelabels.add(label);
        }

        //DEBUGGING CODE
        //Object[] keys = trans.keySet().toArray();

        // check that labels in the tree hit only known labels:
        // this can be more than one label, so check all elements in the map
        for (String nodelabel : nodelabels) {
            if (trans.get(nodelabel) == null)
                throw new SplitsException("Tree: No such taxon: " + nodelabel);
        }
    }

    // public void addToTranslation(Map trans,)

    /**
     * Gets the taxon-label to node-label map
     *
     * @return the map
     */
    public Map getTranslate() {
        return translate;
    }

    /**
     * sets the node-label to taxon translation map
     *
     * @param trans
     */
    public void setTranslate(Map<String, String> trans) {
        this.translate.clear();
        for (String key : trans.keySet()) {
            this.translate.put(key, trans.get(key));
        }
    }

    /**
     * Returns the set of taxa associated with a given node-label
     *
     * @param nlab the node label
     * @return the set of taxa mapped to the given node label
     */
    public TaxaSet getTaxaForLabel(Taxa taxa, String nlab) {
        TaxaSet result = new TaxaSet();
        try {
            if (nlab != null)
                for (int t = 1; t <= taxa.getNtax(); t++) {
                    if (translate.get(nlab).equals(taxa.getLabel(t))) {
                        result.set(t);
                    }
                }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return result;
    }

    /**
     * Writes trees taxa object in nexus format
     *
     * @param w a writer
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        w.write("\nBEGIN " + Trees.NAME + ";\n");
        if (getPartial() || getRooted()) {
            w.write("PROPERTIES");
            if (getPartial())
                w.write(" partialtrees=yes");
            if (getRooted())
                w.write(" rooted=yes");
            w.write(";\n");
        }
        if (translate.size() > 0 &&
                (translate.size() > taxa.getNtax() || getPartial() || !translateIsOneToOne(translate))) {
            w.write("TRANSLATE\n");
            Set<String> keys = translate.keySet();

            for (String key : keys) {
                w.write("'" + key + "' '" + translate.get(key) + "',\n");
            }
            w.write(";\n");
        }

        w.write("[TREES]\n");
        for (int t = 1; t <= getNtrees(); t++) {
            w.write("[" + t + "] tree '" + getName(t) + "'=" + getFlags(t) + " " + getTree(t) + ";\n");
        }
        w.write("END; [" + Trees.NAME + "]\n");
    }

    /**
     * is translate one-to-one?
     *
     * @param translate
     * @return true, if one-to-one
     */
    public boolean translateIsOneToOne(Map<String, String> translate) {
        Set<String> keys = translate.keySet();

        for (String key : keys) {
            if (!key.equals(translate.get(key)))
                return false;
        }
        return true;

    }


    /**
     * Reads a tree object in NexusBlock format
     *
     * @param np   nexus stream parser
     * @param taxa the taxa block
     */
    public void read(NexusStreamParser np, Taxa taxa) throws SplitsException, IOException {
        np.matchBeginBlock(NAME);
        clear();

        if (np.peekMatchIgnoreCase("properties")) {
            List<String> tokens = np.getTokensLowerCase("properties", ";");
            if (np.findIgnoreCase(tokens, "no partialtrees"))
                partial = false;
            if (np.findIgnoreCase(tokens, "partialtrees=no"))
                partial = false;
            if (np.findIgnoreCase(tokens, "partialtrees=yes"))
                partial = true;
            if (np.findIgnoreCase(tokens, "partialtrees"))
                partial = true;
            if (np.findIgnoreCase(tokens, "rooted=yes")) {
                rooted = true;
                rootedGloballySet = true;
            }
            if (np.findIgnoreCase(tokens, "rooted=no")) {
                rooted = false;
                rootedGloballySet = true;
            }
            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in PROPERTIES");
        }

        if (np.peekMatchIgnoreCase("translate")) {
            List<String> taxlabels = new ArrayList<>();
            np.matchIgnoreCase("translate");
            while (!np.peekMatchIgnoreCase(";")) {
                String nodelabel = np.getWordRespectCase();
                String taxlabel = np.getWordRespectCase();
// if we have a translate and have to detect the Tasa use the taxlabels
                taxlabels.add(taxlabel);
                translate.put(nodelabel, taxlabel);

                if (!np.peekMatchIgnoreCase(";"))
                    np.matchIgnoreCase(",");
            }
            np.matchIgnoreCase(";");

            // dirty stuff, set the orig taxa here
            if (!taxa.getMustDetectLabels() && getPartial() && taxlabels.size() > taxa.getNtax()) {
                Taxa origTaxa = new Taxa();
                origTaxa.setNtax(taxlabels.size());
                Object[] list = taxlabels.toArray();
                for (int i = 0; i < list.length; i++) origTaxa.setLabel(i + 1, (String) list[i]);
                if (taxa.getOriginalTaxa() == null || taxa.getOriginalTaxa().getNtax() < origTaxa.getNtax()) {
                    taxa.setOriginalTaxa(origTaxa);
                }
            }
            if (taxa.getMustDetectLabels()) {
                if (getPartial())   // might need to change number of taxa
                    taxa.setNtax(taxlabels.size());
                Object[] list = taxlabels.toArray();
                for (int i = 0; i < list.length; i++) taxa.setLabel(i + 1, (String) list[i]);
                System.err.println("Reset taxa labels from trees-translate command");
            }
            taxa.setMustDetectLabels(false);
        } else if (taxa.getMustDetectLabels()) {
            throw new SplitsException("line " + np.lineno() +
                    ": Taxon labels not given in taxa block, thus TRANSLATE-statement required");
        } else {
            // set the translation table from the taxa:
            translate.clear();
            for (int t = 1; t <= taxa.getNtax(); t++)
                translate.put(taxa.getLabel(t), taxa.getLabel(t));
        }
        while (np.peekMatchIgnoreCase("tree")) {
            np.matchIgnoreCase("tree");
            if (np.peekMatchRespectCase("*"))
                np.matchRespectCase("*"); // don't know why PAUP puts this star in the file....

            String name = np.getWordRespectCase();
            name = name.replaceAll("[ \t\b]+", "_");
            name = name.replaceAll("[:;,]+", ".");
            name = name.replaceAll("\\[", "(");
            name = name.replaceAll("\\]", ")");

            np.matchIgnoreCase("=");
            np.getComment(); // clears comments


            StringBuilder buf = new StringBuilder();

            List<String> tokensToCome = np.getTokensRespectCase(null, ";");
            for (String s : tokensToCome) {
                buf.append(s);
            }

            /*
            while (!np.peekMatchIgnoreCase(";"))
                buf.append(np.getWordRespectCase());
            np.matchIgnoreCase(";");
              */


            boolean isRooted;
            if (rootedGloballySet)
                isRooted = getRooted();
            else {
                String comment = np.getComment();
                isRooted = (comment != null && comment.equalsIgnoreCase("&R"));
            }

            PhyloTree tree = PhyloTree.valueOf(buf.toString(), isRooted);
            if (translate.size() == 0 && taxa.getMustDetectLabels()) {
                int count = 1;
                for (Node n = tree.getFirstNode(); n != null; n = tree.getNextNode(n)) {

                    if (tree.getLabel(n) != null) taxa.setLabel(count++, tree.getLabel(n));
                }
            }
            addTree(name, tree, taxa);
            /*
            np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
            try {
                String tmp = np.getWordRespectCase();
                boolean isRooted;
                if (rootedGloballySet)
                    isRooted = getRooted();
                else
                    isRooted = (np.isComment() != null && np.isComment().equalsIgnoreCase("&R"));
                // System.err.println("tmp: <"+tmp+">");
                PhyloTree tree = PhyloTree.valueOf(tmp, isRooted);


                if (translate.size() == 0 && taxa.getMustDetectLabels()) {
                    int count = 1;
                    for (Node n = tree.getFirstNode(); n != null; n = tree.getNextNode(n)) {

                        if (tree.getLabel(n) != null) taxa.setLabel(count++, tree.getLabel(n));
                    }
                }
                addTree(name, tree, taxa);
            } catch (Exception ex) {
                Basic.caught(ex);
                np.popPunctuationCharacters();
                throw new SplitsException("line " + np.lineno() +
                        ": Add tree failed: " + ex.getMessage());
            }
            np.popPunctuationCharacters();
            np.matchIgnoreCase(";");            
            */
        }
        np.matchEndBlock();
    }

    /**
     * are trees considered rooted? If yes, then any divertex root is preserved
     *
     * @return true, if rooted
     */
    public boolean getRooted() {
        return rooted;
    }

    /**
     * are trees considered rooted?
     *
     * @param rooted
     */
    public void setRooted(boolean rooted) {
        this.rooted = rooted;
    }

    /**
     * Returns true if tree i is rooted, else false
     *
     * @param i number of the tree
     * @return true if tree i is rooted
     */
    public boolean isRooted(int i) {
        return this.getTree(i).getRoot() != null;
    }

    /**
     * Produces a string representation of a NexusBlock object
     *
     * @return object in nexus format
     */
    public String toString(Taxa taxa) {
        StringWriter sw = new StringWriter();
        try {
            write(sw, taxa);
        } catch (Exception ex) {
            return "()";
        }
        return sw.toString();
    }

    /**
     * show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN " + Trees.NAME);
        ps.println("[PROPERTIES PARTIALTREES={YES|NO} ROOTED={YES|NO};]");
        ps.println("[TRANSLATE");
        ps.println("    nodeLabel1 taxon1,");
        ps.println("    nodeLabel2 taxon2,");
        ps.println("    ...");
        ps.println("    nodeLabelN taxonN");
        ps.println(";]");
        ps.println("[TREE name1 = tree1-in-Newick-format;]");
        ps.println("[TREE name2 = tree2-in-Newick-format;]");
        ps.println("...");
        ps.println("[TREE nameM = treeM-in-Newick-format;]");
        ps.println("END;");
    }

    /**
     * does trees block contain a partial tree?
     *
     * @return true, if contains a partial tree
     */
    public boolean getPartial() {
        return partial;
    }

    /**
     * does trees block contain a partial tree
     *
     * @param partial
     */
    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    /**
     * returns the set of taxa contained in this tree.
     *
     * @param taxa  original taxa
     * @param which tree
     * @return set of taxa not present in this tree. Uses original numbering
     */
    public TaxaSet getTaxaInTree(Taxa taxa, int which) {
        PhyloTree tree = getTree(which);

        TaxaSet seen = new TaxaSet();
        Iterator it = tree.nodeIterator();
        while (it.hasNext()) {
            try {
                String nodeLabel = tree.getLabel((Node) it.next());
                if (nodeLabel != null) {
                    String taxonLabel = translate.get(nodeLabel);
                    if (taxa.indexOf(taxonLabel) == -1) {
                        System.err.println("can't find " + nodeLabel + " in ");
                        Taxa.show("taxa", taxa);
                        if (taxa.getOriginalTaxa() != null)
                            Taxa.show("orig", taxa.getOriginalTaxa());
                    } else
                        seen.set(taxa.indexOf(taxonLabel));
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        }
        return seen;
    }

    /**
     * gets the support of this tree, that is, the set of taxa mentioned in it
     *
     * @param taxa
     * @param which tree
     * @return the set of taxa mentioned in this tree
     */
    public TaxaSet getSupport(Taxa taxa, int which) {
        TaxaSet support = new TaxaSet();
        PhyloTree tree = getTree(which);
        Set labels = tree.getNodeLabels();

        for (int t = 1; t <= taxa.getNtax(); t++) {
            if (labels.contains(translate.get(taxa.getLabel(t))))
                support.set(t);
        }
        return support;
    }

    /**
     * gets the value of a format switch
     *
     * @param name
     * @return value of format switch
     */
    public boolean getFormatSwitchValue(String name) {
        return true;
    }

    /**
     * Determine the set of taxa for partial trees.
     * If the block contains partial trees, then the translate statement must mention all
     * taxa. We use this info to build a taxa block
     *
     * @param taxa
     * @throws SplitsException
     */
    public void setTaxaFromPartialTrees(Taxa taxa) throws SplitsException {
        Set<String> taxaLabels = new HashSet<>();
        for (int i = 1; i <= getNtrees(); i++) {
            PhyloTree tree = getTree(i);
            Set<String> nodeLabels = tree.getNodeLabels();
            for (String nodeLabel : nodeLabels) {
                taxaLabels.add(translate.get(nodeLabel));
            }
        }

        //are these taxa equal taxa, if so, do nothing:
        if (taxa.getNtax() == taxaLabels.size() && taxa.contains(taxaLabels))
            return;

        // if they are contained in the original taxa, unhide them:
        if (taxa.getOriginalTaxa() != null && taxa.getOriginalTaxa().contains(taxaLabels)) {
            TaxaSet toHide = new TaxaSet();
            for (int t = 1; t <= taxa.getOriginalTaxa().getNtax(); t++)
                if (!taxaLabels.contains(taxa.getOriginalTaxa().getLabel(t)))
                    toHide.set(t);
            taxa.hideTaxa(toHide);
        } else {
            taxa.setNtax(taxaLabels.size());
            Iterator it = taxaLabels.iterator();
            int t = 0;
            while (it.hasNext()) {
                taxa.setLabel(++t, (String) it.next());
            }
        }
    }

    private Trees originalTrees;
    private Taxa previousTaxa;

    /**
     * induces trees not containing the hidden taxa
     *
     * @param origTaxa
     * @param hiddenTaxa
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet hiddenTaxa) {
        if ((hiddenTaxa == null || hiddenTaxa.cardinality() == 0) && originalTrees == null)
            return;   // nothing to do

        Taxa inducedTaxa = Taxa.getInduced(origTaxa, hiddenTaxa);
        if (previousTaxa != null && inducedTaxa.equals(previousTaxa))
            return; // nothing to do
        previousTaxa = inducedTaxa;

        if (originalTrees == null)
            originalTrees = this.clone(origTaxa); // make a copy

        trees.clear();
        names.clear();
        ntrees = 0;
        // setPartial(originalTrees.getPartial());// this is clobbered by clear

        TreeSelector ts = new TreeSelector();

        for (int tr = 1; tr <= originalTrees.getNtrees(); tr++) {
            Taxa partialTaxa = (Taxa) origTaxa.clone();
            partialTaxa.setOriginalTaxa((Taxa) origTaxa.clone());

            ts.setOptionWhich(tr);

            Splits splits = ts.apply(new Document(), partialTaxa, originalTrees);
            // this many change the set of taxa, if the  tree is only partial

            TaxaSet toHideNewIndices = new TaxaSet();
            TaxaSet toHideOldIndices = new TaxaSet();

            for (int oldId = 1; oldId <= origTaxa.getNtax(); oldId++) {
                if (hiddenTaxa != null && hiddenTaxa.get(oldId)) {
                    int newId = partialTaxa.indexOf(origTaxa.getLabel(oldId));
                    if (newId > 0)
                        toHideNewIndices.set(newId);
                }
                if ((hiddenTaxa != null && hiddenTaxa.get(oldId)) ||
                        partialTaxa.indexOf(origTaxa.getLabel(oldId)) == -1)
                    toHideOldIndices.set(oldId);
            }

            if (toHideNewIndices.cardinality() > 0) {
                splits.hideTaxa(partialTaxa, toHideNewIndices);
                partialTaxa.hideTaxa(toHideOldIndices);
            }

            /*
            try {
                StringWriter sw = new StringWriter();
                partialTaxa.write(sw);
                splits.write(sw, partialTaxa);
                System.err.println(sw.toString());
            } catch (Exception ex) {
                Basic.caught(ex);
            }
            */
            // now splits and partialTaxa should live on the same subset of taxa...


            if (splits.getNsplits() > 0) {
                try {
                    PhyloTree tree = TreesUtilities.treeFromSplits(partialTaxa, splits, translate);
                    TreesUtilities.verifyTree(tree, translate, partialTaxa, false);

                    //System.err.println("induced taxa tree: " + tree.toString());

                    addTree(originalTrees.getName(tr), tree, partialTaxa);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
    }

    /**
     * returns the original set of trees or null
     *
     * @return original trees or null
     */
    public Trees getOriginal() {
        return originalTrees;
    }

    /**
     * restores the original splits
     *
     * @param originalTaxa
     */
    public void restoreOriginal(Taxa originalTaxa) {
        this.copy(originalTaxa, originalTrees);
        previousTaxa = originalTaxa;
    }

    /**
     * save the original trees
     *
     * @param originalTaxa
     */
    public void setOriginal(Taxa originalTaxa) {
        originalTrees = this.clone(originalTaxa);
        previousTaxa = null;
    }


    /**
     * clones a trees object
     *
     * @param taxa
     * @return a clone
     */
    public Trees clone(Taxa taxa) {
        Trees trees = new Trees();
        try {
            StringWriter sw = new StringWriter();
            this.write(sw, taxa);
            StringReader sr = new StringReader(sw.toString());
            trees.read(new NexusStreamParser(sr), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return trees;
    }


    /**
     * copies a trees object
     *
     * @param taxa
     * @param src  source tree
     */
    public void copy(Taxa taxa, Trees src) {
        try {
            StringWriter sw = new StringWriter();
            src.write(sw, taxa);
            StringReader sr = new StringReader(sw.toString());
            this.read(new NexusStreamParser(sr), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * sets the translate map to the identity mapping taxa->taxa
     *
     * @param taxa
     */
    public void setIdentityTranslate(Taxa taxa) {
        Map<String, String> map = new HashMap<>();
        for (int t = 1; t <= taxa.getNtax(); t++)
            map.put(taxa.getLabel(t), taxa.getLabel(t));
        setTranslate(map);
    }

    /**
     * Sets the translate map to taxaid->taxa
     *
     * @param taxa
     */
    public void setNumberedIdentityTranslate(Taxa taxa) {
        Map<String, String> map = new HashMap<>();
        for (int t = 1; t <= taxa.getNtax(); t++)
            map.put("" + t, taxa.getLabel(t));
        setTranslate(map);
    }

    /**
     * changes all node labels using the mapping old-to-new
     *
     * @param old2new maps old names to new names
     */
    public void changeNodeLabels(Map old2new) {
        for (int t = 1; t <= getNtrees(); t++) {
            PhyloTree tree = getTree(t);
            tree.changeLabels(old2new);
        }
    }
}

// EOF
