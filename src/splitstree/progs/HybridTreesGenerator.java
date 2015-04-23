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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CommandLineOptions;
import splitstree.core.Document;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * DESCRIPTION
 *
 * @author huson
 *         Date: 18-Sep-2004
 */
public class HybridTreesGenerator {
    static final int ONLY_P = 1;
    static final int ONLY_Q = 2;
    static final int BOTH_PQ = 0;

    /**
     * The main program.
     * Usage: Reorder -i infile -o outfile
     *
     * @param args the list of options
     */
    public static void main(String[] args) throws Exception {

        CommandLineOptions options = new CommandLineOptions(args);
        options.setDescription
                ("GenerateReticulationTrees - Generate all possible trees for a reticulation scenario");
        String fname = options.getMandatoryOption("-i", "input file", "");
        boolean describe = options.getOption("-d", "describe input format", true, false);
        boolean allPorAllQ = options.getOption("-a", "generate only two trees, all P and all Q", true, false);
        options.done();

        if (describe) {
            String description = "Input is a list of trees. The first tree T is call the base tree. "
                    + "Subsequent trees T_1, T_2, ... are called pruned subtrees. Each pruned tree T_i contains "
                    + "precisely one attachment node labeled h_i. For each such attachment node h_i, the base tree T "
                    + "must contain precisely two copies, h_iP and hi_Q that indicate the two alternative places "
                    + "where T_i attaches to T. For all other taxa we have X_i n X is empty and X_i n X_j is empty.";
            System.out.println(Basic.toMessageString(description));

        }

        Reader r = new FileReader(new File(fname));
        if (r == null)
            throw new IOException("Failed to open file: " + fname);

        splitstree.externalIO.imports.NewickTree newick = new splitstree.externalIO.imports.NewickTree();

        String nexus = newick.apply(r);
        System.err.println("nexus: " + nexus);
        Document doc = new Document();
        doc.execute(nexus);
        Taxa taxa = doc.getTaxa();
        Trees trees = doc.getTrees();
        System.err.println("trees: " + trees);

        // find all nodes labeled hP and hQ, point hP and hQ to their attachment node
        // and then delete the original hP and hQ node
        Node[] hP = new Node[taxa.getNtax() + 1];
        Node[] hQ = new Node[taxa.getNtax() + 1];
        double[] hPwgt = new double[taxa.getNtax() + 1];
        double[] hQwgt = new double[taxa.getNtax() + 1];


        PhyloTree tree = trees.getTree(1); // the base tree
        List toDelete = new LinkedList();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            String label = tree.getLabel(v);
            if (label != null && label.charAt(label.length() - 1) == 'P') {
                String name = label.substring(0, label.length() - 1);
                int t = taxa.indexOf(name);
                if (t == -1)
                    throw new Exception("Couldn't find taxon " + name);
                if (tree.getDegree(v) != 1)
                    throw new Exception("hP taxon " + name + " on node of degree " + tree.getDegree(v));
                Edge e = tree.getFirstAdjacentEdge(v);
                hP[t] = tree.getOpposite(v, e);
                hPwgt[t] = tree.getWeight(e);
                toDelete.add(v);
            } else if (label != null && label.charAt(label.length() - 1) == 'Q') {
                String name = label.substring(0, label.length() - 1);
                int t = taxa.indexOf(name);
                if (t == -1)
                    throw new Exception("Couldn't find taxon " + name);
                if (tree.getDegree(v) != 1)
                    throw new Exception("hQ taxon " + name + " on node of degree " + tree.getDegree(v));
                Edge e = tree.getFirstAdjacentEdge(v);
                hQ[t] = tree.getOpposite(v, e);
                hQwgt[t] = tree.getWeight(e);
                toDelete.add(v);
            }
        }
        for (Object aToDelete : toDelete) tree.deleteNode((Node) aToDelete);

        // copy pruned trees into baseTree:
        for (int i = 2; i <= trees.getNtrees(); i++) {
            tree.add(trees.getTree(i));
        }

        // make map from taxon names to nodes:
        Map name2node = new HashMap();

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            String name = tree.getLabel(v);
            if (name != null) {
                if (name2node.containsKey(name))
                    throw new Exception("multiple occurrence of taxon label: " + name);
                name2node.put(name, v);
            }
        }


        Node[] attachNodes = new Node[taxa.getNtax() + 1];
        // remove node labeled h from prune tree and set attachNodes
        for (int t = 1; t <= taxa.getNtax(); t++) {
            if (hP[t] != null && hQ[t] != null) {
                String name = taxa.getLabel(t);
                Node v = (Node) name2node.get(name);
                if (tree.getDegree(v) != 1)
                    throw new Exception("Attachment taxon " + name + " on node of degree " + tree.getDegree(v));
                Edge e = tree.getFirstAdjacentEdge(v);
                Node w = tree.getOpposite(v, e);
                attachNodes[t] = w;
                tree.deleteNode(v);
            }
        }

        if (allPorAllQ) {
            // generate only two trees: P only and then Q only
            generateTrees(1, hP, hPwgt, hQ, hQwgt, attachNodes, tree, ONLY_P);
            generateTrees(1, hP, hPwgt, hQ, hQwgt, attachNodes, tree, ONLY_Q);
        } else {
            // generate all possible trees:
            generateTrees(1, hP, hPwgt, hQ, hQwgt, attachNodes, tree, BOTH_PQ);
        }
    }

    private static void generateTrees(int t, Node[] hP, double[] hPwgt, Node[] hQ, double[] hQwgt,
                                      Node[] prunedAttachNodes, PhyloTree tree, int usePQ) {
        // get next attachment node:
        while (t < hP.length && (hP[t] == null || hQ[t] == null)) {
            t++;
        }

        if (t == hP.length) // processed all SPRs, print
        {
            System.out.println(tree.toString());
        } else {
            Node a = prunedAttachNodes[t];
            // attach to P node:
            if (usePQ == ONLY_P || usePQ == BOTH_PQ) {
                Edge e = tree.newEdge(hP[t], a);
                tree.setWeight(e, hPwgt[t]);
                generateTrees(t + 1, hP, hPwgt, hQ, hQwgt, prunedAttachNodes, tree, usePQ);
                tree.deleteEdge(e);
            }
            if (usePQ == ONLY_Q || usePQ == BOTH_PQ) {
                // attach to Q node:
                Edge e = tree.newEdge(hQ[t], a);
                tree.setWeight(e, hQwgt[t]);
                generateTrees(t + 1, hP, hPwgt, hQ, hQwgt, prunedAttachNodes, tree, usePQ);
                tree.deleteEdge(e);
            }
        }
    }
}
