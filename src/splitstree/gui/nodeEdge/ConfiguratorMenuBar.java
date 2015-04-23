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

package splitstree.gui.nodeEdge;

/**
 * @author Markus
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

import splitstree.gui.Director;
import splitstree.gui.main.MainViewer;

import javax.swing.*;


public class ConfiguratorMenuBar extends JMenuBar {


    private Configurator conf;
    private Director dir;

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



