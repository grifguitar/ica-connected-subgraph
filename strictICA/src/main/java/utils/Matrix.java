package utils;

public class Matrix {
    public double[][] data;

    public Matrix(int rows, int columns) {
        this.data = new double[rows][columns];
    }

    public Matrix(double[][] data) {
        this.data = data;
    }

    public int numCols() {
        return data[0].length;
    }

    public int numRows() {
        return data.length;
    }

    public void print() {
        System.out.println("##### MATRIX PRINT: #####");
        for (double[] row : data) {
            for (double elem : row) {
                System.out.print(elem + " ");
            }
            System.out.println();
        }
    }

}
