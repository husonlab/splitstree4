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
     * @param reverseCommand
     */
    public void setReverseCommand(ICommand reverseCommand) {
        this.reverseCommand = reverseCommand;
        if (reverseCommand.getReverseCommand() == null)
            reverseCommand.setReverseCommand(this);
    }

    /**
     * gets the reverse command
     *
     * @return
     */
    public ICommand getReverseCommand() {
        return reverseCommand;
    }
}
