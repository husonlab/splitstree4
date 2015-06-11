/**
 * FilterReticulatesPanel.java 
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
package splitstree.gui.algorithms.filter;

import jloda.gui.ActionJList;
import jloda.gui.director.IUpdateableView;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.nexus.Reticulate;
import splitstree.nexus.Taxa;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 01.06.2006
 * Time: 15:10:54
 * To change this template use File | Settings | File Templates.
 */
public class FilterReticulatesPanel extends JPanel implements IUpdateableView, UpdateableActions {
    java.util.List all = new LinkedList();

    // the lists with the information init is done with initInformation() and updated via the actions of the
    // rootComponents, nettedComponents and nettedComponentsBackbones
    private DefaultComboBoxModel box = null;
    private DefaultListModel listNettedComponents;
    private DefaultListModel listNettedComponentsContent;

    //  the combo box for the root component
    private JComboBox rootComponentComboBox = null;
    // the selected backbones for each netted component
    private int[] selectedActiveNettedComponentsBackbones;
    private int selectedActiveNettedComponent = 0;

    private ActionJList jListNettedComponents;
    private ActionJList jListNettedComponentsContent;

    // the labels
    private JLabel possibleNettedCompBackbonesLabel = null;
    private JLabel nettedCompLabel = null;
    private JLabel descriptionLabel = null;

    // the components;
    private JComponent nettedComponentsScrollPane = null;
    // the scroll pane with the different configurations of the netted component selected in the nettedComponentsScrollPane
    private JComponent nettedComponentsBackbonesScrollPane = null;

    // data stuff
    private Reticulate ret = null;
    private Director dir;

    //constructor
    public FilterReticulatesPanel(Director dir) {
        this.dir = dir;
        ret = dir.getDocument().getReticulate();
        if (ret != null) {
            this.box = new DefaultComboBoxModel();
            listNettedComponents = new DefaultListModel();
            listNettedComponentsContent = new DefaultListModel();
            descriptionLabel = new JLabel();
            initInformation();
            GridBagLayout gridBag = new GridBagLayout();
            setLayout(gridBag);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);
            // the root backbone chooser
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 0.1;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(new JLabel("Choose active root component:"), gbc);

            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 0.1;
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            this.rootComponentComboBox = new JComboBox(box);
            this.rootComponentComboBox.setAction(getComboBoxAction(dir));
            add(rootComponentComboBox, gbc);
            this.rootComponentComboBox.setToolTipText("Select active root component of the reticulate network");


            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.FIRST_LINE_END;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 0.1;
            gbc.gridx = 4;
            gbc.gridy = 0;
            JButton apply = new JButton(getApplyAction());
            gridBag.setConstraints(apply, gbc);
            add(apply, gbc);

            // the two parallel chooser for the netted components
            // the label of the left chooser
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.gridwidth = 2;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = .1;
            gbc.gridx = 0;
            gbc.gridy = 1;
            nettedCompLabel = new JLabel("Netted components: ");
            add(nettedCompLabel, gbc);

            // the label of the right chooser
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.gridwidth = 2;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = .1;
            gbc.gridx = 2;
            gbc.gridy = 1;
            possibleNettedCompBackbonesLabel = new JLabel("Possible backbones: ");
            add(possibleNettedCompBackbonesLabel, gbc);

            // the left chooser
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = 2;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.PAGE_START;
            jListNettedComponents = new ActionJList(listNettedComponents);
            jListNettedComponents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jListNettedComponents.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if ((!e.getValueIsAdjusting()) || e.getFirstIndex() == -1)
                        return;
                    int s = ((ActionJList) e.getSource()).getSelectedIndex();
                    //update the celements of the jListNettedComponentsContent
                    listNettedComponentsContent.clear();
                    int index = ret.getContainedNettedComponentsOfActiveRootComponent()[s + 1];
                    selectedActiveNettedComponent = s;
                    //System.out.println("selectionIndex: " + s + "\tnettedCompIndex: " + index);
                    for (int j = 1; j <= ret.getNumberOfNettedComponentBackbones(index); j++) {
                        listNettedComponentsContent.add(j - 1, ret.getNettedComponentBackboneLabel(index, j));
                    }
                    jListNettedComponentsContent.setSelectedIndex(selectedActiveNettedComponentsBackbones[s]);
                }
            });
            // disable jList if no NettedComponents are present
            if(ret.getNNettedComponents()==0)jListNettedComponents.setEnabled(false);
            nettedComponentsScrollPane = new JScrollPane(jListNettedComponents);
            nettedComponentsScrollPane.setToolTipText("Netted Components of the selected root component");
            add(nettedComponentsScrollPane, gbc);
            jListNettedComponents.setSelectedIndex(0);

            // the right chooser
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.gridwidth = 2;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.gridx = 2;
            gbc.gridy = 2;

            jListNettedComponentsContent = new ActionJList(listNettedComponentsContent);
            jListNettedComponentsContent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // disable jList if no NettedComponents are present
            if(ret.getNNettedComponents()==0)jListNettedComponentsContent.setEnabled(false);
            nettedComponentsBackbonesScrollPane = new JScrollPane(jListNettedComponentsContent);
            nettedComponentsBackbonesScrollPane.setToolTipText("Possible backbones of the selected netted Component");
            add(nettedComponentsBackbonesScrollPane, gbc);
            jListNettedComponentsContent.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if ((!e.getValueIsAdjusting()) || e.getFirstIndex() == -1)
                        return;
                    int s = ((ActionJList) e.getSource()).getSelectedIndex();
                    selectedActiveNettedComponentsBackbones[selectedActiveNettedComponent] = s;
                    updateDescriptionLabel();

                }
            });


            updateDescriptionLabel();
            Box box = Box.createHorizontalBox();
            box.add(descriptionLabel);
            box.add(Box.createHorizontalStrut(500));
            box.setBorder(BorderFactory.createEtchedBorder());
            box.setMinimumSize(new Dimension(100, 20));

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.LAST_LINE_START;
            gbc.gridwidth = 5;
            gbc.gridheight = 1;
            gbc.weighty = 0.1;
            gbc.weightx = 0.1;
            gbc.gridx = 0;
            gbc.gridy = 3;
            gridBag.setConstraints(box, gbc);
            add(box, gbc);
            setSize(500, 210);
            if (ret.getContainedNettedComponentsOfActiveRootComponent().length > 0)
                enableNettedComponentSelection();
            else disableNettedComponentSelection();
        }
    }

    private void updateDescriptionLabel() {
        // set new Description Label
        if(selectedActiveNettedComponentsBackbones.length>0){
        StringBuilder newInitLabel = new StringBuilder("backbone is: '" + ret.getRootComponentLabel(ret.getActiveRootComponent()) + "' netted component config is: '");
            for (int selectedActiveNettedComponentsBackbone : selectedActiveNettedComponentsBackbones) {
                newInitLabel.append(" " + (selectedActiveNettedComponentsBackbone + 1));
            }
        newInitLabel.append("'");
        descriptionLabel.setText(newInitLabel.toString());
        }else {
           descriptionLabel.setText( "backbone is: '" + ret.getRootComponentLabel(ret.getActiveRootComponent()) + "'.");
        }
    }

    /**
     * Inits the information shown in the Pane
     */
    private void initInformation() {
        // init the config for the active netted component backbone (since it is a graphics object it starts at 0 NOT 1)
        selectedActiveNettedComponentsBackbones = new int[ret.getContainedNettedComponentsOfActiveRootComponent().length - 1];
        for (int i = 1; i < ret.getContainedNettedComponentsOfActiveRootComponent().length; i++) {
            int index = ret.getContainedNettedComponentsOfActiveRootComponent()[i];
            selectedActiveNettedComponentsBackbones[i - 1] = ret.getActiveNettedComponentBackbone(index) - 1;
        }
        // add all root components to the box list
        this.box = new DefaultComboBoxModel();
        for (int i = 1; i < ret.getNRootComponents(); i++)
            box.addElement(ret.getRootComponentLabel(i));
        box.setSelectedItem(ret.getRootComponentLabel(ret.getActiveRootComponent()));
        // if there are netted Components
        if(ret.getNNettedComponents()>0){
        // add the netted components of the active root component
        int[] activeNettedComponentsIds = ret.getContainedNettedComponentsOfActiveRootComponent();
        listNettedComponents = new DefaultListModel();
        for (int i = 1; i < activeNettedComponentsIds.length; i++) {
            //System.out.println("includedNettedComp: " + ret.getNettedComponentLabel(activeNettedComponentsIds[i]));
            listNettedComponents.add(i - 1, ret.getNettedComponentLabel(activeNettedComponentsIds[i]));
        }
        // add the backbones of the netted component selected in 'selectedActiveNettedComponent'
        listNettedComponentsContent = new DefaultListModel();
        for (int i = 1; i <= ret.getNumberOfNettedComponentBackbones(activeNettedComponentsIds[1]); i++) {
            //System.out.println("label of nettedBackbone: " + ret.getNettedComponentBackboneLabel(activeNettedComponentsIds[1], i));
            listNettedComponentsContent.add(i - 1, ret.getNettedComponentBackboneLabel(activeNettedComponentsIds[1], i));
        }
        } 
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
        Reticulate ret = doc.getReticulate();
        if (ret == null) {
            return;
        }
        updateDescriptionLabel();
        this.rootComponentComboBox.removeAllItems();
        for (int i = 1; i <= ret.getNRootComponents(); i++) {
            rootComponentComboBox.addItem("[" + i + "] '" + ret.getRootComponentLabel(i) + "'");
        }
        rootComponentComboBox.setSelectedIndex(doc.getReticulate().getActiveRootComponent() - 1);
        if (ret.getContainedNettedComponentsOfActiveRootComponent().length > 0)
            enableNettedComponentSelection();
        else disableNettedComponentSelection();

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
    private AbstractAction applyAction;

    private AbstractAction getApplyAction() {
        if (applyAction != null)
            return applyAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // sync the view to the reticulation object
                int[] nettedCompIndexes = ret.getContainedNettedComponentsOfActiveRootComponent();
                for (int i = 1; i < nettedCompIndexes.length; i++) {
                    System.out.println("index: "+ret.indexOfRootComponentLabel((String)box.getSelectedItem())+"\tlabel: "+ box.getSelectedItem());
                    ret.setActiveRootComponent(ret.indexOfRootComponentLabel((String)box.getSelectedItem()));
                    ret.setActiveNettedComponentBackbone(nettedCompIndexes[i], selectedActiveNettedComponentsBackbones[i - 1] + 1);
                }
                // update the director
                dir.execute("update;");
            }

        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Recompute data on active reticulate backbone");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyAction = action;
    }


    /**
     * return the root component selection action
     *
     * @param dir
     * @return
     */
    AbstractAction getComboBoxAction(final Director dir) {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cb = (JComboBox) event.getSource();
                int active = cb.getSelectedIndex() + 1;
                Reticulate ret = dir.getDocument().getReticulate();
                if (active == 0 || ret == null) {
                    disableNettedComponentSelection();
                    return;
                }
                ret.setActiveRootComponent(active);
                updateDescriptionLabel();
                if (ret.getContainedNettedComponentsOfActiveRootComponent().length > 0)
                    enableNettedComponentSelection();
                else disableNettedComponentSelection();
            }
        };
    }


    private void disableNettedComponentSelection() {
        possibleNettedCompBackbonesLabel.setEnabled(false);
        nettedCompLabel.setEnabled(false);
        nettedComponentsScrollPane.setEnabled(false);
        nettedComponentsBackbonesScrollPane.setEnabled(false);
    }

    private void enableNettedComponentSelection() {
        possibleNettedCompBackbonesLabel.setEnabled(true);
        nettedCompLabel.setEnabled(true);
        nettedComponentsScrollPane.setEnabled(true);
        nettedComponentsBackbonesScrollPane.setEnabled(true);
    }

    public JLabel getDescriptionLabel() {
        return descriptionLabel;
    }

    public void setDescriptionLabel(JLabel descriptionLabel) {
        this.descriptionLabel = descriptionLabel;
    }
}
