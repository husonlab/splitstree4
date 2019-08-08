/**
 * ShowTaxaSetViewer.java
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
package splitstree4.gui.TreePainter;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.Alert;
import jloda.swing.util.Geometry;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.WindowListenerAdapter;
import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Taxa;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 24.01.2007
 * Time: 12:31:57
 * To change this template use File | Settings | File Templates.
 */
public class ShowTaxaSetViewer implements IDirectableViewer {

    java.util.List allActions = new LinkedList();
    private boolean uptodate = true;
    private JFrame frame;
    private Director dir;
    private ShowTaxaSetViewer viewer;

    private Sets sets = null;
    private Document doc = null;
    private PhyloGraphView phyloGraphView = null;
    private PhyloSplitsGraph phyloGraph;

    private JPanel button;

    private String boundaryMode = "mode1";
    private String boundaryAppearance = "line";
    private String arcMode = "sickle";
    private int arcLineWidth = 1;
    private int dashSize = 2;


    JPanel optionsPanel;
    JComboBox boundaryModeBox;
    JComboBox boundaryAppearanceBox;
    JComboBox arcModeBox;
    JSpinner lineWidthSpinner;
    JSpinner dashSizeSpinner;

    private int[] cycle = null;

    //constructor

    public ShowTaxaSetViewer(Director dir, PhyloGraphView phyloView) {

        phyloGraphView = phyloView;
        phyloGraph = phyloGraphView.getPhyloGraph();

        viewer = this;
        this.dir = dir;
        doc = dir.getDocument();

        frame = new JFrame();
        setTitle(dir);
        frame.setJMenuBar(setupMenuBar());
        frame.setSize(380, 420);
        dir.setViewerLocation(this);

        // make sure we remove this viewer and listener when it is closed
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                viewer.dir.removeViewer(viewer);
                frame.dispose();
            }
        });
        try {
            this.MakeTaxaSetViewerWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        unlockUserInput();
        //frame.show();

        frame.setVisible(true);
    }

    private void MakeTaxaSetViewerWindow() {

        /**
         * buttons
         */

        // 'APPLY': run algorithm with user-options, without closing this window.
        JButton applyButton = new JButton(getApplyAction());

        // 'OK': run algorithm with user-options, and close this window.
        JButton okButton = new JButton(getOkAction());

        // 'CANCEL': close this window
        JButton cancelButton = new JButton(getCancelAction());

        button = new JPanel();
        LayoutManager buttonLayout = new BoxLayout(button, BoxLayout.X_AXIS);
        button.setLayout(buttonLayout);
        button.add(Box.createHorizontalGlue());

        button.add(cancelButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(applyButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(okButton);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
        button.setSize(300, 60);

        /**
         * option panels
         */


        String[] boundaryModes = {"mode1", "mode2", "mode3"};
        boundaryModeBox = new JComboBox(boundaryModes);
        boundaryModeBox.setBorder(BorderFactory.createTitledBorder("boundary mode"));
        boundaryModeBox.addActionListener(this.getBoundaryModeAction());

        String[] boundaryAppearances = {"line", "circle", "square", "arrow", "none"};
        boundaryAppearanceBox = new JComboBox(boundaryAppearances);
        boundaryAppearanceBox.setBorder(BorderFactory.createTitledBorder("boundary appearance"));
        boundaryAppearanceBox.addActionListener(this.getBoundaryAppearanceAction());

        String[] arcModes = {"sickle", "arc", "dashed"};
        arcModeBox = new JComboBox(arcModes);
        arcModeBox.setBorder(BorderFactory.createTitledBorder("arc mode"));
        arcModeBox.addActionListener(this.getArcModeAction());


        lineWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        lineWidthSpinner.addChangeListener(this.getlineWidthListener);
        lineWidthSpinner.setPreferredSize(new Dimension(40, 30));
        JPanel lineWidthSpinnerPanel = new JPanel();
        lineWidthSpinnerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 50), BorderFactory.createTitledBorder("line width")));
        lineWidthSpinnerPanel.add(lineWidthSpinner);

        dashSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        dashSizeSpinner.addChangeListener(this.getDashSizeListener);
        dashSizeSpinner.setPreferredSize(new Dimension(40, 30));
        JPanel dashSizeSpinnerPanel = new JPanel();
        dashSizeSpinnerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 50), BorderFactory.createTitledBorder("dash size")));
        dashSizeSpinnerPanel.add(dashSizeSpinner);

        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
        spinnerPanel.add(lineWidthSpinnerPanel);
        spinnerPanel.add(dashSizeSpinnerPanel);

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        optionsPanel.add(boundaryModeBox);
        optionsPanel.add(Box.createRigidArea(new Dimension(50, 5)));
        optionsPanel.add(boundaryAppearanceBox);
        optionsPanel.add(Box.createRigidArea(new Dimension(50, 5)));
        optionsPanel.add(arcModeBox);
        optionsPanel.add(Box.createRigidArea(new Dimension(50, 5)));
        optionsPanel.add(spinnerPanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));


        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titleBorder = new TitledBorder(loweredetched, "Options");
        Border space = BorderFactory.createEmptyBorder(15, 15, 15, 15);
        Border optionsBorder = BorderFactory.createCompoundBorder(space, titleBorder);
        optionsPanel.setBorder(optionsBorder);


        //add panels to frame

        frame.getContentPane().add(optionsPanel, BorderLayout.CENTER);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        frame.getRootPane().setDefaultButton(okButton);
    }


    /**
     * setup the menu bar
     */
    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));
        menu.add(getCloseAction());
        menuBar.add(menu);
        return menuBar;
    }

    /**
     * All the Actions of the window
     */

    // do apply
    private AbstractAction applyAction;

    public AbstractAction getApplyAction() {
        AbstractAction action = applyAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    doApplyAction();          // do apply

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

    public AbstractAction getOkAction() {
        AbstractAction action = okAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    doApplyAction();                // do apply & clos window
                    dir.removeViewer(viewer);
                    frame.dispose();

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

    // close window
    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);                  //close window
                frame.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        allActions.add(action);
        return cancelAction = action;
    }

    private AbstractAction closeAction;

    private AbstractAction getCloseAction() {

        AbstractAction action = closeAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);
                frame.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        allActions.add(action);
        return closeAction = action;
    }


    private AbstractAction boundaryModeAction;

    private AbstractAction getBoundaryModeAction() {

        AbstractAction action = boundaryModeAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                boundaryMode = (String) boundaryModeBox.getSelectedItem();
            }
        };
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return boundaryModeAction = action;
    }

    private AbstractAction boundaryAppearanceAction;

    private AbstractAction getBoundaryAppearanceAction() {

        AbstractAction action = boundaryAppearanceAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                boundaryAppearance = (String) boundaryAppearanceBox.getSelectedItem();
            }
        };
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return boundaryAppearanceAction = action;
    }

    private AbstractAction arcModeAction;

    private AbstractAction getArcModeAction() {

        AbstractAction action = arcModeAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                arcMode = (String) arcModeBox.getSelectedItem();
            }
        };
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return arcModeAction = action;
    }


    private ChangeListener getlineWidthListener = new ChangeListener() {

        public void stateChanged(ChangeEvent e) {
            arcLineWidth = ((SpinnerNumberModel) lineWidthSpinner.getModel()).getNumber().intValue();
        }
    };

    private ChangeListener getDashSizeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent e) {
            dashSize = ((SpinnerNumberModel) dashSizeSpinner.getModel()).getNumber().intValue();
        }
    };


    public boolean isUptoDate() {
        return uptodate;
    }

    public JFrame getFrame() {
        return frame;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public void updateView(String what) {

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        setUptoDate(false);
        lockUserInput();

        //frame.show();
        frame.setVisible(true);

        unlockUserInput();
        // Set up to date
        this.uptodate = true;

    }

    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Set Highlighting - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Set Highlighting  - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
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

    public void destroyView() {

    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }


    private void doApplyAction() {
        // remove old WorldShapes
//       SortedSet worldShapes = phyloGraphView.getWorldShapes();
//       while (worldShapes.size()>0){
//         phyloGraphView.removeWorldShape((WorldShape)worldShapes.first());
//       }
        // draw new Set Highlightung..
        drawSetHighlightingWithTaxonomies();
    }

    private void drawSetHighlightingWithTaxonomies() {
        sets = doc.getSets();
        if (sets.getTaxonomyNames().isEmpty()) return;    // no taxonomy available

        PhyloTree phyloTree = sets.getTaxonomy((String) sets.getTaxonomyNames().toArray()[0]); // get first taxonomy (what to do with more than one?)

        for (Iterator allNodes = phyloTree.nodeIterator(); allNodes.hasNext(); ) {  // for all nodes

            Node selectedNode = (Node) allNodes.next(); // get next node

            if ((selectedNode.getOutDegree() > 0) && (selectedNode != phyloTree.getRoot())) {  // if node is group node

                Vector nodes;
                nodes = getDescendantNodes(selectedNode, phyloTree);       // get descendant nodes
                nodes = sortNodes(nodes);                       // sort descendant nodes counterclockwise

                int groupChildrenDepth = getGroupChildrenDepth(selectedNode, phyloTree);   // get depth of group --> distance of sickle to nodes

                Node[] nodesInGraph = new Node[nodes.size()];                   // get nodes in graph
                for (int i = 0; i < nodes.size(); i++) {
                    nodesInGraph[i] = phyloGraph.getTaxon2Node((Integer) nodes.get(i));   // get node to taxa-id
                }

                if (nodesInGraph.length == 1) {
                    drawSetHighlighting(nodesInGraph, phyloTree.getLabel(selectedNode), groupChildrenDepth); //only one taxa, no need to check order
                } else {

                    /*************** check order of seleceted nodes ******************/

                    int[] taxaInCyclicOrder = getCycle();

                    /*for(int i=1;i<taxaInCyclicOrder.length;i++) {
                       System.out.println("taxa in cyclic order: " + taxaInCyclicOrder[i] + ", " + graph.getLabel(graph.getTaxon2Node(taxaInCyclicOrder[i])));
                   } */

                    int start = 0;
                    int additionalNodes = 0;
                    for (int i = 0; i < nodesInGraph.length - 1; i++) {

                        Node nextNodeInCycle = getNextNodeInCylce(nodesInGraph[i]);   // next node in cycle
                        Node nextNode = nodesInGraph[i + 1];                            // next selected node
                        //System.out.println("nextNode: " + graph.getLabel(nextNode) + ", nextNodeInCycle: " + graph.getLabel(nextNodeInCycle));

                        if (nextNodeInCycle != nextNode) {       // if next node in cycle != next selected node
                            Node[] connectedNodes = new Node[i + 1 - start];

                            if ((start == 0) && (phyloGraph.getTaxon2Node(taxaInCyclicOrder[1]) == nodesInGraph[nodesInGraph.length - 1])) { // nodes are connected to the end (crossing 0°)
                                additionalNodes = i + 1;    // save number of connected additional nodes, add them later
                            } else {
                                for (int j = 0, k = start; j < i + 1 - start; j++, k++) {
                                    connectedNodes[j] = nodesInGraph[k];               // add the connected nodes
                                }
                                connectedNodes = checkOrder(connectedNodes);         // check order of connected nodes
                                drawSetHighlighting(connectedNodes, phyloTree.getLabel(selectedNode), groupChildrenDepth);  // draw highlighting of connected nodes
                            }
                            start = i + 1;
                        }
                        if (i == nodesInGraph.length - 2) {                                // last node in cycle
                            Vector connectedNodesVector = new Vector();

                            for (int j = 0, k = start; j < i + 1 - start + 1; j++, k++) {
                                connectedNodesVector.add(nodesInGraph[k]);           // add the connected nodes
                            }
                            if (additionalNodes != 0) {                                  // if there are additional nodes
                                connectedNodesVector.addAll(Arrays.asList(nodesInGraph).subList(0, additionalNodes));
                            }
                            Node[] connectedNodes = new Node[connectedNodesVector.size()];
                            connectedNodesVector.copyInto(connectedNodes);
                            connectedNodes = checkOrder(connectedNodes);             // check order of connected nodes
                            drawSetHighlighting(connectedNodes, phyloTree.getLabel(selectedNode), groupChildrenDepth);  // draw highlighting of connected nodes
                        }
                    } // for all nodes of group
                }  // no single node
            }  // if group node
        } // for all nodes
    }


    /**
     * get all descendant nodes for a given groupNode
     *
     * @param groupNode
     * @return vector of taxaIds of all descendant nodes
     */
    private Vector getDescendantNodes(Node groupNode, PhyloTree phyloTree) {

        Vector descendantNodes = new Vector();

        for (Edge e = groupNode.getFirstOutEdge(); e != null; e = groupNode.getNextOutEdge(e)) {
            final Node childNode = e.getTarget();
            if (childNode.getOutDegree() > 0) {
                descendantNodes.addAll(getDescendantNodes(childNode, phyloTree));
            } else {
                Taxa taxa = doc.getTaxa();
                int taxaId = taxa.indexOf(phyloTree.getLabel(childNode));
                descendantNodes.add(taxaId);
            }
        }
        return descendantNodes;
    }

    /**
     * sort the nodes to be in cyclic order
     *
     * @param nodes
     * @return vector of sorted nodes
     */
    private Vector sortNodes(Vector nodes) {

        if (nodes.isEmpty()) return null;

        Vector helpVector = new Vector(nodes);

        nodes.clear();

        //int[] taxaInCyclicOrder = phyloGraph.getCycle();
        int[] taxaInCyclicOrder = getCycle();

        for (int aTaxaInCyclicOrder : taxaInCyclicOrder) {
            if (helpVector.contains((int) (aTaxaInCyclicOrder))) {
                nodes.add(0, aTaxaInCyclicOrder);    //turn around, to be mathematical positive
            }
        }
        return nodes;
    }

    /**
     * computes the depths of groups for a given node in phyloTree
     *
     * @param selectedNode
     * @return groupChildrenDepth of the selected node
     */
    private int getGroupChildrenDepth(Node selectedNode, PhyloTree taxonomy) {

        int groupChildrenDepth = 0;
        int depth = getPhyloTreeDepth(selectedNode, taxonomy);

        Vector descendantNodes = getDescendantNodesPhyloTree(selectedNode, taxonomy);
        for (int i = 0; i < descendantNodes.size(); i++) {
            Node node = (Node) (descendantNodes.elementAt(i));
            if (getPhyloTreeDepth(node, taxonomy) > groupChildrenDepth)
                groupChildrenDepth = getPhyloTreeDepth(node, taxonomy);
        }
        groupChildrenDepth = groupChildrenDepth - depth;

        return groupChildrenDepth;
    }

    /**
     * computes the depths for a given node in phyloTree
     *
     * @param selectedNode
     * @return depth of the selected node
     */
    private int getPhyloTreeDepth(Node selectedNode, PhyloTree taxonomy) {
        int depth = 0;

        Node oppositeNode = new Node(taxonomy);
        while (oppositeNode != taxonomy.getRoot()) {
            oppositeNode = taxonomy.getOpposite(selectedNode, selectedNode.getFirstInEdge());
            selectedNode = oppositeNode;
            depth++;
        }
        return depth;
    }


    /**
     * get all descendant nodes for a given groupNode
     *
     * @param groupNode
     * @return vector of nodes
     */
    private Vector getDescendantNodesPhyloTree(Node groupNode, PhyloTree taxonomy) {

        Vector descendantNodes = new Vector();
        for (Edge e = groupNode.getFirstOutEdge(); e != null; e = groupNode.getNextOutEdge(e)) {
            Node childNode = e.getTarget();
            if (childNode.getOutDegree() > 0) {
                descendantNodes.addAll(getDescendantNodesPhyloTree(childNode, taxonomy));
            } else {
                descendantNodes.add(childNode);
            }
        }
        return descendantNodes;
    }

    /**
     * draw highlighting of groups
     *
     * @param nodesInGraph nodes to be marked as a group
     * @param groupName    label of grouop
     */
    private void drawSetHighlighting(Node[] nodesInGraph, String groupName, int groupChildrenDepth) {

        //***************  set startNode + endNode  + startLine  + endLine ***************

        Point2D.Double startNode = new Point2D.Double();
        Point2D.Double endNode = new Point2D.Double();

        startNode.setLocation(phyloGraphView.getLocation(nodesInGraph[0]));   // set start node to pos of first node
        endNode.setLocation(phyloGraphView.getLocation(nodesInGraph[nodesInGraph.length - 1]));  // set end node to pos of last node

        Line2D.Double startLine = new Line2D.Double();
        Node firstHelpNode = phyloGraph.getOpposite(nodesInGraph[0], phyloGraph.getFirstAdjacentEdge(nodesInGraph[0])); //opposite Node of startNode
        // set start line from start node to opposite node
        startLine.setLine(phyloGraphView.getLocation(firstHelpNode).getX(), phyloGraphView.getLocation(firstHelpNode).getY(), startNode.getX(), startNode.getY());

        Line2D.Double endLine = new Line2D.Double();
        Node lastHelpNode = phyloGraph.getOpposite(nodesInGraph[nodesInGraph.length - 1], phyloGraph.getFirstAdjacentEdge(nodesInGraph[nodesInGraph.length - 1])); //opposite Node of endNode
        // set end line from end node to opposite node
        endLine.setLine(phyloGraphView.getLocation(lastHelpNode).getX(), phyloGraphView.getLocation(lastHelpNode).getY(), endNode.getX(), endNode.getY());

        //***************  set nextNode + previousNode --> get point between start node & previous node, and point between last node and next node ***************

        Point2D.Double nextNode = new Point2D.Double();
        Point2D.Double previousNode = new Point2D.Double();

        nextNode.setLocation(phyloGraphView.getLocation(getNextNodeInCylce(nodesInGraph[nodesInGraph.length - 1]))); // pos of next node
        previousNode.setLocation(phyloGraphView.getLocation(getPreviousNodeInCylce(nodesInGraph[0])));             // pos of previous node

        // point between start node and previuos node, used for boundary mode 3
        Point2D.Double midFirstPrevious = new Point2D.Double((startNode.getX() + previousNode.getX()) / 2, (startNode.getY() + previousNode.getY()) / 2);
        // point between end node and next node, used for boundary mode 3
        Point2D.Double midEndNext = new Point2D.Double((endNode.getX() + nextNode.getX()) / 2, (endNode.getY() + nextNode.getY()) / 2);


        //*************** determine radius of circle ***************

        double helpExtendAngle = getAngleOfNode(nodesInGraph[nodesInGraph.length - 1]) - getAngleOfNode(nodesInGraph[0]); //angle between start- & endNode
        if (helpExtendAngle < 0) helpExtendAngle += 360;   // angle crosses 0°

        Point2D.Double averageCenter = new Point2D.Double(); // average point of involved taxa
        Point2D.Double maxPoint = new Point2D.Double(startNode.getX(), startNode.getY());
        Point2D.Double minPoint = new Point2D.Double(startNode.getX(), startNode.getY());

        for (Node aNodesInGraph3 : nodesInGraph) {
            Point2D nodePoint = phyloGraphView.getLocation(aNodesInGraph3);
            if (nodePoint.getX() > maxPoint.getX()) maxPoint.setLocation(nodePoint.getX(), maxPoint.getY());
            if (nodePoint.getX() < minPoint.getX()) minPoint.setLocation(nodePoint.getX(), minPoint.getY());
            if (nodePoint.getY() > maxPoint.getY()) maxPoint.setLocation(maxPoint.getX(), nodePoint.getY());
            if (nodePoint.getY() < minPoint.getY()) minPoint.setLocation(minPoint.getX(), nodePoint.getY());
        }
        averageCenter.setLocation((maxPoint.getX() + minPoint.getX()) / 2, (maxPoint.getY() + minPoint.getY()) / 2); //set average point between maximal x&y values

        Point2D.Double averageCenterAll = new Point2D.Double(); //average point of all taxa

        for (Iterator allNodes = phyloGraph.nodeIterator(); allNodes.hasNext(); ) {  //for all nodes
            Node selectedNode = (Node) allNodes.next(); //get next node
            if (selectedNode.getOutDegree() == 0) {
                Point2D nodePoint = phyloGraphView.getLocation(selectedNode);
                if (nodePoint.getX() > maxPoint.getX()) maxPoint.setLocation(nodePoint.getX(), maxPoint.getY());
                if (nodePoint.getX() < minPoint.getX()) minPoint.setLocation(nodePoint.getX(), minPoint.getY());
                if (nodePoint.getY() > maxPoint.getY()) maxPoint.setLocation(maxPoint.getX(), nodePoint.getY());
                if (nodePoint.getY() < minPoint.getY()) minPoint.setLocation(minPoint.getX(), nodePoint.getY());
            }
        }
        averageCenterAll.setLocation((maxPoint.getX() + minPoint.getX()) / 2, (maxPoint.getY() + minPoint.getY()) / 2);

        //phyloGraphView.addWorldShape(new WorldShape("Z", averageCenterAll));
        //phyloGraphView.addWorldShape(new WorldShape("Z", averageCenter));

        double radius;

        Point2D.Double vecStartEnd = (Point2D.Double) Geometry.diff(endNode, startNode); // vector between start & endNode
        Point2D.Double middleVec1Point = new Point2D.Double((startNode.getX() + endNode.getX()) / 2, (startNode.getY() + endNode.getY()) / 2); //point between start- & endNode
        Point2D.Double middleVec1ort = normalizeVec(new Point2D.Double(vecStartEnd.getY(), vecStartEnd.getX() * -1)); //vector orthogonal to vecStartEnd

        if (helpExtendAngle == 0)
            middleVec1ort.setLocation(normalizeVec(new Point2D.Double(Geometry.diff(phyloGraphView.getLocation(firstHelpNode), startNode).getX(), Geometry.diff(phyloGraphView.getLocation(firstHelpNode), startNode).getY())));

        // other approach: do not use orthogonal start-ends-vector, but average angle of involved nodes
        Point2D.Double tmpVec = new Point2D.Double(0, 0);
        for (Node aNodesInGraph2 : nodesInGraph) {

            Point2D.Double currentNodePos = new Point2D.Double(0, 0);
            currentNodePos.setLocation(phyloGraphView.getLocation(aNodesInGraph2));
            Line2D.Double tmpLine = new Line2D.Double();
            Node oppositeNode = phyloGraph.getOpposite(aNodesInGraph2, phyloGraph.getFirstAdjacentEdge(aNodesInGraph2));

            tmpLine.setLine(phyloGraphView.getLocation(oppositeNode).getX(), phyloGraphView.getLocation(oppositeNode).getY(), currentNodePos.getX(), currentNodePos.getY());
            //System.out.println("angle: " + getAngleOfNode(nodesInGraph[0]));

            tmpVec.setLocation(tmpVec.getX() + tmpLine.getX1() - tmpLine.getX2(), tmpVec.getY() + tmpLine.getY1() - tmpLine.getY2());
            //System.out.println("vec:" + tmpVec.toString());
        }

        //System.out.println("vec normalize:" + normalizeVec(tmpVec).toString());

        middleVec1ort = normalizeVec(tmpVec);

        //phyloGraphView.addWorldShape(new WorldShape(new Line2D.Double(middleVec1Point.getX(), middleVec1Point.getY(), middleVec1Point.getX()+tmpVec.getX(),middleVec1Point.getY()+tmpVec.getY())));


        //********************************************


        if (helpExtendAngle > 135) {
            Line2D.Double tmp1 = new Line2D.Double(middleVec1Point.getX(), middleVec1Point.getY(), middleVec1Point.getX() + middleVec1ort.getX(), middleVec1Point.getY() + middleVec1ort.getY());
            Line2D.Double tmp2 = new Line2D.Double(averageCenter.getX(), averageCenter.getY(), averageCenter.getX() + vecStartEnd.getX(), averageCenter.getY() + vecStartEnd.getY());
            Point2D.Double intersect = lineIntersection(tmp1, tmp2);
            if (intersect == null) intersect.setLocation(averageCenter);
            radius = middleVec1Point.distance(intersect);
        } else {
            //radius = (startNode.distance(intersect)+endNode.distance(intersect))/2;
            radius = (maxPoint.getX() - minPoint.getX() + maxPoint.getY() - minPoint.getY()) / 4;
        }

        /*
        *  determine center of circle1
        *  center is placed
        *  on the orthogonal vector to the startEndVector, with the radius of
        *  an average radius of the circle surrounding all taxa     (angle between startNode & endNode<135°)
        *
        *  or on the nearest point on the orthogonal vector to the startEndVector from an average centerPoint of all taxa (angle >135°)
        */

        Point2D.Double center = new Point2D.Double();     //center of first circle

        if (helpExtendAngle > 135) {
            center.setLocation(middleVec1Point.getX() - radius * middleVec1ort.getX(), middleVec1Point.getY() - radius * middleVec1ort.getY());
        } else {
            center.setLocation(middleVec1Point.getX() + radius * middleVec1ort.getX(), middleVec1Point.getY() + radius * middleVec1ort.getY());
        }


        //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(center,center)).getShape(),"X", center));

        //***************  get longest distance of taxa to center of circle to determine radius of circle ***************

        //double helpAngleStart = getAngleOfNode(nodesInGraph[0]);
        //double helpAngleEnd = getAngleOfNode(nodesInGraph[nodesInGraph.length-1]);
        double helpAngleStart = getAngle(startNode, center);
        helpAngleStart -= 5;
        if (helpAngleStart < 0) helpAngleStart += 360;
        double helpAngleEnd = getAngle(endNode, center);
        helpAngleEnd += 5;
        if (helpAngleEnd > 360) helpAngleEnd -= 360;

        double longestDist = 0;
        for (Node aNodesInGraph1 : nodesInGraph) {

            Point2D.Double point1Trans = (Point2D.Double) phyloGraphView.trans.d2w(phyloGraphView.getLabelRect(aNodesInGraph1).getLocation());
            Point2D.Double point2Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph1).getMinX(), (int) phyloGraphView.getLabelRect(aNodesInGraph1).getMaxY()));
            Point2D.Double point3Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph1).getMaxX(), (int) phyloGraphView.getLabelRect(aNodesInGraph1).getMaxY()));
            Point2D.Double point4Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph1).getMaxX(), (int) phyloGraphView.getLabelRect(aNodesInGraph1).getMinY()));

            double angleLabelPoint1 = getAngle(point1Trans, center);
            double angleLabelPoint2 = getAngle(point2Trans, center);
            double angleLabelPoint3 = getAngle(point3Trans, center);
            double angleLabelPoint4 = getAngle(point4Trans, center);

            if (phyloGraphView.getLocation(aNodesInGraph1).distance(center) > longestDist) { //position of node > longest distance so far
                longestDist = phyloGraphView.getLocation(aNodesInGraph1).distance(center);
            }

            if ((point1Trans.distance(center) > longestDist) && (angleLabelPoint1 > helpAngleStart) && (angleLabelPoint1 < helpAngleEnd)) {
                longestDist = point1Trans.distance(center) * 1.05;
            }
            if ((point2Trans.distance(center) > longestDist) && (angleLabelPoint2 > helpAngleStart) && (angleLabelPoint2 < helpAngleEnd)) {
                longestDist = point2Trans.distance(center) * 1.05;
            }
            if ((point3Trans.distance(center) > longestDist) && (angleLabelPoint3 > helpAngleStart) && (angleLabelPoint3 < helpAngleEnd)) {
                longestDist = point3Trans.distance(center) * 1.05;
            }
            if ((point4Trans.distance(center) > longestDist) && (angleLabelPoint4 > helpAngleStart) && (angleLabelPoint4 < helpAngleEnd)) {
                longestDist = point4Trans.distance(center) * 1.05;
            }
        }

        for (Node aNodesInGraph : nodesInGraph) {

            Point2D.Double point1Trans = (Point2D.Double) phyloGraphView.trans.d2w(phyloGraphView.getLabelRect(aNodesInGraph).getLocation());
            Point2D.Double point2Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph).getMinX(), (int) phyloGraphView.getLabelRect(aNodesInGraph).getMaxY()));
            Point2D.Double point3Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph).getMaxX(), (int) phyloGraphView.getLabelRect(aNodesInGraph).getMaxY()));
            Point2D.Double point4Trans = (Point2D.Double) phyloGraphView.trans.d2w(new Point((int) phyloGraphView.getLabelRect(aNodesInGraph).getMaxX(), (int) phyloGraphView.getLabelRect(aNodesInGraph).getMinY()));

            //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(point1Trans,point1Trans)).getShape(),"1", point1Trans));
            //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(point2Trans,point2Trans)).getShape(),"2", point2Trans));
            //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(point3Trans,point3Trans)).getShape(),"3", point3Trans));
            //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(point4Trans,point4Trans)).getShape(),"4", point4Trans));

            Line2D.Double helpLine = new Line2D.Double();
            Line2D.Double helpLine2 = new Line2D.Double();
            helpLine.setLine(point1Trans.getX(), point1Trans.getY(), point4Trans.getX(), point4Trans.getY());
            helpLine2.setLine(point2Trans.getX(), point2Trans.getY(), point3Trans.getX(), point3Trans.getY());

            //phyloGraphView.addWorldShape(new WorldShape(helpLine));

            Point2D.Double helpLineVec = normalizeVec(new Point2D.Double(helpLine.getX2() - helpLine.getX1(), helpLine.getY2() - helpLine.getY1()));
            Line2D.Double tmpLine = new Line2D.Double();
            tmpLine.setLine(helpLine.getX1(), helpLine.getY1(), helpLine.getX1() + helpLineVec.getX(), helpLine.getY1() + helpLineVec.getY());

            Point2D.Double helpLineVec2 = normalizeVec(new Point2D.Double(helpLine2.getX2() - helpLine2.getX1(), helpLine2.getY2() - helpLine2.getY1()));
            Line2D.Double tmpLine2 = new Line2D.Double();
            tmpLine2.setLine(helpLine2.getX1(), helpLine2.getY1(), helpLine2.getX1() + helpLineVec2.getX(), helpLine2.getY1() + helpLineVec2.getY());


            if (intersectLineCircle(tmpLine, center, longestDist) != null && intersectLineCircle(tmpLine2, center, longestDist) != null) {

                Point2D.Double intersect = intersectLineCircle(tmpLine, center, longestDist)[0];
                if (intersect.distance(point1Trans) > intersectLineCircle(tmpLine, center, longestDist)[1].distance(point1Trans))
                    intersect = intersectLineCircle(tmpLine, center, longestDist)[1];
                Point2D.Double intersect2 = intersectLineCircle(tmpLine2, center, longestDist)[0];
                if (intersect2.distance(point2Trans) > intersectLineCircle(tmpLine2, center, longestDist)[0].distance(point2Trans))
                    intersect2 = intersectLineCircle(tmpLine2, center, longestDist)[1];

                double angleIntersect = getAngle(intersect, center);
                double angleIntersect2 = getAngle(intersect2, center);

                //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(intersect,intersect)).getShape(),"i", intersect));
                //phyloGraphView.addWorldShape(new WorldShape(new WorldShape(new Line2D.Double(intersect2,intersect2)).getShape(),"j", intersect2));

                /*System.out.println("inside: " + (intersect.distance(point1Trans)+intersect.distance(point4Trans)==point4Trans.distance(point1Trans)));
                System.out.println("distance: " + (intersect.distance(center)>longestDist));
                System.out.println("angle>: " + (angleIntersect>=helpAngleStart));
                System.out.println("angle<: " + (angleIntersect<=helpAngleEnd));*/

                if ((intersect.distance(point1Trans) + intersect.distance(point4Trans) - point4Trans.distance(point1Trans) < 0.1) //intersection is inside the line
                        && (intersect.distance(center) > longestDist) && (angleIntersect >= helpAngleStart) && (angleIntersect <= helpAngleEnd)) {
                    do {
                        //System.out.println("do...1:  " + phyloGraph.getLabel(nodesInGraph[i]));
                        longestDist = intersect.distance(center) * 1.05;
                        angleIntersect = getAngle(intersectLineCircle(helpLine, center, longestDist)[0], center);
                    }
                    //while((angleIntersect>=helpAngleStart)&&(angleIntersect<=helpAngleEnd));
                    while (false);
                }
                if ((intersect2.distance(point2Trans) + intersect2.distance(point3Trans) - point2Trans.distance(point3Trans) < 0.1) //intersection is inside the line
                        && (intersect2.distance(center) > longestDist) && (angleIntersect2 >= helpAngleStart) && (angleIntersect2 <= helpAngleEnd)) {
                    do {
                        //System.out.println("do...2: " + phyloGraph.getLabel(nodesInGraph[i]));
                        longestDist = intersect2.distance(center) * 1.05;
                        angleIntersect2 = getAngle(intersectLineCircle(helpLine2, center, longestDist)[0], center);
                    }
                    //while((angleIntersect2>=helpAngleStart)&&(angleIntersect2<=helpAngleEnd));
                    while (false);
                }
            }
        }

        longestDist *= 1.10; //increase distance of arc to labels and nodes

        //*************** set circle start- and end point ***************

        double startAngleTmp;
        double endAngleTmp;
        double extendAngleTmp;
        Point2D.Double startVec = new Point2D.Double();
        Point2D.Double endVec = new Point2D.Double();
        startVec = normalizeVec(new Point2D.Double(startLine.getX2() - startLine.getX1(), startLine.getY2() - startLine.getY1())); // get vec of start line
        endVec = normalizeVec(new Point2D.Double(endLine.getX2() - endLine.getX1(), endLine.getY2() - endLine.getY1()));           // get vec of end line
        Point2D.Double circleStartPoint = new Point2D.Double();
        Point2D.Double circleEndPoint = new Point2D.Double();
        double elongateFactor = 0.75 + 0.25 * groupChildrenDepth; //factor which sets the distance of the sickle to the taxa with the longest distance to the center

        switch (boundaryMode) {
            case "mode1": {   // set start- & end point of sickle: intersection of circle with start vec & end vec

                double radiusTmp = longestDist * elongateFactor;
                Line2D.Double tmpStartLine = new Line2D.Double();
                Line2D.Double tmpEndLine = new Line2D.Double();

                startAngleTmp = getAngleOfNode(nodesInGraph[0]);                    // get angle of start node

                endAngleTmp = getAngleOfNode(nodesInGraph[nodesInGraph.length - 1]);  // get angle of end node

                extendAngleTmp = endAngleTmp - startAngleTmp;                         // get extended angle

                if (extendAngleTmp < 0) extendAngleTmp += 360;

                //startVec = normalizeVec(new Point2D.Double(startLine.getX2()-startLine.getX1(),startLine.getY2()-startLine.getY1())); // get vec of start line
                //endVec = normalizeVec(new Point2D.Double(endLine.getX2()-endLine.getX1(),endLine.getY2()-endLine.getY1()));           // get vec of end line

                Point2D.Double firstHelpNodePos = new Point2D.Double(startNode.getX() - longestDist * startVec.getX(), startNode.getY() - longestDist * startVec.getY());
                Point2D.Double lastHelpNodePos = new Point2D.Double(endNode.getX() - longestDist * endVec.getX(), endNode.getY() - longestDist * endVec.getY());

                startAngleTmp = startAngleTmp - 2 - Math.sqrt(extendAngleTmp) / 15; //enlarge angle --> overlap

                if (startAngleTmp < 0) startAngleTmp += 360;
                endAngleTmp = endAngleTmp + 2 + Math.sqrt(extendAngleTmp) / 15; //enlarge angle --> overlap

                if (endAngleTmp > 360) endAngleTmp -= 360;

                tmpStartLine.setLine(firstHelpNodePos.getX(), firstHelpNodePos.getY(),
                        firstHelpNodePos.getX() + getAngle2Vec(startAngleTmp).getX(), firstHelpNodePos.getY() + getAngle2Vec(startAngleTmp).getY());
                tmpEndLine.setLine(lastHelpNodePos.getX(), lastHelpNodePos.getY(),
                        lastHelpNodePos.getX() + getAngle2Vec(endAngleTmp).getX(), lastHelpNodePos.getY() + getAngle2Vec(endAngleTmp).getY());

                Point2D.Double[] circleStartPoint1Array = intersectLineCircle(tmpStartLine, center, radiusTmp); // get intersection of circle and start line

                circleStartPoint.setLocation(circleStartPoint1Array[0]);  // set start point


                Point2D.Double[] circleEndPoint1Array = intersectLineCircle(tmpEndLine, center, radiusTmp);  // get intersection of circle and end line

                circleEndPoint.setLocation(circleEndPoint1Array[0]); // set end point

                break;
            }
            case "mode2":   // set start- & end point of sickle from start node to end node with certain overlap

                startAngleTmp = getAngle(startNode, center);  // get angle of start node

                endAngleTmp = getAngle(endNode, center);      // get angle of end node

                extendAngleTmp = endAngleTmp - startAngleTmp;  // get extended angle

                if (extendAngleTmp < 0) extendAngleTmp += 360;

                startAngleTmp = startAngleTmp - 2 - Math.sqrt(extendAngleTmp) / 15; //enlarge angle --> overlap

                endAngleTmp = endAngleTmp + 2 + Math.sqrt(extendAngleTmp) / 15; //enlarge angle --> overlap


                circleStartPoint.setLocation(center.getX() + elongateFactor * longestDist * getAngle2Vec(startAngleTmp).getX(),
                        center.getY() + elongateFactor * longestDist * getAngle2Vec(startAngleTmp).getY()); // set start point

                circleEndPoint.setLocation(center.getX() + elongateFactor * longestDist * getAngle2Vec(endAngleTmp).getX(),
                        center.getY() + elongateFactor * longestDist * getAngle2Vec(endAngleTmp).getY());     // set end point


                startVec = normalizeVec((Point2D.Double) Geometry.diff(startNode, center)); //vector between startNode and center

                endVec = normalizeVec((Point2D.Double) Geometry.diff(endNode, center));     //vector between endNode and center


                break;
            case "mode3": {  // set start- & end point of sickle using free space to next and previous node

                //startVec = normalizeVec(new Point2D.Double(startLine.getX2()-startLine.getX1(),startLine.getY2()-startLine.getY1()));
                //endVec = normalizeVec(new Point2D.Double(endLine.getX2()-endLine.getX1(),endLine.getY2()-endLine.getY1()));

                double radiusTmp = longestDist * elongateFactor;

                Line2D.Double tmpStartLine = new Line2D.Double();
                Line2D.Double tmpEndLine = new Line2D.Double();

                // set start line from point between first node and previous node with the direction of start vec
                //tmpStartLine.setLine(midFirstPrevious.getX(),midFirstPrevious.getY(),midFirstPrevious.getX() + startVec.getX(),midFirstPrevious.getY() + startVec.getY());
                // set end line from point between last node and next node with the direction of end vec
                //tmpEndLine.setLine(midEndNext.getX(),midEndNext.getY(),midEndNext.getX() + endVec.getX(),midEndNext.getY() + endVec.getY());

                // set start line from point between first node and previous node to center
                tmpStartLine.setLine(center.getX(), center.getY(), midFirstPrevious.getX(), midFirstPrevious.getY());
                // set end line from point between last node and next node to center
                tmpEndLine.setLine(center.getX(), center.getY(), midEndNext.getX(), midEndNext.getY());

                Point2D.Double[] circleStartPoint1Array = intersectLineCircle(tmpStartLine, center, radiusTmp); // get intersection of circle and start line

                circleStartPoint.setLocation(circleStartPoint1Array[0]);  // set start point


                Point2D.Double[] circleEndPoint1Array = intersectLineCircle(tmpEndLine, center, radiusTmp);   // get intersection of circle and end line

                circleEndPoint.setLocation(circleEndPoint1Array[0]);     // set end point

                break;
            }
        }

        //*************** determine center of circle2 (needs real angle of circle...) ***************

        double tempAngle = getAngle(circleEndPoint, center) - getAngle(circleStartPoint, center);  // get extended angle of first circle
        if (tempAngle < 0) tempAngle += 360;


        Point2D.Double center2 = new Point2D.Double();    //center of second circle
        double center2factor = 0.6 + 0.4 * Math.sqrt(Math.sqrt(tempAngle / 360)); //factor which moves the center2 on the orthogonal vector, determines thickness of the sickle

        if (boundaryMode.equals("mode1") || boundaryMode.equals("mode3")) {

            Point2D.Double vecStartEnd1 = (Point2D.Double) Geometry.diff(circleStartPoint, circleEndPoint); // vector between circleStartPoint & circleEndPoint
            Point2D.Double middleVec1Point1 = new Point2D.Double((circleStartPoint.getX() + circleEndPoint.getX()) / 2, (circleStartPoint.getY() + circleEndPoint.getY()) / 2); //point between circleStartPoint- & circleEndPoint
            Point2D.Double middleVec1ort1 = normalizeVec(new Point2D.Double(vecStartEnd1.getY(), vecStartEnd1.getX() * -1)); //vector orthogonal to vecStartEnd1

            double dist = middleVec1Point1.distance(center);

            if (helpExtendAngle > 135)
                center2.setLocation(middleVec1Point1.getX() + middleVec1ort1.getX() * dist * center2factor, middleVec1Point1.getY() + middleVec1ort1.getY() * dist * center2factor);
            else
                center2.setLocation(middleVec1Point1.getX() - middleVec1ort1.getX() * dist * center2factor, middleVec1Point1.getY() - middleVec1ort1.getY() * dist * center2factor);
        } else if (boundaryMode.equals("mode2")) {
            if (helpExtendAngle > 135)
                center2.setLocation(middleVec1Point.getX() + center2factor * -radius * middleVec1ort.getX(), middleVec1Point.getY() + center2factor * -radius * middleVec1ort.getY());
            else
                center2.setLocation(middleVec1Point.getX() + center2factor * radius * middleVec1ort.getX(), middleVec1Point.getY() + center2factor * radius * middleVec1ort.getY());
        }
        //phyloGraphView.addShape(new WorldShape("Z", center2));

        // ***************set elipse width and height ***************

        double elipseWidth = 2 * circleStartPoint.distance(center);  // set diameter of circle 2* the radius
        double elipseHeight = 2 * circleStartPoint.distance(center);

        double elipseWidth2 = 2 * circleStartPoint.distance(center2);
        double elipseHeight2 = 2 * circleStartPoint.distance(center2);

        //*************** set start & extendAngles ***************

        double startAngle;
        double extendAngle;

        startAngle = getAngle(circleStartPoint, center);             // angle of the vector startPoint -> center
        extendAngle = getAngle(circleEndPoint, center) - startAngle;   // range of angle to draw
        if (extendAngle < 0) extendAngle += 360;                         // angle crosses 0°

        double startAngle2;
        double extendAngle2;

        startAngle2 = getAngle(circleStartPoint, center2);
        extendAngle2 = getAngle(circleEndPoint, center2) - startAngle2;
        if (extendAngle2 < 0) extendAngle2 += 360;

        //*************** calculate position of groupLabel and add groupLabel-worldShape ***************

        double labelFactor = elongateFactor + 0.15;       // factor which determines distance between label and sickle
        Point2D.Double positionOfLabel = new Point2D.Double(center.getX() - middleVec1ort.getX() * labelFactor * longestDist, center.getY() - middleVec1ort.getY() * labelFactor * longestDist);

        //     WorldShape dummyTxtShape = new WorldShape(new Line2D.Double(positionOfLabel,positionOfLabel));
        //     WorldShape txtShape = new WorldShape(dummyTxtShape.getShape(), groupName, positionOfLabel);
//        phyloGraphView.addWorldShape(txtShape);


        //phyloGraphView.addWorldShape(new WorldShape(groupName, positionOfLabel));

        //*************** add sector of a circle-Worldshape ***************

        Area fillShape = new Area();
        // first arc of sickle
        Arc2D.Double firstArc = new Arc2D.Double(center2.getX() - elipseWidth2 / 2, center2.getY() - elipseHeight2 / 2, elipseWidth2, elipseHeight2, startAngle2, extendAngle2, Arc2D.OPEN);
        // second arc of sickle
        Arc2D.Double secondArc = new Arc2D.Double(center.getX() - elipseWidth / 2, center.getY() - elipseHeight / 2, elipseWidth, elipseHeight, startAngle, extendAngle, Arc2D.OPEN);


        // fill area bewteen the sickles
        /*Area firstArcArea = new Area(firstArc);
        Area secondArcArea = new Area(secondArc);

        fillShape.add(firstArcArea);
        fillShape.subtract(secondArcArea);

        phyloGraphView.addWorldShape(new WorldShape(fillShape));*/

        //    WorldShape firstArcShape = new WorldShape(firstArc);
        //    firstArcShape.setLineWidth(arcLineWidth);
        //    WorldShape secondArcShape = new WorldShape(secondArc);
        //    secondArcShape.setLineWidth(arcLineWidth);


//        if(arcMode.equals("arc") || arcMode.equals("sickle")) phyloGraphView.addWorldShape(firstArcShape);   // add arc world shapes

//        if(arcMode.equals("sickle")) phyloGraphView.addWorldShape(secondArcShape);  // add arc world shapes

        //*************** add boundary appearance ***************

        double lineLength = radius / 30 + startNode.distance(endNode) / 60;

        // set boundary line 1, from circle start point + small amount in both directions of start vec
        Line2D.Double boundaryLine1 = new Line2D.Double(circleStartPoint.getX() + startVec.getX() * lineLength, circleStartPoint.getY() + startVec.getY() * lineLength,
                circleStartPoint.getX() - startVec.getX() * lineLength, circleStartPoint.getY() - startVec.getY() * lineLength);
        // set boundary line 2, from circle end point + small amount in both directions of end vec
        Line2D.Double boundaryLine2 = new Line2D.Double(circleEndPoint.getX() + endVec.getX() * lineLength, circleEndPoint.getY() + endVec.getY() * lineLength,
                circleEndPoint.getX() - endVec.getX() * lineLength, circleEndPoint.getY() - endVec.getY() * lineLength);

        Point2D.Double startCenterVec = normalizeVec(new Point2D.Double(circleStartPoint.getX() - center.getX(), circleStartPoint.getY() - center.getY()));
        Point2D.Double endCenterVec = normalizeVec(new Point2D.Double(circleEndPoint.getX() - center.getX(), circleEndPoint.getY() - center.getY()));

        Line2D.Double boundaryLineTmp1 = new Line2D.Double(circleStartPoint.getX() + startCenterVec.getX() * lineLength, circleStartPoint.getY() + startCenterVec.getY() * lineLength,
                circleStartPoint.getX() - startCenterVec.getX() * lineLength, circleStartPoint.getY() - startCenterVec.getY() * lineLength);
        Line2D.Double boundaryLineTmp2 = new Line2D.Double(circleEndPoint.getX() + endCenterVec.getX() * lineLength, circleEndPoint.getY() + endCenterVec.getY() * lineLength,
                circleEndPoint.getX() - endCenterVec.getX() * lineLength, circleEndPoint.getY() - endCenterVec.getY() * lineLength);

        //     WorldShape line1 = new WorldShape(boundaryLine1);   // new line world shape
        //     WorldShape line2 = new WorldShape(boundaryLine2);   // new line world shape

        if (boundaryMode.equals("mode3")) {
            //           line1 = new WorldShape(boundaryLineTmp1);
            //           line2 = new WorldShape(boundaryLineTmp2);
        }

//        line1.setLineWidth(arcLineWidth);
//        line2.setLineWidth(arcLineWidth);

        switch (boundaryAppearance) {
            case "line":     // boundary mode: line

//            phyloGraphView.addWorldShape(line1); // add line1 world shape
//            phyloGraphView.addWorldShape(line2); // add line2 world shape
                break;
            case "circle":     // boundary mode: circle

                double circleRadius = radius / 30 + startNode.distance(endNode) / 60;

                Arc2D.Double firstBoundaryCircle = new Arc2D.Double(circleStartPoint.getX() - circleRadius, circleStartPoint.getY() - circleRadius, circleRadius * 2, circleRadius * 2, 0, 360, Arc2D.CHORD);
                Arc2D.Double secondBoundaryCircle = new Arc2D.Double(circleEndPoint.getX() - circleRadius, circleEndPoint.getY() - circleRadius, circleRadius * 2, circleRadius * 2, 0, 360, Arc2D.OPEN);

                //           WorldShape firstBoundaryCircleShape = new WorldShape(firstBoundaryCircle);
                //           firstBoundaryCircleShape.setLineWidth(arcLineWidth);
//            WorldShape secondBoundaryCircleShape = new WorldShape(secondBoundaryCircle);
//            secondBoundaryCircleShape.setLineWidth(arcLineWidth);

//            phyloGraphView.addWorldShape(firstBoundaryCircleShape); // add cirlce1 world shape
//            phyloGraphView.addWorldShape(secondBoundaryCircleShape); // add cirlce2 world shape
                break;
            case "square":      // boundary mode: square
                double squareWidth = radius / 20 + startNode.distance(endNode) / 40;

                Rectangle2D.Double firstSquare = new Rectangle2D.Double(circleStartPoint.getX() - squareWidth * 0.5, circleStartPoint.getY() - squareWidth * 0.5, squareWidth, squareWidth);
                Rectangle2D.Double secondSquare = new Rectangle2D.Double(circleEndPoint.getX() - squareWidth * 0.5, circleEndPoint.getY() - squareWidth * 0.5, squareWidth, squareWidth);

                //           WorldShape firstSquareShape = new WorldShape(firstSquare);
                //           firstSquareShape.setLineWidth(arcLineWidth);
                //           WorldShape secondSquareShape = new WorldShape(secondSquare);
                //           secondSquareShape.setLineWidth(arcLineWidth);

//            phyloGraphView.addWorldShape(firstSquareShape);   // add square1 world shape
//            phyloGraphView.addWorldShape(secondSquareShape);  // add square2 world shape

                break;
            case "arrow":      // boundary mode: arrow

                Point2D.Double arrowStartPoint = new Point2D.Double();
                Point2D.Double arrowEndPoint = new Point2D.Double();

                //     phyloGraphView.addWorldShape(line1);  // add line1 world shape
                //     phyloGraphView.addWorldShape(line2);  // add line2 world shape

                double arrowStartAngle = getAngle(circleStartPoint, center);
                double arrowEndAngle = getAngle(circleEndPoint, center);
                double arrowExtendAngle = arrowEndAngle - arrowStartAngle;
                if (arrowExtendAngle < 0) arrowExtendAngle += 360;

                arrowStartAngle = arrowStartAngle + 2 + Math.sqrt(arrowExtendAngle) / 15; //reduce angle

                if (arrowStartAngle > 360) arrowStartAngle -= 360;
                arrowEndAngle = arrowEndAngle - 2 - Math.sqrt(arrowExtendAngle) / 15; //reduce angle

                if (arrowEndAngle < 0) arrowEndAngle += 360;

                Point2D.Double arrowStartVec = normalizeVec(getAngle2Vec(arrowStartAngle));
                Point2D.Double arrowEndVec = normalizeVec(getAngle2Vec(arrowEndAngle));

                arrowStartPoint.setLocation(center.getX() + elongateFactor * longestDist * arrowStartVec.getX(),
                        center.getY() + elongateFactor * longestDist * arrowStartVec.getY());

                arrowEndPoint.setLocation(center.getX() + elongateFactor * longestDist * arrowEndVec.getX(),
                        center.getY() + elongateFactor * longestDist * arrowEndVec.getY());


                Line2D.Double arrowLineStart1 = new Line2D.Double(arrowStartPoint.getX() + lineLength * arrowStartVec.getX(), arrowStartPoint.getY() + lineLength * arrowStartVec.getY(),
                        arrowStartPoint.getX() - lineLength * arrowStartVec.getX(), arrowStartPoint.getY() - lineLength * arrowStartVec.getY());

                Line2D.Double arrowLineStart2 = new Line2D.Double(circleStartPoint.getX(), circleStartPoint.getY(), arrowStartPoint.getX() + lineLength * arrowStartVec.getX(), arrowStartPoint.getY() + lineLength * arrowStartVec.getY());
                Line2D.Double arrowLineStart3 = new Line2D.Double(circleStartPoint.getX(), circleStartPoint.getY(), arrowStartPoint.getX() - lineLength * arrowStartVec.getX(), arrowStartPoint.getY() - lineLength * arrowStartVec.getY());

                //        WorldShape arrowLineStart1Shape = new WorldShape(arrowLineStart1);
                //        arrowLineStart1Shape.setLineWidth(this.arcLineWidth);
                //        WorldShape arrowLineStart2Shape = new WorldShape(arrowLineStart2);
                //        arrowLineStart2Shape.setLineWidth(this.arcLineWidth);
                //        WorldShape arrowLineStart3Shape = new WorldShape(arrowLineStart3);
                //         arrowLineStart3Shape.setLineWidth(this.arcLineWidth);

                //         phyloGraphView.addWorldShape(arrowLineStart1Shape);
                //         phyloGraphView.addWorldShape(arrowLineStart2Shape);
                //         phyloGraphView.addWorldShape(arrowLineStart3Shape);

                Line2D.Double arrowLineEnd1 = new Line2D.Double(arrowEndPoint.getX() + lineLength * arrowEndVec.getX(), arrowEndPoint.getY() + lineLength * arrowEndVec.getY(),
                        arrowEndPoint.getX() - lineLength * arrowEndVec.getX(), arrowEndPoint.getY() - lineLength * arrowEndVec.getY());

                Line2D.Double arrowLineEnd2 = new Line2D.Double(circleEndPoint.getX(), circleEndPoint.getY(), arrowEndPoint.getX() + lineLength * arrowEndVec.getX(), arrowEndPoint.getY() + lineLength * arrowEndVec.getY());
                Line2D.Double arrowLineEnd3 = new Line2D.Double(circleEndPoint.getX(), circleEndPoint.getY(), arrowEndPoint.getX() - lineLength * arrowEndVec.getX(), arrowEndPoint.getY() - lineLength * arrowEndVec.getY());

                //       WorldShape arrowLineEnd1Shape = new WorldShape(arrowLineEnd1);
                //       arrowLineEnd1Shape.setLineWidth(this.arcLineWidth);
                //       WorldShape arrowLineEnd2Shape = new WorldShape(arrowLineEnd2);
                //       arrowLineEnd2Shape.setLineWidth(this.arcLineWidth);
                //       WorldShape arrowLineEnd3Shape = new WorldShape(arrowLineEnd3);
                //       arrowLineEnd3Shape.setLineWidth(this.arcLineWidth);

                //      phyloGraphView.addWorldShape(arrowLineEnd1Shape);
                //      phyloGraphView.addWorldShape(arrowLineEnd2Shape);
                //      phyloGraphView.addWorldShape(arrowLineEnd3Shape);

                break;
        }

    }

    private int[] getCycle() {

        if (cycle == null) {

            Point2D.Double allPoints = new Point2D.Double();
            int[] labelCycle = new int[phyloGraph.getNodeLabels().size()];
            Vector nodes = new Vector();

            for (Iterator allNodes = phyloGraph.nodeIterator(); allNodes.hasNext(); ) {
                Node currentNode = (Node) allNodes.next();
                if (phyloGraph.getLabel(currentNode) != null) {
                    Point2D currentPoint = phyloGraphView.getLocation(currentNode);
                    allPoints.setLocation(allPoints.getX() + currentPoint.getX(), allPoints.getY() + currentPoint.getY());
                    nodes.add(currentNode);
                }
            }

            Point2D.Double schwerpunkt = new Point2D.Double(allPoints.getX() / phyloGraph.getNodeLabels().size(), allPoints.getY() / phyloGraph.getNodeLabels().size());

            for (int i = 0; i < labelCycle.length; i++) {
                labelCycle[i] = (Integer) phyloGraph.getTaxa((Node) nodes.get(0)).iterator().next();
                Node currentNodeI = (Node) nodes.get(0);
                Point2D currentPointI = phyloGraphView.getLocation(currentNodeI);
                for (int j = 1; j < nodes.size(); j++) {
                    Node currentNodeJ = (Node) nodes.get(j);
                    Point2D currentPointJ = phyloGraphView.getLocation(currentNodeJ);
                    if (this.getAngle(schwerpunkt, new Point2D.Double(currentPointJ.getX(), currentPointJ.getY())) >
                            (this.getAngle(schwerpunkt, new Point2D.Double(currentPointI.getX(), currentPointI.getY())))) {
                        labelCycle[i] = (Integer) phyloGraph.getTaxa((Node) nodes.get(j)).iterator().next();
                        currentNodeI = currentNodeJ;
                        currentPointI = currentPointJ;
                    }
                }
                nodes.remove(currentNodeI);
            }

            cycle = labelCycle;
        }
        return cycle;
    }

    /**
     * determine the correct start- and endNode and switch the positions (only necessary if extendAngle >180°)
     *
     * @param nodesInGraph
     * @return array of nodes in correct order
     */
    private Node[] checkOrder(Node[] nodesInGraph) {

        double startAngle = getAngleOfNode(nodesInGraph[0]);
        double lastAngle = getAngleOfNode(nodesInGraph[nodesInGraph.length - 1]);

        //System.out.println("startAngle: " + startAngle + ", lastAngle: " + lastAngle);

        if (lastAngle - startAngle > 180) {
            //  for(int i=0;i<nodesInGraph.length;i++) System.out.println("nodes before: " + graph.getLabel(nodesInGraph[i]));
            int start = 0, end = nodesInGraph.length - 1;
            double largestAngle = 0;
            for (int i = 0; i < nodesInGraph.length; i++) {
                double helpAngle2;

                if (i == nodesInGraph.length - 1)
                    helpAngle2 = 360 - getAngleOfNode(nodesInGraph[i]) + getAngleOfNode(nodesInGraph[0]);
                else helpAngle2 = getAngleOfNode(nodesInGraph[i + 1]) - getAngleOfNode(nodesInGraph[i]);

                if (helpAngle2 > largestAngle) {         //search largest angle between any involved taxa
                    largestAngle = helpAngle2;
                    start = i + 1;
                    end = i;
                }
                if (start == nodesInGraph.length) start = 0;
            }
            //System.out.println("start: " + start + ", end: " + end + ", largestAngle: " + largestAngle);

            Vector tmp = new Vector();
            for (Node aNodesInGraph : nodesInGraph) tmp.add(tmp.size(), aNodesInGraph);
            Node tmpVal = (Node) tmp.elementAt(start);
            Node tmpVal2 = (Node) tmp.elementAt(end);
            tmp.remove(start);
            tmp.add(0, tmpVal);
            if (start > end) tmp.remove(end + 1);
            else tmp.remove(end);
            tmp.add(tmpVal2);

            tmp.copyInto(nodesInGraph);

            //for(int i=0;i<nodesInGraph.length;i++) System.out.println("nodes after: " + graph.getLabel(nodesInGraph[i]));
        }
        return nodesInGraph;
    }

    /**
     * calculates intersection of two lines, in case of no or infinite intersection return null
     *
     * @param line1
     * @param line2
     * @return intersection
     */
    private Point2D.Double lineIntersection(Line2D.Double line1, Line2D.Double line2) {

        Point2D.Double intersection = new Point2D.Double();

        double m1 = (line1.y2 - line1.y1) / (line1.x2 - line1.x1);
        double m2 = (line2.y2 - line2.y1) / (line2.x2 - line2.x1);
        double xIntersection;
        double yIntersection;

        if (m1 == m2) {
            //System.err.println("no or infinite intersection points");
            return null;
        }

        if (line1.x2 == line1.x1) {                           //(Math.abs(line1.x2-line1.x1)<Math.abs((line1.y2-line1.y1)/1000)){
            xIntersection = line1.x1;
            yIntersection = line2.y1 + ((line1.x1 - line2.x1) / (line2.x2 - line2.x1)) * (line2.y2 - line2.y1);
        } else if (line2.x2 == line2.x1) {                       //(Math.abs(line2.x2-line2.x1)<Math.abs((line2.y2-line2.y1)/1000)){
            xIntersection = line2.x1;
            yIntersection = line1.y1 + ((line2.x1 - line1.x1) / (line1.x2 - line1.x1)) * (line1.y2 - line1.y1);
        } else {

            double b1 = line1.y1 - line1.x1 * m1;
            double b2 = line2.y1 - line2.x1 * m2;

            xIntersection = (b1 - b2) / (m2 - m1);
            yIntersection = m1 * xIntersection + b1;
        }

        intersection.setLocation(xIntersection, yIntersection);

        return intersection;
    }


    /**
     * calculates the angle of the vector oppositeNode-node (-->  = 0°)
     *
     * @param node
     * @return the angle
     */
    private double getAngleOfNode(Node node) {

        double angle = 0;

        Edge e = phyloGraph.getFirstAdjacentEdge(node);
        Node helpNode = phyloGraph.getOpposite(node, e);

        Point2D.Double Vec = new Point2D.Double(phyloGraphView.getLocation(helpNode).getX() - phyloGraphView.getLocation(node).getX(),
                phyloGraphView.getLocation(helpNode).getY() - phyloGraphView.getLocation(node).getY());

        if (Vec.getX() != 0 || Vec.getY() != 0)
            angle = Math.toDegrees(Math.acos(Vec.getY() / Math.sqrt(Vec.getX() * Vec.getX() + Vec.getY() * Vec.getY())));
        if (Vec.getX() < 0) angle = 360 - angle;
        angle += 90;
        if (angle > 360) angle -= 360;

        return angle;

    }

    /**
     * calculates the angle of vector point2-point1 (-->  = 0°)
     *
     * @param point1
     * @param point2
     * @return the angle of the vector point2-point1
     */
    private double getAngle(Point2D.Double point1, Point2D.Double point2) {

        double angle = 0;

        Point2D.Double Vec = new Point2D.Double(point2.getX() - point1.getX(), point2.getY() - point1.getY());

        if (Vec.getX() != 0 || Vec.getY() != 0)
            angle = Math.toDegrees(Math.acos(Vec.getY() / Math.sqrt(Vec.getX() * Vec.getX() + Vec.getY() * Vec.getY())));
        if (Vec.getX() < 0) angle = 360 - angle;
        angle += 90;
        if (angle > 360) angle -= 360;

        return angle;

    }

    /*
    *  returns a point on the unit circle to a given angle
    *
    * @param angle
    * @return point on the unit circle
    */

    private Point2D.Double getAngle2Vec(double angle) {
        Point2D.Double point = new Point2D.Double();
        point.setLocation(Math.cos(Math.toRadians(angle)), -Math.sin(Math.toRadians(angle)));
        return point;
    }


    /*
    *  normalize a vector
    *
    * @param vec
    * @return normalized vector
    */

    private Point2D.Double normalizeVec(Point2D.Double vec) {
        vec.setLocation(vec.getX() / Math.sqrt(vec.getX() * vec.getX() + vec.getY() * vec.getY()), vec.getY() / Math.sqrt(vec.getX() * vec.getX() + vec.getY() * vec.getY()));
        return vec;
    }

    /*
    *  intersection between line and circle
    *
    * @param line
    * @param circleCenter
    * @param radius
    * @return two intersection points or null if there is none (if there is one intersection, both points will be the same)
    */

    private Point2D.Double[] intersectLineCircle(Line2D.Double line, Point2D.Double circleCenter, double radius) {

        Point2D.Double[] intersections = new Point2D.Double[2];

        double m = (line.getY2() - line.getY1()) / (line.getX2() - line.getX1()); //gradient of line
        if (line.getX2() == line.getX1()) m = 10000;   //cheating... otherwise m would be infinite
        double n = line.getY1() - m * line.getX1();     // intersection with y-axis
        double s = n - circleCenter.getY() + m * circleCenter.getX(); // correction of n due to the center of the circle
        double wurzel = (4 * m * m * s * s) - (4 * (m * m + 1) * (s * s - radius * radius));

        if (wurzel < 0) return null;                   //no intersection

        double x1 = (-2 * m * s + Math.sqrt(wurzel)) / (2 * (m * m + 1));
        double x2 = (-2 * m * s - Math.sqrt(wurzel)) / (2 * (m * m + 1));

        intersections[0] = new Point2D.Double(x1 + circleCenter.getX(), m * x1 + s + circleCenter.getY());
        intersections[1] = new Point2D.Double(x2 + circleCenter.getX(), m * x2 + s + circleCenter.getY());

        Point2D.Double help = new Point2D.Double();

        //switch intersections: first intersection is in the direction of the line
        if (line.getP2().distance(circleCenter) > radius) { //p2 ouside the circle
            if (line.getP2().distance(intersections[1]) < line.getP2().distance(intersections[0])) { // if intersections[0] is wrong
                help = intersections[1];              //switch
                intersections[1] = intersections[0];
                intersections[0] = help;
            }
        } else { //p2 inside the circle
            if (line.getP2().distance(intersections[0]) > line.getP1().distance(intersections[0])) {
                help = intersections[1];              //switch
                intersections[1] = intersections[0];
                intersections[0] = help;
            }
        }

        return intersections;
    }

    /**
     * gets the next node in cyclic order
     *
     * @param node
     * @return the next node
     */
    private Node getNextNodeInCylce(Node node) {
        int[] taxaInCyclicOrder = this.getCycle();

        int positionOfTaxa = phyloGraph.getTaxa(node).iterator().next();

        // find pos of taxaId in taxaInCyclicOrder
        for (int j = 0; j < taxaInCyclicOrder.length; j++) {
            if (taxaInCyclicOrder[j] == positionOfTaxa) {
                positionOfTaxa = j;
                break;
            }
        }
        //first taxa in cyclic order --> last element is next
        if (positionOfTaxa == 0) positionOfTaxa = taxaInCyclicOrder.length;

        return phyloGraph.getTaxon2Node(taxaInCyclicOrder[positionOfTaxa - 1]);
    }


    /**
     * gets the previous node in cyclic order
     *
     * @param node
     * @return the previous node
     */
    private Node getPreviousNodeInCylce(Node node) {
        int[] taxaInCyclicOrder = this.getCycle();

        int positionOfTaxa = phyloGraph.getTaxa(node).iterator().next();

        // find pos of taxaId in taxaInCyclicOrder
        for (int j = 0; j < taxaInCyclicOrder.length; j++) {
            if (taxaInCyclicOrder[j] == positionOfTaxa) {
                positionOfTaxa = j;
                break;
            }
        }
        //last taxa in cyclic order --> first element is previous
        if (positionOfTaxa == taxaInCyclicOrder.length - 1) positionOfTaxa = 0;

        return phyloGraph.getTaxon2Node(taxaInCyclicOrder[positionOfTaxa + 1]);
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
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "ShowTaxaSetViewer";
    }
}
