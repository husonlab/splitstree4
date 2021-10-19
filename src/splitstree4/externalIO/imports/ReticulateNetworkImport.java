/**
 * ReticulateNetworkImport.java
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
package splitstree4.externalIO.imports;

import jloda.graph.Edge;
import jloda.graph.IllegalSelfEdgeException;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloSplitsGraph;
import jloda.util.Basic;
import jloda.util.StringUtils;
import splitstree4.core.SplitsException;
import splitstree4.nexus.Network;
import splitstree4.nexus.Reticulate;
import splitstree4.nexus.Taxa;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: 26.01.2006
 * Time: 22:09:27
 * To change this template use File | Settings | File Templates.
 */
public class ReticulateNetworkImport extends FileFilter implements Importer {

    static final boolean EXPERT = true;

    String datatype = null;
    public String Description = "Reticulate Network (*.rnet)";


    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return blockName.equalsIgnoreCase(Network.NAME);
    }


    /**
     * can we import this data?
     *
     * @param input0
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input0) throws IOException {
        if (false) return false;
        BufferedReader input = new BufferedReader(input0);
        String aline = input.readLine();
        return false;
    }


    /**
     * convert input into nexus format
     *
     * @param input
     * @return
     */
    public String apply(Reader input) throws Exception {
        // importing first tree and generating taxa object
        String str;
        BufferedReader br = new BufferedReader(input);
        HashSet labels = new HashSet();
        HashMap knownSubtrees = new HashMap();
        HashMap knownLabel2Node = new HashMap();
        String aLine;
        // we read only one reticulation network, since it may be interleaved or sequential or something inbetween,
        // we first buid the network and than parse it to the nexus format
        Vector reticulationLabels = new Vector();
        Vector reticulationNodes = new Vector();
        Node root = null;
        PhyloSplitsGraph backbone = new PhyloSplitsGraph();
        boolean found = false;
        while ((aLine = br.readLine()) != null) {
            if (aLine.trim().length() == 0 || aLine.startsWith("#"))
                continue; // skip empty lines and comment lines
            //System.err.println("Tree: " + aLine);
            str = "";
            while (aLine != null && (!aLine.contains(";"))) {
                str += aLine;
                aLine = br.readLine();
            }
            if (aLine != null) str += aLine;
            System.out.println(str);
            if (str.trim().length() != 0) {
				str = str.replaceAll(" ", "").replaceAll("\t", "");
				str = StringUtils.removeComments(str, '[', ']');
                // if str contains a "=" its a specification of a suptree
                System.out.println("reading: " + str);
                if (str.contains("=")) {
                    System.out.println("reading subtree");
                    readInSubtree(knownSubtrees, str, backbone, knownLabel2Node, reticulationLabels, reticulationNodes);
                } else {
                    System.out.println("reading reticulate network");
                    root = parseBracketNotation(str, backbone, knownSubtrees);
                }

            }
        }
        System.out.println("Done with readin\n\n");
        // DFS through the network and check for a node that has indegree 2  (reticulation)
        // this node has to be a value in knwonLabel2Node. Take the Newick string of the subtree as a
        // subnetwork into the Nexus Object and replace the two edges leading to it with a node that has  the same label as the internal label.
        HashMap subnetworks = new HashMap();
        ArrayList taxons = new ArrayList();
        String rootComponent = makeNewickRec(root, null, backbone, knownLabel2Node, subnetworks, taxons);

        Taxa taxa = new Taxa();
        Iterator it = taxons.iterator();
        while (it.hasNext()) {
            String label = (String) it.next();
            System.out.println("found taxon label: " + label);
            taxa.add(label);
        }

        Reticulate ret = new Reticulate(taxa);
        it = subnetworks.keySet().iterator();
        while (it.hasNext()) {
            String label = (String) it.next();
            String eNewick = (String) subnetworks.get(label);
            ret.addTreeComponent(label, eNewick);
        }
        ret.addRootComponent("bone1", rootComponent, true);
        StringWriter sw = new StringWriter();
        taxa.write(sw);
        ret.write(sw, taxa);
        System.out.println("Imported:\n#NEXUS\n" + sw.toString());
        return sw.toString();
    }

    private String makeNewickRec(Node start, Edge inEdge, PhyloSplitsGraph backbone, HashMap knownLabel2Node, HashMap subnetworks, ArrayList taxons) throws SplitsException {
        if (backbone.getInDegree(start) == 2 && knownLabel2Node.containsValue(start)) {// found subnetwork
            String label = backbone.getLabel(start);
            System.out.println("found reticulation: " + label + "\t outdegree: " + backbone.getOutDegree(start));
            if (backbone.getOutDegree(start) > 0) {
                StringBuilder subString = new StringBuilder();
                boolean first = true;
                for (Edge f = start.getFirstAdjacentEdge(); f != null; f = start.getNextAdjacentEdge(f)) {
                    if (f != inEdge && f.getSource().equals(start)) {
                        if (first)
                            first = false;
                        else
                            subString.append(",");
                        subString.append(makeNewickRec(start.getOpposite(f), f, backbone, knownLabel2Node, subnetworks, taxons));
                    }
                }
                subnetworks.put(label, subString.toString());
                System.out.println("adding subnetwork: " + label + "\t'" + subString.toString() + "'");
            } else {
                subnetworks.put(label, "");
                taxons.add(label); // this is a leaf
                System.out.println("adding subnetwork: " + label + "\t''");
            }
            // modify graph
            for (Edge e : start.adjacentEdges()) {
                if (e.getTarget().equals(start) && !e.equals(inEdge)) {
                    Node source = e.getSource();
                    Node newTarget = backbone.newNode();
                    backbone.setLabel(newTarget, label);
                    Edge newE = backbone.newEdge(source, newTarget);
                    backbone.setWeight(newE, backbone.getWeight(e));
                    e.deleteEdge();
                }
            }
            String re = label + ":" + (float) (backbone.getWeight(inEdge));
            System.out.println("returning Ret: " + re);
            return re;
        } else if (backbone.getInDegree(start) == 2) {
            throw new SplitsException("Reticulate Network import failed: unknown internal node with inDegree =2: " + backbone.getLabel(start));
        } else {

            StringBuilder subString = new StringBuilder();
            //System.out.println("start: "+start+"\tdegree: "+backbone.getDegree(start)+"\tinEdge: "+inEdge);
            if (backbone.getDegree(start) > 1 || inEdge == null) {
                subString.append("(");
                boolean first = true;
                for (Edge f = start.getFirstAdjacentEdge(); f != null; f = start.getNextAdjacentEdge(f)) {
                    //System.out.println("f: "+f);
                    if (f != inEdge && f.getSource().equals(start)) {
                        if (first)
                            first = false;
                        else
                            subString.append(",");
                        subString.append(makeNewickRec(start.getOpposite(f), f, backbone, knownLabel2Node, subnetworks, taxons));
                    }
                }
                subString.append(")");
            }
            if (backbone.getLabel(start) != null && backbone.getLabel(start).length() > 0) {
                if (backbone.getInDegree(start) == 1 && !subnetworks.containsKey(backbone.getLabel(start))) {
                    taxons.add(backbone.getLabel(start)); // this is a leaf
                }
                subString.append(backbone.getLabel(start));
            }
            if (inEdge != null) {
                subString.append(":").append((float) (backbone.getWeight(inEdge)));
            }
            System.out.println("returning Tree: " + subString.toString());
            return subString.toString();
        }
    }


    public void readInSubtree(HashMap knownSubtrees, String toRead, PhyloSplitsGraph graph, HashMap knownLabel2Node, Vector reticulationLabels, Vector reticulationNodes) throws Exception {
        String key = toRead.substring(0, toRead.indexOf("=")).trim();
        toRead = toRead.substring(toRead.indexOf("=") + 1);
        Node root;
        if (knownLabel2Node.get(key) == null) {
            root = graph.newNode();
            reticulationNodes.add(root);
            reticulationLabels.add(key);
            knownLabel2Node.put(key, root);
        } else
            root = (Node) knownLabel2Node.get(key);
        parseBracketNotationRecursively(new HashMap(), 0, root, 0, toRead, graph, knownLabel2Node);
        knownSubtrees.put(key, root);
    }


    /**
     * parse a tree in newick format
     *
     * @param str
     * @throws IOException
     */
    public Node parseBracketNotation(String str, PhyloSplitsGraph graph, HashMap knownSubtrees) throws IOException {
        Map seen = new HashMap();
        // we have to tread the first node special, its the root and phylograph has no root!!!
        int i = StringUtils.skipSpaces(str, 0);
        if (str.charAt(i) == '(') {
            Node root = graph.newNode();
            i = parseBracketNotationRecursively(seen, 1, root, i + 1, str, graph, knownSubtrees);
            if (str.charAt(i) != ')')
                throw new IOException("Expected ')' at position " + i);
			i = StringUtils.skipSpaces(str, i + 1);
            if (i < str.length() && Character.isLetterOrDigit(str.charAt(i))) // must be a internal label
            {
                int i0 = i;
                StringBuilder buf = new StringBuilder();
                while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                    buf.append(str.charAt(i++));
                String label = buf.toString().trim();
                seen.put(label, root);
                graph.setLabel(root, label);
                if (label.length() == 0)
                    throw new IOException("Expected label at position " + i0);
            }
            return root;
        } else
            throw new IOException("String does not start with: '(': " + str);
    }

    private static final String punct = "),;:";
    private static final String startOfNumber = "-.0123456789";


    /**
     * recursively do the work
     *
     * @param seen  set of seen labels
     * @param depth distance from root
     * @param v     parent node
     * @param i     current position in string
     * @param str   string
     * @return new current position
     * @throws IOException
     */
    public int parseBracketNotationRecursively(Map seen, int depth, Node v, int i, String str, PhyloSplitsGraph graph, HashMap knownSubtrees) throws IOException {
        try {
			for (i = StringUtils.skipSpaces(str, i); i < str.length(); i = StringUtils.skipSpaces(str, i + 1)) {
				Node w = graph.newNode();
				if (str.charAt(i) == '(') {
					i = parseBracketNotationRecursively(seen, depth + 1, w, i + 1, str, graph, knownSubtrees);
					if (str.charAt(i) != ')')
						throw new IOException("Expected ')' at position " + i);
					i = StringUtils.skipSpaces(str, i + 1);
					if (i < str.length() && Character.isLetterOrDigit(str.charAt(i))) // must be internal label
					{
						int i0 = i;
						StringBuilder buf = new StringBuilder();
                        while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                            buf.append(str.charAt(i++));
                        String label = buf.toString().trim();
                        System.out.println("found internal label: " + label + "\tknown: " + knownSubtrees.containsKey(label));
                        if (knownSubtrees.containsKey(label)) {
                            // graph inner label is not unique??
                            w.deleteNode();      //@todo what about the subrtee??
                            w = (Node) knownSubtrees.get(label);
                        } else {
                            System.out.println("found new internal label: " + label);
                            knownSubtrees.put(label, w);
                        }
                        graph.setLabel(w, label);
                        if (label.length() == 0)
                            throw new IOException("Expected label at position " + i0);
                    }
                } else // everything to next ) : or , is considered a label:
                {
                    if (graph.getNumberOfNodes() == 1)
                        throw new IOException("Expected '(' at position " + i);
                    int i0 = i;
                    StringBuilder buf = new StringBuilder();
                    while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                        buf.append(str.charAt(i++));
                    String label = buf.toString().trim();
                    System.out.println("new node: " + label);
                    System.out.println("found label: " + label + "\tknown: " + knownSubtrees.containsKey(label));
                    if (knownSubtrees.containsKey(label)) // label is allready known
                    {
                        w.deleteNode();
                        w = (Node) knownSubtrees.get(label);
                    } else
                        knownSubtrees.put(label, w);
                    graph.setLabel(w, label); //@todo no need this later??
                    if (label.length() == 0)
                        throw new IOException("Expected label at position " + i0);
                }
                Edge e = null;
                if (v != null)
                    try {
                        e = graph.newEdge(v, w);
                        //if(retiuclateEdge) graph.setLabel(e,"reticulateEdge");
                    } catch (IllegalSelfEdgeException e1) {
                        Basic.caught(e1);
                    }

                // detect and read embedded bootstrap values:
				i = StringUtils.skipSpaces(str, i);
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
                            graph.setConfidence(e, weight / 100.0);
                    } catch (Exception ex) {
                        throw new IOException("Expected number at position " + i0 + " (got: '" + number + "')");
                    }
                }

                // read edge weights
                if (i < str.length() && str.charAt(i) == ':') // edge weight is following
                {
					i = StringUtils.skipSpaces(str, i + 1);
                    int i0 = i;
                    StringBuilder buf = new StringBuilder();
                    while (i < str.length() && punct.indexOf(str.charAt(i)) == -1)
                        buf.append(str.charAt(i++));
                    String number = buf.toString().trim();
                    try {
                        double weight = Double.parseDouble(number);
                        if (e != null)
                            graph.setWeight(e, weight);
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
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("rnet"))
                        return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        return false;
    }

    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {
        List extensions = new LinkedList();
        extensions.add("rnet");
        return extensions;
    }

    /**
     * @return description of file matching the filter
     */
    public String getDescription() {
        return Description;
    }


    /**
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     * last ".")
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
