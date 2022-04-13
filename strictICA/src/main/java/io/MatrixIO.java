package io;

import utils.Matrix;
import utils.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.GraphIO.putAndGetId;

public class MatrixIO {
    public static Pair<Matrix, Map<String, Integer>> read(String f, boolean WITH_NAME, Map<Integer, String> rev_map) {
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {

                Map<String, Integer> map = new HashMap<>();

                List<List<Double>> matrixRaw = new ArrayList<>();

                String line;
                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split("\\s");

                    int startPos = 0;

                    if (WITH_NAME) {
                        int id = putAndGetId(map, tokens[0]);
                        if (rev_map != null) {
                            if (!rev_map.containsKey(id)) {
                                rev_map.put(id, tokens[0]);
                            } else {
                                String name = rev_map.get(id);
                                if (!Objects.equals(name, tokens[0])) {
                                    throw new RuntimeException("some different name with equals id");
                                }
                            }
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

                return new Pair<>(new Matrix(matrixData), map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
