import graph.Graph;
import io.GraphIO;
import io.MatrixIO;
import utils.DataAnalysis;
import utils.Matrix;
import utils.Pair;
import utils.ROC;
import visual.DrawAPI;

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

            NewSolver solver = new NewSolver(matrix, graph);

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

            drawing("tmp");
            DrawAPI.run();

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

    public static void drawing(String name) {
        try {
            BufferedReader r = new BufferedReader(new FileReader("./graphics/p.txt", StandardCharsets.UTF_8));
            BufferedReader r1 = new BufferedReader(new FileReader("./graphics/p_ans_0.txt", StandardCharsets.UTF_8));
            BufferedReader r2 = new BufferedReader(new FileReader("./graphics/p_ans_1.txt", StandardCharsets.UTF_8));
            BufferedReader r3 = new BufferedReader(new FileReader("./graphics/p_ans_2.txt", StandardCharsets.UTF_8));
            BufferedReader r4 = new BufferedReader(new FileReader("./graphics/p_ans_3.txt", StandardCharsets.UTF_8));
            Double[] p = r.lines().map(Double::parseDouble).toList().toArray(new Double[0]);
            Boolean[] p1 = r1.lines().map(Double::parseDouble).map(x -> (x.intValue() == 1)).toList().toArray(new Boolean[0]);
            Boolean[] p2 = r2.lines().map(Double::parseDouble).map(x -> (x.intValue() == 1)).toList().toArray(new Boolean[0]);
            Boolean[] p3 = r3.lines().map(Double::parseDouble).map(x -> (x.intValue() == 1)).toList().toArray(new Boolean[0]);
            Boolean[] p4 = r4.lines().map(Double::parseDouble).map(x -> (x.intValue() == 1)).toList().toArray(new Boolean[0]);
            Map<String, Pair<List<Pair<Number, Number>>, String>> lines = new TreeMap<>();
            lines.put("p-vs-0", ROC.getLine(p, p1));
            lines.put("p-vs-1", ROC.getLine(p, p2));
            lines.put("p-vs-2", ROC.getLine(p, p3));
            lines.put("p-vs-3", ROC.getLine(p, p4));
            ROC.draw(name, lines);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
