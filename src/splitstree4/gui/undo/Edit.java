/*
 * Edit.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.undo;

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
