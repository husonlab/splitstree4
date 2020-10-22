/**
 * StatusBar.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.gui.main;

import jloda.util.Basic;
import splitstree4.algorithms.trees.TreeSelector;
import splitstree4.core.Document;
import splitstree4.nexus.*;
import splitstree4.util.SplitsUtilities;

public class StatusBar extends jloda.swing.util.StatusBar {

    private boolean fit, lsFit, taxa, chars, trees, splits, assumptions,
            vertices, edges;

    /**
     * construct a status line
     */
    StatusBar() {

        setDefaults();
        setStatusLine(null);
    }

    /**
     * set defaults
     */
    public void setDefaults() {
        fit = true;
        lsFit = true;
        taxa = true;
        chars = true;
        trees = true;
        splits = true;
        assumptions = true;
        vertices = true;
        edges = true;
    }

    /**
     * set the status line for given document
     *
     * @param doc
     */
    public void setStatusLine(Document doc) {
        if (doc == null || !doc.isValid() || !doc.isValidByName(Taxa.NAME))
            setText2("No data");
        else {
            String status = "";
            try {

                //Check to see if the fit statistics need updating.
                if ((fit || lsFit) && doc.isValidByName(Splits.NAME) && doc.isValidByName(Distances.NAME)) {
                    if (doc.getSplits().getProperties().getFit() < 0 || doc.getSplits().getProperties().getLSFit() < 0)
                        SplitsUtilities.computeFits(true, doc.getSplits(), doc.getDistances(), null);
                }

                if (fit && doc.isValidByName(Splits.NAME) && doc.getSplits().getProperties().getFit() >= 0)
                    status += "Fit=" + Basic.roundSigFig(doc.getSplits().getProperties().getFit(), 5);

                if (lsFit && doc.isValidByName(Splits.NAME) && (doc.getSplits().getProperties().getLSFit() > 0))
                    status += " LSFit=" + Basic.roundSigFig(doc.getSplits().getProperties().getLSFit(), 5);

                if (taxa && doc.isValidByName(Taxa.NAME)) {
                    status += " Taxa=" + doc.getTaxa().getNtax();
                    Taxa original = doc.getTaxa().getOriginalTaxa();
                    if (original != null && original.getNtax() > doc.getTaxa().getNtax())
                        status += " (of " + original.getNtax() + ")";
                }

                if (chars && doc.isValidByName(Characters.NAME)) {
                    if (doc.getCharacters().getNactive() == -1)
                        status += " Chars=" + doc.getCharacters().getNchar();
                    else {
                        status += " Chars=" + doc.getCharacters().getNactive();
                        if (doc.getCharacters().getNchar() > doc.getCharacters().getNactive())
                            status += " (of " + doc.getCharacters().getNchar() + ")";
                    }
                }

                if (trees && doc.isValidByName(Trees.NAME)) {
                    status += " Trees=" + doc.getTrees().getNtrees();
                    Trees original = doc.getTrees().getOriginal();
                    if (original != null && original.getNtrees() != doc.getTrees().getNtrees())
                        status += " (of " + original.getNtrees() + ")";
                }

                if (splits && doc.isValidByName(Splits.NAME)) {
                    status += " Splits=" + doc.getSplits().getNsplits();
                    Splits original = doc.getSplits().getOriginal();
                    if (original != null && original.getNsplits() > doc.getSplits().getNsplits())
                        status += " (of " + original.getNsplits() + ")";
                }

                if (assumptions && doc.isValidByName(Assumptions.NAME)) {
                    Assumptions assumptions = doc.getAssumptions();

                    status += " [";
                    if (assumptions != null) {
                        boolean first = true;
                        if (doc.getUnaligned() != null && assumptions.getUnalignedTransformName() != null) {
                            if (first) status += Unaligned.NAME;
                            status += " > " + assumptions.getUnalignedTransformName().replaceAll(".*\\.", "");
                            first = false;
                        }
                        if (doc.getCharacters() != null && assumptions.getCharactersTransformName() != null) {
                            if (first) status += Characters.NAME;
                            status += " > " + assumptions.getCharactersTransformName().replaceAll(".*\\.", "");
                            first = false;
                        }
                        if (doc.getDistances() != null && assumptions.getDistancesTransformName() != null) {
                            if (first) status += Distances.NAME;
                            status += " > " + assumptions.getDistancesTransformName().replaceAll(".*\\.", "");
                            first = false;
                        }
                        if (doc.getQuartets() != null && assumptions.getQuartetsTransformName() != null) {
                            if (first) status += "Quartets";
                            status += " > " + assumptions.getQuartetsTransformName().replaceAll(".*\\.", "");
                            first = false;
                        }
                        if (doc.getTrees() != null && assumptions.getTreesTransformName() != null) {
                            if (first) status += Trees.NAME;
                            status += " > " + assumptions.getTreesTransformName().replaceAll(".*\\.", "");
                            if (assumptions.getTreesTransformName().equals(Basic.getShortName(TreeSelector.class))) {
                                int which = ((TreeSelector) assumptions.getTreesTransform()).getOptionWhich();
                                if (which > 0)
                                    status += " (" + which + ")";
                            }
                            first = false;
                        }
                        if (doc.getSplits() != null && assumptions.getSplitsTransformName() != null) {
                            if (first) status += Splits.NAME;
                            status += " > " + assumptions.getSplitsTransformName().replaceAll(".*\\.", "");
                        }
                    }
                    status += " ]";
                }

                if (vertices && doc.isValidByName(Network.NAME)) {
                    status += " Vertices=" + doc.getNetwork().getNvertices();
                }

                if (edges && doc.isValidByName(Network.NAME)) {
                    status += " Edges=" + doc.getNetwork().getNedges();
                }
            } catch (Exception e) {
                setText2(status);
                //throw(e);
                //TODO: Handle a Canceled Exception here.... should return control to the user.
            }
            setText2(status);
        }
    }


    public boolean getFit() {
        return fit;
    }

    public void setFit(boolean fit) {
        this.fit = fit;
    }

    public boolean getLsFit() {
        return lsFit;
    }

    public void setLsFit(boolean lsFit) {
        this.lsFit = lsFit;
    }

    public boolean getTaxa() {
        return taxa;
    }

    public void setTaxa(boolean taxa) {
        this.taxa = taxa;
    }

    public boolean getChars() {
        return chars;
    }

    public void setChars(boolean chars) {
        this.chars = chars;
    }

    public boolean getTrees() {
        return trees;
    }

    public void setTrees(boolean trees) {
        this.trees = trees;
    }

    public boolean getSplits() {
        return splits;
    }

    public void setSplits(boolean splits) {
        this.splits = splits;
    }

    public boolean getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(boolean assumptions) {
        this.assumptions = assumptions;
    }

    public boolean getVertices() {
        return vertices;
    }

    public void setVertices(boolean vertices) {
        this.vertices = vertices;
    }

    public boolean getEdges() {
        return edges;
    }

    public void setEdges(boolean edges) {
        this.edges = edges;
    }
}
