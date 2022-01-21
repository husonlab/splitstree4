/*
 * Snap.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.externalIO.exports;

import splitstree4.core.Document;
import splitstree4.nexus.Characters;
import splitstree4.nexus.Taxa;
import splitstree4.nexus.Traits;

import java.io.Writer;
import java.util.*;

/**
 * Output data from within the program to a tab delimited text file. Generally, the user will want to
 * output one block at a time.
 * <p/>
 * Distances are output as a column vector, indexed 12,13,14,....,(n-1)n
 * Splits are output as a topological (design) matrix.
 * <p/>
 * The output is intended mainly for those wanting to use other programs to estimate network settings
 */

public class Snap extends ExporterAdapter /*implements Exporter*/ {

    private String Description = "Exports a SNAP xml file";
    private String Name = "Snap";


    /**
     * can we export this data?
     *
     * @param dp the document being exported
     * @return true, if can handle this export
     */
    public boolean isApplicable(Document dp, Collection selected) {


        LinkedList<String> goodBlocks = new LinkedList<>();
        goodBlocks.add(Taxa.NAME);
        goodBlocks.add(Traits.NAME);
        goodBlocks.add(Characters.NAME);

        return /*false &&*/ blocksAllOK(dp, selected, goodBlocks);
    }


    class SnapInfo implements ExporterInfo {
        private final String exporterName = "Snap";
        public boolean hasCancelled;

        public SnapInfo() {
            hasCancelled = false;
            speciesTrait = "Species";
            u = v = 1;
            alpha = 2;
            beta = 16;
            lambda = 1;
            treeChoice = 1;
            startTree = "";
            sampleTrees = sampleTimes = samplePopsizes = true;
            sampleMutation = false;
            chainLength = 10000000;
            burnin = chainLength / 10;
            subsampling = chainLength / 1000;
            estimatePriorParams = true;
        }


        public String getExporterName() {
            return exporterName;
        }

        public boolean userHasCancelled() {
            return hasCancelled;
        }

        public String speciesTrait;
        public double u, v, alpha, beta, lambda;
        public int treeChoice;
        public String startTree;
        public boolean sampleTrees, sampleTimes, sampleMutation, samplePopsizes, estimatePriorParams;

        public int burnin, chainLength, subsampling;

        //public String extraXML;
    }


    public SnapInfo requestAdditionalInfo(Document doc) {
        SnapExportDialog dialog = new SnapExportDialog(null, true);

        //PriorParams params = estimatePriorParams(doc,)


        dialog.setPopTraits(doc.getTraits().getTraitNames());
        //dialog.setPriorParams(doc);


        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        SnapInfo info = new SnapInfo();

        if (dialog.hasCancelled) {
            info.hasCancelled = true;
            return info;
        }

        info.speciesTrait = (String) dialog.popTrait.getSelectedItem();
        info.u = Double.parseDouble(dialog.uField.getText());
        info.v = Double.parseDouble(dialog.vField.getText());
        double mean, var;
        mean = Double.parseDouble(dialog.meanField.getText());
        var = Double.parseDouble(dialog.varField.getText());
        info.beta = mean / var;
        info.alpha = mean * info.beta;
        info.lambda = Double.parseDouble(dialog.treePriorParamField.getText());
        info.treeChoice = dialog.startTreeCombo.getSelectedIndex();
        info.startTree = dialog.treeTextPane.getText();

        info.sampleTrees = dialog.treeCheckBox.isSelected();
        info.sampleTimes = dialog.timesCheckBox.isSelected();
        info.sampleMutation = dialog.mutationCheckBox.isSelected();
        info.samplePopsizes = dialog.populationCheckBox.isSelected();

        info.burnin = Integer.parseInt(dialog.mcmcBurninField.getText());
        info.chainLength = Integer.parseInt(dialog.mcmcLengthField.getText());
        info.subsampling = Integer.parseInt(dialog.mcmcSampleField.getText());

        info.estimatePriorParams = dialog.estimatePriorCheck.isSelected();

        return info;
    }


    void estimatePriorParams(int[] speciesCounts, int[][] alleleCounts, SnapInfo info) {

        //First estimate the number of constant markers that were removed.
        int nspecies = speciesCounts.length - 1;
        int nchar = alleleCounts[1].length - 1;
        int npairs = 0;
        double total = 0.0;
        for (int i = 1; i <= nspecies; i++) {
            int ni = speciesCounts[i];
            if (ni < 2)
                continue;
            for (int j = i + 1; j <= nspecies; j++) {
                double fi, fj, fij;
                fi = fj = fij = 0.0;
                int nj = speciesCounts[j];
                for (int k = 1; k <= nchar; k++) {
                    int iCount = alleleCounts[i][k];
                    int jCount = alleleCounts[j][k];
                    if (0 < iCount && iCount < ni)
                        fi++;
                    if (0 < jCount && jCount < nj)
                        fj++;
                    if ((0 < iCount && iCount < ni) && (0 < jCount && jCount < nj))
                        fij++;
                }
                if (fi > 0 && fj > 0) {
                    fi /= nchar;
                    fj /= nchar;
                    fij /= nchar;
                    double kappa = 1 - fij / (fi * fj);
                    System.out.println("[" + fi + "," + fj + "," + fij + "]\tKappa for (" + i + "," + j + ") = " + kappa);
                    npairs++;
                    total += kappa;
                }
            }
        }
        double kappa;
        if (npairs > 0)
            kappa = (total / npairs);
        else
            kappa = 0.9;
        if (kappa < 0.0)
            kappa = 0.0;


        System.out.println("Estimated probability of constant site = " + kappa);
        //Now compute the base frequencies
        int numberOnes = 0;
        int ntax = 0;
        for (int i = 1; i <= nspecies; i++) {
            ntax += speciesCounts[i];
            for (int k = 1; k <= nchar; k++)
                numberOnes += alleleCounts[i][k];
        }
        double pi_1 = (double) numberOnes / ((double) (ntax * nchar));   //u/(u+v)
        double pi_0 = 1.0 - pi_1;        //v/(u+v)
        //assume rate = 2uv/(u+v) = 1.0
        info.u = 1.0 / (2 * pi_0);
        info.v = 1.0 / (2 * pi_1);


        //Now a table of pi values within and across species, together with the average distance between individuals

        double[][] piValues = new double[nspecies + 1][nspecies + 1];

        //First within species
        for (int i = 1; i <= nspecies; i++) {
            int ni = speciesCounts[i];
            if (ni > 1) {
                int npair = ni * (ni - 1) / 2;
                double sum = 0.0;
                for (int k = 1; k <= nchar; k++) {
                    int xi = alleleCounts[i][k];
                    sum += ((double) xi * (ni - xi)) / npair;
                }
                piValues[i][i] = sum / nchar;
            } else
                piValues[i][i] = 0;

            for (int j = i + 1; j <= nspecies; j++) {
                int nj = speciesCounts[j];
                double sum = 0.0;
                for (int k = 1; k <= nchar; k++) {
                    double xi = (double) alleleCounts[i][k];
                    double xj = (double) alleleCounts[j][k];
                    sum += (xi * (nj - xj) + (ni - xi) * xj) / ((double) ni * nj);
                }
                piValues[i][j] = sum / nchar;
            }
        }

        //Now correct for the missing non-constant values.
        /*    for(int i=1;i<=nspecies;i++)
for(int j=i;j<=nspecies;j++)
   piValues[i][j] *= (1.0-kappa);   */

        //Estimate the mean and variance of the theta values within populations.
        double sum = 0.0;
        double sum2 = 0.0;
        for (int i = 1; i <= nspecies; i++) {
            double theta_i = piValues[i][i];
            sum += theta_i;
            sum2 += theta_i * theta_i;
        }
        double gammaMean = sum / nspecies;
        double gammaVar = ((double) nspecies) / (nspecies - 1.0) * ((sum2 / nspecies) - gammaMean * gammaMean);
        gammaVar *= 10; //safety margin

        info.beta = gammaMean / gammaVar;
        info.alpha = info.beta * gammaMean;


        //Now compute the average divergence for any pair of species.
        sum = 0.0;
        for (int i = 1; i <= nspecies; i++) {
            for (int j = i + 1; j <= nspecies; j++) {
                sum += piValues[i][j];
            }
        }

        //the average divergence between two individuals equals 2*height + divergence w/in ancestral species
        double meanDivergence = sum / (0.5 * nspecies * (nspecies - 1.0)) - gammaMean;

        /*Estimate the lambda parameter for the yule model.
        Under the yule model, the average length of the path
        in the tree between two leaves is
           \frac{1}{\lambda} \frac{n+1}{n-1} \sum_{r=2}^n \frac{r-1}{r(r+1)}

           which we can estimate by  \pi/\mu,
           where \mu is the mutation rate:
        double \mu = 2 * u * v / (u + v);
             This gives a moment based estimator of
             \lambda =  \frac{mu}{pi} \frac{n+1}{n-1} \sum_{r=2}^n \frac{r-1}{r(r+1)}

         */

        sum = 0.0;
        for (int r = 2; r <= nspecies; r++)
            sum += (r - 1.0) / (r * (r + 1.0));
        info.lambda = (1.0 / meanDivergence) * ((nspecies + 1.0) / (nspecies - 1.0)) * sum;

        //as a sanity check, compute the mean height here.
        sum = 0.0;
        for (int r = 1; r < nspecies; r++)
            sum += (double) r / (nspecies - r);
        double mean = 1.0 / (nspecies * info.lambda) * sum;
        System.out.println("Estimated height of Yule tree = " + mean);

    }

    /**
     * Writes selected blocks to a tab-delimited text file.
     *
     * @param w          Where to write the data
     * @param dp         The document being exported
     * @param blockNames Collection of blocks to be exported
     * @return null
     * @throws Exception
     */
    public Map apply(Writer w, Document dp, Collection blockNames) throws Exception {
        SnapInfo info = new SnapInfo();
        return apply(w, dp, blockNames, info);
    }

    /**
     * Writes selected blocks to a tab-delimited text file.
     *
     * @param w              Where to write the data
     * @param dp             The document being exported
     * @param blockNames     Collection of blocks to be exported
     * @param additionalInfo Additional Info
     * @return null
     * @throws Exception
     */
    public Map apply(Writer w, Document dp, Collection blockNames, ExporterInfo additionalInfo) throws Exception {

        SnapInfo info = (SnapInfo) additionalInfo;

        //SnapExportDialog dialog = new SnapExportDialog(null,true);

        //dialog.setVisible(true);
        //Get the list of species/populations
        Traits traits = dp.getTraits();
        String popHeader = info.speciesTrait;
        String[] allPops = traits.getTraitValues(popHeader);
        Set uniqueSet = new HashSet<>(Arrays.asList(allPops));
        Vector<String> allSpecies = new Vector<>();
        for (Object anUniqueSet : uniqueSet) {
            String s = (String) anUniqueSet;
            if (s != null && s.length() > 0)
                allSpecies.add(s);
        }
        int nspecies = allSpecies.size();
        Taxa taxa = dp.getTaxa();
        Characters chars = dp.getCharacters();
        int nchar = chars.getNchar();


        int[] speciesCounts = new int[nspecies + 1];
        int[][] alleleCounts = new int[nspecies + 1][nchar + 1];

        for (int i = 1; i <= taxa.getNtax(); i++) {
            int thisSpecies = allSpecies.indexOf(allPops[i]) + 1;
            speciesCounts[thisSpecies]++;

            for (int j = 1; j <= nchar; j++)
                if (chars.get(i, j) == '1')
                    alleleCounts[thisSpecies][j]++;
        }

        //Compute the largest allele count over all species.
        int maxCount = 0;
        for (int i = 1; i <= nspecies; i++)
            for (int j = 1; j <= nchar; j++)
                if (alleleCounts[i][j] > maxCount)
                    maxCount = alleleCounts[i][j];

        if (info.estimatePriorParams)
            estimatePriorParams(speciesCounts, alleleCounts, info);


        //OUTPUT


        //Header info
        String s = "";
        s += "<!-- Exported from SplitsTree -->\n";
        s += "<snap version='2.0' namespace='snap:snap.likelihood:beast.util:beast.evolution'>\n\n";
        s += "\n";
        s += "<map name='snapprior'>snap.likelihood.SnAPPrior</map>\n";
        s += "<map name='snaptreelikelihood'>snap.likelihood.SnAPTreeLikelihood</map>\n";


        //Alignment block
        s += "\n\n\n\t<!-- n = " + nchar + " -->\n";
        s += "\t<data spec='snap.Data' id='snapalignment' dataType='integerdata' statecount='" + (maxCount + 1) + "'>\n";
        for (int i = 1; i <= nspecies; i++) {
            s += "\t\t<sequence taxon='" + allSpecies.get(i - 1) + "' totalcount = '" + speciesCounts[i] + "'>\n";
            for (int j = 1; j <= nchar; j++)
                s += alleleCounts[i][j] + ",";
            s += "\n";
            s += "\t\t</sequence>\n";
        }
        s += "\t</data>\n\n\n";


        s += "<run id='mcmc' spec='snap.MCMC' chainLength='" + info.chainLength + "' preBurnin='0' stateBurnin='" + info.burnin + "'>\n";


        //Specifying everything in an MCMC 'state'
        s += "        <state>\n";
        s += "          <tree name='stateNode' spec='ClusterTree' id='tree' nodetype='snap.NodeData' clusterType='upgma'>\n";
        s += "               <input name='taxa' idref='snapalignment'/>\n";
        s += "          </tree>\n";
        s += "          <parameter name='stateNode' id='coalescenceRate' value='10'/>";
        s += "          <parameter name='stateNode' id='v' value='" + info.v + "' lower='0.0'/>\n";
        s += "          <parameter name='stateNode' id='u' value='" + info.u + "' lower='0.0'/>\n";
        s += "          <parameter name='stateNode' id='alpha'  value='" + info.alpha + "' lower='0.0'/>\n";
        s += "          <parameter name='stateNode' id='beta'   value='" + info.beta + "' lower='0.0'/>\n";
        s += "          <parameter name='stateNode' id='lambda' value='" + info.lambda + "' lower='0.0'/>\n";
        s += "          <parameter name='stateNode' id='kappa' value='100' lower='0.0'/>\n";
        s += "        </state>\n";
        s += "\n";

        //Specifying everything in the Posterior distribution


        s += "<distribution id='posterior' spec='beast.core.util.CompoundDistribution'>\n" +
                "            <distribution id='prior' spec='beast.core.util.CompoundDistribution'>\n" +
                "                <distribution spec='beast.math.distributions.Prior' id='lambdaPrior' x='@lambda'>\n" +
                "                    <distribution spec='beast.math.distributions.OneOnX'/>\n" +
                "                </distribution>\n" +
                "                <distribution spec='SnAPPrior' name='distribution' id='snapprior' \n" +
                "                    kappa='@kappa' alpha='@alpha' beta='@beta' lambda='@lambda' rateprior='gamma'\n" +
                "                    coalescenceRate='@coalescenceRate' tree='@tree'\n" +
                "                    />\n" +
                "            </distribution>\n" +
                "<!-- when starting from tree, set initFromTree='true' -->\n" +
                "            <snaptreelikelihood name='distribution' id='treeLikelihood' initFromTree='false' pattern='coalescenceRate' data='@snapalignment' tree='@tree'>\n" +
                "                <siteModel spec='sitemodel.SiteModel' id='siteModel'>\n" +
                "\t\t\t\t      <substModel spec='snap.likelihood.SnapSubstitutionModel'\n" +
                "                    mutationRateU='@u' mutationRateV='@v' coalescenceRate='@coalescenceRate'/>\n" +
                "\t\t\t\t  </siteModel>\n" +
                "            </snaptreelikelihood>\n" +
                "        </distribution>";


        s += "\n";
        s += "        <stateDistribution idref='prior'/>\n";
        s += "\n";


        //OPERATORS

        if (info.sampleTrees) {
            s += "    	<operator spec='operators.NodeSwapper' weight='0' tree='@tree'/>\n";
        }

        if (info.sampleTimes) {
            s += "<operator spec='operators.NodeBudger' weight='4' size='0.5' tree='@tree'/>";
            s += "<operator spec='operators.ScaleOperator' scaleFactor='0.25' weight='0.5' tree='@tree'/>";
        }

        if (info.samplePopsizes) {
            s += "        <operator spec='operators.GammaMover' scale='0.5' weight='4' coalescenceRate='@coalescenceRate'/>\n";
        }

        if (info.samplePopsizes && info.sampleTimes) {
            s += "        <operator spec='operators.RateMixer' scaleFactors='0.25' weight='1' coalescenceRate='@coalescenceRate' tree='@tree'/>\n";
        }
        s += "\n";


        String fileroot = dp.getNameTop();
        System.err.println("fileroot = " + fileroot);

        //Settings for output of MCMC chain
        s += "        <logger logEvery='100'>\n";
        s += "			<model idref='posterior'/>\n";
        s += "            <log idref='u'/>\n";
        s += "            <log idref='v'/>\n";
        s += "            <log idref='prior'/>\n";
        s += "            <log idref='treeLikelihood'/>\n";
        s += "            <log idref='posterior'/>\n";
        s += "            <log spec='beast.evolution.tree.TreeHeightLogger' tree='@tree'/>";
        s += "        </logger>\n";
        s += "        <logger logEvery='100' fileName='" + fileroot + ".$(seed).log'>\n";
        s += "	          <model idref='posterior'/>\n";
        s += "            <log idref='u'/>\n";
        s += "            <log idref='v'/>\n";
        s += "            <log idref='prior'/>\n";
        s += "            <log idref='treeLikelihood'/>\n";
        s += "            <log idref='posterior'/>\n";
        s += "			<log idref='coalescenceRate'/>\n";
        s += "			<log spec='snap.ThetaLogger' coalescenceRate='@coalescenceRate'/>\n";
        s += "            <log spec='beast.evolution.tree.TreeHeightLogger' tree='@tree'/>";
        s += "            <log spec='TreeLengthLogger' tree='@tree'/>";

        s += "        </logger>\n";
        s += "        <logger fileName='" + fileroot + ".$(seed).trees' id='treelog' logEvery='100' mode='tree'>\n";
        s += "            <log id='TreeWithMetaDataLogger0' spec='beast.evolution.tree.TreeWithMetaDataLogger' tree='@tree'>\n";
        s += "                 <metadata coalescenceRate='@coalescenceRate' spec='snap.RateToTheta' id='theta'/>\n";
        s += "            </log>\n";
        s += "        </logger>\n";
        s += "</run>\n";
        s += "\n";
        s += "\n";
        s += "</snap>\n";

        w.write(s);

        return null;
    }

    public String getDescription() {
        return Description;
    }

    public String getName() {
        return Name;
    }


}
