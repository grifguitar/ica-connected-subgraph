import utils.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        BufferedReader in;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new FileReader("./input_data/test_small_025.mtx", StandardCharsets.UTF_8));
            out = new PrintWriter("./output_data/output.txt", StandardCharsets.UTF_8);

            List<List<Double>> matrixRaw = new ArrayList<>();

            String line;
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split("\\s");

                out.println(tokens[0]);

                List<Double> row = new ArrayList<>();
                for (int i = 1; i < tokens.length; i++) {
                    row.add(Double.parseDouble(tokens[i]));
                }
                matrixRaw.add(row);
            }

            double[][] matrixData = new double[matrixRaw.size()][matrixRaw.get(0).size()];
            for (int i = 0; i < matrixRaw.size(); i++) {
                if (matrixRaw.get(i).size() != matrixRaw.get(0).size()) {
                    throw new RuntimeException("wrong input");
                }
                for (int j = 0; j < matrixRaw.get(i).size(); j++) {
                    matrixData[i][j] = matrixRaw.get(i).get(j);
                }
            }

            Matrix matrix = new Matrix(matrixData);

            matrix.print();

            Solver solver = new Solver(matrix);
            if (solver.solve()) {
                solver.printResults();
            } else {
                System.out.println("debug: results not found!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
