/**
 * ReticulateNetwork.java
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
package splitstree4.algorithms.splits;

import jloda.util.UsageException;
import splitstree4.core.Document;
import splitstree4.nexus.Network;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * framework for generating reticulate evolution graphs from splits
 *
 * @author huson, kloepper
 * Date: 16-Sep-2004
 */
public class ReticulateNetwork implements Splits2Network {
    // known methods:
    static final String RECOMB_2005 = "RECOMB2005";
    static final String RECOMB_2007 = "RECOMB2007";

    // known options:
    protected String optionMethod = RECOMB_2007;
    protected boolean optionShowSplits = false;
    String optionOutGroup = Taxa.FIRSTTAXON;
    protected int optionMaxReticulationsPerTangle = 4;
    protected int optionWhich = 1;

    public final static String EQUALANGLE60 = "EqualAngle60";
    public final static String EQUALANGLE90 = "EqualAngle90";
    public final static String EQUALANGLE120 = "EqualAngle120";
    public final static String EQUALANGLE180 = "EqualAngle180";
    public final static String EQUALANGLE360 = "EqualAngle360";
    public static final String EQUALANGLE_PREFIX = "EqualAngle";// equal angle methods prefix

    public final static String RECTANGULARPHYLOGRAM = "RectangularPhylogram";
    public final static String RECTANGULARCLADOGRAM = "RectangularCladogram";

    public String optionLayout = EQUALANGLE120;

    protected boolean optionShowSequences = false; // used in extension RecombinationNetwork
    protected boolean optionShowMutations = false;  // used in extension RecombinationNetwork

    public int optionPercentOffset = 10;

    protected Map split2Chars = null; // map each split to one or more character positions
    protected char[] firstChars = null; // character states for first taxon

    /**
     * Applies the method to the given data
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return the computed set of splits
     */
    public Network apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        switch (optionMethod) {
            case RECOMB_2005:
                ReticulateNetworkRECOMB2005 recomb2005 = new ReticulateNetworkRECOMB2005();
                recomb2005.setOptionShowSplits(getOptionShowSplits());
                recomb2005.setOptionOutGroup(getOptionOutGroup());
                recomb2005.setOptionLayout(getOptionLayout());
                recomb2005.setOptionWhich(getOptionWhich());
                recomb2005.setOptionShowSequences(optionShowSequences);
                recomb2005.setOptionShowMutations(optionShowMutations);
                recomb2005.setOptionPercentOffset(getOptionPercentOffset());
                recomb2005.setSplit2Chars(getSplit2Chars());
                recomb2005.setFirstChars(getFirstChars());

                return recomb2005.apply(doc, taxa, splits);
            case RECOMB_2007:
                GalledNetwork recomb2007 = new GalledNetwork();
                recomb2007.setOptionShowSplits(getOptionShowSplits());
                recomb2007.setOptionOutGroup(getOptionOutGroup());
                recomb2007.setOptionLayout(getOptionLayout());
                recomb2007.setOptionWhich(getOptionWhich());
                recomb2007.setOptionPercentOffset(getOptionPercentOffset());
                recomb2007.setOptionShowSequences(optionShowSequences);
                recomb2007.setOptionShowMutations(optionShowMutations);
                recomb2007.setSplit2Chars(getSplit2Chars());
                // recomb2007.setFirstChars(getFirstChars());

                return recomb2007.apply(doc, taxa, splits);

            default:
                throw new UsageException("ReticulateNetwork: No such method: " + optionMethod);
        }

    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the splits
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValid(taxa) && doc.isValid(splits);
    }


    /**
     * gets the method to use
     *
     * @return name of method
     */
    public String getOptionMethod() {
        return optionMethod;
    }

    /**
     * sets the method to use
     *
     * @param optionMethod
     */
    public void setOptionMethod(String optionMethod) {
        this.optionMethod = optionMethod;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List selectionOptionMethod(Document doc) {
        List methods = new LinkedList();
        methods.add(RECOMB_2007);
        methods.add(RECOMB_2005);
        return methods;
    }

    /**
     * preserve edges in components?
     *
     * @return show splits
     */
    public boolean getOptionShowSplits() {
        return optionShowSplits;
    }

    /**
     * preserve edges in components?
     *
     * @param optionShowSplits
     */
    public void setOptionShowSplits(boolean optionShowSplits) {
        this.optionShowSplits = optionShowSplits;
    }

    public String getOptionOutGroup() {
        return optionOutGroup;
    }

    public void setOptionOutGroup(String optionOutGroup) {
        this.optionOutGroup = optionOutGroup;
    }

    public int getOptionMaxReticulationsPerTangle() {
        return optionMaxReticulationsPerTangle;
    }

    public void setOptionMaxReticulationsPerTangle(int optionMaxReticulationsPerTangle) {
        this.optionMaxReticulationsPerTangle = optionMaxReticulationsPerTangle;
    }

    /**
     * if there is more than one solution for a component, get this one
     *
     * @return which
     */
    public int getOptionWhich() {
        return optionWhich;
    }

    public void setOptionWhich(int optionWhich) {
        this.optionWhich = optionWhich;
    }

    /**
     * gets the split 2 characters map
     *
     * @return splits 2 characters map
     */
    public Map getSplit2Chars() {
        return split2Chars;
    }

    /**
     * sets the split 2 characters map if we want to label nodes and edges by
     *
     * @param split2Chars
     */
    public void setSplit2Chars(Map split2Chars) {
        this.split2Chars = split2Chars;
    }

    /**
     * gets the reference sequence of labeling
     *
     * @return reference sequence
     */
    public char[] getFirstChars() {
        return firstChars;
    }

    /**
     * sets the reference sequence of labeling
     *
     * @param firstChars
     */
    public void setFirstChars(char[] firstChars) {
        this.firstChars = firstChars;
    }


    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return "Reticulate networks: Huson et al, RECOMB 2007, RECOMB 2005";
    }

    /*

    public boolean getOptionShowSequences() {
        return optionShowSequences;
    }

    public void setOptionShowSequences(boolean optionShowSequences) {
        this.optionShowSequences = optionShowSequences;
    }

    public boolean getOptionShowMutations() {
        return optionShowMutations;
    }

    public void setOptionShowMutations(boolean optionShowMutations) {
        this.optionShowMutations = optionShowMutations;
    }
    */

    public String getOptionLayout() {
        return optionLayout;
    }

    public void setOptionLayout(String optionLayout) {
        this.optionLayout = optionLayout;
    }

    /**
     * returns list of all known methods
     *
     * @return methods
     */
    public List selectionOptionLayout(Document doc) {
        List list = new LinkedList();
        list.add(EQUALANGLE60);
        list.add(EQUALANGLE120);
        list.add(EQUALANGLE180);
        list.add(EQUALANGLE360);
        list.add(RECTANGULARPHYLOGRAM);
        list.add(RECTANGULARCLADOGRAM);
        return list;
    }

    /**
     * gets the angle associated with a layout option
     *
     * @param optionLayout
     * @return angle
     */
    public static int getLayoutAngle(String optionLayout) {
        return Integer.parseInt(optionLayout.substring("EqualAngle".length()));
    }


    public int getOptionPercentOffset() {
        return optionPercentOffset;
    }

    public void setOptionPercentOffset(int optionPercentOffset) {
        this.optionPercentOffset = Math.max(0, Math.min(100, optionPercentOffset));
    }

}
