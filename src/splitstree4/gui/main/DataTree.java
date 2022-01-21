/*
 * DataTree.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.main;

import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Taxa;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * ad-hoc attempt to put all datatree stuff into one class
 * Daniel Huson and David Bryant, 11.2009
 */
public class DataTree extends JTree {
    // Things used for keeping the right branches in the JTree open
    final private Director dir;
    final private HashMap<String, Boolean> expandedRows = new HashMap<>();
    final private MyTreeExpansionListener treeExpansionListener;

    /**
     * constructor
     *
     * @param dir
     */
    public DataTree(Director dir) {
        super();
        setModel(new DefaultTreeModel(new DefaultMutableTreeNode("#Nexus")));
        this.dir = dir;
        treeExpansionListener = new MyTreeExpansionListener();
        setRowHeight(0); //Dealing with a MAC problem. (No effect in Windows)
        setCellRenderer(new NexusCellRenderer(dir));

    }

    /**
     * for some reason we need to call this
     */
    public void resetMyTreeExpansionListener() {
        if (getTreeExpansionListeners() != null)
            removeTreeExpansionListener(treeExpansionListener);
        addTreeExpansionListener(treeExpansionListener);
    }

    /**
     * Finds the path in tree as specified by the array of names.
     * Comparison is done using String.equals().
     *
     * @param names is a sequence of names where names[0] is the root and names[i] is a child of names[i-1].
     * @return null if not found.
     */
    public TreePath findByName(String[] names) {
        TreeNode root = (TreeNode) getModel().getRoot();
        return findRec(new TreePath(root), names, 0);
    }

    /**
     * recursively find a path
     *
     * @param parent
     * @param names
     * @param depth
     * @return path
     */
    private TreePath findRec(TreePath parent, String[] names, int depth) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        String name = node.toString();

        // If equal, go down the branch
        if (name.equals(names[depth])) {
            // If at end, return match
            if (depth == names.length - 1) {
                return parent;
            }

            // Traverse children
            if (node.getChildCount() >= 0) {
                for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
                    TreeNode n = e.nextElement();
                    TreePath path = parent.pathByAddingChild(n);
                    TreePath result = findRec(path, names, depth + 1);
                    // Found a match
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        // No match at this branch
        return null;
    }

    /**
     * expand previously expanded nodes
     */
    public void expandAllNodes() {
        for (String name : Document.getListOfBlockNames()) {
            if (dir.getDocument().isValidByName(name)) {
                TreePath path = findByName(new String[]{"#Nexus", name});
                // if (expandedRows.get(path.toString()))
                expandPath(path);
            }
        }
    }

    /**
     * collapse all nodes
     */
    public void collapseAllTreeData() {

        // loop through all possible "paths"
        for (String name : Document.getListOfBlockNames()) {
            if (dir.getDocument().isValidByName(name)) {
                // Find the path (regardless of visibility) that matches the
                // specified sequence of names
                TreePath path = findByName(new String[]{"#Nexus", name});
                collapsePath(path);
            }
        }
    }

    /**
     * Update the data view tab
     */
    public void updateDataTreeView() {
        // System.err.println("UPDATE");
        DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) getModel().getRoot();
        treeRoot.removeAllChildren();
        final Document doc = dir.getDocument();

        if (doc.getTopComments() != null) {
            String name = "Comments";
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(name);
            DefaultMutableTreeNode desc;
            desc = new DefaultMutableTreeNode(doc.getTopComments());

            // Construct the expandedRows HashMap if necessary
            // System.err.println("[Data, [" + name + "]]" + " just got his value updated to false");
            expandedRows.putIfAbsent("[#Nexus, [" + name + "]]", false);
            child.add(desc);
            treeRoot.add(child);
        }
        // show the selected items:
        if (doc.isValidByName(Taxa.NAME)) {
            for (final String name : Document.getListOfBlockNames()) {

                if (dir.getDocument().isValidByName(name)) {
                    // String name2 = "[" + name + "]";
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(name);
                    treeRoot.add(child);

                    DefaultMutableTreeNode desc = null;
                    try {
                        StringWriter w = new StringWriter();
                        var block = doc.getBlockByName(name);
                        if (block instanceof Assumptions)
                            ((Assumptions) block).write(w, doc);
                        else
                            block.write(w, doc.getTaxa());
                        desc = new DefaultMutableTreeNode(w.toString());
                    } catch (IOException ignored) {
                    }

                    // Construct the expandedRows HashMap if necessary
                    expandedRows.putIfAbsent("[#Nexus, [" + name + "]]", false);
                    if (desc != null)
                        child.add(desc);
                }
            }

            ((DefaultTreeModel) treeModel).reload();
            validate();
            // System.err.println("After reload");
            for (String name : Document.getListOfBlockNames()) {
                // i.e. if it is in the nexus file
                if (dir.getDocument().isValidByName(name)) {
                    // Find the path (regardless of visibility) that matches the
                    // specified sequence of names
                    TreePath path = findByName(new String[]{"#Nexus", name});

                    for (String pathString : expandedRows.keySet()) {
                        if (expandedRows.get(pathString) && path.toString().equals(pathString)) {
                            expandPath(path);
                        }
                    }
                }
            }

        }
    }

    final class MyTreeExpansionListener implements TreeExpansionListener {

        public void treeExpanded(TreeExpansionEvent e) {
            // Set the correct Path in the JTree to true
            for (String path : expandedRows.keySet()) {
                if (e.getPath().toString().equals(path)) {
                    expandedRows.put(path, true);
                }
            }
        }

        public void treeCollapsed(TreeExpansionEvent e) {
            // Set the correct Path in the JTree to true
            for (String path : expandedRows.keySet()) {
                String aPath = e.getPath().toString();
                if (aPath.equals(path)) {
                    expandedRows.put(path, false);
                }
            }
        }
    }
}
