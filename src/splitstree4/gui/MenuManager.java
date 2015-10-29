/**
 * MenuManager.java
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
package splitstree4.gui;

import jloda.gui.AppleStuff;
import jloda.util.MenuMnemonics;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * class for creating and managing menus
 * Daniel Huson, 8.2006
 */
public class MenuManager {
    public final static String MENUBAR_TAG = "MenuBar";
    // action values:
    public final static String CHECKBOXMENUITEM = "CheckBox";
    public final static String ALT_NAME = "ALT_NAME"; // alternative name recognized

    private final SortedMap<String, Action> actions = new TreeMap<>();

    /**
     * constructor
     */
    public MenuManager() {
    }

    /**
     * add a collection of actions to the list of available actions
     *
     * @param actions
     */
    public void addAll(Collection<Action> actions) {
        for (Action action : actions) {
            String label = (String) action.getValue(ALT_NAME);
            if (label == null)
                label = (String) action.getValue(Action.NAME);
            if (label != null)
                this.actions.put(label, action);
        }
    }

    /**
     * add an action
     *
     * @param label
     * @param action
     */
    public void add(String label, Action action) {
        actions.put(label, action);
    }

    /**
     * remove the action corresponding to the given label
     *
     * @param label
     */
    public void remove(String label) {
        actions.keySet().remove(label);
    }

    /**
     * remove a collection of actions
     *
     * @param actions
     */
    public void removeAll(Collection<Action> actions) {
        for (Action action : actions) {
            String label = (String) action.getValue(ALT_NAME);
            if (label == null)
                label = (String) action.getValue(Action.NAME);
            if (label != null)
                remove(label);
        }
    }

    /**
     * builds a menu bar from a set of description lines.
     * Description must contain one menu bar line in the format:
     * MenuBar.menuBarLabel=item;item;item...;item, where menuBarLabel must match the
     * given name and each item is of the form Menu.menuBarLabel or simply menuBarLabel,
     * Used in Dendroscope
     *
     * @param menuBarLabel
     * @param descriptions
     * @param menuBar
     * @throws Exception
     */
    public void buildMenuBar(String menuBarLabel, Hashtable<String, String> descriptions, JMenuBar menuBar) throws Exception {
        /*
        System.err.println("Known actions:");
        for (Iterator it = actions.keySet().iterator(); it.hasNext();) {
            System.err.println(it.next());
        }
         */

        menuBarLabel = MENUBAR_TAG + "." + menuBarLabel;
        if (!descriptions.keySet().contains(menuBarLabel))
            throw new Exception("item not found: " + menuBarLabel);

        List<String> menuLabels = getTokens((String) descriptions.get(menuBarLabel));

        for (String menuLabel : menuLabels) {
            if (!menuLabel.startsWith("Menu."))
                menuLabel = "Menu." + menuLabel;

            if (descriptions.keySet().contains(menuLabel)) {
                JMenu menu = buildMenu(menuLabel, descriptions, false);
                addSubMenus(0, menu, descriptions);
                MenuMnemonics.setMnemonics(menu);
                menuBar.add(menu);
            }
        }
    }

    /**
     * builds a menu from a description.
     * Format:
     * Menu.menuLabel=name;item;item;...;item;  where  name is menu name
     * and item is either the menuLabel of an action, | to indicate a separator
     * or @menuLabel to indicate menuLabel name of a submenu
     *
     * @param menuLabel
     * @param descriptions
     * @param addEmptyIcon
     * @return menu
     * @throws Exception
     */
    private JMenu buildMenu(String menuLabel, Hashtable<String, String> descriptions, boolean addEmptyIcon) throws Exception {
        if (!menuLabel.startsWith("Menu."))
            menuLabel = "Menu." + menuLabel;
        String description = descriptions.get(menuLabel);
        if (description == null)
            return null;
        List menuDescription = getTokens(description);
        if (menuDescription.size() == 0)
            return null;
        boolean skipNextSeparator = false;  // avoid double separators
        final Iterator it = menuDescription.iterator();
        final String menuName = (String) it.next();
        final JMenu menu = new JMenu(menuName);
        if (addEmptyIcon)
            menu.setIcon(ResourceManager.getIcon("Empty16.gif"));
        final String[] labels = (String[]) menuDescription.toArray(new String[menuDescription.size()]);
        for (int i = 1; i < labels.length; i++) {
            final String label = labels[i];
            if (i == labels.length - 2 && ProgramProperties.isMacOS() && label.equals("|") && labels[i + 1].equals("Quit"))
                skipNextSeparator = true; // avoid separator at bottom of File menu in mac version

            if (skipNextSeparator && label.equals("|")) {
                skipNextSeparator = false;
                continue;
            }
            skipNextSeparator = false;

            if (label.startsWith("@")) {
                final JMenu subMenu = new JMenu(label);
                menu.add(subMenu);
            } else if (label.equals("|")) {
                menu.addSeparator();
                skipNextSeparator = true;
            } else {
                final Action action = actions.get(label);
                if (action != null) {
                    boolean done = false;
                    if (ProgramProperties.isMacOS()) {
                        switch (label) {
                            case "Quit":
                                AppleStuff.getInstance().setQuitAction(action);
                                if (menu.getItemCount() > 0 && menu.getItem(menu.getItemCount() - 1) == null) {
                                    skipNextSeparator = true;
                                }
                                done = true;
                                break;
                            case "About":
                            case "About...":
                                AppleStuff.getInstance().setAboutAction(action);
                                if (menu.getItemCount() > 0 && menu.getItem(menu.getItemCount() - 1) == null) {
                                    skipNextSeparator = true;
                                }
                                done = true;
                                break;
                            case "Preferences":
                            case "Preferences...":
                                AppleStuff.getInstance().setPreferencesAction(action);
                                if (menu.getItemCount() > 0 && menu.getItem(menu.getItemCount() - 1) == null) {
                                    skipNextSeparator = true;
                                }
                                done = true;
                                break;
                        }
                    }


                    if (!done) {
                        if (action.getValue(CHECKBOXMENUITEM) != null) {
                            JCheckBoxMenuItem cbox = (JCheckBoxMenuItem) action.getValue(CHECKBOXMENUITEM);
                            cbox.setAction(action);
                            cbox.setName((String) action.getValue(Action.NAME));
                            cbox.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
                            menu.add(cbox);
                        } else {
                            menu.add(action);
                        }
                        JMenuItem item = menu.getItem(menu.getItemCount() - 1);
                        // the following makes sure the alt-name is used, if present
                        if (action.getValue(ALT_NAME) != null)
                            item.setText((String) action.getValue(ALT_NAME));
                        else
                            item.setText((String) action.getValue(Action.NAME));
                        item.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
                    }
                    // always add empty icon, if non is given
                    if (action.getValue(AbstractAction.SMALL_ICON) == null)
                        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Empty16.gif"));
                } else {
                    menu.add(label + " #");
                    menu.getItem(menu.getItemCount() - 1).setEnabled(false);
                }
            }
        }

        return menu;
    }

    /**
     * adds submenus to a menu
     *
     * @param depth
     * @param menu
     * @param descriptions
     * @throws Exception
     */
    private void addSubMenus(int depth, JMenu menu, Hashtable<String, String> descriptions) throws Exception {
        if (depth > 5)
            throw new Exception("Submenus: too deep: " + depth);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && item.getText() != null && item.getText().startsWith("@")) {
                String name = item.getText().substring(1);
                item.setText(name);
                JMenu subMenu = buildMenu(name, descriptions, true);
                if (subMenu != null) {
                    addSubMenus(depth + 1, subMenu, descriptions);
                    menu.remove(i);
                    menu.add(subMenu, i);
                }
            }
        }
    }

    /**
     * find named menu
     *
     * @param name
     * @param menuBar
     * @param mayBeSubmenu also search for sub menu
     * @return menu or null
     */
    public static JMenu findMenu(String name, JMenuBar menuBar, boolean mayBeSubmenu) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu result = findMenu(name, menuBar.getMenu(i), mayBeSubmenu);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * searches for menu by name
     *
     * @param name
     * @param menu
     * @param mayBeSubmenu
     * @return menu or null
     */
    public static JMenu findMenu(String name, JMenu menu, boolean mayBeSubmenu) {
        if (menu.getText().equals(name))
            return menu;
        if (mayBeSubmenu) {
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null) {
                    Component comp = item.getComponent();
                    if (comp instanceof JMenu) {
                        JMenu result = findMenu(name, (JMenu) comp, true);
                        if (result != null)
                            return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * get the list of tokens in a description
     *
     * @param str
     * @return list of tokens
     * @throws Exception
     */
    static public List<String> getTokens(String str) throws Exception {
        try {
            int pos = str.indexOf("=");
            str = str.substring(pos + 1).trim();
            StringTokenizer tokenizer = new StringTokenizer(str, ";");
            List<String> result = new LinkedList<>();
            while (tokenizer.hasMoreTokens())
                result.add(tokenizer.nextToken());
            return result;
        } catch (Exception ex) {
            throw new Exception("failed to parse description-line: <" + str + ">: " + ex);
        }
    }
}

