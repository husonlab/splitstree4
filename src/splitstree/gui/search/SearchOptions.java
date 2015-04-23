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

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jan 19, 2006
 * Time: 4:34:38 PM
 * <p/>
 * Just a structure to store options for a search.
 */
public class SearchOptions {
    public boolean caseInsensitive;  //Case Sensitive
    public boolean regExpression; //Interpret string as regular expression
    public boolean wholeWordOnly; //Only return if white space follows word.
    public boolean wrapSearch; //Continue at beginning if not found before end. [or
    // equiv if searching backwards
    public boolean replaceAllSelectionOnly; //only replaceAll within the selected area.

    public SearchOptions() {
        caseInsensitive = false;
        regExpression = false;
        wholeWordOnly = false;
        wrapSearch = false;
    }

    public String toString() {
        String s = "";
        s += "case=" + caseInsensitive + " regExp = " + regExpression + " whole =" + wholeWordOnly + " wrap =" + wrapSearch + " selection = " + replaceAllSelectionOnly;
        return s;
    }
}
