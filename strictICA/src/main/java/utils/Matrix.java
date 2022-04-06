package utils;

import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;

public class Matrix {
    private final double[][] data;

    public Matrix(int rows, int columns) {
        this.data = new double[rows][columns];
    }

    public Matrix(double[][] data) {
        this.data = data;
    }

    public Matrix(double[] vec) {
        this.data = new double[][]{vec};
    }

    public Matrix(SimpleMatrix simpleMatrix) {
        this.data = new double[simpleMatrix.numRows()][simpleMatrix.numCols()];
        for (int i = 0; i < simpleMatrix.numRows(); i++) {
            for (int j = 0; j < simpleMatrix.numCols(); j++) {
                data[i][j] = simpleMatrix.get(i, j);
            }
        }
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
        return new Matrix(this.toSimpleMatrix().transpose());
    }

    public Matrix mult(Matrix other) {
        return new Matrix(this.toSimpleMatrix().mult(other.toSimpleMatrix()));
    }

    private SimpleMatrix toSimpleMatrix() {
        return new SimpleMatrix(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double[] row : data) {
            sb.append(Arrays.toString(row));
            sb.append("\n");
        }
        return "Matrix{\n" + sb + "}\n";
    }
}
