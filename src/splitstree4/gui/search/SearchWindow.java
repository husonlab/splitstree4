/**
 * SearchWindow.java
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
package splitstree4.gui.search;

/**
 * FIND AND REPLACE METHODS:
 * -------------------------
 * The next methods are used in order to compute the
 *  Find/Replace... tool in the Editor Window. This is located in
 *  the Edit menu of the Editor Window and is called from MainViewerMenuBar()
 *  when we create the menu bar.
 *
 * @author Miguel Jett�
 * @since March 10th, 2004
 * New revision: April 14th, 2005
 *  -- Regular expressions were added to the find and replace actions.
 *  -- Changed the the whole look of the DialogBox with new options.
 */

import jloda.gui.WindowListenerAdapter;
import jloda.gui.commands.CommandManager;
import jloda.gui.director.IDirectableViewer;
import jloda.gui.director.IDirector;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.RememberingComboBox;
import splitstree4.gui.Director;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;


public class SearchWindow implements IDirectableViewer {
    private JFrame frame; //Window of the search dialog.
    private JPanel mainPanel;
    private Director dir;
    private SearchActions searchActions;
    boolean uptodate;

    //Fields used when constructing GUI. Note: here we only declare
    //gui components which we need to access later. Other stuff is declared locally.
    private JButton nextButton;
    private JButton prevButton;
    private JButton findAllButton;
    private JButton replaceButton;
    private JButton replaceFindButton;
    private JButton replaceAllButton;

    private RememberingComboBox searchTextCombo;
    private RememberingComboBox replaceTextCombo;

    private JRadioButton entireDocRadio;
    private JRadioButton selectionOnlyRadio;
    private JCheckBox ignoreCaseCheckBox;
    private JCheckBox wrapAroundCheckBox;
    private JCheckBox wholeWordCheckBox;
    private JCheckBox regExpCheckBox;

    private JLabel messageLabel;

    /**
     * Initialises the dialog and the gui
     *
     * @param dir
     */
    public SearchWindow(Director dir) {

        this.dir = dir;
        this.searchActions = new SearchActions(this, dir);
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (ProgramProperties.getProgramIcon() != null)
            frame.setIconImage(ProgramProperties.getProgramIcon().getImage());
        mainPanel = null;
        mainPanel = createGUI();
        frame.getContentPane().add(mainPanel);

        frame.setSize(480, 250);
        dir.setViewerLocation(this);
        frame.setResizable(true);
        setTitle(getNewTitle(dir));

        //

        setSearchOptions(new SearchOptions());
        setUpActions();
        setUpCloseWindowHandler();
        if (dir.isInUpdate())
            lockUserInput();
        else
            unlockUserInput();
        setVisible(true);

        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowActivated(WindowEvent windowEvent) {
                updateView(IDirector.TITLE);
            }
        });

    }

    /**
     * ****************************************
     * Routines for constructing panel.
     */

    private JPanel createGUI() {

        if (mainPanel != null)
            return mainPanel;
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));

        //Do the Top Left Panel
        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.PAGE_AXIS));


        JLabel findLabel = new JLabel("Find:");
        JLabel replaceLabel = new JLabel("Replace:");

        JPanel tempPanel2 = new JPanel();
        tempPanel2.setLayout(new BoxLayout(tempPanel2, BoxLayout.LINE_AXIS));
        findLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        Dimension labelSize = replaceLabel.getPreferredSize();
        findLabel.setMinimumSize(labelSize);
        findLabel.setMaximumSize(labelSize);
        findLabel.setPreferredSize(labelSize);
        tempPanel2.add(findLabel);
        searchTextCombo = new RememberingComboBox();

        findLabel.setLabelFor(searchTextCombo);
        searchTextCombo.setMaximumSize(new Dimension(Short.MAX_VALUE, labelSize.height + 20));
        tempPanel2.add(searchTextCombo);
        tempPanel.add(tempPanel2);

        tempPanel2 = new JPanel();
        tempPanel2.setLayout(new BoxLayout(tempPanel2, BoxLayout.LINE_AXIS));
        replaceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        replaceLabel.setMinimumSize(labelSize);
        replaceLabel.setMaximumSize(labelSize);
        tempPanel2.add(replaceLabel);
        replaceTextCombo = new RememberingComboBox();
        replaceLabel.setLabelFor(replaceTextCombo);
        replaceTextCombo.setMaximumSize(new Dimension(Short.MAX_VALUE, labelSize.height + 20));

        tempPanel2.add(replaceTextCombo);
        tempPanel.add(tempPanel2);


        leftPanel.add(tempPanel);

        //Do the bottom left panel
        tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.LINE_AXIS));

        JPanel scopePanel = new JPanel();
        scopePanel.setLayout(new BoxLayout(scopePanel, BoxLayout.PAGE_AXIS));
        Border etchedBorder = BorderFactory.createEtchedBorder();
        scopePanel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Replace-All Scope"));
        ButtonGroup group = new ButtonGroup();
        entireDocRadio = new JRadioButton("Entire Document");
        scopePanel.add(entireDocRadio);
        group.add(entireDocRadio);
        selectionOnlyRadio = new JRadioButton("Selection Only");
        scopePanel.add(selectionOnlyRadio);
        group.add(selectionOnlyRadio);
        scopePanel.add(Box.createVerticalGlue());
        messageLabel = new JLabel("");
        messageLabel.setEnabled(false);
        scopePanel.add(messageLabel);
        scopePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        tempPanel.add(scopePanel);

        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.LINE_AXIS));

        JPanel tempPanel3 = new JPanel();
        tempPanel3.setLayout(new BoxLayout(tempPanel3, BoxLayout.PAGE_AXIS));
        ignoreCaseCheckBox = new JCheckBox("Ignore Case");
        tempPanel3.add(ignoreCaseCheckBox);
        wrapAroundCheckBox = new JCheckBox("Wrap around");
        tempPanel3.add(wrapAroundCheckBox);
        wholeWordCheckBox = new JCheckBox("Whole words");
        tempPanel3.add(wholeWordCheckBox);
        regExpCheckBox = new JCheckBox("Regular expression");
        tempPanel3.add(regExpCheckBox);
        optionPanel.add(tempPanel3);

        optionPanel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Search Options"));
        tempPanel.add(optionPanel);

        leftPanel.add(tempPanel);

        mainPanel.add(leftPanel);

        //Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");
        findAllButton = new JButton("Find All");
        replaceButton = new JButton("Replace");
        replaceFindButton = new JButton("Replace & Find");
        replaceAllButton = new JButton("Replace All");
        int buttonHeight = replaceFindButton.getPreferredSize().height;
        nextButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));
        prevButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));
        replaceButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));
        replaceAllButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));
        replaceFindButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));
        findAllButton.setMaximumSize(new Dimension(Short.MAX_VALUE, buttonHeight));

        rightPanel.add(nextButton);
        rightPanel.add(prevButton);
        rightPanel.add(findAllButton);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(replaceButton);
        rightPanel.add(replaceFindButton);
        rightPanel.add(replaceAllButton);
        rightPanel.setMaximumSize(new Dimension(replaceFindButton.getPreferredSize().width, Short.MAX_VALUE));
        mainPanel.add(rightPanel);

        // TODO: this keyadapter is not used
        //Make it so user can press return in search window to initiate search
        KeyAdapter ka = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    nextButton.getAction().actionPerformed(null);
                }
            }
        };

        searchTextCombo.addKeyListener(ka);
        replaceTextCombo.addKeyListener(ka);

        return mainPanel;
    }

    /**
     * set the director to a new director
     * If this is used, frame is set not to destroy itself
     *
     * @param dir
     */
    public void changeDirector(final Director dir) {
        this.frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.dir = dir;
        searchActions.setDirector(dir);

        updateView(IDirector.TITLE);
        setUptoDate(true);
    }

    /**
     * setup the actions
     */
    private void setUpActions() {
        nextButton.setAction(searchActions.getFindNextAction());
        prevButton.setAction(searchActions.getFindPrevAction());
        findAllButton.setAction(searchActions.getFindAllAction());
        replaceButton.setAction(searchActions.getReplaceAction());
        replaceFindButton.setAction(searchActions.getReplaceFindAction());
        replaceAllButton.setAction(searchActions.getReplaceAllAction());
    }

    /**
     * get all the actions
     *
     * @return actions
     */
    public SearchActions getActions() {
        return searchActions;
    }

    /**
     * Handle close window action... rather then destroying the window, we make it invisible.
     */
    private void setUpCloseWindowHandler() {
        final SearchWindow me = this;
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                me.setVisible(false);
            }
        });
    }

    /************************************
     * Access and control routines, including implementations of DirectableViewer methods
     */

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * gets the title currently used in the frame
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * Constructs title for find/replace window
     *
     * @param dir
     * @return new title
     */
    private String getNewTitle(Director dir) {
        String newTitle;
        if (dir.getID() == 1)
            newTitle = "Find/Replace - " + dir.getDocument().getTitle();
        else
            newTitle = "Find/Replace - " + dir.getDocument().getTitle() + " [" + dir.getID() + "]";
        return newTitle;
    }

    /**
     * sets title for find/replace window
     *
     * @param newTitle
     */
    private void setTitle(String newTitle) {
        if (!getFrame().getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }


    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            setTitle(getNewTitle(dir));
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
    public void destroyView() throws CanceledException {
        this.getFrame().dispose();
    }

    /**
     * Set visibility of the  window.
     *
     * @param visible
     */
    public void setVisible(boolean visible) {
        frame.setVisible(visible);
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
     * Extract the search options from the buttons set by the gui.
     *
     * @return SearchOptions
     */
    public SearchOptions getSearchOptions() {
        SearchOptions options = new SearchOptions();
        options.caseInsensitive = ignoreCaseCheckBox.isSelected();
        options.wholeWordOnly = (wholeWordCheckBox.isSelected());
        options.regExpression = (regExpCheckBox.isSelected());
        options.wrapSearch = (wrapAroundCheckBox.isSelected());
        options.replaceAllSelectionOnly = (selectionOnlyRadio.isSelected());
        return options;
    }

    public void setSearchOptions(SearchOptions options) {
        ignoreCaseCheckBox.setSelected(options.caseInsensitive);
        wholeWordCheckBox.setSelected(options.wholeWordOnly);
        regExpCheckBox.setSelected(options.regExpression);
        wrapAroundCheckBox.setSelected(options.wrapSearch);
        selectionOnlyRadio.setSelected(options.replaceAllSelectionOnly);
        entireDocRadio.setSelected(!options.replaceAllSelectionOnly);
    }


    /**
     * Returns contents of current search text field
     *
     * @return String
     */
    public String getSearchText() {
        return searchTextCombo.getCurrentText(true);
    }

    /**
     * Returns contents of current replace text field.
     *
     * @return String.
     */
    public String getReplaceText() {
        return replaceTextCombo.getCurrentText(true);
    }

    public void setMessage(String s) {
        messageLabel.setText(s);

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
        return "SearchWindow";
    }
}
