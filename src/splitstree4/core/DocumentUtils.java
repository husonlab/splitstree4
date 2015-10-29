/**
 * DocumentUtils.java
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
package splitstree4.core;

import jloda.util.Alert;
import jloda.util.Basic;
import splitstree4.externalIO.exports.NewickTree;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Sets;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;
import splitstree4.util.NexusFileFilter;
import splitstree4.util.Partition;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * some additional commands
 * Daniel Huson and David Bryant
 */
public class DocumentUtils {
    /**
     * load trees from multiple files
     *
     * @param files
     * @param doc
     */
    static public void loadMultipleTreeFiles(List files, Document doc) {
        if (files == null || files.size() == 0)
            return;
        final StringBuilder buf = new StringBuilder();
        for (Object file1 : files) {
            final String fileName = (String) file1;
            final File file = new File(fileName);
            if (!file.exists() || !file.canRead()) {
                new Alert("Cannot open: " + fileName);
                continue;
            }
            try {
                Document doci = new Document();
                if (NexusFileFilter.isNexusFile(file)) {
                    System.err.println("Attempting to read tree in format: NEXUS");
                    doci.open(null, file);
                    System.err.println("done");
                } else
                    doci.readNexus(new StringReader(ImportManager.importData(file)));

                NewickTree exporter = new NewickTree();
                List list = new LinkedList();
                list.add(Trees.NAME);
                StringWriter sw = new StringWriter();
                if (exporter.isApplicable(doci, list)) {
                    exporter.apply(sw, doci, list);
                    buf.append(sw.toString());
                } else
                    throw new Exception("No trees");
            } catch (Exception ex) {
                Basic.caught(ex);
                new Alert("Import trees failed for file=" + fileName + ": " + ex.getMessage());
            }
        }
        if (buf.toString().length() > 0) {
            try {
                doc.readNexus(new StringReader(ImportManager.importDataFromString(buf.toString())));
                System.err.println("Number of trees loaded: " + (doc.isValidByName(Trees.NAME) ?
                        doc.getTrees().getNtrees() : 0));

            } catch (Exception ex) {
                new Alert("Import trees failed: " + ex.getMessage());
            }
        }
    }

    /**
     * concatenates the sequences found in the different files. Each file must contain precisely the
     * same set of taxa
     *
     * @param files
     * @param doc
     */
    static public void concatenateSequences(List files, Document doc) {
        if (files == null || files.size() == 0)
            return;

        int count = 0;        //Number of files read in so far.
        final Map taxon2sequence = new HashMap(); // maps taxa to sequences

        int currentPos = 1;
        final Partition partition = new Partition();
        final Sets sets = new Sets();

        final Set usedNames = new HashSet();
        final Map taxaOmitted = new TreeMap();

        for (Object file1 : files) {
            final String fileName = (String) file1;
            final File file = new File(fileName);
            if (!file.exists() || !file.canRead()) {
                new Alert("Cannot open: " + fileName);
                continue;
            }

            try {
                Document doci = new Document();
                if (NexusFileFilter.isNexusFile(file)) {
                    System.err.println("Attempting to read characters in format: NEXUS");
                    doci.open(null, file);
                    System.err.println("done");
                } else
                    doci.readNexus(new StringReader(ImportManager.importData(file)));

                if (!doci.isValidByName(Taxa.NAME)
                        || !doci.isValidByName(Characters.NAME))
                    throw new Exception("No character sequences found in file");

                //Extract a valid block name from the filename, dealing with duplicate files\
                //as necessary.
                String name = file.getName();
                int pos = name.lastIndexOf('.');
                if (pos > 0)
                    name = name.substring(0, pos);

                int copyNumber = 2;
                while (usedNames.contains(name)) {
                    if (name.lastIndexOf('_') != -1)
                        name = name.substring(0, name.lastIndexOf('_'));
                    name = name + "_" + copyNumber;
                    copyNumber++;
                }
                usedNames.add(name);

                //Check the taxa... that there are no duplicates and that
                //every taxa was present in the first file.
                //If there are taxa that are not present, they are recorded in taxaOmitted.
                //Valid taxa have their sequences stored in geneSequences.
                HashMap geneSequences = new HashMap();

                Set seen = new HashSet();
                for (int t = 1; t <= doci.getTaxa().getNtax(); t++) {
                    String label = doci.getTaxa().getLabel(t);
                    if (count == 0)
                        taxon2sequence.put(label, new StringBuffer());
                    if (count > 0 && taxon2sequence.get(label) == null) {
                        String geneList = ((String) taxaOmitted.get(label));
                        if (geneList == null)
                            geneList = "";
                        else
                            geneList += ", ";
                        taxaOmitted.put(label, geneList + name);
                    } else
                        geneSequences.put(label, doci.getCharacters().getRowAsString(t));
                    if (seen.contains(label))
                        throw new Exception("Multple occurrence of taxon: " + label);
                    seen.add(label);
                }

                count++;

                //Above we looped through taxa in the gene. Now we loop through taxa present
                //in previous genes. If there are any that are not present in this gene then they
                //must be removed. Note sure how to manage the iterator, so I just keep a list of those
                //to remove and delete them in a second pass.

                List taxaToRemove = new LinkedList();

                for (Object o : taxon2sequence.keySet()) {
                    String label = (String) o;
                    String seq = (String) geneSequences.get(label);
                    if (seq != null) {
                        StringBuffer bufi = (StringBuffer) taxon2sequence.get(label);
                        bufi.append(seq);
                        taxon2sequence.put(label, bufi);
                    } else {
                        //label not present in this gene. Should be omitted from all preceeding genes.
                        String geneList = "";
                        for (Iterator it2 = usedNames.iterator(); it2.hasNext(); ) {
                            String gname = (String) it2.next();
                            if (gname.equals(name))   //Don't put the current gene.
                                continue;
                            if (geneList.length() > 0)
                                geneList += ", ";
                            geneList += (String) it2.next();
                        }
                        taxaOmitted.put(label, geneList);
                        taxaToRemove.add(label);
                    }
                }

                for (Object aTaxaToRemove : taxaToRemove) taxon2sequence.remove(aTaxaToRemove);

                //Add the block into the character partition.
                int newPos = currentPos + doci.getCharacters().getNchar() - 1;
                partition.addBlock(currentPos, newPos, file.getName());
                sets.addCharSet(name, currentPos, newPos);
                currentPos = newPos + 1;


            } catch (Exception ex) {
                Basic.caught(ex);
                new Alert("Concatenate sequences failed for file=" + fileName + ": " + ex.getMessage());
            }
        }
        sets.addCharPartition("input", partition);
        if (taxon2sequence.size() != 0) {
            try {
                StringBuilder buf = new StringBuilder();
                for (Object o : taxon2sequence.keySet()) {
                    String label = (String) o;
                    buf.append("> ").append(label).append("\n");
                    buf.append(taxon2sequence.get(label)).append("\n");
                }
                doc.readNexus(new StringReader(ImportManager.importDataFromString(buf.toString())));
                doc.setSets(sets);

                System.err.println("Number of sequence files concatenated: " + count);
            } catch (Exception ex) {
                new Alert("Concatenate sequences failed: " + ex.getMessage());
            }
        }
        if (!taxaOmitted.isEmpty()) {
            String alert = "Omitted " + taxaOmitted.size() + " taxa that were not present in all files. ";
            alert += "" + doc.getTaxa().getNtax() + " taxa remain.";
            new Alert(alert);
            System.err.println("Taxa Omitted\n============\n");
            for (Object o : taxaOmitted.keySet()) {
                String label = (String) o;
                System.err.println(label + "\t:\t" + taxaOmitted.get(label));
            }
        }
    }
}
