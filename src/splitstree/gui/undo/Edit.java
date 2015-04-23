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

package splitstree.gui.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;

/**
 * Represents an undoable undo operation within the SplitsTree framework. For every
 * user action the GUI should create an appropriate undo object and execute it.
 * Upon execution, the object finds out how to undo its effect and registers
 * itself with the undo system currently in place, as represented by the
 * argument to the execute call.
 *
 * @author Olaf Delgado
 * @version $Id: Edit.java,v 1.1 2005-05-10 16:14:43 huson Exp $
 */
public class Edit extends AbstractUndoableEdit {
    // The command to perform.
    private ICommand cmd;
    // The command to perform upon undo.
    private ICommand reverse_cmd;
    // True if the undo operation has been performed.
    private boolean performed = false;
    // The presentation name for this undo.
    private String presentationName = "";

    /**
     * Constructs a Edit instance.
     *
     * @param cmd the command to perform.
     */
    public Edit(ICommand cmd, String presentationName) {
        this.cmd = cmd;
        this.presentationName = presentationName;
    }

    /**
     * Executes the associated operation and registers it for undo.
     *
     * @param undoSupport register here.
     */
    public void execute(UndoableEditSupport undoSupport) {
        //System.err.println("Executing: "+cmd.getClass().getName());
        //Basic.caught(new Exception(cmd.getClass().getName()));

        reverse_cmd = cmd.execute();
        performed = true;
        if (undoSupport != null) {
            undoSupport.postEdit(this);
        }
    }

    /*
     * The following methods override the corresponding ones in
     * {@link javax.swing.undo.AbstractUndoableEdit}.
     */

    public boolean canUndo() {
        return performed;
    }

    public boolean canRedo() {
        return !performed;
    }

    public void undo() {
        if (canUndo()) {
            cmd = reverse_cmd.execute();
            performed = false;
        } else {
            throw new CannotUndoException();
        }
    }

    public void redo() {
        if (canRedo()) {
            reverse_cmd = cmd.execute();
            performed = true;
        } else {
            throw new CannotRedoException();
        }
    }

    public String getPresentationName() {
        if (presentationName == null) {
            return "";
        } else {
            return presentationName;
        }
    }
}
