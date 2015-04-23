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

