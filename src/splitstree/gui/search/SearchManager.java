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

package splitstree.gui.search;

import java.util.regex.PatternSyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jan 19, 2006
 * Time: 4:25:05 PM
 * <p/>
 * A SearchManager is associated with any kind of object (textComponent, Graph) that
 * can be searched. The interface allows the dialog box or actions to control basic search
 * operations without knowledge of the underlying structure.
 * <p/>
 * When an object implementing SeearchManager is created, it should be associated to some
 * specific object. Every call to Find, Next, etc. should be preceeded by a Synch, which
 * synchs information in the Search Manager (position, selection) with whats in the object.
 * <p/>
 * If an command is not applicable for a search manager, the corresponding procedure
 * should do nothing. Applicability can be tested using isApplicable(searchAction) - this
 * can also be used to test whether buttons should be enabled or disabled.
 */
public interface SearchManager {

    //Standard methods. If SearchManager is updated, remember to add new actions here.
    int NEXT_ACTION = 0;
    int PREV_ACTION = 1;
    int REPLACE_ACTION = 2;
    int REPLACEALL_ACTION = 3;
    int FINDALL_ACTION = 4;


    /**
     * Find next instance
     *
     * @param searchText
     * @param options
     * @return - returns boolean: true if text found, false otherwise
     * @throws PatternSyntaxException
     */
    boolean next(String searchText, SearchOptions options) throws PatternSyntaxException;


    /**
     * Find previous instance
     *
     * @param searchText
     * @param options
     * @return - returns boolean: true if text found, false otherwise
     * @throws PatternSyntaxException
     */
    boolean previous(String searchText, SearchOptions options) throws PatternSyntaxException;

    /**
     * Replace selection with current. Does nothing if selection invalid.
     *
     * @param searchText
     * @param options
     * @throws PatternSyntaxException
     */
    void replace(String searchText, String replaceText, SearchOptions options) throws PatternSyntaxException;


    /**
     * Replace all occurrences of text in document, subject to options.
     *
     * @param searchText
     * @param replaceText
     * @param options
     * @return number of instances replaced
     * @throws PatternSyntaxException
     */
    int replaceAll(String searchText, String replaceText, SearchOptions options) throws PatternSyntaxException;

    /**
     * Selects all occurrences of text in document, subject to options and constraints of document type
     *
     * @param searchText
     * @param options
     * @throws PatternSyntaxException
     */
    void findAll(String searchText, SearchOptions options) throws PatternSyntaxException;

}
