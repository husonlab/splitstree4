/*
 * FilteredSuperNetwork.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.phylo.PhyloTree;
import jloda.phylo.algorithms.Distortion;
import jloda.swing.util.HistogramPanel;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.progress.ProgressCmdLine;
import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.TreesUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * implements a filtered super network
 * daniel huson, 3.2006
 */
public class FilteredSuperNetwork implements Trees2Splits {
    private int optionMinNumberTrees = 1;
    private int optionMaxDistortionScore = 0;
    final private SuperNetwork superNetwork = new SuperNetwork();
    private String optionEdgeWeights = TREESIZEWEIGHTEDMEAN;
    private boolean optionAllTrivial = true;
    private boolean optionUseTotalScore = false;

    private Splits cacheSplits = null; // cache computed splits for use of histograms

    // edge weight options:
    static final String AVERAGERELATIVE = "AverageRelative";
    static final String MEAN = "Mean";
    static final String TREESIZEWEIGHTEDMEAN = "TreeSizeWeightedMean";
    static final String SUM = "Sum";
    static final String MIN = "Min";
    static final String NONE = "None";
    public final static String DESCRIPTION = "Computes a set of filtered splits from partial trees (Huson, Steel and Whitfield, 2006)";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param doc   the document
     * @param taxa  the taxa
     * @param trees a nexus trees block containing one tree
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Trees trees) {
        cacheSplits = null;
        return doc.isValid(taxa) && doc.isValid(trees);
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param trees a nexus trees block containinga list of trees
     * @return the computed set of consensus splits
     */
    public Splits apply(Document doc, Taxa taxa, Trees trees) throws CanceledException, SplitsException {
        // first compute splits using Z-closure method:
        superNetwork.setOptionEdgeWeights(getOptionEdgeWeights());
        Splits splits = superNetwork.apply(doc, taxa, trees);
        cacheSplits = null;

        doc.notifyTasks("FilteredSuperNetwork", "Processing trees");
        doc.notifySetMaximumProgress(splits.getNsplits());

        BitSet[] tree2taxa = new BitSet[trees.getNtrees() + 1];
        for (int t = 1; t <= trees.getNtrees(); t++) {
            tree2taxa[t] = TreesUtilities.getTaxaPresentInPartialTree(taxa, trees, t).getBits();
            TreesUtilities.setNode2taxa(trees.getTree(t), taxa);
            //System.err.println("number of taxa in tree " + t + ":" + tree2taxa[t].cardinality());
            doc.notifySetProgress(t);
        }

        doc.notifyTasks("FilteredSuperNetwork", "Processing splits");
        doc.notifySetMaximumProgress(splits.getNsplits() * trees.getNtrees());
        int progress = 0;
        Splits result = new Splits();
        result.setNtax(taxa.getNtax());

        System.err.println("Filtering splits:");
        if (getOptionUseTotalScore()) {
            for (int s = 1; s <= splits.getNsplits(); s++) {
                int totalScore = 0;
                BitSet A = splits.get(s).getBits();
                BitSet B = splits.get(s).getComplement(taxa.getNtax()).getBits();
                for (int t = 1; t <= trees.getNtrees(); t++) {
                    BitSet treeTaxa = tree2taxa[t];
                    BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                    treeTaxaAndA.and(A);
                    BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                    treeTaxaAndB.and(B);

                    if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                        try {
                            PhyloTree tree = trees.getTree(t);
                            totalScore += Distortion.computeDistortionForSplit(tree, A, B);
                        } catch (IOException ex) {
                            Basic.caught(ex);
                        }
                    }
                    doc.notifySetProgress(++progress);
                }
                if (totalScore <= getOptionMaxDistortionScore())
                    result.add(splits.get(s), splits.getWeight(s));
            }
        } else // do not use total score
        {
            for (int s = 1; s <= splits.getNsplits(); s++) {
                //System.err.print("s " + s + ":");
                BitSet A = splits.get(s).getBits();
                BitSet B = splits.get(s).getComplement(taxa.getNtax()).getBits();
                int count = 0;
                for (int t = 1; t <= trees.getNtrees(); t++) {
                    BitSet treeTaxa = tree2taxa[t];
                    BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                    treeTaxaAndA.and(A);
                    BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                    treeTaxaAndB.and(B);

                    if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                        try {
                            PhyloTree tree = trees.getTree(t);
                            int score = Distortion.computeDistortionForSplit(tree, A, B);
                            //System.err.print(" " + score);
                            if (score <= getOptionMaxDistortionScore())
                                count++;
                            if (count + (trees.getNtrees() - t) < getOptionMinNumberTrees())
                                break; // no hope to get above threshold
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    } else if ((A.cardinality() == 1 || B.cardinality() == 1)
                            && treeTaxaAndB.cardinality() > 0 && treeTaxaAndB.cardinality() > 0) {
                        count++; // is confirmed split
                        //System.err.print(" +");
                    } else {
                        //System.err.print(" .");
                    }
                    doc.notifySetProgress(++progress);
                }
                //System.err.println(" sum=" + count);
                if ((getOptionAllTrivial() && (A.cardinality() == 1 || B.cardinality() == 1))
                        || count >= getOptionMinNumberTrees()) {
                    result.add(splits.get(s), splits.getWeight(s), (float) count / (float) trees.getNtrees());
                }
            }
        }
        System.err.println("Splits: " + splits.getNsplits() + " -> " + result.getNsplits());
        return result;
    }

    /**
     * gets the threshold (value between 0 and 1)
     *
     * @return the threshold
     */
    public int getOptionMinNumberTrees() {
        sync2model();
        return optionMinNumberTrees;
    }

    /**
     * sets the mininum number of trees for which a split but have a good enough score
     *
	 */
    public void setOptionMinNumberTrees(int optionMinNumberTrees) {
        this.optionMinNumberTrees = Math.max(1, optionMinNumberTrees);
        sync2gui();
    }

    public int getOptionMaxDistortionScore() {
        sync2model();
        return optionMaxDistortionScore;
    }

    /**
     * set the max homoplasy score that we will allows per tree
     *
	 */
    public void setOptionMaxDistortionScore(int optionMaxDistortionScore) {
        this.optionMaxDistortionScore = Math.max(0, optionMaxDistortionScore);
        sync2gui();
    }

    public String getOptionEdgeWeights() {
        sync2model();
        return optionEdgeWeights;
    }

    public void setOptionEdgeWeights(String optionEdgeWeights) {
        this.optionEdgeWeights = optionEdgeWeights;
        sync2gui();
    }

    public boolean getOptionAllTrivial() {
        sync2model();
        return optionAllTrivial;
    }

    public void setOptionAllTrivial(boolean optionAllTrivial) {
        this.optionAllTrivial = optionAllTrivial;
        sync2gui();
    }

    public boolean getOptionUseTotalScore() {
        sync2model();
        return optionUseTotalScore;
    }

    public void setOptionUseTotalScore(boolean optionUseTotalScore) {
        this.optionUseTotalScore = optionUseTotalScore;
        sync2gui();
    }


    /**
     * return the possible chocies for optionEdgeWeights
     *
     * @return list of choices
     */
    public List selectionOptionEdgeWeights(Document doc) {
        final List list = new LinkedList();
        list.add(AVERAGERELATIVE);
        list.add(MEAN);
        list.add(TREESIZEWEIGHTEDMEAN);
        list.add(SUM);
        list.add(MIN);
        list.add(NONE);
        return list;
    }

    private JPanel guiPanel = null;
    private JCheckBox guiUseTotalScoreCheckBox = null;
    private JTextField guiMinNumberTreesField = null;
    private JButton guiMinNumberTreesButton = null;
    private JTextField guiMaxDistortionScoreField = null;
    private JCheckBox guiAllTrivialCheckBox = null;
    private JComboBox guiEdgeWeightsComboBox = null;
    final private List guiActions = new LinkedList();

    /**
     * gets an options panel to be used by the gui
     *
     * @return options panel
     */
    public JPanel getGUIPanel(final Document doc) {
        if (guiPanel != null)
            return guiPanel;

        guiPanel = new JPanel();
        guiPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        guiPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 2;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;


        guiPanel.add(new JLabel("UseTotalScore"), gbc);

        gbc.gridx++;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setOptionUseTotalScore(guiUseTotalScoreCheckBox.isSelected());
            }
        };
        guiUseTotalScoreCheckBox = new JCheckBox();
        guiUseTotalScoreCheckBox.addActionListener(action);
        guiActions.add(action);
        guiPanel.add(guiUseTotalScoreCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        guiPanel.add(new JLabel("MinNumberTrees"), gbc);
        guiMinNumberTreesField = new JTextField("" + optionMinNumberTrees, 5);
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    setOptionMinNumberTrees(Integer.parseInt(guiMinNumberTreesField.getText()));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        };
        guiMinNumberTreesField.addActionListener(action);
        guiMinNumberTreesField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
				try {
					optionMinNumberTrees = Integer.parseInt(guiMinNumberTreesField.getText());
				} catch (Exception ignored) {
				}
            }
        });
        guiActions.add(action);
        gbc.gridx++;
        guiPanel.add(guiMinNumberTreesField, gbc);

        JButton but = new JButton();
        but.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sync2model();
                    List values = computeMinTreeNumbersHistogramForGivenMaxDistortionScore(doc);
                    HistogramPanel hp = new HistogramPanel();
                    hp.setIntegerSteps(true);
                    hp.setValues(values);
                    Float result = hp.showThresholdDialog(null, "Minimum number of trees", optionMinNumberTrees);
                    if (result != null)
						setOptionMinNumberTrees(Math.round(result));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        });
        but.setText("Choose...");
        guiMinNumberTreesButton = but;
        gbc.gridx++;
        gbc.weightx = 1;
        guiPanel.add(but, gbc);
        gbc.weightx = 2;

        gbc.gridy++;
        gbc.gridx = 0;
        guiPanel.add(new JLabel("MaxDistortionScore"), gbc);
        guiMaxDistortionScoreField = new JTextField("" + optionMaxDistortionScore, 5);
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                try {
                    setOptionMaxDistortionScore(Integer.parseInt(guiMaxDistortionScoreField.getText()));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        };
        guiMaxDistortionScoreField.addActionListener(action);
        guiMaxDistortionScoreField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
				try {
					optionMaxDistortionScore = Integer.parseInt(guiMaxDistortionScoreField.getText());
				} catch (Exception ignored) {
				}
            }
        });

        guiActions.add(action);
        gbc.gridx++;
        guiPanel.add(guiMaxDistortionScoreField, gbc);

        but = new JButton();
        but.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sync2model();
                    List values;
                    if (!getOptionUseTotalScore())
                        values = computeDistortionHistrogramForGivenMinTreeNumbers(doc);
                    else
                        values = computeDistortionHistrogram(doc);

                    HistogramPanel hp = new HistogramPanel();
                    hp.setIntegerSteps(true);
                    hp.setValues(values);
                    hp.setReverse(true);
                    hp.setIncludeZero(true);
                    Float result = hp.showThresholdDialog(null, "Maximum distortion score", optionMaxDistortionScore);
                    if (result != null)
						setOptionMaxDistortionScore(Math.round(result));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        });
        but.setText("Choose...");
        gbc.gridx++;
        gbc.weightx = 1;
        guiPanel.add(but, gbc);
        gbc.weightx = 2;

        gbc.gridy++;
        gbc.gridx = 0;
        guiPanel.add(new JLabel("AllTrivial"), gbc);

        gbc.gridx++;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setOptionAllTrivial(guiAllTrivialCheckBox.isSelected());
            }
        };
        guiAllTrivialCheckBox = new JCheckBox();
        guiAllTrivialCheckBox.addActionListener(action);
        guiActions.add(action);
        guiPanel.add(guiAllTrivialCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
		guiPanel.add(new JLabel("EdgeWeights"), gbc);

		gbc.gridx++;
		gbc.gridwidth = 2;
		guiEdgeWeightsComboBox = new JComboBox();
		guiEdgeWeightsComboBox.setEditable(false);
		for (Object o : selectionOptionEdgeWeights(null)) {
			String label = (String) o;
			guiEdgeWeightsComboBox.addItem(label);
		}
		guiEdgeWeightsComboBox.addActionListener(e -> {
			JComboBox cbox = (JComboBox) e.getSource();
			setOptionEdgeWeights((String) cbox.getSelectedItem());
		});
		guiPanel.add(guiEdgeWeightsComboBox, gbc);

		sync2gui();
		guiPanel.revalidate();
		//guiPanel.setPreferredSize(new Dimension(300,300));
		//guiPanel.setSize(guiPanel.getPreferredSize());
		guiPanel.setMinimumSize(guiPanel.getPreferredSize());
		return guiPanel;
	}

    /**
     * sync the the model to the GUI
     */
    private void sync2gui() {
        // do not call the getter methods for the variable here because that would cause
        // an infinite loop
        if (guiMinNumberTreesField != null) {
            guiMinNumberTreesField.setText("" + optionMinNumberTrees);
            guiMinNumberTreesField.setEnabled(!optionUseTotalScore);
            guiMinNumberTreesButton.setEnabled(!optionUseTotalScore);
        }
        if (guiMaxDistortionScoreField != null)
            guiMaxDistortionScoreField.setText("" + optionMaxDistortionScore);
        if (guiEdgeWeightsComboBox != null)
            guiEdgeWeightsComboBox.setSelectedItem(optionEdgeWeights);
        if (guiAllTrivialCheckBox != null)
            guiAllTrivialCheckBox.setSelected(optionAllTrivial);
        if (guiUseTotalScoreCheckBox != null)
            guiUseTotalScoreCheckBox.setSelected(optionUseTotalScore);
    }

    private void sync2model() {
        for (Object guiAction : guiActions) {
            AbstractAction action = (AbstractAction) guiAction;
            action.actionPerformed(null);
        }
        if (guiEdgeWeightsComboBox != null)
            setOptionEdgeWeights((String) guiEdgeWeightsComboBox.getSelectedItem());
    }

    /**
     * generate values for histrogram for distortion score, for a given min number of trees
     *
	 */
    private List computeDistortionHistrogramForGivenMinTreeNumbers(final Document doc) throws Exception {
        doc.setProgressListener(new ProgressCmdLine());
        final List values = new LinkedList();
        final Taxa taxa = doc.getTaxa();
        final Trees trees = doc.getTrees();

        if (cacheSplits == null)
        // first compute splits using Z-closure method:
        {
            superNetwork.setOptionEdgeWeights(getOptionEdgeWeights());
            cacheSplits = superNetwork.apply(doc, taxa, trees);
        }
        Splits splits = cacheSplits;

        doc.notifyTasks("FilteredSuperNetwork", "Processing trees");
        doc.notifySetMaximumProgress(splits.getNsplits());

        BitSet[] tree2taxa = new BitSet[trees.getNtrees() + 1];
        for (int t = 1; t <= trees.getNtrees(); t++) {
            tree2taxa[t] = TreesUtilities.getTaxaPresentInPartialTree(taxa, trees, t).getBits();
            TreesUtilities.setNode2taxa(trees.getTree(t), taxa);
            doc.notifySetProgress(t);
        }

        doc.notifyTasks("FilteredSuperNetwork", "Processing splits");
        doc.notifySetMaximumProgress(splits.getNsplits() * trees.getNtrees());
        int progress = 0;
        Splits result = new Splits();
        result.setNtax(taxa.getNtax());

        for (int s = 1; s <= splits.getNsplits(); s++) {
            BitSet A = splits.get(s).getBits();
            BitSet B = splits.get(s).getComplement(taxa.getNtax()).getBits();
            if (optionAllTrivial && (A.cardinality() == 1 || B.cardinality() == 1))
                continue; // all trivial splits are in by default
            int[] scores = new int[trees.getNtrees()];
            for (int t = 1; t <= trees.getNtrees(); t++) {
                BitSet treeTaxa = tree2taxa[t];
                BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                treeTaxaAndA.and(A);
                BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                treeTaxaAndB.and(B);

                if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                    try {
                        PhyloTree tree = trees.getTree(t);
                        int score = Distortion.computeDistortionForSplit(tree, A, B);
                        scores[t - 1] = score;
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                } else if ((A.cardinality() == 1 || B.cardinality() == 1)
                        && treeTaxaAndB.cardinality() > 0 && treeTaxaAndB.cardinality() > 0) {
                    scores[t - 1] = 0; // is trivial split
                } else
                    scores[t - 1] = -1;
                doc.notifySetProgress(++progress);
            }
            Arrays.sort(scores);
            int i = 0;
            while (i < scores.length && scores[i] == -1)
                i++; // skip over non-valid values
            if (i + optionMinNumberTrees - 1 < scores.length)
                values.add((float) scores[i + optionMinNumberTrees - 1]);
        }
        return values;
    }

    /**
     * generate values for histrogram for distortion score
     *
	 */
    private List computeDistortionHistrogram(final Document doc) throws Exception {
        doc.setProgressListener(new ProgressCmdLine());
        final List values = new LinkedList();
        final Taxa taxa = doc.getTaxa();
        final Trees trees = doc.getTrees();

        if (cacheSplits == null)
        // first compute splits using Z-closure method:
        {
            superNetwork.setOptionEdgeWeights(getOptionEdgeWeights());
            cacheSplits = superNetwork.apply(doc, taxa, trees);
        }
        Splits splits = cacheSplits;

        doc.notifyTasks("FilteredSuperNetwork", "Processing trees");
        doc.notifySetMaximumProgress(splits.getNsplits());

        BitSet[] tree2taxa = new BitSet[trees.getNtrees() + 1];
        for (int t = 1; t <= trees.getNtrees(); t++) {
            tree2taxa[t] = TreesUtilities.getTaxaPresentInPartialTree(taxa, trees, t).getBits();
            TreesUtilities.setNode2taxa(trees.getTree(t), taxa);
            doc.notifySetProgress(t);
        }

        doc.notifyTasks("FilteredSuperNetwork", "Processing splits");
        doc.notifySetMaximumProgress(splits.getNsplits() * trees.getNtrees());
        int progress = 0;
        Splits result = new Splits();
        result.setNtax(taxa.getNtax());

        for (int s = 1; s <= splits.getNsplits(); s++) {
            int totalScore = 0;
            BitSet A = splits.get(s).getBits();
            BitSet B = splits.get(s).getComplement(taxa.getNtax()).getBits();
            if (optionAllTrivial && (A.cardinality() == 1 || B.cardinality() == 1))
                continue; // all trivial splits are in by default
            for (int t = 1; t <= trees.getNtrees(); t++) {
                BitSet treeTaxa = tree2taxa[t];
                BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                treeTaxaAndA.and(A);
                BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                treeTaxaAndB.and(B);

                if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                    try {
                        PhyloTree tree = trees.getTree(t);
                        totalScore += Distortion.computeDistortionForSplit(tree, A, B);
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                }
                doc.notifySetProgress(++progress);
            }
            values.add((float) totalScore);
        }
        return values;
    }


    /**
     * generate values for histrogram
     *
	 */
    private List computeMinTreeNumbersHistogramForGivenMaxDistortionScore(final Document doc) throws Exception {
        doc.setProgressListener(new ProgressCmdLine());
        final List values = new LinkedList();
        final Taxa taxa = doc.getTaxa();
        final Trees trees = doc.getTrees();
        if (cacheSplits == null)
        // first compute splits using Z-closure method:
        {
            superNetwork.setOptionEdgeWeights(getOptionEdgeWeights());
            cacheSplits = superNetwork.apply(doc, taxa, trees);
        }
        Splits splits = cacheSplits;

        doc.notifyTasks("FilteredSuperNetwork", "Processing trees");
        doc.notifySetMaximumProgress(splits.getNsplits());

        BitSet[] tree2taxa = new BitSet[trees.getNtrees() + 1];
        for (int t = 1; t <= trees.getNtrees(); t++) {
            tree2taxa[t] = TreesUtilities.getTaxaPresentInPartialTree(taxa, trees, t).getBits();
            TreesUtilities.setNode2taxa(trees.getTree(t), taxa);
            doc.notifySetProgress(t);
        }

        doc.notifyTasks("FilteredSuperNetwork", "Processing splits");
        doc.notifySetMaximumProgress(splits.getNsplits() * trees.getNtrees());
        int progress = 0;
        Splits result = new Splits();
        result.setNtax(taxa.getNtax());

        for (int s = 1; s <= splits.getNsplits(); s++) {
            BitSet A = splits.get(s).getBits();
            BitSet B = splits.get(s).getComplement(taxa.getNtax()).getBits();
            if (optionAllTrivial && (A.cardinality() == 1 || B.cardinality() == 1))
                continue; // all trivial splits are in by default
            int[] scores = new int[trees.getNtrees()];
            for (int t = 1; t <= trees.getNtrees(); t++) {
                BitSet treeTaxa = tree2taxa[t];
                BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                treeTaxaAndA.and(A);
                BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                treeTaxaAndB.and(B);
                if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                    try {
                        PhyloTree tree = trees.getTree(t);
                        int score = Distortion.computeDistortionForSplit(tree, A, B);
                        scores[t - 1] = score;
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                } else if ((A.cardinality() == 1 || B.cardinality() == 1)
                        && treeTaxaAndB.cardinality() > 0 && treeTaxaAndB.cardinality() > 0) {
                    scores[t - 1] = 0; // is trivial split
                } else
                    scores[t - 1] = -1;
                doc.notifySetProgress(++progress);
            }
            Arrays.sort(scores);
            int i = 0;
            while (i < scores.length && scores[i] == -1)
                i++; // skip over non-valid values
            int nTrees = 0;
            while (i < scores.length && scores[i] <= optionMaxDistortionScore) {
                i++;
                nTrees++;
            }
            if (nTrees > 0)
                values.add((float) nTrees);
        }
        return values;
    }
}
