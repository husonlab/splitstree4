/*
 * TextSearchManager.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.search;

import splitstree4.gui.main.MainViewer;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class TextSearchManager implements SearchManager {


    final MainViewer viewer;


    public TextSearchManager(MainViewer viewer) {
        this.viewer = viewer;
    }

    //We start the search at the end of the selection, which could be the dot or the mark.
    private int getSearchStart() {
        Caret caret = viewer.getTextEditor().getInputTextArea().getCaret();
        int dot = caret.getDot();
        int mark = caret.getMark();
        return java.lang.Math.max(dot, mark);
    }


    private void selectMatched(Matcher matcher) {
        //viewer.getTextEditor().getInputTextArea().select(matcher.start(),matcher.end());
        viewer.getTextEditor().getInputTextArea().setCaretPosition(matcher.start());
        viewer.getTextEditor().getInputTextArea().moveCaretPosition(matcher.end());
    }


    private String getText() {
        int length = viewer.getTextEditor().getInputTextArea().getDocument().getLength();
        try {
            return viewer.getTextEditor().getInputTextArea().getText(0, length);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String prepareRegExp(String searchString, SearchOptions options) {
        String regexp = "" + searchString; //Copy the search string over.

        /* Reg expression or not? If not regular expression, we need to surround the above
        with quote literals: \Q expression \E just in case there are some regexp characters
        already there. Note - this will fail if string already contains \E or \Q !!!!!!! */
        if (!options.regExpression) {
            if (regexp.contains("\\E"))
                throw new PatternSyntaxException("Illegal character ''\\'' in search string", searchString, -1);
            regexp = '\\' + "Q" + regexp + '\\' + "E";
        }

        /* If whole word only, insist that there is a word boundary before and after the word */
        if (options.wholeWordOnly)
            regexp = "\b" + regexp + "\b";

        /* Check if case insensitive - if it is, then append (?i) before string */
        if (options.caseInsensitive)
            regexp = "(?i)" + regexp;

        //System.err.println(regexp);

        return regexp;
    }


    private boolean singleSearch(String searchText, boolean forward, SearchOptions options) throws PatternSyntaxException {

        //Do nothing if there is no text.
        if (searchText.length() == 0)
            return false;

        //Search begins at the end of the currently selected portion of text.
        int currentPoint = getSearchStart();


        boolean found = false;

        Pattern pattern = Pattern.compile(prepareRegExp(searchText, options));

        String source = getText();
        Matcher matcher = pattern.matcher(source);

        if (forward)
            found = matcher.find(currentPoint);
        else {
            //This is an inefficient algorithm to handle reverse search. It is a temporary
            //stop gap until reverse searching is built into the API.
            //TODO: Check every once and a while to see when matcher.previous() is implemented in the API.
            //TODO: Consider use of GNU find/replace.
            //TODO: use regions to make searching more efficient when we know the length of the search string to match.
            int pos = 0;
            int searchFrom = 0;
            //System.err.println("Searching backwards before " + currentPoint);
            while (matcher.find(searchFrom) && matcher.end() < currentPoint) {
                pos = matcher.start();
                searchFrom = matcher.end();
                found = true;
                //System.err.println("\tfound at [" + pos + "," + matcher.end() + "]" + " but still looking");
            }
            if (found)
                matcher.find(pos);
            //System.err.println("\tfound at [" + pos + "," + matcher.end() + "]");
        }

        if (!found && options.wrapSearch && currentPoint != 0) {
            matcher = pattern.matcher(source);
            found = matcher.find();
        }

        if (!found)
            return false;

        //System.err.println("Pattern found between positions " + matcher.start() + " and " + matcher.end());
        selectMatched(matcher);
        return true;
    }

    //Find next instance - returns true if one is found.
    public boolean next(String searchText, SearchOptions options) throws PatternSyntaxException {
        return singleSearch(searchText, true, options);
    }

    //Find previous instance - returns true if one is found.
    public boolean previous(String searchText, SearchOptions options) throws PatternSyntaxException {
        return singleSearch(searchText, false, options);
    }

    public void replace(String searchText, String replaceText, SearchOptions options) throws PatternSyntaxException {
        viewer.getTextEditor().getInputTextArea().replaceSelection(replaceText);
    }

    public int replaceAll(String searchText, String replaceText, SearchOptions options) throws PatternSyntaxException {
        Pattern pattern = Pattern.compile(prepareRegExp(searchText, options));
        String source;

        //If we are doing replaceAll only in the selection, then we set the text appropriately.
        //With Jave 1.5 this is done using regions, but for backward compatibility we will use
        //a cruder version.
        if (options.replaceAllSelectionOnly)
            source = viewer.getTextEditor().getInputTextArea().getSelectedText();
        else
            source = getText();

        Matcher matcher = pattern.matcher(source);
        //We do a manual count of the number of >>non-overlapping<< patterns match.
        int count = 0;
        int pos = 0;
        while (matcher.find(pos)) {
            count++;
            pos = matcher.end();
        }

        //Now do the replaceAll
        matcher.replaceAll(replaceText);

        //Now put this back into the text ddocument

        if (options.replaceAllSelectionOnly)
            viewer.getTextEditor().getInputTextArea().replaceSelection(matcher.replaceAll(replaceText));
        else {
            viewer.getTextEditor().getInputTextArea().setText(matcher.replaceAll(replaceText));
            viewer.getTextEditor().getInputTextArea().setCaretPosition(0);
        }

        return count;
    }

    //Not implemented for text editors.... as we cannot select multiple chunks of text.
    public void findAll(String searchText, SearchOptions options) throws PatternSyntaxException {
    }
}
