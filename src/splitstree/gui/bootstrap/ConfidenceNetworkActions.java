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
