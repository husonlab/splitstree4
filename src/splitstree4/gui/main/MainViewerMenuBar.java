/*
 * MainViewerMenuBar.java 
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

package splitstree4.gui.main;


import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.AppleSystemMenuItems;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.InfoMessage;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.MenuMnemonics;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;
import splitstree4.algorithms.characters.*;
import splitstree4.algorithms.distances.*;
import splitstree4.algorithms.splits.*;
import splitstree4.algorithms.trees.*;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.algorithms.AlgorithmsWindow;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;
import splitstree4.util.PluginClassLoader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The menu bar for the main viewer
 * @author huson
 * 11.03
 */
public class MainViewerMenuBar extends JMenuBar {
    private Director dir;
    private MainViewer mainViewer;
    int currentTab;

    /**
     * setup the viewer s menu bar
     *
     * @param viewer
     */
    public MainViewerMenuBar(MainViewer viewer, Director dir) {
        super();

        this.mainViewer = viewer;
        this.dir = dir;

        add(getFileMenu());
        add(getEditMenu());
        add(getViewMenu());

        add(getDataMenu());
        add(getDistancesMenu());
        add(getTreesMenu());
        add(getNetworksMenu());
        add(getAnalysisMenu());
        add(getDrawMenu());

        add(getWindowMenu());

        add(getHelpMenu());

        for (int i = 0; i < this.getMenuCount(); i++)
            MenuMnemonics.setMnemonics(this.getMenu(i));
    }


    private JMenu fileMenu;

    /**
     * returns the tool bar for this simple viewer
     */
    private JMenu getFileMenu() {
        if (fileMenu != null)
            return fileMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));

        menu.add(dir.getActions().getNewProject());
        menu.addSeparator();

        menu.add(dir.getActions().getOpenFile());
        menu.add(getRecentFilesMenu());

        /*
        if (SplitsTreeProperties.isMacOS())
            menu.add(dir.getActions().getImportFile());
        else
            dir.getActions().getImportFile();
        */

        if (!ProgramProperties.isMacOS() || SplitsTreeProperties.getExpertMode()) {
            menu.addSeparator();
            menu.add(mainViewer.getActions().getReplaceFile());
            menu.add(mainViewer.getActions().getCloneAction());
        } else {
            menu.add(mainViewer.getActions().getCloneAction());
        }
        menu.addSeparator();
        if (SplitsTreeProperties.USE_SPLIT_PANE) {
            menu.add(mainViewer.getActions().getInputDataDialog());
            menu.addSeparator();
        }

        menu.add(mainViewer.getActions().getClose());
        menu.add(mainViewer.getActions().getSaveFile());

        JMenuItem saveAs = menu.add(mainViewer.getActions().getSaveAsFile());
        saveAs.setDisplayedMnemonicIndex(5);

        menu.addSeparator();

        menu.add(dir.getActions().getExportFile());
        menu.add(mainViewer.getActions().getSaveImage());
        menu.addSeparator();

        JMenu subMenu = new JMenu("Tools");
        subMenu.add(mainViewer.getActions().getLoadMultipleTrees());
        subMenu.add(mainViewer.getActions().getMultiLabeledTree());
        subMenu.add(mainViewer.getActions().getConcatenateSequences());
        subMenu.add(mainViewer.getActions().getCollapseIdentical());

        menu.add(subMenu);
        menu.addSeparator();

        menu.add(mainViewer.getActions().getPrintIt());
        if (!ProgramProperties.isMacOS()) {
            menu.addSeparator();
            menu.add(dir.getActions().getQuit());
        } else {
            AppleSystemMenuItems.setQuitAction(dir.getActions().getQuit());
        }
        return fileMenu = menu;
    }

    private JMenu editMenu;

    private JMenu getEditMenu() {
        if (editMenu != null)
            return editMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Edit", 'E'));

        menu.add(mainViewer.getActions().getUndo());
        menu.add(mainViewer.getActions().getRedo());
        menu.addSeparator();

        menu.add(mainViewer.getActions().getCut());
        menu.add(mainViewer.getActions().getCopy());
        menu.add(mainViewer.getActions().getPaste());

        menu.addSeparator();
        menu.add(mainViewer.getActions().getSelectAll());
        menu.add(mainViewer.getActions().getSelectNodes());
        menu.add(mainViewer.getActions().getSelectLabeledNodes());
        menu.add(mainViewer.getActions().getSelectEdges());
        menu.addSeparator();

        menu.add(mainViewer.getActions().getSelectLatest());
        menu.add(mainViewer.getActions().getSelectInvert());
        menu.addSeparator();
        menu.add(mainViewer.getActions().getDeselectAll());
        menu.add(mainViewer.getActions().getDeselectNodes());
        menu.add(mainViewer.getActions().getDeselectEdges());
        menu.addSeparator();

        menu.add(mainViewer.getActions().getFindReplaceAction());
        menu.add(mainViewer.getActions().getGotoLine());
        menu.addSeparator();
        menu.add(dir.getActions().getPreferences());

        return editMenu = menu;
    }

    private JMenu viewMenu;

    /**
     * gets the view menu
     */
    private JMenu getViewMenu() {
        if (viewMenu != null)
            return viewMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("View", 'V'));

        menu.add(mainViewer.getActions().getIncreaseFontSize());
        menu.add(mainViewer.getActions().getDecreaseFontSize());
        menu.addSeparator();
        menu.add(getViewMenuDataSubmenu());
        menu.addSeparator();

        menu.add(mainViewer.getActions().getResetLayout());
        menu.addSeparator();
        menu.add(mainViewer.getActions().getZoomIn());
        JMenuItem zoomOut = menu.add(mainViewer.getActions().getZoomOut());
        zoomOut.setDisplayedMnemonicIndex(5);
        menu.add(mainViewer.getActions().getSetScale());
        menu.addSeparator();
        menu.add(mainViewer.getActions().getRotateLeft());
        menu.add(mainViewer.getActions().getRotateRight());
        menu.add(mainViewer.getActions().getFlipHorizontal());

        menu.addSeparator();
        //menu.add(mainViewer.getActions().getNodeEdgeConfigAction());
        menu.add(mainViewer.getActions().getNodeEdgeFormatterAction());
        menu.add(mainViewer.getActions().getConfidenceWindow());
        //menu.add(mainViewer.getActions().getMidpointRoot());

        //menu.add(mainViewer.getActions().getNetworkMidpoint());

        menu.addSeparator();
        JCheckBoxMenuItem cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getMagnifierAction(cbox));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getMagnifyAll(cbox));
        menu.add(cbox);

        menu.addSeparator();
        JMenu subMenu = new JMenu("Label Layout");

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getAutoLayoutLabels(cbox));
        subMenu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getRadiallyLayoutLabels(cbox));
        subMenu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getSimpleLayoutLabels(cbox));
        subMenu.add(cbox);

        menu.add(subMenu);


        return viewMenu = menu;
    }

    private JMenu viewMenuDataSubmenu;

    /**
     * gets the view menu for the data pane
     *
     * @return view menu
     */
    private JMenu getViewMenuDataSubmenu() {
        if (viewMenuDataSubmenu != null)
            return viewMenuDataSubmenu;

        JMenu menu = new JMenu(mainViewer.getActions().getMenuTitleActionDataTab("Data", 'D'));

        menu.add(mainViewer.getActions().getExpandAll());
        menu.add(mainViewer.getActions().getCollapseAll());

        menu.addSeparator();

        JMenu subMenu = new JMenu(dir.getActions().getMenuTitleAction(Characters.NAME, 'C'));
        JCheckBoxMenuItem cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Characters.NAME,
                "Interleave", "No Interleave", 'i'));
        subMenu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Characters.NAME,
                "Transpose", "No Transpose", 't'));
        subMenu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Characters.NAME,
                "Labels", "No Labels", 'l'));
        subMenu.add(cbox);
        menu.add(subMenu);

        subMenu = new JMenu(dir.getActions().getMenuTitleAction(Distances.NAME, 'D'));
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Distances.NAME,
                "Triangle=both", "", 'b'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Distances.NAME,
                "Triangle=upper", "", 'u'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Distances.NAME,
                "Triangle=lower", "", 'l'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Distances.NAME,
                "Diagonal", "No diagonal", 'g'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Distances.NAME,
                "Labels", "No labels", 'b'));
        subMenu.add(cbox);
        menu.add(subMenu);

        subMenu = new JMenu(dir.getActions().getMenuTitleAction(Splits.NAME, 'S'));
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Splits.NAME,
                "Weights", "No weights", 'w'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Splits.NAME,
                "Confidences", "No confidences", 'c'));
        subMenu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getFormatChanger(cbox, Splits.NAME,
                "Labels", "No labels", 'l'));
        subMenu.add(cbox);
        menu.add(subMenu);

        return viewMenuDataSubmenu = menu;
    }


    /**
     * call this when closing
     */
    public void dispose() {
        if (recentFilesListener != null)
            SplitsTreeProperties.removePropertiesListListener(recentFilesListener);
    }

    private JMenu recentFilesMenu;
    private PropertiesListListener recentFilesListener;

    /**
     * gets the recent files menu
     *
     * @return recent files menu
     */
    private JMenu getRecentFilesMenu() {
        if (recentFilesMenu != null)
            return recentFilesMenu;
        JMenu menu = new JMenu("Open Recent");
        menu.setIcon(ResourceManager.getIcon("sun/Open16.gif"));

        recentFilesListener = new PropertiesListListener() {
            public boolean isInterested(String name) {
                return name.equals(SplitsTreeProperties.RECENTFILES);
            }

            public void hasChanged(List recentFileNames) {
                if (recentFilesMenu != null) {
                    recentFilesMenu.removeAll();
                    for (Object recentFileName : recentFileNames) {
                        String fileName = (String) recentFileName;
                        recentFilesMenu.add(dir.getActions().getOpenRecent(fileName));
                    }
                    recentFilesMenu.addSeparator();
                    recentFilesMenu.add(mainViewer.getActions().getClearRecentFilesMenu());
                }
                recentFilesMenu.setEnabled(recentFilesMenu.getItemCount() > 2);
                MenuMnemonics.setMnemonics(recentFilesMenu);
            }
        };
        SplitsTreeProperties.addPropertiesListListener(recentFilesListener);
        return recentFilesMenu = menu;
    }

    private JMenu recentMethodsMenu;
    private PropertiesListListener recentMethodsListener;


    /**
     * gets the recent methods menu
     *
     * @return methods files menu
     */
    private JMenu getRecentMethodsMenu() {
        if (recentMethodsMenu != null)
            return recentMethodsMenu;
        JMenu menu = new JMenu("Configure Recent Methods");
        menu.setMnemonic('R');

        recentMethodsListener = new PropertiesListListener() {
            public boolean isInterested(String name) {
                return name.equals(SplitsTreeProperties.RECENTMETHODS);
            }

            public void hasChanged(List recentMethods) {
                if (recentMethodsMenu != null) {
                    recentMethodsMenu.removeAll();
                    for (Object recentMethod : recentMethods) {
                        String str = (String) recentMethod;
                        String blockName = str.substring(0, str.indexOf("+"));
                        String methodName = str.substring(str.indexOf("+") + 1);
                        try {
                            PluginClassLoader.updatePluginURLClassLoader();
                            URLClassLoader ucl = (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(methodName);
                            if (ucl != null) {
                                Class c = Class.forName(methodName, true, ucl);
                                recentMethodsMenu.add(mainViewer.getActions().getLaunchTransform(null, blockName, c, (char) 0, true));
                            } else {
                                recentMethodsMenu.add(mainViewer.getActions().getLaunchTransform(null, blockName, Class.forName(methodName), (char) 0, true));
                            }
                        } catch (ClassNotFoundException ex) {
                            // don't need to report this., just silently forget this old method
                            //Basic.caught(ex);
                        }
                    }
                    recentMethodsMenu.addSeparator();
                    recentMethodsMenu.add(mainViewer.getActions().getClearRecentMethodsMenu());
                    recentMethodsMenu.setEnabled(recentMethodsMenu.getItemCount() > 2);
                }
                MenuMnemonics.setMnemonics(recentMethodsMenu);
            }
        };
        SplitsTreeProperties.addPropertiesListListener(recentMethodsListener);
        return recentMethodsMenu = menu;
    }

    JMenu taxMenu = new JMenu();

    /**
     * update the taxon sets
     */
    public void updateTaxonSets() {
        if (taxMenu != null) {
            taxMenu.removeAll();
            if (dir.getDocument() != null && dir.getDocument().getSets() != null
                    && dir.getDocument().getSets().getNumTaxSets() != 0) {
                Sets S = dir.getDocument().getSets();
                for (String name : S.getTaxaSetNames()) {
                    taxMenu.add(mainViewer.getActions().getSelectTaxSet(name));
                }
                taxMenu.addSeparator();
            }
            taxMenu.add(mainViewer.getActions().getSelectTaxSet("All"));

            taxMenu.add(mainViewer.getActions().getAddSelectedTaxSet());
            //If there are no nodes selected, disable the addSelected action.
            mainViewer.getActions().getAddSelectedTaxSet().setEnabled(!mainViewer.getSelectedNodes().isEmpty());


            taxMenu.addSeparator();
            taxMenu.add(mainViewer.getActions().getClearAllTaxsSets());
            boolean thereAreSets = dir.getDocument().getSets() != null && dir.getDocument().getSets().getNumTaxSets() > 0;

            mainViewer.getActions().getClearAllTaxsSets().setEnabled(thereAreSets);

            taxMenu.setEnabled(taxMenu.getItemCount() > 1);
        }
    }

    private JMenu dataMenu;

    public JMenu getDataMenu() {
        if (dataMenu != null)
            return dataMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Data", 'D'));
        menu.add(mainViewer.getActions().getKeepOnlySelectedTaxa());
        menu.add(mainViewer.getActions().getExcludeSelectedTaxa());
        menu.add(mainViewer.getActions().getRestoreAllTaxa());

        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Taxa.NAME, AlgorithmsWindow.FILTER, null, 'T'));

        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Taxa.NAME, AlgorithmsWindow.TRAITS, null, (char) 0));

        taxMenu = new JMenu("Taxon Sets");
        menu.add(taxMenu);
        updateTaxonSets();

        menu.add(mainViewer.getActions().getChooseOutgroup());

        menu.addSeparator();
        JCheckBoxMenuItem cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getExcludeGaps(cbox));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getExcludeNonParsimony(cbox));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getExcludeConstant(cbox));
        menu.add(cbox);
        menu.add(mainViewer.getActions().getRestoreAllCharacters());

        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Characters.NAME,
                AlgorithmsWindow.FILTER, null, (char) 0));
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getGreedilyMakeCompatible(cbox));
        menu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getGreedilyMakeWeaklyCompatible(cbox));
        menu.add(cbox);

        menu.add(mainViewer.getActions().getHideSelected());
        menu.add(mainViewer.getActions().getRestoreAllSplits());


        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Splits.NAME,
                AlgorithmsWindow.FILTER, null, (char) 0));
        menu.addSeparator();

        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Trees.NAME,
                AlgorithmsWindow.FILTER, null, (char) 0));
        menu.add(mainViewer.getActions().getEditTreeNames());

        return dataMenu = menu;
    }


    private JMenu distancesMenu;

    /**
     * gets the distances menu
     *
     * @return distances menu
     */
    public JMenu getDistancesMenu() {
        if (distancesMenu != null)
            return distancesMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Distances", 'i'));

        JCheckBoxMenuItem cbox;

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, Uncorrected_P.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, LogDet.class, (char) 0));
        menu.add(cbox);


        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, HKY85.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, JukesCantor.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, K2P.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, K3ST.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, F81.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, F84.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, ProteinMLdist.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, NeiMiller.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, GeneContentDistance.class, (char) 0));
        menu.add(cbox);

        JMenu rmenu = new JMenu("Restriction Site");


        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, Nei_Li_RestrictionDistance.class, (char) 0));
        rmenu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, Upholt.class, (char) 0));
        rmenu.add(cbox);
        menu.add(rmenu);

        return distancesMenu = menu;
    }

    private JMenu treesMenu;

    public JMenu getTreesMenu() {
        if (treesMenu != null)
            return treesMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Trees", 't'));

        JCheckBoxMenuItem cbox;

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, NJ.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, BioNJ.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, UPGMA.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, BunemanTree.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Trees.NAME, TreeSelector.class, (char) 0));
        menu.add(cbox);
        menu.add(mainViewer.getActions().getPreviousTree());
        menu.add(mainViewer.getActions().getNextTree());
        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Trees.NAME, ConsensusTree.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, PhylipParsimony.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, PhyML.class, (char) 0));
        menu.add(cbox);

        return treesMenu = menu;
    }

    private JMenu networksMenu;

    public JMenu getNetworksMenu() {
        if (networksMenu != null)
            return networksMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Networks", 'N'));

        JCheckBoxMenuItem cbox;

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, NeighborNet.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Distances.NAME, SplitDecomposition.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, ParsimonySplits.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Trees.NAME, ConsensusNetwork.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Trees.NAME, SuperNetwork.class, (char) 0));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Trees.NAME, FilteredSuperNetwork.class, (char) 0));
        menu.add(cbox);
        menu.addSeparator();

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, MedianNetwork.class, (char) 0));
        menu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, MedianJoining.class, (char) 0));
        menu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, PrunedQuasiMedian.class, (char) 0));
        menu.add(cbox);

        menu.addSeparator();
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, MinSpanningNetwork.class, (char) 0));
        menu.add(cbox);

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Characters.NAME, SpectralSplits.class, (char) 0));
        menu.add(cbox);
        return networksMenu = menu;
    }


    private JMenu analysisMenu;

    public JMenu getAnalysisMenu() {
        if (analysisMenu != null)
            return analysisMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Analysis", 'A'));

        menu.add(dir.getActions().getBootstrapping());
        menu.add(dir.getActions().getBootstrappingNetwork());
        menu.add(dir.getActions().getConfidenceNetwork());
        menu.addSeparator();

        menu.add(mainViewer.getActions().getEstimateInvariableSites());
        menu.add(mainViewer.getActions().getComputePhylogeneticDiversity());
        menu.add(mainViewer.getActions().getComputeDeltaScore());

        menu.add(mainViewer.getActions().getConductPhiTest());

        /* TODO: both of these seem broken:
        menu.add(viewer.getActions().getTestTreeness());
        menu.add(viewer.getActions().getEstimateAlpha());
        */

        menu.addSeparator();

        menu.add(mainViewer.getActions().getConfigureAllMethods());
        menu.add(getRecentMethodsMenu());

        return analysisMenu = menu;
    }

    private JMenu drawMenu;

    public JMenu getDrawMenu() {
        if (drawMenu != null)
            return drawMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Draw", 'R'));

        JCheckBoxMenuItem cbox;

        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, EqualAngle.class, (char) 0, ResourceManager.getIcon("EqualAngle16.gif")));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, RootedEqualAngle.class, (char) 0, ResourceManager.getIcon("RootedEqualAngle16.gif")));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, ConvexHull.class, (char) 0, ResourceManager.getIcon("ConvexHull16.gif")));
        menu.add(cbox);
        if (SplitsTreeProperties.ALLOW_CLUSTER_NETWORK) {
            cbox = new JCheckBoxMenuItem();
            cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, ClusterNetwork.class, (char) 0, ResourceManager.getIcon("RootedEqualAngle16.gif")));
            menu.add(cbox);
        }
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, Phylogram.class, (char) 0, ResourceManager.getIcon("Phylogram16.gif")));
        menu.add(cbox);
        cbox = new JCheckBoxMenuItem();
        cbox.setAction(mainViewer.getActions().getLaunchTransform(cbox, Splits.NAME, NoGraph.class, (char) 0, ResourceManager.getIcon("Empty16.gif")));
        menu.add(cbox);
        menu.addSeparator();

        menu.add(mainViewer.getActions().getReroot());

        menu.addSeparator();
        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Characters.NAME, AlgorithmsWindow.SELECT, null, (char) 0));
        if (SplitsTreeProperties.getExpertMode()) {
            menu.addSeparator();
            menu.add(mainViewer.getActions().getOptmizeSelected());
        }
        menu.addSeparator();
        menu.add(mainViewer.getActions().getHideSelectedEdges());
        menu.add(mainViewer.getActions().getHideUnselectedSplits());
        menu.add(mainViewer.getActions().getHideIncompatibleEdges());

        menu.add(mainViewer.getActions().getRestoreHiddenEdges());
        menu.addSeparator();
        menu.add(mainViewer.getActions().getAddMethodWindowFilter(Trees.NAME,
                AlgorithmsWindow.SELECT, null, (char) 0));
        return drawMenu = menu;
    }

    public MainViewer getMainViewer() {
        return mainViewer;
    }

    /**
     * get all the useful actions defined in the menus
     *
     * @return list of actions
     */
    public List getActions() {
        List actions = new LinkedList();

        for (int i = 0; i < getMenuCount(); i++) {
            JMenu menu = getMenu(i);
            int top = menu.getItemCount();
            if (menu == windowMenu)
                top = ProjectManager.getWindowMenuBaseSize(windowMenu);

            for (int t = 0; t < top; t++) {
                if (menu.getItem(t) != null && menu.getItem(t).getAction() != null)
                    actions.add(menu.getItem(t).getAction());
            }
        }
        return actions;
    }

    private JMenu windowMenu = null;

    /**
     * gets the windows menu associated with this project
     *
     * @return windows menu
     */
    public JMenu getWindowMenu() {
        if (windowMenu != null)
            return windowMenu;

        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Window", 'W'));

        menu.add(mainViewer.getActions().getWindowSize());

        menu.addSeparator();

        menu.add(dir.getActions().getRunCommand());
        menu.add(dir.getActions().getMessageWindow());

        menu.addSeparator();
        menu.add(mainViewer.getActions().getNodeEdgeFormatterAction());
        menu.add(mainViewer.getActions().getFindReplaceAction());

        return windowMenu = menu;
    }

    private JMenu helpMenu;

    public JMenu getHelpMenu() {
        if (helpMenu != null)
            return drawMenu;
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Help", 'H'));

        if (ProgramProperties.isMacOS()) {
            AppleSystemMenuItems.setAboutAction(dir.getActions().getAboutWindow());
        } else {
            menu.add(dir.getActions().getAboutWindow());
            menu.addSeparator();
        }

        menu.add(dir.getActions().getHowToCite());
        menu.addSeparator();


        JMenu subMenu = new JMenu("Nexus Syntax");
        subMenu.setMnemonic('N');
        subMenu.setIcon(ResourceManager.getIcon("sun/Help16.gif"));

        Iterator it = Document.getListOfBlockNames().iterator();
        ArrayList<Integer> mnemonics = new ArrayList<>();

        while (it.hasNext()) {
            String name = (String) it.next();
            Integer mnemonic = null;
            for (int i = 0; i < name.length(); i++) {

                if (Character.isLetter(name.charAt(i))) {
                    Integer cMnemonic = (int) Character.toUpperCase(name.charAt(i));
                    if (!mnemonics.contains(cMnemonic)) {
                        mnemonic = cMnemonic;
                        mnemonics.add(cMnemonic);
                        break;
                    }
                }
            }
            subMenu.add(dir.getActions().getSyntaxAction(name, mnemonic));
        }
        menu.add(subMenu);

        menu.add(dir.getActions().getCommandHelp());

        menu.addSeparator();

        JMenuItem webSite = new JMenuItem(new AbstractAction("Web Site...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    BasicSwing.openWebPage(new URL("http://ab.inf.uni-tuebingen.de/software/splitstree4/welcome.html"));
                } catch (MalformedURLException ex) {
                    Basic.caught(ex);
                }
            }
        });
        webSite.setIcon(ResourceManager.getIcon("sun/WebComponent16.gif"));
        menu.add(webSite);

        JMenuItem ref = new JMenuItem(new AbstractAction("Reference Manual...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    BasicSwing.openWebPage(new URL("http://ab.inf.uni-tuebingen.de/data/software/splitstree4/download/manual.pdf"));
                } catch (MalformedURLException ex) {
                    Basic.caught(ex);
                }
            }
        });
        ref.setIcon(ResourceManager.getIcon("sun/WebComponent16.gif"));
        menu.add(ref);

        menu.addSeparator();

        menu.add(new AbstractAction("Check For Updates...") {
            /**
             * Invoked when an action occurs.
             *
             * @param ae
             */
            @Override
            public void actionPerformed(ActionEvent ae) {
                ApplicationDisplayMode applicationDisplayMode = ProgramProperties.isUseGUI() ? ApplicationDisplayMode.GUI : ApplicationDisplayMode.CONSOLE;
                UpdateDescriptor updateDescriptor;
                try {
                    updateDescriptor = UpdateChecker.getUpdateDescriptor("http://software-ab.informatik.uni-tuebingen.de/download/splitstree4/updates.xml", applicationDisplayMode);
                } catch (Exception ex) {
                    Basic.caught(ex);
                    new InfoMessage(mainViewer.getFrame(), "Installed version is up-to-date");
                    return;
                }
                if (updateDescriptor.getEntries().length > 0) {
                    if (!ProgramProperties.isUseGUI()) {
                        UpdateDescriptorEntry entry = updateDescriptor.getEntries()[0];
                        new InfoMessage(mainViewer.getFrame(), "New version available: " + entry.getNewVersion()
                                + "\nPlease download from: http://software-ab.informatik.uni-tuebingen.de/download/splitstree4");
                        return;
                    }
                } else {
                    new InfoMessage(mainViewer.getFrame(), "Installed version is up-to-date");
                    return;
                }


                // This will return immediately if you call it from the EDT,
// otherwise it will block until the installer application exits
                ApplicationLauncher.launchApplicationInProcess("1691242391", null, new ApplicationLauncher.Callback() {
                    public void exited(int exitValue) {
                        //TODO add your code here (not invoked on event dispatch thread)
                    }

                    public void prepareShutdown() {
                        ProgramProperties.store();
                    }
                }, ApplicationLauncher.WindowMode.FRAME, null);
            }
        });


        return helpMenu = menu;
    }
}
