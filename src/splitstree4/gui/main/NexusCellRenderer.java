/*
 * NexusCellRenderer.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui.main;

import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import splitstree4.gui.Director;
import splitstree4.nexus.*;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.io.StringReader;

/**
 * MultiLine Cell Renderer for the JTree to display more than one line
 *
 * @author daniel huson, 2010
 */
public class NexusCellRenderer implements TreeCellRenderer {
    public final static String GRAY = "<font color=#a0a0a0>";
    public final static String BLACK = "<font color=#000000>";

    public final static String RED = "<font color=#ff0000>";
    public final static String BLUE = "<font color=#0000ff>";
    public final static String GREEN = "<font color=#00ff00>";

    final private Director dir;

    /**
     * create a multi-line renderer
     */
    public NexusCellRenderer(Director dir) {
        this.dir = dir;
    }

    /**
     * get the tree cell render component
     *
     * @return component
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");

        String text = "?";

        try {
            NexusStreamParser np = new NexusStreamParser(new StringReader(stringValue));
            if (np.peekMatchIgnoreCase("#nexus"))
                np.matchIgnoreCase("#nexus");
            np.matchIgnoreCase("begin");
            String blockName = np.getWordRespectCase();
            // System.err.println("Constructing "+stringValue);
            stringValue = stringValue.substring(1); // skip the initial dash
            NexusBlock block = dir.getDocument().getBlockByName(blockName);
            if (block != null) {
                text = stringValue;
                text = text.replaceFirst("BEGIN", GRAY + "BEGIN").replaceFirst("END;", GRAY + "END;");
                if (block instanceof Taxa) {
                    text = text.replaceFirst("TAXLABELS", "TAXLABELS" + BLACK + "\n");
                } else if (block instanceof Sets) {
                    text = text.replaceFirst(";", ";" + BLACK + "\n");
                } else if (block instanceof Characters) {
                    text = text.replaceFirst("MATRIX", "MATRIX" + BLACK + "\n");
                } else if (block instanceof Distances) {
                    text = text.replaceFirst("MATRIX", "MATRIX" + BLACK + "\n");
                } else if (block instanceof Splits) {
                    text = text.replaceFirst("MATRIX", "MATRIX" + BLACK + "\n");
                } else if (block instanceof Trees) {
                    text = text.replaceFirst("\\[TREES]", "[TREES]" + BLACK + "\n");
                } else if (block instanceof Network) {
                    text = text.replaceFirst("TRANSLATE", "TRANSLATE" + BLACK + "\n");
                } else if (block instanceof Assumptions) {
                    text = text.replaceFirst("st_Assumptions;", "st_Assumptions;" + BLACK + "\n");
                }
				text = StringUtils.trimEmptyLines(text).replaceAll("\t", "&#9;");
				text = "<html><pre><font face=\"monospace\" size=\"3\">" + text + "</font></pre></html>";
            }
        } catch (Exception ex) // not in nexus format, must be simple name or comments
        {
            text = "<html><font face=\"monospace\" size=\"3\">" + dir.getDocument().getNameForDataTree(stringValue) + "</font></html>";
        }

        textPane.setText(text);
        textPane.setEnabled(tree.isEnabled());
        if (isSelected) {
            textPane.setBackground(UIManager.getColor("Tree.selectionBackground"));
        } else {
            textPane.setBackground(UIManager.getColor("Tree.textBackground"));
        }
        textPane.revalidate();
        return textPane;
    }
}

