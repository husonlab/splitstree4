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
import java.util.Iterator;
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
