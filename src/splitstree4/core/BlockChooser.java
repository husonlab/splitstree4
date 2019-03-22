/**
 * BlockChooser.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.core;

import jloda.swing.util.ProgramProperties;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Assumptions;

import javax.swing.*;
import java.awt.*;

/**
 * DESCRIPTION
 *
 * @author huson
 *         Date: 13-Sep-2004
 */
public class BlockChooser {
    /**
     * if input contains more than one data block and does not contain an "uptodate"
     * statement in the assumptions blocks, presents choice of data block to user
     * and then deletes all other input blocks
     *
     * @param doc
     * @return false, if user canceled
     */
    public static boolean show(Component parent, Document doc) {
        if (doc == null)
            return true;
        Assumptions assumptions = doc.getAssumptions();
        if (assumptions == null || assumptions.isUptodate())
            return true;

        Object[] inputValues = doc.getListOfValidInputBlocks().toArray();

        if (inputValues.length <= 1)
            return true;

        Object result = JOptionPane.showInputDialog(parent,
                "Input contains a number of different Nexus data blocks.\n" +
                        "Please choose one type of data as input,\n" +
                        "all other blocks will be discarded:",
                "Choose input data - " + SplitsTreeProperties.getVersion(), JOptionPane.WARNING_MESSAGE,
                ProgramProperties.getProgramIcon(), inputValues, inputValues[0]);

        if (result == null)
            return false;

        doc.keepOnlyThisInputBlock((String) result);
        return true;
    }
}
