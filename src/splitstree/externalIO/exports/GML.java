/**
 * GML.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
 * Export graph in GML format
 *
 * @author Regula Rupp, Daniel Huson and David Bryant,
 *         June 2007
 */
public class GML extends ExporterAdapter implements Exporter {

    private String Description = "Exports to GML records.";

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

        
        exportNetwork(w, doc);

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
        w.write("graph [ \n");
        w.write("\t label \"" + title + "\"\n");

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

        // nodes
        for (int i = 1; i <= nVertices; i++) {

            VertexDescription v = vertices[i];

            // start node
            w.write("\t node [ \n");
            
            // node id
            w.write("\t \t id " + v.getId() + "\n");
            
            // label
            if (v.getLabel() != null) {
                w.write("\t \t label \"" + v.getLabel() + "\"\n");
                w.write("\t \t LabelGraphics [ fill \"#FFFFFF\" ]\n");
                //w.write("\t \t LabelGraphics [ anchor \"s\" ]\n");
                }
            
            // color
            w.write("\t \t graphics [ \n");
            w.write("\t \t \t type \"circle\"\n");
            w.write("\t \t \t fill \"" + castColor(v.getFgc()) + "\"\n");
            w.write("\t \t \t ] \n");
                
            // end node
            w.write("\t \t ] \n");

        }

        // edges
        for (int i = 1; i <= nEdges; i++) {

            EdgeDescription e = edges[i];

            // start edge
            w.write("\t edge [ \n");

            // source, target
            w.write("\t \t source " + e.getSource() + "\n");
            w.write("\t \t target " + e.getTarget() + "\n");
            
            // label
            if (e.getLabel() != null) {
                w.write("\t \t label \"" + e.getLabel() + "\"\n");}
            
            // color
            w.write("\t \t graphics [ \n");
            w.write("\t \t \t type \"line\"\n");
            w.write("\t \t \t width " + (double)e.getLine() + "\n");
            w.write("\t \t \t fill \"" + castColor(e.getFgc()) + "\"\n");
            w.write("\t \t \t ] \n");
            
            // end edge
            w.write("\t \t ] \n");

        }
        w.write("] \n");

        return true;
    }

    private String castColor(Color toCast){
        StringBuilder re = new StringBuilder("#");
        int r = toCast.getRed();
        if (r<16) re.append("0");
    	re.append(Integer.toHexString(r).toUpperCase());
    	int g = toCast.getGreen();
    	if (g<16) re.append("0");
    	re.append(Integer.toHexString(g).toUpperCase());
    	int b = toCast.getBlue();
    	if (b<16) re.append("0");
    	re.append(Integer.toHexString(b).toUpperCase());
    	return re.toString();
    }
    
    public String getDescription() {
        return Description;
    }
}
