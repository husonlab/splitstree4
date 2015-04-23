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

import jloda.graph.Edge;
import jloda.graph.Node;
import splitstree.gui.main.MainViewer;
import splitstree.gui.undo.ChangeEdgeLabelCommand;
import splitstree.gui.undo.ChangeNodeLabelCommand;
import splitstree.gui.undo.Edit;
import splitstree.gui.undo.ICommand;

import java.awt.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Jan 19, 2006
 * Time: 5:02:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class GraphSearchManager implements SearchManager {

    private MainViewer viewer;

    public GraphSearchManager(MainViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * getSelectedNode
     *
     * @return a selected node, if there is exactly one node selected, and null otherwise
     */
    private Node getSelectedNode() {
        Iterator it = viewer.getSelectedNodes().iterator();
        if (!it.hasNext())
            return null;
        Node nodeFound = (Node) it.next();
        if (it.hasNext())
            return null;
        return nodeFound;
    }

    /**
     * getSelectedEdge
     *
     * @return a selectedEdge, if there is exactly one edge selected, and null otherwise
     */
    Edge getSelectedEdge() {
        Iterator it = viewer.getSelectedEdges().iterator();
        if (!it.hasNext())
            return null;
        Edge edgeFound = (Edge) it.next();
        if (it.hasNext())
            return null;
        return edgeFound;
    }


    /**
     * prepareRegexp
     *
     * @param searchString
     * @param options
     * @return regular expression string.
     *         <p/>
     *         We use the regular expression matches for all of our string matching... even when
     *         regular expression is not set. This allows us to code all the extra options (case insenstivity,
     *         matching whole label or partial label, etc.) directly into the regular expression.
     *         <p/>
     *         One catch is that if we are not using regular expression search and the user has entered
     *         a \E into the search string, then we run into problems. Not much we can do about that...
     *         so I just throw an exception. There may be alternatives in future, but I strong suspect
     *         that this is not going to be a major issue.
     */
    private String prepareRegexp(String searchString, SearchOptions options) {

        String regexp = "" + searchString; //Copy the search string over.

        /* Reg expression or not? If not regular expression, we need to surround the above
        with quote literals: \Q expression \E just in case there are some regexp characters
        already there. Note - this will fail if string already contains \E or \Q !!!!!!! */
        if (!options.regExpression) {
            if (regexp.contains("\\E"))
                throw new PatternSyntaxException("Illegal character ''\\'' in search string", searchString, -1);
            regexp = '\\' + "Q" + regexp + '\\' + "E";
        }

        if (options.wholeWordOnly)
            regexp = "^" + regexp + "$";

        /* Check if case insensitive - if it is, then append (?i) before string */
        if (options.caseInsensitive)
            regexp = "(?i)" + regexp;

        System.err.println(regexp);

        return regexp;
    }


    private boolean matchLabel(String regexp, String label, SearchOptions options) {

        if (label == null)
            return false;
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(label);
        return matcher.find();
    }


    //Find next instance - returns true if one is found.
    private boolean singleSearch(String searchText, boolean forward, SearchOptions options) {

        if (forward)
            System.err.println(" Searching forward for the text [" + searchText + "]");
        else
            System.err.println(" Searching backward for the text [" + searchText + "]");
        //System.err.println("Options are " + options);

        //Do nothing if search string is empty.
        if (searchText.length() == 0)
            return false;

        //Check if a *single* edge or node label is selected.
        Node thisNode = getSelectedNode();
        Edge thisEdge = getSelectedEdge();
        if (thisNode != null && thisEdge != null) {
            thisNode = null;
            thisEdge = null;
        }

        // seachingNodes flag to indicate if we are currently searching through nodes
        // or edges.
        boolean searchingNodes = true; //Currently searching nodes.
        if (thisEdge != null)
            searchingNodes = false;

        //We use a count to protect against infinite loops when searching with wrapping text.
        boolean found = false;
        int maxcount = viewer.getGraph().getNumberOfEdges() + viewer.getGraph().getNumberOfNodes();
        int count = 0;

        //Perform search for label matching regular expression.
        //ToDo: For efficiency, compile pattern here.
        String regExp = prepareRegexp(searchText, options);
        while (!found && count < maxcount) {
            if (searchingNodes) {

                if (thisNode != null)
                    if (forward)
                        thisNode = viewer.getGraph().getNextNode(thisNode);
                    else
                        thisNode = viewer.getGraph().getPrevNode(thisNode);
                else if (forward)
                    thisNode = viewer.getGraph().getFirstNode();
                else
                    thisNode = viewer.getGraph().getLastNode();

                if (thisNode == null) //No more nodes
                    if (forward)
                        searchingNodes = false; //No more nodes... switch to edges
                    else if (options.wrapSearch)
                        searchingNodes = false;
                    else
                        break; //Searching back... run out of nodes means end of search.
                else {
                    count++;
                    String label = viewer.getLabel(thisNode);
                    if (matchLabel(regExp, label, options))
                        found = true;
                }
            } else {

                //Searching Edges
                if (thisEdge != null)
                    if (forward)
                        thisEdge = viewer.getGraph().getNextEdge(thisEdge);
                    else
                        thisEdge = viewer.getGraph().getPrevEdge(thisEdge);
                else if (forward)
                    thisEdge = viewer.getGraph().getFirstEdge();
                else
                    thisEdge = viewer.getGraph().getLastEdge();
                if (thisEdge == null)
                    if (forward)
                        if (options.wrapSearch)
                            searchingNodes = true; //No more edges... switch to nodes
                        else
                            break;
                    else
                        searchingNodes = true;
                else {
                    count++;
                    String label = viewer.getLabel(thisEdge);
                    if (matchLabel(regExp, label, options))
                        found = true;
                }
            }
        }

        viewer.selectAllNodes(false);
        viewer.selectAllEdges(false);
        if (found) {
            if (thisNode != null)  //Found a node label.
            {
                viewer.setSelected(thisNode, true);
                final Point p = viewer.trans.w2d(viewer.getLocation(thisNode));
                viewer.scrollRectToVisible(new Rectangle(p.x - 25, p.y - 25, 50, 50));
                viewer.repaint();
            } else  //Found an edge label
            {
                viewer.setSelected(thisEdge, true);
                final Point p = viewer.trans.w2d(viewer.getLocation(thisEdge.getSource()));
                final Point q = viewer.trans.w2d(viewer.getLocation(thisEdge.getTarget()));
                Rectangle rect = new Rectangle(p);
                rect.add(q);
                rect.width += 5;
                rect.height += 5;
                viewer.scrollRectToVisible(rect);
                viewer.repaint();
            }
        }

        //is deselected. This would be the place to adjust scroll bars.

        return found;
    }

    //Find next instance - returns true if one is found.
    public boolean next(String searchText, SearchOptions options) throws PatternSyntaxException {
        return singleSearch(searchText, true, options);
    }

    //Find previous instance - returns true if one is found.
    public boolean previous(String searchText, SearchOptions options) {
        return singleSearch(searchText, false, options);
    }


    private String replaceInString(String source, String searchText, String replaceText, SearchOptions options) {

        if (options.wholeWordOnly)
            return "" + replaceText;
        else {
            String regExp = prepareRegexp(searchText, options);
            return source.replaceAll(regExp, replaceText);
        }
    }

    public void replace(String searchText, String replaceText, SearchOptions options) {
        //Check if a *single* edge or node label is selected.
        Node thisNode = getSelectedNode();
        Edge thisEdge = getSelectedEdge();
        if (thisNode != null && thisEdge != null) {
            thisNode = null;
            thisEdge = null;
        }
        if (thisNode != null) {
            String label = viewer.getLabel(thisNode);
            if (label == null) {
            }
            else {
                String newLabel = replaceInString(label, searchText, replaceText, options);
                final ICommand cmd = new ChangeNodeLabelCommand(viewer, thisNode, newLabel);
                new Edit(cmd, "replace in node label").execute(viewer.getUndoSupportNetwork());
            }
        } else if (thisEdge != null) {
            String label = viewer.getLabel(thisEdge);
            if (label == null) {
            }
            else {
                String newLabel = replaceInString(label, searchText, replaceText, options);
                final ICommand cmd = new ChangeEdgeLabelCommand(viewer, thisEdge, newLabel);
                new Edit(cmd, "replace in edge label").execute(viewer.getUndoSupportNetwork());
            }
        }

    }


    public int replaceAll(String searchText, String replaceText, SearchOptions options) {

        int numNodesChanged = 0;
        int numEdgesChanged = 0;

        Iterator nodeIterator = viewer.getGraph().nodeIterator();
        Iterator edgeIterator = viewer.getGraph().edgeIterator();

        if (options.replaceAllSelectionOnly) {
            nodeIterator = viewer.getSelectedNodes().iterator();
            edgeIterator = viewer.getSelectedEdges().iterator();
        }

        String regExp = prepareRegexp(searchText, options);

        //   CompoundCommand compoundCmd = new CompoundCommand();

        while (nodeIterator.hasNext()) {
            Node thisNode = (Node) nodeIterator.next();
            String label = viewer.getLabel(thisNode);
            if (matchLabel(regExp, label, options)) {
                label = label.replaceAll(regExp, replaceText);
                final ICommand cmd = new ChangeNodeLabelCommand(viewer, thisNode, label);
                //compoundCmd.add(cmd);
                new Edit(cmd, "replace in node label").execute(viewer.getUndoSupportNetwork());
                numNodesChanged++;
            }
        }

        while (edgeIterator.hasNext()) {
            Edge thisEdge = (Edge) edgeIterator.next();
            String label = viewer.getLabel(thisEdge);
            if (matchLabel(regExp, label, options)) {
                label = label.replaceAll(regExp, replaceText);
                final ICommand cmd = new ChangeEdgeLabelCommand(viewer, thisEdge, label);
                //compoundCmd.add(cmd);
                new Edit(cmd, "replace in node label").execute(viewer.getUndoSupportNetwork());
                numEdgesChanged++;
            }
        }

        //new Edit(compoundCmd,"replace all").execute(viewer.getUndoSupportNetwork());

        return numNodesChanged + numEdgesChanged;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void findAll(String searchText, SearchOptions options) {
        Rectangle bbox = null; // need the bbox to adjust the scrollbars

        Iterator nodeIterator = viewer.getGraph().nodeIterator();
        Iterator edgeIterator = viewer.getGraph().edgeIterator();

        if (options.replaceAllSelectionOnly) {
            nodeIterator = viewer.getSelectedNodes().iterator();
            edgeIterator = viewer.getSelectedEdges().iterator();
        }

        String regExp = prepareRegexp(searchText, options);
        viewer.selectAllNodes(false);
        viewer.selectAllEdges(false);

        while (nodeIterator.hasNext()) {
            Node thisNode = (Node) nodeIterator.next();
            String label = viewer.getLabel(thisNode);
            if (matchLabel(regExp, label, options)) {
                viewer.setSelected(thisNode, true);
                if (bbox == null)
                    bbox = new Rectangle(viewer.trans.w2d(viewer.getLocation(thisNode)));
                else
                    bbox.add(viewer.trans.w2d(viewer.getLocation(thisNode)));
            }
        }

        while (edgeIterator.hasNext()) {
            Edge thisEdge = (Edge) edgeIterator.next();
            String label = viewer.getLabel(thisEdge);
            if (matchLabel(regExp, label, options)) {
                viewer.setSelected(thisEdge, true);
                if (bbox == null) {
                    bbox = new Rectangle(viewer.trans.w2d(viewer.getLocation(thisEdge.getSource())));
                    bbox.add(viewer.trans.w2d(viewer.getLocation(thisEdge.getTarget())));
                } else {
                    bbox.add(viewer.trans.w2d(viewer.getLocation(thisEdge.getSource())));
                    bbox.add(viewer.trans.w2d(viewer.getLocation(thisEdge.getTarget())));

                }
            }
        }

        if (bbox != null) {
            bbox.width += 5;
            bbox.height += 5;
            viewer.scrollRectToVisible(bbox);
            viewer.repaint();
        }
    }
}
