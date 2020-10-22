/**
 * LineNumberView.java
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
package splitstree4.gui.main;

/**
 * This is a simple line-number gutter that displays the line
 * numbers at the beginning of each lines.
 *
 * @author Miguel Jett�
 * @since April 11th, 2005
 */

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * LineNumberView is a simple line-number gutter that works correctly
 * even when lines are wrapped in the associated text component.  This
 * is meant to be used as the RowHeaderView in a JScrollPane that
 * contains the associated text component.  Example usage:
 * <pre>
 *   JTextArea ta = new JTextArea();
 *   ta.setLineWrap(true);
 *   ta.setWrapStyleWord(true);
 *   JScrollPane sp = new JScrollPane(ta);
 *   sp.setRowHeaderView(new LineNumberView(ta));
 * </pre>
 *
 * @author Alan Moore
 */
public class LineNumberView extends JComponent {
    // This is for the border to the right of the line numbers.
    // There's probably a UIDefaults value that could be used for this.
    private static final Color BORDER_COLOR = Color.GRAY;

    private static final int WIDTH_TEMPLATE = 99999;
    private static final int MARGIN = 5;

    private FontMetrics viewFontMetrics;
    private int maxNumberWidth;
    private int componentWidth;

    private int textTopInset;
    private int textFontAscent;
    private int textFontHeight;

    private JTextComponent text;
    private SizeSequence sizes;
    private int startLine = 0;
    private boolean structureChanged = true;

    /**
     * Construct a LineNumberView and attach it to the given text component.
     * The LineNumberView will listen for certain kinds of events from the
     * text component and update itself accordingly.
     *
     * @param text the JTextComponnet
     */
    public LineNumberView(JTextComponent text) {
        if (text == null) {
            throw new IllegalArgumentException("Text component cannot be null");
        }
        this.text = text;
        updateCachedMetrics();

        UpdateHandler handler = new UpdateHandler();
        text.getDocument().addDocumentListener(handler);
        text.addPropertyChangeListener(handler);
        text.addComponentListener(handler);

        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
    }

    /**
     * Schedule a repaint because one or more line heights may have changed.
     *
     * @param startLine        the line that changed, if there's only one
     * @param structureChanged if <tt>true</tt>, ignore the line number and
     *                         update all the line heights.
     */
    private void viewChanged(int startLine, boolean structureChanged) {
        this.startLine = startLine;
        this.structureChanged = structureChanged;

        revalidate();
        repaint();
    }

    /**
     * Update the line heights as needed.
     */
    private void updateSizes() {
        if (startLine < 0) {
            return;
        }

        if (structureChanged) {
            int count = getAdjustedLineCount();
            sizes = new SizeSequence(count);
            for (int i = 0; i < count; i++) {
                sizes.setSize(i, getLineHeight(i));
            }
            structureChanged = false;
        } else {
            sizes.setSize(startLine, getLineHeight(startLine));
        }

        startLine = -1;
    }

    /* Copied from javax.swing.text.PlainDocument */
    private int getAdjustedLineCount() {
        // There is an implicit break being modeled at the end of the
        // document to deal with boundary conditions at the end.  This
        // is not desired in the line count, so we detect it and remove
        // its effect if throwing off the count.
        Element map = text.getDocument().getDefaultRootElement();
        int n = map.getElementCount();
        Element lastLine = map.getElement(n - 1);
        if ((lastLine.getEndOffset() - lastLine.getStartOffset()) > 1) {
            return n;
        }

        return n - 1;
    }

    /**
     * Get the height of a line from the JTextComponent.
     *
     * @param index the line number
     */
    private int getLineHeight(int index) {
        int lastPos = sizes.getPosition(index) + textTopInset;
        int height = textFontHeight;
        try {
            Element map = text.getDocument().getDefaultRootElement();
            int lastChar = map.getElement(index).getEndOffset() - 1;
            Rectangle r = text.modelToView(lastChar);
            height = (r.y - lastPos) + r.height;
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return height;
    }

    /**
     * Cache some values that are used a lot in painting or size
     * calculations. Also ensures that the line-number font is not
     * larger than the text component's font (by point-size, anyway).
     */
    private void updateCachedMetrics() {
        Font textFont = text.getFont();
        FontMetrics fm = getFontMetrics(textFont);
        textFontHeight = fm.getHeight();
        textFontAscent = fm.getAscent();
        textTopInset = text.getInsets().top;

        Font viewFont = getFont();
        boolean changed = false;
        if (viewFont == null) {
            viewFont = UIManager.getFont("Label.font");
            changed = true;
        }
        if (viewFont.getSize() > textFont.getSize()) {
            viewFont = viewFont.deriveFont(textFont.getSize2D());
            changed = true;
        }

        viewFontMetrics = getFontMetrics(viewFont);
        maxNumberWidth = viewFontMetrics.stringWidth(String.valueOf(WIDTH_TEMPLATE));
        componentWidth = 2 * MARGIN + maxNumberWidth;

        if (changed) {
            super.setFont(viewFont);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(componentWidth, text.getHeight());
    }

    public void setFont(Font font) {
        super.setFont(font);
        updateCachedMetrics();
    }

    public void paintComponent(Graphics g) {
        updateSizes();
        Rectangle clip = g.getClipBounds();

        g.setColor(getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        g.setColor(getForeground());
        int base = clip.y - textTopInset;
        int first = sizes.getIndex(base);
        int last = sizes.getIndex(base + clip.height);
        String text;

        for (int i = first; i <= last; i++) {
            text = String.valueOf(i + 1);
            int x = MARGIN + maxNumberWidth - viewFontMetrics.stringWidth(text);
            int y = sizes.getPosition(i) + textFontAscent + textTopInset;
            g.drawString(text, x, y);
        }
    }

    class UpdateHandler extends ComponentAdapter
            implements PropertyChangeListener, DocumentListener {
        /**
         * The text component was resized. 'Nuff said.
         */
        public void componentResized(ComponentEvent evt) {
            viewChanged(0, true);
        }

        /**
         * A bound property was changed on the text component. Properties
         * like the font, border, and tab size affect the layout of the
         * whole document, so we invalidate all the line heights here.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            Object oldValue = evt.getOldValue();
            Object newValue = evt.getNewValue();
            String propertyName = evt.getPropertyName();
            if ("document".equals(propertyName)) {
                if (oldValue != null && oldValue instanceof Document) {
                    ((Document) oldValue).removeDocumentListener(this);
                }
                if (newValue != null && newValue instanceof Document) {
                    ((Document) newValue).addDocumentListener(this);
                }
            }

            updateCachedMetrics();
            viewChanged(0, true);
        }

        /**
         * Text was inserted into the document.
         */
        public void insertUpdate(DocumentEvent evt) {
            update(evt);
        }

        /**
         * Text was removed from the document.
         */
        public void removeUpdate(DocumentEvent evt) {
            update(evt);
        }

        /**
         * Text attributes were changed.  In a source-code editor based on
         * StyledDocument, attribute changes should be applied automatically
         * in response to inserts and removals.  Since we're already
         * listening for those, this method should be redundant, but YMMV.
         */
        public void changedUpdate(DocumentEvent evt) {
//      update(evt);
        }

        /**
         * If the undo was confined to a single line, invalidate that
         * line's height.  Otherwise, invalidate them all.
         */
        private void update(DocumentEvent evt) {
            Element map = text.getDocument().getDefaultRootElement();
            int line = map.getElementIndex(evt.getOffset());
            DocumentEvent.ElementChange ec = evt.getChange(map);
            viewChanged(line, ec != null);
        }
    }
}
