import graph.Graph;
import utils.Matrix;
import utils.Pair;

import java.util.*;

public record NewSolution(
        float L1NORM,
        Matrix matrix,
        Graph graph,
        double[] a,
        double[] f,
        double[] g,
        double[] alpha,
        double[] beta,
        double[] q,
        double[] r,
        double[] x
) {
    public boolean adapt() {
        double[] p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);

        double l1norm = 0;
        for (double val : p) {
            l1norm += Math.abs(val);
        }

        if (Math.abs(l1norm) < 0.0001) {
            return false;
        }

        double coeff = L1NORM / l1norm;

        for (int i = 0; i < a.length; i++) {
            a[i] *= coeff;
        }

        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);

        for (int i = 0; i < new_p.length; i++) {
            if (new_p[i] < 0) {
                f[i] = 0;
                alpha[i] = 0;
                g[i] = Math.abs(new_p[i]);
                beta[i] = 1;
            } else {
                f[i] = Math.abs(new_p[i]);
                alpha[i] = 1;
                g[i] = 0;
                beta[i] = 0;
            }
        }

        check();

        for (int i = 0; i < f.length; i++) {
            q[i] = g[i];
        }

        double max_f = -1;
        int argmax_f = -1;
        for (int i = 0; i < q.length; i++) {
            if (q[i] >= max_f) {
                max_f = q[i];
                argmax_f = i;
            }
        }

        if (argmax_f == -1 || max_f == -1) {
            throw new RuntimeException("not found max in q[] array");
        }

        int root_ind = argmax_f;
        for (int i = 0; i < graph.getNodesCount(); i++) r[i] = 0;
        r[root_ind] = 1;

        if (q[root_ind] < 1) {
            return false;
        }

        boolean[] visited = new boolean[graph.getNodesCount()];
        boolean[] visitedEdge = new boolean[graph.getEdges().size()];

        dfs(root_ind, visited, visitedEdge);

        for (int i = 0; i < visited.length; i++)
            if (!visited[i]) throw new RuntimeException("unexpected dfs 1");
        for (int i = 0; i < visitedEdge.length; i++)
            if (!visitedEdge[i]) throw new RuntimeException("unexpected dfs 2");


        return true;
    }

    private void dfs(int u, boolean[] vis, boolean[] visEdge) {
        vis[u] = true;

        List<Pair<Integer, Long>> neighbours = new ArrayList<>(graph.edgesOf(u));
        neighbours.sort(Comparator.comparingDouble(x -> q[x.first]));
        Collections.reverse(neighbours);

        // check:
        for (int i = 0; i < neighbours.size() - 1; i++) {
            if (q[neighbours.get(i).first] < q[neighbours.get(i + 1).first]) {
                throw new RuntimeException("unsorted neighbours");
            }
        }

        for (Pair<Integer, Long> edge : neighbours) {
            //System.out.println(neighbours);

            int to = edge.first;
            int num = edge.second.intValue();
            int back_num = Graph.companionEdge(num);

            // check:
            Pair<Integer, Integer> backE = graph.getEdges().get(back_num);
            if (backE.second != u) {
                throw new RuntimeException("1: unexpected");
            }
            Pair<Integer, Integer> forwE = graph.getEdges().get(num);
            if (forwE.first != u) {
                throw new RuntimeException("2: unexpected");
            }
            if (!Objects.equals(forwE.second, backE.first)) {
                throw new RuntimeException("3: unexpected");
            }

            if (!vis[to]) {
                x[num] = 1;
                x[back_num] = 0;
                visEdge[num] = true;
                visEdge[back_num] = true;
                if (q[u] - q[to] < 0.01) {
                    q[to] = Math.max(q[u] - 0.01, 0);
                }
                dfs(to, vis, visEdge);
            } else {
                if (!visEdge[num]) {
                    x[num] = 0;
                    x[back_num] = 0;
                    visEdge[num] = true;
                    visEdge[back_num] = true;
                }
            }

        }
    }

    private void check() {
        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
        if (new_p.length != f.length) {
            throw new RuntimeException("solution check failed: 1");
        }
        double l1norm = 0;
        for (int i = 0; i < new_p.length; i++) {
            if (Math.abs(new_p[i] - (f[i] - g[i])) > 0.000001) {
                throw new RuntimeException("solution check failed: 2");
            }
            l1norm += Math.abs(new_p[i]);
        }
        if (Math.abs(l1norm - L1NORM) > 0.000001) {
            throw new RuntimeException("solution check failed: 3");
        }
        for (int i = 0; i < alpha.length; i++) {
            if (!((alpha[i] == 0 && beta[i] == 1) || (alpha[i] == 1 && beta[i] == 0))) {
                throw new RuntimeException("solution check failed: 4");
            }
        }
    }

    @Override
    public String toString() {
        return "NewSolution{" +
                "\n| a= " + Arrays.toString(a) +
                "\n| f= " + Arrays.toString(f) +
                "\n| g= " + Arrays.toString(g) +
                "\n| alpha= " + Arrays.toString(alpha) +
                "\n| beta= " + Arrays.toString(beta) +
                "\n| q= " + Arrays.toString(q) +
                "\n| r= " + Arrays.toString(r) +
                "\n| x= " + Arrays.toString(x) +
                "\n}";
    }
}
