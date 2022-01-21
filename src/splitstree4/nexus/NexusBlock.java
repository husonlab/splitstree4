/*
 * NexusBlock.java Copyright (C) 2022 Daniel H. Huson
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
/**
 * @version $Id: NexusBlock.java,v 1.9 2007-09-11 12:30:59 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.nexus;

import java.io.IOException;
import java.io.Writer;


/**
 * NexusBlock base class
 */
abstract public class NexusBlock {
    private boolean valid = true;

    /**
     * set valid state
     *
     * @param valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * get valid state
     *
     * @return valid?
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * write a block, blocks should override this
     *
     * @param w
     * @param taxa
     * @throws IOException
     */
    public abstract void write(Writer w, Taxa taxa) throws IOException;
}

// EOF
