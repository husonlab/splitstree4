/*
 * TreeSelector2.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.trees;

import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Network;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @deprecated
 * Obtains a network from a tree
 */
public class TreeSelector2 /* implements Trees2Network */ {
    private int which = 1; // which tree is to be converted?
    private Trees trees;

    private JPanel guiPanel; // panel for the gui
    private JLabel guiMessage; // message in the gui panel
    private JTextField guiWhich;  // contains "which" in the gui panel

    public static final String DESCRIPTION = "Selects and draws a single tree";

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
    public Network apply(Document doc, Taxa taxa, Trees trees) throws Exception {
        if (which < 0)
            which = 1;
        if (which > trees.getNtrees())
            which = trees.getNtrees();
        setOptionWhich(which);

        taxa.hideAdditionalTaxa(null);
        TaxaSet taxaInTree = trees.getTaxaInTree(taxa, which);
        if (trees.getPartial() || !taxaInTree.equals(taxa.getTaxaSet())) // need to adjust the taxa set!
        {
            taxa.hideAdditionalTaxa(taxaInTree.getComplement(taxa.getNtax()));
        }

        try {
            TreesUtilities.verifyTree(trees.getTree(which), trees.getTranslate(), taxa, true);

        } catch (SplitsException ex) {
            Basic.caught(ex);
        }


        EqualAngleTree eat = new EqualAngleTree();
        eat.setOptionWhich(getOptionWhich());
        return eat.apply(doc, taxa, trees);
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
