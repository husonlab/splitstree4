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

package splitstree.gui;

/**
 * Conceptually, a project consists of a Director, a Document and all associated Viewers.
 * In the code, every Director is a Project.
 *
 * @author huson
 *         Date: 01-Dec-2003
 */
public interface Project {
    void setID(int id); // a project number for each project

    int getID();        // gets the project number

}
