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

package splitstree.gui.analysis;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import splitstree.analysis.AnalysisMethod;
import splitstree.analysis.bootstrap.BootstrapAnalysisMethod;
import splitstree.analysis.characters.CharactersAnalysisMethod;
import splitstree.analysis.distances.DistancesAnalysisMethod;
import splitstree.analysis.network.NetworkAnalysisMethod;
import splitstree.analysis.quartets.QuartetsAnalysisMethod;
import splitstree.analysis.splits.SplitsAnalysisMethod;
import splitstree.analysis.trees.TreesAnalysisMethod;
import splitstree.analysis.unaligned.UnalignedAnalysisMethod;
import splitstree.util.PluginClassLoader;

import javax.swing.*;
import java.util.*;

/**
 * show list of all known analysis methods
 * Daniel Huson and David Bryant, 5.2010
 */
public class AnalysisDialog extends JDialog {

    final JList jList = new JList();

    public AnalysisDialog() {
        final SortedSet<AnalysisMethod> methods = new TreeSet<>();

        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.bootstrap", BootstrapAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.characters", CharactersAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.distances", DistancesAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.network", NetworkAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.quartets", QuartetsAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.splits", SplitsAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.trees", TreesAnalysisMethod.class));
        methods.addAll(PluginClassLoader.getInstancesSorted("splits.analysis.unaligned", UnalignedAnalysisMethod.class));
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
