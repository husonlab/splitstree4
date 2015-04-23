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

package splitstree.gui.main;

import jloda.gui.director.IDirectableViewer;
import jloda.gui.commands.CommandManager;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.gui.WindowListenerAdapter;
import jloda.phylo.PhyloGraphView;
import jloda.phylo.PhyloTree;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.main.SplitsTreeProperties;
import splitstree.nexus.Sets;
import splitstree.nexus.Taxa;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * the TaxaSetViewer
 */
public class TaxaSetViewer implements IDirectableViewer {

    private static boolean treeIsEditable = false;

    private boolean uptodate = true;
    java.util.List allActions = new LinkedList();
    JLabel descriptionLabel = null;
    private JFrame frame;
    private JPanel top;
    private JLabel message;
    private JPanel messagePanel;
    private JPanel button;
    private JPanel groupButtons;
    private JScrollPane treeScrollPane;
    private JTree taxaTree;
    private PhyloTree phyloTree;
    private DefaultTreeModel taxaTreeModel;
    private Director dir;
    private Document doc;

    private TaxaSetViewer viewer;
    private TaxaSetViewerActions tsva;
    private PhyloGraphView phyloGraphView;


    //constructor

    public TaxaSetViewer(Director dir, PhyloGraphView phyloView) {
        // init object
        viewer = this;
        this.dir = dir;
        doc = dir.getDocument();
        phyloGraphView = phyloView;
        // get actionlist
        tsva = new TaxaSetViewerActions(this, dir);

        //list = new DefaultListModel();
        frame = new JFrame();
        setTitle(dir);
        frame.setJMenuBar(setupMenuBar());
        descriptionLabel = new JLabel();
        frame.setSize(600, 460);
        dir.setViewerLocation(this);

        // make sure we remove this viewer when it is closed
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                viewer.dir.removeViewer(viewer);
                frame.dispose();
            }
        });
        try {
            this.makeTaxaSetViewWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        unlockUserInput();
        //frame.show();

        frame.setVisible(true);

    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     */
    public void updateView(String what) {

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        setUptoDate(false);
        lockUserInput();

        if (top != null) frame.remove(top);
        try {
            ((DefaultMutableTreeNode) taxaTreeModel.getRoot()).removeAllChildren();
            Sets sets = doc.getSets();
            if (sets == null) {
                sets = new Sets();
                doc.setSets(sets);
            }
            /*if(sets.getNumTaxonomys()>0) initializeTaxaTreeWithTaxonomies();
            else initializeTaxaTreeWithTaxaSets();*/
            initializeTaxaTreeWithTaxonomies();

        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.getContentPane().add(top, BorderLayout.CENTER);

        //frame.show();
        frame.setVisible(true);

        unlockUserInput();
        // Set up to date
        this.uptodate = true;

    }

    /**
     * recursivly build the JTree from the taxonomy-data
     */
    private void buildTreeRec(Node parentNodePhylo, DefaultMutableTreeNode parentNode) {

        for (Iterator allOutEdges = parentNodePhylo.getOutEdges(); allOutEdges.hasNext(); ) {  //for all children

            Node childNodePhylo = ((Edge) allOutEdges.next()).getOpposite(parentNodePhylo);  //get next child

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(phyloTree.getLabel(childNodePhylo)); //set new child in JTree
            parentNode.add(childNode);                    // add child node to parent node

            if (childNodePhylo.getOutDegree() > 0) {
                buildTreeRec(childNodePhylo, childNode);  //if group node do recursion
            } else {
                childNode.setAllowsChildren(false);       //if leave node, set allowsChildren=false
            }
        }
    }


    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null &&
                    (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(false);
        }
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null && (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(true);
        }
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() {
        this.getFrame().dispose();
    }

    public void GUIClosed() {
        dir.removeViewer(this);
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    /**
     * gets the title of this viewer
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }


    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Taxa Set Editor  - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Taxa Set Editor  - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * setup the menu bar
     */
    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));
        menu.add(tsva.getCloseAction());
        menuBar.add(menu);
        return menuBar;
    }

    /**
     * setup all TaxaSetViewWindow elements
     */

    private void makeTaxaSetViewWindow() {

        // this is the top Pane containg the list and buttons for the setup of the taxa sets
        top = new JPanel();
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titleBorder = new TitledBorder(loweredetched, "Configuration of taxa sets");
        Border space = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border exportBorder = BorderFactory.createCompoundBorder(space, titleBorder);
        GridBagLayout gridBagLayout = new GridBagLayout();
        top.setLayout(gridBagLayout);
        top.setBorder(exportBorder);

        // this is the panel with all the messages
        messagePanel = new JPanel();

        // this is the TaxaTreeModel
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("taxaTree");
        taxaTreeModel = new DefaultTreeModel(root, true);

        /**
         * Tree Scroll Pane & Taxa Tree
         */

        //initializeTaxaTree();
        taxaTree = new JTree(taxaTreeModel);

        taxaTree.setRootVisible(false);
        taxaTree.setEditable(treeIsEditable);
        scrollAllPathsToVisible(root);
        treeScrollPane = new JScrollPane(taxaTree);
        treeScrollPane.setMinimumSize(new Dimension(350, 400));
        treeScrollPane.setPreferredSize(new Dimension(1000, 1000));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        top.add(treeScrollPane, constraints);


        /**
         * group buttons
         */

        // 'GROUP': define a group of taxa.
        JButton groupButton = new JButton(tsva.getGroupAction(messagePanel, taxaTree));
        // 'ADD TO EXISTING GROUP': add a group of tree nodes to an existing group
        JButton addToExistingGroupButton = new JButton(tsva.getAddToExistingGroupAction(messagePanel, taxaTree));
        // 'RENAME GROUP': rename group
        JButton renameGroupButton = new JButton(tsva.getRenameGroupAction(messagePanel, taxaTree));
        // 'REMOVE FROM GROUP': remove taxa from group
        JButton removeFromGroupButton = new JButton(tsva.getRemoveFromGroupAction(messagePanel, taxaTree));
        // 'UNGROUP': ungroup
        JButton unGroupButton = new JButton(tsva.getUngroupAction(messagePanel, taxaTree));
        // 'DELET ALL GROUPS': delete all groups
        JButton deleteAllGroupsButton = new JButton(tsva.getDeleteAllGroups(messagePanel, taxaTree));
        // 'GET SELECTED': get a group of taxa from the graph
        JButton getGraphSelectedButton = new JButton(tsva.getGetGraphSelectedAction(phyloGraphView, messagePanel, taxaTree));
        // 'RESET': reset all changes
        JButton resetButton = new JButton(tsva.getResetAction(messagePanel, taxaTree));
        Dimension buttonSize = new Dimension(addToExistingGroupButton.getPreferredSize());
        groupButton.setMaximumSize(buttonSize);
        addToExistingGroupButton.setMaximumSize(buttonSize);
        renameGroupButton.setMaximumSize(buttonSize);
        removeFromGroupButton.setMaximumSize(buttonSize);
        unGroupButton.setMaximumSize(buttonSize);
        deleteAllGroupsButton.setMaximumSize(buttonSize);
        getGraphSelectedButton.setMaximumSize(buttonSize);
        resetButton.setMaximumSize(buttonSize);
        groupButtons = new JPanel();
        LayoutManager groupButtonLayout = new BoxLayout(groupButtons, BoxLayout.Y_AXIS);
        groupButtons.setLayout(groupButtonLayout);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 15)));
        groupButtons.add(groupButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 5)));
        groupButtons.add(addToExistingGroupButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 5)));
        groupButtons.add(renameGroupButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 5)));
        groupButtons.add(removeFromGroupButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 5)));
        groupButtons.add(unGroupButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 5)));
        groupButtons.add(deleteAllGroupsButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 15)));
        groupButtons.add(resetButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 15)));
        groupButtons.add(getGraphSelectedButton);
        groupButtons.add(Box.createRigidArea(new Dimension(0, 15)));
        groupButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.weightx = 0.1;
        constraints.weighty = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        top.add(groupButtons, constraints);


        /**
         * buttons
         */

        // 'APPLY': run algorithm with user-options, without closing this window.
        JButton applyButton = new JButton(tsva.getApplyAction(messagePanel, taxaTree));

        // 'OK': run algorithm with user-options, and close this window.
        JButton okButton = new JButton(tsva.getOkAction(messagePanel, taxaTree));

        // 'CANCEL': close this window
        JButton cancelButton = new JButton(tsva.getCancelAction());

        // Draw Annotations
        JButton drawAnnotations = new JButton(tsva.getAnnotationAction());

        // optimize cycle of the leaves
        JButton optimizeCylce = new JButton(tsva.getOptimizeCycleAction());

        button = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(button, BoxLayout.X_AXIS);
        button.setLayout(buttonLayout);
        button.add(Box.createHorizontalGlue());
        /*
         * cancel, apply, ok -buttons
         */

        button.add(drawAnnotations);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(optimizeCylce);
        button.add(Box.createRigidArea(new Dimension(60, 0)));
        button.add(cancelButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(applyButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(okButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
        button.setSize(340, 70);

        /**
         * Error Message Pane
         */


        //JLabel iconLabel = new JLabel(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Stop16.gif"),JLabel.CENTER);
        //errorMessagePanel.add(iconLabel);

        message = new JLabel();
        //errorMessage.setForeground(Color.RED);
        messagePanel.add(message);
        button.add(messagePanel);
        messagePanel.setVisible(false);


        frame.getContentPane().add(top, BorderLayout.CENTER);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        frame.getRootPane().setDefaultButton(okButton);

    }

    /**
     * initialize the tree using taxonomies
     */
    private void initializeTaxaTreeWithTaxonomies() {

        taxaTreeModel.reload();
        taxaTree.updateUI();

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTreeModel.getRoot();

        Sets sets = doc.getSets();
        if (sets == null) {
            sets = new Sets();
            doc.setSets(sets);
        }

        if (!sets.getTaxonomyNames().isEmpty()) {
            phyloTree = sets.getTaxonomy((String) sets.getTaxonomyNames().toArray()[0]); //new phyloTree
            buildTreeRec(phyloTree.getRoot(), rootNode);                                            //build tree recursive
        } else { // no taxonomy available

            Taxa taxa = doc.getTaxa();

            DefaultMutableTreeNode[] taxaTreeNodes = new DefaultMutableTreeNode[taxa.getNtax() + 1];
            for (int i = 1; i <= taxa.getNtax(); i++) {
                taxaTreeNodes[i] = new DefaultMutableTreeNode(taxa.getLabel(i));
                taxaTreeNodes[i].setAllowsChildren(false);
                rootNode.add(taxaTreeNodes[i]);
            }
        }


        taxaTree.setSelectionPath(null);
        scrollAllPathsToVisible(rootNode);
        taxaTree.updateUI();
    }

    /**
     * scroll all paths to visible below group node
     *
     * @param groupNode a group node
     */
    public void scrollAllPathsToVisible(DefaultMutableTreeNode groupNode) {
        //DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)taxaTreeModel.getRoot();

        for (Enumeration allNodes = groupNode.breadthFirstEnumeration(); allNodes.hasMoreElements(); ) { //for all nodes
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) allNodes.nextElement();
            //set paths of all leaf nodes visible
            if (!selectedNode.getAllowsChildren()) taxaTree.scrollPathToVisible(new TreePath(selectedNode.getPath()));
        }
    }

    /**
     * display an error message on a JLabel errorMessage
     *
     * @param errorMessageText errorMessage
     */
    public void throwMessage(String errorMessageText, Color textColor) {

        messagePanel.setVisible(true);
        message.setForeground(textColor);
        message.setText(errorMessageText);
    }

    /**
     * initialize the tree using taxasets
     */
    /*  private void initializeTaxaTreeWithTaxaSets() {

   DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) taxaTreeModel.getRoot();

   Taxa taxa = doc.getTaxa();

   DefaultMutableTreeNode[] taxaTreeNodes = new DefaultMutableTreeNode[taxa.getNtax()+1];
   for (int i=1;i<= taxa.getNtax();i++) {
       taxaTreeNodes[i] = new DefaultMutableTreeNode(taxa.getLabel(i));
       taxaTreeNodes[i].setAllowsChildren(false);
       rootNode.add(taxaTreeNodes[i]);
   }
   Sets sets = doc.getSets();
   if(sets == null){
       sets = new Sets();
       doc.setSets(sets);
   }

   Object[] taxaSetNamesArray = sets.getTaxaSetNames().toArray();

   Vector seenTaxa = new Vector();

   for (int i=0; i<sets.getNumTaxSets();i++){

       TaxaSet s = sets.getTaxSet((String)taxaSetNamesArray[i]);

       // make new group
       DefaultMutableTreeNode group = new DefaultMutableTreeNode((String)taxaSetNamesArray[i]);

       // add taxa to group
       for (int k=s.getBits().nextSetBit(1);k!=-1;k=s.getBits().nextSetBit(k+1)){
           if(!seenTaxa.contains(taxaTreeNodes[k])){
               group.add(taxaTreeNodes[k]);
               seenTaxa.add(taxaTreeNodes[k]);
           }
           else System.err.println("can't add taxa in more than one group !");
       }
       rootNode.add(group);
   }

   scrollAllPathsToVisible(rootNode);
   taxaTree.updateUI();

}     */

    /**
     * gets the frame
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return null;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return false;
    }


    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "TaxaSetView";
    }

}
