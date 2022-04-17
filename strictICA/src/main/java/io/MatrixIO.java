package io;

import utils.Matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MatrixIO {
    public static Matrix read(String f, boolean WITH_NAME, Map<Integer, String> rev_map, Map<String, Integer> map) {
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {

                List<List<Double>> matrixRaw = new ArrayList<>();

                String line;
                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split("\\s");

                    int startPos = 0;

                    if (WITH_NAME) {
                        if (rev_map != null && map != null) {
                            if (!map.containsKey(tokens[0])) {
                                map.put(tokens[0], map.size());
                                int id = map.get(tokens[0]);
                                if (!rev_map.containsKey(id)) {
                                    rev_map.put(id, tokens[0]);
                                } else {
                                    throw new RuntimeException("rev_map already contains key: " + tokens[0]);
                                }
                                if (matrixRaw.size() != id) {
                                    throw new RuntimeException("raw matrix size != id");
                                }
                            } else {
                                throw new RuntimeException("map already contains key: " + tokens[0]);
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

                return new Matrix(matrixData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
