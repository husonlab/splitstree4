/**
 * C2Dext.java 
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
/*
* $Id: C2Dext.java,v 1.14 2007-09-11 12:31:00 kloepper Exp $
*/
package splitstree.algorithms.characters;

import jloda.util.parse.NexusStreamParser;
import splitstree.core.Document;
import splitstree.core.SplitsException;
import splitstree.nexus.Characters;
import splitstree.nexus.Distances;
import splitstree.nexus.Taxa;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @deprecated
 * Runs external program
 */
public class C2Dext /* implements Characters2Distances */ {
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
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters chars) {
        return getOptioncmd() != null && chars != null;
    }

    /**
     * Attempts to run the given external command on the given data.
     *
     * @param taxa  the taxa
     * @param chars the input characters
     * @return the computed distances Object
     */
    public Distances apply(Document doc, Taxa taxa, Characters chars) throws Exception {
        {
            String shellCmd = getOptioncmd();
            if (shellCmd == null)
                return null;

            shellCmd = shellCmd.replaceAll("" + getOptionspecial(), " ");

            File infile = File.createTempFile("c2d", ".in");
            infile.deleteOnExit();
            File outfile = File.createTempFile("c2d", ".out");
            outfile.deleteOnExit();

            shellCmd = shellCmd.replaceAll("%i", infile.getCanonicalPath());
            shellCmd = shellCmd.replaceAll("%o", outfile.getCanonicalPath());

            if (informat.equalsIgnoreCase("nexus")) {
                FileWriter w = new FileWriter(infile);
                w.write("#nexus\n");
                taxa.write(w);
                chars.write(w, taxa);
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
            InputStream ins=p.getInputStream();
            BufferedReader bf=new BufferedReader(new Reader(ins));
            String str=bf.readLine();
            while(str!=null)
            {
                System.err.println(str);
                str=bf.readLine();
            }
            bf.close();
            ins.close();
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
