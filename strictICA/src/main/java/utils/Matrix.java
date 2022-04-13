package utils;

import org.apache.commons.math3.linear.*;

import java.util.*;

public class Matrix {
    public static final double EPS = 1e-6;

    private final double[][] data;

    public Matrix(double[][] data) {
        this.data = data;
    }

    public Matrix(double[] vec) {
        this.data = new double[][]{vec};
    }

    public Matrix(RealMatrix mtx) {
        this.data = mtx.getData();
    }

    public int numCols() {
        return data[0].length;
    }

    public int numRows() {
        return data.length;
    }

    public double[] getRow(int row) {
        return data[row];
    }

    public double getElem(int row, int col) {
        return data[row][col];
    }

    public Matrix transpose() {
        return new Matrix(this.toApacheMatrix().transpose());
    }

    public Matrix mult(Matrix other) {
        return new Matrix(this.toApacheMatrix().multiply(other.toApacheMatrix()));
    }

    public Matrix div(double N) {
        double[][] res = new double[numRows()][numCols()];
        for (int row = 0; row < numRows(); row++) {
            for (int col = 0; col < numCols(); col++) {
                res[row][col] = getElem(row, col) / N;
            }
        }
        return new Matrix(res);
    }

    public Matrix inv() {
        return new Matrix(MatrixUtils.inverse(toApacheMatrix()));
    }

    public Pair<double[], double[][]> decomposition() {
        if (numRows() != numCols()) {
            throw new RuntimeException("expected square matrix!");
        }

        for (int i = 0; i < numRows(); i++) {
            for (int j = 0; j < numCols(); j++) {
                if (data[i][j] != data[j][i]) {
                    throw new RuntimeException("expected symmetric matrix!");
                }
            }
        }

        EigenDecomposition eig = new EigenDecomposition(toApacheMatrix());

        if (eig.hasComplexEigenvalues()) {
            throw new RuntimeException("expected non-complex eigen values!");
        }

        double[] eigenValues = eig.getRealEigenvalues();

        if (eigenValues.length != numRows()) {
            throw new RuntimeException("expected eigen values count of N!");
        }

        for (int i = 0; i < eigenValues.length - 1; i++) {
            if (eigenValues[i] < eigenValues[i + 1]) {
                throw new RuntimeException("expected sorted eigen values!");
            }
        }

        double[][] eigenVectors = new double[numRows()][numRows()];
        for (int i = 0; i < eigenVectors.length; i++) {
            double[] eigenVector = eig.getEigenvector(i).toArray();

            if (eigenVector.length != eigenVectors[i].length) {
                throw new RuntimeException("expected eigenVector length of N");
            }

            double sum = 0;
            for (int j = 0; j < eigenVector.length; j++) {
                eigenVectors[i][j] = eigenVector[j];
                sum += eigenVector[j] * eigenVector[j];
            }

            System.out.println("!!! " + sum + " : " + Arrays.toString(eigenVector));
            if (Math.abs(sum - 1) > EPS) {
                throw new RuntimeException("L2-norm of eigen vector must be 1!");
            }

            System.arraycopy(eigenVector, 0, eigenVectors[i], 0, eigenVector.length);
        }

        return new Pair<>(eigenValues, eigenVectors);
    }

//    public Matrix getWhiteningMatrix() {
//        if (numRows() != numCols()) {
//            throw new RuntimeException("expected n * n matrix for decomposition");
//        }
//
//        EigenDecomposition eig = new EigenDecomposition(toApacheMatrix());
//
//        if (eig.hasComplexEigenvalues()) {
//            throw new RuntimeException("EigenDecomposition hasComplexEigenvalues");
//        }
//
//        double[] eigenValues = eig.getRealEigenvalues();
//        System.out.println(Arrays.toString(eigenValues));
//
//        double[][] eigenValuesDiagonalMatrix = new double[eigenValues.length][eigenValues.length];
//        for (int i = 0; i < eigenValues.length; i++) {
//            //eigenValuesDiagonalMatrix[i][i] = eigenValues[i];
//            eigenValuesDiagonalMatrix[i][i] = Math.sqrt(1 / Math.max(EPS, eigenValues[i]));
//        }
//
//        System.out.println(new Matrix(eigenValuesDiagonalMatrix));
//
//        double[][] eigenVectors = new double[numRows()][numRows()];
//        for (int i = 0; i < numRows(); i++) {
//            double[] eigenVector = eig.getEigenvector(i).toArray();
//            if (eigenVector.length != numRows()) {
//                throw new RuntimeException("expected eigenVector length of n");
//            }
//            if (numRows() >= 0) System.arraycopy(eigenVector, 0, eigenVectors[i], 0, numRows());
//        }
//
//        Matrix UT = new Matrix(eigenVectors);
//        Matrix U = UT.transpose();
//        Matrix D = new Matrix(eigenValuesDiagonalMatrix);
//
//        return U.mult(D).mult(UT);
//    }

    private RealMatrix toApacheMatrix() {
        return new Array2DRowRealMatrix(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double[] row : data) {
            for (double elem : row) {
                sb.append(elem);
                sb.append("\t");
            }
            //sb.append(Arrays.toString(row));
            sb.append("\n");
        }
        return "Matrix{\n" + sb + "}\n";
    }

    public static double scalProd(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new RuntimeException("first array length must be equals second array length!");
        }

        double ans = 0;

        for (int i = 0; i < x.length; i++) {
            ans += x[i] * y[i];
        }

        return ans;
    }
}
