package io;

import graph.Edge;
import graph.Graph;
import graph.Node;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraphIO {
    public static Graph read(String f, Map<String, Integer> map) throws IOException {
        Scanner scanner = new Scanner(new FileReader(f, StandardCharsets.UTF_8));

        Set<Edge> edges = new HashSet<>();
        Set<Node> nodes = new HashSet<>();
        Map<Node, Set<Edge>> g = new HashMap<>();

        for (Integer val : map.values()) {
            nodes.add(new Node(val));
        }

        while (scanner.hasNext()) {

            Node left = new Node(map.get(scanner.next()));
            Node right = new Node(map.get(scanner.next()));

            Edge edge = new Edge(left, right);
            edges.add(edge);

            if (!g.containsKey(left)) {
                Set<Edge> set = new HashSet<>();
                set.add(edge);
                g.put(left, set);
            } else {
                g.get(left).add(edge);
            }

            if (!g.containsKey(right)) {
                Set<Edge> set = new HashSet<>();
                set.add(edge);
                g.put(right, set);
            } else {
                g.get(right).add(edge);
            }

        }

        return new Graph(edges, nodes, g);
    }

    public static int putAndGetId(Map<String, Integer> map, String label) {
        map.putIfAbsent(label, map.size());
        return map.get(label);
    }
}
