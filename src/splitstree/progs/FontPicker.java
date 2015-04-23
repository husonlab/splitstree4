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

package splitstree.progs;

// FontPicker.java
// A quick test of the FontChooser dialog.  (see FontChooser.java)
//

import splitstree.util.FontChooser;

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
