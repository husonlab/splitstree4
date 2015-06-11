/**
 * PCoA.java 
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
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
package splitstree.algorithms.distances;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import jloda.graph.Node;
import jloda.graphview.NodeView;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloGraphView;
import jloda.util.Basic;
import jloda.util.ProgressListener;
import splitstree.core.Document;
import splitstree.nexus.Distances;
import splitstree.nexus.Network;
import splitstree.nexus.Taxa;

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * principle coordinate analysis
 * Daniel Huson and David Bryant, 2.2008
 */
public class PCoA implements Distances2Network {
    public final static String DESCRIPTION = "Performs Principle Coordinates Analysis (Gower, J.C. (1966))";
    private Matrix distanceMatrix;
    private double totalSquaredDistance;
    private int rank;
    int numberOfPositiveEigenValues;
    double[] eigenValues;
    private Map<String, double[]> name2vector = new HashMap<>();
    private double[][] vectors;
    private boolean done = false;

    private int optionFirstCoordinate = 1;
    private int optionSecondCoordinate = 2;


    /**
     * Applies the method to the given data
     *
     * @param taxa      the input taxa
     * @param distances the input distances
     * @return the computed network as a Network objec t
     */
    public Network apply(final Document doc, final Taxa taxa, final Distances distances) {
        final PhyloGraphView graphView = new PhyloGraphView();

        Runnable myRunnable = new Runnable() {
            public void run() {
                    PCoA.this.run(taxa, distances, graphView);
            }
        };
        Thread t = new Thread(myRunnable); // myRunnable does the calculations

        t.start(); // Kick off calculations

        ProgressListener progressListener = doc.getProgressListener();
        progressListener.setSubtask("Calculating PCoA");
        progressListener.setMaximum(-1);
        try {
            progressListener.setProgress(-1);
            while (t.isAlive()) {
                Thread.sleep(100L);  // Sleep 1/10 second
                progressListener.checkForCancel();
            }
        } catch (Exception ex) {
            if (t.isAlive()) {
                System.err.println("(Trying to cancel PCoA calculation)");
                t.interrupt();
                // t.stop();  // should never do this!
                System.err.println("CANCELED PCoA calculation");
            }
        }

        Network network = new Network(taxa, graphView);
        network.setLayout(Network.CIRCULAR);
        return network;
    }

    /**
     * run the PCoA algorithm
     *
     * @param taxa
     * @param distances
     * @param graphView
     * @throws IOException
     */
    private void run(Taxa taxa, Distances distances, PhyloGraphView graphView) {
        rank = taxa.getNtax();
        distanceMatrix = new Matrix(rank, rank);
        double sum = 0;
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                if (i == j)
                    distanceMatrix.set(i, j, 0);
                else {
                    double d = distances.get(i + 1, j + 1);
                    distanceMatrix.set(i, j, d);
                    sum += d * d;
                }
            }
        }
        totalSquaredDistance = 2 * sum;
        vectors = new double[rank][];

        PrintWriter pw = new PrintWriter(System.err);
        /*
        System.err.println("distanceMatrix:");
        distanceMatrix.print(pw, rank, rank);
        pw.flush();
        */

        Matrix centered = computeDoubleCenteringOfSquaredMatrix(distanceMatrix);

        /*
        System.err.println("centered:");
        centered.print(pw, rank, rank);
        pw.flush();
        */

        EigenvalueDecomposition eigenValueDecomposition = centered.eig();
        Matrix eigenVectors = eigenValueDecomposition.getV();
        /*
        System.err.println("eigenVectors:");
        eigenVectors.print(pw, rank, rank);
        pw.flush();
        */

        numberOfPositiveEigenValues = 0;
        Matrix positiveEigenValues = eigenValueDecomposition.getD();
        for (int i = 0; i < rank; i++) {
            if (positiveEigenValues.get(i, i) > 0)
                numberOfPositiveEigenValues++;
            else
                positiveEigenValues.set(i, i, 0);
        }
        /*
        System.err.println("positiveEigenValues:");
        positiveEigenValues.print(pw, rank, rank);
        pw.flush();
        */

        // multiple eigenvectors by sqrt of eigenvalues
        Matrix scaledEigenVectors = (Matrix) eigenVectors.clone();
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                double v = scaledEigenVectors.get(i, j);
                v = v * Math.sqrt(positiveEigenValues.get(j, j));
                scaledEigenVectors.set(i, j, v);
            }
        }
        /*
        System.err.println("scaledEigenVectors:");
        scaledEigenVectors.print(pw, rank, rank);
        pw.flush();
        */

        // sort indices by eigenValues
        int[] indices = sortValues(positiveEigenValues);
        // System.err.println("indices: " + Basic.toString(indices));

        eigenValues = new double[numberOfPositiveEigenValues];
        for (int j = 0; j < numberOfPositiveEigenValues; j++) {
            eigenValues[j] = positiveEigenValues.get(indices[j], indices[j]);
        }
        System.err.println("Positive eigenvalues:");
        System.err.println(Basic.toString(eigenValues, ", "));


        for (int i = 0; i < rank; i++) {
            String name = taxa.getLabel(i + 1);
            double[] vector = new double[numberOfPositiveEigenValues];
            name2vector.put(name, vector);
            vectors[i] = vector;
            for (int j = 0; j < numberOfPositiveEigenValues; j++) {
                vector[j] = scaledEigenVectors.get(i, indices[j]);
            }
        }
        done = true;
        final PhyloGraph graph = graphView.getPhyloGraph();

        System.err.println("Stress: " + getStress(getOptionFirstCoordinate(), getOptionSecondCoordinate()));
        for (int t = 1; t <= taxa.getNtax(); t++) {
            String name = taxa.getLabel(t);
            double[] coordinates = getProjection(getOptionFirstCoordinate(), getOptionSecondCoordinate(), name);
            Node v = graph.newNode();
            graph.setLabel(v, name);
            graphView.setLabel(v, name);
            graphView.setLocation(v, 100 * coordinates[0], 100 * coordinates[1]);
            graphView.fitGraphToWindow();
            graphView.setWidth(v, 3);
            graphView.setHeight(v, 3);
            graphView.setColor(v, Color.BLACK);
            graphView.setBackgroundColor(v, Color.BLACK);
            graphView.setShape(v, NodeView.OVAL_NODE);
        }
        graphView.trans.setCoordinateRect(graphView.getBBox());
        graphView.fitGraphToWindow();
        graphView.getScrollPane().revalidate();
    }


    /**
     * Determine whether given method can be applied to given data.
     *
     * @param taxa      the taxa
     * @param distances the distances matrix
     * @return true, if method applies to given data
     */
    public boolean isApplicable(Document doc, Taxa taxa, Distances distances) {
        return doc.isValid(taxa) && doc.isValid(distances);
    }

    /**
     * Gets a short description of the algorithm
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * get coordinates for given name
     *
     * @param name
     * @return coordinates
     */
    public double[] getCoordinates(String name) {
        return name2vector.get(name);
    }

    /**
     * get i-th and j-th coordinates for given name
     *
     * @param i
     * @param j
     * @param name
     * @return (i, j)
     */
    public double[] getProjection(int i, int j, String name) {
        double[] vector = name2vector.get(name);
        return new double[]{vector[i], vector[j]};
    }

    /**
     * given i-th, j-th and k-th coordinates for given name
     *
     * @param i
     * @param j
     * @param k
     * @param name
     * @return (i, j, k)
     */
    public double[] getProjection(int i, int j, int k, String name) {
        double[] vector = name2vector.get(name);
        return new double[]{vector[i], vector[j], vector[k]};
    }

    /**
     * get rank
     *
     * @return rank
     */
    public int getRank() {
        return rank;
    }


    /**
     * compute centered inner product matrix
     *
     * @param matrix
     * @return new matrix
     */
    private Matrix computeDoubleCenteringOfSquaredMatrix(Matrix matrix) {
        int size = matrix.getColumnDimension();
        Matrix result = new Matrix(matrix.getColumnDimension(), matrix.getRowDimension());
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double v1 = 0;
                for (int k = 0; k < size; k++) {
                    v1 += matrix.get(k, j) * matrix.get(k, j) / size;
                }
                double v2 = 0;
                for (int k = 0; k < size; k++) {
                    v2 += matrix.get(i, k) * matrix.get(i, k) / size;
                }
                double v3 = 0;
                for (int k = 0; k < size; k++) {
                    for (int l = 0; l < size; l++) {
                        v3 += matrix.get(k, l) * matrix.get(k, l) / (size * size);
                    }
                }
                double v4 = matrix.get(i, j);
                result.set(i, j, 0.5 * (v1 + v2 - v3 - (v4 * v4)));
            }
        }
        return result;
    }

    /**
     * sort indices by values
     *
     * @param m
     * @return sorted indices
     *         todo: replace by proper sorting
     */
    private int[] sortValues(Matrix m) {
        double[] v = new double[m.getColumnDimension()];
        int[] index = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = m.get(i, i);
            index[i] = i;
        }

        for (int i = 0; i < v.length; i++) {
            for (int j = i + 1; j < v.length; j++) {
                if (Math.abs(v[i]) < Math.abs(v[j])) {
                    double tmpValue = v[j];
                    v[j] = v[i];
                    v[i] = tmpValue;
                    int tmpIndex = index[j];
                    index[j] = index[i];
                    index[i] = tmpIndex;
                }
            }
        }

        return index;
    }

    public boolean isDone() {
        return done;
    }

    public double[] getEigenValues() {
        return eigenValues;
    }

    public double getStress(int i, int j) {
        return getStress(new int[]{i, j});
    }

    public double getStress(int i, int j, int k) {
        return getStress(new int[]{i, j, k});
    }

    public double getStress(int[] indices) {
        double squaredSum = 0;
        for (int a = 0; a < rank; a++) {
            for (int b = 0; b < rank; b++) {
                if (a != b) {
                    double d = 0;
                    for (int z : indices) {
                        d += (vectors[a][z] - vectors[b][z]) * (vectors[a][z] - vectors[b][z]);
                    }
                    d = Math.sqrt(d);
                    squaredSum += (d - distanceMatrix.get(a, b)) * (d - distanceMatrix.get(a, b));
                }
            }
        }
        return Math.sqrt(squaredSum / totalSquaredDistance);
    }

    public int getOptionFirstCoordinate() {
        return optionFirstCoordinate;
    }

    public void setOptionFirstCoordinate(int optionFirstCoordinate) {
        if (optionFirstCoordinate > 0)
            this.optionFirstCoordinate = optionFirstCoordinate;
    }

    public int getOptionSecondCoordinate() {
        return optionSecondCoordinate;
    }

    public void setOptionSecondCoordinate(int optionSecondCoordinate) {
        if (optionSecondCoordinate > 0)
            this.optionSecondCoordinate = optionSecondCoordinate;
    }
}
