/**
 * JTTmodel.java
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
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JTTmodel extends ProteinModel {


    public JTTmodel() {

        //The following vector and matrix were computed using the Q matrix specified
        //Kosiol,C. and Goldman,N. The Different Versions of the Dayhoff Rate Matrix.
        //http://www.ebi.ac.uk/goldman-srv/dayhoff

        //These are an eigenvalue decomposition
        //     V'DV = Pi^(1/2) Q Pi(-1/2)

        this.evals = new double[]{-1.8796581e+00,
                -1.8172172e+00,
                -1.6140312e+00,
                -1.5389657e+00,
                -1.4048698e+00,
                -1.3099505e+00,
                -1.2466845e+00,
                -1.1717975e+00,
                -1.0603172e+00,
                -9.9900609e-01,
                -8.6014395e-01,
                -7.6866978e-01,
                -7.0249779e-01,
                -6.5119696e-01,
                -6.0593575e-01,
                -5.4569403e-01,
                -4.5576764e-01,
                -3.4602835e-01,
                -3.1033314e-01,
                2.4229737e-16};


        this.evecs = new double[][]{{3.4194258e-01, 1.6035947e-01, 2.4975080e-01, -5.2798921e-01, -2.0048493e-01, -2.0948435e-01, -9.2506410e-02, -4.0984135e-01, 5.1265454e-02, 1.3698171e-01, -2.1693449e-01, 1.4862304e-01, 7.3855796e-02, -1.7669582e-01, -1.1321114e-01, 1.2678791e-01, 1.2904797e-01, -3.9680666e-02, 6.6388018e-02, 2.7723983e-01},
                {3.6579492e-03, 6.8921483e-02, 8.0380333e-02, 8.9808102e-02, 3.9544099e-01, -6.4854001e-01, -1.3102336e-01, 9.2068706e-02, -9.1825634e-02, -5.6660489e-02, -3.1495998e-02, -2.7759762e-03, -3.0686106e-01, -6.4654436e-02, 4.0063127e-01, -1.0226885e-01, -9.4674855e-02, -1.8811888e-01, 2.7003260e-02, 2.2595785e-01},
                {7.4175511e-02, 4.5159040e-01, 3.9289087e-01, 1.2679059e-01, 2.3607799e-01, 3.2603277e-01, 2.5390431e-03, 4.2676497e-01, 4.8792446e-02, 4.1764478e-01, -1.1754121e-01, 8.1870024e-02, 1.0186346e-01, 2.4663190e-02, -4.2382010e-02, -2.3004026e-02, -5.5162332e-02, -1.2383751e-01, 6.6325772e-02, 2.0626671e-01},
                {-2.8260731e-02, -1.9482558e-01, -3.9684996e-01, -4.5540174e-01, 2.1450091e-01, -9.5353703e-02, -4.3690554e-02, 2.8845169e-01, -1.4662611e-01, 7.8659694e-02, 1.4334156e-03, -7.4450736e-02, 3.0216862e-01, 3.4141483e-01, -2.6766239e-01, -1.2311817e-01, -8.6338969e-02, -2.4173978e-01, 1.0527706e-01, 2.2642648e-01},
                {1.1278018e-02, 3.5896318e-02, -1.9638345e-02, -1.2879662e-02, -1.7824992e-02, 2.3811352e-02, 3.0922235e-03, 1.1700952e-02, 3.7713517e-04, -4.7352423e-02, 2.2556853e-02, -1.4765656e-01, 6.0458634e-02, 2.9861614e-01, 2.4058800e-01, 8.4536251e-01, -2.8287357e-01, 5.0975183e-02, -6.4851732e-02, 1.4240428e-01},
                {1.7715139e-03, -9.3010913e-03, -5.3482707e-02, -1.2777313e-01, 3.7760940e-01, 3.3701137e-01, 1.1251355e-01, -3.3785230e-01, 6.0532637e-01, -2.0769837e-01, 1.6078029e-01, -6.3946781e-02, 4.3756527e-02, 2.3896694e-02, 2.3848083e-01, -1.5685676e-01, -8.7719226e-02, -1.4823315e-01, 5.9710471e-02, 2.0263504e-01},
                {8.0100235e-03, 8.7527473e-02, 2.5940055e-01, 4.4537578e-01, -2.5508947e-01, -1.4159749e-01, 1.4630274e-02, -2.8182909e-01, -9.7325058e-02, -2.4096746e-01, 4.3147243e-02, -1.4244919e-01, 2.8898404e-01, 3.9663756e-01, -1.9046405e-01, -1.7877981e-01, -8.4736944e-02, -2.8360805e-01, 1.1690905e-01, 2.4863616e-01},
                {-8.0664120e-03, 4.8666276e-02, -6.5651648e-02, 3.1045189e-02, -2.8439951e-02, 1.0119518e-01, 3.9743824e-02, 6.2674009e-02, 1.6139722e-02, -1.3101069e-01, 2.5214258e-01, -1.4517371e-01, -6.1109800e-01, -1.8912436e-01, -5.3064761e-01, 1.9517788e-01, 1.7019234e-02, -2.5387675e-01, 7.2890673e-02, 2.7333848e-01},
                {-7.4272898e-03, -6.6231071e-02, -6.8390818e-02, 6.9965944e-03, -5.6531517e-01, -2.2992146e-01, -8.1115998e-02, 3.8921722e-01, 5.4743028e-01, 1.0090964e-01, 2.1942661e-01, 1.3674837e-01, 4.1942290e-02, -6.0541627e-02, 1.1273706e-01, -8.5117010e-02, -1.8633099e-01, -1.8857490e-02, 1.3015950e-02, 1.5160138e-01},
                {6.4725511e-01, -1.1809954e-01, -1.8682388e-01, 2.4120128e-01, 8.4744703e-02, 1.0628264e-01, -1.1486555e-01, 1.9614357e-01, 1.4097241e-02, -3.0033176e-01, -6.7989482e-02, 3.0234938e-01, -3.5393181e-02, 1.6046219e-01, 1.2159877e-02, 1.6401891e-02, 2.8868113e-01, 2.3265517e-01, 7.6460920e-04, 2.2927919e-01},
                {-1.1406420e-02, 2.6007941e-02, -2.1019804e-02, -1.0444725e-03, -1.9861624e-02, 1.3171592e-02, -2.0815146e-01, -1.3988868e-01, -9.7865210e-02, 3.8824763e-01, 4.5699643e-01, -3.8749248e-01, -6.1197345e-02, 1.6559914e-01, 1.1641117e-01, -1.3240652e-01, 2.7954015e-01, 4.2535728e-01, -5.9523696e-02, 3.0184584e-01},
                {-5.0823869e-03, -9.1784673e-02, -9.5897547e-02, -1.2807307e-01, -3.7543173e-01, 4.1388165e-01, 3.0112340e-02, -5.2905623e-03, -3.7769580e-01, 5.8835343e-04, -1.7598338e-01, 1.3222074e-02, -2.7471763e-01, 3.1538074e-02, 4.7926376e-01, -1.9586868e-01, -1.0384984e-01, -2.4120289e-01, 7.7364202e-02, 2.4392200e-01},
                {-2.8716174e-02, -1.6826612e-02, 1.1427985e-01, -1.0025069e-01, -2.5993349e-02, -1.6056559e-01, 9.0604253e-01, 1.0131797e-01, -4.6964499e-02, -4.8581864e-03, 8.9909237e-02, 8.3684758e-02, -3.8990145e-02, 1.1799009e-01, 5.4652195e-02, -1.5272207e-02, 1.8340170e-01, 1.5843434e-01, -4.1391778e-03, 1.5301626e-01},
                {1.6131897e-03, 2.5348211e-02, -8.0398737e-03, -1.4341177e-02, -2.1860360e-02, -1.8963139e-02, 4.9368402e-02, 7.7145641e-02, 1.3445887e-01, -1.4372110e-01, -5.5260837e-01, -4.3302429e-01, -1.1515127e-01, -2.4773113e-02, -1.6247612e-01, -1.9460071e-01, -3.4625414e-01, 4.5372965e-01, -1.1748816e-01, 2.0132054e-01},
                {-4.6387224e-03, 7.6833234e-02, -2.7691451e-02, 1.2135066e-02, -1.0365238e-02, 4.0315189e-02, 2.9151729e-02, 1.9410895e-01, -1.9899147e-01, -3.3213330e-01, 1.5614660e-01, -2.8013419e-01, 4.6945515e-01, -6.2939465e-01, 1.1511635e-01, 7.8098445e-02, 1.0357833e-01, -1.0054271e-02, 5.3802638e-02, 2.2479313e-01},
                {-9.9408222e-02, -7.6854185e-01, 2.2412505e-01, 2.4123060e-01, 1.1408820e-01, 3.9933508e-02, 9.3872968e-03, -6.1258600e-02, 4.8789236e-02, 3.1324829e-01, -1.7349418e-01, 4.9406600e-02, 7.5812496e-02, -1.9794374e-01, -6.2741249e-02, 1.2975106e-01, 2.6531145e-02, -4.9006397e-02, 5.0080043e-02, 2.6119903e-01},
                {-2.3339006e-01, 2.9913717e-01, -6.2353780e-01, 3.2742178e-01, -1.9730107e-02, -6.8258168e-02, 1.0052038e-01, -2.3923994e-01, 5.7703643e-02, 2.7207141e-01, -2.5507566e-01, 1.9404002e-01, 8.2833934e-02, -1.3355310e-01, -2.0070063e-02, 9.0150161e-02, 1.1373855e-01, -8.5979295e-03, 5.3191422e-02, 2.4190482e-01},
                {1.8641340e-03, 1.0598757e-03, -2.5668667e-03, -3.4522751e-03, -1.2027744e-02, 1.7573826e-02, 5.7255971e-03, 6.9036731e-04, 5.9748822e-03, 1.5197360e-03, -1.5202475e-02, 1.4837477e-02, 4.9559602e-02, -2.7467510e-03, -2.5513216e-02, -3.9538148e-02, 9.5940227e-02, -2.4700749e-01, -9.5381851e-01, 1.1973298e-01},
                {-1.2681719e-03, 1.2016593e-02, 7.9975595e-03, -6.4822637e-04, 9.9878092e-02, 4.8545631e-02, 6.7538223e-03, -1.5968450e-01, -2.6526649e-01, -9.3163992e-03, 3.3271473e-01, 4.6780169e-01, 5.3713344e-02, -1.3150362e-01, -1.2773317e-01, -1.0930500e-01, -6.1928309e-01, 2.9435149e-01, -1.0499018e-01, 1.7973026e-01},
                {-6.2621454e-01, 4.1112057e-02, 2.0555685e-01, -1.5309002e-01, -3.1076013e-03, 5.8064703e-02, -2.4376009e-01, 1.0315871e-01, 2.8323951e-02, -3.4993469e-01, -9.9049436e-02, 3.2529845e-01, -2.8843879e-02, 1.4754992e-01, -2.3945970e-02, 4.8938251e-02, 3.0183990e-01, 2.1215219e-01, 9.2511183e-03, 2.5763139e-01}};


        this.freqs = new double[]{0.076862, 0.051057, 0.042546, 0.051269, 0.020279, 0.041061, 0.061820, 0.074714, 0.022983, 0.052569, 0.091111, 0.059498, 0.023414, 0.040530, 0.050532, 0.068225, 0.058518, 0.014336, 0.032303, 0.066374
        };

        init();
    }
}