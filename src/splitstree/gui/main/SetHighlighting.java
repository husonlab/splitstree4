/**
 * SetHighlighting.java 
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
package splitstree.gui.main;

import jloda.gui.WindowListenerAdapter;
import jloda.gui.commands.CommandManager;
import jloda.gui.director.IDirectableViewer;
import jloda.util.ResourceManager;
import splitstree.core.Document;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.main.SplitsTreeProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

/**
 * User: aschmidt
 * Date: 24.01.2007
 */
public class SetHighlighting implements IDirectableViewer {

    java.util.List allActions = new LinkedList();
    private boolean uptodate = true;
    private JFrame frame;
    private Director dir;
    private Document doc;
    private SetHighlighting setHighlighting;

    //constructor

    public SetHighlighting(Director dir) {

        setHighlighting = this;
        this.dir = dir;
        doc = dir.getDocument();

        frame = new JFrame();
        setTitle(dir);
        frame.setJMenuBar(setupMenuBar());
        frame.setSize(380, 400);
        dir.setViewerLocation(this);

        // make sure we remove this viewer and listener when it is closed
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                setHighlighting.dir.removeViewer(setHighlighting);
                frame.dispose();
            }
        });
        try {
            this.MakeSetHighlightingViewer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        unlockUserInput();
        //frame.show();

        frame.setVisible(true);
    }

    private void MakeSetHighlightingViewer() {

    }


    /**
     * setup the menu bar
     */
    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));
        menu.add(getCloseAction());
        menuBar.add(menu);
        return menuBar;
    }

    /**
     * All the Actions of the window
     */

    private AbstractAction closeAction;

    private AbstractAction getCloseAction() {

        AbstractAction action = closeAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(setHighlighting);
                frame.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        allActions.add(action);
        return closeAction = action;
    }


    public boolean isUptoDate() {
        return uptodate;
    }

    public JFrame getFrame() {
        return frame;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public void updateView(String what) {

        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        setUptoDate(false);
        lockUserInput();

        //frame.show();
        frame.setVisible(true);

        unlockUserInput();
        // Set up to date
        this.uptodate = true;

    }

    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Set Highlighting - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Set Highlighting  - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null &&
                    (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(false);
        }
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null && (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(true);
        }
    }

    public void destroyView() {

    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
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
}
