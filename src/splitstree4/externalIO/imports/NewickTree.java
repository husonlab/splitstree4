/*
 * NewickTree.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.externalIO.imports;

import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.StringUtils;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.NotMultiLabeledException;
import splitstree4.util.TreesUtilities;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.*;

/**
 * Import a tree in newick format
 * ALso, converts a multi-labeled tree into a set of splits
 */

public class NewickTree extends FileFilter implements Importer, FilenameFilter {
    String datatype = null;
	/* Implement first the Importer Interface
	 */
	public static final String Description = "Newick Tree Files (*.new,*.tre, *.tree)";
	private boolean optionConvertMultiLabeledTree = false;

    /**
     * does this importer apply to the type of nexus block
     *
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Trees.NAME);
    }

    /**
     * can we import this data?
     *
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input) throws IOException {
		try {
			String str;
			BufferedReader br = new BufferedReader(input);
			str = br.readLine().trim();
			str = StringUtils.removeComments(str, '[', ']');
			if (str.length() > 0 && str.charAt(0) == '(')
				return true;
		} catch (Exception ignored) {
		}
        return false;
    }

    /**
     * convert input into nexus format
     *
     * @return the input in nexus format
     */
    public String apply(Reader input) throws Exception {

        // importing first tree and generating taxa object
        String str;
        BufferedReader br = new BufferedReader(input);
        HashSet<String> labels = new HashSet<>();
        StringBuilder taxa = new StringBuilder();
        StringBuilder treesString = new StringBuilder();
        StringBuilder translate = new StringBuilder();
        translate.append("begin trees;\n");
        boolean haveWarnedMultipleLabels = false;

        // read in the trees
        int count = 1, size = 0;
        boolean partial = false;
        String aLine;
        while ((aLine = br.readLine()) != null) {
            if (aLine.trim().length() == 0 || aLine.startsWith("#"))
                continue; // skip empty lines and comment lines
            //System.err.println("Tree: " + aLine);
            str = "";
            while (aLine != null && (!aLine.contains(";"))) {
                str += aLine;
                aLine = br.readLine();
            }
            if (aLine != null) str += aLine;
            if (str.trim().length() != 0) {
                str = str.replaceAll(" ", "").replaceAll("\t", "");
                PhyloTree tree = new PhyloTree();
                try {
					tree.parseBracketNotation(StringUtils.removeComments(str, '[', ']'), true);
                    if (TreesUtilities.hasNumbersOnInternalNodes(tree))
                        TreesUtilities.changeNumbersOnInternalNodesToEdgeConfidencies(tree);
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                    throw ex;
                }

                if (getOptionConvertMultiLabeledTree()) {
                    try {
                        Document doc = convertMultiTree2Splits(tree);
                        StringWriter sw = new StringWriter();
                        doc.write(sw);
                        //System.err.println(sw.toString());
                        return sw.toString();
                    } catch (NotMultiLabeledException ex) {
                        Basic.caught(ex);
                    }
                } else {
                    if (tree.getInputHasMultiLabels() && !haveWarnedMultipleLabels) {
                        new Alert("One or more trees contain multiple occurrences of the same taxon-label,"
                                + " these have been made unique by adding suffixes .1, .2 etc");
                        haveWarnedMultipleLabels = true;
                    }
                }
                for (String label : tree.nodeLabels()) {
                    labels.add(label);
                }
                // this is for partial Trees
                if (size == 0) size = labels.size();
                if (labels.size() != size) partial = true;
                treesString.append("tree t").append(count++).append("=").append(str).append("\n");
            }
        }


        translate.append("properties rooted=yes");
        if (partial) translate.append(" partialtrees=yes");
        translate.append(";\n");
        translate.append("TRANSLATE\n");
        treesString.append("\nend;\n");
        // generate Taxa Object
        taxa.append("#nexus\nbegin taxa;\ndimensions ntax=").append(labels.size()).append(";\n");
        taxa.append("taxlabels\n");
        Iterator it = labels.iterator();
        int i = 1;
        while (it.hasNext()) {
            String label = (String) it.next();
            taxa.append("[").append(i++).append("]\t'").append(label).append("'\n");
            translate.append(label).append("\t").append(label).append(",\n");
        }
        translate.append(";\n");
        taxa.append(";\nend;\n");
        //System.err.println("buffer:\n" + taxa.toString() + "\n" + translate + "\n" + treesString);
		return (taxa + "\n" + translate + "\n" + treesString);
    }

    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List<String> getFileExtensions() {
        List<String> extensions = new LinkedList<>();
        extensions.add("new");
        extensions.add("tre");
        extensions.add("tree");
        return extensions;
    }

    /**
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("new")
                            || extension.equalsIgnoreCase("tre")
                            || extension.equalsIgnoreCase("tree")
                            || extension.equalsIgnoreCase("nwk"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean accept(File dir, String name) {
        return accept(new File(dir, name));
    }

    /**
     * @return description of file matching the filter
     */
    public String getDescription() {
        return Description;
    }


    /**
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     *         last ".")
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    /**
     * return a document containing the taxa and splits in the case of a multi-labeled tree
     *
     * @return document containing taxa and splits of a multi-labeled tree
     */
    Document convertMultiTree2Splits(PhyloTree tree) throws NotMultiLabeledException, SplitsException {
        if (tree.getNumberOfNodes() < 2)
            throw new NotMultiLabeledException();

        Node aLeaf = null; // some leaf
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext())
            if (aLeaf == null && v.getDegree() == 1) {
                aLeaf = v;
                break;
            }

        // multiple labels or of the form name.1 name.2 etc
        // name2numbers maps each name to the set of numbers
        // node2nameNumber maps a node to a pair of name and number
        Map<String, BitSet> name2numbers = new HashMap<>();
        NodeArray node2nameNumber = new NodeArray(tree);
        // compute the two maps:
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            String label = tree.getLabel(v);
            if (label != null) {
                String name;
                int number;

                try {
                    int p = label.lastIndexOf(".");
                    name = label.substring(0, p);
                    number = Integer.parseInt(label.substring(p + 1));
                    if (number < 1)
                        throw new Exception("non-positive");
                } catch (Exception ex) { // is single label

                    name = tree.getLabel(v);
                    BitSet numbers = name2numbers.get(name);
                    if (numbers != null)
                        throw new NotMultiLabeledException();
                    name2numbers.put(name, new BitSet());
                    node2nameNumber.put(v, new Pair(name, 0));
                    continue;
                }
                // is multi label
                node2nameNumber.put(v, new Pair(name, number));

                if (name2numbers.get(name) == null)
                    name2numbers.put(name, new BitSet());
                BitSet numbers = name2numbers.get(name);
                if (numbers.get(number))
                    throw new NotMultiLabeledException();
                numbers.set(number);

            }
        }

        // setup a new document:
        int ntax = name2numbers.keySet().size();
        Document doc = new Document();
        // setup taxon set
        doc.setTaxa(new Taxa(ntax));
        Taxa taxa = doc.getTaxa();

        int taxid = 0;

        //check that each name is either single or has numbers 1...k, for some k
        // setup map from names to taxa
        int nonSingleNames = 0;
        for (String name : name2numbers.keySet()) {
            BitSet numbers = name2numbers.get(name);
            for (int i = 1; i <= numbers.cardinality(); i++)
                if (!numbers.get(i))
                    throw new NotMultiLabeledException();
            if (numbers.cardinality() > 0)
                nonSingleNames++;
            taxa.setLabel(++taxid, name);
        }
        if (nonSingleNames == 0)
            throw new NotMultiLabeledException();

        // nodes seen on other side of edge
        EdgeArray edge2SeparatedNodes = new EdgeArray(tree);
        // recursively  compute:
        determineSeparatedNodesRec(aLeaf, aLeaf.getFirstAdjacentEdge(), tree, edge2SeparatedNodes);

        // prepare splits
        doc.setSplits(new Splits());
        Splits splits = doc.getSplits();
        splits.clear();
        splits.setNtax(taxa.getNtax());

        // keep track of splits that we have already seen:
        Set<TaxaSet> seen = new HashSet<>();

        // for each edge, compute all induced splits
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            // count how many times each name occurs on the other side of e
            Map<String, Integer> names2count = new HashMap<>();
            NodeSet reachableLabeledNodes = (NodeSet) edge2SeparatedNodes.get(e);
            for (Node reachableLabeledNode : reachableLabeledNodes) {
                Pair pair = (Pair) node2nameNumber.get(reachableLabeledNode);
                String name = (String) pair.getFirst();
				names2count.putIfAbsent(name, 0);
				names2count.put(name, names2count.get(name) + 1);
            }

            // determine the set of taxa that are on one side or both sides of the split
            TaxaSet oneSide = new TaxaSet();
            TaxaSet bothSides = new TaxaSet();
            for (int t = 1; t <= taxa.getNtax(); t++) {
                String tname = taxa.getLabel(t);
                int cardOriginalNumbers = name2numbers.get(tname).cardinality();
                if (cardOriginalNumbers > 0 && names2count.get(tname) != null
                        && names2count.get(tname) > 0
                        && names2count.get(tname) < cardOriginalNumbers)
                    bothSides.set(t);
                else if (names2count.get(tname) != null && names2count.get(tname) > 0)
                    oneSide.set(t);

            }
            // taxon on other side of split:
            TaxaSet otherSide = new TaxaSet();
            otherSide.or(oneSide);
            otherSide.or(bothSides);
            otherSide = otherSide.getComplement(taxa.getNtax());
            if (oneSide.intersects(otherSide))
                throw new SplitsException("sides intersect");

            // there are five configurations to consider:
            // 1. bothSides=empty, add oneSide/otherSide
            TaxaSet split1 = null, split2 = null;
            if (bothSides.cardinality() == 0) {
                split1 = oneSide;

            }
            // 2. oneSide, otherSide, bothSides all non-empty
            if (oneSide.cardinality() != 0 && otherSide.cardinality() != 0 && bothSides.cardinality() != 0) {
                split1 = oneSide;
                TaxaSet oneSideBothSides = new TaxaSet();
                oneSideBothSides.or(oneSide);
                oneSideBothSides.or(bothSides);
                split2 = oneSideBothSides;
            }
            // 3. oneSide empty, otherSide non empty, must have bothSides non empty
            if (oneSide.cardinality() == 0 && otherSide.cardinality() != 0) {
                if (bothSides.cardinality() == 0)
                    throw new SplitsException("Case 3: bothSides=0");
                split1 = bothSides;
            }
            // 4. oneSide non empty, otherSide empty, must have bothSides non empty
            if (oneSide.cardinality() != 0 && otherSide.cardinality() == 0) {
                if (bothSides.cardinality() == 0)
                    throw new SplitsException("Case 4: bothSides=0");
                split1 = bothSides;
            }
            // 5. oneSide empty, otherSide empty, warn that we can't do this
            if (oneSide.cardinality() == 0 && otherSide.cardinality() == 0) {
                if (bothSides.cardinality() == 0)
                    throw new SplitsException("Case 5: bothSides=0");
                new Alert("Tree contains edge that separates set B from B, will skip");
            }
            if (split1 != null && !seen.contains(split1)) {
                splits.add(split1);
                seen.add(split1);
                seen.add(split1.getComplement(taxa.getNtax()));
            }
            if (split2 != null && !seen.contains(split2)) {
                splits.add(split2);
                seen.add(split2);
                seen.add(split2.getComplement(taxa.getNtax()));
            }

            /* THIS CODE PRODUCES ALL POSSIBLE SPLITS FOR AN SEPARATING EDGE

            if((oneSide.cardinality()==0 || otherSide.cardinality()==0) && bothSides.cardinality()>0
            && bothSides.cardinality()<taxa.getNtax())
                splits.add(bothSides);
            else
            {
                // generate all possible variants, but only for upto 6 taxa
                int card=bothSides.cardinality();
                if(card>6)
                    throw new SplitsException("Too many duplicate taxa spanning a given edge: "+card);
                int[] bothSidesArray=new int[card];
                int pos=0;
                for(int t=bothSides.getBits().nextSetBit(0);t>0;t=bothSides.getBits().nextSetBit(t+1) )
                    bothSidesArray[pos++]=t;
                int top=(1<<card);
                TaxaSet subSet=new TaxaSet();
                for(int bits=0;bits<top;bits++)
                {
                      for(int i=0;i<card;i++)
                      {
                          if(((1<<i) & bits)!=0)
                          subSet.set(bothSidesArray[i]);
                      }
                    subSet.or(oneSide);
                    splits.add(subSet);
                    subSet.clear();
                }
            }
            */
        }

        return doc;
    }

    /**
     * for an edge e=(v,w), compute all nodes that can be reached from w=e.opposite(v) without using e
     *
     */
    private void determineSeparatedNodesRec(Node v, Edge e, PhyloTree tree, EdgeArray edge2labels) {
        NodeSet set = new NodeSet(tree);
        Node w = v.getOpposite(e);

        if (tree.getLabel(w) != null)
            set.add(w);

        for (Edge f = w.getFirstAdjacentEdge(); f != null; f = w.getNextAdjacentEdge(f)) {
            if (f != e) {
                determineSeparatedNodesRec(w, f, tree, edge2labels);
                set.addAll((NodeSet) edge2labels.get(f));
            }
        }
        edge2labels.put(e, set);
    }

    public boolean getOptionConvertMultiLabeledTree() {
        return optionConvertMultiLabeledTree;
    }

    public void setOptionConvertMultiLabeledTree(boolean optionConvertMultiLabeledTree) {
        this.optionConvertMultiLabeledTree = optionConvertMultiLabeledTree;
    }
}
