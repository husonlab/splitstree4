/**
 * MainViewerToolBar.java 
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
/*
 * Created on 05.02.2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package splitstree.gui.main;

import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;
import splitstree.gui.Director;
import splitstree.main.SplitsTreeProperties;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Markus  Franz
 */
public class MainViewerToolBar extends JToolBar {

    List actions = new LinkedList();
    MainViewer viewer;

    public MainViewerToolBar(MainViewer mainViewer, Director dir) {
        super();
        this.viewer = mainViewer;
        actions.addAll(dir.getActions().getAll());
        actions.addAll(mainViewer.getActions().getAll());

        this.setRollover(false);
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setFloatable(false);
        this.setVisible(ProgramProperties.get(SplitsTreeProperties.SHOWTOOLBAR, false));

        configureToolBar();
        //this.add(dir.getActions().getClose());
    }


    /**
     * add a button to the tool bar
     *
     * @param a
     * @return a button in the tool bar
     */
    public JButton add(Action a) {
        JButton button = super.add(a);
        if (a.getValue(AbstractAction.SMALL_ICON) != null)
            button.setText(null);
        // if we dont' have an icon, then keep the text
        return button;
    }


    /**
     * Sets up the SplitsTree toolbar
     */
    private void configureToolBar() {
        final MainViewerToolBar tb = this;

        SplitsTreeProperties.addPropertiesListListener(new PropertiesListListener() {
            public boolean isInterested(String name) {
                return name.equals(SplitsTreeProperties.TOOLBARITEMS);
            }

            public void hasChanged(List toolbarItems) {
                if (!viewer.getDir().isInUpdate()) // if in update, don't change
                {
                    tb.removeAll();
                    for (Object toolbarItem : toolbarItems) {
                        String actionName = (String) toolbarItem;

                        for (Object action : actions) {
                            Action element = (Action) action;
                            if (element.getValue(Action.NAME) != null && element.getValue(Action.NAME).equals(actionName)) {
                                tb.add(element);
                                break;
                            }
                        }
                    }
                    tb.repaint();
                }
            }
        });
        SplitsTreeProperties.addPropertiesListListener(new PropertiesListListener() {
            public boolean isInterested(String name) {
                return name.equals(SplitsTreeProperties.SHOWTOOLBAR);
            }

            public void hasChanged(List ShowToolbar) { // one item, true or false
                if (!viewer.getDir().isInUpdate()) // if in update, don't change
                {
                    String str = (String) ShowToolbar.get(0);
                    tb.setVisible(Boolean.valueOf(str).booleanValue());
                }
            }
        });
    }
}
