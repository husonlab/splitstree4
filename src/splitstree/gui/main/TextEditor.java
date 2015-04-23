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

package splitstree.gui.main;

import jloda.util.Basic;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * This is a Text Editor for the input tab in the main viewer.
 *
 * @author Miguel Jett�
 * @since April 11th, 2005
 *        <p/>
 *        TODO: Known bug: (As of May 2nd, 2005)
 *        <pre>
 *                                                                                                                                     When we load mergedAnimals71.nex, the horizontal scroll bar doesn't
 *                                                                                                                                     seem to be acting right. It will put a huge white space in front
 *                                                                                                                                     of the text and not even align your view to the right position and
 *                                                                                                                                     so it seems like nothing is loaded in the viewport when actually there is.
 *                                                                                                                                     It seems like it is loading the 'new' viewport over the old one so it
 *                                                                                                                                     is leaving the original space in front of it. It is NOT doing this with
 *                                                                                                                                     other files. So I have no idea how to fix this yet.
 *                                                                                                                               </pre>
 */
/*
 * updates: May 2nd, 2005
 *   " Took out the syntax highlighting. All sorts of problems occured with it
 *     and it was not worth the effort to fix all of them only for a couple
 *     of comments and 5 keywords."
 */
public class TextEditor extends JFrame {
    private JTextArea inputTextArea;
    private String inputTextOriginal;
    private int selectedLineInInputText = 0;

    public JScrollPane initializeEditor(MainViewer me) {
        inputTextOriginal = null;

        inputTextArea = new JTextArea();

        // Setting some basic properties
        inputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        inputTextArea.setCaretColor(Color.red);
        inputTextArea.setEditable(true);

        //Adding the lineHighlighter for the JTextPane
        LineHighlightHandler lineHighlightHandler = new LineHighlightHandler(HIGHLIGHT_COLOR);
        lineHighlightHandler.setTextComponent(inputTextArea);
        lineHighlightHandler.addHighlight(0);

        // We use an intermediary Panel in order to stop the line wrapping
        JViewport viewport = new JViewport();
        viewport.setOpaque(true);
        viewport.setBackground(inputTextArea.getBackground());
        viewport.setView(inputTextArea);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(true);
        scrollPane.setBackground(inputTextArea.getBackground());
        scrollPane.setViewport(viewport);

        // Add a line number view so that we can see the line number
        LineNumberView numberView = new LineNumberView(inputTextArea);
        numberView.setFont(new Font("Monospaced", Font.PLAIN, 10));
        scrollPane.setRowHeaderView(numberView);

        return scrollPane;

    }

    //ToDo: These could be system properties
    private static final Color HIGHLIGHT_COLOR = new Color(228, 228, 241);
    private final static Color DEFAULT_BACKGROUND = new Color(230, 163, 4);
    private final static int MARGIN = 5;

    private static class LineHighlightHandler extends DefaultHighlighter.DefaultHighlightPainter {
        private JTextComponent theTextComponent;
        private Highlighter theHighlighter;
        private Object theLastHighlight;

        public LineHighlightHandler(Color aColor) {
            super(aColor);
        }

        void setTextComponent(JTextComponent aTextComponent) {
            theTextComponent = aTextComponent;
            DefaultHighlighter defaultHighlighter = (DefaultHighlighter) theTextComponent.getHighlighter();
            defaultHighlighter.setDrawsLayeredHighlights(true);
            theHighlighter = defaultHighlighter;
            MouseInputListener mil = new MouseInputAdapter() {
                public void mousePressed(MouseEvent e) {
                    resetHighlight();
                }

                public void mouseDragged(MouseEvent e) {


                    removeHighlight();
                }
            };
            theTextComponent.addMouseListener(mil);
            theTextComponent.addMouseMotionListener(mil);
            theTextComponent.addCaretListener(new CaretListener() {
                public void caretUpdate(CaretEvent e) {
                    resetHighlight();
                }
            });

        }

        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            try {
                Rectangle r = c.modelToView(offs0);
                r.x = 0;
                r.width = c.getSize().width;
                g.setColor(getColor());
                g.fillRect(r.x, r.y, r.width, r.height);
                return r;
            } catch (BadLocationException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void resetHighlight() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    theHighlighter.removeHighlight(theLastHighlight);
                    Element root = theTextComponent.getDocument().getDefaultRootElement();
                    int line = root.getElementIndex(theTextComponent.getCaretPosition());
                    Element lineElement = root.getElement(line);
                    int start = lineElement.getStartOffset();
                    addHighlight(start);
                }
            });
        }

        private void addHighlight(int offset) {
            try {
                theLastHighlight = theHighlighter.addHighlight(offset, offset + 1, this);
            } catch (BadLocationException ble) {
                ble.printStackTrace();
            }
        }

        private void removeHighlight() {

            theHighlighter.removeHighlight(theLastHighlight);
        }
    }

    private static class LineNumberPanel extends JPanel implements DocumentListener {
        private JTextComponent theTextComponent;
        private FontMetrics theFontMetrics;
        private int currentRowWidth;

        public LineNumberPanel(JTextComponent aTextComponent) {
            theTextComponent = aTextComponent;
            theTextComponent.getDocument().addDocumentListener(this);
            setOpaque(true);
            setBackground(DEFAULT_BACKGROUND);
            setFont(theTextComponent.getFont());
            theFontMetrics = getFontMetrics(getFont());
            setForeground(theTextComponent.getForeground());
            currentRowWidth = getDesiredRowWidth();
        }

        private void update() {
            int desiredRowWidth = getDesiredRowWidth();
            if (desiredRowWidth != currentRowWidth) {
                currentRowWidth = desiredRowWidth;
                revalidate();
            }
            repaint();
        }

        private int getDesiredRowWidth() {
            Document doc = theTextComponent.getDocument();
            int length = doc.getLength();
            Element map = doc.getDefaultRootElement();
            int nbLines = map.getElementIndex(length) + 1;
            return theFontMetrics.stringWidth(Integer.toString(nbLines));
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            try {
                Document doc = theTextComponent.getDocument();
                int length = doc.getLength();
                Element map = doc.getDefaultRootElement();
                int startLine = map.getElementIndex(0);
                int endline = map.getElementIndex(length);
                for (int line = startLine; line <= endline; line++) {
                    int y = theTextComponent.modelToView(map.getElement(line).getStartOffset()).y + theFontMetrics.getHeight() - theFontMetrics.getDescent();
                    String s = Integer.toString(line + 1);
                    int width = theFontMetrics.stringWidth(s);
                    g.drawString(s, MARGIN + currentRowWidth - width, y);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

        }

        public Dimension getPreferredSize() {
            return new Dimension(2 * MARGIN + currentRowWidth, theTextComponent.getHeight());
        }

        public void insertUpdate(DocumentEvent e) {
            update();
        }

        public void removeUpdate(DocumentEvent e) {
            update();
        }

        public void changedUpdate(DocumentEvent e) {
            update();
        }
    }

    /**
     * gets the current editor text
     *
     * @return String the current editor text
     */
    public String getEditText() {
        if (inputTextArea != null)
            return inputTextArea.getText();
        else
            return "";
    }

    /**
     * gets the font displayed in the undo text area
     *
     * @return font of undo text
     */
    public Font getEditTextFont() {
        if (inputTextArea != null)
            return inputTextArea.getFont();
        else
            return null;
    }

    /**
     * select this line next time undo tab is opened
     *
     * @param lineno
     */
    public void setEditSelectLine(int lineno) {
        Basic.selectLine(inputTextArea, lineno);
        selectedLineInInputText = lineno;
    }

    public int getEditSelectLine() {
        return selectedLineInInputText;
    }

    /**
     * sets the text to be displayed in the editor
     *
     * @param text
     */
    public void setEditText(String text) {
        if (inputTextArea != null)
            inputTextArea.setText(text);
    }

    /**
     * appends the text to the text in the editor
     *
     * @param text
     */
    public void appendEditText(String text) {
        if (inputTextArea != null)
            inputTextArea.append(text);
    }

    /**
     * returns the original undo text
     *
     * @return original undo text
     */
    public String getEditTextOriginal() {
        return inputTextOriginal;
    }

    /**
     * sets the original undo text
     *
     * @param text original undo text
     */
    public void setEditTextOriginal(String text) {
        inputTextOriginal = text;
    }

    /**
     * gets the undo text area
     *
     * @return undo text area
     */
    public JTextArea getInputTextArea() {
        return inputTextArea;
    }
}
