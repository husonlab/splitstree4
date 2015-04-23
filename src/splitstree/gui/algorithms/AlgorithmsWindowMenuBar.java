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

package splitstree.gui.algorithms;

import jloda.util.ResourceManager;
import splitstree.gui.Director;

import javax.swing.*;

/**
 * Allows user to configure all algorithms used in computation of graph
 *
 * @author huson
 *         Date: 04-Dec-2003
 */
public class AlgorithmsWindowMenuBar extends JMenuBar {
    private AlgorithmsWindow viewer;
    private Director dir;

    public AlgorithmsWindowMenuBar(AlgorithmsWindow viewer, Director dir) {
        super();

        this.viewer = viewer;
        this.dir = dir;

        addFileMenu();
        addEditMenu();
    }

    /**
     * returns the tool bar for this simple viewer
     */
    private void addFileMenu() {
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));

        menu.add(viewer.getActions().getClose());
        menu.addSeparator();
        menu.add(dir.getActions().getQuit());
        add(menu);
    }

    private void addEditMenu() {
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Edit", 'E'));

        menu.add(viewer.getActions().getUndo());
        menu.addSeparator();
        JMenuItem menuItem = new JMenuItem(dir.getActions().getCut());
        menuItem.setText("Cut");
        menuItem.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Cut16.gif"));

        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getCopy());
        menuItem.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Copy16.gif"));

        menuItem.setText("Copy");
        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getPaste());
        menuItem.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Paste16.gif"));
        menuItem.setText("Paste");
        menu.add(menuItem);
        menu.addSeparator();
        menuItem = new JMenuItem(dir.getActions().getSelectAll());
        menuItem.setText("Select All");
        menuItem.setIcon(ResourceManager.getIcon("SelectAllText16.gif"));
        menu.add(menuItem);

        add(menu);
    }
}
