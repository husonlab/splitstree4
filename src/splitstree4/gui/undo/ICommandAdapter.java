/*
 * ICommandAdapter.java Copyright (C) 2022 Daniel H. Huson
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
 * DESCRIPTION
 * Daniel Huson and David Bryant
 */
public class ICommandAdapter implements ICommand {
    ICommand reverseCommand = null;

    /**
     * Executes this command.
     *
     * @return a new command which will undo the effect of this execution.
     */
    public ICommand execute() {
        return null;
    }

    /**
     * sets the reverse command for this command and makes this command the reverse of
     * the reverse command
     *
	 */
    public void setReverseCommand(ICommand reverseCommand) {
        this.reverseCommand = reverseCommand;
        if (reverseCommand.getReverseCommand() == null)
            reverseCommand.setReverseCommand(this);
    }

    /**
     * gets the reverse command
     *
	 */
    public ICommand getReverseCommand() {
        return reverseCommand;
    }
}
