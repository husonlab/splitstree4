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

package splitstree.gui.undo;

import jloda.graph.Edge;
import jloda.graph.EdgeSet;
import jloda.graph.Graph;
import jloda.gui.find.IObjectSearcher;
import splitstree.gui.main.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Class for finding and replacing edge labels in a graph
 * Daniel Huson and David Bryant, 7.2008
 */
public class UndoableEdgeLabelSearcher implements IObjectSearcher {
    final private Frame frame;
    final private String name;
    final Graph graph;
    final MainViewer viewer;
    Edge current = null;

    final EdgeSet toSelect;
    final EdgeSet toDeselect;
    public static final String SEARCHER_NAME = "Edges";

    /**
     * constructor
     *
     * @param viewer
     */
    public UndoableEdgeLabelSearcher(MainViewer viewer) {
        this(null, SEARCHER_NAME, viewer);
    }

    /**
     * constructor
     *
     * @param viewer
     */
    public UndoableEdgeLabelSearcher(Frame frame, MainViewer viewer) {
        this(frame, SEARCHER_NAME, viewer);
    }

    /**
     * constructor
     *
     * @param name
     * @param viewer
     */
    public UndoableEdgeLabelSearcher(Frame frame, String name, MainViewer viewer) {
        this.frame = frame;
        this.name = name;
        this.viewer = viewer;
        this.graph = viewer.getGraph();
        toSelect = new EdgeSet(graph);
        toDeselect = new EdgeSet(graph);
    }

    /**
     * get the parent component
     *
     * @return parent
     */
    public Component getParent() {
        return frame;
    }

    /**
     * get the name for this type of search
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * goto the first object
     */
    public boolean gotoFirst() {
        current = graph.getFirstEdge();
        return isCurrentSet();
    }

    /**
     * goto the next object
     */
    public boolean gotoNext() {
        if (current == null)
            gotoFirst();
        else
            current = current.getNext();
        return isCurrentSet();
    }

    /**
     * goto the last object
     */
    public boolean gotoLast() {
        current = graph.getLastEdge();
        return isCurrentSet();
    }

    /**
     * goto the previous object
     */
    public boolean gotoPrevious() {
        if (current == null)
            gotoLast();
        else
            current = current.getPrev();
        return isCurrentSet();
    }

    /**
     * is the current object selected?
     *
     * @return true, if selected
     */
    public boolean isCurrentSelected() {
        return isCurrentSet()
                && viewer.getSelected(current);
    }

    /**
     * set selection state of current object
     *
     * @param select
     */
    public void setCurrentSelected(boolean select) {
        if (current != null) {
            if (select)
                toSelect.add(current);
            else
                toDeselect.add(current);
        }
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        viewer.selectAllEdges(select);
        viewer.repaint();
    }

    /**
     * get the label of the current object
     *
     * @return label
     */
    public String getCurrentLabel() {
        if (current == null)
            return null;
        else
            return viewer.getLabel(current);
    }

    /**
     * set the label of the current object
     *
     * @param newLabel
     */
    public void setCurrentLabel(String newLabel) {
        if (current != null) {
            String currentLabel = viewer.getLabel(current);
            if (newLabel == null && currentLabel != null) {
                final ICommand cmd = new ChangeEdgeLabelCommand(viewer, current, null);
                new Edit(cmd, "replace in edge label").execute(viewer.getUndoSupportNetwork());
                fireLabelChangedListeners(current);
            } else if (newLabel != null && currentLabel != null && !newLabel.equals(currentLabel)) {
                final ICommand cmd = new ChangeEdgeLabelCommand(viewer, current, newLabel);
                new Edit(cmd, "replace in edge label").execute(viewer.getUndoSupportNetwork());
                fireLabelChangedListeners(current);
            }
        }
    }

    /**
     * is a global find possible?
     *
     * @return true, if there is at least one object
     */
    public boolean isGlobalFindable() {
        return graph.getNumberOfEdges() > 0;
    }

    /**
     * is a selection find possible
     *
     * @return true, if at least one object is selected
     */
    public boolean isSelectionFindable() {
        return false;
    }

    /**
     * is the current object set?
     *
     * @return true, if set
     */
    public boolean isCurrentSet() {
        return current != null;
    }

    /**
     * something has been changed or selected, update view
     */
    public void updateView() {
        viewer.selectedEdges.addAll(toSelect);
        viewer.fireDoSelect(toSelect);
        Edge edge = toSelect.getLastElement();
        if (edge != null) {
            final Point p = viewer.trans.w2d(viewer.getLocation(edge.getSource()));
            final Point q = viewer.trans.w2d(viewer.getLocation(edge.getTarget()));

            Rectangle rect = new Rectangle(p.x - 60, p.y - 25, 120, 50);
            rect.add(q);
            viewer.scrollRectToVisible(rect);
        }
        viewer.selectedEdges.removeAll(toDeselect);
        viewer.fireDoDeselect(toDeselect);
        toSelect.clear();
        toDeselect.clear();

        viewer.repaint();
    }

    /**
     * does this searcher support find all?
     *
     * @return true, if find all supported
     */
    public boolean canFindAll() {
        return true;
    }

    final private java.util.List labelChangedListeners = new LinkedList();

    /**
     * fire the label changed listener
     *
     * @param e
     */
    private void fireLabelChangedListeners(Edge e) {
        for (Object labelChangedListener : labelChangedListeners) {
            LabelChangedListener listener = (LabelChangedListener) labelChangedListener;
            listener.doLabelHasChanged(e);
        }
    }

    /**
     * add a label changed listener
     *
     * @param listener
     */
    public void addLabelChangedListener(LabelChangedListener listener) {
        labelChangedListeners.add(listener);
    }

    /**
     * remove a label changed listener
     *
     * @param listener
     */
    public void removeLabelChangedListener(LabelChangedListener listener) {
        labelChangedListeners.remove(listener);
    }

    /**
     * label changed listener
     */
    public interface LabelChangedListener {
        void doLabelHasChanged(Edge e);
    }


    /**
     * how many objects are there?
     *
     * @return number of objects or -1
     */
    public int numberOfObjects() {
        return graph.getNumberOfEdges();
    }

    /**
     * how many selected objects are there?
     *
     * @return number of objects or -1
     */
    public int numberOfSelectedObjects() {
        return viewer.getSelectedEdges().size();
    }

    /**
     * get list of additional buttons to be embedded into find tool bar, or null
     */
    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }
}
