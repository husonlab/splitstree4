/*
 * Coalescent.java Copyright (C) 2022 Daniel H. Huson
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
/**
 * @version $Id: Coalescent.java,v 1.11 2007-09-11 12:31:09 kloepper Exp $
 *
 * @author tobias dezulian
 *
 */
package splitstree4.algorithms.quartets;


import splitstree4.core.Document;
import splitstree4.core.Quartet;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Quartets;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

import java.util.Hashtable;

/**
 * Transforms a set of quartets Q to a set of splits S representing a
 * resolved binary phylogenetic tree by applying the
 * coalescent [Mossel/Steel 2003] method.
 * Resulting splits are of this kind:
 * { A|B:  A union B = X, A intersects B = emptySet,
 * for all a element A
 * for all b element B
 * there is no quartet ab | xy element of Q
 * (i.e. there is no quartet which is incompatible with the split)
 * }
 * <p/>
 * "coalescent method" algorithm:
 * <p/>
 * 1) find a cherry
 * 1a) arbitrarily pick two taxa and check wheather any split
 * puts them on different sides.
 * if none exists then a cherry is found => 2)
 * 1b) try two different taxa and check, repeat
 * 1c) if no taxa are left and no cherry has been found, STOP.
 * 2) coalesce the two taxa into one new taxon and report the split found
 * 3) repeat 1)
 */
public class Coalescent implements Quartets2Splits {

    public static final String DESCRIPTION = "Computes a tree by applying the \"coalescent\" method E. Mossel and M. Steel (2003)";
    private final boolean logging = false;
    //global fields for return value exchange with submethods
    private int cherryPartX = -1; //one taxon of a found cherry
    private int cherryPartY = -1; //one taxon of a found cherry
    /**
     * Since taxa are coalesced the two old taxa are declared invalid and
     * the freshly added one is made valid.
     */
    private TaxaSet validTaxa = null;

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa     the taxa
     * @param quartets the quartets
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Quartets quartets) {
        return true;
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa     the taxa
     * @param quartets the given quartets
     * @return the computed quartets
     */
    public Splits apply(Document doc, Taxa taxa, Quartets quartets) {
        taxa = (Taxa) taxa.clone();
        try {
            Splits splits = new Splits(taxa.getNtax());
            int originalNumberOfTaxa = taxa.getNtax();

            /**
             * start with all trivial splits
             */
            for (int i = 1; i <= taxa.getNtax(); i++) {
                TaxaSet ts = new TaxaSet();
                ts.set(i);
                splits.add(ts);
            }

            //at the beginning all taxa are valid
            validTaxa = new TaxaSet();
            validTaxa.set(1, taxa.getNtax());

            //maps coalesced taxa to the contained singletons (TaxaSet)
            Hashtable coalescenceMap = new Hashtable();

            boolean foundCherry = findACherry(taxa, quartets);
            while (foundCherry) {
                /**
                 * now we know that there is no quartet which puts
                 * x and y on different sides.
                 * let's now
                 *  + create a new taxon which results from the coalescence of
                 *    the two taxons of the cherry
                 *  + insert a split: xy|allTheRest
                 *  + remove every quartet which has got x and y on the same side
                 *    [since that is superfluous information]
                 *  + change y to x in every remaining quartet
                 * thus coalescing the cherry into a new taxon.
                 */

                //create new valid taxon z for this cherry
                int z = taxa.getNtax() + 1;
                taxa.setNtax(z);
                taxa.setLabel(taxa.getNtax()
                        , "[" + taxa.getLabel(cherryPartX) + "+" + taxa.getLabel(cherryPartY) + "]");
                validTaxa.set(z);

                //update coalescenceMap
                TaxaSet coalescedTaxa = new TaxaSet();
                if (cherryPartX > originalNumberOfTaxa)
                    coalescedTaxa.or((TaxaSet) coalescenceMap.get((int) (cherryPartX)));
                else
                    coalescedTaxa.set(cherryPartX);
                if (cherryPartY > originalNumberOfTaxa)
                    coalescedTaxa.or((TaxaSet) coalescenceMap.get((int) (cherryPartY)));
                else
                    coalescedTaxa.set(cherryPartY);
                coalescenceMap.put(z, coalescedTaxa);
                if (logging)
                    System.out.println("creating new taxon: " + z + " containing: " + coalescedTaxa.toString());

                //insert the new split
                splits.add(coalescedTaxa);
                if (logging) System.out.println("+ adding split: " + coalescedTaxa.toString());

                //delete these taxa from the valid ones since they have been
                //replaced by the new taxon
                validTaxa.unset(cherryPartX);
                validTaxa.unset(cherryPartY);

                //update references to the old taxa in all quartets
                //and kick out superfluous quartets
                Quartets newQuartets = new Quartets();
                for (int i = 1; i <= quartets.size(); i++) {
                    Quartet q = quartets.get(i);
                    if (q.isXYonSameSides(cherryPartX, cherryPartY)) {
                        //do not add to new quartets since superfluous
                        if (logging) System.out.println("dropping: " + q.toString());
                    } else {
                        //update references to cherryPartX or cherryPartY
                        //to the new taxon z and then insert the quartet into
                        //newQuartets
                        int a1, a2, b1, b2;
                        if ((q.getA1() == cherryPartX) || (q.getA1() == cherryPartY)) {
                            a1 = z;
                        } else
                            a1 = q.getA1();
                        if ((q.getA2() == cherryPartX) || (q.getA2() == cherryPartY)) {
                            a2 = z;
                        } else
                            a2 = q.getA2();
                        if ((q.getB1() == cherryPartX) || (q.getB1() == cherryPartY)) {
                            b1 = z;
                        } else
                            b1 = q.getB1();
                        if ((q.getB2() == cherryPartX) || (q.getB2() == cherryPartY)) {
                            b2 = z;
                        } else
                            b2 = q.getB2();
                        Quartet qNew = new Quartet(a1, a2, b1, b2, q.getWeight(), q.getLabel());
                        if (logging) System.out.println("adding: " + qNew.toString());
                        newQuartets.add(qNew);
                    }
                }
                quartets = newQuartets;
                if (logging) System.out.println("");
                foundCherry = findACherry(taxa, quartets);

            }

            return splits;
        } catch (SplitsException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Tries to find a cherry among the taxa which is compatible
     * with the given set of quartets.
     * If a cherry exists it is found and is returned in the global
     * cherryPartX and cherryPartY fields.
     *
     * @param taxa
     * @param quartets
     * @return returns wheather a cherry was found
     */
    private boolean findACherry(Taxa taxa, Quartets quartets) {
        try {
            if (validTaxa.cardinality() < 4) return false;
            for (int i = 1; i <= taxa.getNtax(); i++) {
                //if this taxon has been deleted then continue
                if (!validTaxa.get(i)) continue;
                for (int j = i + 1; j <= taxa.getNtax(); j++) {
                    if (!validTaxa.get(j)) continue;
                    boolean cherryFound = true;
                    for (int q = 1; q <= quartets.size(); q++) {
                        Quartet quart = quartets.get(q);
                        /**
                         * is this quartet compatible with the two taxa being
                         * a cherry ?
                         */
                        if (quart.isXYonDifferentSides(i, j)) {
                            cherryFound = false;
                            break;
                        }
                    }
                    if (cherryFound) {
                        cherryPartX = i;
                        cherryPartY = j;
                        if (logging) System.out.println("** found cherry: " + i + "," + j);
                        return true;
                    }
                }
            }
        } catch (SplitsException e) {
            e.printStackTrace();
        }

        return false;

    }

}

// EOF
