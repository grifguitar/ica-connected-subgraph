package utils;

import java.io.PrintWriter;
import java.util.Arrays;

import static utils.Matrix.scalProd;

public class DataAnalysis {
    public static Matrix standartization(Matrix matrix, PrintWriter out, boolean DEBUG, boolean deviation) {
        int components = matrix.numCols();
        int rows = matrix.numRows();

        double[][] res = new double[rows][components];

        if (DEBUG) {
            out.println("# components number: " + components);
        }

        for (int component = 0; component < components; component++) {
            double E = 0;

            for (int row = 0; row < rows; row++) {
                E += matrix.getElem(row, component);
            }

            E /= rows;

            double D = 0;

            for (int row = 0; row < rows; row++) {
                res[row][component] = matrix.getElem(row, component) - E;
                D += res[row][component] * res[row][component];
            }

            D /= rows;

            double st_dev = Math.sqrt(D);

            if (deviation) {
                for (int row = 0; row < rows; row++) {
                    res[row][component] /= st_dev;
                }
            }

            if (DEBUG) {
                out.println();
                out.println("# current component: " + component);
                out.println("# E: " + E + " ; D: " + D + " ; st_dev: " + st_dev);
            }

        }

        return new Matrix(res);
    }

    public static Matrix getCovMatrix(Matrix matrix, PrintWriter out, boolean DEBUG) {
        int components = matrix.numCols();
        int rows = matrix.numRows();

        if (DEBUG) {
            out.println("# components number: " + components);
        }

        double[] EE = new double[components];

        for (int component = 0; component < components; component++) {
            EE[component] = 0;

            for (int row = 0; row < rows; row++) {
                EE[component] += matrix.getElem(row, component);
            }

            EE[component] /= rows;
        }

        for (double E : EE) {
            if (E > Matrix.EPS) {
                throw new RuntimeException("expected zero - mean!");
            }
        }

        if (DEBUG) {
            out.print("# EE: (must be zero values): ");
            for (int i = 0; i < components; i++) {
                out.print(EE[i]);
                out.print(", ");
            }
            out.println();
        }

        double[][] cov_matrix = new double[components][components];

        for (int c1 = 0; c1 < components; c1++) {
            for (int c2 = 0; c2 < components; c2++) {

                cov_matrix[c1][c2] = 0;

                for (int row = 0; row < rows; row++) {
                    cov_matrix[c1][c2] += matrix.getElem(row, c1) * matrix.getElem(row, c2);
                }

                cov_matrix[c1][c2] /= rows;

            }
        }

        return new Matrix(cov_matrix);
    }

    public static Matrix getCovMatrix2(Matrix matrix) {
        int N = matrix.numRows();
        Matrix matrix2 = matrix.transpose();
        return matrix2.mult(matrix).div(N);
    }

    public static Matrix pca(Matrix matrix, int R) {
        Matrix cov_matrix = getCovMatrix2(matrix);

        Pair<double[], double[][]> decompose = cov_matrix.decomposition();

        System.out.println("# pca debug: ");
        System.out.println(Arrays.toString(decompose.first));
        for (int i = 0; i < R; i++) {
            System.out.println("# eigen " + i + ":");
            System.out.println(decompose.first[i]);
            System.out.println(Arrays.toString(decompose.second[i]));
        }
        System.out.println();

        double[][] newData = new double[matrix.numRows()][R];

        for (int i = 0; i < matrix.numRows(); i++) {
            for (int j = 0; j < R; j++) {
                newData[i][j] = scalProd(matrix.getRow(i), decompose.second[j]);
            }
        }

        return new Matrix(newData);
    }

}
