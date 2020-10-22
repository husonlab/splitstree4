/**
 * cpREV45Model.java
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
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree4.models;

/**
 * @author bryant
 * <p/>
 * cpREV45 Model
 * <p/>
 * Extracted from .dat files distributed with PAML 3.13d
 */
public class cpREV45Model extends ProteinModel {
    static public final double[] dvals = {
            -2.07313,
            -2.01921,
            -1.79356,
            -1.61085,
            -1.51498,
            -1.23043,
            -1.16179,
            -1.07154,
            -1.05036,
            -0.914058,
            -0.868912,
            -0.794207,
            -0.732442,
            -0.652434,
            -0.535857,
            -0.4496,
            -0.434444,
            -0.32826,
            -0.193396,
            4.65355e-16};

    static public final double[][] V =
            {{0.00926197, -0.0858898, 0.234984, -0.140525, -0.00516884, 0.414625, 0.242234, -0.303444, -0.0825719, 0.243761, 0.0440661, 0.429081, -0.471696, 0.0491742, 0.044319, 0.0986309, 0.148443, -0.0674635, -0.058245, -0.275681},
                    {0.314113, 0.0233899, -0.0868766, -0.0889307, -0.159754, 0.308579, -0.289067, 0.0610756, -0.00962733, 0.0156619, -0.010231, 0.388171, 0.517198, -0.136032, 0.279871, -0.0812884, -0.260263, -0.156837, -0.0301462, -0.248998},
                    {0.323454, 0.0202933, 0.496122, 0.468855, 0.448981, -0.037528, -0.287229, 0.052549, 0.0600354, -0.00558696, -0.00212274, -0.164546, -0.1678, -0.0591295, 0.0740233, -0.0346475, -0.101229, -0.137477, -0.0394324, -0.202485},
                    {-0.137907, -0.015387, -0.181536, -0.435007, 0.039194, 0.34718, -0.281696, 0.0863562, 0.0904975, -0.0730559, 0.0126784, -0.54598, -0.311125, -0.173393, 0.141311, -0.0399948, -0.119929, -0.19008, -0.0441665, -0.192354},
                    {-0.0127385, -0.0138857, 0.0822211, -0.0638438, -0.0311655, -0.00666138, 0.235657, 0.524416, 0.777225, 0.0254009, -0.0428687, 0.180705, -0.0790362, -0.0253286, -0.0435949, -0.00384878, -0.010726, 0.0105514, -0.000760267, -0.0948683},
                    {0.135395, 0.00796203, -0.0884272, -0.364369, 0.530189, -0.332327, 0.513981, -0.112121, -0.0730833, -0.00592183, 0.0176291, -0.0317102, 0.158177, -0.090455, 0.184399, -0.0402172, -0.204112, -0.127775, -0.035051, -0.194936},
                    {0.20904, 0.0155209, 0.0152838, 0.308193, -0.613956, -0.115887, 0.427951, -0.0458256, -0.054722, -0.0351013, 0.0302829, -0.309136, -0.107592, -0.178231, 0.20024, -0.0357263, -0.139842, -0.170885, -0.0452786, -0.221359},
                    {-0.00593296, -9.08047e-06, 0.0106317, -0.0405386, -0.0214489, -0.0468292, 0.00130112, 0.0248411, -0.0183098, -0.0558805, -0.0178123, -0.0713712, 0.244735, 0.0996009, -0.358079, -0.210104, 0.590524, -0.553235, -0.071412, -0.289828},
                    {-0.0240122, -0.00161526, -0.0154755, -0.0116655, -0.0683411, 0.0141232, 0.0029964, 0.029067, -0.0203108, 0.11239, 0.190151, -0.0635635, -0.0220569, 0.739731, -0.268134, -0.174049, -0.505557, -0.0965324, -0.0172102, -0.158114},
                    {0.0587795, -0.718178, -0.0601256, 0.0422842, 0.0359993, 0.0365513, 0.0357057, 0.234158, -0.170374, -0.312189, -0.00990326, -0.0383795, 0.0264684, 0.18706, 0.151097, -0.0260157, 0.175067, 0.34145, -0.033606, -0.284605},
                    {-0.00491, 0.0975697, 0.0549753, -0.0420806, -0.0587733, -0.0680386, -0.114158, -0.587213, 0.454472, -0.043753, 0.186841, -0.173712, 0.209674, 0.0623235, 0.0267447, -0.0523063, 0.141101, 0.416855, -0.0143925, -0.317805},
                    {-0.841604, -0.0574005, 0.207693, 0.213787, 0.0286835, -0.0505543, 0.00781147, 0.000424149, -0.0434393, -0.00201794, 0.00171434, 0.0880365, 0.180087, -0.117575, 0.207781, -0.0375561, -0.167103, -0.127025, -0.0397996, -0.223607},
                    {-0.00283674, 0.0564726, -0.00450512, 0.0149426, 0.0113404, 0.055149, 0.0375462, 0.0954351, -0.0668545, 0.61708, -0.640016, -0.237107, 0.151933, 0.14253, 0.0956152, -0.0247363, 0.0998318, 0.215577, -0.0134989, -0.148324},
                    {0.00039897, -0.00199633, 0.0199216, -0.0151659, 0.0151362, 0.0304236, 0.0281818, 0.2426, -0.220298, 0.352916, 0.423212, -0.0424739, 0.0578566, -0.4354, -0.45087, -0.173, -0.0679244, 0.31813, 0.040226, -0.225832},
                    {0.0083352, -0.000937407, 0.0494451, -0.0304447, -0.0223646, 0.0125311, -0.00372773, 0.0405704, -0.0282603, -0.0488369, -0.0145343, -0.100851, 0.176366, 0.00895563, -0.262102, 0.908166, -0.0897544, -0.0526888, -0.0468433, -0.207364},
                    {-0.0324104, 0.0341931, -0.760037, 0.420216, 0.186293, -0.0996091, -0.105349, -0.0582791, 0.097999, 0.126293, 0.0227619, 0.168189, -0.231767, -0.0298028, 0.0078056, 0.0794804, 0.0220747, -0.068667, -0.0433172, -0.248998},
                    {0.021673, 0.0445898, 0.1124, -0.318348, -0.248813, -0.663764, -0.390001, 0.107339, -0.108214, 0.0967688, -0.0252913, 0.210124, -0.274962, 0.0172418, 0.12771, 0.0437082, 0.0378354, 0.0124266, -0.043318, -0.232379},
                    {-0.00291195, -6.62409e-07, 0.000919208, 0.000945432, 0.00237527, -0.00209583, 0.00108521, -0.00402225, -0.00782085, -0.00666719, -0.00417986, -0.00717067, -0.0168142, 0.0309127, 0.0484242, 0.0345775, 0.0280737, -0.0714197, 0.985454, -0.134164},
                    {-0.00613393, -0.00321973, 0.00291013, -0.0225191, -0.0342727, -0.00738294, -0.0013968, -0.178396, -0.00964543, -0.393507, -0.576817, 0.142909, -0.122516, -0.223915, -0.480974, -0.183906, -0.295177, 0.0969215, 0.021679, -0.176068},
                    {-0.0371712, 0.675257, -0.00645671, 0.024599, 0.0544486, 0.14631, 0.115147, 0.294636, -0.235384, -0.360543, 0.0201206, 0.034296, -0.0508453, 0.181264, 0.142303, -0.0119855, 0.170308, 0.280145, -0.035494, -0.256905}};

    static public final double[] f = {
            0.076,
            0.062,
            0.041,
            0.037,
            0.009,
            0.038,
            0.049,
            0.084,
            0.025,
            0.081,
            0.101,
            0.05,
            0.022,
            0.051,
            0.043,
            0.062,
            0.054,
            0.018,
            0.031,
            0.066};

    /**
     * constructor
     */
    public cpREV45Model() {

        //These are an eigenvalue decomposition
        //     V'DV = Pi^(1/2) Q Pi(-1/2)

        this.evals = dvals;

        this.evecs = V;

        this.freqs = f;

        init();
    }
}
