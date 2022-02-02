/*
 * SetOutgroupCommand.java Copyright (C) 2022 Daniel H. Huson
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

import splitstree4.core.TaxaSet;
import splitstree4.gui.Director;
import splitstree4.gui.main.MainViewer;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Taxa;

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
