/**
 * DirectorUtilities.java 
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
