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

package splitstree.gui.algorithms.modify;

import jloda.gui.director.IUpdateableView;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.algorithms.AlgorithmsWindow;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Assumptions;
import splitstree.nexus.Splits;

import javax.swing.*;
import java.awt.*;

/**
 * set post processing of splits
 *
 * @author huson
 *         <p/>
 *         17.2.2004
 */
public class ModifySplitsPanel extends JPanel implements IUpdateableView {
    private Director dir;
    private ModifySplitsActions actions;
    private JCheckBox leastSquaresCB;
    private JRadioButton greedyCompatibleRB;
    private JRadioButton closestTreeRB;
    private JRadioButton greedyWeaklyCompatibleRB;
    private JRadioButton applyWeightThresholdRB;
    private JRadioButton applyConfidenceThresholdRB;
    private JRadioButton applyDimensionFilterRB;
    private JTextField weightThresholdInput;
    private JTextField confidenceThresholdInput;
    private JTextField dimensionFilterInput;
    private JRadioButton noneRB;
    private JLabel descriptionLabel;

    /**
     * sets up the algorithms window
     *
     * @param dir
     */
    public ModifySplitsPanel(Director dir) {
        this.dir = dir;
        JFrame parent;
        if (dir.getViewerByClass(AlgorithmsWindow.class) != null)
            parent = dir.getViewerByClass(AlgorithmsWindow.class).getFrame();
        else
            parent = dir.getMainViewerFrame();
        actions = new ModifySplitsActions(dir, parent);
        setup();
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */
    public ModifySplitsActions getActions() {
        return actions;
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            return;
        }
        getActions().setEnableCritical(true);

        Document doc = dir.getDocument();
        if (doc.isValidByName(Assumptions.NAME)) {
            Assumptions.SplitsPostProcess splitsPostProcess =
                    doc.getAssumptions().getSplitsPostProcess();
            boolean value = splitsPostProcess.isLeastSquares();
            leastSquaresCB.setSelected(value);
            String filter = splitsPostProcess.getFilter();
            greedyCompatibleRB.setSelected(filter.equalsIgnoreCase("greedycompatible"));
            closestTreeRB.setSelected(filter.equalsIgnoreCase("closesttree"));
            greedyWeaklyCompatibleRB.setSelected(filter.equalsIgnoreCase("greedyWC"));
            applyWeightThresholdRB.setSelected(filter.equalsIgnoreCase("weight"));
            applyConfidenceThresholdRB.setSelected(filter.equalsIgnoreCase("confidence"));
            applyDimensionFilterRB.setSelected(filter.equalsIgnoreCase("dimension"));

            noneRB.setSelected(filter.equalsIgnoreCase("none"));
            weightThresholdInput.setText("" + splitsPostProcess.getWeightThresholdValue());
            confidenceThresholdInput.setText("" + splitsPostProcess.getConfidenceThresholdValue());
            dimensionFilterInput.setText("" + splitsPostProcess.getDimensionValue());

            this.updateDescription(dir.getDocument().getSplits());

        }
        getActions().updateEnableState();
    }

    /**
     * sets up the panel
     */
    private void setup() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 5, 1, 5);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.WEST;

        int row = 0;

        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        add(new JLabel("Modify weights:"), constraints);
        constraints.gridy = row + 1;
        add(new JLabel("Select splits using filter:"), constraints);


        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        leastSquaresCB = new JCheckBox(getActions().getLeastSquares());
        add(leastSquaresCB, constraints);

        ButtonGroup group = new ButtonGroup();

        constraints.gridx = 1;
        constraints.gridy = row++;
        greedyCompatibleRB = new JRadioButton(getActions().getGreedyCompatible());
        group.add(greedyCompatibleRB);
        add(greedyCompatibleRB, constraints);

        constraints.gridx = 1;
        constraints.gridy = row++;
        closestTreeRB = new JRadioButton(getActions().getClosestTree());
        group.add(closestTreeRB);
        add(closestTreeRB, constraints);

        constraints.gridx = 1;
        constraints.gridy = row++;
        greedyWeaklyCompatibleRB = new JRadioButton(getActions().getGreedyWeaklyCompatible());
        group.add(greedyWeaklyCompatibleRB);
        add(greedyWeaklyCompatibleRB, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = row;
        applyWeightThresholdRB = new JRadioButton();
        applyWeightThresholdRB.setAction(getActions().getApplyWeightThreshold(applyWeightThresholdRB));
        group.add(applyWeightThresholdRB);
        add(applyWeightThresholdRB, constraints);

        // constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        weightThresholdInput = new JTextField("0", 5);
        weightThresholdInput.setMinimumSize(new Dimension(50, 18));

        add(weightThresholdInput, constraints);

        constraints.gridx = 4;
        add(new JButton(getActions().getLaunchWeightHistogram(weightThresholdInput)), constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = row;
        applyConfidenceThresholdRB = new JRadioButton();
        applyConfidenceThresholdRB.setAction(getActions().getApplyConfidenceThreshold(applyConfidenceThresholdRB));
        group.add(applyConfidenceThresholdRB);
        add(applyConfidenceThresholdRB, constraints);
        // constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        confidenceThresholdInput = new JTextField("0", 5);
        confidenceThresholdInput.setMinimumSize(new Dimension(50, 18));
        add(confidenceThresholdInput, constraints);

        constraints.gridx = 4;
        add(new JButton(getActions().getLaunchConfidenceHistogram(confidenceThresholdInput)), constraints);


        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = row;
        applyDimensionFilterRB = new JRadioButton();
        applyDimensionFilterRB.setAction(getActions().getApplyDimensionFilter(applyDimensionFilterRB));
        group.add(applyDimensionFilterRB);
        add(applyDimensionFilterRB, constraints);

        // constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        dimensionFilterInput = new JTextField("0", 5);
        dimensionFilterInput.setMinimumSize(new Dimension(50, 18));
        add(dimensionFilterInput, constraints);

        constraints.gridx = 1;
        constraints.gridy = row++;
        constraints.gridwidth = 1;
        noneRB = new JRadioButton(getActions().getNone());
        group.add(noneRB);
        add(noneRB, constraints);

        constraints.gridx = 1;
        constraints.gridy = row++;
        MainViewer viewer = (MainViewer) dir.getMainViewer();
        JButton hideSelected = new JButton(viewer.getActions().getHideSelected());
        group.add(hideSelected);
        add(hideSelected, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 0;
        add(new JButton(getActions().getApply(weightThresholdInput, confidenceThresholdInput, dimensionFilterInput)), constraints);


        descriptionLabel = new JLabel();
        Box box = Box.createHorizontalBox();
        box.add(descriptionLabel);
        box.add(Box.createHorizontalStrut(500));
        box.setBorder(BorderFactory.createEtchedBorder());

        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 9;
        constraints.gridheight = 1;
        add(box, constraints);
    }

    public void updateDescription(Splits splits) {
        if (splits == null) {
            descriptionLabel.setText("");
            descriptionLabel.setEnabled(false);
            return;
        }

        descriptionLabel.setEnabled(true);
        float totalWeight = 0;
        for (int s = 1; s <= splits.getNsplits(); s++)
            totalWeight += splits.getWeight(s);

        StringBuilder buf = new StringBuilder();
        if (splits.getOriginal() == null || splits.getOriginal().getNsplits() == splits.getNsplits()) {
            buf.append("nSplits=").append(splits.getNsplits()).append(", total weight=").append(totalWeight);
        } else // some stuff is hidden
        {
            Splits original = splits.getOriginal();
            float totalOriginal = 0;
            for (int s = 1; s <= original.getNsplits(); s++) {
                totalOriginal += original.getWeight(s);
            }
            double percentage = 100 * totalWeight / totalOriginal;
            percentage = ((int) (1000 * percentage) / 100) / 10.0;
            buf.append("nSplits=").append(splits.getNsplits()).append(" (of ").append(original.getNsplits()).append("), total weight=").append(totalWeight).append(" (").append(percentage).append("% of ").append(totalOriginal).append(")");
        }
        descriptionLabel.setText(buf.toString());
    }
}


