/*
 * RYSplits.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.algorithms.characters;

import jloda.util.CanceledException;
import splitstree4.core.Document;
import splitstree4.core.TaxaSet;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Splits;
import splitstree4.nexus.Taxa;

/**
 * @author Markus Franz
 * Computes the set of all RY splits from a set of characters
 */
public class RYSplits implements Characters2Splits {
    public final static String DESCRIPTION = "Computes all RY splits";

    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa the taxa
     * @param c    the characters matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Characters c) {
        return taxa != null && c != null && (c.getFormat().getDatatype().equalsIgnoreCase("DNA") || c.getFormat().getDatatype().equalsIgnoreCase("RNA"));
    }

    /**
     * Applies the method to the given data
     *
     * @param taxa  the taxa
     * @param chars the characters matrix
     * @return the computed set of splits
     */
    public Splits apply(Document doc, Taxa taxa, Characters chars) throws CanceledException {

        Splits rySplits = new Splits(chars.getNtax());

        int maxProgress = chars.getNtax();
        doc.notifySetProgress(0);
        doc.notifySubtask("RYSplits");

        for (int i = 1; i <= chars.getNchar(); i++) {
            TaxaSet current = new TaxaSet();
            for (int j = 1; j <= chars.getNtax(); j++) {
                if (chars.get(j, i) == 'a' || chars.get(j, i) == 'g' || chars.get(j, i) == 'A' || chars.get(j, i) == 'G')
                    current.set(j);    //set R to 1
            }

            //if (current.cardinality() > chars.getNtax()/2) current = current.getComplement(chars.getNtax());					//set minimal side to 1

            if (current.cardinality() == 0 || current.cardinality() == chars.getNtax()) continue;

            boolean exists = false;
            for (int j = 1; j <= rySplits.getNsplits(); j++) {

                if (rySplits.get(j).equalsAsSplit(current, chars.getNtax()) || rySplits.get(j).equalsAsSplit(current.getComplement(chars.getNtax()), chars.getNtax())) {
                    rySplits.setWeight(j, rySplits.getWeight(j) + (float) chars.getCharWeight(i));

                    exists = true;
                    break;
                }
            }

            if (!exists) {
                rySplits.add(current, (float) chars.getCharWeight(i));
            }
            doc.notifySetProgress(100 * i / maxProgress);
        }
        doc.notifySetProgress(100);   //set progress to 100%
        // pd.close();								//get rid of the progress listener
        // doc.setProgressListener(null);
        return rySplits;
    }

    /**
     * gets a short description of the algorithm
     *
     * @return a description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

}
