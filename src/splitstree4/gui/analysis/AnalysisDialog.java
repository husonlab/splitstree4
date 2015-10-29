/**
 * AnalysisDialog.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.analysis;

import jloda.util.Basic;
import splitstree4.analysis.AnalysisMethod;
import splitstree4.analysis.bootstrap.BootstrapAnalysisMethod;
import splitstree4.analysis.characters.CharactersAnalysisMethod;
import splitstree4.analysis.distances.DistancesAnalysisMethod;
import splitstree4.analysis.network.NetworkAnalysisMethod;
import splitstree4.analysis.quartets.QuartetsAnalysisMethod;
import splitstree4.analysis.splits.SplitsAnalysisMethod;
import splitstree4.analysis.trees.TreesAnalysisMethod;
import splitstree4.analysis.unaligned.UnalignedAnalysisMethod;
import splitstree4.util.PluginClassLoader;

import javax.swing.*;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * show list of all known analysis methods
 * Daniel Huson and David Bryant, 5.2010
 */
public class AnalysisDialog extends JDialog {

    final JList jList = new JList();

    public AnalysisDialog() {
        final SortedSet<AnalysisMethod> methods = new TreeSet<>();

        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.bootstrap", BootstrapAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.characters", CharactersAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.distances", DistancesAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.network", NetworkAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.quartets", QuartetsAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.splits", SplitsAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.trees", TreesAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splitstree4.analysis.unaligned", UnalignedAnalysisMethod.class));
        jList.setModel(new MyListModel(methods));

    }

    class MyListElement {
        AnalysisMethod method;
        String name;
        String description;

        public MyListElement(AnalysisMethod method) {
            this.method = method;
            this.name = Basic.getShortName(method.getClass());
            this.description = method.getDescription();
        }

        public String toString() {
            return name;
        }
    }

    class MyListModel extends DefaultListModel {
        private final MyListElement[] listElements;

        /**
         * constructor
         *
         * @param methods
         */
        public MyListModel(Collection<AnalysisMethod> methods) {
            listElements = new MyListElement[methods.size()];
            int count = 0;
            for (AnalysisMethod method : methods) {
                listElements[count++] = new MyListElement(method);
            }
        }

        /**
         * Returns the length of the list.
         *
         * @return the length of the list
         */
        public int getSize() {
            return listElements.length;
        }

        /**
         * Returns the value at the specified index.
         *
         * @param index the requested index
         * @return the value at <code>index</code>
         */
        public Object getElementAt(int index) {
            return listElements[index];
        }
    }
}
