/*
 * NexusTreeMerger.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.progs;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dezulian
 * Date: Mar 23, 2004
 * Time: 11:55:53 AM
 * To change this template use Options | File Templates.
 * <p/>
 * <p/>
 * Takes as input a bunch of nexus files,
 * each containing taxa and one tree.
 * Outputs one nexus file containing a taxon block that is
 * the union of all input taxa blocks and a trees block
 * containing all input trees.
 */
public class NexusTreeMerger {

    static class Taxon {
		private static int nextId = 1;

		public String name = "<default>";
		public final int id;
		public final Vector occurrences = new Vector();

		public Taxon(String name) {
			this.name = name;
			this.id = nextId++;
		}
	}

    public static void main(String[] argv) throws Exception {
        if (argv.length != 1) {
            System.out.println("NexusTreeMerger <inputdir> <outputdir>");
            System.exit(-1);
        }

        new NexusTreeMerger().run(argv);
    }

    public void run(String[] argv) throws Exception {
        String DATESTRING = generateDateString();
        System.out.println("hello world   - " + DATESTRING);

        StringBuilder outContent = new StringBuilder();
        File rootDir = new File(argv[0]);
        File inputDir = new File(rootDir, "input");
        File outputDir = new File(rootDir, "output");
        File outputFile = new File(outputDir, "merge_" + DATESTRING + ".nex");

        System.out.println("input dir:   " + inputDir.getAbsolutePath());
        System.out.println("output file: " + outputFile.getAbsolutePath());

		FilenameFilter fnf = (dir, name) -> name.endsWith(".nex") || name.endsWith(".tre");
		File[] children = inputDir.listFiles(fnf);

        HashMap name2taxon = new HashMap();
        ArrayList trees = new ArrayList();
        ArrayList treenames = new ArrayList();
        ArrayList numTaxaPerFile = new ArrayList();
        HashMap tree2taxaSet = new HashMap();

        for (File aChildren : children) {
            HashMap localId2name = new HashMap();

            String treename = aChildren.getName().substring(0, aChildren.getName().lastIndexOf("."));
            tree2taxaSet.put(treename, new HashSet());
            BufferedReader reader = new BufferedReader(new FileReader(aChildren));

            String line;
            StringBuilder fileContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                fileContent.append(line);
            }
            String content = fileContent.toString();
            System.out.println(">>>");
            System.out.println(content);
            System.out.println("<<<");

            final String KEYWORD_BEGIN = "TRANSLATE";
            final String KEYWORD_END = ";";
            int left = content.indexOf(KEYWORD_BEGIN) + KEYWORD_BEGIN.length();
            int right = content.indexOf(KEYWORD_END, left);
            String nameTable = content.substring(left, right);
            StringTokenizer st = new StringTokenizer(nameTable, "\t\r\n, '", false);
            int taxaInThisFile = 0;
            while (st.hasMoreTokens()) {
                int localId = Integer.parseInt(st.nextToken());
                String name = st.nextToken();
                taxaInThisFile++;
                ((HashSet) tree2taxaSet.get(treename)).add(name);

                Object o;
                Taxon taxon;
                if ((o = name2taxon.get(name)) == null) {
                    taxon = new Taxon(name);
                    name2taxon.put(name, taxon);
                } else {
                    taxon = (Taxon) o;
                }
                taxon.occurrences.add(treename);
                localId2name.put(Integer.toString(localId), name);
            }

            left = content.indexOf("TREE", right) + 4;
            right = content.indexOf("=", left);
            //String treename = content.substring(left, right).replace('\'', ' ').trim();
            left = content.indexOf("(", right);
            right = content.indexOf(";", left);
            String tree = content.substring(left, right);

            //call without closing semicolon
            //splitsX.workbench.geneorder.Newick2TreeExperimentalRemap.remap(localId2name, tree);
            String newnewick = sandboxedCall(localId2name, tree);
            treenames.add(treename);
            trees.add(newnewick);
            numTaxaPerFile.add(taxaInThisFile);
        }
        outContent.append("#NEXUS" + "\n");
        outContent.append("\n");
        outContent.append("[!\n");
        outContent.append(DATESTRING).append("* merge of files:").append("\n");
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
			outContent.append(child.getAbsolutePath()).append("{").append(numTaxaPerFile.get(i)).append(" taxa}").append("\n");
		}
		outContent.append("]\n");
		outContent.append("\n");
		outContent.append("\n");

		int numTaxa = Taxon.nextId - 1;
		outContent.append("BEGIN Taxa;" + "\n");
		outContent.append("DIMENSIONS ntax=").append(numTaxa).append(";").append("\n");
		outContent.append("TAXLABELS" + "\n");
		Comparator comp = (o1, o2) -> {
			String one = (String) o1;
			String two = (String) o2;
			return one.compareTo(two);
		};
		TreeSet sortedKeys = new TreeSet(comp);
		{
			Set keys = name2taxon.keySet();
			sortedKeys.addAll(keys);
		}
		{
			int i = 1;
			for (Object sortedKey : sortedKeys) {
				String name = (String) sortedKey;
                //int id = ((Taxon)name2taxon.get(name)).id();
                outContent.append("[").append(i).append("] '").append(name).append("'").append("\n");
                i++;
            }
        }
        outContent.append(";" + "\n");
        outContent.append("END [Taxa];" + "\n");
        outContent.append("BEGIN Trees;" + "\n");
        outContent.append("PROPERTIES partialtrees;" + "\n");
        outContent.append("TRANSLATE" + "\n");
        for (Object sortedKey1 : sortedKeys) {
            String name = (String) sortedKey1;
            outContent.append("\t ").append(name).append("\t\t\t").append(name).append(",").append("\n");
        }
        outContent.append(";" + "\n");
        for (int i = 0; i < treenames.size(); i++) {
            outContent.append("tree ").append(treenames.get(i)).append(" = ").append(trees.get(i)).append("\n");
        }

        outContent.append("END [Trees];" + "\n");
        outContent.append("" + "\n");
        outContent.append("BEGIN st_Analysis;" + "\n");
        outContent.append("END; [st_Analysis];" + "\n");
        outContent.append("BEGIN st_Assumptions;" + "\n");
        outContent.append("	uptodate;" + "\n");
        outContent.append("	treestransform=SuperNetwork;" + "\n");
        outContent.append("	splitstransform=EqualAngle Weights = true;" + "\n");
        outContent.append("	SplitsPostProcess filter=none;" + "\n");
        outContent.append("	exclude;" + "\n");
        outContent.append("	layoutstrategy=snowball;" + "\n");
        outContent.append("	autolayoutnodelabels;" + "\n");
        outContent.append("END; [st_Assumptions]" + "\n");

        //do some statistics regarding how many taxa of the different trees overlapped
        outContent.append("\n");
        outContent.append("\n");
        outContent.append("[!\n");
        for (Object sortedKey : sortedKeys) {
            String name = (String) sortedKey;
            Vector occurrences = ((Taxon) name2taxon.get(name)).occurrences;
            outContent.append(padStringWithTrailingBlanks(name, 35)).append("    ");
            String stars = "";
            for (int i = 0; i < occurrences.size(); i++) {
                stars = stars + "*";
            }
            outContent.append(padStringWithTrailingBlanks(stars, 10)).append("   ");
            for (int i = 0; i < occurrences.size(); i++) {
                String treename = (String) occurrences.elementAt(i);
                outContent.append(treename).append(", ");
            }
            outContent.append("\n");
        }
        outContent.append("\n");
        outContent.append("\n");
        outContent.append("\n");

        //now determine the pairwise overlap between the taxa sets of the input trees:
        final int maxDigits = 2;
        for (int i = 0; i < treenames.size(); i++) {
            outContent.append(" {").append(padStringWithLeadingBlanks(Integer.toString(i), maxDigits)).append("}=").append(treenames.get(i)).append("\n");
        }
        outContent.append("  ").append(padStringWithLeadingBlanks(Integer.toString(0), maxDigits)).append("  ");
        for (int i = 0; i < treenames.size(); i++) {

            outContent.append(" {").append(padStringWithLeadingBlanks(Integer.toString(i), maxDigits)).append("} ");
        }
        outContent.append("\n");
        for (int i = 0; i < treenames.size(); i++) {
            outContent.append(" {").append(padStringWithLeadingBlanks(Integer.toString(i), maxDigits)).append("} ");
            Set taxaInI = (Set) tree2taxaSet.get(treenames.get(i));
            for (Object treename : treenames) {
                HashSet taxaInJ = new HashSet((Set) tree2taxaSet.get(treename));
                taxaInJ.retainAll(taxaInI); //intersect
                outContent.append("  ").append(padStringWithLeadingBlanks(Integer.toString(taxaInJ.size()), maxDigits)).append("  ");
            }
            outContent.append("\n");
        }

        outContent.append("\n");
        outContent.append("\n");
        outContent.append("\n");
        outContent.append("]\n");
        outContent.append("\n");
        outContent.append("\n");

        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
		pw.print(outContent);
		pw.close();
    }

    public String sandboxedCall(HashMap localId2name, String tree) throws Exception {
        String pathToVersionClassFile = ClassLoader.getSystemClassLoader().getResource("splitstree4/Version.class").getPath();
        File classDir = new File(pathToVersionClassFile);
        classDir = classDir.getParentFile();
        classDir = classDir.getParentFile();

        System.out.println(classDir.getAbsolutePath());
        File libDir = new File(classDir.getParentFile(), "sandboxLib");
        File newickconverterJar = new File(libDir, "NewickConverter.jar");
        System.out.println("newickconverterJar: " + newickconverterJar);
        URLClassLoader ucl = new URLClassLoader(new URL[]{newickconverterJar.toURL()});
        Class x = Class.forName("splitsX.workbench.geneorder.Newick2TreeExperimentalRemap", true, ucl);
		System.out.println(x);
        Class[] parameterTypes = new Class[]{HashMap.class, String.class};
        Method remapMethod;
        Object[] arguments = new Object[]{localId2name, tree};
        String result = null;

        try {
            remapMethod = x.getMethod("remap", parameterTypes);
            result = (String) remapMethod.invoke(null, arguments);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        return result;
    }

    public String generateDateString() {
        return new Date().toString().replace(' ', '_').replace(',', '_')
                .replace('=', '_').replace(':', '.');
    }

    private static String padStringWithTrailingBlanks(String orig, int totalSize) {
        StringBuilder sb = new StringBuilder();
		sb.append(orig);
		sb.append(" ".repeat(Math.max(0, totalSize - orig.length())));
        return sb.toString();
    }

    private static String padStringWithLeadingBlanks(String orig, int totalSize) {
        StringBuilder sb = new StringBuilder();
		sb.append(" ".repeat(Math.max(0, totalSize - orig.length())));
		sb.append(orig);
        return sb.toString();
    }


}
