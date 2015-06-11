/**
 * FilterCharactersActions.java 
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
package splitstree.gui.algorithms.filter;

import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.UpdateableActions;
import splitstree.nexus.Characters;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a characters window
 *
 * @author huson
 *         Date: 19-Dec-2003
 */
public class FilterCharactersActions implements UpdateableActions {
    private Director dir;
    private List all = new LinkedList();
    public static final String JCHECKBOX = "JCHECKBOX";
    public static final String JTEXTAREA = "JTEXTAREA";
    public static final String JSLIDER = "JSLIDER";
    public static final String JLABEL = "JLABEL";

    public FilterCharactersActions(Director dir) {
        this.dir = dir;
    }

    /**
     * enable or disable critical actions
     *
     * @param flag show or hide?
     */
    public void setEnableCritical(boolean flag) {
        DirectorActions.setEnableCritical(all, flag);
        // because we don't want to duplicate that code
    }

    /**
     * This is where we update the enable state of all actions!
     */
    public void updateEnableState() {
        DirectorActions.updateEnableState(dir, all);
        // because we don't want to duplicate that code

        boolean canDelete = false;
        Characters chars = dir.getDocument().getCharacters();
        if (chars != null) {
            if (chars.getMask() != null) {
                for (int c = 1; !canDelete && c <= chars.getNchar(); c++)
                    if (chars.isMasked(c))
                        canDelete = true;
            }
        }
        delete.setEnabled(canDelete);
    }

    /**
     * returns all actions
     *
     * @return actions
     */
    public List getAll() {
        return all;
    }

    // here we define the algorithms window specific actions:

    AbstractAction input;

    AbstractAction getInput() {
        if (input != null)
            return input;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Positions to exclude");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return input = action;
    }

    AbstractAction codon1;

    /**
     * ignore first codon postion
     *
     * @return action
     */
    AbstractAction getCodon1() {
        if (codon1 != null)
            return codon1;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!((JCheckBox) codon1.getValue(JCHECKBOX)).isSelected() && !((JCheckBox) codon2.getValue(JCHECKBOX)).isSelected()
                        && !((JCheckBox) codon3.getValue(JCHECKBOX)).isSelected()) {
                    ((JCheckBox) codon1.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon2.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon3.getValue(JCHECKBOX)).setSelected(true);
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude 1st codon position");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return codon1 = action;
    }

    AbstractAction codon2;

    /**
     * ignore second codon postion
     *
     * @return action
     */
    AbstractAction getCodon2() {
        if (codon2 != null)
            return codon2;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!((JCheckBox) codon1.getValue(JCHECKBOX)).isSelected() && !((JCheckBox) codon2.getValue(JCHECKBOX)).isSelected()
                        && !((JCheckBox) codon3.getValue(JCHECKBOX)).isSelected()) {
                    ((JCheckBox) codon1.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon2.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon3.getValue(JCHECKBOX)).setSelected(true);
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude 2nd codon position");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return codon2 = action;
    }

    AbstractAction codon3;

    /**
     * ignore third codon postion
     *
     * @return action
     */
    AbstractAction getCodon3() {
        if (codon3 != null)
            return codon3;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!((JCheckBox) codon1.getValue(JCHECKBOX)).isSelected() && !((JCheckBox) codon2.getValue(JCHECKBOX)).isSelected()
                        && !((JCheckBox) codon3.getValue(JCHECKBOX)).isSelected()) {
                    ((JCheckBox) codon1.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon2.getValue(JCHECKBOX)).setSelected(true);
                    ((JCheckBox) codon3.getValue(JCHECKBOX)).setSelected(true);
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Exclude 3rd codon position");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return codon3 = action;
    }


    AbstractAction reset;

    /**
     * use all positions
     *
     * @return action
     */
    AbstractAction getReset(final JCheckBox excludeGaps,
                            final JCheckBox excludeNonParsimony, final JCheckBox excludeConstant) {
        if (reset != null)
            return reset;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                excludeGaps.setSelected(false);
                excludeNonParsimony.setSelected(false);
                excludeConstant.setSelected(false);
                ((JCheckBox) codon1.getValue(JCHECKBOX)).setSelected(true);
                ((JCheckBox) codon2.getValue(JCHECKBOX)).setSelected(true);
                ((JCheckBox) codon3.getValue(JCHECKBOX)).setSelected(true);
                JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));
                inputTA.setText("");
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Reset to use all positions");
        action.putValue(AbstractAction.NAME, "Reset");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return reset = action;
    }

    AbstractAction apply;

    /**
     * apply
     *
     * @return action
     */
    AbstractAction getApply(final JCheckBox excludeGaps,
                            final JCheckBox excludeNonParsimony,
                            final JCheckBox excludeConstant,
                            final JSlider missingSlider) {
        if (apply != null)
            return apply;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                String cmd = "assume 'exclude";
                if (!excludeGaps.isSelected())
                    cmd += " no";
                cmd += " gaps";
                if (!excludeNonParsimony.isSelected())
                    cmd += " no";
                cmd += " nonparsimony";
                if (!excludeConstant.isSelected())
                    cmd += " no";
                cmd += " constant";
                if (((JCheckBox) codon1.getValue(JCHECKBOX)).isSelected())
                    cmd += " no";
                cmd += " codon1";
                if (((JCheckBox) codon2.getValue(JCHECKBOX)).isSelected())
                    cmd += " no";
                cmd += " codon2";
                if (((JCheckBox) codon3.getValue(JCHECKBOX)).isSelected())
                    cmd += " no";
                cmd += " codon3";
                int missingVal =  missingSlider.getValue();
                if (missingVal == 0)
                    cmd += " missing";
                else if (missingVal == 100)
                    cmd += " no missing";
                else
                    cmd += " missing="+(missingVal/100.0);

                JTextArea inputTA = ((JTextArea) input.getValue(JTEXTAREA));
                cmd += "; exchar= " + inputTA.getText() + "';update;";
                cmd = cmd.replaceAll("\n", " ");
                System.err.println("cmd: "+cmd);
                dir.execute(cmd);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply");
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        all.add(action);
        return apply = action;
    }

    AbstractAction delete;

    /**
     * delete all masked sites from dataset
     *
     * @return action
     */
    AbstractAction getDelete() {
        if (delete != null)
            return delete;
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.execute("deleteexcluded");
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Delete (rather than mask) all excluded characters from data set");
        action.putValue(AbstractAction.NAME, "Delete");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, Characters.NAME);
        all.add(action);
        return delete = action;
    }






}
