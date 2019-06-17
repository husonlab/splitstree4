/**
 * ConfidenceWindow.java
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
package splitstree4.gui.confidence;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.WindowListenerAdapter;
import jloda.util.ProgramProperties;
import splitstree4.gui.Director;
import splitstree4.main.SplitsTreeProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * window to choose how to hi-confidence of edges
 *
 * @author huson & Franz
 *         <p/>
 *         17.2.2004
 */
public class ConfidenceWindow implements IDirectableViewer {
    private boolean uptodate = false;
    private Director dir;
    private ConfidenceWindowActions actions;
    private ConfidenceWindowMenuBar menuBar;
    private JFrame frame;

    /**
     * sets up the algorithms window
     *
     * @param dir
     */
    public ConfidenceWindow(Director dir) {
        this.dir = dir;
        actions = new ConfidenceWindowActions(this, dir);
        menuBar = new ConfidenceWindowMenuBar(this, dir);
        setUptoDate(true);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (ProgramProperties.getProgramIcon() != null)
            frame.setIconImage(ProgramProperties.getProgramIcon().getImage());
        frame.setJMenuBar(menuBar);
        frame.setSize(300, 150);
        dir.setViewerLocation(this);
        setTitle(dir);

        frame.getContentPane().add(getPanel());
        frame.setVisible(true);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                actions.getClose().actionPerformed(null);
            }
        });
    }

    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(Director dir) {
        String newTitle = "Confidence Highlighting - " + SplitsTreeProperties.getVersion();

        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * returns the actions object associated with the window
     *
     * @return actions
     */
    public ConfidenceWindowActions getActions() {
        return actions;
    }


    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what is to be updated
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        uptodate = false;
        getActions().setEnableCritical(true);


        getActions().updateEnableState();
        uptodate = true;
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        getActions().setEnableCritical(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        getActions().setEnableCritical(true);
        getActions().updateEnableState();
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() {
        this.getFrame().dispose();
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    /**
     * returns the frame of the window
     */
    public JFrame getFrame() {
        return frame;
    }

    private JPanel panel = null;

    /**
     * gets the content pane
     *
     * @return the content pane
     */
    private JPanel getPanel() {
        if (panel != null)
            return panel;
        // TODO: fix the layout of this dialog!
        panel = new JPanel();

        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        Box box = Box.createHorizontalBox();
        box.add(Box.createGlue());
        box.add(new JLabel("Highlight Edge Confidence:"));
        box.add(Box.createGlue());
        box.setBorder(BorderFactory.createEtchedBorder());
        panel.add(box);

        JCheckBox cbox = new JCheckBox();
        cbox.setAction(getActions().getEdgeWidth(cbox));
        panel.add(Box.createHorizontalBox().add(cbox));
        cbox = new JCheckBox();
        cbox.setAction(getActions().getEdgeShading(cbox));
        panel.add(Box.createHorizontalBox().add(cbox));
        cbox = new JCheckBox();
        cbox.setAction(getActions().getSelectedOnly(cbox));
        panel.add(Box.createHorizontalBox().add(cbox));

        panel.add(Box.createGlue());

        box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEtchedBorder());
        box.add(new JButton(getActions().getApply()));
        box.add(Box.createGlue());
        box.add(new JButton(getActions().getClose()));
        panel.add(box);
        return panel;
    }

    /**
     * gets the title of this viewer
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return null;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return false;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "ConfidenceWindow";
    }
}
