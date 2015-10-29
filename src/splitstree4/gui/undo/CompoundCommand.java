/**
 * CompoundCommand.java
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
package splitstree4.gui.undo;


import java.util.LinkedList;

/**
 * A sequence of commands treated like a single one.
 *
 * @author Olaf Delgado
 */
public class CompoundCommand extends LinkedList<ICommand> implements ICommand {
    ICommand reverseCommand;

    public void setReverseCommand(ICommand reverseCommand) {
        this.reverseCommand = reverseCommand;
    }

    public ICommand getReverseCommand() {
        return reverseCommand;
    }

    public ICommand execute() {
        if (getReverseCommand() == null) {
            CompoundCommand res = new CompoundCommand();
            for (ICommand cmd : this) {
                // --- the reversal of a compound is the reverse list of reversals
                res.addFirst(cmd.execute());
            }
            setReverseCommand(res);
        }
        return getReverseCommand();
    }
}

