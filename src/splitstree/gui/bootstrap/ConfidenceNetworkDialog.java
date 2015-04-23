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

import jloda.util.Basic;
import jloda.util.CanceledException;
import splitstree.algorithms.util.ConfidenceNetwork;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.gui.Director;
import splitstree.nexus.Splits;
import splitstree.util.SplitMatrix;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 3, 2005
 * Time: 3:41:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfidenceNetworkDialog {
    JDialog dialog;

    private static String[] weightTypes = {"Frequency", "Upper", "Midpoint", "Lower", "Estimate"};

    JFormattedTextField levelField;

    public static final int FREQ = 0;
    public static final int UPPER = 1;
    public static final int MID = 2;
    public static final int LOWER = 3;
    public static final int ESTIMATED = 4;


    private JPanel mainPanel;
    private JComboBox comboBox;
    private JButton cancelButton;
    private JButton applyButton;

    boolean execute; //True when the user presses run
    private Frame owner;

    int levelPercent;


    public boolean executeNetwork() {
        return execute;
    }

    public int getLevel() {
        return levelPercent;
    }

    public int getWeightType() {
        return comboBox.getSelectedIndex();
    }


    /**
     * builds the ConfidenceNetworkDialog and displays it
     *
     * @param dialogOwner
     * @param level
     */
    public ConfidenceNetworkDialog(Frame dialogOwner, int level) {

        //ToDo: Get rid of all the redundant code here!
        dialog = new JDialog(dialogOwner, true); /* Create modal ConfidenceNetworkDialog */
        owner = dialogOwner;
        ConfidenceNetworkActions actions = new ConfidenceNetworkActions(this);

        execute = false;
        levelPercent = level;

        dialog.setTitle("Confidence Network");
        dialog.setLocation(owner.getLocation().x + 200, owner.getLocation().y + 200);


        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.ipadx = 3;
        c.ipady = 3;

        JLabel label = new JLabel("Level (%)");
        mainPanel.add(label, c);

        levelField = new JFormattedTextField(new DecimalFormat("###"));
        levelField.setValue(levelPercent);
        levelField.setColumns(5);
        levelField.setMinimumSize(levelField.getPreferredSize());
        c.gridx++;
        mainPanel.add(levelField, c);
        label.setLabelFor(levelField);

        label = new JLabel("Edge weighting");
        c.gridx = 0;
        c.gridy++;
        mainPanel.add(label, c);

        comboBox = new JComboBox(weightTypes);
        comboBox.setSelectedIndex(3);
        c.gridx++;
        label.setLabelFor(comboBox);
        comboBox.setToolTipText("The method used to assign weights to splits in the confidence network");
        mainPanel.add(comboBox, c);

        cancelButton = new JButton(actions.getCancelAction());
        c.gridx = 0;
        c.gridy++;
        mainPanel.add(cancelButton, c);

        c.gridx++;
        mainPanel.add(Box.createHorizontalGlue(), c);
        applyButton = new JButton(actions.getApplyAction());
        c.gridx++;
        c.anchor = GridBagConstraints.EAST;
        mainPanel.add(applyButton, c);
        mainPanel.setMinimumSize(mainPanel.getPreferredSize());

        dialog.setContentPane(mainPanel);
        Dimension d = mainPanel.getPreferredSize();
        d.setSize(d.width + 10.0, d.height + 10.0);
        dialog.setSize(d);
    }

    /**
     * shows the ConfidenceNetworkDialog and constructs the conficence network, if desired
     *
     * @param dir
     */
    public void showDialog(Director dir) {
        dialog.setVisible(true);

        if (executeNetwork()) {
            try {
                StringWriter sw = new StringWriter();
                Document doc = dir.getDocument();
                doc.getTaxa().write(sw);

                SplitMatrix M = doc.getBootstrap().getSplitMatrix();
                int weightType = getWeightType();
                int level = getLevel();
                System.err.println("Computing " + level + "% Confidence Network");

                doc.notifySubtask("Computing confidence network");
                Splits splits = ConfidenceNetwork.getConfidenceNetwork(M, (double) level / 100, doc);
                try {
                    splits.setCycle(doc.getSplits().getCycle());
                } catch (SplitsException e) {
                    Basic.caught(e);
                }
                splits.write(sw, doc.getTaxa());
                Director newDir = Director.newProject(sw.toString(), dir.getDocument().getFile().getAbsolutePath());
                newDir.getDocument().setTitle("" + level + "% Confidence Network for " + dir.getDocument().getTitle());
                newDir.showMainViewer();
            } catch (CanceledException ex) {
                System.err.println("Confidence network calculation cancelled");
            } catch (IOException ex) {
                Basic.caught(ex);
            }
        }
    }

    public void closeDialog() {
        dialog.setVisible(false);
    }
}
