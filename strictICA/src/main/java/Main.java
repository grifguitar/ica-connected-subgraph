import utils.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final boolean WITH_NAME = false;
    private static final String F_IN = "./input_data/simple_mtx.mtx";
    private static final String F_OUT = "./output_data/output.txt";

    private static Matrix read() {
        try {
            try (PrintWriter out = new PrintWriter(F_OUT, StandardCharsets.UTF_8)) {
                try (BufferedReader in = new BufferedReader(new FileReader(F_IN, StandardCharsets.UTF_8))) {

                    List<List<Double>> matrixRaw = new ArrayList<>();

                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] tokens = line.split("\\s");

                        int startPos = 0;

                        if (WITH_NAME) {
                            out.println(tokens[0]);
                            startPos = 1;
                        }

                        List<Double> row = new ArrayList<>();
                        for (int i = startPos; i < tokens.length; i++) {
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

                    return new Matrix(matrixData);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {

            Matrix matrix = read();

            matrix.print();

            Solver solver = new Solver(matrix);

            if (solver.solve()) {
                solver.printResults();
            } else {
                System.out.println("debug: results not found!");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
