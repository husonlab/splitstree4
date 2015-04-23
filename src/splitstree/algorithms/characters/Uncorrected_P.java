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

/*
* $Id: Uncorrected_P.java,v 1.5 2007-09-11 12:31:00 kloepper Exp $
*/
package splitstree.algorithms.characters;


/**
 * Simple implementation of hamming distances
 */
public class Uncorrected_P extends Hamming implements Characters2Distances {

    public final static String DESCRIPTION = "Calculates uncorrected (observed, \"P\") distances.";
    protected final String TASK = "Uncorrected P Distance";

    protected String getTask() {
        return TASK;
    }


    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }
}

// EOF
