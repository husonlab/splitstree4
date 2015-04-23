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

import jloda.graph.Edge;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.NotOwnerException;
import jloda.util.Pair;
import splitstree.algorithms.trees.TreeSelector;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * actions associated with a characters window
 *
 * @author huson
 *         Date: 19-Dec-2003
 */
public class SelectTreesActions implements UpdateableActions {
    private Director dir;
    private List all = new LinkedList();
    public static final String JLIST = "JLIST";

    public SelectTreesActions(Director dir) {
        this.dir = dir;
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

    AbstractAction input;

    AbstractAction getInput() {
        if (input != null)
            return input;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Trees to select");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        all.add(action);
        return input = action;
    }


    AbstractAction clear;

    /**
     * use all positions
     *
     * @return action
     */
    AbstractAction getClear() {
        if (clear != null)
            return clear;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JList list = ((JList) input.getValue(JLIST));
                list.clearSelection();
                getApply().actionPerformed(null);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear selection");
        action.putValue(AbstractAction.NAME, "Clear");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        all.add(action);
        return clear = action;
    }

    AbstractAction apply;

    /**
     * apply
     *
     * @return action
     */
    AbstractAction getApply() {
        if (apply != null)
            return apply;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                MainViewer viewer = (MainViewer) dir.getViewerByClass(MainViewer.class);

                JList<String> list = ((JList<String>) input.getValue(JLIST));
                Document doc = dir.getDocument();
                doc.setProgressListener(new jloda.util.ProgressCmdLine());

                Taxa taxa = doc.getTaxa();
                Trees trees = doc.getTrees();
                Splits splits = doc.getSplits();
                if (!doc.isValid(trees) || !doc.isValid(splits))
                    return;
                PhyloGraph graph = viewer.getPhyloGraph();

                Set<Pair<TaxaSet,TaxaSet>> selectedTreesSplits = new HashSet<>(); // splits to select
                TreeSelector treeSelector = new TreeSelector();
                for(String treeName:list.getSelectedValuesList()) {
                    int t;
                    for (t = 1; t <= trees.getNtrees(); t++)
                        if (trees.getName(t).equals(treeName))
                            break;
                    if (t <= trees.getNtrees()) {
                        treeSelector.setOptionWhich(t);
                        Taxa tmpTaxa = (Taxa) taxa.clone();

                        Splits tmpSplits = treeSelector.apply(doc, tmpTaxa, trees);
                        for (int s = 1; s <= tmpSplits.getNsplits(); s++) {
                            TaxaSet aSet = new TaxaSet();
                            TaxaSet aComp = new TaxaSet();
                            mapTaxa(tmpSplits.get(s), tmpTaxa, taxa, aSet, aComp);
                            if (aSet.cardinality() > 0 && aComp.cardinality() > 0) {
                                Pair<TaxaSet,TaxaSet> pair = new Pair<>(aSet, aComp);
                                selectedTreesSplits.add(pair);
                            }
                        }
                    }
                }

                boolean oldUseSplitSelectionModel = viewer.getUseSplitSelectionModel();
                viewer.setUseSplitSelectionModel(false);
                try {
                    BitSet mustSelect = new BitSet();
                    BitSet mustUnselect = new BitSet();
                    for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                        int s = graph.getSplit(e);
                        if (s <= 0 || s > splits.getNsplits())
                            continue; // root edge?

                        if (mustSelect.get(s))
                            viewer.setSelected(e, true);
                        else if (mustUnselect.get(s))
                            viewer.setSelected(e, false);
                        else {
                            TaxaSet aSet = splits.get(s);
                            TaxaSet aComp = splits.get(s).getComplement(taxa.getNtax());

                            Iterator it = selectedTreesSplits.iterator();
                            boolean found = false;
                            while (it.hasNext()) {
                                Pair pair = (Pair) it.next();
                                TaxaSet bSet = (TaxaSet) pair.getFirst();
                                TaxaSet bComp = (TaxaSet) pair.getSecond();

                                if ((aSet.contains(bSet) && aComp.contains(bComp))
                                        || (aSet.contains(bComp) && aComp.contains(bSet))) {
                                    viewer.setSelected(e, true);
                                    mustSelect.set(s);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                viewer.setSelected(e, false);
                                mustUnselect.set(s);
                            }
                        }
                    }
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
                viewer.setUseSplitSelectionModel(oldUseSplitSelectionModel);
                viewer.repaint();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Select trees in the graph");
        action.putValue(AbstractAction.NAME, "Select");
        action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return apply = action;
    }

    /**
     * maps a given split of tmpTaxa to a split aSet,aComp of taxa
     *
     * @param split
     * @param tmpTaxa
     * @param taxa
     * @param aSet
     * @param aComp
     */
    private void mapTaxa(TaxaSet split, Taxa tmpTaxa, Taxa taxa, TaxaSet aSet, TaxaSet aComp) {
        for (int t = 1; t <= tmpTaxa.getNtax(); t++) {
            String label = tmpTaxa.getLabel(t);
            int tt = taxa.indexOf(label);
            if (tt != -1) {
                if (split.get(t))
                    aSet.set(tt);
                else
                    aComp.set(tt);
            }
        }
    }
}
