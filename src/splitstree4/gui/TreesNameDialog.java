/*
 * TreesNameDialog.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.gui;

import splitstree4.core.Document;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.Trees;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * provide a dialog for naming imported trees, if necessary
 * Daniel Huson and David Bryant , 2.2006
 * TODO: at the moment this only edits the labels, but some of threshold editing is also implemented
 */
public class TreesNameDialog extends JDialog {
    final Document doc;
    final DefaultListModel listModel = new DefaultListModel();
    final JList list = new JList(listModel);
    final TreesNameDialog me = this;

    /**
     * construct and open dialog
     *
     * @param owner
     * @param doc
     */
    public TreesNameDialog(JFrame owner, Document doc) {
        super(owner, "Tree names - " + SplitsTreeProperties.getVersion());
        this.doc = doc;
        if (!isApplicable(doc))
            return;
        setupDialog(owner);
        loadList();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                java.util.List selected = list.getSelectedValuesList();
                if (selected.size() != 1) return;
                if (event.getClickCount() == 2) {
                    Node node = (Node) selected.get(0);
                    System.err.println("clicked on: " + node);
                    node.edit(me);
                    event.consume();
                }
            }
        });
        setVisible(true);
    }

    /**
     * set up the dialog
     *
     * @param owner
     */
    private void setupDialog(JFrame owner) {
        setLocation(new Point(owner.getLocation().x + 20, owner.getLocation().y + 20));
        setSize(200, 300);
        setModal(true);
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(new JLabel("Double click names to edit:"), BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        bottom.add(new JButton(getCancel()));
        bottom.add(new JButton(getApply()));
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    /**
     * load the list
     */
    void loadList() {
        Trees trees = doc.getTrees();
        for (int t = 1; t <= trees.getNtrees(); t++) {
            listModel.addElement(new Node(trees.getName(t), 0));
        }
    }

    /**
     * save the modified list
     */
    void saveList() {
        Trees trees = doc.getTrees();
        for (int t = 1; t <= trees.getNtrees(); t++) {
            Node node = (Node) listModel.getElementAt(t - 1);
            trees.setName(t, node.name);
        }
    }

    /**
     * is this applicable to the given document
     *
     * @param doc
     * @return true, if this dialog makes sense for the current document
     */
    public static boolean isApplicable(Document doc) {
        return doc.isValidByName(Trees.NAME) && doc.getTrees().getNtrees() > 0;
    }

    AbstractAction apply;

    /**
     * apply action
     *
     * @return apply
     */
    AbstractAction getApply() {
        AbstractAction action = apply;
        if (action != null)
            return action;
        action = new AbstractAction() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                saveList();
                me.setVisible(false);
                me.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply all changes");
        return apply = action;
    }

    AbstractAction cancel;

    /**
     * cancel action
     *
     * @return cancel
     */
    AbstractAction getCancel() {
        AbstractAction action = cancel;
        if (action != null)
            return action;
        action = new AbstractAction() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                me.setVisible(false);
                me.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Discard all changes");
        return cancel = action;
    }

    /**
     * a node to be displayed in the list
     */
    class Node extends Component {
        String name;
        float threshold;

        Node(String label, float threshold) {
            this.name = label;
            this.threshold = threshold;
        }

        /**
         * get the string representation
         *
         * @return
         */
        public String toString() {
            //return "'"+name+"'\t "+threshold;
            return name;
        }

        /**
         * edit the node
         *
         * @param owner
         */
        public void edit(Component owner) {
            String str = JOptionPane.showInputDialog(owner, "Edit tree name:", name);
            if (str != null && str.length() > 0)
                name = str.trim();
            /*
             str=Basic.trim(JOptionPane.showInputDialog(owner,"Edit threshold:",""+threshold));
            if(str.length()>0)
            {
                try {
                    float value = Float.parseFloat(str);
                    threshold=value;
                }
                catch(NumberFormatException ex)
                {
                    Basic.caught(ex);
                }
            }
            */
        }
    }
}
