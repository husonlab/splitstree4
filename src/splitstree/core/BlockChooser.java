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

package splitstree.core;

import jloda.util.ProgramProperties;
import splitstree.main.SplitsTreeProperties;
import splitstree.nexus.Assumptions;

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
