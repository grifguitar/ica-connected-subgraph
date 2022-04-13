package utils;

public class Tuple<S, T, R> {
    public S first;
    public T second;
    public R third;

    public Tuple(S first, T second, R third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public String toString() {
        return "{" + first + ", " + second + ", " + third + "}";
    }
}
