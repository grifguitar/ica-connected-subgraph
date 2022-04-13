package graph;

public class Node {
    private final int num;

    public Node(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        return num == node.getNum();
    }

    @Override
    public int hashCode() {
        return num;
    }
}
