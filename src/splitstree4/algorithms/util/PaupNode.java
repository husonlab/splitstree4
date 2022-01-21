/*
 * PaupNode.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.util;

import splitstree4.nexus.Taxa;

import java.util.Stack;


/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Aug 9, 2005
 * Time: 4:47:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PaupNode {
    // Links
    private PaupNode par;   //Parent node
    private PaupNode firstChild;  //first child
    private PaupNode nextSib;  //next sibling

    //Basic Info
    public int id;  //id. If this is a leaf, then id of taxa. Otherwise an index id.
    public double length;  //length of the branch above this node

    //Data
    public Object data;

    public PaupNode fastNextPre; //next node on a pre order traversal... not reliable after tree edits
    public PaupNode fastNextPost; //next node on a post order traversal... not reliable after tree edits


    //Constructor
    public PaupNode() {
        par = firstChild = nextSib = null;
        id = 0;
    }

    //Getters. No setters since we don't want to handle pointers manually

    /**
     * Returns parent of this node, or null if there is none
     *
     * @return parent
     */
    public PaupNode getPar() {
        return par;
    }

    /**
     * Returns the first child, or null if this node has no children
     *
     * @return first child
     */
    public PaupNode getFirstChild() {
        return firstChild;
    }

    /**
     * Returns next sibling after this one, or null if this is the last sibling
     *
     * @return next sibling
     */
    public PaupNode getNextSib() {
        return nextSib;
    }

    /**
     * Returns true if this node has no children. Equivalent to (getFirstChild()==null)
     *
     * @return boolean. True if it is a leaf.
     */
    public boolean isLeaf() {
        return firstChild == null;
    }

    /**
     * Returns true if
     *
     * @return boolean. True if it is the root (no parent)
     */
    public boolean isRoot() {
        return getPar() == null;
    }

    //Manipulators. Note... these are the only way that the tree structures can be manipulated.

    /**
     * Removes this node from the tree, patching up the links in the tree so that siblings and
     * parents are connected properly. Sets the parent of this node to null
     */
    public final void detachFromParent() {
        if (par == null)
            return;
        if (this == par.firstChild)
            par.firstChild = nextSib;
        else {
            PaupNode sibToLeft;
            for (sibToLeft = par.firstChild; sibToLeft.nextSib != this; sibToLeft = sibToLeft.nextSib) {
            }
            sibToLeft.nextSib = nextSib;
        }
        par = null;
        nextSib = null;
    }

    /**
     * Detaches the node from its tree, and moves it so that it is the first child of newParent.
     * Will do nothing if this=newParent (maybe an error should be generated?)
     *
     * @param newParent
     */

    public void attachAsFirstChildOf(PaupNode newParent) {
        if (this == newParent)
            return;
        if (par != null)
            detachFromParent();
        par = newParent;
        nextSib = newParent.firstChild;
        newParent.firstChild = this;
    }

    /**
     * Detaches the node from its tree, and moves it so that it is the first child of newParent.
     * Will do nothing if this=newParent (maybe an error should be generated?)
     *
     * @param newParent
     */

    public void attachAsFirstChildOf(PaupNode newParent, double length) {
        if (this == newParent)
            return;
        if (par != null)
            detachFromParent();
        par = newParent;
        nextSib = newParent.firstChild;
        newParent.firstChild = this;
        this.length = length;
    }

    /**
     * Detaches the node from its tree, and moves it so that it is the next sibling of newSibling
     *
     * @param newSibling
     */
    public final void attachAsNextSibOf(PaupNode newSibling) {
        if (par != null)
            detachFromParent();
        par = newSibling.par;
        nextSib = newSibling.nextSib;
        newSibling.nextSib = this;
    }

    /**
     * Contracts the edge above this node. In effect, this node and its parent are identified, though
     * this node replaces the parent node. The length of the edge above this node is increased by the length
     * of the edge above the parent.
     */
    public final void contractNode() {
        /*Contraction works in three steps:
           First we move the siblings before me to be my children before my existing children
          Second we move the siblings after me to be my children after my existing children
           At this point, my parent will have no other children except me.
           Third we make me my parents sibling, and remove my parent. */


        if (par == null)
            return;

        if (par.firstChild != this) {
            //First copy firstChild node of parent to me as leftmost child
            PaupNode p = par.firstChild;
            p.attachAsFirstChildOf(this);
            //now copy any remaining siblings that come before me
            while (par.firstChild != this) {
                PaupNode q = par.firstChild;
                q.attachAsNextSibOf(p);
                p = q;
            }
        }

        //Locate the last child of me
        PaupNode p = firstChild;
        if (p != null)
            while (p.nextSib != null)
                p = p.nextSib;

        //Now move any siblings after me to after my last child
        while (nextSib != null) {
            PaupNode q = nextSib;
            if (p != null)
                q.attachAsNextSibOf(p);
            else //Current node has not children
                q.attachAsFirstChildOf(this);
            p = q;
        }

        //the parent will now be a degree two node. We will contract it, and set the lengths.
        p = par;
        length += par.length;
        this.attachAsNextSibOf(p);
        p.detachFromParent();

    }

    /**
     * Returns a deep copy of the current node and all of its descendents.
     * Data fields are copied by reference only.
     * fastPre and fastPost are re-initialised.
     *
     * @return Newly allocated copy of current node and all of its descendents.
     */
    public PaupNode deepCopyTree() {
        PaupNode T = deepCopyTreeRecurse();
        PaupTreeUtils.updateFastPrePost(T);
        return T;
    }

    /**
     * Returns a deep copy of the current node and all of its descendents.
     * Data fields are copied by reference only.
     * fastPre and fastPost fields are set to null
     *
     * @return Newly allocated copy of current node and all of its descendents.
     */
    private PaupNode deepCopyTreeRecurse() {


        PaupNode newNode;

        newNode = new PaupNode();
        newNode.id = id;
        newNode.length = length;
        newNode.data = data;
        newNode.fastNextPost = newNode.fastNextPre = null;

        PaupNode lastSib = null;

        for (PaupNode child = getFirstChild(); child != null; child = child.getNextSib()) {
            PaupNode newChild = child.deepCopyTree();
            if (newChild == null)
                return null;
            if (lastSib == null)
                newChild.attachAsFirstChildOf(newNode);
            else
                newChild.attachAsNextSibOf(lastSib);
            lastSib = newChild;
        }

        return newNode;
    }

    //Navigation and traversals

    /**
     * @return Leftmost leaf in subtree below q
     */
    public PaupNode leftmostLeaf() {
        PaupNode p = this;
        while (!p.isLeaf())
            p = p.getFirstChild();
        return p;
    }

    /**
     * PostOrder traversal (parents after children)
     *
     * @return Next node in a post order traversal, or null if there are none
     */
    public PaupNode nextPost() {
        if (getNextSib() != null)
            return getNextSib().leftmostLeaf();
        else
            return getPar();
    }

    /**
     * Preorder traversal (parents before children)
     *
     * @return Next node in a pre order traversal, or null if there are none
     */
    public PaupNode nextPre() {
        return nextPre(null);
    }

    /**
     * Preorder traversal of a subtree
     *
     * @param root
     * @return Next node in a pre-order traversal of the subtree below (and including) root, or null
     * if there are none
     */
    public PaupNode nextPre(PaupNode root) {
        if (getFirstChild() != null)
            return getFirstChild();
        PaupNode p = this;
        while (p != root && p.getNextSib() == null)
            p = p.getPar();
        if (p == root)
            return null;
        else
            return p.getNextSib();
    }

    /**
     * Returns the last child of the node. Currently does this by looping through the children
     * This could be made constant time if the children we double linked.
     *
     * @return last child of the node, or null if this node has no children
     */
    public PaupNode getLastChild() {
        if (isLeaf())
            return null;
        PaupNode p = getFirstChild();
        while (p.getNextSib() != null)
            p = p.getNextSib();
        return p;
    }

    /**
     * Returns the sibling *before* this one, by starting with the first node and looping.
     *
     * @return previous sibling, or null if this node has no siblings or if it is the first sibling
     * itself.
     */
    public PaupNode getPrevSib() {
        PaupNode par = getPar();
        if (par == null || par.getFirstChild() == this)
            return null;
        PaupNode p = par.getFirstChild();
        while (p.getNextSib() != this)
            p = p.getNextSib();
        return p;
    }

    public int getNChildren() {
        if (isLeaf())
            return 0;
        else {
            int c = 0;
            for (PaupNode u = getFirstChild(); u != null; u = u.getNextSib())
                c++;
            return c;
        }
    }
    //OUTPUT

    /**
     * Print out the tree (or subtree) in Newick Syntax
     *
     * @param taxa
     * @param printBrLengths Print out the branch lengths
     * @return String of Newick format (without semicolon, and without () if p is a leaf
     */
    public String getNewick(Taxa taxa, boolean printBrLengths) {
        String s = "";
        if (isLeaf())
            s += taxa.getLabel(id);
        else {
            s += "(";
            for (PaupNode q = getFirstChild(); q != null; q = q.getNextSib()) {
                s += q.getNewick(taxa, printBrLengths);
                if (q.getNextSib() != null)
                    s += ",";
            }
            s += ")";
        }
        if (printBrLengths && getPar() != null)
            s += ":" + length;

        return s;
    }

    /**
     * Prints out a description of the tree objecet. THis is the same as Newick, except
     * that the id, rather than the taxa name, is output, and branch lengths are printed
     * as full doubles.
     *
     * @return String
     */
    public String writeTreeDescription() {
        String s = "";
        if (isLeaf())
            s += "" + id;
        else {
            s += "(";
            for (PaupNode q = getFirstChild(); q != null; q = q.getNextSib()) {
                s += q.writeTreeDescription();
                if (q.getNextSib() != null)
                    s += ",";
            }
            s += ")";
        }
        if (getPar() != null)
            s += ":" + length;

        return s;
    }

    static class StringIterator {
        String s;
        private int index;

        public StringIterator(String s) {
            this.s = s;
            index = 0;
        }

        public char getNext() {
            char ch = s.charAt(index);
            index++;
            return ch;
        }

        public char peek() {
            return s.charAt(index);
        }

        public boolean hasNext() {
            return index < s.length();
        }

        public int getNextInteger() {
            String num = "";
            while (hasNext() && Character.isDigit(s.charAt(index))) {
                num += "" + getNext();
            }

            return (Integer.parseInt(num));
        }

        public double getNextDouble() {
            String num = "";
            while (hasNext() && (Character.isDigit(s.charAt(index)) || (s.charAt(index) == '.') || (s.charAt(index) == 'E') || (s.charAt(index) == '-'))) {
                num += "" + getNext();
            }

            return (Double.parseDouble(num));
        }
    }

    static public PaupNode readTreeDescription(String s) {
        StringIterator iter = new StringIterator(s);
        return readTreeDescriptionRecurse(iter);
    }

    //
    static private PaupNode readTreeDescriptionRecurse(StringIterator s) {
        PaupNode p = new PaupNode();
        char ch;
        if (s.peek() == '(') {
            s.getNext();
            PaupNode leftSib = null;
            for (; ; ) {
                PaupNode child = readTreeDescriptionRecurse(s);
                if (leftSib == null)
                    child.attachAsFirstChildOf(p);
                else
                    child.attachAsNextSibOf(leftSib);
                leftSib = child;

                ch = s.getNext();

                if (ch == ')')
                    break;
            }
        } else {
            p.id = s.getNextInteger();
        }
        if (s.hasNext()) {
            ch = s.getNext();
            p.length = s.getNextDouble();
        }
        return p;
    }

    /**
     * Tree constructor which constructs a Tree from String nh_string.
     * The tree is considered unrooted if the deepest node has more than
     * two children, otherwise it is considered rooted.
     *
     * @param nh_string String in New Hampshire (NH) or New Hampshire X (NHX) format
     */
    private static PaupNode readTree(String nh_string) throws Exception {
        boolean first;
        int i;
        int nodeId = 0;
        String internal_node_info = "",
                A,
                B,
                next;
        StringBuffer sb;
        Stack st;

        nh_string = PaupTreeUtils.removeWhiteSpace(nh_string);

        nh_string = PaupTreeUtils.removeComments(nh_string);

        // Remove anything before first "(", unless tree is just one node.
        if (!PaupTreeUtils.isEmpty(nh_string) && nh_string.charAt(0) != '('
                && nh_string.contains("(")) {
            int x = nh_string.indexOf("(");
            nh_string = nh_string.substring(x);
        }

        // If ';' at end, remove it.
        if (!PaupTreeUtils.isEmpty(nh_string)
                && nh_string.endsWith(";")) {
            nh_string = nh_string.substring(0, nh_string.length() - 1);
        }

        if (PaupTreeUtils.countAndCheckParantheses(nh_string) <= -1) {
            String message = "Tree: Tree( String ): Error in NHX format: ";
            message += "open parantheses != close parantheses.";
            throw new Exception(message);
        }
        if (!PaupTreeUtils.checkCommas(nh_string)) {
            String message = "Tree: Tree( String ): Error in NHX format: ";
            message += "Commas not properly set.";
            throw new Exception(message);
        }

        // Conversion from nh string to PaupNode.

        // Empty Tree.
        if (nh_string.length() < 1) {
            //TODO: DO something for the empty tree
            return null;
        }

        // Check whether nh string represents a tree with more than
        // one node or just a single node.
        // More than one node.
        else if (nh_string.contains("(")) {

            A = B = "";

            st = new Stack();

            i = 0;

            while (i <= nh_string.length() - 1) {
                if (nh_string.charAt(i) == ',') {
                    st.push(",");
                }
                if (nh_string.charAt(i) == '(') {
                    st.push("(");
                }
                if (nh_string.charAt(i) != ','
                        && nh_string.charAt(i) != '('
                        && nh_string.charAt(i) != ')') {
                    sb = new StringBuffer("");
                    while (i <= nh_string.length() - 1
                            && nh_string.charAt(i) != ')'
                            && nh_string.charAt(i) != ',') {
                        sb.append(nh_string.charAt(i));
                        i++;
                    }
                    i--;
                    st.push(sb.toString());
                }

                // A ")" calls for connection of one kind or another.
                if (nh_string.charAt(i) == ')') {
                    // If present, read information for internal node.
                    if (i <= nh_string.length() - 2
                            && nh_string.charAt(i + 1) != ')'
                            && nh_string.charAt(i + 1) != ',') {
                        i++;
                        sb = new StringBuffer("");
                        while (i <= nh_string.length() - 1
                                && nh_string.charAt(i) != ')'
                                && nh_string.charAt(i) != ',') {
                            sb.append(nh_string.charAt(i));
                            i++;
                        }
                        i--;
                        internal_node_info = sb.toString();
                    }

                    first = true;

                    // Parsing between two parantheses.
                    label:
                    do {

                        A = st.pop().toString();

                        if (st.empty()) {
                            //connectInternal( internal_node_info );
                            break;
                        }

                        B = st.pop().toString();

                        if (st.empty()) {
                            switch (A) {
                                case "(":
                                    //connectInternal( internal_node_info );
                                    st.push("(");
                                    break;
                                case ",":
                                    //connectInternal( internal_node_info );
                                    break label;
                                default:
                                    //addNodeAndConnect( A, internal_node_info );
                                    break label;
                            }
                        } else {
                            next = st.peek().toString();

                            if (!next.equals("(") && B.equals(",")
                                    && !A.equals(",")) {
                                if (first && !next.equals(",")) {
                                    //addNode( A );
                                    st.push(",");
                                } else {
                                    //addNodeAndConnect( A, null_distance );
                                }
                                first = false;
                            } else {
                                first = false;

                                if (next.equals(",") && !B.equals(",")
                                        && !B.equals("(")
                                        && A.equals(",")) {
                                    //addNodeAndConnect( B, null_distance );
                                } else if (!next.equals("(")
                                        && B.equals(",") && A.equals(",")) {
                                    //connectInternal( null_distance );
                                    st.push(",");
                                } else if (next.equals("(") && B.equals(",")
                                        && !A.equals("(") && !A.equals(",")) {
                                    //addNodeAndConnect( A, internal_node_info );
                                    st.pop();
                                    break;
                                } else if (next.equals("(") && !B.equals("(")
                                        && !B.equals(",") && A.equals(",")) {
                                    //addNodeAndConnect( B, internal_node_info );
                                    st.pop();
                                    break;
                                } else if (next.equals("(")
                                        && B.equals(",") && A.equals(",")) {
                                    //connectInternal( null_distance );
                                    //connectInternal( internal_node_info );
                                    st.pop();
                                    break;
                                } else if (next.equals(",") && B.equals("(")
                                        && !A.equals("(") && !A.equals(",")) {
                                    //addNodeAndConnect( A, internal_node_info );
                                    break;
                                } else if (next.equals(",")
                                        && B.equals("(") && A.equals(",")) {
                                    //connectInternal( internal_node_info );
                                    break;
                                } else if (next.equals("(")
                                        && B.equals("(") && !A.equals("(")) {
                                    if (A.equals(",")) {
                                        //connectInternal( internal_node_info );
                                    } else {
                                        //addNodeAndConnect( A,internal_node_info) ;
                                    }
                                    break;
                                } else if (A.equals("(")
                                        && ((next.equals("(") && B.equals(","))
                                        || (next.equals(",") && B.equals("("))
                                        || (next.equals("(") && B.equals("(")))) {
                                    //connectInternal( internal_node_info );
                                    st.push("(");
                                    break;
                                }

                            }

                        } // end of else (st is not empty).

                    } while (true);


                }

                i++;

            } // End of while loop going through nh_string.
        }

        // Just one node.
        // Conversion from nh string to tree object with one node.
        else {
            //TODO: DO something for the tree with one node
            //addNode( nh_string );
            //setRooted( true );
        }
        //TODO: Return the tree
        return new PaupNode();
    }

}
