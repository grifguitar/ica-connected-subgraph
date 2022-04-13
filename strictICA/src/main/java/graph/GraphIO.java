package graph;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphIO {
    private Set<Edge> edges;

    public Graph read(String file, Map<String, Integer> map) throws IOException {
        Scanner scanner = new Scanner(new FileReader(file));
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
        //System.out.println("Finished");
    }

    public static int getId(Map<String, Integer> map, String label) {
        map.putIfAbsent(label, map.size());
        return map.get(label);
    }

    private static Pattern regexp = Pattern.compile("^([^(]+)\\([^)]+\\)$");

    public static String extractName(String raw) {
        Matcher matcher = regexp.matcher(raw);
        if (!matcher.matches()) {
            throw new RuntimeException("No name in " + raw);
        }
        return matcher.group(1);
    }
}
