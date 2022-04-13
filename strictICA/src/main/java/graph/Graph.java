package graph;

import java.util.Map;
import java.util.Set;

public record Graph(Set<Edge> edges, Set<Node> nodes, Map<Node, Set<Edge>> g) {

    public Set<Edge> getEdges() {
        return edges;
    }

    public Set<Edge> edgesOf(Node v) {
        return g.get(v);
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}
