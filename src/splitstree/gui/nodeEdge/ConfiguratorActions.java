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

package splitstree.gui.nodeEdge;

import jloda.util.Alert;
import jloda.util.ResourceManager;
import splitstree.gui.Director;
import splitstree.gui.DirectorActions;
import splitstree.gui.main.MainViewerActions;
import splitstree.gui.undo.*;
import splitstree.nexus.Network;
import splitstree.nexus.Splits;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * actions associated with a node-edge-configurator window
 */
public class ConfiguratorActions {
    private Configurator conf;
    private Director dir;
    private List all = new LinkedList();

    private boolean ignore = false; // ignore firing when in update only of controls

    public ConfiguratorActions(Configurator conf, Director dir) {
        this.conf = conf;
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
    }

    /**
     * returns all actions
     *
     * @return actions
     */
    public List getAll() {
        return all;
    }

    // here we define the configurator window specific actions:

    private AbstractAction close;

    /**
     * close this viewer
     *
     * @return close action
     */
    public AbstractAction getClose() {
        AbstractAction action = close;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                dir.removeViewer(conf);
                conf.getFrame().setVisible(false);
                conf.getFrame().dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('C'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this window");
        action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("Close16.gif"));
        // close is critical because we can't easily kill the worker thread

        all.add(action);
        return close = action;
    }

    private AbstractAction edgeIDs;

    public AbstractAction getEdgeIDs() {
        AbstractAction action = edgeIDs;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    //todo: checkSelectALL
                    boolean noneSelected = conf.getViewer().getSelectedEdges().isEmpty();

                    final ICommand cmd = new EdgeLabelsCommand(conf.getViewer(),
                            conf.getEdgeWeights().isSelected(),
                            conf.getEdgeIDs().isSelected(), conf.getEdgeConfidence().isSelected(),
                            conf.getEdgeIntervalBox().isSelected(), !noneSelected);
                    new Edit(cmd, "edge labels").execute(conf.getViewer().getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('I'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show IDs");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeIDs = action;
    }

    private AbstractAction edgeWeights;

    public AbstractAction getEdgeWeights() {
        AbstractAction action = edgeWeights;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    //todo: checkSelectALL
                    boolean noneSelected = conf.getViewer().getSelectedEdges().isEmpty();
                    final ICommand cmd = new EdgeLabelsCommand(conf.getViewer(),
                            conf.getEdgeWeights().isSelected(),
                            conf.getEdgeIDs().isSelected(), conf.getEdgeConfidence().isSelected(),
                            conf.getEdgeIntervalBox().isSelected(), !noneSelected);
                    new Edit(cmd, "edge labels").execute(conf.getViewer().getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Weights");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('W'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show weights");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return edgeWeights = action;
    }

    private AbstractAction edgeConfidence;

    public AbstractAction getEdgeConfidence() {
        AbstractAction action = edgeConfidence;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    //todo: checkSelectALL
                    boolean noneSelected = conf.getViewer().getSelectedEdges().isEmpty();
                    final ICommand cmd = new EdgeLabelsCommand(conf.getViewer(),
                            conf.getEdgeWeights().isSelected(),
                            conf.getEdgeIDs().isSelected(), conf.getEdgeConfidence().isSelected(),
                            conf.getEdgeIntervalBox().isSelected(), !noneSelected);
                    new Edit(cmd, "edge labels").execute(conf.getViewer().getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Confidence Values");
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('V'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show confidence");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeConfidence = action;
    }

    private AbstractAction edgeInterval;

    public AbstractAction getEdgeInterval() {
        AbstractAction action = edgeInterval;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    //todo: checkSelectALL
                    boolean noneSelected = conf.getViewer().getSelectedEdges().isEmpty();
                    final ICommand cmd = new EdgeLabelsCommand(conf.getViewer(),
                            conf.getEdgeWeights().isSelected(),
                            conf.getEdgeIDs().isSelected(), conf.getEdgeConfidence().isSelected(),
                            conf.getEdgeIntervalBox().isSelected(), !noneSelected);
                    new Edit(cmd, "edge labels").execute(conf.getViewer().getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Intervals");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show confidence interval for edge weight");
        action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);
        all.add(action);
        return edgeInterval = action;
    }


    private AbstractAction edgeFont;

    public AbstractAction getEdgeFont() {
        AbstractAction action = edgeFont;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    //ToDo: checkSelectALL
                    if (selectedValue != null && conf.getViewer().getSelectedEdges().size() >= 0) {
                        String family = selectedValue.toString();
                        ICommand cmd = new EdgeFontCommand(conf.getViewer(), family, -1, -1, -1);
                        new Edit(cmd, "edge font").execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);

        all.add(action);
        return edgeFont = action;
    }

    private AbstractAction edgeFontSize;

    public Action getEdgeFontSize() {
        AbstractAction action = edgeFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore && (event.getActionCommand() == null || event.getActionCommand().equals("comboBoxChanged"))) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    if (conf.getViewer().getSelectedEdges().size() >= 0) //ToDo: checkSelectALL
                        if (selectedValue != null) {
                            int size;
                            try {
                                size = Integer.parseInt((String) selectedValue);
                            } catch (NumberFormatException e) {
                                new Alert(conf.getFrame(), "Font Size must be an integer! Size set to 10.");
                                size = 10;
                                ((JComboBox) event.getSource()).setSelectedItem("10");
                            }

                            ICommand cmd = new EdgeFontCommand(conf.getViewer(), null, -1, -1, size);
                            new Edit(cmd, "font size").execute(conf.getViewer().getUndoSupportNetwork());
                        }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);

        all.add(action);
        return edgeFontSize = action;
    }


    private AbstractAction edgeBold;

    public Action getEdgeFontBold() {
        AbstractAction action = edgeBold;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    if (conf.getViewer().getSelectedEdges().size() >= 0) {  //ToDo: checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        ICommand cmd = new EdgeFontCommand(conf.getViewer(), null, state, -1, -1);
                        new Edit(cmd, "bold " + (state == 1 ? "on" : "off")).execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Bold");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font bold");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);

        all.add(action);
        return edgeBold = action;
    }

    private AbstractAction edgeItalic;

    public Action getEdgeFontItalic() {
        AbstractAction action = edgeItalic;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    if (conf.getViewer().getSelectedEdges().size() >= 0) { //ToDo: checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        ICommand cmd = new EdgeFontCommand(conf.getViewer(), null, -1, state, -1);
                        new Edit(cmd, "italic " + (state == 1 ? "on" : "off")).execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Italic");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font italic");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);

        all.add(action);
        return edgeItalic = action;
    }

    private AbstractAction edgeWidth;

    public AbstractAction getEdgeWidth() {
        AbstractAction action = edgeWidth;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    if (selectedValue != null) {
                        int size = Integer.parseInt((String) selectedValue);
                        ICommand cmd = new EdgeWidthCommand(conf.getViewer(), size);
                        new Edit(cmd, "width").execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Width...");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('S'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set edge width");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_EDGESELECTION, Boolean.TRUE);

        all.add(action);
        return edgeWidth = action;
    }

    private AbstractAction nodeLabels;

    public AbstractAction getNodeLabels() {
        AbstractAction action = nodeLabels;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    boolean names = false;
                    boolean ids = false;
                    switch (((JComboBox) event.getSource()).getSelectedIndex()) {
                        case 0:
                            names = true;
                            break;
                        case 1:
                            ids = true;
                            break;

                        case 2:
                            names = true;
                            ids = true;
                            break;
                        default:
                            break;
                    }
                    //todo: checkSelectALL
                    boolean noneSelected = conf.getViewer().getSelectedNodes().isEmpty();
                    //If no nodes are selected, we presume the command is applied to all nodes.
                    ICommand cmd = new NodeLabelsCommand(conf.getViewer(), names, ids, !noneSelected);
                    new Edit(cmd, "node labels").execute(conf.getViewer().getUndoSupportNetwork());
                }
            }
        };
        action.putValue(AbstractAction.NAME, "IDs");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('I'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Label with taxon ids");
        action.putValue(DirectorActions.DEPENDS_ON, Network.NAME);
        all.add(action);
        return nodeLabels = action;
    }

    private AbstractAction nodeFont;

    public AbstractAction getNodeFont() {
        AbstractAction action = nodeFont;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    //ToDo: CheckSelectALL
                    // if (selectedValue != null && conf.getViewer().getSelectedNodes().size() != 0) {
                    if (selectedValue != null) {


                        String family = selectedValue.toString();
                        ICommand cmd = new NodeFontCommand(conf.getViewer(), family, -1, -1, -1);
                        new Edit(cmd, "Node font").execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeFont = action;
    }

    private AbstractAction nodeFontSize;

    public Action getNodeFontSize() {
        AbstractAction action = nodeFontSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                // only use event comboBoxChanged to avoid multiple events!
                if (!ignore && (event.getActionCommand() == null || event.getActionCommand().equals("comboBoxChanged"))) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();
                    //ToDo: CheckSelectALL
                    if (conf.getViewer().getSelectedNodes().size() >= 0)
                        //      if (conf.getViewer().getSelectedNodes().size() != 0)
                        if (selectedValue != null) {
                            int size;
                            try {
                                size = Integer.parseInt((String) selectedValue);
                            } catch (NumberFormatException e) {
                                new Alert(conf.getFrame(), "Font Size must be an integer! Size set to 10.");
                                size = 10;
                                ((JComboBox) event.getSource()).setSelectedItem("10");
                            }

                            ICommand cmd = new NodeFontCommand(conf.getViewer(), null, -1, -1, size);
                            new Edit(cmd, "font size").execute(conf.getViewer().getUndoSupportNetwork());
                        }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Font...");
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeFontSize = action;
    }

    private AbstractAction nodeBold;

    public Action getNodeFontBold() {
        AbstractAction action = nodeBold;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    if (conf.getViewer().getSelectedNodes().size() >= 0) {  //ToDo: checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        ICommand cmd = new NodeFontCommand(conf.getViewer(), null, state, -1, -1);
                        new Edit(cmd, "bold " + (state == 1 ? "on" : "off")).execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Bold");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font bold");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeBold = action;
    }

    private AbstractAction nodeItalic;

    public Action getNodeFontItalic() {
        AbstractAction action = nodeItalic;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    if (conf.getViewer().getSelectedNodes().size() >= 0) { //checkSelectALL
                        int state = ((JCheckBox) event.getSource()).isSelected() ? 1 : 0;
                        ICommand cmd = new NodeFontCommand(conf.getViewer(), null, -1, state, -1);
                        new Edit(cmd, "italic " + (state == 1 ? "on" : "off")).execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };

        action.putValue(AbstractAction.NAME, "Italic");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('F'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set label font italic");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeItalic = action;
    }

    private AbstractAction nodeSize;

    public AbstractAction getNodeSize() {
        AbstractAction action = nodeSize;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    if (selectedValue != null) {
                        int size = Integer.parseInt((String) selectedValue);
                        ICommand cmd = new NodeSizeCommand(conf.getViewer(), size, size);
                        new Edit(cmd, "size").execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Size...");
        action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('S'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node size");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeSize = action;
    }

    private AbstractAction nodeShape;

    public Action getNodeShape() {

        AbstractAction action = nodeShape;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (!ignore) {
                    Object selectedValue = ((JComboBox) event.getSource()).getSelectedItem();

                    if (selectedValue != null) {
                        int shape = -1;
                        if (selectedValue == "square") shape = 1;
                        if (selectedValue == "none") shape = 0;
                        if (selectedValue == "circle") shape = 2;
                        ICommand cmd = new NodeShapeCommand(conf.getViewer(), shape);
                        new Edit(cmd, "shape").execute(conf.getViewer().getUndoSupportNetwork());
                    }
                }
            }
        };
        action.putValue(AbstractAction.NAME, "Shape...");
        //action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('S'));
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Set node shape");
        //action.putValue(DirectorActions.DEPENDS_ON,Network.NAME);
        action.putValue(MainViewerActions.DEPENDS_ON_NODESELECTION, Boolean.TRUE);

        all.add(action);
        return nodeShape = action;
    }

    //  private AbstractAction confidenceEdgeWidth;

    /**
     * how to display confidence values in graph
     * @return action for displaying confidence values
     */
    /*  public AbstractAction getConfidenceEdgeWidth() {
          AbstractAction action = confidenceEdgeWidth;
          if (action != null) return action;

          action = new AbstractAction() {
              public void actionPerformed(ActionEvent event) {
                  Network network = dir.getDocument().getNetwork();
                  Splits splits=dir.getDocument().getSplits();
                  network.modifyEdgeConfidenceRendering(true,false,splits,conf.getViewer(),false);
                  conf.getViewer().repaint();
              }
          };
          action.putValue(AbstractAction.NAME, "Edge Width...");
          action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('W'));
          action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
          action.putValue(AbstractAction.SHORT_DESCRIPTION, "Display confidence by edge width");
          action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);

          all.add(action);
          return confidenceEdgeWidth = action;
      }*/

    // private AbstractAction confidenceEdgeShade;

    /**
     * how to display confidence values in graph
     * @return action for displaying confidence values
     */
    /*   public AbstractAction getConfidenceEdgeShading() {
           AbstractAction action = confidenceEdgeShade;
           if (action != null) return action;

           action = new AbstractAction() {
               public void actionPerformed(ActionEvent event) {
                   Network network = dir.getDocument().getNetwork();
                   Splits splits=dir.getDocument().getSplits();
                   network.modifyEdgeConfidenceRendering(false,true,splits,conf.getViewer(),false);
                   conf.getViewer().repaint();
               }
           };
           action.putValue(AbstractAction.NAME, "Edge Shade...");
           action.putValue(AbstractAction.MNEMONIC_KEY, new Integer('S'));
           action.putValue(DirectorActions.CRITICAL, Boolean.TRUE);
           action.putValue(AbstractAction.SHORT_DESCRIPTION, "Display confidence by edge shading");
           action.putValue(DirectorActions.DEPENDS_ON, Splits.NAME);

           all.add(action);
           return confidenceEdgeShade = action;
       }*/

    /**
     * get ignore firing of events
     *
     * @return true, if we are currently ignoring firing of events
     */
    public boolean getIgnore() {
        return ignore;
    }

    /**
     * set ignore firing of events
     *
     * @param ignore
     */
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

}
