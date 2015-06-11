/**
 * FilterTaxaPanel.java 
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
/** The taxa window
 *
 * @author Markus Franz
 */
package splitstree.gui.algorithms.filter;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.gui.ActionJList;
import jloda.gui.director.IUpdateableView;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Basic;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.gui.main.MainViewerActions;
import splitstree.nexus.Taxa;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * The select taxa panel
 */
public class FilterTaxaPanel extends JPanel implements IUpdateableView, UpdateableActions {
    java.util.List<Action> all = new LinkedList<>();
    private DefaultListModel<String> listl = null;
    private DefaultListModel<String> listr = null;
    //private JList jlistl = null;
    //private JList jlistr = null;
    private ActionJList jlistl = null;
    private ActionJList jlistr = null;
    JLabel descriptionLabel = null;

    private Director dir;

    //constructor
    public FilterTaxaPanel(Director dir, PhyloGraphView phyloView) {
        this.dir = dir;

        this.listl = new DefaultListModel<>();
        this.listr = new DefaultListModel<>();

        descriptionLabel = new JLabel();

        GridBagLayout gridBag = new GridBagLayout();
        setLayout(gridBag);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(new JLabel("Show"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.gridheight = 4;
        gbc.weighty = 2;
        gbc.weightx = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridy = 1;

        //this.jlistl = new JList(listl);
        this.jlistl = new ActionJList<>(listl); //Using the new kind of JList I created
        JComponent input = new JScrollPane(jlistl);
        input.setToolTipText("Taxa included in computations");
        gridBag.setConstraints(input, gbc);
        add(input);

        //Using custom actionListener for JList (listens for double-click and return key)
        jlistl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] indices = jlistl.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    listr.addElement(listl.remove(indices[i]));
                }
            }
        });

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 1;
        gbc.gridy = 1;

        input = new JButton(getShowAction());
        gridBag.setConstraints(input, gbc);
        add(input);

        gbc.gridx = 1;
        gbc.gridy = 2;

        input = new JButton(getHideAction());
        gridBag.setConstraints(input, gbc);
        add(input, gbc);


        gbc.gridx = 1;
        gbc.gridy = 3;

        input = new JButton(getShowAllAction());
        gridBag.setConstraints(input, gbc);
        add(input, gbc);

        input = new JButton(getHideAllAction());
        gbc.gridx = 1;
        gbc.gridy = 4;
        gridBag.setConstraints(input, gbc);
        add(input, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridx = 2;
        gbc.gridy = 0;
        add(new JLabel("Hide"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.gridheight = 4;
        gbc.weighty = 2;
        gbc.weightx = 2;
        gbc.gridx = 2;
        gbc.gridy = 0;

        gbc.gridy = 1;

        //jlistr = new JList(listr);
        jlistr = new ActionJList<>(listr); //Using the new kind of JList I created
        input = new JScrollPane(jlistr);
        input.setToolTipText("Taxa excluded from computations");
        gridBag.setConstraints(input, gbc);
        add(input);

        //Using custom actionListener for JList (listens for double-click and return key)
        jlistr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] indices = jlistr.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    listl.addElement(listr.remove(indices[i]));
                }
            }
        });


        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridx = 3;
        gbc.gridy = 0;
        input = new JButton(getApplyAction());
        gridBag.setConstraints(input, gbc);
        add(input, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;

        input = new JButton(getShowSelectedAction(phyloView));
        gridBag.setConstraints(input, gbc);
        add(input, gbc);

        gbc.gridx = 2;
        gbc.gridy = 6;

        input = new JButton(getHideSelectedAction(phyloView));
        gridBag.setConstraints(input, gbc);
        add(input, gbc);


        descriptionLabel.setText(" ");
        Box box = Box.createHorizontalBox();
        box.add(descriptionLabel);
        box.add(Box.createHorizontalStrut(500));
        box.setBorder(BorderFactory.createEtchedBorder());
        box.setMinimumSize(new Dimension(100, 20));

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.1;
        gbc.weightx = 0.1;
        gbc.gridwidth = 4;
        gbc.gridheight = 2;
        gbc.gridx = 0;
        gbc.gridy = 7;
        gridBag.setConstraints(box, gbc);
        add(box, gbc);
        setSize(500, 210); // 2 do
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            return;
        }
        Document doc = dir.getDocument();
        if (doc.getTaxa() == null) {
            descriptionLabel.setText(" ");
            return;
        }
        String aline = "nTaxa= " + doc.getTaxa().getNtax();

        int origNtax;
        if (doc.getTaxa().getOriginalTaxa() != null) {
            origNtax = doc.getTaxa().getOriginalTaxa().getNtax();
            aline += " (" + (origNtax - doc.getTaxa().getNtax()) + " of " + origNtax + " hidden)";
        } else
            origNtax = doc.getTaxa().getNtax();
        descriptionLabel.setText(aline);

        this.listl.clear();
        this.listr.clear();

        for (int i = 1; i <= origNtax; i++) {
            int index = doc.getTaxa().indexOf(doc.getTaxa().getOriginalTaxa().getLabel(i));
            if (index > 0)
                listl.addElement("[" + index + "] '" + doc.getTaxa().getOriginalTaxa().getLabel(i) + "'");
            else
                listr.addElement("'" + doc.getTaxa().getOriginalTaxa().getLabel(i) + "'");
        }
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);
        // because we don't want to duplicate that code
        if (dir.getDocument().isValidByName(Taxa.NAME))
            getApplyAction().setEnabled(true);
        else
            getApplyAction().setEnabled(false);
        /*
        for (Iterator it = all.iterator(); it.hasNext();) {
            AbstractAction action = (AbstractAction) it.next();
            if ((action.getValue(MainViewerActions.DEPENDS_ON_NODESELECTION) != null))
                    action.setEnableCancel(dir.getMainViewer().getNumberSelectedNodes() >0);
            if((action.getValue(MainViewerActions.DEPENDS_ON_EDGESELECTION) != null))
            action.setEnableCancel(dir.getMainViewer().getNumberSelectedEdges() >0);
        }
        */
    }

    /**
     * update the enable state
     */
    public void setEnableCritical(boolean flag) {
        for (Action action : all) {
            if ((Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(flag);
        }
    }

    // All the action listeners:

    private AbstractAction hideAction;

    private AbstractAction getHideAction() {
        if (hideAction != null)
            return hideAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] indices = jlistl.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {

                    listr.addElement(listl.remove(indices[i]));
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Hide >");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide selected taxa");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return hideAction = action;
    }

    private AbstractAction hideSelectedAction;

    private AbstractAction getHideSelectedAction(final PhyloGraphView phyloView) {
        if (hideSelectedAction != null)
            return hideSelectedAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    PhyloGraph graph = phyloView.getPhyloGraph();

                    NodeSet selectedNodes = phyloView.getSelectedNodes();
                    for (Node v : selectedNodes) {
                        java.util.List taxa = graph.getNode2Taxa(v);
                        for (Object aTaxa : taxa) {
                            int id = (Integer) aTaxa;
                            String name = dir.getDocument().getTaxa().getLabel(id);
                            int index;
                            for (index = 0; index < listl.size(); index++) {
                                String lname = (String) listl.getElementAt(index);
                                if (lname.endsWith(name + "'"))
                                    break;
                            }
                            if (index < listl.size()) {
                                // Object obj = listl.getElementAt(index);
                                listl.remove(index);
                                listr.addElement("'" + name + "'");
                            }
                        }
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "From graph");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide taxa selected in graph");
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return hideSelectedAction = action;
    }

    private AbstractAction showAction;

    private AbstractAction getShowAction() {
        if (showAction != null)
            return showAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] indices = jlistr.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {

                    listl.addElement(listr.remove(indices[i]));
                }
            }
        };
        action.putValue(AbstractAction.NAME, "< Show");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show selected taxa");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showAction = action;
    }

    private AbstractAction showSelectedAction;

    private AbstractAction getShowSelectedAction(final PhyloGraphView phyloView) {
        if (showSelectedAction != null)
            return showSelectedAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    PhyloGraph graph = phyloView.getPhyloGraph();

                    listl.clear();
                    listr.clear();
                    Iterator it = graph.nodeIterator();
                    while (it.hasNext()) {
                        Node v = (Node) it.next();
                        java.util.List taxaInGraph = graph.getNode2Taxa(v);
                        for (Object aTaxaInGraph : taxaInGraph) {
                            int id = (Integer) aTaxaInGraph;
                            String name = "'" + dir.getDocument().getTaxa().getLabel(id) + "'";
                            if (phyloView.getSelected(v)) {
                                listl.addElement(name);
                            } else {
                                listr.addElement(name);
                            }
                        }
                    }
                    // add all the invisible ones:
                    if (dir.getDocument().getTaxa().getOriginalTaxa() != null) {
                        Taxa origTaxa = dir.getDocument().getTaxa().getOriginalTaxa();
                        for (int od = 1; od <= origTaxa.getNtax(); od++) {
                            String name = "'" + origTaxa.getLabel(od) + "'";
                            if (!listl.contains(name) && !listr.contains(name))
                                listr.addElement(name);
                        }
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "From graph");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show taxa selected in graph");
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showSelectedAction = action;
    }

    private AbstractAction showAllAction;

    private AbstractAction getShowAllAction() {
        if (showAllAction != null)
            return showAllAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                while (listr.size() != 0) {
                    listl.addElement(listr.remove(0));
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Show all");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show all taxa");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showAllAction = action;
    }

    private AbstractAction hideAllAction;

    private AbstractAction getHideAllAction() {
        if (hideAllAction != null)
            return hideAllAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                while (listl.size() != 0) {
                    listr.addElement(listl.remove(0));
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Hide all");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide all taxa");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return hideAllAction = action;
    }

    private AbstractAction applyAction;

    private AbstractAction getApplyAction() {
        if (applyAction != null)
            return applyAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String extaxa = "";

                for (int i = 0; i < listr.getSize(); i++) {
                    if (((String) listr.getElementAt(i)).indexOf(':') != -1)
                        extaxa += (" " + ((String) listr.getElementAt(i)).substring(((String) listr.getElementAt(i)).indexOf(':') + 2));
                    else
                        extaxa += (" " + listr.getElementAt(i));
                }

                try {
                    dir.execute("assume extaxa=" + extaxa);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Recompute data on shown taxa");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyAction = action;
    }
}


