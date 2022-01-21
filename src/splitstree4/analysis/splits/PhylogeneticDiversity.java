/*
 * PhylogeneticDiversity.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.analysis.splits;


import splitstree4.core.Document;
import splitstree4.core.SplitsException;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Distances;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * Computes the phylogenetic diversity of a set of taxa
 * Daniel Huson and David Bryant
 */
public class PhylogeneticDiversity implements SplitsAnalysisMethod {
    final static public String DESCRIPTION = "Computes the phylogenetic diversity for a set of taxa (Faith, 1992)";
    int[] optionSelectedTaxa = null;

    /**
     * gets a description of the method
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Runs the analysis
     *
     * @param taxa   the taxa
     * @param splits the block
     */
    public String apply(Document doc, Taxa taxa, Splits splits) throws Exception {
        TaxaSet selectedTaxa = new TaxaSet();

        if (getOptionSelectedTaxa() != null) {
            for (int i = 0; i < getOptionSelectedTaxa().length; i++) {
                int t = getOptionSelectedTaxa()[i];
                if (t < 1 || t > taxa.getNtax())
                    throw new SplitsException("Taxon-id out of range: " + getOptionSelectedTaxa()[i]);
                selectedTaxa.set(t);
            }
        }
        if (selectedTaxa.cardinality() < 2)
            return "Phylogenetic diversity=0 (because fewer than 2 taxa selected)";

        float total = 0;
        float diversity = 0;
        for (int s = 1; s <= splits.getNsplits(); s++) {
            TaxaSet intersection = new TaxaSet();
            intersection.or(splits.get(s));
            intersection.and(selectedTaxa);
            total += splits.getWeight(s);
            if (intersection.cardinality() > 0 && intersection.cardinality() < selectedTaxa.cardinality())
                diversity += splits.getWeight(s);
        }
        String result = "Phylogenetic diversity=" + diversity;
        if (total > 0)
            result += "\nPercentage of total=" + (float) ((int) (1000.0 * diversity / total) / 10.0) + "%";

        //Now compute the actual diversity = average distance.
        Distances dist = doc.getDistances();
        if (dist != null) {
            int npairs = 0;
            double totalDist = 0.0;
            for (int i = 0; i < getOptionSelectedTaxa().length; i++) {
                int taxon1 = getOptionSelectedTaxa()[i];
                for (int j = 0; j < i; j++) {
                    int taxon2 = getOptionSelectedTaxa()[j];
                    totalDist += dist.get(taxon1, taxon2);
                    npairs++;
                }
            }
            result += "\n Average distance=" + totalDist / npairs;
        }


        return result;
    }

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa   the taxa
     * @param splits the block
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Splits splits) {
        return doc.isValid(taxa) && doc.isValid(splits)
                && doc.getSplits().getProperties().getCompatibility() == Splits.Properties.COMPATIBLE;
    }

    public int[] getOptionSelectedTaxa() {
        return optionSelectedTaxa;
    }

    public void setOptionSelectedTaxa(int[] selectedTaxa) {
        this.optionSelectedTaxa = selectedTaxa;
    }
}
