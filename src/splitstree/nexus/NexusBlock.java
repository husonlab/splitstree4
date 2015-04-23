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

/**
 * @version $Id: NexusBlock.java,v 1.9 2007-09-11 12:30:59 kloepper Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree.nexus;

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
