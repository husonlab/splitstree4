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

package splitstree.gui.confidence;

import splitstree.gui.Director;
import splitstree.gui.main.MainViewer;

import javax.swing.*;

/**
 * the confidence window menu bar
 *
 * @author huson
 *         Date: 17.2.04
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

