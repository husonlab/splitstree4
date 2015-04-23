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
 * $Id: DocumentData.java,v 1.28 2010-04-22 09:49:18 bryant Exp $
*/
package splitstree.core;


import jloda.util.Basic;
import splitstree.nexus.*;

import java.io.File;

/**
 * The main splitstree document data
 */
public class DocumentData {
    // general stuff
    private String title = "untitled";
    private File file = null;
    private int filelength; //Number of lines in the file.
    private boolean dirty = false;

    // nexus blocks
    protected Assumptions assumptions = new Assumptions();
    protected Taxa taxa = null;
    protected Unaligned unaligned = null;
    protected Characters characters = null;
    protected Distances distances = null;
    protected Quartets quartets = null;
    protected Splits splits = null;
    protected Trees trees = null;
    protected Network network = null;
    protected Bootstrap bootstrap = null;
    protected Analysis analysis = new Analysis();
    protected Traits traits = null;
    protected Sets sets = null;
    protected Reticulate reticulate = null;

    /**
     * Constructs an empty Document   data object
     */
    public DocumentData() {
    }

    /**
     * Sets the taxa
     *
     * @param taxa the taxa block to be used
     */

    public void setTaxa(Taxa taxa) {
        this.taxa = taxa;
    }

    /**
     * Gets the taxa
     *
     * @return the taxa block
     */

    public Taxa getTaxa() {
        return this.taxa;
    }

    /**
     * Sets the assumptions
     *
     * @param assumptions the assumptions block to be used
     */

    public void setAssumptions(Assumptions assumptions) {
        this.assumptions = assumptions;
    }

    /**
     * Gets the assumptions
     *
     * @return the assumptions block
     */

    public Assumptions getAssumptions() {
        return this.assumptions;
    }

    /**
     * Sets the unaligned block
     *
     * @param unaligned the unaligned block to be used
     */

    public void setUnaligned(Unaligned unaligned) {
        this.unaligned = unaligned;
    }

    /**
     * Gets the unaligned block
     *
     * @return the unaligned block
     */

    public Unaligned getUnaligned() {
        return this.unaligned;
    }

    /**
     * Sets the characters
     *
     * @param characters the characters block to be used
     */

    public void setCharacters(Characters characters) {
        this.characters = characters;
    }

    /**
     * Gets the characters
     *
     * @return the characters block
     */

    public Characters getCharacters() {
        return this.characters;
    }

    /**
     * Gets the distances
     *
     * @return the distances block
     */

    public Distances getDistances() {
        return this.distances;
    }

    /**
     * Sets the distances
     *
     * @param distances the distances block to be used
     */

    public void setDistances(Distances distances) {
        this.distances = distances;
    }

    /**
     * Sets the quartets
     *
     * @param quartets the quartets block to be used
     */

    public void setQuartets(Quartets quartets) {
        this.quartets = quartets;
    }


    /**
     * Sets the splits
     *
     * @param splits the splits block to be used
     */

    public void setSplits(Splits splits) {
        this.splits = splits;
    }

    /**
     * Sets the Sets block
     *
     * @param sets
     */
    public void setSets(Sets sets) {
        this.sets = sets;
    }

    /**
     * sets the traits block
     *
     * @param traits New traits block
     */
    public void setTraits(Traits traits) {
        this.traits = traits;
    }

    /**
     * sets the trees block
     *
     * @param trees
     */
    public void setTrees(Trees trees) {
        this.trees = trees;
    }

    /**
     * Gets the quartets
     *
     * @return the quartets block
     */

    public Quartets getQuartets() {
        return this.quartets;
    }

    /**
     * Gets the splits
     *
     * @return the splits block
     */

    public Splits getSplits() {
        return this.splits;
    }


    public Sets getSets() {
        return this.sets;
    }

    /**
     * gets the trees block
     *
     * @return trees
     */
    public Trees getTrees() {
        return trees;
    }

    /**
     * Sets the splits graph
     *
     * @param network the network block to be used
     */

    public void setNetwork(Network network) {
        this.network = network;
    }

    /**
     * Gets the splits graph
     *
     * @return the splits graph block
     */

    public Network getNetwork() {
        return this.network;
    }

    /**
     * get the reticulate block
     *
     * @return
     */
    public Reticulate getReticulate() {
        return reticulate;
    }

    /**
     * set the reticulate block
     *
     * @param reticulate
     */
    public void setReticulate(Reticulate reticulate) {
        this.reticulate = reticulate;
    }

    /**
     * Sets the bootstrap block
     *
     * @param bootstrap the bootstrap block
     */

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * Gets the bootstrap block
     *
     * @return the bootstrap block
     */

    public Bootstrap getBootstrap() {
        return this.bootstrap;
    }

    /**
     * Sets the analysis block
     *
     * @param analysis the new analysis  block
     */
    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    /**
     * Gets the analysis block
     *
     * @return the analysis block
     */
    public Analysis getAnalysis() {
        return analysis;
    }

    /**
     * Gets the attributes block
     *
     * @return the attributes block
     */
    public Traits getTraits() {
        return traits;
    }

    /**
     * is this block valid?
     *
     * @param block
     * @return true, if valid
     */
    public boolean isValid(NexusBlock block) {
        return block != null && block.isValid();
    }

    /**
     * Given name of a block, returns associated block. Does  not check if
     * block is valid or not.
     *
     * @param name
     * @return NexusBlock block with that name, or null if there is none.
     */
    public NexusBlock getBlockByName(String name) {
        if (Assumptions.NAME.equals(name))
            return assumptions;
        if (Taxa.NAME.equals(name))
            return taxa;
        if (Unaligned.NAME.equals(name))
            return unaligned;
        if (Characters.NAME.equals(name))
            return characters;
        if (Distances.NAME.equals(name))
            return distances;
        if (Sets.NAME.equals(name))
            return sets;
        if (Quartets.NAME.equals(name))
            return quartets;
        if (Splits.NAME.equals(name))
            return splits;
        if (Trees.NAME.equals(name))
            return trees;
        if (Network.NAME.equals(name))
            return network;
        if (Bootstrap.NAME.equals(name))
            return bootstrap;
        if (Analysis.NAME.equals(name))
            return analysis;
        if (Reticulate.NAME.equals(name))
            return reticulate;
        if (Traits.NAME.equals(name))
            return traits;
        return null;
    }

    /**
     * returns true, if named block is valid, false, else
     *
     * @param name
     * @return block is valid
     */
    public boolean isValidByName(String name) {
        return isValid(getBlockByName(name));
    }


    /**
     * returns the name of the top valid data block
     *
     * @return top valid block or null
     */
    public String getNameTop() {
        if (isValid(unaligned))
            return Unaligned.NAME;
        if (isValid(characters))
            return Characters.NAME;
        if (isValid(distances))
            return Distances.NAME;
        if (isValid(trees))
            return Trees.NAME;
        if (isValid(quartets))
            return Quartets.NAME;
        if (isValid(splits))
            return Splits.NAME;
        if (isValid(reticulate))
            return Reticulate.NAME;
        // sets can't be top, only data blocks
        //  if (isValid(sets))
        //  		return Sets.NAME;
        if (isValid(network))
            return Network.NAME;
        return null;
    }

    /**
     * get the current document title
     *
     * @return String, title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * set the current document title
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * gets the file associated with this document
     *
     * @return file
     */
    public File getFile() {
        return file;
    }

    /**
     * sets the file and title associated with this document
     *
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
        if (file != null)
            setTitle(file.getName());
        else
            setFile("untitled", true);
    }


    /**
     * sets the file to the named one, modifying the name to make it unique, if desired
     *
     * @param name
     * @param unique
     */
    public void setFile(String name, boolean unique) {
        if (unique) {
            file = Basic.getFileWithNewUniqueName(name);
        } else {
            file = new File(name);
        }
        setTitle(file.getName());
    }

    /**
     * adds the given suffix to the given file (if it is not there already)
     *
     * @param orig
     * @param suffix
     */
    public void setFile(File orig, String suffix) {
        String filename = orig.getAbsolutePath();
        if (filename.endsWith(suffix))
            setFile(filename, true);
        else
            setFile(filename + suffix, true);
    }

    /**
     * Returns number of lines in this.file
     *
     * @return int, number of lines
     */
    public int getNumberLines() {
        return filelength;
    }

    public void setNumberLines(int n) {
        this.filelength = n;
    }

    /**
     * has this document beeen modified?
     *
     * @return true, if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * has this document been modified?
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}

// EOF
