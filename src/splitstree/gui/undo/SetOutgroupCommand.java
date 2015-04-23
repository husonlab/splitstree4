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

import splitstree.core.TaxaSet;
import splitstree.gui.Director;
import splitstree.gui.main.MainViewer;
import splitstree.nexus.Sets;
import splitstree.nexus.Taxa;

/**
 * set the outgroup for the dataset
 * daniel Huson, 11.2010
 */
public class SetOutgroupCommand extends ICommandAdapter implements ICommand {
    private final MainViewer viewer;
    private final TaxaSet outgroup;

    /**
     * set the outgroup taxon set
     *
     * @param viewer
     * @param outgroup
     */
    public SetOutgroupCommand(MainViewer viewer, TaxaSet outgroup) {
        this.viewer = viewer;
        this.outgroup = outgroup;

    }

    /**
     * execute the command
     *
     * @return reverse command
     */
    public ICommand execute() {
        final Director dir = viewer.getDir();
        final Taxa taxa = dir.getDocument().getTaxa();
        final String setName = "Outgroup";

        TaxaSet currentOutgroup = null;
        Sets theSets = dir.getDocument().getSets();
        if (theSets != null) {
            currentOutgroup = theSets.getTaxSet(setName, taxa);
        }
        setReverseCommand(new SetOutgroupCommand(viewer, currentOutgroup));

        if (outgroup == null) {
            if (currentOutgroup != null) {
                theSets.removeTaxSet(setName);
            }
        } else {
            if (theSets == null)
                theSets = new Sets();
            else
                theSets.removeTaxSet(setName);
            theSets.addTaxSet(setName, outgroup, taxa);
        }
        dir.getDocument().setSets(theSets);
        viewer.getMenuBar().updateTaxonSets();
        return getReverseCommand();
    }
}
