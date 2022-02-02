/*
 * ConfiguratorMenuBar.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.nodeEdge;

import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;

import javax.swing.*;


public class ConfiguratorMenuBar extends JMenuBar {


	private final Configurator conf;
	private final Director dir;

	public ConfiguratorMenuBar(Configurator conf, Director dir) {
		super();

		this.conf = conf;
		this.dir = dir;

		addFileMenu();
		addEditMenu();
	}

    /**
     * returns the tool bar for this simple viewer
     */
    private void addFileMenu() {
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));

        // viewer version opens new browser, dir version doesn't
        // menu.add(viewer.getActions().getOpenFile());

        menu.addSeparator();

        menu.add(conf.getActions().getClose());
        menu.addSeparator();
        menu.add(dir.getActions().getQuit());
        add(menu);
    }

    private void addEditMenu() {
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Edit", 'E'));

        menu.add(((MainViewer) dir.getViewerByClass(MainViewer.class)).getActions().getUndo());
        menu.add(((MainViewer) dir.getViewerByClass(MainViewer.class)).getActions().getRedo());

        menu.addSeparator();
        JMenuItem menuItem = new JMenuItem(dir.getActions().getCut());
        menuItem.setText("Cut");
        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getCopy());
        menuItem.setText("Copy");
        menu.add(menuItem);
        menuItem = new JMenuItem(dir.getActions().getPaste());
        menuItem.setText("Paste");
        menu.add(menuItem);
        menu.addSeparator();
        menuItem = new JMenuItem(dir.getActions().getSelectAll());
        menuItem.setText("Select All");
        menu.add(menuItem);
        add(menu);
    }
}



