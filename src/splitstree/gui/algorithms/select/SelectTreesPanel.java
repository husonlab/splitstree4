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

package splitstree.gui.algorithms.select;

import jloda.gui.director.IUpdateableView;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.nexus.Trees;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

/**
 * DESCRIPTION
 *
 * @author huson
 *         Date: 19-Dec-2003
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
        // textArea.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

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
