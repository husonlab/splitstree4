/**
 * FontChooser.java
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
package splitstree4.util;

// FontChooser.java
// A font chooser that allows users to pick a font by name, size, style, and
// color.  The color selection is provided by a JColorChooser pane.  This
// dialog builds an AttributeSet suitable for use with JTextPane.
// Source: http://examples.oreilly.com/jswing2/code/ch12/FontChooser.java

//

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * font chooser dialog
 */
public class FontChooser extends JDialog implements ActionListener {

    JColorChooser colorChooser;
    final JComboBox fontName;
    final JCheckBox fontBold;
    final JCheckBox fontItalic;
    final JTextField fontSize;
    final JLabel previewLabel;
    final SimpleAttributeSet attributes;
    Font newFont;
    Color newColor;

    /**
     * construct a new font chooser
     *
     * @param parent          the parent window
     * @param useColorChooser supply color chooser, too, if desired
     */
    public FontChooser(JFrame parent, boolean useColorChooser) {
        super(parent, "Font Chooser", true);
        if (useColorChooser)
            setSize(450, 450);
        else
            setSize(400, 180);
        this.setLocationRelativeTo(parent);

        attributes = new SimpleAttributeSet();

        // Make sure that any way the user cancels the window does the right thing
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeAndCancel();
            }
        });

        // Start the long process of setting up our interface
        Container c = getContentPane();

        JPanel fontPanel = new JPanel();
        fontName = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

        for (int i = 0; i < fontName.getItemCount(); i++)
            if (((String) fontName.getItemAt(i)).equalsIgnoreCase("default")) {
                fontName.setSelectedIndex(i);
                break;
            }
        fontName.addActionListener(this);
        fontSize = new JTextField("10", 4);
        fontSize.setHorizontalAlignment(SwingConstants.RIGHT);
        fontSize.addActionListener(this);
        fontBold = new JCheckBox("Bold");
        // fontBold.setSelected(true);
        fontBold.addActionListener(this);
        fontItalic = new JCheckBox("Italic");
        fontItalic.addActionListener(this);

        fontPanel.add(fontName);
        fontPanel.add(new JLabel(" Size: "));
        fontPanel.add(fontSize);
        fontPanel.add(fontBold);
        fontPanel.add(fontItalic);

        c.add(fontPanel, BorderLayout.NORTH);

        // Set up the color chooser panel and attach a change listener so that color
        // updates get reflected in our preview label.
        if (useColorChooser) {
            colorChooser = new JColorChooser(Color.black);
            colorChooser.getSelectionModel()
                    .addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            updatePreviewColor();
                        }
                    });
            c.add(colorChooser, BorderLayout.CENTER);
        }
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewLabel = new JLabel("Here's a sample of this font.");
        if (colorChooser != null)
            previewLabel.setForeground(colorChooser.getColor());
        previewLabel.setBackground(Color.white);
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        // Add in the Ok and Cancel buttons for our dialog box
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                closeAndSave();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                closeAndCancel();
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(okButton);
        controlPanel.add(cancelButton);
        previewPanel.add(controlPanel, BorderLayout.SOUTH);

        // Give the preview label room to grow.
        previewPanel.setMinimumSize(new Dimension(100, 100));
        previewPanel.setPreferredSize(new Dimension(100, 100));

        c.add(previewPanel, BorderLayout.SOUTH);
    }

    // Ok, something in the font changed, so figure that out and make a
    // new font for the preview label

    public void actionPerformed(ActionEvent ae) {
        // Check the name of the font
        if (!StyleConstants.getFontFamily(attributes)
                .equals(fontName.getSelectedItem())) {
            StyleConstants.setFontFamily(attributes,
                    (String) fontName.getSelectedItem());
        }
        // Check the font size (no error checking yet)
        if (StyleConstants.getFontSize(attributes) !=
                Integer.parseInt(fontSize.getText())) {
            StyleConstants.setFontSize(attributes,
                    Integer.parseInt(fontSize.getText()));
        }
        // Check to see if the font should be bold
        if (StyleConstants.isBold(attributes) != fontBold.isSelected()) {
            StyleConstants.setBold(attributes, fontBold.isSelected());
        }
        // Check to see if the font should be italic
        if (StyleConstants.isItalic(attributes) != fontItalic.isSelected()) {
            StyleConstants.setItalic(attributes, fontItalic.isSelected());
        }
        // and update our preview label
        updatePreviewFont();
    }

    // Get the appropriate font from our attributes object and update
    // the preview label

    protected void updatePreviewFont() {
        String name = StyleConstants.getFontFamily(attributes);
        boolean bold = StyleConstants.isBold(attributes);
        boolean ital = StyleConstants.isItalic(attributes);
        int size = StyleConstants.getFontSize(attributes);

        Font f = new Font(name, (bold ? Font.BOLD : 0) +
                (ital ? Font.ITALIC : 0), size);
        previewLabel.setFont(f);
    }

    // Get the appropriate color from our chooser and update previewLabel

    protected void updatePreviewColor() {
        if (colorChooser != null)
            previewLabel.setForeground(colorChooser.getColor());
        // Manually force the label to repaint
        previewLabel.repaint();
    }

    public Font getNewFont() {
        return newFont;
    }

    public Color getNewColor() {
        return newColor;
    }

    public AttributeSet getAttributes() {
        return attributes;
    }

    public void closeAndSave() {
        // Save font & color information
        newFont = previewLabel.getFont();
        newColor = previewLabel.getForeground();

        // Close the window
        setVisible(false);
    }

    public void closeAndCancel() {
        // Erase any font information and then close the window
        newFont = null;
        newColor = null;
        setVisible(false);
    }
}
