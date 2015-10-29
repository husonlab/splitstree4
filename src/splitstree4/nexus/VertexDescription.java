/**
 * VertexDescription.java
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

import jloda.graph.Node;
import jloda.graphview.NodeView;
import jloda.graphview.ViewBase;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import java.awt.*;
import java.io.IOException;

/**
 * all the properties that a node might have
 * * @author Daniel Huson and David Bryant
 *
 * @version $Id: VertexDescription.java,v 1.17 2010-02-23 15:52:01 huson Exp $
 */
public class VertexDescription implements Cloneable {
    // default values:
    final static int WIDTH = 1;
    final static int HEIGHT = 1;
    final static int LINE = 1;
    final static char NONE_NODE = 'n';
    final static char RECT_NODE = 'r';
    final static char OVAL_NODE = 'o';
    final static Color FGC = Color.black;
    final static Color BGC = Color.black;
    final static String FONT = "Default-PLAIN-10"; // same as in GraphView
    final static Point OFFSET = null;
    Node v;

    int id;
    float x;
    float y;
    int width = WIDTH;
    int height = HEIGHT;
    int line = LINE;
    char shape = OVAL_NODE;
    Color fgc = FGC;
    Color bgc = BGC;
    String label = null;
    Color labelFgc = FGC;
    Color labelBgc = null;
    Point labelOffset = OFFSET;
    byte labelLayout = NodeView.LAYOUT;
    float labelAngle;
    String font = FONT;

    public Node getV() {
        return v;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLine() {
        return line;
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

    public String getLabel() {
        return label;
    }

    public String getFont() {
        return font;
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

    public char getShape() {
        return shape;
    }

    public void setShape(char shape) {
        this.shape = shape;
    }


    /**
     * read a vertex description
     *
     * @param id        vertex id
     * @param nvertices number of vertices
     * @param np
     * @throws java.io.IOException
     */
    void read(int id, int nvertices, NexusStreamParser np) throws IOException {
        np.matchIgnoreCase("" + id);
        this.id = id;
        x = (float) np.getDouble();
        y = (float) np.getDouble();
        java.util.List tokens = np.getTokensLowerCase(null, ",");
        width = np.findIgnoreCase(tokens, "width=", 0, 100, width);
        width = np.findIgnoreCase(tokens, "w=", 0, 100, width);

        height = np.findIgnoreCase(tokens, "height=", 0, 100, height);
        height = np.findIgnoreCase(tokens, "h=", 0, 100, height);

        line = np.findIgnoreCase(tokens, "line=", 0, 100, line);
        line = np.findIgnoreCase(tokens, "l=", 0, 100, line);

        fgc = np.findIgnoreCase(tokens, "fgc=", fgc);
        fgc = np.findIgnoreCase(tokens, "fg=", fgc);
        fgc = np.findIgnoreCase(tokens, "c=", fgc);

        bgc = np.findIgnoreCase(tokens, "bgc=", bgc);
        bgc = np.findIgnoreCase(tokens, "bg=", bgc);
        bgc = np.findIgnoreCase(tokens, "b=", bgc);

        shape = np.findIgnoreCase(tokens, "shape=", "nor", shape);
        shape = np.findIgnoreCase(tokens, "s=", "nor", shape);

        if (tokens.size() != 0)
            throw new IOException("line " + np.lineno() + ": `" + tokens +
                    "' unexpected");
    }

    /**
     * vertex to string
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("").append(id).append(" ").append(x).append(" ").append(y);
        if (width != WIDTH)
            buf.append(" w=").append(width);
        if (height != HEIGHT)
            buf.append(" h=").append(height);
        if (line != LINE)
            buf.append(" l=").append(line);
        if (shape != OVAL_NODE)
            buf.append(" s=").append(shape);
        if (fgc != null && !fgc.equals(FGC))
            buf.append(" fg=").append(fgc.getRed()).append(" ").append(fgc.getGreen()).append(" ").append(fgc.getBlue());
        if (bgc != null && !bgc.equals(BGC))
            buf.append(" bg=").append(bgc.getRed()).append(" ").append(bgc.getGreen()).append(" ").append(bgc.getBlue());
        return buf.toString();
    }

    /**
     * read a vertex label description
     *
     * @param np
     * @throws IOException
     */
    void readLabel(NexusStreamParser np, String prevFont) throws IOException {
        if (prevFont != null)
            font = prevFont;
        label = np.getLabelRespectCase();
        java.util.List tokens = np.getTokensRespectCase(null, ",");
        font = np.findIgnoreCase(tokens, "font=", null, font);
        font = np.findIgnoreCase(tokens, "f=", null, font);

        labelLayout = (byte) np.findIgnoreCase(tokens, "l=", 0, NodeView.MAXLAYOUT, NodeView.LAYOUT);
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

    String labelToString(String prevFont) {
        StringBuilder buf = new StringBuilder();
        buf.append("").append(id).append(" '").append(label).append("'");
        if (labelLayout != ViewBase.LAYOUT)
            buf.append(" l=").append((int) labelLayout);
        if (labelOffset != null && (labelOffset.x != 0 || labelOffset.y != 0))
            buf.append(" x=").append(labelOffset.x).append(" y=").append(labelOffset.y);
        if (labelAngle != 0)
            buf.append(" a=").append(labelAngle);

        if (font != null && (prevFont == null || !font.equals(prevFont)))
            buf.append(" f='").append(font).append("'");
        if (labelFgc != null && !labelFgc.equals(FGC))
            buf.append(" lc=").append(labelFgc.getRed()).append(" ").append(labelFgc.getGreen()).append(" ").append(labelFgc.getBlue());
        if (labelBgc != null && !labelBgc.equals(BGC))
            buf.append(" lk=").append(labelBgc.getRed()).append(" ").append(labelBgc.getGreen()).append(" ").append(labelBgc.getBlue());
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
