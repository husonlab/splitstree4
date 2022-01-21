/*
 * ICommand.java Copyright (C) 2022 Daniel H. Huson
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

/**
 * An interface for undoable command objects. There is just a single required
 * method which executes the command and returns a new command object which
 * will reverse its effects.
 *
 * @author Olaf Delgado
 * @version $Id: ICommand.java,v 1.2 2005-05-18 12:53:03 huson Exp $
 */

public interface ICommand {
    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    ICommand execute();

    void setReverseCommand(ICommand reverseCommand);

    ICommand getReverseCommand();
}
