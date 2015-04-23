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

package splitstree.gui;

import jloda.gui.AppleStuff;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import splitstree.core.Document;
import splitstree.gui.main.MainViewer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * some utilities for the director
 *
 * @author huson
 *         Date: 07-Mar-2004
 */
public class DirectorUtilities {
    /**
     * adds the windows menu
     */

    public static JMenu makeWindowMenu(Director dir, MainViewer viewer) {
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("Window", 'W'));

        if (ProgramProperties.isMacOS()) {
            if (!AppleStuff.getInstance().isAboutDefined())
            AppleStuff.getInstance().setAboutAction(dir.getActions().getAboutWindow());
        } else {
            menu.add(dir.getActions().getAboutWindow());
            menu.addSeparator();
        }

        menu.add(dir.getActions().getHowToCite());
        menu.addSeparator();
        menu.add(viewer.getActions().getWindowSize());

        menu.addSeparator();

        JMenu subMenu = new JMenu("Nexus Syntax");
        subMenu.setMnemonic('N');
        subMenu.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Help16.gif"));

        Iterator it = Document.getListOfBlockNames().iterator();
        ArrayList<Integer> mnemonics = new ArrayList<>();

        while (it.hasNext()) {
            String name = (String) it.next();
            Integer mnemonic = null;
            for (int i = 0; i < name.length(); i++) {

                if (Character.isLetter(name.charAt(i))) {
                    Integer cMnemonic = (int) Character.toUpperCase(name.charAt(i));
                    if (!mnemonics.contains(cMnemonic)) {
                        mnemonic = cMnemonic;
                        mnemonics.add(cMnemonic);
                        break;
                    }
                }
            }
            subMenu.add(dir.getActions().getSyntaxAction(name, mnemonic));
        }
        menu.add(subMenu);

        menu.add(dir.getActions().getCommandHelp());

        menu.addSeparator();

        menu.add(dir.getActions().getRunCommand());
        menu.add(dir.getActions().getMessageWindow());

        menu.addSeparator();
        menu.add(viewer.getActions().getNodeEdgeFormatterAction());
        menu.add(viewer.getActions().getFindReplaceAction());

        return menu;
    }
}
