/**
 * PaupTreeUtils.java 
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
package splitstree.algorithms.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import splitstree.core.TaxaSet;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;

import java.util.Arrays;


/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Aug 9, 2005
 * Time: 5:19:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PaupTreeUtils {

    /**
     * @param q
     * @return Leftmost leaf in subtree below q
     */
    public static PaupNode leftmostLeaf(PaupNode q) {
        PaupNode p = q;
        while (!p.isLeaf())
            p = p.getFirstChild();
        return p;
    }

    /**
     * PostOrder traversal (parents after children)
     *
     * @param q
     * @return Next node in a post order traversal, or null if there are none
     */
    public static PaupNode nextPost(PaupNode q) {
        if (q.getNextSib() != null)
            return leftmostLeaf(q.getNextSib());
        else
            return q.getPar();
    }

    /**
     * Preorder traversal (parents before children)
     *
     * @param v
     * @return Next node in a pre order traversal, or null if there are none
     */
    public static PaupNode nextPre(PaupNode v) {
        return nextPre(v, null);
    }

    /**
     * Preorder traversal of a subtree
     *
     * @param v
     * @param root
     * @return Next node in a pre-order traversal of the subtree below (and including) root
     */
    static public PaupNode nextPre(PaupNode v, PaupNode root) {
        if (v.getFirstChild() != null)
            return v.getFirstChild();
        PaupNode p = v;
        while (p != root && p.getNextSib() == null)
            p = p.getPar();
        if (p == root)
            return null;
        else
            return p.getNextSib();
    }

    /**
     * Fills in the fastPre and fastPost fields for the nodes in the tree.
     * These will become invalid if the tree is changed.
     * One day, these will be incorporated into a proper Java type structure.
     *
     * @param root
     */
    static public void updateFastPrePost(PaupNode root) {
        PaupNode prev = null;
        for (PaupNode v = root.leftmostLeaf(); v != null; v = nextPost(v)) {
            if (prev != null)
                prev.fastNextPost = v;
            prev = v;
        }
        prev = null;
        for (PaupNode v = root; v != null; v = nextPre(v)) {
            if (prev != null)
                prev.fastNextPre = v;
            prev = v;
        }
    }


    /**
     * Takes a phyloTree and a root Node and returns the tree as a PaupNode. If the phyloT has a single
     * node, then returns a single PaupNode. If the root of the phyloTree is at a leaf, then the adjacent
     * edge is subdivided with the root placed on the edge at a distance of 0 from the leaf.
     * <p/>
     * The order of children follows the order in the phyloTree.
     *
     * @param phyloT
     * @param root
     * @return
     * @throws PaupTreeException
     */
    static public PaupNode convert(Taxa taxa, PhyloTree phyloT, Node root) throws PaupTreeException {
        PaupNode v;

        //If the Phylotree has a single node, return a single PaupNode
        if (root.getDegree() == 0) {
            v = new PaupNode();
            String taxaString = phyloT.getLabel(root);
            if (taxaString != null && taxaString.length() > 0) {
                int id = taxa.indexOf(taxaString);
                if (id > 0)
                    v.id = id;
                else
                    throw new PaupTreeException("Couldn't find taxa " + taxaString);
            }
            return v;
        }

        //If the root has degree one, subdivide its incident edge and return a tree with
        //root of degree two.
        if (root.getDegree() == 1) {
            v = new PaupNode();
            Edge e = root.getFirstAdjacentEdge();

            PaupNode lchild = convertRecurse(taxa, phyloT, root, e);
            lchild.length = 0.0;

            PaupNode rchild = convertRecurse(taxa, phyloT, root.getOpposite(e), e);
            rchild.length = phyloT.getWeight(e);

            rchild.attachAsFirstChildOf(v);
            lchild.attachAsFirstChildOf(v);

            return v;
        }

        v = new PaupNode();
        Edge e = root.getFirstAdjacentEdge();

        PaupNode firstchild = convertRecurse(taxa, phyloT, root.getOpposite(e), e);
        firstchild.length = phyloT.getWeight(e);


        v = convertRecurse(taxa, phyloT, root, e);
        firstchild.attachAsFirstChildOf(v);

        return v;
    }

    /**
     * Converts a subtree in the phylotree to a PaupNode tree. The subtree is specified by
     * node pv and the edge connecting the subtree to the rest of the tree. If there are nodes
     * with multiple labels, or internal nodes labelled, a PaupTreeException is thrown.
     *
     * @param phyloT
     * @param pv     Node in the phylotree
     * @return
     * @throws PaupTreeException
     */
    static PaupNode convertRecurse(Taxa taxa, PhyloTree phyloT, Node pv, Edge incomingEdge) throws PaupTreeException {
        PaupNode v = new PaupNode();

        //Check any taxa labels
        String taxaString = phyloT.getLabel(pv);
        if (taxaString != null && taxaString.length() > 0) {
            if (pv.getDegree() > 1)
                throw new PaupTreeException("PaupTree does not support labels on internal nodes");
            int id = taxa.indexOf(taxaString);
            if (id > 0)
                v.id = id;
            else
                throw new PaupTreeException("Couldn't find taxa " + taxaString);
        }
        //Process the children (if any)
        PaupNode lastSib = null;
        for (Edge e = pv.getNextAdjacentEdgeCyclic(incomingEdge); e != incomingEdge; e = pv.getNextAdjacentEdgeCyclic(e)) {
            PaupNode u = convertRecurse(taxa, phyloT, pv.getOpposite(e), e);
            u.length = phyloT.getWeight(e);
            if (lastSib == null)
                u.attachAsFirstChildOf(v);
            else
                u.attachAsNextSibOf(lastSib);
            lastSib = u;
        }

        return v;
    }


    /**
     * Print out the tree (or subtree) in Newick Syntax
     *
     * @param taxa
     * @param p
     * @param printBrLengths Print out the branch lengths
     * @return String of Newick format (without semicolon, and without () if p is a leaf
     */
    static public String getNewick(Taxa taxa, PaupNode p, boolean printBrLengths) {
        String s = "";
        if (p.isLeaf())
            s += taxa.getLabel(p.id);
        else {
            s += "(";
            for (PaupNode q = p.getFirstChild(); q != null; q = q.getNextSib()) {
                s += getNewick(taxa, q, printBrLengths);
                if (q.getNextSib() != null)
                    s += ",";
            }
            s += ")";
        }
        if (printBrLengths && p.getPar() != null)
            s += ":" + p.length;

        return s;
    }

    /**
     * Multiplies every branch length at this node and below by scale.
     *
     * @param root
     * @param scale
     */
    static public void scaleBranchLengths(PaupNode root, double scale) {
        for (PaupNode q = root; q != null; q = nextPre(q, root))
            q.length *= scale;
    }

    static public int getMaxId(PaupNode root) {
        int id = 0;
        for (PaupNode q = root; q != null; q = nextPre(q, root))
            if (q.isLeaf())
                id = Math.max(id, q.id);
        return id;
    }

    /**
     * Given a tree and the number of taxa, returns a splits block containing the splits of the tree.
     *
     * @param v
     * @param ntax
     * @return Splits block
     */
    static public Splits getBinarySplits(PaupNode v, int ntax) {

        //Need to check the case when v has 2 children.
        int nc = v.getNChildren();
        Splits S = new Splits(ntax);
        TaxaSet B = new TaxaSet();
        double length = 0.0;
        for (PaupNode u = v.getFirstChild(); u != null; u = u.getNextSib()) {
            B = getClustersRecurse(u, S);
            if (nc != 2)
                S.add(B, (float) u.length);
            else
                length += u.length;
        }
        if (nc == 2) {
            S.add(B, (float) length);
        }

        return S;
    }

    static private TaxaSet getClustersRecurse(PaupNode v, Splits S) {
        TaxaSet A = new TaxaSet();
        if (v.isLeaf())
            A.set(v.id);
        else
            for (PaupNode u = v.getFirstChild(); u != null; u = u.getNextSib()) {
                TaxaSet B = getClustersRecurse(u, S);
                S.add(B, (float) u.length);
                A.or(B);
            }
        return A;
    }


    /**
     * Removes all white space from String s.
     *
     * @return String s with white space removed
     */
    public static String removeWhiteSpace(String s) {
        int i;
        for (i = 0; i <= s.length() - 1; i++) {
            if (s.charAt(i) == ' ' || s.charAt(i) == '\t'
                    || s.charAt(i) == '\n' || s.charAt(i) == '\r') {
                s = s.substring(0, i) + s.substring(i + 1);
                i--;
            }
        }
        return s;
    }


    /**
     * Removes everythin between '[' and ']' -- except between '[&&NHX' and ']'.
     */
    public static String removeComments(String s) {
        int i,
                j,
                x = 0;
        boolean done;

        for (i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '['
                    && (i > s.length() - 3
                    || !(s.charAt(i + 1) == '&'
                    && s.charAt(i + 2) == '&'
                    && s.charAt(i + 3) == 'N'
                    && s.charAt(i + 4) == 'H'
                    && s.charAt(i + 5) == 'X'))
                    ) {
                j = i;
                i++;
                done = false;
                while (i < s.length() && !done) {
                    if (s.charAt(i) == '[') {
                        x++;
                    } else if (s.charAt(i) == ']') {
                        if (x == 0) {
                            s = s.substring(0, j) + s.substring(i + 1);
                            i = j - 2;
                            done = true;
                        } else {
                            x--;
                        }
                    }
                    i++;
                }
            }
        }
        return s;

    }

    /**
     * Checks whether String s is empty.
     *
     * @return true if empty, false otherwise
     */
    public static boolean isEmpty(String s) {
        return s.length() < 1;
    }

    /**
     * Checks whether number of "(" equals number of ")" in String
     * nh_string potentially representing a Tree in NH or NHX format.
     *
     * @return total number of  open parantheses if no error detected,
     *         -1 for faulty string
     */
    public static int countAndCheckParantheses(String nh_string) {
        int openparantheses = 0, closeparantheses = 0, i;
        for (i = 0; i <= nh_string.length() - 1; i++) {
            if (nh_string.charAt(i) == '(') openparantheses++;
            if (nh_string.charAt(i) == ')') closeparantheses++;
        }
        if (closeparantheses != openparantheses) {
            return -1;
        } else {
            return openparantheses;
        }
    }

    /**
     * Checks the commas of a String nh_string potentially representing a Tree in
     * NH or NHX format. Checks for "()", "(" not preceded by a "("
     * or ",", ",,", "(,", and ",)".
     *
     * @return true if no error detected, false for faulty string
     */
    public static boolean checkCommas(String nh_string) {
        int i;
        for (i = 0; i <= nh_string.length() - 2; i++) {
            if ((nh_string.charAt(i) == '('
                    && nh_string.charAt(i + 1) == ')') ||
                    (nh_string.charAt(i) != ','
                            && nh_string.charAt(i) != '('
                            && nh_string.charAt(i + 1) == '(') ||
                    (nh_string.charAt(i) == ','
                            && nh_string.charAt(i + 1) == ',') ||
                    (nh_string.charAt(i) == '('
                            && nh_string.charAt(i + 1) == ',') ||
                    (nh_string.charAt(i) == ','
                            && nh_string.charAt(i + 1) == ')')
                    ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes length of path to the leftmost leaf.
     *
     * @param v Node in a tre
     * @return length of path to leftmost leaf
     */
    public static double getNodeHeight(PaupNode v) {
        double height = 0.0;
        for (PaupNode x = v; !x.isLeaf(); x = x.getFirstChild())
            height += x.getFirstChild().length;
        return height;
    }


    public static double[] nodeHeights(PaupNode T) {


        int ntax = 0;
        for (PaupNode v = leftmostLeaf(T); v != null; v = nextPost(v)) {
            if (v.isLeaf())
                ntax++;
        }


        double[] heights = new double[ntax];
        int index = 1;
        for (PaupNode v = leftmostLeaf(T); v != null; v = nextPost(v)) {
            if (v.isLeaf()) {
                double height = 0.0;
                for (PaupNode x = v; (x.getPar() != null) && (x == x.getPar().getFirstChild()); x = x.getPar()) {
                    height = height + x.length;      //Height of parent
                    heights[index] = height;
                    index++;
                }
            }
        }
        Arrays.sort(heights);
        return heights;

    }


}
