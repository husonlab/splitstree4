/**
 * SelectTreesPanel.java
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
package splitstree4.gui.algorithms.select;

import jloda.swing.director.IUpdateableView;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.nexus.Trees;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

/**
 * DESCRIPTION
 *
 * @author huson
 * Date: 19-Dec-2003
 */
public class SelectTreesPanel extends JPanel implements IUpdateableView {
    private Director dir;
    private SelectTreesActions actions;
    JList list = new JList();

    /**
     * sets up the algorithms window
     *
     * @param dir
     */
    public SelectTreesPanel(Director dir) {
        this.dir = dir;
        actions = new SelectTreesActions(dir);
        setup();
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */
    public SelectTreesActions getActions() {
        return actions;
    }

    /**
     * ask view to update itself.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            return;
        }
        getActions().setEnableCritical(true);
        Document doc = dir.getDocument();
        list.removeAll();
        Trees trees = doc.getTrees();
        java.util.List names = new LinkedList();
        if (doc.isValid(trees)) {
            for (int t = 1; t <= trees.getNtrees(); t++)
                names.add(trees.getName(t));
        }
        list.setListData(names.toArray());
        getActions().updateEnableState();
    }

    /**
     * sets up the  panel
     */
    private void setup() {
        JPanel panel = this;

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 5, 1, 5);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        panel.add(new JLabel("Choose trees to select in network:"), constraints);

        list = new JList();
        AbstractAction action = getActions().getInput();
        action.putValue(SelectTreesActions.JLIST, list);
        list.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

        JScrollPane scrollP = new JScrollPane(list);

        // ((JTextArea) comp).addPropertyChangeListener(action);
        // textArea.setToolTipText((String) action.get(AbstractAction.SHORT_DESCRIPTION));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 5;
        constraints.gridheight = 3;
        panel.add(scrollP, constraints);

        JButton apply = new JButton(getActions().getApply());
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        panel.add(apply, constraints);

        JButton reset = new JButton(getActions().getClear());
        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        panel.add(reset, constraints);

        /*
        descriptionLabel.setText(" ");
        Box box = Box.createHorizontalBox();
        box.add(descriptionLabel);
        box.add(Box.createHorizontalStrut(500));
        box.setBorder(BorderFactory.createEtchedBorder());
        box.setMinimumSize(new Dimension(100, 20));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 1;
        constraints.weightx = 1;
        constraints.gridwidth = 6;
        constraints.gridheight = 1;
        constraints.gridx = 0;
        constraints.gridy = 7;
        panel.add(box, constraints);
        */
    }
}
