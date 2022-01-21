/*
 * AlgorithmsWindowMenuBar.java Copyright (C) 2022 Daniel H. Huson
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
import splitstree4.gui.Director;

import javax.swing.*;

/**
 * Allows user to configure all algorithms used in computation of graph
 *
 * @author huson
 * Date: 04-Dec-2003
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
        menuItem.setIcon(ResourceManager.getIcon("sun/Cut16.gif"));

        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getCopy());
        menuItem.setIcon(ResourceManager.getIcon("sun/Copy16.gif"));

        menuItem.setText("Copy");
        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getPaste());
        menuItem.setIcon(ResourceManager.getIcon("sun/Paste16.gif"));
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
