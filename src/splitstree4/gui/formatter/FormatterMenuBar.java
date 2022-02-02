/*
 * FormatterMenuBar.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.formatter;

import jloda.swing.director.IDirector;
import jloda.swing.graphview.GraphView;
import jloda.swing.window.MenuMnemonics;
import jloda.util.ProgramProperties;
import splitstree4.gui.main.MainViewer;

import javax.swing.*;


/**
 * menubar for node/edge configurator
 */
public class FormatterMenuBar extends JMenuBar {
    private final Formatter conf;
	private IDirector dir;
    private GraphView viewer;

    /**
     * construtor
     *
	 */
    public FormatterMenuBar(Formatter conf, IDirector dir, GraphView viewer) {
        super();

        this.conf = conf;
        this.dir = dir;
        this.viewer = viewer;

        addFileMenu();
        addEditMenu();
        addOptionsMenu();
    }

    /**
     * returns the tool bar for this simple viewer
     */
    private void addFileMenu() {
        JMenu menu = new JMenu("File");

        // viewer version opens new browser, dir version doesn't
        // menu.add(viewer.getActions().getOpenFile());

        //menu.addSeparator();

        menu.add(conf.getActions().getClose());
        if (!ProgramProperties.isMacOS()) {
            menu.addSeparator();
            menu.add(dir.getMainViewer().getQuit());
        }
        MenuMnemonics.setMnemonics(menu);
        add(menu);
    }

    private void addEditMenu() {
        JMenu menu = new JMenu("Edit");

        //menu.addSeparator();
        JMenuItem menuItem = new JMenuItem(conf.getActions().getCut());
        menuItem.setText("Cut");
        menu.add(menuItem);
        menuItem = new JMenuItem(conf.getActions().getCopy());
        menuItem.setText("Copy");
        menu.add(menuItem);
        menuItem = new JMenuItem(conf.getActions().getPaste());
        menuItem.setText("Paste");
        menu.add(menuItem);
        menu.addSeparator();
        //menuItem = new JMenuItem(viewer.getActions().getSelectAll());
        //menuItem.setText("Select All");
        menu.add(menuItem);
        MenuMnemonics.setMnemonics(menu);
        add(menu);
    }

    private void addOptionsMenu() {
        JMenu menu = new JMenu("Options");
        menu.add(conf.getActions().getSaveDefaultFont());
        MenuMnemonics.setMnemonics(menu);
        add(menu);
    }

    public void setViewer(IDirector dir, MainViewer viewer) {
        this.dir = dir;
        this.viewer = viewer;
    }
}



