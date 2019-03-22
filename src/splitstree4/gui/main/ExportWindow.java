/**
 * ExportWindow.java
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

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.graphview.PhyloGraphView;
import jloda.swing.util.*;
import jloda.util.Basic;
import splitstree4.core.Document;
import splitstree4.externalIO.exports.ExportManager;
import splitstree4.externalIO.exports.ExporterAdapter;
import splitstree4.externalIO.exports.ExporterInfo;
import splitstree4.gui.Director;
import splitstree4.gui.DirectorActions;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Network;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * the export window
 */
public class ExportWindow implements IDirectableViewer {
    private boolean uptodate = true;
    java.util.List allActions = new LinkedList();
    JLabel descriptionLabel = null;
    private JFrame frame;
    private JPanel top;
    private JPanel button;
    private Director dir;
    private Document doc;
    private DefaultListModel list = null;
    private ActionJList jlist = null;
    private String selectedExport = null;
    private ExportWindow viewer;
    private Set selectedBlocksSet = new HashSet();
    private List selectedBlocksList = new LinkedList(); // list in correct order

    //constructor

    public ExportWindow(Director dir) {
        viewer = this;
        this.dir = dir;
        doc = dir.getDocument();

        list = new DefaultListModel();
        frame = new JFrame();
        if (ProgramProperties.getProgramIcon() != null)
            frame.setIconImage(ProgramProperties.getProgramIcon().getImage());
        setTitle(dir);
        frame.setJMenuBar(setupMenuBar());
        descriptionLabel = new JLabel();
        frame.setSize(500, 500);
        dir.setViewerLocation(this);

        // make sure we remove this viewer when it is closed
        frame.addWindowListener(new WindowListenerAdapter() {
            public void windowClosing(WindowEvent event) {
                viewer.dir.removeViewer(viewer);
                frame.dispose();
            }
        });
        try {
            this.MakeExportWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.getContentPane().add(top, BorderLayout.CENTER);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        unlockUserInput();
        frame.setVisible(true);
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
     */
    public void updateView(String what) {
        if (what.equals(Director.TITLE)) {
            setTitle(dir);
            return;
        }
        setUptoDate(false);
        lockUserInput();

        frame.remove(top);
        try {
            this.MakeExportWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.getContentPane().add(top, BorderLayout.CENTER);
        frame.setVisible(true);
        unlockUserInput();
        // Set up to date
        this.uptodate = true;
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null &&
                    (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(false);
        }
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        for (Object allAction : allActions) {
            AbstractAction action = (AbstractAction) allAction;
            if (action.getValue(DirectorActions.CRITICAL) != null && (Boolean) action.getValue(DirectorActions.CRITICAL))
                action.setEnabled(true);
        }
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() {
        this.getFrame().dispose();
    }

    public void GUIClosed() {
        dir.removeViewer(this);
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
     * gets the title of this viewer
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }


    /**
     * sets the title
     *
     * @param dir the director
     */
    public void setTitle(Director dir) {
        String newTitle;

        if (dir.getID() == 1)
            newTitle = "Export  - " + dir.getDocument().getTitle()
                    + " " + SplitsTreeProperties.getVersion();
        else
            newTitle = "Export  - " + dir.getDocument().getTitle()
                    + " [" + dir.getID() + "] - " + SplitsTreeProperties.getVersion();
        if (!frame.getTitle().equals(newTitle))
            frame.setTitle(newTitle);
    }

    /**
     * setup the menu bar
     */
    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(dir.getActions().getMenuTitleAction("File", 'F'));
        menu.addSeparator();
        menu.add(getCloseAction());
        menu.addSeparator();
        menu.add(dir.getActions().getQuit());
        menuBar.add(menu);
        menuBar.add(menu);
        return menuBar;
    }

    private void MakeExportWindow() {
        top = new JPanel();
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titleBorder = new TitledBorder(loweredetched, "Export Window");
        Border space = BorderFactory.createEmptyBorder(20, 15, 15, 15);
        Border exportBorder = BorderFactory.createCompoundBorder(space, titleBorder);

        GridBagLayout gridBagLayout = new GridBagLayout();
        top.setLayout(gridBagLayout);
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 2;

        top.setBorder(exportBorder);
        top.add(MakeBaseClassWindow(), constraints);

        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 2;

        top.add(MakeSuitableExports(doc, new LinkedList()), constraints);

        /**
         * buttons
         */

        // 'APPLY': run algorithm with user-options, without closing this window.
        JButton applyB = new JButton(getApplyAction());

        // 'CANCEL': close this window
        JButton cancelB = new JButton(getCancelAction());

        button = new JPanel();
        LayoutManager buttonLayout = new BoxLayout(button, BoxLayout.X_AXIS);
        button.setLayout(buttonLayout);
        button.add(Box.createHorizontalGlue());
        button.add(cancelB);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.add(applyB);
        button.add(Box.createRigidArea(new Dimension(10, 0)));
        button.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
        button.setSize(300, 60);
        frame.getRootPane().setDefaultButton(applyB);

    }

    /**
     * list of suitable exporters
     *
     * @param doc
     * @param blocks selected blocks
     * @return exporters
     */
    private JScrollPane MakeSuitableExports(Document doc, Collection blocks) {
        String[] categories = null;
        try {

            categories = ExportManager.getSuitableExporter(doc, blocks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        list.clear();
        for (String category : categories) list.addElement(category);
        jlist = new ActionJList(list);
        if (!list.get(0).equals("No suitable Exporter found")) jlist.setSelectedIndex(0);

        //Using custom actionListener for JList (listens for double-click and return key)
        jlist.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // int index = jlist.getSelectedIndex();
                try {
                    selectedExport = (String) jlist.getSelectedValue();
                    if (selectedExport != null && !selectedExport.equalsIgnoreCase("No suitable Exporter found")) {
                        saveDialog();
                        dir.removeViewer(viewer);
                        frame.dispose();
                    }
                } catch (Exception e1) {
                    new Alert(null, "There was an error saving the file: " + e1.getMessage());

                    e1.printStackTrace();
                }
            }
        });
        JScrollPane ex = new JScrollPane(jlist);
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titleBorder = new TitledBorder(loweredetched, "Available Exporter");
        Border space = BorderFactory.createEmptyBorder(20, 15, 15, 15);
        Border exportBorder = BorderFactory.createCompoundBorder(space, titleBorder);
        ex.setBorder(exportBorder);
        ex.setSize(220, 330);
        ex.setPreferredSize(new Dimension(220, 330));
        ex.setMinimumSize(new Dimension(220, 330));

        ex.setToolTipText("Click on the exporter you would like to use.");

        return ex;
    }

    /**
     * makes the nexus classes buttons
     *
     * @return class buttons
     */
    private JPanel MakeBaseClassWindow() {
        JPanel baseClass = new JPanel();
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titleBorder = new TitledBorder(loweredetched, "Available Datatypes");
        Border space = BorderFactory.createEmptyBorder(20, 15, 15, 15);
        Border exportBorder = BorderFactory.createCompoundBorder(space, titleBorder);
        baseClass.setBorder(exportBorder);
        baseClass.setSize(200, 530);
        baseClass.setPreferredSize(new Dimension(200, 530));
        baseClass.setMinimumSize(new Dimension(200, 530));

        baseClass.setLayout(new BoxLayout(baseClass, BoxLayout.Y_AXIS));

        for (String name : Document.getListOfBlockNames()) {
            JCheckBox cbox = new JCheckBox(name);
            cbox.addActionListener(getCheckBoxAction(name));
            if (!dir.getDocument().isValidByName(name))
                cbox.setEnabled(false);
            baseClass.add(cbox);

        }
        baseClass.setToolTipText("Choose data blocks to export.");
        return baseClass;
    }

    // TODO: add to preferences
    static File lastSaveFile = new File(System.getProperty("user.dir"));

    private boolean saveDialog() {

        //First check for additional information.
//Check for any additional info that the exporter requires. For some exporters, this
        //asks for more information, e.g. through a dialog box.
        ExporterInfo additionalInfo = null;
        ExporterAdapter exportAdapter = ExportManager.getExporterAdapterByName(this.selectedExport);
        if (exportAdapter != null)
            additionalInfo = exportAdapter.requestAdditionalInfo(doc);
        if (additionalInfo != null && additionalInfo.userHasCancelled())
            return false;

        File file = null;
        if (!ProgramProperties.isMacOS()) {
            JFileChooser chooser = new JFileChooser(lastSaveFile);
            if (chooser.showSaveDialog(dir.getMainViewerFrame()) == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();

                if (file.exists() && JOptionPane.showConfirmDialog(null,
                        "This file already exists. " +
                                "Would you like to overwrite the existing file?",
                        "Save File",
                        JOptionPane.YES_NO_OPTION) == 1)
                    return false; // overwrite canceled
            }
        } else {
            FileDialog dialog = new FileDialog(dir.getMainViewerFrame(), "Save File", FileDialog.SAVE);
            dialog.setFile(lastSaveFile.getName());
            dialog.setDirectory(lastSaveFile.getParent());
            dialog.setVisible(true);

            if (dialog.getFile() != null)
                file = new File(dialog.getDirectory(), dialog.getFile());
            else
                return false;
        }

        try {
            dir.exportFile(file, this.selectedExport, selectedBlocksList, additionalInfo);
            lastSaveFile = file;
            return true;
        } catch (Exception ex) {
            System.err.println("Save failed: " + ex);
            return false;
        }


    }

    /* All the Actions of the window
    */

    private AbstractAction applyAction;

    public AbstractAction getApplyAction() {
        AbstractAction action = applyAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    selectedExport = (String) jlist.getSelectedValue();
                    if (selectedExport != null && !selectedExport.equalsIgnoreCase("No suitable Exporter found")) {
                        saveDialog();
                        dir.removeViewer(viewer);
                        frame.dispose();
                    } else {
                        JOptionPane.showMessageDialog(null, "No Exporter choosen", "Export",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                    new Alert(null, "Error exporting file: " + ex.getMessage());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply Export");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        allActions.add(action);
        return applyAction = action;
    }

    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);
                frame.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cancel export");
        allActions.add(action);
        return cancelAction = action;
    }


    private Map checkBoxAction = new HashMap();

    private AbstractAction getCheckBoxAction(final String blockName) {
        if (checkBoxAction.containsKey(blockName))
            return (AbstractAction) checkBoxAction.get(blockName);
        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                try {
                    JCheckBox cbox = ((JCheckBox) e.getSource());
                    String name = (String) getValue(AbstractAction.NAME);
                    if (cbox.isSelected()) {
                        if (name.equals(Network.NAME) && !selectedBlocksSet.contains(Network.NAME)) {
                            Document doc = dir.getDocument();
                            doc.getNetwork().syncPhyloGraphView2Network(doc.getTaxa(),
                                    (PhyloGraphView) dir.getViewerByClass(MainViewer.class));
                        }
                        selectedBlocksSet.add(name);
                    } else
                        selectedBlocksSet.remove(name);

                    // re-list in correct order:
                    selectedBlocksList.clear();
                    for (Object block : Document.getListOfBlockNames()) {
                        if (selectedBlocksSet.contains(block))
                            selectedBlocksList.add(block);
                    }

                    String[] exporter = ExportManager.getSuitableExporter(doc, selectedBlocksList);
                    list.clear();
                    for (String anExporter : exporter) {
                        list.addElement(anExporter);
                    }
                    if (!list.get(0).equals("No suitable Exporter found")) jlist.setSelectedIndex(0);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }

        };
        action.putValue(AbstractAction.NAME, blockName);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Export " + blockName);
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(DirectorActions.DEPENDS_ON, blockName);
        allActions.add(action);
        checkBoxAction.put(blockName, action);
        return action;
    }

    private AbstractAction closeAction;

    private AbstractAction getCloseAction() {
        if (closeAction != null)
            return closeAction;

        AbstractAction action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dir.removeViewer(viewer);
                frame.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        allActions.add(action);
        return closeAction = action;
    }

    /**
     * gets the frame
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
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
        return "ExportWindow";
    }
}
