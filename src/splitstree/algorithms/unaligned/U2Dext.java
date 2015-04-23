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

/*
* $Id: U2Dext.java,v 1.10 2007-09-11 12:31:03 kloepper Exp $
*/
package splitstree.algorithms.unaligned;

import jloda.util.parse.NexusStreamParser;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;
import splitstree.nexus.Unaligned;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @deprecated
 * Runs external program
 */
public class U2Dext /* implements Unaligned2Distances */ {
    private char special = '_';
    private String cmd = "";
    private String informat = "nexus";
    private String outformat = "nexus";
    public final static String DESCRIPTION = "Runs external program";

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Sets the external shell command.
     * The special variables %i and %e are replaced by the name of a
     * temporary input and output file, the former containing character data
     * for the extern program and the latter used to obtain the computed
     * distance matrix.
     *
     * @param cmd the command string
     */
    public void setOptioncmd(String cmd) {
        this.cmd = cmd;
    }

    /**
     * Gets the external shell command.
     *
     * @return the command
     */
    public String getOptioncmd() {
        return cmd;
    }

    /**
     * Sets the input file format, default is "nexus", other format is "fasta".
     *
     * @param fformat the specified format
     */
    public void setOptioninformat(String fformat) {
        this.informat = fformat;
    }

    /**
     * Gets the input file format
     *
     * @return the specified format
     */
    public String getOptioninformat() {
        return this.informat;
    }

    /**
     * Sets the output file format, default is "nexus", other format is
     * "simple".
     *
     * @param fformat the specified format
     */
    public void setOptionoutformat(String fformat) {
        this.outformat = fformat;
    }

    /**
     * Gets the output file format
     *
     * @return the specified format
     */
    public String getOptionoutformat() {
        return this.outformat;
    }

    /**
     * Sets the special character that is replaced by blanks before
     * execution of the command string, default is underscore '_'
     *
     * @param ch the new special character
     */
    public void setOptionspecial(char ch) {
        special = ch;
    }

    /**
     * Gets the special character that is replaced by blanks
     *
     * @return the special character
     */
    public char getOptionspecial() {
        return special;
    }

    /**
     * Determine whether the external command can be applied to the given
     * data.
     *
     * @param taxa    the taxa
     * @param unalign the unalign matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Unaligned unalign) {
        return getOptioncmd() != null && unalign != null;
    }

    /**
     * Attempts to run the given external command on the given data.
     *
     * @param taxa    the taxa
     * @param unalign the input unalign
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Unaligned unalign) throws Exception {
        {
            String shellCmd = getOptioncmd();
            if (shellCmd == null)
                return null;

            shellCmd = shellCmd.replaceAll("" + getOptionspecial(), " ");

            File infile = File.createTempFile("u2d", ".in");
            infile.deleteOnExit();
            File outfile = File.createTempFile("u2d", ".out");
            outfile.deleteOnExit();

            shellCmd = shellCmd.replaceAll("%i", infile.getCanonicalPath());
            shellCmd = shellCmd.replaceAll("%o", outfile.getCanonicalPath());

            if (informat.equalsIgnoreCase("nexus")) {
                FileWriter w = new FileWriter(infile);
                w.write("#nexus\n");
                taxa.write(w);
                unalign.write(w, taxa);
                w.close();
            }
            /*
            else if(informat.equalsIgnoreCase("fasta"))
            {
            }
            */
            else
                throw new SplitsException("Unknown informat: " + getOptioninformat());

            Process p = Runtime.getRuntime().exec(shellCmd);
            System.out.println("Command line: " + shellCmd);
            p.waitFor();
            /*
{
FileWriter w=new FileWriter(outfile);
w.write("#nexus\n");
taxa.write(w);
(new Distances(taxa.getNtax())).write(w,taxa);
w.close();
}
            */

            Distances dist = new Distances(taxa.getNtax());

            if (getOptionoutformat().equalsIgnoreCase("nexus")) {
                FileReader r = new FileReader(outfile);
                NexusStreamParser np = new NexusStreamParser(r);
                np.matchIgnoreCase("#nexus");
                Taxa tmp = new Taxa();
                tmp.read(np);
                dist.read(np, taxa);
                r.close();
            }
            /*
            else if(getOptionoutformat().equalsIgnoreCase("simple"))
            {
            }
            */
            else
                throw new SplitsException("Unknown outformat: "
                        + getOptionoutformat());
            return dist;
        }
    }
}

// EOF
