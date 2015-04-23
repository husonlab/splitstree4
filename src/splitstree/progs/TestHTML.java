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
