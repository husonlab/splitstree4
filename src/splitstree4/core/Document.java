/**
 * Document.java
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
/*
 * $Id: Document.java,v 1.201 2010-06-15 09:17:07 huson Exp $
*/

package splitstree4.core;

import jloda.export.*;
import jloda.phylo.PhyloGraphView;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;
import splitstree4.algorithms.Transformation;
import splitstree4.algorithms.additional.ClosestTree;
import splitstree4.algorithms.additional.GreedyCompatible;
import splitstree4.algorithms.additional.GreedyWC;
import splitstree4.algorithms.additional.LeastSquaresWeights;
import splitstree4.algorithms.characters.*;
import splitstree4.algorithms.distances.*;
import splitstree4.algorithms.quartets.Quartets2Splits;
import splitstree4.algorithms.quartets.QuartetsTransform;
import splitstree4.algorithms.reticulate.Reticulate2Network;
import splitstree4.algorithms.reticulate.ReticulateTransform;
import splitstree4.algorithms.splits.Splits2Network;
import splitstree4.algorithms.splits.SplitsTransform;
import splitstree4.algorithms.trees.Trees2Network;
import splitstree4.algorithms.trees.Trees2Splits;
import splitstree4.algorithms.trees.TreesTransform;
import splitstree4.algorithms.unaligned.*;
import splitstree4.algorithms.util.Configurator;
import splitstree4.algorithms.util.MultiGeneAnalysis;
import splitstree4.algorithms.util.Transforms;
import splitstree4.algorithms.util.simulate.SimulationExperiments;
import splitstree4.analysis.bootstrap.TestTreeness;
import splitstree4.analysis.characters.CaptureRecapture;
import splitstree4.analysis.characters.PhiTest;
import splitstree4.externalIO.exports.ExportManager;
import splitstree4.externalIO.exports.ExporterInfo;
import splitstree4.externalIO.imports.ImportManager;
import splitstree4.gui.Director;
import splitstree4.main.SplitsTreeProperties;
import splitstree4.nexus.*;
import splitstree4.util.*;
import splitstree4.util.NexusFileFilter;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

//TODO: keepSplits should perhaps be incorporated better. ALSO, we could consider fixing trees as well

/**
 * The main splitstree document
 */
public class Document extends DocumentData {
    private boolean inBootstrap = false;
    private boolean keepSplits = false;
    private boolean valid = true;
    private Component parent = null;
    private String topComments = null; // the first comment in a nexus file is kept

    private ProgressListener progressListener = new ProgressCmdLine(); // for efficienty, allow only one


    // this is used to buffer node modifications between recomputes
    final public Map<String, VertexDescription> taxon2VertexDescription = new HashMap<>();

    /**
     * Clears the document
     */
    public void clear() {
        deleteDependentBlocks(null);
        taxon2VertexDescription.clear();
    }

    /**
     * Deletes all blocks depending on the given one
     *
     * @param name the name of the block, or null to do all blocks
     */
    void deleteDependentBlocks(String name) {
        boolean found = false;

        if (name == null) // null: delete all
            found = true;

        if (found && taxa != null)
            taxa = null;

        if (found && assumptions != null)
            assumptions = null;


        if (!found && name.equals(Taxa.NAME))
            found = true;

        if (found && unaligned != null)
            unaligned = null;

        if (found && traits != null)
            traits = null;

        if (!found && name.equals(Unaligned.NAME))
            found = true;

        if (found && characters != null)
            characters = null;

        if (!found && name.equals(Characters.NAME))
            found = true;

        if (found && distances != null)
            distances = null;

        if (!found && name.equals(Distances.NAME))
            found = true;

        if (found && quartets != null)
            quartets = null;

        if (!found && name.equals(Quartets.NAME))
            found = true;

        if (found && splits != null)
            splits = null;

        if (!found && name.equals(Splits.NAME))
            found = true;

        if (found && trees != null)
            trees = null;

        if (!found && name.equals(Trees.NAME))
            found = true;

        if (found && reticulate != null)
            reticulate = null;

        if (!found && name.equals(Reticulate.NAME))
            found = true;

        if (found && network != null)
            network = null;

        if (found && bootstrap != null)
            bootstrap = null;


    }

    /**
     * Update the whole document.
     */
    public void update() throws Exception {
        System.err.println("--------- Start update ----------");

        if (assumptions != null && assumptions.isUptodate()) // have just read file and its uptodate
        {
            System.err.println("Nothing to be done, apparently up-to-date");
            assumptions.setUptodate(false);
        } else {
            if (assumptions == null)
                assumptions = new Assumptions();
            update(null);
        }
        System.err.println("--------- End update ----------");
    }


    /**
     * update all date that depends on the named block
     *
     * @param name
     * @throws jloda.util.CanceledException
     * @throws SplitsException
     */
    public void update(String name) throws Exception {
        boolean found = false;  // found block to begin updating at?
        setValid(true); // if update fails, will be set to false

        notifyEnabled(true);

        // TODO: only set name to top block if set of hidden taxa has changed!
        //TODO: FIgure out what the above todo means
        if ((taxa != null) && (assumptions != null)
                && (assumptions.getExTaxa() != null) &&
                ((assumptions.getExTaxa().cardinality() > 0)
                        || (taxa.getNtax() < taxa.getOriginalTaxa().getNtax()))
                && taxaHaveJustChanged(taxa, assumptions)) {


            taxa.hideTaxa(assumptions.getExTaxa());
            name = getNameTop();  // need to update from top valid block
            // below, we will hide taxa in top block
        }

        if (!inBootstrap) {
            notifySetMaximumProgress(100);
        }

        try {
            // if name==null, update all:
            if (name == null)
                found = true;

            // update everything depending on taxa
            if (!found && name.equalsIgnoreCase(Taxa.NAME))
                found = true;
            if (taxa == null)
                return;

            //check to see if taxa are hidden in the Traits block
            if (traits != null) {
                if (taxa.getOriginalTaxa() != null)
                    traits.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());
                else
                    traits.hideTaxa(taxa, assumptions.getExTaxa());
            }

            //check to see if some taxa have been restored, so need to update traits.


            // update all depending on unaligned
            if (!found && name.equalsIgnoreCase(Unaligned.NAME)) {
                found = true;
                if (unaligned == null)
                    throw new SplitsException("Unaligned failed: null block");
                if (taxa.getOriginalTaxa() != null && assumptions.getExTaxa() != null && isFirstValidInputBlock(Unaligned.NAME)) {
                    unaligned.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());
                }
                // post modify here
            }

            if (found && unaligned != null) {
                if (analysis != null)
                    analysis.apply(this, taxa, Unaligned.NAME);
                UnalignedTransform trans = assumptions.getUnalignedTransform();
                if (!inBootstrap) {
                    notifyTasks("Unaligned", assumptions.getUnalignedTransformName());
                    notifySetProgress(-1);
                }
                characters = null;
                distances = null;
                quartets = null;
                if (!this.fixSplits())
                    splits = null;
                trees = null;
                network = null;

                if (!trans.isApplicable(this, taxa, unaligned)) {
                    throw new SplitsException
                            ("Unaligned: transform not applicable: " + assumptions.getUnalignedTransformName());
                }

                if (trans instanceof Unaligned2Characters) {
                    Unaligned2Characters unalign2char = (Unaligned2Characters) trans;

                    System.err.println("Computing UNALIGNED to CHARACTERS: " +
                            Configurator.getOptions(unalign2char));
                    try {
                        characters = unalign2char.apply(this, taxa, unaligned);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Unaligned.NAME);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Unaligned2Distances) {
                    Unaligned2Distances unalign2dist = (Unaligned2Distances) trans;

                    System.err.println("Computing UNALIGNED to DISTANCES: " +
                            Configurator.getOptions(unalign2dist));
                    try {
                        distances = unalign2dist.apply(this, taxa, unaligned);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Unaligned.NAME);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Unaligned2Quartets) {
                    Unaligned2Quartets unalign2quart = (Unaligned2Quartets) trans;

                    System.err.println("Computing UNALIGNED to QUARTETS: " +
                            Configurator.getOptions(unalign2quart));
                    try {
                        quartets = unalign2quart.apply(this, taxa, unaligned);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Unaligned2Splits) {
                    Unaligned2Splits unalign2splits = (Unaligned2Splits) trans;
                    System.err.println("Computing UNALIGNED to SPLITS: " +
                            Configurator.getOptions(unalign2splits));
                    try {
                        if (!fixSplits())
                            splits = unalign2splits.apply(this, taxa, unaligned);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Unaligned2Tree) {
                    Unaligned2Tree unalign2tree = (Unaligned2Tree) trans;
                    System.err.println("Computing UNALIGNED to TREES: " +
                            Configurator.getOptions(unalign2tree));
                    try {
                        trees = unalign2tree.apply(this, taxa, unaligned);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else
                    throw new SplitsException("update failed for UNALIGNED");
            }

            // update all depending on characters
            if (!found && name.equalsIgnoreCase(Characters.NAME)) {
                found = true;
                if (characters == null)
                    throw new SplitsException("Characters failed: null block");
                if (taxa.getOriginalTaxa() != null && assumptions.getExTaxa() != null && isFirstValidInputBlock(Characters.NAME))
                    characters.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());

            }
            // post modify characters here:
            if (found && characters != null)
                CharactersUtilities.maskCharacters(assumptions, sets, characters);

            // do update of dependent data
            if (found && characters != null) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Characters", "analysis");
                    analysis.apply(this, taxa, Characters.NAME);
                }

                if (assumptions.getCharactersTransformName() == null)
                    assumptions.setCharactersTransformName("Uncorrected_P");

                CharactersTransform trans = assumptions.getCharactersTransform();
                if (!inBootstrap) {
                    notifyTasks("Characters", assumptions.getCharactersTransformName());
                    notifySetProgress(-1);
                }
                distances = null;
                quartets = null;

                if (!fixSplits())
                    splits = null;
                trees = null;

                if (!trans.isApplicable(this, taxa, characters)) {
                    throw new SplitsException
                            ("Characters: transform not applicable: " + assumptions.getCharactersTransformName());
                }

                if (trans instanceof Characters2Distances) {
                    Characters2Distances char2dist = (Characters2Distances) trans;

                    System.err.println("Computing CHARACTERS to DISTANCES: " +
                            Configurator.getOptions(char2dist));
                    try {
                        distances = char2dist.apply(this, taxa, characters);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Characters.NAME);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Characters2Quartets) {
                    Characters2Quartets char2quart = (Characters2Quartets) trans;

                    System.err.println("Computing CHARACTERS to QUARTETS: " +
                            Configurator.getOptions(char2quart));
                    try {
                        quartets = char2quart.apply(this, taxa, characters);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Characters2Splits) {
                    Characters2Splits char2splits = (Characters2Splits) trans;
                    System.err.println("Computing CHARACTERS to SPLITS: " +
                            Configurator.getOptions(char2splits));
                    try {
                        if (!fixSplits())
                            splits = char2splits.apply(this, taxa, characters);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Characters.NAME);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                        throw new SplitsException("Algorithm failed: " + ex);
                    }
                } else if (trans instanceof Characters2Trees) {
                    Characters2Trees char2tree = (Characters2Trees) trans;
                    System.err.println("Computing CHARACTERS to TREES: " +
                            Configurator.getOptions(char2tree));
                    trees = char2tree.apply(this, taxa, characters);
                    System.err.println("done");
                    if (assumptions.getTreesTransformName() == null) {
                        assumptions.setTreesTransformName("TreeSelector");
                    }
                } else if (trans instanceof Characters2Network) {
                    Characters2Network chars2network = (Characters2Network) trans;
                    System.err.println("Computing CHARACTERS to NETWORK: " +
                            Configurator.getOptions(chars2network));
                    try {
                        network = chars2network.apply(this, taxa, characters);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Characters.NAME);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                } else
                    throw new SplitsException("update failed for CHARACTERS");

            }

            // update all depending on distances
            if (!found && name.equalsIgnoreCase(Distances.NAME)) {
                found = true;
                if (distances == null)
                    throw new SplitsException("Distances failed: null block");
                if (taxa.getOriginalTaxa() != null && assumptions.getExTaxa() != null && isFirstValidInputBlock(Distances.NAME))
                    distances.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());
                // post modify distances here
            }

            if (found && distances != null) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Distances", "analysis");
                    analysis.apply(this, taxa, Distances.NAME);
                }
                DistancesTransform trans = assumptions.getDistancesTransform();
                if (!inBootstrap) {
                    notifyTasks("Distances", assumptions.getDistancesTransformName());
                    notifySetProgress(-1);
                }
                quartets = null;
                if (!fixSplits())
                    splits = null;
                trees = null;
                network = null;

                if (!trans.isApplicable(this, taxa, distances)) {
                    throw new SplitsException
                            ("Distances: transform not applicable: " + assumptions.getDistancesTransformName());
                }

                if (trans instanceof Distances2Quartets) {
                    Distances2Quartets dist2quart = (Distances2Quartets) trans;
                    System.err.println("Computing DISTANCES to QUARTETS: " +
                            Configurator.getOptions(dist2quart));
                    try {
                        quartets = dist2quart.apply(this, taxa, distances);
                    } catch (Exception ex) {
                        throw new SplitsException("Algorithm failed: " + ex.getMessage());
                    }
                } else if (trans instanceof Distances2Splits) {
                    Distances2Splits dist2splits = (Distances2Splits) trans;
                    System.err.println("Computing DISTANCES to SPLITS: " +
                            Configurator.getOptions(dist2splits));
                    try {
                        if (!fixSplits())
                            splits = dist2splits.apply(this, taxa, distances);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Distances.NAME);
                    } catch (Exception ex) {
                        ex.printStackTrace(); //TODO: FIX.
                        //throw new SplitsException("Algorithm failed: " + ex.getMessage());
                        throw ex;
                    }
                } else if (trans instanceof Distances2Trees) {
                    Distances2Trees dist2tree = (Distances2Trees) trans;
                    System.err.println("Computing DISTANCES to TREES: " +
                            Configurator.getOptions(dist2tree));
                    trees = dist2tree.apply(this, taxa, distances);
                    System.err.println("done");
                    if (assumptions.getTreesTransformName() == null)
                        assumptions.setTreesTransformName("TreeSelector");
                } else if (trans instanceof Distances2Network) {
                    Distances2Network distances2Network = (Distances2Network) trans;
                    System.err.println("Computing DISTANCES to NETWORK: " +
                            Configurator.getOptions(distances2Network));
                    network = distances2Network.apply(this, taxa, distances);
                    System.err.println("done");
                } else
                    throw new SplitsException("update failed for DISTANCES");

            }

            // update all depending on quartets
            if (!found && name.equalsIgnoreCase(Quartets.NAME)) {
                found = true;
                if (quartets == null)
                    throw new SplitsException("Quartets failed: null block");
                if (taxa.getOriginalTaxa() != null && assumptions.getExTaxa() != null && isFirstValidInputBlock(Quartets.NAME))
                    quartets.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());
                // post modify quartets here
            }

            // QUARTETS
            if (found && quartets != null) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Quartets", "analysis");
                    analysis.apply(this, taxa, Quartets.NAME);

                }

                QuartetsTransform trans = assumptions.getQuartetsTransform();
                if (!inBootstrap)
                    notifyTasks("Quartets", assumptions.getQuartetsTransformName());
                splits = null;
                trees = null;
                network = null;

                if (!trans.isApplicable(this, taxa, quartets)) {
                    throw new SplitsException
                            ("Quartets: transform not applicable: " + assumptions.getQuartetsTransformName());
                }

                if (trans instanceof Quartets2Splits) {
                    Quartets2Splits quart2splits = (Quartets2Splits) trans;
                    System.err.println("Computing QUARTETS to SPLITS: " +
                            Configurator.getOptions(quart2splits));
                    if (!fixSplits())
                        splits = quart2splits.apply(this, taxa, quartets);
                } else
                    throw new SplitsException("update failed for QUARTETS");


            }

            // TREES update everything depending on trees
            if (!found && name.equalsIgnoreCase(Trees.NAME)) {
                found = true;
                if (trees == null)
                    throw new SplitsException("Trees failed: null block");
            }
            //  modify trees
            if (found && trees != null)
                updateTreesPostModification(taxa, trees);

            //the following line caused bootstrap to crash if the method is a tree building method. had to take this out. (markus)
            //if(found && trees!=null && !isInBootstrap())
            if (found && trees != null) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Trees", "analysis");
                    analysis.apply(this, taxa, Trees.NAME);
                }
                TreesTransform trans = assumptions.getTreesTransform();
                if (!inBootstrap) {
                    notifyTasks("Trees", assumptions.getTreesTransformName());
                    notifySetProgress(-1);
                }
                if (!fixSplits())
                    splits = null;
                network = null;

                if (!trans.isApplicable(this, taxa, trees)) {
                    throw new SplitsException
                            ("Trees: transform not applicable: " + assumptions.getTreesTransformName());
                }

                if (trans instanceof Trees2Splits) {
                    Trees2Splits tree2splits = (Trees2Splits) trans;
                    System.err.println("Computing TREES to SPLITS: " +
                            Configurator.getOptions(tree2splits));
                    if (!fixSplits())
                        splits = tree2splits.apply(this, taxa, trees);
                    System.err.println("done");
                } else if (trans instanceof Trees2Network) {
                    Trees2Network trees2network = (Trees2Network) trans;
                    System.err.println("Computing TREES to NETWORK: " +
                            Configurator.getOptions(trees2network));
                    network = trees2network.apply(this, taxa, trees);
                    System.err.println("done");
                } else
                    throw new SplitsException("update failed for TREES");

            }

            // SPLITS   update all depending on splits
            if (!found && name.equalsIgnoreCase(Splits.NAME)) {
                found = true;
                if (splits == null)
                    throw new SplitsException("Splits failed: null block");
            }
            //  modify splits
            if (found && splits != null)
                updateSplitsPostModification(taxa, splits, name);

            // apply transformation to splits:
            if (found && splits != null && !isInBootstrap()) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Splits", "analysis");
                    analysis.apply(this, taxa, Splits.NAME);
                }
                SplitsTransform trans = assumptions.getSplitsTransform();
                if (!inBootstrap) {
                    notifyTasks("Splits", assumptions.getSplitsTransformName());
                    notifySetProgress(-1);
                }
                network = null;

                if (!trans.isApplicable(this, taxa, splits)) {
                    throw new SplitsException
                            ("Splits: transform not applicable: " + assumptions.getSplitsTransformName());
                }

                if (trans instanceof Splits2Network) {
                    Splits2Network splits2network = (Splits2Network) trans;
                    System.err.println("Computing SPLITS to NETWORK: " +
                            Configurator.getOptions(splits2network));
                    try {
                        network = splits2network.apply(this, taxa, splits);
                    } catch (CanceledException ex) {
                        throw new CanceledException(Splits.NAME);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                } else
                    throw new SplitsException("update failed for SPLITS");

            }

            if (SplitsTreeProperties.ALLOW_RETICULATE) {
                //RETICULATE update all depending on reticulate
                if (!found && name.equalsIgnoreCase(Reticulate.NAME)) {
                    found = true;
                    if (reticulate == null) {
                        throw new SplitsException("Reticulate failed: null block");
                    }
                }
                //apply transformation to reticulate
                if (found && reticulate != null && !isInBootstrap()) {
                    if (analysis != null) {
                        notifySetProgress(1);
                        notifyTasks("Reticulate", "analysis");
                        analysis.apply(this, taxa, Reticulate.NAME);
                    }
                    ReticulateTransform trans = assumptions.getReticulateTransform();
                    if (!inBootstrap) {
                        notifyTasks("Reticulate", assumptions.getReticulateTransformName());
                        notifySetProgress(-1);
                    }
                    network = null;
                    if (!trans.isApplicable(this, taxa, reticulate)) {
                        throw new SplitsException("Reticulate: tranform not applicable: " + assumptions.getReticulateTransformName());
                    }
                    if (trans instanceof Reticulate2Network) {
                        Reticulate2Network ret2graph = (Reticulate2Network) trans;
                        System.err.println("Computing Reticulate to Network: " + Configurator.getOptions(ret2graph));
                        try {
                            network = ret2graph.apply(this, taxa, reticulate);
                        } catch (CanceledException ex) {
                            throw new CanceledException(Splits.NAME);
                        } catch (Exception ex) {
                            Basic.caught(ex);
                        }
                    } else
                        throw new SplitsException("update failed for Reticulate");
                }
            }

            // if update of stuff above bootstrap, clear bootstrap:
            if (found && bootstrap != null)
                bootstrap = null;

            if (!found && name.equalsIgnoreCase(Bootstrap.NAME)) {
                found = true;
                if (bootstrap == null)
                    throw new SplitsException("Bootstrap failed: null block");
            }
            // update all depending on  network:
            if (!found && name.equalsIgnoreCase(Network.NAME)) {
                found = true;
                if (network == null)
                    throw new SplitsException("Network failed: null block");
                if (taxa.getOriginalTaxa() != null && assumptions.getExTaxa() != null && isFirstValidInputBlock(Network.NAME))
                    network.hideTaxa(taxa.getOriginalTaxa(), assumptions.getExTaxa());
            }
            if (found && network != null) {
                if (analysis != null) {
                    notifySetProgress(-1);
                    notifyTasks("Network", "analysis");

                    analysis.apply(this, taxa, Network.NAME);
                }
            }
        } catch (NoClassDefFoundError ex) {
            setValid(false);
            throw new SplitsException(ex.toString());
        } catch (Exception ex) {
            setValid(false);
            throw ex;
        } finally {
            if (unaligned == null)
                assumptions.setUnalignedTransformName(null);
            if (characters == null)
                assumptions.setCharactersTransformName(null);
            if (distances == null)
                assumptions.setDistancesTransformName(null);
            if (quartets == null)
                assumptions.setQuartetsTransformName(null);
            if (trees == null)
                assumptions.setTreesTransformName(null);
            if (splits == null)
                assumptions.setSplitsTransformName(null);
            if (found)
                setDirty(true);
        }
        //assumptions.setUptodate(true);
    }

    /**
     * does it look like hidden taxa choice has changed?
     *
     * @param taxa
     * @param assumptions
     * @return true or false
     */
    private boolean taxaHaveJustChanged(Taxa taxa, Assumptions assumptions) {
        int sizeOfExTaxa = (assumptions.getExTaxa() == null ? 0 : assumptions.getExTaxa().cardinality());
        int sizeOfHidden = (taxa.getHiddenTaxa() == null ? 0 : taxa.getHiddenTaxa().cardinality());
        //TaxaSet extaxa = assumptions.getExTaxa();

        return sizeOfExTaxa != sizeOfHidden || (sizeOfExTaxa > 0 && !assumptions.getExTaxa().equals(taxa.getHiddenTaxa()));


    }

    /**
     * post modifies the set of splits in such a way that the original ones don't get lost
     *
     * @param taxa
     * @param splits
     * @throws CanceledException
     */
    private void updateSplitsPostModification(Taxa taxa, Splits splits, String targetName) throws CanceledException {

        //Deal with excluded taxa and hidden splits.

        if (targetName == null || !targetName.equals(Splits.NAME)) {
            assumptions.setExSplits(null);    //A new set of splits has been computed, so we delete the now invalid list of hidden splits
        } else {
            Taxa originalTaxa = taxa.getOriginalTaxa();
            if (originalTaxa == null)
                originalTaxa = taxa;

            //If getOriginal is null then this is the original set of splits. Otherwise, we restore the full set.
            if (splits.getOriginal() == null)
                splits.setOriginal(originalTaxa);
            else
                splits.restoreOriginal(originalTaxa);

            //Hide splits
            if (assumptions.getExSplits() != null) {
                splits.hideSplits(originalTaxa, Basic.asBitSet(assumptions.getExSplits()));
            }

            //Hide taxa in splits
            if (assumptions.getExTaxa() != null
                    && assumptions.getExTaxa().cardinality() > 0)
                splits.hideTaxa(originalTaxa, assumptions.getExTaxa());

        }

        // modify or select splits:

        if (assumptions.getSplitsPostProcess().isLeastSquares()) {
            System.err.println("Applying least squares");
            LeastSquaresWeights leastSquares = new LeastSquaresWeights();
            if (leastSquares.isApplicable(this, taxa, splits)) {
                leastSquares.apply(this, taxa, splits);
            }
        }
        int origNSplits = splits.getNsplits();
        if (assumptions.getSplitsPostProcess().getGreedyCompatible()) {
            System.err.println("Applying greedy compatible");
            GreedyCompatible greedyCompatible = new GreedyCompatible();
            if (greedyCompatible.isApplicable(this, taxa, splits)) {
                greedyCompatible.apply(this, taxa, splits);
            }
        }
        if (assumptions.getSplitsPostProcess().getClosestTree()) {
            System.err.println("Applying closest tree");
            ClosestTree closestTree = new ClosestTree();
            if (closestTree.isApplicable(this, taxa, splits)) {
                closestTree.apply(this, taxa, splits);
            }
        }
        if (assumptions.getSplitsPostProcess().getGreedyWC()) {
            System.err.println("Applying greedy weakly compatible");
            GreedyWC greedyWC = new GreedyWC();
            if (greedyWC.isApplicable(this, taxa, splits)) {
                greedyWC.apply(this, taxa, splits);
            }
        }
        if (assumptions.getSplitsPostProcess().getWeightThreshold()) {
            System.err.println("Applying weight threshold=" +
                    assumptions.getSplitsPostProcess().getWeightThresholdValue()
                    + " nsplits: " + splits.getNsplits() + " -> ");
            SplitsUtilities.applyWeightThreshold(splits,
                    assumptions.getSplitsPostProcess().getWeightThresholdValue());
            System.err.println(splits.getNsplits());
        }
        if (assumptions.getSplitsPostProcess().getConfidenceThreshold()) {
            System.err.print("Applying confidence threshold=" +
                    assumptions.getSplitsPostProcess().getConfidenceThresholdValue()
                    + " nsplits: " + splits.getNsplits() + " -> ");
            SplitsUtilities.applyConfidenceThreshold(splits,
                    assumptions.getSplitsPostProcess().getConfidenceThresholdValue());
            System.err.println(splits.getNsplits());
        }
        if (assumptions.getSplitsPostProcess().getDimensionFilter()
                && splits.getProperties().getCompatibility() != Splits.Properties.COMPATIBLE
                && splits.getProperties().getCompatibility() != Splits.Properties.CYCLIC) {
            System.err.print("Applying dimension filter");

            DFilter.applyFilter(this, splits, assumptions.getSplitsPostProcess().getDimensionValue());
            //DANIEL: I think the following would be a better (heuristic) filter.
            //CircularDFilter.applyFilter(this, splits, assumptions.getSplitsPostProcess().getDimensionValue());

        }
        if (splits.getNsplits() != origNSplits && assumptions.getLayoutStrategy() == Assumptions.RECOMPUTE)
            try {
                splits.setCycle(null); // need to recompute cycle
            } catch (SplitsException e) {
                Basic.caught(e);
            }
    }


    /**
     * post modifies the set of trees in such a way that the original ones don't get lost
     *
     * @param taxa
     * @param trees
     */
    private void updateTreesPostModification(Taxa taxa, Trees trees) {
        // setup or restore original trees:
        {
            Taxa theTaxa;

            if (getNameTop().equals(Trees.NAME) && taxa.getOriginalTaxa() != null)
                theTaxa = taxa.getOriginalTaxa();
            else
                theTaxa = taxa;

            if (trees.getOriginal() == null)
                trees.setOriginal(theTaxa);
            else {
                trees.restoreOriginal(theTaxa);
            }

            // hide taxa:
            if (getNameTop().equals(Trees.NAME) && assumptions.getExTaxa() != null
                    && assumptions.getExTaxa().cardinality() > 0) {
                trees.hideTaxa(theTaxa, assumptions.getExTaxa());
            }
        }

        // modify or select trees:

        if (assumptions.getExTrees() != null && assumptions.getExTrees().size() > 0) {
            for (String label : assumptions.getExTrees()) {
                int index = trees.indexOf(label);
                if (index > 0) {
                    if (trees.getNtrees() > 1)
                        trees.removeTree(index);
                } else
                    System.err.println("Hide trees: tree not found: " + label);
            }
            if (trees.getNtrees() == 1) {
                try {
                    trees.setTaxaFromPartialTrees(taxa);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
    }


    /**
     * Reads the document.
     * If the first message read is a taxa block, then all existing blocks
     * are discarded and replaced by the blocks found in the input.
     * If the first block read is not a taxa block,  then the input is
     * assumed to consist of format modifications of existing blocks only
     *
     * @param r the reader
     */
    public void readNexus(Reader r) throws SplitsException, java.io.IOException,
            CanceledException {
        readNexus(new NexusStreamParser(r));
    }

    /**
     * Reads the document.
     * If the first message read is a taxa block, then all existing blocks
     * are discarded and replaced by the blocks found in the input.
     * If the first block read is not a taxa block,  then the input is
     * assumed to consist of format modifications of existing blocks only
     *
     * @param np the nexus stream parser
     */
    public void readNexus(NexusStreamParser np) throws SplitsException,
            java.io.IOException, CanceledException {
        notifySetMaximumProgress(100);
        // be tolerant with respect to presence or absence of #nexus
        if (np.peekMatchIgnoreCase("#nexus"))
            np.matchIgnoreCase("#nexus");

        notifyProgress(np);

        np.setCollectAllComments(true); // want to collect all comments upto taxa block
        if (np.peekMatchBeginBlock(Taxa.NAME))
            setTopComments(np.getComment());
        np.setCollectAllComments(false);

        if (np.peekMatchBeginBlock(Taxa.NAME)) {
            notifySubtask("TAXA");
            deleteDependentBlocks(null);
            taxa = new Taxa();
            System.err.println("Read TAXA:");
            taxa.read(np);
            System.err.println("\t" + taxa.getNtax() + " taxa");

            // if we are reading new data, clobber the sets block!
            sets = null;
        }
        if (taxa == null)
            throw new SplitsException("No valid taxa block");
        // read all other blocks
        while (np.peekNextToken() != NexusStreamParser.TT_EOF) {
            if (np.peekMatchBeginBlock(Unaligned.NAME)) {
                if (unaligned == null)
                    unaligned = new Unaligned();

                System.err.println("Read UNALIGNED:");
                notifySubtask("UNALIGNED");
                unaligned.read(np, taxa);
                notifyProgress(np);
                System.err.println("\t" + taxa.getNtax() + " sequences, datatype: " +
                        unaligned.getFormat().getDatatype());
                notifySetProgress(30);
            } else if (np.peekMatchBeginBlock(Characters.NAME)) {
                if (characters == null)
                    characters = new Characters();
                System.err.println("Read CHARACTERS:");
                notifySubtask("CHARACTERS");
                // try {  NOTE: this exception is caught and handled further up.
                characters.read(np, taxa, this);
                System.err.println("\t" + taxa.getNtax() + " sequences,"
                        + " each " + characters.getNchar() + ", datatype: " +
                        characters.getFormat().getDatatype());
                notifySetProgress(50);
                //} catch (CanceledException ex) {
                // characters = null;
                // throw ex;
                // new Alert("User Canceled Read");
                // return;
                //}
            } else if (np.peekMatchBeginBlock(Distances.NAME)) {
                if (distances == null)
                    distances = new Distances();
                System.err.println("Read DISTANCES:");
                notifySubtask("DISTANCES");
                notifySetProgress(-1);     //Sets indeterminate.
                distances.read(np, taxa);
                System.err.println("\t" + taxa.getNtax() + "x" + taxa.getNtax()
                        + " matrix");
                notifySetProgress(60);
            } else if (np.peekMatchBeginBlock(Quartets.NAME)
                    || np.peekMatchIgnoreCase("begin st_quartets")) {
                if (quartets == null)
                    quartets = new Quartets();
                notifySubtask("QUARTETS");
                System.err.println("Read QUARTETS:");
                quartets.read(np, taxa);
                System.err.println("\t" + quartets.size());
                notifySetProgress(65);
            } else if (np.peekMatchBeginBlock(Trees.NAME)) {
                if (trees == null)
                    trees = new Trees();
                notifySubtask("TREES");
                System.err.println("Read TREES:");
                trees.read(np, taxa);
                System.err.println("\t" + trees.getNtrees());
                notifySetProgress(70);
            } else if (np.peekMatchBeginBlock(Splits.NAME) || np.peekMatchIgnoreCase("begin st_splits")) {
                if (splits == null)
                    splits = new Splits();
                System.err.println("Read SPLITS:");
                notifySubtask("SPLITS");
                splits.read(np, taxa);
                System.err.println("\t" + splits.getSplitsSet().getNsplits() + " splits total");
                notifySetProgress(80);
            } else if (np.peekMatchBeginBlock(Reticulate.NAME)) {
                if (reticulate == null)
                    reticulate = new Reticulate();
                System.err.println("Read RETICULATE:");
                notifySubtask("RETICULATE");
                reticulate.read(np, taxa);
            } else if (np.peekMatchBeginBlock(Network.NAME) || np.peekMatchBeginBlock("st_graph")) {
                if (network == null)
                    network = new Network();
                System.err.println("Read NETWORK:");
                notifySubtask("NETWORK");
                network.read(np, taxa);
                System.err.println("\t" + network.getNvertices() + " vertices, " +
                        network.getNedges() + " edges");
                notifySetProgress(90);
            } else if (np.peekMatchBeginBlock(Assumptions.NAME)) {
                if (assumptions == null)
                    assumptions = new Assumptions();
                notifySubtask("ST_ASSUMPTIONS");
                System.err.println("Read ST_ASSUMPTIONS:");
                assumptions.read(np, taxa);
                if (assumptions.isUptodate())
                    System.err.println("\tup-to-date");
                else
                    System.err.println("\tNot up-to-date");
                notifySetProgress(95);
            } else if (np.peekMatchBeginBlock(Bootstrap.NAME)) {
                if (bootstrap == null)
                    bootstrap = new Bootstrap(this);
                System.err.println("Read ST_BOOTSTRAP:");
                notifySubtask("ST_BOOTSTRAP");
                bootstrap.read(np, taxa, characters, splits);
                System.err.println("\t" + bootstrap.getRuns() + " runs");
            } else if (np.peekMatchBeginBlock(Analysis.NAME)) {
                if (analysis == null)
                    analysis = new Analysis();
                System.err.println("Read ST_ANALYSIS: ");
                notifySubtask("ST_ANALYSIS");
                analysis.read(np);
                System.err.println("\t" + analysis.getNanalyzers() + " analyzers");
            } else if (np.peekMatchBeginBlock(Sets.NAME)) {
                if (sets == null)
                    sets = new Sets();
                System.err.println("Read SETS: ");
                notifySubtask("SETS");
                sets.read(np, taxa, characters);
                if (sets.getNumTaxSets() > 0)
                    System.err.println("\t" + sets.getNumTaxSets() + " taxa sets");
                if (sets.getNumCharSets() > 0)
                    System.err.println("\t" + sets.getNumCharSets() + " char sets");
                if (sets.getNumCharPartitions() > 0)
                    System.err.println("\t" + sets.getNumCharPartitions() + " char partitions");

            } else if (np.peekMatchIgnoreCase(";"))
                np.matchIgnoreCase(";");
            else if (np.peekMatchBeginBlock("data"))
                throw new SplitsException("line " + np.lineno() +
                        ": data block: This file looks like OLD nexus, please import");
            else if (np.peekMatchBeginBlock("SplitsTree")) {
                // read in splitstree commands:
                try {
                    read(np);
                } catch (Exception ex) {
                    throw new SplitsException("Parse SplitsTree block failed: " + ex.getMessage());
                }
            } else if (np.peekMatchBeginBlock("Traits")) {
                // read in attribues block
                if (traits == null)
                    traits = new Traits(taxa.getNtax());
                try {
                    traits.read(np, taxa);
                } catch (Exception ex) {
                    throw new SplitsException("Parse Traits block failed: " + ex.getMessage());
                }

            } else if (np.peekMatchAnyTokenIgnoreCase("begin beginblock")) {
                // try to skip unknown blocks:
                np.matchAnyTokenIgnoreCase("begin beginblock");
                String name = np.getWordFileNamePunctuation();
                np.matchRespectCase(";");
                new Alert(parent, "Skipping NEXUS block: " + name);
                System.err.print("Skipping block '" + name + "': ");
                while (true) {
                    while (!np.peekMatchAnyTokenIgnoreCase("end endblock")) {
                        np.nextToken();
                        if (np.ttype == NexusStreamTokenizer.TT_EOF)
                            throw new SplitsException("line " + np.lineno() +
                                    ": Unexpected EOF while skipping block");
                    }
                    np.matchAnyTokenIgnoreCase("end endblock");
                    if (np.peekMatchRespectCase(";")) {
                        np.matchRespectCase(";");
                        if (np.peekMatchAnyTokenIgnoreCase("begin beginblock"))
                            break;
                        np.nextToken();
                        if (np.ttype == NexusStreamParser.TT_EOF) // EOF ok
                            break;
                    }
                }
                System.err.println("done");
            } else
                throw new SplitsException("line " + np.lineno() +
                        ": illegal statement: " + np.getWordRespectCase());
        }
        if (assumptions == null)
            assumptions = new Assumptions();
    }

    /**
     * Write the document
     */
    public void write(Writer w) throws java.io.IOException {
        w.write("#nexus\n");
        if (getTopComments() != null)
            w.write("[!" + getTopComments() + "]\n");
        write(w, Taxa.NAME);
        write(w, Unaligned.NAME);
        write(w, Characters.NAME);
        write(w, Distances.NAME);
        write(w, Sets.NAME);
        write(w, Quartets.NAME);
        write(w, Trees.NAME);
        write(w, Splits.NAME);
        if (SplitsTreeProperties.ALLOW_RETICULATE) write(w, Reticulate.NAME);
        write(w, Network.NAME);
        write(w, Bootstrap.NAME);
        write(w, Traits.NAME);
        write(w, Analysis.NAME);
        write(w, Assumptions.NAME);
    }

    /**
     * Writes a specific block
     *
     * @param w    the writer
     * @param name the name of the block
     * @return true, if the named block is valid and was successfully written
     */
    public boolean write(Writer w, String name) throws java.io.IOException {
        if (name.equalsIgnoreCase(Taxa.NAME) && taxa != null) {
            //w.write("#nexus\n");
            taxa.write(w);
        } else if (name.equalsIgnoreCase(Unaligned.NAME) && unaligned != null) {
            unaligned.write(w, taxa);
            // System.err.print(" " + unaligned.NAME);
        } else if (name.equalsIgnoreCase(Characters.NAME) && characters != null) {
            characters.write(w, taxa);
        } else if (name.equalsIgnoreCase(Distances.NAME) && distances != null) {
            distances.write(w, taxa);
        } else if (name.equalsIgnoreCase(Quartets.NAME) && quartets != null) {
            quartets.write(w, taxa);
        } else if (name.equalsIgnoreCase(Trees.NAME) && trees != null) {
            trees.write(w, taxa);
        } else if (name.equalsIgnoreCase(Splits.NAME) && splits != null) {
            splits.write(w, taxa);
        } else if (name.equalsIgnoreCase(Reticulate.NAME) && reticulate != null) {
            reticulate.write(w, taxa);
        } else if (name.equalsIgnoreCase(Network.NAME) && network != null) {
            network.write(w, taxa);
        } else if (name.equalsIgnoreCase(Bootstrap.NAME) && bootstrap != null) {
            bootstrap.write(w, taxa);
        } else if (name.equalsIgnoreCase(Traits.NAME) && traits != null) {
            traits.write(w, taxa);
        } else if (name.equalsIgnoreCase(Analysis.NAME) && analysis != null
                && analysis.getNanalyzers() > 3) {
            analysis.write(w);
        } else if (name.equalsIgnoreCase(Assumptions.NAME) && assumptions != null
                && taxa != null) {
            assumptions.write(w, this);
        } else if (name.equalsIgnoreCase(Sets.NAME) && sets != null && taxa != null) {
            sets.write(w, taxa);
        } else
            return false;
        w.flush();
        return true;
    }

    /**
     * Writes a specific block
     *
     * @param name the name of the block
     * @return true, if the named block is valid and was successfully written
     */
    public String getNameForDataTree(String name) {
        StringWriter w = new StringWriter();
        if (name.equalsIgnoreCase(Taxa.NAME) && taxa != null) {
            w.write("Taxa <font color=#a0a0a0>(" + taxa.getNtax() + ")");
        } else if (name.equalsIgnoreCase(Characters.NAME) && characters != null) {
            w.write("Characters <font color=#a0a0a0>(" + characters.getNchar() + ", " + characters.getFormat().getDatatype().toUpperCase() + ")");
        } else if (name.equalsIgnoreCase(Distances.NAME) && distances != null) {
            w.write("Distances");
        } else if (name.equalsIgnoreCase(Trees.NAME) && trees != null) {
            w.write("Trees <font color=#a0a0a0>(" + trees.getNtrees() + ")");
        } else if (name.equalsIgnoreCase(Splits.NAME) && splits != null) {
            w.write("Splits <font color=#a0a0a0>(" + splits.getNsplits() + ")");
        } else if (name.equalsIgnoreCase(Reticulate.NAME) && reticulate != null) {
            w.write("Reticulate <font color=#a0a0a0>(" + reticulate.getNNettedComponents() + ")");
        } else if (name.equalsIgnoreCase(Network.NAME) && network != null) {
            w.write("Network <font color=#a0a0a0>(" + network.getNvertices() + ", " + network.getNedges() + ")");
        } else if (name.equalsIgnoreCase(Bootstrap.NAME) && bootstrap != null) {
            w.write("Bootstrap <font color=#a0a0a0>(" + bootstrap.getRuns() + "runs)");
        } else if (name.equalsIgnoreCase(Traits.NAME) && traits != null) {
            w.write("Traits: <font color=#a0a0a0>(" + traits.getNtraits() + ")");
        } else if (name.equalsIgnoreCase(Analysis.NAME) && analysis != null
                && analysis.getNanalyzers() > 3) {
            w.write("Analyzers: <font color=#a0a0a0>(" + analysis.getNanalyzers() + ")");
        } else if (name.equalsIgnoreCase(Assumptions.NAME) && assumptions != null
                && taxa != null) {
            w.write("Assumptions");
        } else if (name.equalsIgnoreCase(Sets.NAME) && sets != null && taxa != null) {
            if (sets.getNumTaxSets() > 0)
                w.write("Sets <font color=#a0a0a0>(" + (sets.getNumTaxSets() + sets.getNumTaxonomys() + sets.getNumCharSets()) + ")");
        }
        return w.toString();
    }

    /**
     * shows usage of the named transform
     *
     * @param name the name
     */
    public void showUsageTransform(String name) {
        Iterator it = Transforms.getAllTransforms(name).iterator();
        if (!it.hasNext())
            System.out.println("No such transform: " + name);
        while (it.hasNext()) {
            Transformation trans = ((Transformation) it.next());
            System.out.println("Help for transform=" + name + ":");
            System.out.println("Description: " + Configurator.getDescription(trans));
            System.out.println("Syntax: " + Configurator.getUsage(trans));
            System.out.println("Input:  " + Transforms.getInputBlockName(trans));
            System.out.println("Output: " + Transforms.getOutputBlockName(trans));
        }
    }

    /**
     * Shows usage of a specific block
     *
     * @param name the name of the block
     * @return true, if name is valid and usage was shown
     */
    public boolean showUsage(String name) {
        if (name.equalsIgnoreCase(Taxa.NAME)) {
            Taxa.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Unaligned.NAME)
                || name.equals("unaligned")) {
            Unaligned.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Characters.NAME)
                || name.equals("chars")) {
            Characters.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Distances.NAME)
                || name.equals("dist")) {
            Distances.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Quartets.NAME)
                || name.equals("st_quartets")) {
            Quartets.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Trees.NAME)) {
            Trees.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Splits.NAME)
                || name.equals("st_splits")) {
            Splits.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Network.NAME)
                || name.equals("network")) {
            Network.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Bootstrap.NAME)
                || name.equals("bootstrap")) {
            Bootstrap.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Analysis.NAME)
                || name.equals("analysis")) {
            Analysis.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Traits.NAME)
                || name.equals("traits")) {
            Traits.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Sets.NAME)) {
            Sets.showUsage(System.err);
        } else if (name.equalsIgnoreCase(Reticulate.NAME))
            Reticulate.showUsage(System.err);
        else if (name.equalsIgnoreCase(Assumptions.NAME)
                || name.equals("assumptions") || name.equals("assume")) {
            Assumptions.showUsage(System.err);
        } else
            return false;
        return true;
    }


    /**
     * import non-nexus data from a string
     *
     * @param source the source string
     * @return true, if import successful
     */
    public String importDataFromString(Component parent, String source) throws IOException, SplitsException, CanceledException {
        //notifyLockInput();
        clear();
        String result = ImportManager.importDataFromString(source);
        if (result == null || result.equals(""))
            throw new IOException("Unrecognized input format or syntax error in input");
        setDirty(true);
        readNexus(new StringReader(result));
        if (getCharacters() != null && getCharacters().getFormat().getDatatype().equals(Characters.Datatypes.UNKNOWN)) {
            getCharacters().getFormat().setDatatype(chooseDatatype(parent));

            if (!getCharacters().getFormat().getDatatype().equals(Characters.Datatypes.UNKNOWN)) {
                StringWriter w = new StringWriter();
                w.write("#nexus\n");
                if (getTopComments() != null)
                    w.write("[!" + getTopComments() + "]\n");
                write(w, Taxa.NAME);
                write(w, Characters.NAME);
                result = w.toString();
            }
        }
        return result;
    }

    /**
     * puts up a dialog for user to choose datatype
     *
     * @return data type
     */
    public static String chooseDatatype(Component parent) {
        String[] choices = {Characters.Datatypes.PROTEIN,
                Characters.Datatypes.DNA,
                Characters.Datatypes.RNA,
                Characters.Datatypes.STANDARD,
                Characters.Datatypes.UNKNOWN};
        return (String) JOptionPane.showInputDialog(parent, "Can't determine the datatype, please choose:", "Choose Datatype", JOptionPane.QUESTION_MESSAGE, null,
                choices, Characters.Datatypes.UNKNOWN);
    }


    /**
     * Are currently in  a boostrap run
     *
     * @return true, if currently in bootstrap computation
     */
    public boolean isInBootstrap() {
        return inBootstrap;
    }

    /**
     * Sets that we are currently in bootstrap run
     *
     * @param inBootstrap true, if in bootstrap run
     */
    public void setInBootstrap(boolean inBootstrap) {
        this.inBootstrap = inBootstrap;
    }

    /**
     * Checks a flag indicating whether we should leave the splits as is (used
     * during some bootstrap routines)
     *
     * @return fixSplits
     */
    public boolean fixSplits() {
        return keepSplits;
    }

    /**
     * Sets a flag indicating that we should not change the splits.
     *
     * @param fs
     */
    public void setKeepSplits(boolean fs) {
        keepSplits = fs;
    }

    /**
     * is current state of document valid?
     *
     * @return is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * set the current state of the document
     *
     * @param valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }


    /**
     * Compute the number of lines in a file
     *
     * @param file
     */
    private int countNumberLines(File file) {
        int countRec = 0;
        try {
            RandomAccessFile randFile = new RandomAccessFile(file, "r");
            long lastRec = randFile.length();
            randFile.close();
            FileReader fileRead = new FileReader(file);
            LineNumberReader lineRead = new LineNumberReader(fileRead);
            lineRead.skip(lastRec);
            countRec = lineRead.getLineNumber() - 1;
            fileRead.close();
            lineRead.close();
        } catch (IOException e) {
        }
        return countRec;
    }


    /**
     * Open a new file
     *
     * @param file the file
     */
    public void open(Component parent, File file) throws IOException, SplitsException {
        clear();

        setNumberLines(countNumberLines(file));
        NexusStreamParser fp = null;
        try {
            fp = new NexusStreamParser(new FileReader(file));
        } catch (FileNotFoundException e) {
            new Alert(parent, "File open failed: " + e.getMessage());
        }

        try {
            if (fp != null) {
                setFile(file);
                setDirty(false);
                fp.matchIgnoreCase("#nexus");
                readNexus(fp);
            }
        } catch (CanceledException ex) {
            //User pressed cancel during read. Clear, and pass exception.
            clear();
            // throw ex;
        } catch (IOException | SplitsException ex) {
            clear();
            new Alert(parent, "Error parsing file: " + ex.getMessage());
            throw ex;
        }
        try {
            if (!BlockChooser.show(parent, this)) {
                throw new CanceledException();
            }
            update();
        } catch (CanceledException ex) {
            //User pressed cancel during the update.
            //The name of the source block when the user cancelled is returned
            //as a message.
            if (isValidByName(ex.getMessage()))
                deleteDependentBlocks(ex.getMessage());
            else
                clear();
            // throw ex;
        } catch (OutOfMemoryError ex) {
            System.gc();
            new Alert("Out of memory");
            clear();
        } catch (Exception ex) {
            new Alert(parent, "Update failed: " + ex.getMessage());
        }
    }

    /**
     * Save to a file
     *
     * @param file the file
     */
    public void save(File file) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
        } catch (IOException e) {
            new Alert(parent, "File open-to-write failed: " + e.getMessage());
        }
        try {
            if (fw != null) {
                write(fw);
                setDirty(false);
                setFile(file);
                fw.close();
            }
        } catch (IOException e) {
            new Alert(parent, "File write failed: " + e.getMessage());
        }
    }

    /**
     * Exports the data given in the Document using the given exporter;
     *
     * @param file     The name of the file the exporter should write to
     * @param exporter The name of the exporter to be used
     * @param dp       The Document
     * @param complete save all sequence sites
     * @param blocks   list of blocks to export
     * @throws CanceledException
     */
    public void exportFile(File file, String exporter, Document dp, Collection blocks, boolean complete) {
        exportFile(file, exporter, dp, blocks, complete, null);
    }

    /**
     * Exports the data given in the Document using the given exporter;
     *
     * @param file     The name of the file the exporter should write to
     * @param exporter The name of the exporter to be used
     * @param dp       The Document
     * @param complete save all sequence sites
     * @param blocks   list of blocks to export
     * @throws CanceledException
     */
    public void exportFile(File file, String exporter, Document dp, Collection blocks, boolean complete, ExporterInfo additionalInfo) {
        try {
            ExportManager.exportData(file, false, complete, exporter, blocks, this, additionalInfo);
            // update();
        } catch (SplitsException e) {
            new Alert(parent, "File export failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); //DEBUGGING
            Basic.caught(e);
        }
    }

    /**
     * Sets whether the cancel option is open to the user.
     *
     * @param enabled
     */
    public void notifyEnabled(boolean enabled) {
        if (progressListener != null) {
            progressListener.setCancelable(enabled);
        }
    }

    /**
     * notifies the progess monitor to set the maximum
     *
     * @param max
     */
    public void notifySetMaximumProgress(int max) throws CanceledException {
        if (progressListener != null) {
            progressListener.setMaximum(max);
            progressListener.setProgress(0);
        }

    }

    /**
     * Sets the progress bar to the current proportion of the file we've read in.
     *
     * @param np
     */
    public void notifyProgress(NexusStreamParser np) throws CanceledException {
        notifySetProgress((100 * np.lineno() / Math.max(1, getNumberLines())));
    }


    /**
     * notifys the progress listener the progress
     *
     * @param current step number
     */
    public void notifySetProgress(int current) throws CanceledException {
        if (progressListener != null && !this.isInBootstrap()) {
            progressListener.setProgress(current);
        }
    }

    public void notifySubtask(String subtask) {
        if (progressListener != null && !this.isInBootstrap()) {
            progressListener.setSubtask(subtask);
        }
    }

    /**
     * set the task and subtask name
     *
     * @param task
     * @param subtask
     */
    public void notifyTasks(String task, String subtask) {
        progressListener.setTasks(task, subtask);
    }

    /**
     * get the set progress listener
     *
     * @return progress listener
     */
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * set the progress listener
     *
     * @param progressListener
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * returns true, if given transform is applicable at current state of document
     *
     * @param transform
     * @return true, if transform applicable
     */
    public boolean isApplicable(Transformation transform) {
        if (transform == null)
            return false;
        if (UnalignedTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && unaligned != null &&
                    ((UnalignedTransform) transform).isApplicable(this, taxa, unaligned);
        } else if (CharactersTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && characters != null &&
                    ((CharactersTransform) transform).isApplicable(this, taxa, characters);
        } else if (DistancesTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && distances != null &&
                    ((DistancesTransform) transform).isApplicable(this, taxa, distances);
        } else if (TreesTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && trees != null &&
                    ((TreesTransform) transform).isApplicable(this, taxa, trees);
        } else if (QuartetsTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && quartets != null &&
                    ((QuartetsTransform) transform).isApplicable(this, taxa, quartets);
        } else if (SplitsTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && splits != null &&
                    ((SplitsTransform) transform).isApplicable(this, taxa, splits);
        } else if (ReticulateTransform.class.isAssignableFrom(transform.getClass())) {
            return taxa != null && reticulate != null &&
                    ((ReticulateTransform) transform).isApplicable(this, taxa, reticulate);
        } else
            return false;
    }

    /**
     * gets the list of all block names
     *
     * @return all block names
     */
    public static java.util.List<String> getListOfBlockNames() {
        List<String> list = new LinkedList<>();
        list.add(Taxa.NAME);
        list.add(Traits.NAME);
        list.add(Unaligned.NAME);
        list.add(Characters.NAME);
        list.add(Distances.NAME);
        list.add(Trees.NAME);
        list.add(Sets.NAME);
        list.add(Quartets.NAME);
        list.add(Splits.NAME);
        if (SplitsTreeProperties.ALLOW_RETICULATE) list.add(Reticulate.NAME);
        list.add(Network.NAME);
        list.add(Bootstrap.NAME);
        list.add(Assumptions.NAME);
        return list;
    }

    /**
     * gets the list of all block names of currently valid blocks
     *
     * @return names of all valid blocks
     */
    public java.util.List<String> getListOfValidBlocks() {
        List<String> list = new LinkedList<>();
        if (isValidByName(Taxa.NAME))
            list.add(Taxa.NAME);
        if (isValidByName(Unaligned.NAME))
            list.add(Unaligned.NAME);
        if (isValidByName(Characters.NAME))
            list.add(Characters.NAME);
        if (isValidByName(Distances.NAME))
            list.add(Distances.NAME);
        if (isValidByName(Trees.NAME))
            list.add(Trees.NAME);
        if (isValidByName(Quartets.NAME))
            list.add(Quartets.NAME);
        if (isValidByName(Sets.NAME))
            list.add(Sets.NAME);
        if (isValidByName(Splits.NAME))
            list.add(Splits.NAME);
        if (SplitsTreeProperties.ALLOW_RETICULATE)
            if (isValidByName(Reticulate.NAME))
                list.add(Reticulate.NAME);
        if (isValidByName(Network.NAME))
            list.add(Network.NAME);
        if (isValidByName(Bootstrap.NAME))
            list.add(Bootstrap.NAME);
        if (isValidByName(Assumptions.NAME))
            list.add(Assumptions.NAME);
        return list;
    }

    /**
     * gets the list of all block names of currently valid blocks other than
     * the taxa, assumptions or bootstrap block
     *
     * @return names of all valid input blocks
     */
    public java.util.List<String> getListOfValidInputBlocks() {
        List<String> list = new LinkedList<>();

        if (isValidByName(Unaligned.NAME))
            list.add(Unaligned.NAME);
        if (isValidByName(Characters.NAME))
            list.add(Characters.NAME);
        if (isValidByName(Distances.NAME))
            list.add(Distances.NAME);
        if (isValidByName(Trees.NAME))
            list.add(Trees.NAME);
        if (isValidByName(Quartets.NAME))
            list.add(Quartets.NAME);
        if (isValidByName(Splits.NAME))
            list.add(Splits.NAME);
        if (SplitsTreeProperties.ALLOW_RETICULATE)
            if (isValidByName(Reticulate.NAME))
                list.add(Reticulate.NAME);
        if (isValidByName(Network.NAME))
            list.add(Network.NAME);
        return list;
    }

    /**
     * returns true if named block is first valid input block
     *
     * @param name
     * @return true, if first
     */
    public boolean isFirstValidInputBlock(String name) {
        List validInputBlocks = getListOfValidInputBlocks();
        return validInputBlocks.size() > 0 && validInputBlocks.get(0).equals(name);
    }

    /**
     * keeps only the named input block, deletes all others
     *
     * @param name
     */
    public void keepOnlyThisInputBlock(String name) {

        if (isValidByName(Unaligned.NAME) && !name.equals(Unaligned.NAME))
            setUnaligned(null);
        if (isValidByName(Characters.NAME) && !name.equals(Characters.NAME))
            setCharacters(null);
        if (isValidByName(Distances.NAME) && !name.equals(Distances.NAME))
            setDistances(null);
        if (isValidByName(Trees.NAME) && !name.equals(Trees.NAME))
            setTrees(null);
        if (isValidByName(Quartets.NAME) && !name.equals(Quartets.NAME))
            setQuartets(null);
        if (isValidByName(Splits.NAME) && !name.equals(Splits.NAME))
            setSplits(null);
        if (SplitsTreeProperties.ALLOW_RETICULATE)
            if (isValidByName(Reticulate.NAME) && !name.equals(Reticulate.NAME))
                setReticulate(null);
        if (isValidByName(Network.NAME) && !name.equals(Network.NAME))
            setNetwork(null);
    }

    /**
     * writes a bootstrap block as a splits block
     *
     * @param splits splits (for  cycle)
     * @param w      writer
     * @throws IOException
     */
    public void writeBS(Splits splits, Writer w) throws IOException {
        if (isValid(bootstrap)) {
            Splits bsplits = new Splits();

            if (splits != null)
                try {
                    bsplits.setCycle(splits.getCycle().clone());
                } catch (SplitsException e) {
                    Basic.caught(e);
                }
            bsplits.getFormat().setWeights(true);
            // bsplits.getFormat().setConfidences(true);
            bsplits.setNtax(bootstrap.getNtax());
            for (int i = 1; i <= bootstrap.getNsplits(); i++)
                bsplits.add(bootstrap.getBsplits().get(i), bootstrap.getBsplits().getWeight(i), bootstrap.getBsplits().getWeight(i));
            bsplits.write(w, taxa);
        }
    }

    /**
     * gets a string representation of this document in Nexus format
     *
     * @return string representation
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            write(sw);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return sw.toString();
    }

    /**
     * produces a string representation of the named blocks
     *
     * @return string representation    of the named blocks
     */
    public String toString(List blocks) {
        StringWriter w = new StringWriter();
        try {
            w.write("#nexus\n");
            if (getTopComments() != null)
                w.write("[!" + getTopComments() + "]\n");

            for (Object block : blocks) {
                String name = (String) block;
                if (isValidByName(name))
                    write(w, name);
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return w.toString();
    }

    /**
     * gets the value of a format switch,
     *
     * @param blockName
     * @param name      of formating switch
     * @return true or false depending on whether the switch is set or not
     */
    public boolean getFormatSwitchValue(String blockName, String name) {
        if (!isValidByName(blockName))
            return true;
        if (blockName.equals(Unaligned.NAME))
            return unaligned.getFormatSwitchValue(name);
        else if (blockName.equals(Characters.NAME))
            return characters.getFormatSwitchValue(name);
        else if (blockName.equals(Distances.NAME))
            return distances.getFormatSwitchValue(name);
        else if (blockName.equals(Quartets.NAME))
            return quartets.getFormatSwitchValue(name);
        else if (blockName.equals(Trees.NAME))
            return trees.getFormatSwitchValue(name);
        else if (blockName.equals(Splits.NAME))
            return splits.getFormatSwitchValue(name);
        else if (blockName.equals(Reticulate.NAME))
            return reticulate.getFormatSwitchValue(name);
        else if (blockName.equals(Bootstrap.NAME))
            return bootstrap.getFormatSwitchValue(name);
        else
            return true;
    }

    /**
     * reads and executes a splitstree block
     *
     * @param np
     * @throws IOException
     * @throws SplitsException
     * @throws CanceledException
     * @throws Exception
     */
    public void read(NexusStreamParser np) throws Exception {
        np.matchBeginBlock("SplitsTree");
        while (!np.peekMatchEndBlock()) {
            parse(np);
        }
        np.matchEndBlock();
    }

    /**
     * Parses and executes commands from a reader
     *
     * @param np the the nexus stream parser
     */
    public void parse(NexusStreamParser np) throws IOException, SplitsException,
            Exception, CanceledException {

        //We are in Expert mode (all commands available)
        boolean isExpert = SplitsTreeProperties.getExpertMode();

        while (np.peekNextToken() != NexusStreamParser.TT_EOF && !np.peekMatchEndBlock()) {
            if (np.peekMatchIgnoreCase("setprop")) {
                np.matchIgnoreCase("setprop");
                String label = np.getWordRespectCase();
                np.matchIgnoreCase("=");
                String value = np.getWordFileNamePunctuation();
                if (NexusStreamParser.isBoolean(value)) {
                    jloda.util.ProgramProperties.put(label, Boolean.parseBoolean(value));
                } else if (NexusStreamParser.isInteger(value)) {
                    jloda.util.ProgramProperties.put(label, Integer.parseInt(value));
                } else if (NexusStreamParser.isFloat(value)) {
                    jloda.util.ProgramProperties.put(label, Float.parseFloat(value));
                } else
                    jloda.util.ProgramProperties.put(label, value);
                np.matchIgnoreCase(";");
            } else if (np.peekMatchIgnoreCase("save")) // save to a file
            {
                List<String> tokens;
                try {
                    np.pushPunctuationCharacters("=;"); // filename punctuation
                    tokens = np.getTokensRespectCase("save", ";");
                } catch (IOException ex) {
                    np.popPunctuationCharacters();
                    throw ex;
                }
                String fname = np.findIgnoreCase(tokens, "file=", null, null);
                if (fname == null)
                    throw new SplitsException("SAVE: Must specify FILE=filename");
                boolean replace = np.findIgnoreCase(tokens, "replace=", "yes no", "no").equals("yes");
                boolean append = np.findIgnoreCase(tokens, "append=", "yes no", "no").equals("yes");

                List blocks = null;
                if (!np.findIgnoreCase(tokens, "data=all", true, false)) {
                    if (np.findIgnoreCase(tokens, "data=", true, false))
                        blocks = tokens; // remaining tokens must be block names
                }
                System.err.print("Writing file " + fname + ":");
                File file = new File(fname);
                if (!replace && file.exists())
                    throw new SplitsException("File exists: " + fname + ", use REPLACE=yes to overwrite");

                if (blocks == null) // save all blocks
                {
                    FileWriter fw = new FileWriter(file, append);
                    assumptions.setUptodate(true);
                    try {
                        write(fw);
                    } catch (Exception ex) {
                        assumptions.setUptodate(false);
                    }
                    fw.close();
                    System.err.print(" all valid data\n");
                    setFile(file);
                    setDirty(false);
                } else // save all named blocks
                {
                    FileWriter fw = new FileWriter(file, append);

                    fw.write("#nexus\n");
                    for (Object block : blocks) {
                        String blockName = (String) block;
                        System.err.print(" " + blockName);
                        if (!write(fw, blockName))
                            throw new SplitsException("Failed to write block: " + blockName);
                    }
                    System.err.print("\n");
                }
            } else if (np.peekMatchIgnoreCase("show;")) // show all to standard output
            {
                np.matchIgnoreCase("show;");
                StringWriter sw = new StringWriter();
                PrintStream ps = jloda.util.Basic.hideSystemErr();
                try {
                    write(sw);
                    System.out.println(sw.toString());
                } finally {
                    jloda.util.Basic.restoreSystemErr(ps);
                }
            } else if (np.peekMatchIgnoreCase("show")) // show named blocks to standard output
            {
                np.matchIgnoreCase("show data");
                List<String> tokens = np.getTokensLowerCase("=", ";");

                StringWriter w = new StringWriter();
                PrintStream ps = jloda.util.Basic.hideSystemErr();

                try {
                    int n = tokens.size();

                    w.write("#nexus\n");
                    if (getTopComments() != null)
                        w.write("[!" + getTopComments() + "]\n");

                    for (String blockName : tokens) {
                        write(w, blockName);
                    }
                    w.flush();

                    System.out.println(w.toString());
                } finally {
                    jloda.util.Basic.restoreSystemErr(ps);
                }
            } else if (np.peekMatchIgnoreCase("help")) // help for named blocks
            {
                if (np.peekMatchIgnoreCase("help;")) {
                    np.matchIgnoreCase("help;");
                    showUsage(System.out);
                } else if (np.peekMatchIgnoreCase("help transform=")) {
                    np.matchIgnoreCase("help transform=");
                    showUsageTransform(np.getWordRespectCase());
                    np.matchIgnoreCase(";");
                } else {
                    np.matchIgnoreCase("help data");
                    List<String> tokens = np.getTokensLowerCase("=", ";");

                    for (String blockName : tokens) {
                        if (!showUsage(blockName))
                            System.err.print("Invalid block-name: " + blockName);
                    }
                }
            } else if (np.peekMatchIgnoreCase("execute")) // open and execute a file
            {
                np.matchIgnoreCase("execute file=");
                String fname = np.getWordFileNamePunctuation();
                np.matchIgnoreCase(";");

                File file = new File(fname);
                NexusStreamParser fp = new NexusStreamParser(new FileReader(file));
                setFile(file);
                setDirty(false);

                try {
                    fp.matchIgnoreCase("#nexus");
                } catch (IOException ex) {
                    throw new SplitsException
                            ("Nexus file must start with '#nexus' statement");
                }
                readNexus(fp);
                try {
                    update();
                } catch (CanceledException ex) {
                    if (isValidByName(ex.getMessage()))
                        deleteDependentBlocks(ex.getMessage());
                    else
                        clear();
                }
            } else if (np.peekMatchIgnoreCase("open")) // open a file
            {
                np.matchIgnoreCase("open file=");
                File file = new File(np.getWordFileNamePunctuation());
                NexusStreamParser fp = new NexusStreamParser(new FileReader(file));
                setFile(file);
                setDirty(false);
                np.matchIgnoreCase(";");
                try {
                    fp.matchIgnoreCase("#nexus");
                } catch (IOException ex) {
                    throw new SplitsException
                            ("NexusBlock file must start with #nexus");
                }
                readNexus(fp);
            } else if (np.peekMatchIgnoreCase("import")) // import a file with no update
            {
                np.matchIgnoreCase("import file=");
                String fname = np.getWordFileNamePunctuation();
                String dataType = Characters.Datatypes.UNKNOWN;
                if (np.peekMatchIgnoreCase("datatype=")) {
                    np.matchIgnoreCase("datatype=");
                    dataType = np.getWordFileNamePunctuation();
                }
                np.matchIgnoreCase(";");
                String input = ImportManager.importData(new File(fname), dataType);
                readNexus(new StringReader(input));
            } else if (np.peekMatchIgnoreCase("load")) // open or import a file
            {
                np.matchIgnoreCase("load");
                if (np.peekMatchIgnoreCase("treefiles=")) {
                    np.matchIgnoreCase("treefiles");
                    List files = np.getTokensRespectCase("=", ";");
                    DocumentUtils.loadMultipleTreeFiles(files, this);
                    setDirty(false);
                } else if (np.peekMatchIgnoreCase("charfiles=")) {
                    np.matchIgnoreCase("charfiles");
                    List files = np.getTokensRespectCase("=", ";");
                    DocumentUtils.concatenateSequences(files, this);
                    setDirty(false);
                } else if (np.peekMatchIgnoreCase("file=")) {
                    np.matchIgnoreCase("file=");
                    String fname = np.getWordFileNamePunctuation();
                    File file = new File(fname);
                    FileReader reader = new FileReader(file);
                    np.matchIgnoreCase(";");
                    if (!NexusFileFilter.isNexusFile(file)) {
                        reader.close();
                        String input = ImportManager.importData(new File(fname));
                        readNexus(new StringReader(input));
                        if (!BlockChooser.show(parent, this)) {
                            throw new CanceledException();
                        }
                        setFile(fname, true);
                        setDirty(false);
                    }   // assume this is some other format that must be imported
                    else {
                        NexusStreamParser fp = new NexusStreamParser(reader);
                        readNexus(fp);
                        reader.close();
                        setFile(file);
                        setDirty(false);

                    }

                } else
                    np.matchAnyTokenIgnoreCase("file treefiles charfiles"); // will die on this
            } else if (np.peekMatchIgnoreCase("export")) // export a file
            {
                List<String> tokens;
                try {
                    np.pushPunctuationCharacters("=;"); // filename punctuation
                    tokens = np.getTokensRespectCase("export", ";");
                } catch (IOException ex) {
                    np.popPunctuationCharacters();
                    throw ex;
                }
                String fname = np.findIgnoreCase(tokens, "file=", null, null);
                if (fname == null)
                    throw new SplitsException("EXPORT: Must specify FILE=filename");
                boolean replace = np.findIgnoreCase(tokens, "replace=", "yes no", "no").equals("yes");
                boolean append = np.findIgnoreCase(tokens, "append=", "yes no", "no").equals("yes");
                boolean complete = np.findIgnoreCase(tokens, "complete=", "yes no", "yes").equals("yes");
                String exporter = np.findIgnoreCase(tokens, "format=", Basic.collection2string(ExportManager.getExportNames()), "Nexus");

                Collection blocks = null;
                if (!np.findIgnoreCase(tokens, "data=all", true, false)) {
                    if (np.findIgnoreCase(tokens, "data=", true, false))
                        blocks = tokens; // remaining tokens must be block names
                }
                if (tokens.size() > 0)
                    throw new SplitsException("EXPORT: unexpected token: " + tokens.get(0));

                if (blocks == null)
                    blocks = getListOfValidBlocks();

                File file = new File(fname);
                if (!replace && file.exists())
                    throw new SplitsException("File exists: " + fname + ", use REPLACE=yes to overwrite");

                ExportManager.exportData(file, append, complete, exporter, blocks, this);
            } else if (np.peekMatchIgnoreCase("draw")) // change draw parameters of graph
            {
                if (network != null) {
                    network.getDraw().read(np);
                }
            } else if (np.peekMatchIgnoreCase("EXPORTGRAPHICS")) {
                List<String> tokens;
                try {
                    np.pushPunctuationCharacters("=;"); // filename punctuation
                    tokens = np.getTokensRespectCase("EXPORTGRAPHICS", ";");
                } catch (IOException ex) {
                    np.popPunctuationCharacters();
                    throw ex;
                }
                String fname = np.findIgnoreCase(tokens, "file=", null, null);
                if (fname == null)
                    throw new SplitsException("EXPORTGRAPHICS: Must specify FILE=filename");
                String format = np.findIgnoreCase(tokens, "format=", "eps svg gif png jpg pdf", "eps");
                boolean replace = np.findIgnoreCase(tokens, "replace=", "yes no", "no").equals("yes");
                boolean text2shapes = np.findIgnoreCase(tokens, "textasshapes=", "yes no", "no").equals("yes");
                String title = np.findIgnoreCase(tokens, "title=", null, "");
                int scale = np.findIgnoreCase(tokens, "size=", 0, 10000, 600);
                np.checkFindDone(tokens);

                File file = new File(fname);
                if (!replace && file.exists())
                    throw new SplitsException("File exists: " + fname + ", use REPLACE=yes to overwrite");

                if (!isValid(getNetwork()))
                    throw new SplitsException("EXPORTGRAPHICS: invalid network block (try 'update' first)");

                PhyloGraphView graphView = new PhyloGraphView();
                graphView.setSize(scale, scale);
                graphView.getScrollPane().setSize(scale, scale);
                graphView.trans.reset();
                getNetwork().syncNetwork2PhyloGraphView(getTaxa(), getSplits(), graphView);
                graphView.trans.setCoordinateRect(graphView.getBBox());
                graphView.trans.fitToSize(graphView.getBBox(), new Dimension(scale, scale));
                graphView.centerGraph();
                graphView.setCanvasColor(Color.white);

                String oldPoweredBy = graphView.getPOWEREDBY();
                graphView.setPOWEREDBY(title);
                graphView.setAutoLayoutLabels(getAssumptions().getAutoLayoutNodeLabels());

                try {
                    if (format.equalsIgnoreCase("eps")) {
                        if (text2shapes)
                            EPSExportType.writeToFile(file, graphView, EPSGraphics.FONT_OUTLINES);
                        else
                            EPSExportType.writeToFile(file, graphView, EPSGraphics.FONT_TEXT);
                    } else if (format.equalsIgnoreCase("png"))
                        PNGExportType.writeToFile(file, graphView);
                    else if (format.equalsIgnoreCase("pdf"))
                        PDFExportType.writeToFile(file, graphView);
                    else if (format.equalsIgnoreCase("svg"))
                        SVGExportType.writeToFile(file, graphView);
                    else if (format.equalsIgnoreCase("gif"))
                        GIFExportType.writeToFile(file, graphView);
                    else if (format.equalsIgnoreCase("jpg"))
                        JPGExportType.writeToFile(file, graphView);

                    System.err.println(format + " written to file: " + file.toString());
                } finally {
                    graphView.setPOWEREDBY(oldPoweredBy);
                }
            } else if (np.peekMatchIgnoreCase("update;")) // update
            {
                np.matchIgnoreCase("update;");
                try {
                    update();
                } catch (CanceledException ex) {
                    if (isValidByName(ex.getMessage()))
                        deleteDependentBlocks(ex.getMessage());
                    else
                        clear();
                }
            } else if (np.peekMatchIgnoreCase("update cycle;")) // update the cycle
            {
                np.matchIgnoreCase("update cycle;");
                if (taxa != null && splits != null) {
                    SplitsUtilities.computeCycle(this, taxa, splits, assumptions.getLayoutStrategy());
                    update(Splits.NAME);
                }
            } else if (np.peekMatchIgnoreCase("update")) // update a specific block
            {
                np.matchIgnoreCase("update");
                String name = np.getWordRespectCase();
                np.matchIgnoreCase(";");
                update(name);
            } else if (np.peekMatchIgnoreCase("assume")) // set an assumption
            {
                String block = np.convertToBlock("assume", ";", Assumptions.NAME);
                readNexus(new StringReader(block));
                // System.err.println("first dirty: "+assumptions.getFirstDirtyBlock());
                update(assumptions.getFirstDirtyBlock());
            } else if (np.peekMatchIgnoreCase("cycle")) // set the cycle
            {
                np.matchIgnoreCase("cycle");
                boolean keep = false;

                if (np.peekMatchIgnoreCase("keep")) //
                {
                    np.matchIgnoreCase("keep");
                    keep = true;
                    assumptions.setLayoutStrategy(Assumptions.KEEP);
                }

                String block = "begin st_splits;cycle ";
                while (!np.peekMatchIgnoreCase(";")) block += " " + np.getInt(1, taxa.getNtax());
                np.matchIgnoreCase(";");
                block += ";end;";
                readNexus(new StringReader(block));
                update(Splits.NAME);
                if (keep)
                    SplitsUtilities.setPreviousTaxaSplits(taxa, splits);
            } else if (np.peekMatchIgnoreCase("bootstrap")) // run a bootstrap test
            {
                List<String> tokens = np.getTokensLowerCase("bootstrap", ";");
                int runs = np.findIgnoreCase(tokens, "runs=", 1, 10000000, 1000);
                int seed = np.findIgnoreCase(tokens, "seed=", -1000000, 1000000, 0);
                boolean same = np.findIgnoreCase(tokens, "length=same", true, false);
                int length = same ? -1 : np.findIgnoreCase(tokens, "length=", 1, 10000000, -1);
                boolean fixNet = np.findIgnoreCase(tokens, "fixNet=yes", true, false);
                boolean saveWeights = np.findIgnoreCase(tokens, "saveWeights=yes", true, false);
                boolean saveTrees = np.findIgnoreCase(tokens, "saveTrees=yes", true, false);
                String outputFile = np.findIgnoreCase(tokens, "file=", null, null);

                if (tokens.size() > 0)
                    throw new SplitsException("Illegal option(s) in bootstrap command: " + tokens);

                StringBuilder block = new StringBuilder();
                block.append("begin " + Bootstrap.NAME + ";\n");

                block.append("runs=").append(runs).append(";\n");
                if (length <= 0)
                    block.append("length=same;\n");
                else
                    block.append("length=").append(length).append(";\n");
                block.append("seed=");
                block.append(seed);
                block.append(";\n");
                if (fixNet)
                    block.append("fixsplits=yes;\n");
                if (saveWeights)
                    block.append("saveweights=yes; \n");

                if (outputFile != null && outputFile.length() > 0)
                    block.append("OutputFile=\'").append(outputFile).append("\';\n");

                block.append("end;\n");
                setBootstrap(null);
                readNexus(new StringReader(block.toString()));
                //TODO: Maybe this option should be added to the Bootstrap NEXUS syntax?
                getBootstrap().setSaveTrees(saveTrees);

                try {
                    System.err.println("Bootstrapping...");
                    getBootstrap().compute(this);
                    System.err.println("done");
                    /*
                        TODO: There is a bug here - if we just call:
                        */
                    update(Bootstrap.NAME);
                    /*
                    then the Bootstrap block is wiped, due to the code that handles included and excluded taxa
                    and characters.
                    */
                } catch (CanceledException ex) {
                    setBootstrap(null);
                }
                if (getBootstrap() != null && getAnalysis() != null)
                    getAnalysis().apply(this, getTaxa(), Bootstrap.NAME);
            } else if (np.peekMatchIgnoreCase("analysis")) // does analysis
            {
                String block = np.convertToBlock("analysis", ";", Analysis.NAME);
                readNexus(new StringReader(block));
                if (getAnalysis() != null) {
                    getAnalysis().apply(this);
                }

            } else if (np.peekMatchIgnoreCase("invariant")) {
                //TODO: TEMPORARY PATCH - have to integrate better into program
                np.matchIgnoreCase("invariant;");

                CaptureRecapture captureRecapture = new CaptureRecapture();
                if (captureRecapture.isApplicable(this)) {
                    double pinv = (new CaptureRecapture()).estimatePinv(getCharacters());
                    System.err.println("Proportion Invariant Sites = " + pinv);
                }
            } else if (np.peekMatchIgnoreCase("treeness")) {
                //TODO: TEMPORARY PATCH - have to integrate better into program
                np.matchIgnoreCase("treeness;");

                TestTreeness treeness = new TestTreeness();
                if (treeness.isApplicable(this)) { //, getTaxa(), getBootstrap())) {
                    String result = treeness.apply(this);
                    System.err.println(result);
                }
            } else if (np.peekMatchIgnoreCase("phiTest")) {
                np.matchIgnoreCase("phiTest;");
                PhiTest phiTest = new PhiTest();
                if (phiTest.isApplicable(this, getTaxa(), getCharacters())) {
                    String result = phiTest.apply(this, getTaxa(), getCharacters());
                    System.err.println(result);
                }
            } else if (np.peekMatchIgnoreCase("estimateSigma")) {
                List<String> tokens = np.getTokensLowerCase("estimateSigma", ";");
                int nreps = np.findIgnoreCase(tokens, "nreps=", 2, 1000, 100);
                double sigma = DistancesUtilities.estimateSigma(this, (Characters2Distances) getAssumptions().getCharactersTransform(), nreps);
                System.out.println("Estimate of sigma is " + sigma);
            } else if (isExpert && np.peekMatchIgnoreCase("simulateData")) {
                List<String> tokens = np.getTokensLowerCase("simulateData", ";");

//                np.matchIgnoreCase("simulateData");
                int ntax = np.findIgnoreCase(tokens, "ntax=", 2, 1000, 10);
                ntax = np.findIgnoreCase(tokens, "ntaxa=", 2, 1000, ntax);

                double stdev = np.findIgnoreCase(tokens, "stdev=", 0.0, 100.0, 0.0);

                Document newDoc = new Document();
                SimulationExperiments.simulateData(newDoc, ntax, 0.1, 0.0, stdev);
                StringWriter sw = new StringWriter();
                newDoc.getTaxa().write(sw);
                newDoc.getDistances().write(sw, newDoc.getTaxa());


                Director newDir = Director.newProject(sw.toString(), "test");
                newDir.getDocument().setTitle("Gaussian random distances " + this.getTitle());
                newDir.showMainViewer();


            } else if (np.peekMatchIgnoreCase("simulateLARSPaper")) {
                List<String> tokens = np.getTokensRespectCase("simulateLARSPaper", ";");

//                np.matchIgnoreCase("simulateData");
                int ntax = np.findIgnoreCase(tokens, "ntax=", 2, 1000, 10);
                ntax = np.findIgnoreCase(tokens, "ntaxa=", 2, 1000, ntax);

                int power = np.findIgnoreCase(tokens, "power=", 0, 2, 0);
                String[] varOptions = {"OrdinaryLeastSquares", "FitchMargoliash1", "FitchMargoliash2"};
                String var = varOptions[power];

                double noise = np.findIgnoreCase(tokens, "noise=", 0.0, 100.0, 0.0);

                double height = np.findIgnoreCase(tokens, "height=", 0.0, 100.0, 0.1);
                double balance = np.findIgnoreCase(tokens, "balance=", 0.0, 5.0, 0.0);

                double sigma = np.findIgnoreCase(tokens, "sigma=", 0.01, 1000.0, 0.1);

                int nblocks = np.findIgnoreCase(tokens, "nblocks=", 1, 10, 1);


                int seed = np.findIgnoreCase(tokens, "seed=", 0, Integer.MAX_VALUE, 42);

                int reps = np.findIgnoreCase(tokens, "reps=", 1, 10000, 100);

                String filenameBase = np.findIgnoreCase(tokens, "filename=", "\"", "\"", "larsSim");
                filenameBase = np.findIgnoreCase(tokens, "file=", "\"", "\"", filenameBase);

                //Remove spaces inserted between these tokens
                filenameBase = filenameBase.replaceAll(" ", "");


                boolean shortHeader = np.findIgnoreCase(tokens, "shortHeader");

                boolean longHeader = np.findIgnoreCase(tokens, "longHeader");

                //SimulationForLarsPaper.apply(ntax,height,balance,noise,nblocks, var,reps,filenameBase,longHeader,shortHeader,seed);


            } else if (isExpert && np.peekMatchIgnoreCase("haplotypes")) {
                np.matchIgnoreCase("haplotypes");
                Document newDoc = CharactersUtilities.collapseByType(taxa, characters, distances, this);
                setTaxa(newDoc.getTaxa());
                setCharacters(newDoc.getCharacters());
                setDistances(newDoc.getDistances());
                setTitle(newDoc.getTitle());
                setDirty(true);
                update();

            } else if (isExpert && np.peekMatchIgnoreCase("multiGene")) {
                np.matchIgnoreCase("multigene");
                if (getSets() != null && getSets().getNumCharPartitions() > 0) {
                    Sets sets = getSets();
                    String name = np.getWordRespectCase();
                    Partition partition = sets.getCharPartition(name);
                    if (partition == null)
                        new Alert("There is no partition with name " + name);
                    else {
                        SplitMatrix M = MultiGeneAnalysis.multiGene(this, partition);
                        for (int j = 1; j <= M.getNblocks(); j++)
                            System.out.print("\t" + partition.getBlockName(j));
                        for (int i = 1; i <= M.getNsplits(); i++) {
                            System.out.print(i);
                            for (int j = 1; j <= M.getNblocks(); j++) {
                                System.out.print("\t" + M.get(i, j));
                            }
                            System.out.println();
                        }
                    }
                }
                np.matchIgnoreCase(";");
            } else if (np.peekMatchIgnoreCase("version;")) {
                np.matchIgnoreCase("version;");
                System.out.println(SplitsTreeProperties.getVersion());
            } else if (np.peekMatchIgnoreCase("deleteexcluded;")) {
                np.matchIgnoreCase("deleteexcluded;");
                int numRemoved = 0;
                if (isValidByName(Characters.NAME)) {
                    numRemoved = getCharacters().removeMaskedSites();
                }
                System.err.println("Delete excluded sites: " + numRemoved + " deleted");
                if (numRemoved > 0) {
                    assumptions.setExChar(new LinkedList<Integer>());
                    assumptions.setExcludeCodon1(false);
                    assumptions.setExcludeCodon2(false);
                    assumptions.setExcludeCodon3(false);
                    assumptions.setExcludeGaps(false);
                    assumptions.setExcludeConstant(0);
                    assumptions.setExcludeMissing(1.0);   //1.0 -> no characters excluded
                    assumptions.setExcludeNonParsimony(false);
                    update(Characters.NAME);
                }
            } else if (np.peekMatchIgnoreCase("about;")) {
                String message = SplitsTreeProperties.getVersion() +
                        "\n\nVisit http://www.splitstree.org\n\n" +
                        "Daniel Huson (huson@informatik.uni-tuebingen.de)\nDavid Bryant (david.bryant@otago.ac.nz)\n\n" +
                        "Additional programming:\nMarkus Franz\nMig\374el Jett\351\nTobias Kl\366pper\nMichael Schr\366der";

                np.matchIgnoreCase("about;");
                System.out.println(message);
            } else if (np.peekMatchIgnoreCase("quit;")) // quit the program
            {
                np.matchIgnoreCase("quit;");
                System.exit(0);
            } else if (np.peekMatchIgnoreCase(";")) // empty command
            {
                np.matchIgnoreCase(";");
            } else if (np.peekNextToken() != NexusStreamParser.TT_EOF)
                throw new IOException("Unrecognized command: " + np.getWordRespectCase());
        }
    }


    /**
     * Parses and executes commands from a string
     *
     * @param str the string
     */
    public void parse(String str) throws Exception {
        parse(new NexusStreamParser(new StringReader(str + ";")));
    }

    /**
     * Show the command line usage
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("Summary of commands applicable in commandline-mode:");
        ps.println("\tEXECUTE FILE=file - open and execute a file in Nexus-format");
        ps.println("\tOPEN FILE=file - open (but don't execute) a file in Nexus-format");
        ps.println("\tIMPORT FILE=file [DATATYPE={PROTEIN|RNA|DNA|STANDARD|UNKNOWN}]");
        ps.println("\t\t- open (but don't execute) a file in non-Nexus or old-Nexus format");
        ps.println("\tLOAD FILE=file - open or import a file");
        ps.println("\tLOAD TREEFILES=file1 .. filen - load trees from a list of files");
        ps.println("\tLOAD CHARFILES=file1 .. filen - concatenate sequences from a list of files");
        ps.println("\tSAVE FILE=file [REPLACE={YES|NO}] [APPEND={YES|NO}] [DATA={ALL|list-of-blocks}]");
        ps.println("\t\t- save all data or named blocks to a file in Nexus format");
        ps.println("\tEXPORT FILE=file FORMAT=format [REPLACE={YES|NO}] [APPEND={YES|NO}] [DATA=list-of-blocks]");
        ps.println("\t\t - export data in the named format");
        ps.println("\tEXPORTGRAPHICS [format={EPS|PNG|GIF|JPG|SVG|PDF}] [REPLACE={YES|NO}] [TEXTASSHAPES={YES|NO}]\n" +
                "\t               [TITLE=title] [SIZE=number] file=file");
        ps.println("\t\t - export graphics in specified format (default format is EPS, default size is 600)");
        ps.println("\tUPDATE - rerun computations to bring data up-to-date");
        ps.println("\tBOOTSTRAP RUNS=number-of-runs - perform bootstrapping on character data");
        ps.println("\tDELETEEXCLUDED; - delete all sites from characters block that are currently excluded");
        ps.println("\tASSUME assumption - set an assumption, which can be anything contained in the ST_ASSUMPTIONS block");
        ps.println("\tSHOW [DATA=list-of-blocks] - show the named data blocks");
        ps.println("\tCYCLE {KEEP|cycle} - set the graph layout cycle to KEEP or to a given cycle");
        ps.println("\tHELP - show this info");
        ps.println("\tHELP DATA=list-of-blocks - show syntax of named blocks");
        ps.println("\tHELP TRANSFORM=transform - show usage of a specific data transform, e.g. NeighborNet");
        ps.println("\tVERSION - report version");
        ps.println("\tABOUT - show info on version and authors");
        ps.println("\tQUIT - exit program");
        ps.println("\n\tTo begin or end a multi-line input, enter a backslash '\\'");
    }


    /**
     * Parses and executes commands from a string
     *
     * @param str the string
     */
    public void execute(String str) throws IOException, SplitsException,
            Exception, CanceledException {
        execute(new StringReader(str + ";"));
    }

    /**
     * Parses and executes commands from a reader
     *
     * @param r the reader
     */
    private void execute(Reader r) throws Exception {
        NexusStreamParser np = new NexusStreamParser(r);

        if (np.peekMatchAnyTokenIgnoreCase("#nexus begin beginblock"))
            readNexus(np);
        else {
            do {
                parse(np);
            } while (np.peekNextToken() != NexusStreamParser.TT_EOF);
        }
    }

    /**
     * sets the parent for alert windows etc
     *
     * @param parent
     */
    public void setParent(Component parent) {
        this.parent = parent;
    }

    /**
     * gets the parent
     *
     * @return parent used for alert messages in gui mode
     */
    public Component getParent() {
        return parent;
    }

    static private final String processed = "Processed by SplitsTree4";

    /**
     * gets all top comments found in a nexus file
     *
     * @return first comment
     */
    public String getTopComments() {
        if (topComments != null && !topComments.contains(processed))
            return topComments + "\n" + processed;
        else
            return topComments;
    }

    /**
     * sets the the top comments found in a nexus file
     *
     * @param topComments
     */
    public void setTopComments(String topComments) {
        this.topComments = topComments;
    }
}

// EOF
