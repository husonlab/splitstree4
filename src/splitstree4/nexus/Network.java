/*
 * Network.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree4.nexus;

import jloda.graph.*;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.NodeView;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.BasicSwing;
import jloda.util.Basic;
import jloda.util.IteratorUtils;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.TaxaSet;
import splitstree4.util.Interval;
import splitstree4.util.NetworkUtilities;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.*;

/**
 * NexusBlock network class
 */
public class Network extends NexusBlock {
    private int ntax = 0, nvertices = 0, nedges = 0;
    /**
     * Identification string
     */
    final public static String NAME = "Network";
    private Draw draw = null;

    final private Map<Integer, List<String>> translate = new TreeMap<>(); // maps nodes to lists of taxalabels
    private VertexDescription[] vertices = null;
    private EdgeDescription[] edges = null;

    static final Map<String, Font> fonts = new HashMap<>(); // hash all fonts

    private String newick = null; // keep a Newick representation if tree

    /**
     * state of the network is circular
     */
    final public static String CIRCULAR = "circular";
    /**
     * state of the network is rectilinear
     */
    final public static String RECTILINEAR = "rectilinear";
    private String layout = CIRCULAR;

	/**
	 * drawing switches
	 * todo: these belong in assumptions
	 */
	public static class Draw {
		private boolean toScale = true;
		private float hoffset = 0;
		private float voffset = 0;
		private int hflip = 0;
		private int vflip = 0;
		private double rotate = 0;
		private float zoom = -1;

		boolean modifyShowNodeNames = false;
		boolean modifyShowNodeIds = false;
        boolean modifyShowEdgeWeights = false;
        boolean modifyShowEdgeIds = false;
        boolean modifyShowEdgeConfidences = false;
        boolean modifyShowEdgeIntervals = false;
        public boolean modifyConfidenceEdgeWidth = false;
        public boolean modifyConfidenceEdgeShading = false;


        public boolean isToScale() {
            return toScale;
        }

        public void setToScale(boolean toScale) {
            this.toScale = toScale;
        }

        public float getHoffset() {
            return hoffset;
        }

        public void setHoffset(float hoffset) {
            this.hoffset = hoffset;
        }

        public float getVoffset() {
            return voffset;
        }

        public void setVoffset(float voffset) {
            this.voffset = voffset;
        }

        public int getHFlip() {
            return hflip;
        }

        public void setHflip(int hflip) {
            this.hflip = hflip;
        }

        public int getVFlip() {
            return vflip;
        }

        public void setVflip(int vflip) {
            this.vflip = vflip;
        }

        public double getRotate() {
            return rotate;
        }

        public void setRotate(double rotate) {
            this.rotate = rotate;
        }

        public float getZoom() {
            return zoom;
        }

        public void setZoom(float zoom) {
            this.zoom = zoom;
        }

        public void read(NexusStreamParser np) throws IOException {
            List<String> tokens = np.getTokensLowerCase("draw", ";");

            toScale = np.findIgnoreCase(tokens, "to_scale", true, toScale);
            toScale = np.findIgnoreCase(tokens, "equal_edges", false, toScale);

            String taxlabels = np.findIgnoreCase(tokens, "taxlabels=", "name id both none", null);
            if (taxlabels != null)   // instruct syncNetwork2PhyloView to modify node labels
            {
                modifyShowNodeNames = (taxlabels.equalsIgnoreCase("name")
                                       || taxlabels.equalsIgnoreCase("both"));
                modifyShowNodeIds = (taxlabels.equalsIgnoreCase("id")
                        || taxlabels.equalsIgnoreCase("both"));
            }
            String splitlabels = np.findIgnoreCase(tokens, "splitlabels=", "id weight confidence interval none", null);
            if (splitlabels != null)   // instruct syncNetwork2PhyloView to modify edge labels
            {
                modifyShowEdgeWeights = splitlabels.equalsIgnoreCase("weight");
                modifyShowEdgeIds = splitlabels.equalsIgnoreCase("id");
                modifyShowEdgeConfidences = splitlabels.equalsIgnoreCase("confidence");
                modifyShowEdgeIntervals = splitlabels.equalsIgnoreCase("interval");
            }

            String confidenceRendering = np.findIgnoreCase(tokens, "showconfidence=",
                    "none edgewidth edgeshading", null);
            if (confidenceRendering != null) {
                modifyConfidenceEdgeWidth = confidenceRendering.equalsIgnoreCase("edgewidth");
                modifyConfidenceEdgeShading = confidenceRendering.equalsIgnoreCase("edgeshading");
            }


            hoffset = (float) np.findIgnoreCase(tokens, "hoffset=", -1000000, 1000000, hoffset);
            voffset = (float) np.findIgnoreCase(tokens, "voffset=", -1000000, 1000000, voffset);
            hflip = np.findIgnoreCase(tokens, "hflip=", 0, 1, hflip);
            vflip = np.findIgnoreCase(tokens, "vflip=", 0, 1, vflip);
            rotate = np.findIgnoreCase(tokens, "rotateAbout=", -1000, 1000, rotate);
            //  if(np.find(f,"'zoom=auto'"))
            //    zoom= -1;
            zoom = (int) np.findIgnoreCase(tokens, "zoom=", -1000000, 1000000, zoom);

            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens +
                        "' unexpected in DRAW");
        }

        public void write(Writer w) throws IOException {
            w.write("DRAW");
            w.write(isToScale() ? " to_scale" : " equal_edges");
            if (getHoffset() != 0)
                w.write(" hoffset=" + getHoffset());
            if (getVoffset() != 0)
                w.write(" voffset=" + getVoffset());
            if (getHFlip() != 0)
                w.write(" hflip=1");
            if (getVFlip() != 0)
                w.write(" vflip=1");
            if (getRotate() != 0)
                w.write(" rotateAbout=" + (float) getRotate());
            if (getZoom() != -1)
                w.write(" zoom=" + getZoom());
            w.write(";\n");
        }
    }

    /**
     * Construct a new Network object.
     */
    public Network() {
        super();
        draw = new Draw();
    }

    /**
     * Construct a new Network object from a given PhyloGraphView
     *
     * @param graphView the PhyloGraphView
     */
    public Network(Taxa taxa, PhyloGraphView graphView) {
        this();
        syncPhyloGraphView2Network(taxa, graphView);
    }

    /**
     * gets the draw block
     *
     * @return the draw block
     */
    public Draw getDraw() {
        return draw;
    }

    /**
     * sets the draw block
     *
     */
    public void setDraw(Draw draw) {
        this.draw = draw;
    }

    /**
     * Get the number of taxa.
     *
     * @return number of taxa
     */
    public int getNtax() {
        return ntax;
    }

    /**
     * Set the number of taxa
     *
     * @param ntax the number of taxa
     */
    public void setNtax(int ntax) {
        this.ntax = ntax;
    }

    /**
     * Get the number of nvertices
     *
     * @return nvertices the number of vertices.
     */
    public int getNvertices() {
        return nvertices;
    }

    /**
     * Set the number of nvertices
     *
     * @param n the number of vertices.
     */
    public void setNvertices(int n) {
        this.nvertices = n;
        vertices = new VertexDescription[n + 1];
    }

    public VertexDescription[] getVertices() {
        return vertices;
    }

    /**
     * Get the number of edges
     *
     * @return nedges the number of edges
     */
    public int getNedges() {
        return nedges;
    }

    /**
     * Set the number of edges
     *
     * @param n the number of edges
     */
    public void setNedges(int n) {
        this.nedges = n;
        edges = new EdgeDescription[n + 1];
    }

    public EdgeDescription[] getEdges() {
        return edges;
    }

    /**
     * gets the node to taxalabels  map
     *
     * @return the node to taxalabels map
     */
    public Map getTranslate() {
        return translate;
    }

    /**
     * gets the list of taxon labels that a node translates into
     *
     * @param nodeId the id of the node
     * @return list of taxon labels or null
     */
    public List<String> getTranslate(int nodeId) {
        return translate.get(nodeId);
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * sets the node to taxalabels map
     *
     * @param taxa        the taxon block
     * @param nodeId      the integer id of the node
     * @param taxonLabels the labels of the taxa as strings or ids
     */
    public void setTranslate(Taxa taxa, int nodeId, List<Integer> taxonLabels) {
        List<String> labels = new LinkedList<>();
        if (taxonLabels != null) {
            for (Integer t : taxonLabels) {
                if (t > 0 && t <= taxa.getNtax())
                    labels.add(taxa.getLabel(t));
            }
        }
        translate.put(nodeId, labels);
    }

    /**
     * Writes a network object in nexus format
     *
     * @param w    a writer
     * @param taxa the taxa
     */
    public void write(Writer w, Taxa taxa) throws java.io.IOException {
        w.write("\nBEGIN " + Network.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + getNtax() + " nvertices=" + getNvertices()
                + " nedges=" + getNedges() + ";\n");

        draw.write(w);

        if (!getLayout().equalsIgnoreCase(CIRCULAR)) {
            w.write("LAYOUT " + getLayout() + ";\n");
        }

        w.write("TRANSLATE\n");
        for (Integer key : translate.keySet()) {
            List<String> labels = translate.get(key);
            if (labels != null && labels.size() > 0) {
                w.write("" + key);
                for (String label : labels) {
                    w.write(" '" + label + "'");
                }
                w.write(",\n");
            }
        }
        w.write(";\n");

        w.write("VERTICES\n");
        for (int i = 1; i <= getNvertices(); i++)
            w.write(vertices[i].toString() + ",\n");
        w.write(";\n");

        boolean hasVertexLabel = false;
        for (int i = 1; !hasVertexLabel && i <= getNvertices(); i++) {
			if (vertices[i].label != null) {
				hasVertexLabel = true;
				break;
			}
        }
        if (hasVertexLabel) {
            String font = null;
            w.write("VLABELS\n");
            for (int i = 1; i <= getNvertices(); i++)
                if (vertices[i].label != null && vertices[i].label.length() > 0) {
                    w.write(vertices[i].labelToString(font) + ",\n");
                    font = vertices[i].font;
                }
            w.write(";\n");
        }

        w.write("EDGES\n");
        for (int i = 1; i <= getNedges(); i++) {
            w.write(edges[i].toString() + ",\n");

        }
        w.write(";\n");

        boolean hasEdgeLabel = false;
        for (int i = 1; !hasEdgeLabel && i <= getNedges(); i++)
			if (edges[i].label != null) {
				hasEdgeLabel = true;
				break;
			}
        if (hasEdgeLabel) {
            String font = null;
            w.write("ELABELS\n");
            for (int i = 1; i <= getNedges(); i++)
                if (edges[i].label != null) {
                    w.write(edges[i].labelToString(font) + ",\n");
                    font = edges[i].font;
                }
            w.write(";\n");
        }

        boolean hasInternal = false;
        for (int i = 1; !hasInternal && i <= getNedges(); i++)
			if (edges[i].internal != null) {
				hasInternal = true;
				break;
			}

        if (hasInternal) {
            w.write("INTERNAL\n");
            for (int i = 1; i <= getNedges(); i++)
                if (edges[i].internal != null)
                    w.write(edges[i].internalToString() + ",\n");
            w.write(";\n");
        }

        w.write("END; [" + Network.NAME + "]\n");
    }

    /**
     * Reads a splits object in NexusBlock format
     *
     * @param np   nexus stream parser
     * @param taxa the taxa
     */
    public void read(NexusStreamParser np, Taxa taxa) throws IOException {
        getTranslate().clear();

        if (np.peekMatchBeginBlock("st_graph")) // read old graphs
            np.matchBeginBlock("st_graph");
        else
            np.matchBeginBlock(NAME);

        if (getNvertices() == 0 || np.peekMatchIgnoreCase("dimensions")) {
            np.matchIgnoreCase("dimensions ntax=");
            setNtax(np.getInt());
            if (getNtax() != taxa.getNtax())
                throw new IOException("line " + np.lineno() + ": st_graph: ntax=" + getNtax() + " wrong");
            if (np.peekMatchIgnoreCase("nsplits=")) {
                System.err.println("Importing old SGRAPH-format (4Beta1-3)");
                readOld(np);
                return;
            }
            np.matchIgnoreCase("nvertices=");
            setNvertices(np.getInt());
            np.matchIgnoreCase("nedges=");
            setNedges(np.getInt());
            np.matchIgnoreCase(";");
        }

        if (np.peekMatchIgnoreCase("draw")) {
            draw.read(np);
            if (np.peekMatchEndBlock()) {
                np.matchEndBlock();
                return;
            }
        }
        if (np.peekMatchIgnoreCase("layout")) {
            np.matchIgnoreCase("layout");
            if (np.peekMatchIgnoreCase(CIRCULAR))
                setLayout(CIRCULAR);
            else if (np.peekMatchIgnoreCase(RECTILINEAR))
                setLayout(RECTILINEAR);
            np.matchAnyTokenIgnoreCase(CIRCULAR + " " + RECTILINEAR);
            np.matchIgnoreCase(";");
        }

        if (np.peekMatchIgnoreCase("translate")) {
            np.matchIgnoreCase("translate");
            while (!np.peekMatchIgnoreCase(";")) {
                Integer ii = np.getInt();
                while (!np.peekMatchIgnoreCase(",")) {
                    String label = np.getWordRespectCase();
                    if (!translate.containsKey(ii))
                        translate.put(ii, new LinkedList<String>());
                    translate.get(ii).add(label);
                }
                np.matchIgnoreCase(",");
            }
            np.matchIgnoreCase(";");
        }

        np.matchIgnoreCase("vertices");
        for (int i = 1; i <= getNvertices(); i++) {
            vertices[i] = new VertexDescription();
            vertices[i].read(i, getNvertices(), np);
        }
        np.matchIgnoreCase(";");

        int doRecompute = 0;
        if (np.peekMatchIgnoreCase("recompute;")) {
            np.matchIgnoreCase("recompute;");
            doRecompute = 100;
        } else if (np.peekMatchIgnoreCase("recompute runs=")) {
            np.matchIgnoreCase("recompute runs=");
            doRecompute = np.getInt(0, 10000);
            np.matchIgnoreCase(";");
        }

        if (np.peekMatchIgnoreCase("vlabels")) {
            np.matchIgnoreCase("vlabels");
            String font = null;
            while (!np.peekMatchRespectCase(";")) {
                int id = np.getInt();
                vertices[id].readLabel(np, font);
                font = vertices[id].font;
            }
            np.matchIgnoreCase(";");
        }

        np.matchIgnoreCase("edges");
        for (int i = 1; i <= getNedges(); i++) {
            edges[i] = new EdgeDescription();
            edges[i].read(i, getNedges(), np);
        }
        np.matchIgnoreCase(";");

        if (np.peekMatchIgnoreCase("elabels")) {
            np.matchIgnoreCase("elabels");
            String font = null;
            while (!np.peekMatchRespectCase(";")) {
                int id = np.getInt();
                edges[id].readLabel(np, font);
                font = edges[id].font;
            }
            np.matchIgnoreCase(";");
        }

        if (np.peekMatchIgnoreCase("internal")) {
            np.matchIgnoreCase("internal");
            while (!np.peekMatchRespectCase(";")) {
                int id = np.getInt();
                edges[id].readInternal(np);
            }
            np.matchIgnoreCase(";");
        }

        if (doRecompute > 0) {
            NetworkUtilities.computeEmbedding(taxa, this, doRecompute);
        }

        np.matchEndBlock();
    }

    /**
     * Reads a network object in OLD 4beta-1-3 NexusBlock format
     *
     * @param np nexus stream parser
     */
    public void readOld(NexusStreamParser np) throws IOException {

        np.matchIgnoreCase("nsplits=");
        int nsplits = np.getInt();
        np.matchIgnoreCase("nvertices=");
        setNvertices(np.getInt());
        for (int i = 1; i <= getNvertices(); i++)
            vertices[i] = new VertexDescription();
        np.matchIgnoreCase("nedges=");
        setNedges(np.getInt());
        np.matchIgnoreCase(";");

        boolean formatLabels = false;
        boolean formatWeights = false;
        if (np.peekMatchIgnoreCase("format")) {
            List<String> tokens = np.getTokensLowerCase("format", ";");

            formatLabels = np.findIgnoreCase(tokens, "no labels", false, formatLabels);
            formatLabels = np.findIgnoreCase(tokens, "labels", true, formatLabels);

            formatWeights = np.findIgnoreCase(tokens, "no weights", false, formatWeights);
            formatWeights = np.findIgnoreCase(tokens, "weights", true, formatWeights);

            if (tokens.size() != 0)
                throw new IOException("line " + np.lineno() + ": `" + tokens +
                        "' unexpected in FORMAT");
        }

        if (np.peekMatchIgnoreCase("draw")) {
            //List f = np.getTokensLowerCase("draw", ";");
            System.err.println("Skipping DRAW");
        }

        np.matchIgnoreCase("nodelabeloffsets");

        while (!np.peekMatchRespectCase(";")) {
            int vi = np.getInt(1, nvertices);

            VertexDescription vd = vertices[vi];
            vd.labelOffset = new Point(np.getInt(), np.getInt());
            np.matchRespectCase(",");
        }
        np.matchRespectCase(";");

        np.matchIgnoreCase("translate");
        while (!np.peekMatchIgnoreCase(";")) {
            Integer ii = np.getInt();
            while (!np.peekMatchIgnoreCase(",")) {
                String label = np.getWordRespectCase();
                if (!translate.containsKey(ii))
                    translate.put(ii, new LinkedList<String>());
                translate.get(ii).add(label);
            }
            np.matchIgnoreCase(",");
        }
        np.matchIgnoreCase(";");

        np.matchIgnoreCase("vertices");
        for (int i = 1; i <= getNvertices(); i++) {
            np.matchIgnoreCase("" + i);
            VertexDescription vd = vertices[i];
            vd.id = i;

            vd.x = (float) np.getDouble();
            vd.y = (float) np.getDouble();
            np.matchIgnoreCase(",");
        }
        np.matchIgnoreCase(";");

        np.matchIgnoreCase("edges");
        int ei = 1;
        int splitId = 0;
        while (!np.peekMatchIgnoreCase(";")) {
            if (formatLabels) {
                splitId = np.getInt();
                if (splitId <= 0 || splitId > nsplits)
                    throw new IOException("line " + np.lineno() + ": " + splitId + " split id out of range");
            } else
                splitId++;

            float weight = 1;
            if (formatWeights) {
                weight = (float) np.getDouble();
            }


            while (!np.peekMatchIgnoreCase(",")) {
                np.matchIgnoreCase("(");
                EdgeDescription ed = edges[ei] = new EdgeDescription();
                ed.id = ei;
                ed.eclass = splitId;
                int v = np.getInt();
                if (v <= 0 || v > getNvertices())
                    throw new IOException("line " + np.lineno() + ": v=" + v + ": out of range");

                np.matchIgnoreCase(",");
                int w = np.getInt();
                if (w <= 0 || w > getNvertices())
                    throw new IOException("line " + np.lineno() + ": w=" + w + ": out of range");

                ed.source = v;
                ed.target = w;
                ed.weight = weight;
                np.matchIgnoreCase(")");
                ei++;
            }
            np.matchIgnoreCase(",");
        }
        np.matchIgnoreCase(";");
        modifyNodeLabels(true, false, null, null, false);

        np.matchEndBlock();
    }

    /**
     * Produces a string representation of a NexusBlock object
     *
     * @param taxa the taxa block
     * @return object in nexus format
     */
    public String toString(Taxa taxa) {
        StringWriter w = new StringWriter();
        try {
            write(w, taxa);
        } catch (IOException e) {
            Basic.caught(e);
        }
        return w.toString();
    }

    /**
     * Show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN NETWORK;");
        ps.println("\tDIMENSIONS NTAX=number-taxa NVERTICES=number-vertices NEDGES=number-edges;");
        ps.println("\t[DRAW");
        // ps.println("\t    [TAXALABELS={NAME|ID|BOTH|NONE}]");
        // ps.println("\t    [SPLITLABELS={WEIGHT|ID|CONFIDENCE|NONE}]");
        // ps.println("\t    [SHOWCONFIDENCE={NONE|EDGEWIDTH|EDGESHADING}]");
        // ps.println("\t    [{TO_SCALE | EQUAL_EDGES}]");
        // ps.println("\t    [HOFFSET=horizontal-offset]");
        // ps.println("\t    [VOFFSET=vertical-offset]");
        // ps.println("\t    [HFLIP=horizontal-flip]");
        // ps.println("\t    [VFLIP=vertical-flip]");
        ps.println("\t    [ROTATE=rotation]");
        // ps.println("\t    [ZOOM={zoom-factor | AUTO}]");
        ps.println("\t;]");
        ps.println("\tLAYOUT={CIRCULAR|RECTILINEAR} [rooted];");
        ps.println("\t[TRANSLATE");
        ps.println("\t    [vertex_1 taxon_1,");
        ps.println("\t    vertex_2 taxon_2,");
        ps.println("\t    ...");
        ps.println("\t    vertex_ntax taxon_ntax,]");
        ps.println("\t;]");
        ps.println("\tVERTICES");
        ps.println("\t     1 x_1 y_1 [W=n] [H=n] [S={R|O|N}]" +
                "[C=color] [B=color] [L=n],");
        ps.println("\t     2 x_2 y_2 [W=n] [H=n] [S={R|O|N}]" +
                "[C=color] [B=color] [L=n],");
        ps.println("\t     ...");
        ps.println("\t     nvertices x_nvertices y_nvertices [W=n] [H=n] [S={R|O|N}] [C=color] [B=color] [L=n],");
        ps.println("\t;");
        ps.println("\t[RECOMPUTE [RUNS=number-of-iterations];]\n");
        ps.println("\t[VLABELS");
        ps.println("\t    vertex_id label [X= xoffset Y=yoffset] [C=color] [F=font],");
        ps.println("\t    ...");
        ps.println("\t    vertex_id label [X= xoffset Y=yoffset] [C=color] [F=font],");
        ps.println("\t;]");
        ps.println("\tEDGES");
        ps.println("\t    1 vertex_id vertex_id [S=n] [C=color] [L=n],");
        ps.println("\t    2 vertex_id vertex_id [S=n] [C=color] [L=n],");
        ps.println("\t    ...");
        ps.println("\t    nedges vertex_id vertex_id [S=n] [C=color] [L=n],");
        ps.println("\t;");
        ps.println("\t[ELABELS");
        ps.println("\t    edge_id label [X= xoffset Y=yoffset] [C=color] [F=font],");
        ps.println("\t    ...");
        ps.println("\t    edge_id label [X= xoffset Y=yoffset] [C=color] [F=font],");
        ps.println("\t;]");
        ps.println("\t[INTERNAL");
        ps.println("\t    edge_id [x y] [x y] ...,");
        ps.println("\t    ...");
        ps.println("\t    edge_id [x y] [x y] ...,");
        ps.println("\t;]");
        ps.println("\t[SHAPES");
        ps.println("\t...");
        ps.println("\t;]");
        ps.println("END;");

    }

    public String toString() {
        return ("[" + NAME + " ntax=") + ntax + " nvertices=" + nvertices + " nedges=" + nedges + "]\n";
    }

    /**
     * syncronizes the phylograph object to the Network representation of the graph
     */
    public void syncPhyloGraphView2Network(Taxa taxa, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();

        // getDraw().setZoom(graphView.trans.getZoom());
        getDraw().setRotate(graphView.trans.getAngle());
        getDraw().setHflip(graphView.trans.getFlipH() ? 1 : 0);
        getDraw().setVflip(graphView.trans.getFlipV() ? 1 : 0);

        ntax = taxa.getNtax();
        nvertices = graph.getNumberOfNodes();
        nedges = graph.getNumberOfEdges();

        translate.clear();
        vertices = new VertexDescription[nvertices + 1];
        edges = new EdgeDescription[nedges + 1];

        NodeIntArray node2id = new NodeIntArray(graph);

        int vi = 1;
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            NodeView nv = graphView.getNV(v);
            Point2D p = nv.getLocation();
            VertexDescription vd = vertices[vi] = new VertexDescription();
            vd.id = vi;
            vd.v = v;

            vd.x = (float) p.getX();
            vd.y = (float) p.getY();
            vd.line = graphView.getLineWidth(v);
            vd.width = graphView.getWidth(v);
            vd.height = graphView.getHeight(v);
            switch (nv.getShape()) {
                case NodeView.NONE_NODE:
                    vd.setShape(VertexDescription.NONE_NODE);
                    break;
                case NodeView.RECT_NODE:
                    vd.setShape(VertexDescription.RECT_NODE);
                    break;
                default:
                case NodeView.OVAL_NODE:
                    vd.setShape(VertexDescription.OVAL_NODE);
                    break;
            }

            if (nv.getFont() != null)
                vd.font = BasicSwing.getCode(nv.getFont());
            else
                vd.font = BasicSwing.getCode(graphView.getFont());
            if (vd.font != null && vd.font.length() > 0) {
                fonts.put(vd.font, nv.getFont());
            }
            node2id.set(v, vi);
            if (nv.getColor() != null && !nv.getColor().equals(VertexDescription.FGC)) {
                vd.fgc = nv.getColor();
            }
            if (nv.getBackgroundColor() != null) {
                vd.bgc = nv.getBackgroundColor();
            }
            if (nv.getLabelColor() != null && !nv.getLabelColor().equals(VertexDescription.FGC)) {
                vd.labelFgc = nv.getLabelColor();
            }
            if (nv.getLabelBackgroundColor() != null) {
                vd.labelBgc = nv.getLabelBackgroundColor();
            }
            setTranslate(taxa, vi, IteratorUtils.asList(graph.getTaxa(v)));

            if (nv.isLabelVisible() && nv.getLabel() != null && nv.getLabel().length() > 0) {
                vd.label = nv.getLabel();
                vd.labelOffset = nv.getLabelOffset();
                vd.labelLayout = nv.getLabelLayout();
                vd.labelAngle = nv.getLabelAngle();
            }
            // overwrite with graph label, if present...
            else if (nv.isLabelVisible() && graph.getLabel(v) != null && graph.getLabel(v).length() > 0) {
                vd.label = graph.getLabel(v);
                nv.setLabel(vd.label); // TODO: fix this
            }

            vi++;
        }
        int ei = 1;
        for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
            EdgeView ev = graphView.getEV(e);
            int sid = graph.getSplit(e);
            EdgeDescription ed = edges[ei] = new EdgeDescription();
            ed.id = ei;
            ed.e = e;
            ed.eclass = sid;
            ed.source = node2id.get(graph.getSource(e));
            ed.target = node2id.get(graph.getTarget(e));
            ed.weight = (float) graph.getWeight(e);
            ed.internal = graphView.getInternalPoints(e);
            ed.line = graphView.getLineWidth(e);
            ed.label = ev.getLabel();
            if (ev.isLabelVisible() && ed.label != null && ed.label.equals("null"))
                Basic.caught(new Exception("null"));

            ed.labelOffset = ev.getLabelOffset();
            ed.labelLayout = ev.getLabelLayout();
            ed.labelAngle = ev.getLabelAngle();
            if (ev.getFont() != null)
                ed.font = BasicSwing.getCode(ev.getFont());
            if (ed.font != null && ed.font.length() > 0) {
                fonts.put(ed.font, ev.getFont());
            }
            if (ev.getColor() != null && !ev.getColor().equals(EdgeDescription.FGC)) {
                ed.fgc = ev.getColor();
            }
            if (ev.getLabelColor() != null && !ev.getLabelColor().equals(EdgeDescription.FGC)) {
                ed.labelFgc = ev.getLabelColor();
            }
            if (ev.getLabelBackgroundColor() != null) {
                ed.labelBgc = ev.getLabelBackgroundColor();
            }
            if (ev.isLabelVisible() && (ed.label == null || ed.label.length() == 0) && graph.getLabel(e) != null && graph.getLabel(e).length() > 0) {
                ed.label = graph.getLabel(e);
                if (ed.label != null && ed.label.equals("null"))
                    Basic.caught(new Exception("null"));
                ev.setLabel(ed.label); // fix this
            }
            ei++;
        }
        newick = graphView.getNewick(true);
        setValid(true);
    }

    /**
     * syncronizes the network to the PhyloGraphView
     *
     */
    public void syncNetwork2PhyloGraphView(Taxa taxa, Splits splits, PhyloGraphView graphView) {
        PhyloSplitsGraph graph = graphView.getPhyloGraph();
        graph.clear();
        boolean allowMove = true; // disallow, if edges contain internal points

        graphView.trans.setAngle(getDraw().getRotate());
        graphView.trans.setFlipH(getDraw().getHFlip() != 0);
        graphView.trans.setFlipV(getDraw().getVFlip() != 0);

        for (int vi = 1; vi <= getNvertices(); vi++) {
            Node v = graph.newNode();
            VertexDescription vd = vertices[vi];
            vd.v = v;
            NodeView nv = graphView.getNV(v);
            nv.setLocation(new Point2D.Float(vd.x, vd.y));
            nv.setLineWidth((byte) vd.line);
            nv.setWidth(vd.width);
            nv.setHeight(vd.height);
            switch (vd.getShape()) {
                case VertexDescription.NONE_NODE:
                    nv.setShape(NodeView.NONE_NODE);
                    break;
                case VertexDescription.RECT_NODE:
                    nv.setShape(NodeView.RECT_NODE);
                    break;
                default:
                case VertexDescription.OVAL_NODE:
                    nv.setShape(NodeView.OVAL_NODE);
                    break;
            }

            if (vd.fgc != null && !vd.fgc.equals(VertexDescription.FGC)) {
                nv.setColor(vd.fgc);
            }
            if (vd.bgc != null) {
                nv.setBackgroundColor(vd.bgc);
            }
            if (vd.labelFgc != null && !vd.labelFgc.equals(VertexDescription.FGC)) {
                nv.setLabelColor(vd.fgc);
            }
            if (vd.labelBgc != null) {
                nv.setLabelBackgroundColor(vd.bgc);
            }
            if (vd.labelOffset != null)
                nv.setLabelPositionRelative(vd.labelOffset);
            nv.setLabelAngle(vd.labelAngle);
            nv.setLabelLayout(vd.labelLayout);


            if (vd.label != null) {
                graph.setLabel(v, vd.label);
                nv.setLabel(vd.label);
            }

            if (vd.font != null) {
                Font font = fonts.get(vd.font);
                if (font == null)
                    font = Font.decode(vd.font);
                fonts.put(vd.font, font);
                nv.setFont(font);
            }
            List<String> labels = getTranslate(vi);
            if (labels != null) {
                // graph.setLabels(v, labels);
                for (String label : labels) {
                    int t = taxa.indexOf(label);
                    if (t > 0) {
                        graph.addTaxon(v, t);
                    } else
                        System.err.println("taxon " + label + " no t");
                }
            }
            // set info for tool tip text:
            if (graph.getTaxa(v) != null) {
                StringBuilder buf = new StringBuilder();
                for (Integer t : graph.getTaxa(v)) {
                    String info = (taxa.getInfo(t) != null ? taxa.getInfo(t) : taxa.getLabel(t) + " (" + t + ")");
                    if (buf.toString().length() > 0)
                        buf.append(", ");
                    buf.append(info);
                }
                String info = buf.toString().trim();
                if (info.length() > 45)
                    info = info.substring(0, 40) + "...";
                if (info.length() > 0)
                    graph.setInfo(v, info);
            }
        }

        for (int ei = 1; ei <= getNedges(); ei++) {
            EdgeDescription ed = edges[ei];
            Edge e = null;
            try {
                e = graph.newEdge(vertices[ed.source].v, vertices[ed.target].v);
            } catch (IllegalSelfEdgeException e1) {
                Basic.caught(e1);
            }
            ed.e = e;

            EdgeView ev = graphView.getEV(e);
            ev.setLineWidth((byte) ed.line);
            ev.setShape(ed.shape);

            if (ed.fgc != null && !ed.fgc.equals(EdgeDescription.FGC)) {
                ev.setColor(ed.fgc);
                ev.setLabelColor(ed.fgc);
            }
            if (ed.labelFgc != null && !ed.labelFgc.equals(VertexDescription.FGC)) {
                ev.setLabelColor(ed.fgc);
            }
            if (ed.labelBgc != null) {
                ev.setLabelBackgroundColor(ed.bgc);
            }
            graph.setSplit(e, ed.eclass);
            if (ed.label != null) {
                graph.setLabel(e, ed.label);
                ev.setLabel(ed.label);
                ev.setLabelVisible(true);
            } else
                ev.setLabel(null);

            if (ed.font != null) {
                Font font = fonts.get(ed.font);
                if (font == null)
                    font = Font.decode(ed.font);
                fonts.put(ed.font, font);
                ev.setFont(font);
            }
            if (ed.labelOffset != null)
                ev.setLabelPositionRelative(ed.labelOffset);
            ev.setLabelLayout(ed.labelLayout);
            ev.setLabelAngle(ed.labelAngle);

            if (ed.internal != null) {
                graphView.setInternalPoints(e, ed.internal);
                allowMove = false;
            }
            if (ed.weight != EdgeDescription.WEIGHT)
                graph.setWeight(e, ed.weight);

            //graphView.setDirection(e,EdgeView.DIRECTED);
        }
        // if requested to show names or ids, do so
        if (getDraw().modifyShowNodeNames || getDraw().modifyShowNodeIds) {
            modifyNodeLabels(getDraw().modifyShowNodeNames, getDraw().modifyShowNodeIds,
                    taxa, graphView, false);
            getDraw().modifyShowNodeNames = false;
            getDraw().modifyShowNodeIds = false;

        }
        if (getDraw().modifyShowEdgeWeights || getDraw().modifyShowEdgeIds
                || getDraw().modifyShowEdgeConfidences || getDraw().modifyShowEdgeIntervals) {
            modifyEdgeLabels(getDraw().modifyShowEdgeWeights, getDraw().modifyShowEdgeIds,
                    getDraw().modifyShowEdgeConfidences, getDraw().modifyShowEdgeIntervals, splits, graphView, false);
            getDraw().modifyShowEdgeWeights = false;
            getDraw().modifyShowEdgeIds = false;
            getDraw().modifyShowEdgeConfidences = false;
            getDraw().modifyShowEdgeIntervals = false;
        }
        if (getDraw().modifyConfidenceEdgeWidth || getDraw().modifyConfidenceEdgeShading) {
            EdgeIntArray widths = new EdgeIntArray(graph);
            EdgeArray colors = new EdgeArray(graph);
            getEdgeConfidenceHightlighting(getDraw().modifyConfidenceEdgeWidth, getDraw().modifyConfidenceEdgeShading,
                    splits, graphView, false, widths, colors);
            applyWidthsColors(graphView, widths, colors);
            getDraw().modifyConfidenceEdgeWidth = false;
            getDraw().modifyConfidenceEdgeShading = false;
        }

        // todo: comment out the next line to allow moving in rectilinear graphs
        graphView.setAllowMoveNodes(allowMove);
    }

    /**
     * syncs the viewer's respresentation of node labels after a change
     *
     * @param GV the viewer
     */
    public void syncNetworkToNodeLabels(PhyloGraphView GV) {
        if (GV == null || GV.getPhyloGraph() == null)
            return;

        final PhyloSplitsGraph G = GV.getPhyloGraph();

        try {
            for (int vi = 1; vi <= getNvertices(); vi++) {
                VertexDescription vd = vertices[vi];
                GV.setLabel(vd.v, vd.label);
                G.setLabel(vd.v, vd.label);
                GV.setLabelColor(vd.v, vd.labelFgc);
                GV.setLabelBackgroundColor(vd.v, vd.labelBgc);

            }
        } catch (NotOwnerException ex) {
            jloda.util.Basic.caught(ex);
        }
    }

    /**
     * syncs the viewer's respresentation of edge labels after a change
     *
     * @param GV the viewer
     */
    public void syncNetworkToEdgeLabels(PhyloGraphView GV) {
        if (GV == null || GV.getPhyloGraph() == null)
            return;

        final PhyloSplitsGraph G = GV.getPhyloGraph();

        try {
            for (int ei = 1; ei <= getNedges(); ei++) {
                EdgeDescription ed = edges[ei];
                Edge e = ed.e;

                if (ed.label != null && ed.label.length() > 0) {
                    GV.setLabel(e, ed.label);
                    GV.setLabelVisible(e, true);
                    GV.setLabelColor(e, ed.labelFgc);
                    GV.setLabelBackgroundColor(e, ed.labelBgc);
                } else {
                    GV.setLabel(e, null);
                    GV.setLabelVisible(e, false);
                }

                GV.setColor(e, ed.fgc);
                GV.setLineWidth(e, ed.line);
                GV.setShape(e, ed.shape);
            }
        } catch (NotOwnerException ex) {
            jloda.util.Basic.caught(ex);
        }
    }

    /**
     * set taxa labels to show names or ids or both
     *
     * @param selectedOnly apply only to selected nodes?
     */
    public void modifyNodeLabels(boolean showNames, boolean showIDs, Taxa taxa, PhyloGraphView graphView, boolean selectedOnly) {
        for (int vi = 1; vi <= getNvertices(); vi++) {
            try {
                VertexDescription vd = vertices[vi];
                if (graphView != null && selectedOnly && !graphView.getSelected(vd.v))
                    continue;

                vd.label = null;
                if (showNames || showIDs) {
                    java.util.List labels = getTranslate(vi);
                    if (labels != null) {
                        StringBuilder buf = new StringBuilder();
                        Iterator it = labels.iterator();
                        boolean first = true;
                        while (it.hasNext()) {
                            String name = (String) it.next();
                            if (showNames) {
                                if (first)
                                    first = false;
                                else
                                    buf.append(", ");
                                buf.append(name);
                            }
                            if (showIDs) {
                                if (first)
                                    first = false;
                                else
                                    buf.append(" ");
                                buf.append("(").append(taxa.indexOf(name)).append(")");
                            }
                        }
                        vd.label = buf.toString();
                    }
                }
                if (graphView != null)
                    graphView.setLabel(vd.v, vd.label);
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        }
    }

    /**
     * modify edge labels
     *
     * @param graphView      the PhyloGraphView
     * @param selectedOnly   apply only to selected edges?
     */
    public void modifyEdgeLabels(boolean showWeight, boolean showEClass, boolean showConfidence,
                                 boolean showInterval, Splits splits, PhyloGraphView graphView, boolean selectedOnly) {
        BitSet seen = new BitSet(); // avoid giving different edges of the same class a label

        for (int ei = 1; ei <= getNedges(); ei++) {
            EdgeDescription ed = edges[ei];

            try {
                if (graphView != null && selectedOnly && !graphView.getSelected(ed.e))
                    continue;

                String label = "";
                int count = 0;
                if (showEClass && ed.eclass != 0) {
                    if (count > 0)
                        label += ", ";
                    label += "" + ed.eclass;
                    count++;
                }
                if (showWeight) {
                    if (count > 0)
                        label += ", ";
                    if (ed.weight - (int) ed.weight == 0)
                        label += "" + ((int) ed.weight);
                    else
                        label += "" + ed.weight;
                    count++;
                }

                if (splits != null && showInterval && ed.eclass > 0 && ed.eclass <= splits.getNsplits()) {
                    if (count > 0)
                        label += ", ";
                    Interval interval = splits.getInterval(ed.eclass);
                    if (interval != null) {
                        if (count > 0)
                            label += ", ";
                        label += "[" + interval.low + "," + interval.high + "]";
                    }
                }
                if (splits != null && showConfidence && ed.eclass > 0 && ed.eclass <= splits.getNsplits()) {
                    if (count > 0)
                        label += ", ";
                    double confidence = ((int) (1000 * splits.getConfidence(ed.eclass))) / 10.0;
                    if (confidence - (int) confidence == 0)
                        label += "" + ((int) confidence);
                    else
                        label += "" + confidence;
                    count++;
                }
                if (label != null && label.length() > 0 && (ed.eclass <= 0 || !seen.get(ed.eclass))) {
                    ed.label = label;
                    graphView.getPhyloGraph().setLabel(ed.e, ed.label);
                    graphView.setLabel(ed.e, ed.label);
                    graphView.setLabelVisible(ed.e, true);
                    if (ed.eclass > 0)
                        seen.set(ed.eclass);
                } else {
                    ed.label = null;
                    graphView.getPhyloGraph().setLabel(ed.e, ed.label);
                    graphView.setLabel(ed.e, ed.label);
                    graphView.setLabelVisible(ed.e, false);
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        }
    }

    /**
     * get widths and colors for edge confidence rendering
     *
     * @param widths       will return the new edge widths here
     * @param colors       will return the new edge colors here
     */
    public void getEdgeConfidenceHightlighting(boolean edgeWidth, boolean edgeShading, Splits splits, PhyloGraphView graphView, boolean selectedOnly, EdgeIntArray widths, EdgeArray<Color> colors) {

        if (splits == null || graphView == null)
            return;

        for (int ei = 1; ei <= getNedges(); ei++) {
            EdgeDescription ed = edges[ei];

            if (selectedOnly && !graphView.getSelected(ed.e))
                continue;
            if (ed.eclass > 0 && ed.eclass <= splits.getNsplits()) {
                if (edgeShading) {
                    float confidence = splits.getConfidence(ed.eclass);
                    float c = (float) (1.0 - confidence);
                    ed.fgc = new Color(c, c, c);
                } else
                    ed.fgc = EdgeDescription.FGC;
                colors.put(ed.e, ed.fgc);
                if (edgeWidth) {
                    float confidence = splits.getConfidence(ed.eclass);
                    if (confidence <= 0.5)
                        ed.line = 1;
                    else if (confidence <= 0.8)
                        ed.line = 2;
                    else
                        ed.line = 3;
                } else
                    ed.line = EdgeDescription.LINE;
                widths.set(ed.e, ed.line);
            }
        }
    }

    /**
     * apply edge widths and colors
     *
     * @param widths to apply to edges
     * @param colors to apply to edges
     */
    public void applyWidthsColors(PhyloGraphView graphView, EdgeIntArray widths, EdgeArray colors) {

        if (graphView == null)
            return;
        for (int ei = 1; ei <= getNedges(); ei++) {
            EdgeDescription ed = edges[ei];

            if (widths.get(ed.e) != null) {
                ed.line = widths.getInt(ed.e);
                graphView.setLineWidth(ed.e, ed.line);
            }
            if (colors.get(ed.e) != null) {
                ed.fgc = (Color) colors.get(ed.e);
                graphView.setColor(ed.e, ed.fgc);
            }
        }
    }

    /**
     * hide some taxa
     *
     */
    public void hideTaxa(Taxa origTaxa, TaxaSet exTaxa) {
        System.err.println("hideTaxa for Network: not implemented");
    }

    /**
     * if network is phylogenetic tree, return the tree
     *
     * @return tree
     */
    public String getNewick() {
        return newick;
    }

    /**
     * creates the taxon 2 vertex description map for the document to keep
     */
    public void updateTaxon2VertexDescriptionMap(Map<String, VertexDescription> taxon2VertexDescription) {
        for (int vi = 0; vi < vertices.length; vi++) {
            VertexDescription vd = vertices[vi];
            List<String> taxLabels = getTranslate(vi);
            if (taxLabels != null && taxLabels.size() != 0) {
                for (String label : taxLabels) {
                    VertexDescription vd2 = (VertexDescription) vd.clone();
                    taxon2VertexDescription.put(label, vd2);
                }
            }
        }
    }

    /**
     * apply a created taxon2 vertex description map
     *
     */
    public void applyTaxon2VertexDescription(Map taxon2VertexDescription) {
        // this is where the program "remembers" user modifications to labels
        for (int vi = 0; vi < vertices.length; vi++) {
            List<String> taxLabels = getTranslate(vi);
            if (taxLabels != null && taxLabels.size() != 0) {
                for (String label : taxLabels) {
                    if (label != null && label.length() > 0) {
                        VertexDescription vd2 = (VertexDescription) taxon2VertexDescription.get(label);
                        if (vd2 != null) {
                            vd2 = (VertexDescription) vd2.clone();
                            VertexDescription vd = vertices[vi];
                            vd2.x = vd.x;   // don't use positions from map
                            vd2.y = vd.y;
                            vd2.labelOffset = vd.labelOffset;

                            vd2.labelLayout = vd.labelLayout;
                            vd2.line = vd.line;
                            vd2.fgc = vd.fgc;
                            vd2.bgc = vd.bgc;
                            vd2.labelFgc = vd.labelFgc;
                            vd2.labelBgc = vd.labelBgc;
                            vd2.shape = vd.shape;
                            vd2.width = vd.width;
                            vd2.height = vd.height;
                            /* we never keep old label, so don't even use this test...
                            if (Basic.countOccurrences(vd2.label, ',') != Basic.countOccurrences(vertices[vi].label, ','))
                            */
                            vd2.label = vertices[vi].label; // number of commas has changed, don't keep old label
                            vertices[vi] = vd2;
                        }
                    }
                }
            }
        }
    }
}
