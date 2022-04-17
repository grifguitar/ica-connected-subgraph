import graph.Graph;
import io.GraphIO;
import io.MatrixIO;
import utils.DataAnalysis;
import utils.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    static final String F_DEBUG = "./logs/debug.txt";
    static final String F_OUT = "./logs/out.txt";

    public static void main(String[] args) {
        try {

            Map<Integer, String> rev_map = new HashMap<>();
            Map<String, Integer> map = new HashMap<>();

            Matrix matrix = MatrixIO.read("./input_data/test_small_025.mtx", true, rev_map, map);

            matrix = whitening(matrix);

            Graph graph = GraphIO.read("./input_data/test_small_025.graph", map);

            System.out.println(matrix);

            SolverCallback solver = new SolverCallback(matrix, graph);

            if (solver.solve()) {
                try (PrintWriter out = new PrintWriter("./graphics/p.txt")) {
                    solver.printResults(out);
                }

                Matrix ans = MatrixIO.read("./input_data/test_small_025.ans", true, null, null);

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

            SolverCallback.out_debug.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Matrix whitening(Matrix matrix) {
        try {
            try (PrintWriter out = new PrintWriter(F_OUT, StandardCharsets.UTF_8)) {
                try (PrintWriter err = new PrintWriter(F_DEBUG, StandardCharsets.UTF_8)) {

                    Matrix st_mtx = DataAnalysis.standartization(matrix, err, true, true);

                    Matrix cov_mtx = DataAnalysis.getCovMatrix(st_mtx, err, true);
                    err.println("cov_mtx:");
                    err.println(cov_mtx);

                    Matrix cov_mtx_2 = DataAnalysis.getCovMatrix2(st_mtx);
                    err.println("cov_mtx_2:");
                    err.println(cov_mtx_2);

                    Matrix pca_mtx = DataAnalysis.pca(st_mtx, err, 2);

                    Matrix cov_mtx_pca_mtx = DataAnalysis.getCovMatrix(pca_mtx, err, true);
                    err.println("cov_mtx_pca_mtx:");
                    err.println(cov_mtx_pca_mtx);

                    Matrix final_mtx = DataAnalysis.standartization(pca_mtx, err, true, true);
                    out.println("final_mtx:");
                    out.println(final_mtx);

                    Matrix cov_final_mtx = DataAnalysis.getCovMatrix(final_mtx, err, true);
                    err.println("cov_final_mtx:");
                    err.println(cov_final_mtx);

                    return final_mtx;

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
