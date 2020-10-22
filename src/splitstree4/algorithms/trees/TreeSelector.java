/**
 * TreeSelector.java
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
 *
 * @version $Id: TreeSelector.java,v 1.22 2008-07-01 15:01:56 bryant Exp $
 * @author Daniel Huson and David Bryant
 * @version $Id: TreeSelector.java,v 1.22 2008-07-01 15:01:56 bryant Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: TreeSelector.java,v 1.22 2008-07-01 15:01:56 bryant Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */
package splitstree4.algorithms.trees;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.SplitsUtilities;
import splitstree4.util.TreesUtilities;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Obtains splits from a selected tree
 */
public class TreeSelector implements Trees2Splits {
    private int which = 1; // which tree is to be converted?
    private Trees trees;

    private JPanel guiPanel; // panel for the gui
    private JLabel guiMessage; // message in the gui panel
    private JTextField guiWhich;  // contains "which" in the gui panel

    public static String DESCRIPTION = "Converts trees to splits";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one or more tree s
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        if (doc.isValid(taxa) && doc.isValid(trees)) {
            this.trees = trees; // keep a reference to the trees
            return true;
        } else
            return false;
    }

    /**
     * Applies the method to the given data
     *
     * @param doc   the document or null
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) {
        if (which < 0)
            which = 1;
        if (which > trees.getNtrees())
            which = trees.getNtrees();
        setOptionWhich(which);

        if (trees.getNtrees() == 0)
            return new Splits(taxa.getNtax());

        try {
            PhyloTree tree = trees.getTree(which);

            taxa.hideAdditionalTaxa(null);
            TaxaSet taxaInTree = trees.getTaxaInTree(taxa, which);
            if (trees.getPartial() || !taxaInTree.equals(taxa.getTaxaSet())) // need to adjust the taxa set!
            {
                taxa.hideAdditionalTaxa(taxaInTree.getComplement(taxa.getNtax()));
            }

            Splits splits = new Splits(taxa.getNtax());
            if (tree.getNumberOfNodes() == 0)
                return splits;


            Node root = tree.getRoot();
            if (root == null) {
                // choose an arbitrary labeled root
                for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                    if (trees.getTaxaForLabel(taxa, tree.getLabel(v)).cardinality() > 0
                            && tree.getDegree(v) == 1) {
                        root = v;
                        break;
                    }
                }
            }
            if (root == null) // empty tree?
                return splits;

            try {
                TreesUtilities.verifyTree(trees.getTree(which), trees.getTranslate(), taxa, true);
                TreesUtilities.setNode2taxa(trees.getTree(which), taxa);

                if (doc != null)
                    doc.notifyTasks("TreeSelector", "Extracting splits");
                tree2splitsRec(root, null, trees, taxa, splits);
                splits.getProperties().setCompatibility(Splits.Properties.COMPATIBLE);
                if (doc != null)
                    doc.notifyTasks("TreeSelector", "Computing cycle");

                if (doc != null && doc.isValidByName(Assumptions.NAME)
                        && doc.getAssumptions().getLayoutStrategy() == Assumptions.RECOMPUTE) {
                    if (taxa.getNtax() > 0) {
                        Node vFirstTaxon;
                        for (vFirstTaxon = trees.getTree(which).getFirstNode(); vFirstTaxon != null; vFirstTaxon = vFirstTaxon.getNext()) {
                            String label = trees.getTree(which).getLabel(vFirstTaxon);
                            if (label != null && label.equals(taxa.getLabel(1)))
                                break;
                        }
                        if (vFirstTaxon != null)
                            splits.setCycle(trees.getTree(which).getCycle(vFirstTaxon));
                    }
                } else {
                    // if in stabilize, use NNet later to compute cycle
                }
                SplitsUtilities.verifySplits(splits, taxa);

            } catch (SplitsException ex) {
                Basic.caught(ex);
                return new Splits(taxa.getNtax());
            }
            return splits;
        } catch (Exception ex) {
            Basic.caught(ex);
            return new Splits(taxa.getNtax());
        }
    }

    // recursively compute the splits:

    private TaxaSet tree2splitsRec(Node v, Edge e, Trees trees,
                                   Taxa taxa, Splits splits) throws NotOwnerException {
        PhyloTree tree = trees.getTree(which);
        TaxaSet e_taxa = trees.getTaxaForLabel(taxa, tree.getLabel(v));

        for (Edge f : v.adjacentEdges()) {
            if (f != e) {
                TaxaSet f_taxa = tree2splitsRec(tree.getOpposite(v, f), f, trees,
                        taxa, splits);

                // take care at root of tree,
                // if root has degree 2, then root will give rise to only
                //  one split, with weight that equals
                // the sum of the two weights.. make sure we only produce
                // one split by using the edge that has lower id
                boolean ok = true;
                double weight = tree.getWeight(f);
                double confidence = tree.getConfidence(f);
                Node root = tree.getRoot();
                if (root != null && (f.getSource() == root || f.getTarget() == root) &&
                        root.getDegree() == 2 && trees.getTaxaForLabel(taxa, tree.getLabel(root)).cardinality() == 0) {
                    // get the other  edge adjacent to root:
                    Edge g;
                    if (root.getFirstAdjacentEdge() != f)
                        g = root.getFirstAdjacentEdge();
                    else
                        g = root.getLastAdjacentEdge();
                    if (f.getId() < g.getId()) {
                        weight = tree.getWeight(f) + tree.getWeight(g);
                        confidence = 0.5 * (tree.getConfidence(f) + tree.getConfidence(g));
                    } else
                        ok = false;
                }

                if (ok) {
                    if (confidence != 1)
                        splits.getFormat().setConfidences(true);

                    splits.getSplitsSet().add(f_taxa, (float) weight, (float) confidence);
                }
                e_taxa.set(f_taxa);
            }
        }
        return e_taxa;
    }

    /**
     * gets which tree in the list is to be converted
     *
     * @return which
     */
    public int getOptionWhich() {
        return which;
    }

    /**
     * sets which tree is to be converted
     *
     * @param which ?
     */
    public void setOptionWhich(int which) {
        this.which = which;
        if (guiWhich != null)
            guiWhich.setText("" + which);
        if (guiMessage != null) {
            String name = trees.getName(which);
            if (name.length() > 40)
                name = name.substring(0, 40) + "...";

            guiMessage.setText("Tree '" + name
                    + "' ( " + which + " of " + trees.getNtrees() + ")");
        }
    }

    /**
     * gets an options panel to be used by the gui
     *
     * @return options panel
     */
    public JPanel getGUIPanel(Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new JPanel();

        Box vbox = Box.createVerticalBox();
        Box hbox1 = Box.createHorizontalBox();
        guiMessage = new JLabel();
        guiMessage.setText("Tree '" + trees.getName(which)
                + "' ( " + which + " of " + trees.getNtrees() + ")");
        hbox1.add(guiMessage);
        vbox.add(hbox1);
        Box hbox2 = Box.createHorizontalBox();
        hbox2.add(Box.createHorizontalGlue());
        hbox2.add(new JLabel("Which:"));
        guiWhich = new JTextField("" + which, 5);
        guiWhich.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    setOptionWhich(Integer.parseInt(guiWhich.getText()));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        });
        hbox2.add(guiWhich);
        vbox.add(hbox2);

        Box hbox3 = Box.createHorizontalBox();

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                setOptionWhich(1);
            }
        };
        action.putValue(AbstractAction.NAME, "<<");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "First tree");
        hbox3.add(new JButton(action));

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (getOptionWhich() > 1)
                    setOptionWhich(getOptionWhich() - 1);
            }
        };
        action.putValue(AbstractAction.NAME, "<");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Previous tree");
        hbox3.add(new JButton(action));

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (getOptionWhich() < trees.getNtrees())
                    setOptionWhich(getOptionWhich() + 1);
            }
        };
        action.putValue(AbstractAction.NAME, ">");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Next tree");
        hbox3.add(new JButton(action));

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (getOptionWhich() < trees.getNtrees())
                    setOptionWhich(trees.getNtrees());
            }
        };
        action.putValue(AbstractAction.NAME, ">>");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Last tree");
        hbox3.add(new JButton(action));

        vbox.add(hbox3);
        guiPanel.add(vbox);

        return guiPanel;
    }
}

// EOF
