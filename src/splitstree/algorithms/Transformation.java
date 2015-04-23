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

package splitstree.algorithms;

/**
 * @author Daniel Huson and David Bryant, Michael Schr�der
 * @version $Id: Transformation.java,v 1.5 2007-09-11 12:30:59 kloepper Exp $
 */
public interface Transformation {

    /**
     * implementations of algorithms should overwrite this
     * String with a short description of what they do.
     */
    String DESCRIPTION = "No description given for this algorithm.";

    /**
     * implementations of algorithms should overwrite this
     * String with the author's name.
     */
    String CONTACT_NAME = "Unknown author";

    /**
     * implementations of algorithms should overwrite this
     * String with webpage for the project, if available.
     */
    String CONTACT_ADRESS = "No web-address available";

    /**
     * implementations of algorithms should overwrite this
     * String with (email) adress of the author.
     */
    String CONTACT_MAIL = "No contact available";

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    String getDescription();
}
