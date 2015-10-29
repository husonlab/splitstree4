/**
 * SearchActions.java
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
package splitstree4.gui.search;

/**
 * Find and Replace actions.
 * These actions are attached to buttons, checkboxes, RadioButtons.
 *
 * @author Miguel Jett? and David Bryant
 * @since Aug 25th, 2004
 * New revision: January 31, 2006
 *  -- Simplified actions using SearchManagers.
 * New revision: April 14th, 2005
 *  -- Regular expressions were added to the find and replace actions.
 */

import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.gui.main.MainViewer;
import splitstree4.util.EnableDisable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;


public class SearchActions {
    private Director dir;
    final private List<Action> all;
    final private SearchWindow searchWindow;

    SearchActions(SearchWindow searchWindow, Director dir) {
        this.searchWindow = searchWindow;
        this.dir = dir;
        all = new LinkedList<>();
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
        MainViewer mainViewer = (MainViewer) dir.getMainViewer();

        for (Action anAll : all) {
            AbstractAction action = (AbstractAction) anAll;
            // if any of the OK_WITH values are set, one of the tabs must be selected:
            boolean ok = true;
            action.setEnabled(ok);
            // new actions carry an EnableDisable class to decide whether to enable
            EnableDisable ed = (EnableDisable) action.getValue(EnableDisable.ENABLEDISABLE);
            if (ed != null)
                action.setEnabled(ed.enable());
        }
    }

    /**
     * Determines the viewer that is foremost in the screen and returns
     * an appropriate find/replace manager. At the moment, this only
     * works for the MainViewer and the two front tabs HOWEVER, find and replace
     * should be linked to the application and apply to whichever window, message/viewer
     * is in the front.
     * <p/>
     * However, at this stage, there is one find and replace per director.
     * //TODO: implement search managers for the message window as well.
     *
     * @return SearchManager
     */
    private SearchManager getActiveSearchManager() {
        MainViewer viewer = (MainViewer) dir.getMainViewer();
        if (viewer == null)
            return null;
        else
            return new GraphSearchManager(viewer);
    }

    private AbstractAction findNextAction;

    /**
     * Returns an action that executes a find next in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getFindNextAction() {
        AbstractAction action = findNextAction;

        if (action != null)
            return findNextAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();
                if ((searchManager != null) && (options != null) && (searchText != null)
                        && (searchText.length() != 0)) {
                    boolean found = searchManager.next(searchText, options);
                    if (found)
                        searchWindow.setMessage("");
                    else
                        searchWindow.setMessage("Not found");

                }
            }
        };
        action.putValue(AbstractAction.NAME, "Next");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Find next");
        all.add(action);
        return findNextAction = action;
    }


    private AbstractAction findPrevAction;

    /**
     * Returns an action that executes a find previous in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getFindPrevAction() {
        AbstractAction action = findPrevAction;

        if (action != null)
            return findPrevAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();
                if ((searchManager != null) && (options != null) && (searchText != null) && (searchText.length() != 0)) {
                    boolean found = searchManager.previous(searchText, options);
                    if (found)
                        searchWindow.setMessage("");
                    else
                        searchWindow.setMessage("Not found");
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Previous");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Find previous");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return findPrevAction = action;
    }


    private AbstractAction replaceAction;

    /**
     * Returns an action that executes a replace in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getReplaceAction() {
        AbstractAction action = replaceAction;

        if (action != null)
            return replaceAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();
                String replaceText = searchWindow.getReplaceText();

                if ((searchManager != null) && (options != null) && (searchText != null) && (replaceText != null) && (searchText.length() != 0))
                    searchManager.replace(searchText, replaceText, options);
                searchWindow.setMessage("");
            }
        };
        action.putValue(AbstractAction.NAME, "Replace");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Replace current selection");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return replaceAction = action;
    }


    private AbstractAction replaceFindAction;

    /**
     * Returns an action that executes a replace and then find in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getReplaceFindAction() {
        AbstractAction action = replaceFindAction;

        if (action != null)
            return replaceFindAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();
                String replaceText = searchWindow.getReplaceText();

                if ((searchManager != null) && (options != null) && (searchText != null) && (searchText.length() != 0) && (replaceText != null)) {
                    searchManager.replace(searchText, replaceText, options);
                    boolean found = searchManager.next(searchText, options);
                    if (found)
                        searchWindow.setMessage("");
                    else
                        searchWindow.setMessage("Not found");
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Replace and Find");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Replace current selection then search for next");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return replaceFindAction = action;
    }


    private AbstractAction replaceAllAction;

    /**
     * Returns an action that executes a find next in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getReplaceAllAction() {
        AbstractAction action = replaceAllAction;

        if (action != null)
            return replaceAllAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();
                String replaceText = searchWindow.getReplaceText();

                if ((searchManager != null) && (options != null) && (searchText != null) && (searchText.length() != 0) && (replaceText != null)) {
                    int count = searchManager.replaceAll(searchText, replaceText, options);
                    searchWindow.setMessage("" + count + " replaced");
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Replace All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Replaces all instances of text found with replacement text");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return replaceAllAction = action;
    }


    private AbstractAction findAllAction;

    /**
     * Returns an action that executes a find next in the active tab of the main viewer
     * Will do nothing if an error is encountered.
     *
     * @return Action
     */
    public AbstractAction getFindAllAction() {
        AbstractAction action = findAllAction;

        if (action != null)
            return findAllAction;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SearchManager searchManager = getActiveSearchManager();
                SearchOptions options = searchWindow.getSearchOptions();
                String searchText = searchWindow.getSearchText();

                if ((searchManager != null) && (options != null) && (searchText != null) && (searchText.length() != 0))
                    searchManager.findAll(searchText, options);
                searchWindow.setMessage("");
            }
        };
        action.putValue(AbstractAction.NAME, "Find All");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Finds all instances of the search text");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return findAllAction = action;
    }

    public void setDirector(Director dir) {
        this.dir = dir;
    }
}
