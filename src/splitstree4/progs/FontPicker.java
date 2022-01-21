/*
 * FontPicker.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.progs;

// FontPicker.java
// A quick test of the FontChooser dialog.  (see FontChooser.java)
//

import splitstree4.util.FontChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FontPicker extends JFrame {
    public FontPicker() {
        super("JColorChooser Test Frame");
        setSize(200, 100);
        final JButton go = new JButton("Show FontChooser");
        go.addActionListener(new ActionListener() {
            final FontChooser chooser = new FontChooser(FontPicker.this, true);
            boolean first = true;

            public void actionPerformed(ActionEvent e) {
                chooser.setVisible(true);
                // If we got a real font choice, then update our go button
                if (chooser.getNewFont() != null) {
                    go.setFont(chooser.getNewFont());
                    go.setForeground(chooser.getNewColor());
                }
            }
        });
        getContentPane().add(go);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static void main(String args[]) {
        FontPicker fp = new FontPicker();
        fp.setVisible(true);
    }
}
