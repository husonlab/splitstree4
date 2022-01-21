/*
 * AlgorithmsWindowActions.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.algorithms;

import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import splitstree4.algorithms.Transformation;
import splitstree4.algorithms.characters.CharactersTransform;
import splitstree4.algorithms.distances.DistancesTransform;
import splitstree4.algorithms.quartets.QuartetsTransform;
import splitstree4.algorithms.reticulate.ReticulateTransform;
import splitstree4.algorithms.splits.SplitsTransform;
import splitstree4.algorithms.trees.TreesTransform;
import splitstree4.algorithms.unaligned.UnalignedTransform;
import splitstree4.algorithms.util.Configurator;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.UpdateableActions;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;
import splitstree4.util.PluginClassLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * Allows user to configure all algorithms used in computation of graph
 *
 * @author huson
 * Date: 04-Dec-2003
 */
public class
AlgorithmsWindowActions implements UpdateableActions {
    private static String OPTIONS_PANEL = "OPTIONS_PANEL";
    private static String OPTION_ACTIONS = "OPTION_ACTIONS";

    private AlgorithmsWindow viewer;
    private Director dir;
    private List all = new LinkedList();

    public AlgorithmsWindowActions(AlgorithmsWindow viewer, Director dir) {
        this.viewer = viewer;
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

        // update unaligned tab
        List actions = getUnalignedTransformActions();
        Document doc = dir.getDocument();
        if (doc.isValidByName(Unaligned.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getUnalignedTransformName())) {
                    viewer.getComboBox(Unaligned.NAME).setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform,
                                doc.getAssumptions().getUnalignedTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                    break;
                }
            }
        }

        // update characters tab
        actions = getCharactersTransformActions();
        if (doc.isValidByName(Characters.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getCharactersTransformName())) {
                    viewer.getComboBox(Characters.NAME).setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform, doc.getAssumptions().getCharactersTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                    break;
                }
            }
        }

        // update distances tab
        actions = getDistancesTransformActions();
        if (doc.isValidByName(Distances.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getDistancesTransformName())) {
                    JComboBox cbox = viewer.getComboBox(Distances.NAME);
                    cbox.setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform, doc.getAssumptions().getDistancesTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                        System.err.println("Transform: " + doc.getAssumptions().getDistancesTransformName());
                        System.err.println("TransformParam: " + doc.getAssumptions().getDistancesTransformParam());
                    }
                    break;
                }
            }
        }

        // update quartets tab
        actions = getQuartetsTransformActions();
        if (doc.isValidByName(Quartets.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getQuartetsTransformName())) {
                    viewer.getComboBox(Quartets.NAME).setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform,
                                doc.getAssumptions().getQuartetsTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                    break;
                }
            }
        }

        // update trees tab
        actions = getTreesTransformActions();
        if (doc.isValidByName(Trees.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getTreesTransformName())) {
                    viewer.getComboBox(Trees.NAME).setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform,
                                doc.getAssumptions().getTreesTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                    break;
                }
            }
        }

        // update splits tab

        actions = getSplitsTransformActions();
        if (doc.isValidByName(Splits.NAME) && doc.isValidByName(Assumptions.NAME)) {
            for (Object action1 : actions) {
                AbstractAction action = (AbstractAction) action1;
                String name = (String) action.getValue(AbstractAction.NAME);
                if (name.equals(doc.getAssumptions().getSplitsTransformName())) {
                    viewer.getComboBox(Splits.NAME).setSelectedItem(action);
                    Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                    try {
                        Configurator.setOptions(transform,
                                doc.getAssumptions().getSplitsTransformParam());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                    break;
                }
            }
        }

        // update reticulate tab
        if (SplitsTreeProperties.ALLOW_RETICULATE) {
            actions = getReticulateTransformActions();
            if (doc.isValidByName(Reticulate.NAME) && doc.isValidByName(Assumptions.NAME)) {
                for (Object action1 : actions) {
                    AbstractAction action = (AbstractAction) action1;
                    String name = (String) action.getValue(AbstractAction.NAME);
                    if (name.equals(doc.getAssumptions().getReticulateTransformName())) {
                        viewer.getComboBox(Reticulate.NAME).setSelectedItem(action);
                        Transformation transform = (Transformation) action.getValue(DirectorActions.TRANSFORM);
                        try {
                            Configurator.setOptions(transform,
                                    doc.getAssumptions().getReticulateTransformParam());
                        } catch (Exception ex) {
                            Basic.caught(ex);
                        }
                        break;
                    }
                }
            }
        }
        AlgorithmsTab.syncronizeTransform2Tab(doc, getAll());
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

    private AbstractAction close;

    /**
     * close this viewer
     *
     * @return close action
     */
    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.removeViewer(viewer);
                viewer.getFrame().setVisible(false);
                viewer.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(AbstractAction.MNEMONIC_KEY, (int) ('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this viewer");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        // close is critical because we can't easily kill the worker thread

        all.add(action);
        return close = action;
    }


    private AbstractAction undo;

    /**
     * undo action
     */
    public AbstractAction getUndo() {
        AbstractAction action = undo;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                System.err.println("Not implemented");
            }
        };
        action.putValue(AbstractAction.NAME, "Undo");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        // quit.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("quit"));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Undo");

        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Undo16.gif"));

        all.add(action);
        return undo = action;

    }

    private AbstractAction applyUnalignedTransform;

    /**
     * gets the apply unaligned action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplyUnalignedTransform(final JComboBox cbox) {
        if (applyUnalignedTransform != null)
            return applyUnalignedTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                String command = Configurator.getOptions(transform);
                SplitsTreeProperties.addRecentMethod(Unaligned.NAME,
                        transform.getClass().getName());

                dir.execute("assume unalignTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply unalign transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyUnalignedTransform = action;
    }


    private AbstractAction applyCharactersTransform;

    /**
     * apply the set characters transform?
     *
     * @param cbox the combobox
     */
    public AbstractAction getApplyCharactersTransform(final JComboBox cbox) {
        if (applyCharactersTransform != null)
            return applyCharactersTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation) selectedAction.getValue(DirectorActions.TRANSFORM);
                String command = Configurator.getOptions(transform);
                SplitsTreeProperties.addRecentMethod(Characters.NAME, transform.getClass().getName());
                dir.execute("assume charTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply characters transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyCharactersTransform = action;
    }


    private AbstractAction applyDistancesTransform;

    /**
     * gets the apply distances action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplyDistancesTransform(final JComboBox cbox) {
        if (applyDistancesTransform != null)
            return applyDistancesTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                SplitsTreeProperties.addRecentMethod(Distances.NAME,
                        transform.getClass().getName());
                String command = Configurator.getOptions(transform);
                dir.execute("assume distTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply distances transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyDistancesTransform = action;
    }

    private AbstractAction applyQuartetsTransform;

    /**
     * gets the apply quartet action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplyQuartetsTransform(final JComboBox cbox) {
        if (applyQuartetsTransform != null)
            return applyQuartetsTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                SplitsTreeProperties.addRecentMethod(Quartets.NAME,
                        transform.getClass().getName());
                String command = Configurator.getOptions(transform);
                dir.execute("assume quartTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply quartets transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyQuartetsTransform = action;
    }

    private AbstractAction applyTreesTransform;

    /**
     * gets the apply trees action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplyTreesTransform(final JComboBox cbox) {
        if (applyTreesTransform != null)
            return applyTreesTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                SplitsTreeProperties.addRecentMethod(Trees.NAME,
                        transform.getClass().getName());
                String command = Configurator.getOptions(transform);
                dir.execute("assume treesTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply trees transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyTreesTransform = action;
    }


    private AbstractAction applySplitsTransform;

    /**
     * gets the apply splits action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplySplitsTransform(final JComboBox cbox) {
        if (applySplitsTransform != null)
            return applySplitsTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {

                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                SplitsTreeProperties.addRecentMethod(Splits.NAME,
                        transform.getClass().getName());
                String command = Configurator.getOptions(transform);
                dir.execute("assume splitsTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply splits transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applySplitsTransform = action;
    }

    private AbstractAction applyReticulateTransform;

    /**
     * gets the apply reticulate action
     *
     * @param cbox
     * @return apply action
     */
    public AbstractAction getApplyReticulateTransform(final JComboBox cbox) {
        if (applyReticulateTransform != null)
            return applyReticulateTransform;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                AbstractAction selectedAction = (AbstractAction) cbox.getSelectedItem();
                performActions(event, (List) selectedAction.getValue(OPTION_ACTIONS));
                Transformation transform = (Transformation)
                        selectedAction.getValue(DirectorActions.TRANSFORM);
                SplitsTreeProperties.addRecentMethod(Reticulate.NAME,
                        transform.getClass().getName());
                String command = Configurator.getOptions(transform);
                dir.execute("assume reticulateTransform=" + command);
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply reticulate transform");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return applyReticulateTransform = action;
    }


    /**
     * fires all actions in the given list. Used to capture all option settings when
     * apply is pressed
     *
     * @param event
     * @param list
     */
    private void performActions(ActionEvent event, List list) {
        if (list != null) {
            for (Object aList : list) {
                AbstractAction action = (AbstractAction) aList;
                action.actionPerformed(event);
            }
        }
    }

    /**
     * returns the action that should be associated with a transformer combobox
     *
     * @param dir         the director
     * @param tab         the tab containing the cbox
     * @param applyAction the action associated with the corresponding apply button
     * @return comboxbox action
     */
    AbstractAction getComboBoxAction(final Director dir, final AlgorithmsTab tab,
                                     final AbstractAction applyAction) {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JComboBox cb = (JComboBox) event.getSource();

                //Handling Separator Actions
                int i = 0;
                AbstractAction selectedAction = null;
                i = cb.getSelectedIndex();
                if (cb.getItemAt(i) instanceof AbstractAction) {
                    //Normal way to handle actions
                    selectedAction = (AbstractAction) cb.getSelectedItem();
                } else {
                    //If we have a separator we act as if we selected the one right after it
                    selectedAction = (AbstractAction) cb.getItemAt(i + 1);
                }
                //end of handling Separator Actions
                if (selectedAction == null)
                    return;

                Transformation transform = (Transformation) selectedAction.
                        getValue(DirectorActions.TRANSFORM);
                String description = (String) selectedAction.
                        getValue(AbstractAction.SHORT_DESCRIPTION);
                if (tab.getCBox() != null)
                    tab.getCBox().setToolTipText(description);
                if (dir.getDocument().isApplicable(transform))
                    applyAction.setEnabled(true);
                else
                    applyAction.setEnabled(false);

                tab.setDescriptionLabel(transform.getDescription());

                JPanel optionsPanel = (JPanel) selectedAction.getValue(OPTIONS_PANEL);
                if (optionsPanel == null) {
                    List optionActions = new LinkedList();
                    optionsPanel = AlgorithmsTab.createOptionsPanel(dir.getDocument(), transform, optionActions);
                    selectedAction.putValue(OPTION_ACTIONS, optionActions);
                    all.addAll(optionActions);
                    selectedAction.putValue(OPTIONS_PANEL, optionsPanel);
                }
                if (optionsPanel != tab.getOptionsPanel())
                    tab.setOptionsPanel(optionsPanel);

                AlgorithmsTab.syncronizeTransform2Tab(dir.getDocument(), (List) selectedAction.getValue(OPTION_ACTIONS));
            }
        };
    }

    // unaligned transformations:
    private List unalignedTransformActions;

    /**
     * gets the action associated with a specific unaligneds transformation
     *
     * @param clazz
     * @return actions
     */

    public AbstractAction getUnalignedTransformAction(Class clazz) {
        return getTransformAction(clazz, getUnalignedTransformActions());
    }

    /**
     * get the list of all unaligned transformers
     *
     * @return all unaligned transformer actions
     */
    public List getUnalignedTransformActions() {
        List actions = unalignedTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.unaligned", UnalignedTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume unalignTransform=" + command);
                }
            };
            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Unaligned.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return unalignedTransformActions = actions;
    }

    // character transformations:
    private List charactersTransformActions;

    /**
     * gets the action associated with a specific characters transformation
     *
     * @param clazz
     * @return actions
     */

    public AbstractAction getCharactersTransformAction(Class clazz) {
        return getTransformAction(clazz, getCharactersTransformActions());
    }

    /**
     * get the list of all character transformers
     *
     * @return all character transformer actions
     */
    public List getCharactersTransformActions() {
        List actions = charactersTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.characters", CharactersTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;

            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume charTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return charactersTransformActions = actions;
    }

    // distance transformations:
    private List distancesTransformActions;

    /**
     * gets the action associated with a specific distances transformation
     *
     * @param clazz
     * @return actions
     */

    public AbstractAction getDistancesTransformAction(Class clazz) {
        return getTransformAction(clazz, getDistancesTransformActions());
    }

    /**
     * get the list of all distance transformers
     *
     * @return all distance transformer actions
     */
    public List getDistancesTransformActions() {
        List actions = distancesTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.distances", DistancesTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume distTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Distances.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return distancesTransformActions = actions;
    }


    // quartet transformations:
    private List quartetsTransformActions;

    /**
     * gets the action associated with a specific quartets transformation
     *
     * @param clazz
     * @return action
     */

    public AbstractAction getQuartetsTransformAction(Class clazz) {
        return getTransformAction(clazz, getQuartetsTransformActions());
    }

    /**
     * get the list of all quartet transformers
     *
     * @return all quartet transformer actions
     */
    public List getQuartetsTransformActions() {
        List actions = quartetsTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.quartets", QuartetsTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume quartTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Quartets.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return quartetsTransformActions = actions;
    }

    // tree transformations:
    private List treesTransformActions;

    /**
     * gets the action associated with a specific trees transformation
     *
     * @param clazz
     * @return action
     */

    public AbstractAction getTreesTransformAction(Class clazz) {
        return getTransformAction(clazz, getTreesTransformActions());
    }

    /**
     * get the list of all tree transformers
     *
     * @return all tree transformer actions
     */
    public List getTreesTransformActions() {
        List actions = treesTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.trees", TreesTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);
            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume treeTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Trees.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return treesTransformActions = actions;
    }

    // split transformations:
    private List splitsTransformActions;

    /**
     * gets the action associated with a specific splits transformation
     *
     * @param clazz
     * @return action
     */
    public AbstractAction getSplitsTransformAction(Class clazz) {
        return getTransformAction(clazz, getSplitsTransformActions());
    }

    /**
     * get the list of all split transformers
     *
     * @return all split transformer actions
     */
    public List getSplitsTransformActions() {
        List actions = splitsTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.splits", SplitsTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = String.valueOf(transform.getClass());
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume splitsTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return splitsTransformActions = actions;
    }

    private List reticulateTransformActions;

    /**
     * gets the action associated with a specific splits transformation
     *
     * @param clazz
     * @return
     */
    public AbstractAction getReticulateTransformAction(Class clazz) {
        return getTransformAction(clazz, getReticulateTransformActions());
    }

    /**
     * get the list of all split transformers
     *
     * @return all split transformer actions
     */
    public List getReticulateTransformActions() {
        List actions = reticulateTransformActions;
        if (actions != null)
            return actions;
        actions = new LinkedList();
        List transforms = PluginClassLoader.getInstancesSorted("splitstree4.algorithms.reticulate", ReticulateTransform.class);
        for (Object transform1 : transforms) {
            final Transformation transform = (Transformation) transform1;
            String name = transform.getClass().getName();
            // if plugins dont have a package
            if (name.startsWith("class ")) name = name.substring(6);
            if (name.lastIndexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);

            AbstractAction action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    String command = Configurator.getOptions(transform);
                    dir.execute("assume reticulateTransform=" + command);
                }
            };

            action.putValue(AbstractAction.NAME, name);
            action.putValue(AbstractAction.SHORT_DESCRIPTION, transform.getDescription());
            action.putValue(DirectorActions.TRANSFORM, transform);
            action.putValue(DirectorActions.DEPENDS_ON, Reticulate.NAME);
            action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
            action.setEnabled(false);
            actions.add(action);
            all.add(action);
        }
        return reticulateTransformActions = actions;
    }


    /**
     * returns an action for the given class from a list of actions
     */
    private AbstractAction getTransformAction(Class clazz, List transformerActions) {
        System.err.println("trans.class " + clazz.getName());

        for (Object transformerAction : transformerActions) {
            AbstractAction action = (AbstractAction) transformerAction;
            Transformation trans = (Transformation) action.getValue(DirectorActions.TRANSFORM);
            if (trans.getClass().getName().equals(clazz.getName()))
                return action;

        }
        return null;
    }
}
