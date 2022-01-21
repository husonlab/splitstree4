/*
 * ConfidenceWindowMenuBar.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.confidence;

import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;

import javax.swing.*;

/**
 * the confidence window menu bar
 *
 * @author huson
 * Date: 17.2.04
 */
public class ConfidenceWindowMenuBar extends JMenuBar {
    private ConfidenceWindow viewer;
    private Director dir;
    private MainViewer mainViewer;

    public ConfidenceWindowMenuBar(ConfidenceWindow viewer, Director dir) {
        super();

        this.viewer = viewer;
        this.dir = dir;
        this.mainViewer = (MainViewer) dir.getViewerByClass(MainViewer.class);

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

        menu.add(mainViewer.getActions().getUndo());
        menu.add(mainViewer.getActions().getRedo());

        add(menu);
    }
}

