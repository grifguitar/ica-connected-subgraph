package io;

import graph.Graph;
import utils.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraphIO {
    public static Graph read(String f, Map<String, Integer> map) throws IOException {
        Scanner scanner = new Scanner(new FileReader(f, StandardCharsets.UTF_8));

        List<List<Pair<Integer, Long>>> graph = new ArrayList<>();
        List<Pair<Integer, Integer>> edgesList = new ArrayList<>();

        for (int i = 0; i < map.size(); i++) {
            graph.add(new ArrayList<>());
        }

        while (scanner.hasNext()) {
            int _from = map.get(scanner.next());
            int _to = map.get(scanner.next());

            long a = edgesList.size();
            edgesList.add(new Pair<>(_from, _to));
            long b = edgesList.size();
            edgesList.add(new Pair<>(_to, _from));

            graph.get(_from).add(new Pair<>(_to, a));
            graph.get(_to).add(new Pair<>(_from, b));
        }

        return new Graph(graph, edgesList);
    }
}
