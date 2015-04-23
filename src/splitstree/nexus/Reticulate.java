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

package splitstree.nexus;

import jloda.graph.Edge;
import jloda.graph.IllegalSelfEdgeException;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import jloda.util.Basic;
import jloda.util.NotOwnerException;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;
import splitstree.core.SplitsException;

import java.io.*;
import java.util.*;

/**
 * Nexus Block representing a reticulate network
 */
public class Reticulate extends NexusBlock implements Cloneable {
    private boolean verbose = false;
    /**
     * Every value has to start at 1 and goes to =n
     */

    // is the nexus block valid
    private boolean valid = true;
    // is the nexus block up to date
    private boolean uptoDate = false;
    /**
     * Identification string
     */
    final public static String NAME = "Reticulate";
    // the original reticulate object
    private Reticulate originalReticulate;
    // the format subclass
    private Reticulate.Format format = null;

    /**
     * Data stuff
     */

    // the number of taxa
    private int Ntax;
    // the number of tree components
    private int NTreeComponents;
    // the number of netted components, if they exist
    private int NNettedComponents;
    // the number of backbones
    private int NRootComponents;

    // the labels of the tree components
    private Vector treeComponentsLabels;
    // the tree components in extended Newick format
    private Vector treeComponentsStrings;

    // the labels of the netted components
    private Vector nettedComponentsLabels;

    // the labels of the backbones of a netted Component
    private Vector nettedComponentsBackbonesLabels;
    // the Vectors of each netted component  which contains a list of possible networks in extended newick format
    private Vector nettedComponentsBackbonesStrings;

    // the labels of the components containing the root
    private Vector rootComponentsLabels;
    // the components of the reticulation networks  containing the root in eNewick
    private Vector rootComponentsStrings;


    // the roots of  the reticulation networks
    private Node root;
    // the reticulation network, to update the network uptoDate must be false
    private PhyloGraph reticulationNetwork;

    // The ids of the active backbone of each netted Component
    private Vector activeNettedComponentsBackbones;

    // the id of the active root Backbone
    private int activeRootComponent = -1;

    // the NettedComponents that are contained in the active rootBackbone
    private int[] containedNettedComponents;

    /**
     * Constructor
     */
    public Reticulate() {
        treeComponentsLabels = new Vector();
        treeComponentsLabels.add("");
        treeComponentsStrings = new Vector();
        treeComponentsStrings.add("");
        nettedComponentsLabels = new Vector();
        nettedComponentsLabels.add("");
        nettedComponentsBackbonesLabels = new Vector();
        nettedComponentsBackbonesLabels.add("");
        nettedComponentsBackbonesStrings = new Vector();
        nettedComponentsBackbonesStrings.add("");
        rootComponentsLabels = new Vector();
        rootComponentsLabels.add("");
        rootComponentsStrings = new Vector();
        rootComponentsStrings.add("");
        activeNettedComponentsBackbones = new Vector();
        activeNettedComponentsBackbones.add(-1);
        containedNettedComponents = new int[1];
        format = new Format();
    }

    /**
     * Constructor
     *
     * @param taxa
     */
    public Reticulate(Taxa taxa) {
        Ntax = taxa.getNtax();
        // init all Elements at position 0
        treeComponentsLabels = new Vector();
        treeComponentsLabels.add("");
        treeComponentsStrings = new Vector();
        treeComponentsStrings.add("");
        nettedComponentsLabels = new Vector();
        nettedComponentsLabels.add("");
        nettedComponentsBackbonesLabels = new Vector();
        nettedComponentsBackbonesLabels.add("");
        nettedComponentsBackbonesStrings = new Vector();
        nettedComponentsBackbonesStrings.add("");
        rootComponentsLabels = new Vector();
        rootComponentsLabels.add("");
        rootComponentsStrings = new Vector();
        rootComponentsStrings.add("");
        activeNettedComponentsBackbones = new Vector();
        activeNettedComponentsBackbones.add(0, -1);
        containedNettedComponents = new int[1];
        format = new Format();
    }

    /**
     * print the usage to the stream 'out'
     *
     * @param ps the output stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN RETICULATE;");
        ps.println("DIMENSIONS [NTAX=number-of-taxa] nRootComponents=number-of-root-components nNettedComponents=number-of-netted-components nTreeComponents=number-of-tree-components;");
        ps.println("FORMAT");
        ps.println("\t[ACTIVEROOT=id-of-active-root-component;]");
        ps.println("\t[ACTIVENETTEDCOMPONENTS=list-of-active-netted-components-ids;]");
        ps.println("\t[SHOWLABELS= {INTERNAL|TREECOMPONENTS|NETTEDCOMPONENTS};]");
        ps.println("[TREECOMPONENTS");
        ps.println("\t[name1 = eNewick-string of tree-component 1;]");
        ps.println("\t[name2 = eNewick-string of tree-component 2;]");
        ps.println("\t[...                                    ]");
        ps.println("\t[nameM = eNewick-string of tree-component M;]");
        ps.println("]");
        ps.println("[NETTEDCOMPONENTS");
        ps.println("\t[netted-components-name1 =");
        ps.println("\t\t[netted-components-1-backbone-name1 = eNewick-string of netted components 1 backbone 1;]");
        ps.println("\t\t[netted-components-1-backbone-name2 = eNewick-string of netted components 1 backbone 2;]");
        ps.println("\t\t[...                                                                                   ]");
        ps.println("\t\t[netted-components-1-backbone-nameM = eNewick-string of netted components 1 backbone M;]");
        ps.println("\t;]");
        ps.println("\t[netted-components-name2 =");
        ps.println("\t\t[netted-components-2-backbone-name1 = eNewick-string of netted components 2 backbone 1;]");
        ps.println("\t\t[netted-components-2-backbone-name2 = eNewick-string of netted components 2 backbone 2;]");
        ps.println("\t\t[...                                                                                   ]");
        ps.println("\t\t[netted-components-2-backbone-nameM = eNewick-string of netted components 2 backbone M;]");
        ps.println("\t;]");
        ps.println("\t[...]");
        ps.println("\t[netted-components-nameL =");
        ps.println("\t\t[netted-components-L-backbone-name1 = eNewick-string of netted components L backbone 1;]");
        ps.println("\t\t[netted-components-L-backbone-name2 = eNewick-string of netted components L backbone 2;]");
        ps.println("\t\t[...                                                                                   ]");
        ps.println("\t\t[netted-components-L-backbone-nameM = eNewick-string of netted components L backbone M;]");
        ps.println("\t;]");
        ps.println("]");
        ps.println("ROOTCOMPONENTS");
        ps.println("\t[backbone-name1 = eNewick-string of backbone1;]");
        ps.println("\t[backbone-name2 = eNewick-string of backbone2;]");
        ps.println("\t[...                                          ]");
        ps.println("\t[backbone-nameM = eNewick-string of backboneM;]");
        ps.println("END; [Reticulate]");
    }

    /**
     * return the reticulate object with full taxa set
     *
     * @return
     */
    public Reticulate getOriginal() {
        return originalReticulate;
    }

    /**
     * set the original reticulate object
     *
     * @param originalReticulate
     */
    public void setOriginal(Reticulate originalReticulate) {
        this.originalReticulate = originalReticulate;
    }

    /**
     * @param name
     * @return
     */
    public boolean getFormatSwitchValue(String name) {
        return true;
    }

    /**
     * get the number of taxa
     *
     * @return
     */
    public int getNtax() {
        return Ntax;
    }

    /**
     * get the number of root components
     *
     * @return
     */
    public int getNRootComponents() {
        return this.NRootComponents;
    }

    /**
     * get the number of TreeComponents
     *
     * @return
     */
    public int getNTreeComponents() {
        return NTreeComponents;
    }

    /**
     * get the number of netted components
     *
     * @return
     */
    public int getNNettedComponents() {
        return NNettedComponents;
    }


    // everything for the treeComponents

    /**
     * get the label of the TreeComponent
     *
     * @param which which TreeComponent
     * @return
     */
    public String getTreeComponentLabel(int which) {
        return (String) treeComponentsLabels.get(which);
    }

    /**
     * set the label of the TreeComponent
     *
     * @param which which TreeComponent
     * @param label the label
     * @return
     */
    public boolean setTreeComponentLabel(int which, String label) {
        if (treeComponentsLabels.get(which) != null) {
            treeComponentsLabels.set(which, label.trim());
            return true;
        } else
            return false;
    }

    /**
     * return the index of the TreeComponent label
     *
     * @param label
     * @return
     */
    public int indexOfTreeComponentLabel(String label) {
        return treeComponentsLabels.indexOf(label.trim());
    }

    /**
     * return a eNewick representation of the TreeComponent
     *
     * @param which which TreeComponent
     * @return
     */
    public String getTreeComponent(int which) {
        return (String) treeComponentsStrings.get(which);
    }

    /**
     * set the eNewick string of the TreeComponent
     *
     * @param which   which TreeComponent
     * @param eNewick the eNewick string
     * @return
     */
    public boolean setTreeComponent(int which, String eNewick) {
        if (treeComponentsLabels.get(which) != null) {
            treeComponentsStrings.add(which, eNewick.trim());
            uptoDate = false;
            return true;
        } else
            return false;
    }

    /**
     * add a TreeComponent to the list of TreeComponents
     *
     * @param label   the label of the TreeComponent (must be unique)
     * @param eNewick the eNewick string of the TreeComponent
     * @return
     */

    public boolean addTreeComponent(String label, String eNewick) {
        int lId = treeComponentsLabels.indexOf(label.trim());
        int nId = treeComponentsStrings.indexOf(eNewick.trim());
        if (lId == -1 && nId == -1) {
            treeComponentsLabels.add(label.trim());
            treeComponentsStrings.add(eNewick.trim());
            uptoDate = false;
            NTreeComponents++;
            return true;
        } else if (lId != -1) {
            System.err.println("TreeComponent label allready used: " + label);
            return false;
        } else if (nId != -1) {
            System.err.println("TreeComponent eNewick allready used; " + treeComponentsLabels.get(nId));
            return false;
        } else
            return false;
    }

    /**
     * remove a TreeComponent from the list
     *
     * @param which which TreeComponent
     * @return
     */
    public boolean deleteTreeComponent(int which) {
        if (treeComponentsLabels.remove(which) != null && treeComponentsStrings.remove(which) != null) {
            NTreeComponents--;
            uptoDate = false;
            return true;
        }
        return false;
    }

    /**
     * remove a TreeComponent from the list
     *
     * @param label which TreeComponent
     * @return
     */
    public boolean deleteTreeComponent(String label) {
        int which = treeComponentsLabels.indexOf(label.trim());
        return which != -1 && deleteTreeComponent(which);
    }


    // everything for the netted components

    /**
     * get the label of the netted component
     *
     * @param which which netted component
     * @return
     */
    public String getNettedComponentLabel(int which) {
        return (String) nettedComponentsLabels.get(which);
    }

    /**
     * sets the label of the netted component
     *
     * @param which which netted component
     * @param label the label
     * @return
     */
    public boolean setNettedComponentLabel(int which, String label) {
        if (nettedComponentsLabels.get(which) != null) {
            this.nettedComponentsLabels.set(which, label.trim());
            return true;
        } else
            return false;
    }

    /**
     * get the index of the netted component given the label
     *
     * @param label which netted component
     * @return
     */
    public int indexOfNettedComponentLabel(String label) {
        return nettedComponentsLabels.indexOf(label.trim());
    }

    /**
     * add a netted component
     *
     * @param componentLabel the label of the netted component
     * @return
     */
    public boolean addNettedComponent(String componentLabel) {
        if (!nettedComponentsLabels.contains(componentLabel.trim())) {
            nettedComponentsLabels.add(componentLabel.trim());
            Vector backboneLabels = new Vector();
            backboneLabels.add(0, "null");
            nettedComponentsBackbonesLabels.add(backboneLabels);
            Vector eNewicksBackbones = new Vector();
            eNewicksBackbones.add(0, "null");
            nettedComponentsBackbonesStrings.add(eNewicksBackbones);
            activeNettedComponentsBackbones.add(nettedComponentsLabels.indexOf(componentLabel.trim()), -1);
            NNettedComponents++;
            uptoDate = false;
            return true;
        } else
            return false;
    }

    /**
     * delete a netted component from the list
     *
     * @param which which netted component
     * @return
     */
    public boolean deleteNettedComponent(int which) {
        if (nettedComponentsLabels.remove(which) != null && nettedComponentsBackbonesStrings.remove(which) != null && nettedComponentsBackbonesLabels.remove(which) != null
                && (activeNettedComponentsBackbones.get(which) == null || activeNettedComponentsBackbones.remove(which) != null)) {
            NNettedComponents--;
            uptoDate = false;
            return true;
        } else
            return false;
    }

    /**
     * deleta a netted component from the list
     *
     * @param label which netted component
     * @return
     */
    public boolean deleteNettedComponent(String label) {
        int which = nettedComponentsLabels.indexOf(label);
        return which != -1 && deleteNettedComponent(which);
    }


    // for any backbone in a netted component

    /**
     * get the number of backbones within the netted component
     *
     * @param nettedComponent which netted component
     * @return
     */
    public int getNumberOfNettedComponentBackbones(int nettedComponent) {
        return ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).size() - 1;
    }

    /**
     * get the label of a backbone that is contained in a netted component
     *
     * @param nettedComponent which netted component
     * @param which           which backbone
     * @return
     */
    public String getNettedComponentBackboneLabel(int nettedComponent, int which) {
        if (nettedComponentsBackbonesLabels.get(nettedComponent) != null) {
            return (String) ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).get(which);
        } else
            return null;
    }

    /**
     * set the label of a backbone that is contained in a netted component
     *
     * @param nettedComponent which netted component
     * @param which           which backbone
     * @param label           the label for the backbone
     * @return
     */
    public boolean setNettedComponentBackboneLabel(int nettedComponent, int which, String label) {
        if (nettedComponentsBackbonesLabels.get(nettedComponent) != null && ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).get(which) != null) {
            if (!((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).contains(label.trim())) {
                ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).set(which, label.trim());
                return true;
            }
        }
        return false;

    }

    /**
     * get the index of a backbone in the list of backbones within the netted component
     *
     * @param nettedComponent which netted component
     * @param label           the label for the backbone
     * @return
     */
    public int indexOfNettedComponentBackboneLabel(int nettedComponent, String label) {
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            return ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).indexOf(label.trim());
        } else
            return -1;
    }

    /**
     * get the eNewick representation of a backbone contained in a netted component
     *
     * @param nettedComponent which netted component
     * @param which           which backbone
     * @return
     */
    public String getNettedComponentBackbone(int nettedComponent, int which) {
        if (nettedComponentsBackbonesLabels.get(nettedComponent) != null) {
            return (String) ((Vector) nettedComponentsBackbonesStrings.get(nettedComponent)).get(which);
        } else
            return null;
    }

    /**
     * set the eNewick representation of a backbone contained in a netted component
     *
     * @param nettedComponent which netted component
     * @param which           which backbone
     * @param eNewick         the eNewick string
     * @return
     */
    public boolean setNettedComponentBackbone(int nettedComponent, int which, String eNewick) {
        if (nettedComponentsBackbonesLabels.get(nettedComponent) != null && ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).get(which) != null) {
            if ((Integer) activeNettedComponentsBackbones.get(nettedComponent) == which) uptoDate = false;
            if (!((Vector) nettedComponentsBackbonesStrings.get(nettedComponent)).contains(eNewick.trim())) {
                ((Vector) nettedComponentsBackbonesStrings.get(nettedComponent)).set(which, eNewick.trim());
                return true;
            }
        }
        return false;
    }

    /**
     * add a backbone to a netted component
     *
     * @param nettedComponent which netted component
     * @param backboneLabel   the label of the backbone
     * @param eNewick         the eNewick representation
     * @param active          is is the active backbone of the netted component
     * @return
     */
    public boolean addNettedComponentBackbone(int nettedComponent, String backboneLabel, String eNewick, boolean active) {
        //if(verbose)  System.out.println("adding: "+backboneLabel+"\t"+eNewick);
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            Vector bls = (Vector) nettedComponentsBackbonesLabels.get(nettedComponent);
            Vector beNs = (Vector) nettedComponentsBackbonesStrings.get(nettedComponent);
            int lId = bls.indexOf(backboneLabel.trim());
            int nId = beNs.indexOf(eNewick.trim());
            if (lId == -1 && nId == -1) {
                bls.add(backboneLabel.trim());
                beNs.add(eNewick.trim());
                if ((Integer) activeNettedComponentsBackbones.get(nettedComponent) == -1 || active) { // is there a active netted Component set?
                    activeNettedComponentsBackbones.set(nettedComponent, bls.indexOf(backboneLabel.trim()));
                }
                return true;
            } else if (lId != -1) {
                System.err.println("nettedComponent " + nettedComponent + " label allready used: " + backboneLabel);
                return false;
            } else if (nId != -1) {
                System.err.println("nettedComponent " + nettedComponent + " eNewick allready used; " + nettedComponentsBackbonesStrings.get(nId));
                return false;
            } else
                return false;
        } else
            return false;

    }

    /**
     * deletes a backbone from a netted component
     *
     * @param nettedComponent which netted component
     * @param which           which backbone
     * @return
     */
    public boolean deleteNettedComponentBackbone(int nettedComponent, int which) {
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            Vector bls = (Vector) nettedComponentsBackbonesLabels.get(nettedComponent);
            Vector beNs = (Vector) nettedComponentsBackbonesStrings.get(nettedComponent);
            if ((Integer) activeNettedComponentsBackbones.get(nettedComponent) != which)
                if (bls.remove(which) != null && beNs.remove(which) != null) return true;
        }
        return false;
    }

    /**
     * delets a backbone from a netted component
     *
     * @param nettedComponent which netted component
     * @param label           which backbone
     * @return
     */
    public boolean deleteNettedComponentBackbone(int nettedComponent, String label) {
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            int which = ((Vector) nettedComponentsBackbonesLabels.get(nettedComponent)).indexOf(label.trim());
            return deleteNettedComponentBackbone(nettedComponent, which);
        }
        return false;
    }


    // everything for a rootComponent

    /**
     * returns the label of a root component
     *
     * @param which which root component
     * @return
     */
    public String getRootComponentLabel(int which) {
        return (String) rootComponentsLabels.get(which);
    }

    /**
     * set the label of a root component
     *
     * @param which which root component
     * @param label the label
     * @return
     */
    public boolean setRootComponentLabel(int which, String label) {
        if (!rootComponentsLabels.contains(label.trim())) {
            rootComponentsLabels.add(which, label.trim());
            return true;
        } else
            return false;
    }

    /**
     * return the index of the root component
     *
     * @param label which root component
     * @return
     */
    public int indexOfRootComponentLabel(String label) {
        return rootComponentsLabels.indexOf(label.trim());
    }

    /**
     * return the eNewick representation of the root component
     *
     * @param which which root component
     * @return
     */
    public String getRootComponent(int which) {
        return (String) rootComponentsStrings.get(which);
    }

    /**
     * sets the eNewick representation of the root component
     *
     * @param which   which root component
     * @param eNewick the eNewick string
     * @return
     */
    public boolean setRootComponent(int which, String eNewick) {
        if (rootComponentsLabels.get(which) != null) {
            if (which == activeRootComponent) uptoDate = false;
            rootComponentsStrings.set(which, eNewick.trim());
            return true;

        } else
            return false;
    }

    /**
     * add a root component to the list of root components
     *
     * @param label   the label of the new root component
     * @param eNewick the eNewick representation of the root component
     * @param active  is this the active root component
     * @return
     */
    public boolean addRootComponent(String label, String eNewick, boolean active) {
        int lId = rootComponentsLabels.indexOf(label.trim());
        int nId = rootComponentsStrings.indexOf(eNewick.trim());
        if (lId == -1 && nId == -1) {
            rootComponentsLabels.add(label.trim());
            rootComponentsStrings.add(eNewick.trim());
            if (activeRootComponent == -1 || active) activeRootComponent = 1;
            NRootComponents++;
            return true;
        } else if (lId != -1) {
            System.err.println("rootComponent label allready used: " + label);
            return false;
        } else if (nId != -1) {
            System.err.println("rootComponents eNewick allready used; " + rootComponentsLabels.get(nId));
            return false;
        } else
            return false;
    }

    /**
     * deletes a root component from the list
     *
     * @param which which root component
     * @return
     */
    public boolean deleteRootComponent(int which) {
        if (activeRootComponent != which)// do not delete activeRootComponent
            if (rootComponentsLabels.remove(which) != null && rootComponentsStrings.remove(which) != null) {
                NRootComponents--;
                return true;
            }
        return false;
    }

    /**
     * deletes a root component from the list
     *
     * @param label which root component
     * @return
     */
    public boolean deleteRootComponent(String label) {
        int which = rootComponentsLabels.indexOf(label.trim());
        return which != -1 && deleteRootComponent(which);
    }

    /**
     * return the active root component
     *
     * @return
     */
    public int getActiveRootComponent() {
        return activeRootComponent;
    }

    /**
     * set the active root component
     *
     * @param active which root component
     * @return
     */
    public boolean setActiveRootComponent(int active) {
        if (active > 0 && active <= getNRootComponents() && rootComponentsLabels.get(active) != null) {
            activeRootComponent = active;
            uptoDate = false;
            return true;
        } else
            return false;
    }

    /**
     * get the active backbone of the netted component
     *
     * @param nettedComponent which netted component
     * @return
     */
    public int getActiveNettedComponentBackbone(int nettedComponent) {
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            return (Integer) activeNettedComponentsBackbones.get(nettedComponent);
        }
        return -1;
    }

    /**
     * set the active backbone of the netted component
     *
     * @param nettedComponent which netted component
     * @param active          which backbone
     * @return
     */
    public boolean setActiveNettedComponentBackbone(int nettedComponent, int active) {
        if (nettedComponentsLabels.get(nettedComponent) != null) {
            activeNettedComponentsBackbones.set(nettedComponent, active);
            uptoDate = false;
            return true;
        }
        return false;
    }

    /**
     * get the root node of the reticulate network
     *
     * @return
     * @throws Exception
     */
    public Node getRoot() throws Exception {
        if (!uptoDate) getReticulateNetwork();
        return root;
    }

    /**
     * return a array of integers containing the netted components contained in the selected root component (array values start at 1!)
     *
     * @param which which root component
     * @return
     * @throws java.io.IOException
     */

    public int[] getContainedNettedComponentsOfRootComponent(int which) throws IOException {
        TreeSet activeNettedComponents = new TreeSet();
        buildReticulateNetwork(which, activeNettedComponentsBackbones, activeNettedComponents);
        int[] re = new int[activeNettedComponents.size() + 1];
        Object[] o = activeNettedComponents.toArray();
        for (int i = 1; i <= o.length; i++) re[i] = (Integer) o[i - 1];
        return re;
    }

    /**
     * return a array of integers containing the netted components contained in the active root component  array values start at 1!)
     *
     * @return
     */
    public int[] getContainedNettedComponentsOfActiveRootComponent() {
        return containedNettedComponents;
    }

    /**
     * return the phlyograph that represents the reticulate network given its configuration
     *
     * @return
     * @throws java.io.IOException
     */
    public PhyloGraph getReticulateNetwork() throws IOException {
        if (!uptoDate || reticulationNetwork == null) {
            if (NRootComponents == 0) {
                reticulationNetwork = null;
                root = null;
            } else {
                TreeSet activeNettedComponents = new TreeSet();
                reticulationNetwork = buildReticulateNetwork(activeRootComponent, activeNettedComponentsBackbones, activeNettedComponents);
                this.containedNettedComponents = new int[activeNettedComponents.size() + 1];
                Object[] o = activeNettedComponents.toArray();
                for (int i = 1; i <= o.length; i++) this.containedNettedComponents[i] = (Integer) o[i - 1];
                root.checkOwner(reticulationNetwork);
            }
        }
        return reticulationNetwork;
    }

    /**
     * clone the object
     *
     * @return
     */
    public Object clone() {
        try {
            StringWriter sw = new StringWriter();
            this.write(sw, this.getNtax());
            StringReader sr = new StringReader(sw.toString());
            Reticulate re = new Reticulate();
            re.read(new NexusStreamParser(sr), Ntax);
            return re;
        } catch (Exception e) {
            System.err.println("unable to clone Reticulate object : ");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * return the format subclass of this nexus class
     *
     * @return
     */
    public Format getFormat() {
        return format;
    }

    /**
     * The format subclass
     */
    public class Format implements Cloneable {
        private boolean nettedComponents = false;
        private boolean interleaved = false;
        private boolean internalLabels = false;
        private boolean TreeComponentLabels = false;
        private boolean nettedCompLabels = false;

        /**
         * Constructor
         */
        public Format() {
        }

        /**
         * use the interleaved format
         *
         * @return
         */
        public boolean useInterleaved() {
            return interleaved;
        }

        /**
         * @param interleaved
         */
        public void setUseInterleaved(boolean interleaved) {
            this.interleaved = interleaved;
        }

        /**
         * are netted components used
         *
         * @return
         */
        public boolean useNettedComponents() {
            return nettedComponents;
        }

        /**
         * @param nettedComponents
         */
        public void setUseNettedComponents(boolean nettedComponents) {
            this.nettedComponents = nettedComponents;
        }

        /**
         * show internal labels of the graph
         *
         * @return
         */
        public boolean labelInternalLabels() {
            return internalLabels;
        }

        /**
         * if true all internal nodes are labeled
         *
         * @param show
         */
        public void setLabelInternalLabels(boolean show) {
            this.internalLabels = show;
        }

        /**
         * if true the roots of the TreeComponents are labeled
         *
         * @param show
         */
        public void setLabelTreeComponentRoots(boolean show) {
            TreeComponentLabels = show;
        }

        /**
         * @return
         */
        public boolean labelTreeComponentRoots() {
            return TreeComponentLabels;
        }

        /**
         * if true the roots of the netted components are labeled
         *
         * @param show
         */
        public void setLabelNettedComponentsLabels(boolean show) {
            nettedCompLabels = show;
        }

        /**
         * @return
         */
        public boolean labelNettedComponentsLabels() {
            return nettedCompLabels;
        }
    }

    /**
     * IO
     */


    /**
     * write the reticulate block to the given writer
     *
     * @param w    the writer to which the reticulate block should be written
     * @param taxa the nexus taxa object associated with this reticulate object
     * @throws java.io.IOException
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        write(w, taxa.getNtax());
    }

    /**
     * write the reticulate block to the given writer
     *
     * @param w    the writer to which the reticulate block should be written
     * @param nTax the number taxa object associated with this reticulate object
     * @throws IOException
     */
    public void write(Writer w, int nTax) throws IOException {
        w.write("\nBEGIN " + Reticulate.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + getNtax() + " nRootComponents=" + getNRootComponents() + " nNettedComponents=" + getNNettedComponents() + " nTreeComponents=" + getNTreeComponents() + ";\n");
        w.write("FORMAT\n");
        w.write("\tACTIVEROOT=" + getActiveRootComponent() + ";\n");
        if (NNettedComponents > 0) {
            w.write("\tACTIVENETTEDCOMPONENTS=");
            for (int i = 1; i < activeNettedComponentsBackbones.size(); i++)
                w.write("\t" + activeNettedComponentsBackbones.get(i));
            w.write(";\n");
        }
        if (this.format.labelInternalLabels() || this.format.labelTreeComponentRoots() || this.format.labelNettedComponentsLabels()) {
            w.write("\tSHOWLABELS=\t");
            if (format.labelInternalLabels()) w.write("INTERNAL");
            if (format.labelTreeComponentRoots()) w.write("\tTREECOMPONENTS");
            if (format.labelNettedComponentsLabels()) w.write("\tNETTEDCOMPONENTS");
            w.write(";\n");
        }
        w.write("TREECOMPONENTS\n");
        for (int i = 1; i <= getNTreeComponents(); i++)
            w.write("\t" + getTreeComponentLabel(i) + "=" + getTreeComponent(i) + ";\n");
        if (NNettedComponents > 0) {
            w.write("NETTEDCOMPONENTS\n");
            for (int i = 1; i <= getNNettedComponents(); i++) {
                w.write("\t" + getNettedComponentLabel(i) + "=\n");
                if (verbose) System.out.println("nettedcomp Label: " + getNettedComponentLabel(i));
                for (int j = 1; j < ((Vector) nettedComponentsBackbonesLabels.get(i)).size(); j++) {
                    if (verbose)
                        System.out.println(nettedComponentsBackbonesStrings.get(i) + "\n" + nettedComponentsBackbonesLabels.get(i) + "\n" + nettedComponentsBackbonesStrings + "\ti: " + i + "\tj: " + j);
                    if (verbose)
                        System.out.println(nettedComponentsBackbonesLabels.size() + "\t" + nettedComponentsBackbonesStrings.size());
                    w.write("\t\t" + ((Vector) nettedComponentsBackbonesLabels.get(i)).get(j) + "=" + ((Vector) nettedComponentsBackbonesStrings.get(i)).get(j) + ";\n");
                }
                w.write("\t;\n");
            }
        }
        w.write("ROOTCOMPONENTS\n");
        for (int i = 1; i <= getNRootComponents(); i++)
            w.write("\t" + getRootComponentLabel(i) + "=" + getRootComponent(i) + ";\n");
        w.write("END; [" + Reticulate.NAME + "]\n");
    }

    /**
     * read a reticulate nexus block from the given nexus stream parser
     *
     * @param np   the nexus stream parser from which the block should be read
     * @param taxa the nexus taxa object associated with this reticulate object
     * @throws SplitsException
     * @throws IOException
     */
    public void read(NexusStreamParser np, Taxa taxa) throws SplitsException, IOException {
        read(np, taxa.getNtax());
    }

    /**
     * read a reticulate nexus block from the given nexus stream parser
     *
     * @param np   the nexus stream parser from which the block should be read
     * @param nTax the number taxa object associated with this reticulate object
     * @throws SplitsException
     * @throws IOException
     */
    public void read(NexusStreamParser np, int nTax) throws SplitsException, IOException {
        if (verbose) System.out.println("nTax: " + nTax + "\tthis.NTax: " + this.Ntax);
        this.Ntax = nTax;
        np.matchBeginBlock(NAME);
        if (np.peekMatchIgnoreCase("DIMENSIONS")) {
            np.matchIgnoreCase("DIMENSIONS");
            if (np.peekMatchIgnoreCase("ntax=")) {
                np.matchIgnoreCase("ntax=" + getNtax());
            }
            if (np.peekMatchIgnoreCase("nRootComponents=")) {
                np.matchIgnoreCase("nRootComponents=");
                NRootComponents = np.getInt();
                activeRootComponent = 1;
            }
            if (np.peekMatchIgnoreCase("nNettedComponents=")) {
                np.matchIgnoreCase("nNettedComponents=");
                NNettedComponents = np.getInt();
                // init active backbones for each netted component
                for (int i = 1; i <= NNettedComponents; i++) activeNettedComponentsBackbones.add(i, 1);
            }
            if (np.peekMatchIgnoreCase("nTreeComponents=")) {
                np.matchIgnoreCase("nTreeComponents=");
                NTreeComponents = np.getInt();
            }
            np.matchIgnoreCase(";");
        }
        if (verbose)
            System.out.println("NRootComponents: " + NRootComponents + "\tNNettedComponents: " + NNettedComponents + "\tNTreeComponnets: " + NTreeComponents);
        if (np.peekMatchIgnoreCase("FORMAT")) {
            np.matchIgnoreCase("FORMAT");
            if (np.peekMatchIgnoreCase("ACTIVEROOT=")) {
                np.matchIgnoreCase("ACTIVEROOT=");
                activeRootComponent = np.getInt();
                np.matchIgnoreCase(";");
            }
            if (np.peekMatchIgnoreCase("ACTIVENETTEDCOMPONENTS=")) {
                np.matchIgnoreCase("ACTIVENETTEDCOMPONENTS=");
                for (int i = 1; i <= NNettedComponents; i++)
                    activeNettedComponentsBackbones.set(i, np.getInt());
                np.matchIgnoreCase(";");
            }
            if (np.peekMatchIgnoreCase("SHOWLABELS=")) {
                np.matchIgnoreCase("SHOWLABELS=");
                if (np.peekMatchIgnoreCase("INTERNAL")) {
                    np.matchIgnoreCase("INTERNAL");
                    format.setLabelInternalLabels(true);
                }
                if (np.peekMatchIgnoreCase("TREECOMPONENTS")) {
                    np.matchIgnoreCase("TREECOMPONENTS");
                    format.setLabelInternalLabels(true);
                }
                if (np.peekMatchIgnoreCase("NETTEDCOMPONENTS")) {
                    np.matchIgnoreCase("NETTEDCOMPONENT");
                    format.setLabelInternalLabels(true);
                }
                np.matchIgnoreCase(";");
            }
        }
        if (np.peekMatchIgnoreCase("TREECOMPONENTS") && NTreeComponents != 0) {
            np.matchIgnoreCase("TREECOMPONENTS");
            for (int i = 1; i <= NTreeComponents; i++) {
                String name = np.getWordRespectCase();
                name = name.replaceAll("[ \t\b]+", "_");
                name = name.replaceAll("[:;,]+", ".");
                name = name.replaceAll("\\[", "(");
                name = name.replaceAll("\\]", ")");
                np.matchIgnoreCase("=");
                np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
                String eNewick = np.getWordRespectCase();
                np.popPunctuationCharacters();
                np.matchIgnoreCase(";");
                treeComponentsLabels.add(name);
                treeComponentsStrings.add(eNewick);
            }
        } else if (np.peekMatchIgnoreCase("TREECOMPONENTS"))
            throw new SplitsException("Reticulate: tree Component found but NTreeComponents = 0");

        if (np.peekMatchIgnoreCase("NETTEDCOMPONENTS") && NNettedComponents != 0) {
            np.matchIgnoreCase("NETTEDCOMPONENTS");
            for (int i = 1; i <= NNettedComponents; i++) {
                String name = np.getWordRespectCase();
                name = name.replaceAll("[ \t\b]+", "_");
                name = name.replaceAll("[:;,]+", ".");
                name = name.replaceAll("\\[", "(");
                name = name.replaceAll("\\]", ")");
                np.matchIgnoreCase("=");
                // init elements at 0
                Vector backboneNames = new Vector();
                backboneNames.add("");
                Vector backboneeNewick = new Vector();
                backboneeNewick.add("");
                while (!np.peekMatchIgnoreCase(";")) {
                    String backboneName = np.getWordRespectCase();
                    name = name.replaceAll("[ \t\b]+", "_");
                    backboneName = backboneName.replaceAll("[:;,]+", ".");
                    backboneName = backboneName.replaceAll("\\[", "(");
                    backboneName = backboneName.replaceAll("\\]", ")");
                    np.matchIgnoreCase("=");
                    np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
                    String eNewick = np.getWordRespectCase();
                    np.popPunctuationCharacters();
                    np.matchIgnoreCase(";");
                    backboneNames.add(backboneName);
                    backboneeNewick.add(eNewick);
                }
                nettedComponentsLabels.add(name);
                nettedComponentsBackbonesLabels.add(backboneNames);
                nettedComponentsBackbonesStrings.add(backboneeNewick);
                np.matchIgnoreCase(";");
            }
        } else if (np.peekMatchIgnoreCase("NETTEDCOMPONENTS"))
            throw new SplitsException("Reticulate: netted Component found but NnettedComponents = 0");
        if (np.peekMatchIgnoreCase("ROOTCOMPONENTS")) {
            np.matchIgnoreCase("ROOTCOMPONENTS");
            for (int i = 1; i <= NRootComponents; i++) {
                String name = np.getWordRespectCase();
                if (verbose) System.out.println("name: " + name);
                name = name.replaceAll("[ \t\b]+", "_");
                name = name.replaceAll("[:;,]+", ".");
                name = name.replaceAll("\\[", "(");
                name = name.replaceAll("\\]", ")");
                np.matchIgnoreCase("=");
                np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
                String eNewick = np.getWordRespectCase();
                np.popPunctuationCharacters();
                np.matchIgnoreCase(";");
                rootComponentsLabels.add(name);
                rootComponentsStrings.add(eNewick);
            }
        } else {
            if (verbose) System.out.println("word: '" + np.getWordRespectCase() + "'");
            throw new SplitsException("Reticulate: no root components found in nexus block");
        }
        np.matchEndBlock();
        // check that all numbers are correct
        if (NTreeComponents != treeComponentsLabels.size() - 1)
            throw new SplitsException("Expected " + NTreeComponents + " TreeComponents got: " + (treeComponentsLabels.size() - 1));
        else if (NNettedComponents != nettedComponentsLabels.size() - 1)
            throw new SplitsException("Expected " + NNettedComponents + " TreeComponents got: " + (nettedComponentsLabels.size() - 1));
        else if (NRootComponents != rootComponentsLabels.size() - 1)
            throw new SplitsException("Expected " + NRootComponents + " TreeComponents got: " + (rootComponentsLabels.size() - 1));
    }


    // PRIVATE STUFF

    private boolean checkIntegrety() {
        try {
            // for all possible combinations of netted components build the Reticulate Network
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private PhyloGraph buildReticulateNetwork(int rootComponent, Vector activeNettedComponentsBackbones, TreeSet activeNettedComponents) throws IOException {
        // first pasre root component
        String rC = (String) rootComponentsStrings.get(rootComponent);
        HashMap label2Nodes = new HashMap();
        HashSet seen = new HashSet();
        PhyloGraph pg = new PhyloGraph();
        root = parseBracketNotation(rC, label2Nodes, seen, pg, activeNettedComponentsBackbones, activeNettedComponents);
        Iterator it = pg.nodeIterator();
        if (verbose) {
            System.out.println("NODES: ");
            while (it.hasNext()) {
                Node n = (Node) it.next();
                System.out.println(pg.getLabel(n) + "\t" + n);
            }
            System.out.println("-------------------------------------------\n");
            it = pg.edgeIterator();
            while (it.hasNext()) {
                Edge e = (Edge) it.next();
                System.out.println(e);
            }
        }
        root.checkOwner(pg);
        uptoDate = true;
        return pg;
    }

    /**
     *  Stuff for the read
     */

    /**
     * parse a tree in newick format
     *
     * @param str
     * @throws IOException
     */
    private Node parseBracketNotation(String str, HashMap label2Node, HashSet workedLabels, PhyloGraph reticulationNetwork, Vector activeNettedComponentsBackbones, TreeSet activeNettedComponents) throws IOException {
        // we have to read the first node special, its the root and phylograph has no root!!!
        int i = Basic.skipSpaces(str, 0);
        if (str.charAt(i) == '(') {
            HashMap node2internalLabel = new HashMap();
            Node root = reticulationNetwork.newNode();
            i = parseBracketNotationRecursively(1, root, i + 1, str, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
            if (str.charAt(i) != ')')
                throw new IOException("Expected ')' at position " + i);
            i = Basic.skipSpaces(str, i + 1);
            if (i < str.length() && Character.isLetterOrDigit(str.charAt(i))) // must be a internal label
            {
                int i0 = i;
                StringBuilder buf = new StringBuilder();
                while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                    buf.append(str.charAt(i++));
                String label = buf.toString().trim();
                label2Node.put(label, root);
                reticulationNetwork.setLabel(root, label);
                if (label.length() == 0)
                    throw new IOException("Expected label at position " + i0);
            }
            // set the labels if wanted
            for (Object o : node2internalLabel.keySet()) {
                Node n = (Node) o;
                String label = (String) node2internalLabel.get(n);
                if (nettedComponentsLabels.contains(label) && format.labelNettedComponentsLabels()) {
                    reticulationNetwork.setLabel(n, label);
                } else if (treeComponentsLabels.contains(label) && format.labelTreeComponentRoots()) {
                    reticulationNetwork.setLabel(n, label);
                } else if (!nettedComponentsLabels.contains(label) && !treeComponentsLabels.contains(label) && format.labelInternalLabels())
                    reticulationNetwork.setLabel(n, label);
            }
            return root;
        } else {
            // ceck if the string is a netted component
            if (nettedComponentsLabels.contains(str)) {
                int which = nettedComponentsLabels.indexOf(str);
                activeNettedComponents.add(which);

                return parseBracketNotation((String) nettedComponentsBackbonesStrings.get(((Integer) activeNettedComponentsBackbones.get(which)).intValue()), label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents);
            } else
                throw new IOException("String does not start with: '(' and is not a netted component: " + str);
        }
    }


    private static final String punct = "),;:";
    private static final String startOfNumber = "-.0123456789";


    /**
     * recursively do the work
     *
     * @param depth distance from root
     * @param v     parent node
     * @param i     current position in string
     * @param str   string
     * @return new current position
     * @throws IOException
     */
    private int parseBracketNotationRecursively(int depth, Node v, int i, String str, HashMap label2Node, HashSet workedLabels, PhyloGraph reticulationNetwork, Vector activeNettedComponentsBackbones, TreeSet activeNettedComponents, HashMap node2internalLabel) throws IOException {
        try {
            for (i = Basic.skipSpaces(str, i); i < str.length(); i = Basic.skipSpaces(str, i + 1)) {
                Node w = reticulationNetwork.newNode();
                if (str.charAt(i) == '(') {
                    i = parseBracketNotationRecursively(depth + 1, w, i + 1, str, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
                    if (str.charAt(i) != ')')
                        throw new IOException("Expected ')' at position " + i);
                    i = Basic.skipSpaces(str, i + 1);
                    if (i < str.length() && Character.isLetterOrDigit(str.charAt(i))) // must be internal label
                    {
                        int i0 = i;
                        StringBuilder buf = new StringBuilder();
                        while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                            buf.append(str.charAt(i++));
                        String label = buf.toString().trim();
                        if (label.length() == 0)
                            throw new IOException("Expected label at position " + i0);
                        if (workedLabels.contains(label))
                            throw new IOException(" Found label: " + label + ", but label has been used before");
                        if (verbose)
                            System.out.println("found internal label: " + label + "\tknown: " + label2Node.containsKey(label));
                        // a internal label is either
                        // 1.) a netted component
                        // 2.) a unuique tree component
                        // 3.) a label for the subtree connected to the reticulation
                        // 4.) just a label
                        if (nettedComponentsLabels.contains(label)) {  //@todo test this
                            int which = nettedComponentsLabels.indexOf(label);
                            activeNettedComponents.add(which);
                            int active = (Integer) activeNettedComponentsBackbones.get(which);
                            String nettedString = (String) ((Vector) nettedComponentsBackbonesStrings.get(which)).get(active);
                            parseBracketNotationRecursively(0, w, 0, nettedString, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
                            workedLabels.add(label); // nettedCOmponents are unqiue
                        }
                        if (treeComponentsLabels.contains(label) && !label2Node.containsKey(label)) { //@todo test this
                            String treeComponent = (String) treeComponentsStrings.get(treeComponentsLabels.indexOf(label));
                            parseBracketNotationRecursively(0, w, 0, treeComponent, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
                            workedLabels.add(label);  // this treeComponent must be unique
                        } else if (!treeComponentsLabels.contains(label) && !nettedComponentsLabels.contains(label) && label2Node.containsKey(label)) {
                            // we have seen this label before and have now found the subtree
                            Node leaf = (Node) label2Node.get(label);
                            if (leaf.getDegree() == 1) {
                                Node parent = leaf.getAdjacentNodes().next();
                                Edge e = reticulationNetwork.newEdge(parent, w);
                                e.setInfo(parent.getCommonEdge(w).getInfo());
                                reticulationNetwork.setWeight(e, reticulationNetwork.getWeight(parent.getCommonEdge(w)));
                                reticulationNetwork.setConfidence(e, reticulationNetwork.getConfidence(parent.getCommonEdge(w)));
                                leaf.deleteNode();
                                workedLabels.add(label); // label has been worked with twice!!
                            } else
                                throw new IOException("found internal label: " + label + "\t but it is allready used");
                        } else if (!treeComponentsLabels.contains(label) && !nettedComponentsLabels.contains(label)) {
                            // we found the internal label, but for the first time!
                            label2Node.put(label, w);
                        } else {
                            if (nettedComponentsLabels.contains(label))
                                throw new IOException("nettedComponent contains internal label: " + label + "\t but the label is not unique");
                            if (treeComponentsLabels.contains(label))
                                throw new IOException("treeComponent contains internal label: " + label + "\t and the label is not unique");
                        }
                        node2internalLabel.put(w, label);

                    }
                } else // everything to next ) : or , is considered a label:
                {
                    if (reticulationNetwork.getNumberOfNodes() == 1)
                        throw new IOException("Expected '(' at position " + i);
                    int i0 = i;
                    StringBuilder buf = new StringBuilder();
                    while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                        buf.append(str.charAt(i++));
                    String label = buf.toString().trim();
                    if (verbose)
                        System.out.println("found label: " + label + "\tknown: " + label2Node.containsKey(label));
                    if (label.length() == 0)
                        throw new IOException("Expected label at position " + i0);
                    if (workedLabels.contains(label))
                        throw new IOException(" Found label: " + label + ", but label has been used before");
                    if (label2Node.containsKey(label)) {  // there is a subtree known to this label, it is a reticulation
                        w.deleteNode();
                        w = (Node) label2Node.get(label);// w becomes the node allready in the graph
                        workedLabels.add(label);
                    } else {
                        // check if the label is a nettedComponent
                        if (nettedComponentsLabels.contains(label)) {
                            int which = nettedComponentsLabels.indexOf(label);
                            activeNettedComponents.add(which);
                            int active = (Integer) activeNettedComponentsBackbones.get(which);
                            String nettedString = (String) ((Vector) nettedComponentsBackbonesStrings.get(which)).get(active);
                            parseBracketNotationRecursively(0, w, 0, nettedString, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
                            workedLabels.add(label); // nettedCOmponents are unqiue
                            node2internalLabel.put(w, label);

                        } // check if the label is a not yet seen TREECOMPONENTs
                        else if (treeComponentsLabels.contains(label)) {
                            String treeComponent = (String) treeComponentsStrings.get(treeComponentsLabels.indexOf(label));
                            parseBracketNotationRecursively(0, w, 0, treeComponent, label2Node, workedLabels, reticulationNetwork, activeNettedComponentsBackbones, activeNettedComponents, node2internalLabel);
                            label2Node.put(label, w);  // a treeComponent may occur twice
                            node2internalLabel.put(w, label);
                        } else {//this is a tree leaf
                            reticulationNetwork.setLabel(w, label);
                        }
                    }
                }


                Edge e = null;
                if (v != null)
                    try {
                        e = reticulationNetwork.newEdge(v, w);
                        //if(retiuclateEdge) graph.setLabel(e,"reticulateEdge");
                    } catch (IllegalSelfEdgeException e1) {
                        Basic.caught(e1);
                    }

                // detect and read embedded bootstrap values:
                i = Basic.skipSpaces(str, i);
                if (i < str.length() && startOfNumber.indexOf(str.charAt(i)) >= 0) // edge weight is following
                {
                    int i0 = i;
                    StringBuilder buf = new StringBuilder();
                    while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                        buf.append(str.charAt(i++));
                    String number = buf.toString().trim();
                    try {
                        double weight = Double.parseDouble(number);
                        if (e != null)
                            reticulationNetwork.setConfidence(e, weight / 100.0);
                    } catch (Exception ex) {
                        throw new IOException("Expected number at position " + i0 + " (got: '" + number + "')");
                    }
                }

                // read edge weights
                if (i < str.length() && str.charAt(i) == ':') // edge weight is following
                {
                    i = Basic.skipSpaces(str, i + 1);
                    int i0 = i;
                    StringBuilder buf = new StringBuilder();
                    while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                        buf.append(str.charAt(i++));
                    String number = buf.toString().trim();
                    try {
                        double weight = Double.parseDouble(number);
                        if (e != null)
                            reticulationNetwork.setWeight(e, weight);
                    } catch (Exception ex) {
                        throw new IOException("Expected number at position " + i0 + " (got: '" + number + "')");
                    }
                }
                // now i should be pointing to a ',', a ')'  or
                if (i >= str.length()) {
                    if (depth == 0)
                        return i; // finished parsing tree
                    else
                        throw new IOException("Unexpected end of line");
                }
                if (str.charAt(i) == ';' && depth == 0)
                    return i; // finished parsing tree
                else if (str.charAt(i) == ')')
                    return i;
                else if (str.charAt(i) != ',')
                    throw new IOException("Unexpected '" + str.charAt(i)
                            + "' at position " + i);
            }
        } catch (NotOwnerException ex) {
            throw new IOException(ex.getMessage());
        }
        return -1;
    }


    /**
     * stuff for the write
     */


    private String recMakeNewick(Node start, PhyloGraph reticulationNetwork, HashMap node2rTaxaName, HashMap rTaxaName2subtree, Vector labels, Vector rNodes) throws IOException {
        StringBuilder subtree = new StringBuilder();
        Iterator it = start.getAdjacentNodes();
        boolean first = true;
        while (it.hasNext()) {
            Node next = (Node) it.next();
            Edge e = start.getCommonEdge(next);
            if (e.getSource().equals(start)) {
                // check for leaf
                if (first)
                    first = false;
                else
                    subtree.append(",");
                if (next.getOutDegree() == 0) { // leaf
                    if (verbose) System.out.println("is leaf: " + next);
                    subtree.append(reticulationNetwork.getLabel(next)).append(":").append(reticulationNetwork.getWeight(e));
                } else if (next.getInDegree() == 1) { // tree edge
                    if (verbose) System.out.println("is tree edge to node: " + next);
                    subtree.append(recMakeNewick(next, reticulationNetwork, node2rTaxaName, rTaxaName2subtree, labels, rNodes)).append(":").append(reticulationNetwork.getWeight(e));
                } else if (next.getInDegree() == 2) { // reticulation edge
                    if (verbose) System.out.println("is reticulation edge to node: " + next);
                    subtree.append(recMakeNewick(next, reticulationNetwork, node2rTaxaName, rTaxaName2subtree, labels, rNodes)).append(":").append(reticulationNetwork.getWeight(e));
                } else // no reticulation network
                    throw new IOException("no reticulation network");
            }
        }
        if (start.getInDegree() < 2) {
            if (verbose) System.out.println("returning subtree: " + "(" + subtree.toString() + ")");
            return "(" + subtree.toString() + ")";
        } else if (start.getInDegree() == 2) {
            String rLabel = (String) labels.get(rNodes.indexOf(start));
            if (verbose) System.out.println("new reticulation node with label: " + rLabel + " and subtree: " + subtree);
            node2rTaxaName.put(start, rLabel);
            rTaxaName2subtree.put(rLabel, subtree.toString());
            if (verbose) System.out.println("returning subtree: " + rLabel);
            return rLabel;
        } else {
            if (verbose)
                System.out.println("returning wrong subtree because node: " + start + "\t has indegree: " + reticulationNetwork.getInDegree(start) + ": " + subtree);
            throw new IOException("no reticulation network");

        }

    }


}
