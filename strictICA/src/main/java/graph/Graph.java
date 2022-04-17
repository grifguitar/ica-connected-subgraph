package graph;

import utils.Pair;

import java.util.List;

public record Graph(List<List<Pair<Integer, Long>>> graph, List<Pair<Integer, Integer>> edgesList) {

    public List<Pair<Integer, Integer>> getEdges() {
        return edgesList;
    }

    public List<Pair<Integer, Long>> edgesOf(int v) {
        return graph.get(v);
    }

    public int getNodesCount() {
        return graph.size();
    }
}
