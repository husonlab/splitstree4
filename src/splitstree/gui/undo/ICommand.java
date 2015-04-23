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
