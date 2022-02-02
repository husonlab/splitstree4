/*
 * SplitsTreeProperties.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.main;

import jloda.swing.export.ExportImageDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.About;
import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;
import splitstree4.gui.main.MainViewer;
import splitstree4.gui.main.StatusBar;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * manages splitstrees properties, in cooperation with jloda.util.Properties
 *
 * @author huson
 * Date: 11-Nov-2004
 */
public class SplitsTreeProperties {
    static public final boolean ALLOW_CLUSTER_NETWORK = true;

    static boolean expertMode = false;
    // activate the reticulate extension
    final public static boolean ALLOW_RETICULATE = true;

    final public static String OPENFILE = "OpenFile";
    final public static String SAVEFILE = "SaveFile";
    final public static String EXPORTFILE = "ExportFile";
    final public static String RECENTFILES = "RecentFiles";
    final public static String MAXRECENTFILES = "MaxRecentFiles";

    final public static String TOOLBARITEMS = "ToolbarItems";
    final public static String SHOWTOOLBAR = "ShowToolbar";

    final public static String RECENTMETHODS = "RecentMethods";
    final public static String MAXRECENTMETHODS = "MaxRecentMethods";
    final public static String AUTOSCALE = "AutoScale";


    public static final String WINDOW_WIDTH = "WindowWidth";
    public static final String WINDOW_HEIGHT = "WindowHeight";
    public static final String LASTCOMMAND = "LastCommand";

    public static final boolean USE_SPLIT_PANE = true;

    /**
     * sets the program properties
     *
	 */
    public static void initializeProperties(String propertiesFile) {
        ProgramProperties.setProgramVersion(getVersion());
        ProgramProperties.setProgramIcons(ResourceManager.getIcons("SplitsTree16.png", "SplitsTree32.png", "SplitsTree48.png", "SplitsTree64.png"));

        ProgramProperties.setProgramName(getVersion());
        ProgramProperties.setPropertiesFileName(propertiesFile);
        ProgramProperties.setProgramName(getVersion());

        // first set all necessary defaults:
        ProgramProperties.put(OPENFILE, System.getProperty("user.dir"));
        ProgramProperties.put(SAVEFILE, System.getProperty("user.dir"));
        ProgramProperties.put(SplitsTreeProperties.EXPORTFILE, System.getProperty("user.dir"));
        ProgramProperties.put(SplitsTreeProperties.RECENTFILES, "");
        ProgramProperties.put(SplitsTreeProperties.MAXRECENTFILES, 20);
        ProgramProperties.put(SplitsTreeProperties.RECENTMETHODS, "");
        ProgramProperties.put(SplitsTreeProperties.MAXRECENTMETHODS, 15);
        ProgramProperties.put(ExportImageDialog.GRAPHICSFORMAT, ".pdf");
        ProgramProperties.put(ExportImageDialog.GRAPHICSDIR, System.getProperty("user.dir"));
        ProgramProperties.put(SplitsTreeProperties.AUTOSCALE, true);
        ProgramProperties.put(SplitsTreeProperties.TOOLBARITEMS, "Open...;Clone...;Save;Print...;Export Image...;" +
                "Find/Replace...;Reset;Zoom In;Zoom Out;Rotate Left;Rotate Right;Preferences...;" +
                "Message Window...;EqualAngle;RootedEqualAngle;Phylogram;Previous Tree;Next Tree;");
        ProgramProperties.put(SplitsTreeProperties.SHOWTOOLBAR, true);

        // PluginData stuff
        if (ProgramProperties.isMacOS())
            ProgramProperties.put("PluginFolder", System.getProperty("user.home") + "/Library/Preferences/.SplitsTree/plugins");
        else
            ProgramProperties.put("PluginFolder", System.getProperty("user.home") + File.separator + ".SplitsTree/plugins");

        // then read in file to override defaults:
        ProgramProperties.load(propertiesFile);


        if (!ProgramProperties.getProgramVersion().equals(SplitsTreeProperties.getVersion())) {
            // put stuff here that should  be changed on update:
            ProgramProperties.put(SplitsTreeProperties.TOOLBARITEMS, "Open...;Duplicate;Save;Print...;Export Image...;" +
                    "Find/Replace...;Reset;Zoom In;Zoom Out;Rotate Left;Rotate Right;Preferences...;" +
                    "Message Window...;EqualAngle;RootedEqualAngle;Phylogram;Previous Tree;Next Tree;");
            ProgramProperties.put("label-layout-iterations", 10);
        }

        // always reset
        ProgramProperties.put("magnify", false);
        ProgramProperties.put("magnifyall", false);

        // then set latest version:
        ProgramProperties.put("Version", ProgramProperties.getProgramVersion());
    }

    /**
     * setup the initial menu
     */
    private static void initializeMenu() {
        // TODO: finish textual description of menus and then use them.
        // Problem at present is that calls to different algorithms not available as individual actions
        ProgramProperties.put("MenuBar.main", "File;Edit;View;Data;Distances;Trees;Networks;Analysis;Draw;Window;");

        ProgramProperties.put("Menu.File", "File;Open...;@OpenRecent;|;@Replace...;Clone...;|;Close;Save;Save As...;|;"
                + "Export...;Export Image...;|;@Tools;|;Print...;|;Close;Quit;");
        ProgramProperties.put("Menu.OpenRecent", "Recent Files;");
        ProgramProperties.put("Menu.Tools", "Tools;Load Multiple Trees...;Load Multi-Labeled Tree...;Concatenate Sequences...;Group Identical Haplotypes;");

        ProgramProperties.put("Menu.Edit", "Edit;Undo;Redo;|;Cut;Copy;Paste;|;Select All;Invert Selection;|;Find/Replace...;Go To Line...;|;Preferences...;");

        ProgramProperties.put("Menu.View", "View;@ViewData;|;Reset;|;Zoom in;Zoom Out;|;Rotate Left;Rotate Right;Flip;|;Nodes and Edges...;Highlight Confidence...;|;Auto Layout Node Labels;");
        ProgramProperties.put("Menu.ViewData", "Data;|;@ViewDataCharacters;@ViewDataDistances;@ViewDataSplits;");
        ProgramProperties.put("Menu.ViewDataCharacters", "Characters;Interleave;Transpose;Char Labels;");
        ProgramProperties.put("Menu.ViewDataDistances", "Distances;Triangle=both;Triangle=upper;Triangle=lower;Diagonal;Dist Labels;");
        ProgramProperties.put("Menu.ViewDataSplits", "Splits;Weights;Confidences;Split Labels;");

        ProgramProperties.put("Menu.Data", "Data;@DataSource;|;Keep Only Selected Taxa;Exclude Selected Taxa;Restore All Taxa;Filter Taxa...;" +
                "Define Taxa Sets...;|;Exclude Gap Sites;Exclude Parsimony-Uninformative Sites;Exclude Constant Sites;Restore All Sites;Filter Characters...;|;"
                + "Greedily Make Compatible;Greedily Make Weakly Compatible;Exclude Selected Splits;Restore All Splits;Fliter Splits...;|;"
                + "Filter Trees...;Edit Tree Names...;");
        ProgramProperties.put("Menu.DataSource", "Source;Reset Data;Execute;Convert to Nexus;");
    }


    /**
     * apply the properties to a viewer
     *
	 */
    public static void applyProperties(MainViewer viewer) {

        Font nFont = ProgramProperties.get("nFont", Font.decode("Dialog-PLAIN-10"));
        int nSize = ProgramProperties.get("nSize", 2);
        Color nColor = ProgramProperties.get("nColor", Color.BLACK);
        byte nShape = (byte) ProgramProperties.get("nShape", 0);

        viewer.setDefaultNodeFont(nFont);
        //phyloGraphView.setDefaultNodeLabel(nodeLabels.getSelectedItem().toString());
        viewer.setDefaultNodeHeight(nSize);
        viewer.setDefaultNodeWidth(nSize);
        viewer.setDefaultNodeShape(nShape);
        viewer.setDefaultNodeColor(nColor);
        viewer.setDefaultNodeLabelColor(nColor);

        Font eFont = ProgramProperties.get("eFont", Font.decode("Dialog-PLAIN-10"));
        int eWidth = ProgramProperties.get("eWidth", 1);
        Color eColor = ProgramProperties.get("eColor", Color.BLACK);
        viewer.setDefaultEdgeFont(eFont);
        viewer.setDefaultEdgeLineWidth(eWidth);
        viewer.setDefaultEdgeColor(eColor);
        viewer.setDefaultEdgeLabelColor(eColor);

        boolean scaleBar = ProgramProperties.get("scaleBar", true);
        viewer.setDrawScaleBar(scaleBar);
        boolean allowEdit = ProgramProperties.get("allowEdit", false);
        viewer.setAllowEdit(allowEdit);
        boolean maintainEdgeLengths = ProgramProperties.get("mainEdgeLengths", true);
        viewer.setMaintainEdgeLengths(maintainEdgeLengths);
        boolean useSplitSelectionMode = ProgramProperties.get("useSplitSelectionMode", true);
        viewer.setUseSplitSelectionModel(useSplitSelectionMode);

        String status = ProgramProperties.get("statusBar", "FTCA");
        StatusBar sbar = viewer.getStatusBar();
        sbar.setFit(status.indexOf('F') != -1);
        sbar.setLsFit(status.indexOf('L') != -1);
        sbar.setTaxa(status.indexOf('T') != -1);
        sbar.setChars(status.indexOf('C') != -1);
        sbar.setTrees(status.indexOf('t') != -1);
        sbar.setSplits(status.indexOf('S') != -1);
        sbar.setAssumptions(status.indexOf('A') != -1);
        sbar.setVertices(status.indexOf('V') != -1);
        sbar.setEdges(status.indexOf('E') != -1);
        sbar.setStatusLine(viewer.getDir().getDocument());

        // set the system proxy
        if (ProgramProperties.get("useProxy", false)) {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", ProgramProperties.get("proxyAdress", ""));
            System.setProperty("proxyPort", ProgramProperties.get("proxyPort", ""));
            if (ProgramProperties.get("useProxyUser", false)) {
                System.setProperty("http.proxyUser", ProgramProperties.get("proxyUserName", ""));
                // dont set password
                //System.setProperty("http.proxyPassword", ProgramProperties.get("proxyUserPassword", ""));
            }
        } else {
            System.setProperty("proxySet", "false");

        }
    }

    /**
     * add a file to the recent files list
     *
	 */
    public static void addRecentFile(File file) {
        int maxRecentFiles = ProgramProperties.get(SplitsTreeProperties.MAXRECENTFILES, 20);
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(SplitsTreeProperties.RECENTFILES, ""), ";");
        int count = 1;
        java.util.List<String> recentFiles = new LinkedList<>();
        String pathName = file.getAbsolutePath();
        recentFiles.add(pathName);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!pathName.equals(next)) {
                recentFiles.add(next);
                if (++count == maxRecentFiles)
                    break;
            }
        }
        StringBuilder buf = new StringBuilder();
        for (Object recentFile : recentFiles) buf.append(recentFile).append(";");
        ProgramProperties.put(SplitsTreeProperties.RECENTFILES, buf.toString());
        notifyListChange(SplitsTreeProperties.RECENTFILES);
    }

    /**
     * clears the list of recent files
     */
    public static void clearRecentFiles() {
        String str = ProgramProperties.get(SplitsTreeProperties.RECENTFILES, "");
        if (str.length() != 0) {
            ProgramProperties.put(SplitsTreeProperties.RECENTFILES, "");
            notifyListChange(SplitsTreeProperties.RECENTFILES);
        }
    }


    /**
     * add a method to the recent methods list
     *
     * @param blockName  type of block that method applies to
     * @param methodName name of method
     */
    public static void addRecentMethod(String blockName, String methodName) {
        int maxRecentMethods = ProgramProperties.get(SplitsTreeProperties.MAXRECENTMETHODS, 15);
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(SplitsTreeProperties.RECENTMETHODS, ""), ";");
        int count = 1;
        java.util.List<String> recentMethods = new LinkedList<>();
        String fullName = blockName + "+" + methodName;
        recentMethods.add(fullName);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!fullName.equals(next)) {
                recentMethods.add(next);
                if (++count == maxRecentMethods)
                    break;
            }
        }
        StringBuilder buf = new StringBuilder();
        for (String recentMethod : recentMethods) buf.append(recentMethod).append(";");
        ProgramProperties.put(SplitsTreeProperties.RECENTMETHODS, buf.toString());
        notifyListChange(SplitsTreeProperties.RECENTMETHODS);
    }

    /**
     * clears the list of recent files
     */
    public static void clearRecentMethods() {
        String str = ProgramProperties.get(SplitsTreeProperties.RECENTMETHODS, "");
        if (str.length() != 0) {
            ProgramProperties.put(SplitsTreeProperties.RECENTMETHODS, "");
            notifyListChange(SplitsTreeProperties.RECENTMETHODS);
        }
    }


    /**
     * add an action to the toolbar list
     *
	 */
    public static void setToolBar(java.util.List actions, boolean show) {

        StringBuilder buf = new StringBuilder();
        for (Object action1 : actions) {
            AbstractAction action = (AbstractAction) action1;
            if (action != null)
                buf.append((String) action.getValue(Action.NAME)).append(";");
        }

        ProgramProperties.put(SplitsTreeProperties.TOOLBARITEMS, buf.toString());
        notifyListChange(SplitsTreeProperties.TOOLBARITEMS);
        ProgramProperties.put(SplitsTreeProperties.SHOWTOOLBAR, show);
        notifyListChange(SplitsTreeProperties.SHOWTOOLBAR);

    }

	static final java.util.List<PropertiesListListener> propertieslistListeners = new LinkedList<>();

    /**
     * notify listeners that list of values for the given name has changed
     *
     * @param name such as RecentFiles
     */
    public static void notifyListChange(String name) {
        java.util.List<String> list = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(name, ""), ";");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        for (PropertiesListListener listener : propertieslistListeners) {
            if (listener.isInterested(name))
                listener.hasChanged(list);
        }
    }

    /**
     * add recent file listener
     *
	 */
    public static void addPropertiesListListener(PropertiesListListener listener) {
        if (!propertieslistListeners.contains(listener))
            propertieslistListeners.add(listener);
    }

    /**
     * remove recent file listener
     *
	 */
    public static void removePropertiesListListener(PropertiesListListener listener) {
        propertieslistListeners.remove(listener);
    }

    /**
     * gets the version of the program
     *
     * @return version
     */
    public static String getVersion() {
        return Version.SHORT_DESCRIPTION;
    }

    /**
     * this returns just the version number.
     */
    public static String getShortVersion() {
        String version = Version.SHORT_DESCRIPTION;
        version = version.substring(version.indexOf("version") + 8);
        version = version.trim().substring(0, version.indexOf(","));
        return version;
    }


    /**
     * gets the about of the program
     *
     * @return about screen
     */
    public static About getAbout() {
        About.setVersionStringOffset(250, 20);
        if (!About.isSet())
            About.setAbout("SplitsTree4-Splash.png", getVersion(), JDialog.DISPOSE_ON_CLOSE);
        return About.getAbout();
    }

    /**
     * any plugin that declares a variable EXPERT is ignored unless plugin manager is
     * running in expert mode
     *
     * @return true, if in expert mode
     */
    public static boolean getExpertMode() {
        return expertMode;
    }

    /**
     * any plugin that declares a variable EXPERT is ignored unless plugin manager is
     * running in expert mode
     *
	 */
    public static void setExpertMode(boolean exportMode) {
        SplitsTreeProperties.expertMode = exportMode;
    }
}
