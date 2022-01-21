/*
 * pmbModel.java Copyright (C) 2022 Daniel H. Huson
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
/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree4.models;

/**
 * @author bryant
 * <p/>
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class pmbModel extends ProteinModel {


    public pmbModel() {

        /* pmbModel
         *
         * estimated from Blocks+v13Aug01 database.
         *   Prepared by E.Tillier, October March2003.
         *
         * Extracted from pmb.dat file distributed with PAML 3.13d
         */

        //These are an eigenvalue decomposition
        //     V'DV = Pi^(1/2) Q Pi(-1/2)

        this.evals = new double[]{
                -1.84106,
                -1.55982,
                -1.51951,
                -1.39397,
                -1.30463,
                -1.27098,
                -1.20803,
                -1.18627,
                -1.12421,
                -1.09012,
                -0.932845,
                -0.835565,
                -0.81895,
                -0.751751,
                -0.662356,
                -0.632406,
                -0.600843,
                -0.521218,
                -0.476422,
                -2.89074e-16};

        this.evecs = new double[][]{{0.115932, 0.0640013, -0.259257, 0.0215157, -0.0196316, -0.4948, -0.227569, 0.444317, -0.0646669, 0.211457, 0.514948, -0.06096, -0.0399752, -0.0366116, -0.0218354, 0.0959844, 0.0989926, -0.0467125, 0.0491595, -0.27488},
                {-0.00104876, 0.00134451, -0.00778269, 0.0525286, -0.405472, 0.15119, -0.000647471, 0.345029, 0.0964271, -0.185238, -0.135342, 0.544836, -0.398871, -0.0569576, 0.13368, -0.0283127, -0.255691, 0.020274, 0.177548, -0.231871},
                {0.00467861, 0.00758568, -0.122108, 0.144041, -0.382765, -0.460067, 0.0514093, -0.551106, 0.0276805, -0.453355, 0.0588524, -0.10247, 0.075511, -0.010321, 0.0789678, -0.0283354, -0.0800478, -0.0232555, 0.14829, -0.194124},
                {-0.000903147, 0.00168207, -0.0139608, -0.086994, 0.183972, 0.0473424, -0.0547358, 0.413689, 0.169039, -0.372539, -0.265706, -0.330834, 0.469874, -0.223796, 0.124156, -0.00899351, -0.212927, -0.0144898, 0.240829, -0.211407},
                {0.0105084, 0.00106299, -0.00193764, -0.00158228, 0.0025229, 0.00678825, -0.00663193, -0.012004, -0.00394139, 0.0104573, -0.0965678, 0.0191213, 0.00639636, -0.0052054, -0.0307427, -0.0638003, -0.11751, -0.903026, -0.356362, -0.168769},
                {0.0208431, 0.160763, 0.0691058, -0.88869, -0.00723522, -0.095389, -0.0150183, -0.203435, -0.0936731, 0.179089, -0.0350438, 0.0701475, -0.0285719, -0.0577102, 0.0780169, -0.00445626, -0.148494, 0.0148227, 0.12247, -0.1841},
                {0.00230301, -0.0321236, -0.0228964, 0.302715, -0.262444, 0.108923, 0.0670404, -0.145462, -0.172298, 0.669595, -0.168587, -0.0995758, 0.252066, -0.202558, 0.124472, 0.0162268, -0.245305, 0.0099649, 0.223508, -0.23124},
                {-0.000244605, 0.00348596, -0.0187142, -0.00734966, 0.0184706, 0.0691544, 0.0330963, -0.0216272, 0.0280843, 0.0367487, -0.232182, 0.0622428, -0.0196427, 0.0697489, 0.0269341, -0.171697, 0.790494, -0.173005, 0.405797, -0.279349},
                {-0.0017523, -0.00866399, 0.00533232, 0.0270058, 0.0437556, 0.0285742, 0.0262952, 0.0800109, -0.0542967, 0.0504868, -0.0817122, -0.437371, -0.310111, 0.71802, 0.153414, -0.24255, -0.226523, 0.0279714, 0.0982332, -0.17332},
                {0.746808, -0.0601759, 0.156431, 0.00360401, -0.0169165, -0.0128139, 0.250201, -0.0206333, 0.278633, 0.0404642, -0.121539, -0.107799, -0.0969254, -0.0380616, 0.071631, 0.237476, 0.0982482, 0.143233, -0.287444, -0.244681},
                {-0.119003, 0.312851, 0.00874017, 0.100367, 0.0139279, 0.0805352, -0.572684, -0.0876563, -0.287471, -0.0954742, -0.294887, -0.049064, -0.0737728, -0.0176543, 0.0800233, 0.231127, 0.118508, 0.204526, -0.370292, -0.309504},
                {0.00230461, -0.0332841, 0.00161532, 0.218701, 0.757484, -0.215874, 0.0524135, -0.218454, -0.00332572, 0.0205372, -0.0562324, 0.313139, -0.184193, -0.0870854, 0.0933974, 0.00126996, -0.213837, 0.0130508, 0.174417, -0.228028},
                {-0.0992252, -0.919578, -0.140704, -0.148719, -0.00796292, 0.0203168, -0.178488, -0.0337994, -0.114838, -0.0206721, -0.0506002, -0.00819778, -0.0264817, -0.0135593, 0.0385665, 0.0830983, 0.0388851, 0.0698663, -0.123766, -0.148013},
                {-0.00358129, 0.0232645, -0.000360659, -0.00540773, 0.00796336, -0.0862367, 0.493189, 0.175679, -0.565795, -0.130986, -0.0155728, 0.239252, 0.320375, 0.149152, -0.0453768, -0.210566, 0.0487057, 0.162454, -0.279293, -0.212134},
                {0.00202433, -0.00584665, -0.0114656, -0.00218578, -0.0165789, 0.0188677, 0.00900348, -0.0141102, 0.0156772, -0.0116486, -0.136127, -0.00463721, -0.00234507, 0.141875, -0.905541, 0.230064, -0.128422, 0.0171344, 0.167623, -0.20501},
                {-0.071517, -0.0827703, 0.766908, 0.0670015, -0.0112641, 0.206193, -0.0682038, -0.0441161, -0.1141, -0.117674, 0.475133, -0.0673888, 0.0201927, -0.0494487, 0.00446861, 0.0410966, 0.00781021, -0.053529, 0.115319, -0.261174},
                {0.0653325, 0.10286, -0.528188, -0.0317411, 0.0926005, 0.613657, 0.122827, -0.186486, -0.0321009, -0.143964, 0.418172, -0.0696682, -0.0183589, -0.049169, 0.0354186, 0.0835744, -0.0218869, -0.0093409, 0.0264038, -0.237514},
                {0.000444023, -0.000969535, -0.00372855, 0.00843679, 0.00123938, 0.00207416, -0.021005, -0.00995168, 0.0371437, 0.0153002, -0.00259829, -0.244264, -0.301267, -0.472498, -0.250363, -0.699123, -0.00893358, 0.151229, -0.18422, -0.125399},
                {0.00820931, -0.000654046, -0.00144545, 0.00500267, -0.00760668, 0.0550419, -0.286912, -0.109013, 0.489689, 0.12349, 0.126601, 0.340219, 0.444329, 0.313297, -0.0396013, -0.354665, -0.0400334, 0.139954, -0.188921, -0.189668},
                {-0.628315, 0.0513941, 0.0176892, -0.0246592, -0.0153444, -0.0906735, 0.406245, 0.0487768, 0.407305, 0.115176, 0.00286302, -0.12204, -0.104041, -0.0473807, 0.0623057, 0.24334, 0.0939561, 0.103317, -0.254159, -0.267312}};

        this.freqs = new double[]{
                0.075559,
                0.053764,
                0.037684,
                0.044693,
                0.028483,
                0.033893,
                0.053472,
                0.078036,
                0.03004,
                0.059869,
                0.095793,
                0.051997,
                0.021908,
                0.045001,
                0.042029,
                0.068212,
                0.056413,
                0.015725,
                0.035974,
                0.071456};


        init();
    }
}
