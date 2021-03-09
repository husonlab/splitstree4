/**
 * GascuelGamma.java
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
package splitstree4.analysis.characters;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import splitstree4.algorithms.characters.SequenceBasedDistance;
import splitstree4.algorithms.distances.BioNJ;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Trees;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * GascuelGamma
 * <p/>
 * Estimation of the gamma alpha parameter, using the distance based method
 * developed by
 * <p/>
 * Guindon, S. and Gascuel, O. (2002) Efficient biased estimation of evolutionary distances
 * when substitution rates vary across sites. Mol. Biol. Evol. 19:534-543
 * <p/>
 * Currently we use an O(n^3) algorithm. This could be tediously improved to
 * O(n^2) using the O(n^2) algorithm to computer A'WA - if this procedure
 * proves to be a bottleneck (e.g. in a bootstrap).
 */
public class GascuelGamma implements CharactersAnalysisMethod {

    public static String DESCRIPTION = "Estimates alpha parameter for gamma distribution (Guindon & Gascual 2002)";

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * getEdgeSplits. For every edge in the tree, we construct a taxa set containing
     * those taxa on the same side of the edge as the target vertex. This is stored
     * in edgeSplits.
     *
     * @param T
     * @param taxa
     * @param edgeSplits
     */
    static private void getEdgeSplits(PhyloTree T, Taxa taxa, Map edgeSplits) {
        Trees trees = new Trees("", T, taxa);
        if (T.getNumberOfEdges() > 0) {

            Edge e = T.getFirstEdge();
            Node v = e.getSource();
            Node w = e.getTarget();
            getEdgeSplitsRec(T, edgeSplits, trees, taxa, v, e);
            TaxaSet eset = getEdgeSplitsRec(T, edgeSplits, trees, taxa, w, e);
            edgeSplits.put(e, eset);
        }
    }

    /**
     * Recursive procedure used in getEdgeSplits
     *
     * @param T
     * @param edgeSplits
     * @param trees
     * @param taxa
     * @param r          root of the subtree
     * @param e          edge connecting subtree to rest of tree.
     * @return
     */
    static private TaxaSet getEdgeSplitsRec(PhyloTree T, Map edgeSplits, Trees trees, Taxa taxa, Node r, Edge e) {

        final TaxaSet set;
        if (r.getDegree() == 1) {
            set = trees.getTaxaForLabel(taxa, T.getLabel(r));
        } else // degree >=2
        {
            set = new TaxaSet();
            for (Edge f : r.adjacentEdges()) {
                if (f == e)
                    continue;
                Node v = r.getOpposite(f);
                TaxaSet vset = getEdgeSplitsRec(T, edgeSplits, trees, taxa, v, f);
                set.or(vset);
                if (v != f.getTarget())
                    vset.getComplement(taxa.getNtax());
                edgeSplits.put(f, vset);

            }
        }
        return set;
    }

    static private double averageDist(Distances dist, TaxaSet X, TaxaSet Y) {
        int ntax = dist.getNtax();
        double sum = 0.0;
        int count = 0;
        for (int x = 1; x <= ntax; x++) {
            if (!X.get(x))
                continue;
            for (int y = 1; y <= ntax; y++) {
                if (!Y.get(y))
                    continue;
                sum += dist.get(x, y);
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Compute the Q value for an edge (see Guindon and Gascuel 2002)
     * <p/>
     * if the subtrees as A B | C D
     * then the Q-score is | d(A,C) + d(B,C) - d(A,D) - d(B,C) |
     * where d(X,Y) is the average distance from taxa in X to taxa in Y
     *
     * @param T
     * @param edgeSplits
     * @param dist
     * @param e
     * @return
     */

    static private double edgeQ(PhyloTree T, Map edgeSplits, Distances dist, Edge e) {
        Node v = e.getSource();
        Node w = e.getTarget();
        Edge a, b, c, d;

        //If we are at a leaf, then Q value is zero,
        if (v.getDegree() < 3)
            return 0;
        if (w.getDegree() < 3)
            return 0;

        //Locate the four edges

        {
            final Iterator<Edge> it = v.adjacentEdges().iterator();
            a = it.next();
            if (a == e)
                a = it.next();
            b = it.next();
            if (b == e)
                b = it.next();
        }

        {
            final Iterator<Edge> it = w.adjacentEdges().iterator();
            c = it.next();
            if (c == e)
                c = it.next();
            d = it.next();
            if (d == e)
                d = it.next();
        }


        int ntax = dist.getNtax();

        //Construct the four sets.


        TaxaSet A = (TaxaSet) edgeSplits.get(a);
        if (v != a.getSource())
            A = A.getComplement(ntax);

        TaxaSet B = (TaxaSet) edgeSplits.get(b);
        if (v != b.getSource())
            B = B.getComplement(ntax);

        TaxaSet C = (TaxaSet) edgeSplits.get(c);
        if (w != c.getSource())
            C = C.getComplement(ntax);

        TaxaSet D = (TaxaSet) edgeSplits.get(d);
        if (w != d.getSource())
            D = D.getComplement(ntax);


        return (Math.abs(averageDist(dist, A, C) + averageDist(dist, B, D)
                - averageDist(dist, A, D) - averageDist(dist, B, C)));

    }


    static private double evalQscore(PhyloTree T, Taxa taxa, Distances dist) {

        Map edgeSplits = new HashMap();
        getEdgeSplits(T, taxa, edgeSplits);
        double Qsum = 0.0;

        Edge e = T.getFirstEdge();
        do {
            Qsum += edgeQ(T, edgeSplits, dist, e);
            e = T.getNextEdge(e);
        } while (e != T.getLastEdge());

        return Qsum;

    }

    static private double evalGammaParam(double alpha,
                                         Taxa taxa,
                                         Characters characters,
                                         SequenceBasedDistance distTransform) {

        distTransform.setOptionGamma(alpha);
        Distances dist = distTransform.computeDist(characters);
        BioNJ bionj = new BioNJ();
        Trees trees = bionj.computeTrees(taxa, dist);
        PhyloTree T = trees.getTree(1);
        return evalQscore(T, taxa, dist);
    }


    static public double estimateGamma(Taxa taxa, Characters characters, SequenceBasedDistance distTransform) {


        double a, b, tau, aa, bb, faa, fbb;
        tau = 2.0 / (1.0 + Math.sqrt(5.0)); //Golden ratio
        double GS_EPSILON = 0.000001;
        double gmin = 0.1;
        double gmax = 100;


        a = gmin;
        b = gmax;
        aa = a + (1.0 - tau) * (b - a);
        bb = a + tau * (b - a);
        faa = evalGammaParam(aa, taxa, characters, distTransform);
        fbb = evalGammaParam(bb, taxa, characters, distTransform);

        while ((b - a) > GS_EPSILON) {
            // cout<<"["<<a<<","<<aa<<","<<bb<<","<<b<<"] \t \t ("<<faa<<","<<fbb<<")"<<endl;

            if (faa < fbb) {
                b = bb;
                bb = aa;
                fbb = faa;
                aa = a + (1.0 - tau) * (b - a);
                faa = evalGammaParam(aa, taxa, characters, distTransform);
                //System.out.println("faa was the smallest at this iteration :" + faa);
            } else {
                a = aa;
                aa = bb;
                faa = fbb;
                bb = a + tau * (b - a);
                fbb = evalGammaParam(bb, taxa, characters, distTransform);
                //System.out.println("fbb was the smallest at this iteration :" + fbb);
            }
        }

        return b;
    }

    static public boolean isSequenceDist(Document doc) {
        return (SequenceBasedDistance.class.isInstance(doc.getAssumptions().getCharactersTransform()));
    }

    /**
     * Check if the Gascuel/Guindon GAMMA method is applicable.
     *
     * @param doc NEXUS document
     * @return true if distances were calculated from sequences
     * @throws Exception
     */
    public boolean isApplicable(Document doc) {
        return SequenceBasedDistance.class.isInstance(doc.getAssumptions().getCharactersTransform()) && doc.getCharacters() != null;

    }

    /**
     * Estimate the gamma distribution shape parameter using the Gascuel/Guindon method
     *
     * @param doc NEXUS document
     * @return Estimated shape parameter
     * @throws Exception
     */
    public String apply(Document doc) {
        double alpha = estimateGamma(doc.getTaxa(), doc.getCharacters(), (SequenceBasedDistance) doc.getAssumptions().getCharactersTransform());
        String result = "Estimated gamma distribution parameter= ";

        if (alpha > 99.99)
            result += "no rate variation";
        else
            result += "" + (float) alpha;

        return result;
    }

}
