/**
 * FilterCharactersPanel.java 
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
package splitstree4.gui.algorithms.filter;

import jloda.swing.director.IUpdateableView;
import splitstree4.core.Document;
import splitstree4.gui.Director;
import splitstree4.nexus.Assumptions;
import splitstree4.nexus.Characters;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Iterator;

/**
 * DESCRIPTION
 *
 * @author huson
 *         Date: 19-Dec-2003
 */
public class FilterCharactersPanel extends JPanel implements IUpdateableView, ChangeListener {
    private Director dir;
    private FilterCharactersActions actions;

    public static final String MISSINGLABEL = "Allowed missing data level per site:";

    //private JCheckBox codon1cb = null;
    //private JCheckBox codon2cb = null;
    //private JCheckBox codon3cb = null;

    private JLabel descriptionLabel = new JLabel();

    private JCheckBox excludeGaps = new JCheckBox();
    private JCheckBox excludeConstant = new JCheckBox();
    private JCheckBox excludeNonParsimony = new JCheckBox();
    private JLabel sliderLabel = null;
    private JSlider  missingSlider = new JSlider(JSlider.HORIZONTAL,0,100,100);

    /**
     * sets up the algorithms window
     *
     * @param dir   Director
     */
    public FilterCharactersPanel(Director dir) {
        this.dir = dir;
        actions = new FilterCharactersActions(dir);
        setup();
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */
    public FilterCharactersActions getActions() {
        return actions;
    }

    /**
     * ask view to update itself.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            return;
        }
        getActions().setEnableCritical(true);
        Document doc = dir.getDocument();

        //Update codon check boxes from the assumptions block.
        JCheckBox codon1 = (JCheckBox) (getActions().getCodon1().getValue(FilterCharactersActions.JCHECKBOX));
        codon1.setSelected(!doc.isValidByName(Assumptions.NAME)
                || doc.getAssumptions().getExcludeCodon1());
        JCheckBox codon2 = (JCheckBox) (getActions().getCodon2().getValue(FilterCharactersActions.JCHECKBOX));
        codon2.setSelected(!doc.isValidByName(Assumptions.NAME)
                || doc.getAssumptions().getExcludeCodon2());
        JCheckBox codon3 = (JCheckBox) (getActions().getCodon3().getValue(FilterCharactersActions.JCHECKBOX));
        codon3.setSelected(!doc.isValidByName(Assumptions.NAME)
                || doc.getAssumptions().getExcludeCodon3());
        if (doc.isValidByName(Characters.NAME)) {
            codon1.setEnabled(doc.getCharacters().isNucleotides());
            codon2.setEnabled(doc.getCharacters().isNucleotides());
            codon3.setEnabled(doc.getCharacters().isNucleotides());
        }

        //Display the list of excluded characters (from the assumptions block)
        String text = "";
        if (doc.isValidByName(Assumptions.NAME) && doc.getAssumptions().getExChar() != null) {
            StringBuilder buf = new StringBuilder();
            Iterator it = doc.getAssumptions().getExChar().listIterator();
            int first = 0, prev = 0;
            while (it.hasNext()) {
                int c = (Integer) it.next();
                if (first == 0)
                    first = prev = c;
                else if (c == prev + 1)
                    prev = c;
                else // end of interval
                {
                    if (prev == first)
                        buf.append(" ").append(first);
                    else
                        buf.append(" ").append(first).append("-").append(prev);
                    first = prev = c;
                }
            }
            if (first > 0) {
                if (prev == first)
                    buf.append(" ").append(first);
                else
                    buf.append(" ").append(first).append("-").append(prev);
            }
            text = buf.toString();
        }
        System.err.println("Excluded sites:");
        System.err.println(text);
        //JTextArea textArea = (JTextArea) getActions().getInput().getValue(FilterCharactersActions.JTEXTAREA);
        //textArea.setText(text);

        //Prepare message below
        if (dir.getDocument().isValidByName(Characters.NAME)) {
            Characters characters = dir.getDocument().getCharacters();
            if (characters.getNactive() == -1)
                descriptionLabel.setText("nChar= " + characters.getNchar());
            else
                descriptionLabel.setText("nChar= " + characters.getNactive() +
                        " (" + (characters.getNchar() - characters.getNactive())
                        + " of " + characters.getNchar() + " hidden)");
        } else
            descriptionLabel.setText(" ");


        excludeGaps.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeGaps());
        excludeConstant.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeConstant() != 0);
        excludeNonParsimony.setSelected(doc.isValidByName(Assumptions.NAME)
                && doc.getAssumptions().getExcludeNonParsimony());
        excludeGaps.setEnabled(doc.isValidByName(Characters.NAME));
        excludeConstant.setEnabled(doc.isValidByName(Characters.NAME));
        excludeNonParsimony.setEnabled(doc.isValidByName(Characters.NAME));

        double missingVal = 1.0;
        if (doc.isValidByName(Assumptions.NAME))
              missingVal = doc.getAssumptions().getExcludeMissing();
        missingSlider.setValue((int)Math.round(missingVal*100));
        sliderLabel.setText(slideValue(missingSlider.getValue()));
        missingSlider.setEnabled(doc.isValidByName(Characters.NAME));

        getActions().updateEnableState();
    }

    /**
     * sets up the  panel
     */
    private void setup() {
        JPanel panel = this;

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 5, 1, 5);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        panel.add(new JLabel("Enter sites to exclude:"), constraints);

        JTextArea textArea = new JTextArea();
        AbstractAction action = getActions().getInput();
        action.putValue(FilterCharactersActions.JTEXTAREA, textArea);
        textArea.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

        JScrollPane scrollP = new JScrollPane(textArea);

        // ((JTextArea) comp).addPropertyChangeListener(action);
        // textArea.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        panel.add(scrollP, constraints);

        JLabel label = new JLabel("Exclude codons:");
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        constraints.ipadx = GridBagConstraints.EAST;

        panel.add(label, constraints);

        action = getActions().getCodon1();
        JCheckBox codon1cb = new JCheckBox(action);
        action.putValue(FilterCharactersActions.JCHECKBOX, codon1cb);
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(codon1cb, constraints);

        action = getActions().getCodon2();
        JCheckBox codon2cb = new JCheckBox(getActions().getCodon2());
        action.putValue(FilterCharactersActions.JCHECKBOX, codon2cb);
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        panel.add(codon2cb, constraints);

        action = getActions().getCodon3();
        JCheckBox codon3cb = new JCheckBox(getActions().getCodon2());
        action.putValue(FilterCharactersActions.JCHECKBOX, codon3cb);
        constraints.gridx = 3;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        panel.add(codon3cb, constraints);

        JButton apply = new JButton(getActions().getApply(excludeGaps, excludeNonParsimony,
                excludeConstant,missingSlider));
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        panel.add(apply, constraints);

        JButton reset = new JButton(getActions().getReset(excludeGaps, excludeNonParsimony,
                excludeConstant));
        constraints.gridx = 5;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        panel.add(reset, constraints);

        JButton delete = new JButton(getActions().getDelete());
        constraints.gridx = 5;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        panel.add(delete, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.ipadx = GridBagConstraints.WEST;
        panel.add(new JLabel("Exclude gapped sites:"), constraints);
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(excludeGaps, constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.ipadx = GridBagConstraints.WEST;
        panel.add(new JLabel("Exclude constant sites:"), constraints);
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(excludeConstant, constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.ipadx = GridBagConstraints.WEST;
        panel.add(new JLabel("Exclude non parsimonious sites:"), constraints);
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(excludeNonParsimony, constraints);


        missingSlider.setMajorTickSpacing(20);
        missingSlider.setMinorTickSpacing(1);
        missingSlider.setPaintTicks(true);
        missingSlider.setPaintLabels(true);
        missingSlider.setBorder(
                BorderFactory.createEmptyBorder(0,0,10,0));
        Font font = new Font("Serif", Font.PLAIN, 15);
        missingSlider.setFont(font);
        missingSlider.setToolTipText("Exclude sites with more than this level of missing data (1.0 = include all)");
        missingSlider.addChangeListener(this);
        sliderLabel = new JLabel(slideValue(100));
        Dimension d =  sliderLabel.getPreferredSize();
        sliderLabel.setMinimumSize(new Dimension(d.width+60,d.height));
        sliderLabel.setText(slideValue(missingSlider.getValue()));

        //sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 2;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(sliderLabel, constraints);
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.ipadx = GridBagConstraints.CENTER;
        panel.add(missingSlider, constraints);

        descriptionLabel.setText(" ");
        Box box = Box.createHorizontalBox();
        box.add(descriptionLabel);
        box.add(Box.createHorizontalStrut(500));
        box.setBorder(BorderFactory.createEtchedBorder());
        box.setMinimumSize(new Dimension(100, 20));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 1;
        constraints.weightx = 1;
        constraints.gridwidth = 6;
        constraints.gridheight = 1;
        constraints.gridx = 0;
        constraints.gridy = 9;
        panel.add(box, constraints);
    }

    /**
     * Wierd effect in Swing... the whole window changes when the length of the
     * Slider label changes. This tries to fix that.
     * @return  Padding string
     */
    private  String slideValue(int val) {
        return MISSINGLABEL+" ("+val+"%)";
    }

    /**
     * Listen to the slider and update percentage
     * @param e
     */
    public void stateChanged(ChangeEvent e) {
        sliderLabel.setText(slideValue(missingSlider.getValue()));
    }

}
