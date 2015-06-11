/**
 * FilterTreesPanel.java 
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
/** The tree selection window
 *
 * @author Daniel Huson and David Bryant
 * 26.6.04
 */
package splitstree.gui.algorithms.filter;

import jloda.gui.director.IUpdateableView;
import jloda.util.Basic;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

/**
 * The select trees panel
 */
public class FilterTreesPanel extends JPanel implements IUpdateableView, UpdateableActions {
    java.util.List all = new LinkedList();
    private DefaultListModel listl = null;
    private DefaultListModel listr = null;
    private JList jlistl = null;
    private JList jlistr = null;
    JLabel descriptionLabel = null;

    private Director dir;

    //constructor
    public FilterTreesPanel(Director dir) {
        this.dir = dir;

        this.listl = new DefaultListModel();
        this.listr = new DefaultListModel();

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

        this.jlistl = new JList(listl);
        JComponent input = new JScrollPane(jlistl);
        input.setToolTipText("Trees included in computations");
        gridBag.setConstraints(input, gbc);
        add(input);

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

        jlistr = new JList(listr);
        input = new JScrollPane(jlistr);
        input.setToolTipText("Trees excluded from computations");
        gridBag.setConstraints(input, gbc);
        add(input);

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
        gbc.gridy = 6;
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
        if (doc.getTrees() == null) {
            descriptionLabel.setText(" ");
            return;
        }
        String aline = "nTrees= " + doc.getTrees().getNtrees();

        int origNtrees;
        Trees origTrees = doc.getTrees().getOriginal();
        if (origTrees != null) {
            origNtrees = origTrees.getNtrees();
            if (origNtrees > doc.getTrees().getNtrees()) ;
            aline += " (" + (origNtrees - doc.getTrees().getNtrees()) + " of " + origNtrees + " hidden)";
        } else
            origNtrees = doc.getTrees().getNtrees();
        descriptionLabel.setText(aline);

        this.listl.clear();
        this.listr.clear();

        if (origTrees == null)
            origTrees = doc.getTrees();

        for (int i = 1; i <= origNtrees; i++) {
            int index = doc.getTrees().indexOf(origTrees.getName(i));
            if (index > 0)
                listl.addElement("[" + index + "] '" + origTrees.getName(i) + "'");
            else
                listr.addElement("'" + origTrees.getName(i) + "'");
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

    }

    /**
     * update the enable state
     */
    public void setEnableCritical(boolean flag) {
        for (Object anAll : all) {
            AbstractAction action = (AbstractAction) anAll;
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
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide selected trees");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return hideAction = action;
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
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show selected trees");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return showAction = action;
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
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show all trees");
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
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide all trees");
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
                String extrees = "";

                for (int i = 0; i < listr.getSize(); i++) {
                    if (((String) listr.getElementAt(i)).indexOf(':') != -1)
                        extrees += (" " + ((String) listr.getElementAt(i)).substring(((String) listr.getElementAt(i)).indexOf(':') + 2));
                    else
                        extrees += (" " + listr.getElementAt(i));
                }

                try {
                    System.err.println("assume extrees=" + extrees);

                    dir.execute("assume extrees=" + extrees);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Recompute data on shown trees");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyAction = action;
    }
}


