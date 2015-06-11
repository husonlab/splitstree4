/**
 * TestHTML.java 
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
package splitstree.progs;

import jloda.util.Basic;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * test connecting to web page
 * Daniel Huson and David Bryant, DATE
 */
public class TestHTML {
    public TestHTML() {
    }

    static public void main(String[] args) {
        TestHTML testHTML = new TestHTML();

        testHTML.go();
    }

    private void go() {
        JEditorPane editorPane = new JEditorPane();


        editorPane.setEditable(false);
        editorPane.setEditorKit(new HTMLEditorKit());

        editorPane.addHyperlinkListener(
                new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        // Das aenndern des Mauszeigers geht ab
                        // Java 1.3 auch automatisch
                        if (e.getEventType() ==
                                HyperlinkEvent.EventType.ENTERED) {
                            ((JEditorPane) e.getSource()).setCursor(
                                    Cursor.getPredefinedCursor(
                                            Cursor.HAND_CURSOR));
                        } else if (e.getEventType() ==
                                HyperlinkEvent.EventType.EXITED) {
                            ((JEditorPane) e.getSource()).setCursor(
                                    Cursor.getPredefinedCursor(
                                            Cursor.DEFAULT_CURSOR));
                        } else
                            // Hier wird auf ein Klick reagiert
                            if (e.getEventType() ==
                                    HyperlinkEvent.EventType.ACTIVATED) {
                                JEditorPane pane = (JEditorPane) e.getSource();
                                if (e instanceof HTMLFrameHyperlinkEvent) {
                                    HTMLFrameHyperlinkEvent evt =
                                            (HTMLFrameHyperlinkEvent) e;
                                    HTMLDocument doc =
                                            (HTMLDocument) pane.getDocument();
                                    doc.processHTMLFrameHyperlinkEvent(evt);
                                } else try {
                                    // Normaler Link
                                    pane.setPage(e.getURL());
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                }
                            }
                    }
                });

        try {
            editorPane.setPage(new URL("http://www-ab.informatik.uni-tuebingen.de/software/splitstree4/test.html"));
        } catch (IOException e) {
            Basic.caught(e);
        }


        JFrame frame = new JFrame("SplitsTree Registration");
        frame.setSize(600, 600);
        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(editorPane, BorderLayout.CENTER);

        frame.setVisible(true);

    }
}
