/**
 * EdgeDescription.java
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
package splitstree4.nexus;

import jloda.graph.Edge;
import jloda.swing.graphview.EdgeView;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.LinkedList;

/**
 * all the properties that a edge might have
 *
 * @author Daniel Huson and David Bryant
 * @version $Id: EdgeDescription.java,v 1.16 2010-02-23 15:52:01 huson Exp $
 */
public class EdgeDescription implements Cloneable {
    static int ECLASS = 0; // undefined
    static int LINE = 1;
    static Color FGC = Color.black;
    static Color BGC = Color.white;
    static String FONT = "Default-PLAIN-10";
    static Point OFFSET = new Point(0, 0);
    static float WEIGHT = 1.0f;
    static byte SHAPE = EdgeView.POLY_EDGE;

    int id;
    int source;
    int target;
    java.util.List<Point2D> internal; // list of internal points
    int eclass = ECLASS;
    float weight = WEIGHT;
    int line = LINE;
    Color fgc = FGC;
    Color bgc = BGC;
    Color labelFgc = FGC;
    Color labelBgc = null;

    Point labelOffset = OFFSET;
    byte labelLayout = EdgeView.USER;
    byte shape = SHAPE;
    float labelAngle = 0;
    String font = FONT;
    String label = null;

    Edge e;

    public int getEclass() {
        return eclass;
    }

    public float getWeight() {
        return weight;
    }

    public int getTarget() {
        return target;
    }

    public int getSource() {
        return source;
    }

    public int getId() {
        return id;
    }

    public int getLine() {
        return line;
    }

    public byte getShape() {
        return shape;
    }

    public Color getFgc() {
        return fgc;
    }

    public Color getBgc() {
        return bgc;
    }

    public Color getLabelFgc() {
        return labelFgc;
    }

    public Color getLabelBgc() {
        return labelBgc;
    }

    public String getFont() {
        return font;
    }

    public String getLabel() {
        return label;
    }

    public Point getLabelOffset() {
        return labelOffset;
    }

    public byte getLabelLayout() {
        return labelLayout;
    }

    public float getLabelAngle() {
        return labelAngle;
    }

    public void setLabelAngle(float labelAngle) {
        this.labelAngle = labelAngle;
    }

    /**
     * read an edge description
     *
     * @param id     edges id
     * @param nedges number of edges
     * @param np
     * @throws java.io.IOException
     */
    void read(int id, int nedges, NexusStreamParser np) throws IOException {
        np.matchIgnoreCase("" + id);
        this.id = id;
        source = np.getInt();
        target = np.getInt();

        java.util.List<String> tokens = np.getTokensLowerCase(null, ",");
        line = np.findIgnoreCase(tokens, "line=", 0, 100, line);
        line = np.findIgnoreCase(tokens, "l=", 0, 100, line);

        eclass = np.findIgnoreCase(tokens, "eclass=", -10000, 10000, eclass);
        eclass = np.findIgnoreCase(tokens, "s=", -10000, 10000, eclass);

        shape = (byte) np.findIgnoreCase(tokens, "shape=", 0, 10, shape);
        shape = (byte) np.findIgnoreCase(tokens, "h=", 0, 10, shape);


        weight = (float) np.findIgnoreCase(tokens, "weight=", -10000, 10000, weight);
        weight = (float) np.findIgnoreCase(tokens, "w=", -10000, 10000, weight);

        fgc = np.findIgnoreCase(tokens, "fgc=", fgc);
        fgc = np.findIgnoreCase(tokens, "fg=", fgc);
        fgc = np.findIgnoreCase(tokens, "c=", fgc);

        bgc = np.findIgnoreCase(tokens, "bgc=", bgc);
        bgc = np.findIgnoreCase(tokens, "bg=", bgc);
        bgc = np.findIgnoreCase(tokens, "b=", bgc);

        if (tokens.size() != 0)
            throw new IOException("line " + np.lineno() + ": `" + tokens +
                    "' unexpected");
    }

    /**
     * read an edge label description
     *
     * @param np
     * @throws IOException
     */
    void readLabel(NexusStreamParser np, String prevFont) throws IOException {
        if (prevFont != null)
            font = prevFont;
        label = np.getLabelRespectCase();
        if (label != null && label.equals("null"))
            label = null;
        java.util.List<String> tokens = np.getTokensRespectCase(null, ",");
        font = np.findIgnoreCase(tokens, "font=", null, font);
        font = np.findIgnoreCase(tokens, "f=", null, font);

        labelLayout = (byte) np.findIgnoreCase(tokens, "ll=", 0, EdgeView.MAXLAYOUT, EdgeView.EAST);
        int xoffset = labelOffset != null ? labelOffset.x : 0;
        int yoffset = labelOffset != null ? labelOffset.y : 0;
        xoffset = np.findIgnoreCase(tokens, "x=", -10000, +10000, xoffset);
        yoffset = np.findIgnoreCase(tokens, "y=", -10000, +10000, yoffset);
        if (xoffset != 0 || yoffset != 0)
            labelOffset = new Point(xoffset, yoffset);

        labelAngle = np.findIgnoreCase(tokens, "a=", labelAngle);

        labelFgc = np.findIgnoreCase(tokens, "lc=", labelFgc);
        labelBgc = np.findIgnoreCase(tokens, "lk=", labelBgc);

        if (tokens.size() != 0)
            throw new IOException("line " + np.lineno() + ": `" + tokens +
                    "' unexpected");
    }

    /**
     * reads a description of a list of internal points of an edge
     *
     * @param np
     * @throws IOException
     */
    void readInternal(NexusStreamParser np) throws IOException {
        internal = new LinkedList<>();
        while (!np.peekMatchRespectCase(",")) {
            internal.add(new Point2D.Double(np.getDouble(), np.getDouble()));
        }
        np.matchRespectCase(",");
    }

    /**
     * edge to string
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("").append(id).append(" ").append(source).append(" ").append(target);
        if (eclass != ECLASS)
            buf.append(" s=").append(eclass);
        if (weight != WEIGHT)
            buf.append(" w=").append(weight);

        if (shape != SHAPE)
            buf.append(" h=").append(shape);
        if (line != LINE)
            buf.append(" l=").append(line);
        if (fgc != null && !fgc.equals(FGC))
            buf.append(" fg=").append(fgc.getRed()).append(" ").append(fgc.getGreen()).append(" ").append(fgc.getBlue());
        if (bgc != null && !bgc.equals(BGC))
            buf.append(" bg=").append(bgc.getRed()).append(" ").append(bgc.getGreen()).append(" ").append(bgc.getBlue());
        return buf.toString();
    }

    /**
     * edge label to string
     *
     * @return string
     */
    String labelToString(String prevFont) {
        StringBuilder buf = new StringBuilder();
        buf.append("").append(id).append(" '").append(label).append("'");
        buf.append(" ll=").append((int) (labelLayout));
        if (labelOffset != null && (labelOffset.x != 0 || labelOffset.y != 0))
            buf.append(" x=").append(labelOffset.x).append(" y=").append(labelOffset.y);
        if (labelAngle != 0)
            buf.append(" a=").append(labelAngle);
        if (font != null && (prevFont == null || !font.equals(prevFont)))
            buf.append(" f='").append(font).append("'");
        if (labelFgc != null && !labelFgc.equals(FGC))
            buf.append(" lc=").append(labelFgc.getRed()).append(" ").append(labelFgc.getGreen()).append(" ").append(labelFgc.getBlue());
        if (labelBgc != null)
            buf.append(" lk=").append(labelBgc.getRed()).append(" ").append(labelBgc.getGreen()).append(" ").append(labelBgc.getBlue());

        return buf.toString();
    }

    /**
     * convert internal points to string
     *
     * @return string
     */
    String internalToString() {
        StringBuilder buf = new StringBuilder();
        buf.append("").append(id);
        if (internal != null) {
            for (Point2D apt : internal) {
                buf.append(" ").append((float) apt.getX()).append(" ").append((float) apt.getY());
            }
        }
        return buf.toString();
    }

    /**
     * clones this
     *
     * @return clone
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Basic.caught(e);
            return null;
        }
    }
}
