/**
 * AlgorithmsWindow.java
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
package splitstree4.gui.algorithms;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IUpdateableView;
import jloda.swing.window.WindowListenerAdapter;
import jloda.util.ProgramProperties;
import splitstree4.algorithms.Transformation;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.UpdateableActions;
import splitstree4.gui.algorithms.filter.FilterCharactersPanel;
import splitstree4.gui.algorithms.filter.FilterReticulatesPanel;
import splitstree4.gui.algorithms.filter.FilterTaxaPanel;
import splitstree4.gui.algorithms.filter.FilterTreesPanel;
import splitstree4.gui.algorithms.modify.ModifySplitsPanel;
import splitstree4.gui.algorithms.modify.ModifyTaxaPanel;
import splitstree4.gui.main.MainViewer;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Allows user to configure all algorithms used in computation of graph
 *
 * @author huson
 * Date: 04-Dec-2003
 */
public class AlgorithmsWindow implements IDirectableViewer {
    private boolean uptoDate;
    private Director dir;
    private AlgorithmsWindowActions actions;
    private AlgorithmsWindowMenuBar menuBar;
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private String[] tabid2data = new String[10];
    private Map<String, Component> name2tabid = new HashMap<>();
    private java.util.List updateableActions = new LinkedList();
    private java.util.List updateableViews = new LinkedList();
    public static final String METHOD = "Method";
    public static final String FILTER = "Filter";
    public static final String SELECT = "Select";
    public static final String TRAITS = "Traits"; //OK - a violation of the scheme, but easier for the user

    /**
     * sets up the algorithms window
     *
     * @param dir
     */
    public AlgorithmsWindow(Director dir) {
        this.dir = dir;
        actions = new AlgorithmsWindowActions(this, dir);
        addUpdateableActionsListener(actions);
        menuBar = new AlgorithmsWindowMenuBar(this, dir);
        setUptoDate(true);

        frame = new JFrame();
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        /*
        JToolBar toolBar=new JToolBar();
        toolBar.add(getActions().getClose());
        frame.getContentPane().add(toolBar,BorderLayout.PAGE_START);
        */
        // frame.setJMenuBar(menuBar);
        if (ProgramProperties.isMacOS())
            // Original size was (630,350) which did not quite fit all of the options
            //  for some characters2distances methods (like GTR).
            frame.setSize(630, 450); //Needed tweaking on OS X. -DB.
        else
            frame.setSize(560, 450);

        if (SplitsTreeProperties.ALLOW_RETICULATE) {
            if (ProgramProperties.isMacOS())
                // Original size was (630,350) which did not quite fit all of the options
                //  for some characters2distances methods (like GTR).
                frame.setSize(700, 450); //Needed tweaking on OS X. -DB.
            else
                frame.setSize(630, 450);
        }
        dir.setViewerLocation(this);

        setTitle(dir);
        tabbedPane = new JTabbedPane();

        tabid2data[tabbedPane.getTabCount()] = Taxa.NAME;
        Component tab = getTaxaTab();
        name2tabid.put(Taxa.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Unaligned.NAME;
        tab = getUnalignedTab();
        name2tabid.put(Unaligned.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Characters.NAME;
        tab = getCharactersTab();
        name2tabid.put(Characters.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Distances.NAME;
        tab = getDistancesTab();
        name2tabid.put(Distances.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Quartets.NAME;
        tab = getQuartetsTab();
        name2tabid.put(Quartets.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Trees.NAME;
        tab = getTreesTab();
        name2tabid.put(Trees.NAME, tab);
        tabbedPane.add(tab);

        tabid2data[tabbedPane.getTabCount()] = Splits.NAME;
        tab = getSplitsTab();
        name2tabid.put(Splits.NAME, tab);
        tabbedPane.add(tab);

        if (SplitsTreeProperties.ALLOW_RETICULATE) {
            tabid2data[tabbedPane.getTabCount()] = Reticulate.NAME;
            tab = getReticulateTab();
            name2tabid.put(Reticulate.NAME, tab);
            tabbedPane.add(tab);
        }
        notifyUpdateEnableStateListener();

        frame.getContentPane().add(tabbedPane);
        frame.setVisible(true);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                AlgorithmsWindow.this.dir.removeViewer(AlgorithmsWindow.this);
            }

            public void windowActivated(WindowEvent event) {
                super.windowActivated(event);
                // updateView(IDirector.ALL);
                // do not update, as this will clobber what the user just selected in the menu!
            }
        });

        if (dir.isInUpdate())
            lockUserInput();
    }

    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Processing Pipeline - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Processing Pipeline - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * gets the title of this viewer
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptoDate;
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what is to be updated?
     */
    public void updateView(String what) {
        notifyUpdateView(what);

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        // disable all non applicable tabs, and select other one, if neccesary
        boolean changeTab = false;
        int firstEnabledTab = -1;

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            boolean enable = dir.getDocument().isValidByName(tabid2data[i]);
            if (enable && firstEnabledTab == -1)
                firstEnabledTab = i;
            if (!enable && i == tabbedPane.getSelectedIndex())
                changeTab = true;
            tabbedPane.setEnabledAt(i, enable);
        }
        if (changeTab)
            tabbedPane.setSelectedIndex(firstEnabledTab);

        notifyUpdateEnableStateListener();

        setUptoDate(true);
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        notifyEnableCritical(false);
        notifyEnableCritical(false);
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        notifyEnableCritical(true);
        notifyUpdateEnableStateListener();
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() {
        this.getFrame().dispose();
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * gets the frame
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * returns the actions object
     *
     * @return actions
     */
    public AlgorithmsWindowActions getActions() {
        return actions;
    }

    // here we configure the tabs:


    /**
     * the unaligned tab
     *
     * @return
     */
    private Component getUnalignedAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Unaligned.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());

        comboBox.setToolTipText("Choose unaligned transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab, getActions().getApplyUnalignedTransform(comboBox)));
        comboBox.addActionListener(new BlockComboListener(comboBox));

        loadComboBox(comboBox, getActions().getUnalignedTransformActions());

        tab.setCBoxLabel("Choose unaligned transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyUnalignedTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }

    /**
     * sets up the characters tab
     *
     * @return the characters tab
     */
    private Component getCharactersAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Characters.NAME);

        final JComboBox comboBox = new JComboBox();

        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        // Set the ComboBox to hold 20 rows before using the scrollbar
        if (ProgramProperties.isMacOS()) {
            comboBox.setMaximumRowCount(20);
        }

        comboBox.setToolTipText("Choose characters transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab, getActions().getApplyCharactersTransform(comboBox)));

        loadComboBox(comboBox, getActions().getCharactersTransformActions());

        tab.setCBoxLabel("Choose characters transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyCharactersTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }


    /**
     * sets up the distances tab
     *
     * @return
     */
    private Component getDistancesAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Distances.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        // Set the ComboBox to hold 20 rows before using the scrollbar
        if (ProgramProperties.isMacOS()) {
            comboBox.setMaximumRowCount(20);
        }

        comboBox.setToolTipText("Choose distances transformation");

        // set up cBox action (interaction) with apply button
        comboBox.setAction(getActions().getComboBoxAction(dir, tab,
                getActions().getApplyDistancesTransform(comboBox)));

        loadComboBox(comboBox, getActions().getDistancesTransformActions());

        tab.setCBoxLabel("Choose distances transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyDistancesTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });

        return tab;
    }

    /**
     * setup the quartets tab
     *
     * @return
     */
    private Component getQuartetsAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Quartets.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        comboBox.setToolTipText("Choose quartets transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab,
                getActions().getApplyQuartetsTransform(comboBox)));

        loadComboBox(comboBox, getActions().getQuartetsTransformActions());

        tab.setCBoxLabel("Choose quartets transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyQuartetsTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }

    /**
     * setup the trees tab
     *
     * @return
     */
    private Component getTreesAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Trees.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        comboBox.setToolTipText("Choose trees transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab,
                getActions().getApplyTreesTransform(comboBox)));

        loadComboBox(comboBox, getActions().getTreesTransformActions());

        tab.setCBoxLabel("Choose trees transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyTreesTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }


    /**
     * sets up the splits tab  for methods
     *
     * @return
     */
    private Component getSplitsAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Splits.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        comboBox.setToolTipText("Choose splits transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab,
                getActions().getApplySplitsTransform(comboBox)));

        loadComboBox(comboBox, getActions().getSplitsTransformActions());

        tab.setCBoxLabel("Choose splits transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplySplitsTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }

    private Component getReticulateAlgorithmTab() {
        final AlgorithmsTab tab = new AlgorithmsTab();
        tab.setName(Reticulate.NAME);

        final JComboBox comboBox = new JComboBox();
        //For the separator
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBox.addActionListener(new BlockComboListener(comboBox));

        comboBox.setToolTipText("Choose reticulate transformation");

        comboBox.setAction(getActions().getComboBoxAction(dir, tab,
                getActions().getApplyReticulateTransform(comboBox)));

        loadComboBox(comboBox, getActions().getReticulateTransformActions());

        tab.setCBoxLabel("Choose reticulate transformation:");
        tab.setCBox(comboBox);
        tab.setApplyButton(new JButton(getActions().getApplyReticulateTransform(comboBox)));
        tab.setDescriptionLabel("Description");
        tab.setDataSummaryLabel("Data:");
        tab.setOptionsPanel(new JPanel());
        tab.setup();

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = (String) ((AbstractAction) comboBox.getSelectedItem()).getValue(AbstractAction.NAME);
                    boolean hide = ProgramProperties.get("HideDialog." + name, false);
                    tab.hideDialog.setSelected(hide);
                } catch (Exception ex) {
                }
            }
        });
        return tab;
    }


    /**
     * selects the named tab, subtab and transform
     *
     * @param tabName
     * @param subTabName
     * @param transformName
     */
    public void select(String tabName, String subTabName, String transformName) {
        boolean found = false;
        for (int tabId = 0; tabId < tabbedPane.getTabCount(); tabId++) {
            String name = tabbedPane.getTitleAt(tabId);
            if (name.equalsIgnoreCase(tabName))
                found = true;
            if (found && dir.getDocument().isValidByName(name)) {
                tabbedPane.setSelectedIndex(tabId);
                JTabbedPane subTabs = (JTabbedPane) name2tabid.get(name);
                int subTabId = subTabs.indexOfTab(subTabName);

                subTabs.setSelectedIndex(subTabId);
                if (subTabName.equals(AlgorithmsWindow.METHOD)) {
                    AlgorithmsTab tab = (AlgorithmsTab) subTabs.getComponentAt(subTabId);
                    JComboBox cBox = tab.cBox;
                    for (int i = 0; i < cBox.getItemCount(); i++) {

                        // Important to check if the item is an instance of AbstractAction and not only a separator...
                        if (cBox.getItemAt(i) instanceof AbstractAction) {
                            AbstractAction action = (AbstractAction) cBox.getItemAt(i);
                            Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                            if (transform != null) {
                                String transName = transform.getClass().getName();
                                if (transName.equals(transformName)) {
                                    cBox.setSelectedIndex(i);
                                    return;
                                }
                            }
                        }//end if instanceof
                    }
                }
                break;
            }
        }
        getActions().updateEnableState();
    }

    /**
     * loads a combobox with all transforms found in a given package, of given type
     *
     * @param cbox
     * @param actions
     */
    /*
     * old version of this function by Daniel
     */
    /*
         private void loadComboBox(JComboBox cbox, java.util.List actions) {
           Iterator it = actions.iterator();
           while (it.hasNext()) {
             cbox.addItem(it.next());
           }
         }
        */
    private void loadComboBox(JComboBox cbox, java.util.List actions) {
        String SEPARATOR = "SEPARATOR";
        boolean needSeparator = false;
        Object currItem, prevItem;
        prevItem = null;
        for (Object action : actions) {
            currItem = action;

            if (prevItem != null) {
                String name1 = (String) (((AbstractAction) prevItem).getValue(AbstractAction.NAME));
                String name2 = (String) (((AbstractAction) currItem).getValue(AbstractAction.NAME));

                if (name1.compareTo(name2) > 0) {
                    //System.out.println(name1 + " is 'greater' than " + name2 + " ; Need a separator.");
                    //needSeparator = true;
                    //TODO: Need to ask if same type of object!
                }
            }

            if (needSeparator) {
                cbox.addItem(SEPARATOR);
                cbox.addItem(currItem);
                prevItem = currItem;
                needSeparator = false;
            } else {
                cbox.addItem(currItem);
                prevItem = currItem;
            }
        }
    }

    /**
     * gets the combobox associated with a name
     *
     * @param name
     */
    public JComboBox getComboBox(String name) {
        JTabbedPane pane = ((JTabbedPane) name2tabid.get(name));
        return ((AlgorithmsTab) pane.getComponentAt(pane.indexOfTab(AlgorithmsWindow.METHOD))).getCBox();
    }

    /**
     * gets the complete splits tab
     *
     * @return
     */
    private Component getTaxaTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Taxa.NAME);
        tabs.addTab(AlgorithmsWindow.FILTER, getFilterTaxaTab());
        tabs.addTab(AlgorithmsWindow.TRAITS, getModifyTaxaTab());  //TODO: IN progress. Traits panel for the taxa.
        return tabs;
    }

    /**
     * gets the complete unaligned tab
     *
     * @return
     */
    private Component getUnalignedTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Unaligned.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getUnalignedAlgorithmTab());
        //tabs.addTab(AlgorithmsWindow.SELECT,new JPanel());
        //tabs.addTab(AlgorithmsWindow.MODIFY,new JPanel());
        return tabs;
    }

    /**
     * gets the complete charactres tab
     *
     * @return
     */
    private Component getCharactersTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Characters.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getCharactersAlgorithmTab());
        tabs.addTab(AlgorithmsWindow.FILTER, getFilterCharactersTab());
        tabs.addTab(AlgorithmsWindow.SELECT, getSelectCharactersTab());

        // tabs.addTab(AlgorithmsWindow.MODIFY,new JPanel());
        return tabs;
    }

    /**
     * gets the complete distances tab
     *
     * @return
     */
    private Component getDistancesTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Distances.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getDistancesAlgorithmTab());
        //tabs.addTab(AlgorithmsWindow.SELECT,new JPanel());
        //tabs.addTab(AlgorithmsWindow.MODIFY,new JPanel());
        return tabs;
    }


    /**
     * gets the complete quartes tab
     *
     * @return
     */
    private Component getQuartetsTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Quartets.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getQuartetsAlgorithmTab());
        //tabs.addTab(AlgorithmsWindow.SELECT,new JPanel());
        //tabs.addTab(AlgorithmsWindow.MODIFY,new JPanel());
        return tabs;
    }

    /**
     * gets the complete trees tab
     *
     * @return
     */
    private Component getTreesTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Trees.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getTreesAlgorithmTab());
        tabs.addTab(AlgorithmsWindow.FILTER, getFilterTreesTab());
        tabs.addTab(AlgorithmsWindow.SELECT, getSelectTreesTab());
        return tabs;
    }

    /**
     * gets the complete splits tab
     *
     * @return
     */
    private Component getSplitsTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Splits.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getSplitsAlgorithmTab());
        // tabs.addTab(AlgorithmsWindow.SELECT,new JPanel());
        tabs.addTab(AlgorithmsWindow.FILTER, getModifySplitsTab());
        return tabs;
    }

    private Component getReticulateTab() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName(Reticulate.NAME);
        tabs.addTab(AlgorithmsWindow.METHOD, getReticulateAlgorithmTab());
        tabs.addTab(AlgorithmsWindow.FILTER, getFilterReticulatesTab());
        return tabs;
    }

    /**
     * gets the filter taxa tab
     *
     * @return
     */
    private Component getFilterTaxaTab() {
        FilterTaxaPanel panel = new FilterTaxaPanel(dir, (MainViewer) dir.getViewerByClass(MainViewer.class));
        addUpdateableActionsListener(panel);
        addUpdateableView(panel);
        return panel;
    }


    /**
     * gets the filter characters tab
     *
     * @return
     */
    private Component getFilterCharactersTab() {
        FilterCharactersPanel panel = new FilterCharactersPanel(dir);
        addUpdateableActionsListener(panel.getActions());
        addUpdateableView(panel);
        return panel;
    }

    /**
     * gets the select characters tab
     *
     * @return
     */
    private Component getSelectCharactersTab() {
        splitstree4.gui.algorithms.select.SelectCharactersPanel panel = new
                splitstree4.gui.algorithms.select.SelectCharactersPanel(dir);
        addUpdateableActionsListener(panel.getActions());
        addUpdateableView(panel);
        return panel;
    }

    /**
     * gets the select trees tab
     *
     * @return
     */
    private Component getFilterTreesTab() {
        FilterTreesPanel panel = new FilterTreesPanel(dir);
        addUpdateableActionsListener(panel);
        addUpdateableView(panel);
        return panel;
    }

    /**
     * gets the select trees tab
     *
     * @return
     */
    private Component getSelectTreesTab() {
        splitstree4.gui.algorithms.select.SelectTreesPanel
                panel = new splitstree4.gui.algorithms.select.SelectTreesPanel(dir);
        addUpdateableActionsListener(panel.getActions());
        addUpdateableView(panel);
        return panel;
    }


    /**
     * gets the modify taxa tab
     *
     * @return
     */
    private Component getModifyTaxaTab() {
        ModifyTaxaPanel panel = new ModifyTaxaPanel(dir);
        addUpdateableActionsListener(panel);
        addUpdateableView(panel);
        return panel;
    }


    /**
     * gets the modify splits tab
     *
     * @return
     */
    private Component getModifySplitsTab() {
        ModifySplitsPanel panel = new ModifySplitsPanel(dir);
        addUpdateableActionsListener(panel.getActions());
        addUpdateableView(panel);
        return panel;
    }

    private Component getFilterReticulatesTab() {
        FilterReticulatesPanel panel = new FilterReticulatesPanel(dir);
        addUpdateableActionsListener(panel);
        addUpdateableView(panel);
        return panel;
    }


    /**
     * adds an updateable action listener
     *
     * @param ua
     */
    public void addUpdateableActionsListener(UpdateableActions ua) {
        updateableActions.add(ua);
    }

    /**
     * update all actions
     */
    public void notifyUpdateEnableStateListener() {
        for (Object updateableAction : updateableActions) {
            UpdateableActions ua = (UpdateableActions) updateableAction;
            ua.updateEnableState();
        }
    }


    /**
     * chage the selection state of cirtical items
     *
     * @param onOff
     */
    private void notifyEnableCritical(boolean onOff) {
        for (Object updateableAction : updateableActions) {
            UpdateableActions ua = (UpdateableActions) updateableAction;
            ua.setEnableCritical(onOff);
        }
    }

    /**
     * adds an updateable action listener
     *
     * @param uv
     */
    public void addUpdateableView(IUpdateableView uv) {
        updateableViews.add(uv);
    }

    /**
     * update all actions
     */
    public void notifyUpdateView(String what) {
        for (Object updateableView : updateableViews) {
            IUpdateableView uv = (IUpdateableView) updateableView;
            uv.updateView(what);
        }
    }


    /**
     * @author miguel
     * <p/>
     * An inner class to render a combobox that contains objects.
     * (One that may contain Separators as well)
     */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer {
        final String SEPARATOR = "SEPARATOR";
        JSeparator separator;

        public ComboBoxRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(1, 1, 1, 1));
            separator = new JSeparator(JSeparator.HORIZONTAL);
        }

        public Component getListCellRendererComponent(JList list, // uses this object's colors to set up foreground and background colors and set up the font.
                                                      Object value, // the object to render.
                                                      int index, // the index of the object to render.
                                                      boolean isSelected, // determine which colors to use.
                                                      boolean cellHasFocus)// indicates whether the  object to render has the focus.
        {

            String str = (value == null) ? "" : value.toString();
            if (SEPARATOR.equals(str)) {
                return separator;
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value == null) return this;

            setFont(list.getFont());
            String name = (String) (((AbstractAction) value).getValue(AbstractAction.NAME));
            //setText(str);
            setText(name);
            return this;
        }
    }

    /**
     * @author miguel
     * <p/>
     * An inner class to listen to actions performed in a ComboBox with Separators
     */
    class BlockComboListener implements ActionListener {
        final String SEPARATOR = "SEPARATOR";
        JComboBox comboBox;
        Object currentItem;
        int currentIndex;

        BlockComboListener(JComboBox comboBox) {
            this.comboBox = comboBox;
            comboBox.setSelectedIndex(-1); //No selected item
            currentItem = comboBox.getSelectedItem();
            currentIndex = comboBox.getSelectedIndex();
        }

        public void actionPerformed(ActionEvent e) {
            String tempItemName = comboBox.getSelectedItem().toString();

            int i = comboBox.getSelectedIndex();

            if (SEPARATOR.equals(tempItemName)) {
                if (currentIndex > i) {
                    //System.out.println("I'm here!");
                    comboBox.setSelectedIndex(i - 1);
                    currentItem = comboBox.getItemAt(i - 1);
                    currentIndex = i - 1;
                } else {
                    //System.out.println("I'm not, because i = " + i + "and currentIndex = " + currentIndex);
                    comboBox.setSelectedIndex(i + 1);
                    currentItem = comboBox.getItemAt(i + 1);
                    currentIndex = i + 1;
                }
                //System.out.println(tempItemName);
                //System.out.println((String) (((AbstractAction) currentItem).getValue(AbstractAction.NAME)));
            } else {
                currentItem = comboBox.getItemAt(i);
            }
        }

    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return null;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return false;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "MainViewer";
    }
}
