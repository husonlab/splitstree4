/**
 * ModifySplitsActions.java 
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
package splitstree.gui.algorithms.modify;

import jloda.gui.HistogramPanel;
import jloda.util.Basic;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.nexus.Distances;
import splitstree.nexus.Splits;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a modify window
 *
 * @author huson
 *         Date: 17.2.2004
 */
public class ModifySplitsActions implements UpdateableActions {
    final private Director dir;
    final private JFrame parent;
    final private List all = new LinkedList();
    public static final String BUTTON = "JCHECKBOX";
    public static final String JTEXTAREA = "JTEXTAREA";

    /**
     * constructor
     *
     * @param dir
     */
    public ModifySplitsActions(Director dir, JFrame parent) {
        this.dir = dir;
        this.parent = parent;
    }

    /**
     * enable or disable critical actions
     *
     * @param flag show or hide?
     */
    public void setEnableCritical(boolean flag) {
        DirectorActions.setEnableCritical(all, flag);
        // because we don't want to duplicate that code
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);

        // because we don't want to duplicate that code
    }

    /**
     * returns all actions
     *
     * @return actions
     */
    public List getAll() {
        return all;
    }

    // here we define the algorithms window specific actions:

    AbstractAction leastSquares;

    AbstractAction getLeastSquares() {
        if (leastSquares != null)
            return leastSquares;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JCheckBox) event.getSource()).isSelected();
                dir.getDocument().getAssumptions().getSplitsPostProcess().
                        setLeastSquares(value);
            }
        };
        action.putValue(AbstractAction.NAME, "Least Squares (unconstrained)");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Reestimate split weights using Least Squares");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Distances.NAME);
        all.add(action);
        return leastSquares = action;
    }

    AbstractAction greedyCompatible;

    AbstractAction getGreedyCompatible() {
        if (greedyCompatible != null)
            return greedyCompatible;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JRadioButton) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("greedycompatible");
            }
        };
        action.putValue(AbstractAction.NAME, "Greedy Compatible");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Greedily make splits compatible");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return greedyCompatible = action;
    }

    AbstractAction closestTree;

    AbstractAction getClosestTree() {
        if (closestTree != null)
            return closestTree;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JRadioButton) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("closesttree");
            }
        };
        action.putValue(AbstractAction.NAME, "Closest Tree");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Finds the closest compatible subset");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return closestTree = action;
    }


    AbstractAction greedyWeaklyCompatible;

    AbstractAction getGreedyWeaklyCompatible() {
        if (greedyWeaklyCompatible != null)
            return greedyWeaklyCompatible;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JRadioButton) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("greedyWC");
            }
        };
        action.putValue(AbstractAction.NAME, "Greedy Weakly Compatible");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Greedily make splits weakly compatible");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return greedyWeaklyCompatible = action;
    }

    AbstractAction applyWeightThreshold;

    AbstractAction getApplyWeightThreshold(JRadioButton but) {
        if (applyWeightThreshold != null)
            return applyWeightThreshold;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JRadioButton but = (JRadioButton) getValue(DirectorActions.JBUTTON);
                but.setSelected(true);
                dir.getDocument().getAssumptions().getSplitsPostProcess().
                        setFilter("weight");
            }
        };
        action.putValue(AbstractAction.NAME, "Weight threshold");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Keep only splits of weight over threshold");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.JBUTTON, but);
        all.add(action);
        return applyWeightThreshold = action;
    }

    AbstractAction applyConfidenceThreshold;

    AbstractAction getApplyConfidenceThreshold(JRadioButton but) {
        if (applyConfidenceThreshold != null)
            return applyConfidenceThreshold;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JRadioButton but = (JRadioButton) getValue(DirectorActions.JBUTTON);
                but.setSelected(true);
                dir.getDocument().getAssumptions().getSplitsPostProcess().
                        setFilter("confidence");
            }
        };
        action.putValue(AbstractAction.NAME, "Confidence threshold");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Keep only splits of confidence over threshold");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.JBUTTON, but);
        all.add(action);
        return applyConfidenceThreshold = action;
    }

    AbstractAction applyDimensionFilter;

    AbstractAction getApplyDimensionFilter(JRadioButton but) {
        if (applyDimensionFilter != null)
            return applyDimensionFilter;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JRadioButton but = (JRadioButton) getValue(DirectorActions.JBUTTON);
                but.setSelected(true);

                dir.getDocument().getAssumptions().getSplitsPostProcess().
                        setFilter("dimension");
            }
        };
        action.putValue(AbstractAction.NAME, "Set Maximum Dimension");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Limit maximal dimension of graph");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        action.putValue(DirectorActions.JBUTTON, but);
        all.add(action);
        return applyDimensionFilter = action;
    }


    AbstractAction none;

    AbstractAction getNone() {
        if (none != null)
            return none;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                boolean value = ((JRadioButton) event.getSource()).isSelected();
                if (value)
                    dir.getDocument().getAssumptions().getSplitsPostProcess().
                            setFilter("none");
            }
        };
        action.putValue(AbstractAction.NAME, "None");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Don't filter splits");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return none = action;
    }

    AbstractAction apply;

    public AbstractAction getApply(final JTextField weightInput,
                                   final JTextField confidenceInput,
                                   final JTextField dimensionInput) {
        if (apply != null)
            return apply;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    float threshold = Float.parseFloat(weightInput.getText());
                    dir.getDocument().getAssumptions().getSplitsPostProcess()
                            .setWeightThresholdValue(threshold);
                } catch (NumberFormatException ex) {
                    Basic.caught(ex);
                }
                try {
                    float threshold = Float.parseFloat(confidenceInput.getText());
                    dir.getDocument().getAssumptions().getSplitsPostProcess()
                            .setConfidenceThresholdValue(threshold);
                } catch (NumberFormatException ex) {
                    Basic.caught(ex);
                }
                try {
                    int threshold = Integer.parseInt(dimensionInput.getText());
                    dir.getDocument().getAssumptions().getSplitsPostProcess()
                            .setDimensionValue(threshold);
                } catch (NumberFormatException ex) {
                    Basic.caught(ex);
                }
                dir.execute("update " + Splits.NAME);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply modification of splits");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return apply = action;
    }

    AbstractAction launchWeightHistogram;

    public AbstractAction getLaunchWeightHistogram(JTextField weightInput) {
        if (launchWeightHistogram != null)
            return launchWeightHistogram;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Splits splits = dir.getDocument().getSplits();
                List values = new LinkedList();
                for (int i = 1; i <= splits.getNsplits(); i++)
                    values.add(splits.getWeight(i));
                HistogramPanel hp = new HistogramPanel();
                hp.setIncludeZero(true);
                hp.setValues(values);

                JTextField weightInput = (JTextField) getValue(DirectorActions.TEXTAREA);
                float value = 0;
                try {
                    value = (Float.parseFloat(weightInput.getText()));

                } catch (Exception ex) {
                }
                Float result = hp.showThresholdDialog(parent, "Weight threshold",
                        value);
                if (result != null) {
                    weightInput.setText(result.toString());
                    applyWeightThreshold.actionPerformed(null);
                    apply.actionPerformed(null);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Set...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set threshold using histogram");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.TEXTAREA, weightInput);

        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return launchWeightHistogram = action;
    }

    AbstractAction launchConfidenceHistogram;

    public AbstractAction getLaunchConfidenceHistogram(JTextField confidenceInput) {
        if (launchConfidenceHistogram != null)
            return launchConfidenceHistogram;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                Splits splits = dir.getDocument().getSplits();
                List values = new LinkedList();
                for (int i = 1; i <= splits.getNsplits(); i++)
                    values.add(splits.getConfidence(i));
                HistogramPanel hp = new HistogramPanel();
                hp.setIncludeZero(true);
                hp.setValues(values);

                JTextField confidenceInput = (JTextField) getValue(DirectorActions.TEXTAREA);
                float value = 0;
                try {
                    value = (Float.parseFloat(confidenceInput.getText()));

                } catch (Exception ex) {
                }
                Float result = hp.showThresholdDialog(parent, "Confidence Threshold",
                        value);
                if (result != null) {
                    confidenceInput.setText(result.toString());
                    applyConfidenceThreshold.actionPerformed(null);
                    apply.actionPerformed(null);
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Set...");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set threshold using histogram");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.TEXTAREA, confidenceInput);

        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return launchConfidenceHistogram = action;
    }
}
