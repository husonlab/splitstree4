/**
 * Assumptions.java 
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
package splitstree.nexus;

import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree.algorithms.Transformation;
import splitstree.algorithms.characters.CharactersTransform;
import splitstree.algorithms.characters.Uncorrected_P;
import splitstree.algorithms.distances.DistancesTransform;
import splitstree.algorithms.distances.NeighborNet;
import splitstree.algorithms.quartets.Coalescent;
import splitstree.algorithms.quartets.QuartetsTransform;
import splitstree.algorithms.reticulate.ReticulateEqualAngle;
import splitstree.algorithms.reticulate.ReticulateTransform;
import splitstree.algorithms.splits.EqualAngle;
import splitstree.algorithms.splits.SplitsTransform;
import splitstree.algorithms.trees.TreeSelector;
import splitstree.algorithms.trees.TreesTransform;
import splitstree.algorithms.unaligned.Noalign;
import splitstree.algorithms.unaligned.UnalignedTransform;
import splitstree.algorithms.util.Configurator;
import splitstree.core.Document;
import splitstree.core.TaxaSet;
import splitstree.main.SplitsTreeProperties;
import splitstree.util.PluginClassLoader;

import java.io.*;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;


/**
 * The nexus assumptions block. This is where we keep track of the
 * assumptions under which the data is processed.
 */
public class Assumptions extends NexusBlock {
    /**
     * Identification string
     */
    public final static String NAME = "st_Assumptions";
    private TaxaSet exTaxa;
    private List<String> useTaxaSets;
    private List<Integer> exChar;
    private List<String> useCharSets;
    private List<String> exTrees;
    private List<Integer> exSplits;


    public final static String USE_ALL = "all"; // use all taxa or characters

    private SplitsPostProcess splitsPostProcess;
    private boolean uptodate;

    private String unalignTransform;
    private String unalignTransformParam;


    private String charTransform;
    private String charTransformParam;

    private String distTransform;
    private String distTransformParam;

    private String quartetsTransform;
    private String quartetsTransformParam;

    private String treesTransform;
    private String treesTransformParam;

    private String reticulateTransform;
    private String reticulateTransformParam;

    private String splitsTransform;
    private String splitsTransformParam;

    private boolean excludeGaps;
    private double excludeMissing;    //Exclude characters with more than this amount missing. 1.0 if no characters.
    private boolean excludeNonParsimony;
    private boolean excludeCodon1;
    private boolean excludeCodon2;
    private boolean excludeCodon3;
    private int excludeConstant;

    private boolean autoLayoutNodeLabels;
    private boolean radiallyLayoutNodeLabels;

    private String firstDirtyBlock;

    private int layoutStrategy;

    // graph layout strategies:
    final static public int RECOMPUTE = 0;
    final static public int STABILIZE = 1;
    final static public int SNOWBALL = 2;
    final static public int KEEP = 3;

    /**
     * Constructor
     */
    public Assumptions() {
        exTaxa = null;
        useTaxaSets = null;
        exChar = null;
        useCharSets = null;
        exTrees = null;
        exSplits = null;

        splitsPostProcess = new SplitsPostProcess();

        uptodate = false;

        unalignTransform = Basic.getShortName(Noalign.class);
        unalignTransformParam = null;


        charTransform = Basic.getShortName(Uncorrected_P.class);
        charTransformParam = null;

        distTransform = Basic.getShortName(NeighborNet.class);
        distTransformParam = null;

        quartetsTransform = Basic.getShortName(Coalescent.class);
        quartetsTransformParam = null;

        treesTransform = Basic.getShortName(TreeSelector.class);
        treesTransformParam = null;

        splitsTransform = Basic.getShortName(EqualAngle.class);
        splitsTransformParam = null;

        if (SplitsTreeProperties.ALLOW_RETICULATE) {
            reticulateTransform = Basic.getShortName(ReticulateEqualAngle.class);
            reticulateTransformParam = null;
        }
        excludeGaps = false;
        excludeMissing = 1.0;
        excludeNonParsimony = false;
        excludeCodon1 = false;
        excludeCodon2 = false;
        excludeCodon3 = false;
        excludeConstant = 0;

        autoLayoutNodeLabels = true;

        firstDirtyBlock = "";

        layoutStrategy = 0;
    }

    /**
     * has data been marked uptodate i.e. in a complete input file?
     *
     * @return true, if data doesn't need immediate updating
     */
    public boolean isUptodate() {
        return uptodate;
    }

    /**
     * data is update-to-date and next call to update will be ignored
     *
     * @param uptodate
     */
    public void setUptodate(boolean uptodate) {
        this.uptodate = uptodate;
    }

    /**
     * Gets the name of the unaligned transform
     *
     * @return the unaligned transform
     */
    public String getUnalignedTransformName() {
        return unalignTransform;
    }

    /**
     * Sets the unaligned transform
     *
     * @param trans the transform
     */
    public void setUnalignedTransformName(String trans) {
        this.unalignTransform = trans;
    }

    /**
     * Sets the unaligned transform parameters
     *
     * @param param the transform parameters
     */
    public void setUnalignedTransformParam(String param) {
        this.unalignTransformParam = param;
    }

    /**
     * Returns the current unaligned transform parameters
     *
     * @return current unaligned transform parameters
     */
    public String getUnalignedTransformParam() {
        return unalignTransformParam;
    }

    /**
     * Returns the current characters transform parameters
     *
     * @return current characters transform parameters
     */
    public String getCharactersTransformParam() {
        return charTransformParam;
    }

    /**
     * Returns the current distances transform parameters
     *
     * @return current distances transform parameters
     */
    public String getDistancesTransformParam() {
        return distTransformParam;
    }

    /**
     * Returns the current splits transform parameters
     *
     * @return current splits transform parameters
     */
    public String getSplitsTransformParam() {
        return splitsTransformParam;
    }


    /**
     * Gets charTransform
     *
     * @return the charTransform
     */
    public String getCharactersTransformName() {
        return charTransform;
    }

    /**
     * Sets the charTransform
     *
     * @param transform is charTransform
     */
    public void setCharactersTransformName(String transform) {
        this.charTransform = transform;
    }

    /**
     * Sets the charTransformParameter
     *
     * @param param is charTransformParameter
     */
    public void setCharactersTransformParam(String param) {
        this.charTransformParam = param;
    }

    /**
     * Gets distTransform
     *
     * @return the distTransform
     */
    public String getDistancesTransformName() {
        return distTransform;
    }

    /**
     * Sets the distTransform
     *
     * @param transform is distTransform
     */
    public void setDistancesTransformName(String transform) {
        this.distTransform = transform;
    }

    /**
     * Sets the distTransformParam
     *
     * @param param is distTransformParam
     */
    public void setDistancesTransformParam(String param) {
        this.distTransformParam = param;
    }

    /**
     * Gets the quartets transform name
     *
     * @return the quartets transform
     */
    public String getQuartetsTransformName() {
        return quartetsTransform;
    }

    /**
     * Sets the quartests transform
     *
     * @param trans the quartets transform
     */
    public void setQuartetsTransformName(String trans) {
        this.quartetsTransform = trans;
    }

    /**
     * Gets the quarets transforma parameter
     *
     * @return the quartets trajsform parameter
     */
    public String getQuartetsTransformParam() {
        return quartetsTransformParam;
    }

    /**
     * Sets the quartet transform parameters
     *
     * @param param the parameter
     */
    public void setQuartetsTransformParam(String param) {
        this.quartetsTransformParam = param;
    }

    /**
     * gets trees transform parameter string
     *
     * @return the parameters
     */
    public String getTreesTransformParam() {
        return treesTransformParam;
    }

    /**
     * sets the trees transform paramters
     *
     * @param param
     */
    public void setTreesTransformParam(String param) {
        this.treesTransformParam = param;
    }

    /**
     * Gets the tree transform name
     *
     * @return the name
     */
    public String getTreesTransformName() {
        return treesTransform;
    }

    /**
     * Sets the trees transform name
     *
     * @param name
     */
    public void setTreesTransformName(String name) {
        this.treesTransform = name;
    }

    /**
     * Gets splitsTransform
     *
     * @return the splitsTransform
     */
    public String getSplitsTransformName() {
        return splitsTransform;
    }

    /**
     * Sets the splitsTransform
     *
     * @param trans is splitsTransform
     */
    public void setSplitsTransformName(String trans) {
        this.splitsTransform = trans;
    }

    /**
     * Sets the splitsTransformParam
     *
     * @param param is splitsTransformParam
     */
    public void setSplitsTransformParam(String param) {
        this.splitsTransformParam = param;
    }

    /**
     * Gets the name of the reticulate transform
     *
     * @return the reticulate transform
     */
    public String getReticulateTransformName() {
        return this.reticulateTransform;
    }

    /**
     * Sets the reticulateTransform
     *
     * @param trans is reticulateTransform
     */
    public void setReticulateTrasformName(String trans) {
        reticulateTransform = trans;
    }

    /**
     * Returns the current reticulate transform parameters
     *
     * @return current reticulate transform parameters
     */
    public String getReticulateTransformParam() {
        return reticulateTransformParam;
    }

    /**
     * Sets the reticulateTransformParam
     *
     * @param param is reticulateTransformParam
     */
    public void setReticulateTransformParam(String param) {
        this.reticulateTransformParam = param;
    }


    /**
     * Gets the excludeGaps
     *
     * @return the excludeGaps
     */
    public boolean getExcludeGaps() {
        return excludeGaps;
    }

    /**
     * Sets the excludeGaps
     *
     * @param excGaps is excludeGaps
     */
    public void setExcludeGaps(boolean excGaps) {
        this.excludeGaps = excGaps;
    }

    /**
     * Gets the excludeMissing
     * Threshold for missing data in characters. Characters with more than this proportion of missing
     * data are excluded. Hence 1.0 means none are excluded.
     * @return the excludeMissing
     */
    public double getExcludeMissing() {
        return excludeMissing;
    }

    /**
     * Sets the excludeMissing
     * Threshold for missing data in characters. Characters with more than this proportion of missing
     * data are excluded. Hence 1.0 means none are excluded.
     * @param excMissing is excludeMissing
     */
    public void setExcludeMissing(double excMissing) {
        this.excludeMissing = excMissing;
    }

    /**
     * Gets the excludeNonParsimony
     *
     * @return the excludeNonParsimony
     */
    public boolean getExcludeNonParsimony() {
        return excludeNonParsimony;
    }

    /**
     * Sets the excludeNonParsimony
     *
     * @param excNonParsi is excludeNonParsimony
     */
    public void setExcludeNonParsimony(boolean excNonParsi) {
        this.excludeNonParsimony = excNonParsi;
    }

    /**
     * Gets the excludeCodon1
     *
     * @return the excludeCodon1
     */
    public boolean getExcludeCodon1() {
        return excludeCodon1;
    }

    /**
     * Sets the excludeCodon1
     *
     * @param excCodon1 is excludeCodon1
     */
    public void setExcludeCodon1(boolean excCodon1) {
        this.excludeCodon1 = excCodon1;
    }

    /**
     * Gets the excludeCodon2
     *
     * @return the excludeCodon2
     */
    public boolean getExcludeCodon2() {
        return excludeCodon2;
    }

    /**
     * Sets the excludeCodon2
     *
     * @param excCodon2 is excludeCodon2
     */
    public void setExcludeCodon2(boolean excCodon2) {
        this.excludeCodon2 = excCodon2;
    }

    /**
     * Gets the excludeCodon3
     *
     * @return the excludeCodon3
     */
    public boolean getExcludeCodon3() {
        return excludeCodon3;
    }

    /**
     * Sets the excludeCodon3
     *
     * @param excCodon3 is excludeCodon3
     */
    public void setExcludeCodon3(boolean excCodon3) {
        this.excludeCodon3 = excCodon3;
    }

    /**
     * Gets the excludeConstant
     *
     * @return the excludeConstant
     */
    public int getExcludeConstant() {
        return excludeConstant;
    }

    /**
     * Sets the excludeConstant
     *
     * @param excConstant is excludeConstant
     */
    public void setExcludeConstant(int excConstant) {
        this.excludeConstant = excConstant;
    }

    /**
     * Gets the set of taxa that are excluded from all computations
     *
     * @return the list of excluded taxa
     */
    public TaxaSet getExTaxa() {
        return exTaxa;
    }

    /**
     * Gets the set of character positions that are excluded from all
     * computations
     *
     * @return the list of excluded characters
     */
    public List<Integer> getExChar() {
        return exChar;
    }

    /**
     * Sets the set of taxa that are excluded from all computations
     *
     * @param extaxa the set of excluded taxa
     */
    public void setExTaxa(TaxaSet extaxa) {
        this.exTaxa = extaxa;
    }

    public List<String> getUseTaxaSets() {
        return useTaxaSets;
    }

    public void setUseTaxaSets(List<String> useTaxaSets) {
        if (useTaxaSets != null && useTaxaSets.contains(USE_ALL))
            this.useTaxaSets = null;
        else
            this.useTaxaSets = useTaxaSets;
    }

    /**
     * Sets the list of character positions that are excluded from all
     * computations
     *
     * @param exchar the list of excluded characters
     */
    public void setExChar(List<Integer> exchar) {
        this.exChar = exchar;
    }


    public List<String> getUseCharSets() {
        return useCharSets;
    }

    public void setUseCharSets(List<String> useCharSets) {
        if (useCharSets != null && useCharSets.contains(USE_ALL))
            this.useCharSets = null;
        else
            this.useCharSets = useCharSets;
    }

    /**
     * sets the list of trees to be excluded.
     * List must contain names of trees
     *
     * @param extrees
     */
    public void setExTrees(List<String> extrees) {
        this.exTrees = extrees;
    }

    /**
     * returns the list of excluded trees
     *
     * @return extrees
     */
    public List<String> getExTrees() {
        return exTrees;
    }

    /**
     * sets the list of splits to be excluded.
     * List must contain names of splits
     *
     * @param exSplits
     */
    public void setExSplits(List<Integer> exSplits) {
        this.exSplits = exSplits;
    }

    /**
     * returns the list of excluded splits
     *
     * @return exsplits
     */
    public List<Integer> getExSplits() {
        return exSplits;
    }

    /**
     * are we using a heuristic to stabilize the layout of trees?
     * Pairwise stabilize=1, snowball stabilize=2
     *
     * @return stabilize layout?
     */
    public int getLayoutStrategy() {
        return layoutStrategy;
    }

    /**
     * are we using a heuristic to stabilize the layout of trees?
     * * Pairwise stabilize=1, snowball stabilize=2
     *
     * @param layoutStrategy
     */
    public void setLayoutStrategy(int layoutStrategy) {
        this.layoutStrategy = layoutStrategy;
    }

    /**
     * is auto layout of node labels on?
     *
     * @return auto layout node labels?
     */
    public boolean getAutoLayoutNodeLabels() {
        return autoLayoutNodeLabels;
    }

    /**
     * set auto layout of node labels
     *
     * @param autoLayoutNodeLabels
     */
    public void setAutoLayoutNodeLabels(boolean autoLayoutNodeLabels) {
        this.autoLayoutNodeLabels = autoLayoutNodeLabels;
    }


    public boolean getRadiallyLayoutNodeLabels() {
        return radiallyLayoutNodeLabels;
    }

    public void setRadiallyLayoutNodeLabels(boolean radiallyLayoutNodeLabels) {
        this.radiallyLayoutNodeLabels = radiallyLayoutNodeLabels;
    }

    /**
     * Read the assumptions block.
     *
     * @param np the nexus parser
     */
    public void read(NexusStreamParser np, Taxa taxa) throws IOException {
        clearFirstDirtyBlock();
        setUptodate(false);

        if (taxa.getMustDetectLabels())
            throw new IOException("line " + np.lineno() +
                    ": Can't read ASSUMPTIONS block because no taxlabels given in TAXA block");

        np.matchBeginBlock(NAME);

        try {
            np.pushPunctuationCharacters(NexusStreamParser.ASSIGNMENT_PUNCTUATION);
            // catch any expections below and reset punctuation there

            while (!np.peekMatchIgnoreCase("END;")) {
                if (np.peekMatchIgnoreCase("CHARSET"))
                    new Alert("Found CHARSET in ASSUMPTIONS block,please put into a SET-block");
                if (np.peekMatchIgnoreCase("TAXSET"))
                    new Alert("Found TAXSET in ASSUMPTIONS block,please put into a SET-block");
                if (np.peekMatchIgnoreCase("CHARPARTITION"))
                    new Alert("Found CHARPARTITION in ASSUMPTIONS block,please put into a SET-block");
                if (np.peekMatchIgnoreCase("SplitsPostProcess")) {
                    getSplitsPostProcess().read(np);
                } else if (np.peekMatchIgnoreCase("uptodate")) {
                    np.matchIgnoreCase("uptodate;");
                    setUptodate(true);
                } else if (np.peekMatchIgnoreCase("extaxa")) {
                    np.matchIgnoreCase("extaxa");
                    exTaxa = new TaxaSet();

                    if (np.peekMatchIgnoreCase("=none;")) // restore all
                    {
                        np.matchIgnoreCase("=none;");
                    } else // hide the listed ones
                    {
                        List tokens = np.getTokensRespectCase("=", ";");

                        for (Object token : tokens) {
                            boolean ok = false; // found taxon yet?
                            String label = (String) token;

                            int i = taxa.getOriginalTaxa().indexOf(label);
                            if (i != -1) // label found
                                ok = true;
                            else // label not found, perhaps find its id?
                            {
                                try {
                                    i = Integer.parseInt(label);
                                    if (i > 0 && i <= taxa.getOriginalTaxa().getNtax())
                                        ok = true;
                                } catch (Exception ex) {
                                }
                            }
                            if (ok)
                                exTaxa.set(i);
                        }
                    }
                    updateFirstDirtyBlock(Taxa.NAME);
                } else if (np.peekMatchIgnoreCase("exchar")) {
                    np.matchIgnoreCase("exchar");

                    exChar = np.getIntegerList("=", ";");

                    updateFirstDirtyBlock(Characters.NAME);
                } else if (np.peekMatchIgnoreCase("extrees")) {
                    np.matchIgnoreCase("extrees");
                    exTrees = new LinkedList<>();

                    np.pushPunctuationCharacters(NexusStreamParser.SEMICOLON_PUNCTUATION);
                    try {
                        if (np.peekMatchIgnoreCase("=none;")) // restore all
                        {
                            np.matchIgnoreCase("=none;");
                        } else // hide the listed ones
                        {
                            List tokens;
                            tokens = np.getTokensRespectCase("=", ";");
                            // System.err.println("Tokens " + tokens);
                            for (Object token : tokens) {
                                String label = (String) token;
                                exTrees.add(label);
                            }
                        }
                    } finally {
                        np.popPunctuationCharacters();
                    }
                    updateFirstDirtyBlock(Trees.NAME);
                } else if (np.peekMatchIgnoreCase("exsplits")) {
                    np.matchIgnoreCase("exsplits");
                    exSplits = np.getIntegerList("=", ";");
                    updateFirstDirtyBlock(Splits.NAME);
                } else if (np.peekMatchIgnoreCase("usetaxset")) {
                    np.matchIgnoreCase("usetaxset");
                    List<String> sets = np.getTokensRespectCase("=", ";");
                    setUseTaxaSets(sets);
                } else if (np.peekMatchIgnoreCase("usecharset")) {
                    np.matchIgnoreCase("usecharset");
                    List<String> sets = np.getTokensRespectCase("=", ";");
                    setUseCharSets(sets);
                } else if (np.peekMatchIgnoreCase("unaligntransform")) // what we do to characters
                {
                    np.matchIgnoreCase("unaligntransform=");
                    unalignTransform = np.getWordRespectCase();
                    unalignTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Unaligned.NAME);
                } else if (np.peekMatchIgnoreCase("chartransform")) // what we do to characters
                {
                    np.matchIgnoreCase("chartransform=");
                    charTransform = np.getWordRespectCase();
                    charTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Characters.NAME);
                } else if (np.peekMatchIgnoreCase("disttransform")) // what we do to distances
                {
                    np.matchIgnoreCase("disttransform=");
                    distTransform = np.getWordRespectCase();
                    distTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Distances.NAME);
                } else if (np.peekMatchIgnoreCase("quarttransform")) // what we do to quartets
                {
                    np.matchIgnoreCase("quarttransform=");
                    quartetsTransform = np.getWordRespectCase();
                    quartetsTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Quartets.NAME);
                } else if (np.peekMatchIgnoreCase("treestransform")) // what we do to trees
                {
                    np.matchIgnoreCase("treestransform=");
                    treesTransform = np.getWordRespectCase();
                    treesTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Trees.NAME);
                } else if (np.peekMatchIgnoreCase("reticulateTransform")) { // what we do to reticulate
                    if (SplitsTreeProperties.ALLOW_RETICULATE) {

                        np.matchIgnoreCase("reticulateTransform=");
                        reticulateTransform = np.getWordRespectCase();
                        reticulateTransformParam = np.getTokensStringRespectCase(";");
                        updateFirstDirtyBlock(Reticulate.NAME);
                    }
                } else if (np.peekMatchIgnoreCase("splitstransform")) // what we do to splits
                {
                    np.matchIgnoreCase("splitstransform=");
                    splitsTransform = np.getWordRespectCase();
                    splitsTransformParam = np.getTokensStringRespectCase(";");
                    updateFirstDirtyBlock(Splits.NAME);
                } else if (np.peekMatchIgnoreCase("exclude")) {
                    List tokens = np.getTokensLowerCase("exclude", ";");
                    excludeGaps = np.findIgnoreCase(tokens, "no gaps", false, excludeGaps);
                    excludeGaps = np.findIgnoreCase(tokens, "gaps", true, excludeGaps);
                    excludeNonParsimony = np.findIgnoreCase(tokens, "no nonparsimony", false,
                            excludeNonParsimony);
                    excludeNonParsimony = np.findIgnoreCase(tokens, "nonparsimony", true,
                            excludeNonParsimony);

                    if (!np.findIgnoreCase(tokens, "no missing", false, true))
                        excludeMissing = 1.0 ; //Exclude no sites
                    excludeMissing = (double) np.findIgnoreCase(tokens,"missing=",(float)excludeMissing);
                    if (np.findIgnoreCase(tokens, "missing", true, false))
                        excludeMissing = 0.0; //Exclude all characters with missing data

                    if (!np.findIgnoreCase(tokens, "no constant", false, true))
                        excludeConstant = 0;
                    excludeConstant = (int) np.findIgnoreCase(tokens, "constant=", excludeConstant);
                    if (np.findIgnoreCase(tokens, "constant", true, false))
                        excludeConstant = -1;
                    excludeCodon1 = np.findIgnoreCase(tokens, "no codon1", false, excludeCodon1);
                    excludeCodon1 = np.findIgnoreCase(tokens, "codon1", true, excludeCodon1);
                    excludeCodon2 = np.findIgnoreCase(tokens, "no codon2", false, excludeCodon2);
                    excludeCodon2 = np.findIgnoreCase(tokens, "codon2", true, excludeCodon2);
                    excludeCodon3 = np.findIgnoreCase(tokens, "no codon3", false, excludeCodon3);
                    excludeCodon3 = np.findIgnoreCase(tokens, "codon3", true, excludeCodon3);



                    if (tokens.size() > 0)
                        throw new IOException("line " + np.lineno() + ": `"
                                + tokens.get(0) + "' unexpected in EXCLUDE");
                    updateFirstDirtyBlock(Characters.NAME);
                } else if (np.peekMatchIgnoreCase("layoutstrategy=recompute")) {
                    np.matchIgnoreCase("layoutstrategy=recompute;");
                    setLayoutStrategy(RECOMPUTE);
                } else if (np.peekMatchIgnoreCase("layoutstrategy=stabilize")) {
                    np.matchIgnoreCase("layoutstrategy=stabilize;");
                    setLayoutStrategy(STABILIZE);
                } else if (np.peekMatchIgnoreCase("layoutstrategy=snowball")) {
                    np.matchIgnoreCase("layoutstrategy=snowball;");
                    setLayoutStrategy(SNOWBALL);
                } else if (np.peekMatchIgnoreCase("layoutstrategy=keep")) {
                    np.matchIgnoreCase("layoutstrategy=keep;");
                    setLayoutStrategy(KEEP);
                } else if (np.peekMatchIgnoreCase("no autolayoutnodelabels")) {
                    np.matchIgnoreCase("no autolayoutnodelabels;");
                    setAutoLayoutNodeLabels(false);
                } else if (np.peekMatchIgnoreCase("autolayoutnodelabels")) {
                    np.matchIgnoreCase("autolayoutnodelabels;");
                    setAutoLayoutNodeLabels(true);
                    setRadiallyLayoutNodeLabels(false);
                } else if (np.peekMatchIgnoreCase("no radiallylayoutnodelabels")) {
                    np.matchIgnoreCase("no radiallylayoutnodelabels;");
                    setRadiallyLayoutNodeLabels(false);
                } else if (np.peekMatchIgnoreCase("radiallylayoutnodelabels")) {
                    np.matchIgnoreCase("radiallylayoutnodelabels;");
                    setRadiallyLayoutNodeLabels(true);
                    setAutoLayoutNodeLabels(false);
                } else
                    throw new IOException("line " + np.lineno() + ": unexpected: "
                            + np.getWordRespectCase());
            }
        } catch (IOException ex) {
            np.popPunctuationCharacters(); // restore punctation
            throw ex;
        }
        np.matchEndBlock();
    }

    /**
     * Write the assumptions block.   Suppress assumptions for undefined blocks.
     *
     * @param w   the writer
     * @param doc the document
     */
    public void write(Writer w, Document doc) throws IOException {
        write(w, doc, doc.getTaxa());
    }

    /**
     * writeInfoFile the assumptions block in full.  Show all assumptions, whether blocks are defined or not
     *
     * @param w
     * @param taxa
     * @throws IOException
     */
    public void write(Writer w, Taxa taxa) throws IOException {
        write(w, null, taxa);
    }

    private void write(Writer w, Document doc, Taxa taxa) throws IOException {
        w.write("\nBEGIN " + Assumptions.NAME + ";\n");
        if (isUptodate())
            w.write("uptodate;\n");
        if (exTaxa != null && exTaxa.cardinality() > 0) {
            w.write("extaxa=");
            for (int t = exTaxa.getBits().nextSetBit(1); t > 0; t = exTaxa.getBits().nextSetBit(t + 1)) {
                try {
                    if (taxa.getOriginalTaxa() != null) {
                        String name = taxa.getOriginalTaxa().getLabel(t);
                        w.write(" '" + name + "'");
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
            w.write(";\n");
        }
        /*
        if (useTaxaSets != null && useTaxaSets.size() > 0) {
            w.write("usetaxset=");
            Iterator it = useTaxaSets.iterator();
            while (it.hasNext()) {
                String label = (String) it.next();
                w.write(" '" + label + "'");
            }
            w.write(";\n");
        }
        */
        if (exChar != null && exChar.size() > 0) {
            w.write("exchar=");
            int first = 0, prev = 0;
            for (Integer c : exChar) {
                if (first == 0)
                    first = prev = c;
                else if (c == prev + 1)
                    prev = c;
                else // end of interval
                {
                    if (prev == first)
                        w.write(" " + first);
                    else
                        w.write(" " + first + "-" + prev);
                    first = prev = c;
                }
            }
            if (first > 0) {
                if (prev == first)
                    w.write(" " + first);
                else
                    w.write(" " + first + "-" + prev);
            }
            w.write(";\n");
        }
        if (useCharSets != null && useCharSets.size() > 0) {
            w.write("usecharset=");
            for (String label : useCharSets) {
                w.write(" '" + label + "'");
            }
            w.write(";\n");
        }
        if (exTrees != null && exTrees.size() > 0) {
            w.write("extrees=");
            for (String extree : exTrees) {
                w.write(" '" + extree + "'");
            }
            w.write(";\n");
        }
        if (exSplits != null && exSplits.size() > 0) {
            w.write("exsplits=");
            // TODO: make method for writing integer lists
            int first = 0, prev = 0;
            for (Integer s : exSplits) {
                if (first == 0)
                    first = prev = s;
                else if (s == prev + 1)
                    prev = s;
                else // end of interval
                {
                    if (prev == first)
                        w.write(" " + first);
                    else
                        w.write(" " + first + "-" + prev);
                    first = prev = s;
                }
            }
            if (first > 0) {
                if (prev == first)
                    w.write(" " + first);
                else
                    w.write(" " + first + "-" + prev);
            }
            w.write(";\n");
        }

        if (unalignTransform != null && (doc == null || doc.getUnaligned() != null)) {
            w.write("unaligntransform=" + unalignTransform);
            if (unalignTransformParam != null)
                w.write(" " + unalignTransformParam);
            w.write(";\n");
        }

        if (charTransform != null && (doc == null || doc.getCharacters() != null)) {
            w.write("chartransform=" + charTransform);
            if (charTransformParam != null)
                w.write(" " + charTransformParam);
            w.write(";\n");
        }
        if (distTransform != null && (doc == null || doc.getDistances() != null)) {
            w.write("disttransform=" + distTransform);
            if (distTransformParam != null)
                w.write(" " + distTransformParam);
            w.write(";\n");
        }
        if (quartetsTransform != null && (doc == null || doc.getQuartets() != null)) {
            w.write("quarttransform=" + quartetsTransform);
            if (quartetsTransformParam != null)
                w.write(" " + quartetsTransformParam);
            w.write(";\n");
        }
        if (treesTransform != null && (doc == null || doc.getTrees() != null)) {
            w.write("treestransform=" + treesTransform);
            if (treesTransformParam != null)
                w.write(" " + treesTransformParam);
            w.write(";\n");
        }
        if (splitsTransform != null && (doc == null || doc.getSplits() != null)) {
            w.write("splitstransform=" + splitsTransform);
            if (splitsTransformParam != null)
                w.write(" " + splitsTransformParam);
            w.write(";\n");
        }
        if (doc == null || doc.getSplits() != null)
            getSplitsPostProcess().write(w);

        {
            StringWriter sw = new StringWriter();
            if (excludeGaps)
                sw.write(" gaps");
            if (excludeMissing==0.0)
                sw.write(" missing");
            else if (excludeMissing==1.0)
                sw.write(" no missing");
            else
                sw.write(" missing=" + excludeMissing);
            if (excludeNonParsimony)
                sw.write(" nonparsimony");
            if (excludeConstant == -1)
                sw.write(" constant");
            else if (excludeConstant > 0)
                sw.write(" constant " + excludeConstant);     //TODO: Check. Should this be constant= ???
            if (excludeCodon1)
                sw.write(" codon1");
            if (excludeCodon2)
                sw.write(" codon2");
            if (excludeCodon3)
                sw.write(" codon3");
            if (sw.toString().length() > 0)
                w.write(" exclude " + sw.toString() + ";\n");
        }
        if (reticulateTransform != null && (doc == null || doc.getReticulate() != null)) {
            if (SplitsTreeProperties.ALLOW_RETICULATE) {
                w.write("reticulatetransform=" + reticulateTransform);
                if (reticulateTransformParam != null)
                    w.write(" " + reticulateTransformParam);
                w.write(";\n");
            }
        }
        if (getLayoutStrategy() == STABILIZE)
            w.write("layoutstrategy=stabilize;\n");
        else if (getLayoutStrategy() == SNOWBALL)
            w.write("layoutstrategy=snowball;\n");
        else if (getLayoutStrategy() == KEEP)
            w.write("layoutstrategy=keep;\n");

        if (getAutoLayoutNodeLabels())
            w.write("autolayoutnodelabels;\n");
        else if (getRadiallyLayoutNodeLabels())
            w.write("radiallylayoutnodelabels;\n");
        else
            w.write("no autolayoutnodelabels;\n");
        w.write("END; [" + Assumptions.NAME + "]\n");
    }


    /**
     * Produces a string representation of a NexusBlock object
     *
     * @return object in nexus format
     */
    public String toString(Document doc) {
        StringWriter sw = new StringWriter();
        try {
            write(sw, doc);
        } catch (java.io.IOException ex) {
            return "";
        }
        return sw.toString();
    }

    /**
     * Shows assumptions. Shows all assumptions, whether blocks are defined or not
     *
     * @return object in nexus format
     */
    public String toString(Taxa taxa) {
        StringWriter sw = new StringWriter();
        try {
            write(sw, taxa);
        } catch (java.io.IOException ex) {
            return "";
        }
        return sw.toString();
    }

    /**
     * Returns the current unaligned transformation
     *
     * @return an instance of the set unaligned transformation
     */
    public UnalignedTransform getUnalignedTransform() {
        String prefix = "splitstree.algorithms.unaligned.";
        UnalignedTransform trans;
        Class theClass;
        try {
            if (!unalignTransform.contains(".")) {
                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(unalignTransform)) {
                    theClass = Class.forName(unalignTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(unalignTransform));
                } else {
                    theClass = Class.forName(prefix + unalignTransform);
                }
            } else
                theClass = Class.forName(unalignTransform);
            trans = (UnalignedTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + unalignTransform);
            unalignTransform = "Noalign";
            unalignTransformParam = "";
            trans = new Noalign();
        }

        // set the parameters:
        try {
            Configurator.setOptions
                    (trans, getUnalignedTransformParam());
        } catch (Exception ex) {
            System.err.println(getUnalignedTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: " +UnalignedTransformConfigurator.getUsage(trans)+")");

        return trans;
    }


    /**
     * Returns the current characters transformation
     *
     * @return an instance of the set characters transformation
     */
    public CharactersTransform getCharactersTransform() {
        String prefix = "splitstree.algorithms.characters.";
        CharactersTransform trans;
        Class theClass;
        try {
            if (!charTransform.contains(".")) {
                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(charTransform)) {
                    theClass = Class.forName(charTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(charTransform));
                } else {
                    theClass = Class.forName(prefix + charTransform);
                }
            } else
                theClass = Class.forName(charTransform);
            trans = (CharactersTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + charTransform);
            charTransform = Basic.getShortName(Uncorrected_P.class);
            charTransformParam = "";
            trans = new Uncorrected_P();
        }

        // set the parameters:
        try {
            Configurator.setOptions(trans, getCharactersTransformParam());
        } catch (Exception ex) {
            System.err.println(getCharactersTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: " +CharactersTransformConfigurator.getUsage(trans)+")");

        return trans;
    }

    /**
     * Returns the current distances transformation
     *
     * @return an instance of the set distances transformation
     */
    public DistancesTransform getDistancesTransform() {


        String prefix = "splitstree.algorithms.distances.";
        DistancesTransform trans;
        Class theClass;
        try {
            if (!distTransform.contains(".")) {
                // is it a plugin?

                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(distTransform)) {
                    theClass = Class.forName(distTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(distTransform));
                } else {
                    theClass = Class.forName(prefix + distTransform);
                }
            } else
                theClass = Class.forName(distTransform);
            trans = (DistancesTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + distTransform);
            distTransform = "NeighborNet";
            distTransformParam = "";
            trans = new NeighborNet();
        }

        try {
            Configurator.setOptions
                    (trans, getDistancesTransformParam());
        } catch (Exception ex) {
            System.err.println(getDistancesTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: " +DistancesTransformConfigurator.getUsage(trans)+")");
        return trans;
    }

    /**
     * Returns the current quartets transformation
     *
     * @return an instance of the set quartets transformation
     */
    public QuartetsTransform getQuartetsTransform() {
        String prefix = "splitstree.algorithms.quartets.";
        QuartetsTransform trans;
        Class theClass;
        try {
            if (!quartetsTransform.contains(".")) {
                // is it a plugin?

                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(quartetsTransform)) {
                    theClass = Class.forName(quartetsTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(quartetsTransform));
                } else {
                    theClass = Class.forName(prefix + quartetsTransform);
                }
            } else
                theClass = Class.forName(quartetsTransform);
            trans = (QuartetsTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + quartetsTransform);
            quartetsTransform = "Coalescent";
            quartetsTransformParam = "";
            trans = new Coalescent();
        }

        // set the parameters:
        try {
            Configurator.setOptions
                    (trans, getQuartetsTransformParam());
        } catch (Exception ex) {
            System.err.println(getQuartetsTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: "+QuartetsTransformConfigurator.getUsage(trans)+")");
        return trans;
    }

    /**
     * Returns the current splits transformation
     *
     * @return an instance of the set splits transformation
     */
    public SplitsTransform getSplitsTransform() {
        String prefix = "splitstree.algorithms.splitstree.";
        SplitsTransform trans;
        Class theClass;
        try {
            if (!splitsTransform.contains(".")) {
                // is it a plugin?

                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(splitsTransform)) {
                    theClass = Class.forName(splitsTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(splitsTransform));
                } else {
                    theClass = Class.forName(prefix + splitsTransform);
                }
            } else
                theClass = Class.forName(splitsTransform);
            trans = (SplitsTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + splitsTransform);
            splitsTransform = Basic.getShortName(EqualAngle.class);
            splitsTransformParam = "";
            trans = new EqualAngle();
        }

        // set the parameters:
        try {
            Configurator.setOptions
                    (trans, getSplitsTransformParam());
        } catch (Exception ex) {
            System.err.println(getSplitsTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: "+SplitsTransformConfigurator.getUsage(trans)+")");
        return trans;
    }

    /**
     * Returns the current trees transformation
     *
     * @return an instance of the set trees transformation
     */
    public TreesTransform getTreesTransform() {
        String prefix = "splitstree.algorithms.trees.";
        TreesTransform trans;
        Class theClass;
        try {
            if (!treesTransform.contains(".")) {
                // is it a plugin?

                if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(treesTransform)) {
                    theClass = Class.forName(treesTransform, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(treesTransform));
                } else {
                    theClass = Class.forName(prefix + treesTransform);
                }
            } else
                theClass = Class.forName(treesTransform);
            trans = (TreesTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + treesTransform);
            treesTransform = "TreeSelector";
            treesTransformParam = "";
            trans = new TreeSelector();
        }

// set the parameters:
        try {
            Configurator.setOptions
                    (trans, getTreesTransformParam());
        } catch (Exception ex) {
            System.err.println(getTreesTransformName() + ": " + ex);
        }

// System.err.println("(Usage: "+SplitsTransformConfigurator.getUsage(trans)+")");
        return trans;
    }

    /**
     * Returns the current reticulate transformation
     *
     * @return an instance of the set reticulate transformation
     */
    public ReticulateTransform getReticulateTransform() {
        String prefix = "splitstree.algorithms.reticulate.";
        ReticulateTransform trans;
        Class theClass;
        try {
            if (!reticulateTransform.contains("."))
                theClass = Class.forName(prefix + reticulateTransform);
            else
                theClass = Class.forName(reticulateTransform);
            trans = (ReticulateTransform) (theClass.newInstance());
        } catch (Exception ex) {
            System.err.println("Class not found: " + reticulateTransform);
            reticulateTransform = Basic.getShortName(ReticulateEqualAngle.class);
            reticulateTransformParam = "";
            trans = new ReticulateEqualAngle();
        }

        // set the parameters:
        try {
            Configurator.setOptions
                    (trans, getReticulateTransformParam());
        } catch (Exception ex) {
            System.err.println(getReticulateTransformName() + ": " + ex);
        }

        // System.err.println("(Usage: "+SplitsTransformConfigurator.getUsage(trans)+")");
        return trans;
    }

    /**
     * Show the usage of this block
     *
     * @param ps the PrintStream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN ST_ASSUMPTIONS;");
        ps.println("\t[UNALIGNTRANSFORM=name [parameters];]");
        ps.println("\t[CHARTRANSFORM=name [parameters];]");
        ps.println("\t[DISTTRANSFORM=name [parameters];]");
        ps.println("\t[SPLITSTRANSFORM=name [parameters];]");
        ps.println("\t[SPLITSPOSTPROCESS");
        ps.println("\t\t[[NO] LEASTSQUARES]");
        ps.println("\t\t[FILTER={CLOSESTTREE|GREEDYCOMPATIBLE|WEAKLYCOMPATIBLE|WEIGHT VALUE=value|CONFIDENCE VALUE=value|DIMENSION VALUE=value|NONE};]");
        ps.println("\t[EXTAXA={NONE|list-of-original-taxa-labels};]");
        // ps.println("\t[USETAXSET={ALL|list-of-taxset-labels};]");
        ps.println("\t[EXCHAR={NONE|list-of-original-char-positions};]");
        ps.println("\t[EXSPLITS={NONE|list-of-original-split-ids};]");
        ps.println("\t[EXCLUDE [[NO] GAPS] [[NO] NONPARSIMONY]");
        ps.println("\t\t[{NO CONSTANT|CONSTANT [number]}]");
        ps.println("\t\t[[NO] CODON1] [[NO] CODON2] [[NO] CODON3];]");
        ps.println("\t[USECHARSET={ALL|list-of-charset-labels};]");
        ps.println("\t[EXTREES={NONE|list-of-original-tree-labels};]");
        ps.println("\t[LAYOUTSTRATEGY={STABILIZE|SNOWBALL|KEEP};]");
        ps.println("\t[[NO] AUTOLAYOUTNODELABELS;]");
        ps.println("\t[[NO] RADIALLYLAYOUTNODELABELS;]");
        ps.println("\t[UPTODATE;]");
        ps.println("END;");
    }

    /**
     * clear the list of dirty blocks
     */
    public void clearFirstDirtyBlock() {
        firstDirtyBlock = "";
    }

    /**
     * update the first dirty block with the given name
     *
     * @param name the name of the dirty block
     */
    public void updateFirstDirtyBlock(String name) {
        if (name.equals(Taxa.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Taxa.NAME)) return;
        if (name.equals(Unaligned.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Unaligned.NAME)) return;
        if (name.equals(Characters.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Characters.NAME)) return;
        if (name.equals(Distances.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Distances.NAME)) return;
        if (name.equals(Quartets.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Quartets.NAME)) return;
        if (name.equals(Trees.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Trees.NAME)) return;
        if (SplitsTreeProperties.ALLOW_RETICULATE)
            if (name.equals(Reticulate.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Reticulate.NAME)) return;
        if (name.equals(Splits.NAME)) firstDirtyBlock = name;
        if (firstDirtyBlock.equals(Splits.NAME)) return;
        if (name.equals(Network.NAME)) firstDirtyBlock = name;
        // if (firstDirtyBlock.equals(Network.NAME)) return;
    }

    /**
     * returns the first nexus blocks that were made dirty in the last call
     * to read
     *
     * @return the list of dirty block names
     */
    public String getFirstDirtyBlock() {
        return firstDirtyBlock;
    }

    /**
     * returns the splits post process subclass
     *
     * @return splits post process
     */
    public SplitsPostProcess getSplitsPostProcess() {
        return splitsPostProcess;
    }

    /**
     * clone the assumptions block
     *
     * @param taxa
     * @return a clone
     */
    public Assumptions clone(Taxa taxa) {
        Assumptions assumptions = new Assumptions();
        StringWriter w = new StringWriter();
        try {
            write(w, taxa);
            assumptions.read(new NexusStreamParser(new StringReader(w.toString())), taxa);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return assumptions;

    }

    /**
     * determines whether this is a currently set transform
     *
     * @param trans
     * @return true if currently set
     */
    public boolean isSetTransform(Transformation trans) {
        // TODO: check that transform is also of the correct type
        String tName = Basic.getShortName(trans.getClass());
        if (unalignTransform != null && unalignTransform.equals(tName)
                || charTransform != null && charTransform.equals(tName)
                || distTransform != null && distTransform.equals(tName)
                || quartetsTransform != null && quartetsTransform.equals(tName)
                || splitsTransform != null && splitsTransform.equals(tName)
                || treesTransform != null && treesTransform.equals(tName)
                || splitsTransform != null && splitsTransform.equals(tName)
                || reticulateTransform != null && reticulateTransform.equals(tName))
            return true;
        else
            return false;
    }

    public class SplitsPostProcess {
        boolean leastSquares;
        String filter;
        int dimensionValue;
        float weightThresholdValue;
        float confidenceThresholdValue;

        SplitsPostProcess() {
            leastSquares = false;
            filter = "dimension";
            weightThresholdValue = 0;
            confidenceThresholdValue = 0;
            dimensionValue = 4;
        }

        void write(Writer w) throws IOException {
            w.write("SplitsPostProcess");
            if (leastSquares)
                w.write(" leastSquares");
            w.write(" filter=" + filter);
            if (filter.equalsIgnoreCase("weight"))
                w.write(" value=" + weightThresholdValue);
            if (filter.equalsIgnoreCase("confidence"))
                w.write(" value=" + confidenceThresholdValue);
            if (filter.equalsIgnoreCase("dimension"))
                w.write(" value=" + dimensionValue);
            w.write(";\n");
        }

        void read(NexusStreamParser np) throws IOException {

            if (np.peekMatchIgnoreCase("SplitsPostProcess")) {
                List<String> tokens = np.getTokensLowerCase("SplitsPostProcess", ";");

                leastSquares = false;
                np.findIgnoreCase(tokens, "no leastquares");
                leastSquares = np.findIgnoreCase(tokens, "leastsquares");
                np.findIgnoreCase(tokens, "no leastsquaresagain");
                filter = np.findIgnoreCase(tokens, "filter=", "closesttree greedycompatible greedyWC weight confidence dimension none", "none");
                if (filter.equalsIgnoreCase("weight"))
                    weightThresholdValue = (float) np.findIgnoreCase(tokens, "value=", 0, 1000000.0, weightThresholdValue);
                if (filter.equalsIgnoreCase("confidence"))
                    confidenceThresholdValue = (float) np.findIgnoreCase(tokens, "value=", 0, 1000000.0, confidenceThresholdValue);
                if (filter.equalsIgnoreCase("dimension"))
                    dimensionValue = (int) np.findIgnoreCase(tokens, "value=", 0, 1000000.0, dimensionValue);

                if (tokens.size() != 0)
                    throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in SplitsPostProcess");
            }
        }

        public boolean isLeastSquares() {
            return leastSquares;
        }

        public void setLeastSquares(boolean leastSquares) {
            this.leastSquares = leastSquares;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public float getWeightThresholdValue() {
            return weightThresholdValue;
        }

        public void setWeightThresholdValue(float threshold) {
            this.weightThresholdValue = threshold;
        }

        public float getConfidenceThresholdValue() {
            return confidenceThresholdValue;
        }

        public void setConfidenceThresholdValue(float threshold) {
            this.confidenceThresholdValue = threshold;
        }

        public boolean getGreedyCompatible() {
            return filter.equalsIgnoreCase("greedycompatible");
        }

        public boolean getClosestTree() {
            return filter.equalsIgnoreCase("closesttree");
        }

        public boolean getGreedyWC() {
            return filter.equalsIgnoreCase("greedyWC");
        }

        public boolean getWeightThreshold() {
            return filter.equalsIgnoreCase("weight");
        }

        public boolean getConfidenceThreshold() {
            return filter.equalsIgnoreCase("confidence");
        }

        public int getDimensionValue() {
            return dimensionValue;
        }

        public void setDimensionValue(int dimensionValue) {
            this.dimensionValue = dimensionValue;
        }

        public boolean getDimensionFilter() {
            return filter.equalsIgnoreCase("dimension");
        }
    }
}

//EOF
