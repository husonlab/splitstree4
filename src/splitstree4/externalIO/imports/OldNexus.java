/**
 * OldNexus.java
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
package splitstree4.externalIO.imports;


import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kloepper
 * Date: Oct 24, 2003
 * Time: 10:25:16 AM
 * To change this template use Options | File Templates.
 */
public class OldNexus extends FileFilter implements Importer {
    String datatype = null;


    String Description = "Nexus Files (*.nxs,*.nex,*.nexus)";

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    public boolean isApplicableToBlock(String blockName) {
        return true;
    }


    /**
     * can we import this data?
     *
     * @param input0
     * @return true, if can handle this import
     */
    public boolean isApplicable(Reader input0) throws IOException {
        BufferedReader input = new BufferedReader(input0);
        String aline = input.readLine();
        return aline != null && aline.trim().compareToIgnoreCase("#NEXUS") == 0;
    }


    /**
     * convert a input string into nexus format
     *
     * @param input0
     * @return String nexus format
     */
    public String apply(Reader input0) throws IOException {
        StringBuilder topComments = new StringBuilder();

        // first erase all comments and clean the blocks from old stuff
        // we will keep all comments up-to first occurrence of the begin-key word.

        //Strip the comments.  Basically remove anything between [ and ]
        //unless the first bracket is followed immediately by &
        StringBuilder inputNoComments = new StringBuilder();
        BufferedReader inputReader = new BufferedReader(input0);
        int c;
        int inTopComments = 0; //0: before, 1: in, 2: after

        while (true) {
            c = inputReader.read();
            if (c == -1)
                break;

            if ((char) c == '[') {
                if (inTopComments == 0)
                    inTopComments = 1;
                else
                    topComments.append("\n");
                c = inputReader.read();
                if (c == -1)
                    break;
                if ((char) c == '&') {
                    inputNoComments.append("[&");
                } else {
                    while (c >= 0 && (char) c != ']') {
                        if (inTopComments == 1)
                            topComments.append((char) c);
                        c = inputReader.read();
                    }
                    if (c == -1)
                        break;
                }
            } else {
                if (inTopComments == 1 && Character.isLetterOrDigit((char) c) && inputNoComments.length() > "#nexus".length()) {
                    inTopComments = 2;
                }
                inputNoComments.append((char) c);
            }
        }


        int ntax = -1;
        String taxalabels = "_detect_";
        StringBuilder head = new StringBuilder();
        StringBuilder body = new StringBuilder();
        boolean hasTranslateBlock = false; //True if file has a translate block
        StreamTokenizer sk = new StreamTokenizer(new BufferedReader(new StringReader(inputNoComments.toString())));

        sk.resetSyntax();
        sk.eolIsSignificant(false);
        sk.whitespaceChars(0, 32);
        sk.wordChars(33, 126);

        boolean hasCharactersOrDistances = false;

        while (sk.nextToken() != -1) {
            //System.out.println(sk.sval);
            // ignore nexus at start
            if (sk.sval.equalsIgnoreCase("#nexus")) {
                head.append("#nexus\n");
                if (topComments.length() > 0)
                    head.append("[!").append(topComments.toString()).append("]");
            }
            // clean blocks
            else if (sk.sval.equalsIgnoreCase("begin")) {
                sk.nextToken();

                if (sk.sval.equalsIgnoreCase("data;")) {
                    hasCharactersOrDistances = true;
                    body.append("begin characters;\n");
                } else if (sk.sval.equalsIgnoreCase("distances;")) {
                    hasCharactersOrDistances = true;
                    body.append("begin distances;\n");
                } else if (sk.sval.equalsIgnoreCase("trees;")) {
                    body.append("begin trees;\n");
                } else {
                    System.err.println("skipping: " + sk.sval);
                    while (!sk.sval.equalsIgnoreCase("end;")) sk.nextToken();
                }
            } else if (sk.sval.toLowerCase().lastIndexOf("ntax=") != -1) { // set the ntax
                ntax = Integer.parseInt(sk.sval.substring(5));
                body.append(sk.sval).append("\n");
            }

            // if we have a matrix we have to be more careful
            else if (sk.sval.equalsIgnoreCase("MATRIX")) {
                hasCharactersOrDistances = true;
                sk.eolIsSignificant(true);
                while (!sk.sval.equalsIgnoreCase("end;")) {
                    body.append(sk.sval);
                    while (sk.nextToken() != StreamTokenizer.TT_EOL) body.append("\t").append(sk.sval);
                    body.append("\n");
                    sk.nextToken();
                    while (sk.sval == null) {
                        sk.nextToken();
                    }
                }
                body.append("END;\n");
                //cleanup
                sk.eolIsSignificant(false);
            } else if (sk.sval.equalsIgnoreCase("TRANSLATE")) {
                //We have encountered a TRANSLATE block... this is where we should get the taxa,
                // unless we have found characters or distances, in which case we don't keep the labels here
                if (!hasCharactersOrDistances)
                    taxalabels = "";
                hasTranslateBlock = true;
                boolean foundSemicolon = false;
                body.append(sk.sval).append("\n");
                while (!foundSemicolon) {
                    sk.nextToken();        //Should be a number
                    if (sk.sval.equals(";")) {
                        body.append(";");
                        break;
                    }
                    body.append(sk.sval).append("\t");
                    Integer.parseInt(sk.sval);

                    sk.nextToken();    //Should be a name, possibly with a comma or semicolon
                    body.append(sk.sval);
                    String name = sk.sval;
                    //Check if name ends in comma or semicolon.
                    if (name.endsWith(",")) {
                        name = name.substring(0, name.length() - 1);
                    } else if (name.endsWith(";")) {
                        name = name.substring(0, name.length() - 1);
                        foundSemicolon = true;
                    } else {    //Now read in comma or semicolon.
                        sk.nextToken();
                        body.append(sk.sval);
                        if (sk.sval.equals(";"))
                            foundSemicolon = true;
                        else if (!sk.sval.equals(","))
                            throw new IOException("Expecting ',' or ';'");
                    }
                    body.append("\n");
                    if (!hasCharactersOrDistances)
                        taxalabels += name + " ";
                }
            }
            // import a tree
            else if (sk.sval.equalsIgnoreCase("uTree") || sk.sval.equalsIgnoreCase("Tree")) {
                body.append("TREE \t");

                // if the next token is a "*", swallow it.
                sk.nextToken();
                if (!sk.sval.equals("*"))
                    sk.pushBack();

                if (ntax == -1) {
                    sk.nextToken();
                    // we must detect the number of taxa
                    while (!sk.sval.contains(";")) {
                        body.append(sk.sval).append("\t");
                        sk.nextToken();
                    }
                    body.append(sk.sval).append("\n");
                    PhyloTree pt = new PhyloTree();
                    try {
                        pt.parseBracketNotation(sk.sval.substring(0, sk.sval.length() - 1));
                        ntax = pt.computeNumLabeledNodes();
                        //If we haven't encountered a translate block we guess taxa names from
                        //the trees.
                        if (!hasTranslateBlock) {
                            taxalabels = "";
                            for (Node v = pt.getFirstNode(); v != null; v = pt.getNextNode(v))
                                if (pt.getLabel(v) != null)
                                    taxalabels += pt.getLabel(v) + "\n";
                        }
                    } catch (Exception e) {
                        Basic.caught(e);
                    }
                }
            } else if (sk.sval.equalsIgnoreCase("OPTIONS")) {
                while (sk.sval.lastIndexOf(";") == -1) sk.nextToken();
            } else
                body.append(sk.sval).append("\n");
        }
        //System.out.println(reduced);
        // generate a taxa detect labels block
        String taxa = "";
        if (ntax != -1)
            taxa += "begin taxa;\nDIMENSIONS ntax=" + ntax + ";\n taxlabels " + taxalabels + ";\nend;\n";
        else
            throw new IOException("Unable to parse, could not determine the number of taxa");
        //System.out.println(taxa+"\n"+taxa+result.toString());
        return head.toString() + taxa + body.toString();
    }

    /**
     * gets the list of file extensions
     *
     * @return file extensions
     */
    public List getFileExtensions() {
        List extensions = new LinkedList();
        extensions.add("nxs");
        extensions.add("nex");
        extensions.add("nexus");
        return extensions;

    }

    /**
     * @return should File be shown in dialog
     */

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) return true;
            try {// Get the file extension
                String extension = getExtension(f);
                if (extension != null)
                    if (extension.equalsIgnoreCase("nxs")
                            || extension.equalsIgnoreCase("nex")
                            || extension.equalsIgnoreCase("nexus"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * @return description of file matching the filter
     */
    public String getDescription() {
        return Description;
    }


    /**
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     *         last ".")
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
