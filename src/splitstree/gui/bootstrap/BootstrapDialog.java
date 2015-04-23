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

package splitstree.gui.bootstrap;

import jloda.util.Alert;
import splitstree.nexus.Bootstrap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 3, 2005
 * Time: 3:41:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapDialog extends JDialog implements ActionListener {


    private JFormattedTextField numReplicates;
    private JPanel mainPanel;
    private JPanel outerPanel;
    private JButton cancelButton;
    private JButton runButton;
    private JCheckBox saveTreesButton;

    private Bootstrap bootstrap;
    private boolean execute; //True when the user presses run
    private Frame owner;

    int numR;
    boolean fixNet;
    boolean saveW;
    String outfile;


    public boolean executeBootstrap() {
        return execute;
    }

    public BootstrapDialog(Bootstrap bootstrap, Frame dialogOwner) {

        //ToDo: Get rid of all the redundant code here!
        super(dialogOwner, true); /* Create modal ConfidenceNetworkDialog */
        owner = dialogOwner;

        execute = false;

        setTitle("Bootstrap");
        setLocation(owner.getLocation().x + 200, owner.getLocation().y + 200);
        setSize(300, 150);
        /* Get the initial values */
        this.bootstrap = bootstrap;
        numR = bootstrap.getRuns();
        if (numR == 0) numR = 1000;
        boolean saveTrees = bootstrap.getSaveTrees();
        boolean canSaveTrees = bootstrap.getCanSaveTrees();

        /* Set up main Panel */
        outerPanel = new JPanel();
        outerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        outerPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        setupMainPanel();
        outerPanel.add(mainPanel, c);

        saveTreesButton = new JCheckBox("Save trees");
        saveTreesButton.setToolTipText("Save all bootstrap trees to a new document");
        saveTreesButton.setEnabled(canSaveTrees);
        saveTreesButton.setSelected(saveTrees);
        c.gridx = 0;
        c.gridy = 1;
        outerPanel.add(saveTreesButton, c);

        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        outerPanel.add(cancelButton, c);


        c.gridx = 1;
        outerPanel.add(Box.createHorizontalGlue(), c);
        runButton = new JButton("Run");
        runButton.setActionCommand("Run");
        runButton.addActionListener(this);
        c.gridx = 2;
        c.anchor = GridBagConstraints.EAST;
        outerPanel.add(runButton, c);


        this.setContentPane(outerPanel);

        this.setVisible(true);

    }


    private void setupMainPanel() {

        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = c.ipady = 2;
        c.anchor = GridBagConstraints.WEST;
        JLabel label2 = new JLabel("Number of Replicates");
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        mainPanel.add(label2, c);

        numReplicates = new JFormattedTextField(new DecimalFormat("#####"));
        numReplicates.setValue(numR);
        numReplicates.setColumns(5);
        numReplicates.setMinimumSize(numReplicates.getPreferredSize());


        c.gridx = 2;
        c.gridwidth = 1;
        mainPanel.add(numReplicates, c);
        label2.setLabelFor(numReplicates);

    }


    public void actionPerformed(ActionEvent e) {
        if ("Cancel".equals(e.getActionCommand())) {

            this.setVisible(false);
        } else if ("Run".equals(e.getActionCommand())) {
            boolean nrunsOK = true;

            try {
                numReplicates.commitEdit();
            } catch (ParseException ex) {
                System.err.println("Problems with commitEdit()");
                nrunsOK = false;
            }
            if (nrunsOK) {
                try {
                    numR = ((Number) numReplicates.getValue()).intValue();
                } catch (Exception ex) {
                    nrunsOK = false;
                }
            }
            if (!nrunsOK || numR <= 0) {
                new Alert("The number of bootstrap replicates needs to be a positive integer");
            } else {
                bootstrap.setRuns(numR);
                bootstrap.setSaveTrees(saveTreesButton.isSelected());
                execute = true;
                this.setVisible(false);
                System.err.println("Run");
            }
        }

    }


}
