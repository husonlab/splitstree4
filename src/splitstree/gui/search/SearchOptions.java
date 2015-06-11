/**
 * SearchOptions.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
