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

package splitstree.externalIO.exports;

import splitstree.core.Document;
import splitstree.nexus.EdgeDescription;
import splitstree.nexus.Network;
import splitstree.nexus.VertexDescription;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Huson and David Bryant, Michael Schr�der
 * @version $Id: CGViz.java,v 1.18 2007-06-05 09:35:20 huson Exp $
 */
public class CGViz extends ExporterAdapter implements Exporter {

    private String Description = "Exports to CGViz records.";


    public boolean isApplicable(Document doc, Collection blocks) {
        if (blocks.size() != 1 || !blocks.contains(Network.NAME))
            return false;
        return !(doc != null && !doc.isValidByName(Network.NAME));
    }

    /**
     * apply the exporter
     *
     * @param w
     * @param doc
     * @param blocks
     * @return
     * @throws Exception
     */
    public Map apply(Writer w, Document doc, Collection blocks) throws Exception {

        for (Object block1 : blocks) {

            String block = (String) block1;


            if (block.equals("st_Graph")) exportNetwork(w, doc);

        }
        return null;
    }

    private boolean exportNetwork(Writer w, Document doc) throws IOException {

        System.err.println("exporting st_Graph");
        final Network network = doc.getNetwork();
        final int nVertices = network.getNvertices();
        final int nEdges = network.getNedges();

        final VertexDescription[] vertices = network.getVertices();
        final EdgeDescription[] edges = network.getEdges();

        if (nVertices == 0) {
            return false;
        }

        String title = doc.getTitle().replaceFirst("\\.nex|\\.NEX", "");

        w.write("{DATA " + title + "\n");
        w.write("\t[__GLOBAL__] dimension=2\n");

        double minX = 0, maxX = 1;
        double minY = 0, maxY = 1;
        for (int i = 1; i <= nVertices; i++) {

            VertexDescription v = vertices[i];
            if (i == 1) {
                minX = maxX = v.getX();
                minY = maxY = v.getY();
            }
            if (v.getX() < minX)
                minX = v.getX();
            if (v.getY() > maxX)
                maxX = v.getX();
            if (v.getY() < minY)
                minY = v.getY();
            if (v.getY() > maxY)
                maxY = v.getY();
        }

        // vertices
        for (int i = 1; i <= nVertices; i++) {

            VertexDescription v = vertices[i];

            // node id
            w.write("\t[" + v.getId() + "] type=node");

            // node color
            Color bgc = v.getBgc();
            if (!bgc.equals(Color.white))
                w.write(" bgc=" + bgc.getRed() + "," + bgc.getGreen() + "," + bgc.getBlue());
            Color fgc = v.getFgc();
            if (!fgc.equals(Color.black))
                w.write(" fgc=" + fgc.getRed() + "," + fgc.getGreen() + "," + fgc.getBlue());

            // node size
            w.write(" width=" + v.getWidth());
            w.write(" height=" + v.getHeight());

            // node line width
            w.write(" line=" + v.getLine());

            // node coords
            int x = (int) ((v.getX() - minX) / (maxX - minX) * 100000.0 + 1);
            int y = (int) ((maxY - v.getY()) / (maxY - minY) * 100000.0 + 1);

            w.write(": " + x + " " + y + "\n");

            // label
            if (v.getLabel() != null) {
                w.write("\t[" + v.getId() + "] type=nlabel");
                w.write(" text=" + v.getLabel());
                w.write(" font=" + v.getFont());
                w.write(" xoff=" + v.getLabelOffset().x);
                w.write(" yoff=" + v.getLabelOffset().y);
                w.write(": " + x + " " + y + "\n");
            }

        }

        // edges
        for (int i = 1; i <= nEdges; i++) {

            EdgeDescription e = edges[i];

            // edge id
            w.write("\t[" + e.getId() + "] type=edge");

            // source, sink
            w.write(" source=" + e.getSource());
            w.write(" target=" + e.getTarget());

            // class
            w.write(" eclass=" + e.getEclass());

            // weight
            w.write(" weight=" + e.getWeight());

            // edge color
            Color bgc = e.getBgc();
            if (!bgc.equals(Color.white))
                w.write(" bgc=" + bgc.getRed() + "," + bgc.getGreen() + "," + bgc.getBlue());
            Color fgc = e.getFgc();
            if (!fgc.equals(Color.black))
                w.write(" fgc=" + fgc.getRed() + "," + fgc.getGreen() + "," + fgc.getBlue());

            // edge line width
            w.write(" line=" + e.getLine());

            VertexDescription vs = vertices[e.getSource()];
            VertexDescription vt = vertices[e.getTarget()];
            int vsx = (int) ((vs.getX() - minX) / (maxX - minX) * 100000.0 + 1);
            int vsy = (int) ((maxY - vs.getY()) / (maxY - minY) * 100000.0 + 1);
            int vtx = (int) ((vt.getX() - minX) / (maxX - minX) * 100000.0 + 1);
            int vty = (int) ((maxY - vt.getY()) / (maxY - minY) * 100000.0 + 1);

            w.write(": " + vsx + " " + vsy + " " + vtx + " " + vty + "\n");

            // label
            if (e.getLabel() != null) {
                w.write("\t[" + e.getId() + "] type=elabel");
                w.write(" text=" + e.getLabel());
                w.write(" font=" + e.getFont());
                w.write(" xoff=" + e.getLabelOffset().x);
                w.write(" yoff=" + e.getLabelOffset().y);
                w.write(": " + (vsx + vtx) / 2 + " " + (vsy + vty) / 2 + "\n");
            }

        }
        w.write("}");

        return true;
    }


    public String getDescription() {
        return Description;
    }
}


