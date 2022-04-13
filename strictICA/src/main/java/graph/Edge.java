package graph;

public class Edge {
    private final Node from;
    private final Node to;

    public Edge(Node from, Node to) {
        this.from = from;
        this.to = to;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;

        Edge edge = (Edge) o;

        assert (from != null);
        assert (to != null);

        return (from.equals(edge.getFrom()) && to.equals(edge.getTo())) ||
                (from.equals(edge.getTo()) && to.equals(edge.getFrom()));
    }

    @Override
    public int hashCode() {
        return from.hashCode() ^ to.hashCode();
    }
}
