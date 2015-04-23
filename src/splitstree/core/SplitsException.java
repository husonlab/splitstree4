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
 * @version $Id: SplitsException.java,v 1.4 2004-08-24 17:37:07 huson Exp $
 *
 * @author Daniel Huson and David Bryant
 */

package splitstree.core;


/**
 * General splitstree exception
 */
public class SplitsException extends Exception {
    public SplitsException() {
        super();
    }

    public SplitsException(String str) {
        super(str);
    }
}

// EOF

