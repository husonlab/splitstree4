/**
 * Taxa.java
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
 *
 * @version $Id: Taxa.java,v 1.52 2010-05-09 19:15:29 bryant Exp $
 * @author Daniel Huson and David Bryant
 */
/**
 * @version $Id: Taxa.java,v 1.52 2010-05-09 19:15:29 bryant Exp $
 *
 * @author Daniel Huson and David Bryant
 *
 */

package splitstree4.nexus;

import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * NexusBlock taxa class
 */
public class Taxa extends NexusBlock {
    private int ntax = 0;
    private Vector taxLabels;
    private Vector taxInfos;
    private boolean mustDetectLabels = false;
    /**
     * Identification string
     */
    public final static String NAME = "Taxa";
    /**
     * special taxon label for which indexOf always returns 1
     */
    public final static String FIRSTTAXON = "First-Taxon"; // special


    /**
     * Construct a new Taxa object.
     */
    public Taxa() {
        super();
        clear();
    }

    /**
     * Construct a new taxa object and set the initial cardinality to ntax
     *
     * @param ntax
     */
    public Taxa(int ntax) {
        super();
        clear();
        setNtax(ntax);
    }

    /**
     * clears the block
     */
    public void clear() {
        mustDetectLabels = false;
        this.taxLabels = new Vector();
        this.taxInfos = new Vector();
    }

    /**
     * Returns a clone of this block
     *
     * @return a full clone
     */
    public Object clone() //throws CloneNotSupportedException
    {
        Taxa result = new Taxa();

        try {
            result.setNtax(getNtax());
            for (int t = 1; t <= getNtax(); t++) {
                result.setLabel(t, getLabel(t));
                result.setInfo(t, getInfo(t));
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return result;
    }

    /**
     * returns true, if lists of taxa are equal
     *
     * @param taxa the other taxa
     */
    public boolean equals(Taxa taxa) {
        if (getNtax() != taxa.getNtax())
            return false;

        for (int t = 1; t <= getNtax(); t++) {
            if (!getLabel(t).equals(taxa.getLabel(t)))
                return false;
        }
        return true;
    }

    /**
     * Get the number of taxa.
     *
     * @return number of taxa
     */
    public int getNtax() {
        return ntax;
    }

    /**
     * Set the number of taxa.
     *
     * @param ntax the number of taxa
     */
    public void setNtax(int ntax) {
        if (ntax < 0)
            ntax = 0;
        this.ntax = ntax;
        this.taxLabels.setSize(ntax + 1);
        this.taxInfos.setSize(ntax + 1);

    }

    /**
     * Gets the label of the i-th taxon
     *
     * @param i the index of the taxon
     * @return the taxon label
     */
    public String getLabel(int i) {
        return (String) taxLabels.get(i);
    }

    /**
     * Sets the label of the i-th taxon
     *
     * @param i   the index of the taxon
     * @param lab the label
     */
    public void setLabel(int i, String lab) throws SplitsException {
        if (i <= 0 || i > ntax)
            throw new SplitsException("index out of range: " + i);
        if (lab != null) {
            String illegal = ";():\\";
            for (int pos = 0; pos < illegal.length(); pos++)
                if (lab.contains("" + illegal.charAt(pos)))
                    throw new SplitsException("Illegal character '" + illegal.charAt(pos) + "' in taxon label ("
                            + i + "): '" + lab + "'");
        }
        taxLabels.set(i, lab);
    }

    /**
     * Gets the first index of a taxon with the given label
     *
     * @param lab the label
     * @return the index of the first taxon with the given index, or 0
     */
    public int indexOf(String lab) {
        if (lab.equals(Taxa.FIRSTTAXON))
            return 1;
        return taxLabels.indexOf(lab);
    }

    /**
     * Gets the info of the i-th taxon
     *
     * @param i the index of the taxon
     * @return the taxon label
     */
    public String getInfo(int i) {
        return (String) taxInfos.get(i);
    }

    /**
     * Sets the info of the i-th taxon
     *
     * @param i    the index of the taxon
     * @param info the label
     */
    public void setInfo(int i, String info) throws SplitsException {
        if (i <= 0 || i > ntax)
            throw new SplitsException("index out of range: " + i);
        taxInfos.set(i, info);
    }

    /**
     * Show the usage of taxa block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN TAXA;");
        ps.println("DIMENSIONS NTAX=number-of-taxa;");
        ps.println("[TAXLABELS taxon_1 taxon_2 ... taxon_ntax;]");
        ps.println("[TAXINFO info_1 info_2 ... info_ntax;]");
        ps.println("END;");
    }

    /**
     * Writes the taxa object in nexus format
     *
     * @param w a writer
     */
    public void write(Writer w) throws java.io.IOException {
        w.write("\nBEGIN " + Taxa.NAME + ";\n");
        w.write("DIMENSIONS ntax=" + getNtax() + ";\n");
        w.write("TAXLABELS\n");
        for (int i = 1; i <= getNtax(); i++)
            w.write("[" + i + "] '" + getLabel(i) + "'\n");
        w.write(";\n");
        if (hasInfos()) {
            w.write("TAXINFO\n");
            for (int i = 1; i <= getNtax(); i++)
                w.write("[" + i + "] '" + getInfo(i) + "'\n");
            w.write(";\n");
        }
        w.write("END; [" + Taxa.NAME + "]\n");
    }

    /**
     * returns true, if any taxon has an info string associated with it
     *
     * @return true, if some taxon has info
     */
    private boolean hasInfos() {
        for (int t = 1; t <= getNtax(); t++)
            if (getInfo(t) != null && getInfo(t).length() > 0)
                return true;
        return false;


    }

    /**
     * overrides method in NexusBlock
     *
     * @param w
     * @param taxa
     * @throws IOException
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        write(w);
    }


    /**
     * Reads a taxa object in NexusBlock format
     *
     * @param np nexus stream parser
     */
    public void read(NexusStreamParser np) throws IOException, SplitsException {
        np.matchBeginBlock(NAME);

        np.matchIgnoreCase("DIMENSIONS ntax=");
        setNtax(np.getInt());
        np.matchIgnoreCase(";");

        if (np.peekMatchIgnoreCase("taxlabels")) // grab labels now
        {
            np.matchIgnoreCase("taxlabels");
            if (np.peekMatchIgnoreCase("_detect_")) // for compatibility with SplitsTree3:
            {
                np.matchIgnoreCase("_detect_");
                setMustDetectLabels(true);
            } else {
                for (int i = 1; i <= getNtax(); i++) {
                    setLabel(i, np.getLabelRespectCase());

                    //Detect repeated labels
                    for (int j = 1; j < i; j++) {
                        if (getLabel(i).equals(getLabel(j)))
                            throw new IOException("Line " + np.lineno() +
                                    ": " + getLabel(i) + " appears twice in the taxa block");

                    }
                }
            }
            np.matchIgnoreCase(";");
        } else
            setMustDetectLabels(true); // must detect them in other block

        if (np.peekMatchIgnoreCase("taxinfo")) // grab labels now
        {
            np.matchIgnoreCase("taxinfo");

            for (int i = 1; i <= getNtax(); i++)
                setInfo(i, np.getLabelRespectCase());
            np.matchIgnoreCase(";");
        }
        np.matchEndBlock();

        if (getOriginalTaxa() == null)
            setOriginalTaxa((Taxa) clone()); // set the original taxa


    }

    /**
     * produces a full string representation of this nexus block
     *
     * @return object in necus
     */
    public String toString() {
        Writer w = new StringWriter();
        try {
            write(w);
        } catch (IOException ex) {
            Basic.caught(ex);
        }
        return w.toString();
    }


    /**
     * Returns the set of all taxa
     *
     * @return the set 1,2..,ntax
     */
    public TaxaSet getTaxaSet() {
        TaxaSet all = new TaxaSet();
        all.set(1, getNtax());
        return all;
    }

    /**
     * Do we need to detect taxon labels in other block?
     *
     * @return true, if we need to obtain taxon labels from other block
     */
    public boolean getMustDetectLabels() {
        return mustDetectLabels;
    }

    /**
     * Do we need to detect taxon labels in other block?
     *
     * @param mustDetectLabels true, if we need to obtain taxon labels from other block
     */
    public void setMustDetectLabels(boolean mustDetectLabels) {
        this.mustDetectLabels = mustDetectLabels;
        if (!mustDetectLabels && (getOriginalTaxa() == null
                || (getNtax() > 0 && getOriginalTaxa().getLabel(1) == null)))
            setOriginalTaxa((Taxa) clone()); // set the original taxa
        // TODO: this is a bit dirty...
    }

    /**
     * gets the induced taxon set
     *
     * @param origTaxa   nexus block of original taxa
     * @param hiddenTaxa set original taxa are to be hidden
     * @return new nexus block of resulting taxa
     */
    public static Taxa getInduced(Taxa origTaxa, TaxaSet hiddenTaxa) {
        Taxa taxa = new Taxa();
        try {
            if (hiddenTaxa != null)
                taxa.setNtax(origTaxa.getNtax() - hiddenTaxa.cardinality());
            else
                taxa.setNtax(origTaxa.getNtax());

            int count = 0;
            for (int t = 1; t <= origTaxa.getNtax(); t++) {
                if (hiddenTaxa == null || !hiddenTaxa.get(t)) {
                    count++;
                    taxa.setLabel(count, origTaxa.getLabel(t));
                    taxa.setInfo(count, origTaxa.getInfo(t));
                }
            }
        } catch (SplitsException ex) {
            Basic.caught(ex);
        }
        return taxa;
    }

    Taxa originalTaxa;
    TaxaSet hiddenTaxa;

    /**
     * hides the given set of taxa
     *
     * @param hiddenTaxa taxa numbers in original numbering
     */
    public void hideTaxa(TaxaSet hiddenTaxa) {
        if (originalTaxa == null && (hiddenTaxa == null || hiddenTaxa.cardinality() == 0))
            return;   // nothing to do
        this.hiddenTaxa = hiddenTaxa;

        if (originalTaxa == null)
            originalTaxa = (Taxa) this.clone(); // make a copy

        Taxa inducedTaxa = Taxa.getInduced(originalTaxa, hiddenTaxa);

        if (!inducedTaxa.equals(this))
            try {
                setNtax(inducedTaxa.getNtax());
                for (int t = 1; t <= getNtax(); t++) {
                    setLabel(t, inducedTaxa.getLabel(t));
                    setInfo(t, inducedTaxa.getInfo(t));
                }
            } catch (SplitsException ex) {
                Basic.caught(ex);
            }
    }

    /**
     * returns the original taxa set (that existed before hidding of taxa)
     *
     * @return original taxa set
     */
    public Taxa getOriginalTaxa() {
        return originalTaxa;
    }

    /**
     * set the original taxa
     *
     * @param taxa
     */
    public void setOriginalTaxa(Taxa taxa) {
        originalTaxa = taxa;
    }

    /**
     * gets the hidden taxa
     *
     * @return hidden taxa
     */
    public TaxaSet getHiddenTaxa() {
        return hiddenTaxa;
    }

    /**
     * additionally hide more taxa. This is used in the presence of partial trees.
     * Note that these numbers are given with respect to the current taxa set,
     * not the original one!
     *
     * @param additionalHidden
     */
    public void hideAdditionalTaxa(TaxaSet additionalHidden) throws SplitsException {
        if (originalTaxa == null)
            originalTaxa = (Taxa) this.clone(); // make a copy

        TaxaSet additionalO = new TaxaSet();

        int ntax = getOriginalTaxa().getNtax();
        if (additionalHidden != null) {
            for (int oID = 1; oID <= ntax; oID++) {
                int nID = indexOf(originalTaxa.getLabel(oID));
                if (nID != -1 && additionalHidden.get(nID)) {
                    additionalO.set(oID);
                }
            }
        }
        if (hiddenTaxa != null && additionalO != null &&
                hiddenTaxa.intersects(additionalO))
            throw new SplitsException("hidden <" + hiddenTaxa + "> and additional <"
                    + additionalO + "> intersect");

        int count = 0;
        for (int oID = 1; oID <= ntax; oID++) {
            if ((hiddenTaxa == null || !hiddenTaxa.get(oID))
                    && (additionalHidden == null || !additionalO.get(oID))) {
                ++count;
            }
        }
        setNtax(count);
        count = 0;
        for (int oID = 1; oID <= ntax; oID++) {
            {
                if ((hiddenTaxa == null || !hiddenTaxa.get(oID))
                        && (additionalHidden == null || !additionalO.get(oID))) {
                    if (getOriginalTaxa() != null) {
                        setLabel(++count, getOriginalTaxa().getLabel(oID));
                        setInfo(count, getOriginalTaxa().getInfo(oID));
                    }
                }
            }
        }
    }

    /**
     * returns a list of all taxa labels
     *
     * @return all taxa labels
     */
    public List getAllLabels() {
        List all = new LinkedList();
        for (int t = 1; t <= getNtax(); t++)
            all.add(getLabel(t));
        return all;
    }

    /**
     * print to System.err
     */
    public static void show(String label, Taxa taxa) {
        StringWriter sw = new StringWriter();
        try {
            taxa.write(sw, taxa);
        } catch (IOException ex) {
            Basic.caught(ex);
        }
        System.err.println(label + ":\n" + sw.toString());
    }

    /**
     * add a taxon to the taxa block
     *
     * @param taxonLabel
     */
    public void add(String taxonLabel) {
        add(taxonLabel, null);
    }

    /**
     * add a taxon to the taxa block
     *
     * @param taxonLabel
     * @param info       information string associated with taxon
     */
    public void add(String taxonLabel, String info) {
        try {
            Taxa old = (Taxa) clone();
            setNtax(getNtax() + 1);
            for (int t = 1; t <= old.getNtax(); t++) {
                setLabel(t, old.getLabel(t));
                setInfo(t, old.getInfo(t));
            }
            setLabel(getNtax(), taxonLabel);
            setInfo(getNtax(), info);
        } catch (SplitsException ex) {
            Basic.caught(ex);
        }
    }

    /**
     * returns true, if taxa block contains given set of labels
     *
     * @param labels
     * @return true, if all labels contained
     */
    public boolean contains(Collection labels) {
        for (Object label1 : labels) {
            String label = (String) label1;
            if (indexOf(label) == -1)
                return false;
        }
        return true;
    }

    /**
     * gets the set of all labels for the given taxaSet
     *
     * @param taxaSet
     * @return set of labels
     */
    public Set<String> getLabels(TaxaSet taxaSet) {
        Set<String> labels = new HashSet<>();
        for (int i = taxaSet.getBits().nextSetBit(0); i != -1; i = taxaSet.getBits().nextSetBit(i + 1))
            labels.add(getLabel(i));
        return labels;
    }
}

// EOF
