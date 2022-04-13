import utils.DataAnalysis;
import utils.Matrix;
import utils.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static utils.DataAnalysis.pca;
import static utils.DataAnalysis.standartization;

public class Main {
    static final boolean MODE = true;
    static final String F_IN = "./input_data/simple_mtx.mtx";
    static final String F_OUT = "./output_data/output.txt";
    static final String F_OUT_2 = "./output_data/output2.txt";

    static Matrix read(String f, boolean DEBUG, boolean WITH_NAME) {
        try {
            try (PrintWriter out = new PrintWriter(F_OUT, StandardCharsets.UTF_8)) {
                try (BufferedReader in = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {

                    List<List<Double>> matrixRaw = new ArrayList<>();

                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] tokens = line.split("\\s");

                        int startPos = 0;

                        if (WITH_NAME) {
                            if (DEBUG) {
                                for (int i = 1; i < tokens.length; i++) {
                                    out.print(tokens[i]);
                                    out.print("\t");
                                }
                                out.print("\n");
                            }
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

            Matrix matrix = read(F_IN, false, false);

            if (MODE) {

                System.out.println(matrix);

                Solver solver = new Solver(matrix);

                if (solver.solve()) {
                    solver.printResults();

                    Matrix ans = read("./input_data/test_small_025.ans", false, true);

                    for (int w = 0; w < ans.numCols(); w++) {
                        try (PrintWriter out = new PrintWriter("./graphics/p_ans_" + w + ".txt")) {
                            for (int i = 0; i < ans.numRows(); i++) {
                                out.println(ans.getElem(i, w));
                            }
                        }
                    }

                } else {
                    System.out.println("debug: results not found!");
                }

            } else {

                solve(matrix);

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void solve(Matrix matrix) {
        try {
            try (PrintWriter out2 = new PrintWriter(F_OUT_2, StandardCharsets.UTF_8)) {
                try (PrintWriter out = new PrintWriter(F_OUT, StandardCharsets.UTF_8)) {
                    Matrix st_mtx = DataAnalysis.standartization(matrix, out, true, true);

                    Matrix cov_mtx = DataAnalysis.getCovMatrix(st_mtx, out, true);
                    out.println();
                    out.println("cov_mtx");
                    out.println(cov_mtx);
                    out.println();

                    Matrix cov_mtx_2 = DataAnalysis.getCovMatrix2(st_mtx);
                    out.println();
                    out.println("cov_mtx_2");
                    out.println(cov_mtx_2);
                    out.println();

                    Matrix pca = pca(st_mtx, 2);
//                    out2.println();
//                    out2.println("pca");
//                    out2.println(pca);
//                    out2.println();

                    Matrix cov_mtx_pca = DataAnalysis.getCovMatrix(pca, out, true);
                    out.println();
                    out.println("cov_mtx_pca");
                    out.println(cov_mtx_pca);
                    out.println();

                    Matrix final_mtx = standartization(pca, out, true, true);
                    out2.println("final_mtx");
                    out2.println(final_mtx);

                    Matrix cov_final_mtx = DataAnalysis.getCovMatrix(final_mtx, out, true);
                    out.println();
                    out.println("cov_final_mtx");
                    out.println(cov_final_mtx);
                    out.println();

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
