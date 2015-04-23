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

package splitstree.algorithms.splits.reticulateTree;

import jloda.graph.NodeSet;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import splitstree.core.TaxaSet;
import splitstree.nexus.Characters;
import splitstree.nexus.Splits;
import splitstree.nexus.Taxa;
import splitstree.nexus.Trees;


public class OptimizeLayout {


    public PhyloGraphView apply(PhyloGraph graph, PhyloGraphView graphView, NodeSet[] nettedComp, TaxaSet[][] inducedTaxa2origTaxa, ReticulationTree[] reticulationList,
                                Taxa taxa, Splits splits, Characters chars, Trees trees, String outgroup, int angle) {

        return null;
    }

}
