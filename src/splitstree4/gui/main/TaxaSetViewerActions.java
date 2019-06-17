/**
 * TaxaSetViewerActions.java
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
 */
package splitstree4.gui.main;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import splitstree4.core.TaxaSet;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Taxa;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

/**
 * taxa set viewer actions
 * 2008
 */
public class TaxaSetViewerActions {

    private TaxaSetViewer viewer;
    private Director dir;
    private LinkedList allActions;

    public TaxaSetViewerActions(TaxaSetViewer viewer_, Director dir_) {
        this.viewer = viewer_;
        this.dir = dir_;
        this.allActions = new LinkedList();
    }

    /**
     * All the Actions of the window
     */

    // do apply
    private AbstractAction applyAction;

    public AbstractAction getApplyAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = applyAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    doApplyAction(messagePanel, taxaTree);          // do apply

                } catch (Exception ex) {
                    Basic.caught(ex);
                    new Alert(null, "Error : " + ex.getMessage());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply Groups");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return applyAction = action;
    }

    // do apply & close window
    private AbstractAction okAction;

    public AbstractAction getOkAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = okAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    doApplyAction(messagePanel, taxaTree);                // do apply & clos window
                    dir.removeViewer(viewer);
                    viewer.getFrame().dispose();

                } catch (Exception ex) {
                    Basic.caught(ex);
                    new Alert(null, "Error : " + ex.getMessage());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Ok");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Ok");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return okAction = action;
    }


    // draw annotations
    private AbstractAction annotateAction;

    public AbstractAction getAnnotationAction() {
        AbstractAction action = annotateAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

            }
        };
        action.putValue(AbstractAction.NAME, "Draw Annotations");
        allActions.add(action);
        return annotateAction = action;
    }

    //action to optimize the cycle layout
    private AbstractAction cycleOptimisationAction;

    public AbstractAction getOptimizeCycleAction() {
        AbstractAction action = cycleOptimisationAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

            }
        };
        action.putValue(AbstractAction.NAME, "Optimize Cycle");
        allActions.add(action);
        return cycleOptimisationAction = action;
    }


    // close window
    private AbstractAction cancelAction;

    public AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);                  //close window
                viewer.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        allActions.add(action);
        return cancelAction = action;
    }

    // close window
    private AbstractAction closeAction;

    public AbstractAction getCloseAction() {
        if (closeAction != null)
            return closeAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);                   // close window
                viewer.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        allActions.add(action);
        return closeAction = action;
    }

    // create new group and add selected items
    private AbstractAction groupAction;

    public AbstractAction getGroupAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = groupAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doGroupAction(messagePanel, taxaTree);
            }

        };
        action.putValue(AbstractAction.NAME, "group");
        allActions.add(action);
        return groupAction = action;
    }

    // add selected items to an existing group
    private AbstractAction addToExistingGroupAction;

    public AbstractAction getAddToExistingGroupAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = addToExistingGroupAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                TreePath[] selectionTreePaths = taxaTree.getSelectionPaths();

                if (!nodesAreSeleceted(taxaTree)) return;  //no nodes selected

                Vector selectableGroups = getSelectableGroups(selectionTreePaths, taxaTree);   //get available groups

                if (selectableGroups == null) {     //no selectable groups
                    viewer.throwMessage("no selectable groups", Color.RED);
                    return;
                }

                DefaultMutableTreeNode chosenGroup = new DefaultMutableTreeNode();

                chosenGroup = (DefaultMutableTreeNode) JOptionPane.showInputDialog(viewer.getFrame(),
                        "choose group", "choose group",
                        JOptionPane.PLAIN_MESSAGE, null, selectableGroups.toArray(), selectableGroups.elementAt(0));

                if (chosenGroup == null) return; //user cancels action

                TreePath[] newSelectionPaths = new TreePath[taxaTree.getSelectionCount()];

                for (int i = 0; i < taxaTree.getSelectionCount(); i++) { //for all selected nodes
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionTreePaths[i].getLastPathComponent();
                    chosenGroup.add(selectedNode);                          //add node to choosen group
                    newSelectionPaths[i] = new TreePath(selectedNode.getPath());
                }

                taxaTree.setSelectionPaths(newSelectionPaths);      // set new selection path
                removeEmptyGroupsRec((DefaultMutableTreeNode) taxaTree.getModel().getRoot());   // remove empty groups
                viewer.scrollAllPathsToVisible(chosenGroup);             // scroll paths to visible
                taxaTree.updateUI();
            }

        };
        action.putValue(AbstractAction.NAME, "add to existing group");
        allActions.add(action);
        return addToExistingGroupAction = action;
    }

    // rename group
    private AbstractAction RenameGroupAction;

    public AbstractAction getRenameGroupAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = RenameGroupAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                TreePath[] selectionTreePaths = taxaTree.getSelectionPaths();

                if (!nodesAreSeleceted(taxaTree)) return;  //no nodes selected

                for (int i = 0; i < taxaTree.getSelectionCount(); i++) {     //for all selected nodes
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionTreePaths[i].getLastPathComponent();

                    if (selectedNode.getAllowsChildren()) { //rename only group nodes
                        String oldName = (String) selectedNode.getUserObject();
                        String newName = oldName;
                        while (getAllGroups(taxaTree).contains(newName)) {   //ask for group name until new name is unique
                            newName = JOptionPane.showInputDialog(viewer.getFrame(), "please enter new name", selectedNode.getUserObject()); //open dialog to enter new name
                            if (getAllGroups(taxaTree).contains(newName)) {
                                JOptionPane.showMessageDialog(null, "group does already exist", "group does already exist", JOptionPane.ERROR_MESSAGE);
                            } else break;
                        }
                        if (newName == null || newName.equals(""))
                            newName = oldName; //user cancels action (or enter a blank  name), set new name to old name
                        selectedNode.setUserObject(newName);   //apply new name
                    } else viewer.throwMessage("not a group node", Color.RED);
                }
                taxaTree.updateUI();
            }
        };
        action.putValue(AbstractAction.NAME, "rename group");
        allActions.add(action);
        return RenameGroupAction = action;
    }

    // remove items from their group, adding them one level above
    private AbstractAction RemoveFromGroupAction;

    public AbstractAction getRemoveFromGroupAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = RemoveFromGroupAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                TreePath[] selectionTreePaths = taxaTree.getSelectionPaths();

                Vector newSelectionTreePathsVec = new Vector();

                if (!nodesAreSeleceted(taxaTree)) return;  //no nodes selected

                for (int i = 0; i < taxaTree.getSelectionCount(); i++) {  //for all selected nodes
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionTreePaths[i].getLastPathComponent();
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

                    if (parentNode != taxaTree.getModel().getRoot() && selectedNode.getChildCount() == 0) {   //cant remove from root node
                        //System.out.println("node: " + (String)selectedNode.getUserObject() + ", parentNode: " + (String)parentNode.getUserObject());
                        DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent(); //determine group node to insert seleceted node
                        grandParentNode.insert(selectedNode, grandParentNode.getIndex(parentNode));  //insert selected node
                        newSelectionTreePathsVec.add(new TreePath(selectedNode.getPath()));
                    } else {
                        if (selectedNode.getChildCount() != 0) {
                            viewer.throwMessage("cant remove group nodes", Color.RED);
                        } else if (parentNode == taxaTree.getModel().getRoot()) {
                            viewer.throwMessage("cant remove from root node", Color.RED);
                        }

                    }
                    if (parentNode.getChildCount() == 0) {     //if group node is empty, remove it
                        parentNode.removeFromParent();
                    }
                }
                TreePath[] newSelectionTreePaths = new TreePath[newSelectionTreePathsVec.size()];

                if (!newSelectionTreePathsVec.isEmpty()) newSelectionTreePathsVec.copyInto(newSelectionTreePaths);

                taxaTree.setSelectionPaths(newSelectionTreePaths);     // set new selections paths
                viewer.scrollAllPathsToVisible((DefaultMutableTreeNode) taxaTree.getModel().getRoot());  // scroll paths to visible
                taxaTree.updateUI();
            }
        };
        action.putValue(AbstractAction.NAME, "remove from group");
        allActions.add(action);
        return RemoveFromGroupAction = action;
    }

    // remove a group and adding children one level above
    private AbstractAction UngroupAction;

    public AbstractAction getUngroupAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = UngroupAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                TreePath[] selectionTreePaths = taxaTree.getSelectionPaths();

                if (!nodesAreSeleceted(taxaTree)) return;  //no nodes selected

                for (int i = 0; i < taxaTree.getSelectionCount(); i++) {    //for all selected nodes
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionTreePaths[i].getLastPathComponent();
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent(); //determine parent node
                    if (selectedNode.getAllowsChildren()) {   //leaves cant be ungrouped
                        while (selectedNode.getChildCount() > 0) {   //remove children from group until group is empty
                            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) selectedNode.getFirstChild();
                            parentNode.insert(childNode, parentNode.getIndex(selectedNode));
                        }
                        selectedNode.removeFromParent();
                    } else {
                        viewer.throwMessage("not a group node", Color.RED);
                        //break;
                    }

                }
                taxaTree.setSelectionPath(null);
                viewer.scrollAllPathsToVisible((DefaultMutableTreeNode) taxaTree.getModel().getRoot());
                taxaTree.updateUI();
            }
        };
        action.putValue(AbstractAction.NAME, "ungroup");
        allActions.add(action);
        return UngroupAction = action;
    }

    // delete all groups
    private AbstractAction deleteAllGroupsAction;

    public AbstractAction getDeleteAllGroups(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = deleteAllGroupsAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                int n = JOptionPane.showConfirmDialog(viewer.getFrame(), "really delete all groups ?", "delete groups?", JOptionPane.YES_NO_OPTION);
                if (n != 0) return;

                DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();
                for (Enumeration children = rootNode.children(); children.hasMoreElements(); ) {

                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
                    if (childNode.getAllowsChildren()) deleteAllGroupsRec(childNode, taxaTree);

                }
                taxaTree.setSelectionPath(null);
                taxaTree.updateUI();
            }
        };
        action.putValue(AbstractAction.NAME, "delete all groups");
        allActions.add(action);
        return deleteAllGroupsAction = action;
    }

    // reset all changes, load old groups from doc.sets.getTaxonomy
    private AbstractAction ResetAction;

    public AbstractAction getResetAction(final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = ResetAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);
                int n = JOptionPane.showConfirmDialog(viewer.getFrame(), "really reset all changes ?", "reset", JOptionPane.YES_NO_OPTION);
                if (n != 0) return;


                ((DefaultMutableTreeNode) taxaTree.getModel().getRoot()).removeAllChildren();   //remove everything (except root)

                viewer.updateView(Director.ALL); //initialize tree again...
                viewer.scrollAllPathsToVisible((DefaultMutableTreeNode) taxaTree.getModel().getRoot());

                taxaTree.setSelectionPath(null);
                taxaTree.updateUI();
            }
        };
        action.putValue(AbstractAction.NAME, "reset");
        allActions.add(action);
        return ResetAction = action;
    }

    // get the taxa selected in the graph
    private AbstractAction getGraphSelectedAction;

    public AbstractAction getGetGraphSelectedAction(final PhyloGraphView phyloView, final JPanel messagePanel, final JTree taxaTree) {
        AbstractAction action = getGraphSelectedAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                messagePanel.setVisible(false);

                PhyloSplitsGraph graph = phyloView.getPhyloGraph();
                Vector<TreePath> selectedPaths = new Vector<>();
                NodeSet selectedNodes = phyloView.getSelectedNodes();   // get selected items from the graph
                for (Node selectedNode : selectedNodes) {              //for all selected nodes in the graph
                    //if its a taxa node
                    if (selectedNode.getOutDegree() <= 1 && phyloView.getLabel(selectedNode) != null) {
                        for (Integer t : graph.getTaxa(selectedNode)) {
                            DefaultMutableTreeNode selectedNodeTaxa = new DefaultMutableTreeNode(taxaTree);
                            selectedNodeTaxa = null;
                            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();
                            //find coresponding node in taxaTree
                            for (Enumeration allNodes = rootNode.breadthFirstEnumeration(); allNodes.hasMoreElements(); ) { //for all nodes of the taxa tree
                                DefaultMutableTreeNode node = (DefaultMutableTreeNode) allNodes.nextElement();
                                if (node.toString().equals(graph.getLabel(graph.getTaxon2Node(t)))) {
                                    selectedNodeTaxa = node;
                                }
                            }
                            //mark the node in taxaTree
                            if (selectedNodeTaxa != null) {
                                selectedPaths.add(new TreePath(selectedNodeTaxa.getPath()));
                            }
                        }

                    }
                }
                TreePath[] selectionPathsArray = new TreePath[selectedPaths.size()];
                selectedPaths.copyInto(selectionPathsArray);

                taxaTree.setSelectionPaths(selectionPathsArray);
                if (selectionPathsArray.length == 0) viewer.throwMessage("No nodes selected...", Color.RED);

                //do the "normal" group action
                //doGroupAction(); changed to get graph selected
            }

        };
        action.putValue(AbstractAction.NAME, "get from graph");
        allActions.add(action);
        return getGraphSelectedAction = action;
    }


    // do the group action
    private void doGroupAction(final JPanel messagePanel, final JTree taxaTree) {

        messagePanel.setVisible(false);

        TreePath[] selectionTreePaths = taxaTree.getSelectionPaths();

        if (!nodesAreSeleceted(taxaTree)) return;  // no nodes selected

        if (!haveSameParents(selectionTreePaths)) return;   // nodes dont have same parents

        String groupName = "";

        while (getAllGroups(taxaTree).contains(groupName) || groupName.equals("")) {
            groupName = JOptionPane.showInputDialog(viewer.getFrame(), "Please enter group name", "Group Name", 1); //open dialog to enter group name
            if (getAllGroups(taxaTree).contains(groupName)) {
                JOptionPane.showMessageDialog(viewer.getFrame(), "Group already exists", "Group already exists", JOptionPane.WARNING_MESSAGE); //error message if group does already exist
            } else break;
        }

        if (groupName == null || groupName.equals(""))
            return; // user cancels action (or enter a blank name) : do nothing

        DefaultMutableTreeNode group1 = new DefaultMutableTreeNode(groupName); //make new group node

        DefaultMutableTreeNode firstChildNode = (DefaultMutableTreeNode) selectionTreePaths[0].getLastPathComponent();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) firstChildNode.getParent();

        parentNode.insert(group1, parentNode.getIndex(firstChildNode)); //insert group node

        TreePath[] newSelectionPaths = new TreePath[taxaTree.getSelectionCount()];

        for (int i = 0; i < taxaTree.getSelectionCount(); i++) {  //for all selected nodes
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionTreePaths[i].getLastPathComponent();
            group1.add(selectedNode); //add selected nodes to group node
            newSelectionPaths[i] = new TreePath(selectedNode.getPath());  // add path to array of selection paths
        }

        taxaTree.setSelectionPaths(newSelectionPaths);   // set new selections paths

        viewer.scrollAllPathsToVisible(parentNode);       // scroll paths to visible

        taxaTree.updateUI();
    }

    /**
     *  extracts all taxa from a graph-node
     *
     * @param selectedNode
     * @return Vector of strings with all taxa Label
     */
    /*private Vector getNode2TaxaLabel(Node selectedNode){

        PhyloGraph graph = phyloView.getPhyloGraph();
        String allLabel = phyloView.getLabel(graph.getTaxon2Node((Integer)graph.getNode2Taxa(selectedNode).toArray()[0]));
        Vector taxa = new Vector();
        while(allLabel.contains(",")){
            String label = allLabel.substring(0,allLabel.indexOf(","));
            allLabel = allLabel.substring(allLabel.indexOf(",")+2,allLabel.length());
            taxa.add(label);
        }
        taxa.add(allLabel);

        return taxa;
    } */


    /**
     * determine if the selected nodes have the same parent node
     *
     * @param selectionTreePath array with the tree paths to the selected nodes
     * @return true if selected nodes have same parent node, false otherwise
     */
    private boolean haveSameParents(TreePath[] selectionTreePath) {

        for (int i = 0; i < selectionTreePath.length - 1; i++) { //for all selected items
            DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) selectionTreePath[i].getLastPathComponent();
            DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) selectionTreePath[i + 1].getLastPathComponent();
            if (!node1.isNodeSibling(node2)) {       //pairwise check if nodes are siblings
                viewer.throwMessage("nodes dont have same parents", Color.RED);
                return false;
            }
        }
        return true;
    }

    /**
     * determine if any nodes are selected, and throw error message if no nodes are selected
     *
     * @return true if nodes any are selected, false otherwise
     */
    private boolean nodesAreSeleceted(final JTree taxaTree) {
        TreePath[] selectionTreePath = taxaTree.getSelectionPaths();
        if (selectionTreePath == null) {
            viewer.throwMessage("No nodes selected...", Color.RED);
            return false;
        }
        return true;
    }

    /**
     * determine selectable groups for addToExistingGroup action
     *
     * @param selectionTreePath array with the tree paths to the selected nodes
     * @return vector with all selectable group nodes
     */
    private Vector getSelectableGroups(TreePath[] selectionTreePath, final JTree taxaTree) {

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();
        //DefaultMutableTreeNode firstChildNode = (DefaultMutableTreeNode)selectionTreePath[0].getLastPathComponent();
        //DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)firstChildNode.getParent();

        Vector selectableGroupsVector = new Vector();

        for (TreePath aSelectionTreePath : selectionTreePath) {     //for all selected Items

            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) aSelectionTreePath.getLastPathComponent();
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) childNode.getParent();
            Vector selectableGroups = new Vector();

            for (Enumeration allNodes = rootNode.breadthFirstEnumeration(); allNodes.hasMoreElements(); ) {    //for all nodes of the tree
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) allNodes.nextElement();

                if ((selectedNode.getAllowsChildren()) && (selectedNode != rootNode) && (!childNode.isNodeDescendant(selectedNode)) && (selectedNode != parentNode)) {
                    // only insert nodes that are
                    // 1. not a leaf node
                    // 2. not the root node
                    // 3. not a descendant of the selected node
                    // 4. not the parent node of the selected node
                    selectableGroups.add(selectedNode); // add node to vector
                }
            }
            if (selectableGroups.isEmpty()) return null; //if one vector is empty, no groups are selectable
            else selectableGroupsVector.add(selectableGroups);
        }

        Vector finalSelectableGroups;

        if (selectableGroupsVector.isEmpty()) return null; //No groupes available

        finalSelectableGroups = intersectionOfVectors(selectableGroupsVector); //calculate intersection of available group nodes
        if (finalSelectableGroups.isEmpty()) return null;  //intersection is null

        return finalSelectableGroups;
    }

    /**
     * determine all group nodes
     *
     * @return vector with all group nodes
     */
    private Vector getAllGroups(final JTree taxaTree) {
        Vector allGroups = new Vector();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();

        for (Enumeration allNodes = rootNode.breadthFirstEnumeration(); allNodes.hasMoreElements(); ) {      //for all nodes of the tree
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) allNodes.nextElement();
            if ((selectedNode.getAllowsChildren()) && (selectedNode != rootNode)) {          //only insert group nodes
                allGroups.add(selectedNode.getUserObject());
            }
        }
        return allGroups;
    }

    /**
     * determine all group nodes except the given one
     *
     * @return vector with all group nodes except the given one
     */
    private Vector getAllGroups(Object group, final JTree taxaTree) {
        Vector allGroups = new Vector();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();

        for (Enumeration allNodes = rootNode.breadthFirstEnumeration(); allNodes.hasMoreElements(); ) {      //for all nodes of the tree
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) allNodes.nextElement();
            if ((selectedNode.getAllowsChildren()) && (selectedNode != rootNode)) {          //only insert group nodes
                allGroups.add(selectedNode.getUserObject());
            }
        }
        allGroups.remove(group);
        return allGroups;
    }


    /**
     * compute intersection of two vectors
     *
     * @param vector1 a vector
     * @param vector2 a vector
     * @return intersection vector
     */
    private Vector intersectionOfTwoVectors(Vector vector1, Vector vector2) {

        Vector intersection = new Vector();

        //switch vectors --> vector1 contains more elements
        if (vector2.size() > vector1.size()) {
            Vector help = vector1;
            vector1 = vector2;
            vector2 = help;
        }

        //check if items of vector 1 are contained in vector2
        for (int i = 0; i < vector1.size(); i++) {
            if (vector2.contains(vector1.elementAt(i))) {
                intersection.add(vector1.elementAt(i));
            }
        }
        return intersection;
    }

    /**
     * compute intersection of an arbitrary amount of vectors using intersectionOfTwoVectors
     *
     * @param allVectors
     * @return intersection vector
     */
    private Vector intersectionOfVectors(Vector allVectors) {

        if (allVectors.size() == 0) return null;              //vector is empty
        if (allVectors.size() == 1) return (Vector) allVectors.firstElement();    //vector contains only one vector

        Vector intersection = intersectionOfTwoVectors((Vector) allVectors.firstElement(), (Vector) allVectors.elementAt(1));

        for (int i = 2; i < allVectors.size(); i++) {
            intersection = intersectionOfTwoVectors(intersection, (Vector) allVectors.elementAt(i)); //pairwise intersection with former result and next vector
        }
        return intersection;
    }


    /**
     * create and save taxSets
     * only if treedepth is <=2, if treedepth >2, saving taxSets is not possible from a tree (use taxonomy)
     *
     * @return true if depth of tree is <=2, false otherwise
     */
    private boolean doApplyActionTaxSets(final JPanel messagePanel, final JTree taxaTree) {

        messagePanel.setVisible(false);

        if ((((DefaultMutableTreeNode) taxaTree.getModel().getRoot()).getDepth() <= 2)) {

            Taxa taxa = dir.getDocument().getTaxa();

            Sets newSets = new Sets();

            DefaultMutableTreeNode root = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();

            for (Enumeration children = root.children(); children.hasMoreElements(); ) {   //for all children of root
                DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) children.nextElement();
                if (groupNode.getAllowsChildren()) {     //is group node

                    TaxaSet taxaSet = new TaxaSet();    //make new taxaset

                    for (Enumeration groupChildren = groupNode.children(); groupChildren.hasMoreElements(); ) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) groupChildren.nextElement();
                        taxaSet.set(taxa.indexOf(selectedNode.getUserObject().toString())); //add children to treeSet
                    }
                    // newSets.addTaxSet(groupNode.getUserObject().toString(),taxaSet);
                    //todo addTaxSet?
                }
            }
            dir.getDocument().setSets(newSets);
        } else {
            viewer.throwMessage("depth of tree > 2", Color.RED);
            return false;
        }
        return true;
    }

    /**
     * create and save taxonomy
     */
    private void doApplyAction(final JPanel messagePanel, final JTree taxaTree) {
        messagePanel.setVisible(false);
        viewer.throwMessage("changes saved...", Color.blue);
        taxaTree.setSelectionPath(null);
        Sets newSets = new Sets();
        PhyloTree phyloTree = new PhyloTree();       //make new phyloTree
        Node rootNode = new Node(phyloTree);
        phyloTree.setRoot(rootNode);
        fillPhyloTreeRec(rootNode, (DefaultMutableTreeNode) taxaTree.getModel().getRoot(), phyloTree); //fill phyloTree recursive
        newSets.addTaxonomy("root", phyloTree);
        dir.getDocument().setSets(newSets);
        //@todo make update
    }

    /**
     * fillphylotree recursion
     *
     * @param parentNodePhylo the parent node in the phylo tree
     * @param parentNode      the parent node in the JTree
     */
    private void fillPhyloTreeRec(Node parentNodePhylo, DefaultMutableTreeNode parentNode, PhyloTree phyloTree) {

        for (Enumeration children = parentNode.children(); children.hasMoreElements(); ) {      //for all children of parentNode
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
            Node childNodePhylo = new Node(phyloTree);
            phyloTree.setLabel(childNodePhylo, (String) childNode.getUserObject());
            phyloTree.newEdge(parentNodePhylo, childNodePhylo);                              //add childNode to tree
            if ((childNode.getAllowsChildren()) && (childNode.getChildCount() > 0)) {       //if isGroupNode
                fillPhyloTreeRec(childNodePhylo, childNode, phyloTree);                                           //recursion
            }
        }
    }

    /**
     * removes all empty groups starting with root
     *
     * @param root a group node
     * @return true if the group node is deleted, false otherwise
     */
    private boolean removeEmptyGroupsRec(DefaultMutableTreeNode root) {

        for (int i = 0; i < root.getChildCount(); i++) {  //for all nodes of the tree
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) root.getChildAt(i);
            if (removeEmptyGroupsRec(selectedNode)) i--;    //if group is deleted, next child will have same index
        }
        if ((root.getChildCount() == 0) && (root.getAllowsChildren())) {
            root.removeFromParent(); //remove empty group nodes
            return true;             //return true if group node is deleted
        }

        return false;                //return false if node remains
    }

    /**
     * delete all groups and add taxa to root
     *
     * @param groupNode a group node
     */
    private void deleteAllGroupsRec(DefaultMutableTreeNode groupNode, final JTree taxaTree) {

        while (groupNode.getChildCount() > 0) { //remove all children until groupNode is empty
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) groupNode.getFirstChild();
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTree.getModel().getRoot();
            DefaultMutableTreeNode parentNode = groupNode;
            int parentNodeIndex;

            while (!rootNode.isNodeChild(parentNode)) { //determine parentNode in root
                parentNode = (DefaultMutableTreeNode) parentNode.getParent();
            }
            parentNodeIndex = rootNode.getIndex(parentNode); //determine Index of parentNode in root

            if (!childNode.getAllowsChildren()) {   //insert leaf to root at position parentNodeIndex
                rootNode.insert(childNode, parentNodeIndex);
            } else {  //recursion if selecetd Node is a group
                deleteAllGroupsRec(childNode, taxaTree);
            }
        }
        groupNode.removeFromParent();
    }


}
