/**
 * ConfidenceNetworkActions.java 
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
package splitstree.gui.bootstrap;

import jloda.util.Alert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

/**
 * all actions for confidence network ConfidenceNetworkDialog
 * Daniel Huson and David Bryant
 */
public class ConfidenceNetworkActions {
    ConfidenceNetworkDialog ConfidenceNetworkDialog;
    List all;

    public ConfidenceNetworkActions(ConfidenceNetworkDialog dialog) {
        this.ConfidenceNetworkDialog = dialog;
        this.all = new LinkedList();
    }

    AbstractAction cancelAction;

    AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ConfidenceNetworkDialog.closeDialog();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        all.add(action);
        cancelAction = action;
        return action;
    }

    AbstractAction applyAction;

    AbstractAction getApplyAction() {
        AbstractAction action = applyAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ConfidenceNetworkDialog.closeDialog();
                boolean levelOK = true;

                try {
                    ConfidenceNetworkDialog.levelField.commitEdit();
                } catch (ParseException ex) {
                    System.err.println("Problems with commitEdit()");
                    levelOK = false;
                }
                if (levelOK) {
                    try {
                        ConfidenceNetworkDialog.levelPercent = ((Number) ConfidenceNetworkDialog.levelField.getValue()).intValue();
                    } catch (Exception ex) {
                        levelOK = false;
                    }
                }
                if (!levelOK || ConfidenceNetworkDialog.levelPercent < 0 || ConfidenceNetworkDialog.levelPercent > 100)
                {
                    new Alert("The number of bootstrap replicates needs to be an integer percentage");
                } else {
                    ConfidenceNetworkDialog.execute = true;
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        all.add(action);
        applyAction = action;
        return action;
    }
}
